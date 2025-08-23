package sir_draco.survivalskills.utils;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

import sir_draco.survivalskills.SurvivalSkills;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class ItemStackGenerator {

    public static NamespacedKey skillsItemKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
            "SurvivalSkills");

    /**
     * Creates a custom item based on the parameters
     * 
     * @param material     Item material
     * @param amount       Item Stack Quantity
     * @param name         Name
     * @param lore         Can be NULL, List of lore strings
     * @param modelData    Input 0 if no custom model data is necessary
     * @param hideEnchants Whether enchants should be hidden
     * @param enchants     Can be NULL, A map of enchants and their level to add to
     *                     the item
     * @return The Item Stack
     */
    @SuppressWarnings("deprecation")
    public static ItemStack createCustomItem(Material material, int amount, String name, ChatColor nameColor,
            String lore, ArrayList<String> loreList, int modelData, boolean hideEnchants,
            Map<Enchantment, Integer> enchants) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;
        if (nameColor != null)
            name = nameColor + name;
        meta.setDisplayName(name);
        if (lore != null) {
            List<String> loreHolder = new ArrayList<>();
            loreHolder.add(lore);
            meta.setLore(loreHolder);
        }
        if (loreList != null)
            meta.setLore(loreList);
        if (modelData != 0)
            meta.setCustomModelData(modelData);
        meta.getPersistentDataContainer().set(skillsItemKey, PersistentDataType.BOOLEAN, true);
        if (enchants != null)
            for (Map.Entry<Enchantment, Integer> enchant : enchants.entrySet())
                item.addUnsafeEnchantment(enchant.getKey(), enchant.getValue());
        if (hideEnchants)
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    @SuppressWarnings("deprecation")
    public static boolean isCustomItem(ItemStack item, int modelData) {
        if (item == null)
            return false;
        if (item.getItemMeta() == null)
            return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasCustomModelData())
            return false;
        if (!meta.getPersistentDataContainer().has(skillsItemKey, PersistentDataType.BOOLEAN))
            return false;
        return meta.getCustomModelData() == modelData;
    }

    @SuppressWarnings("deprecation")
    public static boolean isCustomItem(ItemStack item) {
        if (item == null)
            return false;
        if (item.getItemMeta() == null)
            return false;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasCustomModelData())
            return false;
        return meta.getPersistentDataContainer().has(skillsItemKey, PersistentDataType.BOOLEAN);
    }

    // Mining Items
    public static ItemStack getUnlimitedTorch() {
        String name = ColorParser.colorizeString("Unlimited Torch",
                ColorParser.generateGradient(ColorParser.rgbToHex(255, 0, 0), ColorParser.rgbToHex(80, 80, 80), 15),
                true);
        String lore = ChatColor.GRAY + "This torch will never run out!";
        return createCustomItem(Material.TORCH, 1, name, null, lore, null, 1, false, null);
    }

    public static ItemStack getMiningHelmet() {
        String name = ChatColor.GRAY.toString() + ChatColor.BOLD + "Mining Helmet";
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add("");
        loreList.add(ChatColor.GRAY + "Haste II Everywhere");
        loreList.add(ChatColor.GRAY + "Fire Resistance and Speed when below" + ChatColor.AQUA + " Y-64");
        loreList.add(ChatColor.GRAY + "Must wear the full set");
        ItemStack helmet = createCustomItem(Material.LEATHER_HELMET, 1, name, null, null, loreList, 3, false, null);
        ItemMeta meta = helmet.getItemMeta();
        if (meta == null)
            return helmet;
        meta.setUnbreakable(true);

        // Set the color of the helmet to gray
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(Color.GRAY);
        }

        helmet.setItemMeta(meta);
        return helmet;
    }

    public static ItemStack getMiningChestplate() {
        String name = ChatColor.GRAY.toString() + ChatColor.BOLD + "Mining Chestplate";
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add("");
        loreList.add(ChatColor.GRAY + "Haste II Everywhere");
        loreList.add(ChatColor.GRAY + "Fire Resistance and Speed when below" + ChatColor.AQUA + " Y-64");
        loreList.add(ChatColor.GRAY + "Must wear the full set");
        ItemStack chestplate = createCustomItem(Material.LEATHER_CHESTPLATE, 1, name, null, null, loreList, 3, false,
                null);
        ItemMeta meta = chestplate.getItemMeta();
        if (meta == null)
            return chestplate;
        meta.setUnbreakable(true);

        // Set the color of the helmet to gray
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(Color.GRAY);
        }

        chestplate.setItemMeta(meta);
        return chestplate;
    }

    public static ItemStack getMiningLeggings() {
        String name = ChatColor.GRAY.toString() + ChatColor.BOLD + "Mining Leggings";
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add("");
        loreList.add(ChatColor.GRAY + "Haste II Everywhere");
        loreList.add(ChatColor.GRAY + "Fire Resistance and Speed when below" + ChatColor.AQUA + " Y-64");
        loreList.add(ChatColor.GRAY + "Must wear the full set");
        ItemStack leggings = createCustomItem(Material.LEATHER_LEGGINGS, 1, name, null, null, loreList, 3, false, null);
        ItemMeta meta = leggings.getItemMeta();
        if (meta == null)
            return leggings;
        meta.setUnbreakable(true);

        // Set the color of the helmet to gray
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(Color.GRAY);
        }

        leggings.setItemMeta(meta);
        return leggings;
    }

    public static ItemStack getMiningBoots() {
        String name = ChatColor.GRAY.toString() + ChatColor.BOLD + "Mining Boots";
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add("");
        loreList.add(ChatColor.GRAY + "Haste II Everywhere");
        loreList.add(ChatColor.GRAY + "Fire Resistance and Speed when below" + ChatColor.AQUA + " Y-64");
        loreList.add(ChatColor.GRAY + "Must wear the full set");
        ItemStack boots = createCustomItem(Material.LEATHER_BOOTS, 1, name, null, null, loreList, 3, false, null);
        ItemMeta meta = boots.getItemMeta();
        if (meta == null)
            return boots;
        meta.setUnbreakable(true);

        // Set the color of the helmet to gray
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(Color.GRAY);
        }

        boots.setItemMeta(meta);
        return boots;
    }

    public static ItemStack getZapWand() {
        String name = ChatColor.GOLD.toString() + ChatColor.BOLD + "Zap Wand";
        String lore = ChatColor.GRAY + "Right click the ground to throw lightning";
        return createCustomItem(Material.LIGHTNING_ROD, 1, name, null, lore, null, 27, false, null);
    }

    public static ItemStack getBeaconHelmet() {
        List<List<String>> colors = new ArrayList<>();
        colors.add(ColorParser.generateGradient("#FF0000", "#00FF00", "Beacon "));
        colors.add(ColorParser.generateGradient("#00FF00", "#0000FF", "Helmet"));
        String name = ColorParser.colorizeString("Beacon Helmet", ColorParser.gradientConnector(colors), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add("");
        loreList.add(ChatColor.GRAY + "Beacon Effect Everywhere");
        loreList.add(ChatColor.GRAY + "Must wear the full set");
        ItemStack helmet = createCustomItem(Material.LEATHER_HELMET, 1, name, null, null, loreList, 29, false, null);
        ItemMeta meta = helmet.getItemMeta();
        if (meta == null)
            return helmet;
        meta.setUnbreakable(true);

        // Set the color of the helmet and armor value
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            NamespacedKey defenseKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                    "beaconHelmetDefense");
            NamespacedKey knockbackKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                    "beaconHelmetKnockback");
            @SuppressWarnings("UnstableApiUsage")
            AttributeModifier modifierDefense = new AttributeModifier(defenseKey, 4,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD);
            @SuppressWarnings("UnstableApiUsage")
            AttributeModifier modifierKnockback = new AttributeModifier(knockbackKey, 2.0,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD);
            leatherMeta.addAttributeModifier(Attribute.ARMOR, modifierDefense);
            leatherMeta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, modifierKnockback);
            leatherMeta.setColor(Color.WHITE);
        }

        helmet.setItemMeta(meta);
        return helmet;
    }

    public static ItemStack getBeaconChestplate() {
        List<List<String>> colors = new ArrayList<>();
        colors.add(ColorParser.generateGradient("#FF0000", "#00FF00", "Beacon "));
        colors.add(ColorParser.generateGradient("#00FF00", "#0000FF", "Chestplate"));
        String name = ColorParser.colorizeString("Beacon Chestplate", ColorParser.gradientConnector(colors), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add("");
        loreList.add(ChatColor.GRAY + "Beacon Effect Everywhere");
        loreList.add(ChatColor.GRAY + "Must wear the full set");
        ItemStack chestplate = createCustomItem(Material.LEATHER_CHESTPLATE, 1, name, null, null, loreList, 29, false,
                null);
        ItemMeta meta = chestplate.getItemMeta();
        if (meta == null)
            return chestplate;
        meta.setUnbreakable(true);

        // Set the color of the chestplate and armor value
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            NamespacedKey defenseKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                    "beaconChestplateDefense");
            NamespacedKey knockbackKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                    "beaconChestplateKnockback");
            @SuppressWarnings("UnstableApiUsage")
            AttributeModifier modifierDefense = new AttributeModifier(defenseKey, 9,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST);
            @SuppressWarnings("UnstableApiUsage")
            AttributeModifier modifierKnockback = new AttributeModifier(knockbackKey, 2.0,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST);
            leatherMeta.addAttributeModifier(Attribute.ARMOR, modifierDefense);
            leatherMeta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, modifierKnockback);
            leatherMeta.setColor(Color.WHITE);
        }

        chestplate.setItemMeta(meta);
        return chestplate;
    }

    public static ItemStack getBeaconLeggings() {
        List<List<String>> colors = new ArrayList<>();
        colors.add(ColorParser.generateGradient("#FF0000", "#00FF00", "Beacon "));
        colors.add(ColorParser.generateGradient("#00FF00", "#0000FF", "Leggings"));
        String name = ColorParser.colorizeString("Beacon Leggings", ColorParser.gradientConnector(colors), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add("");
        loreList.add(ChatColor.GRAY + "Beacon Effect Everywhere");
        loreList.add(ChatColor.GRAY + "Must wear the full set");
        ItemStack leggings = createCustomItem(Material.LEATHER_LEGGINGS, 1, name, null, null, loreList, 29, false,
                null);
        ItemMeta meta = leggings.getItemMeta();
        if (meta == null)
            return leggings;
        meta.setUnbreakable(true);

        // Set the color of the leggings and armor value
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            NamespacedKey defenseKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                    "beaconLeggingsDefense");
            NamespacedKey knockbackKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                    "beaconLeggingsKnockback");
            @SuppressWarnings("UnstableApiUsage")
            AttributeModifier modifierDefense = new AttributeModifier(defenseKey, 7,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS);
            @SuppressWarnings("UnstableApiUsage")
            AttributeModifier modifierKnockback = new AttributeModifier(knockbackKey, 2.0,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS);
            leatherMeta.addAttributeModifier(Attribute.ARMOR, modifierDefense);
            leatherMeta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, modifierKnockback);
            leatherMeta.setColor(Color.WHITE);
        }

        leggings.setItemMeta(meta);
        return leggings;
    }

    public static ItemStack getBeaconBoots() {
        List<List<String>> colors = new ArrayList<>();
        colors.add(ColorParser.generateGradient("#FF0000", "#00FF00", "Beacon "));
        colors.add(ColorParser.generateGradient("#00FF00", "#0000FF", "Boots"));
        String name = ColorParser.colorizeString("Beacon Boots", ColorParser.gradientConnector(colors), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add("");
        loreList.add(ChatColor.GRAY + "Beacon Effect Everywhere");
        loreList.add(ChatColor.GRAY + "Must wear the full set");
        ItemStack boots = createCustomItem(Material.LEATHER_BOOTS, 1, name, null, null, loreList, 29, false, null);
        ItemMeta meta = boots.getItemMeta();
        if (meta == null)
            return boots;
        meta.setUnbreakable(true);

        // Set the color of the boots and armor value
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            NamespacedKey defenseKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                    "beaconBootsDefense");
            NamespacedKey knockbackKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                    "beaconBootsKnockback");
            @SuppressWarnings("UnstableApiUsage")
            AttributeModifier modifierDefense = new AttributeModifier(defenseKey, 4,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET);
            @SuppressWarnings("UnstableApiUsage")
            AttributeModifier modifierKnockback = new AttributeModifier(knockbackKey, 2.0,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET);
            leatherMeta.addAttributeModifier(Attribute.ARMOR, modifierDefense);
            leatherMeta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, modifierKnockback);
            leatherMeta.setColor(Color.WHITE);
        }

        boots.setItemMeta(meta);
        return boots;
    }

    public static ItemStack getPowerOre() {
        String name = ColorParser.colorizeString("Power Ore",
                ColorParser.generateGradient("#8008FB", "#FD242D", "Power Ore"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "The ore is teeming with energy");
        Map<Enchantment, Integer> enchants = new HashMap<>();
        enchants.put(Enchantment.KNOCKBACK, 10);
        return createCustomItem(Material.OBSIDIAN, 1, name, null, null, loreList, 44, true, enchants);
    }

    public static ItemStack getPowerLaser() {
        String name = ColorParser.colorizeString("Power Laser",
                ColorParser.generateGradient("#8008FB", "#FD242D", "Power Laser"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "The laser is teeming with energy");
        loreList.add(ChatColor.LIGHT_PURPLE + "Right click to shoot a laser");
        return createCustomItem(Material.END_CRYSTAL, 1, name, null, null, loreList, 50, false, null);
    }

    public static ItemStack getPowerHelmet() {
        String name = ColorParser.colorizeString("Power Helmet",
                ColorParser.generateGradient("#8008FB", "#FD242D", "Power Helmet"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "The helmet is teeming with energy");
        ItemStack helmet = createCustomItem(Material.LEATHER_HELMET, 1, name, null, null, loreList, 51, false, null);
        ItemMeta meta = helmet.getItemMeta();
        if (meta == null)
            return helmet;
        meta.setUnbreakable(true);

        if (meta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(Color.fromRGB(73, 0, 110));

            NamespacedKey defenseKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                    "powerHelmetDefense");
            NamespacedKey toughnessKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                    "powerHelmetToughness");
            NamespacedKey knockbackKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                    "powerHelmetKnockback");
            NamespacedKey burningKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                    "powerHelmetBurning");

            AttributeModifier modifierDefense = new AttributeModifier(defenseKey, 6,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD);
            AttributeModifier modifierToughness = new AttributeModifier(toughnessKey, 2,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD);
            AttributeModifier modifierKnockback = new AttributeModifier(knockbackKey, 20,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.HEAD);
            AttributeModifier modifierBurning = new AttributeModifier(burningKey, -1,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.HEAD);

            meta.addAttributeModifier(Attribute.ARMOR, modifierDefense);
            meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, modifierToughness);
            meta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, modifierKnockback);
            meta.addAttributeModifier(Attribute.BURNING_TIME, modifierBurning);
        }

        helmet.setItemMeta(meta);
        return helmet;
    }

    public static ItemStack getPowerChestplate() {
        String name = ColorParser.colorizeString("Power Chestplate",
                ColorParser.generateGradient("#8008FB", "#FD242D", "Power Chestplate"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "The chestplate is teeming with energy");
        loreList.add(ChatColor.LIGHT_PURPLE + "The full armor absorbs all damage");
        loreList.add(ChatColor.LIGHT_PURPLE + "When it has reached its limit you will take twice as much damage");
        loreList.add(ChatColor.LIGHT_PURPLE + "Sneak twice in quick succession to unleash the stored power!");
        ItemStack chestplate = createCustomItem(Material.LEATHER_CHESTPLATE, 1, name, null, null, loreList, 51, false,
                null);
        ItemMeta meta = chestplate.getItemMeta();
        if (meta == null)
            return chestplate;
        meta.setUnbreakable(true);

        if (meta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(Color.fromRGB(73, 0, 110));

            NamespacedKey armorKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                    "powerChestplateArmor");
            NamespacedKey toughnessKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                    "powerChestplateToughness");
            NamespacedKey knockbackKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                    "powerChestplateKnockback");
            NamespacedKey burningKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                    "powerChestplateBurning");

            AttributeModifier modifierArmor = new AttributeModifier(armorKey, 12,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST);
            AttributeModifier modifierToughness = new AttributeModifier(toughnessKey, 2,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST);
            AttributeModifier modifierKnockback = new AttributeModifier(knockbackKey, 20,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.CHEST);
            AttributeModifier modifierBurning = new AttributeModifier(burningKey, -1,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.CHEST);

            meta.addAttributeModifier(Attribute.ARMOR, modifierArmor);
            meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, modifierToughness);
            meta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, modifierKnockback);
            meta.addAttributeModifier(Attribute.BURNING_TIME, modifierBurning);
        }

        chestplate.setItemMeta(meta);
        return chestplate;
    }

    public static ItemStack getPowerLeggings() {
        String name = ColorParser.colorizeString("Power Leggings",
                ColorParser.generateGradient("#8008FB", "#FD242D", "Power Leggings"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "The leggings are teeming with energy");
        ItemStack leggings = createCustomItem(Material.LEATHER_LEGGINGS, 1, name, null, null, loreList, 51, false,
                null);
        ItemMeta meta = leggings.getItemMeta();
        if (meta == null)
            return leggings;
        meta.setUnbreakable(true);

        if (meta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(Color.fromRGB(73, 0, 110));

            NamespacedKey armorKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                    "powerLeggingsArmor");
            NamespacedKey toughnessKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                    "powerLeggingsToughness");
            NamespacedKey knockbackKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                    "powerLeggingsKnockback");
            NamespacedKey burningKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                    "powerLeggingsBurning");

            AttributeModifier modifierArmor = new AttributeModifier(armorKey, 9,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS);
            AttributeModifier modifierToughness = new AttributeModifier(toughnessKey, 2,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS);
            AttributeModifier modifierKnockback = new AttributeModifier(knockbackKey, 20,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.LEGS);
            AttributeModifier modifierBurning = new AttributeModifier(burningKey, -1,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.LEGS);

            meta.addAttributeModifier(Attribute.ARMOR, modifierArmor);
            meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, modifierToughness);
            meta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, modifierKnockback);
            meta.addAttributeModifier(Attribute.BURNING_TIME, modifierBurning);
        }

        leggings.setItemMeta(meta);
        return leggings;
    }

    public static ItemStack getPowerBoots() {
        String name = ColorParser.colorizeString("Power Boots",
                ColorParser.generateGradient("#8008FB", "#FD242D", "Power Boots"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "The boots are teeming with energy");
        ItemStack boots = createCustomItem(Material.LEATHER_BOOTS, 1, name, null, null, loreList, 51, false, null);
        ItemMeta meta = boots.getItemMeta();
        if (meta == null)
            return boots;
        meta.addAttributeModifier(Attribute.ARMOR,
                new AttributeModifier(skillsItemKey, 6, AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.FEET));
        meta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE,
                new AttributeModifier(skillsItemKey, 2.0, AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.FEET));
        meta.addAttributeModifier(Attribute.BURNING_TIME,
                new AttributeModifier(skillsItemKey, 0, AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.FEET));
        meta.setUnbreakable(true);

        if (meta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(Color.fromRGB(73, 0, 110));

            NamespacedKey armorKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                    "powerBootsArmor");
            NamespacedKey toughnessKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                    "powerBootsToughness");
            NamespacedKey knockbackKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                    "powerBootsKnockback");
            NamespacedKey burningKey = new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class),
                    "powerBootsBurning");

            AttributeModifier modifierArmor = new AttributeModifier(armorKey, 6,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET);
            AttributeModifier modifierToughness = new AttributeModifier(toughnessKey, 2,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET);
            AttributeModifier modifierKnockback = new AttributeModifier(knockbackKey, 0,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.FEET);
            AttributeModifier modifierBurning = new AttributeModifier(burningKey, -1,
                    AttributeModifier.Operation.MULTIPLY_SCALAR_1, EquipmentSlotGroup.FEET);

            meta.addAttributeModifier(Attribute.ARMOR, modifierArmor);
            meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, modifierToughness);
            meta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, modifierKnockback);
            meta.addAttributeModifier(Attribute.BURNING_TIME, modifierBurning);
        }

        boots.setItemMeta(meta);
        return boots;
    }

    public static ItemStack getPowerSword() {
        String name = ColorParser.colorizeString("Power Sword",
                ColorParser.generateGradient("#8008FB", "#FD242D", "Power Sword"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "The sword is teeming with energy");
        loreList.add(ChatColor.LIGHT_PURPLE + "Right click to perform a dash attack");
        ItemStack sword = createCustomItem(Material.NETHERITE_SWORD, 1, name, null, null, loreList, 47, false, null);
        ItemMeta meta = sword.getItemMeta();
        if (meta == null)
            return sword;
        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(skillsItemKey, 15,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        meta.setUnbreakable(true);
        sword.setItemMeta(meta);
        return sword;
    }

    public static ItemStack getPowerDrill() {
        String name = ColorParser.colorizeString("Power Drill",
                ColorParser.generateGradient("#8008FB", "#FD242D", "Power Drill"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "The drill is teeming with energy");
        loreList.add(ChatColor.LIGHT_PURPLE + "Demolish blocks with ease");
        ItemStack drill = createCustomItem(Material.NETHERITE_PICKAXE, 1, name, null, null, loreList, 48, false, null);
        ItemMeta meta = drill.getItemMeta();
        if (meta == null)
            return drill;
        meta.addAttributeModifier(Attribute.BLOCK_BREAK_SPEED, new AttributeModifier(skillsItemKey, -0.75,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        meta.setUnbreakable(true);
        drill.setItemMeta(meta);
        return drill;
    }

    // Building

    public static ItemStack getSortWand() {
        String name = ChatColor.BLUE.toString() + ChatColor.BOLD + "Sort Wand";
        String lore = ChatColor.GRAY + "Left click a chest to sort its inventory";
        return createCustomItem(Material.BLAZE_ROD, 1, name, null, lore, null, 16, false, null);
    }

    // Exploring
    public static ItemStack getMagnet() {
        String name = ColorParser.colorizeString("Magnet", ColorParser.generateGradient("#FF7400", "#ADF3FD", "Magnet"),
                true);
        String lore = ChatColor.GRAY + "Attracts items to you when held in your hand";
        HashMap<Enchantment, Integer> enchants = new HashMap<>();
        enchants.put(Enchantment.FORTUNE, 1);
        return createCustomItem(Material.HOPPER, 1, name, null, lore, null, 32, true, enchants);
    }

    public static ItemStack getJumpingBoots() {
        String name = ColorParser.colorizeString("Jumping Boots",
                ColorParser.generateGradient("#FFFFFF", "#015210", "Jumping Boots"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add("");
        loreList.add(ChatColor.GRAY + "Jump Boost II Everywhere");
        loreList.add(ChatColor.GRAY + "Prevents Fall Damage");
        return createCustomItem(Material.IRON_BOOTS, 1, name, null, null, loreList, 4, false, null);
    }

    public static ItemStack getWandererBoots() {
        String name = ColorParser.colorizeString("Wanderer Boots",
                ColorParser.generateGradient("#FFF000", "#FFDC6E", "Wanderer Boots"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Speed I Everywhere");
        loreList.add(ChatColor.GRAY + "Must wear the full set");
        return createCustomItem(Material.CHAINMAIL_BOOTS, 1, name, null, null, loreList, 5, false, null);
    }

    public static ItemStack getWandererLeggings() {
        String name = ColorParser.colorizeString("Wanderer Leggings",
                ColorParser.generateGradient("#FFF000", "#FFDC6E", "Wanderer Leggings"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Speed I Everywhere");
        loreList.add(ChatColor.GRAY + "Must wear the full set");
        return createCustomItem(Material.CHAINMAIL_LEGGINGS, 1, name, null, null, loreList, 5, false, null);
    }

    public static ItemStack getWandererChestplate() {
        String name = ColorParser.colorizeString("Wanderer Chestplate",
                ColorParser.generateGradient("#FFF000", "#FFDC6E", "Wanderer Chestplate"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Speed I Everywhere");
        loreList.add(ChatColor.GRAY + "Must wear the full set");
        return createCustomItem(Material.CHAINMAIL_CHESTPLATE, 1, name, null, null, loreList, 5, false, null);
    }

    public static ItemStack getWandererHelmet() {
        String name = ColorParser.colorizeString("Wanderer Helmet",
                ColorParser.generateGradient("#FFF000", "#FFDC6E", "Wanderer Helmet"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Speed I Everywhere");
        loreList.add(ChatColor.GRAY + "Must wear the full set");
        return createCustomItem(Material.CHAINMAIL_HELMET, 1, name, null, null, loreList, 5, false, null);
    }

    public static ItemStack getCaveFinder() {
        String name = ColorParser.colorizeString("Cave Finder",
                ColorParser.generateGradient("#FFFFFF", "#000000", "Cave Finder"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add("");
        loreList.add(ChatColor.GRAY + "Right Click to find the nearest cave");
        loreList.add(ChatColor.GRAY + "Tells you if it is a confirmed cave or a potential dark spot");
        return createCustomItem(Material.COMPASS, 1, name, null, null, loreList, 6, false, null);
    }

    public static ItemStack getTravelerBoots() {
        String name = ColorParser.colorizeString("Traveler Boots",
                ColorParser.generateGradient("#FF8800", "#FFDC6E", "Traveler Boots"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Speed II Everywhere");
        loreList.add(ChatColor.GRAY + "Must wear the full set");
        return createCustomItem(Material.DIAMOND_BOOTS, 1, name, null, null, loreList, 7, false, null);
    }

    public static ItemStack getTravelerLeggings() {
        String name = ColorParser.colorizeString("Traveler Leggings",
                ColorParser.generateGradient("#FF8800", "#FFDC6E", "Traveler Leggings"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Speed II Everywhere");
        loreList.add(ChatColor.GRAY + "Must wear the full set");
        return createCustomItem(Material.DIAMOND_LEGGINGS, 1, name, null, null, loreList, 7, false, null);
    }

    public static ItemStack getTravelerChestplate() {
        String name = ColorParser.colorizeString("Traveler Chestplate",
                ColorParser.generateGradient("#FF8800", "#FFDC6E", "Traveler Chestplate"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Speed II Everywhere");
        loreList.add(ChatColor.GRAY + "Must wear the full set");
        return createCustomItem(Material.DIAMOND_CHESTPLATE, 1, name, null, null, loreList, 7, false, null);
    }

    public static ItemStack getTravelerHelmet() {
        String name = ColorParser.colorizeString("Traveler Helmet",
                ColorParser.generateGradient("#FF8800", "#FFDC6E", "Traveler Helmet"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Speed II Everywhere");
        loreList.add(ChatColor.GRAY + "Must wear the full set");
        return createCustomItem(Material.DIAMOND_HELMET, 1, name, null, null, loreList, 7, false, null);
    }

    public static ItemStack getGillHelmet() {
        String name = ColorParser.colorizeString("Gill Helmet",
                ColorParser.generateGradient("#86A8FF", "#ADF3FD", "Gill Helmet"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add("");
        loreList.add(ChatColor.GRAY + "Water Breathing Everywhere");
        loreList.add(ChatColor.GRAY + "Lightning Fast Swim Speed");
        loreList.add(ChatColor.GRAY + "Must wear the full set");
        ItemStack helmet = createCustomItem(Material.LEATHER_HELMET, 1, name, null, null, loreList, 19, false, null);
        ItemMeta meta = helmet.getItemMeta();
        if (meta == null)
            return helmet;
        meta.setUnbreakable(true);

        // Set the color of the helmet to blue
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(Color.BLUE);
        }

        helmet.setItemMeta(meta);
        return helmet;
    }

    public static ItemStack getGillChestplate() {
        String name = ColorParser.colorizeString("Gill Chestplate",
                ColorParser.generateGradient("#86A8FF", "#ADF3FD", "Gill Chestplate"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add("");
        loreList.add(ChatColor.GRAY + "Water Breathing Everywhere");
        loreList.add(ChatColor.GRAY + "Lightning Fast Swim Speed");
        loreList.add(ChatColor.GRAY + "Must wear the full set");
        ItemStack chestplate = createCustomItem(Material.LEATHER_CHESTPLATE, 1, name, null, null, loreList, 19, false,
                null);
        ItemMeta meta = chestplate.getItemMeta();
        if (meta == null)
            return chestplate;
        meta.setUnbreakable(true);

        // Set the color of the chestplate to blue
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(Color.BLUE);
        }

        chestplate.setItemMeta(meta);
        return chestplate;
    }

    public static ItemStack getGillLeggings() {
        String name = ColorParser.colorizeString("Gill Leggings",
                ColorParser.generateGradient("#86A8FF", "#ADF3FD", "Gill Leggings"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add("");
        loreList.add(ChatColor.GRAY + "Water Breathing Everywhere");
        loreList.add(ChatColor.GRAY + "Lightning Fast Swim Speed");
        loreList.add(ChatColor.GRAY + "Must wear the full set");
        ItemStack leggings = createCustomItem(Material.LEATHER_LEGGINGS, 1, name, null, null, loreList, 19, false,
                null);
        ItemMeta meta = leggings.getItemMeta();
        if (meta == null)
            return leggings;
        meta.setUnbreakable(true);

        // Set the color of the leggings to blue
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(Color.BLUE);
        }

        leggings.setItemMeta(meta);
        return leggings;
    }

    public static ItemStack getGillBoots() {
        String name = ColorParser.colorizeString("Gill Boots",
                ColorParser.generateGradient("#86A8FF", "#ADF3FD", "Gill Boots"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add("");
        loreList.add(ChatColor.GRAY + "Water Breathing Everywhere");
        loreList.add(ChatColor.GRAY + "Lightning Fast Swim Speed");
        loreList.add(ChatColor.GRAY + "Must wear the full set");
        ItemStack boots = createCustomItem(Material.LEATHER_BOOTS, 1, name, null, null, loreList, 19, false, null);
        ItemMeta meta = boots.getItemMeta();
        if (meta == null)
            return boots;
        meta.setUnbreakable(true);

        // Set the color of the boots to blue
        if (meta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(Color.BLUE);
        }

        boots.setItemMeta(meta);
        return boots;
    }

    public static ItemStack getAdventurerBoots() {
        String name = ColorParser.colorizeString("Adventurer Boots",
                ColorParser.generateGradient("#FF0000", "#FFDC6E", "Adventurer Boots"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add("");
        loreList.add(ChatColor.GRAY + "Speed III Everywhere");
        loreList.add(ChatColor.GRAY + "Prevents Fall Damage");
        loreList.add(ChatColor.GRAY + "Must wear the full set");
        ItemStack item = createCustomItem(Material.NETHERITE_BOOTS, 1, name, null, null, loreList, 8, false, null);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getAdventurerLeggings() {
        String name = ColorParser.colorizeString("Adventurer Leggings",
                ColorParser.generateGradient("#FF0000", "#FFDC6E", "Adventurer Leggings"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add("");
        loreList.add(ChatColor.GRAY + "Speed III Everywhere");
        loreList.add(ChatColor.GRAY + "Prevents Fall Damage");
        loreList.add(ChatColor.GRAY + "Must wear the full set");
        ItemStack item = createCustomItem(Material.NETHERITE_LEGGINGS, 1, name, null, null, loreList, 8, false, null);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getAdventurerChestplate() {
        String name = ColorParser.colorizeString("Adventurer Chestplate",
                ColorParser.generateGradient("#FF0000", "#FFDC6E", "Adventurer Chestplate"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add("");
        loreList.add(ChatColor.GRAY + "Speed III Everywhere");
        loreList.add(ChatColor.GRAY + "Prevents Fall Damage");
        loreList.add(ChatColor.GRAY + "Must wear the full set");
        ItemStack item = createCustomItem(Material.NETHERITE_CHESTPLATE, 1, name, null, null, loreList, 8, false, null);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getAdventurerHelmet() {
        String name = ColorParser.colorizeString("Adventurer Helmet",
                ColorParser.generateGradient("#FF0000", "#FFDC6E", "Adventurer Helmet"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add("");
        loreList.add(ChatColor.GRAY + "Speed III Everywhere");
        loreList.add(ChatColor.GRAY + "Prevents Fall Damage");
        loreList.add(ChatColor.GRAY + "Must wear the full set");
        ItemStack item = createCustomItem(Material.NETHERITE_HELMET, 1, name, null, null, loreList, 8, false, null);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    // Farming
    public static ItemStack getWateringCan() {
        String name = ColorParser.colorizeString("Watering Can",
                ColorParser.generateGradient("#00FFFF", "#000000", "Watering Can"), true);
        String lore = ChatColor.GRAY + "Right Click to water crops in a 5x5 area";
        return createCustomItem(Material.PRISMARINE_SHARD, 1, name, null, lore, null, 9, false, null);
    }

    public static ItemStack getUnlimitedBoneMeal() {
        String name = ColorParser.colorizeString("Unlimited Bonemeal",
                ColorParser.generateGradient("#000000", "#515151", "Unlimited Bonemeal"), true);
        String lore = ChatColor.GRAY + "Infinite crop growth";
        return createCustomItem(Material.BONE_MEAL, 1, name, null, lore, null, 10, false, null);
    }

    public static ItemStack getHarvester() {
        String name = ColorParser.colorizeString("Harvester",
                ColorParser.generateGradient("#00FF00", "#000000", "Harvester"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add("");
        loreList.add(ChatColor.GRAY + "Breaks and replants crops for you!");
        loreList.add(ChatColor.GRAY + "Cooldown of 3 seconds");
        ItemStack item = createCustomItem(Material.NETHERITE_HOE, 1, name, null, null, loreList, 11, false, null);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    // Boss Items
    public static ItemStack getGiantBossItem() {
        String giantName = ColorParser.colorizeString("Giant Head",
                ColorParser.generateGradient(ColorParser.rgbToHex(0, 255, 0), ColorParser.rgbToHex(80, 80, 80), 10),
                true);
        String giantLore = ChatColor.GRAY + "I wonder how big his...";
        return createCustomItem(Material.ZOMBIE_HEAD, 1, giantName, null, giantLore, null, 2, false, null);
    }

    public static ItemStack getFishingBossItem() {
        String fishName = ColorParser.colorizeString("Sea King Scale",
                ColorParser.generateGradient("#00FFFF", "#0000FF", 14), true);
        String fishLore = ChatColor.GRAY + "Your fishing perseverance was noticed by the king";
        return createCustomItem(Material.PRISMARINE_SHARD, 1, fishName, null, fishLore, null, 2, false, null);
    }

    public static ItemStack getBroodMotherBossItem() {
        String spiderName = ColorParser.colorizeString("BroodMother Web",
                ColorParser.generateGradient("#FFFFFF", "#43000E", 15), true);
        String spiderLore = ChatColor.GRAY + "The Queen recognized your greatness as she fell";
        return createCustomItem(Material.COBWEB, 1, spiderName, null, spiderLore, null, 2, false, null);
    }

    public static ItemStack getElderGuardianBossItem() {
        String fishName = ColorParser.colorizeString("Guardian Eye",
                ColorParser.generateGradient("#001EFF", "#E900FF", 12), true);
        String fishLore = ChatColor.GRAY + "It seems like it is still watching you";
        return createCustomItem(Material.ENDER_EYE, 1, fishName, null, fishLore, null, 2, false, null);
    }

    public static ItemStack getWardenBossItem() {
        String wardenName = ColorParser.colorizeString("Warden Heart",
                ColorParser.generateGradient("#323232", "#C776FF", 12), true);
        String wardenLore = ChatColor.GRAY + "You faintly hear the shrieks of ancient times";
        return createCustomItem(Material.ECHO_SHARD, 1, wardenName, null, wardenLore, null, 2, false, null);
    }

    public static ItemStack getVillagerBossItem() {
        String villagerName = ColorParser.colorizeString("Minecraft Essence",
                ColorParser.generateGradient("#2C1F0C", "#005306", 17), true);
        String villagerLore = ChatColor.GRAY + "The villager gained access to the game code itself";
        return createCustomItem(Material.PLAYER_HEAD, 1, villagerName, null, villagerLore, null, 2, false, null);
    }

    public static ItemStack getEnderDragonBossItem() {
        String dragonName = ColorParser.colorizeString("Dragon Head",
                ColorParser.generateGradient("#8C00FF", "#180152", 14), true);
        String dragonLore = ChatColor.GRAY + "The ender dragon guarded the void from invaders";
        return createCustomItem(Material.DRAGON_HEAD, 1, dragonName, null, dragonLore, null, 2, false, null);
    }

    // Summoner Items
    public static ItemStack getGiantSummoner() {
        String name = ColorParser.colorizeString("Giant Summoner Egg", ColorParser.generateGradient(
                ColorParser.rgbToHex(0, 255, 0), ColorParser.rgbToHex(80, 80, 80), "Giant Summoner Egg"), true);
        String lore = ChatColor.GRAY + "Must spawn at night";
        return createCustomItem(Material.ZOMBIE_SPAWN_EGG, 1, name, null, lore, null, 12, false, null);
    }

    public static ItemStack getBroodMotherSummoner() {
        String name = ColorParser.colorizeString("BroodMother Summoner Egg",
                ColorParser.generateGradient("#FFFFFF", "#43000E", "BroodMother Summoner Egg"), true);
        String lore = ChatColor.GRAY + "Make sure you are prepared";
        return createCustomItem(Material.SPIDER_SPAWN_EGG, 1, name, null, lore, null, 13, false, null);
    }

    public static ItemStack getVillagerSummoner() {
        String name = ColorParser.colorizeString("Villager Summoner Egg",
                ColorParser.generateGradient("#2C1F0C", "#005306", "Villager Summoner Egg"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add("");
        loreList.add(ChatColor.GRAY + "The true final boss of Minecraft");
        loreList.add(ChatColor.RED + "This boss is very difficult and will require many attempts!");
        loreList.add(ChatColor.RED.toString() + ChatColor.BOLD + "THIS IS A DESTRUCTIVE FIGHT!!!");
        return createCustomItem(Material.VILLAGER_SPAWN_EGG, 1, name, null, null, loreList, 14, false, null);
    }

    // Fishing Exotics
    public static ItemStack getUnlimitedWaterBucket() {
        String name = ColorParser.colorizeString("Unlimited Water Bucket",
                ColorParser.generateGradient("#08C8FB", "#1D5E67", "Unlimited Water Bucket"), true);
        String lore = ChatColor.GRAY + "Infinite water";
        return createCustomItem(Material.WATER_BUCKET, 1, name, null, lore, null, 20, false, null);
    }

    public static ItemStack getUnlimitedLavaBucket() {
        String name = ColorParser.colorizeString("Unlimited Lava Bucket",
                ColorParser.generateGradient("#FB5908", "#DA0A0A", "Unlimited Lava Bucket"), true);
        String lore = ChatColor.GRAY + "Infinite lava";
        return createCustomItem(Material.LAVA_BUCKET, 1, name, null, lore, null, 21, false, null);
    }

    public static ItemStack getWeatherArtifact() {
        String name = ColorParser.colorizeString("Weather Artifact",
                ColorParser.generateGradient("#FFF000", "#2E3435", "Weather Artifact"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Right click to change the weather");
        loreList.add(ChatColor.GRAY + "1 Hour Cooldown");
        loreList.add(ChatColor.GRAY + "Last Used: Never");
        return createCustomItem(Material.BREEZE_ROD, 1, name, null, null, loreList, 22, false, null);
    }

    public static ItemStack getTimeArtifact() {
        String name = ColorParser.colorizeString("Time Artifact",
                ColorParser.generateGradient("#FFFFFF", "#000000", "Time Artifact"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Right click to change the time");
        loreList.add(ChatColor.GRAY + "1 Hour Cooldown");
        loreList.add(ChatColor.GRAY + "Last Used: Never");
        return createCustomItem(Material.CLOCK, 1, name, null, null, loreList, 23, false, null);
    }

    public static ItemStack getExperienceMultiplierVoucher(int multiplier, int duration) {
        String name = ColorParser.colorizeString("XP Voucher",
                ColorParser.generateGradient("#FFC600", "#FFF080", multiplier + "x XP Voucher"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Multiplies XP gained for " + duration + " minutes");
        ItemStack voucher = createCustomItem(Material.PAPER, 1, name, null, null, loreList, 26, false, null);
        ItemMeta meta = voucher.getItemMeta();
        if (meta == null)
            return voucher;

        // Store the multiplier and duration in the item
        meta.getPersistentDataContainer().set(
                new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class), "xpMultiplier"),
                PersistentDataType.INTEGER, multiplier);
        meta.getPersistentDataContainer().set(
                new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class), "xpDuration"),
                PersistentDataType.INTEGER, duration);

        voucher.setItemMeta(meta);
        return voucher;
    }

    public static ItemStack getUnlimitedEmptyBucket() {
        String name = ColorParser.colorizeString("Unlimited Empty Bucket",
                ColorParser.generateGradient("#FFFFFF", "#777777", "Unlimited Empty Bucket"), true);
        String lore = ChatColor.GRAY + "Infinite empty bucket";
        return createCustomItem(Material.BUCKET, 1, name, null, lore, null, 30, false, null);
    }

    public static ItemStack getUnlimitedRocket() {
        String name = ColorParser.colorizeString("Unlimited Rocket",
                ColorParser.generateGradient("#FF0000", "#FFFFFF", "Unlimited Rocket"), true);
        String lore = ChatColor.GRAY + "Infinite rocket";
        return createCustomItem(Material.FIREWORK_ROCKET, 1, name, null, lore, null, 31, false, null);
    }

    // Unique Mob Drops - See GodListener for mapping of mob to item
    public static ItemStack getWebShooter() {
        String name = ColorParser.colorizeString("Web Shooter",
                ColorParser.generateGradient("#FFFFFF", "#2E3526", "Web Shooter"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Right click to shoot a web");
        loreList.add(ChatColor.GRAY + "Used in the God Trophy quest");
        loreList.add("");
        loreList.add("~Spider Exotic~");
        return createCustomItem(Material.COBWEB, 1, name, null, null, loreList, 33, false, null);
    }

    public static ItemStack getUnlimitedTippedArrow() {
        String name = ColorParser.colorizeString("Unlimited Tipped Arrow",
                ColorParser.generateGradient("#FB08F2", "#ADF3FD", "Unlimited Tipped Arrow"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "A different tipped arrow is used every time");
        loreList.add(ChatColor.GRAY + "Used in the God Trophy quest");
        loreList.add("");
        loreList.add("~Skeleton Exotic~");
        return createCustomItem(Material.TIPPED_ARROW, 1, name, null, null, loreList, 34, false, null);
    }

    public static ItemStack getVillagerRevivalArtifact() {
        String name = ColorParser.colorizeString("Villager Revival Artifact",
                ColorParser.generateGradient("#83FB08", "#FCFF7A", "Villager Revival Artifact"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Right click a zombie villager to revive it");
        loreList.add(ChatColor.GRAY + "Used in the God Trophy quest");
        loreList.add("");
        loreList.add("~Zombie Exotic~");
        return createCustomItem(Material.EMERALD, 1, name, null, null, loreList, 35, false, null);
    }

    public static ItemStack getEnderEssence() {
        String name = ColorParser.colorizeString("Ender Essence",
                ColorParser.generateGradient("#29006E", "#C54FFF", "Ender Essence"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Right click to teleport 5 blocks ahead");
        loreList.add(ChatColor.GRAY + "Used in the God Trophy quest");
        loreList.add("");
        loreList.add("~Enderman Exotic~");
        return createCustomItem(Material.ENDER_PEARL, 1, name, null, null, loreList, 36, false, null);
    }

    public static ItemStack getCreeperEssence() {
        String name = ColorParser.colorizeString("Creeper Essence",
                ColorParser.generateGradient("#199D00", "#FFFFFF", "Creeper Essence"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Right click to explode");
        loreList.add(ChatColor.GRAY + "Used in the God Trophy quest");
        loreList.add("");
        loreList.add("~Creeper Exotic~");
        return createCustomItem(Material.GUNPOWDER, 1, name, null, null, loreList, 37, false, null);
    }

    public static ItemStack getPotionBag(int id) {
        String name = ColorParser.colorizeString("Potion Bag",
                ColorParser.generateGradient("#FFFFFF", "#000000", "Potion Bag"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Right click to open");
        loreList.add("~Witch Exotic~");
        ItemStack bag = createCustomItem(Material.CHEST, 1, name, null, null, loreList, 38, false, null);
        ItemMeta meta = bag.getItemMeta();
        if (meta == null)
            return bag;
        meta.getPersistentDataContainer().set(
                new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class), "potion_bag"),
                PersistentDataType.INTEGER, id);
        bag.setItemMeta(meta);
        return bag;
    }

    public static ItemStack getMagicBagOfWind() {
        String name = ColorParser.colorizeString("Magic Bag O' Wind",
                ColorParser.generateGradient("#08FB75", "#ADF3FD", "Magic Bag O' Wind"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Right click to launch yourself in the air");
        loreList.add(ChatColor.GRAY + "Used in the God Trophy quest");
        loreList.add("");
        loreList.add("~Breeze Exotic~");
        return createCustomItem(Material.WIND_CHARGE, 1, name, null, null, loreList, 39, false, null);
    }

    public static ItemStack getDragonBreathCannon() {
        String name = ColorParser.colorizeString("Dragon Breath Cannon",
                ColorParser.generateGradient("#5608FB", "#343434", "Dragon Breath Cannon"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Right click to shoot dragon breath");
        loreList.add(ChatColor.GRAY + "Used in the God Trophy quest");
        loreList.add("");
        loreList.add("~Ender Dragon Exotic~");
        return createCustomItem(Material.DRAGON_BREATH, 1, name, null, null, loreList, 40, false, null);
    }

    public static ItemStack getUnlimitedWitherRose() {
        String name = ColorParser.colorizeString("Unlimited Wither Rose",
                ColorParser.generateGradient("#000000", "#515151", "Unlimited Wither Rose"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Infinite wither rose");
        loreList.add(ChatColor.GRAY + "Used in the God Trophy quest");
        loreList.add("");
        loreList.add("~Wither Skeleton Exotic~");
        return createCustomItem(Material.WITHER_ROSE, 1, name, null, null, loreList, 41, false, null);
    }

    public static ItemStack getUnlimitedSponge() {
        String name = ColorParser.colorizeString("Unlimited Sponge",
                ColorParser.generateGradient("#ADFDDD", "#F8FB08", "Unlimited Sponge"), true);
        String lore = ChatColor.GRAY + "Soak up the world!";
        return createCustomItem(Material.SPONGE, 1, name, null, lore, null, 46, false, null);
    }

    public static ItemStack getBroodingSilk() {
        String name = ColorParser.colorizeString("Brooding Silk",
                ColorParser.generateGradient("#FFFFFF", "#43000E", "Brooding Silk"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "The queen's web is stronger than usual");
        loreList.add(ChatColor.GRAY + "A rare drop from the Broodmother");
        return createCustomItem(Material.STRING, 1, name, null, null, loreList, 49, false, null);
    }

    public static ItemStack getTridentLauncher() {
        String name = ColorParser.colorizeString("Trident Launcher",
                ColorParser.generateGradient("#5584FF", "#FFE44D", "Trident Launcher"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Right click to shoot a trident");
        loreList.add(ChatColor.GRAY + "Used in the God Trophy quest");
        loreList.add("");
        loreList.add("~Drowned Exotic~");

        ItemStack item = createCustomItem(Material.TRIDENT, 1, name, null, null, loreList, 43, false, null);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;
        meta.setUnbreakable(true);
        item.setItemMeta(meta);

        return item;
    }

    // God Quest
    public static ItemStack getTurtleHelmet() {
        String name = ColorParser.colorizeString("Turtle Helmet",
                ColorParser.generateGradient("#005616", "#03C1A5", "Turtle Helmet"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Used in the God Trophy quest");
        ItemStack helmet = createCustomItem(Material.TURTLE_HELMET, 1, name, null, null, loreList, 42, false, null);
        ItemMeta meta = helmet.getItemMeta();
        if (meta == null)
            return helmet;
        meta.setUnbreakable(true);
        helmet.setItemMeta(meta);
        return helmet;
    }

    public static ItemStack getGoatHorn() {
        String name = ColorParser.colorizeString("Goat Horn",
                ColorParser.generateGradient("#4F2300", "#404040", "Goat Horn"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Used in the God Trophy quest");
        return createCustomItem(Material.GOAT_HORN, 1, name, null, null, loreList, 42, false, null);
    }

    public static ItemStack getFirstAlbum() {
        String name = ColorParser.colorizeString("First Album",
                ColorParser.generateGradient("#2E2E2E", "#9B9B9B", "First Album"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Used in the God Trophy quest");
        return createCustomItem(Material.NOTE_BLOCK, 1, name, null, null, loreList, 42, false, null);
    }

    public static ItemStack getSecondAlbum() {
        String name = ColorParser.colorizeString("Second Album",
                ColorParser.generateGradient("#2E2E2E", "#9B9B9B", "Second Album"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Used in the God Trophy quest");
        return createCustomItem(Material.NOTE_BLOCK, 1, name, null, null, loreList, 42, false, null);
    }

    public static ItemStack getMusicKnowledgeDisc() {
        String name = ColorParser.colorizeString("Music Knowledge Disc",
                ColorParser.generateGradient("#2E2E2E", "#9B9B9B", "Music Knowledge Disc"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Used in the God Trophy quest");
        return createCustomItem(Material.MUSIC_DISC_13, 1, name, null, null, loreList, 42, false, null);
    }

    public static ItemStack getFirstSherd() {
        String name = ColorParser.colorizeString("First Sherd",
                ColorParser.generateGradient("#FBA334", "#342601", "First Sherd"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Used in the God Trophy quest");
        return createCustomItem(Material.FLOWER_POT, 1, name, null, null, loreList, 42, false, null);
    }

    public static ItemStack getSecondSherd() {
        String name = ColorParser.colorizeString("Second Sherd",
                ColorParser.generateGradient("#FBA334", "#342601", "Second Sherd"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Used in the God Trophy quest");
        return createCustomItem(Material.FLOWER_POT, 1, name, null, null, loreList, 42, false, null);
    }

    public static ItemStack getSherdRelic() {
        String name = ColorParser.colorizeString("Sherd Relic",
                ColorParser.generateGradient("#FBA334", "#342601", "Sherd Relic"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Used in the God Trophy quest");
        return createCustomItem(Material.FLOWER_POT, 1, name, null, null, loreList, 42, false, null);
    }

    public static ItemStack getFirstTrim() {
        String name = ColorParser.colorizeString("First Trim",
                ColorParser.generateGradient("#A3A3A3", "#330048", "First Trim"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Used in the God Trophy quest");
        return createCustomItem(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1, name, null, null, loreList, 42, false,
                null);
    }

    public static ItemStack getSecondTrim() {
        String name = ColorParser.colorizeString("Second Trim",
                ColorParser.generateGradient("#A3A3A3", "#330048", "Second Trim"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Used in the God Trophy quest");
        return createCustomItem(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1, name, null, null, loreList, 42, false,
                null);
    }

    public static ItemStack getTrimRelic() {
        String name = ColorParser.colorizeString("Trim Relic",
                ColorParser.generateGradient("#A3A3A3", "#330048", "Trim Relic"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Used in the God Trophy quest");
        return createCustomItem(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, 1, name, null, null, loreList, 42, false,
                null);
    }

    public static ItemStack getWarriorEmblem() {
        String name = ColorParser.colorizeString("Warrior Emblem",
                ColorParser.generateGradient("#FF0000", "#480000", "Warrior Emblem"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Used in the God Trophy quest");
        return createCustomItem(Material.ENCHANTED_BOOK, 1, name, null, null, loreList, 42, false, null);
    }

    // Miscellaneous
    public static ItemStack getTrophyItem(Material mat, String name, String lore) {
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "A Testament Of Your Progress");
        loreList.add("");
        loreList.add(lore);
        Map<Enchantment, Integer> enchants = new HashMap<>();
        enchants.put(Enchantment.KNOCKBACK, 5);
        return createCustomItem(mat, 1, name, null, null, loreList, 999, true, enchants);
    }

    public static ItemStack getFireResistancePotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta == null)
            return potion;
        meta.setBasePotionType(PotionType.FIRE_RESISTANCE);
        potion.setItemMeta(meta);
        return potion;
    }

    public static ItemStack getJumpPowerPotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta == null)
            return potion;
        meta.setBasePotionType(PotionType.LEAPING);
        potion.setItemMeta(meta);
        return potion;
    }

    public static ItemStack getSpeedPotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta == null)
            return potion;
        meta.setBasePotionType(PotionType.SWIFTNESS);
        potion.setItemMeta(meta);
        return potion;
    }

    public static ItemStack getSortOfStonePick() {
        String name = ChatColor.GRAY.toString() + ChatColor.BOLD + "Sort Of Stone Pickaxe";
        String lore = ChatColor.GRAY + "Sure";
        HashMap<Enchantment, Integer> enchants = new HashMap<>();
        enchants.put(Enchantment.EFFICIENCY, 2);
        enchants.put(Enchantment.UNBREAKING, 5);
        return createCustomItem(Material.STONE_PICKAXE, 1, name, null, lore, null, 0, false, enchants);
    }

    public static ItemStack getUnlimitedTropicalFishBucket() {
        String name = ColorParser.colorizeString("Unlimited Tropical Fish Bucket",
                ColorParser.generateGradient("#084CFB", "#ADF3FD", "Unlimited Tropical Fish Bucket"), true);
        String lore = ChatColor.GRAY + "Infinite tropical fish";
        return createCustomItem(Material.TROPICAL_FISH_BUCKET, 1, name, null, lore, null, 17, false, null);
    }

    public static ItemStack getFireworkCannon() {
        List<List<String>> colors = new ArrayList<>();
        colors.add(ColorParser.generateGradient("#FF0000", "#00FF00", 5));
        colors.add(ColorParser.generateGradient("#00FF00", "#0000FF", 5));
        colors.add(ColorParser.generateGradient("#0000FF", "#FF00FF", 5));
        String name = ColorParser.colorizeString("Firework Cannon", ColorParser.gradientConnector(colors), true);
        String lore = ChatColor.GRAY + "Shoots a firework in the direction you are looking";
        return createCustomItem(Material.CAMPFIRE, 1, name, null, lore, null, 18, false, null);
    }

    public static ItemStack getHardNautilusShell() {
        String name = ChatColor.WHITE.toString() + ChatColor.BOLD + "Hard Nautilus Shell";
        return createCustomItem(Material.NAUTILUS_SHELL, 1, name, null, null, null, 24, false, null);
    }

    public static ItemStack getHardHeartOfTheSea() {
        String name = ChatColor.YELLOW.toString() + ChatColor.BOLD + "Hard Heart Of The Sea";
        return createCustomItem(Material.HEART_OF_THE_SEA, 1, name, null, null, null, 25, false, null);
    }

    public static ItemStack getBronzeIngot() {
        String name = ChatColor.GOLD.toString() + ChatColor.BOLD + "Bronze Ingot";
        return createCustomItem(Material.COPPER_INGOT, 1, name, null, null, null, 28, false, null);
    }

    public static ItemStack getGodTrophyBase() {
        String name = ColorParser.colorizeString("God Trophy Base",
                ColorParser.generateGradient("#FFFF00", "#FFFFFF", "God Trophy Base"), true);
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Used to craft the God Trophy");
        return createCustomItem(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, 1, name, null, null, loreList, 45, false, null);
    }

    public static ItemStack getTeleportAnchor() {
        String name = ChatColor.DARK_GRAY.toString() + ChatColor.BOLD + "Teleport Anchor";
        ArrayList<String> loreList = new ArrayList<>();
        loreList.add(ChatColor.GRAY + "Place down to add to the teleporter network");
        return createCustomItem(Material.RESPAWN_ANCHOR, 1, name, null, null, loreList, 55, false, null);
    }
}
