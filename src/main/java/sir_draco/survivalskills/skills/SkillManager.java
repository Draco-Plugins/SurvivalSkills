package sir_draco.survivalskills.skills;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.abilities.AbilityTimer;
import sir_draco.survivalskills.boards.Leaderboard;
import sir_draco.survivalskills.boards.LeaderboardPlayer;
import sir_draco.survivalskills.boards.SkillScoreboard;
import sir_draco.survivalskills.rewards.PlayerRewards;
import sir_draco.survivalskills.rewards.Reward;
import sir_draco.survivalskills.trophy.TrophyType;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.FileUtils;
import sir_draco.survivalskills.utils.SkillDisplay;
import sir_draco.survivalskills.utils.items.ItemStackGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public class SkillManager {

    public static final double scalar = SkillMath.SCALAR;
    public static final double exponentialScalar = SkillMath.EXPONENTIAL_SCALAR;
    private static final String EXPERIENCE_FIELD = "Experience";
    private static final String LEVEL_FIELD = "Level";
    private static final String MULTIPLIER_FIELD = "Multiplier";
    private static final String MULTIPLIER_TIMER_FIELD = "MultiplierTimer";
    private static final String XP_VOUCHER_ABILITY = "XPVoucher";
    public static final String MINING = "Mining";
    public static final String EXPLORING = "Exploring";
    public static final String FARMING = "Farming";
    public static final String BUILDING = "Building";
    public static final String FIGHTING = "Fighting";
    public static final String FISHING = "Fishing";
    public static final String CRAFTING = "Crafting";
    public static final String MAIN = "Main";

    private final SurvivalSkills plugin;
    private final Map<SkillCategory, Double> xpMultipliers = new EnumMap<>(SkillCategory.class);
    private final Map<UUID, Map<SkillCategory, Boolean>> maxLevelMessagesShown = new HashMap<>();
    private static final HashMap<UUID, SkillsHolder> playerSkills = new HashMap<>();

    private PlayerRewards defaultPlayerRewards; // Holds the default information for rewards
    private double multiplier = 1;

    public SkillManager(SurvivalSkills plugin) {
        this.plugin = plugin;
        loadMultipliers();
        loadDefaultRewards();
        runSkillAutoSave();
    }

    private boolean hasShownMaxLevelMessage(UUID uuid, SkillCategory skillCategory) {
        return maxLevelMessagesShown.getOrDefault(uuid, Map.of()).getOrDefault(skillCategory, false);
    }

    private void markMaxLevelMessageShown(UUID uuid, SkillCategory skillCategory) {
        maxLevelMessagesShown.computeIfAbsent(uuid, ignored -> new HashMap<>()).put(skillCategory, true);
    }

    public void clearMaxLevelMessages(UUID uuid) {
        maxLevelMessagesShown.remove(uuid);
    }

    public static void experienceEvent(SurvivalSkills plugin, Player p, double xp, SkillCategory skillCategory) {
        // Handle multipliers
        xp *= plugin.getSkillManager().getMultiplier();
        if (plugin.getAbilityManager().getAbility(p, "XPVoucher") != null)
            xp *= plugin.getSkillManager().getPlayerMultiplier(p);

        UUID uuid = p.getUniqueId();
        Skill skill = SkillManager.getSkill(uuid, skillCategory);
        if (skill == null) {
            Bukkit.getLogger().log(Level.WARNING, "Skill not found for player " + p.getName() + ": " + skillCategory);
            return;
        }

        if (skill.getLevel() >= plugin.getTrophyManager().playerMaxSkillLevel(uuid)) {
            SkillScoreboard.updateScoreboard(p, skillCategory);
            if (skill.getLevel() == Skill.MAX_LEVEL
                    || plugin.getSkillManager().hasShownMaxLevelMessage(uuid, skillCategory)) return;
            if (!plugin.getSkillManager().isMaxSkillMessageEnabled(p)) return;
            p.sendRawMessage(ChatColor.DARK_BLUE + "You have reached your current max level for: " + ChatColor.AQUA + skillCategory);
            p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
            plugin.getSkillManager().markMaxLevelMessageShown(uuid, skillCategory);
            return;
        }

        if (skill.getLevel() >= Skill.MAX_LEVEL) {
            SkillScoreboard.updateScoreboard(p);
            return;
        }
        xp = checkXPCap(skill.getExperience(), xp, plugin.getTrophyManager().playerMaxSkillLevel(uuid), skillCategory);

        if (plugin.getShowScoreboard().containsKey(p.getUniqueId())
                && plugin.getShowScoreboard().get(p.getUniqueId())
                && xp != 0)
            sendActionBarMessage(p, ChatColor.GRAY + skillCategory.getDisplayName() + ChatColor.YELLOW + " (+" + xp + ")");
        if (skill.changeExperience(xp, plugin.getTrophyManager().playerMaxSkillLevel(uuid))) {
            SkillDisplay.levelUpNotification(p, skill);

            // Ensure that player rewards are loaded
            PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
            if (rewards == null) {
                Bukkit.getLogger().warning("Player rewards are not loaded for " + p.getName());
                return;
            }

            rewards.handleReward(p, skill, true);
            plugin.getSkillManager().updateLeaderboardScore(p, skill.getSkillCategory());

            if (skill.getSkillCategory() == SkillCategory.MAIN) {
                SkillScoreboard.updateNametags(p);
            }
        }

        // Check the main skill
        plugin.getSkillManager().syncMainSkill(p);
        SkillScoreboard.updateScoreboard(p, skillCategory);
    }

    /**
     * Ensures that the XP cap is not exceeded
     */
    public static double checkXPCap(double totalXP, double xp, int levelCap, SkillCategory skillCategory) {
        double xpForLevel = totalExperienceForLevel(levelCap, skillCategory);
        if (totalXP > xpForLevel)
            return 0;
        if (totalXP + xp > xpForLevel)
            return xpForLevel - totalXP;
        return xp;
    }

    public static void sendActionBarMessage(Player p, String message) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(message));
    }

    public static void setScore(LeaderboardPlayer player, Player p, SurvivalSkills plugin, SkillCategory skillCategory) {
        player.setScore(SkillCategory.ALL, Leaderboard.getLeaderboardScore(p, SkillCategory.ALL));
        player.setScore(skillCategory, Leaderboard.getLeaderboardScore(p, skillCategory));
    }

    /**
     * Returns the amount of XP needed for all levels up to
     * and including the level inputted
     */
    public static int totalExperienceForLevel(int level, SkillCategory skillCategory) {
        return SkillMath.totalExperienceForLevel(level, skillCategory,
                SurvivalSkills.getInstance().isExponentialXP());
    }

    public void loadMultipliers() {
        FileConfiguration config = plugin.getTrueConfig();

        // Load XP settings
        SkillCategory.baseSkills().forEach((SkillCategory skillCategory) ->
                setXpMultiplier(skillCategory, config.getDouble(skillCategory.getXpConfigKey())));

        // Load global multiplier
        if (config.get("SkillXPMultiplier") != null)
            multiplier = config.getDouble("SkillXPMultiplier");
        if (config.get("ExponentialXP") != null)
            plugin.setExponentialXP(config.getBoolean("ExponentialXP"));
    }

    public void saveGlobalMultiplier(double multiplier) {
        this.multiplier = multiplier;
        File file = new File(plugin.getDataFolder(), FileUtils.CONFIG_YML);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("SkillXPMultiplier", multiplier);
        try {
            config.save(file);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save config file", e);
        }
    }

    /**
     * Loads the default rewards for each skill
     */
    public void loadDefaultRewards() {
        FileConfiguration config = plugin.getTrueConfig();
        defaultPlayerRewards = new PlayerRewards();
        SkillCategory.mainSkills().forEach((SkillCategory skillCategory) ->
                loadRewardConfig(skillCategory.getDisplayName(), config));
    }

    public void loadPlayerRewards(Player p) {
        Objects.requireNonNull(p, "Player cannot be null");
        if (defaultPlayerRewards == null || defaultPlayerRewards.getRewardList() == null) {
            Bukkit.getLogger().warning("Player rewards are not loaded");
            return;
        }

        SkillsHolder holder = playerSkills.get(p.getUniqueId());
        if (holder == null && p.isOnline()) {
            holder = restorePlayerSkills(p);
        }
        if (holder == null) {
            Bukkit.getLogger().warning("Player " + p.getName() + " does not have any skills");
            return;
        }

        holder.getPlayerRewards().enableRewards(p, holder.getSkills());
    }

    public void loadPlayerSkills(UUID uuid, FileConfiguration data) {
        if (playerSkills.containsKey(uuid)) return;

        ArrayList<Skill> skills = new ArrayList<>();
        for (SkillCategory skillCategory : SkillCategory.mainSkills()) {
            skills.add(new Skill(data.getDouble(configPath(uuid, skillCategory, EXPERIENCE_FIELD)),
                    data.getInt(configPath(uuid, skillCategory, LEVEL_FIELD)), skillCategory));
        }

        SkillsHolder holder = new SkillsHolder(skills, getNewPlayerRewards());
        playerSkills.put(uuid, holder);
    }

    private SkillsHolder restorePlayerSkills(Player p) {
        UUID uuid = p.getUniqueId();
        File dataFile = new File(plugin.getDataFolder(), FileUtils.PLAYERDATA_YML);
        if (dataFile.exists()) {
            FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
            if (data.contains(uuid.toString())) {
                loadPlayerSkills(uuid, data);
            }
        }

        if (!playerSkills.containsKey(uuid)) {
            ArrayList<Skill> skills = new ArrayList<>();
            for (SkillCategory skillCategory : SkillCategory.mainSkills()) {
                skills.add(new Skill(0, 1, skillCategory));
            }
            playerSkills.put(uuid, new SkillsHolder(skills, getNewPlayerRewards()));
        }

        Bukkit.getLogger().log(Level.WARNING,
                String.format("[SurvivalSkills] Restored missing skill data for online player %s", p.getName()));
        return playerSkills.get(uuid);
    }

    public void loadRewardConfig(String type, FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection(type);
        if (section == null)
            return;
        section.getKeys(false).forEach(key -> {
            // Load whether it is enabled, the level it is unlocked, and the type of reward
            boolean enabled = config.getBoolean(type + "." + key + ".Enabled");
            int level = config.getInt(type + "." + key + ".Level");
            String rewardType = config.getString(type + "." + key + ".Type");
            defaultPlayerRewards.addReward(SkillCategory.fromString(type), new Reward(SkillCategory.fromString(type), key, rewardType, level, enabled));
        });
    }

    public void loadPlayerMultiplier(Player p, FileConfiguration data) {
        if (!playerSkills.containsKey(p.getUniqueId()))
            return;

        // Get multiplier
        UUID uuid = p.getUniqueId();
        if (!data.contains(uuid.toString()))
            return;
        if (!data.contains(playerConfigPath(uuid, MULTIPLIER_FIELD)))
            return;
        double multiplier = data.getDouble(playerConfigPath(uuid, MULTIPLIER_FIELD));
        playerSkills.get(uuid).setSkillMultiplier(multiplier);

        // Get time left
        int time = 3600;
        if (data.contains(playerConfigPath(uuid, MULTIPLIER_TIMER_FIELD)))
            time = data.getInt(playerConfigPath(uuid, MULTIPLIER_TIMER_FIELD));

        AbilityTimer timer = new AbilityTimer(plugin, "XPVoucher", p, time, 0);
        timer.runTaskTimerAsynchronously(plugin, 0, 20);
        plugin.getAbilityManager().addAbility(p, timer);
    }

    public void saveSkillData(FileConfiguration data) {
        for (Map.Entry<UUID, SkillsHolder> player : playerSkills.entrySet()) {
            UUID uuid = player.getKey();
            for (Skill skill : player.getValue().getSkills()) {
                saveSkill(skill, uuid, data);
            }
        }
    }

    public void savePlayerSkillData(UUID uuid, FileConfiguration data) {
        if (playerSkills.containsKey(uuid)) {
            for (Skill skill : playerSkills.get(uuid).getSkills()) {
                saveSkill(skill, uuid, data);
            }
        } else
            Bukkit.getLogger().warning("UUID " + uuid + " does not have any skills");
    }

    public void savePlayerMultiplier(Player p, FileConfiguration data) {
        if (!playerSkills.containsKey(p.getUniqueId()))
            return;

        if (!plugin.getAbilityManager().saveAbilityTimerState(p, XP_VOUCHER_ABILITY, data,
                playerConfigPath(p.getUniqueId(), MULTIPLIER_TIMER_FIELD))) {
            data.set(playerConfigPath(p.getUniqueId(), MULTIPLIER_FIELD), null);
            return;
        }
        savePlayerMultiplierValue(p, data);
    }

    private void savePlayerMultiplierValue(Player p, FileConfiguration data) {
        data.set(playerConfigPath(p.getUniqueId(), MULTIPLIER_FIELD),
                playerSkills.get(p.getUniqueId()).getSkillMultiplier());
    }

    private static void saveSkill(Skill skill, UUID uuid, FileConfiguration data) {
        data.set(configPath(uuid, skill.getSkillCategory(), LEVEL_FIELD), skill.getLevel());
        data.set(configPath(uuid, skill.getSkillCategory(), EXPERIENCE_FIELD), skill.getExperience());
    }

    private static String configPath(UUID uuid, SkillCategory skillCategory, String field) {
        return uuid + "." + skillCategory + "." + field;
    }

    private static String playerConfigPath(UUID uuid, String field) {
        return uuid + "." + field;
    }

    /**
     * Recalculate and synchronize the Main skill's experience as the average of
     * the (capped) total experience of the 7 base skills. Works identically for
     * exponential and logarithmic XP systems. Handles level-up logic, rewards,
     * leaderboard score updates, and god trophy awarding.
     */
    public void syncMainSkill(Player p) {
        if (!playerSkills.containsKey(p.getUniqueId()))
            return;
        UUID uuid = p.getUniqueId();
        Skill main = getSkill(uuid, SkillCategory.MAIN);

        int levelCap = plugin.getTrophyManager().playerMaxSkillLevel(uuid);
        double totalXP = 0.0;
        int contributingSkills = 0;
        for (Skill skill : playerSkills.get(uuid).getSkills()) {
            if (skill.getSkillCategory() == SkillCategory.MAIN)
                continue;
            // Cap each skill's experience at MAX_EXPERIENCE to ensure the main skill
            // calculation does not exceed allowed limits,
            totalXP += Math.min(skill.getExperience(), Skill.MAX_EXPERIENCE);
            contributingSkills++;
        }
        if (contributingSkills == 0)
            return; // Should never happen but protects divide-by-zero
        double averageXP = totalXP / contributingSkills; // Deterministic main XP

        int previousLevel = main.getLevel();
        // Setting experience will internally adjust level; but enforce level cap after.
        main.setExperience((int) averageXP);

        if (main.getLevel() > levelCap)
            main.setLevel(levelCap);

        boolean leveledUp = main.getLevel() > previousLevel;
        if (!leveledUp)
            return;

        // Level-up notification & rewards
        SkillDisplay.levelUpNotification(p, main);
        PlayerRewards rewards = getPlayerRewards(p);
        if (rewards != null) {
            rewards.handleReward(p, main, true);
        }

        if (main.getLevel() == Skill.MAX_LEVEL) {
            awardGodTrophy(p);
        }

        // Update leaderboard scores for Main
        updateLeaderboardScore(p, main.getSkillCategory());
    }

    private void awardGodTrophy(Player p) {
        Map<TrophyType, Boolean> trophies = plugin.getTrophyManager().getTrophyTracker().get(p.getUniqueId());
        if (trophies == null || trophies.getOrDefault(TrophyType.GOD, false)) return;

        trophies.put(TrophyType.GOD, true);
        plugin.getTrophyManager().getTrophyTracker().put(p.getUniqueId(), trophies);
        if (!p.getInventory().addItem(ItemStackGenerator.getGodTrophyBase()).isEmpty()) {
            p.getWorld().dropItem(p.getLocation(), ItemStackGenerator.getGodTrophyBase());
        }

        plugin.getServer().broadcastMessage(ChatColor.AQUA + p.getName() + " has maxed out all of their skills!");
        plugin.getServer().broadcastMessage(ChatColor.GREEN + "Congratulate the hard work they put in!");
        Bukkit.getOnlinePlayers().forEach((Player player) ->
                player.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1));
        p.sendRawMessage(ChatColor.GREEN
                + "You have been awarded the god trophy base for maxing out all of your skills!");
        p.sendRawMessage(ChatColor.AQUA
                + "Surround the base with power ore in a crafting table to create the god trophy!");
    }

    private void updateLeaderboardScore(Player p, SkillCategory skillCategory) {
        LeaderboardPlayer leaderboardPlayer = plugin.getLeaderboardTracker().computeIfAbsent(p.getUniqueId(),
                ignored -> Leaderboard.createLeaderboardPlayer(p));
        setScore(leaderboardPlayer, p, plugin, skillCategory);
    }

    public void runSkillAutoSave() {
        new BukkitRunnable() {
            @Override
            public void run() {
                FileUtils.savePlayerData();
                plugin.getAbilityManager().saveToolBelts();
            }
        }.runTaskTimer(plugin, 1200, 1200);
    }

    public void setPlayerMultiplier(Player p, double multiplier) {
        if (!playerSkills.containsKey(p.getUniqueId()))
            return;
        playerSkills.get(p.getUniqueId()).setSkillMultiplier(multiplier);
    }

    public double getPlayerMultiplier(Player p) {
        if (!playerSkills.containsKey(p.getUniqueId()))
            return 1.0;
        return playerSkills.get(p.getUniqueId()).getSkillMultiplier();
    }

    public static Skill getSkill(UUID uuid, SkillCategory skillCategory) {
        if (!playerSkills.containsKey(uuid)) {
            return new Skill(0, 0, skillCategory);
        }
        return playerSkills.get(uuid).getSkill(skillCategory);
    }

    public static int getSkillLevel(UUID uuid, SkillCategory skillCategory) {
        Skill skill = getSkill(uuid, skillCategory);
        return skill.getLevel();
    }

    public PlayerRewards getPlayerRewards(Player p) {
        Objects.requireNonNull(p, "Player cannot be null");
        SkillsHolder holder = playerSkills.get(p.getUniqueId());
        if (holder == null && p.isOnline()) {
            holder = restorePlayerSkills(p);
            holder.getPlayerRewards().enableRewards(p, holder.getSkills());
        }
        return holder == null ? null : holder.getPlayerRewards();
    }

    public PlayerRewards getNewPlayerRewards() {
        return new PlayerRewards(Objects.requireNonNull(defaultPlayerRewards, "Default rewards are not loaded").getRewardList());
    }

    public void clearPlayerData() {
        playerSkills.clear();
        maxLevelMessagesShown.clear();
    }

    public boolean isMaxSkillMessageEnabled(Player p) {
        if (!playerSkills.containsKey(p.getUniqueId()))
            return true;
        return playerSkills.get(p.getUniqueId()).isMaxSkillMessageEnabled();
    }

    public HashMap<UUID, SkillsHolder> getPlayerSkills() {
        return playerSkills;
    }

    public double getXpMultiplier(SkillCategory skillCategory) {
        if (!skillCategory.isBaseSkill()) {
            throw new IllegalArgumentException("XP multipliers are only available for base skills: " + skillCategory);
        }
        return xpMultipliers.getOrDefault(skillCategory, 0.0);
    }

    public void setXpMultiplier(SkillCategory skillCategory, double xpMultiplier) {
        if (!skillCategory.isBaseSkill()) {
            throw new IllegalArgumentException("XP multipliers are only available for base skills: " + skillCategory);
        }
        xpMultipliers.put(skillCategory, xpMultiplier);
    }

    public double getBuildingXP() {
        return getXpMultiplier(SkillCategory.BUILDING);
    }

    public void setBuildingXP(double buildingXP) {
        setXpMultiplier(SkillCategory.BUILDING, buildingXP);
    }

    public double getCraftingXP() {
        return getXpMultiplier(SkillCategory.CRAFTING);
    }

    public void setCraftingXP(double craftingXP) {
        setXpMultiplier(SkillCategory.CRAFTING, craftingXP);
    }

    public double getExploringXP() {
        return getXpMultiplier(SkillCategory.EXPLORING);
    }

    public void setExploringXP(double exploringXP) {
        setXpMultiplier(SkillCategory.EXPLORING, exploringXP);
    }

    public double getFarmingXP() {
        return getXpMultiplier(SkillCategory.FARMING);
    }

    public void setFarmingXP(double farmingXP) {
        setXpMultiplier(SkillCategory.FARMING, farmingXP);
    }

    public double getFightingXP() {
        return getXpMultiplier(SkillCategory.FIGHTING);
    }

    public void setFightingXP(double fightingXP) {
        setXpMultiplier(SkillCategory.FIGHTING, fightingXP);
    }

    public double getFishingXP() {
        return getXpMultiplier(SkillCategory.FISHING);
    }

    public void setFishingXP(double fishingXP) {
        setXpMultiplier(SkillCategory.FISHING, fishingXP);
    }

    public double getMiningXP() {
        return getXpMultiplier(SkillCategory.MINING);
    }

    public void setMiningXP(double miningXP) {
        setXpMultiplier(SkillCategory.MINING, miningXP);
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public PlayerRewards getDefaultPlayerRewards() {
        return defaultPlayerRewards;
    }
}
