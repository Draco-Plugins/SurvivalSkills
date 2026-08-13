package sir_draco.survivalskills.god_questline.powerore;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PowerOreChallengeTest {

    @Test
    void memoryMatchTaskTypeCreatesMemoryMatchTask() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        PowerOreChallenge challenge = new PowerOreChallenge(
                mock(Location.class), player, PowerOreChallenge.TaskType.MEMORY_MATCH);

        assertInstanceOf(PowerOreMemoryMatchTask.class, challenge.getTask());
    }

    @Test
    void chickenHerdingTaskTypeCreatesChickenHerdingTask() {
        Player player = mock(Player.class);
        Location location = mock(Location.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(location.clone()).thenReturn(location);

        PowerOreChallenge challenge = new PowerOreChallenge(
                location, player, PowerOreChallenge.TaskType.CHICKEN_HERDING);

        assertInstanceOf(PowerOreChickenHerdingTask.class, challenge.getTask());
    }

    @Test
    void miniBossTestFactoryCreatesMiniBossTask() {
        Player player = mock(Player.class);
        Location location = mock(Location.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(location.clone()).thenReturn(location);

        PowerOreChallenge challenge = PowerOreChallenge.forMiniBossTest(location, player);

        assertInstanceOf(PowerOreMiniBossTask.class, challenge.getTask());
    }
}
