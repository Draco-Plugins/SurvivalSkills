package sir_draco.survivalskills.skill_listeners;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoEatModeTest {
    private static final Map<Material, Integer> NUTRITION = Map.of(
            Material.DRIED_KELP, 1, Material.BREAD, 5, Material.COOKED_BEEF, 8);
    private static final Map<Material, Float> SATURATION = Map.of(
            Material.DRIED_KELP, 0.6f, Material.BREAD, 1.2f, Material.COOKED_BEEF, 1.6f);
    private static final List<ItemStack> FOODS = List.of(new ItemStack(Material.BREAD),
            new ItemStack(Material.COOKED_BEEF), new ItemStack(Material.DRIED_KELP));

    @Test
    void inventoryOrderPreservesSlots() {
        assertEquals(FOODS, AutoEatMode.INVENTORY_ORDER.order(FOODS, 6, NUTRITION, SATURATION));
    }

    @Test
    void bestSaturationFirstOrdersByRatioDescending() {
        assertEquals(List.of(Material.COOKED_BEEF, Material.BREAD, Material.DRIED_KELP),
                types(AutoEatMode.BEST_SATURATION_FIRST.order(FOODS, 6, NUTRITION, SATURATION)));
    }

    @Test
    void worstFoodFirstOrdersByNutritionAscending() {
        assertEquals(List.of(Material.DRIED_KELP, Material.BREAD, Material.COOKED_BEEF),
                types(AutoEatMode.WORST_FOOD_FIRST.order(FOODS, 6, NUTRITION, SATURATION)));
    }

    @Test
    void balancedChoosesClosestNutritionToHungerGap() {
        assertEquals(List.of(Material.BREAD, Material.COOKED_BEEF, Material.DRIED_KELP),
                types(AutoEatMode.BALANCED.order(FOODS, 6, NUTRITION, SATURATION)));
    }

    private static List<Material> types(List<ItemStack> items) {
        return items.stream().map((ItemStack item) -> item.getType()).toList();
    }
}
