package sir_draco.survivalskills.skill_listeners;

import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.Repairable;
import sir_draco.survivalskills.utils.Recipes.ArmorUpgradeRecipeData;
import sir_draco.survivalskills.utils.Recipes.ArmorUpgradeRecipeData.ArmorUpgradeRecipe;

import java.util.List;
import java.util.Optional;

/** Validates custom armor upgrade inputs and carries their enchantments to the result. */
public final class ArmorUpgradeCraftingListener implements Listener {

    @EventHandler
    public void prepareArmorUpgrade(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        findUpgrade(recipe).ifPresent((ArmorUpgradeRecipe upgrade) -> {
            ItemStack resultTemplate = recipe.getResult();
            ItemStack result = createUpgradedResult(event.getInventory().getMatrix(), resultTemplate,
                    upgrade.requiredItem()).orElseGet(() -> new ItemStack(Material.AIR));
            event.getInventory().setResult(result);
        });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void validateArmorUpgradeCraft(CraftItemEvent event) {
        findUpgrade(event.getRecipe()).ifPresent((ArmorUpgradeRecipe upgrade) -> {
            Optional<ItemStack> result = createUpgradedResult(event.getInventory().getMatrix(),
                    event.getRecipe().getResult(), upgrade.requiredItem());
            if (result.isEmpty()) {
                event.setCancelled(true);
                event.getInventory().setResult(new ItemStack(Material.AIR));
                return;
            }
            event.getInventory().setResult(result.get());
        });
    }

    static Optional<ItemStack> createUpgradedResult(ItemStack[] matrix, ItemStack resultTemplate,
                                                     ItemStack requiredItem) {
        return findMatchingIngredient(matrix, requiredItem).map((ItemStack ingredient) -> {
            ItemStack result = resultTemplate.clone();
            result.addUnsafeEnchantments(ingredient.getEnchantments());
            return result;
        });
    }

    private static Optional<ItemStack> findMatchingIngredient(ItemStack[] matrix, ItemStack requiredItem) {
        for (ItemStack ingredient : matrix) {
            if (ingredient == null || ingredient.getType() != requiredItem.getType()) continue;
            if (normalizeEnchantingMetadata(requiredItem).isSimilar(normalizeEnchantingMetadata(ingredient)))
                return Optional.of(ingredient);
        }
        return Optional.empty();
    }

    private static ItemStack normalizeEnchantingMetadata(ItemStack item) {
        ItemStack normalized = item.clone();
        List<Enchantment> enchantments = List.copyOf(normalized.getEnchantments().keySet());
        for (Enchantment enchantment : enchantments)
            normalized.removeEnchantment(enchantment);
        if (normalized.getItemMeta() instanceof Repairable repairable && repairable.hasRepairCost()) {
            repairable.setRepairCost(0);
            normalized.setItemMeta(repairable);
        }
        return normalized;
    }

    private static Optional<ArmorUpgradeRecipe> findUpgrade(Recipe recipe) {
        if (!(recipe instanceof Keyed keyedRecipe)) return Optional.empty();
        return ArmorUpgradeRecipeData.find(keyedRecipe.getKey().getKey());
    }
}
