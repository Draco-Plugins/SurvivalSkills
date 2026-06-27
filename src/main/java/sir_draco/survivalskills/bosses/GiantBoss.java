package sir_draco.survivalskills.bosses;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.ProjectileCalculator;

import java.util.ArrayList;

public class GiantBoss extends Boss {
    private static final double GIANT_SCALE = 4;
    private static final double HAND_HEIGHT = GIANT_SCALE * 1.625;
    private static final double HAND_OFFSET = GIANT_SCALE * 0.75;
    private static final int ATTACK_COOLDOWN_STAGE_1 = 20 * 10;
    private static final int ATTACK_COOLDOWN_STAGE_2 = 20 * 5;
    private static final int ATTACK_COOLDOWN_STAGE_3 = 20 * 2;
    private static final int ROCK_THROW_MAX_DISTANCE = 50;
    private static final double ROCK_THROW_SPEED = 1.5;

    private Zombie giant;
    private int attackCooldown = ATTACK_COOLDOWN_STAGE_1;
    private int targetCooldown = 20;
    private boolean activeStompJump = false;

    private GiantBoss() {
        super("Giant", 4, 15, 150, 10, 5, 0.2, 3);
    }

    public static GiantBoss create(Location loc) {
        GiantBoss boss = new GiantBoss();
        if (!boss.spawnBoss(EntityType.ZOMBIE, loc)) return null;
        boss.giant = (Zombie) boss.getBoss();
        boss.giant.setGravity(true);
        EntityEquipment helmet = boss.giant.getEquipment();
        if (helmet != null) helmet.setHelmet(new ItemStack(Material.LEATHER_HELMET));
        AttributeInstance size = boss.giant.getAttribute(Attribute.SCALE);
        if (size != null) size.setBaseValue(GIANT_SCALE);
        boss.setTargetRange(100, 50);
        return boss;
    }

    @Override
    public void run() {
        if (boss == null || giant.isDead()) {
            cancel();
            return;
        }

        if (getHealthPercentage() != 1) checkStage(true, Sound.ENTITY_ZOMBIE_HORSE_DEATH);
        updateBossBar();
        manageBossBarPlayers();
        if (isDisableAttack()) return;
        if (targetCooldown > 0) {
            targetCooldown--;
        } else {
            targetCooldown = 20;
            setNearestPlayerAsTarget();
        }

        if (attackCooldown > 0) attackCooldown--;
        else {
            attackCooldown = getCooldownForStage(getStage());
            attack();
        }
    }

    @Override
    public void attack() {
        if (getStage() == 1) startStomp();
        else if (getStage() == 2) {
            double rand = Math.random();
            if (rand < 0.5) startStomp();
            else startRockThrowVolley();

            if (rand < 0.25) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        spawnMinions(3);
                    }
                }.runTaskLater(SurvivalSkills.getPlugin(SurvivalSkills.class), 80);
            }
        }
        else {
            double rand = Math.random();
            if (rand < 0.33) startStomp();
            else if (rand < 0.66) startRockThrowVolley();
            else roar();

            if (rand < 0.25) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        spawnMinions(5);
                    }
                }.runTaskLater(SurvivalSkills.getPlugin(SurvivalSkills.class), 80);
            }
        }
    }

    
    // Method for giant death animation
    @Override
    public void deathAnimation() {
        // Turn the ground beneath the giant into mycelium with the highest density being right below where he died
        Location deathLoc = giant.getLocation().getBlock().getLocation();
        if (deathLoc.getWorld() == null) return;
        World world = deathLoc.getWorld();
        int radius = 5;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -1; y <= radius; y++) {
                    Location blockLoc = world.getBlockAt(x, y, z).getLocation();
                    if (blockLoc.distanceSquared(deathLoc) > 25) continue;
                    if (blockLoc.getBlock().getType() == Material.AIR) continue;
                    if (world.getBlockAt(x, y + 1, z).getType() != Material.AIR) continue;
                    // Increase density close to death location
                    if (Math.random() > (double) 1 / blockLoc.distance(deathLoc)) continue;
                    blockLoc.getBlock().setType(Material.MYCELIUM);
                    break;
                }
            }
        }

        // Spawn potion effect particles near the death location
        for (int d = 1; d <= 10; d ++) {
            for (int i = 0; i < 360; i += 10) {
                if (Math.random() > 0.1 / d) continue;
                double angle = Math.toRadians(i);
                double x = deathLoc.getX() + (d * Math.cos(angle));
                double z = deathLoc.getZ() + (d * Math.sin(angle));
                Location particleLoc = new Location(deathLoc.getWorld(), x, deathLoc.getY() + 0.5, z);
                deathLoc.getWorld().spawnParticle(Particle.ASH, particleLoc, 1);
            }
        }
    }


    public void startStomp() {
        if (activeStompJump) return;
        if (giant.getTarget() == null) return;

        // Launch the giant into the air
        activeStompJump = true;
        Location targetLoc = giant.getTarget().getLocation();
        giant.setVelocity(ProjectileCalculator.getLivingEntityProjectileVector(giant.getLocation(), targetLoc, 1, true));

        // Damage the players when it lands
        new BukkitRunnable() {
            @Override
            public void run() {
                if (activeStompJump && giant.isOnGround()) {
                    stomp();
                    cancel();
                }
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), 5, 1);
    }

    
    public void stomp() {
        // Create a shockwave effect
        new BukkitRunnable() {

            final int radius = 7;
            final double maxDamage = 10;
            final Location centerPoint = giant.getLocation();
            int layer = 1;

            @Override
            public void run() {
                if (centerPoint.getWorld() == null) {
                    cancel();
                    return;
                }

                if (layer == 1) centerPoint.getWorld().playSound(centerPoint, Sound.ENTITY_GENERIC_EXPLODE, 1, 1);

                // Determine the particle size based on the layer
                Particle particle;
                if (layer < 2) particle = Particle.EXPLOSION_EMITTER;
                else if (layer < 4) particle = Particle.EXPLOSION;
                else particle = Particle.SMOKE;
                int steps = 12 / layer;
                int step = 360 / steps;

                // Spawn particle ring in a circle around the center point that is 'layer' blocks away
                for (int i = 0; i < 360; i += step) {
                    double angle = Math.toRadians(i);
                    double x = centerPoint.getX() + (layer * Math.cos(angle));
                    double z = centerPoint.getZ() + (layer * Math.sin(angle));
                    Location particleLoc = new Location(centerPoint.getWorld(), x, centerPoint.getY() + 0.5, z);
                    centerPoint.getWorld().spawnParticle(particle, particleLoc, 1);
                }

                // Cause damage to players in the area
                for (Entity ent : centerPoint.getWorld().getNearbyEntities(centerPoint, layer * 1.5, 4, layer * 1.5)) {
                    if (!(ent instanceof Player p)) continue;
                    double damage = maxDamage / layer;
                    p.damage(damage);
                }

                layer++;
                if (layer == radius) cancel();
            }
        }.runTaskTimer(SurvivalSkills.getPlugin(SurvivalSkills.class), 0, 5);
        activeStompJump = false;
    }


    private void startRockThrowVolley() {
        new BukkitRunnable() {
            private int counter = 0;
            @Override
            public void run() {
                if (counter == 3) {
                    cancel();
                    return;
                }
                rockThrow();
                counter++;
            }
        }.runTaskTimer(SurvivalSkills.getPlugin(SurvivalSkills.class), 0, 10);
    }

    // Method for rock throw attack
    public void rockThrow() {
        // Get a random target player within a 20 block radius
        LivingEntity giantTarget = giant.getTarget();
        if (giantTarget == null) return;
        if (!(giantTarget instanceof Player)) return;
        Player target = (Player) giantTarget;
        if (giant.getLocation().distance(target.getLocation()) > ROCK_THROW_MAX_DISTANCE) return;

        // Spawn a passable block at the hand of the giant
        Location handLoc = getGiantHandLocation(target.getLocation());
        if (handLoc.getWorld() == null) return;
        handLoc.getWorld().playSound(handLoc, Sound.BLOCK_STONE_PLACE, 1, 1);
        FallingBlock stone = handLoc.getWorld().spawnFallingBlock(handLoc, Material.STONE.createBlockData());
        stone.setCancelDrop(true);
        stone.setDropItem(false);
        stone.setGravity(false);

        // Set the velocity of the stone to fly towards the target player
        Vector velocity = ProjectileCalculator.getNoGravityVector(handLoc, target.getLocation().clone().add(0, 1, 0), ROCK_THROW_SPEED);
        stone.setVelocity(velocity);

        // Create a bukkit runnable that constantly checks if the stone is near a player and if so, deal damage
        new BukkitRunnable() {
            @Override
            public void run() {
                if (stone.isDead()) {
                    cancel();
                    return;
                }

                for (Entity ent : stone.getWorld().getNearbyEntities(stone.getLocation(), 0.5, 0.5, 0.5)) {
                    if (!(ent instanceof Player p)) continue;
                    double distance = giant.getLocation().distance(stone.getLocation());
                    p.damage(Math.max(5, distance));
                    stone.remove();
                    cancel();
                }
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);
    }

    // Method for giant roar attack
    public void roar() {
        // Spawn particles in a sphere with a radius of 5 blocks
        new BukkitRunnable() {

            final int radius = 12;
            final Location centerPoint = giant.getLocation().clone().add(0, 12, 0);
            int layer = 1;

            @Override
            public void run() {
                if (centerPoint.getWorld() == null) {
                    cancel();
                    return;
                }

                if (layer == 1) centerPoint.getWorld().playSound(centerPoint, Sound.ENTITY_RAVAGER_ROAR, 1, 1);
                Particle particle = Particle.SMOKE;

                // Spawn particles in a sphere with a radius of 'layer' blocks
                double epsilon = 0.1;
                for (int x = -layer; x <= layer; x++) {
                    for (int y = -11; y <= 1; y++) {
                        for (int z = -layer; z <= layer; z++) {
                            double distance = Math.sqrt(x * x + y * y + z * z);
                            if (Math.abs(distance - layer) < epsilon) {
                                Location particleLoc = centerPoint.clone().add(x, y, z);
                                centerPoint.getWorld().spawnParticle(particle, particleLoc, 3);

                                if (layer > 7) continue;
                                if (particleLoc.getBlock().getType() == Material.AIR) continue;
                                if (particleLoc.getBlock().getType() == Material.BEDROCK) continue;
                                particleLoc.getBlock().breakNaturally();
                            }
                        }
                    }
                }

                // If a player is in radius damage them by 20 / layer and apply knock back
                for (Entity ent : centerPoint.getWorld().getNearbyEntities(centerPoint, layer, 15, layer)) {
                    if (!(ent instanceof Player p)) continue;
                    double damage = (double) 20 / layer;
                    p.damage(damage);
                    p.setVelocity(ProjectileCalculator.getDirectionVector(p.getLocation(), centerPoint).multiply((double) 2 / layer));
                }

                layer++;
                if (layer == radius) cancel();
            }
        }.runTaskTimer(SurvivalSkills.getPlugin(SurvivalSkills.class), 0, 5);
    }

    // Method for spawning minions
    public void spawnMinions(int count) {
        int nearbyZombieCount = 0;
        for (Entity entity : giant.getNearbyEntities(10, 10, 10)) {
            if (entity instanceof Zombie) nearbyZombieCount++;
        }
        if (nearbyZombieCount >= 10) return;
        else count = Math.min(10 - nearbyZombieCount, count);

        ArrayList<Location> spawnPoints = findNearbyValidSpawnPoints(count);
        for (Location loc : spawnPoints) {
            if (loc.getWorld() == null) continue;
            Zombie zombie = (Zombie) loc.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
            zombie.setCustomName(ChatColor.DARK_GREEN + "Giant Minion");
            zombie.setCustomNameVisible(true);
            loc.getWorld().playSound(loc, Sound.BLOCK_GRAVEL_BREAK, 1, 1);
        }
    }


    private Location getGiantHandLocation(Location target) {
        // Normalize the direction vector to make its length 1
        Vector unitVector = ProjectileCalculator.getDirectionVector(giant.getLocation(), target);
        // Calculate the vector that is orthogonal to the direction vector
        return giant.getLocation().clone().add(-unitVector.getZ() * HAND_OFFSET, HAND_HEIGHT, unitVector.getZ() * HAND_OFFSET);
    }

    private int getCooldownForStage(int stage) {
        switch (stage) {
            case 1:
                return ATTACK_COOLDOWN_STAGE_1;
            case 2:
                return ATTACK_COOLDOWN_STAGE_2;
            case 3:
                return ATTACK_COOLDOWN_STAGE_3;
            default:
                return 0;
        }
    }
}
