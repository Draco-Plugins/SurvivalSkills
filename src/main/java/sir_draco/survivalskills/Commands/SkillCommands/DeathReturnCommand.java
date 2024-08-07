package sir_draco.survivalskills.Commands.SkillCommands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.Boards.LeaderboardPlayer;
import sir_draco.survivalskills.SurvivalSkills;

public class DeathReturnCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public DeathReturnCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("deathreturn");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;
        LeaderboardPlayer player = plugin.getLeaderboardTracker().get(p.getUniqueId());
        if (player == null) {
            p.sendRawMessage(ChatColor.RED + "An error occurred while trying to get your data.");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        int deaths = player.getDeathScore();
        if (deaths < 75) {
            p.sendRawMessage(ChatColor.RED + "You must die at least 75 times to use this command.");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        if (!plugin.getPlayerListener().getDeathLocations().containsKey(p)) {
            p.sendRawMessage(ChatColor.RED + "You have no death location to return to.");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        p.teleport(plugin.getPlayerListener().getDeathLocations().get(p));
        p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        return true;
    }
}
