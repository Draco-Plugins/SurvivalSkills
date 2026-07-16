package sir_draco.survivalskills.god_questline.trial_mobs;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.ColorParser;

import java.util.Map;

public class GrimWither extends TrialBoss {

	// ======================== CONSTANTS ========================

	// Constructor defaults
	private static final String BOSS_ID = "grimWither";
	private static final String BOSS_NAME = "Grim Wither";
	private static final double BASE_HEALTH = 200;
	private static final double BASE_DAMAGE = 15;
	private static final double BASE_SPEED = 0.3;
	private static final double BASE_SCALE = 1.5;
	private static final int GRADIENT_LENGTH = 11;

	// Attack probability thresholds
	private static final double POISON_TRIDENT_CHANCE = 0.2;
	private static final double MIST_CIRCLE_CHANCE = 0.5;
	private static final double GRAVITY_SPHERE_CHANCE = 0.7;
	private static final double TELEPORT_CHANCE = 0.9;

	// Cooldown
	private static final int ATTACK_COOLDOWN_TICKS = 20 * 3;

	// Head offset for scaled wither (1.5x)
	private static final double HEAD_OFFSET_Y = 3.75;

	// Delayed teleport
	private static final int TELEPORT_DELAY_TICKS = 60;
	private static final int TELEPORT_PARTICLE_COUNT = 30;

	// Gravity sphere
	private static final float SPHERE_INITIAL_RADIUS = 0.8f;
	private static final int SPHERE_DURATION_TICKS = 200;
	private static final double LAUNCH_SPEED = 1.2;
	private static final double LAUNCH_Y_OFFSET = 0.4;
	private static final int EXPLOSION_DELAY_TICKS = 60;
	private static final int LAUNCH_PHASE_TICKS = 20;
	private static final double VELOCITY_DECELERATION = 0.8;
	private static final double PULL_RADIUS = 6.0;
	private static final double PULL_STRENGTH = 0.15;
	private static final double EXPLOSION_RADIUS = 4.0;
	private static final double EXPLOSION_DAMAGE = 12.0;
	private static final double KNOCKBACK_STRENGTH = 1.5;
	private static final int WITHER_EFFECT_DURATION = 100;
	private static final int WITHER_EFFECT_AMPLIFIER = 1;

	// Poison trident projectile
	private static final double TRIDENT_INITIAL_SPEED = 2.0;
	private static final int TRIDENT_MAX_LIFETIME = 100;
	private static final double TRIDENT_DECELERATION = 0.02;
	private static final double TRIDENT_MIN_SPEED = 0.1;
	private static final int MAX_TRAIL_POINTS = 60;
	private static final double TRIDENT_COLLISION_RADIUS = 0.8;
	private static final double POISON_DAMAGE = 10.0;

	// Poison cloud / trail
	private static final int POISON_DURATION_TICKS = 200;
	private static final int POISON_AMPLIFIER = 2;
	private static final float POISON_CLOUD_RADIUS = 3.0f;
	private static final int POISON_CLOUD_DURATION = 200;
	private static final int TRAIL_CLOUD_DURATION = 60;
	private static final float TRAIL_CLOUD_RADIUS = 1.0f;

	// Death mist ball
	private static final float MIST_INITIAL_RADIUS = 0.5f;
	private static final int MIST_MAX_DURATION = 400;
	private static final double TRACKING_SPEED = 0.12;
	private static final double SPLASH_RADIUS = 3.0;
	private static final double MAX_SPLASH_DAMAGE = 8.0;
	private static final double MIST_COLLISION_RADIUS = 0.7;
	private static final double INSTANT_KILL_DAMAGE = 1000;

	// Wither mist circle
	private static final int MIST_CIRCLE_RADIUS = 3;
	private static final int PARTICLE_DENSITY = 100;
	private static final int MIST_CIRCLE_DURATION_TICKS = 5 * 20;
	private static final int WITHER_CIRCLE_DURATION = 60;
	private static final int WITHER_CIRCLE_AMPLIFIER = 1;

	// Ground search bounds
	private static final int MAX_GROUND_SEARCH_DOWN = 10;
	private static final int MAX_GROUND_SEARCH_UP = 20;

	// ======================== FIELDS ========================

	private int cooldown = ATTACK_COOLDOWN_TICKS;
	private Wither wither;

	// ======================== FACTORY & CONSTRUCTORS ========================

	private static TrialBoss.Builder createBuilder(Map<ItemStack, Double> drops,
												   double healthMultiplier,
												   double damageMultiplier,
												   String color1,
												   String color2) {
		return new TrialBoss.Builder()
				.id(BOSS_ID)
				.name(ColorParser.colorizeString(BOSS_NAME,
						ColorParser.generateGradient(color1, color2, GRADIENT_LENGTH), true))
				.maxHealth(BASE_HEALTH * healthMultiplier)
				.damage(BASE_DAMAGE * damageMultiplier)
				.defense(0)
				.speed(BASE_SPEED)
				.scale(BASE_SCALE)
				.type(EntityType.WITHER)
				.drops(drops);
	}

	public GrimWither(Map<ItemStack, Double> drops) {
		super(createBuilder(drops, 1.0, 1.0, "#EC6000", "#FB0808"));
	}

	public GrimWither(Map<ItemStack, Double> drops, double healthMultiplier, double damageMultiplier) {
		super(createBuilder(drops, healthMultiplier, damageMultiplier, "#F7F7F7", "#3D3D3F"));
	}

	// ======================== GUARD HELPERS ========================

	private boolean isWitherAlive() {
		return wither != null && !wither.isDead();
	}

	// ======================== RUN LOOP ========================

    @Override
    protected void onTick() {
        if (cooldown > 0) cooldown--;
        else {
            cooldown = ATTACK_COOLDOWN_TICKS;
            attack();
        }
    }

	// ======================== ATTACK SELECTION ========================

	@Override
	public void attack() {
		if (!isWitherAlive()) return;

		double chance = Math.random();
		if (chance < POISON_TRIDENT_CHANCE) shootPoisonTrident();
		else if (chance < MIST_CIRCLE_CHANCE) spawnWitherMistCircle();
		else if (chance < GRAVITY_SPHERE_CHANCE) shootGravitySphere();
		else if (chance < TELEPORT_CHANCE) delayedTeleport();
		else shootDeathMistBall();
	}

	// ======================== STARTUP ========================

	@Override
	public void startScript() {
		this.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);
	}

	// ======================== ABILITIES ========================

	private void delayedTeleport() {
		if (!isWitherAlive()) return;

		LivingEntity target = wither.getTarget();
		if (!(target instanceof Player player)) return;

		final Location originalLocation = player.getLocation().clone();

		player.getWorld().spawnParticle(
				Particle.PORTAL,
				originalLocation.clone().add(0, 1, 0),
				TELEPORT_PARTICLE_COUNT, 0.5, 1.0, 0.5, 0.05
		);

		new BukkitRunnable() {
			private int counter = 0;

			@Override
			public void run() {
				counter++;

				if (counter >= TELEPORT_DELAY_TICKS) {
					if (player.isOnline() && !player.isDead()) {
						player.sendMessage(ChatColor.DARK_PURPLE.toString() + ChatColor.BOLD
								+ "Grim Wither: " + ChatColor.RESET + ChatColor.RED + "Return!");
						player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);

						player.getWorld().spawnParticle(
								Particle.REVERSE_PORTAL,
								player.getLocation().add(0, 1, 0),
								TELEPORT_PARTICLE_COUNT, 0.5, 1.0, 0.5, 0.1
						);

						player.teleport(originalLocation);

						player.getWorld().spawnParticle(
								Particle.PORTAL,
								originalLocation.clone().add(0, 1, 0),
								TELEPORT_PARTICLE_COUNT, 0.5, 1.0, 0.5, 0.1
						);
					}

					cancel();
				}
			}
		}.runTaskTimer(SurvivalSkills.getInstance(), 0L, 1L);
	}

	private void shootGravitySphere() {
		if (!isWitherAlive()) return;

		Location headLoc = wither.getLocation().clone().add(0, HEAD_OFFSET_Y, 0);

		AreaEffectCloud sphere = (AreaEffectCloud) wither.getWorld().spawnEntity(
				headLoc, EntityType.AREA_EFFECT_CLOUD);
		sphere.setRadius(SPHERE_INITIAL_RADIUS);
		sphere.setDuration(SPHERE_DURATION_TICKS);
		sphere.setColor(Color.fromRGB(128, 0, 128));
		sphere.setParticle(Particle.DRAGON_BREATH);
		sphere.setMetadata("gravity_sphere", new FixedMetadataValue(SurvivalSkills.getInstance(), true));

		wither.getWorld().playSound(headLoc, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.5f);
		wither.getWorld().spawnParticle(Particle.DRAGON_BREATH, headLoc, 20, 0.3, 0.3, 0.3, 0.05);
		wither.getWorld().spawnParticle(Particle.REVERSE_PORTAL, headLoc, 10, 0.2, 0.2, 0.2, 0.1);

		Vector direction = headLoc.getDirection().multiply(LAUNCH_SPEED).setY(LAUNCH_Y_OFFSET);

		new GravitySphereTask(sphere, direction).runTaskTimer(SurvivalSkills.getInstance(), 1L, 1L);
	}

	private void shootPoisonTrident() {
		if (!isWitherAlive()) return;

		Location headLoc = wither.getLocation().clone().add(0, HEAD_OFFSET_Y, 0);
		LivingEntity target = wither.getTarget();
		if (target == null) return;
		Vector direction = target.getEyeLocation().subtract(headLoc).toVector().normalize();

		Trident trident = (Trident) wither.getWorld().spawnEntity(headLoc, EntityType.TRIDENT);
		trident.setVelocity(direction.multiply(TRIDENT_INITIAL_SPEED));
		trident.setGlowing(true);
		trident.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
		trident.setGravity(false);
		trident.setPersistent(false);
		trident.setMetadata("poison_trident", new FixedMetadataValue(SurvivalSkills.getInstance(), true));

		if (headLoc.getWorld() != null) {
			headLoc.getWorld().playSound(headLoc, Sound.ITEM_TRIDENT_THROW, 2.0f, 0.6f);
		}

		ItemStack tridentItem = new ItemStack(Material.TRIDENT);
		ItemMeta meta = tridentItem.getItemMeta();
		if (meta != null) {
			meta.addEnchant(Enchantment.LOYALTY, 1, true);
			tridentItem.setItemMeta(meta);
		}

		ArmorStand invisibleStand = (ArmorStand) headLoc.getWorld().spawnEntity(
				headLoc, EntityType.ARMOR_STAND);
		invisibleStand.setVisible(false);
		invisibleStand.setGravity(false);
		invisibleStand.setInvulnerable(true);
		invisibleStand.setSmall(true);
		invisibleStand.setMarker(true);
		if (invisibleStand.getEquipment() != null) {
			invisibleStand.getEquipment().setItemInMainHand(tridentItem);
		}
		invisibleStand.setMetadata("poison_trident_stand",
				new FixedMetadataValue(SurvivalSkills.getInstance(), true));

		Particle.DustOptions greenDust = new Particle.DustOptions(Color.fromRGB(0, 180, 0), 1.0f);
		Particle.DustOptions impactDust = new Particle.DustOptions(Color.fromRGB(0, 180, 0), 2.0f);

		new PoisonTridentTask(trident, invisibleStand, greenDust, impactDust)
				.runTaskTimer(SurvivalSkills.getInstance(), 1L, 1L);
	}

	private void shootDeathMistBall() {
		if (!isWitherAlive()) return;

		Location headLoc = wither.getLocation().clone().add(0, HEAD_OFFSET_Y, 0);

		AreaEffectCloud mistBall = (AreaEffectCloud) wither.getWorld().spawnEntity(
				headLoc, EntityType.AREA_EFFECT_CLOUD);
		mistBall.setRadius(MIST_INITIAL_RADIUS);
		mistBall.setDuration(MIST_MAX_DURATION);
		mistBall.setColor(Color.fromRGB(10, 10, 10));
		mistBall.setParticle(Particle.LARGE_SMOKE);
		mistBall.setMetadata("death_mist", new FixedMetadataValue(SurvivalSkills.getInstance(), true));

		wither.getWorld().playSound(headLoc, Sound.ENTITY_WITHER_SHOOT, 1.2f, 0.5f);
		wither.getWorld().spawnParticle(Particle.LARGE_SMOKE, headLoc, 15, 0.2, 0.2, 0.2, 0.05);
		wither.getWorld().spawnParticle(Particle.SOUL, headLoc, 5, 0.1, 0.1, 0.1, 0.02);

		new DeathMistTask(mistBall).runTaskTimer(SurvivalSkills.getInstance(), 1L, 1L);
	}

	private void spawnWitherMistCircle() {
		if (!isWitherAlive()) return;

		final Location center = wither.getLocation().clone();
		final Location[] particleLocations = new Location[PARTICLE_DENSITY];

		for (int i = 0; i < PARTICLE_DENSITY; i++) {
			double angle = 2 * Math.PI * i / PARTICLE_DENSITY;
			double x = MIST_CIRCLE_RADIUS * Math.cos(angle);
			double z = MIST_CIRCLE_RADIUS * Math.sin(angle);

			Location particleLoc = center.clone().add(x, 0, z);
			Location groundLoc = findGroundBelow(particleLoc);

			if (groundLoc != null) {
				groundLoc.add(0, 0.1, 0);
				particleLocations[i] = groundLoc;
			}
		}

		new BukkitRunnable() {
			int ticksRemaining = MIST_CIRCLE_DURATION_TICKS;

			@Override
			public void run() {
				if (!isWitherAlive() || ticksRemaining <= 0) {
					cancel();
					return;
				}

				for (Location loc : particleLocations) {
					if (loc != null && loc.getWorld() != null) {
						loc.getWorld().spawnParticle(
								Particle.LARGE_SMOKE, loc, 1, 0.05, 0.05, 0.05, 0);
						loc.getWorld().spawnParticle(
								Particle.SOUL, loc, 1, 0.05, 0.05, 0.05, 0);
					}
				}

				World centerWorld = center.getWorld();
				if (centerWorld == null) return;
				for (Entity entity : centerWorld.getNearbyEntities(
						center, MIST_CIRCLE_RADIUS, MIST_CIRCLE_RADIUS, MIST_CIRCLE_RADIUS)) {
					if (entity instanceof Player player) {
						Location playerLoc = player.getLocation();
						double distanceSquared = Math.pow(playerLoc.getX() - center.getX(), 2)
								+ Math.pow(playerLoc.getZ() - center.getZ(), 2);

						if (distanceSquared <= MIST_CIRCLE_RADIUS * MIST_CIRCLE_RADIUS) {
							player.addPotionEffect(new PotionEffect(
									PotionEffectType.WITHER, WITHER_CIRCLE_DURATION, WITHER_CIRCLE_AMPLIFIER));
							player.playSound(player.getLocation(), Sound.ENTITY_SPLASH_POTION_BREAK, 0.5f, 0.8f);
						}
					}
				}

				ticksRemaining--;
			}
		}.runTaskTimer(SurvivalSkills.getInstance(), 0L, 4L);
	}

	// ======================== GROUND SEARCH ========================

	private Location findGroundBelow(Location start) {
		Location check = start.clone();

		// If current block is solid, search upward for a passable block
		if (!check.getBlock().isPassable()) {
			if (check.add(0, 1, 0).getBlock().isPassable()) {
				return check.subtract(0, 1, 0);
			}
			// check is now at Y+1; continue searching up
			for (int i = 0; i < MAX_GROUND_SEARCH_UP; i++) {
				check.add(0, 1, 0);
				if (check.getBlock().isPassable()) {
					return check.subtract(0, 1, 0);
				}
			}
			return null;
		}

		// Current block is passable; search downward for the surface
		for (int i = 0; i < MAX_GROUND_SEARCH_DOWN; i++) {
			check.subtract(0, 1, 0);
			if (!check.getBlock().isPassable()) return check.add(0, 1, 0);
			if (check.getY() <= -64) return null;
		}

		return null;
	}

	// ======================== TYPE-SPECIFIC SPAWN ========================

	@Override
	public void handleTypeSpecificSpawn() {
		this.wither = (Wither) getBoss();
	}

	// ======================== DUPLICATE ========================

	@Override
	public GrimWither duplicate(double healthMultiplier, double damageMultiplier) {
		return new GrimWither(getDrops(), healthMultiplier, damageMultiplier);
	}

	@Override
	public GrimWither duplicate() {
		return new GrimWither(getDrops());
	}

	// ======================== INNER CLASSES ========================

	private class GravitySphereTask extends BukkitRunnable {
		private final AreaEffectCloud sphere;
		private final Vector direction;
		private int ticksLived = 0;
		private boolean isExploding = false;

		GravitySphereTask(AreaEffectCloud sphere, Vector direction) {
			this.sphere = sphere;
			this.direction = direction;
		}

		@Override
		public void run() {
			if (!sphere.isValid() || ticksLived >= EXPLOSION_DELAY_TICKS + 10) {
				if (sphere.isValid()) sphere.remove();
				cancel();
				return;
			}

			// Launch phase: move and decelerate
			if (ticksLived < LAUNCH_PHASE_TICKS) {
				sphere.teleport(sphere.getLocation().add(direction));
				if (ticksLived % 5 == 0) {
					direction.multiply(VELOCITY_DECELERATION);
				}
			}

			// Pre-explosion pulsating build-up
			if (ticksLived >= EXPLOSION_DELAY_TICKS - 20 && !isExploding) {
				sphere.getWorld().spawnParticle(
						Particle.DRAGON_BREATH,
						sphere.getLocation(),
						15, 0.6, 0.6, 0.6, 0.05);

				if (ticksLived % 4 == 0) {
					sphere.setRadius(sphere.getRadius() + 0.15f);
				} else if (ticksLived % 2 == 0) {
					sphere.setRadius(Math.max(SPHERE_INITIAL_RADIUS, sphere.getRadius() - 0.1f));
				}
			}

			// Pull nearby entities toward the sphere
			pullEntities();

			// Explosion
			if (ticksLived == EXPLOSION_DELAY_TICKS) {
				detonate();
			}

			ticksLived++;
		}

		private void pullEntities() {
			for (Entity entity : sphere.getNearbyEntities(PULL_RADIUS, PULL_RADIUS, PULL_RADIUS)) {
				if (!(entity instanceof LivingEntity) || entity == wither || entity instanceof ArmorStand) {
					continue;
				}

				Vector pullDirection = sphere.getLocation().toVector()
						.subtract(entity.getLocation().toVector());
				double distance = pullDirection.length();
				if (distance <= 0.5) continue;

				double pullFactor = 1.0 - (distance / PULL_RADIUS);
				pullFactor = Math.min(1.0, pullFactor * 1.5);

				pullDirection.normalize().multiply(PULL_STRENGTH * pullFactor);
				entity.setVelocity(entity.getVelocity().add(pullDirection));

				if (ticksLived % 5 == 0) {
					entity.getWorld().spawnParticle(
							Particle.PORTAL,
							entity.getLocation().add(0, 1, 0),
							5, 0.2, 0.4, 0.2, 0);
				}
			}
		}

		private void detonate() {
			isExploding = true;
			Location explosionLoc = sphere.getLocation();
			World world = explosionLoc.getWorld();
			if (world == null) return;

			world.spawnParticle(Particle.DRAGON_BREATH, explosionLoc, 80, 2.0, 2.0, 2.0, 0.2);
			world.spawnParticle(Particle.REVERSE_PORTAL, explosionLoc, 40, 1.5, 1.5, 1.5, 0.5);
			world.playSound(explosionLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.7f);

			for (Entity entity : world.getNearbyEntities(
					explosionLoc, EXPLOSION_RADIUS, EXPLOSION_RADIUS, EXPLOSION_RADIUS)) {
				if (entity instanceof LivingEntity livingEntity && entity != wither) {
					double distance = entity.getLocation().distance(explosionLoc);
					double damage = EXPLOSION_DAMAGE * (1 - (distance / EXPLOSION_RADIUS));

					if (damage > 0) {
						livingEntity.damage(damage, wither);

						Vector knockback = entity.getLocation().toVector()
								.subtract(explosionLoc.toVector())
								.normalize()
								.multiply(KNOCKBACK_STRENGTH);
						entity.setVelocity(entity.getVelocity().add(knockback));

						if (livingEntity instanceof Player) {
							livingEntity.addPotionEffect(new PotionEffect(
									PotionEffectType.WITHER, WITHER_EFFECT_DURATION, WITHER_EFFECT_AMPLIFIER));
						}
					}
				}
			}
		}
	}

	private class PoisonTridentTask extends BukkitRunnable {
		private final Trident trident;
		private final ArmorStand invisibleStand;
		private final Particle.DustOptions greenDust;
		private final Particle.DustOptions impactDust;
		private final Location[] poisonTrailLocations = new Location[MAX_TRAIL_POINTS];
		private int ticksLived = 0;
		private double currentSpeed = TRIDENT_INITIAL_SPEED;
		private int trailPointIndex = 0;

		PoisonTridentTask(Trident trident, ArmorStand invisibleStand,
						  Particle.DustOptions greenDust, Particle.DustOptions impactDust) {
			this.trident = trident;
			this.invisibleStand = invisibleStand;
			this.greenDust = greenDust;
			this.impactDust = impactDust;
		}

		@Override
		public void run() {
			ticksLived++;

			if (!trident.isValid() || ticksLived >= TRIDENT_MAX_LIFETIME) {
				cleanupAndLeaveTrail();
				cancel();
				return;
			}

			// Match armor stand to trident position
			invisibleStand.teleport(trident.getLocation());

			// Decelerate
			currentSpeed = Math.max(TRIDENT_MIN_SPEED, currentSpeed - TRIDENT_DECELERATION);
			trident.setVelocity(trident.getVelocity().normalize().multiply(currentSpeed));

			// Record trail particles
			if (ticksLived % 2 == 0) {
				recordTrailPoint();
			}

			// Block collision
			if (!trident.getLocation().getBlock().isPassable()) {
				createImpactEffect(trident.getLocation());
				cleanupAndLeaveTrail();
				cancel();
				return;
			}

			// Entity collision
			for (Entity entity : trident.getNearbyEntities(
					TRIDENT_COLLISION_RADIUS, TRIDENT_COLLISION_RADIUS, TRIDENT_COLLISION_RADIUS)) {
				if (entity instanceof LivingEntity livingEntity
						&& entity != wither && !(entity instanceof ArmorStand)) {
					handleEntityHit(livingEntity);
					cleanupAndLeaveTrail();
					cancel();
					return;
				}
			}

			// Periodic trail poison check
			if (ticksLived % 5 == 0) {
				checkPlayersInTrail();
			}
		}

		private void recordTrailPoint() {
			Location trailLoc = trident.getLocation().clone();
			poisonTrailLocations[trailPointIndex] = trailLoc;
			trailPointIndex = (trailPointIndex + 1) % MAX_TRAIL_POINTS;

			World world = trailLoc.getWorld();
			if (world != null) {
				world.spawnParticle(Particle.DUST, trailLoc,
						8, 0.2, 0.2, 0.2, 0.01, greenDust);
			}
		}

		private void createImpactEffect(Location location) {
			World world = location.getWorld();
			if (world == null) return;

			world.spawnParticle(Particle.DUST, location, 50, 1.0, 1.0, 1.0, 0.1, impactDust);
			world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

			AreaEffectCloud poisonCloud = (AreaEffectCloud) world.spawnEntity(
					location, EntityType.AREA_EFFECT_CLOUD);
			poisonCloud.setRadius(POISON_CLOUD_RADIUS);
			poisonCloud.setDuration(POISON_CLOUD_DURATION);
			poisonCloud.setParticle(Particle.DUST, greenDust);
			poisonCloud.setColor(Color.GREEN);
			poisonCloud.addCustomEffect(
					new PotionEffect(PotionEffectType.POISON, POISON_DURATION_TICKS, POISON_AMPLIFIER), true);
		}

		private void handleEntityHit(LivingEntity target) {
			target.damage(POISON_DAMAGE, wither);
			target.addPotionEffect(
					new PotionEffect(PotionEffectType.POISON, POISON_DURATION_TICKS, POISON_AMPLIFIER));

			World world = target.getWorld();
			world.playSound(target.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 0.5f);
			world.spawnParticle(Particle.DUST, target.getLocation().add(0, 1, 0),
					30, 0.5, 0.5, 0.5, 0.1, greenDust);
		}

		private void checkPlayersInTrail() {
			World world = trident.getWorld();
			if (world == null) return;
			for (Player player : world.getPlayers()) {
				if (player.getGameMode() == GameMode.SPECTATOR) continue;

				for (Location poisonLoc : poisonTrailLocations) {
					if (poisonLoc != null && player.getLocation().distance(poisonLoc) < 1.0) {
						if (!player.hasPotionEffect(PotionEffectType.POISON)) {
							player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
							player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.5f, 1.0f);
						}
						break;
					}
				}
			}
		}

		private void cleanupAndLeaveTrail() {
			if (trident.isValid()) trident.remove();
			if (invisibleStand.isValid()) invisibleStand.remove();

			for (int i = 0; i < poisonTrailLocations.length; i += 4) {
				Location loc = poisonTrailLocations[i];
				if (loc == null) continue;
				World world = loc.getWorld();
				if (world == null) continue;

				AreaEffectCloud poisonCloud = (AreaEffectCloud) world.spawnEntity(
						loc, EntityType.AREA_EFFECT_CLOUD);
				poisonCloud.setRadius(TRAIL_CLOUD_RADIUS);
				poisonCloud.setDuration(TRAIL_CLOUD_DURATION);
				poisonCloud.setParticle(Particle.DUST, greenDust);
				poisonCloud.setColor(Color.GREEN);
				poisonCloud.addCustomEffect(
						new PotionEffect(PotionEffectType.POISON, TRAIL_CLOUD_DURATION, 1), true);
			}
		}
	}

	private class DeathMistTask extends BukkitRunnable {
		private final AreaEffectCloud mistBall;
		private int ticksLived = 0;

		DeathMistTask(AreaEffectCloud mistBall) {
			this.mistBall = mistBall;
		}

		@Override
		public void run() {
			if (!isWitherAlive()) {
				mistBall.remove();
				cancel();
				return;
			}

			if (!mistBall.isValid() || ticksLived++ > MIST_MAX_DURATION) {
				mistBall.remove();
				cancel();
				return;
			}

			// Particle trail
			mistBall.getWorld().spawnParticle(Particle.LARGE_SMOKE,
					mistBall.getLocation(), 3, 0.1, 0.1, 0.1, 0.01);
			mistBall.getWorld().spawnParticle(Particle.SOUL,
					mistBall.getLocation(), 1, 0.05, 0.05, 0.05, 0);

			// Block collision
			if (!mistBall.getLocation().getBlock().isPassable()) {
				handleExplosion(mistBall.getLocation());
				return;
			}

			// Track target
			LivingEntity target = wither.getTarget();
			if (target != null && target.isValid() && !target.isDead()) {
				Vector direction = target.getLocation().add(0, 1, 0)
						.subtract(mistBall.getLocation())
						.toVector()
						.normalize()
						.multiply(TRACKING_SPEED);
				mistBall.teleport(mistBall.getLocation().add(direction));
			}

			// Player collision
			for (Entity entity : mistBall.getNearbyEntities(
					MIST_COLLISION_RADIUS, MIST_COLLISION_RADIUS, MIST_COLLISION_RADIUS)) {
				if (entity instanceof Player player) {
					handlePlayerHit(player);
					return;
				}
			}
		}

		private void handleExplosion(Location location) {
			if (location.getWorld() == null) return;

			location.getWorld().spawnParticle(Particle.LARGE_SMOKE, location, 40, 1.5, 1.5, 1.5, 0.1);
			location.getWorld().spawnParticle(Particle.SOUL, location, 20, 1.0, 1.0, 1.0, 0.1);
			location.getWorld().playSound(location, Sound.ENTITY_WITHER_HURT, 1.0f, 0.5f);

			location.getWorld().getNearbyEntities(
					location, SPLASH_RADIUS, SPLASH_RADIUS, SPLASH_RADIUS).forEach(entity -> {
				if (entity instanceof Player player) {
					double distance = player.getLocation().distance(location);
					double damage = MAX_SPLASH_DAMAGE * (1 - (distance / SPLASH_RADIUS));
					if (damage > 0) player.damage(damage, wither);
				}
			});

			mistBall.remove();
			cancel();
		}

		private void handlePlayerHit(Player player) {
			player.damage(INSTANT_KILL_DAMAGE, wither);

			Location loc = player.getLocation();
			if (loc.getWorld() == null) return;
			loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 30, 0.5, 1, 0.5, 0.1);
			loc.getWorld().spawnParticle(Particle.SOUL, loc, 15, 0.3, 0.8, 0.3, 0.05);
			loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_DEATH, 0.8f, 0.5f);

			mistBall.remove();
			cancel();
		}
	}
}
