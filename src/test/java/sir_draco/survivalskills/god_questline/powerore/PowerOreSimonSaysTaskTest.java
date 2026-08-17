package sir_draco.survivalskills.god_questline.powerore;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PowerOreSimonSaysTaskTest {

    @Test
    void ignoresExtraClickAfterRoundSequenceIsComplete() throws ReflectiveOperationException {
        PowerOreChallenge challenge = mock(PowerOreChallenge.class);
        Player player = mock(Player.class);
        Inventory inventory = mock(Inventory.class);
        InventoryView inventoryView = mock(InventoryView.class);
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        PowerOreSimonSaysTask task = new PowerOreSimonSaysTask(challenge, player);

        when(challenge.getStatus()).thenReturn(PowerOreChallenge.Status.RUNNING);
        when(inventoryView.getTopInventory()).thenReturn(inventory);
        when(event.getView()).thenReturn(inventoryView);
        when(event.getRawSlot()).thenReturn(1);
        when(inventory.getSize()).thenReturn(9);

        setField(task, "inventory", inventory);
        setField(task, "acceptingInput", true);
        setField(task, "inputIndex", 4);
        getSequence(task).addAll(List.of(0, 1, 2, 3));

        assertDoesNotThrow(() -> task.handleClick(event));
        verify(event).setCancelled(true);
    }

    @SuppressWarnings("unchecked")
    private static List<Integer> getSequence(PowerOreSimonSaysTask task) throws ReflectiveOperationException {
        Field field = PowerOreSimonSaysTask.class.getDeclaredField("currentSequence");
        field.setAccessible(true);
        return (List<Integer>) field.get(task);
    }

    private static void setField(PowerOreSimonSaysTask task, String fieldName, Object value)
            throws ReflectiveOperationException {
        Field field = PowerOreSimonSaysTask.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(task, value);
    }
}
