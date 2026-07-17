package sir_draco.survivalskills.super_enchanting;

import org.junit.jupiter.api.Test;
import sir_draco.survivalskills.super_enchanting.SuperEnchantingRules.UpgradeCost;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuperEnchantingRulesTest {

    @Test
    void mapsSpecifiedBaseTiersToSuperMaximums() {
        assertEquals(8, SuperEnchantingRules.getMaximumLevel(5));
        assertEquals(5, SuperEnchantingRules.getMaximumLevel(3));
        assertEquals(4, SuperEnchantingRules.getMaximumLevel(2));
        assertEquals(1, SuperEnchantingRules.getMaximumLevel(1));
        assertEquals(4, SuperEnchantingRules.getMaximumLevel(4));
    }

    @Test
    void assignsCostsByLevelAboveVanillaMaximum() {
        assertEquals(Optional.of(new UpgradeCost(1, 30)), SuperEnchantingRules.getUpgradeCost(5, 5));
        assertEquals(Optional.of(new UpgradeCost(5, 50)), SuperEnchantingRules.getUpgradeCost(5, 6));
        assertEquals(Optional.of(new UpgradeCost(20, 100)), SuperEnchantingRules.getUpgradeCost(5, 7));
        assertTrue(SuperEnchantingRules.getUpgradeCost(5, 8).isEmpty());
        assertTrue(SuperEnchantingRules.getUpgradeCost(5, 4).isEmpty());
    }

    @Test
    void limitsLowerBaseTiersToTheirSpecifiedMaximums() {
        assertEquals(Optional.of(new UpgradeCost(1, 30)), SuperEnchantingRules.getUpgradeCost(3, 3));
        assertEquals(Optional.of(new UpgradeCost(5, 50)), SuperEnchantingRules.getUpgradeCost(3, 4));
        assertTrue(SuperEnchantingRules.getUpgradeCost(3, 5).isEmpty());
        assertTrue(SuperEnchantingRules.getUpgradeCost(1, 1).isEmpty());
    }

    @Test
    void awardsFragmentsForEachTrialDifficulty() {
        assertEquals(1, SuperEnchantingRules.getTrialFragmentReward(1));
        assertEquals(3, SuperEnchantingRules.getTrialFragmentReward(2));
        assertEquals(7, SuperEnchantingRules.getTrialFragmentReward(3));
        assertEquals(12, SuperEnchantingRules.getTrialFragmentReward(4));
        assertEquals(25, SuperEnchantingRules.getTrialFragmentReward(5));
    }
}
