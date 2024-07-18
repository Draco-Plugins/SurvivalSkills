package sir_draco.survivalskills.Rewards;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.Skills.Skill;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PlayerRewards {

    private final HashMap<String, ArrayList<Reward>> rewardList = new HashMap<>();
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

            switch (skill.getSkillName()) {
                case "Mining":
                    switch (reward.getName()) {
                        case "FortuneI":
                            setFortuneChance(0.2);
                            break;
                        case "FortuneII":
                            setFortuneChance(0.4);
                            break;
                        case "FortuneIII":
                            setFortuneChance(0.5);
                            break;
                        case "ArmorI":
                            setProtectionPercentage(0.05);
                            break;
                        case "ArmorII":
                            setProtectionPercentage(0.1);
                            break;
                        case "ArmorIII":
                            setProtectionPercentage(0.15);
                            break;
                        case "ArmorIV":
                            setProtectionPercentage(0.2);
                            break;
                        case "UnbreakableTools":
                            setUnbreakableTools(true);
                            break;
                    }
                    break;
                case "Exploring":
                    switch (reward.getName()) {
                        case "SwimI":
                            setSwimSpeed(1.4);
                            break;
                        case "SwimII":
                            setSwimSpeed(1.8);
                            break;
                        case "SwimIII":
                            setSwimSpeed(2.2);
                            break;
                        case "SwimIV":
                            setSwimSpeed(2.6);
                            break;
                        case "SwimV":
                            setSwimSpeed(3.0);
                            break;
                        case "SpeedI":
                            p.setWalkSpeed(0.22f);
                            break;
                        case "SpeedII":
                            p.setWalkSpeed(0.24f);
                            break;
                        case "SpeedIII":
                            p.setWalkSpeed(0.26f);
                            break;
                        case "SpeedIV":
                            p.setWalkSpeed(0.28f);
                            break;
                        case "SpeedV":
                            p.setWalkSpeed(0.30f);
                            break;
                    }
                    break;
                case "Farming":
                    switch (reward.getName()) {
                        case "DoubleCropsI":
                            setCropDoubleChance(0.25);
                            break;
                        case "DoubleCropsII":
                            setCropDoubleChance(0.5);
                            break;
                        case "DoubleCropsIII":
                            setCropDoubleChance(0.75);
                            break;
                        case "DoubleCropsIV":
                            setCropDoubleChance(1);
                            break;
                        case "HealthI":
                            setPlayerMaxHealth(p, 22);
                            break;
                        case "HealthII":
                            setPlayerMaxHealth(p, 24);
                            break;
                        case "HealthIII":
                            setPlayerMaxHealth(p, 26);
                            break;
                        case "HealthIV":
                            setPlayerMaxHealth(p, 28);
                            break;
                        case "HealthV":
                            setPlayerMaxHealth(p, 30);
                            break;
                        case "HealthVI":
                            setPlayerMaxHealth(p, 32);
                            break;
                        case "HealthVII":
                            setPlayerMaxHealth(p, 34);
                            break;
                        case "HealthVIII":
                            setPlayerMaxHealth(p, 36);
                            break;
                        case "HealthIX":
                            setPlayerMaxHealth(p, 38);
                            break;
                        case "HealthX":
                            setPlayerMaxHealth(p, 40);
                            break;
                    }
                    break;
                case "Building":
                    switch (reward.getName()) {
                        case "BlockReturnI":
                            setBlockBlackChance(0.05);
                            break;
                        case "BlockReturnII":
                            setBlockBlackChance(0.1);
                            break;
                        case "BlockReturnIII":
                            setBlockBlackChance(0.15);
                            break;
                        case "BlockReturnIV":
                            setBlockBlackChance(0.2);
                            break;
                        case "BlockReturnV":
                            setBlockBlackChance(0.25);
                            break;
                        case "BlockReturnVI":
                            setBlockBlackChance(0.3);
                            break;
                        case "BlockReturnVII":
                            setBlockBlackChance(0.35);
                            break;
                        case "BlockReturnVIII":
                            setBlockBlackChance(0.4);
                            break;
                        case "BlockReturnIX":
                            setBlockBlackChance(0.45);
                            break;
                        case "BlockReturnX":
                            setBlockBlackChance(0.5);
                            break;
                        case "ExtendedReach":
                            AttributeInstance reach = p.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE);
                            if (reach != null) reach.setBaseValue(6);
                            break;
                    }
                    break;
                case "Fighting":
                    switch (reward.getName()) {
                        case "LifestealI":
                            setLifesteal(0.05);
                            break;
                        case "LifestealII":
                            setLifesteal(0.1);
                            break;
                        case "LifestealIII":
                            setLifesteal(0.15);
                            break;
                        case "LifestealIV":
                            setLifesteal(0.2);
                            break;
                        case "LifestealV":
                            setLifesteal(0.25);
                            break;
                        case "CriticalI":
                            setCriticalChance(0.1);
                            break;
                        case "CriticalII":
                            setCriticalChance(0.2);
                            break;
                    }
                    break;
                case "Fishing":
                    switch (reward.getName()) {
                        case "CommonLootI":
                            setCommonFishingLootChance(0.25);
                            break;
                        case "CommonLootII":
                            setCommonFishingLootChance(0.4);
                            break;
                        case "CommonLootIII":
                            setCommonFishingLootChance(0.55);
                            break;
                        case "CommonLootIV":
                            setCommonFishingLootChance(0.65);
                            break;
                        case "CommonLootV":
                            setCommonFishingLootChance(0.75);
                            break;
                        case "RareLootI":
                            setRareFishingLootChance(0.1);
                            break;
                        case "RareLootII":
                            setRareFishingLootChance(0.2);
                            break;
                        case "RareLootIII":
                            setRareFishingLootChance(0.3);
                            break;
                        case "RareLootIV":
                            setRareFishingLootChance(0.35);
                            break;
                        case "RareLootV":
                            setRareFishingLootChance(0.4);
                            break;
                        case "EpicLootI":
                            setEpicFishingLootChance(0.02);
                            break;
                        case "EpicLootII":
                            setEpicFishingLootChance(0.03);
                            break;
                        case "EpicLootIII":
                            setEpicFishingLootChance(0.04);
                            break;
                        case "EpicLootIV":
                            setEpicFishingLootChance(0.045);
                            break;
                        case "EpicLootV":
                            setEpicFishingLootChance(0.05);
                            break;
                        case "LegendaryLootI":
                            setLegendaryFishingLootChance(0.005);
                            break;
                        case "LegendaryLootII":
                            setLegendaryFishingLootChance(0.0075);
                            break;
                        case "LegendaryLootIII":
                            setLegendaryFishingLootChance(0.01);
                            break;
                        case "ExperienceI":
                            setExperienceMultiplier(1.1);
                            break;
                        case "ExperienceII":
                            setExperienceMultiplier(1.2);
                            break;
                        case "ExperienceIII":
                            setExperienceMultiplier(1.3);
                            break;
                        case "ExperienceIV":
                            setExperienceMultiplier(1.4);
                            break;
                        case "ExperienceV":
                            setExperienceMultiplier(1.5);
                            break;
                        case "ExperienceVI":
                            setExperienceMultiplier(1.6);
                            break;
                        case "ExperienceVII":
                            setExperienceMultiplier(1.7);
                            break;
                        case "ExperienceVIII":
                            setExperienceMultiplier(1.8);
                            break;
                        case "ExperienceIX":
                            setExperienceMultiplier(1.9);
                            break;
                        case "ExperienceX":
                            setExperienceMultiplier(2);
                            break;
                        case "FasterFishingI":
                            setFishingMaxTickSpeed(500);
                            setFishingMinTickSpeed(80);
                            break;
                        case "FasterFishingII":
                            setFishingMaxTickSpeed(350);
                            setFishingMinTickSpeed(60);
                            break;
                        case "FasterFishingIII":
                            setFishingMaxTickSpeed(225);
                            setFishingMinTickSpeed(45);
                            break;
                        case "FasterFishingIV":
                            setFishingMaxTickSpeed(150);
                            setFishingMinTickSpeed(30);
                            break;
                        case "FasterFishingV":
                            setFishingMaxTickSpeed(80);
                            setFishingMinTickSpeed(20);
                            break;
                    }
                    break;
                case "Crafting":
                    switch (reward.getName()) {
                        case "ExtraOutputI":
                            setExtraOutput(0.05);
                            break;
                        case "ExtraOutputII":
                            setExtraOutput(0.1);
                            break;
                        case "ExtraOutputIII":
                            setExtraOutput(0.15);
                            break;
                        case "ExtraOutputIV":
                            setExtraOutput(0.2);
                            break;
                        case "ExtraOutputV":
                            setExtraOutput(0.25);
                            break;
                        case "ExtraOutputVI":
                            setExtraOutput(0.3);
                            break;
                        case "ExtraOutputVII":
                            setExtraOutput(0.35);
                            break;
                        case "ExtraOutputVIII":
                            setExtraOutput(0.4);
                            break;
                        case "ExtraOutputIX":
                            setExtraOutput(0.45);
                            break;
                        case "ExtraOutputX":
                            setExtraOutput(0.5);
                            break;
                        case "MaterialsBackI":
                            setMaterialsBack(0.1);
                            break;
                        case "MaterialsBackII":
                            setMaterialsBack(0.2);
                            break;
                        case "MaterialsBackIII":
                            setMaterialsBack(0.3);
                            break;
                        case "MaterialsBackIV":
                            setMaterialsBack(0.4);
                            break;
                        case "MaterialsBackV":
                            setMaterialsBack(0.5);
                            break;
                    }
                    break;
                case "Main":
                    switch (reward.getName()) {
                        case "SetHomeI":
                            if (p.hasPermission("essentials.sethome.multiple.two")) break;
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + p.getName() + " permission set essentials.sethome.multiple.two true");
                            break;
                        case "SetHomeII":
                            if (p.hasPermission("essentials.sethome.multiple.three")) break;
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + p.getName() + " permission set essentials.sethome.multiple.three true");
                            break;
                        case "SetHomeIII":
                            if (p.hasPermission("essentials.sethome.multiple.four")) break;
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + p.getName() + " permission set essentials.sethome.multiple.four true");
                            break;
                        case "SetHomeIV":
                            if (p.hasPermission("essentials.sethome.multiple.five")) break;
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + p.getName() + " permission set essentials.sethome.multiple.five true");
                            break;
                    }
                    break;
            }
        }
    }

    public void handleReward(SurvivalSkills plugin, Player p, Skill skill, String type, boolean notify) {
        // See if there is a valid reward for this level
        int level = skill.getLevel();
        Reward reward = getLevelReward(type, level);
        if (reward == null) return;
        if (!reward.isEnabled()) return;
        if (reward.isApplied()) return;

        // Notify the player and check what type of reward it is
        if (notify) RewardNotifications.notifyPlayer(type, reward.getName(), p);
        reward.applyReward();
        switch (type) {
            case "Mining":
                switch (reward.getName()) {
                    case "FortuneI":
                        setFortuneChance(0.2);
                        break;
                    case "FortuneII":
                        setFortuneChance(0.4);
                        break;
                    case "FortuneIII":
                        setFortuneChance(0.5);
                        break;
                    case "ArmorI":
                        setProtectionPercentage(0.05);
                        break;
                    case "ArmorII":
                        setProtectionPercentage(0.1);
                        break;
                    case "ArmorIII":
                        setProtectionPercentage(0.15);
                        break;
                    case "ArmorIV":
                        setProtectionPercentage(0.2);
                        break;
                    case "VeinMinerII":
                        plugin.getMiningListener().getVeinminerTracker().put(p, 1);
                    case "UnbreakableTools":
                        setUnbreakableTools(true);
                        break;
                }
                break;
            case "Exploring":
                switch (reward.getName()) {
                    case "SwimI":
                        setSwimSpeed(1.4);
                        break;
                    case "SwimII":
                        setSwimSpeed(1.8);
                        break;
                    case "SwimIII":
                        setSwimSpeed(2.2);
                        break;
                    case "SwimIV":
                        setSwimSpeed(2.6);
                        break;
                    case "SwimV":
                        setSwimSpeed(3.0);
                        break;
                    case "SpeedI":
                        p.setWalkSpeed(0.22f);
                        break;
                    case "SpeedII":
                        p.setWalkSpeed(0.24f);
                        break;
                    case "SpeedIII":
                        p.setWalkSpeed(0.26f);
                        break;
                    case "SpeedIV":
                        p.setWalkSpeed(0.28f);
                        break;
                    case "SpeedV":
                        p.setWalkSpeed(0.3f);
                        break;
                }
                break;
            case "Farming":
                switch (reward.getName()) {
                    case "DoubleCropsI":
                        setCropDoubleChance(0.25);
                        break;
                    case "DoubleCropsII":
                        setCropDoubleChance(0.5);
                        break;
                    case "DoubleCropsIII":
                        setCropDoubleChance(0.75);
                        break;
                    case "DoubleCropsIV":
                        setCropDoubleChance(1);
                        break;
                    case "HealthI":
                        setPlayerMaxHealth(p, 22);
                        break;
                    case "HealthII":
                        setPlayerMaxHealth(p, 24);
                        break;
                    case "HealthIII":
                        setPlayerMaxHealth(p, 26);
                        break;
                    case "HealthIV":
                        setPlayerMaxHealth(p, 28);
                        break;
                    case "HealthV":
                        setPlayerMaxHealth(p, 30);
                        break;
                    case "HealthVI":
                        setPlayerMaxHealth(p, 32);
                        break;
                    case "HealthVII":
                        setPlayerMaxHealth(p, 34);
                        break;
                    case "HealthVIII":
                        setPlayerMaxHealth(p, 36);
                        break;
                    case "HealthIX":
                        setPlayerMaxHealth(p, 38);
                        break;
                    case "HealthX":
                        setPlayerMaxHealth(p, 40);
                        break;
                }
                break;
            case "Building":
                switch (reward.getName()) {
                    case "BlockReturnI":
                        setBlockBlackChance(0.05);
                        break;
                    case "BlockReturnII":
                        setBlockBlackChance(0.1);
                        break;
                    case "BlockReturnIII":
                        setBlockBlackChance(0.15);
                        break;
                    case "BlockReturnIV":
                        setBlockBlackChance(0.2);
                        break;
                    case "BlockReturnV":
                        setBlockBlackChance(0.25);
                        break;
                    case "BlockReturnVI":
                        setBlockBlackChance(0.3);
                        break;
                    case "BlockReturnVII":
                        setBlockBlackChance(0.35);
                        break;
                    case "BlockReturnVIII":
                        setBlockBlackChance(0.4);
                        break;
                    case "BlockReturnIX":
                        setBlockBlackChance(0.45);
                        break;
                    case "BlockReturnX":
                        setBlockBlackChance(0.5);
                        break;
                    case "ExtendedReach":
                        AttributeInstance reach = p.getAttribute(Attribute.PLAYER_BLOCK_INTERACTION_RANGE);
                        if (reach != null) reach.setBaseValue(6);
                        break;
                }
                break;
            case "Fighting":
                switch (reward.getName()) {
                    case "LifestealI":
                        setLifesteal(0.05);
                        break;
                    case "LifestealII":
                        setLifesteal(0.1);
                        break;
                    case "LifestealIII":
                        setLifesteal(0.15);
                        break;
                    case "LifestealIV":
                        setLifesteal(0.2);
                        break;
                    case "LifestealV":
                        setLifesteal(0.25);
                        break;
                    case "CriticalI":
                        setCriticalChance(0.1);
                        break;
                    case "CriticalII":
                        setCriticalChance(0.2);
                        break;
                }
                break;
            case "Fishing":
                switch (reward.getName()) {
                    case "CommonLootI":
                        setCommonFishingLootChance(0.25);
                        break;
                    case "CommonLootII":
                        setCommonFishingLootChance(0.4);
                        break;
                    case "CommonLootIII":
                        setCommonFishingLootChance(0.55);
                        break;
                    case "CommonLootIV":
                        setCommonFishingLootChance(0.65);
                        break;
                    case "CommonLootV":
                        setCommonFishingLootChance(0.75);
                        break;
                    case "RareLootI":
                        setRareFishingLootChance(0.1);
                        break;
                    case "RareLootII":
                        setRareFishingLootChance(0.2);
                        break;
                    case "RareLootIII":
                        setRareFishingLootChance(0.3);
                        break;
                    case "RareLootIV":
                        setRareFishingLootChance(0.35);
                        break;
                    case "RareLootV":
                        setRareFishingLootChance(0.4);
                        break;
                    case "EpicLootI":
                        setEpicFishingLootChance(0.02);
                        break;
                    case "EpicLootII":
                        setEpicFishingLootChance(0.03);
                        break;
                    case "EpicLootIII":
                        setEpicFishingLootChance(0.04);
                        break;
                    case "EpicLootIV":
                        setEpicFishingLootChance(0.045);
                        break;
                    case "EpicLootV":
                        setEpicFishingLootChance(0.05);
                        break;
                    case "LegendaryLootI":
                        setLegendaryFishingLootChance(0.005);
                        break;
                    case "LegendaryLootII":
                        setLegendaryFishingLootChance(0.0075);
                        break;
                    case "LegendaryLootIII":
                        setLegendaryFishingLootChance(0.01);
                        break;
                    case "ExperienceI":
                        setExperienceMultiplier(1.1);
                        break;
                    case "ExperienceII":
                        setExperienceMultiplier(1.2);
                        break;
                    case "ExperienceIII":
                        setExperienceMultiplier(1.3);
                        break;
                    case "ExperienceIV":
                        setExperienceMultiplier(1.4);
                        break;
                    case "ExperienceV":
                        setExperienceMultiplier(1.5);
                        break;
                    case "ExperienceVI":
                        setExperienceMultiplier(1.6);
                        break;
                    case "ExperienceVII":
                        setExperienceMultiplier(1.7);
                        break;
                    case "ExperienceVIII":
                        setExperienceMultiplier(1.8);
                        break;
                    case "ExperienceIX":
                        setExperienceMultiplier(1.9);
                        break;
                    case "ExperienceX":
                        setExperienceMultiplier(2);
                        break;
                    case "FasterFishingI":
                        setFishingMaxTickSpeed(500);
                        setFishingMinTickSpeed(80);
                        break;
                    case "FasterFishingII":
                        setFishingMaxTickSpeed(350);
                        setFishingMinTickSpeed(60);
                        break;
                    case "FasterFishingIII":
                        setFishingMaxTickSpeed(225);
                        setFishingMinTickSpeed(45);
                        break;
                    case "FasterFishingIV":
                        setFishingMaxTickSpeed(150);
                        setFishingMinTickSpeed(30);
                        break;
                    case "FasterFishingV":
                        setFishingMaxTickSpeed(80);
                        setFishingMinTickSpeed(20);
                        break;
                    case "AutoTrashII":
                        if (plugin.getFishingListener().getTrashInventories().containsKey(p)) {
                            plugin.getFishingListener().getTrashInventories().get(p).upgradeTrashSize();
                        }
                        break;
                }
                break;
            case "Crafting":
                switch (reward.getName()) {
                    case "ExtraOutputI":
                        setExtraOutput(0.05);
                        break;
                    case "ExtraOutputII":
                        setExtraOutput(0.1);
                        break;
                    case "ExtraOutputIII":
                        setExtraOutput(0.15);
                        break;
                    case "ExtraOutputIV":
                        setExtraOutput(0.2);
                        break;
                    case "ExtraOutputV":
                        setExtraOutput(0.25);
                        break;
                    case "ExtraOutputVI":
                        setExtraOutput(0.3);
                        break;
                    case "ExtraOutputVII":
                        setExtraOutput(0.35);
                        break;
                    case "ExtraOutputVIII":
                        setExtraOutput(0.4);
                        break;
                    case "ExtraOutputIX":
                        setExtraOutput(0.45);
                        break;
                    case "ExtraOutputX":
                        setExtraOutput(0.5);
                        break;
                    case "MaterialsBackI":
                        setMaterialsBack(0.1);
                        break;
                    case "MaterialsBackII":
                        setMaterialsBack(0.2);
                        break;
                    case "MaterialsBackIII":
                        setMaterialsBack(0.3);
                        break;
                    case "MaterialsBackIV":
                        setMaterialsBack(0.4);
                        break;
                    case "MaterialsBackV":
                        setMaterialsBack(0.5);
                        break;
                }
                break;
            case "Main":
                if (Bukkit.getServer().getPluginManager().getPlugin("Essentials") == null
                        || !Bukkit.getServer().getPluginManager().isPluginEnabled("Essentials")) break;
                switch (reward.getName()) {
                    case "SetHomeI":
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + p.getName() + " permission set essentials.sethome.multiple.one true");
                        break;
                    case "SetHomeII":
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + p.getName() + " permission set essentials.sethome.multiple.two true");
                        break;
                    case "SetHomeIII":
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + p.getName() + " permission set essentials.sethome.multiple.three true");
                        break;
                    case "SetHomeIV":
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + p.getName() + " permission set essentials.sethome.multiple.four true");
                        break;
                }
                break;
        }
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
        AttributeInstance attribute = p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
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
