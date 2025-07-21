package sir_draco.survivalskills.Commands.DefaultCommands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.Objects;

@SuppressWarnings("NullableProblems")
public class ToggleBossMusic implements CommandExecutor {

    public ToggleBossMusic() {
        Objects.requireNonNull(SurvivalSkills.getInstance().getCommand("togglebossmusic")).setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player p)) return false;

        if (SurvivalSkills.getInstance().getFightingListener().getNoBossMusic().contains(p)) {
            SurvivalSkills.getInstance().getFightingListener().getNoBossMusic().remove(p);
            p.sendRawMessage(ChatColor.GREEN + "Boss music is now enabled.");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        } else {
            SurvivalSkills.getInstance().getFightingListener().getNoBossMusic().add(p);
            p.sendRawMessage(ChatColor.GREEN + "Boss music is now disabled.");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        }

        return true;
    }
}
