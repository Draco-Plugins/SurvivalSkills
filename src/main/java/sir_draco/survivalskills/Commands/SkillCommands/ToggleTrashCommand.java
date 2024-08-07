package sir_draco.survivalskills.Commands.SkillCommands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.Rewards.Reward;
import sir_draco.survivalskills.SurvivalSkills;

public class ToggleTrashCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public ToggleTrashCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("toggletrash");
        if (command != null) command.setExecutor(this);
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;

        Reward reward = plugin.getSkillManager().getPlayerRewards(p).getReward("Fishing", "AutoTrashI");
        if (reward == null || !reward.isEnabled()) {
            p.sendRawMessage(ChatColor.RED + "That reward is not enabled");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }
        if (!reward.isApplied()) {
            p.sendRawMessage(ChatColor.RED + "You need to be fishing level " + ChatColor.AQUA
                    + reward.getLevel() + ChatColor.RED + " to toggle auto trash");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        if (plugin.getFishingListener().getDisabledAutoTrash().contains(p)) {
            plugin.getFishingListener().getDisabledAutoTrash().remove(p);
            p.sendRawMessage(ChatColor.GREEN + "Auto trash is now enabled");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        }
        else {
            plugin.getFishingListener().getDisabledAutoTrash().add(p);
            p.sendRawMessage(ChatColor.GREEN + "Auto trash is now disabled");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        }
        return true;
    }
}
