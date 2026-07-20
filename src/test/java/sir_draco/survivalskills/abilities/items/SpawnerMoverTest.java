package sir_draco.survivalskills.abilities.items;

import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpawnerMoverTest {

    @Test
    void storesTheCompleteSpawnerBlockStateInTheItemMetadata() {
        BlockStateMeta meta = mock(BlockStateMeta.class);
        CreatureSpawner spawner = configuredGuardianSpawner();

        SpawnerMover.storeSpawner(meta, spawner);

        verify(meta).setBlockState(spawner);
        verify(meta).setLore(SpawnerMover.createLore(spawner));
    }

    @Test
    void loreShowsTheSpawnerTypeAndCustomizedSettings() {
        List<String> lore = SpawnerMover.createLore(configuredGuardianSpawner());

        assertTrue(lore.stream().anyMatch((String line) -> line.contains("Guardian")));
        assertTrue(lore.stream().anyMatch((String line) -> line.contains("100-240 ticks")));
        assertTrue(lore.stream().anyMatch((String line) -> line.contains("Spawn Radius") && line.contains("9")));
        assertTrue(lore.stream().anyMatch((String line) -> line.contains("Potential Spawns") && line.contains("2")));
    }

    @Test
    void placesACopyOfTheStoredStateAtTheNewLocation() {
        CreatureSpawner storedSpawner = mock(CreatureSpawner.class);
        BlockState copiedState = mock(BlockState.class);
        Location target = mock(Location.class);
        when(storedSpawner.copy(target)).thenReturn(copiedState);
        when(copiedState.update(true, false)).thenReturn(true);

        assertTrue(SpawnerMover.placeSpawner(storedSpawner, target));
        verify(storedSpawner).copy(target);
        verify(copiedState).update(true, false);
    }

    private static CreatureSpawner configuredGuardianSpawner() {
        CreatureSpawner spawner = mock(CreatureSpawner.class);
        when(spawner.getSpawnedType()).thenReturn(EntityType.GUARDIAN);
        when(spawner.getDelay()).thenReturn(40);
        when(spawner.getMinSpawnDelay()).thenReturn(100);
        when(spawner.getMaxSpawnDelay()).thenReturn(240);
        when(spawner.getSpawnCount()).thenReturn(7);
        when(spawner.getMaxNearbyEntities()).thenReturn(20);
        when(spawner.getRequiredPlayerRange()).thenReturn(12);
        when(spawner.getSpawnRange()).thenReturn(9);
        when(spawner.getPotentialSpawns()).thenReturn(List.of(mock(org.bukkit.block.spawner.SpawnerEntry.class),
                mock(org.bukkit.block.spawner.SpawnerEntry.class)));
        return spawner;
    }
}
