package sir_draco.survivalskills.skill_listeners.fighting;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DragonManagerTest {

    @Test
    void dragonHealthUsesPlayerCountWithOnePlayerMinimum() {
        assertEquals(250, DragonManager.calculateDragonHealth(0));
        assertEquals(250, DragonManager.calculateDragonHealth(1));
        assertEquals(750, DragonManager.calculateDragonHealth(3));
    }

    @Test
    void respawnParticipantsRequireExactEndWorldAndRadius() {
        World dragonWorld = world(World.Environment.THE_END);
        World otherEndWorld = world(World.Environment.THE_END);
        World normalWorld = world(World.Environment.NORMAL);
        Location dragonLocation = new Location(dragonWorld, 0, 70, 0);
        Player insideRadius = player(dragonWorld, 199.0);
        Player onRadiusBoundary = player(dragonWorld, 200.0);
        Player outsideRadius = player(dragonWorld, 200.01);
        Player inOtherEndWorld = player(otherEndWorld, 0.0);
        Player inNormalWorld = player(normalWorld, 0.0);

        List<Player> participants = DragonManager.findRespawnParticipants(dragonLocation,
                List.of(insideRadius, onRadiusBoundary, outsideRadius, inOtherEndWorld, inNormalWorld));

        assertEquals(List.of(insideRadius, onRadiusBoundary), participants);
    }

    @Test
    void nonEndDragonLocationHasNoRespawnParticipants() {
        World normalWorld = world(World.Environment.NORMAL);
        Location dragonLocation = new Location(normalWorld, 0, 70, 0);
        Player nearbyPlayer = player(normalWorld, 0.0);

        assertTrue(DragonManager.findRespawnParticipants(dragonLocation, List.of(nearbyPlayer)).isEmpty());
    }

    @Test
    void initialDragonAllowsAnyPlayerDamage() {
        Player participant = mock(Player.class);
        Player otherPlayer = mock(Player.class);

        assertTrue(DragonManager.canPlayerDamageDragon(false, List.of(participant), otherPlayer));
    }

    @Test
    void respawnDragonAllowsOnlyParticipantDamage() {
        Player participant = mock(Player.class);
        Player otherPlayer = mock(Player.class);

        assertTrue(DragonManager.canPlayerDamageDragon(true, List.of(participant), participant));
        assertFalse(DragonManager.canPlayerDamageDragon(true, List.of(participant), otherPlayer));
    }

    private static World world(World.Environment environment) {
        World world = mock(World.class);
        when(world.getEnvironment()).thenReturn(environment);
        return world;
    }

    private static Player player(World world, double x) {
        Player player = mock(Player.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(new Location(world, x, 70, 0));
        return player;
    }
}
