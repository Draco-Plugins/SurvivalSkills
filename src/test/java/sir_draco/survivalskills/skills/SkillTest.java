package sir_draco.survivalskills.skills;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import sir_draco.survivalskills.SurvivalSkills;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class SkillTest {

    private static final int MAX_EXPERIENCE = 1_000_000;
    private static final SkillCategory BASE_CATEGORY = SkillCategory.MINING;

    private SurvivalSkills plugin;
    private MockedStatic<SurvivalSkills> survivalSkills;

    @BeforeEach
    void setUp() {
        plugin = mock(SurvivalSkills.class);
        survivalSkills = mockStatic(SurvivalSkills.class);
        survivalSkills.when(SurvivalSkills::getInstance).thenReturn(plugin);
    }

    @AfterEach
    void tearDown() {
        survivalSkills.close();
    }

    @Nested
    class ExpectedLevelTests {

        @Test
        void mapsBoundariesAndOutOfRangeExperienceForBothCurves() {
            for (boolean exponential : new boolean[] { false, true }) {
                useCurve(exponential);
                Skill skill = skillAtLevel(1, BASE_CATEGORY, exponential);

                assertEquals(1, skill.getExpectedLevel(0));
                assertEquals(1, skill.getExpectedLevel(expectedTotal(1, BASE_CATEGORY, exponential)));
                assertEquals(5, skill.getExpectedLevel(expectedTotal(5, BASE_CATEGORY, exponential)));
                assertEquals(50, skill.getExpectedLevel(expectedTotal(50, BASE_CATEGORY, exponential)));
                assertEquals(Skill.MAX_LEVEL, skill.getExpectedLevel(MAX_EXPERIENCE));
                assertEquals(Skill.MAX_LEVEL, skill.getExpectedLevel(MAX_EXPERIENCE + 1));
            }
        }
    }

    @Nested
    class NextLevelExperienceTests {

        @Test
        void rawGapIsPositiveAndMatchesKnownPairsForBothCurves() {
            for (boolean exponential : new boolean[] { false, true }) {
                useCurve(exponential);
                Skill skill = skillAtLevel(1, BASE_CATEGORY, exponential);

                for (int level = 1; level < Skill.MAX_LEVEL; level++)
                    assertTrue(skill.xpRequiredForNextLevel(level) >= 0, "level " + level);

                assertEquals(expectedGap(1, BASE_CATEGORY, exponential), skill.xpRequiredForNextLevel(1));
                assertEquals(expectedGap(99, BASE_CATEGORY, exponential), skill.xpRequiredForNextLevel(99));
            }
        }

        @Test
        void remainingExperienceReflectsStartPartialProgressAndLevelOneSpecialCase() {
            useCurve(true);
            int level = 10;
            int start = expectedTotal(level, BASE_CATEGORY, true);
            int gap = expectedGap(level, BASE_CATEGORY, true);
            Skill atStart = new Skill(start, level, BASE_CATEGORY);
            Skill partial = new Skill(start + 25, level, BASE_CATEGORY);
            Skill levelOneWithProgress = new Skill(expectedTotal(1, BASE_CATEGORY, true) + 1, 1, BASE_CATEGORY);

            assertEquals(gap, atStart.xpRemainingForNextLevel(level));
            assertEquals(gap - 25, partial.xpRemainingForNextLevel(level));
            assertEquals(expectedGap(1, BASE_CATEGORY, true), levelOneWithProgress.xpRemainingForNextLevel(1));
        }
    }

    @Nested
    class SetterTests {

        @Test
        void setExperienceAssignsClampsAndRecalculatesLevel() {
            useCurve(true);
            Skill skill = skillAtLevel(1, BASE_CATEGORY, true);

            skill.setExperience(500);
            assertEquals(skill.getExpectedLevel(500), skill.getLevel());
            assertEquals(500, skill.getExperience());

            skill.setExperience(0);
            assertEquals(1, skill.getExperience());
            assertEquals(1, skill.getLevel());

            skill.setExperience(2_000_000);
            assertEquals(MAX_EXPERIENCE, skill.getExperience());
            assertEquals(Skill.MAX_LEVEL, skill.getLevel());
        }

        @Test
        void setLevelAssignsClampsAndRecalculatesExperience() {
            useCurve(true);
            Skill skill = skillAtLevel(1, BASE_CATEGORY, true);

            assertLevelAndExperience(skill, 1, BASE_CATEGORY, true);
            skill.setLevel(50);
            assertLevelAndExperience(skill, 50, BASE_CATEGORY, true);
            skill.setLevel(101);
            assertLevelAndExperience(skill, Skill.MAX_LEVEL, BASE_CATEGORY, true);
            skill.setLevel(0);
            assertLevelAndExperience(skill, 1, BASE_CATEGORY, true);
        }
    }

    @Nested
    class ChangeExperienceTests {

        @Test
        void gainBelowThresholdDoesNotLevelUp() {
            useCurve(true);
            Skill skill = skillAtLevel(10, BASE_CATEGORY, true);

            assertFalse(skill.changeExperience(1, Skill.MAX_LEVEL));
            assertEquals(10, skill.getLevel());
            assertEquals(expectedTotal(10, BASE_CATEGORY, true) + 1, skill.getExperience());
        }

        @Test
        void gainAcrossThresholdLevelsUpAndLeavesConsistentState() {
            useCurve(true);
            Skill skill = skillAtLevel(10, BASE_CATEGORY, true);
            int gain = expectedGap(10, BASE_CATEGORY, true);

            assertTrue(skill.changeExperience(gain, Skill.MAX_LEVEL));
            assertEquals(11, skill.getLevel());
            assertEquals(expectedTotal(11, BASE_CATEGORY, true), skill.getExperience());
            assertEquals(skill.getExpectedLevel((int) skill.getExperience()), skill.getLevel());
        }

        @Test
        void gainIsRejectedAtAbsoluteAndCurrentLevelCaps() {
            useCurve(true);
            Skill maximum = skillAtLevel(Skill.MAX_LEVEL, BASE_CATEGORY, true);
            double maximumExperience = maximum.getExperience();
            Skill capped = skillAtLevel(10, BASE_CATEGORY, true);
            double cappedExperience = capped.getExperience();

            assertFalse(maximum.changeExperience(100, Skill.MAX_LEVEL));
            assertEquals(maximumExperience, maximum.getExperience());
            assertFalse(capped.changeExperience(100, 10));
            assertEquals(cappedExperience, capped.getExperience());
        }

        @Test
        void lossBelowMinimumClampsWithoutLevelChange() {
            useCurve(true);
            Skill skill = skillAtLevel(1, BASE_CATEGORY, true);

            assertFalse(skill.changeExperience(-100, Skill.MAX_LEVEL));
            assertEquals(1, skill.getExperience());
            assertEquals(1, skill.getLevel());
        }

        @Test
        void oversizedGainClampsToMaximumExperience() {
            useCurve(false);
            Skill skill = skillAtLevel(99, BASE_CATEGORY, false);

            assertTrue(skill.changeExperience(2_000_000, Skill.MAX_LEVEL));
            assertEquals(MAX_EXPERIENCE, skill.getExperience());
            assertEquals(Skill.MAX_LEVEL, skill.getLevel());
        }
    }

    @Nested
    class ChangeLevelTests {

        @Test
        void positiveAndNegativeChangesRecalculateExperience() {
            useCurve(true);
            Skill skill = skillAtLevel(50, BASE_CATEGORY, true);

            assertTrue(skill.changeLevel(1));
            assertLevelAndExperience(skill, 51, BASE_CATEGORY, true);

            assertTrue(skill.changeLevel(-1));
            assertLevelAndExperience(skill, 50, BASE_CATEGORY, true);
        }

        @Test
        void changesBeyondBoundsClampAndReturnFalse() {
            useCurve(true);
            Skill upper = skillAtLevel(50, BASE_CATEGORY, true);
            Skill lower = skillAtLevel(50, BASE_CATEGORY, true);

            assertFalse(upper.changeLevel(200));
            assertEquals(Skill.MAX_LEVEL, upper.getLevel());
            assertFalse(lower.changeLevel(-200));
            assertEquals(1, lower.getLevel());
        }
    }

    @Test
    void categoryVariantsUseDifferentLevelOneHundredClamping() {
        useCurve(false);
        Skill main = skillAtLevel(Skill.MAX_LEVEL, SkillCategory.MAIN, false);
        Skill base = skillAtLevel(Skill.MAX_LEVEL, BASE_CATEGORY, false);

        assertEquals(MAX_EXPERIENCE, main.getExperience());
        assertEquals(expectedTotal(Skill.MAX_LEVEL, BASE_CATEGORY, false), base.getExperience());
        assertTrue(base.getExperience() <= MAX_EXPERIENCE);
    }

    private void useCurve(boolean exponential) {
        when(plugin.isExponentialXP()).thenReturn(exponential);
    }

    private Skill skillAtLevel(int level, SkillCategory category, boolean exponential) {
        return new Skill(expectedTotal(level, category, exponential), level, category);
    }

    private void assertLevelAndExperience(Skill skill, int level, SkillCategory category, boolean exponential) {
        assertEquals(level, skill.getLevel());
        assertEquals(expectedTotal(level, category, exponential), skill.getExperience());
    }

    private int expectedGap(int level, SkillCategory category, boolean exponential) {
        return expectedTotal(level + 1, category, exponential) - expectedTotal(level, category, exponential);
    }

    private int expectedTotal(int level, SkillCategory category, boolean exponential) {
        double sum = 0;
        for (int currentLevel = 1; currentLevel <= level; currentLevel++) {
            sum += exponential
                    ? Math.pow(currentLevel, SkillManager.exponentialScalar)
                    : Math.log(currentLevel) * SkillManager.scalar;
        }
        if (level >= Skill.MAX_LEVEL && category == SkillCategory.MAIN) return MAX_EXPERIENCE;
        return (int) Math.floor(Math.min(sum, MAX_EXPERIENCE));
    }
}
