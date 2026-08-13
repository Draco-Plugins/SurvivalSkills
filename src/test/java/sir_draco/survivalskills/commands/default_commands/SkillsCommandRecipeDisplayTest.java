package sir_draco.survivalskills.commands.default_commands;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import sir_draco.survivalskills.utils.Recipes.SmallShapedSpec;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkillsCommandRecipeDisplayTest {

    @Test
    void usesCustomExactIngredientInsteadOfBukkitMaterialSnapshot() {
        ItemStack customArmor = mock(ItemStack.class);
        ItemStack customArmorClone = mock(ItemStack.class);
        when(customArmor.clone()).thenReturn(customArmorClone);

        SmallShapedSpec recipeSpec = recipeSpec(mock(ItemStack.class), customArmor);

        ItemStack displayIngredient = SkillsCommand.getDisplayIngredient(recipeSpec, 'A').orElseThrow();

        assertSame(customArmorClone, displayIngredient);
    }

    private static SmallShapedSpec recipeSpec(ItemStack result, ItemStack exactIngredient) {
        return new SmallShapedSpec("customarmor", result, "AAA:ABA:AAA", exactIngredient, null, null,
                null, Material.LEATHER_CHESTPLATE, null);
    }
}
