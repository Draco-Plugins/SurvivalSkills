package sir_draco.survivalskills.utils.Recipes;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

import java.util.List;
import java.util.Objects;

/**
 * Declarative spec for a shapeless recipe. Recipe choices preserve whether an
 * ingredient should match an exact custom item or any item of a material.
 */
public record ShapelessSpec(String keyName, ItemStack result, List<RecipeChoice> ingredients)
        implements RecipeSpec {

    public ShapelessSpec {
        Objects.requireNonNull(keyName);
        Objects.requireNonNull(result);
        ingredients = List.copyOf(Objects.requireNonNull(ingredients));
    }
}
