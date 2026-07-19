package sir_draco.survivalskills.god_questline.trial;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Owns the activeTrial collection of active {@link Trial}s and the pending trials, and maintains
 * an O(1) index from each player to the trial they are currently participating in. State is
 * mutated exclusively through the focused methods below so that lookups stay consistent and
 * callers never touch the underlying collections directly.
 */
public class TrialRegistry {

    private static final TrialRegistry INSTANCE = new TrialRegistry();

    private final List<Trial> trials = new ArrayList<>();
    private final Map<Player, Trial> playerTrialIndex = new HashMap<>();
    private final Map<Player, PendingTrial> pendingTrials = new HashMap<>();
    private final Map<UUID, ArrayList<Integer>> playerGamemodesBeaten = new HashMap<>();
    private final Set<Inventory> trialSelectionInventories = new HashSet<>();

    private TrialRegistry() {
    }

    public static TrialRegistry getInstance() {
        return INSTANCE;
    }

    // --- Active trials ----------------------------------------------------

    public void registerTrial(Trial trial) {
        trials.add(trial);
        for (Player p : trial.getPlayers())
            playerTrialIndex.put(p, trial);
    }

    public void unregisterTrial(Trial trial) {
        trials.remove(trial);
        for (Player p : trial.getPlayers())
            if (playerTrialIndex.get(p) == trial)
                playerTrialIndex.remove(p);
    }

    /** Removes a single player from the index when they leave a trial without unregistering it. */
    public void removePlayerFromIndex(Player p) {
        playerTrialIndex.remove(p);
    }

    public Optional<Trial> getPlayerTrial(Player p) {
        return Optional.ofNullable(playerTrialIndex.get(p));
    }

    public boolean isInTrial(Player p) {
        return playerTrialIndex.containsKey(p);
    }

    public List<Trial> getTrials() {
        return Collections.unmodifiableList(trials);
    }

    public boolean hasActiveTrials() {
        return !trials.isEmpty();
    }

    // --- Pending trials ---------------------------------------------------

    public void putPendingTrial(Player master, PendingTrial trial) {
        PendingTrial previousTrial = pendingTrials.put(master, trial);
        if (previousTrial == null || previousTrial == trial)
            return;
        previousTrial.endPendingTrial();
        previousTrial.dispose();
    }

    public PendingTrial getPendingTrial(Player master) {
        return pendingTrials.get(master);
    }

    public boolean hasPendingTrial(Player master) {
        return pendingTrials.containsKey(master);
    }

    public void removePendingTrial(Player master) {
        PendingTrial pendingTrial = pendingTrials.remove(master);
        if (pendingTrial != null)
            pendingTrial.dispose();
    }

    public Map<Player, PendingTrial> getPendingTrials() {
        return Collections.unmodifiableMap(pendingTrials);
    }

    // --- Completed gamemodes ---------------------------------------------

    public void addCompletedGamemode(Player player, int difficulty) {
        ArrayList<Integer> modes = playerGamemodesBeaten.computeIfAbsent(
                player.getUniqueId(), (UUID key) -> new ArrayList<>());
        if (!modes.contains(difficulty))
            modes.add(difficulty);
    }

    public ArrayList<Integer> getCompletedGamemodes(Player player) {
        return playerGamemodesBeaten.get(player.getUniqueId());
    }

    public boolean hasCompletedGamemodes(Player player) {
        return playerGamemodesBeaten.containsKey(player.getUniqueId());
    }

    public Map<UUID, ArrayList<Integer>> getPlayerGamemodesBeaten() {
        return Collections.unmodifiableMap(playerGamemodesBeaten);
    }

    public void setCompletedGamemodes(Player player, ArrayList<Integer> gamemodes) {
        playerGamemodesBeaten.put(player.getUniqueId(), gamemodes);
    }

    /** Removes all trial-owned references to a player who is leaving the server. */
    public void cleanupPlayer(Player player) {
        playerTrialIndex.remove(player);
        playerGamemodesBeaten.remove(player.getUniqueId());

        PendingTrial ownedTrial = pendingTrials.remove(player);
        if (ownedTrial != null) {
            ownedTrial.endPendingTrial();
            ownedTrial.dispose();
        }

        pendingTrials.values().forEach(
                (PendingTrial pendingTrial) -> pendingTrial.removeDisconnectedPlayer(player));
    }

    // --- Trial selection inventories ------------------------------------

    public void registerTrialSelectionInventory(Inventory inv) {
        trialSelectionInventories.add(inv);
    }

    public void unregisterTrialSelectionInventory(Inventory inv) {
        trialSelectionInventories.remove(inv);
    }

    public boolean isTrialSelectionInventory(Inventory inv) {
        return trialSelectionInventories.contains(inv);
    }

    /** Clears all session-only state after it has been persisted during plugin shutdown. */
    public void clearAll() {
        pendingTrials.values().forEach((PendingTrial pendingTrial) -> pendingTrial.dispose());
        pendingTrials.clear();
        playerTrialIndex.clear();
        playerGamemodesBeaten.clear();
        trialSelectionInventories.clear();
        trials.clear();
    }
}
