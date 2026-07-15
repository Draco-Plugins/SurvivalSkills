package sir_draco.survivalskills.skill_listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;
import java.util.List;

public final class AutoEatGui implements Listener {
    private static final String FOOD_TITLE = "Auto Eat Foods - Page ";
    private static final String MODE_TITLE = "Auto Eat Mode";
    private static final int FOODS_PER_PAGE = 45;
    private static final int PREVIOUS_SLOT = 48;
    private static final int MODE_SLOT = 49;
    private static final int NEXT_SLOT = 50;
    private static final int MODE_BACK_SLOT = 22;
    private static final List<Integer> MODE_SLOTS = List.of(10, 12, 14, 16);

    private final SurvivalSkills plugin;

    public AutoEatGui(SurvivalSkills plugin) {
        this.plugin = plugin;
    }

    public void openFoodPage(Player player, int requestedPage) {
        List<Material> foods = plugin.getFarmingListener().getFilterableFoods();
        int totalPages = Math.max(1, (foods.size() + FOODS_PER_PAGE - 1) / FOODS_PER_PAGE);
        int page = Math.max(0, Math.min(requestedPage, totalPages - 1));
        Inventory inventory = Bukkit.createInventory(null, 54, FOOD_TITLE + (page + 1));
        int start = page * FOODS_PER_PAGE;
        for (int index = start; index < Math.min(start + FOODS_PER_PAGE, foods.size()); index++) {
            Material food = foods.get(index);
            boolean blacklisted = plugin.getFarmingListener().isBlacklisted(player, food);
            inventory.setItem(index - start, createItem(food, null, List.of((blacklisted ? ChatColor.RED : ChatColor.GREEN)
                    + (blacklisted ? "Currently blacklisted. Click to allow." : "Currently allowed. Click to blacklist."))));
        }
        if (page > 0) inventory.setItem(PREVIOUS_SLOT, createItem(Material.ARROW, ChatColor.YELLOW + "Previous Page", List.of()));
        AutoEatMode mode = plugin.getFarmingListener().getAutoEatMode(player);
        inventory.setItem(MODE_SLOT, createItem(Material.COMPASS, ChatColor.AQUA + "Mode: " + mode.getDisplayName(),
                List.of(ChatColor.GRAY + mode.getDescription(), ChatColor.YELLOW + "Click to change mode.")));
        if (page + 1 < totalPages) inventory.setItem(NEXT_SLOT, createItem(Material.ARROW, ChatColor.YELLOW + "Next Page", List.of()));
        player.openInventory(inventory);
    }

    public void openModePage(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, MODE_TITLE);
        AutoEatMode activeMode = plugin.getFarmingListener().getAutoEatMode(player);
        AutoEatMode[] modes = AutoEatMode.values();
        for (int index = 0; index < modes.length; index++) {
            AutoEatMode mode = modes[index];
            List<String> lore = new ArrayList<>(List.of(ChatColor.GRAY + mode.getDescription()));
            if (mode == activeMode) lore.add(ChatColor.GREEN + "Current mode");
            else lore.add(ChatColor.YELLOW + "Click to select.");
            inventory.setItem(MODE_SLOTS.get(index), createItem(modeMaterial(mode), ChatColor.AQUA + mode.getDisplayName(), lore));
        }
        inventory.setItem(MODE_BACK_SLOT, createItem(Material.ARROW, ChatColor.YELLOW + "Back", List.of()));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();
        if (!title.startsWith(FOOD_TITLE) && !title.equals(MODE_TITLE)) return;
        event.setCancelled(true);
        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        if (title.equals(MODE_TITLE)) handleModeClick(player, event.getRawSlot());
        else handleFoodClick(player, event.getRawSlot(), parsePage(title), event.getCurrentItem());
    }

    private void handleFoodClick(Player player, int slot, int page, ItemStack clickedItem) {
        if (slot == PREVIOUS_SLOT) openFoodPage(player, page - 1);
        else if (slot == MODE_SLOT) openModePage(player);
        else if (slot == NEXT_SLOT) openFoodPage(player, page + 1);
        else if (slot >= 0 && slot < FOODS_PER_PAGE && clickedItem != null
                && plugin.getFarmingListener().getFilterableFoods().contains(clickedItem.getType())) {
            plugin.getFarmingListener().toggleBlacklistedFood(player, clickedItem.getType());
            openFoodPage(player, page);
        }
    }

    private void handleModeClick(Player player, int slot) {
        if (slot == MODE_BACK_SLOT) {
            openFoodPage(player, 0);
            return;
        }
        int modeIndex = MODE_SLOTS.indexOf(slot);
        if (modeIndex < 0) return;
        plugin.getFarmingListener().setAutoEatMode(player, AutoEatMode.values()[modeIndex]);
        openModePage(player);
    }

    private static int parsePage(String title) {
        try {
            return Math.max(0, Integer.parseInt(title.substring(FOOD_TITLE.length())) - 1);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        if (name != null) meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static Material modeMaterial(AutoEatMode mode) {
        return switch (mode) {
            case INVENTORY_ORDER -> Material.CHEST;
            case BEST_SATURATION_FIRST -> Material.GOLDEN_CARROT;
            case WORST_FOOD_FIRST -> Material.DRIED_KELP;
            case BALANCED -> Material.CLOCK;
        };
    }
}
