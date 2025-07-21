package sir_draco.survivalskills.Commands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.SurvivalSkills;

public class VeinminerCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public VeinminerCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        plugin.getCommand("veinminer").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;

        // Check for level requirements reset and active times and radius
        if (!plugin.getDefaultPlayerRewards().getReward("Mining", "VeinminerI").isEnabled()) {
            p.sendRawMessage(ChatColor.RED + "Veinminer is not enabled on this server");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return false;
        }

        // Check if veinminer is being disabled
        if (plugin.getMiningListener().getVeinminerTracker().containsKey(p)) {
            plugin.getMiningListener().getVeinminerTracker().remove(p);
            p.sendRawMessage(ChatColor.YELLOW + "Veinminer has been disabled!");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            return true;
        }

        // Enable veinminer
        if (!plugin.getPlayerRewards(p).getReward("Mining", "VeinminerI").isApplied() && !plugin.isForced(p, strings)) {
            if (p.hasPermission("survivalskills.op")) {
                p.sendRawMessage(ChatColor.RED + "To force veinminer use: " + ChatColor.AQUA + "/veinminer force");
            }
            p.sendRawMessage(ChatColor.RED + "You need to be mining level " + ChatColor.AQUA
                    + plugin.getDefaultPlayerRewards().getReward("Mining", "VeinminerI").getLevel()
                    + ChatColor.RED + " to use veinminer");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }
        else if (plugin.getPlayerRewards(p).getReward("Mining", "VeinminerII").isApplied()) plugin.getMiningListener().getVeinminerTracker().put(p, 1);
        else plugin.getMiningListener().getVeinminerTracker().put(p, 0);

        p.sendRawMessage(ChatColor.GREEN + "Veinminer has been enabled!");
        p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

        return true;
    }
}
