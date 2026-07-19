package sir_draco.survivalskills.god_questline.trial;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrialRegistryLifecycleTest {

    private final TrialRegistry registry = TrialRegistry.getInstance();

    @BeforeEach
    void setUp() {
        registry.clearAll();
    }

    @AfterEach
    void tearDown() {
        registry.clearAll();
    }

    @Test
    void cleanupPlayerRemovesCompletedAndPendingState() {
        Player player = mockPlayer();
        PendingTrial pendingTrial = new PendingTrial(player);
        registry.putPendingTrial(player, pendingTrial);
        registry.setCompletedGamemodes(player, new ArrayList<>(List.of(1, 3)));

        registry.cleanupPlayer(player);

        assertFalse(registry.hasPendingTrial(player));
        assertFalse(registry.hasCompletedGamemodes(player));
        assertTrue(pendingTrial.getPlayers().isEmpty());
    }

    @Test
    void replacingPendingTrialDisposesPreviousParty() {
        Player player = mockPlayer();
        PendingTrial previousTrial = new PendingTrial(player);
        PendingTrial replacementTrial = new PendingTrial(player);
        registry.putPendingTrial(player, previousTrial);

        registry.putPendingTrial(player, replacementTrial);

        assertTrue(previousTrial.getPlayers().isEmpty());
        assertSame(replacementTrial, registry.getPendingTrial(player));
    }

    @Test
    void removingPendingTrialUnregistersItsInventory() throws ReflectiveOperationException {
        Player player = mockPlayer();
        PendingTrial pendingTrial = new PendingTrial(player);
        Inventory inventory = mock(Inventory.class);
        java.lang.reflect.Field inventoryField = PendingTrial.class.getDeclaredField("playerManager");
        inventoryField.setAccessible(true);
        inventoryField.set(pendingTrial, inventory);
        registry.registerTrialSelectionInventory(inventory);
        registry.putPendingTrial(player, pendingTrial);

        registry.removePendingTrial(player);

        assertFalse(registry.isTrialSelectionInventory(inventory));
        verify(inventory).clear();
    }

    @Test
    void confirmingPartyClosesSelectionInventory() {
        Player player = mockPlayer();
        PendingTrial pendingTrial = new PendingTrial(player);

        pendingTrial.confirmParty();

        verify(player).closeInventory();
    }

    private Player mockPlayer() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        return player;
    }
}
