package sir_draco.survivalskills;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeMaker {

    /**
     * Creates recipes for all the trophies
     */
    public static void trophyRecipes(SurvivalSkills plugin) {
        // Create namespace keys for the recipes and ensure old ones are removed to be updated
        NamespacedKey caveKey = new NamespacedKey(plugin, "cave");
        NamespacedKey forestKey = new NamespacedKey(plugin, "forest");
        NamespacedKey farmingKey = new NamespacedKey(plugin, "farming");
        NamespacedKey oceanKey = new NamespacedKey(plugin, "ocean");
        NamespacedKey fishingKey = new NamespacedKey(plugin, "fishing");
        NamespacedKey blackKey = new NamespacedKey(plugin, "black");
        NamespacedKey whiteKey = new NamespacedKey(plugin, "white");
        NamespacedKey colorKey = new NamespacedKey(plugin, "color");
        NamespacedKey netherKey = new NamespacedKey(plugin, "nether");
        NamespacedKey endKey = new NamespacedKey(plugin, "end");
        NamespacedKey championKey = new NamespacedKey(plugin, "champion");

        ArrayList<NamespacedKey> recipeKeys = plugin.getRecipeKeys();
        HashMap<Integer, ItemStack> trophyItems = plugin.getTrophyManager().getTrophyItems();

        recipeKeys.add(caveKey);
        recipeKeys.add(forestKey);
        recipeKeys.add(farmingKey);
        recipeKeys.add(oceanKey);
        recipeKeys.add(fishingKey);
        recipeKeys.add(blackKey);
        recipeKeys.add(whiteKey);
        recipeKeys.add(colorKey);
        recipeKeys.add(netherKey);
        recipeKeys.add(endKey);
        recipeKeys.add(championKey);

        denseWoolRecipes(plugin);

        if (plugin.getServer().getRecipe(caveKey) != null) plugin.getServer().removeRecipe(caveKey);
        if (plugin.getServer().getRecipe(forestKey) != null) plugin.getServer().removeRecipe(forestKey);
        if (plugin.getServer().getRecipe(farmingKey) != null) plugin.getServer().removeRecipe(farmingKey);
        if (plugin.getServer().getRecipe(oceanKey) != null) plugin.getServer().removeRecipe(oceanKey);
        if (plugin.getServer().getRecipe(fishingKey) != null) plugin.getServer().removeRecipe(fishingKey);
        if (plugin.getServer().getRecipe(blackKey) != null) plugin.getServer().removeRecipe(blackKey);
        if (plugin.getServer().getRecipe(whiteKey) != null) plugin.getServer().removeRecipe(whiteKey);
        if (plugin.getServer().getRecipe(colorKey) != null) plugin.getServer().removeRecipe(colorKey);
        if (plugin.getServer().getRecipe(netherKey) != null) plugin.getServer().removeRecipe(netherKey);
        if (plugin.getServer().getRecipe(endKey) != null) plugin.getServer().removeRecipe(endKey);
        if (plugin.getServer().getRecipe(championKey) != null) plugin.getServer().removeRecipe(championKey);

        // Cave Trophy
        String name1 = ChatColor.GRAY + ChatColor.BOLD.toString() + "Cave Trophy";
        String lore1 = ChatColor.GRAY + "~You have made the caves your domain~";
        ItemStack caveTrophy = ItemStackGenerator.getTrophyItem(Material.DIAMOND_PICKAXE, name1, lore1);
        trophyItems.put(1, caveTrophy);
        ShapelessRecipe caveRecipe = new ShapelessRecipe(caveKey, caveTrophy);
        caveRecipe.addIngredient(Material.COAL_BLOCK);
        caveRecipe.addIngredient(Material.IRON_BLOCK);
        caveRecipe.addIngredient(Material.REDSTONE_BLOCK);
        caveRecipe.addIngredient(Material.GOLD_BLOCK);
        caveRecipe.addIngredient(Material.DIAMOND_BLOCK);
        caveRecipe.addIngredient(Material.LAPIS_BLOCK);
        caveRecipe.addIngredient(Material.AMETHYST_BLOCK);
        caveRecipe.addIngredient(Material.EMERALD_BLOCK);
        caveRecipe.addIngredient(Material.COPPER_BLOCK);
        plugin.getServer().addRecipe(caveRecipe);

        // Forest Trophy
        String name2 = ChatColor.GREEN + ChatColor.BOLD.toString() + "Forest Trophy";
        String lore2 = ChatColor.DARK_GREEN + "~You have made the forests your domain~";
        ItemStack forestTrophy = ItemStackGenerator.getTrophyItem(Material.OAK_SAPLING, name2, lore2);
        trophyItems.put(2, forestTrophy);
        ShapelessRecipe forestRecipe = new ShapelessRecipe(forestKey, forestTrophy);
        forestRecipe.addIngredient(Material.OAK_LOG);
        forestRecipe.addIngredient(Material.BIRCH_LOG);
        forestRecipe.addIngredient(Material.ACACIA_LOG);
        forestRecipe.addIngredient(Material.CHERRY_LOG);
        forestRecipe.addIngredient(Material.SPRUCE_LOG);
        forestRecipe.addIngredient(Material.MANGROVE_LOG);
        forestRecipe.addIngredient(Material.DARK_OAK_LOG);
        forestRecipe.addIngredient(Material.JUNGLE_LOG);
        forestRecipe.addIngredient(Material.MUSHROOM_STEW);
        plugin.getServer().addRecipe(forestRecipe);

        // Farming Trophy
        String name3 = ChatColor.GOLD + ChatColor.BOLD.toString() + "Farming Trophy";
        String lore3 = ChatColor.YELLOW + "~Crops live and die based on your will~";
        ItemStack farmingTrophy = ItemStackGenerator.getTrophyItem(Material.GOLDEN_CARROT, name3, lore3);
        trophyItems.put(3, farmingTrophy);
        ShapelessRecipe farmingRecipe = new ShapelessRecipe(farmingKey, farmingTrophy);
        farmingRecipe.addIngredient(Material.POTATO);
        farmingRecipe.addIngredient(Material.PUMPKIN_PIE);
        farmingRecipe.addIngredient(Material.SWEET_BERRIES);
        farmingRecipe.addIngredient(Material.CARROT);
        farmingRecipe.addIngredient(Material.BEETROOT);
        farmingRecipe.addIngredient(Material.BREAD);
        farmingRecipe.addIngredient(Material.COOKIE);
        farmingRecipe.addIngredient(Material.MELON_SLICE);
        farmingRecipe.addIngredient(Material.CAKE);
        plugin.getServer().addRecipe(farmingRecipe);

        // Ocean Trophy
        String name4 = ChatColor.DARK_BLUE + ChatColor.BOLD.toString() + "Ocean Trophy";
        String lore4 = ChatColor.BLUE + "~The waves are pulled towards you as you walk by~";
        ItemStack oceanTrophy = ItemStackGenerator.getTrophyItem(Material.TRIDENT, name4, lore4);
        trophyItems.put(4, oceanTrophy);
        ShapelessRecipe oceanRecipe = new ShapelessRecipe(oceanKey, oceanTrophy);
        oceanRecipe.addIngredient(Material.DRIED_KELP_BLOCK);
        oceanRecipe.addIngredient(Material.HEART_OF_THE_SEA);
        oceanRecipe.addIngredient(Material.FIRE_CORAL_BLOCK);
        oceanRecipe.addIngredient(Material.SEA_LANTERN);
        oceanRecipe.addIngredient(Material.TURTLE_EGG);
        oceanRecipe.addIngredient(Material.SPONGE);
        oceanRecipe.addIngredient(Material.PUFFERFISH_BUCKET);
        oceanRecipe.addIngredient(Material.TRIDENT);
        oceanRecipe.addIngredient(Material.SEA_PICKLE);
        plugin.getServer().addRecipe(oceanRecipe);

        // Fishing Recipe
        String name5 = ChatColor.DARK_AQUA + ChatColor.BOLD.toString() + "Fishing Trophy";
        String lore5 = ChatColor.AQUA + "~Fish swim towards your hook out of respect~";
        ItemStack fishingTrophy = ItemStackGenerator.getTrophyItem(Material.FISHING_ROD, name5, lore5);
        trophyItems.put(5, fishingTrophy);
        ShapedRecipe fishingRecipe = new ShapedRecipe(fishingKey, fishingTrophy);
        fishingRecipe.shape("ABC", "DEF", "GHI");
        fishingRecipe.setIngredient('A', Material.INK_SAC);
        fishingRecipe.setIngredient('B', Material.COD);
        fishingRecipe.setIngredient('C', Material.NAUTILUS_SHELL);
        fishingRecipe.setIngredient('D', Material.PUFFERFISH);
        fishingRecipe.setIngredient('E', Material.FISHING_ROD);
        fishingRecipe.setIngredient('F', Material.SALMON);
        fishingRecipe.setIngredient('G', Material.TROPICAL_FISH);
        fishingRecipe.setIngredient('H', Material.LILY_PAD);
        // Fishing Boss
        fishingRecipe.setIngredient('I', new RecipeChoice.ExactChoice(ItemStackGenerator.getFishingBossItem()));
        plugin.getServer().addRecipe(fishingRecipe);

        // Color Recipe
        Map<Enchantment, Integer> enchants = new HashMap<>();
        enchants.put(Enchantment.KNOCKBACK, 1);
        // Black
        String name6 = ChatColor.BLACK + ChatColor.BOLD.toString() + "Black Fragment";
        String lore6 = ChatColor.DARK_GRAY + "~You merely adopted the dark, I was born in it~";
        ItemStack blackFragment = ItemStackGenerator.createCustomItem(Material.BLACK_WOOL, 1, name6, ChatColor.DARK_GRAY, lore6, null, 15, true, enchants);
        ShapedRecipe blackRecipe = new ShapedRecipe(blackKey, blackFragment);
        blackRecipe.shape("ABC", "DEF", "G  ");
        blackRecipe.setIngredient('A', new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.LIGHT_GRAY_WOOL, 1, "Bundle Of Dense Light Gray Wool", ChatColor.GRAY, "The sheep are naked", null, 15, true, enchants)));
        blackRecipe.setIngredient('B', new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.GRAY_WOOL, 1, "Bundle Of Dense Gray Wool", ChatColor.DARK_GRAY, "The sheep are naked", null, 15, true, enchants)));
        blackRecipe.setIngredient('C', new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.BLACK_WOOL, 1, "Bundle Of Dense Black Wool", ChatColor.BLACK, "The sheep are naked", null, 15, true, enchants)));
        blackRecipe.setIngredient('D', new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.BROWN_WOOL, 1, "Bundle Of Dense Brown Wool", ChatColor.getByChar("#6E2C00"), "The sheep are naked", null, 15, true, enchants)));
        blackRecipe.setIngredient('E', new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.RED_WOOL, 1, "Bundle Of Dense Red Wool", ChatColor.RED, "The sheep are naked", null, 15, true, enchants)));
        blackRecipe.setIngredient('F', new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.ORANGE_WOOL, 1, "Bundle Of Dense Orange Wool", ChatColor.getByChar("#FF8C00"), "The sheep are naked", null, 15, true, enchants)));
        blackRecipe.setIngredient('G', new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.YELLOW_WOOL, 1, "Bundle Of Dense Yellow Wool", ChatColor.YELLOW, "The sheep are naked", null, 15, true, enchants)));
        plugin.getServer().addRecipe(blackRecipe);

        // White
        String name7 = ChatColor.WHITE + ChatColor.BOLD.toString() + "White Fragment";
        String lore7 = ChatColor.WHITE + "~All colors become one~";
        ItemStack whiteFragment = ItemStackGenerator.createCustomItem(Material.WHITE_WOOL, 1, name7, ChatColor.WHITE, lore7, null, 15, true, enchants);
        ShapedRecipe whiteRecipe = new ShapedRecipe(whiteKey, whiteFragment);
        whiteRecipe.shape("ABC", "DEF", "GHI");
        whiteRecipe.setIngredient('A', new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.PINK_WOOL, 1, "Bundle Of Dense Pink Wool", ChatColor.getByChar("#FF00A2"), "The sheep are naked", null, 15, true, enchants)));
        whiteRecipe.setIngredient('B', new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.MAGENTA_WOOL, 1, "Bundle Of Dense Magenta Wool", ChatColor.LIGHT_PURPLE, "The sheep are naked", null, 15, true, enchants)));
        whiteRecipe.setIngredient('C', new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.PURPLE_WOOL, 1, "Bundle Of Dense Purple Wool", ChatColor.DARK_PURPLE, "The sheep are naked", null , 15, true, enchants)));
        whiteRecipe.setIngredient('D', new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.BLUE_WOOL, 1, "Bundle Of Dense Blue Wool", ChatColor.DARK_BLUE, "The sheep are naked", null, 15, true, enchants)));
        whiteRecipe.setIngredient('E', new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.LIGHT_BLUE_WOOL, 1, "Bundle Of Dense Light Blue Wool", ChatColor.BLUE, "The sheep are naked", null, 15, true, enchants)));
        whiteRecipe.setIngredient('F', new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.CYAN_WOOL, 1, "Bundle Of Dense Cyan Wool", ChatColor.getByChar("#009696"), "The sheep are naked", null, 15, true, enchants)));
        whiteRecipe.setIngredient('G', new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.GREEN_WOOL, 1, "Bundle Of Dense Green Wool", ChatColor.DARK_GREEN, "The sheep are naked", null, 15, true, enchants)));
        whiteRecipe.setIngredient('H', new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.LIME_WOOL, 1, "Bundle Of Dense Lime Wool", ChatColor.GREEN, "The sheep are naked", null, 15, true, enchants)));
        whiteRecipe.setIngredient('I', new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.WHITE_WOOL, 1, "Bundle Of Dense White Wool", ChatColor.GRAY, "The sheep are naked", null, 15, true, enchants)));
        plugin.getServer().addRecipe(whiteRecipe);

        // Color
        List<List<String>> colors = new ArrayList<>();
        colors.add(ColorParser.generateGradient("#FF0000", "#00FF00", 4));
        colors.add(ColorParser.generateGradient("#00FF00", "#0000FF", 4));
        colors.add(ColorParser.generateGradient("#0000FF", "#FF00FF", 4));
        String name8 = ColorParser.colorizeString("Color Trophy", ColorParser.gradientConnector(colors), true);
        List<List<String>> loreColors = new ArrayList<>();
        loreColors.add(ColorParser.generateGradient("#FF0000", "#00FF00", 10));
        loreColors.add(ColorParser.generateGradient("#00FF00", "#0000FF", 10));
        loreColors.add(ColorParser.generateGradient("#0000FF", "#FF00FF", 10));
        loreColors.add(ColorParser.generateGradient("#FF00FF", "#FF0000", 9));
        String lore8 = ColorParser.colorizeString("~Rainbows follow you wherever you walk~", ColorParser.gradientConnector(loreColors), false);
        ItemStack colorTrophy = ItemStackGenerator.getTrophyItem(Material.SHEARS, name8, lore8);
        trophyItems.put(6, colorTrophy);

        ShapedRecipe colorRecipe = new ShapedRecipe(colorKey, colorTrophy);
        colorRecipe.shape("A", "B", "C");
        colorRecipe.setIngredient('A', new RecipeChoice.ExactChoice(whiteFragment));
        colorRecipe.setIngredient('B', new RecipeChoice.ExactChoice(blackFragment));
        colorRecipe.setIngredient('C', Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
        plugin.getServer().addRecipe(colorRecipe);

        // Nether Recipe
        String name9 = ChatColor.DARK_RED + ChatColor.BOLD.toString() + "Nether Trophy";
        String lore9 = ChatColor.RED + "~The fires of hell feel cold on your skin~";
        ItemStack netherTrophy = ItemStackGenerator.getTrophyItem(Material.NETHERRACK, name9, lore9);
        trophyItems.put(7, netherTrophy);
        ShapelessRecipe netherRecipe = new ShapelessRecipe(netherKey, netherTrophy);
        netherRecipe.addIngredient(Material.BLACKSTONE);
        netherRecipe.addIngredient(Material.CRIMSON_STEM);
        netherRecipe.addIngredient(Material.WARPED_STEM);
        netherRecipe.addIngredient(Material.NETHER_BRICKS);
        netherRecipe.addIngredient(Material.NETHERITE_BLOCK);
        netherRecipe.addIngredient(Material.NETHERRACK);
        netherRecipe.addIngredient(Material.QUARTZ);
        netherRecipe.addIngredient(Material.NETHER_WART);
        netherRecipe.addIngredient(Material.BLAZE_ROD);
        plugin.getServer().addRecipe(netherRecipe);

        // End Recipe
        String name10 = ColorParser.colorizeString("End Trophy", ColorParser.generateGradient("#9600FF", "#C800FF", 10), true);
        String lore10 = ColorParser.colorizeString("~Space warps around your fingers~", ColorParser.generateGradient("#C800FF", "#9600FF", 33), false);
        ItemStack endTrophy = ItemStackGenerator.getTrophyItem(Material.END_STONE, name10, lore10);
        trophyItems.put(8, endTrophy);
        ShapelessRecipe endRecipe = new ShapelessRecipe(endKey, endTrophy);
        endRecipe.addIngredient(Material.END_ROD);
        endRecipe.addIngredient(Material.DRAGON_HEAD);
        endRecipe.addIngredient(Material.CHORUS_FRUIT);
        endRecipe.addIngredient(Material.ELYTRA);
        endRecipe.addIngredient(Material.END_CRYSTAL);
        endRecipe.addIngredient(Material.ENDER_PEARL);
        endRecipe.addIngredient(Material.END_STONE);
        endRecipe.addIngredient(Material.SHULKER_BOX);
        endRecipe.addIngredient(Material.CHORUS_FLOWER);
        plugin.getServer().addRecipe(endRecipe);

        // Champion Trophy
        String name11 = ColorParser.colorizeString("Champion Trophy", ColorParser.generateGradient("#FF0000", "#FFE200", 15), true);
        String lore11 = ColorParser.colorizeString("~The realm of the gods is nearby~", ColorParser.generateGradient("#FFE200", "#FF0000", 33), false);
        ItemStack championTrophy = ItemStackGenerator.getTrophyItem(Material.DIAMOND_SWORD, name11, lore11);
        trophyItems.put(9, championTrophy);
        ShapedRecipe championRecipe = new ShapedRecipe(championKey, championTrophy);
        championRecipe.shape("ABC", "DEF", "G  ");
        championRecipe.setIngredient('A', Material.NETHER_STAR);
        // Ender Dragon
        championRecipe.setIngredient('B', new RecipeChoice.ExactChoice(ItemStackGenerator.getEnderDragonBossItem()));
        // Giant
        championRecipe.setIngredient('C', new RecipeChoice.ExactChoice(ItemStackGenerator.getGiantBossItem()));
        // BroodMother
        championRecipe.setIngredient('D', new RecipeChoice.ExactChoice(ItemStackGenerator.getBroodMotherBossItem()));
        // Elder Guardian
        championRecipe.setIngredient('E', new RecipeChoice.ExactChoice(ItemStackGenerator.getElderGuardianBossItem()));
        // Warden
        championRecipe.setIngredient('F', new RecipeChoice.ExactChoice(ItemStackGenerator.getWardenBossItem()));
        // Villager Boss
        championRecipe.setIngredient('G', new RecipeChoice.ExactChoice(ItemStackGenerator.getVillagerBossItem()));
        plugin.getServer().addRecipe(championRecipe);

        // God Trophy (No recipe)
        String name12 = ColorParser.colorizeString("God Trophy", ColorParser.generateGradient("#FFFF00", "#FFFFFF", 10), true);
        String lore12 = ColorParser.colorizeString("~There is nothing you can not do~", ColorParser.generateGradient("#FFFFFF", "#FFFF00", 33), false);
        trophyItems.put(10, ItemStackGenerator.getTrophyItem(Material.GRASS_BLOCK, name12, lore12));
    }

    public static void denseWoolRecipes(SurvivalSkills plugin) {
        Map<Enchantment, Integer> enchants = new HashMap<>();
        enchants.put(Enchantment.KNOCKBACK, 1);
        denseWoolRecipesHelper(plugin, Material.WHITE_WOOL, "White", ChatColor.GRAY, enchants);
        denseWoolRecipesHelper(plugin, Material.GRAY_WOOL, "Gray", ChatColor.DARK_GRAY, enchants);
        denseWoolRecipesHelper(plugin, Material.LIGHT_GRAY_WOOL, "Light Gray", ChatColor.GRAY, enchants);
        denseWoolRecipesHelper(plugin, Material.BROWN_WOOL, "Brown", ChatColor.getByChar("#6E2C00"), enchants);
        denseWoolRecipesHelper(plugin, Material.BLACK_WOOL, "Black", ChatColor.BLACK, enchants);
        denseWoolRecipesHelper(plugin, Material.RED_WOOL, "Red", ChatColor.RED, enchants);
        denseWoolRecipesHelper(plugin, Material.ORANGE_WOOL, "Orange", ChatColor.getByChar("#FF8C00"), enchants);
        denseWoolRecipesHelper(plugin, Material.YELLOW_WOOL, "Yellow", ChatColor.YELLOW, enchants);
        denseWoolRecipesHelper(plugin, Material.PINK_WOOL, "Pink", ChatColor.getByChar("#FF00A2"), enchants);
        denseWoolRecipesHelper(plugin, Material.MAGENTA_WOOL, "Magenta", ChatColor.LIGHT_PURPLE, enchants);
        denseWoolRecipesHelper(plugin, Material.PURPLE_WOOL, "Purple", ChatColor.DARK_PURPLE, enchants);
        denseWoolRecipesHelper(plugin, Material.BLUE_WOOL, "Blue", ChatColor.DARK_BLUE, enchants);
        denseWoolRecipesHelper(plugin, Material.LIGHT_BLUE_WOOL, "Light Blue", ChatColor.BLUE, enchants);
        denseWoolRecipesHelper(plugin, Material.CYAN_WOOL, "Cyan", ChatColor.getByChar("#009696"), enchants);
        denseWoolRecipesHelper(plugin, Material.GREEN_WOOL, "Green", ChatColor.DARK_GREEN, enchants);
        denseWoolRecipesHelper(plugin, Material.LIME_WOOL, "Lime", ChatColor.GREEN, enchants);
    }

    public static void denseWoolRecipesHelper(SurvivalSkills plugin, Material material, String color, ChatColor chatColor, Map<Enchantment, Integer> enchants) {
        String tag = color.toLowerCase().replaceAll("\\s", "");
        int model = 15;
        ItemStack wool1 = ItemStackGenerator.createCustomItem(material, 1, "Compacted " + color + " Wool", null, "Very soft", null, model, true, enchants);
        ItemStack wool2 = ItemStackGenerator.createCustomItem(material, 1, "Dense " + color + " Wool", chatColor, "Not very soft", null, model, true, enchants);
        ItemStack wool3 = ItemStackGenerator.createCustomItem(material, 1, "Bundle Of Dense " + color + " Wool", chatColor, "The sheep are naked", null, model, true, enchants);
        NamespacedKey tag1 = makeRecipeWithSingleIngredient(plugin, new ItemStack(material), wool1, tag + "1");
        NamespacedKey tag2 = makeRecipeWithSingleIngredient(plugin, wool1, wool2, tag + "2");
        NamespacedKey tag3 = makeRecipeWithSingleIngredient(plugin, wool2, wool3, tag + "3");

        ArrayList<NamespacedKey> recipeKeys = plugin.getRecipeKeys();
        if (!plugin.isWoolRecipes()) {
            recipeKeys.add(tag1);
            recipeKeys.add(tag2);
            recipeKeys.add(tag3);
            plugin.setWoolRecipes(true);
        }
    }

    /**
     * Loads a shapeless recipe that has nine of the same ingredient
     */
    public static NamespacedKey makeRecipeWithSingleIngredient(SurvivalSkills plugin, ItemStack ingredient, ItemStack result, String name) {
        NamespacedKey key = new NamespacedKey(plugin, name);
        if (plugin.getServer().getRecipe(key) != null) plugin.getServer().removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape("AAA", "AAA", "AAA");
        recipe.setIngredient('A', new RecipeChoice.ExactChoice(ingredient));
        plugin.getServer().addRecipe(recipe);
        return key;
    }

    public static void rewardRecipes(SurvivalSkills plugin) {
        NamespacedKey torchKey = createKey("torch", plugin);
        NamespacedKey bronzeKey = createKey("bronze", plugin);
        NamespacedKey zapWandKey = createKey("zapwand", plugin);
        NamespacedKey mineHelmetKey = createKey("minehelmet", plugin);
        NamespacedKey mineChestplateKey = createKey("minechestplate", plugin);
        NamespacedKey mineLeggingsKey = createKey("mineleggings", plugin);
        NamespacedKey mineBootsKey = createKey("mineboots", plugin);
        NamespacedKey jumpBootsKey = createKey("jumpboots", plugin);
        NamespacedKey jumpBootsKey2 = createKey("jumpboots2", plugin);
        NamespacedKey wandererHelmetKey = createKey("wandererhelmet", plugin);
        NamespacedKey wandererChestplateKey = createKey("wandererchestplate", plugin);
        NamespacedKey wandererLeggingsKey = createKey("wandererleggings", plugin);
        NamespacedKey wandererBootsKey = createKey("wandererboots", plugin);
        NamespacedKey travelerHelmetKey = createKey("travelerhelmet", plugin);
        NamespacedKey travelerChestplateKey = createKey("travelerchestplate", plugin);
        NamespacedKey travelerLeggingsKey = createKey("travelerleggings", plugin);
        NamespacedKey travelerBootsKey = createKey("travelerboots", plugin);
        NamespacedKey gillHelmetKey = createKey("gillhelmet", plugin);
        NamespacedKey hardNautilusShellKey = createKey("hardnautilusshell", plugin);
        NamespacedKey hardHeartOfTheSeaKey = createKey("hardheartofthesea", plugin);
        NamespacedKey gillChestplateKey = createKey("gillchestplate", plugin);
        NamespacedKey gillLeggingsKey = createKey("gillleggings", plugin);
        NamespacedKey gillBootsKey = createKey("gillboots", plugin);
        NamespacedKey adventurerHelmetKey = createKey("adventurerhelmet", plugin);
        NamespacedKey adventurerChestplateKey = createKey("adventurerchestplate", plugin);
        NamespacedKey adventurerLeggingsKey = createKey("adventurerleggings", plugin);
        NamespacedKey adventurerBootsKey = createKey("adventurerboots", plugin);
        NamespacedKey caveFinder = createKey("cavefinder", plugin);
        NamespacedKey wateringCanKey = createKey("wateringcan", plugin);
        NamespacedKey unlimitedBoneMealKey = createKey("unlimitedbonemeal", plugin);
        NamespacedKey harvesterKey = createKey("harvester", plugin);
        NamespacedKey giantBoss = createKey("giantboss", plugin);
        NamespacedKey broodMotherBoss = createKey("broodmotherboss", plugin);
        NamespacedKey villagerboss = createKey("villagerboss", plugin);
        NamespacedKey sortOfStonePick = createKey("sortofstonepick", plugin);
        NamespacedKey gapple = createKey("gapple", plugin);
        NamespacedKey fireworkcannon = createKey("fireworkcannon", plugin);
        NamespacedKey sortwand = createKey("sortwand", plugin);

        ItemStackGenerator.createSmallShapedRecipe(torchKey, ItemStackGenerator.getUnlimitedTorch(), "ABA:BCB:ABA",
                null, null, null, Material.LAVA_BUCKET, Material.COAL_BLOCK, Material.TORCH);
        ItemStackGenerator.createSmallShapedRecipe(sortOfStonePick, ItemStackGenerator.getSortOfStonePick(), "AAA: B : B ",
                null, null, null, Material.COBBLED_DEEPSLATE, Material.STICK, null);
        ItemStackGenerator.createSmallShapedRecipe(bronzeKey, ItemStackGenerator.getBronzeIngot(), "AAA:ABA:AAA",
                null, null, null, Material.COPPER_BLOCK, Material.GOLD_BLOCK, null);
        ItemStackGenerator.createSmallShapedRecipe(zapWandKey, ItemStackGenerator.getZapWand(), "DDD:AAA:DDD",
                ItemStackGenerator.getBronzeIngot(), null, null, null, null, null);

        ItemStackGenerator.createSmallShapedRecipe(mineHelmetKey, ItemStackGenerator.getMiningHelmet(), "ABA:C C:DDD",
                ItemStackGenerator.getFireResistancePotion(), null, null, null, Material.LEATHER_HELMET, Material.DIAMOND_BLOCK);
        ItemStackGenerator.createSmallShapedRecipe(mineChestplateKey, ItemStackGenerator.getMiningChestplate(), "C C:CBC:CAC",
                null, null, null, Material.CAKE, Material.LEATHER_CHESTPLATE, Material.DIAMOND_BLOCK);
        ItemStackGenerator.createSmallShapedRecipe(mineLeggingsKey, ItemStackGenerator.getMiningLeggings(), "CBC:CAC:C C",
                null, null, null, Material.CAKE, Material.LEATHER_LEGGINGS, Material.DIAMOND_BLOCK);
        ItemStackGenerator.createSmallShapedRecipe(mineBootsKey, ItemStackGenerator.getMiningBoots(), "DDD:ABA:C C",
                ItemStackGenerator.getFireResistancePotion(), null, null, null, Material.LEATHER_BOOTS, Material.DIAMOND_BLOCK);

        ItemStackGenerator.createSmallShapedRecipe(jumpBootsKey, ItemStackGenerator.getJumpingBoots(), "ABA:C C:DDD",
                ItemStackGenerator.getJumpPowerPotion(), null, null, null, Material.IRON_BOOTS, Material.SLIME_BLOCK);
        ItemStackGenerator.createSmallShapedRecipe(jumpBootsKey2, ItemStackGenerator.getJumpingBoots(), "DDD:ABA:C C",
                ItemStackGenerator.getJumpPowerPotion(), null, null, null, Material.IRON_BOOTS, Material.SLIME_BLOCK);

        ItemStackGenerator.createSmallShapedRecipe(wandererHelmetKey, ItemStackGenerator.getWandererHelmet(), "BAB:B B:DDD",
                ItemStackGenerator.getSpeedPotion(), null, null, null, Material.CHAIN, null);
        ItemStackGenerator.createSmallShapedRecipe(wandererChestplateKey, ItemStackGenerator.getWandererChestplate(), "B B:BAB:BBB",
                ItemStackGenerator.getSpeedPotion(), null, null, null, Material.CHAIN, null);
        ItemStackGenerator.createSmallShapedRecipe(wandererLeggingsKey, ItemStackGenerator.getWandererLeggings(), "BAB:B B:B B",
                ItemStackGenerator.getSpeedPotion(), null, null, null, Material.CHAIN, null);
        ItemStackGenerator.createSmallShapedRecipe(wandererBootsKey, ItemStackGenerator.getWandererBoots(), "DDD:B B:BAB",
                ItemStackGenerator.getSpeedPotion(), null, null, null, Material.CHAIN, null);

        ItemStackGenerator.createSmallShapedRecipe(travelerHelmetKey, ItemStackGenerator.getTravelerHelmet(), "BAB:B B:DDD",
                ItemStackGenerator.getWandererHelmet(), null, null, null, Material.DIAMOND, null);
        ItemStackGenerator.createSmallShapedRecipe(travelerChestplateKey, ItemStackGenerator.getTravelerChestplate(), "B B:BAB:BBB",
                ItemStackGenerator.getWandererChestplate(), null, null, null, Material.DIAMOND, null);
        ItemStackGenerator.createSmallShapedRecipe(travelerLeggingsKey, ItemStackGenerator.getTravelerLeggings(), "BAB:B B:B B",
                ItemStackGenerator.getWandererLeggings(), null, null, null, Material.DIAMOND, null);
        ItemStackGenerator.createSmallShapedRecipe(travelerBootsKey, ItemStackGenerator.getTravelerBoots(), "DDD:BAB:B B",
                ItemStackGenerator.getWandererBoots(), null, null, null, Material.DIAMOND, null);

        ItemStackGenerator.createSmallShapedRecipe(hardNautilusShellKey, ItemStackGenerator.getHardNautilusShell(), "AAA:AAA:AAA",
                null, null, null, Material.NAUTILUS_SHELL, null, null);
        ItemStackGenerator.createSmallShapedRecipe(hardHeartOfTheSeaKey, ItemStackGenerator.getHardHeartOfTheSea(), "AAA:AAA:AAA",
                null, null, null, Material.HEART_OF_THE_SEA, null, null);

        ItemStack shells = ItemStackGenerator.getHardNautilusShell();
        ItemStack heartOfTheSeas = ItemStackGenerator.getHardHeartOfTheSea();
        ItemStackGenerator.createSmallShapedRecipe(gillHelmetKey, ItemStackGenerator.getGillHelmet(), "AAA:B B:DDD",
                shells, heartOfTheSeas, null, null, null, null);
        ItemStackGenerator.createSmallShapedRecipe(gillChestplateKey, ItemStackGenerator.getGillChestplate(), "A A:BBB:AAA",
                shells, heartOfTheSeas, null, null, null, null);
        ItemStackGenerator.createSmallShapedRecipe(gillLeggingsKey, ItemStackGenerator.getGillLeggings(), "AAA:A A:B B",
                shells, heartOfTheSeas, null, null, null, null);
        ItemStackGenerator.createSmallShapedRecipe(gillBootsKey, ItemStackGenerator.getGillBoots(), "DDD:A A:B B",
                shells, heartOfTheSeas, null, null, null, null);

        ItemStackGenerator.createSmallShapedRecipe(adventurerHelmetKey, ItemStackGenerator.getAdventurerHelmet(), "BAB:C C:DDD",
                ItemStackGenerator.getTravelerHelmet(), null, null, null, Material.NETHERITE_INGOT, Material.FEATHER);
        ItemStackGenerator.createSmallShapedRecipe(adventurerChestplateKey, ItemStackGenerator.getAdventurerChestplate(), "C C:BAB:CCC",
                ItemStackGenerator.getTravelerChestplate(), null, null, null, Material.NETHERITE_INGOT, Material.FEATHER);
        ItemStackGenerator.createSmallShapedRecipe(adventurerLeggingsKey, ItemStackGenerator.getAdventurerLeggings(), "BAB:C C:C C",
                ItemStackGenerator.getTravelerLeggings(), null, null, null, Material.NETHERITE_INGOT, Material.FEATHER);
        ItemStackGenerator.createSmallShapedRecipe(adventurerBootsKey, ItemStackGenerator.getAdventurerBoots(), "DDD:BAB:C C",
                ItemStackGenerator.getTravelerBoots(), null, null, null, Material.NETHERITE_INGOT, Material.FEATHER);

        ItemStackGenerator.createSmallShapedRecipe(caveFinder, ItemStackGenerator.getCaveFinder(), "AAA:ABA:AAA",
                null, null, null, Material.REDSTONE_BLOCK, Material.COMPASS, null);
        ItemStackGenerator.createSmallShapedRecipe(wateringCanKey, ItemStackGenerator.getWateringCan(), "ABA:ACA:AAA",
                null, null, null, Material.LAPIS_BLOCK, Material.NAUTILUS_SHELL, Material.WATER_BUCKET);
        ItemStackGenerator.createSmallShapedRecipe(unlimitedBoneMealKey, ItemStackGenerator.getUnlimitedBoneMeal(), "AAA:ABA:AAA",
                null, null, null, Material.BONE_BLOCK, Material.GOLDEN_APPLE, null);
        ItemStackGenerator.createSmallShapedRecipe(harvesterKey, ItemStackGenerator.getHarvester(), "ABA:BCB:ABA",
                null, null, null, Material.REDSTONE_BLOCK, Material.WHEAT_SEEDS, Material.NETHERITE_HOE);

        ItemStackGenerator.createSmallShapedRecipe(giantBoss, ItemStackGenerator.getGiantSummoner(), "AAA:ABA:AAA",
                null, null, null, Material.ROTTEN_FLESH, Material.EGG, null);
        ItemStackGenerator.createSmallShapedRecipe(broodMotherBoss, ItemStackGenerator.getBroodMotherSummoner(), "AAA:CBC:AAA",
                null, null, null, Material.STRING, Material.EGG, Material.SPIDER_EYE);
        ItemStackGenerator.createSmallShapedRecipe(villagerboss, ItemStackGenerator.getVillagerSummoner(), "AAA:CBC:AAA",
                null, null, null, Material.EMERALD_BLOCK, Material.EGG, Material.TOTEM_OF_UNDYING);

        ItemStackGenerator.createSmallShapedRecipe(gapple, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE), "AAA:ABA:AAA",
                null, null, null, Material.GOLD_BLOCK, Material.GOLDEN_APPLE, null);
        ItemStackGenerator.createSmallShapedRecipe(fireworkcannon, ItemStackGenerator.getFireworkCannon(), "AAA:ABA:ACA",
                null, null, null, Material.FIREWORK_ROCKET, Material.CAMPFIRE, Material.BLAZE_POWDER);
        ItemStackGenerator.createSmallShapedRecipe(sortwand, ItemStackGenerator.getSortWand(), "CAC:ABA:CAC",
                null, null, null, Material.COMPARATOR, Material.BLAZE_ROD, Material.CHEST);
    }

    public static NamespacedKey createKey(String name, SurvivalSkills plugin) {
        NamespacedKey key = new NamespacedKey(plugin, name);
        if (plugin.getServer().getRecipe(key) != null) plugin.getServer().removeRecipe(key);
        ArrayList<NamespacedKey> recipeKeys = plugin.getRecipeKeys();
        recipeKeys.add(key);
        return key;
    }
}
