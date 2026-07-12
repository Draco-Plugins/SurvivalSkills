package sir_draco.survivalskills.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;

/**
 * Pairs a Bukkit recipe with its namespace key so the {@link RecipeRegistrar}
 * can remove/re-add it as needed when the recipe set changes.
 */
public record RecipeRecord(Recipe recipe, NamespacedKey key) {}