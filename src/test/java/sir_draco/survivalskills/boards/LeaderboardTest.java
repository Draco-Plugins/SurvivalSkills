package sir_draco.survivalskills.boards;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.rewards.PlayerRewards;
import sir_draco.survivalskills.skills.Skill;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.skills.SkillManager;
import sir_draco.survivalskills.skills.SkillsHolder;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LeaderboardTest {

    private final YamlConfiguration leaderboardData = new YamlConfiguration();
    private final Map<UUID, LeaderboardPlayer> leaderboardTracker = new HashMap<>();
    private Map<UUID, SkillsHolder> playerSkills;
    private Field pluginInstanceField;
    private SurvivalSkills previousPluginInstance;

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        pluginInstanceField = SurvivalSkills.class.getDeclaredField("instance");
        pluginInstanceField.setAccessible(true);
        previousPluginInstance = (SurvivalSkills) pluginInstanceField.get(null);

        SurvivalSkills plugin = mock(SurvivalSkills.class);
        when(plugin.getLeaderboardData()).thenReturn(leaderboardData);
        when(plugin.getLeaderboardTracker()).thenReturn(leaderboardTracker);
        pluginInstanceField.set(null, plugin);

        Field playerSkillsField = SkillManager.class.getDeclaredField("playerSkills");
        playerSkillsField.setAccessible(true);
        playerSkills = getPlayerSkills(playerSkillsField);
        playerSkills.clear();
    }

    @AfterEach
    void tearDown() throws IllegalAccessException {
        playerSkills.clear();
        pluginInstanceField.set(null, previousPluginInstance);
    }

    @Test
    void createsLiveSkillScoresWithoutPersistedLeaderboardData() {
        UUID uuid = UUID.randomUUID();
        Player player = createPlayer(uuid, "Miner");
        loadSkills(uuid, SkillCategory.MINING, 12);

        LeaderboardPlayer leaderboardPlayer = Leaderboard.createLeaderboardPlayer(player);

        assertEquals(12, leaderboardPlayer.getScore(SkillCategory.MINING));
        assertEquals(19, leaderboardPlayer.getScore());
        assertEquals(0, leaderboardPlayer.getScore(SkillCategory.DEATHS));
    }

    @Test
    void joiningPlayerRefreshesLiveSkillsAndKeepsPersistedDeaths() {
        UUID uuid = UUID.randomUUID();
        Player player = createPlayer(uuid, "ReturningMiner");
        loadSkills(uuid, SkillCategory.MINING, 12);
        leaderboardData.set(uuid + ".Deaths", 4);

        LeaderboardPlayer stalePlayer = leaderboardPlayer("ReturningMiner", SkillCategory.MINING, 0);
        leaderboardTracker.put(uuid, stalePlayer);

        LeaderboardPlayer refreshedPlayer = Leaderboard.initializeLeaderboardForPlayer(player);

        assertNotSame(stalePlayer, refreshedPlayer);
        assertEquals(12, refreshedPlayer.getScore(SkillCategory.MINING));
        assertEquals(19, refreshedPlayer.getScore());
        assertEquals(4, refreshedPlayer.getScore(SkillCategory.DEATHS));
    }

    @Test
    void deathsLeaderboardRanksMostDeathsFirstAndIncludesZeroScores() {
        leaderboardTracker.put(UUID.randomUUID(), leaderboardPlayer("NoDeaths", SkillCategory.DEATHS, 0));
        leaderboardTracker.put(UUID.randomUUID(), leaderboardPlayer("SomeDeaths", SkillCategory.DEATHS, 3));
        leaderboardTracker.put(UUID.randomUUID(), leaderboardPlayer("MostDeaths", SkillCategory.DEATHS, 8));

        List<String> sortedLeaderboard = Leaderboard.sortLeaderboard(SkillCategory.DEATHS, -1);

        assertEquals(3, sortedLeaderboard.size());
        assertTrue(sortedLeaderboard.get(0).contains("MostDeaths"));
        assertTrue(sortedLeaderboard.get(1).contains("SomeDeaths"));
        assertTrue(sortedLeaderboard.get(2).contains("NoDeaths"));
    }

    private Player createPlayer(UUID uuid, String displayName) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getDisplayName()).thenReturn(displayName);
        return player;
    }

    private void loadSkills(UUID uuid, SkillCategory raisedCategory, int raisedLevel) {
        ArrayList<Skill> skills = SkillCategory.mainSkills().stream()
                .map((SkillCategory skillCategory) -> new Skill(0,
                        skillCategory == raisedCategory ? raisedLevel : 1, skillCategory))
                .collect(ArrayList::new,
                        (ArrayList<Skill> loadedSkills, Skill skill) -> loadedSkills.add(skill),
                        (ArrayList<Skill> firstSkills, ArrayList<Skill> secondSkills) -> firstSkills.addAll(secondSkills));
        playerSkills.put(uuid, new SkillsHolder(skills, new PlayerRewards()));
    }

    private static LeaderboardPlayer leaderboardPlayer(String name, SkillCategory category, int score) {
        EnumMap<SkillCategory, Integer> scores = new EnumMap<>(SkillCategory.class);
        scores.put(category, score);
        return new LeaderboardPlayer(name, scores);
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, SkillsHolder> getPlayerSkills(Field playerSkillsField) throws IllegalAccessException {
        return (Map<UUID, SkillsHolder>) playerSkillsField.get(null);
    }
}
