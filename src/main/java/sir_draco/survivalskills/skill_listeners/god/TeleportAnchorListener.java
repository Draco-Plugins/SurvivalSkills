package sir_draco.survivalskills.skill_listeners.god;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.abilities.godItems.TeleporterAnchor;
import sir_draco.survivalskills.utils.FileUtils;
import sir_draco.survivalskills.utils.Utils;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages teleport anchor placement, breaking, naming (via a temporary chat
 * prompt) and the teleporter GUI.
 */
public class TeleportAnchorListener implements Listener {

    // Teleport anchor item
    private static final int MODEL_TELEPORT_ANCHOR = 55;

    // Anchor naming prompt
    private static final int ANCHOR_NAME_TIMEOUT_SECONDS = 30;
    private static final long TICKS_PER_SECOND = 20L;
    private static final int ANCHOR_NAME_MAX_LENGTH = 32;
    private static final String ANCHOR_NAME_CANCEL = "cancel";

    private final Map<Location, TeleporterAnchor> teleportAnchors = new HashMap<>();
    private final Map<Player, Integer> teleportGUIPage = new HashMap<>();

    public TeleportAnchorListener() {
        FileUtils.loadTeleportAnchors(teleportAnchors);
    }

    // --- Block break / place ---

    @EventHandler
    public void onBreakBlock(BlockBreakEvent e) {
        Location loc = e.getBlock().getLocation();
        if (!Material.RESPAWN_ANCHOR.equals(e.getBlock().getType()) || !teleportAnchors.containsKey(loc))
            return;

        TeleporterAnchor anchor = teleportAnchors.get(loc);
        Player p = e.getPlayer();
        if (!anchor.ownerId().equals(p.getUniqueId())) {
            e.setCancelled(true);
            p.sendMessage(ChatColor.RED + "You can only break your own teleport anchors!");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }

        teleportAnchors.remove(loc);
        e.setDropItems(false);
        if (loc.getWorld() == null)
            return;
        loc.getWorld().dropItemNaturally(loc, sir_draco.survivalskills.utils.items.ItemStackGenerator.getTeleportAnchor());
        p.sendMessage(ChatColor.YELLOW + "Teleport anchor '" + anchor.name() + "' removed!");
    }

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent e) {
        ItemStack item = e.getItemInHand();
        if (!ItemStackGeneratorUtils.isCustomItem(item, MODEL_TELEPORT_ANCHOR))
            return;

        Player p = e.getPlayer();
        if (SurvivalSkills.getInstance().isGriefPreventionEnabled()
                && Utils.checkForClaim(p, e.getBlock().getLocation())) {
            p.sendMessage(ChatColor.RED + "You cannot place teleport anchors in someone else's claim!");
            e.setCancelled(true);
            return;
        }

        if (SurvivalSkills.getInstance().isWorldGuardEnabled()) {
            boolean canPlace = SurvivalSkills.getInstance().getWorldGuardProvider()
                    .canPlaceBlockInRegion(p, e.getBlock().getLocation());
            if (!canPlace && !p.hasPermission("survivalskills.op")) {
                p.sendMessage(ChatColor.RED + "You cannot place teleport anchors in this region!");
                e.setCancelled(true);
                return;
            }
        }

        p.sendMessage(ChatColor.GREEN + "Please type a name for this teleport anchor in chat:");
        p.sendMessage(ChatColor.GRAY + "Type 'cancel' to cancel placement.");

        Location placementLoc = e.getBlock().getLocation();
        SurvivalSkills.getInstance().getServer()
                .getScheduler()
                .runTaskLater(SurvivalSkills.getInstance(), () -> promptForAnchorName(p, placementLoc), 1L);
    }

    // --- Anchor interaction ---

    @EventHandler
    public void onRightClickTeleportAnchor(PlayerInteractEvent e) {
        if (!Action.RIGHT_CLICK_BLOCK.equals(e.getAction()))
            return;
        if (e.getHand() == null || !EquipmentSlot.HAND.equals(e.getHand()))
            return;
        if (e.getClickedBlock() == null)
            return;
        if (!Material.RESPAWN_ANCHOR.equals(e.getClickedBlock().getType()))
            return;

        Player p = e.getPlayer();
        Location location = e.getClickedBlock().getLocation();
        if (!teleportAnchors.containsKey(location))
            return;

        e.setCancelled(true);

        List<TeleporterAnchor> availableAnchors = collectAccessibleAnchors(p);
        if (availableAnchors.isEmpty()) {
            p.sendMessage(ChatColor.RED + "No teleport anchors available!");
            return;
        }

        teleportGUIPage.put(p, 0);
        Inventory gui = TeleporterAnchor.createTeleporterGUI(availableAnchors, p, 0);
        p.openInventory(gui);
    }

    private List<TeleporterAnchor> collectAccessibleAnchors(Player p) {
        List<TeleporterAnchor> availableAnchors = new ArrayList<>();
        for (TeleporterAnchor anchor : teleportAnchors.values()) {
            if (anchor.ownerId().equals(p.getUniqueId())
                    || (anchor.location().getWorld() != null
                            && anchor.location().getWorld().equals(p.getWorld()))) {
                availableAnchors.add(anchor);
            }
        }
        return availableAnchors;
    }

    // --- Teleporter GUI ---

    @EventHandler
    public void onGUIDrag(InventoryDragEvent e) {
        if (!e.getView().getTitle().contains("Teleporter Network"))
            return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onGUIClick(InventoryClickEvent e) {
        if (!e.getView().getTitle().contains("Teleporter Network"))
            return;

        e.setCancelled(true);

        ItemStack clickedItem = e.getCurrentItem();
        if (clickedItem == null)
            return;

        Player p = (Player) e.getWhoClicked();
        int currentPage = teleportGUIPage.getOrDefault(p, 0);

        // Navigation buttons are not end portal frames - leave them to the
        // generic handler. Otherwise the click targets an anchor: teleport to
        // the matching one and stop handling.
        if (Material.END_PORTAL_FRAME.equals(clickedItem.getType())) {
            for (TeleporterAnchor anchor : teleportAnchors.values()) {
                if (!TeleporterAnchor.isCorrectAnchor(clickedItem, anchor))
                    continue;
                teleportToAnchor(p, anchor);
                return;
            }
            return;
        }

        List<TeleporterAnchor> availableAnchors = collectAccessibleAnchors(p);
        TeleporterAnchor.handleGUIClick(p, clickedItem, availableAnchors, currentPage);
    }

    private void teleportToAnchor(Player p, TeleporterAnchor anchor) {
        if (anchor.teleportPlayer(p)) {
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        } else {
            p.sendMessage(ChatColor.RED + "Failed to teleport to anchor!");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        }
    }

    @EventHandler
    public void onGUIClose(InventoryCloseEvent e) {
        if (!e.getView().getTitle().contains("Teleporter Network"))
            return;
        teleportGUIPage.remove((Player) e.getPlayer());
    }

    // --- Anchor naming prompt ---

    /**
     * Prompts a player to name their teleport anchor via chat input, with a
     * timeout and disconnect/quit cleanup.
     */
    private void promptForAnchorName(Player player, Location location) {
        final SurvivalSkills plugin = SurvivalSkills.getInstance();
        final AnchorNamePrompt prompt = new AnchorNamePrompt(player, location);
        plugin.getServer().getPluginManager().registerEvents(prompt, plugin);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!prompt.isHandled()) {
                prompt.unregister();
                player.sendMessage(ChatColor.YELLOW + "Teleport anchor naming timed out.");
            }
        }, TICKS_PER_SECOND * ANCHOR_NAME_TIMEOUT_SECONDS);
    }

    /**
     * Self-unregistering listener that captures a single chat message to name
     * a teleport anchor, plus a quit handler for cleanup.
     */
    private final class AnchorNamePrompt implements Listener {
        private final Player player;
        private final Location location;
        private boolean handled = false;

        AnchorNamePrompt(Player player, Location location) {
            this.player = player;
            this.location = location;
        }

        boolean isHandled() {
            return handled;
        }

        synchronized void unregister() {
            if (handled)
                return;
            handled = true;
            AsyncPlayerChatEvent.getHandlerList().unregister(this);
            PlayerQuitEvent.getHandlerList().unregister(this);
        }

        @EventHandler
        public void onPlayerChat(AsyncPlayerChatEvent e) {
            if (!e.getPlayer().equals(player))
                return;

            e.setCancelled(true);
            String input = e.getMessage().trim();
            unregister();

            if (input.equalsIgnoreCase(ANCHOR_NAME_CANCEL)) {
                player.sendMessage(ChatColor.YELLOW + "Teleport anchor placement cancelled.");
                return;
            }
            if (input.isEmpty() || input.length() > ANCHOR_NAME_MAX_LENGTH) {
                player.sendMessage(ChatColor.RED + "Anchor name must be between 1 and 32 characters!");
                return;
            }
            for (TeleporterAnchor existingAnchor : teleportAnchors.values()) {
                if (!existingAnchor.name().equalsIgnoreCase(input))
                    continue;
                player.sendMessage(ChatColor.RED + "Another anchor with that name already exists!");
                return;
            }

            Bukkit.getScheduler().runTask(SurvivalSkills.getInstance(), () -> placeAnchor(player, location, input));
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent e) {
            if (e.getPlayer().equals(player))
                unregister();
        }
    }

    private void placeAnchor(Player player, Location location, String name) {
        if (location.getWorld() == null) {
            player.sendMessage(ChatColor.RED + "Invalid world for anchor placement!");
            return;
        }

        TeleporterAnchor anchor = new TeleporterAnchor(name, location, player.getUniqueId());
        teleportAnchors.put(location, anchor);

        player.sendMessage(ChatColor.GREEN + "Teleport anchor '" + name + "' placed successfully!");
        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 1.0f, 1.0f);
        location.getWorld().spawnParticle(Particle.PORTAL, location.clone().add(0.5, 1, 0.5),
                20, 0.3, 0.3, 0.3, 0.1);
    }

    // --- Accessors ---

    /** Unmodifiable view for persistence; anchor state is mutated only via events. */
    public Map<Location, TeleporterAnchor> getTeleportAnchors() {
        return Collections.unmodifiableMap(teleportAnchors);
    }
}