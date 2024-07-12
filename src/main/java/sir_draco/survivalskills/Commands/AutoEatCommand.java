package sir_draco.survivalskills.Commands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.SurvivalSkills;

public class AutoEatCommand implements CommandExecutor {

    private SurvivalSkills plugin;

    public AutoEatCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        plugin.getCommand("autoeat").setExecutor(this);
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;
        // Check for level requirements
        if (!plugin.getDefaultPlayerRewards().getReward("Farming", "AutoEat").isEnabled()) {
            p.sendRawMessage(ChatColor.RED + "Auto Eat is not enabled on this server");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return false;
        }

        if (!plugin.getPlayerRewards(p).getReward("Farming", "AutoEat").isApplied() && !plugin.isForced(p, strings)) {
            if (p.hasPermission("survivalskills.op")) {
                p.sendRawMessage(ChatColor.RED + "To force auto eat use: " + ChatColor.AQUA + "/autoeat force");
            }
            p.sendRawMessage(ChatColor.GREEN + "You need to be farming level " + ChatColor.AQUA +
                    plugin.getDefaultPlayerRewards().getReward("Farming", "AutoEat").getLevel()
                    + ChatColor.GREEN + " to use Auto Eat");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        // Check if auto eat is being disabled
        if (plugin.getFarmingListener().getAutoEat().contains(p)) {
            plugin.getFarmingListener().getAutoEat().remove(p);
            p.sendRawMessage(ChatColor.YELLOW + "Auto Eat has been disabled!");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            return true;
        }
        plugin.getFarmingListener().getAutoEat().add(p);
        p.sendRawMessage(ChatColor.YELLOW + "Auto Eat has been enabled!");
        p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        return true;
    }
}
