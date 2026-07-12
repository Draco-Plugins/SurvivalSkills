package sir_draco.survivalskills.trophy;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public enum TrophyType {
    CAVE(1, "CaveTrophy", Material.DIAMOND_PICKAXE),
    FOREST(2, "ForestTrophy", Material.OAK_SAPLING),
    FARMING(3, "FarmingTrophy", Material.GOLDEN_CARROT),
    OCEAN(4, "OceanTrophy", Material.TRIDENT),
    FISHING(5, "FishingTrophy", Material.FISHING_ROD),
    COLOR(6, "ColorTrophy", Material.SHEARS),
    NETHER(7, "NetherTrophy", Material.NETHERRACK),
    END(8, "EndTrophy", Material.END_STONE),
    CHAMPION(9, "ChampionTrophy", Material.DIAMOND_SWORD),
    GOD(10, "GodTrophy", Material.GRASS_BLOCK);

    private final int id;
    private final String name;
    private final Material material;

    private static final Map<String, TrophyType> BY_NAME =
            Arrays.stream(values()).collect(Collectors.toMap((TrophyType trophyType) -> trophyType.getName(),
                    Function.identity()));
    private static final Map<Integer, TrophyType> BY_ID =
            Arrays.stream(values()).collect(Collectors.toMap((TrophyType trophyType) -> trophyType.getId(),
                    Function.identity()));
    private static final Map<Material, TrophyType> BY_MATERIAL =
            Arrays.stream(values()).collect(Collectors.toMap((TrophyType trophyType) -> trophyType.getMaterial(),
                    Function.identity()));

    TrophyType(int id, String name, Material material) {
        this.id = id;
        this.name = name;
        this.material = material;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Material getMaterial() {
        return material;
    }

    public static TrophyType fromName(String name) {
        TrophyType type = BY_NAME.get(name);
        return type != null ? type : GOD;
    }

    public static TrophyType fromId(int id) {
        TrophyType type = BY_ID.get(id);
        if (type == null) {
            throw new IllegalArgumentException("Unknown trophy id: " + id);
        }
        return type;
    }

    public static Optional<TrophyType> fromMaterial(Material material) {
        return Optional.ofNullable(BY_MATERIAL.get(material));
    }
}
