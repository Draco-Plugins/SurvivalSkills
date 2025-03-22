package sir_draco.survivalskills.GodQuestline.TrialMobs;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;
import java.util.HashMap;

public class TrialBoss extends BukkitRunnable{

    private final String name;
    private final double maxHealth;
    private final double damage;
    private final double defense;
    private final double speed;
    private final double scale;
    private final EntityType type;
    private final HashMap<ItemStack, Double> drops;
    private final ArrayList<Entity> summons = new ArrayList<>();

    private LivingEntity boss;
    private BossBar bossBar;
    private ArrayList<Location> spawnLocations = null;

    public TrialBoss(String name, double maxHealth, double damage, double defense, double speed, double scale,
                     EntityType type, HashMap<ItemStack, Double> drops) {
        this.name = name;
        this.maxHealth = Math.ceil(maxHealth);
        this.damage = Math.ceil(damage);
        this.defense = defense;
        this.speed = speed;
        this.scale = scale;
        this.type = type;
        this.drops = drops;
    }

    @Override
    public void run() {
        if (!boss.isDead()) cancel();
    }

    public boolean spawn(Location loc) {
        World world = loc.getWorld();
        if (world == null) {
            cancel();
            return false;
        }

        try {
            Location spawnLocation = getValidSpawn(loc);
            if (type.equals(EntityType.GHAST)) spawnLocation.add(0, 2, 0);
            boss = (LivingEntity) world.spawnEntity(spawnLocation, type);
        } catch (Exception e) {
            Bukkit.getLogger().warning("Exception while spawning boss: " + name);
            cancel();
            return false;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                applyAttributes();
                healthBar();
                manageBossBarPlayers();
                handleTypeSpecificSpawn();
            }
        }.runTaskLater(SurvivalSkills.getPlugin(SurvivalSkills.class), 1);
        return true;
    }

    public Location getValidSpawn(Location loc) {
        Location newloc = loc.clone().add(0, 1, 0);
        if (loc.getBlock().getType() == Material.AIR && loc.clone().add(0, 1, 0).getBlock().getType() == Material.AIR) return newloc;
        return getValidSpawn(newloc);
    }

    public void handleTypeSpecificSpawn() {}

    public void dropItems() {
        if (drops == null) return;
        for (ItemStack item : drops.keySet()) {
            double chance = drops.get(item);
            if (Math.random() < chance) boss.getWorld().dropItemNaturally(boss.getLocation(), item);
        }
    }

    public void applyAttributes() {
        boss.setMetadata("trialboss", new FixedMetadataValue(SurvivalSkills.getInstance(), true));
        boss.setCustomName(name);
        boss.setCustomNameVisible(true);
        boss.setRemoveWhenFarAway(true);
        boss.setCanPickupItems(false);
        boss.setAI(true);
        boss.setCollidable(true);
        boss.setGravity(true);
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
        AttributeInstance scale = boss.getAttribute(Attribute.SCALE);
        if (scale != null) scale.setBaseValue(this.scale);
        boss.setHealth(maxHealth);
    }

    public void scaleHealth(int scale) {
        AttributeInstance health = boss.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) health.setBaseValue(maxHealth * scale);
        boss.setHealth(maxHealth * scale);
    }

    public void death() {
        dropItems();
        if (bossBar != null) bossBar.removeAll();
        NamespacedKey key = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class), "boss" + boss.getUniqueId());
        if (Bukkit.getBossBar(key) != null) Bukkit.removeBossBar(key);
        boss.getWorld().playSound(boss.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
        if (!boss.isDead()) boss.remove();
        if (!summons.isEmpty()) {
            for (Entity e : summons) {
                if (e.isDead()) continue;
                e.remove();
            }
        }
        cancel();
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
            if (!(e instanceof Player p)) continue;
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

    public double getHealthPercentage() {
        return boss.getHealth() / maxHealth;
    }

    public LivingEntity getBoss() {
        return boss;
    }

    public void setTarget(Player p) {
        if (boss instanceof Mob) ((Mob) boss).setTarget(p);
    }

    public ArrayList<Entity> getSummons() {
        return summons;
    }

    public HashMap<ItemStack, Double> getDrops() {
        return drops;
    }

    public void setSpawnLocations(ArrayList<Location> spawnLocations) {
        this.spawnLocations = spawnLocations;
    }

    public ArrayList<Location> getSpawnLocations() {
        return spawnLocations;
    }

    public TrialBoss duplicate() {
        return new TrialBoss(name, maxHealth, damage, defense, speed, scale, type, drops);
    }

    public TrialBoss duplicate(double healthMultiplier, double damageMultiplier) {
        return new TrialBoss(name, maxHealth * healthMultiplier, damage * damageMultiplier, defense, speed, scale, type, drops);
    }
}
