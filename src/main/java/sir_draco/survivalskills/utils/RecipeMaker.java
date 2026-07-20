package sir_draco.survivalskills.utils;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.*;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.items.ItemStackBuilder;
import sir_draco.survivalskills.utils.items.ItemStackGenerator;
import sir_draco.survivalskills.utils.Recipes.GodRecipeData;
import sir_draco.survivalskills.utils.Recipes.ArmorUpgradeRecipeData;
import sir_draco.survivalskills.utils.Recipes.ArmorUpgradeRecipeData.ArmorUpgradeRecipe;
import sir_draco.survivalskills.utils.Recipes.RecipeSpec;
import sir_draco.survivalskills.utils.Recipes.RewardRecipeData;
import sir_draco.survivalskills.utils.Recipes.ShapedSpec;
import sir_draco.survivalskills.utils.Recipes.SmallShapedSpec;
import sir_draco.survivalskills.utils.Recipes.TrophyRecipeData;
import sir_draco.survivalskills.utils.Recipes.TrophyRecipeData.TrophyRecipe;
import sir_draco.survivalskills.utils.items.ItemModelData;
import sir_draco.survivalskills.super_enchanting.SuperEnchantingItems;

import java.util.*;

public class RecipeMaker {

        private static final int DENSE_WOOL_MODEL_DATA = ItemModelData.DENSE_WOOL.getId();

        // Enchantments shared by every dense wool item and fragment recipe.
        private static final Map<Enchantment, Integer> KNOCKBACK_ENCHANTS =
                Map.of(Enchantment.KNOCKBACK, 1);

        // Shape characters for the 3x3 grid, indexed by slot position (row-major).
        private static final char[] SHAPE_CHARS = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I'};

        // ── Dense wool color palette ────────────────────────────────────────────
        // A single source of truth shared by the compaction recipes and the
        // black/white fragment recipes.

        private enum DenseWoolColor {
                WHITE(Material.WHITE_WOOL, "White", ChatColor.GRAY),
                GRAY(Material.GRAY_WOOL, "Gray", ChatColor.DARK_GRAY),
                LIGHT_GRAY(Material.LIGHT_GRAY_WOOL, "Light Gray", ChatColor.GRAY),
                BROWN(Material.BROWN_WOOL, "Brown", ChatColor.getByChar("#6E2C00")),
                BLACK(Material.BLACK_WOOL, "Black", ChatColor.BLACK),
                RED(Material.RED_WOOL, "Red", ChatColor.RED),
                ORANGE(Material.ORANGE_WOOL, "Orange", ChatColor.getByChar("#FF8C00")),
                YELLOW(Material.YELLOW_WOOL, "Yellow", ChatColor.YELLOW),
                PINK(Material.PINK_WOOL, "Pink", ChatColor.getByChar("#FF00A2")),
                MAGENTA(Material.MAGENTA_WOOL, "Magenta", ChatColor.LIGHT_PURPLE),
                PURPLE(Material.PURPLE_WOOL, "Purple", ChatColor.DARK_PURPLE),
                BLUE(Material.BLUE_WOOL, "Blue", ChatColor.DARK_BLUE),
                LIGHT_BLUE(Material.LIGHT_BLUE_WOOL, "Light Blue", ChatColor.BLUE),
                CYAN(Material.CYAN_WOOL, "Cyan", ChatColor.getByChar("#009696")),
                GREEN(Material.GREEN_WOOL, "Green", ChatColor.DARK_GREEN),
                LIME(Material.LIME_WOOL, "Lime", ChatColor.GREEN);

                private final Material material;
                private final String displayName;
                private final ChatColor chatColor;

                DenseWoolColor(Material material, String displayName, ChatColor chatColor) {
                        this.material = material;
                        this.displayName = displayName;
                        this.chatColor = chatColor;
                }

                public Material material() { return material; }

                public String displayName() { return displayName; }

                public ChatColor chatColor() { return chatColor; }
        }

        // Full palette used by the compaction recipes (declaration = registration order).
        private static final List<DenseWoolColor> DENSE_WOOL_COLORS = List.of(DenseWoolColor.values());

        // Warm palette combined into the Black Fragment (add-order preserved).
        private static final List<DenseWoolColor> BLACK_FRAGMENT_WOOLS = List.of(
                DenseWoolColor.LIGHT_GRAY, DenseWoolColor.GRAY, DenseWoolColor.BLACK,
                DenseWoolColor.BROWN, DenseWoolColor.RED, DenseWoolColor.ORANGE,
                DenseWoolColor.YELLOW);

        // Cool palette combined into the White Fragment (add-order preserved).
        private static final List<DenseWoolColor> WHITE_FRAGMENT_WOOLS = List.of(
                DenseWoolColor.PINK, DenseWoolColor.MAGENTA, DenseWoolColor.PURPLE,
                DenseWoolColor.BLUE, DenseWoolColor.LIGHT_BLUE, DenseWoolColor.CYAN,
                DenseWoolColor.GREEN, DenseWoolColor.LIME, DenseWoolColor.WHITE);

        // ── Shaped recipe builders ───────────────────────────────────────────

        public static void createSmallShapedRecipe(NamespacedKey key, ItemStack result, String shape, ItemStack as,
                        ItemStack bs, ItemStack cs, Material am, Material bm, Material cm) {
                createSmallShapedRecipe(key, result, shape, as, bs, cs, am, bm, cm, Optional.empty());
        }

        private static void createSmallShapedRecipe(NamespacedKey key, ItemStack result, String shape, ItemStack as,
                        ItemStack bs, ItemStack cs, Material am, Material bm, Material cm,
                        Optional<Character> flexibleExactSlot) {
                ShapedRecipe recipe = new ShapedRecipe(key, result);
                String[] shapes = shape.split(":");

                // DDD is used to indicate an empty row. Bukkit does not handle empty rows without a placeholder
                if (shapes[0].contains("DDD"))
                        recipe.shape(shapes[1], shapes[2]);
                else if (shapes[2].contains("DDD"))
                        recipe.shape(shapes[0], shapes[1]);
                else
                        recipe.shape(shapes[0], shapes[1], shapes[2]);

                setOptionalIngredient(recipe, 'A', as, am, flexibleExactSlot.filter(slot -> slot == 'A').isPresent());
                setOptionalIngredient(recipe, 'B', bs, bm, flexibleExactSlot.filter(slot -> slot == 'B').isPresent());
                setOptionalIngredient(recipe, 'C', cs, cm, flexibleExactSlot.filter(slot -> slot == 'C').isPresent());

                RecipeRegistrar.addShapedRecipe(recipe, key);
        }

        public static void createShapedRecipe(NamespacedKey key, ItemStack result, ItemStack a, ItemStack b,
                        ItemStack c, ItemStack d, ItemStack e, ItemStack f, ItemStack g, ItemStack h, ItemStack i,
                        boolean unique) {
                ShapedRecipe recipe = new ShapedRecipe(key, result);
                ItemStack[] items = {a, b, c, d, e, f, g, h, i};
                recipe.shape(buildShape(items));
                for (int idx = 0; idx < items.length; idx++) {
                        if (items[idx] == null)
                                continue;
                        if (unique)
                                recipe.setIngredient(SHAPE_CHARS[idx], new RecipeChoice.ExactChoice(items[idx]));
                        else
                                recipe.setIngredient(SHAPE_CHARS[idx], items[idx].getType());
                }
                RecipeRegistrar.addShapedRecipe(recipe, key);
        }

        // ── Dense wool helpers ───────────────────────────────────────────────

        private static ItemStack buildDenseWoolBundle(DenseWoolColor color) {
                return new ItemStackBuilder(color.material(), 1,
                                color.chatColor() + "Bundle Of Dense " + color.displayName() + " Wool")
                                .lore(List.of("The sheep are naked")).modelData(DENSE_WOOL_MODEL_DATA)
                                .hideEnchants(true).enchants(KNOCKBACK_ENCHANTS).build();
        }

        private static void addDenseWoolBundleIngredients(ShapelessRecipe recipe, List<DenseWoolColor> colors) {
                for (DenseWoolColor color : colors)
                        recipe.addIngredient(new RecipeChoice.ExactChoice(buildDenseWoolBundle(color)));
        }

        // ── Trophy recipes ───────────────────────────────────────────────────

        private static void registerShapelessTrophy(NamespacedKey key, TrophyRecipe recipe,
                        Map<Integer, ItemStack> trophyItems) {
                ItemStack trophy = ItemStackGenerator.getTrophyItem(recipe.icon(), recipe.displayName(), recipe.lore());
                trophyItems.put(recipe.id(), trophy);
                ShapelessRecipe shapeless = new ShapelessRecipe(key, trophy);
                for (Material ingredient : recipe.ingredients())
                        shapeless.addIngredient(ingredient);
                if (recipe.exactChoiceIngredient() != null)
                        shapeless.addIngredient(new RecipeChoice.ExactChoice(recipe.exactChoiceIngredient()));
                RecipeRegistrar.addShapelessRecipe(shapeless, key);
        }

        /**
         * Creates recipes for all the trophies.
         */
        public static void trophyRecipes(SurvivalSkills plugin) {
                // Keys are created in display order to keep plugin recipeKeys ordered.
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

                Map<Integer, ItemStack> trophyItems = plugin.getTrophyManager().getTrophyItems();
                denseWoolRecipes(plugin);

                // Standard shapeless trophies (cave, forest, farming, ocean, fishing, nether, end)
                Map<Integer, NamespacedKey> shapelessTrophyKeys = Map.of(
                        1, caveKey, 2, forestKey, 3, farmingKey, 4, oceanKey,
                        5, fishingKey, 7, netherKey, 8, endKey);
                for (TrophyRecipe recipe : TrophyRecipeData.TROPHY_RECIPES)
                        registerShapelessTrophy(shapelessTrophyKeys.get(recipe.id()), recipe, trophyItems);

                // Fragment recipes produce the ingredients consumed by the Color Trophy.
                ItemStack blackFragment = registerBlackFragment(blackKey);
                ItemStack whiteFragment = registerWhiteFragment(whiteKey);
                registerColorTrophy(colorKey, blackFragment, whiteFragment, trophyItems);
                registerChampionTrophy(championKey, trophyItems);
                registerGodTrophy(godKey, trophyItems);
        }

        private static ItemStack registerBlackFragment(NamespacedKey key) {
                String name = ChatColor.BLACK + ChatColor.BOLD.toString() + "Black Fragment";
                String lore = ChatColor.DARK_GRAY + "~You merely adopted the dark, I was born in it~";
                ItemStack fragment = new ItemStackBuilder(Material.BLACK_WOOL, 1, ChatColor.DARK_GRAY + name)
                                .lore(List.of(lore)).modelData(DENSE_WOOL_MODEL_DATA).hideEnchants(true)
                                .enchants(KNOCKBACK_ENCHANTS).build();
                ShapelessRecipe recipe = new ShapelessRecipe(key, fragment);
                addDenseWoolBundleIngredients(recipe, BLACK_FRAGMENT_WOOLS);
                RecipeRegistrar.addShapelessRecipe(recipe, key);
                return fragment;
        }

        private static ItemStack registerWhiteFragment(NamespacedKey key) {
                String name = ChatColor.WHITE + ChatColor.BOLD.toString() + "White Fragment";
                String lore = ChatColor.WHITE + "~All colors become one~";
                ItemStack fragment = new ItemStackBuilder(Material.WHITE_WOOL, 1, ChatColor.WHITE + name)
                                .lore(List.of(lore)).modelData(DENSE_WOOL_MODEL_DATA).hideEnchants(true)
                                .enchants(KNOCKBACK_ENCHANTS).build();
                ShapelessRecipe recipe = new ShapelessRecipe(key, fragment);
                addDenseWoolBundleIngredients(recipe, WHITE_FRAGMENT_WOOLS);
                RecipeRegistrar.addShapelessRecipe(recipe, key);
                return fragment;
        }

        private static void registerColorTrophy(NamespacedKey key, ItemStack blackFragment, ItemStack whiteFragment,
                        Map<Integer, ItemStack> trophyItems) {
                List<List<String>> nameColors = new ArrayList<>();
                nameColors.add(ColorParser.generateGradient("#FF0000", "#00FF00", 4));
                nameColors.add(ColorParser.generateGradient("#00FF00", "#0000FF", 4));
                nameColors.add(ColorParser.generateGradient("#0000FF", "#FF00FF", 4));
                String name = ColorParser.colorizeString("Color Trophy", ColorParser.gradientConnector(nameColors), true);
                List<List<String>> loreColors = new ArrayList<>();
                loreColors.add(ColorParser.generateGradient("#FF0000", "#00FF00", 10));
                loreColors.add(ColorParser.generateGradient("#00FF00", "#0000FF", 10));
                loreColors.add(ColorParser.generateGradient("#0000FF", "#FF00FF", 10));
                loreColors.add(ColorParser.generateGradient("#FF00FF", "#FF0000", 9));
                String lore = ColorParser.colorizeString("~Rainbows follow you wherever you walk~",
                                ColorParser.gradientConnector(loreColors), false);
                ItemStack colorTrophy = ItemStackGenerator.getTrophyItem(Material.SHEARS, name, lore);
                trophyItems.put(6, colorTrophy);

                ShapelessRecipe recipe = new ShapelessRecipe(key, colorTrophy);
                recipe.addIngredient(new RecipeChoice.ExactChoice(whiteFragment));
                recipe.addIngredient(new RecipeChoice.ExactChoice(blackFragment));
                recipe.addIngredient(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
                RecipeRegistrar.addShapelessRecipe(recipe, key);
        }

        private static void registerChampionTrophy(NamespacedKey key, Map<Integer, ItemStack> trophyItems) {
                String name = ColorParser.colorizeString("Champion Trophy",
                                ColorParser.generateGradient("#FF0000", "#FFE200", 15), true);
                String lore = ColorParser.colorizeString("~The realm of the gods is nearby~",
                                ColorParser.generateGradient("#FFE200", "#FF0000", 33), false);
                ItemStack championTrophy = ItemStackGenerator.getTrophyItem(Material.DIAMOND_SWORD, name, lore);
                trophyItems.put(9, championTrophy);
                ShapelessRecipe recipe = new ShapelessRecipe(key, championTrophy);
                recipe.addIngredient(Material.NETHER_STAR);
                recipe.addIngredient(new RecipeChoice.ExactChoice(ItemStackGenerator.getEnderDragonBossItem()));
                recipe.addIngredient(new RecipeChoice.ExactChoice(ItemStackGenerator.getGiantBossItem()));
                recipe.addIngredient(new RecipeChoice.ExactChoice(ItemStackGenerator.getBroodMotherBossItem()));
                recipe.addIngredient(new RecipeChoice.ExactChoice(ItemStackGenerator.getElderGuardianBossItem()));
                recipe.addIngredient(new RecipeChoice.ExactChoice(ItemStackGenerator.getWardenBossItem()));
                recipe.addIngredient(new RecipeChoice.ExactChoice(ItemStackGenerator.getVillagerBossItem()));
                RecipeRegistrar.addShapelessRecipe(recipe, key);
        }

        private static void registerGodTrophy(NamespacedKey key, Map<Integer, ItemStack> trophyItems) {
                String name = ColorParser.colorizeString("God Trophy",
                                ColorParser.generateGradient("#FFFF00", "#FFFFFF", 10), true);
                String lore = ColorParser.colorizeString("~There is nothing you can not do~",
                                ColorParser.generateGradient("#FFFFFF", "#FFFF00", 33), false);
                ItemStack godTrophy = ItemStackGenerator.getTrophyItem(Material.GRASS_BLOCK, name, lore);
                createSmallShapedRecipe(key, godTrophy, "DAD:ABA:DAD",
                                ItemStackGenerator.getPowerOre(), ItemStackGenerator.getGodTrophyBase(), null, null,
                                null, null);
                trophyItems.put(10, godTrophy);
        }

        public static void denseWoolRecipes(SurvivalSkills plugin) {
                for (DenseWoolColor color : DENSE_WOOL_COLORS)
                        denseWoolRecipesHelper(plugin, color);
        }

        private static void denseWoolRecipesHelper(SurvivalSkills plugin, DenseWoolColor color) {
                String tag = color.displayName().toLowerCase().replaceAll("\\s", "");
                ItemStack wool1 = new ItemStackBuilder(color.material(), 1, "Compacted " + color.displayName() + " Wool")
                                .lore(List.of("Very soft")).modelData(DENSE_WOOL_MODEL_DATA).hideEnchants(true)
                                .enchants(KNOCKBACK_ENCHANTS).build();
                ItemStack wool2 = color == DenseWoolColor.WHITE
                                ? ItemStackGenerator.getDenseWhiteWool()
                                : new ItemStackBuilder(color.material(), 1,
                                                color.chatColor() + "Dense " + color.displayName() + " Wool")
                                                .lore(List.of("Not very soft")).modelData(DENSE_WOOL_MODEL_DATA)
                                                .hideEnchants(true).enchants(KNOCKBACK_ENCHANTS).build();
                ItemStack wool3 = buildDenseWoolBundle(color);
                NamespacedKey tag1 = makeRecipeWithSingleIngredient(plugin, new ItemStack(color.material()), wool1, tag + "1");
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
                RecipeRegistrar.addShapedRecipe(recipe, key);
                return key;
        }

        // ── Reward recipes ───────────────────────────────────────────────────

        public static void rewardRecipes(SurvivalSkills plugin) {
                for (SmallShapedSpec spec : RewardRecipeData.ALL) {
                        Optional<Character> flexibleExactSlot = ArmorUpgradeRecipeData.find(spec.keyName())
                                        .map((ArmorUpgradeRecipe upgrade) -> upgrade.ingredientSlot());
                        createSmallShapedRecipe(createKey(spec.keyName(), plugin), spec.result(), spec.shape(),
                                        spec.exactA(), spec.exactB(), spec.exactC(),
                                        spec.matA(), spec.matB(), spec.matC(), flexibleExactSlot);
                }
        }

        public static void pipeRecipes(SurvivalSkills plugin) {
                NamespacedKey wrenchKey = createKey("pipe_wrench", plugin);
                ShapedRecipe wrench = new ShapedRecipe(wrenchKey, ItemStackGenerator.getWrench());
                wrench.shape("III", " R ", " D ");
                wrench.setIngredient('I', Material.IRON_INGOT);
                wrench.setIngredient('R', Material.REDSTONE_BLOCK);
                wrench.setIngredient('D', Material.DIAMOND);
                RecipeRegistrar.addShapedRecipe(wrench, wrenchKey);

                NamespacedKey pipeKey = createKey("transfer_pipe", plugin);
                ItemStack result = ItemStackGenerator.getTransferPipe();
                result.setAmount(2);
                ShapedRecipe pipe = new ShapedRecipe(pipeKey, result);
                pipe.shape("GGG", "CRC", "GGG");
                pipe.setIngredient('G', Material.GLASS);
                pipe.setIngredient('C', Material.COPPER_INGOT);
                pipe.setIngredient('R', Material.REDSTONE_BLOCK);
                RecipeRegistrar.addShapedRecipe(pipe, pipeKey);
        }

        public static void superEnchantingRecipe(SurvivalSkills plugin) {
                NamespacedKey key = createKey("super_enchanting_table", plugin);
                ShapedRecipe recipe = new ShapedRecipe(key,
                                SuperEnchantingItems.createSuperEnchantingTable(plugin));
                recipe.shape(" F ", "SES", " S ");
                recipe.setIngredient('S', Material.SCULK);
                recipe.setIngredient('F', new RecipeChoice.ExactChoice(
                                SuperEnchantingItems.createTrialFragment(plugin, 1)));
                recipe.setIngredient('E', Material.ENCHANTING_TABLE);
                RecipeRegistrar.addShapedRecipe(recipe, key);
        }

        // ── God recipes ──────────────────────────────────────────────────────

        public static void godRecipes(SurvivalSkills plugin) {
                for (GodRecipeData.Entry entry : GodRecipeData.ALL) {
                        RecipeSpec spec = entry.spec();
                        NamespacedKey key = createGodKey(spec.keyName(), plugin, entry.stage());
                        switch (spec) {
                        case SmallShapedSpec s -> createSmallShapedRecipe(key, s.result(), s.shape(),
                                        s.exactA(), s.exactB(), s.exactC(),
                                        s.matA(), s.matB(), s.matC());
                        case ShapedSpec s -> createShapedRecipe(key, s.result(), s.a(), s.b(), s.c(), s.d(),
                                        s.e(), s.f(), s.g(), s.h(), s.i(), s.unique());
                        }
                }
        }

        // ── Key factories ────────────────────────────────────────────────────

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

        // ── Shared building blocks ──────────────────────────────────────────

        private static void setOptionalIngredient(ShapedRecipe recipe, char slot, ItemStack exactChoice,
                        Material material, boolean flexibleExactChoice) {
                if (exactChoice != null) {
                        if (flexibleExactChoice)
                                recipe.setIngredient(slot, exactChoice.getType());
                        else
                                recipe.setIngredient(slot, new RecipeChoice.ExactChoice(exactChoice));
                }
                else if (material != null)
                        recipe.setIngredient(slot, material);
        }

        private static String[] buildShape(ItemStack[] items) {
                String[] rows = new String[3];
                for (int row = 0; row < 3; row++) {
                        StringBuilder sb = new StringBuilder(3);
                        for (int col = 0; col < 3; col++) {
                                int idx = row * 3 + col;
                                sb.append(items[idx] == null ? ' ' : SHAPE_CHARS[idx]);
                        }
                        rows[row] = sb.toString();
                }
                return rows;
        }
}
