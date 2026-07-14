package sir_draco.survivalskills.skill_listeners.fighting;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import sir_draco.survivalskills.bosses.Boss;
import sir_draco.survivalskills.bosses.BroodMotherBoss;
import sir_draco.survivalskills.bosses.GiantBoss;
import sir_draco.survivalskills.bosses.VillagerBoss;
import sir_draco.survivalskills.utils.items.ItemStackGenerator;

import java.util.function.Supplier;

/**
 * Central metadata for the fightable bosses. Replaces the scattered switch/if-else
 * chains that previously dispatched on {@link EntityType} in multiple methods.
 * Each entry bundles the entity type, spawn-egg material, broadcast strings,
 * XP multiplier, boss-item supplier and spawn factory so boss handling can be
 * driven by a single lookup instead of duplicated conditionals.
 */
public enum BossType {

    GIANT("The Giant", "Giant", EntityType.ZOMBIE, Material.ZOMBIE_SPAWN_EGG, true, 500.0,
            ItemStackGenerator::getGiantBossItem,
            (loc, summoner, noMusic) -> GiantBoss.create(loc)),
    BROOD_MOTHER("The BroodMother", "BroodMother", EntityType.SPIDER, Material.SPIDER_SPAWN_EGG, false, 1000.0,
            ItemStackGenerator::getBroodMotherBossItem,
            (loc, summoner, noMusic) -> BroodMotherBoss.create(loc)),
    EXILED_ONE("The Exiled One", "exiled one", EntityType.VILLAGER, Material.VILLAGER_SPAWN_EGG, false, 5000.0,
            ItemStackGenerator::getVillagerBossItem,
            (loc, summoner, noMusic) -> VillagerBoss.create(loc, summoner, noMusic)),
    ENDER_DRAGON("Ender Dragon", null, EntityType.ENDER_DRAGON, null, false, 0.0,
            ItemStackGenerator::getEnderDragonBossItem,
            null);

    /** Header used in the "has been slain!" broadcast, e.g. "The Giant". */
    private final String displayName;
    /** Phrase slotted into spawn-failure messages ("spawn the " + spawnName); null for the dragon. */
    private final String spawnName;
    private final EntityType entityType;
    private final Material spawnEgg;
    private final boolean requiresNight;
    private final double xpMultiplier;
    private final Supplier<ItemStack> bossItem;
    private final BossFactory factory;

    BossType(String displayName, String spawnName, EntityType entityType, Material spawnEgg,
             boolean requiresNight, double xpMultiplier, Supplier<ItemStack> bossItem, BossFactory factory) {
        this.displayName = displayName;
        this.spawnName = spawnName;
        this.entityType = entityType;
        this.spawnEgg = spawnEgg;
        this.requiresNight = requiresNight;
        this.xpMultiplier = xpMultiplier;
        this.bossItem = bossItem;
        this.factory = factory;
    }

    public static BossType fromEntityType(EntityType type) {
        for (BossType t : values())
            if (t.entityType == type) return t;
        return null;
    }

    public static BossType fromSpawnEgg(Material material) {
        for (BossType t : values())
            if (t.spawnEgg == material) return t;
        return null;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSpawnName() {
        return spawnName;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public boolean requiresNight() {
        return requiresNight;
    }

    public double getXpMultiplier() {
        return xpMultiplier;
    }

    public ItemStack getBossItem() {
        return bossItem.get();
    }

    /**
     * Spawn a new boss instance. The dragon is world/respawn spawned, so its factory
     * is null and this method must not be called for {@link #ENDER_DRAGON}.
     */
    public Boss create(Location loc, Player summoner, boolean noMusic) {
        if (factory == null)
            throw new IllegalStateException("Cannot factory-spawn boss " + name());
        return factory.create(loc, summoner, noMusic);
    }

    @FunctionalInterface
    interface BossFactory {
        Boss create(Location loc, Player summoner, boolean noMusic);
    }
}