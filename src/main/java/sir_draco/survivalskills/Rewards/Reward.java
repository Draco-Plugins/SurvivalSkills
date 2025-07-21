package sir_draco.survivalskills.Rewards;

public class Reward {

    private final String skillType;
    private final String name;
    private final String rewardType;

    private int level;
    private boolean enabled;
    private boolean applied = false;

    public Reward(String skillType, String name, String rewardType, int level, boolean enabled) {
        this.skillType = skillType;
        this.name = name;
        this.rewardType = rewardType;
        this.level = level;
        this.enabled = enabled;
    }

    public Reward copyReward() {
        return new Reward(skillType, name, rewardType, level, enabled);
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

    public String getSkillType() {
        return skillType;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (applied) applied = false;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
