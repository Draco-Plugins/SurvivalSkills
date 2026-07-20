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
}
