package sir_draco.survivalskills.god_questline;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import sir_draco.survivalskills.TrialGUI;

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

    @EventHandler
    public void onUpgradeGUIClick(InventoryClickEvent event) {
        if (TrialGUI.getUpgradeInventories().contains(event.getView().getTopInventory())
                && !TrialGUI.getUpgradeInventories().contains(event.getInventory())) {
            event.setCancelled(true);
            return;
        }

        if (!TrialGUI.getUpgradeInventories().contains(event.getInventory())) return;

        event.setCancelled(true);
        TrialGUI.handleUpgradeClick(event);
    }

    @EventHandler
    public void onUpgradeGUIDrag(InventoryDragEvent event) {
        if (TrialGUI.getUpgradeInventories().contains(event.getView().getTopInventory())) {
            event.setCancelled(true);
            return;
        }
        if (!TrialGUI.getUpgradeInventories().contains(event.getInventory())) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onUpgradeGUIClose(InventoryCloseEvent event) {
        if (!TrialGUI.getUpgradeInventories().contains(event.getInventory())) return;
        TrialGUI.removeInventory(event.getInventory());
    }
}