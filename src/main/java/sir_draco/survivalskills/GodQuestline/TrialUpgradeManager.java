package sir_draco.survivalskills.GodQuestline;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TrialUpgradeManager implements Listener {

    private static final Map<UUID, PlayerTrialUpgrades> playerUpgrades = new HashMap<>();

    public static PlayerTrialUpgrades getPlayerUpgrades(Player player) {
        return getPlayerUpgrades(player.getUniqueId());
    }

    public static PlayerTrialUpgrades getPlayerUpgrades(UUID playerId) {
        if (!playerUpgrades.containsKey(playerId)) {
            PlayerTrialUpgrades upgrades = new PlayerTrialUpgrades(playerId);
            upgrades.loadFromFile();
            playerUpgrades.put(playerId, upgrades);
        }
        return playerUpgrades.get(playerId);
    }

    public static void awardTrialPoints(Player player, int points) {
        PlayerTrialUpgrades upgrades = getPlayerUpgrades(player);
        upgrades.addPoints(points);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Pre-load player upgrades
        getPlayerUpgrades(event.getPlayer());
    }
}