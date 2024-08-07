package sir_draco.survivalskills.Bosses;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.Utils.ProjectileCalculator;

import java.util.ArrayList;

public class GiantBoss extends Boss {

    private Zombie giant;
    private int attackCooldownDefault = 20 * 10;
    private int attackCooldown = attackCooldownDefault;
    private int targetCooldown = 20;
    private boolean activeStompJump = false;
    private boolean loadingJump = false;

    public GiantBoss(Location loc) {
        super("Giant", 4, 15, 150, 10, 5, 0.2, EntityType.ZOMBIE, loc, 3);
        if (isSpawnSuccess()) {
            giant = (Zombie) getBoss();
            giant.setGravity(true);
            EntityEquipment helmet = giant.getEquipment();
            if (helmet != null) helmet.setHelmet(new ItemStack(Material.LEATHER_HELMET));
            AttributeInstance size = giant.getAttribute(Attribute.GENERIC_SCALE);
            if (size != null) size.setBaseValue(4);
        }
    }

    @Override
    public void run() {
        if (!isSpawnSuccess() || giant.isDead()) {
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
            attackCooldown = attackCooldownDefault;
            attack();
        }

        if (activeStompJump && giant.isOnGround()) stomp();

        if (giant.getTarget() != null && !activeStompJump && !loadingJump) {
            faceLocation(giant.getTarget().getLocation());
            attackNearbyPlayers();
        }
    }

    @Override
    public void attack() {
        if (getStage() == 1) startStomp();
        else if (getStage() == 2) {
            if (attackCooldownDefault != 20 * 5) attackCooldownDefault = 20 * 5;
            double rand = Math.random();
            if (rand < 0.5) startStomp();
            else {
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
            if (attackCooldownDefault != 20 * 2) attackCooldownDefault = 20 * 2;
            double rand = Math.random();
            if (rand < 0.33) startStomp();
            else if (rand < 0.66) {
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
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                for (int y = -1; y <= 5; y++) {
                    Location blockLoc = deathLoc.clone().add(x, y, z);
                    if (blockLoc.distance(deathLoc) > 5) continue;
                    if (blockLoc.getBlock().getType() == Material.AIR) continue;
                    if (blockLoc.clone().add(0, 1, 0).getBlock().getType() != Material.AIR) continue;
                    // Increase density close to death location
                    if (Math.random() > (double) 1 / blockLoc.distance(deathLoc)) continue;
                    blockLoc.getBlock().setType(Material.MYCELIUM);
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

    // Method for giant stomp attack
    public void startStomp() {
        // Make the giant jump into the air and land on the ground
        if (activeStompJump || loadingJump) {
            Bukkit.getLogger().info("Giant is already jumping");
            return;
        }
        if (giant.getTarget() == null) return;
        loadingJump = true;
        giant.teleport(giant.getLocation().clone().add(0, 0.3, 0));
        new BukkitRunnable() {
            @Override
            public void run() {
                Location targetLoc = giant.getTarget().getLocation();
                Vector velocity = ProjectileCalculator.getVector(giant.getLocation(), targetLoc, 1);
                double distance = giant.getLocation().distance(targetLoc);
                giant.setVelocity(velocity.multiply(1.5));
            }
        }.runTaskLater(SurvivalSkills.getPlugin(SurvivalSkills.class), 1);
        new BukkitRunnable() {
            @Override
            public void run() {
                activeStompJump = true;
                loadingJump = false;
            }
        }.runTaskLater(SurvivalSkills.getPlugin(SurvivalSkills.class), 10);
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
                    if (!(ent instanceof Player)) continue;
                    Player p = (Player) ent;
                    double damage = maxDamage / layer;
                    p.damage(damage);
                }

                layer++;
                if (layer == radius) cancel();
            }
        }.runTaskTimer(SurvivalSkills.getPlugin(SurvivalSkills.class), 0, 5);
        activeStompJump = false;
    }

    // Method for rock throw attack
    public void rockThrow() {
        // Get a random target player within a 20 block radius
        // Gravity is -0.08 blocks per tick squared
        Player target = null;
        for (Entity entity : giant.getNearbyEntities(20, 20, 20)) {
            if (!(entity instanceof Player)) continue;
            if (!giant.hasLineOfSight(entity)) continue;
            target = (Player) entity;
            break;
        }
        if (target == null) return;

        // Spawn a passable block at the hand of the giant
        Location handLoc = getGiantHandLocation(target.getLocation());
        if (handLoc.getWorld() == null) return;
        handLoc.getWorld().playSound(handLoc, Sound.BLOCK_STONE_PLACE, 1, 1);
        FallingBlock stone = handLoc.getWorld().spawnFallingBlock(handLoc, Material.STONE.createBlockData());
        stone.setCancelDrop(true);
        stone.setDropItem(false);
        stone.setGravity(false);

        // Set the velocity of the stone to fly towards the target player
        Vector velocity = ProjectileCalculator.getNoGravityVector(handLoc, target.getLocation().clone().add(0, 1, 0), 1.5);
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
                    if (!(ent instanceof Player)) continue;
                    Player p = (Player) ent;
                    double distance = giant.getLocation().distance(stone.getLocation());
                    p.damage(Math.max(5, distance));
                    stone.remove();
                    cancel();
                }
            }
        }.runTaskTimer(SurvivalSkills.getPlugin(SurvivalSkills.class), 0, 1);
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
                    if (!(ent instanceof Player)) continue;
                    Player p = (Player) ent;
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

    public ArrayList<Location> findNearbyValidSpawnPoints(int count) {
        ArrayList<Location> validSpawnPoints = new ArrayList<>();
        ArrayList<Block> blockList = new ArrayList<>();
        Location giantLoc = giant.getLocation().getBlock().getLocation();
        for (int i = 0; i < count; i++) {
            if (blockList.size() >= count) break;
            for (int x = -5; x <= 5; x++) {
                if (blockList.size() >= count) break;
                for (int z = -5; z <= 5; z++) {
                    if (blockList.size() >= count) break;
                    for (int y = 0; y <= 5; y++) {
                        if (blockList.size() >= count) break;
                        Location blockLoc = giantLoc.clone().add(x, y, z);
                        if (blockList.contains(blockLoc.getBlock())) continue;
                        if (blockLoc.distance(giant.getLocation()) > 5) continue;
                        if (blockLoc.getBlock().getType() == Material.AIR) continue;
                        if (blockLoc.clone().add(0, 1, 0).getBlock().getType() != Material.AIR) continue;
                        if (blockLoc.clone().add(0, 2, 0).getBlock().getType() != Material.AIR) continue;
                        blockList.add(blockLoc.getBlock());
                        validSpawnPoints.add(blockLoc.clone().add(0.5, 1, 0.5));
                    }
                }
            }
        }
        return validSpawnPoints;
    }

    // Method for targeting a player
    public void setNearestPlayerAsTarget() {
        Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Entity entity : giant.getNearbyEntities(100, 50, 100)) {
            if (!(entity instanceof Player)) continue;

            double distance = entity.getLocation().distance(giant.getLocation());
            if (distance < nearestDistance) {
                nearestPlayer = (Player) entity;
                nearestDistance = distance;
            }
        }

        if (nearestPlayer != null) giant.setTarget(nearestPlayer);
    }

    public Location getGiantHandLocation(Location target) {
        // Normalize the direction vector to make its length 1
        Vector unitVector = ProjectileCalculator.getDirectionVector(giant.getLocation(), target);
        // Calculate the vector that is orthogonal to the direction vector
        Vector orthogonalVector;
        if (unitVector.getX() > 0 && unitVector.getZ() > 0) orthogonalVector = new Vector(-unitVector.getZ(), 0, unitVector.getX());
        else if (unitVector.getX() > 0 && unitVector.getZ() < 0) orthogonalVector = new Vector(unitVector.getZ(), 0, -unitVector.getX());
        else if (unitVector.getX() < 0 && unitVector.getZ() > 0) orthogonalVector = new Vector(-unitVector.getZ(), 0, unitVector.getX());
        else orthogonalVector = new Vector(unitVector.getZ(), 0, -unitVector.getX());
        orthogonalVector = orthogonalVector.multiply(2d);

        return giant.getLocation().clone().add(orthogonalVector.getX(), 7.5, orthogonalVector.getZ());
    }

    public void faceLocation(Location loc) {
        Vector direction = loc.toVector().subtract(giant.getLocation().toVector()).normalize();
        Location target = giant.getLocation().clone();
        target.setYaw((float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ())));
        target.setPitch(90 - (float) Math.toDegrees(Math.acos(-direction.getY())));
        giant.teleport(target);
    }

    public void attackNearbyPlayers() {
        if (attackCooldown % 20 != 0) {
            if (attackCooldown % 4 != 0) return;
            // Make the giant move towards its target player
            if (giant.getTarget() == null) return;
            Vector direction = ProjectileCalculator.getDirectionVector(giant.getLocation(), giant.getTarget().getLocation());
            if (!giant.isOnGround()) direction.setY(0);
            giant.setVelocity(direction.multiply(0.1));
            return;
        }
        ArrayList<Player> nearbyPlayers = new ArrayList<>();
        for (Entity entity : giant.getNearbyEntities(3, 12, 3)) {
            if (!(entity instanceof Player)) continue;
            nearbyPlayers.add((Player) entity);
        }
        if (nearbyPlayers.isEmpty()) return;
        for (Player p : nearbyPlayers) p.damage(5);
        giant.swingMainHand();
        giant.swingOffHand();
    }
}
