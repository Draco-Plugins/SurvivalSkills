package sir_draco.survivalskills.god_questline.trial;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Owns the spectator bookkeeping and the per-player trial/spectator scoreboards. Exposes focused
 * mutation methods instead of leaking the internal maps so callers cannot leave related state
 * half-updated (e.g. recording a spectator without a target).
 */
public class TrialSpectatorManager {

    private static final TrialSpectatorManager INSTANCE = new TrialSpectatorManager();

    private final Map<Player, Location> spectatingPlayers = new HashMap<>();
    private final Map<Player, Player> spectatorTargets = new HashMap<>();
    private final Map<Player, Scoreboard> trialScoreboards = new HashMap<>();
    private final Map<Player, Scoreboard> spectatorScoreboards = new HashMap<>();

    private TrialSpectatorManager() {
    }

    public static TrialSpectatorManager getInstance() {
        return INSTANCE;
    }

    // --- Spectators ------------------------------------------------------

    public void addSpectator(Player spectator, Player target, Location origin) {
        spectatingPlayers.put(spectator, origin);
        spectatorTargets.put(spectator, target);
    }

    /**
     * Removes the spectator, returning the location they should be teleported back to. Clears
     * both the origin and target entries atomically.
     */
    public Optional<Location> removeSpectator(Player spectator) {
        spectatorTargets.remove(spectator);
        Location origin = spectatingPlayers.remove(spectator);
        return Optional.ofNullable(origin);
    }

    public boolean isSpectating(Player player) {
        return spectatingPlayers.containsKey(player);
    }

    public Location getSpectatorOrigin(Player player) {
        return spectatingPlayers.get(player);
    }

    public Player getSpectatorTarget(Player player) {
        return spectatorTargets.get(player);
    }

    // --- Trial scoreboards ----------------------------------------------

    public void setTrialScoreboard(Player player, Scoreboard scoreboard) {
        trialScoreboards.put(player, scoreboard);
    }

    public Scoreboard getTrialScoreboard(Player player) {
        return trialScoreboards.get(player);
    }

    public void removeTrialScoreboard(Player player) {
        trialScoreboards.remove(player);
    }

    // --- Spectator scoreboards ------------------------------------------

    public void setSpectatorScoreboard(Player player, Scoreboard scoreboard) {
        spectatorScoreboards.put(player, scoreboard);
    }

    public Scoreboard getSpectatorScoreboard(Player player) {
        return spectatorScoreboards.get(player);
    }

    public void removeSpectatorScoreboard(Player player) {
        spectatorScoreboards.remove(player);
    }
}