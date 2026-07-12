package sir_draco.survivalskills.utils;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.commands.default_commands.SkillsCommand;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Defers Bukkit recipe registration: recipes are pushed onto a stack during
 * plugin startup and drained in batches each tick. Draining incrementally
 * avoids freezing the main thread with one large {@code addRecipe} burst.
 */
public final class RecipeRegistrar {

    private static final int BATCH_SIZE = 10;

    private final static Deque<RecipeRecord> recipeStack = new ArrayDeque<>();

    private RecipeRegistrar() {}

    public static void addShapedRecipe(ShapedRecipe recipe, NamespacedKey key) {
        recipeStack.push(new RecipeRecord(recipe, key));
    }

    public static void addShapelessRecipe(ShapelessRecipe recipe, NamespacedKey key) {
        recipeStack.push(new RecipeRecord(recipe, key));
    }

    public static void emptyRecipeStack(SurvivalSkills plugin) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (recipeStack.isEmpty()) {
                    // Command can only be loaded if all recipes have been loaded
                    new SkillsCommand(SurvivalSkills.getInstance());
                    cancel();
                    return;
                }

                // Process multiple recipes per tick to reduce total processing time
                processBatch(plugin);
            }
        }.runTaskTimer(plugin, 1, 1);
    }

    private static void processBatch(SurvivalSkills plugin) {
        int batchSize = Math.min(BATCH_SIZE, recipeStack.size());
        List<RecipeRecord> batch = new ArrayList<>();

        // Collect batch of recipes
        for (int i = 0; i < batchSize; i++) {
            if (!recipeStack.isEmpty()) {
                batch.add(recipeStack.pop());
            }
        }

        // Process the entire batch
        for (RecipeRecord recipeRecord : batch) {
            NamespacedKey key = recipeRecord.key();
            Recipe newRecipe = recipeRecord.recipe();
            Recipe existing = Bukkit.getRecipe(key);

            // If an identical recipe already exists, skip any work
            if (existing != null) {
                if (RecipeComparator.recipesEqual(existing, newRecipe))
                    continue;

                // If the existing recipe is different, remove it
                plugin.getServer().removeRecipe(key);
            }

            // Add the new/updated recipe
            plugin.getServer().addRecipe(newRecipe);
        }
    }
}