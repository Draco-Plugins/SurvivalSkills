package sir_draco.survivalskills.god_questline.trial;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TrialUpgradeManager implements Listener {

    private static final Map<UUID, PlayerTrialUpgrades> playerUpgrades = new HashMap<>();

    /**
     * Safe public accessor: retrieves player upgrades, loading from disk if
     * not yet in the cache.
     */
    public static PlayerTrialUpgrades getPlayerUpgrades(Player player) {
        return loadPlayer(player.getUniqueId());
    }

    /**
     * Cache-only lookup. Returns null if the player's data hasn't been loaded yet.
     * Most callers should use {@link #loadPlayer(Player)} for safe access.
     */
    public static PlayerTrialUpgrades getPlayerUpgrades(UUID playerId) {
        return playerUpgrades.get(playerId);
    }

    /**
     * Explicitly loads a player's upgrade data from disk into the cache.
     */
    public static PlayerTrialUpgrades loadPlayer(Player player) {
        return loadPlayer(player.getUniqueId());
    }

    /**
     * Explicitly loads a player's upgrade data from disk into the cache.
     */
    public static PlayerTrialUpgrades loadPlayer(UUID playerId) {
        if (playerUpgrades.containsKey(playerId)) {
            return playerUpgrades.get(playerId);
        }
        PlayerTrialUpgrades upgrades = new PlayerTrialUpgrades(playerId);
        upgrades.loadFromFile();
        playerUpgrades.put(playerId, upgrades);
        return upgrades;
    }

    public static void awardTrialPoints(Player player, int points) {
        PlayerTrialUpgrades upgrades = loadPlayer(player);
        upgrades.addPoints(points);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        loadPlayer(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        PlayerTrialUpgrades upgrades = playerUpgrades.get(playerId);
        if (upgrades != null) {
            upgrades.saveToFile();
            playerUpgrades.remove(playerId);
        }
    }

    @EventHandler
    public void onUpgradeGUIClick(InventoryClickEvent event) {
        if (!TrialGUI.isUpgradeInventory(event.getInventory()))
            return;

        // If the upgrade GUI is showing but they clicked their own inventory, block the interaction
        if (event.getInventory() != event.getView().getTopInventory()) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);
        TrialGUI.handleUpgradeClick(event);
    }

    @EventHandler
    public void onUpgradeGUIDrag(InventoryDragEvent event) {
        if (TrialGUI.isUpgradeInventory(event.getInventory())
                || TrialGUI.isUpgradeInventory(event.getView().getTopInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onUpgradeGUIClose(InventoryCloseEvent event) {
        if (!TrialGUI.isUpgradeInventory(event.getInventory()))
            return;
        TrialGUI.removeInventory(event.getInventory());

        // The purchase round is over: reset leftover points and persist upgrades
        if (!(event.getPlayer() instanceof Player player))
            return;
        PlayerTrialUpgrades upgrades = loadPlayer(player);
        upgrades.resetPoints();
        upgrades.saveToFile();
    }
}