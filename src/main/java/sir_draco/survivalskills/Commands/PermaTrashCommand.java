package sir_draco.survivalskills.Commands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.Abilities.AutoTrash;
import sir_draco.survivalskills.Rewards.Reward;
import sir_draco.survivalskills.SurvivalSkills;

public class PermaTrashCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public PermaTrashCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("permatrash");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;

        Reward reward = plugin.getSkillManager().getPlayerRewards(p).getReward("Fishing", "PermaTrash");
        if ((!reward.isEnabled() || !reward.isApplied()) && !plugin.isForced(p, strings)) {
            p.sendRawMessage(ChatColor.RED + "You have to be fishing level " + ChatColor.AQUA + reward.getLevel() + ChatColor.RED + " to use this ability");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        AutoTrash trash;
        if (!plugin.getFishingListener().getPermaTrash().containsKey(p)) {
            trash = new AutoTrash(false);
            plugin.getFishingListener().getPermaTrash().put(p, trash);
        }
        else trash = plugin.getFishingListener().getPermaTrash().get(p);

        plugin.getFishingListener().getOpenTrashInventories().add(p);
        trash.openTrashInventory(p);
        return true;
    }
}
