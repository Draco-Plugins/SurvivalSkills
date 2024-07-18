package sir_draco.survivalskills.Commands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.Abilities.AbilityTimer;
import sir_draco.survivalskills.Abilities.WaterBreathingTimer;
import sir_draco.survivalskills.Rewards.RewardNotifications;
import sir_draco.survivalskills.SurvivalSkills;

public class WaterBreathingCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public WaterBreathingCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("waterbreathing");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;
        // Check for level requirements
        if (!plugin.getSkillManager().getDefaultPlayerRewards().getReward("Fishing", "WaterBreathingI").isEnabled()) {
            p.sendRawMessage(ChatColor.RED + "Water Breathing is not enabled on this server");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return false;
        }

        if (!plugin.getSkillManager().getPlayerRewards(p).getReward("Fishing", "WaterBreathingI").isApplied() && !plugin.isForced(p, strings)) {
            if (p.hasPermission("survivalskills.op")) {
                p.sendRawMessage(ChatColor.RED + "To force water breathing use: " + ChatColor.AQUA + "/waterbreathing force");
            }
            p.sendRawMessage(ChatColor.GREEN + "You need to be fishing level " + ChatColor.AQUA +
                    plugin.getSkillManager().getDefaultPlayerRewards().getReward("Fishing", "WaterBreathingI").getLevel()
                    + ChatColor.GREEN + " to use Water Breathing");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        // Check for cooldown
        AbilityTimer timer = plugin.getAbilityManager().getAbility(p, "WaterBreathing");
        if (timer != null) {
            if (timer.isActive()) {
                p.sendRawMessage(ChatColor.YELLOW + "Water Breathing is already active");
            }
            else {
                p.sendRawMessage(ChatColor.RED + "You can use water breathing again in: "
                        + RewardNotifications.cooldown(timer.getTimeTillReset()));
            }
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        // Give water breathing
        if (!plugin.getSkillManager().getPlayerRewards(p).getReward("Fishing", "WaterBreathingII").isApplied()) {
            enableWaterBreathing(p, 900, 3600);
        }
        else enableWaterBreathing(p, 1800, 1800);
        return true;
    }

    public void enableWaterBreathing(Player p, int activeTime, int cooldownTime) {
        AbilityTimer abilityTimer = new AbilityTimer(plugin, "WaterBreathing", p, activeTime, cooldownTime);
        abilityTimer.runTaskTimerAsynchronously(plugin, 0, 20);
        WaterBreathingTimer waterBreathingTimer = new WaterBreathingTimer(plugin, activeTime, p);
        waterBreathingTimer.runTaskTimerAsynchronously(plugin, 0, 20);
        plugin.getFishingListener().getWaterBreathers().add(p);
        p.sendRawMessage(ChatColor.GREEN + "Water Breathing has been enabled!");
        p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
    }
}
