package sir_draco.survivalskills.skill_listeners.god;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static sir_draco.survivalskills.skill_listeners.god.PowerOreChallengeListener.ForgePrerequisite.ACTIVE_CHALLENGE;
import static sir_draco.survivalskills.skill_listeners.god.PowerOreChallengeListener.ForgePrerequisite.EXPERIENCE_LEVELS;
import static sir_draco.survivalskills.skill_listeners.god.PowerOreChallengeListener.ForgePrerequisite.OVERWORLD;
import static sir_draco.survivalskills.skill_listeners.god.PowerOreChallengeListener.ForgePrerequisite.READY;
import static sir_draco.survivalskills.skill_listeners.god.PowerOreChallengeListener.ForgePrerequisite.STAND_ON_CLICKED_OBSIDIAN;
import static sir_draco.survivalskills.skill_listeners.god.PowerOreChallengeListener.ForgePrerequisite.UNLOCK_POWER_ORE;

class PowerOreChallengeListenerTest {

    @Test
    void positioningIsTheFirstForgeHint() {
        assertEquals(STAND_ON_CLICKED_OBSIDIAN,
                PowerOreChallengeListener.firstMissingForgePrerequisite(false, false, false, false, true));
    }

    @Test
    void unlockIsCheckedAfterPositioning() {
        assertEquals(UNLOCK_POWER_ORE,
                PowerOreChallengeListener.firstMissingForgePrerequisite(true, false, false, false, true));
    }

    @Test
    void experienceIsCheckedAfterUnlock() {
        assertEquals(EXPERIENCE_LEVELS,
                PowerOreChallengeListener.firstMissingForgePrerequisite(true, true, false, false, true));
    }

    @Test
    void overworldIsCheckedAfterExperience() {
        assertEquals(OVERWORLD,
                PowerOreChallengeListener.firstMissingForgePrerequisite(true, true, true, false, true));
    }

    @Test
    void activeChallengeIsCheckedLast() {
        assertEquals(ACTIVE_CHALLENGE,
                PowerOreChallengeListener.firstMissingForgePrerequisite(true, true, true, true, true));
    }

    @Test
    void satisfiedPrerequisitesPrepareAForge() {
        assertEquals(READY,
                PowerOreChallengeListener.firstMissingForgePrerequisite(true, true, true, true, false));
    }
}
