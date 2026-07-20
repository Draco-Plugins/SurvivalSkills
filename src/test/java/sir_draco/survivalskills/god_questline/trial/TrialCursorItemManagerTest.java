package sir_draco.survivalskills.god_questline.trial;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrialCursorItemManagerTest {

    @Test
    void emptyCursorDoesNotModifyInventory() {
        Player player = mock(Player.class);
        ItemStack cursorItem = mock(ItemStack.class);
        when(player.getItemOnCursor()).thenReturn(cursorItem);
        when(cursorItem.getType()).thenReturn(Material.AIR);

        TrialCursorItemManager.returnToInventory(player);

        verify(player, never()).getInventory();
        verify(player, never()).setItemOnCursor(argThat((ItemStack item) -> item != null));
    }

    @Test
    void cursorItemReturnsToInventoryBeforeRewardInventoryOpens() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack cursorItem = mock(ItemStack.class);
        ItemStack cursorCopy = mock(ItemStack.class);
        when(player.getItemOnCursor()).thenReturn(cursorItem);
        when(player.getInventory()).thenReturn(inventory);
        when(cursorItem.getType()).thenReturn(Material.ENCHANTED_BOOK);
        when(cursorItem.clone()).thenReturn(cursorCopy);
        when(inventory.addItem(cursorCopy)).thenReturn(new HashMap<>());

        TrialCursorItemManager.returnToInventory(player);

        verify(inventory).addItem(cursorCopy);
        verify(player).setItemOnCursor(argThat((ItemStack item) -> item.getType().equals(Material.AIR)));
    }

    @Test
    void cursorOverflowDropsAtPlayerLocation() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        World world = mock(World.class);
        Location location = mock(Location.class);
        ItemStack cursorItem = mock(ItemStack.class);
        ItemStack cursorCopy = mock(ItemStack.class);
        ItemStack leftover = mock(ItemStack.class);
        when(player.getItemOnCursor()).thenReturn(cursorItem);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(location);
        when(cursorItem.getType()).thenReturn(Material.ENCHANTED_BOOK);
        when(cursorItem.clone()).thenReturn(cursorCopy);
        HashMap<Integer, ItemStack> leftovers = new HashMap<>();
        leftovers.put(0, leftover);
        when(inventory.addItem(cursorCopy)).thenReturn(leftovers);

        TrialCursorItemManager.returnToInventory(player);

        verify(world).dropItemNaturally(location, leftover);
        verify(player).setItemOnCursor(argThat((ItemStack item) -> item.getType().equals(Material.AIR)));
    }
}
