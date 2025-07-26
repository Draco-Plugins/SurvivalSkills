package sir_draco.survivalskills.abilities;

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
import sir_draco.survivalskills.rewards.Reward;
import sir_draco.survivalskills.SurvivalSkills;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class AbilityManager {

    private static final ArrayList<EntityType> domainMobs = new ArrayList<>();
    public static final String FLIGHT = ".Flight";
    public static final String SPELUNKER = ".Spelunker";
    public static final String LAST_USED_SUFFIX = ".LastUsedTimestamp";

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
        if (!data.contains(p.getUniqueId() + FLIGHT)) return;

        int activeTime = data.getInt(p.getUniqueId() + ".Flight.ActiveTime");
        int cooldownTime = data.getInt(p.getUniqueId() + ".Flight.CooldownTime");
        float speed = (float) data.getDouble(p.getUniqueId() + ".Flight.Speed");

        // Apply offline cooldown if ability was in cooldown mode
        if (activeTime <= 0 && cooldownTime > 0 && data.contains(p.getUniqueId() + FLIGHT + LAST_USED_SUFFIX)) {
            long lastUsedTimestamp = data.getLong(p.getUniqueId() + FLIGHT + LAST_USED_SUFFIX);
            long currentTime = System.currentTimeMillis();
            long elapsedTimeMs = currentTime - lastUsedTimestamp;
            int elapsedSeconds = (int) (elapsedTimeMs / 1000);

            // Reduce cooldown time by elapsed time
            cooldownTime = Math.max(0, cooldownTime - elapsedSeconds);

            // If cooldown is complete, notify player and skip creating the timer
            if (cooldownTime == 0) {
                p.sendRawMessage(ChatColor.GREEN + "Your Flight ability has reset while you were offline!");
                data.set(p.getUniqueId() + FLIGHT, null);
                return;
            }
        }

        AbilityTimer timer = new AbilityTimer(plugin, "Flight", p, activeTime, cooldownTime);
        timer.runTaskTimerAsynchronously(plugin, 0, 20);
        timer.setFlightSpeed(speed);
        addAbility(p, timer);

        if (activeTime <= 0) return;
        p.setAllowFlight(true);
        p.setFlying(true);
        p.setFlySpeed(speed);
        FlyingTimer flyingTimer = new FlyingTimer(p, activeTime);
        plugin.getFlightCommand().getFlyingTimers().put(p, flyingTimer);
        flyingTimer.runTaskTimerAsynchronously(plugin, 0, 20);
        int minutes = activeTime / 60;
        int seconds = activeTime % 60;
        p.sendRawMessage(ChatColor.GREEN + "Your flight will end in " + ChatColor.AQUA + minutes +
                ChatColor.GREEN + " minutes " + ChatColor.AQUA + seconds + ChatColor.GREEN + " seconds");
    }

    public void loadSpelunker(Player p, FileConfiguration data) {
        if (!data.contains(p.getUniqueId() + SPELUNKER)) return;

        int activeTime = data.getInt(p.getUniqueId() + ".Spelunker.ActiveTime");
        int cooldownTime = data.getInt(p.getUniqueId() + ".Spelunker.CooldownTime");
        int radius = data.getInt(p.getUniqueId() + ".Spelunker.Radius");

        // Apply offline cooldown if ability was in cooldown mode
        if (activeTime <= 0 && cooldownTime > 0 && data.contains(p.getUniqueId() + SPELUNKER + LAST_USED_SUFFIX)) {
            long lastUsedTimestamp = data.getLong(p.getUniqueId() + SPELUNKER + LAST_USED_SUFFIX);
            long currentTime = System.currentTimeMillis();
            long elapsedTimeMs = currentTime - lastUsedTimestamp;
            int elapsedSeconds = (int) (elapsedTimeMs / 1000);

            // Reduce cooldown time by elapsed time
            cooldownTime = Math.max(0, cooldownTime - elapsedSeconds);

            // If cooldown is complete, notify player and skip creating the timer
            if (cooldownTime == 0) {
                p.sendRawMessage(ChatColor.GREEN + "Your Spelunker ability has reset while you were offline!");
                data.set(p.getUniqueId() + SPELUNKER, null);
                return;
            }
        }

        AbilityTimer timer = new AbilityTimer(plugin, "Spelunker", p, activeTime, cooldownTime);
        timer.runTaskTimerAsynchronously(plugin, 0, 20);
        addAbility(p, timer);

        if (activeTime <= 0) return;

        // Restart the spelunker ability if it was active
        SpelunkerAbilitySync spelunker = new SpelunkerAbilitySync(plugin, radius, p);
        spelunker.runTaskTimer(plugin, 0, 10);
        plugin.getMiningListener().getSpelunkerTracker().put(p, spelunker);

        int minutes = activeTime / 60;
        int seconds = activeTime % 60;
        p.sendRawMessage(ChatColor.GREEN + "Your spelunker will end in " + ChatColor.AQUA + minutes +
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
            Bukkit.getLogger().log(Level.WARNING, String.format("Failed to save Tool Belts for Survival Skills plugin: %s", e.getMessage()));
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
            if (data.contains(p.getUniqueId() + FLIGHT)) data.set(p.getUniqueId() + FLIGHT, null);
            return;
        }

        data.set(p.getUniqueId() + ".Flight.ActiveTime", timer.getActiveTimeLeft());
        data.set(p.getUniqueId() + ".Flight.CooldownTime", timer.getTimeTillReset());
        data.set(p.getUniqueId() + ".Flight.Speed", timer.getFlightSpeed());

        // Save the current timestamp for offline cooldown calculation
        if (!timer.isActive() && timer.getTimeTillReset() > 0) {
            data.set(p.getUniqueId() + FLIGHT + LAST_USED_SUFFIX, System.currentTimeMillis());
        }
    }

    public void saveSpelunkerTimer(Player p, FileConfiguration data) {
        AbilityTimer timer = getAbility(p, "Spelunker");
        if (timer == null) {
            if (data.contains(p.getUniqueId() + SPELUNKER)) data.set(p.getUniqueId() + SPELUNKER, null);
            return;
        }

        data.set(p.getUniqueId() + ".Spelunker.ActiveTime", timer.getActiveTimeLeft());
        data.set(p.getUniqueId() + ".Spelunker.CooldownTime", timer.getTimeTillReset());

        // Save the current timestamp for offline cooldown calculation
        if (!timer.isActive() && timer.getTimeTillReset() > 0) {
            data.set(p.getUniqueId() + SPELUNKER + LAST_USED_SUFFIX, System.currentTimeMillis());
        }

        // Save the radius from the active spelunker or use default based on player's rewards
        int radius = 5; // Default radius
        SpelunkerAbilitySync activeSpelunker = plugin.getMiningListener().getSpelunkerTracker().get(p);
        if (activeSpelunker != null) {
            radius = activeSpelunker.getRadius();
        } else {
            // Determine radius based on player's spelunker level if no active spelunker
            if (plugin.getSkillManager().getPlayerRewards(p).getReward("Mining", "SpelunkerIII").isApplied()) {
                radius = 15;
            } else if (plugin.getSkillManager().getPlayerRewards(p).getReward("Mining", "SpelunkerII").isApplied()) {
                radius = 10;
            }
        }
        data.set(p.getUniqueId() + ".Spelunker.Radius", radius);
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

    public void addScannedMobs(List<Entity> entities) {
        mobsScanned.addAll(entities);
    }

    public void removeScannedMobs(List<Entity> entities) {
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

    public Map<String, Particle> getTrails() {
        return trails;
    }

    public Map<Player, TrailEffect> getTrailTracker() {
        return trailTracker;
    }

    public Map<Player, ArrayList<AbilityTimer>> getTimerTracker() {
        return timerTracker;
    }

    public static List<EntityType> getDomainMobs() {
        return domainMobs;
    }

    public Map<Player, BloodyDomain> getBloodyDomainTracker() {
        return bloodyDomainTracker;
    }
}
