package sir_draco.survivalskills.Commands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.Rewards.Reward;
import sir_draco.survivalskills.SurvivalSkills;

public class TogglePhantomsCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public TogglePhantomsCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("togglephantoms");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;

        Reward reward = plugin.getPlayerRewards(p).getReward("Fighting", "TogglePhantomSpawns");
        if (reward == null || !reward.isEnabled()) {
            p.sendRawMessage(ChatColor.RED + "That reward is not enabled");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }
        if (!reward.isApplied()) {
            p.sendRawMessage(ChatColor.RED + "You need to be fighting level " + ChatColor.AQUA
                    + reward.getLevel() + ChatColor.RED + " to toggle phantom spawns");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        if (plugin.getFightingListener().getNoPhantomSpawns().contains(p)) {
            plugin.getFightingListener().getNoPhantomSpawns().remove(p);
            p.sendRawMessage(ChatColor.GREEN + "Phantom spawns are now enabled");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        } else {
            plugin.getFightingListener().getNoPhantomSpawns().add(p);
            p.sendRawMessage(ChatColor.GREEN + "Phantom spawns are now disabled");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        }
        return true;
    }
}
