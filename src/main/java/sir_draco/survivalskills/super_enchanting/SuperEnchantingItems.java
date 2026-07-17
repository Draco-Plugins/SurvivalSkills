package sir_draco.survivalskills.super_enchanting;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.EnchantingTable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.items.ItemModelData;
import sir_draco.survivalskills.utils.items.ItemStackBuilder;

import java.util.List;

public final class SuperEnchantingItems {

    private static final String ITEM_TYPE_KEY = "super_enchanting_item";
    private static final String TABLE_BLOCK_KEY = "super_enchanting_table";
    private static final String TRIAL_FRAGMENT_TYPE = "trial_fragment";
    private static final String SUPER_TABLE_TYPE = "super_enchanting_table";

    private SuperEnchantingItems() {
    }

    public static ItemStack createTrialFragment(SurvivalSkills plugin, int amount) {
        ItemStack fragment = new ItemStackBuilder(Material.ECHO_SHARD, amount,
                ChatColor.LIGHT_PURPLE.toString() + ChatColor.BOLD + "Trial Fragment")
                .lore(List.of(ChatColor.GRAY + "Earned by completing trials",
                        ChatColor.DARK_PURPLE + "Used for super enchanting"))
                .modelData(ItemModelData.TRIAL_FRAGMENT.getId())
                .build();
        setItemType(fragment, plugin, TRIAL_FRAGMENT_TYPE);
        return fragment;
    }

    public static ItemStack createSuperEnchantingTable(SurvivalSkills plugin) {
        ItemStack table = new ItemStackBuilder(Material.ENCHANTING_TABLE, 1,
                ChatColor.LIGHT_PURPLE.toString() + ChatColor.BOLD + "Super Enchanting Table")
                .lore(List.of(ChatColor.GRAY + "Raises enchantments beyond their normal limits",
                        ChatColor.AQUA + "Requires trial fragments and experience levels"))
                .modelData(ItemModelData.SUPER_ENCHANTING_TABLE.getId())
                .build();
        setItemType(table, plugin, SUPER_TABLE_TYPE);
        return table;
    }

    public static boolean isTrialFragment(ItemStack item, SurvivalSkills plugin) {
        return hasItemType(item, plugin, TRIAL_FRAGMENT_TYPE);
    }

    public static boolean isSuperEnchantingTable(ItemStack item, SurvivalSkills plugin) {
        return hasItemType(item, plugin, SUPER_TABLE_TYPE);
    }

    public static boolean markSuperEnchantingTable(Block block, SurvivalSkills plugin) {
        if (!(block.getState() instanceof EnchantingTable table))
            return false;
        table.getPersistentDataContainer().set(tableBlockKey(plugin), PersistentDataType.BOOLEAN, true);
        return table.update(true, false);
    }

    public static boolean isSuperEnchantingTable(Block block, SurvivalSkills plugin) {
        if (!(block.getState() instanceof EnchantingTable table))
            return false;
        return table.getPersistentDataContainer().has(tableBlockKey(plugin), PersistentDataType.BOOLEAN);
    }

    private static void setItemType(ItemStack item, SurvivalSkills plugin, String type) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;
        meta.getPersistentDataContainer().set(itemTypeKey(plugin), PersistentDataType.STRING, type);
        item.setItemMeta(meta);
    }

    private static boolean hasItemType(ItemStack item, SurvivalSkills plugin, String type) {
        if (item == null || item.getType().isAir())
            return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return false;
        String itemType = meta.getPersistentDataContainer().get(itemTypeKey(plugin), PersistentDataType.STRING);
        return type.equals(itemType);
    }

    private static NamespacedKey itemTypeKey(SurvivalSkills plugin) {
        return new NamespacedKey(plugin, ITEM_TYPE_KEY);
    }

    private static NamespacedKey tableBlockKey(SurvivalSkills plugin) {
        return new NamespacedKey(plugin, TABLE_BLOCK_KEY);
    }
}
