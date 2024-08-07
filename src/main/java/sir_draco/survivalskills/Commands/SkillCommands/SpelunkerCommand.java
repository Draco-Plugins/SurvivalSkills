package sir_draco.survivalskills.Commands.SkillCommands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.Abilities.AbilityTimer;
import sir_draco.survivalskills.Abilities.SpelunkerAbilitySync;
import sir_draco.survivalskills.Rewards.RewardNotifications;
import sir_draco.survivalskills.SurvivalSkills;

public class SpelunkerCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public SpelunkerCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("spelunker");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;
        // Check for level requirements reset and active times and radius
        if (!plugin.getSkillManager().getDefaultPlayerRewards().getReward("Mining", "SpelunkerI").isEnabled()) {
            p.sendRawMessage(ChatColor.RED + "Spelunker is not enabled on this server");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return false;
        }

        // Show them how much time they have left
        if (strings.length == 1 && strings[0].equalsIgnoreCase("time")) {
            AbilityTimer timer = plugin.getAbilityManager().getAbility(p, "Spelunker");
            if (timer != null && timer.isActive()) {
                p.sendRawMessage(ChatColor.RED + "Spelunking time left: " + RewardNotifications.cooldown(timer.getActiveTimeLeft()));
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                return true;
            }
        }

        int resetTime;
        int activeTime;
        int radius;
        if (!plugin.getSkillManager().getPlayerRewards(p).getReward("Mining", "SpelunkerI").isApplied() && !plugin.isForced(p, strings)) {
            if (p.hasPermission("survivalskills.op")) {
                p.sendRawMessage(ChatColor.RED + "To force spelunker use: " + ChatColor.AQUA + "/spelunker force");
            }
            p.sendRawMessage(ChatColor.RED + "You need to be mining level " + ChatColor.AQUA
                    + plugin.getSkillManager().getDefaultPlayerRewards().getReward("Mining", "SpelunkerI").getLevel()
                    + ChatColor.RED + " to use Spelunker");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }
        else if (!plugin.getSkillManager().getPlayerRewards(p).getReward("Mining", "SpelunkerII").isApplied()) {
            resetTime = 3600; // 60 minutes
            activeTime = 300; // 5 minutes
            radius = 5;
        }
        else if (!plugin.getSkillManager().getPlayerRewards(p).getReward("Mining", "SpelunkerIII").isApplied()) {
            resetTime = 1800; // 30 minutes
            activeTime = 900; // 15 minutes
            radius = 10;
        }
        else {
            resetTime = 1800; // 30 minutes
            activeTime = 1800; // 30 minutes
            radius = 15;
        }

        if (plugin.isForced(p, strings)) resetTime = 3;

        // Check if it is already active
        AbilityTimer timer = plugin.getAbilityManager().getAbility(p, "Spelunker");
        if (timer == null) {
            // No timer means a new one can be made
            newTimer(p, activeTime, resetTime, radius);
            return true;
        }
        else if (timer.isActive()){
            // Disable spelunker
            timer.endAbility();
            p.sendRawMessage(ChatColor.YELLOW + "Spelunker has been disabled!");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            return true;
        }

        // If a timer exists, and it is not active then it has to be in cooldown mode
        if (plugin.isForced(p, strings)) {
            timer.endCooldown();
            plugin.getAbilityManager().removeAbility(p, "Spelunker");
            newTimer(p, activeTime, resetTime, radius);
        }
        else {
            p.sendRawMessage(ChatColor.RED + "You can use spelunker again in: " + RewardNotifications.cooldown(timer.getTimeTillReset()));
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        }
        return true;
    }

    public void newTimer(Player p, int activeTime, int resetTime, int radius) {
        AbilityTimer timer = new AbilityTimer(plugin, "Spelunker", p, activeTime, resetTime);
        timer.runTaskTimerAsynchronously(plugin, 0, 20);
        plugin.getAbilityManager().addAbility(p, timer);
        SpelunkerAbilitySync spelunker = new SpelunkerAbilitySync(plugin, radius, p);
        spelunker.runTaskTimer(plugin, 0, 10);
        plugin.getMiningListener().getSpelunkerTracker().put(p, spelunker);
        p.sendRawMessage(ChatColor.GREEN + "Spelunker has been enabled!");
        p.playSound(p, Sound.ENTITY_WANDERING_TRADER_DRINK_POTION, 1, 1);
    }
}
