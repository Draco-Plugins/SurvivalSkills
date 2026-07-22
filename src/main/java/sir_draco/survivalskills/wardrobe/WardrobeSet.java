package sir_draco.survivalskills.wardrobe;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record WardrobeSet(Map<WardrobeArmorSlot, ItemStack> items) {

    public WardrobeSet {
        Objects.requireNonNull(items, "Wardrobe items cannot be null");
        items = copyItems(items);
    }

    public static WardrobeSet empty() {
        return new WardrobeSet(Map.of());
    }

    public static WardrobeSet fromInventory(PlayerInventory inventory) {
        Objects.requireNonNull(inventory, "Player inventory cannot be null");
        EnumMap<WardrobeArmorSlot, ItemStack> equippedItems = new EnumMap<>(WardrobeArmorSlot.class);
        for (WardrobeArmorSlot armorSlot : WardrobeArmorSlot.values()) {
            armorSlot.getEquippedItem(inventory).ifPresent((ItemStack item) -> equippedItems.put(armorSlot, item));
        }
        return new WardrobeSet(equippedItems);
    }

    @Override
    public Map<WardrobeArmorSlot, ItemStack> items() {
        return copyItems(items);
    }

    public Optional<ItemStack> getItem(WardrobeArmorSlot armorSlot) {
        Objects.requireNonNull(armorSlot, "Armor slot cannot be null");
        return Optional.ofNullable(items.get(armorSlot)).map((ItemStack item) -> item.clone());
    }

    public WardrobeSet withItem(WardrobeArmorSlot armorSlot, Optional<ItemStack> item) {
        Objects.requireNonNull(armorSlot, "Armor slot cannot be null");
        Objects.requireNonNull(item, "Armor item cannot be null");
        EnumMap<WardrobeArmorSlot, ItemStack> updatedItems = new EnumMap<>(WardrobeArmorSlot.class);
        items.forEach((WardrobeArmorSlot slot, ItemStack armorItem) -> updatedItems.put(slot, armorItem.clone()));
        item.ifPresentOrElse(
                (ItemStack armorItem) -> updatedItems.put(armorSlot, armorItem.clone()),
                () -> updatedItems.remove(armorSlot));
        return new WardrobeSet(updatedItems);
    }

    public void equip(PlayerInventory inventory) {
        Objects.requireNonNull(inventory, "Player inventory cannot be null");
        for (WardrobeArmorSlot armorSlot : WardrobeArmorSlot.values()) {
            armorSlot.setEquippedItem(inventory, getItem(armorSlot));
        }
    }

    private static Map<WardrobeArmorSlot, ItemStack> copyItems(Map<WardrobeArmorSlot, ItemStack> source) {
        EnumMap<WardrobeArmorSlot, ItemStack> copiedItems = new EnumMap<>(WardrobeArmorSlot.class);
        source.forEach((WardrobeArmorSlot armorSlot, ItemStack item) ->
                copiedItems.put(Objects.requireNonNull(armorSlot), Objects.requireNonNull(item).clone()));
        return Map.copyOf(copiedItems);
    }
}
