package sir_draco.survivalskills.god_questline.trial_mobs;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.god_questline.trial.TrialManager;
import sir_draco.survivalskills.utils.ColorParser;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class HellsGatekeeper extends TrialBoss {

	// ======================== CONSTANTS ========================

	// Constructor defaults
	private static final String BOSS_ID = "hellsGatekeeper";
	private static final String BOSS_NAME = "Hell's Gatekeeper";
	private static final double BASE_HEALTH = 166;
	private static final double BASE_DAMAGE = 10;
	private static final double BASE_SPEED = 0.3;
	private static final double BASE_SCALE = 2;
	private static final int GRADIENT_LENGTH = 17;

	// Cooldowns
	private static final int ATTACK_COOLDOWN_TICKS = 20 * 3;
	private static final int SUMMON_COOLDOWN_TICKS = 20 * 20;

	// Fire storm attack
	private static final int FIRE_STORM_DURATION_TICKS = 20 * 5;
	private static final int FIRE_STORM_PARTICLES_PER_TICK = 10;
	private static final double FIRE_STORM_DAMAGE = 4;
	private static final int FIRE_TICK_DURATION = 40;
	private static final double FIRE_STORM_SPREAD = 3.0;
	private static final double HEIGHT_SPREAD = 5.0;
	private static final double FIRE_DETECTION_RADIUS = 2.0;

	// Minion spawning
	private static final int MINION_SPAWN_COUNT = 5;
	private static final int MAGMA_CUBE_SIZE = 3;
	private static final double MAGMA_CUBE_HEALTH = 10;

	// ======================== FIELDS ========================

	private int attackCooldown = ATTACK_COOLDOWN_TICKS;
	private int summonCooldown = 0;
	private WitherSkeleton witherSkeleton;

	// ======================== FACTORY & CONSTRUCTORS ========================

	private static TrialBoss.Builder createBuilder(Map<ItemStack, Double> drops,
												   double healthMultiplier,
												   double damageMultiplier) {
		return new TrialBoss.Builder()
				.id(BOSS_ID)
				.name(ColorParser.colorizeString(BOSS_NAME,
						ColorParser.generateGradient("#EC6000", "#FB0808", GRADIENT_LENGTH), true))
				.maxHealth(BASE_HEALTH * healthMultiplier)
				.damage(BASE_DAMAGE * damageMultiplier)
				.defense(0)
				.speed(BASE_SPEED)
				.scale(BASE_SCALE)
				.type(EntityType.WITHER_SKELETON)
				.drops(drops);
	}

	public HellsGatekeeper(Map<ItemStack, Double> drops) {
		this(drops, 1.0, 1.0);
	}

	public HellsGatekeeper(Map<ItemStack, Double> drops, double healthMultiplier, double damageMultiplier) {
		super(createBuilder(drops, healthMultiplier, damageMultiplier));
	}

	// ======================== RUN LOOP ========================

    @Override
    protected void onTick() {
        if (summonCooldown > 0) summonCooldown--;

        if (attackCooldown > 0) attackCooldown--;
        else {
            attackCooldown = ATTACK_COOLDOWN_TICKS;
            attack();
        }
    }

	// ======================== ATTACK SELECTION ========================

	@Override
	public void attack() {
		if (ThreadLocalRandom.current().nextDouble() < 0.5) {
			fireStorm();
			return;
		}

		if (summonCooldown > 0) return;

		double roll = ThreadLocalRandom.current().nextDouble();
		if (roll < 0.3) spawnMagmaCubes();
		else if (roll < 0.7) spawnBlazes();
		else spawnPiglinBrutes();
		summonCooldown = SUMMON_COOLDOWN_TICKS;
	}

	// ======================== STARTUP ========================

	@Override
	public void startScript() {
		this.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);
	}

	// ======================== ABILITIES ========================

	private void fireStorm() {
		new BukkitRunnable() {
			int ticks = 0;

			@Override
			public void run() {
				if (ticks >= FIRE_STORM_DURATION_TICKS) {
					cancel();
					return;
				}

				Location bossLocation = witherSkeleton.getLocation();
				World world = bossLocation.getWorld();
				if (world == null) {
					cancel();
					return;
				}

				ThreadLocalRandom random = ThreadLocalRandom.current();

				for (int i = 0; i < FIRE_STORM_PARTICLES_PER_TICK; i++) {
					double angle = random.nextDouble() * 2 * Math.PI;
					double distance = random.nextDouble() * FIRE_STORM_SPREAD;
					double x = bossLocation.getX() + distance * Math.cos(angle);
					double z = bossLocation.getZ() + distance * Math.sin(angle);
					double y = bossLocation.getY() + random.nextDouble() * HEIGHT_SPREAD;

					Location particleLocation = new Location(world, x, y, z);
					world.spawnParticle(Particle.FLAME, particleLocation, 20, 0.5, 0.5, 0.5, 0.01);

					for (Player player : world.getPlayers()) {
						if (player.getLocation().distance(particleLocation) <= FIRE_DETECTION_RADIUS) {
							player.damage(FIRE_STORM_DAMAGE);
							player.setFireTicks(FIRE_TICK_DURATION);
						}
					}
				}
				ticks++;
			}
		}.runTaskTimer(SurvivalSkills.getPlugin(SurvivalSkills.class), 0, 1);
	}

	private void spawnMagmaCubes() {
		spawnMinions(EntityType.MAGMA_CUBE, ChatColor.RED + "Hell's Cube", MINION_SPAWN_COUNT, cube -> {
			MagmaCube magmaCube = (MagmaCube) cube;
			magmaCube.setSize(MAGMA_CUBE_SIZE);
			AttributeInstance health = magmaCube.getAttribute(Attribute.MAX_HEALTH);
			if (health != null) health.setBaseValue(MAGMA_CUBE_HEALTH);
			magmaCube.setHealth(MAGMA_CUBE_HEALTH);
		});
	}

	private void spawnBlazes() {
		spawnMinions(EntityType.BLAZE, ChatColor.RED + "Hell's Blaze", MINION_SPAWN_COUNT, null);
	}

	private void spawnPiglinBrutes() {
		spawnMinions(EntityType.PIGLIN_BRUTE, ChatColor.RED + "Hell's Brute", MINION_SPAWN_COUNT, null);
	}

	// ======================== TYPE-SPECIFIC SPAWN ========================

	@Override
	public void handleTypeSpecificSpawn() {
		this.witherSkeleton = (WitherSkeleton) getBoss();
		ItemStack[] armor = new ItemStack[4];
		armor[2] = TrialManager.getTrialItem(Material.NETHERITE_CHESTPLATE, 1);
		if (witherSkeleton.getEquipment() != null)
			witherSkeleton.getEquipment().setArmorContents(armor);
	}

	// ======================== DUPLICATE ========================

	@Override
	public HellsGatekeeper duplicate(double healthMultiplier, double damageMultiplier) {
		return new HellsGatekeeper(getDrops(), healthMultiplier, damageMultiplier);
	}

	@Override
	public HellsGatekeeper duplicate() {
		return new HellsGatekeeper(getDrops());
	}
}
