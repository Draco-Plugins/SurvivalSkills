package sir_draco.survivalskills.skill_listeners.fishing;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;

import java.util.List;
import java.util.Map;

/**
 * The rarity tier of a fishing loot drop. Each tier is associated with its own
 * loot table and enchantment pool (populated at runtime from {@code fishing_loot.yml})
 * plus a small validation predicate. Together these replace the four near-duplicate
 * {@code type == 1..4} branches that previously existed in {@code getMaterial} and
 * {@code getStackableMaterial}.
 *
 * <p>Tier constants are immutable in identity; their loot data is assigned exactly
 * once during config load and treated as read-only thereafter.</p>
 *
 * <ul>
 *   <li>{@link #getEnchantTier()} &mdash; the {@link EnchantTier} used to clamp
 *       enchanted-book levels (or {@code null} when no book enchantments apply).</li>
 *   <li>{@link #isDragonGated()} &mdash; whether an ELYTRA drop requires the host
 *       world to have its {@code killedfirstdragon} marker set (legendary only).</li>
 * </ul>
 *
 * <p>The exotic tier is intentionally not represented here: its drops are fixed
 * {@link org.bukkit.inventory.ItemStack}s (custom items) rather than plain
 * {@link Material}s, so it is handled by a separate code path in
 * {@link FishingLootManager}.</p>
 */
public enum LootTier {

    LEGENDARY(EnchantTier.LEGENDARY, true),
    EPIC(EnchantTier.EPIC, false),
    RARE(EnchantTier.RARE, false),
    COMMON(null, false);

    /** Maximum number of rejection-sampling attempts when looking for a valid material. */
    public static final int MAX_DRAW_RETRIES = 100;

    private final EnchantTier enchantTier;
    private final boolean dragonGated;

    // Populated once by FishingLootManager during config load; read-only afterwards.
    private List<Material> lootTable;
    private Map<Enchantment, Integer> enchantments;

    LootTier(EnchantTier enchantTier, boolean dragonGated) {
        this.enchantTier = enchantTier;
        this.dragonGated = dragonGated;
    }

    void configure(List<Material> lootTable, Map<Enchantment, Integer> enchantments) {
        this.lootTable = lootTable;
        this.enchantments = enchantments;
    }

    public EnchantTier getEnchantTier() {
        return enchantTier;
    }

    public boolean isDragonGated() {
        return dragonGated;
    }

    public List<Material> getLootTable() {
        return lootTable;
    }

    public Map<Enchantment, Integer> getEnchantments() {
        return enchantments;
    }

    /**
     * Validation predicate for {@code Material}s drawn from non-exotic tiers:
     * legendary ELYTRA only drops once the host world has recorded that the
     * first ender dragon has been killed.
     */
    public boolean isAllowed(Material material, World world) {
        if (!dragonGated)
            return true;
        if (material == Material.ELYTRA && !world.hasMetadata("killedfirstdragon"))
            return false;
        return true;
    }
}