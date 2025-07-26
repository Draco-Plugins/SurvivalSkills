package sir_draco.survivalskills.god_questline;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.persistence.PersistentDataType;
import sir_draco.survivalskills.god_questline.TrialMobs.*;

import java.util.HashMap;

public class WaveGenerator {
    // Difficulty constants
    public static final int EASY = 1;
    public static final int MEDIUM = 2;
    public static final int HARD = 3;
    public static final int GOD = 4;
    public static final int DEATH = 5;

    // Store mobs to reuse across difficulties
    private final HashMap<String, WaveMob> mobRegistry = new HashMap<>();

    private final HashMap<Integer, HashMap<Integer, Wave>> cachedWaves = new HashMap<>();

    private BlazingGhast blazingGhast;
    private FrostRevenant frostRevenant;
    private HellsGatekeeper hellsGatekeeper;
    private GrimWither grimWither;

    public HashMap<Integer, Wave> getWavesForDifficulty(int difficulty) {
        // Return cached waves if available
        if (cachedWaves.containsKey(difficulty)) return cachedWaves.get(difficulty);

        // Generate and cache if not available
        HashMap<Integer, Wave> waves = createWavesForDifficulty(difficulty);
        cachedWaves.put(difficulty, waves);
        return waves;
    }

    public HashMap<Integer, Wave> createWavesForDifficulty(int difficulty) {
        HashMap<Integer, Wave> difficultyWaves = new HashMap<>();

        // Define scaling factors based on difficulty
        double healthMultiplier = getHealthMultiplier(difficulty);
        double damageMultiplier = getDamageMultiplier(difficulty);
        double countMultiplier = getCountMultiplier(difficulty);
        int maxWaves = getMaxWaves(difficulty);

        // Initialize mob and boss registry if empty
        if (mobRegistry.isEmpty()) initializeMobRegistry();

        // Generate waves for this difficulty
        for (int waveNum = 1; waveNum <= maxWaves; waveNum++) {
            Wave wave = createWave(waveNum, healthMultiplier, damageMultiplier, countMultiplier);
            difficultyWaves.put(waveNum, wave);
        }

        return difficultyWaves;
    }

    private void initializeMobRegistry() {
        // Basic mobs
        // Zombie
        HashMap<ItemStack, Double> zombieDrops = new HashMap<>();
        zombieDrops.put(TrialManager.getTrialItem(Material.STONE_SWORD, 1), 0.1);
        mobRegistry.put("zombie", new WaveMob(ChatColor.GREEN + "Zombie", EntityType.ZOMBIE, 10, 2,
                0.3, 1, null, null, zombieDrops));

        // Weak Skeleton
        HashMap<ItemStack, Double> skeletonDrops = new HashMap<>();
        skeletonDrops.put(TrialManager.getTrialItem(Material.BOW, 1), 0.1);
        skeletonDrops.put(TrialManager.getTrialItem(Material.ARROW, 8), 0.1);
        mobRegistry.put("weakSkeleton", new WaveMob(ChatColor.GRAY + "Weak Skeleton", EntityType.SKELETON, 6, 3,
                0.2, 1, new ItemStack(Material.BOW), null, skeletonDrops));

        // Spider
        mobRegistry.put("spider", new WaveMob(ChatColor.BLACK + "Spider", EntityType.SPIDER, 13, 3,
                0.3, 1.25, null, null, null));

        // Weak Zombie
        mobRegistry.put("weakZombie", new WaveMob(ChatColor.GREEN + "Weak Zombie", EntityType.ZOMBIE, 10, 2,
                0.2, 1, null, null, zombieDrops));

        // Skeleton
        mobRegistry.put("skeleton", new WaveMob(ChatColor.GRAY + "Skeleton", EntityType.SKELETON, 6, 2,
                0.2, 1, new ItemStack(Material.BOW), null, skeletonDrops));

        // Zombie Knight
        HashMap<ItemStack, Double> knightDrops = new HashMap<>();
        knightDrops.put(TrialManager.getTrialItem(Material.IRON_SWORD, 1), 0.15);
        knightDrops.put(TrialManager.getTrialItem(Material.IRON_BOOTS, 1), 0.15);
        knightDrops.put(TrialManager.getTrialItem(Material.IRON_LEGGINGS, 1), 0.15);
        knightDrops.put(TrialManager.getTrialItem(Material.IRON_CHESTPLATE, 1), 0.15);
        knightDrops.put(TrialManager.getTrialItem(Material.IRON_HELMET, 1), 0.15);
        ItemStack[] armor = new ItemStack[4];
        armor[0] = TrialManager.getTrialItem(Material.IRON_BOOTS, 1);
        armor[1] = TrialManager.getTrialItem(Material.IRON_LEGGINGS, 1);
        armor[2] = TrialManager.getTrialItem(Material.IRON_CHESTPLATE, 1);
        armor[3] = TrialManager.getTrialItem(Material.IRON_HELMET, 1);
        mobRegistry.put("zombieBoss", new WaveMob(ChatColor.GREEN + "Zombie Knight", EntityType.ZOMBIE, 16, 3,
                0.25, 1.25, TrialManager.getTrialItem(Material.IRON_SWORD, 1), armor, knightDrops));

        // Stray
        mobRegistry.put("stray", new WaveMob(ChatColor.GRAY + "Stray", EntityType.STRAY, 13, 3,
                0.2, 1, new ItemStack(Material.BOW), null, skeletonDrops));

        // Husk
        mobRegistry.put("husk", new WaveMob(ChatColor.GOLD + "Husk", EntityType.HUSK, 20, 6,
                0.2, 1, null, null, null));

        // Creeper
        HashMap<ItemStack, Double> creeperDrops = new HashMap<>();
        creeperDrops.put(TrialManager.getTrialItem(Material.BREAD, 4), 0.25);
        mobRegistry.put("creeper", new WaveMob(ChatColor.GREEN + "Creeper", EntityType.CREEPER, 13, 3,
                0.3, 1, null, null, creeperDrops));

        // Silverfish
        HashMap<ItemStack, Double> weaponBooks = new HashMap<>();
        weaponBooks.put(getEBook(Enchantment.SHARPNESS), 0.1);
        weaponBooks.put(getEBook(Enchantment.SMITE), 0.1);
        weaponBooks.put(getEBook(Enchantment.BANE_OF_ARTHROPODS), 0.1);
        weaponBooks.put(getEBook(Enchantment.SWEEPING_EDGE), 0.1);
        weaponBooks.put(getEBook(Enchantment.KNOCKBACK), 0.1);
        weaponBooks.put(getEBook(Enchantment.FIRE_ASPECT), 0.1);
        weaponBooks.put(getEBook(Enchantment.POWER), 0.1);
        mobRegistry.put("silverfish", new WaveMob(ChatColor.GRAY + "Silverfish", EntityType.SILVERFISH, 4, 2,
                0.3, 1, null, null, weaponBooks));

        // Endermite
        mobRegistry.put("endermite", new WaveMob(ChatColor.DARK_PURPLE + "Endermite", EntityType.ENDERMITE, 4, 2,
                0.3, 1, null, null, weaponBooks));

        // Pillager
        HashMap<ItemStack, Double> crossbowDrops = new HashMap<>();
        crossbowDrops.put(TrialManager.getTrialItem(Material.CROSSBOW, 1), 0.1);
        crossbowDrops.put(TrialManager.getTrialItem(Material.ARROW, 16), 0.1);
        mobRegistry.put("pillager", new WaveMob(ChatColor.GRAY + "Pillager", EntityType.PILLAGER, 13, 3,
                0.2, 1, new ItemStack(Material.CROSSBOW), null, crossbowDrops));

        // Vindicator
        HashMap<ItemStack, Double> vindicatorDrops = new HashMap<>();
        vindicatorDrops.put(TrialManager.getTrialItem(Material.IRON_AXE, 1), 0.2);
        mobRegistry.put("vindicator", new WaveMob(ChatColor.DARK_GREEN + "Vindicator", EntityType.VINDICATOR, 16, 4,
                0.3, 1, new ItemStack(Material.IRON_AXE), null, vindicatorDrops));

        // Drowned
        HashMap<ItemStack, Double> tridentDrops = new HashMap<>();
        tridentDrops.put(TrialManager.getTrialItem(Material.TRIDENT, 1), 0.1);
        mobRegistry.put("drowned", new WaveMob(ChatColor.DARK_BLUE + "Drowned", EntityType.DROWNED, 16, 4,
                0.2, 1, new ItemStack(Material.TRIDENT), null, tridentDrops));

        // Evoker
        HashMap<ItemStack, Double> armorBooks = new HashMap<>();
        armorBooks.put(getEBook(Enchantment.PROTECTION), 0.1);
        armorBooks.put(getEBook(Enchantment.FIRE_PROTECTION), 0.1);
        armorBooks.put(getEBook(Enchantment.BLAST_PROTECTION), 0.1);
        armorBooks.put(getEBook(Enchantment.PROJECTILE_PROTECTION), 0.1);
        mobRegistry.put("evoker", new WaveMob(ChatColor.LIGHT_PURPLE + "Evoker", EntityType.EVOKER, 20, 6,
                0.2, 1, null, null, armorBooks));

        // Cave Spider
        mobRegistry.put("caveSpider", new WaveMob(ChatColor.DARK_GREEN + "Cave Spider", EntityType.CAVE_SPIDER, 13, 3,
                0.3, 1, null, null, null));

        // Phantom
        mobRegistry.put("phantom", new WaveMob(ChatColor.DARK_PURPLE + "Phantom", EntityType.PHANTOM, 13, 3,
                0.3, 1, null, null, null));

        // Wither Skeleton
        mobRegistry.put("witherSkeleton", new WaveMob(ChatColor.BLACK + "Wither Skeleton", EntityType.WITHER_SKELETON, 13, 4,
                0.35, 1, new ItemStack(Material.STONE_SWORD), null, null));

        // Piglin Brute
        HashMap<ItemStack, Double> meatDrops = new HashMap<>();
        meatDrops.put(TrialManager.getTrialItem(Material.COOKED_BEEF, 4), 0.1);
        mobRegistry.put("piglinBrute", new WaveMob(ChatColor.GOLD + "Piglin Brute", EntityType.PIGLIN_BRUTE, 20, 6,
                0.3, 1, new ItemStack(Material.GOLDEN_AXE), null, meatDrops));

        // Ravager
        mobRegistry.put("ravager", new WaveMob(ChatColor.DARK_RED + "Ravager", EntityType.RAVAGER, 26, 7,
                0.1, 1, null, null, meatDrops));

        // Enderman
        mobRegistry.put("enderman", new WaveMob(ChatColor.DARK_PURPLE + "Enderman", EntityType.ENDERMAN, 20, 6,
                0.25, 0.75, null, null, null));

        // Blaze
        mobRegistry.put("blaze", new WaveMob(ChatColor.RED + "Blaze", EntityType.BLAZE, 16, 4,
                0.3, 1, null, null, null));

        // Boss mobs
        blazingGhast = new BlazingGhast(armorBooks);
        frostRevenant = new FrostRevenant(weaponBooks);
        hellsGatekeeper = new HellsGatekeeper(null);
        grimWither = new GrimWither(null);
    }

    private Wave createWave(int waveNum, double healthMultiplier, double damageMultiplier, double countMultiplier) {
        Wave wave = new Wave();

        switch (waveNum) {
            case 1:
                wave.addWaveMob(getScaledMob("weakZombie", healthMultiplier, damageMultiplier),
                        scaleCount(3, countMultiplier));
                break;
            case 2:
                wave.addWaveMob(getScaledMob("weakSkeleton", healthMultiplier, damageMultiplier),
                        scaleCount(3, countMultiplier));
                break;
            case 3:
                wave.addWaveMob(getScaledMob("spider", healthMultiplier, damageMultiplier),
                        scaleCount(5, countMultiplier));
                break;
            case 4:
                wave.addWaveMob(getScaledMob("weakZombie", healthMultiplier, damageMultiplier),
                        scaleCount(3, countMultiplier));
                wave.addWaveMob(getScaledMob("skeleton", healthMultiplier, damageMultiplier),
                        scaleCount(2, countMultiplier));
                break;
            case 5:
                wave.addWaveMob(getScaledMob("zombieBoss", healthMultiplier, damageMultiplier));
                wave.addWaveMob(getScaledMob("zombie", healthMultiplier, damageMultiplier),
                        scaleCount(3, countMultiplier));
                break;
            case 6:
                wave.addWaveMob(getScaledMob("stray", healthMultiplier, damageMultiplier),
                        scaleCount(3, countMultiplier));
                wave.addWaveMob(getScaledMob("spider", healthMultiplier, damageMultiplier),
                        scaleCount(3, countMultiplier));
                break;
            case 7:
                wave.addWaveMob(getScaledMob("husk", healthMultiplier, damageMultiplier),
                        scaleCount(6, countMultiplier));
                break;
            case 8:
                wave.addWaveMob(getScaledMob("creeper", healthMultiplier, damageMultiplier),
                        scaleCount(4, countMultiplier));
                wave.addWaveMob(getScaledMob("weakSkeleton", healthMultiplier, damageMultiplier),
                        scaleCount(2, countMultiplier));
                break;
            case 9:
                wave.addWaveMob(getScaledMob("silverfish", healthMultiplier, damageMultiplier),
                        scaleCount(6, countMultiplier));
                wave.addWaveMob(getScaledMob("endermite", healthMultiplier, damageMultiplier),
                        scaleCount(6, countMultiplier));
                break;
            case 10:
                wave.setBossWave(true);
                wave.setBoss(getBlazingGhast(healthMultiplier, damageMultiplier));
                break;
            case 11:
                wave.addWaveMob(getScaledMob("pillager", healthMultiplier, damageMultiplier),
                        scaleCount(4, countMultiplier));
                wave.addWaveMob(getScaledMob("vindicator", healthMultiplier, damageMultiplier),
                        scaleCount(2, countMultiplier));
                break;
            case 12:
                wave.addWaveMob(getScaledMob("drowned", healthMultiplier, damageMultiplier),
                        scaleCount(3, countMultiplier));
                break;
            case 13:
                wave.addWaveMob(getScaledMob("evoker", healthMultiplier, damageMultiplier));
                wave.addWaveMob(getScaledMob("caveSpider", healthMultiplier, damageMultiplier),
                        scaleCount(4, countMultiplier));
                wave.addWaveMob(getScaledMob("pillager", healthMultiplier, damageMultiplier),
                        scaleCount(2, countMultiplier));
                break;
            case 14:
                wave.addWaveMob(getScaledMob("phantom", healthMultiplier, damageMultiplier),
                        scaleCount(8, countMultiplier));
                break;
            case 15:
                wave.setBossWave(true);
                wave.setBoss(getFrostRevenant(healthMultiplier, damageMultiplier));
                break;
            case 16:
                wave.addWaveMob(getScaledMob("witherSkeleton", healthMultiplier, damageMultiplier),
                        scaleCount(3, countMultiplier));
                wave.addWaveMob(getScaledMob("piglinBrute", healthMultiplier, damageMultiplier),
                        scaleCount(3, countMultiplier));
                break;
            case 17:
                wave.addWaveMob(getScaledMob("ravager", healthMultiplier, damageMultiplier),
                        scaleCount(2, countMultiplier));
                wave.addWaveMob(getScaledMob("pillager", healthMultiplier, damageMultiplier),
                        scaleCount(4, countMultiplier));
                break;
            case 18:
                wave.addWaveMob(getScaledMob("enderman", healthMultiplier, damageMultiplier),
                        scaleCount(4, countMultiplier));
                break;
            case 19:
                wave.addWaveMob(getScaledMob("blaze", healthMultiplier, damageMultiplier),
                        scaleCount(4, countMultiplier));
                wave.addWaveMob(getScaledMob("witherSkeleton", healthMultiplier, damageMultiplier),
                        scaleCount(2, countMultiplier));
                wave.addWaveMob(getScaledMob("evoker", healthMultiplier, damageMultiplier));
                break;
            case 20:
                wave.setBossWave(true);
                wave.setBoss(getHellsGatekeeper(healthMultiplier, damageMultiplier));
                break;
            case 21:
                wave.addWaveMob(getScaledMob("creeper", healthMultiplier, damageMultiplier),
                        scaleCount(4, countMultiplier));
                wave.addWaveMob(getScaledMob("enderman", healthMultiplier, damageMultiplier),
                        scaleCount(3, countMultiplier));
                break;
            case 22:
                wave.addWaveMob(getScaledMob("zombieBoss", healthMultiplier, damageMultiplier),
                        scaleCount(3, countMultiplier));
                wave.addWaveMob(getScaledMob("drowned", healthMultiplier, damageMultiplier),
                        scaleCount(2, countMultiplier));
                wave.addWaveMob(getScaledMob("skeleton", healthMultiplier, damageMultiplier),
                        scaleCount(3, countMultiplier));
                break;
            case 23:
                wave.addWaveMob(getScaledMob("husk", healthMultiplier, damageMultiplier),
                        scaleCount(6, countMultiplier));
                wave.addWaveMob(getScaledMob("blaze", healthMultiplier, damageMultiplier),
                        scaleCount(3, countMultiplier));
                break;
            case 24:
                wave.addWaveMob(getScaledMob("ravager", healthMultiplier, damageMultiplier),
                        scaleCount(2, countMultiplier));
                wave.addWaveMob(getScaledMob("vindicator", healthMultiplier, damageMultiplier),
                        scaleCount(3, countMultiplier));
                wave.addWaveMob(getScaledMob("evoker", healthMultiplier, damageMultiplier),
                        scaleCount(2, countMultiplier));
                break;
            case 25:
                wave.setBossWave(true);
                wave.setBoss(getGrimWither(healthMultiplier, damageMultiplier));
                break;
        }

        return wave;
    }

    private WaveMob getScaledMob(String mobId, double healthMultiplier, double damageMultiplier) {
        WaveMob baseMob = mobRegistry.get(mobId);
        WaveMob scaledMob = baseMob.duplicate();

        // Apply scaling
        scaledMob.setMaxHealth((int)(Math.ceil(scaledMob.getHealth() * healthMultiplier)));
        scaledMob.setDamage((int)(Math.ceil(scaledMob.getDamage() * damageMultiplier)));

        return scaledMob;
    }

    private int scaleCount(int baseCount, double countMultiplier) {
        return Math.max(1, (int)(baseCount * countMultiplier));
    }

    private double getHealthMultiplier(int difficulty) {
        return switch (difficulty) {
            case EASY -> 0.7;
            case MEDIUM -> 0.8;
            case GOD -> 1.5;
            case DEATH -> 2.0;
            default -> 1.0;
        };
    }

    private double getDamageMultiplier(int difficulty) {
        return switch (difficulty) {
            case EASY -> 0.6;
            case MEDIUM -> 0.8;
            case GOD -> 1.5;
            case DEATH -> 2.0;
            default -> 1.0;
        };
    }

    private double getCountMultiplier(int difficulty) {
        return switch (difficulty) {
            case EASY -> 0.7;
            case MEDIUM -> 0.8;
            case GOD -> 1.5;
            case DEATH -> 2.0;
            default -> 1.0;
        };
    }

    private int getMaxWaves(int difficulty) {
        return switch (difficulty) {
            case MEDIUM -> 10;
            case HARD -> 15;
            case GOD -> 20;
            case DEATH -> 25;
            default -> 5;
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

    public BlazingGhast getBlazingGhast(double healthMultiplier, double damageMultiplier) {
        return blazingGhast.duplicate(healthMultiplier, damageMultiplier);
    }

    public FrostRevenant getFrostRevenant(double healthMultiplier, double damageMultiplier) {
        return frostRevenant.duplicate(healthMultiplier, damageMultiplier);
    }

    public HellsGatekeeper getHellsGatekeeper(double healthMultiplier, double damageMultiplier) {
        return hellsGatekeeper.duplicate(healthMultiplier, damageMultiplier);
    }

    public GrimWither getGrimWither(double healthMultiplier, double damageMultiplier) {
        return grimWither.duplicate(healthMultiplier, damageMultiplier);
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
