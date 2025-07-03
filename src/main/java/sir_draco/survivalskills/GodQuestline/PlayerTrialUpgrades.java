package sir_draco.survivalskills.GodQuestline;

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

        TrialTree.TrialUpgrade upgrade = TrialTree.getUpgrade(upgradeId);
        int currentLevel = getUpgradeLevel(upgradeId);
        int cost = upgrade.getCost(currentLevel + 1);

        availablePoints -= cost;
        upgradeLevels.put(upgradeId, currentLevel + 1);

        saveToFile();
        return true;
    }

    public void addPoints(int points) {
        this.availablePoints += points;
        saveToFile();
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
        config.set(path + ".points", availablePoints);

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

        availablePoints = config.getInt(path + ".points", 0);

        if (config.contains(path + ".upgrades")) {
            for (String upgradeId : config.getConfigurationSection(path + ".upgrades").getKeys(false)) {
                int level = config.getInt(path + ".upgrades." + upgradeId, 0);
                upgradeLevels.put(upgradeId, level);
            }
        }
    }

    public static PlayerTrialUpgrades getPlayerUpgrades(Player player) {
        return TrialUpgradeManager.getPlayerUpgrades(player);
    }
}