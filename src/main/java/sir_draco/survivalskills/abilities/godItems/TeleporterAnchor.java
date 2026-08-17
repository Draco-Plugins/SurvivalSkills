package sir_draco.survivalskills.abilities.godItems;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.items.ItemStackBuilder;

import java.util.*;

/**
 * TeleporterAnchor handles the placement and management of teleportation
 * anchors.
 * When placed, anchors allow players to teleport between different named
 * locations.
 */
public record TeleporterAnchor(String name, Location location, UUID ownerId) {

    public static final NamespacedKey GUI_ACTION_KEY = new NamespacedKey(SurvivalSkills.getInstance(), "gui_action");

    /**
     * Creates a new TeleporterAnchor instance
     *
     * @param name     The display name for this anchor
     * @param location The world location of the anchor
     * @param ownerId  The UUID of the player who placed this anchor
     */
    public TeleporterAnchor(String name, Location location, UUID ownerId) {
        this.name = name;
        this.location = location.clone();
        this.ownerId = ownerId;
    }

    /**
     * Teleports a player to this anchor's location safely
     *
     * @param player The player to teleport
     * @return true if teleportation was successful, false otherwise
     */
    public boolean teleportPlayer(Player player) {
        if (location.getWorld() == null) {
            player.sendMessage(ChatColor.RED + "The destination world is no longer available!");
            return false;
        }

        Location safeLocation = findSafeTeleportLocation(location.clone().add(0, 1, 0));
        if (safeLocation == null) {
            player.sendMessage(ChatColor.RED + "Cannot find a safe location near the anchor!");
            return false;
        }

        if (safeLocation.getWorld() == null) {
            player.sendMessage(ChatColor.RED + "The anchor's world is not loaded!");
            return false;
        }

        // Add teleportation effects
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);

        boolean success = player.teleport(safeLocation);

        if (success) {
            safeLocation.getWorld().playSound(safeLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
            safeLocation.getWorld().spawnParticle(Particle.PORTAL, safeLocation.add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0.1);
            player.sendMessage(ChatColor.GREEN + "Teleported to " + ChatColor.GOLD + name + ChatColor.GREEN + "!");
        }

        return success;
    }

    /**
     * Finds a safe location for teleportation near the anchor
     *
     * @param startLocation The starting location to search from
     * @return A safe location or null if none found
     */
    private Location findSafeTeleportLocation(Location startLocation) {
        Location searchLocation = startLocation.clone();

        // First try the exact location
        if (isSafeTeleportLocation(searchLocation)) {
            return searchLocation;
        }

        // Search in expanding radius around the anchor
        for (int radius = 1; radius <= 5; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) == radius || Math.abs(z) == radius) { // Only check perimeter
                        Location testLocation = startLocation.clone().add(x, 0, z);

                        // Try different Y levels
                        for (int y = 2; y >= -2; y--) {
                            Location finalLocation = testLocation.clone().add(0, y, 0);
                            if (isSafeTeleportLocation(finalLocation)) {
                                return finalLocation;
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Checks if a location is safe for teleportation
     *
     * @param location The location to check
     * @return true if the location is safe, false otherwise
     */
    private boolean isSafeTeleportLocation(Location location) {
        if (location.getWorld() == null)
            return false;

        Block feetBlock = location.getBlock();
        Block headBlock = location.getBlock().getRelative(0, 1, 0);
        Block groundBlock = location.getBlock().getRelative(0, -1, 0);

        // Check that feet and head blocks are not solid
        boolean feetClear = !feetBlock.getType().isSolid() && !feetBlock.isLiquid();
        boolean headClear = !headBlock.getType().isSolid() && !headBlock.isLiquid();

        // Check that there's solid ground below (or allow some liquids like water)
        boolean hasGround = groundBlock.getType().isSolid() || groundBlock.isLiquid();

        return feetClear && headClear && hasGround;
    }

    /**
     * Creates a GUI inventory showing available anchors for teleportation with
     * pagination support
     *
     * @param availableAnchors List of anchors the player can teleport to
     * @param player           The player viewing the GUI
     * @param page             The current page number (0-based)
     * @return The created inventory
     */
    public static Inventory createTeleporterGUI(List<TeleporterAnchor> availableAnchors, Player player, int page) {
        final int ITEMS_PER_PAGE = 45; // Reserve bottom row for navigation
        final int INVENTORY_SIZE = 54;

        // Calculate pagination
        int totalPages = Math.max(1, (int) Math.ceil((double) availableAnchors.size() / ITEMS_PER_PAGE));
        int startIndex = page * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, availableAnchors.size());

        // Create title with proper page information
        String title = ChatColor.DARK_PURPLE + "Teleporter Network " + ChatColor.GRAY + "(" + (page + 1) + "/"
                + totalPages + ")";

        // Ensure title doesn't exceed Minecraft's 32 character limit for inventory
        // titles
        if (title.length() > 32) {
            title = ChatColor.DARK_PURPLE + "Teleporters " + ChatColor.GRAY + "(" + (page + 1) + "/" + totalPages + ")";
        }

        Inventory gui = Bukkit.createInventory(null, INVENTORY_SIZE, title);

        // Add anchor items for current page
        int slotIndex = 0;
        for (int i = startIndex; i < endIndex && slotIndex < ITEMS_PER_PAGE; i++) {
            TeleporterAnchor anchor = availableAnchors.get(i);
            ItemStack anchorItem = createAnchorDisplayItem(anchor, player);
            gui.setItem(slotIndex, anchorItem);
            slotIndex++;
        }

        // Add navigation items in bottom row (slots 45-53)
        addNavigationItems(gui, page, totalPages);

        return gui;
    }

    /**
     * Adds navigation items to the GUI for pagination
     *
     * @param gui         The inventory to add navigation to
     * @param currentPage Current page number (0-based)
     * @param totalPages  Total number of pages
     */
    private static void addNavigationItems(Inventory gui, int currentPage, int totalPages) {
        // Previous page button
        if (currentPage > 0) {
            ItemStack prevButton = createNavigationButton(Material.ARROW, ChatColor.YELLOW + "Previous Page",
                    List.of(ChatColor.GRAY + "Go to page " + currentPage), "prev_page");
            gui.setItem(48, prevButton);
        }

        // Next page button
        if (currentPage < totalPages - 1) {
            ItemStack nextButton = createNavigationButton(Material.ARROW, ChatColor.YELLOW + "Next Page",
                    List.of(ChatColor.GRAY + "Go to page " + (currentPage + 2)), "next_page");
            gui.setItem(50, nextButton);
        }

        // Close button
        ItemStack closeButton = createNavigationButton(Material.BARRIER, ChatColor.RED + "Close",
                List.of(ChatColor.GRAY + "Close the teleporter menu"), "close");
        gui.setItem(49, closeButton);
    }

    /**
     * Creates a navigation button for the GUI
     *
     * @param material The material for the button
     * @param name     The display name
     * @param lore     The button lore
     * @param action   The action identifier
     * @return The created navigation button
     */
    private static ItemStack createNavigationButton(Material material, String name, List<String> lore, String action) {
        ItemStack button = new ItemStackBuilder(material, 1, name)
                .lore(new ArrayList<>(lore)).build();

        // Store action in persistent data for identification
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(GUI_ACTION_KEY, PersistentDataType.STRING, action);
            button.setItemMeta(meta);
        }

        return button;
    }

    /**
     * Creates an ItemStack representing an anchor in the GUI
     *
     * @param anchor The anchor to represent
     * @param viewer The player viewing the GUI
     * @return The created ItemStack
     */
    private static ItemStack createAnchorDisplayItem(TeleporterAnchor anchor, Player viewer) {
        ArrayList<String> lore = new ArrayList<>();
        if (anchor.location.getWorld() != null)
            lore.add(ChatColor.GRAY + "World: " + ChatColor.WHITE + anchor.location.getWorld().getEnvironment().name());
        lore.add(ChatColor.GRAY + "Location: " + ChatColor.WHITE + (int) anchor.location.getX() + ", "
                + (int) anchor.location.getY() + ", " + (int) anchor.location.getZ());

        World anchorWorld = anchor.location.getWorld();
        if (anchorWorld == null) {
            lore.add(ChatColor.GRAY + "Distance: " + ChatColor.WHITE + "Unavailable");
        } else if (!anchorWorld.equals(viewer.getWorld())) {
            lore.add(ChatColor.GRAY + "Distance: " + ChatColor.WHITE + "Different world");
        } else {
            double distance = viewer.getLocation().distance(anchor.location);
            lore.add(ChatColor.GRAY + "Distance: " + ChatColor.WHITE + String.format("%.1f blocks", distance));
        }
        lore.add("");
        lore.add(ChatColor.GREEN + "Click to teleport!");

        return new ItemStackBuilder(Material.END_PORTAL_FRAME, 1, ChatColor.GOLD + anchor.name)
                .lore(lore).build();
    }

    /**
     * Gets the action from a navigation button
     *
     * @param item The item to check
     * @return The action string, or null if not a navigation button
     */
    public static String getNavigationAction(ItemStack item) {
        if (item == null || item.getItemMeta() == null)
            return null;

        return item.getItemMeta().getPersistentDataContainer().get(GUI_ACTION_KEY, PersistentDataType.STRING);
    }

    public static boolean isCorrectAnchor(ItemStack item, TeleporterAnchor anchor) {
        // Extract the name from the anchor's ItemMeta
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName())
            return false;
        String displayName = ChatColor.stripColor(meta.getDisplayName());
        return displayName.equals(anchor.name());
    }

    public static void handleGUIClick(Player player, ItemStack clickedItem, List<TeleporterAnchor> anchors,
            int currentPage) {
        String action = getNavigationAction(clickedItem);
        if (action == null)
            return;

        switch (action) {
            case "prev_page":
                // Handle previous page logic
                if (currentPage > 0)
                    player.openInventory(createTeleporterGUI(anchors, player, currentPage - 1));
                break;
            case "next_page":
                // Handle next page logic
                if (currentPage < (int) Math.ceil((double) anchors.size() / 45) - 1)
                    player.openInventory(createTeleporterGUI(anchors, player, currentPage + 1));
                break;
            case "close":
                player.closeInventory();
                break;
            default:

        }
    }
}
