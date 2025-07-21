package sir_draco.survivalskills.Skills;

import sir_draco.survivalskills.Rewards.PlayerRewards;

import java.util.ArrayList;

public class SkillsHolder {

    private final ArrayList<Skill> skills = new ArrayList<>();
    private final PlayerRewards playerRewards;

    private boolean maxSkillMessageEnabled = false;
    private double skillMultiplier = 1;

    public SkillsHolder(ArrayList<Skill> skills, PlayerRewards playerRewards) {
        this.skills.addAll(skills);
        this.playerRewards = playerRewards;
    }

    /**
     * Gets the specified skill of a player
     */
    public Skill getSkill(String skillName) {
        if (skills.isEmpty()) {
            Skill skill = new Skill(0, 0, skillName);
            skills.add(skill);
            return skill;
        }
        for (Skill skill : skills) if (skill.getSkillName().equalsIgnoreCase(skillName)) return skill;
        return new Skill(0, 0, skillName);
    }

    public ArrayList<Skill> getSkills() {
        return skills;
    }

    public PlayerRewards getPlayerRewards() {
        return playerRewards;
    }

    public boolean isMaxSkillMessageEnabled() {
        return maxSkillMessageEnabled;
    }

    public void setMaxSkillMessageEnabled(boolean maxSkillMessageEnabled) {
        this.maxSkillMessageEnabled = maxSkillMessageEnabled;
    }

    public double getSkillMultiplier() {
        return skillMultiplier;
    }

    public void setSkillMultiplier(double skillMultiplier) {
        this.skillMultiplier = skillMultiplier;
    }
}
