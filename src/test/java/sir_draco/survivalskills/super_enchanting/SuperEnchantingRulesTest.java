package sir_draco.survivalskills.super_enchanting;

import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;
import sir_draco.survivalskills.super_enchanting.SuperEnchantingRules.UpgradeCost;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuperEnchantingRulesTest {

    private static final NamespacedKey EFFICIENCY = NamespacedKey.minecraft("efficiency");
    private static final NamespacedKey SHARPNESS = NamespacedKey.minecraft("sharpness");

    @Test
    void mapsSpecifiedBaseTiersToSuperMaximums() {
        assertEquals(8, SuperEnchantingRules.getMaximumLevel(5));
        assertEquals(5, SuperEnchantingRules.getMaximumLevel(3));
        assertEquals(4, SuperEnchantingRules.getMaximumLevel(2));
        assertEquals(1, SuperEnchantingRules.getMaximumLevel(1));
        assertEquals(7, SuperEnchantingRules.getMaximumLevel(4));
    }

    @Test
    void assignsCostsByUpgradeLevel() {
        UpgradeCost vanillaUpgradeCost = new UpgradeCost(1, 20);
        assertEquals(Optional.of(vanillaUpgradeCost), SuperEnchantingRules.getUpgradeCost(5, 1));
        assertEquals(Optional.of(vanillaUpgradeCost), SuperEnchantingRules.getUpgradeCost(5, 2));
        assertEquals(Optional.of(vanillaUpgradeCost), SuperEnchantingRules.getUpgradeCost(5, 3));
        assertEquals(Optional.of(vanillaUpgradeCost), SuperEnchantingRules.getUpgradeCost(5, 4));
        assertEquals(Optional.of(new UpgradeCost(1, 30)), SuperEnchantingRules.getUpgradeCost(5, 5));
        assertEquals(Optional.of(new UpgradeCost(5, 50)), SuperEnchantingRules.getUpgradeCost(5, 6));
        assertEquals(Optional.of(new UpgradeCost(20, 100)), SuperEnchantingRules.getUpgradeCost(5, 7));
        assertTrue(SuperEnchantingRules.getUpgradeCost(5, 8).isEmpty());
        assertTrue(SuperEnchantingRules.getUpgradeCost(5, 0).isEmpty());
    }

    @Test
    void limitsLowerBaseTiersToTheirSpecifiedMaximums() {
        assertEquals(Optional.of(new UpgradeCost(1, 20)), SuperEnchantingRules.getUpgradeCost(3, 1));
        assertEquals(Optional.of(new UpgradeCost(1, 20)), SuperEnchantingRules.getUpgradeCost(3, 2));
        assertEquals(Optional.of(new UpgradeCost(1, 30)), SuperEnchantingRules.getUpgradeCost(3, 3));
        assertEquals(Optional.of(new UpgradeCost(5, 50)), SuperEnchantingRules.getUpgradeCost(3, 4));
        assertTrue(SuperEnchantingRules.getUpgradeCost(3, 5).isEmpty());
        assertTrue(SuperEnchantingRules.getUpgradeCost(1, 1).isEmpty());
    }

    @Test
    void mapsEfficiencyToItsOwnSuperMaximum() {
        assertEquals(10, SuperEnchantingRules.getMaximumLevel(5, EFFICIENCY));
        assertEquals(Optional.of(new UpgradeCost(1, 30)), SuperEnchantingRules.getUpgradeCost(5, EFFICIENCY, 5));
        assertEquals(Optional.of(new UpgradeCost(5, 50)), SuperEnchantingRules.getUpgradeCost(5, EFFICIENCY, 6));
        assertEquals(Optional.of(new UpgradeCost(20, 100)), SuperEnchantingRules.getUpgradeCost(5, EFFICIENCY, 7));
        assertEquals(Optional.of(new UpgradeCost(30, 100)), SuperEnchantingRules.getUpgradeCost(5, EFFICIENCY, 8));
        assertEquals(Optional.of(new UpgradeCost(50, 100)), SuperEnchantingRules.getUpgradeCost(5, EFFICIENCY, 9));
        assertTrue(SuperEnchantingRules.getUpgradeCost(5, EFFICIENCY, 10).isEmpty());
        assertTrue(SuperEnchantingRules.getUpgradeCost(5, EFFICIENCY, 0).isEmpty());
    }

    @Test
    void keepsOtherEnchantmentsAtStandardSuperMaximums() {
        assertEquals(8, SuperEnchantingRules.getMaximumLevel(5, SHARPNESS));
        assertEquals(Optional.of(new UpgradeCost(20, 100)), SuperEnchantingRules.getUpgradeCost(5, SHARPNESS, 7));
        assertTrue(SuperEnchantingRules.getUpgradeCost(5, SHARPNESS, 8).isEmpty());
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
