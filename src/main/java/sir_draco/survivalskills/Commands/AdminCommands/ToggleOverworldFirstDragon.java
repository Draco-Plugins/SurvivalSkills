package sir_draco.survivalskills.Commands.AdminCommands;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.metadata.FixedMetadataValue;
import sir_draco.survivalskills.SurvivalSkills;

public class ToggleOverworldFirstDragon implements CommandExecutor {

    private final SurvivalSkills plugin;

    public ToggleOverworldFirstDragon(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("toggleoverworldfirstdragon");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        World world = Bukkit.getWorld("world");
        if (world == null) {
            sender.sendMessage("World not found");
            return false;
        }

        if (world.hasMetadata("killedfirstdragon")) {
            world.removeMetadata("killedfirstdragon", plugin);
            sender.sendMessage("First dragon kill removed");
        }
        else {
            world.setMetadata("killedfirstdragon", new FixedMetadataValue(plugin, true));
            sender.sendMessage("World has now killed the first dragon");
        }
        return true;
    }
}
