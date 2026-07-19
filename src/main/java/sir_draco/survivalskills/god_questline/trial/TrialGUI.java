package sir_draco.survivalskills.god_questline.trial;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import sir_draco.survivalskills.utils.ItemNameUtils;

import java.util.*;

public final class TrialGUI {

    private static final int POINTS_DISPLAY_SLOT = 4;
    private static final Map<UUID, Inventory> playerInventories = new HashMap<>();
    private static final Map<Inventory, Map<Integer, String>> upgradeSlotMappings = new HashMap<>();

    private TrialGUI() {
    }

    public static void openUpgradeGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_PURPLE + "Trial Upgrades");

        ItemStack border = createBorderItem();
        if (border == null) {
            return;
        }

        for (int i = 0; i < 9; i++) {
            if (i == POINTS_DISPLAY_SLOT) {
                continue;
            }
            inv.setItem(i, border);
        }
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, border);
        }
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, border);
            inv.setItem(i + 8, border);
        }

        Inventory previousInventory = playerInventories.put(player.getUniqueId(), inv);
        if (previousInventory != null)
            upgradeSlotMappings.remove(previousInventory);
        player.openInventory(inv);
        populateUpgradeInventory(inv, player);
    }

    /**
     * Refreshes the upgrade items in-place so the player can upgrade multiple
     * items without closing and reopening the inventory. This allows the close
     * event to act as a reliable "round of upgrading is done" signal.
     */
    public static void refreshUpgradeGUI(Player player) {
        Inventory inv = playerInventories.get(player.getUniqueId());
        if (inv == null) {
            return;
        }
        populateUpgradeInventory(inv, player);
    }

    // --- Border decoration ---

    private static ItemStack createBorderItem() {
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        if (meta == null) {
            return null;
        }
        meta.setDisplayName(" ");
        border.setItemMeta(meta);
        return border;
    }

    // --- Inventory population ---

    private static void populateUpgradeInventory(Inventory inv, Player player) {
        PlayerTrialUpgrades playerUpgrades = TrialUpgradeManager.getPlayerUpgrades(player);
        Map<String, TrialTree.TrialUpgrade> allUpgrades = TrialTree.getAllUpgrades();

        inv.setItem(POINTS_DISPLAY_SLOT, createPointsDisplay(playerUpgrades.getAvailablePoints()));

        // Build fresh slot -> upgradeId mapping for this inventory
        Map<Integer, String> slotMapping = new HashMap<>();
        upgradeSlotMappings.put(inv, slotMapping);

        final int[] upgradeSlots = {19, 21, 23, 25, 28, 30, 32, 34};
        int slotIndex = 0;

        for (TrialTree.TrialUpgrade upgrade : allUpgrades.values()) {
            if (slotIndex >= upgradeSlots.length) {
                break;
            }

            final int slot = upgradeSlots[slotIndex];
            if (upgrade != null) {
                ItemStack upgradeItem = createUpgradeItem(upgrade, playerUpgrades);
                inv.setItem(slot, upgradeItem);
                slotMapping.put(slot, upgrade.getId());
            } else {
                inv.setItem(slot, createLockedSlotItem());
            }
            slotIndex++;
        }

        // Fill any remaining empty slots
        while (slotIndex < upgradeSlots.length) {
            inv.setItem(upgradeSlots[slotIndex], createLockedSlotItem());
            slotIndex++;
        }
    }

    private static ItemStack createPointsDisplay(int availablePoints) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(ChatColor.GOLD + "Trial Points: " + ChatColor.YELLOW + availablePoints);
        meta.setLore(List.of(ChatColor.GRAY + "Available to spend on upgrades"));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createLockedSlotItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(ChatColor.RED + "Locked");
        item.setItemMeta(meta);
        return item;
    }

    // --- Upgrade item rendering ---

    private static ItemStack createUpgradeItem(TrialTree.TrialUpgrade upgrade, PlayerTrialUpgrades playerUpgrades) {
        final int currentLevel = playerUpgrades.getUpgradeLevel(upgrade.getId());
        final Material material = getUpgradeMaterial(upgrade.getType());

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        final String levelDisplay = currentLevel > 0
                ? " §7(Level " + currentLevel + "/" + upgrade.getMaxLevel() + ")"
                : "";
        meta.setDisplayName(ChatColor.AQUA + upgrade.getName() + levelDisplay);

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + upgrade.getDescription());
        lore.add("");

        if (currentLevel < upgrade.getMaxLevel()) {
            final int nextLevel = currentLevel + 1;
            final int cost = upgrade.getCost(nextLevel);

            lore.add(ChatColor.YELLOW + "Next Level Benefits:");
            lore.addAll(getUpgradeBenefits(upgrade, nextLevel));
            lore.add("");
            lore.add(ChatColor.GOLD + "Cost: " + cost + " points");

            if (playerUpgrades.getAvailablePoints() >= cost) {
                lore.add(ChatColor.GREEN + "Click to upgrade!");
            } else {
                lore.add(ChatColor.RED + "Not enough points!");
            }
        } else {
            lore.add(ChatColor.GREEN + "✓ Max Level Reached");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // --- Material mapping ---

    private static Material getUpgradeMaterial(TrialTree.UpgradeType type) {
        return switch (type) {
            case STARTING_WEAPON -> Material.DIAMOND_SWORD;
            case STARTING_ARMOR -> Material.DIAMOND_CHESTPLATE;
            case STARTING_FOOD -> Material.COOKED_BEEF;
            case ARROW_QUANTITY -> Material.ARROW;
            case DAMAGE_BOOST -> Material.BLAZE_POWDER;
            case SPEED_BOOST -> Material.SUGAR;
            case DODGE_CHANCE -> Material.FEATHER;
            case ENCHANT_CHANCE -> Material.ENCHANTED_BOOK;
        };
    }

    // --- Benefit text generation (no more unchecked casts) ---

    private static List<String> getUpgradeBenefits(TrialTree.TrialUpgrade upgrade, int level) {
        List<String> benefits = new ArrayList<>();

        switch (upgrade.getType()) {
            case STARTING_WEAPON -> {
                List<Material> materials = upgrade.getMaterialList("materials");
                if (!materials.isEmpty()) {
                    benefits.add(ChatColor.WHITE + "• Start with: " + ItemNameUtils.formatMaterialName(materials.get(level)));
                }
            }
            case STARTING_ARMOR -> {
                List<Material> helmets = upgrade.getMaterialList("helmet");
                if (!helmets.isEmpty()) {
                    benefits.add(ChatColor.WHITE + "• Full " + ItemNameUtils.formatArmorTier(helmets.get(level)) + " armor set");
                }
            }
            case STARTING_FOOD -> {
                List<Integer> amounts = upgrade.getIntegerList("amounts");
                if (!amounts.isEmpty()) {
                    benefits.add(ChatColor.WHITE + "• Start with: " + amounts.get(level) + " cooked beef");
                }
            }
            case ARROW_QUANTITY -> {
                List<Integer> amounts = upgrade.getIntegerList("amounts");
                if (!amounts.isEmpty()) {
                    benefits.add(ChatColor.WHITE + "• Start with: " + amounts.get(level) + " arrows + bow");
                }
            }
            case DAMAGE_BOOST -> {
                List<Double> multipliers = upgrade.getDoubleList("multipliers");
                if (!multipliers.isEmpty()) {
                    final int percentage = (int) ((multipliers.get(level) - 1.0) * 100);
                    benefits.add(ChatColor.WHITE + "• +" + percentage + "% damage to trial mobs");
                }
            }
            case SPEED_BOOST -> {
                List<Double> multipliers = upgrade.getDoubleList("multipliers");
                if (!multipliers.isEmpty()) {
                    final int percentage = (int) ((multipliers.get(level) - 1.0) * 100);
                    benefits.add(ChatColor.WHITE + "• +" + percentage + "% movement speed");
                }
            }
            case DODGE_CHANCE -> {
                List<Double> chances = upgrade.getDoubleList("chances");
                if (!chances.isEmpty()) {
                    final int percentage = (int) (chances.get(level) * 100);
                    benefits.add(ChatColor.WHITE + "• " + percentage + "% chance to dodge attacks");
                }
            }
            case ENCHANT_CHANCE -> {
                List<Double> chances = upgrade.getDoubleList("chances");
                if (!chances.isEmpty()) {
                    final int percentage = (int) (chances.get(level) * 100);
                    benefits.add(ChatColor.WHITE + "• " + percentage + "% chance for enchanted rewards");
                }
            }
        }

        return benefits;
    }

    // --- Click handling (slot-based lookup, no more substring matching) ---

    public static void handleUpgradeClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getCurrentItem() == null) {
            return;
        }

        final int slot = event.getSlot();
        Map<Integer, String> slotMapping = upgradeSlotMappings.get(event.getInventory());
        if (slotMapping == null) {
            return;
        }

        final String upgradeId = slotMapping.get(slot);
        if (upgradeId == null) {
            return;
        }

        TrialTree.TrialUpgrade upgrade = TrialTree.getUpgrade(upgradeId);
        if (upgrade == null) {
            return;
        }

        PlayerTrialUpgrades playerUpgrades = TrialUpgradeManager.getPlayerUpgrades(player);
        boolean success = playerUpgrades.purchaseUpgrade(upgradeId);
        if (!success) {
            player.sendMessage(ChatColor.RED + "You are unable to upgrade " + upgrade.getName() + "!");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "Upgraded " + upgrade.getName() + "!");
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);

        if (!canUpgradeAnythingElse(player)) {
            player.sendMessage(ChatColor.RED + "You have no more upgrades available!");
            player.closeInventory();
        } else {
            refreshUpgradeGUI(player);
        }
    }

    // --- Upgrade availability check (fixed: now checks max level) ---

    public static boolean canUpgradeAnythingElse(Player player) {
        PlayerTrialUpgrades playerUpgrades = TrialUpgradeManager.getPlayerUpgrades(player);

        for (TrialTree.TrialUpgrade upgrade : TrialTree.getAllUpgrades().values()) {
            final int currentLevel = playerUpgrades.getUpgradeLevel(upgrade.getId());
            if (currentLevel >= upgrade.getMaxLevel()) {
                continue; // Already maxed out — no further levels
            }

            final int nextLevel = currentLevel + 1;
            if (playerUpgrades.getAvailablePoints() >= upgrade.getCost(nextLevel)) {
                return true;
            }
        }

        return false;
    }

    // --- Lifecycle management ---

    public static boolean isUpgradeInventory(Inventory inv) {
        return playerInventories.containsValue(inv);
    }

    public static Collection<Inventory> getUpgradeInventories() {
        return playerInventories.values();
    }

    public static void removeInventory(Inventory inventory) {
        upgradeSlotMappings.remove(inventory);
        playerInventories.values().remove(inventory);
    }

    public static void removePlayer(UUID playerId) {
        Inventory inv = playerInventories.remove(playerId);
        if (inv != null) {
            upgradeSlotMappings.remove(inv);
        }
    }

    public static void clearAll() {
        playerInventories.clear();
        upgradeSlotMappings.clear();
    }
}
