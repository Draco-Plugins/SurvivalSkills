package sir_draco.survivalskills.Commands.SkillCommands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.Abilities.AbilityTimer;
import sir_draco.survivalskills.Abilities.FlyingTimer;
import sir_draco.survivalskills.Rewards.RewardNotifications;
import sir_draco.survivalskills.SurvivalSkills;

public class FlightCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public FlightCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("flight");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;

        // Check if the player can use the command
        if (!plugin.getSkillManager().getDefaultPlayerRewards().getReward("Building", "FlightI").isEnabled()) {
            p.sendRawMessage(ChatColor.RED + "Flight is not enabled on this server");
            return false;
        }

        // Check if the player has a cooldown
        AbilityTimer timer = plugin.getAbilityManager().getAbility(p, "Flight");
        if (timer != null) {
            if (timer.isActive()) timer.endAbility();
            else {
                p.sendRawMessage(ChatColor.RED + "You can use flight again in: " + RewardNotifications.cooldown(timer.getTimeTillReset()));
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return false;
            }
            return true;
        }
        else if (p.getAllowFlight()) {
            p.setAllowFlight(false);
            p.setFlying(false);
            p.sendRawMessage(ChatColor.YELLOW + "You have disabled your flight!");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            return true;
        }

        // Give flight
        int resetTime;
        int activeTime;
        int speed;
        float baseSpeed = 0.025f;
        if (!plugin.getSkillManager().getPlayerRewards(p).getReward("Building", "FlightI").isEnabled()
            || !plugin.getSkillManager().getPlayerRewards(p).getReward("Building", "FlightI").isApplied()) {
            p.sendRawMessage(ChatColor.RED + "You need to be building level " + ChatColor.AQUA
                    + plugin.getSkillManager().getDefaultPlayerRewards().getReward("Building", "FlightI").getLevel()
                    + ChatColor.RED + " to use Flight");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }
        else if (!plugin.getSkillManager().getPlayerRewards(p).getReward("Building", "FlightII").isEnabled()
                || !plugin.getSkillManager().getPlayerRewards(p).getReward("Building", "FlightII").isApplied()) {
            resetTime = 3600; // 60 minutes
            activeTime = 300; // 5 minutes
            speed = 1;
        }
        else if (!plugin.getSkillManager().getPlayerRewards(p).getReward("Building", "FlightIII").isEnabled() ||
                !plugin.getSkillManager().getPlayerRewards(p).getReward("Building", "FlightIII").isApplied()) {
            resetTime = 1800; // 30 minutes
            activeTime = 900; // 15 minutes
            speed = 2;
        }
        else if (!plugin.getSkillManager().getPlayerRewards(p).getReward("Building", "FlightIV").isEnabled() ||
                !plugin.getSkillManager().getPlayerRewards(p).getReward("Building", "FlightIV").isApplied()){
            resetTime = 1800; // 30 minutes
            activeTime = 1800; // 30 minutes
            speed = 3;
        }
        else {
            p.setAllowFlight(true);
            p.setFlying(true);
            speed = 4;
            p.setFlySpeed(baseSpeed * speed);
            p.sendRawMessage(ChatColor.GREEN + "You have enabled your flight!");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            return true;
        }

        p.setAllowFlight(true);
        p.setFlying(true);
        p.setFlySpeed(baseSpeed * speed);
        AbilityTimer abilityTimer = new AbilityTimer(plugin, "Flight", p, activeTime, resetTime);
        abilityTimer.setFlightSpeed(baseSpeed * speed);
        abilityTimer.runTaskTimerAsynchronously(plugin, 0, 20);
        plugin.getAbilityManager().addAbility(p, abilityTimer);
        FlyingTimer flyingTimer = new FlyingTimer(p, activeTime);
        flyingTimer.runTaskTimerAsynchronously(plugin, 0, 20);
        p.sendRawMessage(ChatColor.GREEN + "You have enabled your flight for " + ChatColor.AQUA
                + (activeTime / 60) + ChatColor.GREEN + " minutes!");
        p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        return true;
    }
}
