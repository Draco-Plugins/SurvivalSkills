package sir_draco.survivalskills.skills;

public final class SkillMath {
    public static final double SCALAR = 2749.22119298367;
    public static final double EXPONENTIAL_SCALAR = 2.260176;

    private SkillMath() {
    }

    public static int totalExperienceForLevel(int level, SkillCategory skillCategory, boolean exponentialXp) {
        double sum = 0;
        for (int currentLevel = 1; currentLevel <= level; currentLevel++) {
            sum += exponentialXp
                    ? Math.pow(currentLevel, EXPONENTIAL_SCALAR)
                    : Math.log(currentLevel) * SCALAR;
        }

        if (level >= Skill.MAX_LEVEL
                && (skillCategory == SkillCategory.MAIN || sum > Skill.MAX_EXPERIENCE)) {
            return Skill.MAX_EXPERIENCE;
        }
        return (int) Math.floor(Math.min(sum, Skill.MAX_EXPERIENCE));
    }
}
