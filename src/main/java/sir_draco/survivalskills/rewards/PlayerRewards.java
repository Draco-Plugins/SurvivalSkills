package sir_draco.survivalskills.rewards;

import org.bukkit.Bukkit;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.skills.Skill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PlayerRewards {

    private final HashMap<String, ArrayList<Reward>> rewardList = new HashMap<>();
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

    public PlayerRewards(HashMap<String, ArrayList<Reward>> rewardList) {
        for (Map.Entry<String, ArrayList<Reward>> skillType : rewardList.entrySet()) {
            ArrayList<Reward> rewards = new ArrayList<>();
            for (Reward reward : skillType.getValue()) rewards.add(reward.copyReward());
            this.rewardList.put(skillType.getKey(), rewards);
        }
    }

    public void addReward(String type, Reward reward) {
        if (!rewardList.containsKey(type)) {
            ArrayList<Reward> rewards = new ArrayList<>();
            rewards.add(reward);
            rewardList.put(type, rewards);
        }
        else rewardList.get(type).add(reward);
    }

    public Reward getLevelReward(String type, int level) {
        for (Reward reward : rewardList.get(type)) if (reward.getLevel() == level && reward.isEnabled()) return reward;
        return null;
    }

    public Reward getReward(String type, String name) {
        for (Reward reward : rewardList.get(type)) if (reward.getName().equalsIgnoreCase(name)) return reward;
        return null;
    }

    public void enableRewards(Player p, ArrayList<Skill> skills) {
        if (skills == null) return;
        setPlayerMaxHealth(p, 20);
        for (Skill skill : skills) {
            if (rewardList.get(skill.getSkillName()) == null) {
                Bukkit.getLogger().warning("Can't find the skill: " + skill.getSkillName());
                continue;
            }
            enableSkillRewards(p, skill);
        }
    }

    public void enableSkillRewards(Player p, Skill skill) {
        int level = skill.getLevel();
        for (Reward reward : rewardList.get(skill.getSkillName())) {
            if (reward.getLevel() <= level && reward.isEnabled()) reward.applyReward();
            else continue;

            var effect = RewardEffects.getEffect(skill.getSkillName(), reward.getName());
            if (effect != null) effect.apply(this, p);
        }
    }

    public void handleReward(Player p, Skill skill, String type, boolean notify) {
        int level = skill.getLevel();
        Reward reward = getLevelReward(type, level);
        if (reward == null) return;
        if (!reward.isEnabled()) return;
        if (reward.isApplied()) return;

        if (notify) RewardNotifications.notifyPlayer(type, reward.getName(), p);
        reward.applyReward();

        var effect = RewardEffects.getEffect(type, reward.getName());
        if (effect != null) effect.apply(this, p);
    }

    public HashMap<String, ArrayList<Reward>> getRewardList() {
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
