package sir_draco.survivalskills.trophy;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import sir_draco.survivalskills.rewards.Reward;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.external.providers.CitizensRegistryProvider;
import sir_draco.survivalskills.god_questline.GodTrophyEffects;
import sir_draco.survivalskills.god_questline.GodTrophyQuest;
import sir_draco.survivalskills.utils.ColorParser;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class TrophyManager {

    public static final String npcName = ColorParser.colorizeString("God Trophy",
            ColorParser.generateGradient("#FFFF00", "#FFFFFF", 10), true);

    private static final int MAX_TROPHY_ID = 1_000_000;
    private static final int BASE_SKILL_CAP = 10;
    private static final int SKILL_CAP_PER_TROPHY = 10;

    private final SurvivalSkills plugin;
    private final Map<UUID, Map<TrophyType, Boolean>> trophyTracker = new HashMap<>();
    private final HashMap<Location, Trophy> trophies = new HashMap<>();
    private final HashMap<Integer, ItemStack> trophyItems = new HashMap<>();
    private final HashMap<UUID, GodTrophyQuest> playerGodQuestData = new HashMap<>();
    private final HashMap<UUID, Integer> godNPCIDs = new HashMap<>();

    public static CitizensRegistryProvider registry = null;
    private boolean godQuestEnabled;

    public TrophyManager(SurvivalSkills plugin) {
        this.plugin = plugin;
        if (plugin.isCitizensEnabled()) {
            registry = new CitizensRegistryProvider();
        }

        loadTrophies();
        loadGodQuestStatus(plugin.isCitizensEnabled());
    }

    /**
     * Gets the location of trophies, their type, and
     * who placed the trophy from a file
     */
    public void loadTrophies() {
        FileConfiguration trophyData = plugin.getTrophyData();
        ConfigurationSection section = trophyData.getConfigurationSection("");

        // Shutdown and clear existing trophies to avoid duplicates
        for (Trophy t : trophies.values()) {
            t.shutdownTrophy();
        }
        trophies.clear();

        if (section != null) {
            section.getKeys(false).forEach(key -> {
                Location loc = trophyData.getLocation(key + ".Location");
                String uuidString = trophyData.getString(key + ".UUID");
                if (uuidString == null) {
                    trophyData.set(key, null);
                }
                else {
                    int id = Integer.parseInt(key);
                    UUID uuid = UUID.fromString(uuidString);
                    String type = trophyData.getString(key + ".Type");
                    String playerName = trophyData.getString(key + ".PlayerName");

                    Trophy trophy = new Trophy(loc, uuid, type, id, playerName);
                    // Server load: not a fresh placement, so skip placement-only animations
                    trophy.spawnTrophy(plugin, false);
                    trophies.put(loc, trophy);
                }
            });
        }
        Map<Integer, Location> activeGodTrophies = trophies.entrySet().stream()
                .filter((Map.Entry<Location, Trophy> trophyEntry) ->
                        trophyEntry.getValue().getTrophyType() == TrophyType.GOD.getId())
                .collect(Collectors.toUnmodifiableMap(
                        (Map.Entry<Location, Trophy> trophyEntry) -> trophyEntry.getValue().getID(),
                        (Map.Entry<Location, Trophy> trophyEntry) -> trophyEntry.getKey().clone()));
        GodTrophyEffects.removeOrphanedCrystals(plugin, activeGodTrophies);
    }

    public void loadPlayerTrophies(UUID uuid, FileConfiguration data) {
        if (trophyTracker.containsKey(uuid)) return;
        HashMap<TrophyType, Boolean> trophyList = new HashMap<>();
        for (TrophyType type : TrophyType.values()) {
            trophyList.put(type, data.getBoolean(uuid + "." + type.getName()));
        }
        trophyTracker.put(uuid, trophyList);
    }

    public void loadGodQuestStatus(boolean isCitizensEnabled) {
        FileConfiguration config = plugin.getTrueConfig();
        godQuestEnabled = config.getBoolean("GodQuestEnabled");

        if (isCitizensEnabled) return;
        if (godQuestEnabled) {
            Bukkit.getLogger().log(Level.WARNING, "[Survival Skills] God Questline is enabled but Citizens plugin is not installed. Disabling God Questline.");
            godQuestEnabled = false;
        }
    }

    public void saveTrophies() throws IOException {
        FileConfiguration trophyData = plugin.getTrophyData();
        if (trophyData == null) return;

        if (trophies.isEmpty()) {
            plugin.saveResource("trophydata.yml", true);
            return;
        }

        for (Map.Entry<Location, Trophy> trophy : trophies.entrySet()) {
            trophyData.set(trophy.getValue().getID() + ".Location", trophy.getKey());
            trophyData.set(trophy.getValue().getID() + ".UUID", trophy.getValue().getUuid().toString());
            trophyData.set(trophy.getValue().getID() + ".Type", trophy.getValue().getType());
            trophyData.set(trophy.getValue().getID() + ".PlayerName", trophy.getValue().getPlayerName());
        }

        trophyData.save(plugin.getTrophyFile());
    }

    public void saveTrophyData(FileConfiguration data) {
        for (Map.Entry<UUID, Map<TrophyType, Boolean>> player : trophyTracker.entrySet()) {
            savePlayerTrophyData(player.getKey(), data);
        }
    }

    public void savePlayerTrophyData(UUID uuid, FileConfiguration data) {
        if (trophyTracker.containsKey(uuid)) {
            for (Map.Entry<TrophyType, Boolean> entry : trophyTracker.get(uuid).entrySet()) {
                String key = uuid + "." + entry.getKey().getName();
                // Never downgrade an already-true trophy to false
                if (!data.getBoolean(key)) {
                    data.set(key, entry.getValue());
                }
            }
        }
        else Bukkit.getLogger().warning("UUID " + uuid + " does not have a trophy status");
    }

    public void saveGodQuestData(FileConfiguration data) {
        for (Map.Entry<UUID, GodTrophyQuest> player : playerGodQuestData.entrySet()) {
            savePlayerGodQuestData(player.getKey(), data);
        }
    }

    public void savePlayerGodQuestData(UUID uuid, FileConfiguration data) {
        if (playerGodQuestData.containsKey(uuid)) {
            GodTrophyQuest quest = playerGodQuestData.get(uuid);
            data.set(uuid + ".Phase", quest.getPhase());
            data.set(uuid + ".ItemCount", quest.getCurrentItemCount());
        }
        else Bukkit.getLogger().warning("UUID " + uuid + " does not have a god quest status");
    }

    public void disableTrophies() {
        for (Map.Entry<Location, Trophy> trophy : trophies.entrySet()) {
            trophy.getValue().shutdownTrophy();
        }
    }

    /**
     * Counts how many trophies a player has obtained and returns the
     * max level a player can be
     */
    public int playerMaxSkillLevel(UUID uuid) {
        Map<TrophyType, Boolean> playerTrophies = trophyTracker.get(uuid);
        if (playerTrophies == null) {
            ensurePlayerTrophiesLoaded(uuid);
            playerTrophies = trophyTracker.get(uuid);
            if (playerTrophies == null) return BASE_SKILL_CAP;
        }
        int count = 0;
        for (Boolean earned : playerTrophies.values()) {
            if (earned) count++;
        }
        return BASE_SKILL_CAP + (count * SKILL_CAP_PER_TROPHY);
    }

    /**
     * Attempts to load trophy data for a single player from playerdata.yml.
     * Called as a fallback when a player's trophy tracker entry is missing.
     */
    private void ensurePlayerTrophiesLoaded(UUID uuid) {
        File dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) return;
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        if (data.contains(uuid.toString())) {
            loadPlayerTrophies(uuid, data);
        }
    }

    public int generateTrophyID() {
        int attempts = 0;
        int id;
        do {
            if (++attempts > MAX_TROPHY_ID) {
                throw new IllegalStateException("No available trophy IDs after " + MAX_TROPHY_ID + " attempts");
            }
            id = ThreadLocalRandom.current().nextInt(1, MAX_TROPHY_ID + 1);
        } while (isTrophyIDTaken(id));
        return id;
    }

    private boolean isTrophyIDTaken(int id) {
        for (Trophy trophy : trophies.values()) {
            if (trophy.getID() == id) return true;
        }
        return false;
    }

    public void removeTrophy(Location loc) {
        FileConfiguration trophyData = plugin.getTrophyData();
        Trophy trophy = trophies.remove(loc);
        if (trophy == null) return;
        trophy.shutdownTrophy();
        trophyData.set(String.valueOf(trophy.getID()), null);
        try {
            trophyData.save(plugin.getTrophyFile());
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.WARNING, "[SurvivalSkills] Failed to save trophy data after removal", e);
        }
    }

    public int getRewardLevel(SkillCategory skillCategory, String reward) {
        for (Reward r : plugin.getSkillManager().getDefaultPlayerRewards().getRewardList().get(skillCategory)) {
            if (r.getName().equalsIgnoreCase(reward)) return r.getLevel();
        }
        return 0;
    }

    public Map<UUID, Map<TrophyType, Boolean>> getTrophyTracker() {
        return trophyTracker;
    }

    public Map<Integer, ItemStack> getTrophyItems() {
        return trophyItems;
    }

    public ItemStack getTrophyItem(int type) {
        if (!trophyItems.containsKey(type)) return new ItemStack(Material.AIR);
        return trophyItems.get(type);
    }

    public Map<Location, Trophy> getTrophies() {
        return trophies;
    }

    public Map<UUID, GodTrophyQuest> getPlayerGodQuestData() {
        return playerGodQuestData;
    }

    public void setGodQuestEnabled(boolean godQuestEnabled) {
        this.godQuestEnabled = godQuestEnabled;
    }

    public boolean isGodQuestEnabled() {
        return godQuestEnabled;
    }

    public static CitizensRegistryProvider getCitizensRegistryProvider() {
        return registry;
    }

    public Map<UUID, Integer> getGodNPCIDs() {
        return godNPCIDs;
    }
}
