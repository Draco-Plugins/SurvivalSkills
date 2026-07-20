package sir_draco.survivalskills.god_questline.trial;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TrialEnchantmentCombinationTest {

    @Test
    void appliesBookLevelToUnenchantedItem() {
        assertEquals(3, TrialEventListener.getCombinedEnchantmentLevel(0, 3, 5));
    }

    @Test
    void incrementsMatchingLevels() {
        assertEquals(3, TrialEventListener.getCombinedEnchantmentLevel(2, 2, 5));
    }

    @Test
    void doesNotDowngradeHigherItemLevel() {
        assertEquals(4, TrialEventListener.getCombinedEnchantmentLevel(4, 2, 5));
    }

    @Test
    void doesNotExceedMaximumLevel() {
        assertEquals(5, TrialEventListener.getCombinedEnchantmentLevel(5, 5, 5));
    }
}
