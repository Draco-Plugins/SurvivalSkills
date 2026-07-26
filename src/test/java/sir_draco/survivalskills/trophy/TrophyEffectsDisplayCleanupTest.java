package sir_draco.survivalskills.trophy;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrophyEffectsDisplayCleanupTest {

    @Test
    void checksForDuplicatesWithinHalfBlockOfDisplayLocation() {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        when(plugin.getName()).thenReturn("SurvivalSkills");
        World world = mock(World.class);
        when(world.getNearbyEntities(any(Location.class), eq(0.5), eq(0.5), eq(0.5))).thenReturn(List.of());

        TrophyEffects effects = createEffects(plugin, world, TrophyType.CAVE);

        effects.checkForDuplicate(Material.DIAMOND_PICKAXE);

        verify(world).getNearbyEntities(any(Location.class), eq(0.5), eq(0.5), eq(0.5));
    }

    @Test
    void retainsNearbyItemWithoutDisplayEnchantment() {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        when(plugin.getName()).thenReturn("SurvivalSkills");
        World world = mock(World.class);
        Item nearbyItem = mock(Item.class);
        ItemStack nearbyItemStack = mock(ItemStack.class);
        when(nearbyItemStack.getType()).thenReturn(Material.DIAMOND_PICKAXE);
        when(nearbyItemStack.getEnchantments()).thenReturn(Map.of());
        when(nearbyItem.getItemStack()).thenReturn(nearbyItemStack);
        when(world.getNearbyEntities(any(Location.class), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(nearbyItem));

        TrophyEffects effects = createEffects(plugin, world, TrophyType.CAVE);

        effects.checkForDuplicate(Material.DIAMOND_PICKAXE);

        verify(nearbyItem, never()).remove();
    }

    private TrophyEffects createEffects(SurvivalSkills plugin, World world, TrophyType type) {
        Trophy trophy = mock(Trophy.class);
        return new TrophyEffects(plugin, new Location(world, 10, 64, 20), type, trophy,
                "Player", UUID.randomUUID());
    }

}
