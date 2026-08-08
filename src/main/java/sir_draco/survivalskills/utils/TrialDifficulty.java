package sir_draco.survivalskills.utils;

import sir_draco.survivalskills.god_questline.trial.WaveGenerator;

final class TrialDifficulty {

    private TrialDifficulty() {
    }

    static int getSoloDifficultyForCoop(int coopTrueDifficulty) {
        return coopTrueDifficulty - 1;
    }

    static String getDifficultyNameFromTrueDifficulty(int trueDifficulty) {
        return WaveGenerator.getDifficultyName((trueDifficulty + 1) / 2);
    }
}
