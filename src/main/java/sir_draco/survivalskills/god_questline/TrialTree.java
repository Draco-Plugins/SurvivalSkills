package sir_draco.survivalskills.god_questline;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.util.*;

@SuppressWarnings("unchecked")
public class TrialTree {

    public enum UpgradeType {
        STARTING_WEAPON,
        STARTING_ARMOR,
        STARTING_FOOD,
        ARROW_QUANTITY,
        DAMAGE_BOOST,
        SPEED_BOOST,
        DODGE_CHANCE,
        ENCHANT_CHANCE
    }

    public static class TrialUpgrade {
        private final String id;
        private final String name;
        private final String description;
        private final UpgradeType type;
        private final int maxLevel;
        private final Map<Integer, Integer> costPerLevel;
        private final Map<String, Object> upgradeData;

        public TrialUpgrade(String id, String name, String description, UpgradeType type,
                            int maxLevel, Map<Integer, Integer> costPerLevel, Map<String, Object> upgradeData) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.type = type;
            this.maxLevel = maxLevel;
            this.costPerLevel = costPerLevel;
            this.upgradeData = upgradeData;
        }

        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public UpgradeType getType() { return type; }
        public int getMaxLevel() { return maxLevel; }
        public int getCost(int level) { return costPerLevel.getOrDefault(level, 0); }
        public Map<String, Object> getUpgradeData() { return upgradeData; }
    }

    private static final Map<String, TrialUpgrade> UPGRADES = new HashMap<>();

    static {
        initializeUpgrades();
    }

    private static void initializeUpgrades() {
        // Starting Weapon Upgrades
        Map<Integer, Integer> weaponCosts = Map.of(1, 300, 2, 1000, 3, 2000, 4, 3500);
        Map<String, Object> weaponData = Map.of("materials", Arrays.asList(
                Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD));
        UPGRADES.put("starting_weapon", new TrialUpgrade("starting_weapon", "Better Starting Weapon",
                "Upgrade your starting weapon quality", UpgradeType.STARTING_WEAPON, 4, weaponCosts, weaponData));

        // Starting Armor Upgrades
        Map<Integer, Integer> armorCosts = Map.of(1, 500, 2, 1500, 3, 3000, 4, 5000, 5, 7500);
        Map<String, Object> armorData = Map.of(
                "helmet", Arrays.asList(Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET,
                                        Material.DIAMOND_HELMET, Material.NETHERITE_HELMET),
                "chestplate", Arrays.asList(Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE,
                                            Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE),
                "leggings", Arrays.asList(Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS,
                                          Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS),
                "boots", Arrays.asList(Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS,
                                       Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS));
        UPGRADES.put("starting_armor", new TrialUpgrade("starting_armor", "Better Starting Armor",
                "Upgrade your starting armor quality", UpgradeType.STARTING_ARMOR, 4, armorCosts, armorData));

        // Starting Food Upgrades
        Map<Integer, Integer> foodCosts = Map.of(1, 200, 2, 700, 3, 1400, 4, 2500);
        Map<String, Object> foodData = Map.of("amounts", Arrays.asList(1, 3, 8, 16, 32));
        UPGRADES.put("starting_food", new TrialUpgrade("starting_food", "More Starting Food",
                "Increase starting food quantity", UpgradeType.STARTING_FOOD, 4, foodCosts, foodData));

        // Arrow Quantity Upgrades
        Map<Integer, Integer> arrowCosts = Map.of(1, 1000, 2, 1500, 3, 1700, 4, 2000, 5, 3000);
        Map<String, Object> arrowData = Map.of("amounts", Arrays.asList(0, 16, 32, 48, 64, 96));
        UPGRADES.put("arrow_quantity", new TrialUpgrade("arrow_quantity", "Starting Arrows",
                "Start with arrows for ranged combat", UpgradeType.ARROW_QUANTITY, 5, arrowCosts, arrowData));

        // Damage Boost Upgrades
        Map<Integer, Integer> damageCosts = Map.of(1, 1000, 2, 2000, 3, 4000, 4, 6000, 5, 8000);
        Map<String, Object> damageData = Map.of("multipliers", Arrays.asList(1.0, 1.1, 1.2, 1.35, 1.5, 1.75));
        UPGRADES.put("damage_boost", new TrialUpgrade("damage_boost", "Damage Boost",
                "Increase damage dealt to trial mobs", UpgradeType.DAMAGE_BOOST, 5, damageCosts, damageData));

        // Speed Boost Upgrades
        Map<Integer, Integer> speedCosts = Map.of(1, 800, 2, 1600, 3, 3200, 4, 4800);
        Map<String, Object> speedData = Map.of("multipliers", Arrays.asList(1.0, 1.15, 1.3, 1.5, 2.0));
        UPGRADES.put("speed_boost", new TrialUpgrade("speed_boost", "Speed Boost",
                "Increase movement speed during trials", UpgradeType.SPEED_BOOST, 3, speedCosts, speedData));

        // Dodge Chance Upgrades
        Map<Integer, Integer> dodgeCosts = Map.of(1, 1250, 2, 3000, 3, 5000, 4, 7500);
        Map<String, Object> dodgeData = Map.of("chances", Arrays.asList(0.0, 0.05, 0.1, 0.15, 0.25));
        UPGRADES.put("dodge_chance", new TrialUpgrade("dodge_chance", "Dodge Chance",
                "Chance to avoid damage completely", UpgradeType.DODGE_CHANCE, 4, dodgeCosts, dodgeData));

        // Enchant Chance Upgrades
        Map<Integer, Integer> enchantCosts = Map.of(1, 1600, 2, 4000, 3, 8000);
        Map<String, Object> enchantData = Map.of("chances", Arrays.asList(0.0, 0.15, 0.3, 0.5));
        UPGRADES.put("enchant_chance", new TrialUpgrade("enchant_chance", "Enchanted Rewards",
                "Chance for wave rewards to have enchantments", UpgradeType.ENCHANT_CHANCE, 3, enchantCosts, enchantData));
    }

    public static Map<String, TrialUpgrade> getAllUpgrades() {
        return new HashMap<>(UPGRADES);
    }

    public static TrialUpgrade getUpgrade(String id) {
        return UPGRADES.get(id);
    }

    public static List<ItemStack> getStartingItems(PlayerTrialUpgrades upgrades) {
        List<ItemStack> items = new ArrayList<>();

        // Add weapon
        int weaponLevel = upgrades.getUpgradeLevel("starting_weapon");
        TrialUpgrade weaponUpgrade = getUpgrade("starting_weapon");
        List<Material> weapons = (List<Material>) weaponUpgrade.getUpgradeData().get("materials");
        items.add(TrialManager.getTrialItem(weapons.get(weaponLevel), 1));

        // Add armor if upgraded
        int armorLevel = upgrades.getUpgradeLevel("starting_armor");
        if (armorLevel > 0) {
            TrialUpgrade armorUpgrade = getUpgrade("starting_armor");
            Map<String, Object> armorData = armorUpgrade.getUpgradeData();

            List<Material> helmets = (List<Material>) armorData.get("helmet");
            List<Material> chestplates = (List<Material>) armorData.get("chestplate");
            List<Material> leggings = (List<Material>) armorData.get("leggings");
            List<Material> boots = (List<Material>) armorData.get("boots");

            items.add(TrialManager.getTrialItem(helmets.get(armorLevel), 1));
            items.add(TrialManager.getTrialItem(chestplates.get(armorLevel), 1));
            items.add(TrialManager.getTrialItem(leggings.get(armorLevel), 1));
            items.add(TrialManager.getTrialItem(boots.get(armorLevel), 1));
        }

        // Add food
        int foodLevel = upgrades.getUpgradeLevel("starting_food");
        TrialUpgrade foodUpgrade = getUpgrade("starting_food");
        List<Integer> foodAmounts = (List<Integer>) foodUpgrade.getUpgradeData().get("amounts");
        items.add(TrialManager.getTrialItem(Material.COOKED_BEEF, foodAmounts.get(foodLevel)));

        // Add arrows
        int arrowLevel = upgrades.getUpgradeLevel("arrow_quantity");
        if (arrowLevel > 0) {
            TrialUpgrade arrowUpgrade = getUpgrade("arrow_quantity");
            List<Integer> arrowAmounts = (List<Integer>) arrowUpgrade.getUpgradeData().get("amounts");
            items.add(TrialManager.getTrialItem(Material.ARROW, arrowAmounts.get(arrowLevel)));
            items.add(TrialManager.getTrialItem(Material.BOW, 1));
        }

        return items;
    }

    public static double getDamageMultiplier(PlayerTrialUpgrades upgrades) {
        int level = upgrades.getUpgradeLevel("damage_boost");
        TrialUpgrade upgrade = getUpgrade("damage_boost");
        List<Double> multipliers = (List<Double>) upgrade.getUpgradeData().get("multipliers");
        return multipliers.get(level);
    }

    public static double getSpeedMultiplier(PlayerTrialUpgrades upgrades) {
        int level = upgrades.getUpgradeLevel("speed_boost");
        TrialUpgrade upgrade = getUpgrade("speed_boost");
        List<Double> multipliers = (List<Double>) upgrade.getUpgradeData().get("multipliers");
        return multipliers.get(level);
    }

    public static double getDodgeChance(PlayerTrialUpgrades upgrades) {
        int level = upgrades.getUpgradeLevel("dodge_chance");
        TrialUpgrade upgrade = getUpgrade("dodge_chance");
        List<Double> chances = (List<Double>) upgrade.getUpgradeData().get("chances");
        return chances.get(level);
    }

    public static double getEnchantChance(PlayerTrialUpgrades upgrades) {
        int level = upgrades.getUpgradeLevel("enchant_chance");
        TrialUpgrade upgrade = getUpgrade("enchant_chance");
        List<Double> chances = (List<Double>) upgrade.getUpgradeData().get("chances");
        return chances.get(level);
    }

    @SuppressWarnings("deprecation")
    public static ItemStack applyRandomEnchantments(ItemStack item, double enchantChance) {
        if (Math.random() > enchantChance) return item;

        List<Enchantment> validEnchants = new ArrayList<>();
        for (Enchantment enchant : Enchantment.values()) {
            if (enchant.canEnchantItem(item)) {
                validEnchants.add(enchant);
            }
        }

        if (validEnchants.isEmpty()) return item;

        Random random = new Random();
        int enchantCount = random.nextInt(3) + 1; // 1-3 enchantments

        for (int i = 0; i < enchantCount && !validEnchants.isEmpty(); i++) {
            Enchantment enchant = validEnchants.remove(random.nextInt(validEnchants.size()));
            int level = random.nextInt(enchant.getMaxLevel()) + 1;
            item.addUnsafeEnchantment(enchant, level);
        }

        return item;
    }
}