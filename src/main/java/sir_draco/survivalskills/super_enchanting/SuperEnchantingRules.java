package sir_draco.survivalskills.super_enchanting;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;

import java.util.Optional;

public final class SuperEnchantingRules {

    private static final int EFFICIENCY_MAX_LEVEL = 10;
    private static final NamespacedKey EFFICIENCY_KEY = NamespacedKey.minecraft("efficiency");

    private static final UpgradeCost VANILLA_UPGRADE = new UpgradeCost(1, 20);
    private static final UpgradeCost FIRST_UPGRADE = new UpgradeCost(1, 30);
    private static final UpgradeCost SECOND_UPGRADE = new UpgradeCost(5, 50);
    private static final UpgradeCost THIRD_UPGRADE = new UpgradeCost(20, 100);
    private static final UpgradeCost FOURTH_UPGRADE = new UpgradeCost(30, 100);
    private static final UpgradeCost FIFTH_UPGRADE = new UpgradeCost(50, 100);

    private SuperEnchantingRules() {
    }

    @SuppressWarnings("deprecation")
    public static int getMaximumLevel(Enchantment enchantment) {
        return getMaximumLevel(enchantment.getMaxLevel(), enchantment.getKey());
    }

    public static int getMaximumLevel(int baseMaximumLevel) {
        return switch (baseMaximumLevel) {
            case 5 -> 8;
            case 4 -> 7;
            case 3 -> 5;
            case 2 -> 4;
            default -> baseMaximumLevel;
        };
    }

    static int getMaximumLevel(int baseMaximumLevel, NamespacedKey enchantmentKey) {
        if (EFFICIENCY_KEY.equals(enchantmentKey))
            return EFFICIENCY_MAX_LEVEL;
        return getMaximumLevel(baseMaximumLevel);
    }

    @SuppressWarnings("deprecation")
    public static Optional<UpgradeCost> getUpgradeCost(Enchantment enchantment, int currentLevel) {
        return getUpgradeCost(enchantment.getMaxLevel(), enchantment.getKey(), currentLevel);
    }

    public static Optional<UpgradeCost> getUpgradeCost(int baseMaximumLevel, int currentLevel) {
        return getUpgradeCost(baseMaximumLevel, getMaximumLevel(baseMaximumLevel), currentLevel);
    }

    static Optional<UpgradeCost> getUpgradeCost(int baseMaximumLevel, NamespacedKey enchantmentKey,
                                                int currentLevel) {
        return getUpgradeCost(baseMaximumLevel, getMaximumLevel(baseMaximumLevel, enchantmentKey), currentLevel);
    }

    private static Optional<UpgradeCost> getUpgradeCost(int baseMaximumLevel, int maximumLevel, int currentLevel) {
        if (currentLevel < 1 || currentLevel >= maximumLevel)
            return Optional.empty();
        if (currentLevel < baseMaximumLevel)
            return Optional.of(VANILLA_UPGRADE);

        return switch (currentLevel - baseMaximumLevel + 1) {
            case 1 -> Optional.of(FIRST_UPGRADE);
            case 2 -> Optional.of(SECOND_UPGRADE);
            case 3 -> Optional.of(THIRD_UPGRADE);
            case 4 -> Optional.of(FOURTH_UPGRADE);
            case 5 -> Optional.of(FIFTH_UPGRADE);
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
