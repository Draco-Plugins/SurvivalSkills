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
import sir_draco.survivalskills.bosses.Attacks.DeathRainProjectile;
import sir_draco.survivalskills.bosses.Attacks.DragonCannon;
import sir_draco.survivalskills.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DragonBoss extends Boss {
    // Maximum allowed vertical height for the dragon before being forced back down
    private static final int MAX_DRAGON_HEIGHT = 150; // blocks (Y coordinate)
    private static final List<Vector> DEATH_RAIN_VECTORS = List.of(
        new Vector(0.5, -0.1, 0.5),
        new Vector(-0.5, -0.1, 0.5),
        new Vector(0.5, -0.1, -0.5),
        new Vector(-0.5, -0.1, -0.5),
        new Vector(0.5, -0.1, 0),
        new Vector(-0.5, -0.1, 0),
        new Vector(0, -0.1, 0.5),
        new Vector(0, -0.1, -0.5)
    );
    private static final int MAX_ENDERMAN_SPAWNS = 5;

    private EnderDragon dragon;
    private final List<Location> crystalLocations = new ArrayList<>();
    private final List<Player> players = new ArrayList<>();

    private int lightningCounter = 20 * 30;
    private int attackCounter = 20 * 10;
    private int timeSinceDragonFollowerSpawn = 0;
    private boolean initialized = false;
    private boolean crystalsFound = false;
    private boolean isRespawn = false;

    private DragonBoss(String name, int spawnRadiusRequired, int spawnHeightRequired, double maxHealth, double damage,
            double defense, double speed) {
        super(name, spawnRadiusRequired, spawnHeightRequired, maxHealth, damage, defense, speed, 3);
    }

    public static DragonBoss attachToDragon(String name, int spawnRadiusRequired, int spawnHeightRequired,
                                            double maxHealth, double damage, double defense, double speed,
                                            LivingEntity entity) {
        DragonBoss boss = new DragonBoss(name, spawnRadiusRequired, spawnHeightRequired, maxHealth, damage, defense, speed);
        boss.attachToEntity(entity);
        boss.dragon = (EnderDragon) entity;
        boss.dragonAttributes();

        broadcastDragonMessage(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Ender Dragon: "
                + ChatColor.RESET + "So you have finally come to challenge me?", Sound.ENTITY_ENDER_DRAGON_GROWL);
        return boss;
    }

    @Override
    public void run() {
        if (shouldCancel()) {
            cancel();
            return;
        }

        enforceAltitudeLimit();
        initializeCrystals();
        handleStageTransitions();
        tickCooldowns();

        if (isDisableAttack()) return;
        executeLightningStrike();
        executeAttack();
    }

    private boolean shouldCancel() {
        return dragon.isDead() || dragon.getHealth() <= 0 || Bukkit.getOnlinePlayers().isEmpty();
    }

    private void initializeCrystals() {
        if (crystalsFound) return;
        getCrystalLocations();
        crystalsFound = true;
    }

    private void handleStageTransitions() {
        int stageBefore = getStage();
        checkStage(false, Sound.ENTITY_ENDER_DRAGON_GROWL);
        if (stageBefore != getStage()) {
            onStageEnter(getStage());
        }
    }

    private void tickCooldowns() {
        if (!initialized) {
            initialized = true;
            setAppliedAttributes(true);
        }

        if (lightningCounter > 0) lightningCounter--;
        if (attackCounter > 0) attackCounter--;
        if (timeSinceDragonFollowerSpawn > 0) timeSinceDragonFollowerSpawn--;
    }

    private void executeLightningStrike() {
        if (lightningCounter > 0) return;
        lightningStrike((int) Math.max(5, (1 - getHealthPercentage()) * 40));
        lightningCounter = getLightningCooldown();
    }

    private void executeAttack() {
        if (attackCounter > 0) return;
        attack();
        attackCounter = getAttackCooldown();
    }

    private int getLightningCooldown() {
        return switch (getStage()) {
            case 1 -> 20 * 30;
            case 2 -> 20 * 10;
            case 3 -> 20 * 5;
            default -> 20 * 30;
        };
    }

    private int getAttackCooldown() {
        return switch (getStage()) {
            case 1 -> 20 * 10;
            case 2 -> 20 * 4;
            case 3 -> 20;
            default -> 20 * 10;
        };
    }

    @Override
    public void attack() {
        switch (getStage()) {
            case 1, 2 -> {
                if (Math.random() < 0.5) executeCannonAttack();
                else deathRain();
            }
            case 3 -> {
                double chance = Math.random();
                if (chance > 0.66) executeCannonAttack();
                else if (chance > 0.33) deathRain();
                else spawnAngryEndermen();
            }
        }
    }

    private void onStageEnter(int newStage) {
        if (newStage == 3) {
            resetCrystals();
            broadcastDragonMessage(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Ender Dragon: "
                    + ChatColor.RESET + "I will not go down so easily", Sound.ENTITY_ENDER_DRAGON_HURT);
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
                    broadcastDragonMessage(ChatColor.GREEN + "The Exiled One can now be summoned!", Sound.ENTITY_WITHER_SPAWN);
                }
            }.runTaskLater(SurvivalSkills.getInstance(), 20 * 30);

            Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Ender Dragon: "
                    + ChatColor.RESET + "This is only the beginning");

            dragon.getWorld().setMetadata("killedfirstdragon",
                    new FixedMetadataValue(SurvivalSkills.getInstance(), true));
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "toggleoverworldfirstdragon");
        }
        cancel();
    }

    public void dragonAttributes() {
        double maxHealth = getMaxHealth();
        Utils.updateEntityAttributeInstance(dragon, Attribute.MAX_HEALTH, maxHealth);
        dragon.setHealth(maxHealth);
        dragon.setMetadata("boss", new FixedMetadataValue(SurvivalSkills.getInstance(), true));
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
        if (loc.getY() <= MAX_DRAGON_HEIGHT) return;
        World world = loc.getWorld();
        if (world == null) return;

        int highestY = world.getHighestBlockYAt(loc.getBlockX(), loc.getBlockZ());
        double targetY = highestY + 10; // a little above the surface / obsidian pillars
        // Safety clamp in case highestY is void-ish or extreme
        if (targetY < 5) targetY = 70; // fallback typical island height

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
            randomLoc(orgLoc, radius).ifPresent(end::strikeLightning);
        }
    }

    public Optional<Location> randomLoc(Location origin, int radius) {
        if (origin.getY() < 30) return Optional.empty();

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
            if (descent == maxDescent) continue;

            Material m = probe.getBlock().getType();
            if (m == Material.BEDROCK
                    || m == Material.END_PORTAL
                    || m == Material.END_PORTAL_FRAME) {
                continue; // Skip portal / frame / bedrock
            }

            return Optional.of(probe);
        }
        return Optional.empty();
    }

    public void executeCannonAttack() {
        // Get nearby player as a target
        Optional<Player> nearestTargetOptional = findNearbyTarget(dragon, 150);
        if (nearestTargetOptional.isEmpty()) return;
        Player p = nearestTargetOptional.get();

        new DragonCannon(p.getLocation().clone(), dragon.getLocation().clone()).cannon();
    }

    public void spawnAngryEndermen() {
        if (timeSinceDragonFollowerSpawn > 0) {
            executeCannonAttack();
            return;
        }

        for (int i = 0; i < MAX_ENDERMAN_SPAWNS; i++)
            spawnAngryEnderman();
        timeSinceDragonFollowerSpawn = 20 * 30;
    }

    private void spawnAngryEnderman() {
        // Get spawn location
        Optional<Location> locOptional = randomLoc(dragon.getLocation(), 30);
        if (locOptional.isEmpty()) return;
        Location loc = locOptional.get();

        // Spawn enderman
        Enderman eman = (Enderman) dragon.getWorld().spawnEntity(loc, EntityType.ENDERMAN);
        eman.setCustomName(ChatColor.LIGHT_PURPLE + "Dragon Worshipper");
        eman.setCustomNameVisible(true);
        Utils.updateEntityAttributeInstance(eman, Attribute.MAX_HEALTH, 50);
        eman.setHealth(50);
        Utils.updateEntityAttributeInstance(eman, Attribute.MOVEMENT_SPEED, 0.5);
        Utils.updateEntityAttributeInstance(eman, Attribute.ATTACK_DAMAGE, 15);

        // Handle enderman spawn actions
        eman.getWorld().spawnParticle(Particle.PORTAL, eman.getLocation().clone().add(0, 1, 0), 15);
        Optional<Player> targetOptional = findNearbyTarget(eman, 20);
        if (targetOptional.isEmpty()) return;
        Player p = targetOptional.get();
        eman.setTarget(p);
        eman.teleportTowards(p);
        eman.attack(p);
    }

    public void deathRain() {
        Location startLoc = dragon.getLocation().clone();
        for (int i = 0; i < 8; i++) {
            Vector finalVec = DEATH_RAIN_VECTORS.get(i);
            new DeathRainProjectile(startLoc.clone(), finalVec).launch();
        }
    }

    public void lifeSteal() {
        AttributeInstance health = dragon.getAttribute(Attribute.MAX_HEALTH);
        if (health == null) return;
        dragon.setHealth(Math.min(dragon.getHealth() + 30, health.getValue()));
        broadcastDragonMessage(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Ender Dragon: " +
                ChatColor.RESET + "I will feed off your fallen comrade", Sound.ENTITY_GENERIC_DRINK);
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

    public void respawnDragonInitPlayers(List<Player> players) {
        isRespawn = true;
        this.players.addAll(players);
    }

    public void addPlayer(Player p) {
        players.add(p);
    }

    public List<Player> getPlayers() {
        return players;
    }

    public boolean isRespawn() {
        return isRespawn;
    }


    /**
     * Find the nearest player to the entity, ensuring they are part of the fight
     *
     * @return The player or null if no player is found
     */
    private Optional<Player> findNearbyTarget(LivingEntity entity, int radius) {
        Optional<Player> p = Optional.empty();
        for (Entity ent : entity.getNearbyEntities(radius, radius, radius)) {
            if (!(ent instanceof Player player)) continue;
            if (!players.contains(player)) continue;
            p = Optional.of(player);
            break;
        }
        return p;
    }


    private static void broadcastDragonMessage(String message, Sound sound) {
        Bukkit.broadcastMessage(message);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().getEnvironment().equals(World.Environment.THE_END)) continue;
            p.playSound(p.getLocation(), sound, 1, 1);
        }
    }
}
