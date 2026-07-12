package sir_draco.survivalskills.utils.Recipes;

import org.bukkit.inventory.ItemStack;

/**
 * Declarative spec for a full 3x3 shaped recipe, consumed by
 * {@link RecipeMaker#createShapedRecipe}. Null grid slots render as spaces in
 * the shape. When {@code unique} is true, ingredients are registered as
 * ExactChoice (preserving item metadata); otherwise only the material matters.
 */
public record ShapedSpec(String keyName, ItemStack result, boolean unique, ItemStack a, ItemStack b,
        ItemStack c, ItemStack d, ItemStack e, ItemStack f, ItemStack g, ItemStack h, ItemStack i)
        implements RecipeSpec {}