package sir_draco.survivalskills.abilities;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.skill_listeners.MiningSkill;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VeinMinerAsyncTest {

    @Test
    void cleanupAfterFailureClearsAllTransientState() {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        when(plugin.getName()).thenReturn("SurvivalSkills");
        MiningSkill miningSkill = new MiningSkill(plugin, 1);
        Player player = mock(Player.class);
        Block block = mock(Block.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        miningSkill.getVeinTracker().put(player, new ArrayList<>());

        VeinMinerAsync veinMiner = new VeinMinerAsync(
                plugin, player, miningSkill, block, Material.DIAMOND_ORE, 1);

        assertTrue(miningSkill.isVeinMinerActive(player));
        veinMiner.cleanupAfterFailure();
        veinMiner.cleanupAfterFailure();

        assertFalse(miningSkill.isVeinMinerActive(player));
        assertFalse(miningSkill.getVeinTracker().containsKey(player));
        verify(player).removeMetadata(VeinMinerAsync.VEIN_MINER_BREAK_METADATA, plugin);
    }

    @Test
    void veinMiningUsesNaturalDropsWhenEventDropsAreEnabled() {
        Block block = mock(Block.class);
        ItemStack pickaxe = mock(ItemStack.class);

        VeinMinerAsync.breakBlock(block, pickaxe, true);

        verify(block).breakNaturally(pickaxe);
        verify(block, never()).setType(Material.AIR);
    }

    @Test
    void veinMiningDoesNotDropItemsAgainWhenEventDropsAreDisabled() {
        Block block = mock(Block.class);
        ItemStack pickaxe = mock(ItemStack.class);

        VeinMinerAsync.breakBlock(block, pickaxe, false);

        verify(block).setType(Material.AIR);
        verify(block, never()).breakNaturally(pickaxe);
    }
}
