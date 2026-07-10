package sir_draco.survivalskills.commands.skill_commands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.utils.LocationUtils;


public class UpCommand implements CommandExecutor {

    public static final String UP_COMMAND = "UpCommand";
    private final SurvivalSkills plugin;

    public UpCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("ssup");
        if (command != null)
            command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player p))
            return false;
        // Check for level requirements
        if (!plugin.getSkillManager().getDefaultPlayerRewards().getReward(SkillCategory.MINING, UP_COMMAND).isEnabled()) {
            p.sendRawMessage(ChatColor.RED + "/ssup is not enabled on this server");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return false;
        }

        if (!plugin.getSkillManager().getPlayerRewards(p).getReward(SkillCategory.MINING, UP_COMMAND).isApplied()
                && !plugin.isForced(p, strings)) {
            if (p.hasPermission("survivalskills.op")) {
                p.sendRawMessage(ChatColor.RED + "To force /ssup use: " + ChatColor.AQUA + "/ssup force");
            }
            p.sendRawMessage(ChatColor.GREEN + "You need to be mining level " + ChatColor.AQUA +
                    plugin.getSkillManager().getDefaultPlayerRewards().getReward(SkillCategory.MINING, UP_COMMAND).getLevel()
                    + ChatColor.GREEN + " to use /ssup");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        // The player can't be in the nether or the end
        if (!p.getWorld().getEnvironment().equals(World.Environment.NORMAL)) {
            p.sendRawMessage(ChatColor.RED + "You can only use /ssup in the Overworld");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        // Teleport the player to the surface
        p.teleport(LocationUtils.getSurfaceBlock(p.getLocation()));
        p.playSound(p, Sound.ENTITY_PLAYER_TELEPORT, 1, 1);
        p.sendRawMessage(ChatColor.YELLOW + "Teleported to the surface");
        return true;
    }
}
