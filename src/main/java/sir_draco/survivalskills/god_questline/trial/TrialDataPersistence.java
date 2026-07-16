package sir_draco.survivalskills.god_questline.trial;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.TrialUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Single source of truth for the trial data file ({@code trialdata.yml}) and the trial-building
 * config. Centralizes the previously-duplicated "load config if null" blocks and exposes the
 * persistence operations (protected areas, completed trials, trial-building ownership) the rest
 * of the system needs.
 */
public class TrialDataPersistence {

    private static final long CLEANUP_INTERVAL = 60 * 20L; // 1 minute in ticks
    private static final String TRIAL_DATA_FILE = "trialdata.yml";

    private static final String BUILDING_FILE = "trialbuilding.yml";

    private static TrialDataPersistence instance;

    private FileConfiguration trialDataConfig = null;
    private FileConfiguration trialBuildingConfig = null;
    private final HashMap<UUID, TrialBuildingData> trialBuildingOwnership = new HashMap<>();

    public static synchronized TrialDataPersistence getInstance() {
        if (instance == null)
            instance = new TrialDataPersistence();
        return instance;
    }

    private TrialDataPersistence() {
    }

    // --- Centralized config loading --------------------------------------

    /**
     * Lazily loads (and caches) the trial data config exactly once, regardless of how many
     * callers race in. Replaces the four duplicated load-or-skip blocks in the original code.
     */
    public synchronized FileConfiguration getOrLoadTrialDataConfig() {
        if (trialDataConfig != null)
            return trialDataConfig;
        File file = new File(SurvivalSkills.getInstance().getDataFolder(), TRIAL_DATA_FILE);
        if (!file.exists())
            SurvivalSkills.getInstance().saveResource(TRIAL_DATA_FILE, true);
        trialDataConfig = YamlConfiguration.loadConfiguration(file);
        return trialDataConfig;
    }

    public FileConfiguration getOrLoadTrialBuildingConfig() {
        if (trialBuildingConfig != null)
            return trialBuildingConfig;
        File file = new File(SurvivalSkills.getInstance().getDataFolder(), BUILDING_FILE);
        if (!file.exists())
            return null;
        trialBuildingConfig = YamlConfiguration.loadConfiguration(file);
        return trialBuildingConfig;
    }

    public void setTrialBuildingConfig(FileConfiguration config) {
        this.trialBuildingConfig = config;
    }

    // --- Protected areas --------------------------------------------------

    public void loadProtectedAreas() {
        FileConfiguration config = getOrLoadTrialDataConfig();
        ConfigurationSection section = config.getConfigurationSection("");
        if (section == null || section.getKeys(false).isEmpty())
            return;

        ProtectedAreaManager areas = ProtectedAreaManager.getInstance();
        for (String key : section.getKeys(false)) {
            if (!config.contains(key + ".ProtectedArea"))
                continue;
            String minString = (String) config.get(key + ".ProtectedArea.Min");
            String maxString = (String) config.get(key + ".ProtectedArea.Max");
            if (minString == null || maxString == null)
                continue;
            String[] minLocation = minString.split(":");
            String[] maxLocation = maxString.split(":");
            if (minLocation.length != 3 || maxLocation.length != 3)
                continue;

            BoundingBox box = new BoundingBox();
            box.resize(Double.parseDouble(minLocation[0]), Double.parseDouble(minLocation[1]),
                    Double.parseDouble(minLocation[2]),
                    Double.parseDouble(maxLocation[0]), Double.parseDouble(maxLocation[1]),
                    Double.parseDouble(maxLocation[2]));

            String worldString = (String) config.get(key + ".ProtectedArea.World");
            if (worldString == null)
                continue;
            World world = Bukkit.getWorld(worldString);
            areas.putProtectedArea(UUID.fromString(key), new ProtectedArea(box, world));
        }
    }

public void saveAllAndShutdown(TrialRegistry registry) {
        // End active trials first so their state is flushed.
        if (registry.hasActiveTrials())
            for (Trial trial : new ArrayList<>(registry.getTrials()))
                trial.endTrial();

        // Preserve the original behaviour: with no protected areas there is nothing to persist.
        if (!ProtectedAreaManager.getInstance().hasProtectedAreas())
            return;

        saveProtectedAreas();
        saveTrialBuildingData(false);

        for (Map.Entry<org.bukkit.entity.Player, ArrayList<Integer>> entry : registry.getPlayerGamemodesBeaten().entrySet())
            TrialUtils.saveCompletedTrials(entry.getKey(), getOrLoadTrialDataConfig(), true);

        saveTrialDataFile("Failed to save protected areas to " + TRIAL_DATA_FILE);
    }

    private void saveProtectedAreas() {
        FileConfiguration config = getOrLoadTrialDataConfig();
        ProtectedAreaManager areas = ProtectedAreaManager.getInstance();
        for (Map.Entry<UUID, ProtectedArea> entry : areas.getProtectedAreas().entrySet()) {
            BoundingBox box = entry.getValue().boundingBox();
            String minLocation = box.getMinX() + ":" + box.getMinY() + ":" + box.getMinZ();
            String maxLocation = box.getMaxX() + ":" + box.getMaxY() + ":" + box.getMaxZ();
            String base = entry.getKey().toString() + ".ProtectedArea.";
            config.set(base + "Min", minLocation);
            config.set(base + "Max", maxLocation);
            // Save world name (not UUID) for reliability across reloads.
            config.set(base + "World", entry.getValue().world().getName());
        }
    }

    // --- Completed trials ------------------------------------------------

    public void loadCompletedTrials(org.bukkit.entity.Player p, TrialRegistry registry) {
        FileConfiguration config = getOrLoadTrialDataConfig();
        ArrayList<Integer> gamemodesBeaten = new ArrayList<>();

        if (config.contains(p.getUniqueId() + ".CompletedTrials")) {
            // Support both lists of integers and lists of strings.
            try {
                List<?> raw = config.getList(p.getUniqueId() + ".CompletedTrials");
                if (raw != null) {
                    for (Object o : raw) {
                        if (o instanceof Integer i)
                            gamemodesBeaten.add(i);
                        else if (o instanceof String s) {
                            try {
                                gamemodesBeaten.add(Integer.parseInt(s));
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Fallback - parse the string list directly.
                for (String gamemode : config.getStringList(p.getUniqueId() + ".CompletedTrials")) {
                    try {
                        gamemodesBeaten.add(Integer.parseInt(gamemode));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        registry.setCompletedGamemodes(p, gamemodesBeaten);
    }

    // --- Trial building ownership ---------------------------------------

    public void registerTrialBuilding(UUID userId, Location location) {
        if (trialBuildingOwnership.containsKey(userId)) {
            updateTrialUsage(userId);
            return;
        }
        trialBuildingOwnership.put(userId, new TrialBuildingData(userId, System.currentTimeMillis(), location));
    }

    public void updateTrialUsage(UUID trialBuildingId) {
        TrialBuildingData data = trialBuildingOwnership.get(trialBuildingId);
        if (data != null)
            trialBuildingOwnership.put(trialBuildingId,
                    new TrialBuildingData(data.owner(), System.currentTimeMillis(), data.location()));
    }

    public void removeTrialBuilding(UUID buildingId) {
        trialBuildingOwnership.remove(buildingId);
        getOrLoadTrialDataConfig().set(buildingId.toString(), null);
    }

    public void loadTrialBuildingData() {
        FileConfiguration config = getOrLoadTrialDataConfig();
        ConfigurationSection section = config.getConfigurationSection("");
        if (section == null)
            return;

        for (String key : section.getKeys(false)) {
            if (!config.contains(key + ".Owner"))
                continue;
            try {
                UUID buildingId = UUID.fromString(key);
                UUID owner = UUID.fromString(Objects.requireNonNull(config.getString(key + ".Owner")));
                long lastUsed = config.getLong(key + ".LastUsed", System.currentTimeMillis());
                String locationStr = config.getString(key + ".Location");
                if (locationStr != null) {
                    Location location = stringToLocation(locationStr);
                    if (location != null)
                        trialBuildingOwnership.put(buildingId, new TrialBuildingData(owner, lastUsed, location));
                }
            } catch (Exception e) {
                SurvivalSkills.getInstance().getLogger().warning("Failed to load trial building data for " + key);
            }
        }
    }

    public void saveTrialBuildingData(boolean saveFile) {
        FileConfiguration config = getOrLoadTrialDataConfig();

        // Remove building entries that are no longer owned, but keep player completed-trial
        // entries (identified by the absence of an ".Owner" child key).
        for (String key : new ArrayList<>(config.getKeys(false))) {
            if (config.contains(key + ".CompletedTrials"))
                continue;
            if (!config.contains(key + ".Owner"))
                continue;
            try {
                UUID uuid = UUID.fromString(key);
                if (!trialBuildingOwnership.containsKey(uuid))
                    config.set(key, null);
            } catch (IllegalArgumentException ignored) {
                // Not a UUID-shaped key; leave it alone.
            }
        }

        for (Map.Entry<UUID, TrialBuildingData> entry : trialBuildingOwnership.entrySet()) {
            String key = entry.getKey().toString();
            TrialBuildingData data = entry.getValue();
            config.set(key + ".Owner", data.owner().toString());
            config.set(key + ".LastUsed", data.lastUsed());
            config.set(key + ".Location", locationToString(data.location()));
        }

        if (saveFile)
            saveTrialDataFile("Failed to save trial building data");
    }

    public void saveCompletedTrialsOnQuit(org.bukkit.entity.Player p, TrialRegistry registry) {
        TrialUtils.saveCompletedTrials(p, getOrLoadTrialDataConfig(), false);
    }

    private void saveTrialDataFile(String failureMessage) {
        try {
            File file = new File(SurvivalSkills.getInstance().getDataFolder(), TRIAL_DATA_FILE);
            getOrLoadTrialDataConfig().save(file);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "[SurvivalSkills] " + failureMessage, e);
        }
    }

    private String locationToString(Location loc) {
        return Objects.requireNonNull(loc.getWorld()).getUID() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ();
    }

    private Location stringToLocation(String str) {
        String[] parts = str.split(":");
        if (parts.length != 4)
            return null;
        World world = Bukkit.getWorld(UUID.fromString(parts[0]));
        if (world == null)
            return null;
        return new Location(world, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
    }

    // --- Cleanup task ----------------------------------------------------

    public void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpiredTrialBuildings();
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), CLEANUP_INTERVAL, CLEANUP_INTERVAL);
    }

    private void cleanupExpiredTrialBuildings() {
        ArrayList<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, TrialBuildingData> entry : trialBuildingOwnership.entrySet()) {
            if (!entry.getValue().isExpired())
                continue;
            UUID buildingId = entry.getKey();
            TrialBuildingData buildingData = entry.getValue();
            Location buildingLocation = buildingData.location();

            if (buildingLocation.getWorld() == null) {
                Bukkit.getLogger().warning("[SurvivalSkills] World no longer exists for expired trial building owned by "
                        + Bukkit.getOfflinePlayer(buildingData.owner()).getName() + ". Removing from records only.");
                toRemove.add(buildingId);
                continue;
            }

            // Load the chunk so removal is safe, then clear blocks and the protected area.
            World world = buildingLocation.getWorld();
            int chunkX = buildingLocation.getBlockX() >> 4;
            int chunkZ = buildingLocation.getBlockZ() >> 4;
            if (!world.isChunkLoaded(chunkX, chunkZ))
                world.loadChunk(chunkX, chunkZ);

            ProtectedArea area = ProtectedAreaManager.getInstance().getProtectedArea(buildingId);
            if (area != null) {
                TrialUtils.removeProtectedArea(area);
                ProtectedAreaManager.getInstance().removeProtectedArea(buildingId);
            }
            toRemove.add(buildingId);
            Bukkit.getLogger().info("[SurvivalSkills] Removed expired trial building owned by "
                    + Bukkit.getOfflinePlayer(buildingData.owner()).getName());
        }

        for (UUID id : toRemove)
            trialBuildingOwnership.remove(id);
        if (!toRemove.isEmpty())
            saveTrialBuildingData(true);
    }

    }