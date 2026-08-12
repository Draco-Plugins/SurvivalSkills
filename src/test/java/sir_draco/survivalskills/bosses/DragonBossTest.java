package sir_draco.survivalskills.bosses;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DragonBossTest {

    @Test
    void proximityBoundaryIncludesThreeBlocks() {
        Location origin = new Location(null, 0, 0, 0);

        assertTrue(DragonBoss.isWithinRadius(origin, new Location(null, 3, 0, 0),
                DragonBoss.ENDERMITE_TRIGGER_RADIUS));
        assertFalse(DragonBoss.isWithinRadius(origin, new Location(null, 3.01, 0, 0),
                DragonBoss.ENDERMITE_TRIGGER_RADIUS));
    }

    @Test
    void proximityRequiresTheSameWorld() {
        World firstWorld = mock(World.class);
        World secondWorld = mock(World.class);

        assertFalse(DragonBoss.isWithinRadius(new Location(firstWorld, 0, 0, 0),
                new Location(secondWorld, 0, 0, 0), DragonBoss.GROUND_SLAM_RADIUS));
    }

    @Test
    void groundSlamChanceUsesFivePercentExclusiveBoundary() {
        assertTrue(DragonBoss.isGroundSlamRoll(0.0));
        assertTrue(DragonBoss.isGroundSlamRoll(0.049999));
        assertFalse(DragonBoss.isGroundSlamRoll(0.05));
    }

    @Test
    void regeneratorChanceUsesFivePercentExclusiveBoundary() {
        assertTrue(DragonBoss.shouldSpawnRegenerator(0.0));
        assertTrue(DragonBoss.shouldSpawnRegenerator(0.049999));
        assertFalse(DragonBoss.shouldSpawnRegenerator(0.05));
    }

    @Test
    void regenerationHealsOnePercentOfMaximumHealth() {
        assertEquals(255.0, DragonBoss.calculateRegeneratedHealth(250.0, 500.0));
    }

    @Test
    void regenerationDoesNotExceedMaximumHealth() {
        assertEquals(500.0, DragonBoss.calculateRegeneratedHealth(499.0, 500.0));
    }

    @Test
    void requestedBalanceValuesRemainExact() {
        assertEquals(30.0, DragonBoss.GROUND_SLAM_DAMAGE);
        assertEquals(20, DragonBoss.STUN_DURATION_TICKS);
        assertEquals(100, DragonBoss.REGENERATOR_HEAL_INTERVAL_TICKS);
        assertEquals(75.0, DragonBoss.REGENERATOR_HEALTH);
        assertEquals(10.0, DragonBoss.REGENERATOR_DAMAGE);
        assertEquals(0.7, DragonBoss.REGENERATOR_SCALE);
    }

    @Test
    void participantMergeDoesNotAddDuplicates() {
        Player firstPlayer = mock(Player.class);
        Player secondPlayer = mock(Player.class);
        List<Player> currentPlayers = new ArrayList<>(List.of(firstPlayer));

        DragonBoss.addUniquePlayers(currentPlayers, List.of(firstPlayer, secondPlayer));
        DragonBoss.addUniquePlayers(currentPlayers, List.of(firstPlayer, secondPlayer));

        assertEquals(List.of(firstPlayer, secondPlayer), currentPlayers);
    }

    @Test
    void wrathFlightStateRoundTripsWhenGameModeIsUnchanged() {
        Player player = mock(Player.class);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(player.getAllowFlight()).thenReturn(true);
        when(player.isFlying()).thenReturn(true);
        when(player.getFlySpeed()).thenReturn(0.075F);
        when(player.isOnline()).thenReturn(true);
        DragonBoss.FlightState state = DragonBoss.captureFlightState(player);

        DragonBoss.suppressFlight(player);
        DragonBoss.restoreFlightState(player, state);

        verify(player).setAllowFlight(false);
        verify(player).setFlying(false);
        verify(player).setAllowFlight(true);
        verify(player).setFlying(true);
        verify(player).setFlySpeed(0.075F);
    }

    @Test
    void wrathDoesNotRestoreStateAfterGameModeChange() {
        Player player = mock(Player.class);
        when(player.isOnline()).thenReturn(true);
        when(player.getGameMode()).thenReturn(GameMode.CREATIVE);
        DragonBoss.FlightState state = new DragonBoss.FlightState(GameMode.SURVIVAL, true, true, 0.075F);

        DragonBoss.restoreFlightState(player, state);

        verify(player, never()).setAllowFlight(true);
        verify(player, never()).setFlying(true);
        verify(player, never()).setFlySpeed(0.075F);
    }
}
