package sir_draco.survivalskills.commands.default_commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.wardrobe.WardrobeGui;

import java.util.Objects;

public final class WardrobeCommand implements CommandExecutor {

    private final WardrobeGui wardrobeGui;

    public WardrobeCommand(SurvivalSkills plugin, WardrobeGui wardrobeGui) {
        this.wardrobeGui = Objects.requireNonNull(wardrobeGui, "Wardrobe GUI cannot be null");
        PluginCommand command = plugin.getCommand("wardrobe");
        if (command != null)
            command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments) {
        if (!(sender instanceof Player player))
            return false;
        wardrobeGui.open(player);
        return true;
    }
}
