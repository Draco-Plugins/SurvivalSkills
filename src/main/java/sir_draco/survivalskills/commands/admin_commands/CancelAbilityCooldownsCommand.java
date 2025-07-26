package sir_draco.survivalskills.commands.admin_commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.abilities.AbilityTimer;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;
import java.util.Map;

@SuppressWarnings("NullableProblems")
public class CancelAbilityCooldownsCommand implements CommandExecutor {

    public CancelAbilityCooldownsCommand(SurvivalSkills plugin) {
        PluginCommand command = plugin.getCommand("cancelabilitycooldowns");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player p)) return false;

        if (strings.length < 1) {
            p.sendRawMessage(ChatColor.RED + "Usage: /cancelabilitycooldowns <player>");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        // Get the player
        Player target = null;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getName().equalsIgnoreCase(strings[0])) continue;
            target = player;
            break;
        }

        if (target == null) {
            p.sendRawMessage(ChatColor.RED + "Player not found");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return false;
        }

        // Cancel the cooldowns
        Map<Player, ArrayList<AbilityTimer>> timerTracker = SurvivalSkills.getInstance().getAbilityManager().getTimerTracker();
        if (!timerTracker.containsKey(target)) {
            p.sendRawMessage(ChatColor.RED + "Player has no active cooldowns");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return false;
        }

        ArrayList<AbilityTimer> timers = new ArrayList<>(timerTracker.get(target));
        for (AbilityTimer timer : timers) {
            timer.endAbility();
            timer.endCooldown();
        }
        p.sendRawMessage(ChatColor.GREEN + "Cancelled all cooldowns for " + target.getName());
        p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        return true;
    }
}
