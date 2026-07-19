package sir_draco.survivalskills.skill_listeners;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArmorUpgradeCraftingListenerTest {

    @Test
    void copiesEnchantmentsFromMatchingCustomArmor() {
        @SuppressWarnings("unchecked")
        Map<Enchantment, Integer> enchantments = mock(Map.class);
        ItemStack required = mock(ItemStack.class);
        ItemStack normalizedRequired = mock(ItemStack.class);
        ItemStack enchantedIngredient = mock(ItemStack.class);
        ItemStack ingredientWithoutEnchantments = mock(ItemStack.class);
        ItemStack resultTemplate = mock(ItemStack.class);
        ItemStack result = mock(ItemStack.class);

        when(required.getType()).thenReturn(Material.DIAMOND_HELMET);
        when(required.clone()).thenReturn(normalizedRequired);
        when(normalizedRequired.getEnchantments()).thenReturn(Map.of());
        when(enchantedIngredient.getType()).thenReturn(Material.DIAMOND_HELMET);
        when(enchantedIngredient.clone()).thenReturn(ingredientWithoutEnchantments);
        when(enchantedIngredient.getEnchantments()).thenReturn(enchantments);
        when(ingredientWithoutEnchantments.getEnchantments()).thenReturn(enchantments);
        when(enchantments.keySet()).thenReturn(Set.of());
        when(normalizedRequired.isSimilar(ingredientWithoutEnchantments)).thenReturn(true);
        when(resultTemplate.clone()).thenReturn(result);

        Optional<ItemStack> upgraded = ArmorUpgradeCraftingListener.createUpgradedResult(
                new ItemStack[] {enchantedIngredient}, resultTemplate, required);

        assertTrue(upgraded.isPresent());
        assertSame(result, upgraded.get());
        verify(result).addUnsafeEnchantments(enchantments);
    }

    @Test
    void rejectsSameMaterialWhenCustomMetadataDoesNotMatch() {
        ItemStack required = mock(ItemStack.class);
        ItemStack normalizedRequired = mock(ItemStack.class);
        ItemStack vanillaIngredient = mock(ItemStack.class);
        ItemStack normalizedIngredient = mock(ItemStack.class);
        ItemStack resultTemplate = mock(ItemStack.class);

        when(required.getType()).thenReturn(Material.DIAMOND_BOOTS);
        when(required.clone()).thenReturn(normalizedRequired);
        when(normalizedRequired.getEnchantments()).thenReturn(Map.of());
        when(vanillaIngredient.getType()).thenReturn(Material.DIAMOND_BOOTS);
        when(vanillaIngredient.clone()).thenReturn(normalizedIngredient);
        when(normalizedIngredient.getEnchantments()).thenReturn(Map.of());
        when(normalizedRequired.isSimilar(normalizedIngredient)).thenReturn(false);

        Optional<ItemStack> upgraded = ArmorUpgradeCraftingListener.createUpgradedResult(
                new ItemStack[] {vanillaIngredient}, resultTemplate, required);

        assertTrue(upgraded.isEmpty());
    }
}
