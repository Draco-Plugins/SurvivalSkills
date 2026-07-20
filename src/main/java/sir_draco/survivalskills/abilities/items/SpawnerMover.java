package sir_draco.survivalskills.abilities.items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntitySnapshot;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import sir_draco.survivalskills.utils.items.ItemModelData;
import sir_draco.survivalskills.utils.items.ItemStackBuilder;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class SpawnerMover {

    private static final String DISPLAY_NAME = ChatColor.LIGHT_PURPLE + "Spawner Mover";

    private SpawnerMover() {}

    public static ItemStack createEmpty() {
        ItemStack mover = new ItemStackBuilder(Material.SPAWNER, 1, DISPLAY_NAME)
                .lore(List.of(
                        ChatColor.GRAY + "Stored Spawner: " + ChatColor.AQUA + "Empty",
                        ChatColor.DARK_GRAY + "Right click a spawner to pick it up"))
                .modelData(ItemModelData.SPAWNER_MOVER.getId())
                .build();
        ItemMeta meta = mover.getItemMeta();
        if (meta == null) return mover;
        meta.setMaxStackSize(1);
        mover.setItemMeta(meta);
        return mover;
    }

    public static ItemStack createCarrying(CreatureSpawner spawner) {
        ItemStack mover = createEmpty();
        ItemMeta meta = mover.getItemMeta();
        if (!(meta instanceof BlockStateMeta blockStateMeta)) {
            throw new IllegalStateException("Spawner Mover does not support block-state metadata");
        }
        storeSpawner(blockStateMeta, spawner);
        mover.setItemMeta(blockStateMeta);
        return mover;
    }

    public static boolean isSpawnerMover(ItemStack item) {
        return ItemStackGeneratorUtils.isCustomItem(item, ItemModelData.SPAWNER_MOVER.getId());
    }

    public static Optional<CreatureSpawner> getStoredSpawner(ItemStack item) {
        if (!isSpawnerMover(item)) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof BlockStateMeta blockStateMeta) || !blockStateMeta.hasBlockState()) {
            return Optional.empty();
        }
        BlockState storedState = blockStateMeta.getBlockState();
        return storedState instanceof CreatureSpawner spawner ? Optional.of(spawner) : Optional.empty();
    }

    static void storeSpawner(BlockStateMeta meta, CreatureSpawner spawner) {
        meta.setBlockState(spawner);
        meta.setLore(createLore(spawner));
    }

    public static boolean placeSpawner(CreatureSpawner spawner, org.bukkit.Location location) {
        BlockState placedState = spawner.copy(location);
        return placedState.update(true, false);
    }

    static List<String> createLore(CreatureSpawner spawner) {
        return List.of(
                ChatColor.GRAY + "Stored Spawner: " + ChatColor.AQUA + getSpawnerType(spawner),
                ChatColor.GRAY + "Current Delay: " + ChatColor.AQUA + spawner.getDelay() + " ticks",
                ChatColor.GRAY + "Cooldown: " + ChatColor.AQUA + spawner.getMinSpawnDelay()
                        + "-" + spawner.getMaxSpawnDelay() + " ticks",
                ChatColor.GRAY + "Spawn Count: " + ChatColor.AQUA + spawner.getSpawnCount(),
                ChatColor.GRAY + "Max Nearby: " + ChatColor.AQUA + spawner.getMaxNearbyEntities(),
                ChatColor.GRAY + "Player Range: " + ChatColor.AQUA + spawner.getRequiredPlayerRange() + " blocks",
                ChatColor.GRAY + "Spawn Radius: " + ChatColor.AQUA + spawner.getSpawnRange() + " blocks",
                ChatColor.GRAY + "Potential Spawns: " + ChatColor.AQUA + spawner.getPotentialSpawns().size(),
                ChatColor.DARK_GRAY + "Right click a block to place this spawner");
    }

    private static String getSpawnerType(CreatureSpawner spawner) {
        EntitySnapshot spawnedEntity = spawner.getSpawnedEntity();
        EntityType entityType = spawnedEntity == null ? spawner.getSpawnedType() : spawnedEntity.getEntityType();
        if (entityType == null) return "Custom / Random";

        String[] words = entityType.getKeyOrThrow().getKey().split("_");
        return java.util.Arrays.stream(words)
                .map((String word) -> word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1))
                .collect(java.util.stream.Collectors.joining(" "));
    }
}
