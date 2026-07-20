package sir_draco.survivalskills.pipes;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.inventory.InventoryHolder;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PipeManagerTest {
    @Test
    void discoversBothDoubleChestHalfLocationsFromSideHolders() {
        UUID worldUuid = UUID.randomUUID();
        World world = mock(World.class);
        when(world.getUID()).thenReturn(worldUuid);
        Chest leftChest = chestAt(world, 10, 64, 20);
        Chest rightChest = chestAt(world, 11, 64, 20);
        DoubleChest doubleChest = mock(DoubleChest.class);
        when(doubleChest.getLeftSide()).thenReturn(leftChest);
        when(doubleChest.getRightSide()).thenReturn(rightChest);

        Optional<Set<PipeLocation>> locations = PipeManager.doubleChestLocations(doubleChest);

        assertEquals(Optional.of(Set.of(
                new PipeLocation(worldUuid, 10, 64, 20),
                new PipeLocation(worldUuid, 11, 64, 20))), locations);
    }

    @Test
    void rejectsDoubleChestWhenBothChestHalvesCannotBeIdentified() {
        World world = mock(World.class);
        when(world.getUID()).thenReturn(UUID.randomUUID());
        DoubleChest doubleChest = mock(DoubleChest.class);
        Chest leftChest = chestAt(world, 10, 64, 20);
        InventoryHolder unidentifiedSide = mock(InventoryHolder.class);
        when(doubleChest.getLeftSide()).thenReturn(leftChest);
        when(doubleChest.getRightSide()).thenReturn(unidentifiedSide);

        assertTrue(PipeManager.doubleChestLocations(doubleChest).isEmpty());
    }

    private static Chest chestAt(World world, int x, int y, int z) {
        Block block = mock(Block.class);
        when(block.getWorld()).thenReturn(world);
        when(block.getX()).thenReturn(x);
        when(block.getY()).thenReturn(y);
        when(block.getZ()).thenReturn(z);
        Chest chest = mock(Chest.class);
        when(chest.getBlock()).thenReturn(block);
        return chest;
    }
}
