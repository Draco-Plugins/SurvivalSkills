package sir_draco.survivalskills.skill_listeners.fishing;

import org.bukkit.inventory.ItemStack;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FishingAbilityManagerTest {

    @Test
    void unlimitedPowderSnowUsesVanillaPlacementAndKeepsBucket() {
        ItemStack bucket = mock(ItemStack.class);
        ItemStack preservedBucket = mock(ItemStack.class);
        when(bucket.clone()).thenReturn(preservedBucket);
        PlayerBucketEmptyEvent event = mock(PlayerBucketEmptyEvent.class);

        FishingAbilityManager.preservePowderSnowBucket(event, bucket);

        verify(event).setItemStack(preservedBucket);
        verify(event, never()).setCancelled(true);
    }
}
