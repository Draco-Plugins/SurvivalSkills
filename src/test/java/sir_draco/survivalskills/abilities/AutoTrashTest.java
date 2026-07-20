package sir_draco.survivalskills.abilities;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutoTrashTest {

    @Test
    void upgradeReplacesInventoryAndMovesCurrentViewer() {
        Inventory smallInventory = mock(Inventory.class);
        Inventory largeInventory = mock(Inventory.class);
        Player player = mock(Player.class);
        when(smallInventory.getViewers()).thenReturn(List.of(player));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.createInventory(null, 27, "Auto Trash")).thenReturn(smallInventory);
            bukkit.when(() -> Bukkit.createInventory(null, 54, "Auto Trash")).thenReturn(largeInventory);
            AutoTrash trash = new AutoTrash(false, false);

            trash.upgradeTrashSize();

            assertTrue(trash.isBig());
            assertSame(largeInventory, trash.getTrashInventory());
            verify(player).openInventory(largeInventory);
            verify(smallInventory, never()).clear();
        }
    }

    @Test
    void upgradeIsIdempotent() {
        Inventory smallInventory = mock(Inventory.class);
        Inventory largeInventory = mock(Inventory.class);
        when(smallInventory.getViewers()).thenReturn(List.of());

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.createInventory(null, 27, "Auto Trash")).thenReturn(smallInventory);
            bukkit.when(() -> Bukkit.createInventory(null, 54, "Auto Trash")).thenReturn(largeInventory);
            AutoTrash trash = new AutoTrash(false, false);

            trash.upgradeTrashSize();
            trash.upgradeTrashSize();

            bukkit.verify(() -> Bukkit.createInventory(null, 54, "Auto Trash"), times(1));
        }
    }

    @Test
    void permaTrashUpgradePreservesInventoryTitle() {
        Inventory smallInventory = mock(Inventory.class);
        Inventory largeInventory = mock(Inventory.class);
        when(smallInventory.getViewers()).thenReturn(List.of());

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.createInventory(null, 27, "Perma Trash")).thenReturn(smallInventory);
            bukkit.when(() -> Bukkit.createInventory(null, 54, "Perma Trash")).thenReturn(largeInventory);
            AutoTrash trash = new AutoTrash(false, true);

            trash.upgradeTrashSize();

            assertSame(largeInventory, trash.getTrashInventory());
            bukkit.verify(() -> Bukkit.createInventory(null, 54, "Perma Trash"), times(1));
        }
    }

    @Test
    void openingAlreadyViewedTrashDoesNotReopenInventory() {
        Inventory inventory = mock(Inventory.class);
        InventoryView inventoryView = mock(InventoryView.class);
        Player player = mock(Player.class);
        when(player.getOpenInventory()).thenReturn(inventoryView);
        when(inventoryView.getTopInventory()).thenReturn(inventory);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.createInventory(null, 54, "Auto Trash")).thenReturn(inventory);
            AutoTrash trash = new AutoTrash(true, false);

            trash.openTrashInventory(player);

            assertTrue(trash.isBig());
            verify(player, never()).openInventory(inventory);
        }
    }
}
