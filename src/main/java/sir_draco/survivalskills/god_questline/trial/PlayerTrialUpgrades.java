package sir_draco.survivalskills.god_questline.trial;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.SurvivalSkills;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerTrialUpgrades {

    private final UUID playerId;
    private final Map<String, Integer> upgradeLevels;
    private int availablePoints;

    public PlayerTrialUpgrades(UUID playerId) {
        this.playerId = playerId;
        this.upgradeLevels = new HashMap<>();
        this.availablePoints = 0;

        // Initialize all upgrades to level 0
        for (String upgradeId : TrialTree.getAllUpgrades().keySet()) {
            upgradeLevels.put(upgradeId, 0);
        }
    }

    public int getUpgradeLevel(String upgradeId) {
        return upgradeLevels.getOrDefault(upgradeId, 0);
    }

    public boolean canUpgrade(String upgradeId) {
        TrialTree.TrialUpgrade upgrade = TrialTree.getUpgrade(upgradeId);
        if (upgrade == null) return false;

        int currentLevel = getUpgradeLevel(upgradeId);
        if (currentLevel >= upgrade.getMaxLevel()) return false;

        int cost = upgrade.getCost(currentLevel + 1);
        return availablePoints >= cost;
    }

    public boolean purchaseUpgrade(String upgradeId) {
        if (!canUpgrade(upgradeId)) return false;
        int currentLevel = getUpgradeLevel(upgradeId);

        resetPoints();
        upgradeLevels.put(upgradeId, currentLevel + 1);

        return true;
    }

    public void addPoints(int points) {
        this.availablePoints += points;
    }

    public void resetPoints() {
        this.availablePoints = 0;
        saveToFile();
    }

    public int getAvailablePoints() {
        return availablePoints;
    }

    public void saveToFile() {
        File file = new File(SurvivalSkills.getInstance().getDataFolder(), "trial-upgrades.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        String path = playerId.toString();

        for (Map.Entry<String, Integer> entry : upgradeLevels.entrySet()) {
            config.set(path + ".upgrades." + entry.getKey(), entry.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            SurvivalSkills.getInstance().getLogger().warning("Failed to save trial upgrades for " + playerId);
        }
    }

    public void loadFromFile() {
        File file = new File(SurvivalSkills.getInstance().getDataFolder(), "trial-upgrades.yml");
        if (!file.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        String path = playerId.toString();

        if (!config.contains(path)) return;

        if (config.contains(path + ".upgrades")) {
            ConfigurationSection section = config.getConfigurationSection(path + ".upgrades");
            if (section == null) return;
            for (String upgradeId : section.getKeys(false)) {
                int level = config.getInt(path + ".upgrades." + upgradeId, 0);
                upgradeLevels.put(upgradeId, level);
            }
        }
    }

    public static PlayerTrialUpgrades getPlayerUpgrades(Player player) {
        return TrialUpgradeManager.getPlayerUpgrades(player);
    }
}