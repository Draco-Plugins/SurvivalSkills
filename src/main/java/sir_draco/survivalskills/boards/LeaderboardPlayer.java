package sir_draco.survivalskills.boards;

import sir_draco.survivalskills.skills.SkillCategory;

import java.util.EnumMap;

public class LeaderboardPlayer {

    private final String name;
    private EnumMap<SkillCategory, Integer> scores = new EnumMap<>(SkillCategory.class);

    public LeaderboardPlayer(String name, EnumMap<SkillCategory, Integer> scores) {
        this.name = name;
        this.scores = scores;
    }

    public String getName() {
        return name;
    }

    public int getScore() {
        return scores.getOrDefault(SkillCategory.ALL, 0);
    }

    public int getScore(SkillCategory category) {
        return scores.getOrDefault(category, 0);
    }

    public void setScore(SkillCategory category, int score) {
        scores.put(category, score);
    }

    public int getScore(String category) {
        try {
            return scores.getOrDefault(SkillCategory.fromString(category), 0);
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }
}
