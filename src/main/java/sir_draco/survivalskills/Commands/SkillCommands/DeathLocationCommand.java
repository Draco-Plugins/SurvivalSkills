package sir_draco.survivalskills.Commands.SkillCommands;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.SurvivalSkills;

public class DeathLocationCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public DeathLocationCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("deathlocation");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;

        if (!plugin.getSkillManager().getDefaultPlayerRewards().getReward("Main", "DeathLocationTracker").isEnabled()) {
            p.sendRawMessage(ChatColor.RED + "Death Location Tracking is not enabled on this server");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return false;
        }

        if (!plugin.getSkillManager().getPlayerRewards(p).getReward("Main", "DeathLocationTracker").isApplied() && !plugin.isForced(p, strings)) {
            if (p.hasPermission("survivalskills.op")) {
                p.sendRawMessage(ChatColor.RED + "To force death location tracking use: " + ChatColor.AQUA + "/deathlocation force");
            }
            p.sendRawMessage(ChatColor.RED + "You need to be level " + ChatColor.AQUA
                    + plugin.getSkillManager().getDefaultPlayerRewards().getReward("Main", "DeathLocationTracker").getLevel()
                    + ChatColor.RED + " to use the Death Location Tracker");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        if (!plugin.getMainListener().getDeathLocations().containsKey(p) || plugin.getMainListener().getDeathLocations().get(p).isEmpty()) {
            p.sendRawMessage(ChatColor.RED + "You have not died in the past hour");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        sendDeathLocationInformation(p);
        return true;
    }

    public void sendDeathLocationInformation(Player p) {
        p.sendRawMessage(ChatColor.GRAY + "Death Locations "
                + ChatColor.DARK_GRAY + "(" + plugin.getMainListener().getDeathLocations().get(p).size() + ")" + ChatColor.GRAY + ":");
        p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        for (Location loc : plugin.getMainListener().getDeathLocations().get(p)) {
            p.sendRawMessage(ChatColor.YELLOW + "You died at: " + ChatColor.AQUA + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
            sendArrowParticles(p, loc);
        }
    }

    public void sendArrowParticles(Player p, Location loc) {
        Vector direction = getDeathDirection(p.getLocation().clone().add(0, 2, 0), loc);
        int scale = 5;
        for (int i = 1; i <= 30; i++) {
            double xDir = direction.getX() * ((double) i / scale);
            double zDir = direction.getZ() * ((double) i / scale);
            double yDir = direction.getY() * ((double) i / scale);
            p.spawnParticle(Particle.DUST, p.getLocation().clone().add(xDir, 2 + yDir, zDir),
                    1, new Particle.DustOptions(Color.BLUE, 1));
        }
    }

    public Vector getDeathDirection(Location playerLoc, Location deathLoc) {
        return deathLoc.toVector().subtract(playerLoc.toVector()).normalize();
    }
}
