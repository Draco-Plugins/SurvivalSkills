package sir_draco.survivalskills.skill_listeners.fishing;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.Test;
import sir_draco.survivalskills.abilities.AutoTrash;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TrashManagerTest {

    @Test
    void keepsViewingStateWhenOpeningTrashClosesPreviousInventory() {
        TrashManager trashManager = new TrashManager();
        Player player = mock(Player.class);
        Inventory trashInventory = mock(Inventory.class);
        Inventory previousInventory = mock(Inventory.class);
        AutoTrash trash = mock(AutoTrash.class);
        InventoryCloseEvent event = mock(InventoryCloseEvent.class);

        when(trash.getTrashInventory()).thenReturn(trashInventory);
        when(event.getPlayer()).thenReturn(player);
        when(event.getInventory()).thenReturn(previousInventory);
        trashManager.addTrashInventory(player, trash);
        trashManager.getOpenTrashInventories().add(player);

        trashManager.onTrashClose(event);

        assertTrue(trashManager.getOpenTrashInventories().contains(player));
    }

    @Test
    void clearsViewingStateWhenTrashInventoryCloses() {
        TrashManager trashManager = new TrashManager();
        Player player = mock(Player.class);
        Inventory trashInventory = mock(Inventory.class);
        AutoTrash trash = mock(AutoTrash.class);
        InventoryCloseEvent event = mock(InventoryCloseEvent.class);

        when(trash.getTrashInventory()).thenReturn(trashInventory);
        when(event.getPlayer()).thenReturn(player);
        when(event.getInventory()).thenReturn(trashInventory);
        trashManager.addTrashInventory(player, trash);
        trashManager.getOpenTrashInventories().add(player);

        trashManager.onTrashClose(event);

        assertFalse(trashManager.getOpenTrashInventories().contains(player));
    }

    @Test
    void markingTrashOpenIsIdempotent() {
        TrashManager trashManager = new TrashManager();
        Player player = mock(Player.class);

        trashManager.markTrashInventoryOpen(player);
        trashManager.markTrashInventoryOpen(player);

        assertEquals(1, trashManager.getOpenTrashInventories().size());
    }

    @Test
    void closingTrashRemovesAllLegacyDuplicateEntries() {
        TrashManager trashManager = new TrashManager();
        Player player = mock(Player.class);
        Inventory trashInventory = mock(Inventory.class);
        AutoTrash trash = mock(AutoTrash.class);
        InventoryCloseEvent event = mock(InventoryCloseEvent.class);
        when(trash.getTrashInventory()).thenReturn(trashInventory);
        when(event.getPlayer()).thenReturn(player);
        when(event.getInventory()).thenReturn(trashInventory);
        trashManager.addTrashInventory(player, trash);
        trashManager.getOpenTrashInventories().add(player);
        trashManager.getOpenTrashInventories().add(player);

        trashManager.onTrashClose(event);

        assertFalse(trashManager.getOpenTrashInventories().contains(player));
    }

    @Test
    void removingPlayerClearsAllTrashSessionState() {
        TrashManager trashManager = new TrashManager();
        Player player = mock(Player.class);
        AutoTrash sessionTrash = mock(AutoTrash.class);
        AutoTrash permaTrash = mock(AutoTrash.class);
        trashManager.addTrashInventory(player, sessionTrash);
        trashManager.getPermaTrash().put(player, permaTrash);
        trashManager.getOpenTrashInventories().add(player);
        trashManager.getDisabledAutoTrash().add(player);

        trashManager.removePlayer(player);

        assertFalse(trashManager.getTrashInventories().containsKey(player));
        assertFalse(trashManager.getPermaTrash().containsKey(player));
        assertFalse(trashManager.getOpenTrashInventories().contains(player));
        assertFalse(trashManager.getDisabledAutoTrash().contains(player));
    }
}
