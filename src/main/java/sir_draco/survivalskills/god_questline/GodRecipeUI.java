package sir_draco.survivalskills.god_questline;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import sir_draco.survivalskills.utils.RecipeSlotLayout;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class GodRecipeUI {

    private final ArrayList<Inventory> inventories = new ArrayList<>();
    private int currentInv = 0;

    public GodRecipeUI(List<NamespacedKey> keyList, GodTrophyQuest.QuestProgress questProgress) {
        Objects.requireNonNull(keyList);
        Objects.requireNonNull(questProgress);
        createInventories(keyList, questProgress);
    }

    public void open(Player p) {
        currentInv = 0;
        p.openInventory(inventories.get(currentInv));
    }

    public void handleClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;
        if (e.getCurrentItem() == null) return;
        e.setCancelled(true);

        ItemStack arrow = e.getCurrentItem();
        ItemMeta meta = arrow.getItemMeta();
        if (!arrow.getType().equals(Material.ARROW)) return;
        if (meta == null) return;
        if (meta.getDisplayName().equalsIgnoreCase("arrow")) return;

        Player p = (Player) e.getWhoClicked();

        if (ItemStackGeneratorUtils.hasCustomModelData(meta)) {
            if (currentInv + 1 >= inventories.size()) currentInv = -1;
            currentInv += 1;
            Inventory inv = inventories.get(currentInv);
            p.openInventory(inv);
        }
        else {
            if (currentInv - 1 < 0) currentInv = inventories.size();
            currentInv -= 1;
            Inventory inv = inventories.get(currentInv);
            p.openInventory(inv);
        }
    }

    public void handleDrag(InventoryDragEvent e) {
        e.setCancelled(true);
        ItemStack arrow = e.getOldCursor();
        ItemMeta meta = arrow.getItemMeta();
        if (!arrow.getType().equals(Material.ARROW)) return;
        if (meta == null) return;
        if (meta.getDisplayName().equalsIgnoreCase("arrow")) return;

        Player p = (Player) e.getWhoClicked();

        if (ItemStackGeneratorUtils.hasCustomModelData(meta)) {
            if (currentInv + 1 >= inventories.size()) currentInv = -1;
            currentInv += 1;
            Inventory inv = inventories.get(currentInv);
            p.openInventory(inv);
        }
        else {
            if (currentInv - 1 < 0) currentInv = inventories.size();
            currentInv -= 1;
            Inventory inv = inventories.get(currentInv);
            p.openInventory(inv);
        }
    }

    public void createInventories(List<NamespacedKey> recipeKeys, GodTrophyQuest.QuestProgress questProgress) {
        if (recipeKeys.isEmpty()) {
            Inventory inventory = Bukkit.createInventory(null, 36, "God Quest Progress");
            decorateFooter(inventory, questProgress, false, false);
            inventories.add(inventory);
            return;
        }

        int totalPages = (int) Math.ceil(recipeKeys.size() / 2.0);
        for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
            int currentPage = pageIndex + 1;
            Inventory inventory = Bukkit.createInventory(null, 36,
                    "God Recipes: Page " + currentPage + "/" + totalPages);
            int firstRecipeIndex = pageIndex * 2;
            addRecipe(recipeKeys, firstRecipeIndex, inventory);
            if (firstRecipeIndex + 1 < recipeKeys.size())
                addRecipe(recipeKeys, firstRecipeIndex + 1, inventory);
            decorateFooter(inventory, questProgress, pageIndex > 0, currentPage < totalPages);
            inventories.add(inventory);
        }
    }

    private void decorateFooter(Inventory inventory, GodTrophyQuest.QuestProgress questProgress,
            boolean showBack, boolean showNext) {
        ItemStack bottom = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        for (int slot = 27; slot <= 35; slot++)
            inventory.setItem(slot, bottom);

        inventory.setItem(31, createProgressItem(questProgress));
        if (showBack)
            inventory.setItem(30, createArrow(ChatColor.RED + "Back", false));
        if (showNext)
            inventory.setItem(32, createArrow(ChatColor.BLUE + "Next", true));
    }

    private ItemStack createProgressItem(GodTrophyQuest.QuestProgress questProgress) {
        ItemStack progressItem = new ItemStack(Material.COMPASS);
        ItemMeta meta = progressItem.getItemMeta();
        if (meta == null)
            return progressItem;
        meta.setDisplayName(ChatColor.GOLD + "God Quest Progress");
        meta.setLore(List.of(
                ChatColor.YELLOW + "Current Step: " + ChatColor.WHITE + questProgress.currentStep(),
                ChatColor.YELLOW + "Progress: " + ChatColor.AQUA + questProgress.currentCount()
                        + ChatColor.WHITE + " / " + ChatColor.AQUA + questProgress.goal(),
                ChatColor.YELLOW + "Next Step: " + ChatColor.WHITE + questProgress.nextStep()));
        progressItem.setItemMeta(meta);
        return progressItem;
    }

    private ItemStack createArrow(String displayName, boolean pointsForward) {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        if (meta == null)
            return arrow;
        meta.setDisplayName(displayName);
        if (pointsForward)
            ItemStackGeneratorUtils.setCustomModelData(meta, 1);
        arrow.setItemMeta(meta);
        return arrow;
    }

    public void addRecipe(List<NamespacedKey> recipeKeys, int recipeIndex, Inventory inv) {
        NamespacedKey key = recipeKeys.get(recipeIndex);
        if (key == null) return;
        List<Integer> slots = RecipeSlotLayout.getRecipePositions(recipeIndex + 1);
        Recipe recipe = Bukkit.getRecipe(key);
        switch (recipe) {
            case ShapedRecipe shapedRecipe -> {
                String[] shape = shapedRecipe.getShape();
                Map<Character, ItemStack> ingredients = shapedRecipe.getIngredientMap();
                Map<Character, RecipeChoice> recipeChoices = shapedRecipe.getChoiceMap();

                for (int i = 0; i < shape.length * 3; i++) {
                    int slot = i % 3;
                    String layer;
                    if (i <= 2) layer = shape[0];
                    else if (i <= 5) layer = shape[1];
                    else layer = shape[2];
                    if (slot >= layer.length()) continue;
                    char c = layer.charAt(slot);
                    if (c == ' ' || c == 'D') continue;

                    if (recipeChoices.containsKey(c) && !(recipeChoices.get(c) instanceof RecipeChoice.MaterialChoice)) {
                        RecipeChoice.ExactChoice choice = (RecipeChoice.ExactChoice) recipeChoices.get(c);
                        if (choice == null) continue;
                        inv.setItem(slots.get(i), choice.getItemStack());
                    }
                    else if (ingredients.containsKey(c)) inv.setItem(slots.get(i), ingredients.get(c));

                }
                inv.setItem(slots.getLast(), shapedRecipe.getResult());
            }
            case ShapelessRecipe shapelessRecipe -> {
                List<ItemStack> ingredients = shapelessRecipe.getIngredientList();
                int slot = 0;
                if (ingredients.isEmpty()) return;
                for (ItemStack ingredient : ingredients) {
                    inv.setItem(slots.get(slot), ingredient);
                    slot++;
                }

                inv.setItem(slots.getLast(), shapelessRecipe.getResult());
            }
            case null, default -> {}
        }
    }

    public ArrayList<Inventory> getInventories() {
        return inventories;
    }

    public int getCurrentInv() {
        return currentInv;
    }
}
