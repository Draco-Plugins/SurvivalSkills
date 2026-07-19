package sir_draco.survivalskills.god_questline.trial_mobs;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.MagmaCube;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.ProjectileCalculator;

import java.util.Map;

public class BlazingGhast extends TrialBoss {

    // ======================== CONSTANTS ========================

    private static final String BOSS_ID = "blazingGhast";
    private static final String BOSS_NAME = ChatColor.RED + "Blazing Ghast";
    private static final double BASE_HEALTH = 100;
    private static final double BASE_DAMAGE = 10;
    private static final int ATTACK_COOLDOWN_TICKS = 20 * 3;
    private static final int SUMMON_COOLDOWN_TICKS = 20 * 20;
    private static final int PERCH_AI_FREEZE_TICKS = 20 * 3;
    private static final int MAGMA_CUBE_SPAWN_COUNT = 5;
    private static final int MAGMA_CUBE_SIZE = 3;
    private static final double MAGMA_CUBE_HEALTH = 10;
    private static final String MAGMA_CUBE_NAME = ChatColor.RED + "Blazing Magma Cube";
    private static final double TELEPORT_VERTICAL_OFFSET = 7;
    private static final double PERCH_VERTICAL_OFFSET = 1;
    private static final double MAX_TARGET_DISTANCE = 30;
    private static final double FIREBALL_SPEED = 0.5;
    private static final int FIREBALL_COUNT = 3;
    private static final int FIREBALL_INTERVAL_TICKS = 20;

    // Attack probability thresholds
    private static final double PERCH_CHANCE = 0.2;
    private static final double BLAZE_FIREBALLS_CHANCE = 0.5;
    private static final double CHARGE_PLAYER_CHANCE = 0.8;

    // ======================== FIELDS ========================

    private int attackCooldown = ATTACK_COOLDOWN_TICKS;
    private int summonCooldown = 0;

    private Ghast ghast = null;

    // ======================== FACTORY & CONSTRUCTORS ========================

    private static TrialBoss.Builder createBuilder(Map<ItemStack, Double> drops,
                                                   double healthMultiplier,
                                                   double damageMultiplier) {
        return new TrialBoss.Builder()
                .id(BOSS_ID)
                .name(BOSS_NAME)
                .maxHealth(BASE_HEALTH * healthMultiplier)
                .damage(BASE_DAMAGE * damageMultiplier)
                .defense(0)
                .speed(0.2)
                .scale(1)
                .type(EntityType.GHAST)
                .drops(drops);
    }

    public BlazingGhast(Map<ItemStack, Double> drops) {
        super(createBuilder(drops, 1.0, 1.0));
    }

    public BlazingGhast(Map<ItemStack, Double> drops, double healthMultiplier, double damageMultiplier) {
        super(createBuilder(drops, healthMultiplier, damageMultiplier));
    }

    // ======================== TICK ========================

    @Override
    protected void onTick() {
        if (summonCooldown > 0) summonCooldown--;

        if (attackCooldown > 0) attackCooldown--;
        else {
            attackCooldown = ATTACK_COOLDOWN_TICKS;
            attack();
        }

        checkTooFar();
    }

    // ======================== ATTACK SELECTION ========================

    @Override
    public void attack() {
        double chance = Math.random();
        if (chance < PERCH_CHANCE) perch();
        else if (chance < BLAZE_FIREBALLS_CHANCE) blazeFireballs();
        else if (chance < CHARGE_PLAYER_CHANCE) chargePlayer();
        else if (summonCooldown == 0) {
            spawnMagmaCubes();
            summonCooldown = SUMMON_COOLDOWN_TICKS;
        }
    }

    // ======================== STARTUP ========================

    @Override
    public void handleTypeSpecificSpawn() {
        this.ghast = (Ghast) getBoss();
    }

    @Override
    public void startScript() {
        this.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);
    }

    // ======================== ABILITIES ========================

    public void checkTooFar() {
        if (ghast == null) return;
        if (ghast.getTarget() == null) return;
        if (ghast.getLocation().distance(ghast.getTarget().getLocation()) > MAX_TARGET_DISTANCE) {
            ghast.teleport(ghast.getTarget().getLocation().add(0, TELEPORT_VERTICAL_OFFSET, 0));
        }
    }

    public void perch() {
        if (ghast == null) return;
        ghast.getWorld().playSound(ghast.getLocation(), Sound.ENTITY_GHAST_WARN, 1, 1);
        if (ghast.getTarget() == null) return;
        ghast.teleport(ghast.getTarget().getLocation().add(0, PERCH_VERTICAL_OFFSET, 0));
        ghast.setAI(false);
        ghast.setVelocity(new Vector(0, 0, 0));
        new BukkitRunnable() {
            @Override
            public void run() {
                if (isSpawned() && ghast != null)
                    ghast.setAI(true);
            }
        }.runTaskLater(SurvivalSkills.getPlugin(SurvivalSkills.class), PERCH_AI_FREEZE_TICKS);
    }

    public void blazeFireballs() {
        new BukkitRunnable() {
            int fireballCount = 0;
            @Override
            public void run() {
                if (!isSpawned() || ghast == null || ghast.getTarget() == null
                        || fireballCount >= FIREBALL_COUNT) {
                    cancel();
                    return;
                }
                Location target = ghast.getTarget().getLocation();
                Fireball fireball = ghast.getWorld().spawn(ghast.getLocation(), Fireball.class);
                fireball.setAcceleration(ProjectileCalculator.getNoGravityVector(
                        ghast.getLocation(), target, FIREBALL_SPEED));
                fireballCount++;
            }
        }.runTaskTimer(SurvivalSkills.getPlugin(SurvivalSkills.class), 0, FIREBALL_INTERVAL_TICKS);
    }

    public void chargePlayer() {
        if (ghast == null) return;
        ghast.setCharging(true);
    }

    public void spawnMagmaCubes() {
        spawnMinions(EntityType.MAGMA_CUBE, MAGMA_CUBE_NAME, MAGMA_CUBE_SPAWN_COUNT, entity -> {
            MagmaCube cube = (MagmaCube) entity;
            cube.setSize(MAGMA_CUBE_SIZE);
            AttributeInstance health = cube.getAttribute(Attribute.MAX_HEALTH);
            if (health != null) health.setBaseValue(MAGMA_CUBE_HEALTH);
            cube.setHealth(MAGMA_CUBE_HEALTH);
        });
    }

    // ======================== DUPLICATE ========================

    public BlazingGhast duplicate(double healthMultiplier, double damageMultiplier) {
        return new BlazingGhast(getDrops(), healthMultiplier, damageMultiplier);
    }

    public BlazingGhast duplicate() {
        return new BlazingGhast(getDrops());
    }
}
