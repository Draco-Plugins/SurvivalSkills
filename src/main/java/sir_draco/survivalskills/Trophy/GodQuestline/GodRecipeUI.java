package sir_draco.survivalskills.Trophy.GodQuestline;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import sir_draco.survivalskills.Utils.RecipeMaker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GodRecipeUI {

    private final ArrayList<Inventory> inventories = new ArrayList<>();
    private int currentInv = 0;

    public GodRecipeUI(ArrayList<NamespacedKey> keyList) {
        if (keyList.isEmpty()) {
            inventories.add(Bukkit.createInventory(null, 9, "Error"));
            return;
        }
        createInventories(keyList);
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

        if (meta.hasCustomModelData()) {
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

        if (meta.hasCustomModelData()) {
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

    public void createInventories(ArrayList<NamespacedKey> list) {
        ItemStack bottom = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        if (meta == null) return;
        meta.setDisplayName(ChatColor.RED + "Back");
        back.setItemMeta(meta);

        ItemStack front = new ItemStack(Material.ARROW);
        meta = front.getItemMeta();
        if (meta == null) return;
        meta.setDisplayName(ChatColor.BLUE + "Next");
        meta.setCustomModelData(1);
        front.setItemMeta(meta);

        int totalPages = (int) Math.ceil((double) list.size() / 2);
        int currPage = 1;
        for (int i = 1; i <= list.size(); i++) {
            Inventory inv = Bukkit.createInventory(null, 36, "God Recipes: Page " + currPage + "/" + totalPages);

            if (currPage == 1) {
                inv.setItem(35, bottom);
                inv.setItem(34, bottom);
                inv.setItem(33, bottom);
                inv.setItem(32, front);
                inv.setItem(31, bottom);
                inv.setItem(30, bottom);
                inv.setItem(29, bottom);
                inv.setItem(28, bottom);
                inv.setItem(27, bottom);
            }
            else if (currPage == totalPages) {
                inv.setItem(35, bottom);
                inv.setItem(34, bottom);
                inv.setItem(33, bottom);
                inv.setItem(32, bottom);
                inv.setItem(31, bottom);
                inv.setItem(30, back);
                inv.setItem(29, bottom);
                inv.setItem(28, bottom);
                inv.setItem(27, bottom);
            }
            else {
                inv.setItem(35, bottom);
                inv.setItem(34, bottom);
                inv.setItem(33, bottom);
                inv.setItem(32, front);
                inv.setItem(31, bottom);
                inv.setItem(30, back);
                inv.setItem(29, bottom);
                inv.setItem(28, bottom);
                inv.setItem(27, bottom);
            }

            addRecipe(list, i - 1, inv);

            if (i % 2 == 0 || i == list.size()) {
                inventories.add(inv);
                currPage += 1;
            }
        }
    }

    public void addRecipe(ArrayList<NamespacedKey> recipeKeys, int recipeIndex, Inventory inv) {
        NamespacedKey key = recipeKeys.get(recipeIndex);
        if (key == null) return;
        ArrayList<Integer> slots = RecipeMaker.getRecipePositions(recipeIndex);
        Recipe recipe = Bukkit.getRecipe(key);
        if (recipe == null) return;
        if (recipe instanceof ShapedRecipe) {
            ShapedRecipe shapedRecipe = (ShapedRecipe) recipe;
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

                if (ingredients.containsKey(c)) inv.setItem(slots.get(i), ingredients.get(c));
                else if (recipeChoices.containsKey(c)) {
                    RecipeChoice.ExactChoice choice = (RecipeChoice.ExactChoice) recipeChoices.get(c);
                    inv.setItem(slots.get(i), choice.getItemStack());
                }
            }
            inv.setItem(slots.get(slots.size() - 1), shapedRecipe.getResult());
        }
        else if (recipe instanceof ShapelessRecipe) {
            ShapelessRecipe shapelessRecipe = (ShapelessRecipe) recipe;
            List<ItemStack> ingredients = shapelessRecipe.getIngredientList();
            int slot = 0;
            if (ingredients.isEmpty()) return;
            for (ItemStack ingredient : ingredients) {
                inv.setItem(slots.get(slot), ingredient);
                slot++;
            }

            inv.setItem(slots.get(slots.size() - 1), shapelessRecipe.getResult());
        }
    }
}
