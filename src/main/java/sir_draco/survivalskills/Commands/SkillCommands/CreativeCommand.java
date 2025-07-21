package sir_draco.survivalskills.Commands.SkillCommands;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.SurvivalSkills;

@SuppressWarnings("NullableProblems")
public class CreativeCommand implements CommandExecutor {

    public CreativeCommand(SurvivalSkills plugin) {
        PluginCommand command = plugin.getCommand("sscreative");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player p)) return false;
        if (!p.hasPermission("survivalskills.creative")) return false;

        GameMode mode = p.getGameMode();
        if (mode == GameMode.CREATIVE) {
            p.setGameMode(GameMode.SURVIVAL);
            p.sendRawMessage(ChatColor.GREEN + "You are now in survival mode.");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        }
        else {
            p.setGameMode(GameMode.CREATIVE);
            p.sendRawMessage(ChatColor.GREEN + "You are now in creative mode.");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        }
        return true;
    }
}
