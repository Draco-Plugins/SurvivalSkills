package sir_draco.survivalskills.abilities.items;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GiantSwordTest {

    @Test
    void dropChanceIncludesExactlyTheFirstTenPercentOfRolls() {
        assertTrue(GiantSword.shouldDrop(0));
        assertTrue(GiantSword.shouldDrop(0.099999));
        assertFalse(GiantSword.shouldDrop(0.1));
        assertFalse(GiantSword.shouldDrop(1));
    }

    @Test
    void doublesVanillaExperienceWithoutOverflowing() {
        assertEquals(0, GiantSword.doubleExperience(0));
        assertEquals(10, GiantSword.doubleExperience(5));
        assertEquals(Integer.MAX_VALUE, GiantSword.doubleExperience(Integer.MAX_VALUE));
    }
}
