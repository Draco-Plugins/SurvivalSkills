package sir_draco.survivalskills.skill_listeners.god;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
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

    @Test
    void pendingZapWandStrikeIsPlayerSpecificAndConsumedOnce() {
        PowerOreChallengeListener listener = new PowerOreChallengeListener(false);
        UUID ownerId = UUID.randomUUID();
        UUID otherPlayerId = UUID.randomUUID();
        Location clickedLocation = mock(Location.class);
        listener.trackPendingZapWandStrike(ownerId, clickedLocation);

        assertTrue(listener.consumePendingZapWandStrike(otherPlayerId).isEmpty());
        assertEquals(Optional.of(clickedLocation), listener.consumePendingZapWandStrike(ownerId));
        assertTrue(listener.consumePendingZapWandStrike(ownerId).isEmpty());
    }
}
