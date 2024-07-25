package sir_draco.survivalskills.Commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.Rewards.PlayerRewards;
import sir_draco.survivalskills.Rewards.Reward;
import sir_draco.survivalskills.Skills.Skill;
import sir_draco.survivalskills.SurvivalSkills;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public class SurvivalSkillsCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public SurvivalSkillsCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("survivalskills");
        if (command != null) command.setExecutor(this);
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;
        if (strings.length == 0) {
            p.sendMessage("§6SurvivalSkills §7- §eVersion: " + plugin.getDescription().getVersion());
            p.sendMessage("§6SurvivalSkills §7- §eAuthor: Sir_Draco");
            p.sendMessage("§6SurvivalSkills §7- §eCommands: /survivalskills help");
            return true;
        }

        if (strings[0].equalsIgnoreCase("help")) {
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            p.sendRawMessage(ChatColor.AQUA + "Survival Skills Commands:");
            p.sendRawMessage(ChatColor.GRAY + "/togglescoreboard - Toggles the XP scoreboard");
            p.sendRawMessage(ChatColor.GRAY + "/skills - Gives information about your skills");
            p.sendRawMessage(ChatColor.GRAY + "/skills tree - Shows the skill tree");
            p.sendRawMessage(ChatColor.GRAY + "/skills recipes - Shows all custom recipes");
            p.sendRawMessage(ChatColor.GRAY + "/spelunker");
            p.sendRawMessage(ChatColor.GRAY + "/veinminer");
            p.sendRawMessage(ChatColor.GRAY + "/ssnv - night vision");
            p.sendRawMessage(ChatColor.GRAY + "/peacefulminer");
            p.sendRawMessage(ChatColor.GRAY + "/autoeat");
            p.sendRawMessage(ChatColor.GRAY + "/sseat");
            p.sendRawMessage(ChatColor.GRAY + "/flight");
            p.sendRawMessage(ChatColor.GRAY + "/mobscanner");
            p.sendRawMessage(ChatColor.GRAY + "/waterbreathing");
            p.sendRawMessage(ChatColor.GRAY + "/deathlocation");
            return true;
        }

        if (strings[0].equalsIgnoreCase("xp")) {
            if (strings.length <= 2) {
                p.sendRawMessage(ChatColor.RED + "Correct usage: " + ChatColor.GRAY + "/survivalskills xp <player> <skill> <add/remove/set> <amount>");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
            Player target = plugin.getServer().getPlayer(strings[1]);
            if (target == null) {
                p.sendRawMessage(ChatColor.RED + "Player not found.");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
            if (isNotSkill(p.getUniqueId(), strings[2])) {
                p.sendRawMessage(ChatColor.RED + "Skill not found.");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
            Skill skill = plugin.getSkillManager().getSkill(target.getUniqueId(), strings[2]);
            handleXP(p, target, skill, strings);
            updateRewards(target, skill);
            return true;
        }

        if (strings[0].equalsIgnoreCase("level")) {
            if (strings.length <= 2) {
                p.sendRawMessage(ChatColor.RED + "Correct usage: " + ChatColor.GRAY + "/survivalskills level <player> <skill> <add/remove/set> <amount>");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
            Player target = plugin.getServer().getPlayer(strings[1]);
            if (target == null) {
                p.sendRawMessage(ChatColor.RED + "Player not found.");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
            if (strings[2].equalsIgnoreCase("all")) {
                for (Skill skill : plugin.getSkillManager().getPlayerSkills().get(target.getUniqueId())) {
                    handleLevel(p, target, skill, strings);
                    updateRewards(target, skill);
                }
                return true;
            }
            if (isNotSkill(p.getUniqueId(), strings[2])) {
                p.sendRawMessage(ChatColor.RED + "Skill not found.");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
            Skill skill = plugin.getSkillManager().getSkill(target.getUniqueId(), strings[2]);
            handleLevel(p, target, skill, strings);
            updateRewards(target, skill);
            return true;
        }

        if (strings[0].equalsIgnoreCase("skillxp")) {
            if (strings.length < 3) {
                p.sendRawMessage(ChatColor.RED + "Correct usage: " + ChatColor.GRAY + "/survivalskills skillxp <skill> <amount>");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }

            int amount;
            try {
                amount = Integer.parseInt(strings[2]);
                if (amount < 0) {
                    p.sendRawMessage(ChatColor.RED + "XP less than 0 won't work.");
                    p.sendRawMessage(ChatColor.GREEN + "XP amount set to 1!");
                    p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    amount = 1;
                }
            } catch (NumberFormatException e) {
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                p.sendMessage(ChatColor.RED + "Please enter a valid number.");
                return true;
            }

            String skill = strings[1];
            if (skill.equalsIgnoreCase("building")) {
                plugin.getSkillManager().setBuildingXP(amount);
                p.sendRawMessage(ChatColor.GREEN + "Building skill XP set to " + ChatColor.AQUA + amount);
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                File config = new File(plugin.getDataFolder(), "config.yml");
                YamlConfiguration configuration = YamlConfiguration.loadConfiguration(config);
                configuration.set("BuildingXP", amount);
                try {
                    configuration.save(config);
                } catch (Exception e) {
                    Bukkit.getLogger().warning("Could not save config file.");
                }
            }
            else if (skill.equalsIgnoreCase("fighting")) {
                plugin.getSkillManager().setFightingXP(amount);
                p.sendRawMessage(ChatColor.GREEN + "Fighting skill XP set to " + ChatColor.AQUA + amount);
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                File config = new File(plugin.getDataFolder(), "config.yml");
                YamlConfiguration configuration = YamlConfiguration.loadConfiguration(config);
                configuration.set("FightingXP", amount);
                try {
                    configuration.save(config);
                } catch (Exception e) {
                    Bukkit.getLogger().warning("Could not save config file.");
                }
            }
            else if (skill.equalsIgnoreCase("farming")) {
                plugin.getSkillManager().setFarmingXP(amount);
                p.sendRawMessage(ChatColor.GREEN + "Farming skill XP set to " + ChatColor.AQUA + amount);
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                File config = new File(plugin.getDataFolder(), "config.yml");
                YamlConfiguration configuration = YamlConfiguration.loadConfiguration(config);
                configuration.set("FarmingXP", amount);
                try {
                    configuration.save(config);
                } catch (Exception e) {
                    Bukkit.getLogger().warning("Could not save config file.");
                }
            }
            else if (skill.equalsIgnoreCase("fishing")) {
                plugin.getSkillManager().setFishingXP(amount);
                p.sendRawMessage(ChatColor.GREEN + "Fishing skill XP set to " + ChatColor.AQUA + amount);
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                File config = new File(plugin.getDataFolder(), "config.yml");
                YamlConfiguration configuration = YamlConfiguration.loadConfiguration(config);
                configuration.set("FishingXP", amount);
                try {
                    configuration.save(config);
                } catch (Exception e) {
                    Bukkit.getLogger().warning("Could not save config file.");
                }
            }
            else if (skill.equalsIgnoreCase("mining")) {
                plugin.getSkillManager().setMiningXP(amount);
                p.sendRawMessage(ChatColor.GREEN + "Mining skill XP set to " + ChatColor.AQUA + amount);
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                File config = new File(plugin.getDataFolder(), "config.yml");
                YamlConfiguration configuration = YamlConfiguration.loadConfiguration(config);
                configuration.set("MiningXP", amount);
                try {
                    configuration.save(config);
                } catch (Exception e) {
                    Bukkit.getLogger().warning("Could not save config file.");
                }
            }
            else if (skill.equalsIgnoreCase("exploring")) {
                plugin.getSkillManager().setExploringXP(amount);
                p.sendRawMessage(ChatColor.GREEN + "Exploring skill XP set to " + ChatColor.AQUA + amount);
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                File config = new File(plugin.getDataFolder(), "config.yml");
                YamlConfiguration configuration = YamlConfiguration.loadConfiguration(config);
                configuration.set("ExploringXP", amount);
                try {
                    configuration.save(config);
                } catch (Exception e) {
                    Bukkit.getLogger().warning("Could not save config file.");
                }
            }
            else if (skill.equalsIgnoreCase("crafting")) {
                plugin.getSkillManager().setCraftingXP(amount);
                p.sendRawMessage(ChatColor.GREEN + "Crafting skill XP set to " + ChatColor.AQUA + amount);
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                File config = new File(plugin.getDataFolder(), "config.yml");
                YamlConfiguration configuration = YamlConfiguration.loadConfiguration(config);
                configuration.set("CraftingXP", amount);
                try {
                    configuration.save(config);
                } catch (Exception e) {
                    Bukkit.getLogger().warning("Could not save config file.");
                }
            }
            else {
                p.sendRawMessage(ChatColor.RED + "Skill not found.");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
            return true;
        }

        if (strings[0].equalsIgnoreCase("togglereward")) {
            if (strings.length < 2) {
                p.sendRawMessage(ChatColor.RED + "Correct usage: " + ChatColor.GRAY + "/survivalskills togglereward <reward>");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }

            Reward reward = findReward(strings[1]);
            if (reward == null) {
                p.sendRawMessage(ChatColor.RED + "Reward not found.");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
            toggleReward(reward);
            p.sendRawMessage(ChatColor.GREEN + "Toggled reward: " + ChatColor.GRAY + reward.getName());
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            return true;
        }

        if (strings[0].equalsIgnoreCase("rewardlevel")) {
            if (strings.length < 3) {
                p.sendRawMessage(ChatColor.RED + "Correct usage: " + ChatColor.GRAY + "/survivalskills rewardlevel <reward> <level>");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }

            Reward reward = findReward(strings[1]);
            if (reward == null) {
                p.sendRawMessage(ChatColor.RED + "Reward not found.");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }

            int level;
            try {
                level = Integer.parseInt(strings[2]);
                if (level < 1) level = Math.abs(level);
                if (level > 100) level = 100;
            } catch (NumberFormatException e) {
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                p.sendMessage(ChatColor.RED + "Please enter a valid number.");
                return true;
            }

            changeRewardLevel(reward, level);
            p.sendRawMessage(ChatColor.GREEN + "Change reward level: " + ChatColor.GRAY + reward.getName() + " to " + reward.getLevel());
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            return true;
        }

        p.sendRawMessage(ChatColor.RED + "Correct usage: " + ChatColor.GRAY + "/survivalskills help/level/xp");
        p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        return true;
    }

    public boolean isNotSkill(UUID uuid, String skill) {
        for (Skill s : plugin.getSkillManager().getPlayerSkills().get(uuid)) {
            if (s.getSkillName().equalsIgnoreCase(skill)) return false;
        }
        return true;
    }

    public void handleXP(Player p, Player target, Skill skill, String[] strings) {
        if (strings.length <= 4) {
            p.sendMessage(ChatColor.RED + "Correct usage: " + ChatColor.GRAY + "/survivalskills xp <player> <skill> add/remove/set <amount>");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(strings[4]);
            if (amount < 0) amount = Math.abs(amount);
        } catch (NumberFormatException e) {
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            p.sendMessage(ChatColor.RED + "Please enter a valid number.");
            return;
        }

        if (strings[3].equalsIgnoreCase("add")) {
            skill.changeExperience(amount, skill.getMaxLevel());
            target.sendMessage(ChatColor.GREEN + "Added " + ChatColor.GRAY + amount + ChatColor.GREEN + " XP to " + ChatColor.GRAY + skill.getSkillName());
            target.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            p.sendRawMessage(ChatColor.GREEN + "Added " + ChatColor.GRAY + amount + ChatColor.GREEN + " XP to "
                    + ChatColor.GRAY + skill.getSkillName() + ChatColor.GREEN + " for " + ChatColor.GRAY + target.getName());
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        }
        else if (strings[3].equalsIgnoreCase("remove")) {
            skill.changeExperience(-amount, skill.getMaxLevel());
            target.sendMessage(ChatColor.GREEN + "Removed " + ChatColor.GRAY + amount + ChatColor.GREEN + " XP from " + ChatColor.GRAY + skill.getSkillName());
            target.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            p.sendRawMessage(ChatColor.GREEN + "Removed " + ChatColor.GRAY + amount + ChatColor.GREEN + " XP from "
                    + ChatColor.GRAY + skill.getSkillName() + ChatColor.GREEN + " for " + ChatColor.GRAY + target.getName());
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        }
        else if (strings[3].equalsIgnoreCase("set")) {
            skill.setExperience(amount);
            target.sendMessage(ChatColor.GREEN + "Set " + ChatColor.GRAY + skill.getSkillName() + ChatColor.GREEN + " XP to " + ChatColor.GRAY + amount);
            target.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            p.sendRawMessage(ChatColor.GREEN + "Set " + ChatColor.GRAY + skill.getSkillName() + ChatColor.GREEN
                    + " XP to " + ChatColor.GRAY + amount + " for " + ChatColor.GREEN + target.getName());
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        }
        else {
            p.sendRawMessage(ChatColor.RED + "Correct usage: " + ChatColor.GRAY + "/survivalskills xp <skill> add/remove/set <amount>");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        }
    }

    public void handleLevel(Player p, Player target, Skill skill, String[] strings) {
        if (strings.length <= 4) {
            p.sendMessage(ChatColor.RED + "Correct usage: " + ChatColor.GRAY + "/survivalskills level <player> <skill> add/remove/set <amount>");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(strings[4]);
            if (amount < 0) amount = Math.abs(amount);
        } catch (NumberFormatException e) {
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            p.sendMessage(ChatColor.RED + "Please enter a valid number.");
            return;
        }

        if (strings[3].equalsIgnoreCase("add")) {
            skill.changeLevel(amount);
            target.sendMessage(ChatColor.GREEN + "Added " + ChatColor.GRAY + amount + ChatColor.GREEN + " levels to " + ChatColor.GRAY + skill.getSkillName());
            target.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            p.sendRawMessage(ChatColor.GREEN + "Added " + ChatColor.GRAY + amount + ChatColor.GREEN + " levels to "
                    + ChatColor.GRAY + skill.getSkillName() + ChatColor.GREEN + " for " + ChatColor.GRAY + target.getName());
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        }
        else if (strings[3].equalsIgnoreCase("remove")) {
            skill.changeLevel(-amount);
            target.sendMessage(ChatColor.GREEN + "Removed " + ChatColor.GRAY + amount + ChatColor.GREEN + " levels from " + ChatColor.GRAY + skill.getSkillName());
            target.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            p.sendRawMessage(ChatColor.GREEN + "Removed " + ChatColor.GRAY + amount + ChatColor.GREEN + " levels from "
                    + ChatColor.GRAY + skill.getSkillName() + ChatColor.GREEN + " for " + ChatColor.GRAY + target.getName());
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        }
        else if (strings[3].equalsIgnoreCase("set")) {
            skill.setLevel(amount);
            target.sendMessage(ChatColor.GREEN + "Set " + ChatColor.GRAY + skill.getSkillName() + ChatColor.GREEN + " level to " + ChatColor.GRAY + amount);
            target.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            p.sendRawMessage(ChatColor.GREEN + "Set " + ChatColor.GRAY + skill.getSkillName() + ChatColor.GREEN
                    + " level to " + ChatColor.GRAY + amount + " for " + ChatColor.GREEN + target.getName());
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        }
        else {
            p.sendMessage(ChatColor.RED + "Correct usage: " + ChatColor.GRAY + "/survivalskills level <skill> add/remove/set <amount>");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        }
    }

    public void updateRewards(Player p, Skill skill) {
        for (Reward reward : plugin.getSkillManager().getPlayerRewards(p).getRewardList().get(skill.getSkillName())) {
            if (skill.getLevel() >= reward.getLevel() && !reward.isApplied() && reward.isEnabled()) {
                p.sendMessage(ChatColor.GREEN + "You have unlocked a new reward: " + ChatColor.GRAY + reward.getName());
            }
        }
        plugin.getSkillManager().getPlayerRewards(p).enableSkillRewards(p, skill);
    }

    public Reward findReward(String rewardName) {
        for (Map.Entry<String, ArrayList<Reward>> skill : plugin.getSkillManager().getDefaultPlayerRewards().getRewardList().entrySet()) {
            for (Reward reward : skill.getValue()) {
                if (!reward.getName().equalsIgnoreCase(rewardName)) continue;
                return reward;
            }
        }
        return null;
    }

    public void toggleReward(Reward reward) {
        reward.setEnabled(!reward.isEnabled());
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set(reward.getSkillType() + "." + reward.getName() + ".Enabled", reward.isEnabled());
        try {
            config.save(configFile);
        } catch (Exception e) {
            Bukkit.getLogger().warning("Could not save config file.");
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
            rewards.getReward(reward.getSkillType(), reward.getName()).setEnabled(reward.isEnabled());
        }
    }

    public void changeRewardLevel(Reward reward, int level) {
        reward.setLevel(level);
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set(reward.getSkillType() + "." + reward.getName() + ".Level", level);
        try {
            config.save(configFile);
        } catch (Exception e) {
            Bukkit.getLogger().warning("Could not save config file.");
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
            rewards.getReward(reward.getSkillType(), reward.getName()).setLevel(level);
        }
    }
}
