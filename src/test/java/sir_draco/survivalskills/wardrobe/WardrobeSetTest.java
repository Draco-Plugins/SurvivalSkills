package sir_draco.survivalskills.wardrobe;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WardrobeSetTest {

    @Test
    void armorSlotsOnlyAcceptTheirArmorType() {
        assertTrue(WardrobeArmorSlot.HELMET.accepts(new ItemStack(Material.DIAMOND_HELMET)));
        assertTrue(WardrobeArmorSlot.CHESTPLATE.accepts(new ItemStack(Material.LEATHER_CHESTPLATE)));
        assertTrue(WardrobeArmorSlot.LEGGINGS.accepts(new ItemStack(Material.NETHERITE_LEGGINGS)));
        assertTrue(WardrobeArmorSlot.BOOTS.accepts(new ItemStack(Material.GOLDEN_BOOTS)));
        assertFalse(WardrobeArmorSlot.HELMET.accepts(new ItemStack(Material.DIAMOND_BOOTS)));
        assertTrue(WardrobeArmorSlot.fromItem(new ItemStack(Material.STONE)).isEmpty());
    }

    @Test
    void wardrobeSetDefensivelyCopiesStoredItems() {
        ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
        WardrobeSet wardrobeSet = new WardrobeSet(Map.of(WardrobeArmorSlot.HELMET, helmet));

        helmet.setAmount(2);
        ItemStack retrievedHelmet = wardrobeSet.getItem(WardrobeArmorSlot.HELMET).orElseThrow();
        retrievedHelmet.setAmount(3);

        assertEquals(1, wardrobeSet.getItem(WardrobeArmorSlot.HELMET).orElseThrow().getAmount());
    }

    @Test
    void replacingAndRemovingItemsCreatesNewSets() {
        WardrobeSet emptySet = WardrobeSet.empty();
        WardrobeSet storedSet = emptySet.withItem(WardrobeArmorSlot.BOOTS,
                Optional.of(new ItemStack(Material.IRON_BOOTS)));
        WardrobeSet clearedSet = storedSet.withItem(WardrobeArmorSlot.BOOTS, Optional.empty());

        assertTrue(emptySet.getItem(WardrobeArmorSlot.BOOTS).isEmpty());
        assertTrue(storedSet.getItem(WardrobeArmorSlot.BOOTS).isPresent());
        assertTrue(clearedSet.getItem(WardrobeArmorSlot.BOOTS).isEmpty());
    }

    @Test
    void equippedArmorCanBeCapturedAndRestored() {
        ItemStack helmet = new ItemStack(Material.IRON_HELMET);
        PlayerInventory sourceInventory = mock(PlayerInventory.class);
        when(sourceInventory.getHelmet()).thenReturn(helmet);
        PlayerInventory targetInventory = mock(PlayerInventory.class);

        WardrobeSet wardrobeSet = WardrobeSet.fromInventory(sourceInventory);
        wardrobeSet.equip(targetInventory);

        verify(targetInventory).setHelmet(argThat((ItemStack item) -> item.getType() == Material.IRON_HELMET));
        verify(targetInventory).setChestplate(argThat((ItemStack item) -> item.getType() == Material.AIR));
        verify(targetInventory).setLeggings(argThat((ItemStack item) -> item.getType() == Material.AIR));
        verify(targetInventory).setBoots(argThat((ItemStack item) -> item.getType() == Material.AIR));
    }

    @Test
    void layoutAndYamlPathsRemainStable() {
        assertEquals(List.of(28, 42, 58), WardrobeManager.UNLOCK_LEVELS);
        assertEquals(10, WardrobeGui.getStorageSlot(0, WardrobeArmorSlot.HELMET));
        assertEquals(40, WardrobeGui.getStorageSlot(1, WardrobeArmorSlot.BOOTS));
        assertEquals(43, WardrobeGui.getStorageSlot(2, WardrobeArmorSlot.BOOTS));
        assertEquals(46, WardrobeGui.getSwapButtonSlot(0));
        assertEquals(49, WardrobeGui.getSwapButtonSlot(1));
        assertEquals(52, WardrobeGui.getSwapButtonSlot(2));

        UUID playerId = UUID.fromString("12345678-1234-1234-1234-123456789012");
        assertEquals(playerId + ".Sets.2.Chestplate",
                WardrobeManager.itemPath(playerId, 1, WardrobeArmorSlot.CHESTPLATE));
    }
}
