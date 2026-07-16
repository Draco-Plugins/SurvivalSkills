package sir_draco.survivalskills.god_questline.trial;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Scoreboard;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.god_questline.trial_mobs.TrialBoss;
import sir_draco.survivalskills.god_questline.trial_mobs.WaveMob;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Backwards-compatible façade for the trial system. Real state and behaviour live in the focused
 * managers ({@link TrialRegistry}, {@link ProtectedAreaManager}, {@link TrialDataPersistence},
 * {@link TrialRewardManager}, {@link TrialSpectatorManager}) and the {@link TrialEventListener}.
 * The static methods here simply delegate so existing callers keep working while gaining
 * encapsulated, focused mutation points.
 */
public final class TrialManager {

    private static final WaveGenerator waveGenerator = new WaveGenerator();

    private TrialManager() {
    }

    /** Runs once at plugin enable to build reward tables and load persisted data. */
    public static void initialize() {
        TrialRewardManager.getInstance().loadRewards();
        TrialDataPersistence.getInstance().loadTrialBuildingData();
        TrialDataPersistence.getInstance().startCleanupTask();
    }

    // --- Trial registry delegation ---------------------------------------

    public static boolean isTrialPlayer(Player p) {
        return TrialRegistry.getInstance().isInTrial(p);
    }

    public static boolean isInTrial(Player p) {
        return TrialRegistry.getInstance().isInTrial(p);
    }

    public static List<Trial> getTrials() {
        return TrialRegistry.getInstance().getTrials();
    }

    public static void unregisterTrial(Trial trial) {
        TrialRegistry.getInstance().unregisterTrial(trial);
    }

    public static void registerTrial(Trial trial) {
        TrialRegistry.getInstance().registerTrial(trial);
    }

    /** Removes a participant from the player index when they leave a trial that stays active. */
    public static void removePlayerFromTrialIndex(Player p) {
        TrialRegistry.getInstance().removePlayerFromIndex(p);
    }

    public static void addCompletedGamemode(Player player, int difficulty) {
        TrialRegistry.getInstance().addCompletedGamemode(player, difficulty);
    }

    public static ArrayList<Integer> getCompletedGamemodes(Player player) {
        return TrialRegistry.getInstance().getCompletedGamemodes(player);
    }

    public static boolean hasCompletedGamemodes(Player player) {
        return TrialRegistry.getInstance().hasCompletedGamemodes(player);
    }

    // --- Pending trials --------------------------------------------------

    public static void putPendingTrial(Player master, PendingTrial trial) {
        TrialRegistry.getInstance().putPendingTrial(master, trial);
    }

    public static PendingTrial getPendingTrial(Player master) {
        return TrialRegistry.getInstance().getPendingTrial(master);
    }

    public static boolean hasPendingTrial(Player master) {
        return TrialRegistry.getInstance().hasPendingTrial(master);
    }

    public static void removePendingTrial(Player master) {
        TrialRegistry.getInstance().removePendingTrial(master);
    }

    public static Map<Player, PendingTrial> getPendingTrials() {
        return TrialRegistry.getInstance().getPendingTrials();
    }

    // --- Trial selection inventories ------------------------------------

    public static void registerTrialSelectionInventory(Inventory inv) {
        TrialRegistry.getInstance().registerTrialSelectionInventory(inv);
    }

    public static void unregisterTrialSelectionInventory(Inventory inv) {
        TrialRegistry.getInstance().unregisterTrialSelectionInventory(inv);
    }

    public static boolean isTrialSelectionInventory(Inventory inv) {
        return TrialRegistry.getInstance().isTrialSelectionInventory(inv);
    }

    // --- Protected areas -------------------------------------------------

    public static void putProtectedArea(UUID ownerId, ProtectedArea area) {
        ProtectedAreaManager.getInstance().putProtectedArea(ownerId, area);
    }

    public static ProtectedArea getProtectedArea(UUID ownerId) {
        return ProtectedAreaManager.getInstance().getProtectedArea(ownerId);
    }

    public static boolean hasProtectedArea(UUID ownerId) {
        return ProtectedAreaManager.getInstance().hasProtectedArea(ownerId);
    }

    public static void removeProtectedArea(UUID ownerId) {
        ProtectedAreaManager.getInstance().removeProtectedArea(ownerId);
    }

    public static Map<UUID, ProtectedArea> getProtectedAreas() {
        return ProtectedAreaManager.getInstance().getProtectedAreas();
    }

    public static boolean isLocationProtected(Location location) {
        return ProtectedAreaManager.getInstance().isLocationProtected(location);
    }

    public static Long getBuildingCreationCooldown(UUID ownerId) {
        return ProtectedAreaManager.getInstance().getBuildingCreationCooldown(ownerId);
    }

    public static void setBuildingCreationCooldown(UUID ownerId, long timestamp) {
        ProtectedAreaManager.getInstance().setBuildingCreationCooldown(ownerId, timestamp);
    }

    // --- Spectator + scoreboards ---------------------------------------

    public static void addSpectator(Player spectator, Player target, Location origin) {
        TrialSpectatorManager.getInstance().addSpectator(spectator, target, origin);
    }

    public static Optional<Location> removeSpectator(Player spectator) {
        return TrialSpectatorManager.getInstance().removeSpectator(spectator);
    }

    public static boolean isSpectating(Player player) {
        return TrialSpectatorManager.getInstance().isSpectating(player);
    }

    public static Location getSpectatorOrigin(Player player) {
        return TrialSpectatorManager.getInstance().getSpectatorOrigin(player);
    }

    public static Player getSpectatorTarget(Player player) {
        return TrialSpectatorManager.getInstance().getSpectatorTarget(player);
    }

    public static void setTrialScoreboard(Player player, Scoreboard scoreboard) {
        TrialSpectatorManager.getInstance().setTrialScoreboard(player, scoreboard);
    }

    public static Scoreboard getTrialScoreboard(Player player) {
        return TrialSpectatorManager.getInstance().getTrialScoreboard(player);
    }

    public static void removeTrialScoreboard(Player player) {
        TrialSpectatorManager.getInstance().removeTrialScoreboard(player);
    }

    public static void setSpectatorScoreboard(Player player, Scoreboard scoreboard) {
        TrialSpectatorManager.getInstance().setSpectatorScoreboard(player, scoreboard);
    }

    public static Scoreboard getSpectatorScoreboard(Player player) {
        return TrialSpectatorManager.getInstance().getSpectatorScoreboard(player);
    }

    // --- Persistence -----------------------------------------------------

    public static void loadProtectedAreas() {
        TrialDataPersistence.getInstance().loadProtectedAreas();
    }

    public static void loadCompletedTrials(Player p) {
        TrialDataPersistence.getInstance().loadCompletedTrials(p, TrialRegistry.getInstance());
    }

    public static void handleTrials() {
        TrialDataPersistence.getInstance().saveAllAndShutdown(TrialRegistry.getInstance());
    }

    public static void registerTrialBuilding(UUID userId, Location location) {
        TrialDataPersistence.getInstance().registerTrialBuilding(userId, location);
    }

    public static void removeTrialBuilding(UUID buildingId) {
        TrialDataPersistence.getInstance().removeTrialBuilding(buildingId);
    }

    public static FileConfiguration getTrialBuildingConfig() {
        return TrialDataPersistence.getInstance().getOrLoadTrialBuildingConfig();
    }

    public static void setTrialBuildingConfig(FileConfiguration config) {
        TrialDataPersistence.getInstance().setTrialBuildingConfig(config);
    }

    // --- Rewards ---------------------------------------------------------

    public static void openRewardGUI(Player p, int wave) {
        TrialRewardManager.getInstance().openRewardGUI(p, wave);
    }

    public static ItemStack getTrialItem(Material material, int amount) {
        return TrialRewardManager.getInstance().getTrialItem(material, amount);
    }

    public static NamespacedKey getTrialObjectKey() {
        return TrialRewardManager.getInstance().getTrialObjectKey();
    }

    // --- Wave spawning (stateless helpers) ------------------------------

    public static WaveGenerator getWaveGenerator() {
        return waveGenerator;
    }

    public static Wave spawnWave(Wave wave, ArrayList<Location> spawningSpots, ArrayList<Player> players, int playerCount) {
        if (spawningSpots == null || spawningSpots.isEmpty()) {
            SurvivalSkills.getInstance().getLogger().warning("No spawning spots provided for wave " + wave);
            return null;
        }
        if (wave == null)
            return null;
        Wave waveObject = wave.duplicate();
        if (waveObject == null) {
            SurvivalSkills.getInstance().getLogger().warning("Wave " + wave + " does not exist");
            return null;
        }

        if (waveObject.isBossWave()) {
            TrialBoss boss = waveObject.getBoss();
            boss.setSpawnLocations(spawningSpots);
            boss.spawn(spawningSpots.get((int) (Math.random() * spawningSpots.size())));
            boss.scaleHealth(playerCount);
            boss.startScript();
            return waveObject;
        }

        if (playerCount > 1)
            waveObject.scaleWaveMobs(playerCount);

        for (WaveMob waveMob : waveObject.getWaveMobs())
            waveObject.spawnMob(getRandomSpawningSpot(spawningSpots), waveMob, players);

        return waveObject;
    }

    public static Location getRandomSpawningSpot(ArrayList<Location> spawningSpots) {
        return spawningSpots.get((int) (Math.random() * spawningSpots.size()));
    }
}