package sir_draco.survivalskills.Abilities;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import sir_draco.survivalskills.SurvivalSkills;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AbilityManager {

    private final SurvivalSkills plugin;
    private final HashMap<Player, ArrayList<AbilityTimer>> timerTracker = new HashMap<>();
    private final HashMap<Player, TrailEffect> trailTracker = new HashMap<>();
    private final HashMap<String, Particle> trails = new HashMap<>();

    public AbilityManager(SurvivalSkills plugin) {
        this.plugin = plugin;
        createTrails();
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

    public HashMap<String, Particle> getTrails() {
        return trails;
    }

    public HashMap<Player, TrailEffect> getTrailTracker() {
        return trailTracker;
    }

    public HashMap<Player, ArrayList<AbilityTimer>> getTimerTracker() {
        return timerTracker;
    }
}
