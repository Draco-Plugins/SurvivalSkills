package sir_draco.survivalskills.skill_listeners.builderswand;

import org.bukkit.Material;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BuilderWandListenerTest {

    @Test
    void selectsOnlyTheHandActuallyHoldingTheWand() {
        assertEquals(EquipmentSlot.HAND, BuilderWandListener.selectWandHand(true, false).orElseThrow());
        assertEquals(EquipmentSlot.OFF_HAND, BuilderWandListener.selectWandHand(false, true).orElseThrow());
        assertTrue(BuilderWandListener.selectWandHand(false, false).isEmpty());
    }

    @Test
    void mainHandTakesPriorityWhenBothHandsContainWands() {
        assertEquals(EquipmentSlot.HAND, BuilderWandListener.selectWandHand(true, true).orElseThrow());
    }

    @Test
    void inventoryEligibilityRejectsEveryStackWithItemMetadata() {
        ItemStack plainBlocks = mock(ItemStack.class);
        when(plainBlocks.getType()).thenReturn(Material.STONE);
        when(plainBlocks.hasItemMeta()).thenReturn(false);
        ItemStack customBlocks = mock(ItemStack.class);
        when(customBlocks.getType()).thenReturn(Material.STONE);
        when(customBlocks.hasItemMeta()).thenReturn(true);

        assertTrue(BuilderWandListener.isEligibleInventoryStack(plainBlocks, Material.STONE));
        assertFalse(BuilderWandListener.isEligibleInventoryStack(customBlocks, Material.STONE));
        assertFalse(BuilderWandListener.isEligibleInventoryStack(plainBlocks, Material.DIRT));
    }

    @Test
    void placementDataIsClonedAndClearsWaterloggingWithoutChangingOrientation() {
        Stairs referenceData = mock(Stairs.class);
        Stairs placementData = mock(Stairs.class);
        when(referenceData.clone()).thenReturn(placementData);

        assertSame(placementData, BuilderWandListener.preparePlacementData(referenceData));
        verify(placementData).setWaterlogged(false);
    }

    @Test
    void consumesEligibleStacksInSlotOrderAndSkipsCustomStacks() {
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack firstStack = stack(Material.STONE, 3, false);
        ItemStack customStack = stack(Material.STONE, 64, true);
        ItemStack secondStack = stack(Material.STONE, 4, false);
        ItemStack remainingStack = mock(ItemStack.class);
        when(secondStack.clone()).thenReturn(remainingStack);
        when(inventory.getItem(0)).thenReturn(firstStack);
        when(inventory.getItem(1)).thenReturn(customStack);
        when(inventory.getItem(2)).thenReturn(secondStack);

        BuilderWandListener.consumeBlocks(inventory, Material.STONE, 5);

        verify(inventory).setItem(0, null);
        verify(remainingStack).setAmount(2);
        verify(inventory).setItem(2, remainingStack);
    }

    private static ItemStack stack(Material material, int amount, boolean hasMetadata) {
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(material);
        when(item.getAmount()).thenReturn(amount);
        when(item.hasItemMeta()).thenReturn(hasMetadata);
        return item;
    }
}
