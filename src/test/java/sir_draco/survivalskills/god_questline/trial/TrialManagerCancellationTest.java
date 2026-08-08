package sir_draco.survivalskills.god_questline.trial;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrialManagerCancellationTest {

    private final TrialRegistry registry = TrialRegistry.getInstance();
    private final ProtectedAreaManager protectedAreaManager = ProtectedAreaManager.getInstance();

    @BeforeEach
    void setUp() {
        registry.clearAll();
        protectedAreaManager.clearAll();
    }

    @AfterEach
    void tearDown() {
        registry.clearAll();
        protectedAreaManager.clearAll();
    }

    @Test
    void cancelPendingTrialRemovesFreshBuildingState() {
        Player player = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        PendingTrial pendingTrial = new PendingTrial(player);
        ProtectedArea protectedArea = mock(ProtectedArea.class);
        TrialManager.putPendingTrial(player, pendingTrial);
        TrialManager.putProtectedArea(playerId, protectedArea);
        TrialManager.setBuildingCreationCooldown(playerId, 123L);

        TrialManager.cancelPendingTrial(player, true);

        assertFalse(TrialManager.hasPendingTrial(player));
        assertFalse(TrialManager.hasProtectedArea(playerId));
        assertNull(TrialManager.getBuildingCreationCooldown(playerId));
        verify(player).closeInventory();
    }

    @Test
    void cancelPendingTrialPreservesExistingBuildingState() {
        Player player = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        PendingTrial pendingTrial = new PendingTrial(player);
        ProtectedArea protectedArea = mock(ProtectedArea.class);
        TrialManager.putPendingTrial(player, pendingTrial);
        TrialManager.putProtectedArea(playerId, protectedArea);
        TrialManager.setBuildingCreationCooldown(playerId, 123L);

        TrialManager.cancelPendingTrial(player, false);

        assertFalse(TrialManager.hasPendingTrial(player));
        assertSame(protectedArea, TrialManager.getProtectedArea(playerId));
        assertEquals(123L, TrialManager.getBuildingCreationCooldown(playerId));
        verify(player).closeInventory();
    }
}
