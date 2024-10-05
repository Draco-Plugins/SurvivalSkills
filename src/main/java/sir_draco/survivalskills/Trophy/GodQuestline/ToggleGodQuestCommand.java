package sir_draco.survivalskills.Trophy.GodQuestline;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.SurvivalSkills;

public class ToggleGodQuestCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public ToggleGodQuestCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("togglegodquest");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        boolean currentStatus = plugin.getTrophyManager().isGodQuestEnabled();
        plugin.getTrophyManager().setGodQuestEnabled(!currentStatus);

        if (!(sender instanceof Player)) return true;
        Player p = (Player) sender;
        p.sendRawMessage(ChatColor.GREEN + "God Quest is now " + (currentStatus ? "disabled" : "enabled"));
        p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        return true;
    }
}
