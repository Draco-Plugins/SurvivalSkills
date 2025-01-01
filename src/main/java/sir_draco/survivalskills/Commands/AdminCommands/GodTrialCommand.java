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
import sir_draco.survivalskills.GodQuestline.ProtectedArea;
import sir_draco.survivalskills.GodQuestline.RelativeBlock;
import sir_draco.survivalskills.GodQuestline.Trial;
import sir_draco.survivalskills.GodQuestline.TrialManager;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.Utils.TrialUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

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

        if (!p.isOp()) {
            p.sendRawMessage(ChatColor.RED + "Draco is not ready for you to use this command");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return false;
        }

        if (strings.length == 1) {
            if (strings[0].equalsIgnoreCase("end")) {
                for (Trial trial : TrialManager.getTrials()) {
                    if (!trial.getPlayer().equals(p)) continue;
                    trial.endTrial();
                    p.sendRawMessage(ChatColor.GREEN + "Ended trial");
                    p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    return true;
                }

                p.sendRawMessage(ChatColor.RED + "You are not in an active trial");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
            else if (strings[0].equalsIgnoreCase("delete")) {
                for (Trial trial : TrialManager.getTrials()) {
                    if (!trial.getPlayer().equals(p)) continue;
                    trial.deleteTrial();
                    p.sendRawMessage(ChatColor.GREEN + "Your trial has been deleted");
                    p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    return true;
                }

                for (Map.Entry<UUID, ProtectedArea> protectedArea : TrialManager.getProtectedAreas().entrySet()) {
                    if (!protectedArea.getKey().equals(p.getUniqueId())) continue;
                    TrialUtils.removeProtectedArea(protectedArea.getValue());
                    TrialUtils.removeSavedProtectedArea(p.getUniqueId());
                    TrialManager.getProtectedAreas().remove(protectedArea.getKey());
                    p.sendRawMessage(ChatColor.GREEN + "Your trial building has been deleted");
                    p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    return true;
                }

                p.sendRawMessage(ChatColor.RED + "No active trials or trial buildings found");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
            else if (strings[0].equalsIgnoreCase("restart")) {
                for (Trial trial : TrialManager.getTrials()) {
                    if (!trial.getPlayer().equals(p)) continue;
                    trial.restartTrial();
                    p.sendRawMessage(ChatColor.GREEN + "Your trial has been restarted");
                    p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    return true;
                }

                p.sendRawMessage(ChatColor.RED + "No active trial to restart");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
        }
        else if (strings.length >= 2 && p.hasPermission("survivalskills.op")) {
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

            if (strings[0].equalsIgnoreCase("end")) {
                for (Trial trial : TrialManager.getTrials()) {
                    if (!trial.getPlayer().equals(target)) continue;
                    trial.endTrial();
                    target.sendRawMessage(ChatColor.GREEN + "Your trial has been ended by: " + p.getName());
                    p.sendRawMessage(ChatColor.GREEN + "Ended trial for " + target.getName());
                    p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    return true;
                }

                p.sendRawMessage(ChatColor.RED + "Player is not in a trial");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
            else if (strings[0].equalsIgnoreCase("delete")) {
                for (Trial trial : TrialManager.getTrials()) {
                    if (!trial.getPlayer().equals(target)) continue;
                    trial.deleteTrial();
                    TrialUtils.removeSavedProtectedArea(p.getUniqueId());
                    target.sendRawMessage(ChatColor.GREEN + "Your trial has been deleted by: " + p.getName());
                    p.sendRawMessage(ChatColor.GREEN + "Deleted trial for " + target.getName());
                    p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    return true;
                }

                p.sendRawMessage(ChatColor.RED + "Player is not in a trial");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
            else if (strings[0].equalsIgnoreCase("restart")) {
                for (Trial trial : TrialManager.getTrials()) {
                    if (!trial.getPlayer().equals(target)) continue;
                    trial.restartTrial();
                    target.sendRawMessage(ChatColor.GREEN + "Your trial has been restarted by: " + p.getName());
                    p.sendRawMessage(ChatColor.GREEN + "Restarted trial for " + target.getName());
                    p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    return true;
                }

                p.sendRawMessage(ChatColor.RED + "No active trial to restart");
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

        Location pLocation = p.getLocation().clone().add(0, 1, 0).getBlock().getLocation();

        // Check if the player has an active god quest

        // Check if they have unlocked the god trial

        // Check if the player has a pre-existing structure
        if (TrialManager.getProtectedAreas().containsKey(p.getUniqueId())) {
            // Get the center block of the trial building from the protected area
            ProtectedArea area = TrialManager.getProtectedAreas().get(p.getUniqueId());
            if (!p.getWorld().equals(area.world())) {
                p.sendRawMessage(ChatColor.RED + "You are in the wrong world to start the trial");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }

            BoundingBox box = area.boundingBox();
            Location centerLocation = new Location(pLocation.getWorld(), box.getCenterX(), box.getMinY() + 1, box.getCenterZ());

            // Create the Trial
            Trial trial = new Trial(p, area, centerLocation);
            TrialManager.getTrials().add(trial);
            trial.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);
            return true;
        }

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

        // Check if there are any existing claims nearby

        // Load the trial building
        ArrayList<RelativeBlock> blocks = TrialUtils.loadTrialBuilding(config);

        // Create bounding box around the trial building
        BoundingBox box = new BoundingBox();
        box.resize(pLocation.getX() - 26, pLocation.getY() - 1, pLocation.getZ() - 26,
                pLocation.getX() + 26, pLocation.getY() + 29, pLocation.getZ() + 26);
        ProtectedArea protectedArea = new ProtectedArea(box, pLocation.getWorld());

        TrialManager.getProtectedAreas().put(p.getUniqueId(), protectedArea);

        // Create the Trial
        Trial trial = new Trial(blocks, p, protectedArea, pLocation);
        TrialManager.getTrials().add(trial);
        trial.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);
        return true;
    }
}
