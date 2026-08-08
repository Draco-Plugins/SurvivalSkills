package sir_draco.survivalskills.boards;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import sir_draco.survivalskills.god_questline.trial.TrialSpectatorManager;
import sir_draco.survivalskills.skills.SkillCategory;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SkillScoreboardTest {

    @AfterEach
    void tearDown() {
        TrialSpectatorManager.getInstance().clearAll();
    }

    @Test
    void spectatorScoreboardUsesUniqueRowsAndDisplaysCurrentAndMaximumStats() {
        Player spectator = mock(Player.class);
        Scoreboard board = mock(Scoreboard.class);
        Objective objective = mock(Objective.class);
        Score scoreboardScore = mock(Score.class);
        Team healthTeam = mock(Team.class);
        Team foodTeam = mock(Team.class);
        Team scoreTeam = mock(Team.class);
        Team timeTeam = mock(Team.class);

        when(board.registerNewTeam("Health")).thenReturn(healthTeam);
        when(board.registerNewTeam("Food")).thenReturn(foodTeam);
        when(board.registerNewTeam("Score")).thenReturn(scoreTeam);
        when(board.registerNewTeam("Time")).thenReturn(timeTeam);
        when(board.getObjective(DisplaySlot.SIDEBAR)).thenReturn(objective);
        when(objective.getScore(anyString())).thenReturn(scoreboardScore);
        TrialSpectatorManager.getInstance().setSpectatorScoreboard(spectator, board);

        SkillScoreboard.updateTrialSpectatorScoreboard(spectator, "Trial Player", 20, 20, 20, 150, 65);

        verify(healthTeam).addEntry(ChatColor.DARK_GRAY.toString());
        verify(foodTeam).addEntry(ChatColor.GRAY.toString());
        verify(scoreTeam).addEntry(ChatColor.DARK_BLUE.toString());
        verify(timeTeam).addEntry(ChatColor.BLUE.toString());
        verify(healthTeam).setPrefix(ChatColor.GOLD + "Health: " + ChatColor.AQUA + "20/20");
        verify(foodTeam).setPrefix(ChatColor.GOLD + "Food: " + ChatColor.AQUA + "20/20");
        verify(spectator).setScoreboard(board);
    }

    @Test
    void playerListInitializesEachPlayersOwnDeathScore() {
        Scoreboard board = mock(Scoreboard.class);
        Objective deathObjective = mock(Objective.class);
        Score firstPlayerScore = mock(Score.class);
        Score secondPlayerScore = mock(Score.class);
        Player firstPlayer = mock(Player.class);
        Player secondPlayer = mock(Player.class);
        UUID firstUuid = UUID.randomUUID();
        UUID secondUuid = UUID.randomUUID();

        when(board.getObjective(SkillCategory.DEATHS.getDisplayName())).thenReturn(deathObjective);
        when(deathObjective.getScore("FirstPlayer")).thenReturn(firstPlayerScore);
        when(deathObjective.getScore("SecondPlayer")).thenReturn(secondPlayerScore);
        when(firstPlayer.getUniqueId()).thenReturn(firstUuid);
        when(firstPlayer.getName()).thenReturn("FirstPlayer");
        when(secondPlayer.getUniqueId()).thenReturn(secondUuid);
        when(secondPlayer.getName()).thenReturn("SecondPlayer");

        Map<UUID, LeaderboardPlayer> leaderboardTracker = Map.of(
                firstUuid, leaderboardPlayer("FirstPlayer", 0),
                secondUuid, leaderboardPlayer("SecondPlayer", 7));

        SkillScoreboard.loadPlayerDeaths(board, List.of(firstPlayer, secondPlayer), leaderboardTracker);

        verify(firstPlayerScore).setScore(0);
        verify(secondPlayerScore).setScore(7);
    }

    private static LeaderboardPlayer leaderboardPlayer(String name, int deaths) {
        EnumMap<SkillCategory, Integer> scores = new EnumMap<>(SkillCategory.class);
        scores.put(SkillCategory.DEATHS, deaths);
        return new LeaderboardPlayer(name, scores);
    }
}
