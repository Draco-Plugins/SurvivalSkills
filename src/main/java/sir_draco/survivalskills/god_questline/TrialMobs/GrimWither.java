package sir_draco.survivalskills.god_questline.TrialMobs;

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

import java.util.HashMap;
import java.util.Objects;
public class GrimWither extends TrialBoss {

    private final int maxCooldown = 20 * 3;

    private int cooldown = maxCooldown;

    private Wither wither = null;

    public GrimWither(HashMap<ItemStack, Double> drops) {
        super("grimWither", ColorParser.colorizeString("Grim Wither",
                ColorParser.generateGradient("#EC6000", "#FB0808", 11), true),
                200, 15, 0, 0.3, 1.5, EntityType.WITHER, drops);
    }

    public GrimWither(HashMap<ItemStack, Double> drops, double healthMultiplier, double damageMultiplier) {
        super("grimWither", ColorParser.colorizeString("Grim Wither",
                ColorParser.generateGradient("#F7F7F7", "#3D3D3F", 11), true),
                200 * healthMultiplier, 15 * damageMultiplier, 0, 0.3, 1.5, EntityType.WITHER, drops);
    }

    @Override
    public void run() {
        if (getBoss() == null || getBoss().isDead()) {
            cancel();
            return;
        }

        updateBossBar();
        manageBossBarPlayers();

        // Handle attacks
        if (cooldown > 0) cooldown--;
        else {
            cooldown = maxCooldown;
            attack();
        }
    }

    @Override
    public void attack() {
        double chance = Math.random();
        if (chance < 0.2) shootPoisonTrident();
        else if (chance < 0.5) spawnWitherMistCircle();
        else if (chance < 0.7) shootGravitySphere();
        else if (chance < 0.9) delayedTeleport();
        else shootDeathMistBall();
    }

    public void startScript() {
        this.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);
    }

    private void delayedTeleport() {
        if (wither == null || wither.isDead()) return;

        LivingEntity target = wither.getTarget();
        if (!(target instanceof Player player)) return;

        // Store the player's current location
        final Location originalLocation = player.getLocation().clone();

        // Visual effect at the marked location
        player.getWorld().spawnParticle(
                Particle.PORTAL,
                originalLocation.clone().add(0, 1, 0),
                30, 0.5, 1.0, 0.5, 0.05
        );

        // Teleport after 3 seconds
        new BukkitRunnable() {
            private int counter = 0;

            @Override
            public void run() {
                counter++;

                // After 3 seconds (60 ticks)
                if (counter >= 60) {
                    // Only teleport if player is still online and alive
                    if (player.isOnline() && !player.isDead()) {
                        // Message and sound before teleport
                        player.sendMessage(ChatColor.DARK_PURPLE.toString() + ChatColor.BOLD + "Grim Wither: " + ChatColor.RESET + ChatColor.RED + "Return!");
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);

                        // Teleport effect at departure location
                        player.getWorld().spawnParticle(
                                Particle.REVERSE_PORTAL,
                                player.getLocation().add(0, 1, 0),
                                30, 0.5, 1.0, 0.5, 0.1
                        );

                        // Teleport player
                        player.teleport(originalLocation);

                        // Teleport effect at arrival
                        player.getWorld().spawnParticle(
                                Particle.PORTAL,
                                originalLocation.clone().add(0, 1, 0),
                                30, 0.5, 1.0, 0.5, 0.1
                        );
                    }

                    cancel();
                }
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), 0L, 1L);
    }

    private void shootGravitySphere() {
        if (wither == null || wither.isDead()) return;

        // Get the center head position (scaled wither is 1.5x)
        Location headLoc = wither.getLocation().clone().add(0, 3.75, 0);

        // Create the gravity sphere using an area effect cloud
        AreaEffectCloud sphere = (AreaEffectCloud) wither.getWorld().spawnEntity(
                headLoc, EntityType.AREA_EFFECT_CLOUD);

        // Configure the sphere
        sphere.setRadius(0.8f);
        sphere.setDuration(200);
        sphere.setColor(Color.fromRGB(128, 0, 128)); // Purple
        sphere.setParticle(Particle.DRAGON_BREATH);
        sphere.setMetadata("gravity_sphere", new FixedMetadataValue(SurvivalSkills.getInstance(), true));

        // Initial spawn effects
        wither.getWorld().playSound(headLoc, Sound.BLOCK_END_PORTAL_SPAWN, 1.0f, 1.5f);
        wither.getWorld().spawnParticle(Particle.DRAGON_BREATH, headLoc, 20, 0.3, 0.3, 0.3, 0.05);
        wither.getWorld().spawnParticle(Particle.REVERSE_PORTAL, headLoc, 10, 0.2, 0.2, 0.2, 0.1);

        // Launch direction - aimed slightly upward for an arcing effect
        Vector direction = headLoc.getDirection().multiply(1.2).setY(0.4);

        new BukkitRunnable() {
            private int ticksLived = 0;
            private boolean isExploding = false;

            @Override
            public void run() {
                // 3 seconds
                int EXPLOSION_TIME = 60;
                if (!sphere.isValid() || ticksLived >= EXPLOSION_TIME + 10) {
                    if (sphere.isValid()) sphere.remove();
                    cancel();
                    return;
                }

                // Update position for first 20 ticks (1 second)
                if (ticksLived < 20) {
                    sphere.teleport(sphere.getLocation().add(direction));

                    // Slow down over time
                    if (ticksLived % 5 == 0) {
                        direction.multiply(0.8);
                    }
                }

                // Start preparing for explosion
                if (ticksLived >= EXPLOSION_TIME - 20 && !isExploding) {
                    // Increase particle frequency as explosion approaches
                    sphere.getWorld().spawnParticle(
                            Particle.DRAGON_BREATH,
                            sphere.getLocation(),
                            15, 0.6, 0.6, 0.6, 0.05
                    );

                    // Pulsating effect
                    if (ticksLived % 4 == 0) {
                        sphere.setRadius(sphere.getRadius() + 0.15f);
                    } else if (ticksLived % 2 == 0) {
                        sphere.setRadius(Math.max(0.8f, sphere.getRadius() - 0.1f));
                    }
                }

                // Pull effect on nearby entities
                double PULL_RADIUS = 6.0;
                for (Entity entity : sphere.getNearbyEntities(PULL_RADIUS, PULL_RADIUS, PULL_RADIUS)) {
                    if (entity instanceof LivingEntity && entity != wither && !(entity instanceof ArmorStand)) {
                        Vector pullDirection = sphere.getLocation().toVector()
                                .subtract(entity.getLocation().toVector());

                        double distance = pullDirection.length();
                        if (distance <= 0.5) {
                            continue; // Prevent extreme velocities when very close
                        }

                        // Stronger pull when closer
                        double pullFactor = 1.0 - (distance / PULL_RADIUS);
                        pullFactor = Math.min(1.0, pullFactor * 1.5); // Increase pull effect

                        double PULL_STRENGTH = 0.15;
                        pullDirection.normalize().multiply(PULL_STRENGTH * pullFactor);
                        entity.setVelocity(entity.getVelocity().add(pullDirection));

                        // Visual effect to show pull
                        if (ticksLived % 5 == 0) {
                            entity.getWorld().spawnParticle(
                                    Particle.PORTAL,
                                    entity.getLocation().add(0, 1, 0),
                                    5, 0.2, 0.4, 0.2, 0
                            );
                        }
                    }
                }

                // Explosion
                if (ticksLived == EXPLOSION_TIME) {
                    isExploding = true;
                    Location explosionLoc = sphere.getLocation();

                    // Visual and sound effects
                    Objects.requireNonNull(explosionLoc.getWorld()).spawnParticle(
                            Particle.DRAGON_BREATH,
                            explosionLoc,
                            80, 2.0, 2.0, 2.0, 0.2
                    );
                    explosionLoc.getWorld().spawnParticle(
                            Particle.REVERSE_PORTAL,
                            explosionLoc,
                            40, 1.5, 1.5, 1.5, 0.5
                    );
                    explosionLoc.getWorld().playSound(
                            explosionLoc,
                            Sound.ENTITY_GENERIC_EXPLODE,
                            1.5f, 0.7f
                    );

                    // Damage nearby entities
                    double EXPLOSION_RADIUS = 4.0;
                    for (Entity entity : explosionLoc.getWorld().getNearbyEntities(
                            explosionLoc, EXPLOSION_RADIUS, EXPLOSION_RADIUS, EXPLOSION_RADIUS)) {
                        if (entity instanceof LivingEntity livingEntity && entity != wither) {
                            double distance = entity.getLocation().distance(explosionLoc);
                            double EXPLOSION_DAMAGE = 12.0;
                            double damage = EXPLOSION_DAMAGE * (1 - (distance / EXPLOSION_RADIUS));

                            if (damage > 0) {
                                livingEntity.damage(damage, wither);
                                // Knock back entities from explosion center
                                Vector knockback = entity.getLocation().toVector()
                                        .subtract(explosionLoc.toVector())
                                        .normalize()
                                        .multiply(1.5);
                                entity.setVelocity(entity.getVelocity().add(knockback));

                                // Apply wither effect
                                if (livingEntity instanceof Player) {
                                    livingEntity.addPotionEffect(new PotionEffect(
                                            PotionEffectType.WITHER, 100, 1
                                    ));
                                }
                            }
                        }
                    }
                }

                ticksLived++;
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), 1L, 1L);
    }

    public void shootPoisonTrident() {
        if (wither == null || wither.isDead()) return;

        // Get the wither's location, adjusted for the scaled size (1.5x)
        Location witherHeadLoc = wither.getLocation().clone().add(0, 3.75, 0);

        // Calculate initial direction vector toward the target
        LivingEntity target = wither.getTarget();
        if (target == null) return;
        Vector direction = target.getEyeLocation().subtract(witherHeadLoc).toVector().normalize();

        // Spawn the trident
        Trident trident = (Trident) wither.getWorld().spawnEntity(witherHeadLoc, EntityType.TRIDENT);

        // Configure the trident
        trident.setVelocity(direction.multiply(2.0));
        trident.setGlowing(true);
        trident.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        trident.setGravity(false);
        trident.setPersistent(false);
        trident.setMetadata("poison_trident", new FixedMetadataValue(SurvivalSkills.getInstance(), true));

        // Play sound effect
        Objects.requireNonNull(witherHeadLoc.getWorld()).playSound(witherHeadLoc, Sound.ITEM_TRIDENT_THROW, 2.0f, 0.6f);

        // Create visual overlay with armor stand
        ItemStack tridentItem = new ItemStack(Material.TRIDENT);
        ItemMeta meta = tridentItem.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.LOYALTY, 1, true);
            tridentItem.setItemMeta(meta);
        }

        ArmorStand invisibleStand = (ArmorStand) witherHeadLoc.getWorld().spawnEntity(
                witherHeadLoc, EntityType.ARMOR_STAND);
        invisibleStand.setVisible(false);
        invisibleStand.setGravity(false);
        invisibleStand.setInvulnerable(true);
        invisibleStand.setSmall(true);
        invisibleStand.setMarker(true);
        if (invisibleStand.getEquipment() != null) {
            invisibleStand.getEquipment().setItemInMainHand(tridentItem);
        }
        invisibleStand.setMetadata("poison_trident_stand", new FixedMetadataValue(SurvivalSkills.getInstance(), true));

        // Cache particle color option for better performance
        final Particle.DustOptions greenDust = new Particle.DustOptions(Color.fromRGB(0, 180, 0), 1.0f);
        final Particle.DustOptions impactDust = new Particle.DustOptions(Color.fromRGB(0, 180, 0), 2.0f);

        // Use a more efficient data structure for the trail
        final int maxTrailPoints = 60;
        final Location[] poisonTrailLocations = new Location[maxTrailPoints];
        int trailIndex = 0;

        new BukkitRunnable() {
            private int ticksLived = 0;
            private double currentSpeed = 2.0;
            private int trailPointIndex = 0;

            @Override
            public void run() {
                ticksLived++;

                // Check if the trident is still valid
                int MAX_LIFETIME = 100;
                if (!trident.isValid() || ticksLived >= MAX_LIFETIME) {
                    cleanupEntities();
                    cancel();
                    return;
                }

                // Update the armor stand position
                invisibleStand.teleport(trident.getLocation());

                // Decelerate the trident
                double DECELERATION = 0.02;
                currentSpeed = Math.max(0.1, currentSpeed - DECELERATION);
                Vector newVelocity = trident.getVelocity().normalize().multiply(currentSpeed);
                trident.setVelocity(newVelocity);

                // Record trail position (only store every other tick for efficiency)
                if (ticksLived % 2 == 0) {
                    Location trailLoc = trident.getLocation().clone();
                    poisonTrailLocations[trailPointIndex] = trailLoc;
                    trailPointIndex = (trailPointIndex + 1) % maxTrailPoints;

                    // Spawn poison particles
                    Objects.requireNonNull(trailLoc.getWorld()).spawnParticle(
                            Particle.DUST,
                            trailLoc,
                            8, 0.2, 0.2, 0.2, 0.01,
                            greenDust
                    );
                }

                // Check for block collision
                if (!trident.getLocation().getBlock().isPassable()) {
                    createImpactEffect(trident.getLocation(), impactDust);
                    cleanupEntities();
                    cancel();
                    return;
                }

                // Check for entity collision
                for (Entity entity : trident.getNearbyEntities(0.8, 0.8, 0.8)) {
                    if (entity instanceof LivingEntity livingEntity && entity != wither && !(entity instanceof ArmorStand)) {
                        handleEntityHit(livingEntity);
                        cleanupEntities();
                        cancel();
                        return;
                    }
                }

                // Check for players in the poison trail (every 5 ticks)
                if (ticksLived % 5 == 0) {
                    checkPlayersInTrail();
                }
            }

            private void createImpactEffect(Location location, Particle.DustOptions dustOptions) {
                World world = location.getWorld();
                if (world == null) return;

                world.spawnParticle(
                        Particle.DUST,
                        location,
                        50, 1.0, 1.0, 1.0, 0.1,
                        dustOptions
                );
                world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

                // Create a larger poison cloud at impact site
                AreaEffectCloud poisonCloud = (AreaEffectCloud) world.spawnEntity(
                        location, EntityType.AREA_EFFECT_CLOUD);
                poisonCloud.setRadius(3.0f);
                poisonCloud.setDuration(200);
                poisonCloud.setParticle(Particle.DUST, greenDust);
                poisonCloud.setColor(Color.GREEN);
                poisonCloud.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 200, 2), true);
            }

            private void handleEntityHit(LivingEntity target) {
                // Deal damage and apply poison effect
                target.damage(10.0, wither);
                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 2));

                // Visual and sound effects
                World world = target.getWorld();
                world.playSound(target.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 0.5f);
                world.spawnParticle(
                        Particle.DUST,
                        target.getLocation().add(0, 1, 0),
                        30, 0.5, 0.5, 0.5, 0.1,
                        greenDust
                );
            }

            private void checkPlayersInTrail() {
                for (Player player : trident.getWorld().getPlayers()) {
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

            private void cleanupEntities() {
                if (trident.isValid()) trident.remove();
                if (invisibleStand.isValid()) invisibleStand.remove();

                // Create final poison clouds along the trail (not at every point for performance)
                for (int i = 0; i < poisonTrailLocations.length; i += 4) {
                    Location loc = poisonTrailLocations[i];
                    if (loc != null) {
                        World world = loc.getWorld();
                        if (world != null) {
                            AreaEffectCloud poisonCloud = (AreaEffectCloud) world.spawnEntity(
                                    loc, EntityType.AREA_EFFECT_CLOUD);
                            poisonCloud.setRadius(1.0f);
                            poisonCloud.setDuration(60);
                            poisonCloud.setParticle(Particle.DUST, greenDust);
                            poisonCloud.setColor(Color.GREEN);
                            poisonCloud.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 60, 1), true);
                        }
                    }
                }
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), 1L, 1L);
    }

    private void shootDeathMistBall() {
        if (wither == null || wither.isDead()) return;

        // Get the center head position (scaled wither is 1.5x)
        Location headLoc = wither.getLocation().clone().add(0, 3.75, 0);

        // Create the mist ball using an area effect cloud
        AreaEffectCloud mistBall = (AreaEffectCloud) wither.getWorld().spawnEntity(
                headLoc, EntityType.AREA_EFFECT_CLOUD);

        // Configure the mist ball
        mistBall.setRadius(0.5f);
        mistBall.setDuration(400);
        mistBall.setColor(Color.fromRGB(10, 10, 10));
        mistBall.setParticle(Particle.LARGE_SMOKE);
        mistBall.setMetadata("death_mist", new FixedMetadataValue(SurvivalSkills.getInstance(), true));

        // Initial spawn effects
        wither.getWorld().playSound(headLoc, Sound.ENTITY_WITHER_SHOOT, 1.2f, 0.5f);
        wither.getWorld().spawnParticle(Particle.LARGE_SMOKE, headLoc, 15, 0.2, 0.2, 0.2, 0.05);
        wither.getWorld().spawnParticle(Particle.SOUL, headLoc, 5, 0.1, 0.1, 0.1, 0.02);

        new BukkitRunnable() {
            private int ticksLived = 0;
            private static final double TRACKING_SPEED = 0.12;
            private static final double SPLASH_RADIUS = 3.0;
            private static final double MAX_SPLASH_DAMAGE = 8.0;

            @Override
            public void run() {
                if (wither == null || wither.isDead()) {
                    mistBall.remove();
                    cancel();
                    return;
                }

                if (!mistBall.isValid() || ticksLived++ > 400) {
                    mistBall.remove();
                    cancel();
                    return;
                }

                // Particle trail effect
                mistBall.getWorld().spawnParticle(Particle.LARGE_SMOKE,
                        mistBall.getLocation(), 3, 0.1, 0.1, 0.1, 0.01);
                mistBall.getWorld().spawnParticle(Particle.SOUL,
                        mistBall.getLocation(), 1, 0.05, 0.05, 0.05, 0);

                // Check block collision
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

                // Check player collision
                for (Entity entity : mistBall.getNearbyEntities(0.7, 0.7, 0.7)) {
                    if (entity instanceof Player player) {
                        handlePlayerHit(player);
                        return;
                    }
                }
            }

            private void handleExplosion(Location location) {
                // Visual and sound effects
                if (location.getWorld() == null) return;
                location.getWorld().spawnParticle(Particle.LARGE_SMOKE, location, 40, 1.5, 1.5, 1.5, 0.1);
                location.getWorld().spawnParticle(Particle.SOUL, location, 20, 1.0, 1.0, 1.0, 0.1);
                location.getWorld().playSound(location, Sound.ENTITY_WITHER_HURT, 1.0f, 0.5f);

                // Splash damage
                location.getWorld().getNearbyEntities(location, SPLASH_RADIUS, SPLASH_RADIUS, SPLASH_RADIUS).forEach(entity -> {
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
                // Kill effect
                player.damage(1000, wither);

                // Visual and sound effects
                Location loc = player.getLocation();
                if (loc.getWorld() == null) return;
                loc.getWorld().spawnParticle(Particle.LARGE_SMOKE, loc, 30, 0.5, 1, 0.5, 0.1);
                loc.getWorld().spawnParticle(Particle.SOUL, loc, 15, 0.3, 0.8, 0.3, 0.05);
                loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_DEATH, 0.8f, 0.5f);

                mistBall.remove();
                cancel();
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), 1L, 1L);
    }

    private void spawnWitherMistCircle() {
        if (wither == null || wither.isDead()) return;

        // Use the wither's initial location for the circle center
        final Location center = wither.getLocation().clone();
        final int radius = 3;
        final int particleDensity = 100; // Controls how many particles make up the circle
        final int duration = 5 * 20; // Duration in ticks (5 seconds)

        // Pre-calculate particle positions to improve efficiency
        final Location[] particleLocations = new Location[particleDensity];

        // Find ground locations for all particles in the circle
        for (int i = 0; i < particleDensity; i++) {
            double angle = 2 * Math.PI * i / particleDensity;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);

            Location particleLoc = center.clone().add(x, 0, z);
            Location groundLoc = findGroundBelow(particleLoc);

            if (groundLoc != null) {
                groundLoc.add(0, 0.1, 0); // Slightly above ground
                particleLocations[i] = groundLoc;
            }
        }

        new BukkitRunnable() {
            int ticksRemaining = duration;

            @Override
            public void run() {
                if (wither == null || wither.isDead() || ticksRemaining <= 0) {
                    cancel();
                    return;
                }

                // Display particles at pre-calculated locations
                for (Location loc : particleLocations) {
                    if (loc != null && loc.getWorld() != null) {
                        loc.getWorld().spawnParticle(
                                Particle.LARGE_SMOKE, loc, 1, 0.05, 0.05, 0.05, 0);
                        loc.getWorld().spawnParticle(
                                Particle.SOUL, loc, 1, 0.05, 0.05, 0.05, 0);
                    }
                }

                // Apply wither effect to players within the circle
                for (Entity entity : Objects.requireNonNull(center.getWorld()).getNearbyEntities(center, radius, radius, radius)) {
                    if (entity instanceof Player player) {

                        // Check if player is within circle on XZ plane (horizontal radius)
                        Location playerLoc = player.getLocation();
                        double distanceSquared = Math.pow(playerLoc.getX() - center.getX(), 2)
                                + Math.pow(playerLoc.getZ() - center.getZ(), 2);

                        if (distanceSquared <= radius * radius) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 60, 1));

                            // Visual feedback
                            player.playSound(player.getLocation(), Sound.ENTITY_SPLASH_POTION_BREAK, 0.5f, 0.8f);
                        }
                    }
                }

                ticksRemaining--;
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), 0L, 4L); // Run every 4 ticks (5 times per second)
    }

    private Location findGroundBelow(Location start) {
        Location check = start.clone();

        // Check if we're already at a solid block
        if (!check.getBlock().isPassable()) {
            if (check.add(0, 1, 0).getBlock().isPassable())
                return check.subtract(0, 1, 0);
            // Look for a solid block above the current location
            else return findGroundBelow(check);
        }


        // Look downward for a solid block
        for (int i = 0; i < 10; i++) {
            check.subtract(0, 1, 0);
            if (!check.getBlock().isPassable()) return check.add(0, 1, 0);

            // If we hit bedrock or the void, stop searching
            if (check.getY() <= -64) return null;
        }

        // Couldn't find ground within maxDistance
        return null;
    }

    @Override
    public void handleTypeSpecificSpawn() {
        this.wither = (Wither) getBoss();
    }

    public GrimWither duplicate(double healthMultiplier, double damageMultiplier) {
        return new GrimWither(getDrops(), healthMultiplier, damageMultiplier);
    }

    public GrimWither duplicate() {
        return new GrimWither(getDrops());
    }
}
