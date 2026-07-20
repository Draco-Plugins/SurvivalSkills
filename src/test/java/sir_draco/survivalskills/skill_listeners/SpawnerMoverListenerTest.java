package sir_draco.survivalskills.skill_listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.logging.Logger;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpawnerMoverListenerTest {

    @Test
    void carryingMoverInteractionCancelsNativePlacement() {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        SpawnerMoverListener listener = spy(new SpawnerMoverListener(plugin));
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack mover = mock(ItemStack.class);
        BlockStateMeta meta = mock(BlockStateMeta.class);
        CreatureSpawner storedSpawner = mock(CreatureSpawner.class);
        Block clickedBlock = mock(Block.class);

        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getHand()).thenReturn(EquipmentSlot.HAND);
        when(event.getClickedBlock()).thenReturn(clickedBlock);
        when(event.getPlayer()).thenReturn(player);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(mover);
        when(mover.getType()).thenReturn(Material.SPAWNER);
        when(mover.getAmount()).thenReturn(1);
        when(mover.getItemMeta()).thenReturn(meta);
        when(meta.hasBlockState()).thenReturn(true);
        when(meta.getDisplayName()).thenReturn(ChatColor.LIGHT_PURPLE + "Spawner Mover");
        when(meta.getBlockState()).thenReturn(storedSpawner);
        doNothing().when(listener).placeSpawner(event, EquipmentSlot.HAND, storedSpawner);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getLogger).thenReturn(mock(Logger.class));
            listener.onSpawnerMoverUse(event);
        }

        verify(event).setCancelled(true);
    }

    @Test
    void nativeSpawnerMoverBlockPlacementIsCancelled() {
        SpawnerMoverListener listener = new SpawnerMoverListener(mock(SurvivalSkills.class));
        BlockPlaceEvent event = mock(BlockPlaceEvent.class);
        ItemStack mover = mock(ItemStack.class);
        BlockStateMeta meta = mock(BlockStateMeta.class);
        when(event.getItemInHand()).thenReturn(mover);
        when(mover.getType()).thenReturn(Material.SPAWNER);
        when(mover.getItemMeta()).thenReturn(meta);
        when(meta.hasBlockState()).thenReturn(true);
        when(meta.getDisplayName()).thenReturn(ChatColor.LIGHT_PURPLE + "Spawner Mover");
        when(meta.getBlockState()).thenReturn(mock(CreatureSpawner.class));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getLogger).thenReturn(mock(Logger.class));
            listener.onSpawnerMoverPlace(event);
        }

        verify(event).setCancelled(true);
    }
}
