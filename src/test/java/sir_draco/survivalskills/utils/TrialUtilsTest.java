package sir_draco.survivalskills.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TrialUtilsTest {

    @Test
    void coopDifficultyRequiresSoloDifficultyWithTheSameName() {
        assertAll(
                () -> assertEquals(1, TrialDifficulty.getSoloDifficultyForCoop(2)),
                () -> assertEquals(3, TrialDifficulty.getSoloDifficultyForCoop(4)),
                () -> assertEquals(5, TrialDifficulty.getSoloDifficultyForCoop(6)),
                () -> assertEquals(7, TrialDifficulty.getSoloDifficultyForCoop(8)),
                () -> assertEquals(9, TrialDifficulty.getSoloDifficultyForCoop(10))
        );
    }

    @Test
    void trueDifficultyUsesTheCorrectDisplayName() {
        assertAll(
                () -> assertEquals("Easy", TrialDifficulty.getDifficultyNameFromTrueDifficulty(1)),
                () -> assertEquals("Medium", TrialDifficulty.getDifficultyNameFromTrueDifficulty(3)),
                () -> assertEquals("Hard", TrialDifficulty.getDifficultyNameFromTrueDifficulty(5)),
                () -> assertEquals("God", TrialDifficulty.getDifficultyNameFromTrueDifficulty(7)),
                () -> assertEquals("Death", TrialDifficulty.getDifficultyNameFromTrueDifficulty(9))
        );
    }
}
