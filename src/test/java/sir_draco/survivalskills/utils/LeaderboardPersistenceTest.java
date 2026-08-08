package sir_draco.survivalskills.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sir_draco.survivalskills.boards.LeaderboardPlayer;
import sir_draco.survivalskills.skills.SkillCategory;

import java.io.File;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeaderboardPersistenceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void totalScoreRoundTripsThroughLeaderboardFile() {
        UUID uuid = UUID.randomUUID();
        EnumMap<SkillCategory, Integer> scores = scoresWithMainSkillLevel(2);
        scores.put(SkillCategory.ALL, 16);
        scores.put(SkillCategory.DEATHS, 5);

        Map<UUID, LeaderboardPlayer> leaderboardTracker = new HashMap<>();
        leaderboardTracker.put(uuid, new LeaderboardPlayer("PlayerOne", scores));
        File leaderboardFile = temporaryDirectory.resolve("leaderboard.yml").toFile();

        FileUtils.saveLeaderboard(leaderboardTracker, new YamlConfiguration(), leaderboardFile);

        Map<UUID, LeaderboardPlayer> loadedTracker = new HashMap<>();
        FileUtils.loadLeaderboard(YamlConfiguration.loadConfiguration(leaderboardFile), loadedTracker);
        LeaderboardPlayer loadedPlayer = loadedTracker.get(uuid);
        assertEquals(16, loadedPlayer.getScore());
        assertEquals(5, loadedPlayer.getScore(SkillCategory.DEATHS));
    }

    @Test
    void legacyLeaderboardWithoutTotalCalculatesItFromSavedSkills() {
        UUID uuid = UUID.randomUUID();
        YamlConfiguration leaderboardData = new YamlConfiguration();
        leaderboardData.set(uuid + ".Name", "LegacyPlayer");
        SkillCategory.mainSkills().forEach((SkillCategory skillCategory) -> leaderboardData.set(
                uuid + "." + skillCategory.getDisplayName().replace(" ", ""), 3));

        Map<UUID, LeaderboardPlayer> loadedTracker = new HashMap<>();
        FileUtils.loadLeaderboard(leaderboardData, loadedTracker);

        assertEquals(24, loadedTracker.get(uuid).getScore());
    }

    private static EnumMap<SkillCategory, Integer> scoresWithMainSkillLevel(int level) {
        EnumMap<SkillCategory, Integer> scores = new EnumMap<>(SkillCategory.class);
        SkillCategory.allSkills().forEach((SkillCategory skillCategory) -> scores.put(skillCategory, 0));
        SkillCategory.mainSkills().forEach((SkillCategory skillCategory) -> scores.put(skillCategory, level));
        return scores;
    }
}
