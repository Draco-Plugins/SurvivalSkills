package sir_draco.survivalskills.Skills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.Rewards.PlayerRewards;
import sir_draco.survivalskills.Rewards.Reward;
import sir_draco.survivalskills.SurvivalSkills;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkillManager {

    private static final ArrayList<String> skillNames = new ArrayList<>();

    private final SurvivalSkills plugin;
    private final HashMap<UUID, ArrayList<Skill>> playerSkills = new HashMap<>();
    private final HashMap<Player, PlayerRewards> rewardTracker = new HashMap<>();

    private PlayerRewards playerRewards; // Holds the default information for rewards
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
        playerRewards = new PlayerRewards();
        for (String skill : skillNames) loadRewardConfig(skill, config);
    }

    public void loadPlayerRewards(Player p) {
        if (playerRewards == null || playerRewards.getRewardList() == null) {
            Bukkit.getLogger().warning("Player rewards are not loaded");
            return;
        }

        if (rewardTracker.containsKey(p)) {
            rewardTracker.get(p).enableRewards(p, playerSkills.get(p.getUniqueId()));
            return;
        }

        rewardTracker.put(p, new PlayerRewards(playerRewards.getRewardList()));
        rewardTracker.get(p).enableRewards(p, playerSkills.get(p.getUniqueId()));
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
        playerSkills.put(uuid, skills);
    }

    public void loadRewardConfig(String type, FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection(type);
        if (section == null) return;
        section.getKeys(false).forEach(key -> {
            // Load whether it is enabled, the level it is unlocked, and the type of reward
            boolean enabled = config.getBoolean(type + "." + key + ".Enabled");
            int level = config.getInt(type + "." + key + ".Level");
            String rewardType = config.getString(type + "." + key + ".Type");
            playerRewards.addReward(type, new Reward(type, key, rewardType, level, enabled));
        });
    }

    public void saveSkillData(FileConfiguration data) {
        for (Map.Entry<UUID, ArrayList<Skill>> player : playerSkills.entrySet()) {
            UUID uuid = player.getKey();
            for (Skill skill : player.getValue()) {
                data.set(uuid + "." + skill.getSkillName() + ".Level", skill.getLevel());
                data.set(uuid + "." + skill.getSkillName() + ".Experience", skill.getExperience());
            }
        }
    }

    public void savePlayerSkillData(UUID uuid, FileConfiguration data) {
        if (playerSkills.containsKey(uuid)) {
            for (Skill skill : playerSkills.get(uuid)) {
                data.set(uuid + "." + skill.getSkillName() + ".Level", skill.getLevel());
                data.set(uuid + "." + skill.getSkillName() + ".Experience", skill.getExperience());
            }
        }
        else Bukkit.getLogger().warning("UUID " + uuid + " does not have any skills");
    }

    /**
     * Gets the specified skill of a player
     */
    public Skill getSkill(UUID uuid, String skillName) {
        if (playerSkills.isEmpty()) {
            ArrayList<Skill> skills = new ArrayList<>();
            skills.add(new Skill(0, 0, skillName));
            playerSkills.put(uuid, skills);
        }
        if (playerSkills.get(uuid) == null) return new Skill(0, 0, skillName);
        for (Skill skill : playerSkills.get(uuid)) if (skill.getSkillName().equalsIgnoreCase(skillName)) return skill;
        Skill skill = new Skill(0, 0, skillName);
        playerSkills.get(uuid).add(skill);
        return skill;
    }

    public void updateExploringStats(UUID uuid) {
        Skill exploring = getSkill(uuid, "Exploring");
        exploring.changeExperience(plugin.getExploringListener().getPlayerSteps(uuid) * exploringXP,
                plugin.getTrophyManager().playerMaxSkillLevel(uuid));
    }

    public void checkMainXP(Player p) {
        if (!playerSkills.containsKey(p.getUniqueId())) return;
        double totalXP = 0;
        for (Skill skill : playerSkills.get(p.getUniqueId())) {
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

    public PlayerRewards getPlayerRewards(Player p) {
        if (!rewardTracker.containsKey(p)) {
            loadPlayerRewards(p);
            return rewardTracker.get(p);
        }
        return rewardTracker.get(p);
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
        return playerRewards;
    }

    public HashMap<UUID, ArrayList<Skill>> getPlayerSkills() {
        return playerSkills;
    }
}