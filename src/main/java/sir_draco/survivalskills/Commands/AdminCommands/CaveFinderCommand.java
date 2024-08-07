package sir_draco.survivalskills.Commands.AdminCommands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.Abilities.CaveFinderAsync;
import sir_draco.survivalskills.SurvivalSkills;

public class CaveFinderCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public CaveFinderCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("cavefinder");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;
        CaveFinderAsync caveFinder = new CaveFinderAsync(p, plugin);
        caveFinder.runTaskAsynchronously(plugin);
        return true;
    }
}
