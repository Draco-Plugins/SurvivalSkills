package sir_draco.survivalskills.utils.Recipes;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Declarative spec for a small (up to 3-ingredient) shaped recipe, consumed by
 * {@link RecipeMaker#createSmallShapedRecipe}. Slots A/B/C each take either an
 * ExactChoice {@link ItemStack} or a {@link Material} fallback, or {@code null}
 * when the slot is unused per the supplied shape string.
 */
public record SmallShapedSpec(String keyName, ItemStack result, String shape, ItemStack exactA,
        ItemStack exactB, ItemStack exactC, Material matA, Material matB, Material matC)
        implements RecipeSpec {}