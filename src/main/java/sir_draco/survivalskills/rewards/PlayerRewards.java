package sir_draco.survivalskills.rewards;

import org.bukkit.Bukkit;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.boards.Leaderboard;
import sir_draco.survivalskills.boards.LeaderboardPlayer;
import sir_draco.survivalskills.rewards.RewardEffects.RewardEffect;
import sir_draco.survivalskills.skills.Skill;
import sir_draco.survivalskills.skills.SkillCategory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PlayerRewards {

    private final HashMap<SkillCategory, ArrayList<Reward>> rewardList = new HashMap<>();
    // private final double exoticFishingLootChance = 0.0001;

    private boolean unbreakableTools = false;
    private boolean addedDeathResistance = false;
    private double fortuneChance = 0;
    private double protectionPercentage = 0;
    private double swimSpeed = 0;
    private double cropDoubleChance = 0;
    private double blockBlackChance = 0;
    private double lifesteal = 0;
    private double criticalChance = 0;
    private double commonFishingLootChance = 0;
    private double rareFishingLootChance = 0;
    private double epicFishingLootChance = 0;
    private double legendaryFishingLootChance = 0;
    private double experienceMultiplier = 1;
    private double materialsBack = 0;
    private double extraOutput = 0;
    private int fishingMinTickSpeed = 100;
    private int fishingMaxTickSpeed = 600;

    public PlayerRewards() {}

    /**
     * Applies the effects for the death skill
     * Creates a LeaderboardPlayer instance if one does not exist
     * @param p
     */
    public static void handleDeathSkillEffects(Player p) {
        SurvivalSkills plugin = SurvivalSkills.getInstance();
        LeaderboardPlayer leaderboardPlayer = plugin.getLeaderboardTracker().get(p.getUniqueId());
        int deaths = 0;
        if (leaderboardPlayer == null) {
            leaderboardPlayer = Leaderboard.createLeaderboardPlayer(p);
            plugin.getLeaderboardTracker().put(p.getUniqueId(), leaderboardPlayer);
            deaths = Leaderboard.getLeaderboardScore(p, SkillCategory.DEATHS);
        }
        else {
            // If a player was already online, then their death score is already available
            deaths = leaderboardPlayer.getScore(SkillCategory.DEATHS);
        }

        // Handle death skill effects
        if (deaths >= 40) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false, true));
        }
        if (deaths >= 50) {
            PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
            rewards.setProtectionPercentage(rewards.getProtectionPercentage() + 0.1);
            rewards.setAddedDeathResistance(true);
        }
    }

    public PlayerRewards(HashMap<SkillCategory, ArrayList<Reward>> rewardList) {
        for (Map.Entry<SkillCategory, ArrayList<Reward>> skillType : rewardList.entrySet()) {
            ArrayList<Reward> rewards = new ArrayList<>();
            for (Reward reward : skillType.getValue()) rewards.add(reward.copyReward());
            this.rewardList.put(skillType.getKey(), rewards);
        }
    }

    public void addReward(SkillCategory skillCategory, Reward reward) {
        if (!rewardList.containsKey(skillCategory)) {
            ArrayList<Reward> rewards = new ArrayList<>();
            rewards.add(reward);
            rewardList.put(skillCategory, rewards);
        }
        else rewardList.get(skillCategory).add(reward);
    }

    public Reward getLevelReward(SkillCategory skillCategory, int level) {
        for (Reward reward : rewardList.get(skillCategory)) if (reward.getLevel() == level && reward.isEnabled()) return reward;
        return null;
    }

    public Reward getReward(SkillCategory skillCategory, String name) {
        for (Reward reward : rewardList.get(skillCategory)) if (reward.getName().equalsIgnoreCase(name)) return reward;
        return null;
    }

    public void enableRewards(Player p, ArrayList<Skill> skills) {
        if (skills == null) return;
        setPlayerMaxHealth(p, 20);
        for (Skill skill : skills) {
            if (rewardList.get(skill.getSkillCategory()) == null) {
                Bukkit.getLogger().warning("Can't find the skill: " + skill.getSkillCategory());
                continue;
            }
            enableSkillRewards(p, skill);
        }
    }

    public void enableSkillRewards(Player p, Skill skill) {
        int level = skill.getLevel();
        for (Reward reward : rewardList.get(skill.getSkillCategory())) {
            if (reward.getLevel() <= level && reward.isEnabled()) reward.applyReward();
            else continue;

            var effect = RewardEffects.getEffect(skill.getSkillCategory().getDisplayName(), reward.getName());
            if (effect != null) effect.apply(this, p);
        }
    }

    public void handleReward(Player p, Skill skill, boolean notify) {
        int level = skill.getLevel();
        Reward reward = getLevelReward(skill.getSkillCategory(), level);
        if (reward == null) return;
        if (!reward.isEnabled()) return;
        if (reward.isApplied()) return;

        if (notify) RewardNotifications.notifyPlayer(skill.getSkillCategory().getDisplayName(), reward.getName(), p);
        reward.applyReward();

        RewardEffect effect = RewardEffects.getEffect(skill.getSkillCategory().getDisplayName(), reward.getName());
        if (effect != null) effect.apply(this, p);
    }

    public HashMap<SkillCategory, ArrayList<Reward>> getRewardList() {
        return rewardList;
    }

    public void setFortuneChance(double fortuneChance) {
        this.fortuneChance = fortuneChance;
    }

    public double getFortuneChance() {
        return fortuneChance;
    }

    public void setProtectionPercentage(double protectionPercentage) {
        this.protectionPercentage = protectionPercentage;
    }

    public double getProtectionPercentage() {
        return protectionPercentage;
    }

    public void setUnbreakableTools(boolean unbreakableTools) {
        this.unbreakableTools = unbreakableTools;
    }

    public boolean isUnbreakableTools() {
        return unbreakableTools;
    }

    public void setSwimSpeed(double swimSpeed) {
        this.swimSpeed = swimSpeed;
    }

    public double getSwimSpeed() {
        return swimSpeed;
    }

    public double getCropDoubleChance() {
        return cropDoubleChance;
    }

    public void setPlayerMaxHealth(Player p, int health) {
        AttributeInstance attribute = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (attribute == null) return;
        attribute.setBaseValue(health);
    }

    public void setCropDoubleChance(double cropDoubleChance) {
        this.cropDoubleChance = cropDoubleChance;
    }

    public double getBlockBlackChance() {
        return blockBlackChance;
    }

    public void setBlockBlackChance(double blockBlackChance) {
        this.blockBlackChance = blockBlackChance;
    }

    public double getLifesteal() {
        return lifesteal;
    }

    public void setLifesteal(double lifesteal) {
        this.lifesteal = lifesteal;
    }

    public double getCriticalChance() {
        return criticalChance;
    }

    public void setCriticalChance(double criticalChance) {
        this.criticalChance = criticalChance;
    }

    public double getCommonFishingLootChance() {
        return commonFishingLootChance;
    }

    public void setCommonFishingLootChance(double commonFishingLootChance) {
        this.commonFishingLootChance = commonFishingLootChance;
    }

    public double getRareFishingLootChance() {
        return rareFishingLootChance;
    }

    public void setRareFishingLootChance(double rareFishingLootChance) {
        this.rareFishingLootChance = rareFishingLootChance;
    }

    public double getEpicFishingLootChance() {
        return epicFishingLootChance;
    }

    public void setEpicFishingLootChance(double epicFishingLootChance) {
        this.epicFishingLootChance = epicFishingLootChance;
    }

    public double getLegendaryFishingLootChance() {
        return legendaryFishingLootChance;
    }

    public void setLegendaryFishingLootChance(double legendaryFishingLootChance) {
        this.legendaryFishingLootChance = legendaryFishingLootChance;
    }

    public double getExoticFishingLootChance() {
        return 0.0001;
    }

    public double getExperienceMultiplier() {
        return experienceMultiplier;
    }

    public void setExperienceMultiplier(double experienceMultiplier) {
        this.experienceMultiplier = experienceMultiplier;
    }

    public int getFishingMaxTickSpeed() {
        return fishingMaxTickSpeed;
    }

    public void setFishingMaxTickSpeed(int fishingMaxTickSpeed) {
        this.fishingMaxTickSpeed = fishingMaxTickSpeed;
    }

    public int getFishingMinTickSpeed() {
        return fishingMinTickSpeed;
    }

    public void setFishingMinTickSpeed(int fishingMinTickSpeed) {
        this.fishingMinTickSpeed = fishingMinTickSpeed;
    }

    public double getExtraOutput() {
        return extraOutput;
    }

    public void setExtraOutput(double doubleOutput) {
        this.extraOutput = doubleOutput;
    }

    public double getMaterialsBack() {
        return materialsBack;
    }

    public void setMaterialsBack(double materialsBack) {
        this.materialsBack = materialsBack;
    }

    public void setAddedDeathResistance(boolean addedDeathResistance) {
        this.addedDeathResistance = addedDeathResistance;
    }

    public boolean isAddedDeathResistance() {
        return addedDeathResistance;
    }
}
