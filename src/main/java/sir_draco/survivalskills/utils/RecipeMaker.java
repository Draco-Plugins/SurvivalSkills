package sir_draco.survivalskills.utils;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.commands.default_commands.SkillStatsCommand;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.*;

public class RecipeMaker {

        private final static Deque<RecipeRecord> recipeStack = new ArrayDeque<>();

        public static void createSmallShapedRecipe(NamespacedKey key, ItemStack result, String shape, ItemStack as,
                        ItemStack bs, ItemStack cs, Material am, Material bm, Material cm) {
                ShapedRecipe recipe = new ShapedRecipe(key, result);
                String[] shapes = shape.split(":");

                if (shapes[0].contains("DDD"))
                        recipe.shape(shapes[1], shapes[2]);
                else if (shapes[2].contains("DDD"))
                        recipe.shape(shapes[0], shapes[1]);
                else
                        recipe.shape(shapes[0], shapes[1], shapes[2]);

                if (as != null)
                        recipe.setIngredient('A', new RecipeChoice.ExactChoice(as));
                else if (am != null)
                        recipe.setIngredient('A', am);

                if (bs != null)
                        recipe.setIngredient('B', new RecipeChoice.ExactChoice(bs));
                else if (bm != null)
                        recipe.setIngredient('B', bm);

                if (cs != null)
                        recipe.setIngredient('C', new RecipeChoice.ExactChoice(cs));
                else if (cm != null)
                        recipe.setIngredient('C', cm);

                RecipeMaker.addShapedRecipe(recipe, key);
        }

        public static void createShapedRecipe(NamespacedKey key, ItemStack result, ItemStack a, ItemStack b,
                        ItemStack c, ItemStack d,
                        ItemStack e, ItemStack f, ItemStack g, ItemStack h, ItemStack i, boolean unique) {
                ShapedRecipe recipe = new ShapedRecipe(key, result);

                // Create the shape
                StringBuilder shape1 = new StringBuilder();
                if (a != null)
                        shape1.append("A");
                else
                        shape1.append(" ");
                if (b != null)
                        shape1.append("B");
                else
                        shape1.append(" ");
                if (c != null)
                        shape1.append("C");
                else
                        shape1.append(" ");

                StringBuilder shape2 = new StringBuilder();
                if (d != null)
                        shape2.append("D");
                else
                        shape2.append(" ");
                if (e != null)
                        shape2.append("E");
                else
                        shape2.append(" ");
                if (f != null)
                        shape2.append("F");
                else
                        shape2.append(" ");

                StringBuilder shape3 = new StringBuilder();
                if (g != null)
                        shape3.append("G");
                else
                        shape3.append(" ");
                if (h != null)
                        shape3.append("H");
                else
                        shape3.append(" ");
                if (i != null)
                        shape3.append("I");
                else
                        shape3.append(" ");

                recipe.shape(shape1.toString(), shape2.toString(), shape3.toString());

                // Set the ingredients
                if (unique) {
                        if (a != null)
                                recipe.setIngredient('A', new RecipeChoice.ExactChoice(a));
                        if (b != null)
                                recipe.setIngredient('B', new RecipeChoice.ExactChoice(b));
                        if (c != null)
                                recipe.setIngredient('C', new RecipeChoice.ExactChoice(c));
                        if (d != null)
                                recipe.setIngredient('D', new RecipeChoice.ExactChoice(d));
                        if (e != null)
                                recipe.setIngredient('E', new RecipeChoice.ExactChoice(e));
                        if (f != null)
                                recipe.setIngredient('F', new RecipeChoice.ExactChoice(f));
                        if (g != null)
                                recipe.setIngredient('G', new RecipeChoice.ExactChoice(g));
                        if (h != null)
                                recipe.setIngredient('H', new RecipeChoice.ExactChoice(h));
                        if (i != null)
                                recipe.setIngredient('I', new RecipeChoice.ExactChoice(i));
                } else {
                        if (a != null)
                                recipe.setIngredient('A', a.getType());
                        if (b != null)
                                recipe.setIngredient('B', b.getType());
                        if (c != null)
                                recipe.setIngredient('C', c.getType());
                        if (d != null)
                                recipe.setIngredient('D', d.getType());
                        if (e != null)
                                recipe.setIngredient('E', e.getType());
                        if (f != null)
                                recipe.setIngredient('F', f.getType());
                        if (g != null)
                                recipe.setIngredient('G', g.getType());
                        if (h != null)
                                recipe.setIngredient('H', h.getType());
                        if (i != null)
                                recipe.setIngredient('I', i.getType());
                }

                RecipeMaker.addShapedRecipe(recipe, key);
        }

        /**
         * Creates recipes for all the trophies
         */
        public static void trophyRecipes(SurvivalSkills plugin) {
                // Create namespace keys for the recipes and ensure old ones are removed to be
                // updated
                NamespacedKey caveKey = createKey("cave", plugin);
                NamespacedKey forestKey = createKey("forest", plugin);
                NamespacedKey farmingKey = createKey("farming", plugin);
                NamespacedKey oceanKey = createKey("ocean", plugin);
                NamespacedKey fishingKey = createKey("fishing", plugin);
                NamespacedKey blackKey = createKey("black", plugin);
                NamespacedKey whiteKey = createKey("white", plugin);
                NamespacedKey colorKey = createKey("color", plugin);
                NamespacedKey netherKey = createKey("nether", plugin);
                NamespacedKey endKey = createKey("end", plugin);
                NamespacedKey championKey = createKey("champion", plugin);
                NamespacedKey godKey = createKey("god", plugin);

                HashMap<Integer, ItemStack> trophyItems = plugin.getTrophyManager().getTrophyItems();
                denseWoolRecipes(plugin);

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
                addShapelessRecipe(caveRecipe, caveKey);

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
                forestRecipe.addIngredient(Material.PALE_OAK_LOG);
                addShapelessRecipe(forestRecipe, forestKey);

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
                addShapelessRecipe(farmingRecipe, farmingKey);

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
                addShapelessRecipe(oceanRecipe, oceanKey);

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
                addShapedRecipe(fishingRecipe, fishingKey);

                // Color Recipe
                Map<Enchantment, Integer> enchants = new HashMap<>();
                enchants.put(Enchantment.KNOCKBACK, 1);
                // Black
                String name6 = ChatColor.BLACK + ChatColor.BOLD.toString() + "Black Fragment";
                String lore6 = ChatColor.DARK_GRAY + "~You merely adopted the dark, I was born in it~";
                ItemStack blackFragment = ItemStackGenerator.createCustomItem(Material.BLACK_WOOL, 1, name6,
                                ChatColor.DARK_GRAY, lore6, null, 15, true, enchants);
                ShapedRecipe blackRecipe = new ShapedRecipe(blackKey, blackFragment);
                blackRecipe.shape("ABC", "DEF", "G  ");
                blackRecipe.setIngredient('A',
                                new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(
                                                Material.LIGHT_GRAY_WOOL, 1, "Bundle Of Dense Light Gray Wool",
                                                ChatColor.GRAY, "The sheep are naked", null, 15, true, enchants)));
                blackRecipe.setIngredient('B',
                                new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.GRAY_WOOL, 1,
                                                "Bundle Of Dense Gray Wool", ChatColor.DARK_GRAY, "The sheep are naked",
                                                null, 15, true, enchants)));
                blackRecipe.setIngredient('C',
                                new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.BLACK_WOOL, 1,
                                                "Bundle Of Dense Black Wool", ChatColor.BLACK, "The sheep are naked",
                                                null, 15, true, enchants)));
                blackRecipe.setIngredient('D',
                                new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.BROWN_WOOL, 1,
                                                "Bundle Of Dense Brown Wool", ChatColor.getByChar("#6E2C00"),
                                                "The sheep are naked", null, 15, true, enchants)));
                blackRecipe.setIngredient('E',
                                new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.RED_WOOL, 1,
                                                "Bundle Of Dense Red Wool", ChatColor.RED, "The sheep are naked", null,
                                                15, true, enchants)));
                blackRecipe.setIngredient('F',
                                new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.ORANGE_WOOL,
                                                1, "Bundle Of Dense Orange Wool", ChatColor.getByChar("#FF8C00"),
                                                "The sheep are naked", null, 15, true, enchants)));
                blackRecipe.setIngredient('G',
                                new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.YELLOW_WOOL,
                                                1, "Bundle Of Dense Yellow Wool", ChatColor.YELLOW,
                                                "The sheep are naked", null, 15, true, enchants)));
                addShapedRecipe(blackRecipe, blackKey);

                // White
                String name7 = ChatColor.WHITE + ChatColor.BOLD.toString() + "White Fragment";
                String lore7 = ChatColor.WHITE + "~All colors become one~";
                ItemStack whiteFragment = ItemStackGenerator.createCustomItem(Material.WHITE_WOOL, 1, name7,
                                ChatColor.WHITE, lore7, null, 15, true, enchants);
                ShapedRecipe whiteRecipe = new ShapedRecipe(whiteKey, whiteFragment);
                whiteRecipe.shape("ABC", "DEF", "GHI");
                whiteRecipe.setIngredient('A',
                                new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.PINK_WOOL, 1,
                                                "Bundle Of Dense Pink Wool", ChatColor.getByChar("#FF00A2"),
                                                "The sheep are naked", null, 15, true, enchants)));
                whiteRecipe.setIngredient('B',
                                new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.MAGENTA_WOOL,
                                                1, "Bundle Of Dense Magenta Wool", ChatColor.LIGHT_PURPLE,
                                                "The sheep are naked", null, 15, true, enchants)));
                whiteRecipe.setIngredient('C',
                                new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.PURPLE_WOOL,
                                                1, "Bundle Of Dense Purple Wool", ChatColor.DARK_PURPLE,
                                                "The sheep are naked", null, 15, true, enchants)));
                whiteRecipe.setIngredient('D',
                                new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.BLUE_WOOL, 1,
                                                "Bundle Of Dense Blue Wool", ChatColor.DARK_BLUE, "The sheep are naked",
                                                null, 15, true, enchants)));
                whiteRecipe.setIngredient('E',
                                new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(
                                                Material.LIGHT_BLUE_WOOL, 1, "Bundle Of Dense Light Blue Wool",
                                                ChatColor.BLUE, "The sheep are naked", null, 15, true, enchants)));
                whiteRecipe.setIngredient('F',
                                new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.CYAN_WOOL, 1,
                                                "Bundle Of Dense Cyan Wool", ChatColor.getByChar("#009696"),
                                                "The sheep are naked", null, 15, true, enchants)));
                whiteRecipe.setIngredient('G',
                                new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.GREEN_WOOL, 1,
                                                "Bundle Of Dense Green Wool", ChatColor.DARK_GREEN,
                                                "The sheep are naked", null, 15, true, enchants)));
                whiteRecipe.setIngredient('H',
                                new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.LIME_WOOL, 1,
                                                "Bundle Of Dense Lime Wool", ChatColor.GREEN, "The sheep are naked",
                                                null, 15, true, enchants)));
                whiteRecipe.setIngredient('I',
                                new RecipeChoice.ExactChoice(ItemStackGenerator.createCustomItem(Material.WHITE_WOOL, 1,
                                                "Bundle Of Dense White Wool", ChatColor.GRAY, "The sheep are naked",
                                                null, 15, true, enchants)));
                addShapedRecipe(whiteRecipe, whiteKey);

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
                String lore8 = ColorParser.colorizeString("~Rainbows follow you wherever you walk~",
                                ColorParser.gradientConnector(loreColors), false);
                ItemStack colorTrophy = ItemStackGenerator.getTrophyItem(Material.SHEARS, name8, lore8);
                trophyItems.put(6, colorTrophy);

                ShapedRecipe colorRecipe = new ShapedRecipe(colorKey, colorTrophy);
                colorRecipe.shape("A", "B", "C");
                colorRecipe.setIngredient('A', new RecipeChoice.ExactChoice(whiteFragment));
                colorRecipe.setIngredient('B', new RecipeChoice.ExactChoice(blackFragment));
                colorRecipe.setIngredient('C', Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
                addShapedRecipe(colorRecipe, colorKey);

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
                addShapelessRecipe(netherRecipe, netherKey);

                // End Recipe
                String name10 = ColorParser.colorizeString("End Trophy",
                                ColorParser.generateGradient("#9600FF", "#C800FF", 10), true);
                String lore10 = ColorParser.colorizeString("~Space warps around your fingers~",
                                ColorParser.generateGradient("#C800FF", "#9600FF", 33), false);
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
                addShapelessRecipe(endRecipe, endKey);

                // Champion Trophy
                String name11 = ColorParser.colorizeString("Champion Trophy",
                                ColorParser.generateGradient("#FF0000", "#FFE200", 15), true);
                String lore11 = ColorParser.colorizeString("~The realm of the gods is nearby~",
                                ColorParser.generateGradient("#FFE200", "#FF0000", 33), false);
                ItemStack championTrophy = ItemStackGenerator.getTrophyItem(Material.DIAMOND_SWORD, name11, lore11);
                trophyItems.put(9, championTrophy);
                ShapedRecipe championRecipe = new ShapedRecipe(championKey, championTrophy);
                championRecipe.shape("ABC", "DEF", "G  ");
                championRecipe.setIngredient('A', Material.NETHER_STAR);
                // Ender Dragon
                championRecipe.setIngredient('B',
                                new RecipeChoice.ExactChoice(ItemStackGenerator.getEnderDragonBossItem()));
                // Giant
                championRecipe.setIngredient('C', new RecipeChoice.ExactChoice(ItemStackGenerator.getGiantBossItem()));
                // BroodMother
                championRecipe.setIngredient('D',
                                new RecipeChoice.ExactChoice(ItemStackGenerator.getBroodMotherBossItem()));
                // Elder Guardian
                championRecipe.setIngredient('E',
                                new RecipeChoice.ExactChoice(ItemStackGenerator.getElderGuardianBossItem()));
                // Warden
                championRecipe.setIngredient('F', new RecipeChoice.ExactChoice(ItemStackGenerator.getWardenBossItem()));
                // Villager Boss
                championRecipe.setIngredient('G',
                                new RecipeChoice.ExactChoice(ItemStackGenerator.getVillagerBossItem()));
                addShapedRecipe(championRecipe, championKey);

                // God Trophy
                String name12 = ColorParser.colorizeString("God Trophy",
                                ColorParser.generateGradient("#FFFF00", "#FFFFFF", 10), true);
                String lore12 = ColorParser.colorizeString("~There is nothing you can not do~",
                                ColorParser.generateGradient("#FFFFFF", "#FFFF00", 33), false);
                ItemStack godTrophy = ItemStackGenerator.getTrophyItem(Material.GRASS_BLOCK, name12, lore12);
                createSmallShapedRecipe(godKey, godTrophy, "DAD:ABA:DAD",
                                ItemStackGenerator.getPowerOre(), ItemStackGenerator.getGodTrophyBase(), null, null,
                                null, null);
                trophyItems.put(10, godTrophy);
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
                denseWoolRecipesHelper(plugin, Material.ORANGE_WOOL, "Orange", ChatColor.getByChar("#FF8C00"),
                                enchants);
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

        public static void denseWoolRecipesHelper(SurvivalSkills plugin, Material material, String color,
                        ChatColor chatColor, Map<Enchantment, Integer> enchants) {
                String tag = color.toLowerCase().replaceAll("\\s", "");
                int model = 15;
                ItemStack wool1 = ItemStackGenerator.createCustomItem(material, 1, "Compacted " + color + " Wool", null,
                                "Very soft", null, model, true, enchants);
                ItemStack wool2 = ItemStackGenerator.createCustomItem(material, 1, "Dense " + color + " Wool",
                                chatColor, "Not very soft", null, model, true, enchants);
                ItemStack wool3 = ItemStackGenerator.createCustomItem(material, 1, "Bundle Of Dense " + color + " Wool",
                                chatColor, "The sheep are naked", null, model, true, enchants);
                NamespacedKey tag1 = makeRecipeWithSingleIngredient(plugin, new ItemStack(material), wool1, tag + "1");
                NamespacedKey tag2 = makeRecipeWithSingleIngredient(plugin, wool1, wool2, tag + "2");
                NamespacedKey tag3 = makeRecipeWithSingleIngredient(plugin, wool2, wool3, tag + "3");

                List<NamespacedKey> recipeKeys = plugin.getRecipeKeys();
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
        public static NamespacedKey makeRecipeWithSingleIngredient(SurvivalSkills plugin, ItemStack ingredient,
                        ItemStack result, String name) {
                NamespacedKey key = new NamespacedKey(plugin, name);
                ShapedRecipe recipe = new ShapedRecipe(key, result);
                recipe.shape("AAA", "AAA", "AAA");
                recipe.setIngredient('A', new RecipeChoice.ExactChoice(ingredient));
                addShapedRecipe(recipe, key);
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
                NamespacedKey beaconHelmetKey = createKey("beaconhelmet", plugin);
                NamespacedKey beaconChestplateKey = createKey("beaconchestplate", plugin);
                NamespacedKey beaconLeggingsKey = createKey("beaconleggings", plugin);
                NamespacedKey beaconBootsKey = createKey("beaconboots", plugin);
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
                NamespacedKey gapple = createKey("gapple", plugin);
                NamespacedKey fireworkcannon = createKey("fireworkcannon", plugin);
                NamespacedKey sortwand = createKey("sortwand", plugin);
                NamespacedKey magnetKey = createKey("magnet", plugin);
                NamespacedKey powerSwordKey = createKey("powersword", plugin);
                NamespacedKey powerDrillKey = createKey("powerdrill", plugin);
                NamespacedKey powerLaserKey = createKey("powerlaser", plugin);
                NamespacedKey powerHelmetKey = createKey("powerhelmet", plugin);
                NamespacedKey powerChestplateKey = createKey("powerchestplate", plugin);
                NamespacedKey powerLeggingsKey = createKey("powerleggings", plugin);
                NamespacedKey powerBootsKey = createKey("powerboots", plugin);
                NamespacedKey teleportAnchorKey = createKey("teleportanchor", plugin);

                createSmallShapedRecipe(torchKey, ItemStackGenerator.getUnlimitedTorch(), "ABA:BCB:ABA",
                                null, null, null, Material.LAVA_BUCKET, Material.COAL_BLOCK, Material.TORCH);
                createSmallShapedRecipe(bronzeKey, ItemStackGenerator.getBronzeIngot(), "AAA:ABA:AAA",
                                null, null, null, Material.COPPER_BLOCK, Material.GOLD_BLOCK, null);
                createSmallShapedRecipe(zapWandKey, ItemStackGenerator.getZapWand(), "DDD:AAA:DDD",
                                ItemStackGenerator.getBronzeIngot(), null, null, null, null, null);

                createSmallShapedRecipe(mineHelmetKey, ItemStackGenerator.getMiningHelmet(), "ABA:C C:DDD",
                                ItemStackGenerator.getFireResistancePotion(), null, null, null, Material.LEATHER_HELMET,
                                Material.DIAMOND_BLOCK);
                createSmallShapedRecipe(mineChestplateKey, ItemStackGenerator.getMiningChestplate(), "C C:CBC:CAC",
                                null, null, null, Material.CAKE, Material.LEATHER_CHESTPLATE, Material.DIAMOND_BLOCK);
                createSmallShapedRecipe(mineLeggingsKey, ItemStackGenerator.getMiningLeggings(), "CBC:CAC:C C",
                                null, null, null, Material.CAKE, Material.LEATHER_LEGGINGS, Material.DIAMOND_BLOCK);
                createSmallShapedRecipe(mineBootsKey, ItemStackGenerator.getMiningBoots(), "DDD:ABA:C C",
                                ItemStackGenerator.getFireResistancePotion(), null, null, null, Material.LEATHER_BOOTS,
                                Material.DIAMOND_BLOCK);

                createSmallShapedRecipe(beaconHelmetKey, ItemStackGenerator.getBeaconHelmet(), "AAA:ABA:DDD",
                                null, null, null, Material.BEACON, Material.NETHERITE_HELMET, null);
                createSmallShapedRecipe(beaconChestplateKey, ItemStackGenerator.getBeaconChestplate(), "ABA:AAA:AAA",
                                null, null, null, Material.BEACON, Material.NETHERITE_CHESTPLATE, null);
                createSmallShapedRecipe(beaconLeggingsKey, ItemStackGenerator.getBeaconLeggings(), "AAA:ABA:A A",
                                null, null, null, Material.BEACON, Material.NETHERITE_LEGGINGS, null);
                createSmallShapedRecipe(beaconBootsKey, ItemStackGenerator.getBeaconBoots(), "DDD:ABA:A A",
                                null, null, null, Material.BEACON, Material.NETHERITE_BOOTS, null);

                createSmallShapedRecipe(jumpBootsKey, ItemStackGenerator.getJumpingBoots(), "ABA:C C:DDD",
                                ItemStackGenerator.getJumpPowerPotion(), null, null, null, Material.IRON_BOOTS,
                                Material.SLIME_BLOCK);
                createSmallShapedRecipe(jumpBootsKey2, ItemStackGenerator.getJumpingBoots(), "DDD:ABA:C C",
                                ItemStackGenerator.getJumpPowerPotion(), null, null, null, Material.IRON_BOOTS,
                                Material.SLIME_BLOCK);

                createSmallShapedRecipe(wandererHelmetKey, ItemStackGenerator.getWandererHelmet(), "BAB:B B:DDD",
                                ItemStackGenerator.getSpeedPotion(), null, null, null, Material.IRON_CHAIN, null);
                createSmallShapedRecipe(wandererChestplateKey, ItemStackGenerator.getWandererChestplate(),
                                "B B:BAB:BBB",
                                ItemStackGenerator.getSpeedPotion(), null, null, null, Material.IRON_CHAIN, null);
                createSmallShapedRecipe(wandererLeggingsKey, ItemStackGenerator.getWandererLeggings(), "BAB:B B:B B",
                                ItemStackGenerator.getSpeedPotion(), null, null, null, Material.IRON_CHAIN, null);
                createSmallShapedRecipe(wandererBootsKey, ItemStackGenerator.getWandererBoots(), "DDD:B B:BAB",
                                ItemStackGenerator.getSpeedPotion(), null, null, null, Material.IRON_CHAIN, null);

                createSmallShapedRecipe(travelerHelmetKey, ItemStackGenerator.getTravelerHelmet(), "BAB:B B:DDD",
                                ItemStackGenerator.getWandererHelmet(), null, null, null, Material.DIAMOND, null);
                createSmallShapedRecipe(travelerChestplateKey, ItemStackGenerator.getTravelerChestplate(),
                                "B B:BAB:BBB",
                                ItemStackGenerator.getWandererChestplate(), null, null, null, Material.DIAMOND, null);
                createSmallShapedRecipe(travelerLeggingsKey, ItemStackGenerator.getTravelerLeggings(), "BAB:B B:B B",
                                ItemStackGenerator.getWandererLeggings(), null, null, null, Material.DIAMOND, null);
                createSmallShapedRecipe(travelerBootsKey, ItemStackGenerator.getTravelerBoots(), "DDD:BAB:B B",
                                ItemStackGenerator.getWandererBoots(), null, null, null, Material.DIAMOND, null);

                createSmallShapedRecipe(hardNautilusShellKey, ItemStackGenerator.getHardNautilusShell(), "AAA:AAA:AAA",
                                null, null, null, Material.NAUTILUS_SHELL, null, null);
                createSmallShapedRecipe(hardHeartOfTheSeaKey, ItemStackGenerator.getHardHeartOfTheSea(), "AAA:AAA:AAA",
                                null, null, null, Material.HEART_OF_THE_SEA, null, null);

                ItemStack shells = ItemStackGenerator.getHardNautilusShell();
                ItemStack heartOfTheSeas = ItemStackGenerator.getHardHeartOfTheSea();
                createSmallShapedRecipe(gillHelmetKey, ItemStackGenerator.getGillHelmet(), "AAA:B B:DDD",
                                shells, heartOfTheSeas, null, null, null, null);
                createSmallShapedRecipe(gillChestplateKey, ItemStackGenerator.getGillChestplate(), "A A:BBB:AAA",
                                shells, heartOfTheSeas, null, null, null, null);
                createSmallShapedRecipe(gillLeggingsKey, ItemStackGenerator.getGillLeggings(), "AAA:A A:B B",
                                shells, heartOfTheSeas, null, null, null, null);
                createSmallShapedRecipe(gillBootsKey, ItemStackGenerator.getGillBoots(), "DDD:A A:B B",
                                shells, heartOfTheSeas, null, null, null, null);

                createSmallShapedRecipe(adventurerHelmetKey, ItemStackGenerator.getAdventurerHelmet(), "BAB:C C:DDD",
                                ItemStackGenerator.getTravelerHelmet(), null, null, null, Material.NETHERITE_INGOT,
                                Material.FEATHER);
                createSmallShapedRecipe(adventurerChestplateKey, ItemStackGenerator.getAdventurerChestplate(),
                                "C C:BAB:CCC",
                                ItemStackGenerator.getTravelerChestplate(), null, null, null, Material.NETHERITE_INGOT,
                                Material.FEATHER);
                createSmallShapedRecipe(adventurerLeggingsKey, ItemStackGenerator.getAdventurerLeggings(),
                                "BAB:C C:C C",
                                ItemStackGenerator.getTravelerLeggings(), null, null, null, Material.NETHERITE_INGOT,
                                Material.FEATHER);
                createSmallShapedRecipe(adventurerBootsKey, ItemStackGenerator.getAdventurerBoots(), "DDD:BAB:C C",
                                ItemStackGenerator.getTravelerBoots(), null, null, null, Material.NETHERITE_INGOT,
                                Material.FEATHER);

                createSmallShapedRecipe(caveFinder, ItemStackGenerator.getCaveFinder(), "AAA:ABA:AAA",
                                null, null, null, Material.REDSTONE_BLOCK, Material.COMPASS, null);
                createSmallShapedRecipe(wateringCanKey, ItemStackGenerator.getWateringCan(), "ABA:ACA:AAA",
                                null, null, null, Material.LAPIS_BLOCK, Material.NAUTILUS_SHELL, Material.WATER_BUCKET);
                createSmallShapedRecipe(unlimitedBoneMealKey, ItemStackGenerator.getUnlimitedBoneMeal(), "AAA:ABA:AAA",
                                null, null, null, Material.BONE_BLOCK, Material.GOLDEN_APPLE, null);
                createSmallShapedRecipe(harvesterKey, ItemStackGenerator.getHarvester(), "ABA:BCB:ABA",
                                null, null, null, Material.REDSTONE_BLOCK, Material.WHEAT_SEEDS,
                                Material.NETHERITE_HOE);

                createSmallShapedRecipe(giantBoss, ItemStackGenerator.getGiantSummoner(), "AAA:ABA:AAA",
                                null, null, null, Material.ROTTEN_FLESH, Material.EGG, null);
                createSmallShapedRecipe(broodMotherBoss, ItemStackGenerator.getBroodMotherSummoner(), "AAA:CBC:AAA",
                                null, null, null, Material.STRING, Material.EGG, Material.SPIDER_EYE);
                createSmallShapedRecipe(villagerboss, ItemStackGenerator.getVillagerSummoner(), "AAA:CBC:AAA",
                                null, null, null, Material.EMERALD_BLOCK, Material.EGG, Material.TOTEM_OF_UNDYING);

                // Other
                createSmallShapedRecipe(gapple, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE), "AAA:ABA:AAA",
                                null, null, null, Material.GOLD_BLOCK, Material.GOLDEN_APPLE, null);
                createSmallShapedRecipe(fireworkcannon, ItemStackGenerator.getFireworkCannon(), "AAA:ABA:ACA",
                                null, null, null, Material.FIREWORK_ROCKET, Material.CAMPFIRE, Material.BLAZE_POWDER);
                createSmallShapedRecipe(sortwand, ItemStackGenerator.getSortWand(), "CAC:ABA:CAC",
                                null, null, null, Material.COMPARATOR, Material.BLAZE_ROD, Material.CHEST);
                createSmallShapedRecipe(magnetKey, ItemStackGenerator.getMagnet(), "ABA:BCB:ABA",
                                null, null, null, Material.IRON_BLOCK, Material.REDSTONE_BLOCK, Material.COPPER_BLOCK);

                // Power Ore Suite
                createSmallShapedRecipe(powerSwordKey, ItemStackGenerator.getPowerSword(), "DAD:BAB:DCD",
                                ItemStackGenerator.getPowerOre(), ItemStackGenerator.getZapWand(),
                                ItemStackGenerator.getBroodingSilk(), null, null, null);
                createSmallShapedRecipe(powerDrillKey, ItemStackGenerator.getPowerDrill(), "DAD:ABA:DBD",
                                ItemStackGenerator.getPowerOre(), null, null, null, Material.BEACON, null);
                createSmallShapedRecipe(powerLaserKey, ItemStackGenerator.getPowerLaser(), "DAD:ABA:DAD",
                                ItemStackGenerator.getPowerOre(), null, null, null, Material.END_CRYSTAL, null);
                createSmallShapedRecipe(powerHelmetKey, ItemStackGenerator.getPowerHelmet(), "AAA:BCB:DDD",
                                ItemStackGenerator.getPowerOre(), null, ItemStackGenerator.getBeaconHelmet(), null,
                                Material.NETHERITE_BLOCK, null);
                createSmallShapedRecipe(powerChestplateKey, ItemStackGenerator.getPowerChestplate(), "BDB:ACA:AAA",
                                ItemStackGenerator.getPowerOre(), null, ItemStackGenerator.getBeaconChestplate(), null,
                                Material.NETHERITE_BLOCK, null);
                createSmallShapedRecipe(powerLeggingsKey, ItemStackGenerator.getPowerLeggings(), "BCB:ADA:ADA",
                                ItemStackGenerator.getPowerOre(), null, ItemStackGenerator.getBeaconLeggings(), null,
                                Material.NETHERITE_BLOCK, null);
                createSmallShapedRecipe(powerBootsKey, ItemStackGenerator.getPowerBoots(), "DDD:BCB:ADA",
                                ItemStackGenerator.getPowerOre(), null, ItemStackGenerator.getBeaconBoots(), null,
                                Material.NETHERITE_BLOCK, null);

                createSmallShapedRecipe(teleportAnchorKey, ItemStackGenerator.getTeleportAnchor(), "DAD:BCB:DAD",
                                ItemStackGenerator.getPowerOre(), null, null, null, Material.ENDER_PEARL,
                                Material.RESPAWN_ANCHOR);
        }

        public static void godRecipes(SurvivalSkills plugin) {
                NamespacedKey turtleKey = createGodKey("turtle", plugin, 1);
                NamespacedKey goatKey = createGodKey("goat", plugin, 1);
                NamespacedKey firstAlbumKey = createGodKey("firstalbum", plugin, 2);
                NamespacedKey secondAlbumKey = createGodKey("secondalbum", plugin, 2);
                NamespacedKey musicKnowledgeKey = createGodKey("musicknowledge", plugin, 2);
                NamespacedKey firstSherdKey = createGodKey("firstsherd", plugin, 3);
                NamespacedKey secondSherdKey = createGodKey("secondsherd", plugin, 3);
                NamespacedKey sherdRelicKey = createGodKey("sherdrelic", plugin, 3);
                NamespacedKey firstTrimKey = createGodKey("firsttrim", plugin, 3);
                NamespacedKey secondTrimKey = createGodKey("secondtrim", plugin, 3);
                NamespacedKey trimRelicKey = createGodKey("trimrelic", plugin, 3);
                NamespacedKey warriorEmblemKey = createGodKey("warrioremblem", plugin, 4);

                createSmallShapedRecipe(turtleKey, ItemStackGenerator.getTurtleHelmet(), "AAA:ABA:AAA",
                                null, null, null, Material.TURTLE_SCUTE, Material.TURTLE_EGG, null);
                createShapedRecipe(goatKey, ItemStackGenerator.getGoatHorn(),
                                getGoatHorn(MusicInstrument.CALL_GOAT_HORN),
                                getGoatHorn(MusicInstrument.ADMIRE_GOAT_HORN),
                                getGoatHorn(MusicInstrument.DREAM_GOAT_HORN),
                                getGoatHorn(MusicInstrument.FEEL_GOAT_HORN),
                                getGoatHorn(MusicInstrument.PONDER_GOAT_HORN),
                                getGoatHorn(MusicInstrument.SEEK_GOAT_HORN),
                                getGoatHorn(MusicInstrument.SING_GOAT_HORN),
                                getGoatHorn(MusicInstrument.YEARN_GOAT_HORN), null, true);

                createShapedRecipe(firstAlbumKey, ItemStackGenerator.getFirstAlbum(),
                                new ItemStack(Material.MUSIC_DISC_5),
                                new ItemStack(Material.MUSIC_DISC_11), new ItemStack(Material.MUSIC_DISC_13),
                                new ItemStack(Material.MUSIC_DISC_BLOCKS),
                                new ItemStack(Material.MUSIC_DISC_CAT), new ItemStack(Material.MUSIC_DISC_CHIRP),
                                new ItemStack(Material.MUSIC_DISC_CREATOR),
                                new ItemStack(Material.MUSIC_DISC_CREATOR_MUSIC_BOX),
                                new ItemStack(Material.MUSIC_DISC_FAR), false);
                createShapedRecipe(secondAlbumKey, ItemStackGenerator.getSecondAlbum(),
                                new ItemStack(Material.MUSIC_DISC_MALL),
                                new ItemStack(Material.MUSIC_DISC_MELLOHI),
                                new ItemStack(Material.MUSIC_DISC_OTHERSIDE),
                                new ItemStack(Material.MUSIC_DISC_PIGSTEP),
                                new ItemStack(Material.MUSIC_DISC_RELIC), new ItemStack(Material.MUSIC_DISC_STAL),
                                new ItemStack(Material.MUSIC_DISC_STRAD),
                                new ItemStack(Material.MUSIC_DISC_WAIT), new ItemStack(Material.MUSIC_DISC_WARD),
                                false);
                createSmallShapedRecipe(musicKnowledgeKey, ItemStackGenerator.getMusicKnowledgeDisc(), "DDD:ABC:DDD",
                                ItemStackGenerator.getFirstAlbum(), null, ItemStackGenerator.getSecondAlbum(), null,
                                Material.MUSIC_DISC_PRECIPICE, null);

                createShapedRecipe(firstSherdKey, ItemStackGenerator.getFirstSherd(),
                                new ItemStack(Material.SHEAF_POTTERY_SHERD),
                                new ItemStack(Material.SHELTER_POTTERY_SHERD),
                                new ItemStack(Material.ANGLER_POTTERY_SHERD),
                                new ItemStack(Material.ARCHER_POTTERY_SHERD),
                                new ItemStack(Material.ARMS_UP_POTTERY_SHERD),
                                new ItemStack(Material.BLADE_POTTERY_SHERD),
                                new ItemStack(Material.BREWER_POTTERY_SHERD),
                                new ItemStack(Material.BURN_POTTERY_SHERD), new ItemStack(Material.FLOW_POTTERY_SHERD),
                                false);
                createShapedRecipe(secondSherdKey, ItemStackGenerator.getSecondSherd(),
                                new ItemStack(Material.DANGER_POTTERY_SHERD),
                                new ItemStack(Material.EXPLORER_POTTERY_SHERD),
                                new ItemStack(Material.FRIEND_POTTERY_SHERD),
                                new ItemStack(Material.GUSTER_POTTERY_SHERD),
                                new ItemStack(Material.HEART_POTTERY_SHERD),
                                new ItemStack(Material.HEARTBREAK_POTTERY_SHERD),
                                new ItemStack(Material.HOWL_POTTERY_SHERD),
                                new ItemStack(Material.MINER_POTTERY_SHERD),
                                new ItemStack(Material.MOURNER_POTTERY_SHERD), false);
                createShapedRecipe(sherdRelicKey, ItemStackGenerator.getSherdRelic(), null,
                                new ItemStack(Material.SKULL_POTTERY_SHERD),
                                new ItemStack(Material.PLENTY_POTTERY_SHERD), ItemStackGenerator.getFirstSherd(),
                                new ItemStack(Material.SNORT_POTTERY_SHERD),
                                ItemStackGenerator.getSecondSherd(), new ItemStack(Material.PRIZE_POTTERY_SHERD),
                                new ItemStack(Material.SCRAPE_POTTERY_SHERD),
                                null, true);

                createShapedRecipe(firstTrimKey, ItemStackGenerator.getFirstTrim(),
                                new ItemStack(Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE),
                                new ItemStack(Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE),
                                new ItemStack(Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE),
                                new ItemStack(Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE),
                                new ItemStack(Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE),
                                new ItemStack(Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE),
                                new ItemStack(Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE),
                                new ItemStack(Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE),
                                new ItemStack(Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE), false);
                createShapedRecipe(secondTrimKey, ItemStackGenerator.getSecondTrim(),
                                new ItemStack(Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE),
                                new ItemStack(Material.WILD_ARMOR_TRIM_SMITHING_TEMPLATE),
                                new ItemStack(Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE),
                                new ItemStack(Material.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE),
                                new ItemStack(Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE),
                                new ItemStack(Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE),
                                new ItemStack(Material.VEX_ARMOR_TRIM_SMITHING_TEMPLATE),
                                new ItemStack(Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE),
                                new ItemStack(Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE), false);
                createSmallShapedRecipe(trimRelicKey, ItemStackGenerator.getTrimRelic(), "DDD:ABC:DDD",
                                ItemStackGenerator.getFirstTrim(), null, ItemStackGenerator.getSecondTrim(), null,
                                Material.GOLD_BLOCK, null);

                ShapelessRecipe warriorEmblemRecipe = new ShapelessRecipe(warriorEmblemKey,
                                ItemStackGenerator.getWarriorEmblem());
                warriorEmblemRecipe.addIngredient(Material.NETHERITE_SWORD);
                warriorEmblemRecipe.addIngredient(Material.NETHERITE_HELMET);
                addShapelessRecipe(warriorEmblemRecipe, warriorEmblemKey);
                createShapedRecipe(warriorEmblemKey, ItemStackGenerator.getWarriorEmblem(),
                                new ItemStack(Material.NETHERITE_SWORD),
                                new ItemStack(Material.NETHERITE_HELMET), new ItemStack(Material.NETHERITE_CHESTPLATE),
                                new ItemStack(Material.NETHERITE_LEGGINGS),
                                new ItemStack(Material.NETHERITE_BOOTS), new ItemStack(Material.MACE),
                                new ItemStack(Material.TRIDENT),
                                new ItemStack(Material.CROSSBOW), new ItemStack(Material.TOTEM_OF_UNDYING), false);
        }

        public static ItemStack getGoatHorn(MusicInstrument hornType) {
                ItemStack horn = new ItemStack(Material.GOAT_HORN);
                MusicInstrumentMeta meta = (MusicInstrumentMeta) horn.getItemMeta();
                if (meta == null)
                        return horn;
                meta.setInstrument(hornType);
                horn.setItemMeta(meta);
                return horn;
        }

        public static NamespacedKey createKey(String name, SurvivalSkills plugin) {
                NamespacedKey key = new NamespacedKey(plugin, name);
                plugin.getRecipeKeys().add(key);
                return key;
        }

        public static NamespacedKey createGodKey(String name, SurvivalSkills plugin, int stage) {
                NamespacedKey key = new NamespacedKey(plugin, name);
                plugin.getGodRecipeKeys().put(key, stage);
                return key;
        }

        public static List<Integer> getRecipePositions(int slot) {
                ArrayList<Integer> positions = new ArrayList<>();
                if (slot % 2 == 1) {
                        positions.add(0);
                        positions.add(1);
                        positions.add(2);
                        positions.add(9);
                        positions.add(10);
                        positions.add(11);
                        positions.add(18);
                        positions.add(19);
                        positions.add(20);
                        positions.add(12);
                } else {
                        positions.add(5);
                        positions.add(6);
                        positions.add(7);
                        positions.add(14);
                        positions.add(15);
                        positions.add(16);
                        positions.add(23);
                        positions.add(24);
                        positions.add(25);
                        positions.add(17);
                }
                return positions;
        }

        public static void addShapedRecipe(ShapedRecipe recipe, NamespacedKey key) {
                recipeStack.push(new RecipeRecord(recipe, key));
        }

        public static void addShapelessRecipe(ShapelessRecipe recipe, NamespacedKey key) {
                recipeStack.push(new RecipeRecord(recipe, key));
        }

        public static void emptyRecipeStack(SurvivalSkills plugin) {
                new BukkitRunnable() {
                        @Override
                        public void run() {
                                if (recipeStack.isEmpty()) {
                                        new SkillStatsCommand(SurvivalSkills.getInstance());
                                        cancel();
                                        return;
                                }

                                // Process multiple recipes per tick to reduce total processing time
                                int batchSize = Math.min(10, recipeStack.size()); // Process up to 10 recipes per tick
                                List<RecipeRecord> batch = new ArrayList<>();

                                // Collect batch of recipes
                                for (int i = 0; i < batchSize; i++) {
                                        if (!recipeStack.isEmpty()) {
                                                batch.add(recipeStack.pop());
                                        }
                                }

                                // Process the entire batch
                                for (RecipeRecord recipeRecord : batch) {
                                        NamespacedKey key = recipeRecord.key();
                                        Recipe newRecipe = recipeRecord.recipe();
                                        Recipe existing = Bukkit.getRecipe(key);

                                        // If an identical recipe already exists, skip any work
                                        if (existing != null) {
                                                if (recipesEqual(existing, newRecipe))
                                                        continue;

                                                // If the existing recipe is different, remove it
                                                plugin.getServer().removeRecipe(key);
                                        }

                                        // Add the new/updated recipe
                                        plugin.getServer().addRecipe(newRecipe);
                                }
                        }
                }.runTaskTimer(plugin, 1, 1);
        }

        /**
         * Compares two Bukkit recipes for logical equality (ingredients + result).
         * If types differ it returns false.
         */
        private static boolean recipesEqual(Recipe a, Recipe b) {
                if (a.equals(b))
                        return true;
                if (a == null || b == null)
                        return false;

                // Compare shaped recipes
                if (a instanceof ShapedRecipe sa && b instanceof ShapedRecipe sb) {
                        if (!itemStacksSimilar(sa.getResult(), sb.getResult(), true))
                                return false;
                        String[] shapeA = sa.getShape();
                        String[] shapeB = sb.getShape();
                        if (shapeA.length != shapeB.length)
                                return false;
                        for (int i = 0; i < shapeA.length; i++)
                                if (!Objects.equals(shapeA[i], shapeB[i]))
                                        return false;

                        // Collect characters used (excluding spaces)
                        Set<Character> chars = new HashSet<>();
                        for (String line : shapeA)
                                for (char c : line.toCharArray())
                                        if (c != ' ')
                                                chars.add(c);

                        Map<Character, ItemStack> ingA = sa.getIngredientMap();
                        Map<Character, ItemStack> ingB = sb.getIngredientMap();
                        Map<Character, RecipeChoice> choiceA = sa.getChoiceMap();
                        Map<Character, RecipeChoice> choiceB = sb.getChoiceMap();

                        for (char c : chars) {
                                // Prefer choice maps (ExactChoice / MaterialChoice) if present
                                RecipeChoice ca = choiceA.get(c);
                                RecipeChoice cb = choiceB.get(c);
                                if (ca != null || cb != null) {
                                        if (!recipeChoicesEqual(ca, cb))
                                                return false;
                                        continue;
                                }
                                ItemStack ia = ingA.get(c);
                                ItemStack ib = ingB.get(c);
                                if (!itemStacksSimilar(ia, ib, false))
                                        return false;
                        }
                        return true;
                }

                // Compare shapeless recipes
                if (a instanceof ShapelessRecipe sa && b instanceof ShapelessRecipe sb) {
                        if (!itemStacksSimilar(sa.getResult(), sb.getResult(), true))
                                return false;
                        List<ItemStack> listA = sa.getIngredientList();
                        List<ItemStack> listB = sb.getIngredientList();
                        if (listA.size() != listB.size())
                                return false;

                        // Multiset compare ignoring order
                        List<ItemStack> remaining = new ArrayList<>(listB);
                        outer: for (ItemStack ia : listA) {
                                for (int i = 0; i < remaining.size(); i++) {
                                        if (itemStacksSimilar(ia, remaining.get(i), false)) {
                                                remaining.remove(i);
                                                continue outer;
                                        }
                                }
                                return false; // no match for ingredient
                        }
                        return remaining.isEmpty();
                }

                // Different recipe types (or unhandled types) -> treat as different so
                // replacement occurs
                return false;
        }

        private static boolean recipeChoicesEqual(RecipeChoice a, RecipeChoice b) {
                if (a.equals(b))
                        return true;
                if (a == null || b == null)
                        return false;
                if (!a.getClass().equals(b.getClass()))
                        return false;

                // ExactChoice
                if (a instanceof RecipeChoice.ExactChoice ea && b instanceof RecipeChoice.ExactChoice eb) {
                        List<ItemStack> la = ea.getChoices();
                        List<ItemStack> lb = eb.getChoices();
                        if (la.size() != lb.size())
                                return false;
                        for (int i = 0; i < la.size(); i++)
                                if (!itemStacksSimilar(la.get(i), lb.get(i), false))
                                        return false;
                        return true;
                }

                // MaterialChoice
                if (a instanceof RecipeChoice.MaterialChoice ma && b instanceof RecipeChoice.MaterialChoice mb) {
                        return new HashSet<>(ma.getChoices()).equals(new HashSet<>(mb.getChoices()));
                }

                // Fallback: not equal (covers new choice types)
                return false;
        }

        private static boolean itemStacksSimilar(ItemStack a, ItemStack b, boolean checkAmount) {
                if (a.equals(b))
                        return true;
                if (a == null || b == null)
                        return false;
                if (!a.getType().equals(b.getType()))
                        return false;
                ItemMeta aMeta = a.getItemMeta();
                ItemMeta bMeta = b.getItemMeta();
                if (aMeta == null || bMeta == null)
                        return false;
                if (!aMeta.equals(bMeta))
                        return false;
                return !checkAmount || a.getAmount() == b.getAmount();
        }
}
