package sir_draco.survivalskills.bosses;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;

public abstract class Boss extends BukkitRunnable {
    private final String name;
    private final int spawnRadiusRequired;
    private final int spawnHeightRequired;
    private final int maxStage;
    private final double maxHealth;
    private final double damage;
    private final double defense;

    private final int INVINCIBILITY_TICKS = 100;

    private double speed;
    protected LivingEntity boss;
    private BossBarManager bossBarManager;
    private boolean disableAttack = false;
    private boolean appliedAttributes = false;
    private int stage = 1;
    private int targetRangeXZ = 50;
    private int targetRangeY = 50;

    public Boss(String name, int spawnRadiusRequired, int spawnHeightRequired, double maxHealth, double damage,
            double defense, double speed, int maxStage) {
        this.name = name;
        this.spawnRadiusRequired = spawnRadiusRequired;
        this.spawnHeightRequired = spawnHeightRequired;
        this.maxHealth = maxHealth;
        this.damage = damage;
        this.defense = defense;
        this.speed = speed;
        this.maxStage = maxStage;
    }

    @Override
    public abstract void run();

    public abstract void attack();


    /**
     * Checks what stage the boss should be in.
     * If the boss is not in the proper stage, the boss will change stage and play a cry sound
     * @param invincibilityPhase {@link Boolean} - should the boss have invincibility frames between phases
     * @param crySound {@link Sound} - the sound to play when the boss changes stage
     */
    public void checkStage(boolean invincibilityPhase, Sound crySound) {
        // The percentage of health missing divided by the increment for each stage
        if (!appliedAttributes) return;

        int properStage = (int) Math.ceil((1 - getHealthPercentage()) * maxStage);
        if (stage >= properStage) return;
        stage = properStage;

        playStageSound(crySound);

        if (invincibilityPhase) startInvincibilityPhase();
    }


    /**
     * Toggles the AI and disables the attacks of the boss
     */
    public void toggleAI() {
        boss.setAI(!boss.hasAI());
        setDisableAttack(!disableAttack);
    }


    /**
     * 
     * @param type {@link EntityType} - the type of entity to spawn
     * @param loc {@link Location} - the location to spawn the boss
     * @return boolean - whether the boss was successfully spawned
     */
    public boolean spawnBoss(EntityType type, Location loc) {
        World world = loc.getWorld();
        if (world == null) return false;

        if (!ensureSpawnSpace(loc)) return false;

        try {
            boss = (LivingEntity) world.spawnEntity(loc, type);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "Exception while spawning boss: " + name);
            return false;
        }

        bossBarManager = new BossBarManager(boss, name, maxHealth);
        new BukkitRunnable() {
            @Override
            public void run() {
                applyAttributes();
                bossBarManager.create();
                bossBarManager.updatePlayers();
            }
        }.runTaskLater(SurvivalSkills.getPlugin(SurvivalSkills.class), 1);
        return true;
    }


    /**
     * Sets the entity to the boss instance and instantiates the boss bar
     * @param entity {@link LivingEntity} - the entity to attach the boss to
     */
    public void attachToEntity(LivingEntity entity) {
        this.boss = entity;
        this.bossBarManager = new BossBarManager(boss, name, maxHealth);
    }


    /**
     * Applies the basic attributes such as health and speed
     */
    public void applyAttributes() {
        boss.setMetadata("boss", new FixedMetadataValue(SurvivalSkills.getInstance(), true));
        boss.setCustomName(name);
        boss.setCustomNameVisible(true);
        boss.setRemoveWhenFarAway(false);
        boss.setCanPickupItems(false);
        boss.setAI(true);
        boss.setCollidable(true);
        boss.setGravity(true);
        boss.setGlowing(true);
        boss.setPersistent(true);

        AttributeInstance attack = boss.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attack != null) attack.setBaseValue(damage);
        AttributeInstance armor = boss.getAttribute(Attribute.ARMOR);
        if (armor != null) armor.setBaseValue(defense);
        AttributeInstance speedAttribute = boss.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttribute != null) speedAttribute.setBaseValue(this.speed);
        AttributeInstance knockbackResistance = boss.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (knockbackResistance != null) knockbackResistance.setBaseValue(1);
        AttributeInstance health = boss.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) health.setBaseValue(maxHealth);
        boss.setHealth(maxHealth);
        appliedAttributes = true;
    }


    /**
     * Ensures that the boss can be spawned at the given location
     * @param spawnLocation {@link Location} - the location to spawn the boss
     * @return boolean - whether the boss was successfully spawned
     */
    public boolean ensureSpawnSpace(Location spawnLocation) {
        for (int x = -spawnRadiusRequired; x <= spawnRadiusRequired; x++) {
            for (int y = 0; y <= spawnHeightRequired; y++) {
                for (int z = -spawnRadiusRequired; z <= spawnRadiusRequired; z++) {
                    Location loc = spawnLocation.clone().add(x, y, z);
                    if (!loc.getBlock().isPassable()) return false;
                }
            }
        }
        return true;
    }


    /**
     * Finds all valid spawn points within the given radius
     * @param count int - the number of valid spawn points to find
     * @return ArrayList<Location> - the valid spawn points
     */
    public ArrayList<Location> findNearbyValidSpawnPoints(int count) {
        ArrayList<Location> validSpawnPoints = new ArrayList<>();
        HashSet<Block> usedBlocks = new HashSet<>();
        Location bossLoc = getBoss().getLocation();
        Location originLoc = bossLoc.getBlock().getLocation();

        searchLoop:
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                for (int y = 0; y <= 5; y++) {
                    if (validSpawnPoints.size() >= count) break searchLoop;

                    Location blockLoc = originLoc.clone().add(x, y, z);
                    Block block = blockLoc.getBlock();

                    if (usedBlocks.contains(block)) continue;
                    if (blockLoc.distance(bossLoc) > 5) continue;
                    if (block.getType() == Material.AIR) continue;
                    if (blockLoc.clone().add(0, 1, 0).getBlock().getType() != Material.AIR) continue;
                    if (blockLoc.clone().add(0, 2, 0).getBlock().getType() != Material.AIR) continue;

                    usedBlocks.add(block);
                    validSpawnPoints.add(blockLoc.clone().add(0.5, 1, 0.5));
                }
            }
        }
        return validSpawnPoints;
    }


    /**
     * Triggers the death animation and removes the boss
     */
    public void death() {
        deathAnimation();
        boss.getWorld().playSound(boss.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
        cleanup();
    }


    /**
     * Cleans up the boss entity and runnable
     */
    public void cleanup() {
        if (bossBarManager != null) bossBarManager.removeAll();
        if (boss == null) {
            cancel();
            return;
        }
        if (!boss.isDead()) boss.remove();
        cancel();
    }

    public void deathAnimation() {
        boss.getWorld().createExplosion(boss.getLocation(), 4);
    }

    public void manageBossBarPlayers() {
        if (bossBarManager != null) bossBarManager.updatePlayers();
    }

    public void updateBossBar() {
        if (bossBarManager != null) bossBarManager.update();
    }

    public void setNearestPlayerAsTarget() {
        Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;
        LivingEntity b = getBoss();

        for (Entity entity : b.getNearbyEntities(targetRangeXZ, targetRangeY, targetRangeXZ)) {
            if (!(entity instanceof Player player)) continue;

            double distance = entity.getLocation().distance(b.getLocation());
            if (distance < nearestDistance) {
                nearestPlayer = player;
                nearestDistance = distance;
            }
        }

        if (nearestPlayer != null) ((Mob) b).setTarget(nearestPlayer);
    }

    public String getName() {
        return name;
    }

    public double getMaxHealth() {
        return maxHealth;
    }

    public void setHealth(double health) {
        boss.setHealth(health);
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public void setTargetRange(int xz, int y) {
        this.targetRangeXZ = xz;
        this.targetRangeY = y;
    }

    public double getHealthPercentage() {
        return boss.getHealth() / maxHealth;
    }

    public void setHealthPercentage(double healthPercentage) {
        if (healthPercentage > 100) healthPercentage = 100;
        if (healthPercentage <= 0) {
            throw new IllegalArgumentException("[Survival Skills] Can not set bosses health to 0 or below. Use commands to kill the boss directly.");
        }
        boss.setHealth((healthPercentage / 100d) * maxHealth);
    }

    public LivingEntity getBoss() {
        return boss;
    }

    public void setDisableAttack(boolean disableAttack) {
        this.disableAttack = disableAttack;
    }

    public boolean isDisableAttack() {
        return disableAttack;
    }

    public int getStage() {
        return stage;
    }

    public void setAppliedAttributes(boolean appliedAttributes) {
        this.appliedAttributes = appliedAttributes;
    }


    private void startInvincibilityPhase() {
        boss.setInvulnerable(true);
        // Give 5 seconds of invincibility if changing stages for the first time
        new BukkitRunnable() {
            private int count = 0;

            @Override
            public void run() {
                if (count == INVINCIBILITY_TICKS) {
                    boss.setInvulnerable(false);
                    this.cancel();
                    return;
                }
                count++;
                boss.setInvulnerable(true);

                for (int i = 0; i < 10; i++) {
                    double xOffset = (Math.random() - 0.5) * 4;
                    double yOffset = (Math.random() - 0.5) * 4;
                    double zOffset = (Math.random() - 0.5) * 4;
                    Location loc = boss.getLocation().clone().add(xOffset, yOffset, zOffset);
                    boss.getWorld().spawnParticle(Particle.WHITE_SMOKE, loc, 1, Math.random() - 0.5,
                            Math.random() - 0.5, Math.random() - 0.5);
                }
            }
        }.runTaskTimerAsynchronously(SurvivalSkills.getPlugin(SurvivalSkills.class), 0, 1);
    }


    private void playStageSound(Sound crySound) {
        if (crySound == null) return;
        boss.getWorld().playSound(boss.getLocation(), crySound, 1, 1);
        // Right now only the ender dragon should have a sound
        if (!crySound.equals(Sound.ENTITY_ENDER_DRAGON_GROWL)) return;
        // On every stage except number 2
        if (stage != 2) return;
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Ender Dragon: "
            + ChatColor.RESET + "Time to step it up a notch!");
    }
}
