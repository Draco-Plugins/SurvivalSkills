package sir_draco.survivalskills.skill_listeners.builderswand;

import java.util.Arrays;
import java.util.List;

public enum BuilderWandTier {
    BASIC(32, 0, 5),
    REINFORCED(48, 2, 11),
    MASTER(55, 4, 21);

    private final int requiredLevel;
    private final int reachBonus;
    private final int blockLimit;

    BuilderWandTier(int requiredLevel, int reachBonus, int blockLimit) {
        this.requiredLevel = requiredLevel;
        this.reachBonus = reachBonus;
        this.blockLimit = blockLimit;
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public int getReachBonus() {
        return reachBonus;
    }

    public int getBlockLimit() {
        return blockLimit;
    }

    public static List<BuilderWandTier> unlockedAt(int buildingLevel) {
        return Arrays.stream(values())
                .filter((BuilderWandTier tier) -> buildingLevel >= tier.requiredLevel)
                .toList();
    }

    public static BuilderWandTier highestUnlockedAt(int buildingLevel) {
        return unlockedAt(buildingLevel).stream().reduce((first, second) -> second).orElse(BASIC);
    }

    public static BuilderWandTier modeFor(int storedMode, int buildingLevel) {
        List<BuilderWandTier> unlocked = unlockedAt(buildingLevel);
        if (unlocked.isEmpty()) {
            return BASIC;
        }
        int validMode = Math.max(0, Math.min(storedMode, unlocked.size() - 1));
        return unlocked.get(validMode);
    }

    public static BuilderWandTier nextMode(BuilderWandTier current, int buildingLevel) {
        List<BuilderWandTier> unlocked = unlockedAt(buildingLevel);
        if (unlocked.isEmpty()) {
            return BASIC;
        }
        int currentIndex = unlocked.indexOf(current);
        return unlocked.get((currentIndex + 1) % unlocked.size());
    }
}
