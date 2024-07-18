package sir_draco.survivalskills.Commands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.SurvivalSkills;

public class EatCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public EatCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("sseat");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;

        // Check for level requirements
        if (!plugin.getSkillManager().getDefaultPlayerRewards().getReward("Farming", "Eat").isEnabled()) {
            p.sendRawMessage(ChatColor.RED + "Eat is not enabled on this server");
            return false;
        }

        if (!plugin.getSkillManager().getPlayerRewards(p).getReward("Farming", "Eat").isApplied() && !plugin.isForced(p, strings)) {
            if (p.hasPermission("survivalskills.op")) {
                p.sendRawMessage(ChatColor.RED + "To force eat use: " + ChatColor.AQUA + "/sseat force");
            }
            p.sendRawMessage(ChatColor.GREEN + "You need to be farming level " + ChatColor.AQUA +
                    plugin.getSkillManager().getDefaultPlayerRewards().getReward("Farming", "Eat").getLevel()
                    + ChatColor.GREEN + " to use Eat");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        // Feed the player
        p.setFoodLevel(20);
        p.playSound(p, Sound.ENTITY_PLAYER_BURP, 1, 1);
        return true;
    }
}
