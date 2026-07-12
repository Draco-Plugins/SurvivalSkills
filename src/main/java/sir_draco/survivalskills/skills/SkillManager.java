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
import sir_draco.survivalskills.utils.items.ItemStackGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class SkillManager {

    private static final ArrayList<String> skillNames = new ArrayList<>();
    public static final double scalar = 2749.22119298367;
    public static final double exponentialScalar = 2.260176;
    // Global hard cap aligning with Skill.maxExperience (1,000,000) so level 100
    // reachable for all skills
    private static final int MAX_EXPERIENCE = 1_000_000;

    public static final String MINING = "Mining";
    public static final String EXPLORING = "Exploring";
    public static final String FARMING = "Farming";
    public static final String BUILDING = "Building";
    public static final String FIGHTING = "Fighting";
    public static final String FISHING = "Fishing";
    public static final String CRAFTING = "Crafting";
    public static final String MAIN = "Main";

    private final SurvivalSkills plugin;
    private static final HashMap<UUID, SkillsHolder> playerSkills = new HashMap<>();

    private PlayerRewards defaultPlayerRewards; // Holds the default information for rewards
    private double buildingXP;
    private double miningXP;
    private double fishingXP;
    private double exploringXP;
    private double farmingXP;
    private double fightingXP;
    private double craftingXP;
    private double multiplier = 1;

    public SkillManager(SurvivalSkills plugin) {
        this.plugin = plugin;
        createSkillNamesList();
        loadMultipliers();
        loadDefaultRewards();
        runSkillAutoSave();
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
            if (skill.getLevel() == Skill.MAX_LEVEL || skill.isCurrentMaxMessage()) return;
            if (!plugin.getSkillManager().isMaxSkillMessageEnabled(p)) return;
            p.sendRawMessage(ChatColor.DARK_BLUE + "You have reached your current max level for: " + ChatColor.AQUA + skillCategory);
            p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
            skill.setCurrentMaxMessage(true);
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
            skill.levelUpNotification(p);

            // Ensure that player rewards are loaded
            PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
            if (rewards == null) {
                Bukkit.getLogger().warning("Player rewards are not loaded for " + p.getName());
                return;
            }

            rewards.handleReward(p, skill, true);
            if (plugin.getLeaderboardTracker().containsKey(p.getUniqueId())) {
                LeaderboardPlayer player = plugin.getLeaderboardTracker().get(p.getUniqueId());
                setScore(player, p, plugin, skill.getSkillCategory());
                plugin.getLeaderboardTracker().put(p.getUniqueId(), player);
            } else {
                plugin.getLeaderboardTracker().put(p.getUniqueId(), Leaderboard.createLeaderboardPlayer(p));
                setScore(plugin.getLeaderboardTracker().get(p.getUniqueId()), p, plugin, skill.getSkillCategory());
            }

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
        double sum = 0;
        if (SurvivalSkills.getInstance().isExponentialXP()) {
            for (int i = 1; i <= level; i++)
                sum += Math.pow(i, exponentialScalar);
        } else {
            for (int i = 1; i <= level; i++)
                sum += Math.log(i) * scalar;
        }
        // Clamp any curve so that level 100 never exceeds the hard max experience.
        if (level >= Skill.MAX_LEVEL) {
            // Main skill historically forced to exactly 1,000,000 at level 100
            if (skillCategory == SkillCategory.MAIN) return MAX_EXPERIENCE;
            if (sum > MAX_EXPERIENCE) return MAX_EXPERIENCE;
        }
        return (int) Math.floor(Math.min(sum, MAX_EXPERIENCE));
    }

    public void loadMultipliers() {
        FileConfiguration config = plugin.getTrueConfig();

        // Load XP settings
        buildingXP = config.getDouble("BuildingXP");
        miningXP = config.getDouble("MiningXP");
        fishingXP = config.getDouble("FishingXP");
        exploringXP = config.getDouble("ExploringXP");
        farmingXP = config.getDouble("FarmingXP");
        fightingXP = config.getDouble("FightingXP");
        craftingXP = config.getDouble("CraftingXP");

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
        for (String skill : skillNames)
            loadRewardConfig(skill, config);
    }

    public void loadPlayerRewards(Player p) {
        if (defaultPlayerRewards == null || defaultPlayerRewards.getRewardList() == null) {
            Bukkit.getLogger().warning("Player rewards are not loaded");
            return;
        }

        if (!playerSkills.containsKey(p.getUniqueId())) {
            Bukkit.getLogger().warning("Player " + p.getName() + " does not have any skills");
            return;
        }

        SkillsHolder holder = playerSkills.get(p.getUniqueId());
        holder.getPlayerRewards().enableRewards(p, holder.getSkills());
    }

    public void loadPlayerSkills(UUID uuid, FileConfiguration data) {
        if (playerSkills.containsKey(uuid)) return;

        ArrayList<Skill> skills = new ArrayList<>();
        for (SkillCategory skillCategory : SkillCategory.mainSkills()) {
            skills.add(new Skill(data.getDouble(uuid + "." + skillCategory + ".Experience"), data.getInt(uuid + "." + skillCategory + ".Level"), skillCategory));
        }

        SkillsHolder holder = new SkillsHolder(skills, getNewPlayerRewards());
        playerSkills.put(uuid, holder);
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
        if (!data.contains(uuid + ".Multiplier"))
            return;
        double multiplier = data.getDouble(uuid + ".Multiplier");
        playerSkills.get(uuid).setSkillMultiplier(multiplier);

        // Get time left
        int time = 3600;
        if (data.contains(uuid + ".MultiplierTimer"))
            time = data.getInt(uuid + ".MultiplierTimer");

        AbilityTimer timer = new AbilityTimer(plugin, "XPVoucher", p, time, 0);
        timer.runTaskTimerAsynchronously(plugin, 0, 20);
        plugin.getAbilityManager().addAbility(p, timer);
    }

    public void saveSkillData(FileConfiguration data) {
        for (Map.Entry<UUID, SkillsHolder> player : playerSkills.entrySet()) {
            UUID uuid = player.getKey();
            for (Skill skill : player.getValue().getSkills()) {
                data.set(uuid + "." + skill.getSkillCategory() + ".Level", skill.getLevel());
                data.set(uuid + "." + skill.getSkillCategory() + ".Experience", skill.getExperience());
            }
        }
    }

    public void savePlayerSkillData(UUID uuid, FileConfiguration data) {
        if (playerSkills.containsKey(uuid)) {
            for (Skill skill : playerSkills.get(uuid).getSkills()) {
                data.set(uuid + "." + skill.getSkillCategory() + ".Level", skill.getLevel());
                data.set(uuid + "." + skill.getSkillCategory() + ".Experience", skill.getExperience());
            }
        } else
            Bukkit.getLogger().warning("UUID " + uuid + " does not have any skills");
    }

    public void savePlayerMultiplier(Player p, FileConfiguration data) {
        if (!playerSkills.containsKey(p.getUniqueId()))
            return;

        AbilityTimer timer = plugin.getAbilityManager().getAbility(p, "XPVoucher");
        if (timer != null)
            data.set(p.getUniqueId() + ".MultiplierTimer", timer.getActiveTimeLeft());
        else {
            data.set(p.getUniqueId() + ".Multiplier", null);
            data.set(p.getUniqueId() + ".MultiplierTimer", null);
            return;
        }

        data.set(p.getUniqueId() + ".Multiplier", playerSkills.get(p.getUniqueId()).getSkillMultiplier());
    }

    public void updateExploringStats(UUID uuid) {
        Skill exploring = getSkill(uuid, SkillCategory.EXPLORING);
        exploring.changeExperience(plugin.getExploringListener().getPlayerSteps(uuid) * exploringXP,
                plugin.getTrophyManager().playerMaxSkillLevel(uuid));
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
            totalXP += Math.min(skill.getExperience(), MAX_EXPERIENCE);
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
        main.levelUpNotification(p);
        PlayerRewards rewards = getPlayerRewards(p);
        if (rewards != null) {
            rewards.handleReward(p, main, true);
        }

        if (main.getLevel() == Skill.MAX_LEVEL) {
            // TODO: Convert to method
            Map<TrophyType, Boolean> trophies = plugin.getTrophyManager().getTrophyTracker().get(p.getUniqueId());
            if (trophies != null) {
                trophies.put(TrophyType.GOD, true);
                plugin.getTrophyManager().getTrophyTracker().put(p.getUniqueId(), trophies);
            }

            if (!p.getInventory().addItem(ItemStackGenerator.getGodTrophyBase()).isEmpty())
                p.getWorld().dropItem(p.getLocation(), ItemStackGenerator.getGodTrophyBase());

            plugin.getServer().broadcastMessage(ChatColor.AQUA + p.getName() + " has maxed out all of their skills!");
            plugin.getServer().broadcastMessage(ChatColor.GREEN + "Congratulate the hard work they put in!");
            for (Player player : Bukkit.getOnlinePlayers())
                player.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
            p.sendRawMessage(
                    ChatColor.GREEN + "You have been awarded the god trophy base for maxing out all of your skills!");
            p.sendRawMessage(
                    ChatColor.AQUA + "Surround the base with power ore in a crafting table to create the god trophy!");
        }

        // Update leaderboard scores for Main
        if (plugin.getLeaderboardTracker().containsKey(p.getUniqueId())) {
            LeaderboardPlayer player = plugin.getLeaderboardTracker().get(p.getUniqueId());
            setScore(player, p, plugin, main.getSkillCategory());
            plugin.getLeaderboardTracker().put(p.getUniqueId(), player);
        } else {
            plugin.getLeaderboardTracker().put(p.getUniqueId(), Leaderboard.createLeaderboardPlayer(p));
            setScore(plugin.getLeaderboardTracker().get(p.getUniqueId()), p, plugin, main.getSkillCategory());
        }
    }

    public void runSkillAutoSave() {
        new BukkitRunnable() {
            @Override
            public void run() {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        FileUtils.savePlayerData();

                        plugin.getAbilityManager().saveToolBelts();
                    }
                }.runTask(plugin);
            }
        }.runTaskTimerAsynchronously(plugin, 900, 900);
    }

    public void createSkillNamesList() {
        skillNames.add("Building");
        skillNames.add("Mining");
        skillNames.add("Farming");
        skillNames.add("Fighting");
        skillNames.add("Fishing");
        skillNames.add("Exploring");
        skillNames.add("Crafting");
        skillNames.add("Main");
    }

    public void setPlayerMultiplier(Player p, double multiplier) {
        if (!playerSkills.containsKey(p.getUniqueId()))
            return;
        playerSkills.get(p.getUniqueId()).setSkillMultiplier(multiplier);
        Bukkit.getLogger().info("Set multiplier for " + p.getName() + " to " + multiplier);
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
        if (!playerSkills.containsKey(p.getUniqueId()))
            return null;
        return playerSkills.get(p.getUniqueId()).getPlayerRewards();
    }

    public PlayerRewards getNewPlayerRewards() {
        return new PlayerRewards(defaultPlayerRewards.getRewardList());
    }

    public boolean isMaxSkillMessageEnabled(Player p) {
        if (!playerSkills.containsKey(p.getUniqueId()))
            return true;
        return playerSkills.get(p.getUniqueId()).isMaxSkillMessageEnabled();
    }

    public HashMap<UUID, SkillsHolder> getPlayerSkills() {
        return playerSkills;
    }

    public double getBuildingXP() {
        return buildingXP;
    }

    public void setBuildingXP(double buildingXP) {
        this.buildingXP = buildingXP;
    }

    public double getCraftingXP() {
        return craftingXP;
    }

    public void setCraftingXP(double craftingXP) {
        this.craftingXP = craftingXP;
    }

    public double getExploringXP() {
        return exploringXP;
    }

    public void setExploringXP(double exploringXP) {
        this.exploringXP = exploringXP;
    }

    public double getFarmingXP() {
        return farmingXP;
    }

    public void setFarmingXP(double farmingXP) {
        this.farmingXP = farmingXP;
    }

    public double getFightingXP() {
        return fightingXP;
    }

    public void setFightingXP(double fightingXP) {
        this.fightingXP = fightingXP;
    }

    public double getFishingXP() {
        return fishingXP;
    }

    public void setFishingXP(double fishingXP) {
        this.fishingXP = fishingXP;
    }

    public double getMiningXP() {
        return miningXP;
    }

    public void setMiningXP(double miningXP) {
        this.miningXP = miningXP;
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
