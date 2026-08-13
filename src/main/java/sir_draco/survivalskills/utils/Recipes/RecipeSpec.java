package sir_draco.survivalskills.utils.Recipes;

import org.bukkit.inventory.ItemStack;

/**
 * Marker supertype for declarative recipe specs. Sealed so the registration
 * loop in {@link RecipeMaker} can exhaustively switch over the concrete recipe
 * types without a fallback branch.
 */
public sealed interface RecipeSpec permits ShapelessSpec, SmallShapedSpec, ShapedSpec {
    String keyName();

    ItemStack result();
}
