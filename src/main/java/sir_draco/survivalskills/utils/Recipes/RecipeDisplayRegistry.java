package sir_draco.survivalskills.utils.Recipes;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Stores the original custom item stacks used to render plugin recipe GUIs. */
public final class RecipeDisplayRegistry {

    private static final ConcurrentMap<NamespacedKey, SmallShapedSpec> RECIPES = new ConcurrentHashMap<>();

    private RecipeDisplayRegistry() {}

    public static void register(NamespacedKey key, SmallShapedSpec recipeSpec) {
        RECIPES.put(key, copy(recipeSpec));
    }

    public static Optional<SmallShapedSpec> find(NamespacedKey key) {
        return Optional.ofNullable(RECIPES.get(key)).map(RecipeDisplayRegistry::copy);
    }

    private static SmallShapedSpec copy(SmallShapedSpec recipeSpec) {
        return new SmallShapedSpec(recipeSpec.keyName(), recipeSpec.result().clone(), recipeSpec.shape(),
                clone(recipeSpec.exactA()), clone(recipeSpec.exactB()), clone(recipeSpec.exactC()),
                recipeSpec.matA(), recipeSpec.matB(), recipeSpec.matC());
    }

    private static ItemStack clone(ItemStack item) {
        return item == null ? null : item.clone();
    }
}
