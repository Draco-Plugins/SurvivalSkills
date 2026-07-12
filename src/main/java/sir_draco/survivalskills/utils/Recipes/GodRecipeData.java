package sir_draco.survivalskills.utils.Recipes;

import org.bukkit.Material;
import org.bukkit.MusicInstrument;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MusicInstrumentMeta;
import sir_draco.survivalskills.utils.items.ItemStackGenerator;

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

    // TODO: Determine which of these can be shapeless recipes
    public static final List<Entry> ALL = List.of(
            new Entry(1, new SmallShapedSpec("turtle", ItemStackGenerator.getTurtleHelmet(),
                    "AAA:ABA:AAA", null, null, null, Material.TURTLE_SCUTE, Material.TURTLE_EGG,
                    null)),
            new Entry(1, new ShapedSpec("goat", ItemStackGenerator.getGoatHorn(), true,
                    getGoatHorn(MusicInstrument.CALL_GOAT_HORN),
                    getGoatHorn(MusicInstrument.ADMIRE_GOAT_HORN),
                    getGoatHorn(MusicInstrument.DREAM_GOAT_HORN),
                    getGoatHorn(MusicInstrument.FEEL_GOAT_HORN),
                    getGoatHorn(MusicInstrument.PONDER_GOAT_HORN),
                    getGoatHorn(MusicInstrument.SEEK_GOAT_HORN),
                    getGoatHorn(MusicInstrument.SING_GOAT_HORN),
                    getGoatHorn(MusicInstrument.YEARN_GOAT_HORN), null)),

            new Entry(2, new ShapedSpec("firstalbum", ItemStackGenerator.getFirstAlbum(), false,
                    new ItemStack(Material.MUSIC_DISC_5), new ItemStack(Material.MUSIC_DISC_11),
                    new ItemStack(Material.MUSIC_DISC_13), new ItemStack(Material.MUSIC_DISC_BLOCKS),
                    new ItemStack(Material.MUSIC_DISC_CAT), new ItemStack(Material.MUSIC_DISC_CHIRP),
                    new ItemStack(Material.MUSIC_DISC_CREATOR),
                    new ItemStack(Material.MUSIC_DISC_CREATOR_MUSIC_BOX),
                    new ItemStack(Material.MUSIC_DISC_FAR))),
            new Entry(2, new ShapedSpec("secondalbum", ItemStackGenerator.getSecondAlbum(), false,
                    new ItemStack(Material.MUSIC_DISC_MALL), new ItemStack(Material.MUSIC_DISC_MELLOHI),
                    new ItemStack(Material.MUSIC_DISC_OTHERSIDE),
                    new ItemStack(Material.MUSIC_DISC_PIGSTEP),
                    new ItemStack(Material.MUSIC_DISC_RELIC), new ItemStack(Material.MUSIC_DISC_STAL),
                    new ItemStack(Material.MUSIC_DISC_STRAD), new ItemStack(Material.MUSIC_DISC_WAIT),
                    new ItemStack(Material.MUSIC_DISC_WARD))),
            new Entry(2, new SmallShapedSpec("musicknowledge",
                    ItemStackGenerator.getMusicKnowledgeDisc(), "DDD:ABC:DDD",
                    ItemStackGenerator.getFirstAlbum(), null, ItemStackGenerator.getSecondAlbum(),
                    null, Material.MUSIC_DISC_PRECIPICE, null)),

            new Entry(3, new ShapedSpec("firstsherd", ItemStackGenerator.getFirstSherd(), false,
                    new ItemStack(Material.SHEAF_POTTERY_SHERD),
                    new ItemStack(Material.SHELTER_POTTERY_SHERD),
                    new ItemStack(Material.ANGLER_POTTERY_SHERD),
                    new ItemStack(Material.ARCHER_POTTERY_SHERD),
                    new ItemStack(Material.ARMS_UP_POTTERY_SHERD),
                    new ItemStack(Material.BLADE_POTTERY_SHERD),
                    new ItemStack(Material.BREWER_POTTERY_SHERD),
                    new ItemStack(Material.BURN_POTTERY_SHERD),
                    new ItemStack(Material.FLOW_POTTERY_SHERD))),
            new Entry(3, new ShapedSpec("secondsherd", ItemStackGenerator.getSecondSherd(),
                    false, new ItemStack(Material.DANGER_POTTERY_SHERD),
                    new ItemStack(Material.EXPLORER_POTTERY_SHERD),
                    new ItemStack(Material.FRIEND_POTTERY_SHERD),
                    new ItemStack(Material.GUSTER_POTTERY_SHERD),
                    new ItemStack(Material.HEART_POTTERY_SHERD),
                    new ItemStack(Material.HEARTBREAK_POTTERY_SHERD),
                    new ItemStack(Material.HOWL_POTTERY_SHERD),
                    new ItemStack(Material.MINER_POTTERY_SHERD),
                    new ItemStack(Material.MOURNER_POTTERY_SHERD))),
            new Entry(3, new ShapedSpec("sherdrelic", ItemStackGenerator.getSherdRelic(), true,
                    null, new ItemStack(Material.SKULL_POTTERY_SHERD),
                    new ItemStack(Material.PLENTY_POTTERY_SHERD),
                    ItemStackGenerator.getFirstSherd(),
                    new ItemStack(Material.SNORT_POTTERY_SHERD),
                    ItemStackGenerator.getSecondSherd(),
                    new ItemStack(Material.PRIZE_POTTERY_SHERD),
                    new ItemStack(Material.SCRAPE_POTTERY_SHERD), null)),

            new Entry(3, new ShapedSpec("firsttrim", ItemStackGenerator.getFirstTrim(), false,
                    new ItemStack(Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE),
                    new ItemStack(Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE),
                    new ItemStack(Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE),
                    new ItemStack(Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE),
                    new ItemStack(Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE),
                    new ItemStack(Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE),
                    new ItemStack(Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE),
                    new ItemStack(Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE),
                    new ItemStack(Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE))),
            new Entry(3, new ShapedSpec("secondtrim", ItemStackGenerator.getSecondTrim(),
                    false, new ItemStack(Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE),
                    new ItemStack(Material.WILD_ARMOR_TRIM_SMITHING_TEMPLATE),
                    new ItemStack(Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE),
                    new ItemStack(Material.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE),
                    new ItemStack(Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE),
                    new ItemStack(Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE),
                    new ItemStack(Material.VEX_ARMOR_TRIM_SMITHING_TEMPLATE),
                    new ItemStack(Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE),
                    new ItemStack(Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE))),
            new Entry(3, new SmallShapedSpec("trimrelic", ItemStackGenerator.getTrimRelic(),
                    "DDD:ABC:DDD", ItemStackGenerator.getFirstTrim(), null,
                    ItemStackGenerator.getSecondTrim(), null, Material.GOLD_BLOCK, null)),

            new Entry(4, new ShapedSpec("warrioremblem", ItemStackGenerator.getWarriorEmblem(),
                    false, new ItemStack(Material.NETHERITE_SWORD),
                    new ItemStack(Material.NETHERITE_HELMET),
                    new ItemStack(Material.NETHERITE_CHESTPLATE),
                    new ItemStack(Material.NETHERITE_LEGGINGS),
                    new ItemStack(Material.NETHERITE_BOOTS), new ItemStack(Material.MACE),
                    new ItemStack(Material.TRIDENT), new ItemStack(Material.CROSSBOW),
                    new ItemStack(Material.TOTEM_OF_UNDYING))));

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