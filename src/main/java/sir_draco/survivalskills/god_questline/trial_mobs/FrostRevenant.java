package sir_draco.survivalskills.god_questline.trial_mobs;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.god_questline.trial.TrialManager;
import sir_draco.survivalskills.utils.ProjectileCalculator;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class FrostRevenant extends TrialBoss {

    // ======================== CONSTANTS ========================

    // Constructor defaults
    private static final String BOSS_ID = "frostRevenant";
    private static final String BOSS_NAME = ChatColor.LIGHT_PURPLE + "Frost Revenant";
    private static final double BASE_HEALTH = 200;
    private static final double BASE_DAMAGE = 10;
    private static final double BASE_SPEED = 0.35;
    private static final double BASE_SCALE = 1.5;

    // Attack probability thresholds
    private static final double SNOWBALL_ATTACK_CHANCE = 0.75;

    // Cooldowns
    private static final int ATTACK_COOLDOWN_TICKS = 20 * 3;
    private static final int SUMMON_COOLDOWN_TICKS = 20 * 20;

    // Wolf minions
    private static final int ANGRY_WOLF_SPAWN_COUNT = 5;
    private static final double ANGRY_WOLF_HEALTH = 10;
    private static final String REVENANT_WOLF_NAME = ChatColor.RED + "Revenant Wolf";

    // Snowball projectile
    private static final double SNOWBALL_SPEED = 1.5;

    // ======================== FIELDS ========================

    private int cooldown = ATTACK_COOLDOWN_TICKS;
    private int summonCooldown = 0;

    private Stray stray = null;

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
                .speed(BASE_SPEED)
                .scale(BASE_SCALE)
                .type(EntityType.STRAY)
                .drops(drops);
    }

    public FrostRevenant(Map<ItemStack, Double> drops) {
        this(drops, 1.0, 1.0);
    }

    public FrostRevenant(Map<ItemStack, Double> drops, double healthMultiplier, double damageMultiplier) {
        super(createBuilder(drops, healthMultiplier, damageMultiplier));
    }

    // ======================== TICK ========================

    @Override
    protected void onTick() {
        keepWolvesAngry();

        if (summonCooldown > 0) summonCooldown--;

        if (cooldown > 0) cooldown--;
        else {
            cooldown = ATTACK_COOLDOWN_TICKS;
            attack();
        }
    }

    // ======================== ATTACK SELECTION ========================

    @Override
    public void attack() {
        // 75% chance: fire a freezing snowball
        if (Math.random() < SNOWBALL_ATTACK_CHANCE) {
            freezingSnowBall();
            return;
        }

        // 25% chance: summon wolves, but only if summon cooldown has expired
        if (summonCooldown > 0) {
            return; // Still recharging summon - skip this attack cycle
        }

        spawnAngryWolves();
        summonCooldown = SUMMON_COOLDOWN_TICKS;
    }

    // ======================== STARTUP ========================

    @Override
    public void handleTypeSpecificSpawn() {
        this.stray = (Stray) getBoss();
        spawnAngryWolves();
    }

    public void startScript() {
        this.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);
    }

    // ======================== ABILITIES ========================

    public void freezingSnowBall() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (stray == null || stray.getTarget() == null) return;
                stray.launchProjectile(Snowball.class,
                        ProjectileCalculator.getNoGravityVector(stray.getEyeLocation(),
                                stray.getTarget().getEyeLocation(), SNOWBALL_SPEED));
            }
        }.runTask(SurvivalSkills.getPlugin(SurvivalSkills.class));
    }

    public void spawnAngryWolves() {
        Optional<Player> target = findTrialPlayerTarget();
        spawnMinions(EntityType.WOLF, REVENANT_WOLF_NAME, ANGRY_WOLF_SPAWN_COUNT, entity -> {
            Wolf wolf = (Wolf) entity;
            wolf.setAngry(true);
            target.ifPresent(player -> wolf.setTarget(player));
            AttributeInstance health = wolf.getAttribute(Attribute.MAX_HEALTH);
            if (health != null) health.setBaseValue(ANGRY_WOLF_HEALTH);
            wolf.setHealth(ANGRY_WOLF_HEALTH);
        });
    }

    private void keepWolvesAngry() {
        Optional<Player> target = findTrialPlayerTarget();
        target.ifPresent(player -> getSummons().stream()
                .flatMap(entity -> entity instanceof Wolf wolf && !wolf.isDead()
                        ? Stream.of(wolf)
                        : Stream.empty())
                .forEach(wolf -> {
                    wolf.setAngry(true);
                    wolf.setTarget(player);
                }));
    }

    private Optional<Player> findTrialPlayerTarget() {
        Optional<Player> trialPlayer = TrialManager.getTrials().stream()
                .filter(trial -> trial.getWave() != null && this.equals(trial.getWave().getBoss()))
                .flatMap(trial -> trial.getPlayers().stream())
                .filter(player -> player.isOnline() && !player.isDead())
                .findFirst();
        if (trialPlayer.isPresent()) return trialPlayer;

        if (stray != null && stray.getTarget() instanceof Player player
                && player.isOnline() && !player.isDead()) {
            return Optional.of(player);
        }
        return Optional.empty();
    }

    // ======================== DUPLICATE ========================

    public FrostRevenant duplicate(double healthMultiplier, double damageMultiplier) {
        return new FrostRevenant(getDrops(), healthMultiplier, damageMultiplier);
    }

    public FrostRevenant duplicate() {
        return new FrostRevenant(getDrops());
    }
}
