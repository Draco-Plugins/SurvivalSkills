package sir_draco.survivalskills.utils.items;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;

public class ItemStackBuilder {
    private final Material material;
    private final int amount;
    private final String name;
    private List<String> lore;
    private int modelData;
    private boolean hideEnchants;
    private Map<Enchantment, Integer> enchants;

    public ItemStackBuilder(Material material, int amount, String name) {
        this.material = material;
        this.amount = amount;
        this.name = name;
    }

    public ItemStackBuilder lore(String loreLine) {
        this.lore = List.of(loreLine);
        return this;
    }

    public ItemStackBuilder lore(List<String> loreLines) {
        this.lore = loreLines;
        return this;
    }

    public ItemStackBuilder modelData(int modelData) {
        this.modelData = modelData;
        return this;
    }

    public ItemStackBuilder hideEnchants(boolean hideEnchants) {
        this.hideEnchants = hideEnchants;
        return this;
    }

    public ItemStackBuilder enchants(Map<Enchantment, Integer> enchants) {
        this.enchants = enchants;
        return this;
    }

    public ItemStack build() {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(name);
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore);
        }
        if (modelData != 0) {
            ItemStackGeneratorUtils.setCustomModelData(meta, modelData);
        }
        meta.getPersistentDataContainer().set(ItemStackGeneratorUtils.skillsItemKey,
                PersistentDataType.BOOLEAN, true);
        if (enchants != null) {
            for (Map.Entry<Enchantment, Integer> enchant : enchants.entrySet()) {
                item.addUnsafeEnchantment(enchant.getKey(), enchant.getValue());
            }
        }
        if (hideEnchants) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        return item;
    }
}
