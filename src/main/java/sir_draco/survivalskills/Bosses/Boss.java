package sir_draco.survivalskills.Bosses;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;

public class Boss extends BukkitRunnable {
    private final String name;
    private final int spawnRadiusRequired;
    private final int spawnHeightRequired;
    private final int maxStage;
    private final boolean spawnSuccess;
    private final double maxHealth;
    private final double damage;
    private final double defense;

    private double speed;
    private LivingEntity boss;
    private BossBar bossBar;
    private boolean disableAttack = false;
    private boolean appliedAttributes = false;
    private int stage = 1;

    public Boss(String name, int spawnRadiusRequired, int spawnHeightRequired, double maxHealth, double damage, double defense, double speed, EntityType type, Location loc, int maxStage) {
        this.name = name;
        this.spawnRadiusRequired = spawnRadiusRequired;
        this.spawnHeightRequired = spawnHeightRequired;
        this.maxHealth = maxHealth;
        this.damage = damage;
        this.defense = defense;
        this.speed = speed;
        this.maxStage = maxStage;

        if (type.equals(EntityType.ENDER_DRAGON)) {
            spawnSuccess = true;
            return;
        }
        spawnSuccess = spawn(type, loc);
        if (spawnSuccess) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    applyAttributes();
                    healthBar();
                    manageBossBarPlayers();
                }
            }.runTaskLater(SurvivalSkills.getPlugin(SurvivalSkills.class), 1);
        }
    }

    @Override
    public void run() {
        if (!spawnSuccess || boss.isDead()) cancel();
        if (getHealthPercentage() != 1) checkStage(false, null);
    }

    public void checkStage(boolean invincibilityFrames, Sound crySound) {
        // The percentage of health missing divided by the increment for each stage
        if (!appliedAttributes) return;
        int properStage = (int) Math.ceil((1 - getHealthPercentage()) / (1d / maxStage));
        if (stage >= properStage) return;
        if (crySound != null) {
            boss.getWorld().playSound(boss.getLocation(), crySound, 1, 1);
            if (crySound.equals(Sound.ENTITY_ENDER_DRAGON_GROWL)) {
                stage = properStage;
                if (stage == 2) {
                    Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Ender Dragon: "
                            + ChatColor.RESET + "Time to step it up a notch!");
                }
            }
        }
        if (invincibilityFrames) {
            boss.setInvulnerable(true);
            stage = properStage;
            // Give 5 seconds of invincibility if changing stages for the first time
            new BukkitRunnable() {
                private int count = 0;
                @Override
                public void run() {
                    if (count == 100) {
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
                        boss.getWorld().spawnParticle(Particle.WHITE_SMOKE, loc, 1, Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5);
                    }
                }
            }.runTaskTimerAsynchronously(SurvivalSkills.getPlugin(SurvivalSkills.class), 0, 1);
        }
    }

    public void toggleAI() {
        boss.setAI(!boss.hasAI());
        setDisableAttack(!disableAttack);
    }

    public boolean spawn(EntityType type, Location loc) {
        World world = loc.getWorld();
        if (world == null) return false;

        boolean adequateSpawnSpace = ensureSpawnSpace(loc);
        if (!adequateSpawnSpace) return false;

        try {
            boss = (LivingEntity) world.spawnEntity(loc, type);
        } catch (Exception e) {
            Bukkit.getLogger().warning("Exception while spawning boss: " + name);
            return false;
        }
        return true;
    }

    public void applyAttributes() {
        boss.setMetadata("boss", new FixedMetadataValue(SurvivalSkills.getPlugin(SurvivalSkills.class), true));
        boss.setCustomName(name);
        boss.setCustomNameVisible(true);
        boss.setRemoveWhenFarAway(false);
        boss.setCanPickupItems(false);
        boss.setAI(true);
        boss.setCollidable(true);
        boss.setGravity(true);
        boss.setGlowing(true);
        boss.setPersistent(true);

        AttributeInstance attack = boss.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (attack != null) attack.setBaseValue(damage);
        AttributeInstance armor = boss.getAttribute(Attribute.GENERIC_ARMOR);
        if (armor != null) armor.setBaseValue(defense);
        AttributeInstance speedAttribute = boss.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttribute != null) speedAttribute.setBaseValue(this.speed);
        AttributeInstance knockbackResistance = boss.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE);
        if (knockbackResistance != null) knockbackResistance.setBaseValue(1);
        AttributeInstance health = boss.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (health != null) health.setBaseValue(maxHealth);
        boss.setHealth(maxHealth);
        appliedAttributes = true;
    }

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

    public void despawnBoss() {
        if (bossBar != null) bossBar.removeAll();
        NamespacedKey key = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class), "boss" + boss.getUniqueId());
        if (Bukkit.getBossBar(key) != null) Bukkit.removeBossBar(key);
        if (boss != null) boss.remove();
        cancel();
    }

    public void death() {
        deathAnimation();
        if (bossBar != null) bossBar.removeAll();
        NamespacedKey key = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class), "boss" + boss.getUniqueId());
        if (Bukkit.getBossBar(key) != null) Bukkit.removeBossBar(key);
        boss.getWorld().playSound(boss.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
        if (!boss.isDead()) boss.remove();
        cancel();
    }

    public void deathAnimation() {
        boss.getWorld().createExplosion(boss.getLocation(), 4);
    }

    // Method for giant health bar
    public void healthBar() {
        Bukkit.getServer().getBossBars().forEachRemaining(bar -> {
            for (Player p : Bukkit.getOnlinePlayers()) bar.removePlayer(p);
        });
        NamespacedKey key = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class), "boss" + boss.getUniqueId());
        bossBar = Bukkit.createBossBar(key, name, BarColor.WHITE, BarStyle.SOLID);
        addNearbyPlayersToBossBar();
    }

    public void manageBossBarPlayers() {
        addNearbyPlayersToBossBar();
        checkCurrentBossBarPlayers();
    }

    public void addNearbyPlayersToBossBar() {
        if (bossBar == null) return;
        for (Entity e : boss.getNearbyEntities(50, 50, 50)) {
            if (!(e instanceof Player)) continue;
            Player p = (Player) e;
            if (p.getLocation().distance(boss.getLocation()) > 50) continue;
            if (bossBar.getPlayers().contains(p)) continue;
            bossBar.addPlayer(p);
        }
    }

    public void checkCurrentBossBarPlayers() {
        if (bossBar == null) return;
        ArrayList<Player> removePlayers = new ArrayList<>();
        for (Player p : bossBar.getPlayers()) {
            if (p.isOnline()) continue;
            if (p.getLocation().distance(boss.getLocation()) <= 50) continue;
            removePlayers.add(p);
        }
        if (removePlayers.isEmpty()) return;
        for (Player p : removePlayers) bossBar.removePlayer(p);
    }

    public void updateBossBar() {
        if (bossBar == null) return;
        bossBar.setProgress(getHealthPercentage());
        if (getHealthPercentage() < 0.33) bossBar.setColor(BarColor.RED);
        else if (getHealthPercentage() < 0.66) bossBar.setColor(BarColor.YELLOW);
        else bossBar.setColor(BarColor.WHITE);
    }

    public void attack() {
        Bukkit.getLogger().info("Default Attack");
        Entity target = null;
        for (Entity e : boss.getNearbyEntities(10, 10, 10)) {
            if (e instanceof Player) {
                target = e;
                break;
            }
        }
        if (target == null) return;
        boss.attack(target);
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

    public double getHealthPercentage() {
        return boss.getHealth() / maxHealth;
    }

    public void setHealthPercentage(double healthPercentage) {
        if (healthPercentage > 100) healthPercentage = 100;
        if (healthPercentage <= 0) healthPercentage = 1;
        boss.setHealth((healthPercentage / 100d) * maxHealth);
    }

    public LivingEntity getBoss() {
        return boss;
    }

    public boolean isSpawnSuccess() {
        return spawnSuccess;
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

    public void setBoss(LivingEntity boss) {
        this.boss = boss;
    }

    public void setAppliedAttributes(boolean appliedAttributes) {
        this.appliedAttributes = appliedAttributes;
    }
}
