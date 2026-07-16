package sir_draco.survivalskills.skill_listeners;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FarmingSkillWateringCanTest {

    @Test
    void advancesCopperOneOxidationStageAtATime() {
        assertEquals(Optional.of(Material.EXPOSED_COPPER),
                FarmingSkill.getNextOxidationStage(Material.COPPER_BLOCK));
        assertEquals(Optional.of(Material.WEATHERED_COPPER),
                FarmingSkill.getNextOxidationStage(Material.EXPOSED_COPPER));
        assertEquals(Optional.of(Material.OXIDIZED_COPPER),
                FarmingSkill.getNextOxidationStage(Material.WEATHERED_COPPER));
        assertTrue(FarmingSkill.getNextOxidationStage(Material.OXIDIZED_COPPER).isEmpty());
    }

    @Test
    void advancesShapedCopperAndLeavesWaxedCopperUnchanged() {
        assertEquals(Optional.of(Material.EXPOSED_CUT_COPPER_STAIRS),
                FarmingSkill.getNextOxidationStage(Material.CUT_COPPER_STAIRS));
        assertEquals(Optional.of(Material.OXIDIZED_COPPER_BULB),
                FarmingSkill.getNextOxidationStage(Material.WEATHERED_COPPER_BULB));
        assertTrue(FarmingSkill.getNextOxidationStage(Material.WAXED_COPPER_BLOCK).isEmpty());
    }

    @Test
    void advancesAmethystBudsUntilTheyBecomeACluster() {
        assertEquals(Optional.of(Material.MEDIUM_AMETHYST_BUD),
                FarmingSkill.getNextAmethystGrowthStage(Material.SMALL_AMETHYST_BUD));
        assertEquals(Optional.of(Material.LARGE_AMETHYST_BUD),
                FarmingSkill.getNextAmethystGrowthStage(Material.MEDIUM_AMETHYST_BUD));
        assertEquals(Optional.of(Material.AMETHYST_CLUSTER),
                FarmingSkill.getNextAmethystGrowthStage(Material.LARGE_AMETHYST_BUD));
        assertTrue(FarmingSkill.getNextAmethystGrowthStage(Material.AMETHYST_CLUSTER).isEmpty());
    }
}
