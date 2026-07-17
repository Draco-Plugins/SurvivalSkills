package sir_draco.survivalskills.super_enchanting;

import java.util.Optional;

public final class SuperEnchantingRules {

    private static final UpgradeCost FIRST_UPGRADE = new UpgradeCost(1, 30);
    private static final UpgradeCost SECOND_UPGRADE = new UpgradeCost(5, 50);
    private static final UpgradeCost THIRD_UPGRADE = new UpgradeCost(20, 100);

    private SuperEnchantingRules() {
    }

    public static int getMaximumLevel(int baseMaximumLevel) {
        return switch (baseMaximumLevel) {
            case 5 -> 8;
            case 3 -> 5;
            case 2 -> 4;
            default -> baseMaximumLevel;
        };
    }

    public static Optional<UpgradeCost> getUpgradeCost(int baseMaximumLevel, int currentLevel) {
        if (currentLevel < baseMaximumLevel || currentLevel >= getMaximumLevel(baseMaximumLevel))
            return Optional.empty();

        return switch (currentLevel - baseMaximumLevel + 1) {
            case 1 -> Optional.of(FIRST_UPGRADE);
            case 2 -> Optional.of(SECOND_UPGRADE);
            case 3 -> Optional.of(THIRD_UPGRADE);
            default -> Optional.empty();
        };
    }

    public static int getTrialFragmentReward(int difficulty) {
        return switch (difficulty) {
            case 1 -> 1;
            case 2 -> 3;
            case 3 -> 7;
            case 4 -> 12;
            case 5 -> 25;
            default -> throw new IllegalArgumentException("Unknown trial difficulty: " + difficulty);
        };
    }

    public record UpgradeCost(int fragmentCost, int levelCost) {
        public UpgradeCost {
            if (fragmentCost < 1 || levelCost < 1)
                throw new IllegalArgumentException("Upgrade costs must be positive");
        }
    }
}
