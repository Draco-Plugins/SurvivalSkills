package sir_draco.survivalskills.utils.Recipes;

import org.bukkit.inventory.ItemStack;

/**
 * Marker supertype for declarative recipe specs. Sealed so the registration
 * loop in {@link RecipeMaker} can exhaustively switch over the two concrete
 * recipe shapes without a fallback branch.
 */
public sealed interface RecipeSpec permits SmallShapedSpec, ShapedSpec {
    String keyName();

    ItemStack result();
}