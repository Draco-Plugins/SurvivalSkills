package sir_draco.survivalskills.Commands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.SurvivalSkills;

public class PeacefulMinerCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public PeacefulMinerCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        plugin.getCommand("peacefulminer").setExecutor(this);
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;

        // Check for if its enabled
        if (!plugin.getDefaultPlayerRewards().getReward("Mining", "PeacefulMiner").isEnabled()) {
            p.sendRawMessage(ChatColor.RED + "Peaceful Miner is not enabled on this server");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return false;
        }

        // Check if peaceful mining is allowed
        if (!plugin.getPlayerRewards(p).getReward("Mining", "PeacefulMiner").isApplied() && !plugin.isForced(p, strings)) {
            if (p.hasPermission("survivalskills.op")) {
                p.sendRawMessage(ChatColor.RED + "To force peaceful miner use: " + ChatColor.AQUA + "/peacefulminer force");
            }
            p.sendRawMessage(ChatColor.RED + "You need to be mining level " + ChatColor.AQUA
                    + plugin.getDefaultPlayerRewards().getReward("Mining", "PeacefulMiner").getLevel()
                    + ChatColor.RED + " to use Peaceful Miner");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        // Add miner to list or remove them
        if (plugin.getMiningListener().getPeacefulMiners().contains(p)) {
            plugin.getMiningListener().getPeacefulMiners().remove(p);
            p.sendRawMessage(ChatColor.YELLOW + "Peaceful Mining has been disabled!");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        }
        else {
            plugin.getMiningListener().getPeacefulMiners().add(p);
            p.sendRawMessage(ChatColor.YELLOW + "Peaceful Mining has been enabled!");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        }

        return true;
    }
}
