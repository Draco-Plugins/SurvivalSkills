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
import org.bukkit.scheduler.BukkitRunnable;

import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TrialGUI {

    private static final List<Inventory> upgradeInventories = new ArrayList<>();

    private TrialGUI() {
        // Private constructor to prevent instantiation
    }

    public static void openUpgradeGUI(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_PURPLE + "Trial Upgrades");

        PlayerTrialUpgrades playerUpgrades = TrialUpgradeManager.getPlayerUpgrades(player);
        Map<String, TrialTree.TrialUpgrade> allUpgrades = TrialTree.getAllUpgrades();

        // Create header with player points
        ItemStack pointsDisplay = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta pointsMeta = pointsDisplay.getItemMeta();
        if (pointsMeta == null)
            return; // Safety check
        pointsMeta.setDisplayName(ChatColor.GOLD + "Trial Points: " + playerUpgrades.getAvailablePoints());
        pointsMeta.setLore(List.of(ChatColor.GRAY + "Earn points by completing trials",
                ChatColor.GRAY + "the inventory closes if you can't afford anything else",
                ChatColor.RED + "Your points reset every trial!"));
        pointsDisplay.setItemMeta(pointsMeta);
        inv.setItem(4, pointsDisplay);

        // Place upgrades in specific slots
        int[] upgradeSlots = { 19, 21, 23, 25, 28, 30, 32, 34 };
        int slotIndex = 0;

        for (String upgradeId : List.of("starting_weapon", "starting_armor", "starting_food",
                "arrow_quantity", "damage_boost", "speed_boost",
                "dodge_chance", "enchant_chance")) {
            if (slotIndex >= upgradeSlots.length)
                break;

            TrialTree.TrialUpgrade upgrade = allUpgrades.get(upgradeId);
            if (upgrade != null) {
                ItemStack upgradeItem = createUpgradeItem(upgrade, playerUpgrades);
                inv.setItem(upgradeSlots[slotIndex], upgradeItem);
                slotIndex++;
            }
        }

        // Add border decoration
        ItemStack border = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta == null)
            return; // Safety check
        borderMeta.setDisplayName(" ");
        border.setItemMeta(borderMeta);

        for (int i = 0; i < 9; i++) {
            if (i == 4)
                continue;
            inv.setItem(i, border);
        }
        for (int i = 45; i < 54; i++)
            inv.setItem(i, border);
        for (int i = 9; i < 45; i += 9) {
            inv.setItem(i, border);
            inv.setItem(i + 8, border);
        }

        upgradeInventories.add(inv);
        player.openInventory(inv);
    }

    private static ItemStack createUpgradeItem(TrialTree.TrialUpgrade upgrade, PlayerTrialUpgrades playerUpgrades) {
        int currentLevel = playerUpgrades.getUpgradeLevel(upgrade.getId());
        Material material = getUpgradeMaterial(upgrade.getType());

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item; // Safety check

        // Set display name with level
        String levelDisplay = currentLevel > 0 ? " §7(Level " + currentLevel + "/" + upgrade.getMaxLevel() + ")" : "";
        meta.setDisplayName(ChatColor.AQUA + upgrade.getName() + levelDisplay);

        // Create lore
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + upgrade.getDescription());
        lore.add("");

        if (currentLevel < upgrade.getMaxLevel()) {
            int nextLevel = currentLevel + 1;
            int cost = upgrade.getCost(nextLevel);

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

    @SuppressWarnings("unchecked")
    private static List<String> getUpgradeBenefits(TrialTree.TrialUpgrade upgrade, int level) {
        List<String> benefits = new ArrayList<>();

        switch (upgrade.getType()) {
            case STARTING_WEAPON -> {
                List<Material> materials = (List<Material>) upgrade.getUpgradeData().get("materials");
                benefits.add(ChatColor.WHITE + "• Start with: " + formatMaterialName(materials.get(level)));
            }
            case STARTING_ARMOR -> {
                Map<String, Object> armorData = upgrade.getUpgradeData();
                List<Material> helmets = (List<Material>) armorData.get("helmet");
                benefits.add(ChatColor.WHITE + "• Full " + formatArmorTier(helmets.get(level)) + " armor set");
            }
            case STARTING_FOOD -> {
                List<Integer> amounts = (List<Integer>) upgrade.getUpgradeData().get("amounts");
                benefits.add(ChatColor.WHITE + "• Start with: " + amounts.get(level) + " cooked beef");
            }
            case ARROW_QUANTITY -> {
                List<Integer> amounts = (List<Integer>) upgrade.getUpgradeData().get("amounts");
                benefits.add(ChatColor.WHITE + "• Start with: " + amounts.get(level) + " arrows + bow");
            }
            case DAMAGE_BOOST -> {
                List<Double> multipliers = (List<Double>) upgrade.getUpgradeData().get("multipliers");
                int percentage = (int) ((multipliers.get(level) - 1.0) * 100);
                benefits.add(ChatColor.WHITE + "• +" + percentage + "% damage to trial mobs");
            }
            case SPEED_BOOST -> {
                List<Double> multipliers = (List<Double>) upgrade.getUpgradeData().get("multipliers");
                int percentage = (int) ((multipliers.get(level) - 1.0) * 100);
                benefits.add(ChatColor.WHITE + "• +" + percentage + "% movement speed");
            }
            case DODGE_CHANCE -> {
                List<Double> chances = (List<Double>) upgrade.getUpgradeData().get("chances");
                int percentage = (int) (chances.get(level) * 100);
                benefits.add(ChatColor.WHITE + "• " + percentage + "% chance to dodge attacks");
            }
            case ENCHANT_CHANCE -> {
                List<Double> chances = (List<Double>) upgrade.getUpgradeData().get("chances");
                int percentage = (int) (chances.get(level) * 100);
                benefits.add(ChatColor.WHITE + "• " + percentage + "% chance for enchanted rewards");
            }
        }

        return benefits;
    }

    private static String formatMaterialName(Material material) {
        String name = material.name().toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();

        for (String word : words) {
            if (!formatted.isEmpty())
                formatted.append(" ");
            formatted.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }

        return formatted.toString();
    }

    private static String formatArmorTier(Material helmet) {
        String name = helmet.name().toLowerCase();
        if (name.contains("leather"))
            return "Leather";
        if (name.contains("chainmail"))
            return "Chainmail";
        if (name.contains("iron"))
            return "Iron";
        if (name.contains("diamond"))
            return "Diamond";
        if (name.contains("netherite"))
            return "Netherite";
        return "Unknown";
    }

    public static void handleUpgradeClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (event.getCurrentItem() == null)
            return;

        ItemStack item = event.getCurrentItem();
        if (item.getItemMeta() == null || !item.getItemMeta().hasDisplayName())
            return;

        String displayName = item.getItemMeta().getDisplayName();
        PlayerTrialUpgrades playerUpgrades = TrialUpgradeManager.getPlayerUpgrades(player);

        // Find the upgrade based on display name
        for (Map.Entry<String, TrialTree.TrialUpgrade> entry : TrialTree.getAllUpgrades().entrySet()) {
            TrialTree.TrialUpgrade upgrade = entry.getValue();

            if (!displayName.contains(upgrade.getName()))
                continue;
            // Purchase the upgrade
            boolean success = playerUpgrades.purchaseUpgrade(upgrade.getId());
            if (!success) {
                player.sendMessage(ChatColor.RED + "You are unable to upgrade " + upgrade.getName() + "!");
                return;
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    playerUpgrades.saveToFile();
                }
            }.runTaskAsynchronously(SurvivalSkills.getInstance());

            player.sendMessage(ChatColor.GREEN + "Upgraded " + upgrade.getName() + "!");
            player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);

            if (!canUpgradeAnythingElse(player)) {
                player.sendMessage(ChatColor.RED + "You have no more upgrades available!");
                player.closeInventory();
            } else {
                // Reopen the upgrade GUI to reflect changes
                openUpgradeGUI(player);
            }
            return;
        }
    }

    public static List<Inventory> getUpgradeInventories() {
        return upgradeInventories;
    }

    public static void removeInventory(Inventory inventory) {
        upgradeInventories.remove(inventory);
    }

    public static boolean canUpgradeAnythingElse(Player p) {
        PlayerTrialUpgrades playerUpgrades = TrialUpgradeManager.getPlayerUpgrades(p);

        for (TrialTree.TrialUpgrade upgrade : TrialTree.getAllUpgrades().values()) {
            if (playerUpgrades.getAvailablePoints() >= upgrade
                    .getCost(playerUpgrades.getUpgradeLevel(upgrade.getId()) + 1)) {
                return true;
            }
        }

        return false;
    }
}