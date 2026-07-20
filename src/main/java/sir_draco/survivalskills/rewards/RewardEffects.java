package sir_draco.survivalskills.rewards;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.abilities.AutoTrash;

import java.util.*;

public class RewardEffects {

    @FunctionalInterface
    public interface RewardEffect {
        void apply(PlayerRewards pr, Player p);
    }

    @FunctionalInterface
    public interface DoubleRewardSetter {
        void accept(PlayerRewards pr, double value);
    }

    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private static String toRoman(int n) {
        if (n < 1 || n > 10) throw new IllegalArgumentException("Roman numeral out of range: " + n);
        return ROMAN[n - 1];
    }

    private static final Map<String, RewardEffect> EFFECTS = buildEffects();

    public static RewardEffect getEffect(String type, String reward) {
        return EFFECTS.get(type + ":" + reward);
    }

    public static Set<String> getRegisteredEffectKeys() {
        return Collections.unmodifiableSet(EFFECTS.keySet());
    }

    // -----------------------------------------------------------------------
    // Builder
    // -----------------------------------------------------------------------

    private static Map<String, RewardEffect> buildEffects() {
        var m = new HashMap<String, RewardEffect>();

        // -- Mining --
        tieredDouble(m, "Mining", "Fortune", map(1, 0.2, 2, 0.4, 3, 0.5), (pr, val) -> pr.setFortuneChance(val));
        tieredDouble(m, "Mining", "Armor", map(1, 0.05, 2, 0.10, 3, 0.15, 4, 0.20), (pr, val) -> pr.setProtectionPercentage(val));
        put(m, "Mining", "UnbreakableTools", (pr, p) -> pr.setUnbreakableTools(true));
        put(m, "Mining", "VeinMinerII", (pr, p) -> {
            SurvivalSkills.getInstance().getMiningListener().getVeinminerTracker().put(p, true);
            pr.setUnbreakableTools(true);
        });

        // -- Exploring --
        linearSwimSpeed(m, "Exploring", "Swim", 5, 1.4, 0.4);
        linearWalkSpeed(m, "Exploring", "Speed", 5, 0.22f, 0.02f);

        // -- Farming --
        tieredDouble(m, "Farming", "DoubleCrops", map(1, 0.25, 2, 0.5, 3, 0.75, 4, 1.0), (pr, val) -> pr.setCropDoubleChance(val));
        linearHealth(m, "Farming", "Health", 10, 11);
        put(m, "Farming", "Timberman", (pr, p) -> {
            if (p.hasPermission("timberman.use")) return;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + p.getName() + " permission set timberman.use true");
        });

        // -- Building --
        linearDouble(m, "Building", "BlockReturn", 10, 0.05, 0.05, (pr, val) -> pr.setBlockBlackChance(val));
        put(m, "Building", "ExtendedReach", (pr, p) -> {
            AttributeInstance reach = p.getAttribute(Attribute.BLOCK_INTERACTION_RANGE);
            if (reach != null) reach.setBaseValue(6);
        });

        // -- Fighting --
        tieredDouble(m, "Fighting", "Lifesteal", map(1, 0.05, 2, 0.10, 3, 0.15, 4, 0.20, 5, 0.25), (pr, val) -> pr.setLifesteal(val));
        tieredDouble(m, "Fighting", "Critical", map(1, 0.10, 2, 0.20), (pr, val) -> pr.setCriticalChance(val));
        put(m, "Fighting", "BloodyDomain", (pr, p) -> SurvivalSkills.getInstance().getAbilityManager().startBloodyDomain(p));

        // -- Fishing --
        tieredDouble(m, "Fishing", "CommonLoot", map(1, 0.25, 2, 0.40, 3, 0.55, 4, 0.65, 5, 0.75), (pr, val) -> pr.setCommonFishingLootChance(val));
        tieredDouble(m, "Fishing", "RareLoot", map(1, 0.05, 2, 0.15, 3, 0.20, 4, 0.25, 5, 0.30), (pr, val) -> pr.setRareFishingLootChance(val));
        tieredDouble(m, "Fishing", "EpicLoot", map(1, 0.005, 2, 0.01, 3, 0.015, 4, 0.02, 5, 0.025), (pr, val) -> pr.setEpicFishingLootChance(val));
        tieredDouble(m, "Fishing", "LegendaryLoot", map(1, 0.002, 2, 0.0035, 3, 0.005), (pr, val) -> pr.setLegendaryFishingLootChance(val));
        linearDouble(m, "Fishing", "Experience", 10, 1.1, 0.1, (pr, val) -> pr.setExperienceMultiplier(val));
        fasterFishingEffects(m);
        put(m, "Fishing", "AutoTrashII", (pr, p) -> {
            AutoTrash trash = SurvivalSkills.getInstance().getFishingListener().getTrashInventories().get(p);
            if (trash != null) trash.upgradeTrashSize();
        });

        // -- Crafting --
        linearDouble(m, "Crafting", "ExtraOutput", 10, 0.05, 0.05, (pr, val) -> pr.setExtraOutput(val));
        linearDouble(m, "Crafting", "MaterialsBack", 5, 0.10, 0.10, (pr, val) -> pr.setMaterialsBack(val));

        // -- Main --
        setHomePermissions(m);

        return Collections.unmodifiableMap(m);
    }

    // -----------------------------------------------------------------------
    // Specialised family builders
    // -----------------------------------------------------------------------

    private static void fasterFishingEffects(Map<String, RewardEffect> m) {
        put(m, "Fishing", "FasterFishingI", (pr, p) -> {
            pr.setFishingMaxTickSpeed(500);
            pr.setFishingMinTickSpeed(80);
        });
        put(m, "Fishing", "FasterFishingII", (pr, p) -> {
            pr.setFishingMaxTickSpeed(350);
            pr.setFishingMinTickSpeed(60);
        });
        put(m, "Fishing", "FasterFishingIII", (pr, p) -> {
            pr.setFishingMaxTickSpeed(225);
            pr.setFishingMinTickSpeed(45);
        });
        put(m, "Fishing", "FasterFishingIV", (pr, p) -> {
            pr.setFishingMaxTickSpeed(150);
            pr.setFishingMinTickSpeed(30);
        });
        put(m, "Fishing", "FasterFishingV", (pr, p) -> {
            pr.setFishingMaxTickSpeed(80);
            pr.setFishingMinTickSpeed(20);
        });
    }

    private static void setHomePermissions(Map<String, RewardEffect> m) {
        String type = "Main";
        String prefix = "SetHome";
        String[] perms = {"one", "two", "three", "four"};
        for (int level = 1; level <= 4; level++) {
            String perm = "essentials.sethome.multiple." + perms[level - 1];
            String key = type + ":" + prefix + toRoman(level);
            m.put(key, (pr, p) -> {
                if (Bukkit.getServer().getPluginManager().getPlugin("Essentials") == null
                        || !Bukkit.getServer().getPluginManager().isPluginEnabled("Essentials")) return;
                if (p.hasPermission(perm)) return;
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + p.getName() + " permission set " + perm + " true");
            });
        }
    }

    // -----------------------------------------------------------------------
    // Generic helpers
    // -----------------------------------------------------------------------

    private static void put(Map<String, RewardEffect> m, String type, String reward, RewardEffect effect) {
        m.put(type + ":" + reward, effect);
    }

    private static void tieredDouble(Map<String, RewardEffect> m, String type, String prefix,
                                      Map<Integer, Double> values,
                                      DoubleRewardSetter setter) {
        for (var entry : values.entrySet()) {
            double val = entry.getValue();
            m.put(type + ":" + prefix + toRoman(entry.getKey()), (pr, p) -> setter.accept(pr, val));
        }
    }

    private static void linearDouble(Map<String, RewardEffect> m, String type, String prefix,
                                      int maxLevel, double start, double step,
                                      DoubleRewardSetter setter) {
        for (int level = 1; level <= maxLevel; level++) {
            double val = start + (level - 1) * step;
            m.put(type + ":" + prefix + toRoman(level), (pr, p) -> setter.accept(pr, val));
        }
    }

    private static void linearSwimSpeed(Map<String, RewardEffect> m, String type, String prefix,
                                         int maxLevel, double start, double step) {
        for (int level = 1; level <= maxLevel; level++) {
            double val = start + (level - 1) * step;
            m.put(type + ":" + prefix + toRoman(level), (pr, p) -> pr.setSwimSpeed(val));
        }
    }

    private static void linearWalkSpeed(Map<String, RewardEffect> m, String type, String prefix,
                                         int maxLevel, float start, float step) {
        for (int level = 1; level <= maxLevel; level++) {
            float val = start + (level - 1) * step;
            m.put(type + ":" + prefix + toRoman(level), (pr, p) -> p.setWalkSpeed(val));
        }
    }

    private static void linearHealth(Map<String, RewardEffect> m, String type, String prefix,
                                      int maxLevel, int startHearts) {
        for (int level = 1; level <= maxLevel; level++) {
            int hearts = startHearts + level - 1;
            int hp = hearts * 2;
            m.put(type + ":" + prefix + toRoman(level), (pr, p) -> pr.setPlayerMaxHealth(p, hp));
        }
    }

    // -----------------------------------------------------------------------
    // Map / utils
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> map(Object... entries) {
        if (entries.length % 2 != 0) throw new IllegalArgumentException("Odd number of entries");
        var map = new LinkedHashMap<K, V>();
        for (int i = 0; i < entries.length; i += 2)
            map.put((K) entries[i], (V) entries[i + 1]);
        return map;
    }
}
