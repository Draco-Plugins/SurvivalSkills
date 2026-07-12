package sir_draco.survivalskills.utils.Recipes;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import sir_draco.survivalskills.utils.ColorParser;
import sir_draco.survivalskills.utils.items.ItemStackGenerator;

import java.util.List;

/**
 * Declarative specs for the simple shapeless trophy recipes. Each entry pairs
 * a trophy id (used to store/display the trophy) with an icon, display name,
 * lore line, and a fixed set of ingredients (plus an optional ExactChoice
 * ingredient such as a boss drop).
 */
public final class TrophyRecipeData {

    private TrophyRecipeData() {}

    public record TrophyRecipe(int id, Material icon, String displayName, String lore,
            List<Material> ingredients, ItemStack exactChoiceIngredient) {}

    public static final List<TrophyRecipe> TROPHY_RECIPES = List.of(
            new TrophyRecipe(1, Material.DIAMOND_PICKAXE,
                    ChatColor.GRAY + ChatColor.BOLD.toString() + "Cave Trophy",
                    ChatColor.GRAY + "~You have made the caves your domain~",
                    List.of(Material.COAL_BLOCK, Material.IRON_BLOCK, Material.REDSTONE_BLOCK,
                            Material.GOLD_BLOCK, Material.DIAMOND_BLOCK, Material.LAPIS_BLOCK,
                            Material.AMETHYST_BLOCK, Material.EMERALD_BLOCK, Material.COPPER_BLOCK),
                    null),
            new TrophyRecipe(2, Material.OAK_SAPLING,
                    ChatColor.GREEN + ChatColor.BOLD.toString() + "Forest Trophy",
                    ChatColor.DARK_GREEN + "~You have made the forests your domain~",
                    List.of(Material.OAK_LOG, Material.BIRCH_LOG, Material.ACACIA_LOG,
                            Material.CHERRY_LOG, Material.SPRUCE_LOG, Material.MANGROVE_LOG,
                            Material.DARK_OAK_LOG, Material.JUNGLE_LOG, Material.PALE_OAK_LOG),
                    null),
            new TrophyRecipe(3, Material.GOLDEN_CARROT,
                    ChatColor.GOLD + ChatColor.BOLD.toString() + "Farming Trophy",
                    ChatColor.YELLOW + "~Crops live and die based on your will~",
                    List.of(Material.POTATO, Material.PUMPKIN_PIE, Material.SWEET_BERRIES,
                            Material.CARROT, Material.BEETROOT, Material.BREAD, Material.COOKIE,
                            Material.MELON_SLICE, Material.CAKE),
                    null),
            new TrophyRecipe(4, Material.TRIDENT,
                    ChatColor.DARK_BLUE + ChatColor.BOLD.toString() + "Ocean Trophy",
                    ChatColor.BLUE + "~The waves are pulled towards you as you walk by~",
                    List.of(Material.DRIED_KELP_BLOCK, Material.HEART_OF_THE_SEA,
                            Material.FIRE_CORAL_BLOCK, Material.SEA_LANTERN, Material.TURTLE_EGG,
                            Material.SPONGE, Material.PUFFERFISH_BUCKET, Material.TRIDENT,
                            Material.SEA_PICKLE),
                    null),
            new TrophyRecipe(5, Material.FISHING_ROD,
                    ChatColor.DARK_AQUA + ChatColor.BOLD.toString() + "Fishing Trophy",
                    ChatColor.AQUA + "~Fish swim towards your hook out of respect~",
                    List.of(Material.INK_SAC, Material.COD, Material.NAUTILUS_SHELL,
                            Material.PUFFERFISH, Material.FISHING_ROD, Material.SALMON,
                            Material.TROPICAL_FISH, Material.LILY_PAD),
                    ItemStackGenerator.getFishingBossItem()),
            new TrophyRecipe(7, Material.NETHERRACK,
                    ChatColor.DARK_RED + ChatColor.BOLD.toString() + "Nether Trophy",
                    ChatColor.RED + "~The fires of hell feel cold on your skin~",
                    List.of(Material.BLACKSTONE, Material.CRIMSON_STEM, Material.WARPED_STEM,
                            Material.NETHER_BRICKS, Material.NETHERITE_BLOCK, Material.NETHERRACK,
                            Material.QUARTZ, Material.NETHER_WART, Material.BLAZE_ROD),
                    null),
            new TrophyRecipe(8, Material.END_STONE,
                    ColorParser.colorizeString("End Trophy",
                            ColorParser.generateGradient("#9600FF", "#C800FF", 10), true),
                    ColorParser.colorizeString("~Space warps around your fingers~",
                            ColorParser.generateGradient("#C800FF", "#9600FF", 33), false),
                    List.of(Material.END_ROD, Material.DRAGON_HEAD, Material.CHORUS_FRUIT,
                            Material.ELYTRA, Material.END_CRYSTAL, Material.ENDER_PEARL,
                            Material.END_STONE, Material.SHULKER_BOX, Material.CHORUS_FLOWER),
                    null));
}