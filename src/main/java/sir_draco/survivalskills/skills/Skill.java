package sir_draco.survivalskills.skills;

import sir_draco.survivalskills.SurvivalSkills;

public class Skill {
    public static final int MAX_LEVEL = 100;
    public static final int MAX_EXPERIENCE = 1_000_000;
    private final SkillCategory skillCategory;

    private double experience;
    private int expSoFarInLevel = 0;
    private int rawExperienceForNextLevel;
    private int level;

    public Skill(double experience, int level, SkillCategory skillCategory) {
        this.experience = experience;
        this.level = level;
        this.skillCategory = skillCategory;
        // Store the total XP to go from the current level to the next level
        setExperienceSoFarInLevel();
        setRawExperienceForNextLevel();

    }

    /**
     * Ensures that the experience is in the appropriate range and updates the level accordingly
     */
    public void setExperience(int experience) {
        if (experience > MAX_EXPERIENCE) this.experience = MAX_EXPERIENCE;
        else if (experience < 1) this.experience = 1;
        else this.experience = experience;

        recalculateLevel();
        setRawExperienceForNextLevel();
        setExperienceSoFarInLevel();
    }

    public double getExperience() {
        return experience;
    }

    /**
     * Prevents experience from being added if already at max level
     * Changes the level if appropriate
     * @param experience The amount of experience to change
     * @param currentMaxLevel The max level the skill can be
     * @return True if there is a level up, false otherwise
     */
    public boolean changeExperience(double experience, int currentMaxLevel) {
        if (level >= MAX_LEVEL) return false;
        if (level >= currentMaxLevel) return false;

        if (this.experience + experience > MAX_EXPERIENCE) this.experience = MAX_EXPERIENCE;
        else this.experience += experience;

        if (this.experience < 1) {
            this.experience = 1;
            return false;
        }

        boolean levelChanged = level < getExpectedLevel((int) this.experience);
        if (levelChanged) {
            level++;
            setRawExperienceForNextLevel();
        }
        setExperienceSoFarInLevel();
        return levelChanged;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Ensures that the level is in the appropriate range and updates the experience accordingly
     */
    public void setLevel(int level) {
        if (level > MAX_LEVEL) this.level = MAX_LEVEL;
        else this.level = Math.max(level, 1);

        setRawExperienceForNextLevel();
        experience = experienceForLevel(this.level);
        setExperienceSoFarInLevel();
    }

    /**
     * Changes the level and updates how much XP is needed for the next level
     * If XP is under what is required it manually changes the XP
     * @param level The amount of levels to change by
     * @return True if the level changes, false otherwise
     */
    public boolean changeLevel(int level) {
        if (this.level + level > MAX_LEVEL) {
            this.level = MAX_LEVEL;
            return false;
        }
        else if (this.level + level < 1) {
            this.level = 1;
            return false;
        }
        else this.level += level;

        setRawExperienceForNextLevel();
        experience = experienceForLevel(this.level);
        setExperienceSoFarInLevel();
        return true;
    }

    public SkillCategory getSkillCategory() {
        return skillCategory;
    }

    public void setRawExperienceForNextLevel() {
        rawExperienceForNextLevel = xpRequiredForNextLevel(level);
    }

    public int getRawExperienceForNextLevel() {
        return rawExperienceForNextLevel;
    }

    /**
     * Returns the XP needed to go from the current level to
     * the next level regardless of current XP
     */
    public int xpRequiredForNextLevel(int currentLevel) {
        return experienceForLevel(currentLevel + 1) - experienceForLevel(currentLevel);
    }

    /**
     * Returns the XP needed to go from the current level to
     * the next level taking into account how much XP
     * this skill currently has
     */
    public int xpRemainingForNextLevel(int currentLevel) {
        if (currentLevel == 1) return xpRequiredForNextLevel(currentLevel);
        int soFar = (int) experience - experienceForLevel(currentLevel);
        if (soFar < 0) return xpRequiredForNextLevel(currentLevel);
        return xpRequiredForNextLevel(currentLevel) - soFar;
    }

    public void setExperienceSoFarInLevel() {
        expSoFarInLevel = (int) experience - experienceForLevel(level);
    }

    public int getExperienceSoFarInLevel() {
        return expSoFarInLevel;
    }

    /**
     * Returns the level that a skill would be
     * given a specific amount of total XP
     */
    public int getExpectedLevel(int exp) {
        if (exp < 1) return 1;

        for (int level = 1; level <= MAX_LEVEL; level++) {
            int xpRequiredForLevel = experienceForLevel(level);
            if (exp < xpRequiredForLevel) {
                return Math.max(1, level - 1);
            }
        }
        return MAX_LEVEL;
    }

    private void recalculateLevel() {
        level = Math.min(getExpectedLevel((int) experience), MAX_LEVEL);
    }

    private int experienceForLevel(int level) {
        return SkillMath.totalExperienceForLevel(level, skillCategory, SurvivalSkills.getInstance().isExponentialXP());
    }

    public String toString() {
        return "Skill {" +
                "experience=" + experience +
                ", expSoFarInLevel=" + expSoFarInLevel +
                ", rawExperienceForNextLevel=" + rawExperienceForNextLevel +
                ", level=" + level +
                ", maxLevel=" + MAX_LEVEL +
                ", skillCategory='" + skillCategory.getDisplayName() + '\'' +
                '}';
    }
}
