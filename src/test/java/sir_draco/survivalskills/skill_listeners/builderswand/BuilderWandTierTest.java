package sir_draco.survivalskills.skill_listeners.builderswand;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuilderWandTierTest {

    @Test
    void unlocksReachAndPlacementModesAtSpecifiedLevels() {
        assertEquals(List.of(), BuilderWandTier.unlockedAt(31));
        assertEquals(List.of(BuilderWandTier.BASIC), BuilderWandTier.unlockedAt(32));
        assertEquals(List.of(BuilderWandTier.BASIC, BuilderWandTier.REINFORCED),
                BuilderWandTier.unlockedAt(48));
        assertEquals(List.of(BuilderWandTier.BASIC, BuilderWandTier.REINFORCED, BuilderWandTier.MASTER),
                BuilderWandTier.unlockedAt(55));
        assertEquals(4, BuilderWandTier.highestUnlockedAt(55).getReachBonus());
    }

    @Test
    void cyclesOnlyThroughUnlockedModes() {
        assertEquals(BuilderWandTier.BASIC, BuilderWandTier.nextMode(BuilderWandTier.BASIC, 32));
        assertEquals(BuilderWandTier.REINFORCED, BuilderWandTier.nextMode(BuilderWandTier.BASIC, 48));
        assertEquals(BuilderWandTier.BASIC, BuilderWandTier.nextMode(BuilderWandTier.REINFORCED, 48));
        assertEquals(BuilderWandTier.MASTER, BuilderWandTier.nextMode(BuilderWandTier.REINFORCED, 55));
    }

    @Test
    void clampsStoredModeWhenPlayerNoLongerHasHigherTier() {
        assertEquals(BuilderWandTier.BASIC, BuilderWandTier.modeFor(2, 32));
        assertEquals(BuilderWandTier.REINFORCED, BuilderWandTier.modeFor(2, 48));
        assertEquals(BuilderWandTier.MASTER, BuilderWandTier.modeFor(2, 55));
    }
}
