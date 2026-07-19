package sir_draco.survivalskills.god_questline.trial;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class TrialSpectatorManagerLifecycleTest {

    private final TrialSpectatorManager manager = TrialSpectatorManager.getInstance();

    @BeforeEach
    void setUp() {
        manager.clearAll();
    }

    @AfterEach
    void tearDown() {
        manager.clearAll();
    }

    @Test
    void removingTargetReleasesSpectatorRelationshipAndScoreboards() {
        Player spectator = mock(Player.class);
        Player target = mock(Player.class);
        manager.addSpectator(spectator, target, mock(Location.class));
        manager.setSpectatorScoreboard(spectator, mock(Scoreboard.class));
        manager.setTrialScoreboard(target, mock(Scoreboard.class));

        manager.removePlayer(target);

        assertFalse(manager.isSpectating(spectator));
        assertNull(manager.getSpectatorTarget(spectator));
        assertNull(manager.getSpectatorScoreboard(spectator));
        assertNull(manager.getTrialScoreboard(target));
    }
}
