package sir_draco.survivalskills.abilities.items;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerDrillTaskTest {

    @Test
    void blockPositionsAreUniqueImmutableCoordinates() {
        List<PowerDrillTask.BlockPosition> positions = PowerDrillTask.getBlockPositions(
                0, 64, 0, 1, 0, 0);

        assertEquals(189, positions.size());
        assertEquals(positions.size(), new HashSet<>(positions).size());
        assertTrue(positions.contains(new PowerDrillTask.BlockPosition(0, 64, 0)));
        assertTrue(positions.contains(new PowerDrillTask.BlockPosition(20, 65, 1)));
        assertThrows(UnsupportedOperationException.class,
                () -> positions.add(new PowerDrillTask.BlockPosition(21, 64, 0)));
    }

    @Test
    void negativeDirectionsUseMinecraftFloorCoordinates() {
        List<PowerDrillTask.BlockPosition> positions = PowerDrillTask.getBlockPositions(
                0, 64, 0, -1, 0, 0);

        assertEquals(189, positions.size());
        assertTrue(positions.contains(new PowerDrillTask.BlockPosition(-20, 63, -1)));
        assertTrue(positions.contains(new PowerDrillTask.BlockPosition(0, 65, 1)));
    }
}
