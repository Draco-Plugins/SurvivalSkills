package sir_draco.survivalskills.utils.Recipes;

import org.bukkit.Material;
import org.bukkit.MusicInstrument;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import sir_draco.survivalskills.utils.items.ItemStackGenerator;

import java.util.Arrays;
import java.util.List;

/**
 * Declarative recipe definitions for the god-questline item suite. Entries pair
 * a god-quest {@code stage} (1-4) with a {@link RecipeSpec} and are listed in
 * registration order so the runtime loop preserves both {@code createGodKey}
 * ordering and recipe-stack push order.
 */
public final class GodRecipeData {

    private GodRecipeData() {}

    /** Pairs a god-quest stage with its declarative recipe spec. */
    public record Entry(int stage, RecipeSpec spec) {}

    public static final List<Entry> ALL = List.of(
            new Entry(1, new SmallShapedSpec("turtle", ItemStackGenerator.getTurtleHelmet(),
                    "AAA:ABA:AAA", null, null, null, Material.TURTLE_SCUTE, Material.TURTLE_EGG,
                    null)),
            new Entry(1, shapelessExactRecipe("goat", ItemStackGenerator.getGoatHorn(),
                    getGoatHorn(MusicInstrument.CALL_GOAT_HORN),
                    getGoatHorn(MusicInstrument.ADMIRE_GOAT_HORN),
                    getGoatHorn(MusicInstrument.DREAM_GOAT_HORN),
                    getGoatHorn(MusicInstrument.FEEL_GOAT_HORN),
                    getGoatHorn(MusicInstrument.PONDER_GOAT_HORN),
                    getGoatHorn(MusicInstrument.SEEK_GOAT_HORN),
                    getGoatHorn(MusicInstrument.SING_GOAT_HORN),
                    getGoatHorn(MusicInstrument.YEARN_GOAT_HORN))),

            new Entry(2, shapelessMaterialRecipe("firstalbum", ItemStackGenerator.getFirstAlbum(),
                    Material.MUSIC_DISC_5, Material.MUSIC_DISC_11, Material.MUSIC_DISC_13,
                    Material.MUSIC_DISC_BLOCKS, Material.MUSIC_DISC_CAT, Material.MUSIC_DISC_CHIRP,
                    Material.MUSIC_DISC_CREATOR, Material.MUSIC_DISC_CREATOR_MUSIC_BOX,
                    Material.MUSIC_DISC_FAR)),
            new Entry(2, shapelessMaterialRecipe("secondalbum", ItemStackGenerator.getSecondAlbum(),
                    Material.MUSIC_DISC_MALL, Material.MUSIC_DISC_MELLOHI,
                    Material.MUSIC_DISC_OTHERSIDE, Material.MUSIC_DISC_PIGSTEP,
                    Material.MUSIC_DISC_RELIC, Material.MUSIC_DISC_STAL, Material.MUSIC_DISC_STRAD,
                    Material.MUSIC_DISC_WAIT, Material.MUSIC_DISC_WARD)),
            new Entry(2, shapelessRecipe("musicknowledge", ItemStackGenerator.getMusicKnowledgeDisc(),
                    exact(ItemStackGenerator.getFirstAlbum()), material(Material.MUSIC_DISC_PRECIPICE),
                    exact(ItemStackGenerator.getSecondAlbum()))),

            new Entry(3, shapelessMaterialRecipe("firstsherd", ItemStackGenerator.getFirstSherd(),
                    Material.SHEAF_POTTERY_SHERD, Material.SHELTER_POTTERY_SHERD,
                    Material.ANGLER_POTTERY_SHERD, Material.ARCHER_POTTERY_SHERD,
                    Material.ARMS_UP_POTTERY_SHERD, Material.BLADE_POTTERY_SHERD,
                    Material.BREWER_POTTERY_SHERD, Material.BURN_POTTERY_SHERD,
                    Material.FLOW_POTTERY_SHERD)),
            new Entry(3, shapelessMaterialRecipe("secondsherd", ItemStackGenerator.getSecondSherd(),
                    Material.DANGER_POTTERY_SHERD, Material.EXPLORER_POTTERY_SHERD,
                    Material.FRIEND_POTTERY_SHERD, Material.GUSTER_POTTERY_SHERD,
                    Material.HEART_POTTERY_SHERD, Material.HEARTBREAK_POTTERY_SHERD,
                    Material.HOWL_POTTERY_SHERD, Material.MINER_POTTERY_SHERD,
                    Material.MOURNER_POTTERY_SHERD)),
            new Entry(3, shapelessExactRecipe("sherdrelic", ItemStackGenerator.getSherdRelic(),
                    new ItemStack(Material.SKULL_POTTERY_SHERD),
                    new ItemStack(Material.PLENTY_POTTERY_SHERD),
                    ItemStackGenerator.getFirstSherd(),
                    new ItemStack(Material.SNORT_POTTERY_SHERD),
                    ItemStackGenerator.getSecondSherd(),
                    new ItemStack(Material.PRIZE_POTTERY_SHERD),
                    new ItemStack(Material.SCRAPE_POTTERY_SHERD))),

            new Entry(3, shapelessMaterialRecipe("firsttrim", ItemStackGenerator.getFirstTrim(),
                    Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE,
                    Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE,
                    Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE,
                    Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE,
                    Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE,
                    Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE,
                    Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE,
                    Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE,
                    Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE)),
            new Entry(3, shapelessMaterialRecipe("secondtrim", ItemStackGenerator.getSecondTrim(),
                    Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE,
                    Material.WILD_ARMOR_TRIM_SMITHING_TEMPLATE,
                    Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE,
                    Material.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE,
                    Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE,
                    Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE,
                    Material.VEX_ARMOR_TRIM_SMITHING_TEMPLATE,
                    Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE,
                    Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE)),
            new Entry(3, shapelessRecipe("trimrelic", ItemStackGenerator.getTrimRelic(),
                    exact(ItemStackGenerator.getFirstTrim()), material(Material.GOLD_BLOCK),
                    exact(ItemStackGenerator.getSecondTrim()))),

            new Entry(4, shapelessMaterialRecipe("warrioremblem", ItemStackGenerator.getWarriorEmblem(),
                    Material.NETHERITE_SWORD, Material.NETHERITE_HELMET,
                    Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS,
                    Material.NETHERITE_BOOTS, Material.MACE, Material.TRIDENT, Material.CROSSBOW,
                    Material.TOTEM_OF_UNDYING)));

    private static ShapelessSpec shapelessMaterialRecipe(String keyName, ItemStack result,
            Material... ingredients) {
        List<RecipeChoice> choices = Arrays.stream(ingredients)
                .<RecipeChoice>map((Material material) -> new RecipeChoice.MaterialChoice(material))
                .toList();
        return new ShapelessSpec(keyName, result, choices);
    }

    private static ShapelessSpec shapelessExactRecipe(String keyName, ItemStack result,
            ItemStack... ingredients) {
        List<RecipeChoice> choices = Arrays.stream(ingredients)
                .<RecipeChoice>map((ItemStack ingredient) -> new RecipeChoice.ExactChoice(ingredient))
                .toList();
        return new ShapelessSpec(keyName, result, choices);
    }

    private static ShapelessSpec shapelessRecipe(String keyName, ItemStack result,
            RecipeChoice... ingredients) {
        return new ShapelessSpec(keyName, result, List.of(ingredients));
    }

    private static RecipeChoice material(Material material) {
        return new RecipeChoice.MaterialChoice(material);
    }

    private static RecipeChoice exact(ItemStack ingredient) {
        return new RecipeChoice.ExactChoice(ingredient);
    }

    /**
     * Builds a goat horn ItemStack tuned to a specific instrument for use as a
     * recipe ingredient.
     */
    private static ItemStack getGoatHorn(MusicInstrument hornType) {
        ItemStack horn = new ItemStack(Material.GOAT_HORN);
        MusicInstrumentMeta meta = (MusicInstrumentMeta) horn.getItemMeta();
        if (meta == null)
            return horn;
        meta.setInstrument(hornType);
        horn.setItemMeta(meta);
        return horn;
    }
}
