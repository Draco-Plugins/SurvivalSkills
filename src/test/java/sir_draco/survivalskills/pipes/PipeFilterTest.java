package sir_draco.survivalskills.pipes;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PipeFilterTest {
    @Test
    void materialFilterMatchesItemsOfTheSameType() {
        PipeFilter filter = new PipeFilter(Material.STONE, Set.of());

        assertTrue(filter.matches(item(Material.STONE)));
        assertFalse(filter.matches(item(Material.DIRT)));
    }

    @Test
    void enchantedBookFilterMatchesRequiredEnchantmentRegardlessOfLevel() {
        NamespacedKey sharpness = key("minecraft:sharpness");
        NamespacedKey unbreaking = key("minecraft:unbreaking");
        PipeFilter filter = new PipeFilter(Material.ENCHANTED_BOOK, Set.of(sharpness));

        assertTrue(filter.matches(Material.ENCHANTED_BOOK, Set.of(sharpness)));
        assertTrue(filter.matches(Material.ENCHANTED_BOOK, Set.of(sharpness, unbreaking)));
        assertFalse(filter.matches(Material.ENCHANTED_BOOK, Set.of(unbreaking)));
        assertFalse(filter.matches(Material.BOOK, Set.of()));
    }

    @Test
    void serializedEnchantedBookFilterKeepsEnchantmentKeysButNotLevels() {
        NamespacedKey sharpness = key("minecraft:sharpness");
        PipeFilter filter = new PipeFilter(Material.ENCHANTED_BOOK, Set.of(sharpness));

        assertEquals(filter, PipeFilter.deserialize(filter.serialize()).orElseThrow());
    }

    @Test
    void legacyMaterialFilterCanStillBeDeserialized() {
        assertEquals(new PipeFilter(Material.DIAMOND, Set.of()),
                PipeFilter.deserialize("DIAMOND").orElseThrow());
    }

    private static ItemStack item(Material material) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(material);
        return item;
    }

    private static NamespacedKey key(String serialized) {
        return Objects.requireNonNull(NamespacedKey.fromString(serialized));
    }
}
