package sir_draco.survivalskills.Bosses;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.Utils.ProjectileCalculator;

import java.util.ArrayList;

public class BroodMotherBoss extends Boss {

    private final int minionMax = 15;

    private Spider spider;
    private int attackCooldownDefault = 20 * 10;
    private int attackCooldown = attackCooldownDefault;
    private int targetCooldown = 20;

    public BroodMotherBoss(Location loc) {
        super("BroodMother", 3, 3, 300, 15, 2, 0.25, EntityType.SPIDER, loc, 3);
        if (isSpawnSuccess()) {
            spider = (Spider) getBoss();
            AttributeInstance size = spider.getAttribute(Attribute.GENERIC_SCALE);
            if (size != null) size.setBaseValue(3);
        }
    }

    @Override
    public void run() {
        if (!isSpawnSuccess() || spider.isDead()) {
            cancel();
            return;
        }

        if (getHealthPercentage() != 1) checkStage(true, Sound.ENTITY_GHAST_SCREAM);
        updateBossBar();
        manageBossBarPlayers();
        if (isDisableAttack()) return;
        if (targetCooldown > 0) targetCooldown--;
        else {
            targetCooldown = 20;
            setNearestPlayerAsTarget();
        }

        if (attackCooldown > 0) attackCooldown--;
        else {
            attackCooldown = attackCooldownDefault;
            attack();
        }
    }

    @Override
    public void attack() {
        if (getStage() == 2) attackCooldownDefault = 20 * 5;
        if (getStage() == 3) attackCooldownDefault = 20 * 3;

        // Handle web spray attack
        double chance = Math.random();
        boolean spraySuccess = false;
        if (chance > getHealthPercentage()) {
            spraySuccess = true;
            for (int i = 0; i < 20; i++) {
                Location loc = spider.getLocation().clone().add(0, 1, 0);
                Vector velocity = new Vector(Math.random() - 0.5, 0.5, Math.random() - 0.5).normalize();
                FallingBlock cobweb = spider.getWorld().spawnFallingBlock(loc, Material.COBWEB.createBlockData());
                cobweb.setDropItem(true);
                cobweb.setHurtEntities(false);
                cobweb.setVelocity(velocity);
            }
            spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_EGG_THROW, 1, 1);
        }

        // Handle minion spawn attack
        chance = Math.random();
        if (chance > getHealthPercentage() && getHealthPercentage() < 0.5) spawnMinions(spider.getTarget(), Math.max(1, (int) (1 - getHealthPercentage()) * minionMax));

        // Handle lunge attack
        if (spraySuccess) return;
        if (spider.getTarget() == null) {
            setNearestPlayerAsTarget();
            return;
        }
        Location targetLoc = spider.getTarget().getLocation();
        Vector direction = ProjectileCalculator.getVector(spider.getLocation(), targetLoc, 1.5);
        spider.setVelocity(direction);
        spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_DEATH, 1, 1);
    }

    @Override
    public void deathAnimation() {
        Spider dummy = (Spider) spider.getWorld().spawnEntity(spider.getLocation(), EntityType.SPIDER);
        dummy.setAI(false);
        dummy.setInvulnerable(true);
        dummy.setGravity(false);
        dummy.setVelocity(new Vector(0, 0.1, 0));

        new BukkitRunnable() {
            @Override
            public void run() {
                // Spawn cobweb projectiles
                for (int i = 0; i < 10; i++) {
                    Location loc = dummy.getLocation().clone().add(0, 1, 0);
                    Vector velocity = new Vector(Math.random() - 0.5, 0.5, Math.random() - 0.5).normalize().multiply(0.5);
                    FallingBlock cobweb = dummy.getWorld().spawnFallingBlock(loc, Material.COBWEB.createBlockData());
                    cobweb.setVelocity(velocity);
                }
                dummy.remove();
            }
        }.runTaskLater(SurvivalSkills.getPlugin(SurvivalSkills.class), 80);
    }

    // Method for spawning minions
    public void spawnMinions(LivingEntity target, int count) {
        // If there are already 10 cave spiders near the boss, don't spawn more
        int nearbyCaveSpiderCount = 0;
        for (Entity entity : spider.getNearbyEntities(10, 10, 10)) {
            if (entity instanceof CaveSpider) nearbyCaveSpiderCount++;
        }
        if (nearbyCaveSpiderCount >= minionMax) return;
        else count = Math.min(minionMax - nearbyCaveSpiderCount, count);

        ArrayList<Location> spawnPoints = findNearbyValidSpawnPoints(count);
        for (Location loc : spawnPoints) {
            if (loc.getWorld() == null) continue;
            CaveSpider caveSpider = (CaveSpider) loc.getWorld().spawnEntity(loc, EntityType.CAVE_SPIDER);
            caveSpider.setCustomName(ChatColor.RED + "BroodMother Minion");
            caveSpider.setCustomNameVisible(true);
            caveSpider.setTarget(target);
            loc.getWorld().playSound(loc, Sound.BLOCK_GRAVEL_BREAK, 1, 1);
            loc.getBlock().setType(Material.COBWEB);
        }
    }

    public ArrayList<Location> findNearbyValidSpawnPoints(int count) {
        ArrayList<Location> validSpawnPoints = new ArrayList<>();
        ArrayList<Block> blockList = new ArrayList<>();
        Location giantLoc = spider.getLocation().getBlock().getLocation();
        for (int i = 0; i < count; i++) {
            for (int x = -5; x <= 5; x++) {
                for (int z = -5; z <= 5; z++) {
                    for (int y = 0; y <= 5; y++) {
                        Location blockLoc = giantLoc.clone().add(x, y, z);
                        if (blockList.contains(blockLoc.getBlock())) continue;
                        if (blockLoc.distance(spider.getLocation()) > 5) continue;
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

        for (Entity entity : spider.getNearbyEntities(50, 50, 50)) {
            if (!(entity instanceof Player)) continue;

            double distance = entity.getLocation().distance(spider.getLocation());
            if (distance < nearestDistance) {
                nearestPlayer = (Player) entity;
                nearestDistance = distance;
            }
        }

        if (nearestPlayer != null) spider.setTarget(nearestPlayer);
    }
}
