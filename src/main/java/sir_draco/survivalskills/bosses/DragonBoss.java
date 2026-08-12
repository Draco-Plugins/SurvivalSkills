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
import sir_draco.survivalskills.bosses.attacks.DeathRainProjectile;
import sir_draco.survivalskills.bosses.attacks.DragonCannon;
import sir_draco.survivalskills.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
    static final double GROUND_SLAM_CHANCE = 0.05;
    static final double REGENERATOR_SPAWN_CHANCE = 0.05;
    static final double ENDERMITE_TRIGGER_RADIUS = 3.0;
    static final double GROUND_SLAM_RADIUS = 20.0;
    static final double GROUND_SLAM_DAMAGE = 30.0;
    static final int STUN_DURATION_TICKS = 20;
    static final int REGENERATOR_HEAL_INTERVAL_TICKS = 20 * 5;
    static final double REGENERATOR_HEAL_PERCENTAGE = 0.01;
    static final double REGENERATOR_HEALTH = 75.0;
    static final double REGENERATOR_DAMAGE = 10.0;
    static final double REGENERATOR_SCALE = 0.7;
    private static final int EXPLODING_ENDERMITE_COUNT = 3;
    private static final float ENDERMITE_EXPLOSION_POWER = 0.1F;
    private static final int GROUND_SLAM_TIMEOUT_TICKS = 20 * 4;
    private static final double GROUND_SLAM_IMPACT_RADIUS = 5.0;
    private static final double GROUND_SLAM_SPEED = 3.0;
    private static final double GROUND_SLAM_KNOCKBACK = 2.0;
    private static final double GROUND_SLAM_VERTICAL_KNOCKBACK = 0.75;
    private static final String STUN_MESSAGE = ChatColor.DARK_PURPLE + "You have been stunned!";

    private EnderDragon dragon;
    private final List<Location> crystalLocations = new ArrayList<>();
    private final List<Player> players = new ArrayList<>();
    private final List<Endermite> explosiveEndermites = new ArrayList<>();
    private final Map<Player, FlightState> suppressedFlightStates = new HashMap<>();
    private final Map<Player, StunState> stunnedPlayers = new HashMap<>();

    private int lightningCounter = 20 * 30;
    private int attackCounter = 20 * 10;
    private int timeSinceDragonFollowerSpawn = 0;
    private int groundSlamTicks = 0;
    private int regeneratorHealCounter = 0;
    private Location groundSlamTarget;
    private Enderman regenerator;
    private boolean initialized = false;
    private boolean crystalsFound = false;
    private boolean isRespawn = false;
    private boolean dragonsWrath = false;

    record FlightState(GameMode gameMode, boolean allowFlight, boolean flying, float flySpeed) {}

    private record StunState(int ticksRemaining, float walkSpeed) {}

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
            cleanupEncounterState();
            cancel();
            return;
        }

        enforceAltitudeLimit();
        initializeCrystals();
        handleStageTransitions();
        tickCooldowns();
        updateDragonsWrath();
        updateStunnedPlayers();
        updateExplosiveEndermites();
        updateRegenerator();

        if (updateGroundSlam()) return;

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
        if (isGroundSlamRoll(Math.random()) && startGroundSlam()) {
            trySpawnRegenerator();
            return;
        }

        switch (getStage()) {
            case 1, 2 -> {
                double chance = Math.random();
                if (chance < 1.0 / 3.0) executeCannonAttack();
                else if (chance < 2.0 / 3.0) deathRain();
                else spawnExplodingEndermites();
            }
            case 3 -> {
                double chance = Math.random();
                if (chance < 0.25) executeCannonAttack();
                else if (chance < 0.5) deathRain();
                else if (chance < 0.75) spawnAngryEndermen();
                else spawnExplodingEndermites();
            }
        }
        trySpawnRegenerator();
    }

    private void onStageEnter(int newStage) {
        if (newStage == 3) {
            resetCrystals();
            broadcastDragonMessage(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Ender Dragon: "
                    + ChatColor.RESET + "I will not go down so easily", Sound.ENTITY_ENDER_DRAGON_HURT);
        }
    }

    private void updateDragonsWrath() {
        if (!dragonsWrath && getHealthPercentage() <= 0.5) {
            dragonsWrath = true;
            broadcastDragonMessage(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Ender Dragon: "
                    + ChatColor.RESET + "Dragon's Wrath has stripped you of your powers!",
                    Sound.ENTITY_ENDER_DRAGON_GROWL);
        }
        if (!dragonsWrath) return;

        restoreFlightOutsideFight();
        players.stream()
                .filter((Player player) -> player.isOnline())
                .filter((Player player) -> player.getWorld().equals(dragon.getWorld()))
                .forEach((Player player) -> {
                    suppressedFlightStates.computeIfAbsent(player,
                            (Player participant) -> captureFlightState(participant));
                    suppressFlight(player);
                });
    }

    private void restoreFlightOutsideFight() {
        Iterator<Map.Entry<Player, FlightState>> iterator = suppressedFlightStates.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Player, FlightState> entry = iterator.next();
            Player player = entry.getKey();
            if (!player.isOnline() || player.getWorld().equals(dragon.getWorld())) continue;
            restoreFlightState(player, entry.getValue());
            iterator.remove();
        }
    }

    static FlightState captureFlightState(Player player) {
        return new FlightState(player.getGameMode(), player.getAllowFlight(), player.isFlying(), player.getFlySpeed());
    }

    static void suppressFlight(Player player) {
        player.setFlying(false);
        player.setAllowFlight(false);
    }

    static void restoreFlightState(Player player, FlightState state) {
        if (!player.isOnline() || player.getGameMode() != state.gameMode()) return;
        player.setAllowFlight(state.allowFlight());
        player.setFlying(state.allowFlight() && state.flying());
        player.setFlySpeed(state.flySpeed());
    }

    private void spawnExplodingEndermites() {
        for (int i = 0; i < EXPLODING_ENDERMITE_COUNT; i++) {
            Optional<Location> locationOptional = randomLoc(dragon.getLocation(), 30);
            if (locationOptional.isEmpty()) continue;

            Location spawnLocation = locationOptional.get().clone().add(0, 1, 0);
            Endermite endermite = (Endermite) dragon.getWorld().spawnEntity(spawnLocation, EntityType.ENDERMITE);
            endermite.setCustomName(ChatColor.DARK_PURPLE + "Unstable Endermite");
            endermite.setCustomNameVisible(true);
            endermite.setPersistent(true);
            endermite.setRemoveWhenFarAway(false);
            findNearbyTarget(endermite, 150).ifPresent((Player player) -> endermite.setTarget(player));
            explosiveEndermites.add(endermite);
        }
    }

    private void updateExplosiveEndermites() {
        Iterator<Endermite> iterator = explosiveEndermites.iterator();
        while (iterator.hasNext()) {
            Endermite endermite = iterator.next();
            if (!endermite.isValid() || endermite.isDead()) {
                iterator.remove();
                continue;
            }

            boolean playerInRange = players.stream()
                    .filter((Player player) -> player.isOnline())
                    .filter((Player player) -> player.getWorld().equals(endermite.getWorld()))
                    .anyMatch((Player player) -> isWithinRadius(player.getLocation(), endermite.getLocation(),
                            ENDERMITE_TRIGGER_RADIUS));
            if (!playerInRange) continue;

            detonateEndermite(endermite);
            iterator.remove();
        }
    }

    private void detonateEndermite(Endermite endermite) {
        Location explosionLocation = endermite.getLocation();
        players.stream()
                .filter((Player player) -> player.isOnline())
                .filter((Player player) -> player.getWorld().equals(endermite.getWorld()))
                .filter((Player player) -> isWithinRadius(player.getLocation(), explosionLocation,
                        ENDERMITE_TRIGGER_RADIUS))
                .forEach((Player player) -> stunPlayer(player));
        endermite.getWorld().createExplosion(explosionLocation, ENDERMITE_EXPLOSION_POWER, false, false, endermite);
        endermite.remove();
    }

    private void stunPlayer(Player player) {
        stunnedPlayers.compute(player, (Player stunnedPlayer, StunState currentState) -> {
            float walkSpeed = currentState == null ? stunnedPlayer.getWalkSpeed() : currentState.walkSpeed();
            return new StunState(STUN_DURATION_TICKS, walkSpeed);
        });
        player.setWalkSpeed(0);
        player.setVelocity(new Vector());
        Utils.sendActionBarMessage(player, STUN_MESSAGE);
    }

    private void updateStunnedPlayers() {
        Iterator<Map.Entry<Player, StunState>> iterator = stunnedPlayers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Player, StunState> entry = iterator.next();
            Player player = entry.getKey();
            StunState state = entry.getValue();
            if (!player.isOnline()) {
                iterator.remove();
                continue;
            }
            if (state.ticksRemaining() <= 1) {
                player.setWalkSpeed(state.walkSpeed());
                iterator.remove();
                continue;
            }

            player.setWalkSpeed(0);
            player.setVelocity(new Vector());
            entry.setValue(new StunState(state.ticksRemaining() - 1, state.walkSpeed()));
        }
    }

    private boolean startGroundSlam() {
        Optional<Player> targetOptional = findNearbyTarget(dragon, 150);
        if (targetOptional.isEmpty()) return false;

        Location playerLocation = targetOptional.get().getLocation();
        int groundY = dragon.getWorld().getHighestBlockYAt(playerLocation.getBlockX(), playerLocation.getBlockZ());
        groundSlamTarget = new Location(dragon.getWorld(), playerLocation.getX(), groundY + 1,
                playerLocation.getZ());
        groundSlamTicks = 0;
        dragon.setPhase(EnderDragon.Phase.CHARGE_PLAYER);
        broadcastDragonMessage(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Ender Dragon: "
                + ChatColor.RESET + "Be crushed beneath my wrath!", Sound.ENTITY_ENDER_DRAGON_GROWL);
        return true;
    }

    private boolean updateGroundSlam() {
        if (groundSlamTarget == null) return false;
        groundSlamTicks++;

        Vector direction = groundSlamTarget.toVector().subtract(dragon.getLocation().toVector());
        boolean reachedTarget = direction.lengthSquared() <= GROUND_SLAM_IMPACT_RADIUS * GROUND_SLAM_IMPACT_RADIUS;
        double horizontalDistanceSquared = direction.getX() * direction.getX() + direction.getZ() * direction.getZ();
        boolean reachedGround = dragon.getLocation().getY() <= groundSlamTarget.getY() + GROUND_SLAM_IMPACT_RADIUS
                && horizontalDistanceSquared <= GROUND_SLAM_IMPACT_RADIUS * GROUND_SLAM_IMPACT_RADIUS;
        if (reachedTarget || reachedGround || groundSlamTicks >= GROUND_SLAM_TIMEOUT_TICKS) {
            completeGroundSlam();
            return true;
        }

        if (direction.lengthSquared() > 0) dragon.setVelocity(direction.normalize().multiply(GROUND_SLAM_SPEED));
        return true;
    }

    private void completeGroundSlam() {
        Location impactLocation = groundSlamTarget;
        if (impactLocation == null || impactLocation.getWorld() == null) {
            clearGroundSlam();
            return;
        }

        World world = impactLocation.getWorld();
        world.playSound(impactLocation, Sound.ENTITY_GENERIC_EXPLODE, 5, 0.5F);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, impactLocation, 8, 4, 1, 4, 0);
        world.spawnParticle(Particle.END_ROD, impactLocation, 250,
                GROUND_SLAM_RADIUS / 2, 1, GROUND_SLAM_RADIUS / 2, 0.25);

        players.stream()
                .filter((Player player) -> player.isOnline())
                .filter((Player player) -> player.getWorld().equals(world))
                .filter((Player player) -> isWithinRadius(player.getLocation(), impactLocation, GROUND_SLAM_RADIUS))
                .forEach((Player player) -> applyGroundSlam(player, impactLocation));
        attackCounter = Math.max(attackCounter, getAttackCooldown());
        clearGroundSlam();
    }

    private void applyGroundSlam(Player player, Location impactLocation) {
        player.damage(GROUND_SLAM_DAMAGE, dragon);
        Vector knockback = player.getLocation().toVector().subtract(impactLocation.toVector());
        knockback.setY(0);
        if (knockback.lengthSquared() == 0) knockback.setX(1);
        knockback.normalize().multiply(GROUND_SLAM_KNOCKBACK).setY(GROUND_SLAM_VERTICAL_KNOCKBACK);
        player.setVelocity(knockback);
    }

    private void clearGroundSlam() {
        groundSlamTarget = null;
        groundSlamTicks = 0;
    }

    private void trySpawnRegenerator() {
        if (!shouldSpawnRegenerator(Math.random()) || hasLivingRegenerator()) return;
        Optional<Location> locationOptional = randomLoc(dragon.getLocation(), 30);
        if (locationOptional.isEmpty()) return;

        Location spawnLocation = locationOptional.get().clone().add(0, 1, 0);
        Enderman enderman = (Enderman) dragon.getWorld().spawnEntity(spawnLocation, EntityType.ENDERMAN);
        enderman.setCustomName(ChatColor.LIGHT_PURPLE + "Dragon Regenerator");
        enderman.setCustomNameVisible(true);
        enderman.setPersistent(true);
        enderman.setRemoveWhenFarAway(false);
        Utils.updateEntityAttributeInstance(enderman, Attribute.MAX_HEALTH, REGENERATOR_HEALTH);
        enderman.setHealth(REGENERATOR_HEALTH);
        Utils.updateEntityAttributeInstance(enderman, Attribute.ATTACK_DAMAGE, REGENERATOR_DAMAGE);
        Utils.updateEntityAttributeInstance(enderman, Attribute.SCALE, REGENERATOR_SCALE);
        findNearbyTarget(enderman, 150).ifPresent((Player player) -> enderman.setTarget(player));
        enderman.getWorld().spawnParticle(Particle.PORTAL, enderman.getLocation().clone().add(0, 1, 0), 30,
                1, 1, 1, 0.1);
        regenerator = enderman;
        regeneratorHealCounter = 0;
    }

    private void updateRegenerator() {
        if (!hasLivingRegenerator()) {
            regenerator = null;
            regeneratorHealCounter = 0;
            return;
        }
        if (!regenerator.getWorld().equals(dragon.getWorld())) return;

        regeneratorHealCounter++;
        if (regeneratorHealCounter < REGENERATOR_HEAL_INTERVAL_TICKS) return;
        regeneratorHealCounter = 0;
        dragon.setHealth(calculateRegeneratedHealth(dragon.getHealth(), getEntityMaxHealth()));
        dragon.getWorld().spawnParticle(Particle.HEART, dragon.getLocation(), 12, 2, 2, 2, 0.1);
        dragon.getWorld().playSound(dragon.getLocation(), Sound.ENTITY_GENERIC_DRINK, 2, 1);
    }

    private boolean hasLivingRegenerator() {
        return regenerator != null && regenerator.isValid() && !regenerator.isDead();
    }

    private void cleanupEncounterState() {
        explosiveEndermites.stream()
                .filter((Endermite endermite) -> endermite.isValid())
                .forEach((Endermite endermite) -> endermite.remove());
        explosiveEndermites.clear();
        if (regenerator != null && regenerator.isValid()) regenerator.remove();
        regenerator = null;
        regeneratorHealCounter = 0;
        stunnedPlayers.forEach((Player player, StunState state) -> {
            if (player.isOnline()) player.setWalkSpeed(state.walkSpeed());
        });
        stunnedPlayers.clear();
        suppressedFlightStates.forEach((Player player, FlightState state) -> restoreFlightState(player, state));
        suppressedFlightStates.clear();
        clearGroundSlam();
    }

    static boolean isGroundSlamRoll(double roll) {
        return roll < GROUND_SLAM_CHANCE;
    }

    static boolean shouldSpawnRegenerator(double roll) {
        return roll < REGENERATOR_SPAWN_CHANCE;
    }

    static double calculateRegeneratedHealth(double currentHealth, double maximumHealth) {
        return Math.min(maximumHealth, currentHealth + maximumHealth * REGENERATOR_HEAL_PERCENTAGE);
    }

    static boolean isWithinRadius(Location first, Location second, double radius) {
        if (!java.util.Objects.equals(first.getWorld(), second.getWorld())) return false;
        double x = first.getX() - second.getX();
        double y = first.getY() - second.getY();
        double z = first.getZ() - second.getZ();
        return x * x + y * y + z * z <= radius * radius;
    }

    static void addUniquePlayers(List<Player> currentPlayers, List<Player> newPlayers) {
        newPlayers.stream()
                .filter((Player player) -> !currentPlayers.contains(player))
                .forEach((Player player) -> currentPlayers.add(player));
    }

    @Override
    public void deathAnimation() {
        cleanupEncounterState();
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

    @Override
    public void cleanup() {
        cleanupEncounterState();
        super.cleanup();
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
        initializePlayers(players);
    }

    public void initializePlayers(List<Player> players) {
        addUniquePlayers(this.players, players);
    }

    public void addPlayer(Player p) {
        if (!players.contains(p)) players.add(p);
    }

    public List<Player> getPlayers() {
        return players;
    }

    public boolean isRespawn() {
        return isRespawn;
    }

    public boolean isFlightSuppressed(Player player) {
        return dragonsWrath && players.contains(player) && player.getWorld().equals(dragon.getWorld());
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
