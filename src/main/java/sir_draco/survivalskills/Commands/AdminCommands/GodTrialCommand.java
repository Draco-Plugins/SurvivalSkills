package sir_draco.survivalskills.Commands.AdminCommands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import sir_draco.survivalskills.GodQuestline.RelativeBlock;
import sir_draco.survivalskills.GodQuestline.Trial;
import sir_draco.survivalskills.GodQuestline.TrialManager;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.Utils.TrialUtils;

import java.io.File;
import java.util.ArrayList;

@SuppressWarnings("NullableProblems")
public class GodTrialCommand implements CommandExecutor {

    private FileConfiguration config = null;

    public GodTrialCommand(SurvivalSkills plugin) {
        PluginCommand command = plugin.getCommand("godtrial");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player p)) return false;

        if (p.hasPermission("survivalskills.op") && strings.length >= 1) {
            if (strings[0].equalsIgnoreCase("end")) {
                if (strings.length < 2) {
                    p.sendRawMessage(ChatColor.RED + "Usage: /godtrial end <player>");
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                    return true;
                }

                // Find the player
                Player target = null;
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!player.getName().equalsIgnoreCase(strings[1])) continue;
                    target = player;
                    break;
                }

                if (target == null) {
                    p.sendRawMessage(ChatColor.RED + "Player not found");
                    p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                    return false;
                }

                for (Trial trial : TrialManager.getTrials()) {
                    if (!trial.getPlayer().equals(target)) continue;
                    trial.endTrial();
                    p.sendRawMessage(ChatColor.GREEN + "Ended trial for " + target.getName());
                    p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    return true;
                }

                p.sendRawMessage(ChatColor.RED + "Player is not in a trial");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
        }

        if (config == null) {
            File file = new File(SurvivalSkills.getInstance().getDataFolder(), "trialbuilding.yml");
            if (!file.exists()) {
                p.sendRawMessage(ChatColor.RED + "No trial building saved in trialbuilding.yml");
                p.sendRawMessage(ChatColor.YELLOW + "Admins need to use /storetrialbuilding to save a trial building");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
            config = YamlConfiguration.loadConfiguration(file);
        }

        Location pLocation = p.getLocation().getBlock().getLocation();

        // Check if the player has an active god quest

        // Check if they have unlocked the god trial

        // Check if the player has an empty 50x50x30 area around them
        for (int i = -25; i <= 25; i++) {
            for (int j = 0; j <= 30; j++) {
                for (int k = -25; k <= 25; k++) {
                    if (!pLocation.clone().add(i, j, k).getBlock().getType().isAir()) {
                        p.sendRawMessage(ChatColor.RED + "You do not have enough space to start the trial");
                        p.sendRawMessage(ChatColor.YELLOW + "Stand in the middle of an empty 50x50x30 area");
                        p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                        return true;
                    }
                }
            }
        }

        // Load the trial building
        ArrayList<RelativeBlock> blocks = TrialUtils.loadTrialBuilding(config);

        // Create bounding box around the trial building
        BoundingBox box = new BoundingBox();
        box.resize(pLocation.getX() - 25, pLocation.getY() - 1, pLocation.getZ() - 25,
                pLocation.getX() + 25, pLocation.getY() + 30, pLocation.getZ() + 25);

        // Create the Trial
        Trial trial = new Trial(blocks, p, box, pLocation);
        trial.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);
        TrialManager.getTrials().add(trial);
        return true;
    }
}
