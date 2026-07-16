package sir_draco.survivalskills.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Optional;

public final class ItemNameUtils {

    private ItemNameUtils() {
    }

    /**
     * Converts a Material enum name to a human-readable title-case string.
     * e.g. DIAMOND_SWORD -> "Diamond Sword"
     */
    public static String formatMaterialName(Material material) {
        if (material == null) {
            return "Unknown";
        }
        String name = material.name().toLowerCase().replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder formatted = new StringBuilder();

        for (String word : words) {
            if (!formatted.isEmpty()) {
                formatted.append(" ");
            }
            formatted.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }

        return formatted.toString();
    }

    /**
     * Extracts the armor tier name from a helmet Material.
     * e.g. DIAMOND_HELMET -> "Diamond", LEATHER_HELMET -> "Leather"
     */
    public static String formatArmorTier(Material helmet) {
        if (helmet == null) {
            return "Unknown";
        }
        String name = helmet.name().toLowerCase();
        if (name.contains("leather")) {
            return "Leather";
        }
        if (name.contains("chainmail")) {
            return "Chainmail";
        }
        if (name.contains("iron")) {
            return "Iron";
        }
        if (name.contains("diamond")) {
            return "Diamond";
        }
        if (name.contains("netherite")) {
            return "Netherite";
        }
        return "Unknown";
    }

    /**
     * Safely retrieves ItemMeta from an ItemStack, returning Optional.empty()
     * if the item or its meta is null.
     */
    public static Optional<ItemMeta> getItemMetaSafe(ItemStack item) {
        if (item == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(item.getItemMeta());
    }
}
