package sir_draco.survivalskills.utils.items;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.ColorParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemStackGenerator {

    private ItemStackGenerator() {}

    // Mining Items

    public static ItemStack getUnlimitedTorch() {
        String name = ColorParser.gradientName("Unlimited Torch",
                ColorParser.rgbToHex(255, 0, 0), ColorParser.rgbToHex(80, 80, 80), true);
        return new ItemStackBuilder(Material.TORCH, 1, name)
                .lore(ChatColor.GRAY + "This torch will never run out!")
                .modelData(1)
                .build();
    }

    private static final List<String> MINING_LORE = List.of("",
            ChatColor.GRAY + "Haste II Everywhere",
            ChatColor.GRAY + "Fire Resistance and Speed when below" + ChatColor.AQUA + " Y-64",
            ChatColor.GRAY + "Must wear the full set");

    public static ItemStack getMiningHelmet() {
        return ItemStackGeneratorUtils.createLeatherArmorPiece(Material.LEATHER_HELMET,
                ChatColor.GRAY.toString() + ChatColor.BOLD + "Mining Helmet",
                MINING_LORE, 3, Color.GRAY);
    }

    public static ItemStack getMiningChestplate() {
        return ItemStackGeneratorUtils.createLeatherArmorPiece(Material.LEATHER_CHESTPLATE,
                ChatColor.GRAY.toString() + ChatColor.BOLD + "Mining Chestplate",
                MINING_LORE, 3, Color.GRAY);
    }

    public static ItemStack getMiningLeggings() {
        return ItemStackGeneratorUtils.createLeatherArmorPiece(Material.LEATHER_LEGGINGS,
                ChatColor.GRAY.toString() + ChatColor.BOLD + "Mining Leggings",
                MINING_LORE, 3, Color.GRAY);
    }

    public static ItemStack getMiningBoots() {
        return ItemStackGeneratorUtils.createLeatherArmorPiece(Material.LEATHER_BOOTS,
                ChatColor.GRAY.toString() + ChatColor.BOLD + "Mining Boots",
                MINING_LORE, 3, Color.GRAY);
    }

    public static ItemStack getZapWand() {
        return new ItemStackBuilder(Material.LIGHTNING_ROD, 1,
                ChatColor.GOLD.toString() + ChatColor.BOLD + "Zap Wand")
                .lore(ChatColor.GRAY + "Right click the ground to throw lightning")
                .modelData(27)
                .build();
    }

    // Beacon Armor

    private static final List<String> BEACON_LORE = List.of("",
            ChatColor.GRAY + "Beacon Effect Everywhere",
            ChatColor.GRAY + "Must wear the full set");

    private static String beaconName(String piece) {
        List<List<String>> colors = new ArrayList<>();
        colors.add(ColorParser.generateGradient("#FF0000", "#00FF00", "Beacon "));
        colors.add(ColorParser.generateGradient("#00FF00", "#0000FF", piece));
        return ColorParser.colorizeString("Beacon " + piece, ColorParser.gradientConnector(colors), true);
    }

    public static ItemStack getBeaconHelmet() {
        return ItemStackGeneratorUtils.createLeatherArmorPiece(Material.LEATHER_HELMET, beaconName("Helmet"),
                BEACON_LORE, 29, Color.WHITE,
                meta -> ItemStackGeneratorUtils.addBeaconModifiers(meta, "beaconHelmet", 4, EquipmentSlotGroup.HEAD));
    }

    public static ItemStack getBeaconChestplate() {
        return ItemStackGeneratorUtils.createLeatherArmorPiece(Material.LEATHER_CHESTPLATE, beaconName("Chestplate"),
                BEACON_LORE, 29, Color.WHITE,
                meta -> ItemStackGeneratorUtils.addBeaconModifiers(meta, "beaconChestplate", 9, EquipmentSlotGroup.CHEST));
    }

    public static ItemStack getBeaconLeggings() {
        return ItemStackGeneratorUtils.createLeatherArmorPiece(Material.LEATHER_LEGGINGS, beaconName("Leggings"),
                BEACON_LORE, 29, Color.WHITE,
                meta -> ItemStackGeneratorUtils.addBeaconModifiers(meta, "beaconLeggings", 7, EquipmentSlotGroup.LEGS));
    }

    public static ItemStack getBeaconBoots() {
        return ItemStackGeneratorUtils.createLeatherArmorPiece(Material.LEATHER_BOOTS, beaconName("Boots"),
                BEACON_LORE, 29, Color.WHITE,
                meta -> ItemStackGeneratorUtils.addBeaconModifiers(meta, "beaconBoots", 4, EquipmentSlotGroup.FEET));
    }

    // Power Items

    private static String powerGradient(String name) {
        return ColorParser.gradientName(name, "#8008FB", "#FD242D", true);
    }

    public static ItemStack getPowerOre() {
        Map<Enchantment, Integer> enchants = Map.of(Enchantment.KNOCKBACK, 10);
        return new ItemStackBuilder(Material.OBSIDIAN, 1, powerGradient("Power Ore"))
                .lore(List.of(ChatColor.GRAY + "The ore is teeming with energy"))
                .modelData(44)
                .hideEnchants(true)
                .enchants(enchants)
                .build();
    }

    public static ItemStack getPowerLaser() {
        return new ItemStackBuilder(Material.END_CRYSTAL, 1, powerGradient("Power Laser"))
                .lore(List.of(
                        ChatColor.GRAY + "The laser is teeming with energy",
                        ChatColor.LIGHT_PURPLE + "Right click to shoot a laser"))
                .modelData(50)
                .build();
    }

    private static final List<String> POWER_ARMOR_COLOR = List.of(
            ChatColor.GRAY + "The helmet is teeming with energy");
    private static final List<String> POWER_ARMOR_CHEST = List.of(
            ChatColor.GRAY + "The chestplate is teeming with energy",
            ChatColor.LIGHT_PURPLE + "The full armor absorbs all damage",
            ChatColor.LIGHT_PURPLE + "When it has reached its limit you will take twice as much damage",
            ChatColor.LIGHT_PURPLE + "Sneak twice in quick succession to unleash the stored power!");
    private static final List<String> POWER_ARMOR_LEGS = List.of(
            ChatColor.GRAY + "The leggings are teeming with energy");
    private static final List<String> POWER_ARMOR_BOOTS = List.of(
            ChatColor.GRAY + "The boots are teeming with energy");

    private static final Color POWER_COLOR = Color.fromRGB(73, 0, 110);

    public static ItemStack getPowerHelmet() {
        return ItemStackGeneratorUtils.createLeatherArmorPiece(Material.LEATHER_HELMET, powerGradient("Power Helmet"),
                POWER_ARMOR_COLOR, 51, POWER_COLOR,
                meta -> ItemStackGeneratorUtils.addPowerModifiers(meta, "powerHelmet", 6, EquipmentSlotGroup.HEAD));
    }

    public static ItemStack getPowerChestplate() {
        return ItemStackGeneratorUtils.createLeatherArmorPiece(Material.LEATHER_CHESTPLATE, powerGradient("Power Chestplate"),
                POWER_ARMOR_CHEST, 51, POWER_COLOR,
                meta -> ItemStackGeneratorUtils.addPowerModifiers(meta, "powerChestplate", 12, EquipmentSlotGroup.CHEST));
    }

    public static ItemStack getPowerLeggings() {
        return ItemStackGeneratorUtils.createLeatherArmorPiece(Material.LEATHER_LEGGINGS, powerGradient("Power Leggings"),
                POWER_ARMOR_LEGS, 51, POWER_COLOR,
                meta -> ItemStackGeneratorUtils.addPowerModifiers(meta, "powerLeggings", 9, EquipmentSlotGroup.LEGS));
    }

    public static ItemStack getPowerBoots() {
        return ItemStackGeneratorUtils.createLeatherArmorPiece(Material.LEATHER_BOOTS, powerGradient("Power Boots"),
                POWER_ARMOR_BOOTS, 51, POWER_COLOR,
                meta -> ItemStackGeneratorUtils.addPowerModifiers(meta, "powerBoots", 6, EquipmentSlotGroup.FEET, 0));
    }

    public static ItemStack getPowerSword() {
        ItemStack item = new ItemStackBuilder(Material.NETHERITE_SWORD, 1, powerGradient("Power Sword"))
                .lore(List.of(
                        ChatColor.GRAY + "The sword is teeming with energy",
                        ChatColor.LIGHT_PURPLE + "Right click to perform a dash attack"))
                .modelData(47)
                .build();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE,
                new AttributeModifier(ItemStackGeneratorUtils.skillsItemKey, 15,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getPowerDrill() {
        ItemStack item = new ItemStackBuilder(Material.NETHERITE_PICKAXE, 1, powerGradient("Power Drill"))
                .lore(List.of(
                        ChatColor.GRAY + "The drill is teeming with energy",
                        ChatColor.LIGHT_PURPLE + "Demolish blocks with ease"))
                .modelData(48)
                .build();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.addAttributeModifier(Attribute.BLOCK_BREAK_SPEED,
                new AttributeModifier(ItemStackGeneratorUtils.skillsItemKey, -0.75,
                        AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND));
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    // Building

    public static ItemStack getSortWand() {
        return new ItemStackBuilder(Material.BLAZE_ROD, 1,
                ChatColor.BLUE.toString() + ChatColor.BOLD + "Sort Wand")
                .lore(ChatColor.GRAY + "Left click a chest to sort its inventory")
                .modelData(16)
                .build();
    }

    // Exploring

    public static ItemStack getMagnet() {
        Map<Enchantment, Integer> enchants = Map.of(Enchantment.FORTUNE, 1);
        return new ItemStackBuilder(Material.HOPPER, 1,
                ColorParser.gradientName("Magnet", "#FF7400", "#ADF3FD", true))
                .lore(ChatColor.GRAY + "Attracts items to you when held in your hand")
                .modelData(32)
                .hideEnchants(true)
                .enchants(enchants)
                .build();
    }

    // Jumping Boots
    public static ItemStack getJumpingBoots() {
        return ItemStackGeneratorUtils.createArmorPiece(Material.IRON_BOOTS,
                ColorParser.gradientName("Jumping Boots", "#FFFFFF", "#015210", true),
                List.of("",
                        ChatColor.GRAY + "Jump Boost II Everywhere",
                        ChatColor.GRAY + "Prevents Fall Damage"),
                4);
    }

    // Wanderer Armor

    private static final List<String> WANDERER_LORE = List.of(
            ChatColor.GRAY + "Speed I Everywhere",
            ChatColor.GRAY + "Must wear the full set");

    private static String wandererName(String piece) {
        return ColorParser.gradientName("Wanderer " + piece, "#FFF000", "#FFDC6E", true);
    }

    public static ItemStack getWandererBoots() {
        return ItemStackGeneratorUtils.createArmorPiece(Material.CHAINMAIL_BOOTS, wandererName("Boots"), WANDERER_LORE, 5);
    }

    public static ItemStack getWandererLeggings() {
        return ItemStackGeneratorUtils.createArmorPiece(Material.CHAINMAIL_LEGGINGS, wandererName("Leggings"), WANDERER_LORE, 5);
    }

    public static ItemStack getWandererChestplate() {
        return ItemStackGeneratorUtils.createArmorPiece(Material.CHAINMAIL_CHESTPLATE, wandererName("Chestplate"), WANDERER_LORE, 5);
    }

    public static ItemStack getWandererHelmet() {
        return ItemStackGeneratorUtils.createArmorPiece(Material.CHAINMAIL_HELMET, wandererName("Helmet"), WANDERER_LORE, 5);
    }

    // Traveler Armor

    private static final List<String> TRAVELER_LORE = List.of(
            ChatColor.GRAY + "Speed II Everywhere",
            ChatColor.GRAY + "Must wear the full set");

    private static String travelerName(String piece) {
        return ColorParser.gradientName("Traveler " + piece, "#FF8800", "#FFDC6E", true);
    }

    public static ItemStack getTravelerBoots() {
        return ItemStackGeneratorUtils.createArmorPiece(Material.DIAMOND_BOOTS, travelerName("Boots"), TRAVELER_LORE, 7);
    }

    public static ItemStack getTravelerLeggings() {
        return ItemStackGeneratorUtils.createArmorPiece(Material.DIAMOND_LEGGINGS, travelerName("Leggings"), TRAVELER_LORE, 7);
    }

    public static ItemStack getTravelerChestplate() {
        return ItemStackGeneratorUtils.createArmorPiece(Material.DIAMOND_CHESTPLATE, travelerName("Chestplate"), TRAVELER_LORE, 7);
    }

    public static ItemStack getTravelerHelmet() {
        return ItemStackGeneratorUtils.createArmorPiece(Material.DIAMOND_HELMET, travelerName("Helmet"), TRAVELER_LORE, 7);
    }

    // Adventurer Armor

    private static final List<String> ADVENTURER_LORE = List.of("",
            ChatColor.GRAY + "Speed III Everywhere",
            ChatColor.GRAY + "Prevents Fall Damage",
            ChatColor.GRAY + "Must wear the full set");

    private static String adventurerName(String piece) {
        return ColorParser.gradientName("Adventurer " + piece, "#FF0000", "#FFDC6E", true);
    }

    public static ItemStack getAdventurerBoots() {
        return ItemStackGeneratorUtils.createArmorPiece(Material.NETHERITE_BOOTS, adventurerName("Boots"), ADVENTURER_LORE, 8);
    }

    public static ItemStack getAdventurerLeggings() {
        return ItemStackGeneratorUtils.createArmorPiece(Material.NETHERITE_LEGGINGS, adventurerName("Leggings"), ADVENTURER_LORE, 8);
    }

    public static ItemStack getAdventurerChestplate() {
        return ItemStackGeneratorUtils.createArmorPiece(Material.NETHERITE_CHESTPLATE, adventurerName("Chestplate"), ADVENTURER_LORE, 8);
    }

    public static ItemStack getAdventurerHelmet() {
        return ItemStackGeneratorUtils.createArmorPiece(Material.NETHERITE_HELMET, adventurerName("Helmet"), ADVENTURER_LORE, 8);
    }

    // Other Exploring Items

    public static ItemStack getCaveFinder() {
        return new ItemStackBuilder(Material.COMPASS, 1,
                ColorParser.gradientName("Cave Finder", "#FFFFFF", "#000000", true))
                .lore(List.of("",
                        ChatColor.GRAY + "Right Click to find the nearest cave",
                        ChatColor.GRAY + "Tells you if it is a confirmed cave or a potential dark spot"))
                .modelData(6)
                .build();
    }

    // Gill Armor

    private static final List<String> GILL_LORE = List.of("",
            ChatColor.GRAY + "Water Breathing Everywhere",
            ChatColor.GRAY + "Lightning Fast Swim Speed",
            ChatColor.GRAY + "Must wear the full set");

    private static String gillName(String piece) {
        return ColorParser.gradientName("Gill " + piece, "#86A8FF", "#ADF3FD", true);
    }

    public static ItemStack getGillHelmet() {
        return ItemStackGeneratorUtils.createLeatherArmorPiece(Material.LEATHER_HELMET, gillName("Helmet"), GILL_LORE, 19, Color.BLUE);
    }

    public static ItemStack getGillChestplate() {
        return ItemStackGeneratorUtils.createLeatherArmorPiece(Material.LEATHER_CHESTPLATE, gillName("Chestplate"), GILL_LORE, 19, Color.BLUE);
    }

    public static ItemStack getGillLeggings() {
        return ItemStackGeneratorUtils.createLeatherArmorPiece(Material.LEATHER_LEGGINGS, gillName("Leggings"), GILL_LORE, 19, Color.BLUE);
    }

    public static ItemStack getGillBoots() {
        return ItemStackGeneratorUtils.createLeatherArmorPiece(Material.LEATHER_BOOTS, gillName("Boots"), GILL_LORE, 19, Color.BLUE);
    }

    // Farming

    public static ItemStack getWateringCan() {
        return new ItemStackBuilder(Material.PRISMARINE_SHARD, 1,
                ColorParser.gradientName("Watering Can", "#00FFFF", "#000000", true))
                .lore(ChatColor.GRAY + "Right Click to water crops in a 5x5 area")
                .modelData(9)
                .build();
    }

    public static ItemStack getUnlimitedBoneMeal() {
        return new ItemStackBuilder(Material.BONE_MEAL, 1,
                ColorParser.gradientName("Unlimited Bonemeal", "#000000", "#515151", true))
                .lore(ChatColor.GRAY + "Infinite crop growth")
                .modelData(10)
                .build();
    }

    public static ItemStack getHarvester() {
        ItemStack item = new ItemStackBuilder(Material.NETHERITE_HOE, 1,
                ColorParser.gradientName("Harvester", "#00FF00", "#000000", true))
                .lore(List.of("",
                        ChatColor.GRAY + "Breaks and replants crops for you!",
                        ChatColor.GRAY + "Cooldown of 3 seconds"))
                .modelData(11)
                .build();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    // Boss Items

    public static ItemStack getGiantBossItem() {
        return new ItemStackBuilder(Material.ZOMBIE_HEAD, 1,
                ColorParser.gradientName("Giant Head", ColorParser.rgbToHex(0, 255, 0), ColorParser.rgbToHex(80, 80, 80), true))
                .lore(ChatColor.GRAY + "I wonder how big his...")
                .modelData(2)
                .build();
    }

    public static ItemStack getFishingBossItem() {
        return new ItemStackBuilder(Material.PRISMARINE_SHARD, 1,
                ColorParser.gradientName("Sea King Scale", "#00FFFF", "#0000FF", true))
                .lore(ChatColor.GRAY + "Your fishing perseverance was noticed by the king")
                .modelData(2)
                .build();
    }

    public static ItemStack getBroodMotherBossItem() {
        return new ItemStackBuilder(Material.COBWEB, 1,
                ColorParser.gradientName("BroodMother Web", "#FFFFFF", "#43000E", true))
                .lore(ChatColor.GRAY + "The Queen recognized your greatness as she fell")
                .modelData(2)
                .build();
    }

    public static ItemStack getElderGuardianBossItem() {
        return new ItemStackBuilder(Material.ENDER_EYE, 1,
                ColorParser.gradientName("Guardian Eye", "#001EFF", "#E900FF", true))
                .lore(ChatColor.GRAY + "It seems like it is still watching you")
                .modelData(2)
                .build();
    }

    public static ItemStack getWardenBossItem() {
        return new ItemStackBuilder(Material.ECHO_SHARD, 1,
                ColorParser.gradientName("Warden Heart", "#323232", "#C776FF", true))
                .lore(ChatColor.GRAY + "You faintly hear the shrieks of ancient times")
                .modelData(2)
                .build();
    }

    public static ItemStack getVillagerBossItem() {
        return new ItemStackBuilder(Material.PLAYER_HEAD, 1,
                ColorParser.gradientName("Minecraft Essence", "#2C1F0C", "#005306", true))
                .lore(ChatColor.GRAY + "The villager gained access to the game code itself")
                .modelData(2)
                .build();
    }

    public static ItemStack getEnderDragonBossItem() {
        return new ItemStackBuilder(Material.DRAGON_HEAD, 1,
                ColorParser.gradientName("Dragon Head", "#8C00FF", "#180152", true))
                .lore(ChatColor.GRAY + "The ender dragon guarded the void from invaders")
                .modelData(2)
                .build();
    }

    // Summoner Items

    public static ItemStack getGiantSummoner() {
        return new ItemStackBuilder(Material.ZOMBIE_SPAWN_EGG, 1,
                ColorParser.gradientName("Giant Summoner Egg", ColorParser.rgbToHex(0, 255, 0), ColorParser.rgbToHex(80, 80, 80), true))
                .lore(ChatColor.GRAY + "Must spawn at night")
                .modelData(12)
                .build();
    }

    public static ItemStack getBroodMotherSummoner() {
        return new ItemStackBuilder(Material.SPIDER_SPAWN_EGG, 1,
                ColorParser.gradientName("BroodMother Summoner Egg", "#FFFFFF", "#43000E", true))
                .lore(ChatColor.GRAY + "Make sure you are prepared")
                .modelData(13)
                .build();
    }

    public static ItemStack getVillagerSummoner() {
        return new ItemStackBuilder(Material.VILLAGER_SPAWN_EGG, 1,
                ColorParser.gradientName("Villager Summoner Egg", "#2C1F0C", "#005306", true))
                .lore(List.of("",
                        ChatColor.GRAY + "The true final boss of Minecraft",
                        ChatColor.RED + "This boss is very difficult and will require many attempts!",
                        ChatColor.RED.toString() + ChatColor.BOLD + "THIS IS A DESTRUCTIVE FIGHT!!!"))
                .modelData(14)
                .build();
    }

    // Fishing Exotics

    public static ItemStack getUnlimitedWaterBucket() {
        return new ItemStackBuilder(Material.WATER_BUCKET, 1,
                ColorParser.gradientName("Unlimited Water Bucket", "#08C8FB", "#1D5E67", true))
                .lore(ChatColor.GRAY + "Infinite water")
                .modelData(20)
                .build();
    }

    public static ItemStack getUnlimitedLavaBucket() {
        return new ItemStackBuilder(Material.LAVA_BUCKET, 1,
                ColorParser.gradientName("Unlimited Lava Bucket", "#FB5908", "#DA0A0A", true))
                .lore(ChatColor.GRAY + "Infinite lava")
                .modelData(21)
                .build();
    }

    public static ItemStack getWeatherArtifact() {
        return new ItemStackBuilder(Material.BREEZE_ROD, 1,
                ColorParser.gradientName("Weather Artifact", "#FFF000", "#2E3435", true))
                .lore(List.of(
                        ChatColor.GRAY + "Right click to change the weather",
                        ChatColor.GRAY + "1 Hour Cooldown",
                        ChatColor.GRAY + "Last Used: Never"))
                .modelData(22)
                .build();
    }

    public static ItemStack getTimeArtifact() {
        return new ItemStackBuilder(Material.CLOCK, 1,
                ColorParser.gradientName("Time Artifact", "#FFFFFF", "#000000", true))
                .lore(List.of(
                        ChatColor.GRAY + "Right click to change the time",
                        ChatColor.GRAY + "1 Hour Cooldown",
                        ChatColor.GRAY + "Last Used: Never"))
                .modelData(23)
                .build();
    }

    public static ItemStack getExperienceMultiplierVoucher(int multiplier, int duration) {
        ItemStack item = new ItemStackBuilder(Material.PAPER, 1,
                ColorParser.colorizeString("XP Voucher",
                        ColorParser.generateGradient("#FFC600", "#FFF080", multiplier + "x XP Voucher"), true))
                .lore(List.of(ChatColor.GRAY + "Multiplies XP gained for " + duration + " minutes"))
                .modelData(26)
                .build();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.getPersistentDataContainer().set(
                new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class), "xpMultiplier"),
                PersistentDataType.INTEGER, multiplier);
        meta.getPersistentDataContainer().set(
                new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class), "xpDuration"),
                PersistentDataType.INTEGER, duration);

        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getUnlimitedEmptyBucket() {
        return new ItemStackBuilder(Material.BUCKET, 1,
                ColorParser.gradientName("Unlimited Empty Bucket", "#FFFFFF", "#777777", true))
                .lore(ChatColor.GRAY + "Infinite empty bucket")
                .modelData(30)
                .build();
    }

    public static ItemStack getUnlimitedRocket() {
        return new ItemStackBuilder(Material.FIREWORK_ROCKET, 1,
                ColorParser.gradientName("Unlimited Rocket", "#FF0000", "#FFFFFF", true))
                .lore(ChatColor.GRAY + "Infinite rocket")
                .modelData(31)
                .build();
    }

    public static ItemStack getUnlimitedTropicalFishBucket() {
        return new ItemStackBuilder(Material.TROPICAL_FISH_BUCKET, 1,
                ColorParser.gradientName("Unlimited Tropical Fish Bucket", "#084CFB", "#ADF3FD", true))
                .lore(ChatColor.GRAY + "Infinite tropical fish")
                .modelData(17)
                .build();
    }

    public static ItemStack getFireworkCannon() {
        List<List<String>> colors = new ArrayList<>();
        colors.add(ColorParser.generateGradient("#FF0000", "#00FF00", 5));
        colors.add(ColorParser.generateGradient("#00FF00", "#0000FF", 5));
        colors.add(ColorParser.generateGradient("#0000FF", "#FF00FF", 5));
        String name = ColorParser.colorizeString("Firework Cannon", ColorParser.gradientConnector(colors), true);
        return new ItemStackBuilder(Material.CAMPFIRE, 1, name)
                .lore(ChatColor.GRAY + "Shoots a firework in the direction you are looking")
                .modelData(18)
                .build();
    }

    // Unique Mob Drops

    public static ItemStack getWebShooter() {
        return new ItemStackBuilder(Material.COBWEB, 1,
                ColorParser.gradientName("Web Shooter", "#FFFFFF", "#2E3526", true))
                .lore(List.of(
                        ChatColor.GRAY + "Right click to shoot a web",
                        ChatColor.GRAY + "Used in the God Trophy quest",
                        "",
                        "~Spider Exotic~"))
                .modelData(33)
                .build();
    }

    public static ItemStack getUnlimitedTippedArrow() {
        return new ItemStackBuilder(Material.TIPPED_ARROW, 1,
                ColorParser.gradientName("Unlimited Tipped Arrow", "#FB08F2", "#ADF3FD", true))
                .lore(List.of(
                        ChatColor.GRAY + "A different tipped arrow is used every time",
                        ChatColor.GRAY + "Used in the God Trophy quest",
                        "",
                        "~Skeleton Exotic~"))
                .modelData(34)
                .build();
    }

    public static ItemStack getVillagerRevivalArtifact() {
        return new ItemStackBuilder(Material.EMERALD, 1,
                ColorParser.gradientName("Villager Revival Artifact", "#83FB08", "#FCFF7A", true))
                .lore(List.of(
                        ChatColor.GRAY + "Right click a zombie villager to revive it",
                        ChatColor.GRAY + "Used in the God Trophy quest",
                        "",
                        "~Zombie Exotic~"))
                .modelData(35)
                .build();
    }

    public static ItemStack getEnderEssence() {
        return new ItemStackBuilder(Material.ENDER_PEARL, 1,
                ColorParser.gradientName("Ender Essence", "#29006E", "#C54FFF", true))
                .lore(List.of(
                        ChatColor.GRAY + "Right click to teleport 5 blocks ahead",
                        ChatColor.GRAY + "Used in the God Trophy quest",
                        "",
                        "~Enderman Exotic~"))
                .modelData(36)
                .build();
    }

    public static ItemStack getCreeperEssence() {
        return new ItemStackBuilder(Material.GUNPOWDER, 1,
                ColorParser.gradientName("Creeper Essence", "#199D00", "#FFFFFF", true))
                .lore(List.of(
                        ChatColor.GRAY + "Right click to explode",
                        ChatColor.GRAY + "Used in the God Trophy quest",
                        "",
                        "~Creeper Exotic~"))
                .modelData(37)
                .build();
    }

    public static ItemStack getPotionBag(int id) {
        ItemStack item = new ItemStackBuilder(Material.CHEST, 1,
                ColorParser.gradientName("Potion Bag", "#FFFFFF", "#000000", true))
                .lore(List.of(
                        ChatColor.GRAY + "Right click to open",
                        "~Witch Exotic~"))
                .modelData(38)
                .build();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.getPersistentDataContainer().set(
                new NamespacedKey(SurvivalSkills.getPlugin(SurvivalSkills.class), "potion_bag"),
                PersistentDataType.INTEGER, id);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getNewPotionBag() {
        return getPotionBag(ItemStackGeneratorUtils.nextPotionBagId());
    }

    public static ItemStack getMagicBagOfWind() {
        return new ItemStackBuilder(Material.WIND_CHARGE, 1,
                ColorParser.gradientName("Magic Bag O' Wind", "#08FB75", "#ADF3FD", true))
                .lore(List.of(
                        ChatColor.GRAY + "Right click to launch yourself in the air",
                        ChatColor.GRAY + "Used in the God Trophy quest",
                        "",
                        "~Breeze Exotic~"))
                .modelData(39)
                .build();
    }

    public static ItemStack getDragonBreathCannon() {
        return new ItemStackBuilder(Material.DRAGON_BREATH, 1,
                ColorParser.gradientName("Dragon Breath Cannon", "#5608FB", "#343434", true))
                .lore(List.of(
                        ChatColor.GRAY + "Right click to shoot dragon breath",
                        ChatColor.GRAY + "Used in the God Trophy quest",
                        "",
                        "~Ender Dragon Exotic~"))
                .modelData(40)
                .build();
    }

    public static ItemStack getUnlimitedWitherRose() {
        return new ItemStackBuilder(Material.WITHER_ROSE, 1,
                ColorParser.gradientName("Unlimited Wither Rose", "#000000", "#515151", true))
                .lore(List.of(
                        ChatColor.GRAY + "Infinite wither rose",
                        ChatColor.GRAY + "Used in the God Trophy quest",
                        "",
                        "~Wither Skeleton Exotic~"))
                .modelData(41)
                .build();
    }

    public static ItemStack getUnlimitedSponge() {
        return new ItemStackBuilder(Material.SPONGE, 1,
                ColorParser.gradientName("Unlimited Sponge", "#ADFDDD", "#F8FB08", true))
                .lore(ChatColor.GRAY + "Soak up the world!")
                .modelData(46)
                .build();
    }

    public static ItemStack getBroodingSilk() {
        return new ItemStackBuilder(Material.STRING, 1,
                ColorParser.gradientName("Brooding Silk", "#FFFFFF", "#43000E", true))
                .lore(List.of(
                        ChatColor.GRAY + "The queen's web is stronger than usual",
                        ChatColor.GRAY + "A rare drop from the Broodmother"))
                .modelData(49)
                .build();
    }

    public static ItemStack getTridentLauncher() {
        ItemStack item = new ItemStackBuilder(Material.TRIDENT, 1,
                ColorParser.gradientName("Trident Launcher", "#5584FF", "#FFE44D", true))
                .lore(List.of(
                        ChatColor.GRAY + "Right click to shoot a trident",
                        ChatColor.GRAY + "Used in the God Trophy quest",
                        "",
                        "~Drowned Exotic~"))
                .modelData(43)
                .build();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    // ──────────────────────────────────────────────
    // God Quest Items
    // ──────────────────────────────────────────────

    public static ItemStack getTurtleHelmet() {
        ItemStack item = new ItemStackBuilder(Material.TURTLE_HELMET, 1,
                ColorParser.gradientName("Turtle Helmet", "#005616", "#03C1A5", true))
                .lore(List.of(ChatColor.GRAY + "Used in the God Trophy quest"))
                .modelData(42)
                .build();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getGoatHorn() {
        return ItemStackGeneratorUtils.createGodQuestItem(Material.GOAT_HORN, "Goat Horn", "#4F2300", "#404040");
    }

    public static ItemStack getFirstAlbum() {
        return ItemStackGeneratorUtils.createGodQuestItem(Material.NOTE_BLOCK, "First Album", "#2E2E2E", "#9B9B9B");
    }

    public static ItemStack getSecondAlbum() {
        return ItemStackGeneratorUtils.createGodQuestItem(Material.NOTE_BLOCK, "Second Album", "#2E2E2E", "#9B9B9B");
    }

    public static ItemStack getMusicKnowledgeDisc() {
        return ItemStackGeneratorUtils.createGodQuestItem(Material.MUSIC_DISC_13, "Music Knowledge Disc", "#2E2E2E", "#9B9B9B");
    }

    public static ItemStack getFirstSherd() {
        return ItemStackGeneratorUtils.createGodQuestItem(Material.FLOWER_POT, "First Sherd", "#FBA334", "#342601");
    }

    public static ItemStack getSecondSherd() {
        return ItemStackGeneratorUtils.createGodQuestItem(Material.FLOWER_POT, "Second Sherd", "#FBA334", "#342601");
    }

    public static ItemStack getSherdRelic() {
        return ItemStackGeneratorUtils.createGodQuestItem(Material.FLOWER_POT, "Sherd Relic", "#FBA334", "#342601");
    }

    public static ItemStack getFirstTrim() {
        return ItemStackGeneratorUtils.createGodQuestItem(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, "First Trim", "#A3A3A3", "#330048");
    }

    public static ItemStack getSecondTrim() {
        return ItemStackGeneratorUtils.createGodQuestItem(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, "Second Trim", "#A3A3A3", "#330048");
    }

    public static ItemStack getTrimRelic() {
        return ItemStackGeneratorUtils.createGodQuestItem(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE, "Trim Relic", "#A3A3A3", "#330048");
    }

    public static ItemStack getWarriorEmblem() {
        return ItemStackGeneratorUtils.createGodQuestItem(Material.ENCHANTED_BOOK, "Warrior Emblem", "#FF0000", "#480000");
    }

    // Miscellaneous

    public static ItemStack getTrophyItem(Material mat, String name, String lore) {
        Map<Enchantment, Integer> enchants = Map.of(Enchantment.KNOCKBACK, 5);
        ItemStack item = new ItemStackBuilder(mat, 1, name)
                .lore(List.of(
                        ChatColor.GRAY + "A Testament Of Your Progress",
                        "",
                        lore))
                .modelData(999)
                .hideEnchants(true)
                .enchants(enchants)
                .build();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(ItemStackGeneratorUtils.trophyItemKey,
                    PersistentDataType.STRING, "TrophyItem");
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack getFireResistancePotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta == null) return potion;
        meta.setBasePotionType(PotionType.FIRE_RESISTANCE);
        potion.setItemMeta(meta);
        return potion;
    }

    public static ItemStack getJumpPowerPotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta == null) return potion;
        meta.setBasePotionType(PotionType.LEAPING);
        potion.setItemMeta(meta);
        return potion;
    }

    public static ItemStack getSpeedPotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta == null) return potion;
        meta.setBasePotionType(PotionType.SWIFTNESS);
        potion.setItemMeta(meta);
        return potion;
    }

    public static ItemStack getHardNautilusShell() {
        return new ItemStackBuilder(Material.NAUTILUS_SHELL, 1,
                ChatColor.WHITE.toString() + ChatColor.BOLD + "Hard Nautilus Shell")
                .modelData(24)
                .build();
    }

    public static ItemStack getHardHeartOfTheSea() {
        return new ItemStackBuilder(Material.HEART_OF_THE_SEA, 1,
                ChatColor.YELLOW.toString() + ChatColor.BOLD + "Hard Heart Of The Sea")
                .modelData(25)
                .build();
    }

    public static ItemStack getBronzeIngot() {
        return new ItemStackBuilder(Material.COPPER_INGOT, 1,
                ChatColor.GOLD.toString() + ChatColor.BOLD + "Bronze Ingot")
                .modelData(28)
                .build();
    }

    public static ItemStack getGodTrophyBase() {
        return new ItemStackBuilder(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, 1,
                ColorParser.gradientName("God Trophy Base", "#FFFF00", "#FFFFFF", true))
                .lore(List.of(ChatColor.GRAY + "Used to craft the God Trophy"))
                .modelData(45)
                .build();
    }

    public static ItemStack getTeleportAnchor() {
        return new ItemStackBuilder(Material.RESPAWN_ANCHOR, 1,
                ChatColor.DARK_GRAY.toString() + ChatColor.BOLD + "Teleport Anchor")
                .lore(List.of(ChatColor.GRAY + "Place down to add to the teleporter network"))
                .modelData(55)
                .build();
    }
}
