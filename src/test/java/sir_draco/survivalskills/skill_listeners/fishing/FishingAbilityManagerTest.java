package sir_draco.survivalskills.skill_listeners.fishing;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FishingAbilityManagerTest {

    @Test
    void restoresUnlimitedPowderSnowBucketToOriginalMainHandSlot() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack preservedBucket = mock(ItemStack.class);
        when(player.getInventory()).thenReturn(inventory);

        FishingAbilityManager.restorePowderSnowBucket(player, EquipmentSlot.HAND, 4, preservedBucket);

        verify(inventory).setItem(4, preservedBucket);
        verify(inventory, never()).setItemInOffHand(preservedBucket);
        verify(player).updateInventory();
    }

    @Test
    void restoresUnlimitedPowderSnowBucketToOffHand() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack preservedBucket = mock(ItemStack.class);
        when(player.getInventory()).thenReturn(inventory);

        FishingAbilityManager.restorePowderSnowBucket(player, EquipmentSlot.OFF_HAND, 2, preservedBucket);

        verify(inventory).setItemInOffHand(preservedBucket);
        verify(inventory, never()).setItem(2, preservedBucket);
        verify(player).updateInventory();
    }
}
