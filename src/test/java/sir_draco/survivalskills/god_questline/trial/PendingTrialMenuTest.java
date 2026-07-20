package sir_draco.survivalskills.god_questline.trial;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PendingTrialMenuTest {

    @Test
    void partyRefreshDoesNotReopenVisibleManager() throws ReflectiveOperationException {
        Player trialMaster = mock(Player.class);
        Inventory playerManager = mock(Inventory.class);
        PendingTrial pendingTrial = pendingTrialWithManager(trialMaster, playerManager);
        InventoryView openView = mock(InventoryView.class);
        when(trialMaster.getOpenInventory()).thenReturn(openView);
        when(openView.getTopInventory()).thenReturn(playerManager);

        pendingTrial.openPlayerManagerIfNeeded();

        verify(trialMaster, never()).openInventory(any(Inventory.class));
    }

    @Test
    void partyRefreshOpensManagerWhenLeaderIsViewingAnotherInventory() throws ReflectiveOperationException {
        Player trialMaster = mock(Player.class);
        Inventory playerManager = mock(Inventory.class);
        PendingTrial pendingTrial = pendingTrialWithManager(trialMaster, playerManager);
        InventoryView openView = mock(InventoryView.class);
        when(trialMaster.getOpenInventory()).thenReturn(openView);
        when(openView.getTopInventory()).thenReturn(mock(Inventory.class));

        pendingTrial.openPlayerManagerIfNeeded();

        verify(trialMaster).openInventory(playerManager);
    }

    private PendingTrial pendingTrialWithManager(Player trialMaster, Inventory playerManager)
            throws ReflectiveOperationException {
        PendingTrial pendingTrial = new PendingTrial(trialMaster);
        Field inventoryField = PendingTrial.class.getDeclaredField("playerManager");
        inventoryField.setAccessible(true);
        inventoryField.set(pendingTrial, playerManager);
        return pendingTrial;
    }
}
