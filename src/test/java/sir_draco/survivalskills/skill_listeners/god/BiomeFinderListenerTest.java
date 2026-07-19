package sir_draco.survivalskills.skill_listeners.god;

import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BiomeFinderListenerTest {

    @Test
    void calculatesPickerPagesForEmptyPartialAndFullPages() {
        assertEquals(1, BiomeFinderListener.calculateTotalPages(0));
        assertEquals(1, BiomeFinderListener.calculateTotalPages(45));
        assertEquals(2, BiomeFinderListener.calculateTotalPages(46));
        assertEquals(2, BiomeFinderListener.calculateTotalPages(90));
    }

    @Test
    void formatsVanillaBiomeKeyAsReadableName() {
        assertEquals("Old Growth Pine Taiga",
                BiomeFinderListener.formatBiomeName(NamespacedKey.minecraft("old_growth_pine_taiga")));
    }

    @Test
    void includesNamespaceForCustomBiomeKey() {
        assertEquals("Crystal Forest (example)",
                BiomeFinderListener.formatBiomeName(new NamespacedKey("example", "crystal_forest")));
    }
}
