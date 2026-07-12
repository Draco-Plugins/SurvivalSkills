package sir_draco.survivalskills.utils.Recipes;

import org.bukkit.Material;

import org.bukkit.inventory.ItemStack;
import sir_draco.survivalskills.utils.items.ItemStackGenerator;

import java.util.List;

/**
 * Declarative recipe definitions for the reward item suite. Each entry maps to
 * one {@link RecipeMaker#createSmallShapedRecipe} call. Entries are listed in
 * registration order so the runtime loop preserves both key-creation order and
 * recipe-stack push order.
 */
public final class RewardRecipeData {

    private RewardRecipeData() {}

    public static final List<SmallShapedSpec> ALL = List.of(
            new SmallShapedSpec("torch", ItemStackGenerator.getUnlimitedTorch(), "ABA:BCB:ABA",
                    null, null, null, Material.LAVA_BUCKET, Material.COAL_BLOCK, Material.TORCH),
            new SmallShapedSpec("bronze", ItemStackGenerator.getBronzeIngot(), "AAA:ABA:AAA",
                    null, null, null, Material.COPPER_BLOCK, Material.GOLD_BLOCK, null),
            new SmallShapedSpec("zapwand", ItemStackGenerator.getZapWand(), "DDD:AAA:DDD",
                    ItemStackGenerator.getBronzeIngot(), null, null, null, null, null),

            new SmallShapedSpec("minehelmet", ItemStackGenerator.getMiningHelmet(), "ABA:C C:DDD",
                    ItemStackGenerator.getFireResistancePotion(), null, null, null,
                    Material.LEATHER_HELMET, Material.DIAMOND_BLOCK),
            new SmallShapedSpec("minechestplate", ItemStackGenerator.getMiningChestplate(),
                    "C C:CBC:CAC", null, null, null, Material.CAKE, Material.LEATHER_CHESTPLATE,
                    Material.DIAMOND_BLOCK),
            new SmallShapedSpec("mineleggings", ItemStackGenerator.getMiningLeggings(),
                    "CBC:CAC:C C", null, null, null, Material.CAKE, Material.LEATHER_LEGGINGS,
                    Material.DIAMOND_BLOCK),
            new SmallShapedSpec("mineboots", ItemStackGenerator.getMiningBoots(), "DDD:ABA:C C",
                    ItemStackGenerator.getFireResistancePotion(), null, null, null,
                    Material.LEATHER_BOOTS, Material.DIAMOND_BLOCK),

            new SmallShapedSpec("beaconhelmet", ItemStackGenerator.getBeaconHelmet(), "AAA:ABA:DDD",
                    null, null, null, Material.BEACON, Material.NETHERITE_HELMET, null),
            new SmallShapedSpec("beaconchestplate", ItemStackGenerator.getBeaconChestplate(),
                    "ABA:AAA:AAA", null, null, null, Material.BEACON,
                    Material.NETHERITE_CHESTPLATE, null),
            new SmallShapedSpec("beaconleggings", ItemStackGenerator.getBeaconLeggings(),
                    "AAA:ABA:A A", null, null, null, Material.BEACON,
                    Material.NETHERITE_LEGGINGS, null),
            new SmallShapedSpec("beaconboots", ItemStackGenerator.getBeaconBoots(), "DDD:ABA:A A",
                    null, null, null, Material.BEACON, Material.NETHERITE_BOOTS, null),

            new SmallShapedSpec("jumpboots", ItemStackGenerator.getJumpingBoots(), "ABA:C C:DDD",
                    ItemStackGenerator.getJumpPowerPotion(), null, null, null, Material.IRON_BOOTS,
                    Material.SLIME_BLOCK),

            new SmallShapedSpec("wandererhelmet", ItemStackGenerator.getWandererHelmet(),
                    "BAB:B B:DDD", ItemStackGenerator.getSpeedPotion(), null, null, null,
                    Material.IRON_CHAIN, null),
            new SmallShapedSpec("wandererchestplate", ItemStackGenerator.getWandererChestplate(),
                    "B B:BAB:BBB", ItemStackGenerator.getSpeedPotion(), null, null, null,
                    Material.IRON_CHAIN, null),
            new SmallShapedSpec("wandererleggings", ItemStackGenerator.getWandererLeggings(),
                    "BAB:B B:B B", ItemStackGenerator.getSpeedPotion(), null, null, null,
                    Material.IRON_CHAIN, null),
            new SmallShapedSpec("wandererboots", ItemStackGenerator.getWandererBoots(),
                    "DDD:B B:BAB", ItemStackGenerator.getSpeedPotion(), null, null, null,
                    Material.IRON_CHAIN, null),

            new SmallShapedSpec("travelerhelmet", ItemStackGenerator.getTravelerHelmet(),
                    "BAB:B B:DDD", ItemStackGenerator.getWandererHelmet(), null, null, null,
                    Material.DIAMOND, null),
            new SmallShapedSpec("travelerchestplate", ItemStackGenerator.getTravelerChestplate(),
                    "B B:BAB:BBB", ItemStackGenerator.getWandererChestplate(), null, null, null,
                    Material.DIAMOND, null),
            new SmallShapedSpec("travelerleggings", ItemStackGenerator.getTravelerLeggings(),
                    "BAB:B B:B B", ItemStackGenerator.getWandererLeggings(), null, null, null,
                    Material.DIAMOND, null),
            new SmallShapedSpec("travelerboots", ItemStackGenerator.getTravelerBoots(),
                    "DDD:BAB:B B", ItemStackGenerator.getWandererBoots(), null, null, null,
                    Material.DIAMOND, null),

            new SmallShapedSpec("hardnautilusshell", ItemStackGenerator.getHardNautilusShell(),
                    "AAA:AAA:AAA", null, null, null, Material.NAUTILUS_SHELL, null, null),
            new SmallShapedSpec("hardheartofthesea", ItemStackGenerator.getHardHeartOfTheSea(),
                    "AAA:AAA:AAA", null, null, null, Material.HEART_OF_THE_SEA, null, null),

            new SmallShapedSpec("gillhelmet", ItemStackGenerator.getGillHelmet(), "AAA:B B:DDD",
                    ItemStackGenerator.getHardNautilusShell(),
                    ItemStackGenerator.getHardHeartOfTheSea(), null, null, null, null),
            new SmallShapedSpec("gillchestplate", ItemStackGenerator.getGillChestplate(),
                    "A A:BBB:AAA", ItemStackGenerator.getHardNautilusShell(),
                    ItemStackGenerator.getHardHeartOfTheSea(), null, null, null, null),
            new SmallShapedSpec("gillleggings", ItemStackGenerator.getGillLeggings(),
                    "AAA:A A:B B", ItemStackGenerator.getHardNautilusShell(),
                    ItemStackGenerator.getHardHeartOfTheSea(), null, null, null, null),
            new SmallShapedSpec("gillboots", ItemStackGenerator.getGillBoots(), "DDD:A A:B B",
                    ItemStackGenerator.getHardNautilusShell(),
                    ItemStackGenerator.getHardHeartOfTheSea(), null, null, null, null),

            new SmallShapedSpec("adventurerhelmet", ItemStackGenerator.getAdventurerHelmet(),
                    "BAB:C C:DDD", ItemStackGenerator.getTravelerHelmet(), null, null, null,
                    Material.NETHERITE_INGOT, Material.FEATHER),
            new SmallShapedSpec("adventurerchestplate", ItemStackGenerator.getAdventurerChestplate(),
                    "C C:BAB:CCC", ItemStackGenerator.getTravelerChestplate(), null, null, null,
                    Material.NETHERITE_INGOT, Material.FEATHER),
            new SmallShapedSpec("adventurerleggings", ItemStackGenerator.getAdventurerLeggings(),
                    "BAB:C C:C C", ItemStackGenerator.getTravelerLeggings(), null, null, null,
                    Material.NETHERITE_INGOT, Material.FEATHER),
            new SmallShapedSpec("adventurerboots", ItemStackGenerator.getAdventurerBoots(),
                    "DDD:BAB:C C", ItemStackGenerator.getTravelerBoots(), null, null, null,
                    Material.NETHERITE_INGOT, Material.FEATHER),

            new SmallShapedSpec("cavefinder", ItemStackGenerator.getCaveFinder(), "AAA:ABA:AAA",
                    null, null, null, Material.REDSTONE_BLOCK, Material.COMPASS, null),
            new SmallShapedSpec("wateringcan", ItemStackGenerator.getWateringCan(), "ABA:ACA:AAA",
                    null, null, null, Material.LAPIS_BLOCK, Material.NAUTILUS_SHELL,
                    Material.WATER_BUCKET),
            new SmallShapedSpec("unlimitedbonemeal", ItemStackGenerator.getUnlimitedBoneMeal(),
                    "AAA:ABA:AAA", null, null, null, Material.BONE_BLOCK, Material.GOLDEN_APPLE,
                    null),
            new SmallShapedSpec("harvester", ItemStackGenerator.getHarvester(), "ABA:BCB:ABA",
                    null, null, null, Material.REDSTONE_BLOCK, Material.WHEAT_SEEDS,
                    Material.NETHERITE_HOE),

            new SmallShapedSpec("giantboss", ItemStackGenerator.getGiantSummoner(), "AAA:ABA:AAA",
                    null, null, null, Material.ROTTEN_FLESH, Material.EGG, null),
            new SmallShapedSpec("broodmotherboss", ItemStackGenerator.getBroodMotherSummoner(),
                    "AAA:CBC:AAA", null, null, null, Material.STRING, Material.EGG,
                    Material.SPIDER_EYE),
            new SmallShapedSpec("villagerboss", ItemStackGenerator.getVillagerSummoner(),
                    "AAA:CBC:AAA", null, null, null, Material.EMERALD_BLOCK, Material.EGG,
                    Material.TOTEM_OF_UNDYING),

            new SmallShapedSpec("gapple", new ItemStack(Material.ENCHANTED_GOLDEN_APPLE),
                    "AAA:ABA:AAA", null, null, null, Material.GOLD_BLOCK, Material.GOLDEN_APPLE,
                    null),
            new SmallShapedSpec("fireworkcannon", ItemStackGenerator.getFireworkCannon(),
                    "AAA:ABA:ACA", null, null, null, Material.FIREWORK_ROCKET, Material.CAMPFIRE,
                    Material.BLAZE_POWDER),
            new SmallShapedSpec("sortwand", ItemStackGenerator.getSortWand(), "CAC:ABA:CAC",
                    null, null, null, Material.COMPARATOR, Material.BLAZE_ROD, Material.CHEST),
            new SmallShapedSpec("magnet", ItemStackGenerator.getMagnet(), "ABA:BCB:ABA",
                    null, null, null, Material.IRON_BLOCK, Material.REDSTONE_BLOCK,
                    Material.COPPER_BLOCK),

            new SmallShapedSpec("powersword", ItemStackGenerator.getPowerSword(), "DAD:BAB:DCD",
                    ItemStackGenerator.getPowerOre(), ItemStackGenerator.getZapWand(),
                    ItemStackGenerator.getBroodingSilk(), null, null, null),
            new SmallShapedSpec("powerdrill", ItemStackGenerator.getPowerDrill(), "DAD:ABA:DBD",
                    ItemStackGenerator.getPowerOre(), null, null, null, Material.BEACON, null),
            new SmallShapedSpec("powerlaser", ItemStackGenerator.getPowerLaser(), "DAD:ABA:DAD",
                    ItemStackGenerator.getPowerOre(), null, null, null, Material.END_CRYSTAL, null),
            new SmallShapedSpec("powerhelmet", ItemStackGenerator.getPowerHelmet(), "AAA:BCB:DDD",
                    ItemStackGenerator.getPowerOre(), null, ItemStackGenerator.getBeaconHelmet(),
                    null, Material.NETHERITE_BLOCK, null),
            new SmallShapedSpec("powerchestplate", ItemStackGenerator.getPowerChestplate(),
                    "BDB:ACA:AAA", ItemStackGenerator.getPowerOre(), null,
                    ItemStackGenerator.getBeaconChestplate(), null, Material.NETHERITE_BLOCK, null),
            new SmallShapedSpec("powerleggings", ItemStackGenerator.getPowerLeggings(),
                    "BCB:ADA:ADA", ItemStackGenerator.getPowerOre(), null,
                    ItemStackGenerator.getBeaconLeggings(), null, Material.NETHERITE_BLOCK, null),
            new SmallShapedSpec("powerboots", ItemStackGenerator.getPowerBoots(), "DDD:BCB:ADA",
                    ItemStackGenerator.getPowerOre(), null, ItemStackGenerator.getBeaconBoots(),
                    null, Material.NETHERITE_BLOCK, null),

            new SmallShapedSpec("teleportanchor", ItemStackGenerator.getTeleportAnchor(),
                    "DAD:BCB:DAD", ItemStackGenerator.getPowerOre(), null, null, null,
                    Material.ENDER_PEARL, Material.RESPAWN_ANCHOR));
}