package sir_draco.survivalskills.Commands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.Abilities.TrailEffect;
import sir_draco.survivalskills.Rewards.Reward;
import sir_draco.survivalskills.SurvivalSkills;

public class ToggleTrailCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public ToggleTrailCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("toggletrail");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;

        if (strings.length > 0) {
            // Get trail
            String trail = null;
            for (String str : plugin.getTrails().keySet()) {
                if (str.equalsIgnoreCase(strings[0])) {
                    trail = str;
                    break;
                }
            }

            if (trail == null) {
                p.sendRawMessage(ChatColor.RED + "Invalid trail!");
                p.sendRawMessage(ChatColor.YELLOW + "Dust, Water, Happy, Dragon, Electric, Enchantment, Ominous, " +
                        "Love, Flame, BlueFlame, Cherry, Rainbow");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }

            // Check if they have unlocked it
            String rewardName = trail + "Trail";
            Reward reward = plugin.getPlayerRewards(p).getReward("Main", rewardName);
            if (reward == null) {
                p.sendRawMessage(ChatColor.RED + "You have not unlocked this trail!");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
            else if (!reward.isEnabled()) {
                p.sendRawMessage(ChatColor.RED + "This trail is not enabled!");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
            else if (!reward.isApplied() && !plugin.isForced(p, strings)) {
                p.sendRawMessage(ChatColor.RED + "You have not unlocked this trail!");
                p.sendRawMessage(ChatColor.YELLOW + "It unlocks at Main level: " + ChatColor.AQUA + reward.getLevel());
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }

            // Remove previous trail
            if (plugin.getTrailTracker().containsKey(p)) {
                plugin.getTrailTracker().get(p).cancel();
                plugin.getTrailTracker().remove(p);
            }

            int dustType = 1;
            if (trail.equalsIgnoreCase("Dust")) dustType = 2;
            else if (trail.equalsIgnoreCase("Rainbow")) dustType = 3;

            TrailEffect effect = new TrailEffect(p, plugin.getTrails().get(trail), dustType, trail);
            effect.runTaskTimer(plugin, 0, 1);
            plugin.getTrailTracker().put(p, effect);
            p.sendRawMessage(ChatColor.GREEN + "Trail enabled!");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            return true;
        }

        // They input the command wrong
        if (!plugin.getTrailTracker().containsKey(p)) {
            p.sendRawMessage(ChatColor.GREEN + "Please specifiy a trail!");
            p.sendRawMessage(ChatColor.YELLOW + "Dust, Water, Happy, Dragon, Electric, Enchantment, Ominous, Love, " +
                    "Flame, BlueFlame, Cherry, Rainbow");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        // Remove existing trail
        plugin.getTrailTracker().get(p).cancel();
        plugin.getTrailTracker().remove(p);
        p.sendRawMessage(ChatColor.GREEN + "Trail disabled!");
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        return true;
    }
}
