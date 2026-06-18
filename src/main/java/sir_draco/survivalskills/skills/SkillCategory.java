package sir_draco.survivalskills.skills;

import java.util.List;

public enum SkillCategory {
    ALL,
    BUILDING,
    CRAFTING,
    EXPLORING,
    FARMING,
    FIGHTING,
    FISHING,
    MINING,
    MAIN,
    DEATHS,
    SOLO_TRIALS,
    COOP_TRIALS;

    
    public String getDisplayName() {
        return switch (this) {
            case ALL -> "All";
            case BUILDING -> "Building";
            case CRAFTING -> "Crafting";
            case EXPLORING -> "Exploring";
            case FARMING -> "Farming";
            case FIGHTING -> "Fighting";
            case FISHING -> "Fishing";
            case MINING -> "Mining";
            case MAIN -> "Main";
            case DEATHS -> "Deaths";
            case SOLO_TRIALS -> "Solo Trials";
            case COOP_TRIALS -> "Coop Trials";
        };
    }

    public boolean isMainSkill() {
        return switch (this) {
            case BUILDING, CRAFTING, EXPLORING, FARMING, FIGHTING, FISHING, MINING, MAIN -> true;
            default -> false;
        };
    }


    public boolean isBaseSkill() {
        return switch (this) {
            case BUILDING, CRAFTING, EXPLORING, FARMING, FIGHTING, FISHING, MINING -> true;
            default -> false;
        };
    }

    public boolean isSkillTree() {
        return switch (this) {
            case BUILDING, CRAFTING, EXPLORING, FARMING, FIGHTING, FISHING, MINING, MAIN, DEATHS -> true;
            default -> false;
        };
    }

    public boolean isSkill() {
        return switch (this) {
            case BUILDING, CRAFTING, EXPLORING, FARMING, FIGHTING, FISHING, MINING, MAIN -> true;
            default -> false;
        };
    }


    public String getXpConfigKey() {
        if (!isBaseSkill()) throw new UnsupportedOperationException("Not a main skill: " + this);
        return getDisplayName() + "XP";
    }


    public static boolean isMainSkill(String skillName) {
        try {
            return fromString(skillName).isMainSkill();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }


    public static boolean isBaseSkill(String skillName) {
        try {
            return fromString(skillName).isBaseSkill();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }


    public static boolean isSkillTreeSkill(String skillName) {
        try {
            return fromString(skillName).isSkillTree();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }


    public static boolean isSkill(String skillName) {
        try {
            return fromString(skillName).isSkill();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static List<SkillCategory> baseSkills() {
        return List.of(BUILDING, CRAFTING, EXPLORING, FARMING, FIGHTING, FISHING, MINING);
    }


    public static List<SkillCategory> mainSkills() {
        return List.of(BUILDING, CRAFTING, EXPLORING, FARMING, FIGHTING, FISHING, MINING, MAIN);
    }

    
    public static List<SkillCategory> allSkills() {
        return List.of(BUILDING, CRAFTING, EXPLORING, FARMING, FIGHTING, FISHING, MINING, MAIN, DEATHS, SOLO_TRIALS, COOP_TRIALS);
    }


    public static SkillCategory fromString(String string) {
        for (var category : values()) {
            if (category.name().equalsIgnoreCase(string)) return category;
        }
        throw new IllegalArgumentException("Unknown skill: " + string);
    }
}
