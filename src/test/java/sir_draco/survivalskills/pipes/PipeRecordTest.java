package sir_draco.survivalskills.pipes;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PipeRecordTest {
    @Test
    void defensivelyCopiesCollections() {
        List<UUID> receivers = new ArrayList<>(List.of(UUID.randomUUID()));
        PipeFilter stoneFilter = new PipeFilter(Material.STONE, Set.of());
        Set<PipeFilter> whitelist = new HashSet<>(Set.of(stoneFilter));
        PipeRecord record = new PipeRecord(UUID.randomUUID(), UUID.randomUUID(),
                new PipeLocation(UUID.randomUUID(), 1, 2, 3), PipeType.SENDER,
                Optional.empty(), receivers, whitelist);

        receivers.clear();
        whitelist.clear();

        assertEquals(1, record.receiverUuids().size());
        assertEquals(Set.of(stoneFilter), record.whitelist());
        assertThrows(UnsupportedOperationException.class, () -> record.receiverUuids().clear());
        assertThrows(UnsupportedOperationException.class, () -> record.whitelist().clear());
    }

    @Test
    void receiverCanReferenceSender() {
        UUID sender = UUID.randomUUID();
        PipeRecord record = new PipeRecord(UUID.randomUUID(), UUID.randomUUID(),
                new PipeLocation(UUID.randomUUID(), 1, 2, 3), PipeType.RECEIVER,
                Optional.of(sender), List.of(), Set.of(new PipeFilter(Material.DIAMOND, Set.of())));

        assertEquals(Optional.of(sender), record.senderUuid());
    }

    @Test
    void locationEqualityUsesWorldAndBlockCoordinates() {
        UUID world = UUID.randomUUID();
        PipeLocation first = new PipeLocation(world, -1, 64, 32);
        PipeLocation same = new PipeLocation(world, -1, 64, 32);
        PipeLocation otherWorld = new PipeLocation(UUID.randomUUID(), -1, 64, 32);

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, otherWorld);
        assertEquals(-1, first.chunkX());
        assertEquals(2, first.chunkZ());
    }
}
