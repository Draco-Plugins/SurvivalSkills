package sir_draco.survivalskills.god_questline.trial;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.SurvivalSkills;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlayerTrialUpgrades {

    private final UUID playerId;
    private final Map<String, Integer> upgradeLevels;
    private int availablePoints;

    public PlayerTrialUpgrades(UUID playerId) {
        this.playerId = playerId;
        this.upgradeLevels = new ConcurrentHashMap<>();
        this.availablePoints = 0;

        // Initialize all upgrades to level 0
        for (String upgradeId : TrialTree.getAllUpgrades().keySet()) {
            upgradeLevels.put(upgradeId, 0);
        }
    }

    /**
     * Defensive guard: getOrDefault ensures unknown upgrade IDs (e.g., from bugs
     * or future upgrades not initialized at construction time) silently return 0
     * rather than throwing NullPointerException.
     */
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
        TrialTree.TrialUpgrade upgrade = TrialTree.getUpgrade(upgradeId);
        if (upgrade == null) return false;

        int currentLevel = getUpgradeLevel(upgradeId);
        if (currentLevel >= upgrade.getMaxLevel()) return false;

        int cost = upgrade.getCost(currentLevel + 1);
        if (!spendPoints(cost)) return false;

        upgradeLevels.put(upgradeId, currentLevel + 1);
        return true;
    }

    /**
     * Deducts points during a trial round. Points are transient (per-round) and
     * do not persist across server restarts — that is by design.
     */
    public boolean spendPoints(int cost) {
        if (cost < 0 || availablePoints < cost) return false;
        availablePoints -= cost;
        return true;
    }

    public void addPoints(int points) {
        this.availablePoints += points;
    }

    /**
     * Resets all unspent points for the current round. Points are
     * round-scoped and intentionally do not survive server restarts.
     */
    public void resetPoints() {
        this.availablePoints = 0;
    }

    public int getAvailablePoints() {
        return availablePoints;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void saveToFile() {
        File folder = new File(SurvivalSkills.getInstance().getDataFolder(), "trial-upgrades");
        folder.mkdirs();
        File file = new File(folder, playerId + ".yml");

        FileConfiguration config = new YamlConfiguration();
        for (Map.Entry<String, Integer> entry : upgradeLevels.entrySet()) {
            config.set(entry.getKey(), entry.getValue());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            SurvivalSkills.getInstance().getLogger().log(Level.SEVERE,
                    String.format("[SurvivalSkills] Failed to save trial upgrades for %s", playerId), e);
        }
    }

    public void loadFromFile() {
        File folder = new File(SurvivalSkills.getInstance().getDataFolder(), "trial-upgrades");
        File file = new File(folder, playerId + ".yml");
        if (!file.exists()) return;

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String upgradeId : config.getKeys(false)) {
            int level = config.getInt(upgradeId, 0);
            upgradeLevels.put(upgradeId, level);
        }
    }

    public static PlayerTrialUpgrades getPlayerUpgrades(Player player) {
        return TrialUpgradeManager.getPlayerUpgrades(player);
    }
}