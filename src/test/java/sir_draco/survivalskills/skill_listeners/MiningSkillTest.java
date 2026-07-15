package sir_draco.survivalskills.skill_listeners;

import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.abilities.items.PowerDrillTask;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MiningSkillTest {

    @Test
    void unlimitedTorchDoesNotDropWhenBroken() {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        when(plugin.getName()).thenReturn("SurvivalSkills");
        MiningSkill miningSkill = new MiningSkill(plugin, 0);
        BlockBreakEvent event = mock(BlockBreakEvent.class);
        Block block = mock(Block.class);
        Chunk chunk = mock(Chunk.class);
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        when(event.getBlock()).thenReturn(block);
        when(block.getChunk()).thenReturn(chunk);
        when(chunk.getPersistentDataContainer()).thenReturn(data);
        when(block.getX()).thenReturn(1);
        when(block.getY()).thenReturn(64);
        when(block.getZ()).thenReturn(2);
        when(data.get(any(NamespacedKey.class), eq(PersistentDataType.LONG_ARRAY)))
                .thenReturn(new long[] { ((long) 1 << 36) | ((long) 2 << 32) | 64 });

        miningSkill.preventUnlimitedTorchDrops(event);

        verify(event).setDropItems(false);
        verify(data).remove(any(NamespacedKey.class));
    }

    @Test
    void ordinaryTorchKeepsItsDrops() {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        when(plugin.getName()).thenReturn("SurvivalSkills");
        MiningSkill miningSkill = new MiningSkill(plugin, 0);
        BlockBreakEvent event = mock(BlockBreakEvent.class);
        Block block = mock(Block.class);
        Chunk chunk = mock(Chunk.class);
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        when(event.getBlock()).thenReturn(block);
        when(block.getChunk()).thenReturn(chunk);
        when(chunk.getPersistentDataContainer()).thenReturn(data);

        miningSkill.preventUnlimitedTorchDrops(event);

        verify(event, never()).setDropItems(false);
        verify(data, never()).remove(any(NamespacedKey.class));
    }

    @Test
    void unlimitedTorchPositionIsStoredInChunkData() {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        when(plugin.getName()).thenReturn("SurvivalSkills");
        MiningSkill miningSkill = new MiningSkill(plugin, 0);
        Block block = mock(Block.class);
        Chunk chunk = mock(Chunk.class);
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        when(block.getChunk()).thenReturn(chunk);
        when(chunk.getPersistentDataContainer()).thenReturn(data);
        when(block.getX()).thenReturn(1);
        when(block.getY()).thenReturn(64);
        when(block.getZ()).thenReturn(2);
        when(data.getOrDefault(any(NamespacedKey.class), eq(PersistentDataType.LONG_ARRAY), any(long[].class)))
                .thenReturn(new long[0]);

        miningSkill.markUnlimitedTorch(block);

        verify(data).set(any(NamespacedKey.class), eq(PersistentDataType.LONG_ARRAY),
                eq(new long[] { ((long) 1 << 36) | ((long) 2 << 32) | 64 }));
    }

    @Test
    void drillGeneratedBreakDoesNotStartVeinMiner() {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        when(plugin.getName()).thenReturn("SurvivalSkills");
        MiningSkill miningSkill = new MiningSkill(plugin, 1);
        Player player = mock(Player.class);
        BlockBreakEvent event = mock(BlockBreakEvent.class);
        when(player.hasMetadata(PowerDrillTask.DRILL_BREAK_METADATA)).thenReturn(true);

        miningSkill.veinminerChecker(player, event);

        verifyNoInteractions(event);
    }

    @Test
    void powerDrillIsRejectedByVeinMinerAdmissionPolicy() {
        assertTrue(MiningSkill.shouldSkipVeinMiner(true, false));
    }
}
