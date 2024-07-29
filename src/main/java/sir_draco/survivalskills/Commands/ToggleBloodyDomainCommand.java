package sir_draco.survivalskills.Commands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.Abilities.BloodyDomain;
import sir_draco.survivalskills.Rewards.Reward;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.HashMap;

public class ToggleBloodyDomainCommand implements CommandExecutor {

    public ToggleBloodyDomainCommand() {
        PluginCommand command = SurvivalSkills.getInstance().getCommand("togglebloodydomain");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;

        Reward reward = SurvivalSkills.getInstance().getSkillManager().getPlayerRewards(p).getReward("Fighting", "Bloody Domain");
        if (reward == null || !reward.isEnabled()) {
            p.sendRawMessage(ChatColor.RED + "Bloody Domain is not enabled");
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }
        if (!reward.isApplied()) {
            p.sendRawMessage(ChatColor.RED + "You need to be fighting level " + ChatColor.AQUA + reward.getLevel()
                    + ChatColor.RED + " to toggle Bloody Domain");
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        HashMap<Player, BloodyDomain> bloodyDomainTracker = SurvivalSkills.getInstance().getAbilityManager().getBloodyDomainTracker();
        if (bloodyDomainTracker.containsKey(p)) {
            bloodyDomainTracker.get(p).cancel();
            bloodyDomainTracker.remove(p);
            p.sendRawMessage(ChatColor.GREEN + "Bloody Domain has been disabled.");
            p.playSound(p, Sound.BLOCK_NOTE_BLOCK_HARP, 1, 0.5f);
        }
        else {
            BloodyDomain bloodyDomain = new BloodyDomain(p);
            bloodyDomain.runTaskTimer(SurvivalSkills.getInstance(), 0, 20);
            bloodyDomainTracker.put(p, bloodyDomain);
            p.sendRawMessage(ChatColor.GREEN + "Bloody Domain has been enabled.");
            p.playSound(p, Sound.BLOCK_NOTE_BLOCK_HARP, 1, 2);
        }
        return true;
    }
}
