package sir_draco.survivalskills.utils.items;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataType;

import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.ColorParser;

import java.util.List;
import java.util.function.Consumer;

public class ItemStackGeneratorUtils {

    public static NamespacedKey skillsItemKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
            "SurvivalSkills");
    public static NamespacedKey trophyItemKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
            "SurvivalSkillsTrophy");

    private static int potionBagIdCounter = 0;

    // Helpers

    public static ItemStack createLeatherArmorPiece(Material material, String name,
            List<String> lore, int modelData, Color color) {
        return createLeatherArmorPiece(material, name, lore, modelData, color, null);
    }

    public static ItemStack createLeatherArmorPiece(Material material, String name,
            List<String> lore, int modelData, Color color, Consumer<ItemMeta> attributeConfig) {
        ItemStack item = new ItemStackBuilder(material, 1, name)
                .lore(lore)
                .modelData(modelData)
                .build();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setUnbreakable(true);
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(color);
        }
        if (attributeConfig != null) {
            attributeConfig.accept(meta);
        }
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createArmorPiece(Material material, String name,
            List<String> lore, int modelData) {
        ItemStack item = new ItemStackBuilder(material, 1, name)
                .lore(lore)
                .modelData(modelData)
                .build();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack createGodQuestItem(Material material, String name, String hex1, String hex2) {
        return new ItemStackBuilder(material, 1, ColorParser.gradientName(name, hex1, hex2, true))
                .lore(List.of(ChatColor.GRAY + "Used in the God Trophy quest"))
                .modelData(42)
                .build();
    }

    // Custom model data helpers

    public static void setCustomModelData(ItemMeta meta, int modelData) {
        CustomModelDataComponent customModelData = meta.getCustomModelDataComponent();
        customModelData.setFloats(List.of((float) modelData));
        meta.setCustomModelDataComponent(customModelData);
    }

    public static boolean hasCustomModelData(ItemMeta meta) {
        return meta.hasCustomModelDataComponent()
                && !meta.getCustomModelDataComponent().getFloats().isEmpty();
    }

    public static boolean hasCustomModelData(ItemMeta meta, int modelData) {
        return hasCustomModelData(meta)
                && meta.getCustomModelDataComponent().getFloats().contains((float) modelData);
    }

    public static boolean isCustomItem(ItemStack item, int modelData) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        if (!meta.getPersistentDataContainer().has(skillsItemKey, PersistentDataType.BOOLEAN)) return false;
        return hasCustomModelData(meta, modelData);
    }

    public static boolean isCustomItem(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        if (!hasCustomModelData(meta)) return false;
        return meta.getPersistentDataContainer().has(skillsItemKey, PersistentDataType.BOOLEAN);
    }

    public static void addBeaconModifiers(ItemMeta meta, String keyPrefix, int armor, EquipmentSlotGroup slot) {
        meta.addAttributeModifier(Attribute.ARMOR,
                new AttributeModifier(new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class), keyPrefix + "Defense"),
                        armor, AttributeModifier.Operation.ADD_NUMBER, slot));
        meta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE,
                new AttributeModifier(new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class), keyPrefix + "Knockback"),
                        2.0, AttributeModifier.Operation.ADD_NUMBER, slot));
    }

    public static void addPowerModifiers(ItemMeta meta, String keyPrefix, int armor, EquipmentSlotGroup slot) {
        addPowerModifiers(meta, keyPrefix, armor, slot, 20);
    }

    public static void addPowerModifiers(ItemMeta meta, String keyPrefix, int armor, EquipmentSlotGroup slot, int knockback) {
        meta.addAttributeModifier(Attribute.ARMOR,
                new AttributeModifier(key(keyPrefix + "Armor"), armor,
                        AttributeModifier.Operation.ADD_NUMBER, slot));
        meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS,
                new AttributeModifier(key(keyPrefix + "Toughness"), 2,
                        AttributeModifier.Operation.ADD_NUMBER, slot));
        meta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE,
                new AttributeModifier(key(keyPrefix + "Knockback"), knockback,
                        AttributeModifier.Operation.ADD_NUMBER, slot));
        meta.addAttributeModifier(Attribute.BURNING_TIME,
                new AttributeModifier(key(keyPrefix + "Burning"), -1,
                        AttributeModifier.Operation.MULTIPLY_SCALAR_1, slot));
    }

    private static NamespacedKey key(String name) {
        return new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class), name);
    }

    public static int nextPotionBagId() {
        return potionBagIdCounter++;
    }
}
