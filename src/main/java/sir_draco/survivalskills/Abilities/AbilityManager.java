package sir_draco.survivalskills.Abilities;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import sir_draco.survivalskills.Rewards.Reward;
import sir_draco.survivalskills.SurvivalSkills;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AbilityManager {

    private static final ArrayList<EntityType> domainMobs = new ArrayList<>();

    private final SurvivalSkills plugin;
    private final HashMap<Player, ArrayList<AbilityTimer>> timerTracker = new HashMap<>();
    private final HashMap<Player, TrailEffect> trailTracker = new HashMap<>();
    private final HashMap<String, Particle> trails = new HashMap<>();
    private final HashMap<Player, BloodyDomain> bloodyDomainTracker = new HashMap<>();
    private final ArrayList<Entity> mobsScanned = new ArrayList<>();

    public AbilityManager(SurvivalSkills plugin) {
        this.plugin = plugin;
        createTrails();
        createDomainMobs();
    }

    public Inventory loadToolBelt(Player p) {
        FileConfiguration data = plugin.getToolBeltData();
        if (!data.contains(p.getUniqueId().toString())) return null;

        Inventory toolBelt = Bukkit.createInventory(p, 9, "Tool Belt");
        ConfigurationSection section = data.getConfigurationSection(p.getUniqueId().toString());
        if (section == null) return null;
        for (String key : section.getKeys(false)) {
            ItemStack item = section.getItemStack(key);
            if (item == null) continue;
            toolBelt.addItem(item);
        }
        return toolBelt;
    }

    public void loadFlight(Player p, FileConfiguration data) {
        if (!data.contains(p.getUniqueId() + ".Flight")) return;

        int activeTime = data.getInt(p.getUniqueId() + ".Flight.ActiveTime");
        int cooldownTime = data.getInt(p.getUniqueId() + ".Flight.CooldownTime");
        float speed = (float) data.getDouble(p.getUniqueId() + ".Flight.Speed");

        AbilityTimer timer = new AbilityTimer(plugin, "Flight", p, activeTime, cooldownTime);
        timer.runTaskTimerAsynchronously(plugin, 0, 20);
        timer.setFlightSpeed(speed);
        addAbility(p, timer);

        if (activeTime <= 0) return;
        p.setAllowFlight(true);
        p.setFlying(true);
        p.setFlySpeed(speed);
        FlyingTimer flyingTimer = new FlyingTimer(p, activeTime);
        flyingTimer.runTaskTimerAsynchronously(plugin, 0, 20);
        int minutes = activeTime / 60;
        int seconds = activeTime % 60;
        p.sendRawMessage(ChatColor.GREEN + "Your flight will end in " + ChatColor.AQUA + minutes +
                ChatColor.GREEN + " minutes " + ChatColor.AQUA + seconds + ChatColor.GREEN + " seconds");
    }

    public void saveToolBelt(Player p, Inventory inv) {
        if (!inv.getViewers().isEmpty()) return;

        FileConfiguration data = plugin.getToolBeltData();
        int slot = 0;
        data.set(p.getUniqueId().toString(), null);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) continue;
            data.set(p.getUniqueId() + "." + slot, inv.getItem(i));
            slot++;
        }
    }

    public void saveToolBeltFile(File file, FileConfiguration data) {
        try {
            data.save(file);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save Tool Belts", e);
        }
    }

    public void saveToolBelts() {
        File file = plugin.getToolBeltFile();
        FileConfiguration data = plugin.getToolBeltData();
        for (Map.Entry<Player, Inventory> toolBelt : plugin.getMiningListener().getToolBelts().entrySet())
            saveToolBelt(toolBelt.getKey(), toolBelt.getValue());

        saveToolBeltFile(file, data);
    }

    public void saveFlightTimer(Player p, FileConfiguration data) {
        AbilityTimer timer = getAbility(p, "Flight");
        if (timer == null) {
            if (data.contains(p.getUniqueId() + ".Flight")) data.set(p.getUniqueId() + ".Flight", null);
            return;
        }

        data.set(p.getUniqueId() + ".Flight.ActiveTime", timer.getActiveTimeLeft());
        data.set(p.getUniqueId() + ".Flight.CooldownTime", timer.getTimeTillReset());
        data.set(p.getUniqueId() + ".Flight.Speed", timer.getFlightSpeed());
    }

    public void startBloodyDomain(Player p) {
        Reward reward = plugin.getSkillManager().getPlayerRewards(p).getReward("Fighting", "BloodyDomain");
        if (reward == null || !reward.isApplied()) return;

        BloodyDomain domain = new BloodyDomain(p);
        domain.runTaskTimer(plugin, 0, 20);
        bloodyDomainTracker.put(p, domain);
    }

    public void endPlayerTimers(Player p) {
        if (!timerTracker.containsKey(p)) return;
        for (AbilityTimer timer : timerTracker.get(p)) timer.endAbility();
    }

    public void addAbility(Player p, AbilityTimer timer) {
        timerTracker.computeIfAbsent(p, k -> new ArrayList<>());
        timerTracker.get(p).add(timer);
    }

    public void removeAbility(Player p, String ability) {
        if (!timerTracker.containsKey(p)) return;
        timerTracker.get(p).removeIf(timer -> timer.getName().equalsIgnoreCase(ability));
    }

    public AbilityTimer getAbility(Player p, String ability) {
        if (!timerTracker.containsKey(p)) return null;
        for (AbilityTimer timer : timerTracker.get(p)) if (timer.getName().equalsIgnoreCase(ability)) return timer;
        return null;
    }

    public void addScannedMobs(ArrayList<Entity> entities) {
        mobsScanned.addAll(entities);
    }

    public void removeScannedMobs(ArrayList<Entity> entities) {
        mobsScanned.removeAll(entities);
    }

    public void removeGlowFromScannedMobs() {
        for (Entity entity : mobsScanned) entity.setGlowing(false);
    }

    public void createTrails() {
        trails.put("Dust", Particle.DUST);
        trails.put("Water", Particle.SPLASH);
        trails.put("Happy", Particle.HAPPY_VILLAGER);
        trails.put("Dragon", Particle.DRAGON_BREATH);
        trails.put("Electric", Particle.ELECTRIC_SPARK);
        trails.put("Enchantment", Particle.ENCHANT);
        trails.put("Ominous", Particle.TRIAL_OMEN);
        trails.put("Love", Particle.HEART);
        trails.put("Flame", Particle.FLAME);
        trails.put("BlueFlame", Particle.SOUL_FIRE_FLAME);
        trails.put("Cherry", Particle.CHERRY_LEAVES);
        trails.put("Rainbow", Particle.DUST);
    }

    public void createDomainMobs() {
        domainMobs.add(EntityType.BLAZE);
        domainMobs.add(EntityType.BOGGED);
        domainMobs.add(EntityType.BREEZE);
        domainMobs.add(EntityType.CAVE_SPIDER);
        domainMobs.add(EntityType.CREEPER);
        domainMobs.add(EntityType.DROWNED);
        domainMobs.add(EntityType.ENDERMAN);
        domainMobs.add(EntityType.ENDERMITE);
        domainMobs.add(EntityType.EVOKER);
        domainMobs.add(EntityType.GHAST);
        domainMobs.add(EntityType.GUARDIAN);
        domainMobs.add(EntityType.HOGLIN);
        domainMobs.add(EntityType.HUSK);
        domainMobs.add(EntityType.ILLUSIONER);
        domainMobs.add(EntityType.MAGMA_CUBE);
        domainMobs.add(EntityType.PHANTOM);
        domainMobs.add(EntityType.PIGLIN);
        domainMobs.add(EntityType.PIGLIN_BRUTE);
        domainMobs.add(EntityType.PILLAGER);
        domainMobs.add(EntityType.RAVAGER);
        domainMobs.add(EntityType.SHULKER);
        domainMobs.add(EntityType.SILVERFISH);
        domainMobs.add(EntityType.SKELETON);
        domainMobs.add(EntityType.SLIME);
        domainMobs.add(EntityType.SPIDER);
        domainMobs.add(EntityType.STRAY);
        domainMobs.add(EntityType.VEX);
        domainMobs.add(EntityType.VINDICATOR);
        domainMobs.add(EntityType.WITCH);
        domainMobs.add(EntityType.WITHER_SKELETON);
        domainMobs.add(EntityType.ZOGLIN);
        domainMobs.add(EntityType.ZOMBIE);
        domainMobs.add(EntityType.ZOMBIE_VILLAGER);
    }

    public HashMap<String, Particle> getTrails() {
        return trails;
    }

    public HashMap<Player, TrailEffect> getTrailTracker() {
        return trailTracker;
    }

    public HashMap<Player, ArrayList<AbilityTimer>> getTimerTracker() {
        return timerTracker;
    }

    public static ArrayList<EntityType> getDomainMobs() {
        return domainMobs;
    }

    public HashMap<Player, BloodyDomain> getBloodyDomainTracker() {
        return bloodyDomainTracker;
    }
}
