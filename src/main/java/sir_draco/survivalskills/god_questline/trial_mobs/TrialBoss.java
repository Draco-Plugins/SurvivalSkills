package sir_draco.survivalskills.god_questline.trial_mobs;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public abstract class TrialBoss extends BukkitRunnable {

    private static final double BOSS_BAR_RADIUS = 50;
    private static final double BOSS_ATTACK_RANGE = 10;
    private static final double BOSS_BAR_RED_THRESHOLD = 0.33;
    private static final double BOSS_BAR_YELLOW_THRESHOLD = 0.66;
    private static final int ATTRIBUTE_APPLY_DELAY_TICKS = 1;
    private static final double KNOCKBACK_RESISTANCE_VALUE = 1.0;
    private static final int GHAST_VERTICAL_OFFSET_Y = 2;
    private static final int SPAWN_Y_OFFSET = 1;
    private static final float DEATH_SOUND_VOLUME = 1.0f;
    private static final float DEATH_SOUND_PITCH = 1.0f;

    private final String id;
    private final String name;
    private final double maxHealth;
    private final double damage;
    private final double defense;
    private final double speed;
    private final double scale;
    private final EntityType type;
    private final Map<ItemStack, Double> drops;
    private final List<Entity> summons = new ArrayList<>();

    private LivingEntity boss;
    private BossBar bossBar;
    private List<Location> spawnLocations = null;

    protected TrialBoss(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.maxHealth = Math.ceil(builder.maxHealth);
        this.damage = Math.ceil(builder.damage);
        this.defense = builder.defense;
        this.speed = builder.speed;
        this.scale = builder.scale;
        this.type = builder.type;
        this.drops = builder.drops;
    }

    public static class Builder {
        private String id;
        private String name;
        private double maxHealth;
        private double damage;
        private double defense;
        private double speed;
        private double scale;
        private EntityType type;
        private Map<ItemStack, Double> drops;

        public Builder id(String id) { this.id = id; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder maxHealth(double maxHealth) { this.maxHealth = maxHealth; return this; }
        public Builder damage(double damage) { this.damage = damage; return this; }
        public Builder defense(double defense) { this.defense = defense; return this; }
        public Builder speed(double speed) { this.speed = speed; return this; }
        public Builder scale(double scale) { this.scale = scale; return this; }
        public Builder type(EntityType type) { this.type = type; return this; }
        public Builder drops(Map<ItemStack, Double> drops) { this.drops = drops; return this; }
    }

    @Override
    public final void run() {
        if (boss == null || boss.isDead()) {
            if (boss != null) boss.remove();
            cancel();
            return;
        }
        updateBossBar();
        manageBossBarPlayers();
        onTick();
    }

    protected void onTick() {}

    public boolean isSpawned() {
        return boss != null && !boss.isDead();
    }

    public boolean spawn(Location loc) {
        World world = loc.getWorld();
        if (world == null) {
            cancel();
            return false;
        }

        try {
            Location spawnLocation = getValidSpawn(loc);
            if (type.equals(EntityType.GHAST)) spawnLocation.add(0, GHAST_VERTICAL_OFFSET_Y, 0);
            boss = (LivingEntity) world.spawnEntity(spawnLocation, type);
            boss.setInvulnerable(true);
        } catch (Exception e) {
            Bukkit.getLogger().warning(String.format("[SurvivalSkills] Exception while spawning boss: %s", name));
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
                if (boss != null) boss.setInvulnerable(false);
            }
        }.runTaskLater(SurvivalSkills.getPlugin(SurvivalSkills.class), ATTRIBUTE_APPLY_DELAY_TICKS);
        return true;
    }

    public Location getValidSpawn(Location loc) {
        Location newloc = loc.clone().add(0, SPAWN_Y_OFFSET, 0);
        World world = loc.getWorld();
        if (world == null) return newloc;

        while (newloc.getY() < world.getMaxHeight()) {
            if (newloc.getBlock().getType() == Material.AIR
                    && newloc.clone().add(0, 1, 0).getBlock().getType() == Material.AIR) {
                return newloc;
            }
            newloc.add(0, 1, 0);
        }
        return loc;
    }

    public void handleTypeSpecificSpawn() {}

    public void dropItems() {
        if (!isSpawned() || drops == null) return;
        for (Map.Entry<ItemStack, Double> entry : drops.entrySet()) {
            if (Math.random() < entry.getValue()) {
                boss.getWorld().dropItemNaturally(boss.getLocation(), entry.getKey());
            }
        }
    }

    public void applyAttributes() {
        if (!isSpawned()) return;
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
        if (knockbackResistance != null) knockbackResistance.setBaseValue(KNOCKBACK_RESISTANCE_VALUE);
        AttributeInstance health = boss.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) health.setBaseValue(maxHealth);
        AttributeInstance scale = boss.getAttribute(Attribute.SCALE);
        if (scale != null) scale.setBaseValue(this.scale);
        boss.setHealth(maxHealth);
    }

    public void scaleHealth(int multiplier) {
        if (!isSpawned()) return;
        AttributeInstance health = boss.getAttribute(Attribute.MAX_HEALTH);
        if (health != null) health.setBaseValue(maxHealth * multiplier);
        boss.setHealth(maxHealth * multiplier);
    }

    public abstract void startScript();

    public abstract TrialBoss duplicate();

    public abstract TrialBoss duplicate(double healthMultiplier, double damageMultiplier);

    public void death() {
        if (!isSpawned()) return;
        dropItems();
        if (bossBar != null) bossBar.removeAll();
        NamespacedKey key = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class), "boss" + boss.getUniqueId());
        if (Bukkit.getBossBar(key) != null) Bukkit.removeBossBar(key);
        boss.getWorld().playSound(boss.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, DEATH_SOUND_VOLUME, DEATH_SOUND_PITCH);
        if (!boss.isDead()) boss.remove();
        for (Entity e : summons) {
            if (!e.isDead()) e.remove();
        }
        cancel();
    }

    // Method for giant health bar
    public void healthBar() {
        if (!isSpawned()) return;
        NamespacedKey key = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class), "boss" + boss.getUniqueId());
        bossBar = Bukkit.createBossBar(key, name, BarColor.WHITE, BarStyle.SOLID);
        bossBar.setProgress(1.0);
        addNearbyPlayersToBossBar();
    }

    public void manageBossBarPlayers() {
        addNearbyPlayersToBossBar();
        checkCurrentBossBarPlayers();
    }

    public void addNearbyPlayersToBossBar() {
        if (bossBar == null || !isSpawned()) return;
        for (Entity e : boss.getNearbyEntities(BOSS_BAR_RADIUS, BOSS_BAR_RADIUS, BOSS_BAR_RADIUS)) {
            if (!(e instanceof Player p)) continue;
            if (p.getLocation().distance(boss.getLocation()) > BOSS_BAR_RADIUS) continue;
            if (bossBar.getPlayers().contains(p)) continue;
            bossBar.addPlayer(p);
        }
    }

    public void checkCurrentBossBarPlayers() {
        if (bossBar == null || !isSpawned()) return;
        List<Player> removePlayers = new ArrayList<>();
        for (Player p : bossBar.getPlayers()) {
            if (!p.isOnline() || p.getLocation().distance(boss.getLocation()) > BOSS_BAR_RADIUS) {
                removePlayers.add(p);
            }
        }
        for (Player p : removePlayers) bossBar.removePlayer(p);
    }

    public void updateBossBar() {
        if (bossBar == null || !isSpawned()) return;
        bossBar.setProgress(getHealthPercentage());
        if (getHealthPercentage() < BOSS_BAR_RED_THRESHOLD) bossBar.setColor(BarColor.RED);
        else if (getHealthPercentage() < BOSS_BAR_YELLOW_THRESHOLD) bossBar.setColor(BarColor.YELLOW);
        else bossBar.setColor(BarColor.WHITE);
    }

    public void attack() {
        if (!isSpawned()) return;
        Entity target = null;
        for (Entity e : boss.getNearbyEntities(BOSS_ATTACK_RANGE, BOSS_ATTACK_RANGE, BOSS_ATTACK_RANGE)) {
            if (e instanceof Player) {
                target = e;
                break;
            }
        }
        if (target == null) return;
        boss.attack(target);
    }

    public double getHealthPercentage() {
        if (!isSpawned()) return 0;
        return boss.getHealth() / maxHealth;
    }

    public LivingEntity getBoss() {
        return boss;
    }

    public void setTarget(Player p) {
        if (!isSpawned()) return;
        if (boss instanceof Mob) ((Mob) boss).setTarget(p);
    }

    @SuppressWarnings("unchecked")
    protected <T extends LivingEntity> void spawnMinions(EntityType type, String name, int count, Consumer<T> customizer) {
        if (getSpawnLocations() == null) return;

        List<Location> locations = getSpawnLocations();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < count; i++) {
            Location loc = locations.get(random.nextInt(locations.size()));
            if (loc.getWorld() == null) continue;

            T entity = (T) loc.getWorld().spawnEntity(loc, type);
            entity.setMetadata("trialmob", new FixedMetadataValue(SurvivalSkills.getInstance(), true));
            entity.setMetadata("spawned", new FixedMetadataValue(SurvivalSkills.getInstance(), true));
            entity.setCustomName(name);
            entity.setCustomNameVisible(true);
            addSummon(entity);

            if (customizer != null) customizer.accept(entity);
        }
    }

    public void addSummon(Entity entity) {
        summons.add(entity);
    }

    public List<Entity> getSummons() {
        return Collections.unmodifiableList(summons);
    }

    public Map<ItemStack, Double> getDrops() {
        return drops == null ? null : Collections.unmodifiableMap(drops);
    }

    public void setSpawnLocations(List<Location> spawnLocations) {
        this.spawnLocations = spawnLocations;
    }

    public List<Location> getSpawnLocations() {
        return spawnLocations;
    }

    public String getId() {
        return id;
    }
}
