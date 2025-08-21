package sir_draco.survivalskills.bosses;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.ProjectileCalculator;

import java.util.ArrayList;

public class DragonBoss extends Boss {

    private final EnderDragon dragon;
    private final ArrayList<Location> crystalLocations = new ArrayList<>();
    private final ArrayList<Player> players = new ArrayList<>();

    // Maximum allowed vertical height for the dragon before being forced back down
    private static final int MAX_DRAGON_HEIGHT = 150; // blocks (Y coordinate)

    private int lightningDefault = 20 * 30;
    private int lightningCounter = lightningDefault;
    private int attackDefault = 20 * 10;
    private int attackCounter = attackDefault;
    private int timeSinceDragonFollowerSpawn = 0;
    private boolean regeneratedCrystals = false;
    private boolean enableStages = false;
    private boolean gotCrystals = false;
    private boolean isRespawn = false;

    public DragonBoss(String name, int spawnRadiusRequired, int spawnHeightRequired, double maxHealth, double damage,
            double defense, double speed, EntityType type, Location loc, LivingEntity entity) {
        super(name, spawnRadiusRequired, spawnHeightRequired, maxHealth, damage, defense, speed, type, loc, 3);
        setBoss(entity);
        dragon = (EnderDragon) getBoss();
        dragonAttributes();

        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Ender Dragon: "
                + ChatColor.RESET + "So you have finally come to challenge me?");
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().getEnvironment().equals(World.Environment.THE_END))
                continue;
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 1);
        }
    }

    @Override
    public void run() {
        if (dragon.isDead()) {
            cancel();
            return;
        }
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            cancel();
            return;
        }

        // Ensure the dragon does not fly too high above the End island
        enforceAltitudeLimit();

        if (!gotCrystals) {
            getCrystalLocations();
            gotCrystals = true;
        }

        checkStage(false, Sound.ENTITY_ENDER_DRAGON_GROWL);
        if (isDisableAttack())
            return;
        if (lightningCounter == 0) {
            lightningStrike((int) Math.max(5, (1 - getHealthPercentage()) * 40));
            lightningCounter = lightningDefault;
        } else
            lightningCounter--;

        if (attackCounter == 0) {
            attack();
            attackCounter = attackDefault;
        } else
            attackCounter--;

        if (!enableStages) {
            enableStages = true;
            setAppliedAttributes(true);
        }

        if (timeSinceDragonFollowerSpawn > 0)
            timeSinceDragonFollowerSpawn--;
    }

    @Override
    public void attack() {
        if (getStage() == 1) {
            if (Math.random() < 0.5)
                cannon();
            else
                deathRain();
        } else if (getStage() == 2) {
            if (attackDefault != 160) {
                attackDefault = 20 * 4;
                lightningDefault = 20 * 10;
            }
            if (Math.random() < 0.5)
                cannon();
            else
                deathRain();
        } else {
            if (!regeneratedCrystals) {
                resetCrystals();
                Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Ender Dragon: "
                        + ChatColor.RESET + "I will not go down so easily");
                regeneratedCrystals = true;
            }
            if (attackDefault != 100) {
                lightningDefault = 20 * 5;
                attackDefault = 20;
            }

            double chance = Math.random();
            if (chance > 0.66)
                cannon();
            else if (chance > 0.33)
                deathRain();
            else
                spawnAngryEndermen();
        }
    }

    @Override
    public void deathAnimation() {
        if (dragon.getWorld().hasMetadata("killedfirstdragon")) {
            Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Ender Dragon: "
                    + ChatColor.RESET + "I always come back");
        } else {
            dragon.getWorld().setGameRule(GameRule.KEEP_INVENTORY, false);
            Bukkit.broadcastMessage(
                    ChatColor.GRAY + "[Server] " + ChatColor.ITALIC + "Keep Inventory Disabled in the End");
            new BukkitRunnable() {
                @Override
                public void run() {
                    Bukkit.broadcastMessage(ChatColor.GREEN + "The Exiled One can now be summoned!");
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1, 1);
                    }
                }
            }.runTaskLater(SurvivalSkills.getPlugin(SurvivalSkills.class), 20 * 15);

            Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Ender Dragon: "
                    + ChatColor.RESET + "This is only the beginning");

            dragon.getWorld().setMetadata("killedfirstdragon",
                    new FixedMetadataValue(SurvivalSkills.getPlugin(SurvivalSkills.class), true));
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "toggleoverworldfirstdragon");
        }
        cancel();
    }

    public void dragonAttributes() {
        AttributeInstance health = dragon.getAttribute(Attribute.MAX_HEALTH);
        if (health != null)
            health.setBaseValue(getMaxHealth());
        dragon.setHealth(getMaxHealth());
        dragon.setMetadata("boss", new FixedMetadataValue(SurvivalSkills.getPlugin(SurvivalSkills.class), true));
        if (!dragon.getWorld().hasMetadata("killedfirstdragon")) {
            dragon.getWorld().setGameRule(GameRule.KEEP_INVENTORY, true);
            Bukkit.broadcastMessage(
                    ChatColor.GRAY + "[Server] " + ChatColor.ITALIC + "Keep Inventory Enabled in the End");
        }
    }

    /**
     * Keeps the dragon from flying excessively high. If the dragon's Y exceeds
     * MAX_DRAGON_HEIGHT it is teleported back down just above the highest solid
     * block at its current X/Z (to avoid suffocation) and given a slight downward
     * velocity so it resumes normal flight.
     */
    private void enforceAltitudeLimit() {
        Location loc = dragon.getLocation();
        if (loc.getY() <= MAX_DRAGON_HEIGHT)
            return;
        World world = loc.getWorld();
        if (world == null)
            return;

        int highestY = world.getHighestBlockYAt(loc.getBlockX(), loc.getBlockZ());
        double targetY = highestY + 10; // a little above the surface / obsidian pillars
        // Safety clamp in case highestY is void-ish or extreme
        if (targetY < 5)
            targetY = 70; // fallback typical island height

        Location newLoc = new Location(world, loc.getX(), targetY, loc.getZ(), loc.getYaw(), loc.getPitch());
        dragon.teleport(newLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
        dragon.setVelocity(new Vector(0, -0.6, 0));

        // Optional subtle particles to mask teleport (avoid spam by not broadcasting)
        world.spawnParticle(Particle.PORTAL, newLoc.clone().add(0, 2, 0), 30, 1, 1, 1, 0.1);
    }

    public void lightningStrike(int numStrikes) {
        Location orgLoc = dragon.getLocation();
        World end = dragon.getWorld();

        int radius = 30;
        for (int i = 0; i < numStrikes; i++) {
            Location loc = randomLoc(orgLoc, radius);
            if (loc != null)
                end.strikeLightning(loc);
        }
    }

    public Location randomLoc(Location origin, int radius) {
        if (origin.getY() < 30)
            return null;

        final int maxTries = 40;
        for (int tries = 0; tries < maxTries; tries++) {
            double x = Math.floor((Math.random() - 0.5) * radius * 2);
            double z = Math.floor((Math.random() - 0.5) * radius * 2);
            Location probe = origin.clone().add(x, 0, z);

            // Descend until we hit a solid, non‑air block (cap descent to avoid void walks)
            int maxDescent = 64;
            int descent = 0;
            while (probe.getBlock().isPassable() && descent < maxDescent && probe.getY() > -32) {
                probe.subtract(0, 1, 0);
                descent++;
            }
            if (descent == maxDescent)
                continue;

            Material m = probe.getBlock().getType();
            if (m == Material.BEDROCK
                    || m == Material.END_PORTAL
                    || m == Material.END_PORTAL_FRAME) {
                continue; // Skip portal / frame / bedrock
            }

            return probe;
        }
        return null;
    }

    public void cannon() {
        // Get nearby player as a target
        Player p = null;
        for (Entity ent : dragon.getNearbyEntities(100, 100, 100)) {
            if (!(ent instanceof Player player))
                continue;
            if (!players.contains(player))
                continue;
            p = player;
            break;
        }
        if (p == null)
            return;

        Location targetLocation = p.getLocation().clone();
        Location originLocation = dragon.getLocation().clone();
        Vector vector = ProjectileCalculator.getNoGravityVector(originLocation, targetLocation, 1.0);

        new BukkitRunnable() {

            private final Location targetLoc = targetLocation;
            private final Location loc = originLocation;
            private final Vector vec = vector;

            @Override
            public void run() {
                if (loc.getWorld() == null) {
                    cancel();
                    return;
                }

                if (loc.distance(targetLoc) < 1) {
                    loc.getWorld().createExplosion(loc, 3, false, false);
                    cancel();
                    return;
                }
                loc.add(vec);
                loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 3);
            }
        }.runTaskTimer(SurvivalSkills.getPlugin(SurvivalSkills.class), 0, 1);
    }

    public void spawnAngryEndermen() {
        if (timeSinceDragonFollowerSpawn > 0) {
            cannon();
            return;
        }

        for (int i = 0; i < 5; i++) {
            Location loc = randomLoc(dragon.getLocation(), 30);
            if (loc == null)
                continue;
            Enderman eman = (Enderman) dragon.getWorld().spawnEntity(loc, EntityType.ENDERMAN);
            eman.setCustomName(ChatColor.LIGHT_PURPLE + "Dragon Worshipper");
            eman.setCustomNameVisible(true);
            AttributeInstance health = eman.getAttribute(Attribute.MAX_HEALTH);
            if (health != null)
                health.setBaseValue(50);
            eman.setHealth(50);
            AttributeInstance speed = eman.getAttribute(Attribute.MOVEMENT_SPEED);
            if (speed != null)
                speed.setBaseValue(0.5);
            AttributeInstance damage = eman.getAttribute(Attribute.ATTACK_DAMAGE);
            if (damage != null)
                damage.setBaseValue(15);

            timeSinceDragonFollowerSpawn = 20 * 30;
            eman.getWorld().spawnParticle(Particle.PORTAL, eman.getLocation().clone().add(0, 1, 0), 15);
            Player p = null;
            for (Entity ent : eman.getNearbyEntities(10, 10, 10)) {
                if (!(ent instanceof Player))
                    continue;
                p = (Player) ent;
                break;
            }
            if (p == null)
                continue;
            eman.setTarget(p);
            eman.teleportTowards(p);
            eman.attack(p);
        }
    }

    public void deathRain() {
        Location startLoc = dragon.getLocation().clone();
        for (int i = 0; i < 8; i++) {
            Vector finalVec = getVector(i);
            new BukkitRunnable() {

                private final World world = dragon.getWorld();
                private final Vector vector = finalVec;
                private final Location loc = startLoc.clone();

                @Override
                public void run() {
                    if (loc.getY() <= -10) {
                        cancel();
                        return;
                    }
                    if (!loc.getBlock().isEmpty()) {
                        // Set the block above on fire
                        loc.add(0, 1, 0).getBlock().setType(Material.FIRE);
                        if (loc.getWorld() != null)
                            loc.getWorld().createExplosion(loc, 4, false, false);
                        cancel();
                        return;
                    }

                    world.spawnParticle(Particle.DUST, loc, 1, new Particle.DustOptions(Color.PURPLE, 1));
                    loc.add(vector);

                    // Slow down the movement in each direction
                    if (vector.getX() < 0 && Math.abs(vector.getX()) >= 0.01)
                        vector.setX(vector.getX() + 0.01);
                    if (vector.getX() >= 0.01)
                        vector.setX(vector.getX() - 0.01);
                    if (vector.getZ() < 0 && Math.abs(vector.getZ()) >= 0.01)
                        vector.setZ(vector.getZ() + 0.01);
                    if (vector.getZ() >= 0.01)
                        vector.setZ(vector.getZ() - 0.01);
                }
            }.runTaskTimer(SurvivalSkills.getPlugin(SurvivalSkills.class), 0, 1);
        }
    }

    private static Vector getVector(int i) {
        return switch (i) {
            case 0 -> new Vector(0.5, -0.1, 0.5);
            case 1 -> new Vector(-0.5, -0.1, 0.5);
            case 2 -> new Vector(0.5, -0.1, -0.5);
            case 3 -> new Vector(-0.5, -0.1, -0.5);
            case 4 -> new Vector(0.5, -0.1, 0);
            case 5 -> new Vector(-0.5, -0.1, 0);
            case 6 -> new Vector(0, -0.1, 0.5);
            case 7 -> new Vector(0, -0.1, -0.5);
            default -> null;
        };
    }

    public void lifeSteal() {
        AttributeInstance health = dragon.getAttribute(Attribute.MAX_HEALTH);
        if (health == null)
            return;
        dragon.setHealth(Math.min(dragon.getHealth() + 20, health.getValue()));
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Ender Dragon: " +
                ChatColor.RESET + "I will feed off your fallen comrade");
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().getEnvironment().equals(World.Environment.THE_END))
                continue;
            p.playSound(p, Sound.ENTITY_GENERIC_DRINK, 1, 1);
        }
    }

    public void getCrystalLocations() {
        World world = dragon.getWorld();
        for (Entity ent : world.getEntities()) {
            if (!(ent instanceof EnderCrystal))
                continue;
            crystalLocations.add(ent.getLocation());
        }
    }

    public void resetCrystals() {
        for (Location loc : crystalLocations) {
            if (loc.getWorld() == null)
                continue;
            EnderCrystal crystal = (EnderCrystal) loc.getWorld().spawnEntity(loc, EntityType.END_CRYSTAL);
            crystal.setBeamTarget(dragon.getLocation());
        }
    }

    public void setCanAttackPlayers(ArrayList<Player> players) {
        isRespawn = true;
        this.players.addAll(players);
    }

    public void addCanAttackPlayer(Player p) {
        players.add(p);
    }

    public ArrayList<Player> getPlayers() {
        return players;
    }

    public boolean isRespawn() {
        return isRespawn;
    }
}
