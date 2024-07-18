package sir_draco.survivalskills.Commands;

import org.bukkit.Bukkit;
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

public class AutoTrashCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public AutoTrashCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("autotrash");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;

        boolean big = false;
        Reward reward = plugin.getSkillManager().getPlayerRewards(p).getReward("Fishing", "AutoTrashI");
        if (reward == null) {
            Bukkit.getLogger().warning("AutoTrashI reward not found for player " + p.getName());
            p.sendRawMessage(ChatColor.RED + "An error occurred while trying to use this ability");
            return true;
        }
        if ((!reward.isEnabled() || !reward.isApplied()) && !plugin.isForced(p, strings)) {
            p.sendRawMessage(ChatColor.RED + "You have to be fishing level " + ChatColor.AQUA + reward.getLevel() + ChatColor.RED + " to use this ability");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        Reward reward2 = plugin.getSkillManager().getPlayerRewards(p).getReward("Fishing", "AutoTrashII");
        if (reward2 == null) {
            Bukkit.getLogger().warning("AutoTrashII reward not found for player " + p.getName());
            p.sendRawMessage(ChatColor.RED + "An error occurred while trying to use this ability");
            return true;
        }
        if (reward2.isEnabled() && reward2.isApplied()) big = true;

        if (!plugin.getFishingListener().getTrashInventories().containsKey(p)) {
            AutoTrash trash = new AutoTrash(big);
            plugin.getFishingListener().addTrashInventory(p, trash);
        }

        AutoTrash trash = plugin.getFishingListener().getTrashInventories().get(p);
        plugin.getFishingListener().getOpenTrashInventories().add(p);
        trash.openTrashInventory(p);
        return true;
    }
}
