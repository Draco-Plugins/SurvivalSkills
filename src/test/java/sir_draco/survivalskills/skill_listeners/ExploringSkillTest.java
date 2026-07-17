package sir_draco.survivalskills.skill_listeners;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExploringSkillTest {

    @Test
    void keepsStepsBelowTheExperienceThreshold() {
        ExploringSkill.MovementExperience result = ExploringSkill.calculateMovementExperience(35, 64);

        assertEquals(0, result.awardableSteps());
        assertEquals(99, result.remainingSteps());
    }

    @Test
    void awardsACompleteThresholdAndPreservesTheRemainder() {
        ExploringSkill.MovementExperience result = ExploringSkill.calculateMovementExperience(90, 25);

        assertEquals(100, result.awardableSteps());
        assertEquals(15, result.remainingSteps());
    }

    @Test
    void awardsEveryCompleteThresholdFromLargeMovementUpdates() {
        ExploringSkill.MovementExperience result = ExploringSkill.calculateMovementExperience(75, 250);

        assertEquals(300, result.awardableSteps());
        assertEquals(25, result.remainingSteps());
    }
}
