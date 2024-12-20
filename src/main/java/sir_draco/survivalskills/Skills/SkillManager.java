package sir_draco.survivalskills.Skills;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.Abilities.AbilityTimer;
import sir_draco.survivalskills.Boards.Leaderboard;
import sir_draco.survivalskills.Boards.LeaderboardPlayer;
import sir_draco.survivalskills.Boards.SkillScoreboard;
import sir_draco.survivalskills.Rewards.PlayerRewards;
import sir_draco.survivalskills.Rewards.Reward;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.Utils.ItemStackGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkillManager {

    private static final ArrayList<String> skillNames = new ArrayList<>();
    public static final double scalar = 2749.22119298367;

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

    public static void experienceEvent(SurvivalSkills plugin, Player p, double xp, String skillName) {
        // Handle multipliers
        xp *= plugin.getSkillManager().getMultiplier();
        if (plugin.getAbilityManager().getAbility(p, "XPVoucher") != null)
            xp *= plugin.getSkillManager().getPlayerMultiplier(p);

        UUID uuid = p.getUniqueId();
        Skill skill = SkillManager.getSkill(uuid, skillName);
        if (skill.getLevel() >= plugin.getTrophyManager().playerMaxSkillLevel(uuid)) {
            SkillScoreboard.updateScoreboard(plugin, p, "Main");
            if (skill.getLevel() == 100 || skill.isCurrentMaxMessage()) return;
            if (!plugin.getSkillManager().isMaxSkillMessageEnabled(p)) return;
            p.sendRawMessage(ChatColor.DARK_BLUE + "You have reached your current max level for: " + ChatColor.AQUA + skillName);
            p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
            skill.setCurrentMaxMessage(true);
            return;
        }

        if (skill.getLevel() >= 100) return;
        xp = checkXPCap(skill.getExperience(), xp, plugin.getTrophyManager().playerMaxSkillLevel(uuid), skillName);

        if (plugin.getToggledScoreboard().containsKey(p.getUniqueId()) && plugin.getToggledScoreboard().get(p.getUniqueId())
                && xp != 0)
            sendActionBarMessage(p, ChatColor.GRAY + skillName + ChatColor.YELLOW + " (+" + xp + ")");
        if (skill.changeExperience(xp, plugin.getTrophyManager().playerMaxSkillLevel(uuid))) {
            skill.levelUpNotification(p);
            plugin.getSkillManager().getPlayerRewards(p).handleReward(plugin, p, skill, skillName, true);
            if (plugin.getLeaderboardTracker().containsKey(p.getUniqueId())) {
                LeaderboardPlayer player = plugin.getLeaderboardTracker().get(p.getUniqueId());
                setScore(player, p, plugin, skill.getSkillName());
                plugin.getLeaderboardTracker().put(p.getUniqueId(), player);
            }
            else {
                plugin.getLeaderboardTracker().put(p.getUniqueId(), Leaderboard.createLeaderboardPlayer(plugin, p));
                setScore(plugin.getLeaderboardTracker().get(p.getUniqueId()), p, plugin, skill.getSkillName());
            }
        }

        // Check the main skill
        Skill main = SkillManager.getSkill(uuid, "Main");
        if (main.getLevel() >= plugin.getTrophyManager().playerMaxSkillLevel(uuid)) {
            plugin.getSkillManager().checkMainXP(p);
            SkillScoreboard.updateScoreboard(plugin, p, skillName);
            return;
        }
        if (main.changeExperience(xp / 7.0, plugin.getTrophyManager().playerMaxSkillLevel(uuid))) {
            main.levelUpNotification(p);
            plugin.getSkillManager().getPlayerRewards(p).handleReward(plugin, p, main, "Main", true);
            if (main.getLevel() == 100) {
                HashMap<String, Boolean> trophies = plugin.getTrophyManager().getTrophyTracker().get(p.getUniqueId());
                trophies.put("GodTrophy", true);
                plugin.getTrophyManager().getTrophyTracker().put(p.getUniqueId(), trophies);

                // Add the god trophy to the player's inventory, if their inventory is full drop it
                if (!p.getInventory().addItem(ItemStackGenerator.getGodTrophyBase()).isEmpty())
                    p.getWorld().dropItem(p.getLocation(), ItemStackGenerator.getGodTrophyBase());

                plugin.getServer().broadcastMessage(ChatColor.AQUA + p.getName() + " has maxed out all of their skills!");
                plugin.getServer().broadcastMessage(ChatColor.GREEN + "Congratulate the hard work they put in!");
                for (Player player : Bukkit.getOnlinePlayers())
                    player.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
                p.sendRawMessage(ChatColor.GREEN + "You have been awarded the god trophy base for maxing out all of your skills!");
                p.sendRawMessage(ChatColor.AQUA + "Surround the base with power ore in a crafting table to create the god trophy!");
            }

            if (plugin.getLeaderboardTracker().containsKey(p.getUniqueId())) {
                LeaderboardPlayer player = plugin.getLeaderboardTracker().get(p.getUniqueId());
                setScore(player, p, plugin, main.getSkillName());
                plugin.getLeaderboardTracker().put(p.getUniqueId(), player);
            }
            else {
                plugin.getLeaderboardTracker().put(p.getUniqueId(), Leaderboard.createLeaderboardPlayer(plugin, p));
                setScore(plugin.getLeaderboardTracker().get(p.getUniqueId()), p, plugin, main.getSkillName());
            }
        }
        SkillScoreboard.updateScoreboard(plugin, p, skillName);
        if (xp != 0) experienceEvent(plugin, p, 0, skillName);
    }

    /**
     * Ensures that the XP cap is not exceeded
     */
    public static double checkXPCap(double totalXP, double xp, int levelCap, String skillName) {
        double xpForLevel = totalExperienceForLevel(levelCap, skillName);
        if (totalXP > xpForLevel) return 0;
        if (totalXP + xp > xpForLevel) return xpForLevel - totalXP;
        return xp;
    }

    public static void sendActionBarMessage(Player p, String message) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(message));
    }

    public static void setScore(LeaderboardPlayer player, Player p, SurvivalSkills plugin, String skillName) {
        player.setScore(Leaderboard.getLeaderboardScore(plugin, p, "All"));
        int skillScore = Leaderboard.getLeaderboardScore(plugin, p, skillName);
        switch (skillName) {
            case "Building":
                player.setBuildingScore(skillScore);
                break;
            case "Mining":
                player.setMiningScore(skillScore);
                break;
            case "Farming":
                player.setFarmingScore(skillScore);
                break;
            case "Fighting":
                player.setFightingScore(skillScore);
                break;
            case "Fishing":
                player.setFishingScore(skillScore);
                break;
            case "Crafting":
                player.setCraftingScore(skillScore);
                break;
            case "Exploring":
                player.setExploringScore(skillScore);
                break;
            case "Main":
                player.setMainScore(skillScore);
                break;
            case "Death":
                player.setDeathScore(skillScore);
                break;
        }
    }

    /**
     * Returns the amount of XP needed for all levels up to
     * and including the level inputted
     */
    public static int totalExperienceForLevel(int level, String skillName) {
        double sum = 0;
        for (int i = 1; i <= level; i++) sum += Math.log(i) * scalar;

        if (skillName.equals("Main"))
            if (level >= 100) return 1000000;
        return (int) Math.floor(sum);
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
        if (config.get("SkillXPMultiplier") != null) multiplier = config.getDouble("SkillXPMultiplier");
    }

    /**
     * Loads the default rewards for each skill
     */
    public void loadDefaultRewards() {
        FileConfiguration config = plugin.getTrueConfig();
        defaultPlayerRewards = new PlayerRewards();
        for (String skill : skillNames) loadRewardConfig(skill, config);
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
        skills.add(new Skill(data.getDouble(uuid + ".Main.Experience"), data.getInt(uuid + ".Main.Level"), "Main"));
        skills.add(new Skill(data.getDouble(uuid + ".Building.Experience"), data.getInt(uuid + ".Building.Level"), "Building"));
        skills.add(new Skill(data.getDouble(uuid + ".Mining.Experience"), data.getInt(uuid + ".Mining.Level"), "Mining"));
        skills.add(new Skill(data.getDouble(uuid + ".Fishing.Experience"), data.getInt(uuid + ".Fishing.Level"), "Fishing"));
        skills.add(new Skill(data.getDouble(uuid + ".Exploring.Experience"), data.getInt(uuid + ".Exploring.Level"), "Exploring"));
        skills.add(new Skill(data.getDouble(uuid + ".Farming.Experience"), data.getInt(uuid + ".Farming.Level"), "Farming"));
        skills.add(new Skill(data.getDouble(uuid + ".Fighting.Experience"), data.getInt(uuid + ".Fighting.Level"), "Fighting"));
        skills.add(new Skill(data.getDouble(uuid + ".Crafting.Experience"), data.getInt(uuid + ".Crafting.Level"), "Crafting"));

        SkillsHolder holder = new SkillsHolder(skills, getNewPlayerRewards());
        playerSkills.put(uuid, holder);
    }

    public void loadRewardConfig(String type, FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection(type);
        if (section == null) return;
        section.getKeys(false).forEach(key -> {
            // Load whether it is enabled, the level it is unlocked, and the type of reward
            boolean enabled = config.getBoolean(type + "." + key + ".Enabled");
            int level = config.getInt(type + "." + key + ".Level");
            String rewardType = config.getString(type + "." + key + ".Type");
            defaultPlayerRewards.addReward(type, new Reward(type, key, rewardType, level, enabled));
        });
    }

    public void loadPlayerMultiplier(Player p, FileConfiguration data) {
        if (!playerSkills.containsKey(p.getUniqueId())) return;

        // Get multiplier
        UUID uuid = p.getUniqueId();
        if (!data.contains(uuid.toString())) return;
        if (!data.contains(uuid + ".Multiplier")) return;
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
                data.set(uuid + "." + skill.getSkillName() + ".Level", skill.getLevel());
                data.set(uuid + "." + skill.getSkillName() + ".Experience", skill.getExperience());
            }
        }
    }

    public void savePlayerSkillData(UUID uuid, FileConfiguration data) {
        if (playerSkills.containsKey(uuid)) {
            for (Skill skill : playerSkills.get(uuid).getSkills()) {
                data.set(uuid + "." + skill.getSkillName() + ".Level", skill.getLevel());
                data.set(uuid + "." + skill.getSkillName() + ".Experience", skill.getExperience());
            }
        }
        else Bukkit.getLogger().warning("UUID " + uuid + " does not have any skills");
    }

    public void savePlayerMultiplier(Player p, FileConfiguration data) {
        if (!playerSkills.containsKey(p.getUniqueId())) return;

        AbilityTimer timer = plugin.getAbilityManager().getAbility(p, "XPVoucher");
        if (timer != null) data.set(p.getUniqueId() + ".MultiplierTimer", timer.getActiveTimeLeft());
        else {
            data.set(p.getUniqueId() + ".Multiplier", null);
            data.set(p.getUniqueId() + ".MultiplierTimer", null);
            return;
        }

        data.set(p.getUniqueId() + ".Multiplier", playerSkills.get(p.getUniqueId()).getSkillMultiplier());
    }

    public void updateExploringStats(UUID uuid) {
        Skill exploring = getSkill(uuid, "Exploring");
        exploring.changeExperience(plugin.getExploringListener().getPlayerSteps(uuid) * exploringXP,
                plugin.getTrophyManager().playerMaxSkillLevel(uuid));
    }

    public void checkMainXP(Player p) {
        if (!playerSkills.containsKey(p.getUniqueId())) return;
        double totalXP = 0;
        for (Skill skill : playerSkills.get(p.getUniqueId()).getSkills()) {
            if (skill.getSkillName().equalsIgnoreCase("Main")) continue;
            totalXP += skill.getExperience();
        }

        totalXP /= 7.0;
        Skill main = getSkill(p.getUniqueId(), "Main");

        if (main.getExperience() < totalXP || main.getExperience() > totalXP + 10.0) {
            main.setExperience((int) totalXP);
            plugin.savePlayerData(p);
        }
    }

    public void runSkillAutoSave() {
        new BukkitRunnable() {
            @Override
            public void run() {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            plugin.savePlayerData();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

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
        if (!playerSkills.containsKey(p.getUniqueId())) return;
        playerSkills.get(p.getUniqueId()).setSkillMultiplier(multiplier);
        Bukkit.getLogger().info("Set multiplier for " + p.getName() + " to " + multiplier);
    }

    public double getPlayerMultiplier(Player p) {
        if (!playerSkills.containsKey(p.getUniqueId())) return 1.0;
        return playerSkills.get(p.getUniqueId()).getSkillMultiplier();
    }

    public static Skill getSkill(UUID uuid, String skillName) {
        if (!playerSkills.containsKey(uuid)) {
            return new Skill(0, 0, skillName);
        }
        return playerSkills.get(uuid).getSkill(skillName);
    }

    public static int getSkillLevel(UUID uuid, String skillName) {
        Skill skill = getSkill(uuid, skillName);
        return skill.getLevel();
    }

    public PlayerRewards getPlayerRewards(Player p) {
        if (!playerSkills.containsKey(p.getUniqueId())) return null;
        return playerSkills.get(p.getUniqueId()).getPlayerRewards();
    }

    public PlayerRewards getNewPlayerRewards() {
        return new PlayerRewards(defaultPlayerRewards.getRewardList());
    }

    public boolean isMaxSkillMessageEnabled(Player p) {
        if (!playerSkills.containsKey(p.getUniqueId())) return true;
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
