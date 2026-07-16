package sir_draco.survivalskills.god_questline.trial;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.persistence.PersistentDataType;

import sir_draco.survivalskills.god_questline.trial_mobs.*;

import java.util.*;

public class WaveGenerator {
    // Difficulty constants
    public static final int EASY = 1;
    public static final int MEDIUM = 2;
    public static final int HARD = 3;
    public static final int GOD = 4;
    public static final int DEATH = 5;

    private record WaveEntry(String mobId, int baseCount) {}
    private record WaveDefinition(List<WaveEntry> entries, String bossId) {}
    private record DifficultyConfig(double healthMultiplier, double damageMultiplier,
                                    double countMultiplier, int maxWaves) {}

    // Store mobs and bosses to reuse across difficulties
    private final Map<String, WaveMob> mobRegistry = new HashMap<>();
    private final Map<String, TrialBoss> bossRegistry = new HashMap<>();
    private final Map<Integer, Map<Integer, Wave>> cachedWaves = new HashMap<>();

    private static final Map<Integer, WaveDefinition> WAVE_DEFINITIONS = createWaveDefinitions();

    private static Map<Integer, WaveDefinition> createWaveDefinitions() {
        Map<Integer, WaveDefinition> defs = new HashMap<>();
        defs.put(1, new WaveDefinition(List.of(new WaveEntry("weakZombie", 3)), null));
        defs.put(2, new WaveDefinition(List.of(new WaveEntry("weakSkeleton", 3)), null));
        defs.put(3, new WaveDefinition(List.of(new WaveEntry("spider", 5)), null));
        defs.put(4, new WaveDefinition(List.of(
                new WaveEntry("weakZombie", 3),
                new WaveEntry("skeleton", 2)
        ), null));
        defs.put(5, new WaveDefinition(List.of(
                new WaveEntry("zombieBoss", 1),
                new WaveEntry("zombie", 3)
        ), null));
        defs.put(6, new WaveDefinition(List.of(
                new WaveEntry("stray", 3),
                new WaveEntry("spider", 3)
        ), null));
        defs.put(7, new WaveDefinition(List.of(new WaveEntry("husk", 6)), null));
        defs.put(8, new WaveDefinition(List.of(
                new WaveEntry("creeper", 4),
                new WaveEntry("weakSkeleton", 2)
        ), null));
        defs.put(9, new WaveDefinition(List.of(
                new WaveEntry("silverfish", 6),
                new WaveEntry("endermite", 6)
        ), null));
        defs.put(10, new WaveDefinition(List.of(), "blazingGhast"));
        defs.put(11, new WaveDefinition(List.of(
                new WaveEntry("pillager", 4),
                new WaveEntry("vindicator", 2)
        ), null));
        defs.put(12, new WaveDefinition(List.of(new WaveEntry("drowned", 3)), null));
        defs.put(13, new WaveDefinition(List.of(
                new WaveEntry("evoker", 1),
                new WaveEntry("caveSpider", 4),
                new WaveEntry("pillager", 2)
        ), null));
        defs.put(14, new WaveDefinition(List.of(new WaveEntry("phantom", 8)), null));
        defs.put(15, new WaveDefinition(List.of(), "frostRevenant"));
        defs.put(16, new WaveDefinition(List.of(
                new WaveEntry("witherSkeleton", 3),
                new WaveEntry("piglinBrute", 3)
        ), null));
        defs.put(17, new WaveDefinition(List.of(
                new WaveEntry("ravager", 2),
                new WaveEntry("pillager", 4)
        ), null));
        defs.put(18, new WaveDefinition(List.of(new WaveEntry("enderman", 4)), null));
        defs.put(19, new WaveDefinition(List.of(
                new WaveEntry("blaze", 4),
                new WaveEntry("witherSkeleton", 2),
                new WaveEntry("evoker", 1)
        ), null));
        defs.put(20, new WaveDefinition(List.of(), "hellsGatekeeper"));
        defs.put(21, new WaveDefinition(List.of(
                new WaveEntry("creeper", 4),
                new WaveEntry("enderman", 3)
        ), null));
        defs.put(22, new WaveDefinition(List.of(
                new WaveEntry("zombieBoss", 3),
                new WaveEntry("drowned", 2),
                new WaveEntry("skeleton", 3)
        ), null));
        defs.put(23, new WaveDefinition(List.of(
                new WaveEntry("husk", 6),
                new WaveEntry("blaze", 3)
        ), null));
        defs.put(24, new WaveDefinition(List.of(
                new WaveEntry("ravager", 2),
                new WaveEntry("vindicator", 3),
                new WaveEntry("evoker", 2)
        ), null));
        defs.put(25, new WaveDefinition(List.of(), "grimWither"));
        return Collections.unmodifiableMap(defs);
    }

    public Map<Integer, Wave> getWavesForDifficulty(int difficulty) {
        // Return cached waves if available
        if (cachedWaves.containsKey(difficulty)) return cachedWaves.get(difficulty);

        // Generate and cache if not available
        Map<Integer, Wave> waves = createWavesForDifficulty(difficulty);
        cachedWaves.put(difficulty, waves);
        return waves;
    }

    public Map<Integer, Wave> createWavesForDifficulty(int difficulty) {
        Map<Integer, Wave> difficultyWaves = new HashMap<>();
        DifficultyConfig config = getDifficultyConfig(difficulty);

        // Initialize mob and boss registry if empty
        if (mobRegistry.isEmpty()) initializeMobRegistry();

        // Generate waves for this difficulty
        for (int waveNum = 1; waveNum <= config.maxWaves(); waveNum++) {
            Wave wave = createWave(waveNum, config.healthMultiplier(), config.damageMultiplier(),
                    config.countMultiplier());
            difficultyWaves.put(waveNum, wave);
        }

        return difficultyWaves;
    }

    private void initializeMobRegistry() {
        // Shared drop tables
        Map<ItemStack, Double> weaponBooks = new HashMap<>();
        weaponBooks.put(getEBook(Enchantment.SHARPNESS), 0.1);
        weaponBooks.put(getEBook(Enchantment.SMITE), 0.1);
        weaponBooks.put(getEBook(Enchantment.BANE_OF_ARTHROPODS), 0.1);
        weaponBooks.put(getEBook(Enchantment.SWEEPING_EDGE), 0.1);
        weaponBooks.put(getEBook(Enchantment.KNOCKBACK), 0.1);
        weaponBooks.put(getEBook(Enchantment.FIRE_ASPECT), 0.1);
        weaponBooks.put(getEBook(Enchantment.POWER), 0.1);

        Map<ItemStack, Double> armorBooks = new HashMap<>();
        armorBooks.put(getEBook(Enchantment.PROTECTION), 0.1);
        armorBooks.put(getEBook(Enchantment.FIRE_PROTECTION), 0.1);
        armorBooks.put(getEBook(Enchantment.BLAST_PROTECTION), 0.1);
        armorBooks.put(getEBook(Enchantment.PROJECTILE_PROTECTION), 0.1);

        // Basic mobs
        mobRegistry.put("zombie", new WaveMob.Builder(ChatColor.GREEN + "Zombie", EntityType.ZOMBIE)
                .health(10).damage(2).speed(0.3).size(1.0)
                .drops(Map.of(TrialManager.getTrialItem(Material.STONE_SWORD, 1), 0.1))
                .build());

        mobRegistry.put("weakSkeleton", new WaveMob.Builder(ChatColor.GRAY + "Weak Skeleton", EntityType.SKELETON)
                .health(6).damage(3).speed(0.2).size(1.0)
                .hand(new ItemStack(Material.BOW))
                .drops(Map.of(
                        TrialManager.getTrialItem(Material.BOW, 1), 0.1,
                        TrialManager.getTrialItem(Material.ARROW, 8), 0.1
                ))
                .build());

        mobRegistry.put("spider", new WaveMob.Builder(ChatColor.BLACK + "Spider", EntityType.SPIDER)
                .health(13).damage(3).speed(0.3).size(1.25)
                .build());

        mobRegistry.put("weakZombie", new WaveMob.Builder(ChatColor.GREEN + "Weak Zombie", EntityType.ZOMBIE)
                .health(10).damage(2).speed(0.2).size(1.0)
                .drops(Map.of(TrialManager.getTrialItem(Material.STONE_SWORD, 1), 0.1))
                .build());

        mobRegistry.put("skeleton", new WaveMob.Builder(ChatColor.GRAY + "Skeleton", EntityType.SKELETON)
                .health(6).damage(2).speed(0.2).size(1.0)
                .hand(new ItemStack(Material.BOW))
                .drops(Map.of(
                        TrialManager.getTrialItem(Material.BOW, 1), 0.1,
                        TrialManager.getTrialItem(Material.ARROW, 8), 0.1
                ))
                .build());

        ItemStack[] knightArmor = {
                TrialManager.getTrialItem(Material.IRON_BOOTS, 1),
                TrialManager.getTrialItem(Material.IRON_LEGGINGS, 1),
                TrialManager.getTrialItem(Material.IRON_CHESTPLATE, 1),
                TrialManager.getTrialItem(Material.IRON_HELMET, 1)
        };
        mobRegistry.put("zombieBoss", new WaveMob.Builder(ChatColor.GREEN + "Zombie Knight", EntityType.ZOMBIE)
                .health(16).damage(3).speed(0.25).size(1.25)
                .hand(TrialManager.getTrialItem(Material.IRON_SWORD, 1))
                .armor(knightArmor)
                .drops(Map.of(
                        TrialManager.getTrialItem(Material.IRON_SWORD, 1), 0.15,
                        TrialManager.getTrialItem(Material.IRON_BOOTS, 1), 0.15,
                        TrialManager.getTrialItem(Material.IRON_LEGGINGS, 1), 0.15,
                        TrialManager.getTrialItem(Material.IRON_CHESTPLATE, 1), 0.15,
                        TrialManager.getTrialItem(Material.IRON_HELMET, 1), 0.15
                ))
                .build());

        mobRegistry.put("stray", new WaveMob.Builder(ChatColor.GRAY + "Stray", EntityType.STRAY)
                .health(13).damage(3).speed(0.2).size(1.0)
                .hand(new ItemStack(Material.BOW))
                .drops(Map.of(
                        TrialManager.getTrialItem(Material.BOW, 1), 0.1,
                        TrialManager.getTrialItem(Material.ARROW, 8), 0.1
                ))
                .build());

        mobRegistry.put("husk", new WaveMob.Builder(ChatColor.GOLD + "Husk", EntityType.HUSK)
                .health(20).damage(6).speed(0.2).size(1.0)
                .build());

        mobRegistry.put("creeper", new WaveMob.Builder(ChatColor.GREEN + "Creeper", EntityType.CREEPER)
                .health(13).damage(3).speed(0.3).size(1.0)
                .drops(Map.of(TrialManager.getTrialItem(Material.BREAD, 4), 0.25))
                .build());

        mobRegistry.put("silverfish", new WaveMob.Builder(ChatColor.GRAY + "Silverfish", EntityType.SILVERFISH)
                .health(4).damage(2).speed(0.3).size(1.0)
                .drops(weaponBooks)
                .build());

        mobRegistry.put("endermite", new WaveMob.Builder(ChatColor.DARK_PURPLE + "Endermite", EntityType.ENDERMITE)
                .health(4).damage(2).speed(0.3).size(1.0)
                .drops(weaponBooks)
                .build());

        mobRegistry.put("pillager", new WaveMob.Builder(ChatColor.GRAY + "Pillager", EntityType.PILLAGER)
                .health(13).damage(3).speed(0.2).size(1.0)
                .hand(new ItemStack(Material.CROSSBOW))
                .drops(Map.of(
                        TrialManager.getTrialItem(Material.CROSSBOW, 1), 0.1,
                        TrialManager.getTrialItem(Material.ARROW, 16), 0.1
                ))
                .build());

        mobRegistry.put("vindicator", new WaveMob.Builder(ChatColor.DARK_GREEN + "Vindicator", EntityType.VINDICATOR)
                .health(16).damage(4).speed(0.3).size(1.0)
                .hand(new ItemStack(Material.IRON_AXE))
                .drops(Map.of(TrialManager.getTrialItem(Material.IRON_AXE, 1), 0.2))
                .build());

        mobRegistry.put("drowned", new WaveMob.Builder(ChatColor.DARK_BLUE + "Drowned", EntityType.DROWNED)
                .health(16).damage(4).speed(0.2).size(1.0)
                .hand(new ItemStack(Material.TRIDENT))
                .drops(Map.of(TrialManager.getTrialItem(Material.TRIDENT, 1), 0.1))
                .build());

        mobRegistry.put("evoker", new WaveMob.Builder(ChatColor.LIGHT_PURPLE + "Evoker", EntityType.EVOKER)
                .health(20).damage(6).speed(0.2).size(1.0)
                .drops(armorBooks)
                .build());

        mobRegistry.put("caveSpider", new WaveMob.Builder(ChatColor.DARK_GREEN + "Cave Spider", EntityType.CAVE_SPIDER)
                .health(13).damage(3).speed(0.3).size(1.0)
                .build());

        mobRegistry.put("phantom", new WaveMob.Builder(ChatColor.DARK_PURPLE + "Phantom", EntityType.PHANTOM)
                .health(13).damage(3).speed(0.3).size(1.0)
                .build());

        mobRegistry.put("witherSkeleton", new WaveMob.Builder(ChatColor.BLACK + "Wither Skeleton", EntityType.WITHER_SKELETON)
                .health(13).damage(4).speed(0.35).size(1.0)
                .hand(new ItemStack(Material.STONE_SWORD))
                .build());

        mobRegistry.put("piglinBrute", new WaveMob.Builder(ChatColor.GOLD + "Piglin Brute", EntityType.PIGLIN_BRUTE)
                .health(20).damage(6).speed(0.3).size(1.0)
                .hand(new ItemStack(Material.GOLDEN_AXE))
                .drops(Map.of(TrialManager.getTrialItem(Material.COOKED_BEEF, 4), 0.1))
                .build());

        mobRegistry.put("ravager", new WaveMob.Builder(ChatColor.DARK_RED + "Ravager", EntityType.RAVAGER)
                .health(26).damage(7).speed(0.1).size(1.0)
                .drops(Map.of(TrialManager.getTrialItem(Material.COOKED_BEEF, 4), 0.1))
                .build());

        mobRegistry.put("enderman", new WaveMob.Builder(ChatColor.DARK_PURPLE + "Enderman", EntityType.ENDERMAN)
                .health(20).damage(6).speed(0.25).size(0.75)
                .build());

        mobRegistry.put("blaze", new WaveMob.Builder(ChatColor.RED + "Blaze", EntityType.BLAZE)
                .health(16).damage(4).speed(0.3).size(1.0)
                .build());

        // Boss mobs
        bossRegistry.put("blazingGhast", new BlazingGhast(armorBooks));
        bossRegistry.put("frostRevenant", new FrostRevenant(weaponBooks));
        bossRegistry.put("hellsGatekeeper", new HellsGatekeeper(null));
        bossRegistry.put("grimWither", new GrimWither(null));
    }

    private Wave createWave(int waveNum, double healthMultiplier, double damageMultiplier, double countMultiplier) {
        WaveDefinition def = WAVE_DEFINITIONS.get(waveNum);
        if (def == null) {
            throw new IllegalArgumentException("Unknown wave number: " + waveNum);
        }

        Wave wave = new Wave();
        if (def.bossId() != null) {
            wave.setBoss(getBoss(def.bossId(), healthMultiplier, damageMultiplier));
        } else {
            for (WaveEntry entry : def.entries()) {
                wave.addWaveMob(
                        getScaledMob(entry.mobId(), healthMultiplier, damageMultiplier),
                        scaleCount(entry.baseCount(), countMultiplier)
                );
            }
        }
        return wave;
    }

    private WaveMob getScaledMob(String mobId, double healthMultiplier, double damageMultiplier) {
        WaveMob baseMob = mobRegistry.get(mobId);
        if (baseMob == null) {
            throw new IllegalStateException("Unknown mob ID: " + mobId);
        }
        WaveMob scaledMob = baseMob.duplicate();

        // Apply scaling
        scaledMob.setMaxHealth((int)(Math.ceil(scaledMob.getMaxHealth() * healthMultiplier)));
        scaledMob.setDamage((int)(Math.ceil(scaledMob.getDamage() * damageMultiplier)));

        return scaledMob;
    }

    private int scaleCount(int baseCount, double countMultiplier) {
        return Math.max(1, (int)(baseCount * countMultiplier));
    }

    private DifficultyConfig getDifficultyConfig(int difficulty) {
        return switch (difficulty) {
            case EASY -> new DifficultyConfig(0.7, 0.6, 0.7, 5);
            case MEDIUM -> new DifficultyConfig(0.8, 0.8, 0.8, 10);
            case HARD -> new DifficultyConfig(1.0, 1.0, 1.0, 15);
            case GOD -> new DifficultyConfig(1.5, 1.5, 1.5, 20);
            case DEATH -> new DifficultyConfig(2.0, 2.0, 2.0, 25);
            default -> new DifficultyConfig(1.0, 1.0, 1.0, 5);
        };
    }

    public static ItemStack getEBook(Enchantment ench) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
        if (meta == null) return item;
        meta.addStoredEnchant(ench, 1, true);
        meta.getPersistentDataContainer().set(TrialManager.getTrialObjectKey(), PersistentDataType.STRING, "trialitem");
        item.setItemMeta(meta);
        return item;
    }

    private TrialBoss getBoss(String bossId, double healthMultiplier, double damageMultiplier) {
        TrialBoss baseBoss = bossRegistry.get(bossId);
        if (baseBoss == null) {
            throw new IllegalStateException("Unknown boss ID: " + bossId);
        }
        return baseBoss.duplicate(healthMultiplier, damageMultiplier);
    }

    public static String getDifficultyName(int difficulty) {
        return switch (difficulty) {
            case EASY -> "Easy";
            case MEDIUM -> "Medium";
            case HARD -> "Hard";
            case GOD -> "God";
            case DEATH -> "Death";
            default -> "Unknown";
        };
    }
}
