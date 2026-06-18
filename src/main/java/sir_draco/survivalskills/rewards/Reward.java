package sir_draco.survivalskills.rewards;

import sir_draco.survivalskills.skills.SkillCategory;

public class Reward {

    private final SkillCategory skillCategory;
    private final String name;
    private final String rewardType;

    private int level;
    private boolean enabled;
    private boolean applied = false;

    public Reward(SkillCategory skillCategory, String name, String rewardType, int level, boolean enabled) {
        this.skillCategory = skillCategory;
        this.name = name;
        this.rewardType = rewardType;
        this.level = level;
        this.enabled = enabled;
    }

    public Reward copyReward() {
        return new Reward(skillCategory, name, rewardType, level, enabled);
    }

    public void applyReward() {
        applied = true;
    }

    public boolean isApplied() {
        return applied;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getLevel() {
        return level;
    }

    public String getName() {
        return name;
    }

    public SkillCategory getSkillCategory() {
        return skillCategory;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (applied) applied = false;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
