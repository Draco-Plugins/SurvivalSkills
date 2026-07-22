package sir_draco.survivalskills.wardrobe;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Objects;
import java.util.Optional;

public enum WardrobeArmorSlot {
    HELMET("Helmet", Material.IRON_HELMET, "_HELMET"),
    CHESTPLATE("Chestplate", Material.IRON_CHESTPLATE, "_CHESTPLATE"),
    LEGGINGS("Leggings", Material.IRON_LEGGINGS, "_LEGGINGS"),
    BOOTS("Boots", Material.IRON_BOOTS, "_BOOTS");

    private final String displayName;
    private final Material iconMaterial;
    private final String materialSuffix;

    WardrobeArmorSlot(String displayName, Material iconMaterial, String materialSuffix) {
        this.displayName = displayName;
        this.iconMaterial = iconMaterial;
        this.materialSuffix = materialSuffix;
    }

    public boolean accepts(ItemStack item) {
        Objects.requireNonNull(item, "Armor item cannot be null");
        return item.getType().name().endsWith(materialSuffix);
    }

    public Optional<ItemStack> getEquippedItem(PlayerInventory inventory) {
        Objects.requireNonNull(inventory, "Player inventory cannot be null");
        ItemStack item = switch (this) {
            case HELMET -> inventory.getHelmet();
            case CHESTPLATE -> inventory.getChestplate();
            case LEGGINGS -> inventory.getLeggings();
            case BOOTS -> inventory.getBoots();
        };
        return Optional.ofNullable(item).map((ItemStack armorItem) -> armorItem.clone());
    }

    public void setEquippedItem(PlayerInventory inventory, Optional<ItemStack> item) {
        Objects.requireNonNull(inventory, "Player inventory cannot be null");
        Objects.requireNonNull(item, "Armor item cannot be null");
        ItemStack equippedItem = item.map((ItemStack armorItem) -> armorItem.clone())
                .orElseGet(() -> new ItemStack(Material.AIR));
        switch (this) {
            case HELMET -> inventory.setHelmet(equippedItem);
            case CHESTPLATE -> inventory.setChestplate(equippedItem);
            case LEGGINGS -> inventory.setLeggings(equippedItem);
            case BOOTS -> inventory.setBoots(equippedItem);
        }
    }

    public static Optional<WardrobeArmorSlot> fromItem(ItemStack item) {
        if (isEmpty(item))
            return Optional.empty();
        for (WardrobeArmorSlot armorSlot : values()) {
            if (armorSlot.accepts(item))
                return Optional.of(armorSlot);
        }
        return Optional.empty();
    }

    public static boolean isEmpty(ItemStack item) {
        if (item == null)
            return true;
        return switch (item.getType()) {
            case AIR, CAVE_AIR, VOID_AIR -> true;
            default -> false;
        };
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getIconMaterial() {
        return iconMaterial;
    }
}
