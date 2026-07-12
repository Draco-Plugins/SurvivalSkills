package sir_draco.survivalskills.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes the inventory slot positions used to render a recipe (3x3 grid + result)
 * inside the recipe-display inventories shared by {@code /skills recipes} and the
 * God Recipe UI. Two recipes share a single page: odd-numbered recipes occupy the
 * left grid, even-numbered recipes occupy the right grid.
 */
public final class RecipeSlotLayout {

    private RecipeSlotLayout() {}

    public static List<Integer> getRecipePositions(int slot) {
        ArrayList<Integer> positions = new ArrayList<>();
        if (slot % 2 == 1) {
            positions.addAll(List.of(0, 1, 2, 9, 10, 11, 18, 19, 20, 12));
        } else {
            positions.addAll(List.of(5, 6, 7, 14, 15, 16, 23, 24, 25, 17));
        }
        return positions;
    }
}