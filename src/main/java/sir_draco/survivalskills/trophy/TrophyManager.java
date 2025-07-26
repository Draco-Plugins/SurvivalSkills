package sir_draco.survivalskills.trophy;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.MemoryNPCDataStore;
import net.citizensnpcs.api.npc.NPCRegistry;
import org.bukkit.*;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.rewards.Reward;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.god_questline.GodTrophyQuest;
import sir_draco.survivalskills.utils.ColorParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TrophyManager {

    public final static NPCRegistry registry = CitizensAPI.createAnonymousNPCRegistry(new MemoryNPCDataStore());
    public final static String npcName = ColorParser.colorizeString("God Trophy",
            ColorParser.generateGradient("#FFFF00", "#FFFFFF", 10), true);

    private final SurvivalSkills plugin;
    private final HashMap<UUID, HashMap<String, Boolean>> trophyTracker = new HashMap<>();
    private final HashMap<Location, Trophy> trophies = new HashMap<>();
    private final HashMap<Integer, ItemStack> trophyItems = new HashMap<>();
    private final HashMap<UUID, GodTrophyQuest> playerGodQuestData = new HashMap<>();
    private final HashMap<UUID, Integer> godNPCIDs = new HashMap<>();

    private boolean godQuestEnabled;
    private static int nextID = 1;

    public TrophyManager(SurvivalSkills plugin) {
        this.plugin = plugin;
        loadTrophies();
        loadGodQuestStatus();
    }

    /**
     * Gets the location of trophies, their type, and
     * who placed the trophy from a file
     */
    public void loadTrophies() {
        FileConfiguration trophyData = plugin.getTrophyData();
        ConfigurationSection section = trophyData.getConfigurationSection("");
        if (section == null) return;

        // Shutdown and clear existing trophies to avoid duplicates
        for (Trophy t : trophies.values()) {
            t.shutdownTrophy();
        }
        trophies.clear();

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
                trophy.spawnTrophy(plugin);
                trophies.put(loc, trophy);
            }
        });
    }

    public void loadPlayerTrophies(UUID uuid, FileConfiguration data) {
        if (trophyTracker.containsKey(uuid)) return;
        HashMap<String, Boolean> trophyList = new HashMap<>();
        trophyList.put("CaveTrophy", data.getBoolean(uuid + ".CaveTrophy"));
        trophyList.put("ForestTrophy", data.getBoolean(uuid + ".ForestTrophy"));
        trophyList.put("FarmingTrophy", data.getBoolean(uuid + ".FarmingTrophy"));
        trophyList.put("OceanTrophy", data.getBoolean(uuid + ".OceanTrophy"));
        trophyList.put("FishingTrophy", data.getBoolean(uuid + ".FishingTrophy"));
        trophyList.put("ColorTrophy", data.getBoolean(uuid + ".ColorTrophy"));
        trophyList.put("NetherTrophy", data.getBoolean(uuid + ".NetherTrophy"));
        trophyList.put("EndTrophy", data.getBoolean(uuid + ".EndTrophy"));
        trophyList.put("ChampionTrophy", data.getBoolean(uuid + ".ChampionTrophy"));
        trophyList.put("GodTrophy", data.getBoolean(uuid + ".GodTrophy"));
        trophyTracker.put(uuid, trophyList);
    }

    public void loadGodQuestStatus() {
        FileConfiguration config = plugin.getTrueConfig();
        godQuestEnabled = config.getBoolean("GodQuestEnabled");
    }

    public void saveTrophies() throws IOException {
        FileConfiguration trophyData = plugin.getTrophyData();
        if (trophyData == null) return;

        if (trophies.isEmpty()) {
            plugin.saveResource("trophydata.yml", true);
            return;
        }

        for (Map.Entry<Location, Trophy> trophy : trophies.entrySet()) {
            trophy.getValue().shutdownTrophy();
            trophyData.set(trophy.getValue().getID() + ".Location", trophy.getKey());
            trophyData.set(trophy.getValue().getID() + ".UUID", trophy.getValue().getUUID().toString());
            trophyData.set(trophy.getValue().getID() + ".Type", trophy.getValue().getType());
            trophyData.set(trophy.getValue().getID() + ".PlayerName", trophy.getValue().getPlayerName());
        }

        trophyData.save(plugin.getTrophyFile());
    }

    public void saveTrophyData(FileConfiguration data) {
        for (Map.Entry<UUID, HashMap<String, Boolean>> player : trophyTracker.entrySet()) {
            savePlayerTrophyData(player.getKey(), data);
        }
    }

    public void savePlayerTrophyData(UUID uuid, FileConfiguration data) {
        if (trophyTracker.containsKey(uuid)) {
            for (Map.Entry<String, Boolean> list : trophyTracker.get(uuid).entrySet()) {
                if (data.get(uuid + "." + list.getKey()) != null) {
                    boolean trophy = data.getBoolean(uuid + "." + list.getKey());
                    if (trophy) continue;
                    data.set(uuid + "." + list.getKey(), list.getValue());
                }
                else data.set(uuid + "." + list.getKey(), list.getValue());
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
        int count = 0;
        if (trophyTracker.get(uuid) == null)  {
            // Try to load the player's trophies after a delay
            new BukkitRunnable() {
                @Override
                public void run() {
                    loadTrophies();
                }
            }.runTaskLater(plugin, 20);
            return 10;
        }
        for (Map.Entry<String, Boolean> list : trophyTracker.get(uuid).entrySet()) if (list.getValue()) count++;
        return 10 + (count * 10);
    }

    public int generateTrophyID() {
        int id;
        boolean unique;
        do {
            id = (int) Math.ceil(Math.random() * 1000000);
            unique = true;
            for (Trophy trophy : trophies.values()) {
                if (trophy.getID() == id) {
                    unique = false;
                    break;
                }
            }
        } while (!unique);
        return id;
    }

    public void removeTrophy(Location loc) {
        FileConfiguration trophyData = plugin.getTrophyData();
        Trophy trophy = trophies.get(loc);
        trophies.remove(loc);
        if (trophy == null) return;
        if (trophyData.get("" + trophy.getID()) == null) return;
        trophyData.set("" + trophy.getID(), null);
    }

    public int getRewardLevel(String type, String reward) {
        for (Reward r : plugin.getSkillManager().getDefaultPlayerRewards().getRewardList().get(type)) {
            if (r.getName().equalsIgnoreCase(reward)) return r.getLevel();
        }
        return 0;
    }

    public HashMap<UUID, HashMap<String, Boolean>> getTrophyTracker() {
        return trophyTracker;
    }

    public HashMap<Integer, ItemStack> getTrophyItems() {
        return trophyItems;
    }

    public ItemStack getTrophyItem(int type) {
        if (!trophyItems.containsKey(type)) return new ItemStack(Material.AIR);
        return trophyItems.get(type);
    }

    public HashMap<Location, Trophy> getTrophies() {
        return trophies;
    }

    public HashMap<UUID, GodTrophyQuest> getPlayerGodQuestData() {
        return playerGodQuestData;
    }

    public void setGodQuestEnabled(boolean godQuestEnabled) {
        this.godQuestEnabled = godQuestEnabled;
    }

    public boolean isGodQuestEnabled() {
        return godQuestEnabled;
    }

    public static int getNextID() {
        int num = nextID;
        nextID++;
        return num;
    }

    public static NPCRegistry getRegistry() {
        return registry;
    }

    public HashMap<UUID, Integer> getGodNPCIDs() {
        return godNPCIDs;
    }
}
