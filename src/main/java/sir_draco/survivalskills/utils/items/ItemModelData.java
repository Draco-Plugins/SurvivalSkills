package sir_draco.survivalskills.utils.items;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum ItemModelData {
    UNLIMITED_TORCH(1),
    BOSS_ITEM(2),
    MINING_ARMOR(3),
    JUMPING_BOOTS(4),
    WANDERER_ARMOR(5),
    CAVE_FINDER(6),
    TRAVELER_ARMOR(7),
    ADVENTURER_ARMOR(8),
    WATERING_CAN(9),
    UNLIMITED_BONE_MEAL(10),
    HARVESTER(11),
    GIANT_SUMMON(12),
    BROOD_MOTHER_SUMMON(13),
    THE_EXILED_ONE_SUMMON(14),
    DENSE_WOOL(15),
    SORT_WAND(16),
    UNLIMITED_TROPICAL_FISH_BUCKET(17),
    FIREWORK_CANNON(18),
    GILL_ARMOR(19),
    UNLIMITED_WATER_BUCKET(20),
    UNLIMITED_LAVA_BUCKET(21),
    WEATHER_ARTIFACT(22),
    TIME_ARTIFACT(23),
    HARD_NAUTILUS_SHELL(24),
    HARD_HEART_OF_THE_SEA(25),
    XP_VOUCHER(26),
    ZAP_WAND(27),
    BRONZE_INGOT(28),
    BEACON_ARMOR(29),
    UNLIMITED_EMPTY_BUCKET(30),
    UNLIMITED_ROCKET(31),
    MAGNET(32),
    WEB_SHOOTER(33),
    UNLIMITED_TIPPED_ARROW(34),
    VILLAGER_REVIVAL_ARTIFACT(35),
    ENDER_ESSENCE(36),
    CREEPER_ESSENCE(37),
    POTION_BAG(38),
    MAGIC_BAG_OF_WIND(39),
    DRAGON_BREATH_CANNON(40),
    UNLIMITED_WITHER_ROSE(41),
    GOD_QUEST_ITEM(42),
    TRIDENT_LAUNCHER(43),
    POWER_ORE(44),
    GOD_TROPHY_BASE(45),
    UNLIMITED_SPONGE(46),
    POWER_SWORD(47),
    POWER_DRILL(48),
    BROODING_SILK(49),
    POWER_LASER(50),
    POWER_ARMOR(51),
    UNLIMITED_POWDER_SNOW_BUCKET(52),
    SNOWBALL_CANNON(53),
    WRENCH(54),
    TELEPORT_ANCHOR(55),
    TRANSFER_PIPE(56),
    TROPHY(999);

    private final int id;

    private static final Map<Integer, ItemModelData> BY_ID =
            Arrays.stream(values()).collect(Collectors.toMap(
                    (ItemModelData data) -> data.id,
                    Function.identity()));

    ItemModelData(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static Optional<ItemModelData> fromId(int id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static ItemModelData fromIdOrThrow(int id) {
        ItemModelData data = BY_ID.get(id);
        if (data == null) {
            throw new IllegalArgumentException("Unknown ItemModelData id: " + id);
        }
        return data;
    }
}
