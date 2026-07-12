package sir_draco.survivalskills.commands.admin_commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.rewards.PlayerRewards;
import sir_draco.survivalskills.rewards.Reward;
import sir_draco.survivalskills.skills.Skill;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.skills.SkillManager;
import sir_draco.survivalskills.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class SurvivalSkillsCommand implements CommandExecutor {
    private final SurvivalSkills plugin;

    public SurvivalSkillsCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("survivalskills");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) return false;
        if (args.length == 0) {
            sendVersionBanner(p);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help" -> handleHelp(p);
            case "xp" -> handleXp(p, args);
            case "level" -> handleLevel(p, args);
            case "skillxp" -> handleSkillXp(p, args);
            case "togglereward" -> handleToggleReward(p, args);
            case "rewardlevel" -> handleRewardLevel(p, args);
            default -> sendUsageError(p, "/survivalskills help/level/xp");
        }
        return true;
    }

    // Subcommand handlers
    private void sendVersionBanner(Player p) {
        p.sendMessage("§6SurvivalSkills §7- §eVersion: " + plugin.getDescription().getVersion());
        p.sendMessage("§6SurvivalSkills §7- §eAuthor: Sir_Draco");
        p.sendMessage("§6SurvivalSkills §7- §eCommands: /survivalskills help");
    }

    private void handleHelp(Player p) {
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
    }

    private void handleXp(Player admin, String[] args) {
        if (args.length <= 2) {
            sendUsageError(admin, "/survivalskills xp <player> <skill> <add/remove/set> <amount>");
            return;
        }
        Optional<Player> targetOpt = Utils.findPlayer(args[1]);
        if (targetOpt.isEmpty()) {
            sendErrorMessage(admin, "Player not found.");
            return;
        }
        Player target = targetOpt.get();
        if (!SkillCategory.isMainSkill(args[2])) {
            sendErrorMessage(admin, "Skill not found.");
            return;
        }
        Skill skill = SkillManager.getSkill(target.getUniqueId(), SkillCategory.fromString(args[2]));
        modifyXp(admin, target, skill, args);
        updateRewards(target, skill);
    }

    private void handleLevel(Player admin, String[] args) {
        if (args.length <= 2) {
            sendUsageError(admin, "/survivalskills level <player> <skill> <add/remove/set> <amount>");
            return;
        }
        Optional<Player> levelTargetOpt = Utils.findPlayer(args[1]);
        if (levelTargetOpt.isEmpty()) {
            sendErrorMessage(admin, "Player not found.");
            return;
        }
        Player target = levelTargetOpt.get();
        if (args[2].equalsIgnoreCase("all")) {
            for (Skill skill : plugin.getSkillManager().getPlayerSkills().get(target.getUniqueId()).getSkills()) {
                modifyLevel(admin, target, skill, args);
                updateRewards(target, skill);
            }
            return;
        }
        if (!SkillCategory.isMainSkill(args[2])) {
            sendErrorMessage(admin, "Skill not found.");
            return;
        }
        Skill skill = SkillManager.getSkill(target.getUniqueId(), SkillCategory.fromString(args[2]));
        modifyLevel(admin, target, skill, args);
        updateRewards(target, skill);
    }

    private void handleSkillXp(Player p, String[] args) {
        if (args.length < 3) {
            sendUsageError(p, "/survivalskills skillxp <skill> <amount>");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
            if (amount < 0) {
                p.sendRawMessage(ChatColor.RED + "XP less than 0 won't work.");
                p.sendRawMessage(ChatColor.GREEN + "XP amount set to 1!");
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                amount = 1;
            }
        } catch (NumberFormatException e) {
            playErrorSound(p);
            p.sendMessage(ChatColor.RED + "Please enter a valid number.");
            return;
        }

        if (!SkillCategory.isBaseSkill(args[1])) {
            sendErrorMessage(p, "Skill not found.");
            return;
        }
        SkillCategory skill = SkillCategory.fromString(args[1]);
        setSkillExperience(skill, amount);
        p.sendRawMessage(ChatColor.GREEN + skill.getDisplayName() + " skill XP set to " + ChatColor.AQUA + amount);
        p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        saveConfigValue(skill.getXpConfigKey(), amount);
    }

    private void setSkillExperience(SkillCategory skill, int amount) {
        SkillManager skillManager = Objects.requireNonNull(
                plugin.getSkillManager(),
                "Skill manager is unavailable"
        );

        switch (skill) {
            case BUILDING -> skillManager.setBuildingXP(amount);
            case CRAFTING -> skillManager.setCraftingXP(amount);
            case EXPLORING -> skillManager.setExploringXP(amount);
            case FARMING -> skillManager.setFarmingXP(amount);
            case FIGHTING -> skillManager.setFightingXP(amount);
            case FISHING -> skillManager.setFishingXP(amount);
            case MINING -> skillManager.setMiningXP(amount);
            default -> throw new IllegalArgumentException("Unsupported base skill: " + skill);
        }
    }

    private void handleToggleReward(Player p, String[] args) {
        if (args.length < 2) {
            sendUsageError(p, "/survivalskills togglereward <reward>");
            return;
        }
        Optional<Reward> reward = findReward(args[1]);
        if (reward.isEmpty()) {
            sendErrorMessage(p, "Reward not found.");
            return;
        }
        Reward r = reward.get();
        toggleReward(r);
        p.sendRawMessage(ChatColor.GREEN + "Toggled reward: " + ChatColor.GRAY + r.getName());
        p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
    }

    private void handleRewardLevel(Player p, String[] args) {
        if (args.length < 3) {
            sendUsageError(p, "/survivalskills rewardlevel <reward> <level>");
            return;
        }
        Optional<Reward> reward = findReward(args[1]);
        if (reward.isEmpty()) {
            sendErrorMessage(p, "Reward not found.");
            return;
        }

        int level;
        try {
            level = Integer.parseInt(args[2]);
            if (level < 1) level = Math.abs(level);
            if (level > Skill.MAX_LEVEL) level = Skill.MAX_LEVEL;
        } catch (NumberFormatException e) {
            playErrorSound(p);
            p.sendMessage(ChatColor.RED + "Please enter a valid number.");
            return;
        }

        Reward r = reward.get();
        changeRewardLevel(r, level);
        p.sendRawMessage(ChatColor.GREEN + "Change reward level: " + ChatColor.GRAY + r.getName() + " to " + r.getLevel());
        p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
    }

    // XP / level adjustment on a target player
    private void modifyXp(Player admin, Player target, Skill skill, String[] args) {
        if (args.length <= 4) {
            sendUsageError(admin, "/survivalskills xp <player> <skill> add/remove/set <amount>");
            return;
        }
        Integer amount = parseAmountWithAbs(admin, args, 4);
        if (amount == null) return;

        String op = args[3];
        SkillCategory category = skill.getSkillCategory();
        if (op.equalsIgnoreCase("add")) {
            skill.changeExperience(amount, Skill.MAX_LEVEL);
            notifyBoth(admin, target,
                    ChatColor.GREEN + "Added " + ChatColor.GRAY + amount + ChatColor.GREEN + " XP to " + ChatColor.GRAY + category,
                    ChatColor.GREEN + "Added " + ChatColor.GRAY + amount + ChatColor.GREEN + " XP to "
                            + ChatColor.GRAY + category + ChatColor.GREEN + " for " + ChatColor.GRAY + target.getName());
        } else if (op.equalsIgnoreCase("remove")) {
            skill.changeExperience(-amount, Skill.MAX_LEVEL);
            notifyBoth(admin, target,
                    ChatColor.GREEN + "Removed " + ChatColor.GRAY + amount + ChatColor.GREEN + " XP from " + ChatColor.GRAY + category,
                    ChatColor.GREEN + "Removed " + ChatColor.GRAY + amount + ChatColor.GREEN + " XP from "
                            + ChatColor.GRAY + category + ChatColor.GREEN + " for " + ChatColor.GRAY + target.getName());
        } else if (op.equalsIgnoreCase("set")) {
            skill.setExperience(amount);
            notifyBoth(admin, target,
                    ChatColor.GREEN + "Set " + ChatColor.GRAY + category + ChatColor.GREEN + " XP to " + ChatColor.GRAY + amount,
                    ChatColor.GREEN + "Set " + ChatColor.GRAY + category + ChatColor.GREEN
                            + " XP to " + ChatColor.GRAY + amount + " for " + ChatColor.GREEN + target.getName());
        } else {
            sendUsageError(admin, "/survivalskills xp <skill> add/remove/set <amount>");
        }
    }

    private void modifyLevel(Player admin, Player target, Skill skill, String[] args) {
        if (args.length <= 4) {
            // Original level branch uses sendMessage (not sendRawMessage) and plays no sound here.
            admin.sendMessage(ChatColor.RED + "Correct usage: " + ChatColor.GRAY
                    + "/survivalskills level <player> <skill> add/remove/set <amount>");
            return;
        }
        Integer amount = parseAmountWithAbs(admin, args, 4);
        if (amount == null) return;

        String op = args[3];
        SkillCategory category = skill.getSkillCategory();
        if (op.equalsIgnoreCase("add")) {
            skill.changeLevel(amount);
            notifyBoth(admin, target,
                    ChatColor.GREEN + "Added " + ChatColor.GRAY + amount + ChatColor.GREEN + " levels to " + ChatColor.GRAY + category,
                    ChatColor.GREEN + "Added " + ChatColor.GRAY + amount + ChatColor.GREEN + " levels to "
                            + ChatColor.GRAY + category + ChatColor.GREEN + " for " + ChatColor.GRAY + target.getName());
        } else if (op.equalsIgnoreCase("remove")) {
            skill.changeLevel(-amount);
            notifyBoth(admin, target,
                    ChatColor.GREEN + "Removed " + ChatColor.GRAY + amount + ChatColor.GREEN + " levels from " + ChatColor.GRAY + category,
                    ChatColor.GREEN + "Removed " + ChatColor.GRAY + amount + ChatColor.GREEN + " levels from "
                            + ChatColor.GRAY + category + ChatColor.GREEN + " for " + ChatColor.GRAY + target.getName());
        } else if (op.equalsIgnoreCase("set")) {
            skill.setLevel(amount);
            notifyBoth(admin, target,
                    ChatColor.GREEN + "Set " + ChatColor.GRAY + category + ChatColor.GREEN + " level to " + ChatColor.GRAY + amount,
                    ChatColor.GREEN + "Set " + ChatColor.GRAY + category + ChatColor.GREEN
                            + " level to " + ChatColor.GRAY + amount + " for " + ChatColor.GREEN + target.getName());
        } else {
            admin.sendMessage(ChatColor.RED + "Correct usage: " + ChatColor.GRAY
                    + "/survivalskills level <skill> add/remove/set <amount>");
            playErrorSound(admin);
        }
    }

    private void updateRewards(Player p, Skill skill) {
        for (Reward reward : plugin.getSkillManager().getPlayerRewards(p).getRewardList().get(skill.getSkillCategory())) {
            if (skill.getLevel() >= reward.getLevel() && !reward.isApplied() && reward.isEnabled()) {
                p.sendMessage(ChatColor.GREEN + "You have unlocked a new reward: " + ChatColor.GRAY + reward.getName());
            }
        }
        plugin.getSkillManager().getPlayerRewards(p).enableSkillRewards(p, skill);
    }

    // Reward mutation (default config + live per-player state)
    Optional<Reward> findReward(String rewardName) {
        for (ArrayList<Reward> rewards : plugin.getSkillManager().getDefaultPlayerRewards().getRewardList().values()) {
            for (Reward reward : rewards) {
                if (reward.getName().equalsIgnoreCase(rewardName)) return Optional.of(reward);
            }
        }
        return Optional.empty();
    }

    void toggleReward(Reward reward) {
        reward.setEnabled(!reward.isEnabled());
        saveConfigValue(reward.getSkillCategory() + "." + reward.getName() + ".Enabled", reward.isEnabled());
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
            rewards.getReward(reward.getSkillCategory(), reward.getName()).setEnabled(reward.isEnabled());
        }
    }

    void changeRewardLevel(Reward reward, int level) {
        reward.setLevel(level);
        saveConfigValue(reward.getSkillCategory() + "." + reward.getName() + ".Level", level);
        for (Player p : Bukkit.getOnlinePlayers()) {
            PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
            rewards.getReward(reward.getSkillCategory(), reward.getName()).setLevel(level);
        }
    }

    // Small response helpers (sound + message patterns repeated everywhere)
    private void sendUsageError(Player p, String usage) {
        p.sendRawMessage(ChatColor.RED + "Correct usage: " + ChatColor.GRAY + usage);
        playErrorSound(p);
    }

    private void sendErrorMessage(Player p, String message) {
        p.sendRawMessage(ChatColor.RED + message);
        playErrorSound(p);
    }

    private void playErrorSound(Player p) {
        p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    }

    /**
     * Notifies both the target and the admin that a skill modification happened.
     * The target hears the success sound emanating from the admin's location
     * (preserving the original playSound(target, admin, ...) overload).
     */
    private void notifyBoth(Player admin, Player target, String targetMessage, String adminMessage) {
        target.sendMessage(targetMessage);
        target.playSound(admin, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        admin.sendRawMessage(adminMessage);
        admin.playSound(admin, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
    }

    /**
     * Parses {@code args[index]} as a non-negative int (negatives abs'd).
     * Returns null on failure after sending the standard error response.
     */
    private Integer parseAmountWithAbs(Player admin, String[] args, int index) {
        try {
            return Math.abs(Integer.parseInt(args[index]));
        } catch (NumberFormatException e) {
            playErrorSound(admin);
            admin.sendMessage(ChatColor.RED + "Please enter a valid number.");
            return null;
        }
    }

    // Config persistence
    private void saveConfigValue(String path, Object value) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set(path, value);
        try {
            config.save(configFile);
        } catch (Exception e) {
            Bukkit.getLogger().warning("Could not save config file.");
        }
    }
}
