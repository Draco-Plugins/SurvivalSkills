package sir_draco.survivalskills.utils.Recipes;

import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Identifies reward recipes whose custom armor ingredient may carry enchantments. */
public final class ArmorUpgradeRecipeData {

    private static final Map<String, Character> UPGRADE_INGREDIENT_SLOTS = Map.ofEntries(
            Map.entry("travelerhelmet", 'A'),
            Map.entry("travelerchestplate", 'A'),
            Map.entry("travelerleggings", 'A'),
            Map.entry("travelerboots", 'A'),
            Map.entry("adventurerhelmet", 'A'),
            Map.entry("adventurerchestplate", 'A'),
            Map.entry("adventurerleggings", 'A'),
            Map.entry("adventurerboots", 'A'),
            Map.entry("powerhelmet", 'C'),
            Map.entry("powerchestplate", 'C'),
            Map.entry("powerleggings", 'C'),
            Map.entry("powerboots", 'C'));

    private ArmorUpgradeRecipeData() {}

    public static Optional<ArmorUpgradeRecipe> find(String keyName) {
        Character ingredientSlot = UPGRADE_INGREDIENT_SLOTS.get(keyName);
        if (ingredientSlot == null) return Optional.empty();

        return RewardRecipeData.ALL.stream()
                .filter((SmallShapedSpec spec) -> spec.keyName().equals(keyName))
                .findFirst()
                .map((SmallShapedSpec spec) -> new ArmorUpgradeRecipe(ingredientSlot,
                        getExactIngredient(spec, ingredientSlot)));
    }

    private static ItemStack getExactIngredient(SmallShapedSpec spec, char ingredientSlot) {
        ItemStack ingredient = switch (ingredientSlot) {
            case 'A' -> spec.exactA();
            case 'B' -> spec.exactB();
            case 'C' -> spec.exactC();
            default -> throw new IllegalArgumentException("Unsupported ingredient slot: " + ingredientSlot);
        };
        return Objects.requireNonNull(ingredient, "Armor upgrade ingredient must be an exact choice");
    }

    public record ArmorUpgradeRecipe(char ingredientSlot, ItemStack requiredItem) {

        public ArmorUpgradeRecipe {
            requiredItem = Objects.requireNonNull(requiredItem).clone();
        }

        @Override
        public ItemStack requiredItem() {
            return requiredItem.clone();
        }
    }
}
