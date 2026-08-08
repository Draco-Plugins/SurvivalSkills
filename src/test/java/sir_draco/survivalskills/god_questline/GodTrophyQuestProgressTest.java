package sir_draco.survivalskills.god_questline;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GodTrophyQuestProgressTest {

    @Test
    void stepMetadataCoversEveryActivePhase() {
        IntStream.range(0, 59).forEach((int phase) -> {
            Optional<GodTrophyQuest.QuestProgress> progress =
                    GodTrophyQuest.progressForPhase(phase, 0, true);

            assertTrue(progress.isPresent(), "Missing progress metadata for phase " + phase);
            assertFalse(progress.get().currentStep().isBlank());
            assertTrue(progress.get().goal() > 0);
            assertFalse(progress.get().nextStep().isBlank());
        });

        assertTrue(GodTrophyQuest.progressForPhase(-1, 0, true).isEmpty());
        assertTrue(GodTrophyQuest.progressForPhase(59, 0, true).isEmpty());
    }

    @Test
    void bulkAndVillagerGoalsMatchQuestRequirements() {
        assertProgress(1, 2_000, "Bread", "5,000 carrots");
        assertProgress(12, 64, "Cakes", "500 coal blocks");
        assertProgress(13, 500, "Coal Blocks", "500 copper blocks");
        assertProgress(22, 1, "Modified Turtle Helmet", "Goat Horn");
        assertProgress(47, 1_000, "Villager Trades", "Warrior Emblem");
        assertProgress(58, 1, "All Minecraft Advancements", "Trial of the Gods");
    }

    @Test
    void displayedCountIsClampedToTheCurrentGoal() {
        GodTrophyQuest.QuestProgress negative = GodTrophyQuest.progressForPhase(1, -20, true).orElseThrow();
        GodTrophyQuest.QuestProgress excessive = GodTrophyQuest.progressForPhase(1, 3_000, true).orElseThrow();

        assertEquals(0, negative.currentCount());
        assertEquals(2_000, excessive.currentCount());
    }

    @Test
    void disabledAdvancementsShowsTheTrialAsTheNextStep() {
        GodTrophyQuest.QuestProgress progress = GodTrophyQuest.progressForPhase(57, 0, false).orElseThrow();

        assertEquals("Trial of the Gods", progress.nextStep());
    }

    @Test
    void completedGroupPercentagesCoverEveryBoundary() {
        List<Integer> completedPhases = List.of(13, 22, 27, 45, 47, 48, 49, 58, 59);
        List<Integer> expectedPercentages = List.of(11, 22, 33, 44, 56, 67, 78, 89, 100);

        List<Integer> actualPercentages = completedPhases.stream()
                .map((Integer phase) -> GodTrophyQuest.completionPercentage(phase))
                .toList();

        assertEquals(expectedPercentages, actualPercentages);
    }

    @Test
    void introAndWithinGroupTransitionsDoNotCompleteMilestones() {
        assertTrue(GodTrophyQuest.completedPercentageForTransition(0, 1).isEmpty());
        assertTrue(GodTrophyQuest.completedPercentageForTransition(1, 2).isEmpty());
        assertEquals(OptionalInt.of(11), GodTrophyQuest.completedPercentageForTransition(12, 13));
    }

    @Test
    void disabledAdvancementTransitionEndsAtOneHundredPercent() {
        assertEquals(OptionalInt.of(100), GodTrophyQuest.completedPercentageForTransition(57, 59));
    }

    private static void assertProgress(int phase, int goal, String currentStep, String nextStep) {
        GodTrophyQuest.QuestProgress progress = GodTrophyQuest.progressForPhase(phase, 0, true).orElseThrow();
        assertEquals(goal, progress.goal());
        assertEquals(currentStep, progress.currentStep());
        assertEquals(nextStep, progress.nextStep());
    }
}
