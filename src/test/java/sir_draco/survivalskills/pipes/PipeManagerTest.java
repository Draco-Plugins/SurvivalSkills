package sir_draco.survivalskills.pipes;

import org.bukkit.Material;
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
    void recognizesCopperChestVariantsAsPipeChests() {
        Set<Material> copperChestMaterials = Set.of(
                Material.COPPER_CHEST, Material.EXPOSED_COPPER_CHEST,
                Material.WEATHERED_COPPER_CHEST, Material.OXIDIZED_COPPER_CHEST,
                Material.WAXED_COPPER_CHEST, Material.WAXED_EXPOSED_COPPER_CHEST,
                Material.WAXED_WEATHERED_COPPER_CHEST, Material.WAXED_OXIDIZED_COPPER_CHEST);

        assertTrue(copperChestMaterials.stream().allMatch((Material material) ->
                PipeManager.isPipeChest(material)));
    }

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

    @Test
    void advancesReceiverCursorByPerCycleProcessingCap() {
        assertEquals(50, PipeManager.MAX_RECEIVERS_PER_CYCLE);
        assertEquals(50, PipeManager.receiverProcessingCount(500));
        assertEquals(50, PipeManager.nextReceiverCursor(0, 500));
        assertEquals(100, PipeManager.nextReceiverCursor(50, 500));
    }

    @Test
    void capsReceiverProcessingAtBoundary() {
        assertEquals(0, PipeManager.receiverProcessingCount(-1));
        assertEquals(0, PipeManager.receiverProcessingCount(0));
        assertEquals(49, PipeManager.receiverProcessingCount(49));
        assertEquals(50, PipeManager.receiverProcessingCount(50));
        assertEquals(50, PipeManager.receiverProcessingCount(51));
    }

    @Test
    void wrapsReceiverCursorAfterProcessingCappedBatch() {
        assertEquals(25, PipeManager.nextReceiverCursor(75, 100));
        assertEquals(40, PipeManager.nextReceiverCursor(Integer.MAX_VALUE - 10, Integer.MAX_VALUE));
    }

    @Test
    void rotatesReceiverPriorityWhenBelowPerCycleCap() {
        assertEquals(1, PipeManager.nextReceiverCursor(0, 25));
        assertEquals(0, PipeManager.nextReceiverCursor(24, 25));
        assertEquals(0, PipeManager.nextReceiverCursor(0, 0));
    }

    @Test
    void reachesEveryReceiverAcrossCappedCycles() {
        for (int receiverCount = 1; receiverCount <= 1_000; receiverCount++) {
            boolean[] processed = new boolean[receiverCount];
            int cursor = 0;
            int cyclesForFullPass = (receiverCount + PipeManager.MAX_RECEIVERS_PER_CYCLE - 1)
                    / PipeManager.MAX_RECEIVERS_PER_CYCLE;
            for (int cycle = 0; cycle < cyclesForFullPass; cycle++) {
                int receiversToProcess = PipeManager.receiverProcessingCount(receiverCount);
                for (int offset = 0; offset < receiversToProcess; offset++) {
                    processed[(cursor + offset) % receiverCount] = true;
                }
                cursor = PipeManager.nextReceiverCursor(cursor, receiverCount);
            }
            for (int receiver = 0; receiver < receiverCount; receiver++) {
                assertTrue(processed[receiver],
                        "Receiver " + receiver + " was skipped in a system of size " + receiverCount);
            }
        }
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
