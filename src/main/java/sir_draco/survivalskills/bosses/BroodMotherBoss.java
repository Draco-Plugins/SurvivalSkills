package sir_draco.survivalskills.bosses;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.ProjectileCalculator;

import java.util.ArrayList;

public class BroodMotherBoss extends Boss {

    private static final int ATTACK_COOLDOWN_STAGE_1 = 20 * 10;
    private static final int ATTACK_COOLDOWN_STAGE_2 = 20 * 5;
    private static final int ATTACK_COOLDOWN_STAGE_3 = 20 * 3;
    private static final int TARGETING_COOLDOWN_MAX = 20;
    private static final int MINION_MAX = 15;

    private Spider spider;
    private int attackCooldown = ATTACK_COOLDOWN_STAGE_1;
    private int targetCooldown = TARGETING_COOLDOWN_MAX;

    private BroodMotherBoss() {
        super("BroodMother", 3, 3, 300, 15, 2, 0.25, 3);
    }

    public static BroodMotherBoss create(Location loc) {
        BroodMotherBoss boss = new BroodMotherBoss();
        if (!boss.spawnBoss(EntityType.SPIDER, loc)) return null;
        boss.spider = (Spider) boss.getBoss();
        AttributeInstance size = boss.spider.getAttribute(Attribute.SCALE);
        if (size != null) size.setBaseValue(3);
        return boss;
    }

    @Override
    public void run() {
        if (boss == null || spider.isDead()) {
            cancel();
            return;
        }

        if (getHealthPercentage() != 1) checkStage(true, Sound.ENTITY_GHAST_SCREAM);
        updateBossBar();
        manageBossBarPlayers();
        if (isDisableAttack()) return;
        if (targetCooldown > 0) targetCooldown--;
        else {
            targetCooldown = TARGETING_COOLDOWN_MAX;
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
        // Handle web spray attack
        boolean spraySuccess = false;
        if (Math.random() > getHealthPercentage()) {
            spraySuccess = true;
            spawnCobwebSpray(20, 0.5);
        }

        // Handle minion spawn attack
        if (getHealthPercentage() < 0.5 && Math.random() > getHealthPercentage()) spawnMinions(spider.getTarget(), Math.max(1, (int) ((1 - getHealthPercentage()) * MINION_MAX)));

        // Handle lunge attack, don't lunge if it already sprayed
        if (spraySuccess) return;
        lungeAttack();
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
                spawnCobwebSpray(10, 1.5);
                dummy.remove();
            }
        }.runTaskLater(SurvivalSkills.getPlugin(SurvivalSkills.class), 80);
    }

    // Method for spawning minions
    private void spawnMinions(LivingEntity target, int count) {
        // If there are already 10 cave spiders near the boss, don't spawn more
        int nearbyCaveSpiderCount = 0;
        for (Entity entity : spider.getNearbyEntities(10, 10, 10)) {
            if (entity instanceof CaveSpider) nearbyCaveSpiderCount++;
        }
        if (nearbyCaveSpiderCount >= MINION_MAX) return;
        else count = Math.min(MINION_MAX - nearbyCaveSpiderCount, count);

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


    private void spawnCobwebSpray(int count, double speedMultiplier) {
        for (int i = 0; i < 20; i++) {
            Location loc = spider.getLocation().clone().add(0, 1, 0);
            Vector velocity = new Vector(Math.random() - speedMultiplier, speedMultiplier, Math.random() - speedMultiplier).normalize();
            FallingBlock cobweb = spider.getWorld().spawnFallingBlock(loc, Material.COBWEB.createBlockData());
            cobweb.setDropItem(true);
            cobweb.setHurtEntities(false);
            cobweb.setVelocity(velocity);
        }
        spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_EGG_THROW, 1, 1);
    }


    private void lungeAttack() {
        if (spider.getTarget() == null) {
            setNearestPlayerAsTarget();
            return;
        }
        Location targetLoc = spider.getTarget().getLocation();
        Vector direction = ProjectileCalculator.getLivingEntityProjectileVector(spider.getLocation(), targetLoc, 1.5, false);
        spider.setVelocity(direction);
        spider.getWorld().playSound(spider.getLocation(), Sound.ENTITY_SPIDER_DEATH, 1, 1);
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
