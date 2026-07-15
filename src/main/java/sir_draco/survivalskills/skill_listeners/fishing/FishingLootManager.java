package sir_draco.survivalskills.skill_listeners.fishing;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.rewards.PlayerRewards;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.utils.FileUtils;
import sir_draco.survivalskills.utils.items.ItemStackGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Owns every loot-related concern for the fishing skill: the per-tier loot
 * tables and enchantment pools (loaded from {@code fishing_loot.yml}), the
 * drop-rolling logic, enchanted-book generation and the non-stackable item
 * guard. Previously all of this lived inline in {@code FishingSkill} as five
 * hardcoded {@code createXxxLootTable()} methods and the heavily duplicated
 * {@code getMaterial}/{@code getStackableMaterial} pair.
 *
 * <p>The drop tiers are modelled by {@link LootTier} (which also carries the
 * per-tier validation predicate), and book level-clamping by {@link EnchantTier}.
 * The duplicated four-branch {@code type == 1..4} dispatch is replaced by a
 * single {@link #drawLoot(Player, LootTier)} entry point.</p>
 */
public class FishingLootManager {

    private static final String CONFIG_FILE = "fishing_loot.yml";

    /** Per-material luck-of-the-sea bonus applied to each tier's base drop chance. */
    private static final double COMMON_LUCK_BONUS = 0.15;
    private static final double RARE_LUCK_BONUS = 0.05;
    private static final double EPIC_LUCK_BONUS = 0.025;
    private static final double LEGENDARY_LUCK_BONUS = 0.005;
    private static final double EXOTIC_LUCK_BONUS = 0.00005;

    /** Materials that should never stack when drawn (each counts toward the variety cap). */
    private static final Set<Material> NON_STACKABLE_MATERIALS = EnumSet.of(
            Material.LEATHER_HORSE_ARMOR,
            Material.IRON_HORSE_ARMOR,
            Material.GOLDEN_HORSE_ARMOR,
            Material.DIAMOND_HORSE_ARMOR,
            Material.SADDLE);

    /** Rare-tier materials that always drop as a single item. */
    private static final Set<Material> SINGLE_ITEM_RARE = EnumSet.of(
            Material.LEATHER_HORSE_ARMOR,
            Material.NAME_TAG,
            Material.SADDLE);

    /** Epic-tier materials that always drop as a single item. */
    private static final Set<Material> SINGLE_ITEM_EPIC = EnumSet.of(
            Material.GOLDEN_HORSE_ARMOR,
            Material.IRON_HORSE_ARMOR,
            Material.HEART_OF_THE_SEA,
            Material.GHAST_TEAR);

    private static final int NON_STACKABLE_LIMIT = 5;
    private static final int EPIC_XP_BOTTLE_FLOOR = 12;
    private static final int EPIC_XP_BOTTLE_ROLL = 24;
    private static final int LEGENDARY_XP_BOTTLE_FLOOR = 48;
    private static final int LEGENDARY_XP_BOTTLE_ROLL = 64;
    private static final int EPIC_TIER_LEVEL_THRESHOLD = 4;
    private static final int EPIC_LEVEL_FLOOR = 3;
    private static final int RARE_LEVEL_FLOOR = 1;
    private static final int STACKABLE_ROLL_LOW = 3;
    private static final int COMMON_ROLL = 2;

    private final SurvivalSkills plugin;

    // Per-player running count of non-stackable items drawn this session, used to
    // force variety in drops once the cap is reached.
    private final Map<Player, Integer> nonStackableItems = new HashMap<>();

    // Exotic items cannot be expressed as plain Materials; their identifiers map
    // to factory methods on ItemStackGenerator. Loaded once from config.
    private final List<ItemStack> exoticLootTable = new ArrayList<>();

    public FishingLootManager(SurvivalSkills plugin) {
        this.plugin = plugin;
        loadLootTables();
    }

    // =================================================================
    // Config loading
    // =================================================================

    private void loadLootTables() {
        File file = new File(plugin.getDataFolder(), CONFIG_FILE);
        if (!file.exists())
            plugin.saveResource(CONFIG_FILE, false);
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);

        configureTier(LootTier.LEGENDARY, loadMaterials(data, "legendary"), loadEnchantments(data, "legendary_enchantments"));
        configureTier(LootTier.EPIC, loadMaterials(data, "epic"), loadEnchantments(data, "epic_enchantments"));
        configureTier(LootTier.RARE, loadMaterials(data, "rare"), loadEnchantments(data, "rare_enchantments"));
        configureTier(LootTier.COMMON, loadMaterials(data, "common"), Map.of());

        loadExoticLoot(data);
    }

    private void configureTier(LootTier tier, List<Material> materials, Map<Enchantment, Integer> enchantments) {
        if (materials.isEmpty())
            Bukkit.getLogger().log(Level.WARNING,
                    String.format("[SurvivalSkills] %s loot table is empty in %s", tier, CONFIG_FILE));
        tier.configure(materials, enchantments);
    }

    private List<Material> loadMaterials(FileConfiguration data, String sectionName) {
        List<Material> materials = new ArrayList<>();
        List<String> raw = data.getStringList(sectionName);
        if (raw.isEmpty())
            return materials;
        for (String entry : raw) {
            Material material = Material.getMaterial(entry);
            if (material == null) {
                Bukkit.getLogger().log(Level.WARNING,
                        String.format("[SurvivalSkills] Unknown material %s in %s/%s", entry, CONFIG_FILE, sectionName));
                continue;
            }
            materials.add(material);
        }
        return materials;
    }

    private Map<Enchantment, Integer> loadEnchantments(FileConfiguration data, String sectionName) {
        Map<Enchantment, Integer> enchantments = new HashMap<>();
        ConfigurationSection section = data.getConfigurationSection(sectionName);
        if (section == null)
            return enchantments;
        for (String key : section.getKeys(false)) {
            Enchantment enchant = FileUtils.getEnchantFromKey(key);
            if (enchant == null) {
                Bukkit.getLogger().log(Level.WARNING,
                        String.format("[SurvivalSkills] Unknown enchantment %s in %s/%s", key, CONFIG_FILE, sectionName));
                continue;
            }
            enchantments.put(enchant, section.getInt(key));
        }
        return enchantments;
    }

    private void loadExoticLoot(FileConfiguration data) {
        Map<String, Supplier<ItemStack>> suppliers = new HashMap<>();
        suppliers.put("WEATHER_ARTIFACT", ItemStackGenerator::getWeatherArtifact);
        suppliers.put("TIME_ARTIFACT", ItemStackGenerator::getTimeArtifact);
        suppliers.put("UNLIMITED_WATER_BUCKET", ItemStackGenerator::getUnlimitedWaterBucket);
        suppliers.put("UNLIMITED_LAVA_BUCKET", ItemStackGenerator::getUnlimitedLavaBucket);
        suppliers.put("UNLIMITED_POWDER_SNOW_BUCKET", ItemStackGenerator::getUnlimitedPowderSnowBucket);
        suppliers.put("UNLIMITED_EMPTY_BUCKET", ItemStackGenerator::getUnlimitedEmptyBucket);
        suppliers.put("UNLIMITED_ROCKET", ItemStackGenerator::getUnlimitedRocket);

        List<String> raw = data.getStringList("exotic");
        if (raw.isEmpty()) {
            // Fallback to the full default set so drops never silently vanish.
            raw = List.of("WEATHER_ARTIFACT", "TIME_ARTIFACT", "UNLIMITED_WATER_BUCKET",
                    "UNLIMITED_LAVA_BUCKET", "UNLIMITED_POWDER_SNOW_BUCKET", "UNLIMITED_EMPTY_BUCKET",
                    "UNLIMITED_ROCKET");
        }
        for (String entry : raw) {
            Supplier<ItemStack> supplier = suppliers.get(entry);
            if (supplier == null) {
                Bukkit.getLogger().log(Level.WARNING,
                        String.format("[SurvivalSkills] Unknown exotic item %s in %s", entry, CONFIG_FILE));
                continue;
            }
            exoticLootTable.add(supplier.get());
        }
    }

    // =================================================================
    // Drop rolling
    // =================================================================

    /**
     * Returns the number of items a player draws per cast based on their
     * unlocked "FishingLine" reward tiers.
     */
    public int getFishingLineNumber(Player p) {
        PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
        if (rewards.getReward(SkillCategory.FISHING, "FishingLineV").isApplied())
            return 10;
        if (rewards.getReward(SkillCategory.FISHING, "FishingLineIV").isApplied())
            return 7;
        if (rewards.getReward(SkillCategory.FISHING, "FishingLineIII").isApplied())
            return 5;
        if (rewards.getReward(SkillCategory.FISHING, "FishingLineII").isApplied())
            return 3;
        if (rewards.getReward(SkillCategory.FISHING, "FishingLineI").isApplied())
            return 2;
        return 1;
    }

    /**
     * Builds the list of items dropped on a single successful cast, applying the
     * tier-probability table and the player's luck-of-the-sea bonus.
     */
    public ArrayList<ItemStack> getItemsToDrop(Player p, int luckLevel) {
        ArrayList<ItemStack> items = new ArrayList<>();
        int lineNumber = getFishingLineNumber(p);
        PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);

        double commonPercentage = rewards.getCommonFishingLootChance() + (luckLevel * COMMON_LUCK_BONUS);
        double rarePercentage = rewards.getRareFishingLootChance() + (luckLevel * RARE_LUCK_BONUS);
        double epicPercentage = rewards.getEpicFishingLootChance() + (luckLevel * EPIC_LUCK_BONUS);
        double legendaryPercentage = rewards.getLegendaryFishingLootChance() + (luckLevel * LEGENDARY_LUCK_BONUS);
        double exoticPercentage = rewards.getExoticFishingLootChance() + (luckLevel * EXOTIC_LUCK_BONUS);

        // A disabled tier (base chance 0) cannot be re-enabled by luck bonuses.
        if (rewards.getCommonFishingLootChance() == 0)
            commonPercentage = 0;
        if (rewards.getRareFishingLootChance() == 0)
            rarePercentage = 0;
        if (rewards.getEpicFishingLootChance() == 0)
            epicPercentage = 0;
        if (rewards.getLegendaryFishingLootChance() == 0)
            legendaryPercentage = 0;

        for (int i = 1; i <= lineNumber; i++) {
            double chance = Math.random();
            if (chance < exoticPercentage) {
                items.add(exoticLootTable.get(getRandomPositiveInteger(exoticLootTable.size()) - 1));
                p.sendRawMessage(ChatColor.LIGHT_PURPLE + "You have caught an exotic item!");
                p.playSound(p, Sound.ENTITY_DOLPHIN_ATTACK, 1, 1);
                p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            } else if (legendaryPercentage != 0 && chance < legendaryPercentage) {
                items.add(buildLegendaryDrop(p));
            } else if (epicPercentage != 0 && chance < epicPercentage) {
                items.add(buildEpicDrop(p));
            } else if (rarePercentage != 0 && chance < rarePercentage) {
                items.add(buildRareDrop(p));
            } else if (commonPercentage != 0 && chance < commonPercentage) {
                items.add(new ItemStack(drawLoot(p, LootTier.COMMON), getRandomPositiveInteger(COMMON_ROLL)));
            }
        }

        return items;
    }

    private ItemStack buildLegendaryDrop(Player p) {
        Material mat = drawLoot(p, LootTier.LEGENDARY);
        if (mat.equals(Material.ENCHANTED_BOOK))
            return getEnchantedBook(LootTier.LEGENDARY.getEnchantments(), EnchantTier.LEGENDARY);
        if (mat.equals(Material.EXPERIENCE_BOTTLE))
            return new ItemStack(Material.EXPERIENCE_BOTTLE, Math.max(LEGENDARY_XP_BOTTLE_FLOOR,
                    getRandomPositiveInteger(LEGENDARY_XP_BOTTLE_ROLL)));
        return new ItemStack(mat);
    }

    private ItemStack buildEpicDrop(Player p) {
        Material mat = drawLoot(p, LootTier.EPIC);
        if (mat.equals(Material.ENCHANTED_BOOK))
            return getEnchantedBook(LootTier.EPIC.getEnchantments(), EnchantTier.EPIC);
        if (mat.equals(Material.EXPERIENCE_BOTTLE))
            return new ItemStack(Material.EXPERIENCE_BOTTLE, Math.max(EPIC_XP_BOTTLE_FLOOR,
                    getRandomPositiveInteger(EPIC_XP_BOTTLE_ROLL)));
        if (SINGLE_ITEM_EPIC.contains(mat))
            return new ItemStack(mat);
        return new ItemStack(mat, getRandomPositiveInteger(STACKABLE_ROLL_LOW));
    }

    private ItemStack buildRareDrop(Player p) {
        Material mat = drawLoot(p, LootTier.RARE);
        if (mat.equals(Material.ENCHANTED_BOOK))
            return getEnchantedBook(LootTier.RARE.getEnchantments(), EnchantTier.RARE);
        if (SINGLE_ITEM_RARE.contains(mat))
            return new ItemStack(mat);
        return new ItemStack(mat, getRandomPositiveInteger(STACKABLE_ROLL_LOW));
    }

    /**
     * Draws a single material from {@code tier}'s loot table, honouring the
     * non-stackable variety cap and the tier's validation predicate. Replaces
     * the four near-identical {@code type == 1..4} branches of the old
     * {@code getMaterial} method.
     */
    public Material drawLoot(Player p, LootTier tier) {
        nonStackableItems.computeIfAbsent(p, k -> 0);
        if (nonStackableItems.get(p) >= NON_STACKABLE_LIMIT)
            return getStackableMaterial(tier, p);

        List<Material> table = tier.getLootTable();
        Material mat = table.get(getRandomPositiveInteger(table.size()) - 1);
        trackNonStackable(p, mat);
        if (!tier.isAllowed(mat, p.getWorld()))
            return drawLoot(p, tier);
        return mat;
    }

    /**
     * Draws a material that is guaranteed to be stackable (and tier-valid).
     * Replaces the four near-identical recursive branches of the old
     * {@code getStackableMaterial} method with rejection sampling bounded by
     * {@link LootTier#MAX_DRAW_RETRIES}, then a deterministic fallback scan so a
     * valid item is still returned when the table is dominated by non-stackable
     * entries (where the original could stack-overflow).
     */
    public Material getStackableMaterial(LootTier tier, Player p) {
        List<Material> table = tier.getLootTable();
        for (int i = 0; i < LootTier.MAX_DRAW_RETRIES; i++) {
            Material mat = table.get(getRandomPositiveInteger(table.size()) - 1);
            if (NON_STACKABLE_MATERIALS.contains(mat))
                continue;
            if (!tier.isAllowed(mat, p.getWorld()))
                continue;
            return mat;
        }
        for (Material mat : table)
            if (!NON_STACKABLE_MATERIALS.contains(mat) && tier.isAllowed(mat, p.getWorld()))
                return mat;
        return Material.AIR;
    }

    /**
     * Tracks how many non-stackable materials the player has drawn this session.
     * Uses {@code merge} so the compute is safe against absent entries (fixing a
     * potential NPE from the old {@code get/Mutating put} idiom).
     */
    private void trackNonStackable(Player p, Material mat) {
        if (NON_STACKABLE_MATERIALS.contains(mat))
            nonStackableItems.merge(p, 1, (Integer currentCount, Integer addedCount) ->
                    currentCount.intValue() + addedCount.intValue());
    }

    // =================================================================
    // Enchanted books
    // =================================================================

    /**
     * Creates an enchanted book carrying a randomly selected enchantment from
     * {@code enchantments}, with the stored level clamped according to {@code tier}.
     */
    public ItemStack getEnchantedBook(Map<Enchantment, Integer> enchantments, EnchantTier tier) {
        int random = getRandomPositiveInteger(enchantments.size());
        int i = 1;
        for (Enchantment enchantment : enchantments.keySet()) {
            if (i++ != random)
                continue;

            int level = enchantments.get(enchantment);
            ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
            if (meta == null || enchantment == null)
                return new ItemStack(Material.AIR);

            int applied = switch (tier) {
                case LEGENDARY -> level;
                case EPIC -> level >= EPIC_TIER_LEVEL_THRESHOLD
                        ? Math.max(EPIC_LEVEL_FLOOR, getRandomPositiveInteger(level))
                        : clampLowLevel(level);
                case RARE -> clampLowLevel(level);
            };
            meta.addStoredEnchant(enchantment, applied, false);
            book.setItemMeta(meta);
            return book;
        }
        return new ItemStack(Material.AIR);
    }

    private int clampLowLevel(int level) {
        return level == 1 ? 1 : Math.max(RARE_LEVEL_FLOOR, getRandomPositiveInteger(level));
    }

    public int getRandomPositiveInteger(int size) {
        return (int) Math.ceil((Math.random() * size));
    }
}
