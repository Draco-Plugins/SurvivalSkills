package sir_draco.survivalskills.commands.skill_commands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.abilities.AutoTrash;
import sir_draco.survivalskills.rewards.Reward;
import sir_draco.survivalskills.skills.SkillCategory;
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
        if (!(sender instanceof Player p)) return false;

        boolean big = false;
        Reward reward = plugin.getSkillManager().getPlayerRewards(p).getReward(SkillCategory.FISHING, "PermaTrashI");
        if ((!reward.isEnabled() || !reward.isApplied()) && !plugin.isForced(p, strings)) {
            p.sendRawMessage(ChatColor.RED + "You have to be fishing level " + ChatColor.AQUA + reward.getLevel() + ChatColor.RED + " to use this ability");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        Reward bigReward = plugin.getSkillManager().getPlayerRewards(p).getReward(SkillCategory.FISHING, "PermaTrashII");
        if (bigReward.isEnabled() && bigReward.isApplied()) big = true;

        AutoTrash trash;
        if (!plugin.getFishingListener().getPermaTrash().containsKey(p)) {
            trash = new AutoTrash(big, true);
            plugin.getFishingListener().getPermaTrash().put(p, trash);
        }
        else {
            trash = plugin.getFishingListener().getPermaTrash().get(p);
            if (!trash.isBig() && big) trash.upgradeTrashSize();
        }

        plugin.getFishingListener().markTrashInventoryOpen(p);
        trash.openTrashInventory(p);
        return true;
    }
}
