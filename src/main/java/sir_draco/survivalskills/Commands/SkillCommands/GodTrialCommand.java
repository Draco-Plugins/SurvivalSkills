package sir_draco.survivalskills.Commands.SkillCommands;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import sir_draco.survivalskills.Abilities.BloodyDomain;
import sir_draco.survivalskills.GodQuestline.*;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.Utils.TrialUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("NullableProblems")
public class GodTrialCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public GodTrialCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("godtrial");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player p)) return false;

        if (strings.length >= 2 && strings[0].equalsIgnoreCase("spectate")) {
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

            if (TrialUtils.removeTrialSpectator(p, target)) return true;
            if (TrialUtils.addTrialSpectator(p, target)) return true;

            p.sendRawMessage(ChatColor.RED + "Player is not in a trial");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        if (strings.length == 1) {
            if (strings[0].equalsIgnoreCase("end")) {
                for (Trial trial : TrialManager.getTrials()) {
                    if (!trial.getPlayers().contains(p)) continue;
                    if (!trial.getTrialMaster().equals(p)) {
                        p.sendRawMessage(ChatColor.RED + "Only the trial master can end the trial");
                        p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                        return true;
                    }
                    trial.endTrial();
                    for (Player trialPlayer : trial.getPlayers()) {
                        trialPlayer.sendRawMessage(ChatColor.GREEN + "Trial ended");
                        trialPlayer.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    }
                    return true;
                }

                p.sendRawMessage(ChatColor.RED + "You are not in an active trial");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
            else if (strings[0].equalsIgnoreCase("delete")) {
                for (Trial trial : TrialManager.getTrials()) {
                    if (!trial.getPlayers().contains(p)) continue;
                    if (!trial.getTrialMaster().equals(p)) {
                        p.sendRawMessage(ChatColor.RED + "Only the trial master can delete the trial");
                        p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                        return true;
                    }
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

                // Remove the trial building from the TrialManager
                TrialManager.removeTrialBuilding(p.getUniqueId());

                p.sendRawMessage(ChatColor.RED + "No active trials or trial buildings found");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
            else if (strings[0].equalsIgnoreCase("restart")) {
                for (Trial trial : TrialManager.getTrials()) {
                    if (!trial.getPlayers().contains(p)) continue;
                    if (!trial.getTrialMaster().equals(p)) {
                        p.sendRawMessage(ChatColor.RED + "Only the trial master can restart the trial");
                        p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                        return true;
                    }
                    trial.restartTrial();
                    for (Player trialPlayer : trial.getPlayers()) {
                        trialPlayer.sendRawMessage(ChatColor.GREEN + "Trial restarted");
                        trialPlayer.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    }
                    return true;
                }

                p.sendRawMessage(ChatColor.RED + "No active trial to restart");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
            else if (strings[0].equalsIgnoreCase("quit")) {
                for (Trial trial : TrialManager.getTrials()) {
                    if (!trial.getPlayers().contains(p)) continue;
                    trial.quitTrial(p);
                    return true;
                }

                for (PendingTrial trial : TrialManager.getPendingTrials().values()) {
                    if (trial.getTrialMaster().equals(p)) {
                        trial.getPlayers().forEach(player -> {
                            player.sendRawMessage(ChatColor.GREEN + "Trial master has quit the trial");
                            player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                            player.closeInventory();
                        });
                        trial.getPlayers().clear();
                        TrialManager.getPendingTrials().remove(p);
                        p.sendRawMessage(ChatColor.GREEN + "You have quit the trial");
                        p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                        return true;
                    }

                    if (!trial.getPlayers().contains(p)) continue;
                    trial.getPlayers().remove(p);
                    trial.updatePlayerManager();
                    p.sendRawMessage(ChatColor.GREEN + "You have quit the trial");
                    p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    return true;
                }

                p.sendRawMessage(ChatColor.RED + "You are not in an active trial and have no trials pending");
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
                    if (!trial.getPlayers().contains(target)) continue;
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
                    if (!trial.getPlayers().contains(target)) continue;
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
                    if (!trial.getPlayers().contains(target)) continue;
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
            else if (strings[0].equalsIgnoreCase("nextwave")) {
                for (Trial trial : TrialManager.getTrials()) {
                    if (!trial.getPlayers().contains(target)) continue;
                    trial.endWave();
                    target.sendRawMessage(ChatColor.GREEN + "Next wave initiated by: " + p.getName());
                    p.sendRawMessage(ChatColor.GREEN + "Next wave initiated for: " + target.getName());
                    p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    return true;
                }

                p.sendRawMessage(ChatColor.RED + "No active trial to start next wave for");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
            else if (strings.length >= 3 && strings[0].equalsIgnoreCase("setwave")) {
                int waveNumber;
                try {
                    waveNumber = Integer.parseInt(strings[2]);
                } catch (NumberFormatException e) {
                    p.sendRawMessage(ChatColor.RED + "Invalid wave number");
                    p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                    return false;
                }

                for (Trial trial : TrialManager.getTrials()) {
                    if (!trial.getPlayers().contains(target)) continue;
                    trial.endWave();
                    trial.setWave(waveNumber);
                    target.sendRawMessage(ChatColor.GREEN + "Your wave has been set to wave " + ChatColor.AQUA +
                            waveNumber + ChatColor.GREEN + "by: " + p.getName());
                    p.sendRawMessage(ChatColor.GREEN + "Wave has been set to wave " + ChatColor.AQUA +
                            waveNumber + ChatColor.GREEN + "for: " + target.getName());
                    p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    return true;
                }

                p.sendRawMessage(ChatColor.RED + "No active trial to set the wave of");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
            else if (strings[0].equalsIgnoreCase("delink")) {
                for (Trial trial : TrialManager.getTrials()) {
                    if (!trial.getPlayers().contains(target)) continue;
                    trial.endTrial();
                    TrialUtils.removeSavedProtectedArea(p.getUniqueId());
                    target.sendRawMessage(ChatColor.GREEN + "Your trial has been delinked by: " + p.getName());
                    p.sendRawMessage(ChatColor.GREEN + "Delinked trial for " + target.getName());
                    p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    return true;
                }

                for (Map.Entry<UUID, ProtectedArea> protectedArea : TrialManager.getProtectedAreas().entrySet()) {
                    if (!protectedArea.getKey().equals(target.getUniqueId())) continue;
                    TrialUtils.removeSavedProtectedArea(target.getUniqueId());
                    TrialManager.getProtectedAreas().remove(protectedArea.getKey());
                    target.sendRawMessage(ChatColor.GREEN + "Your trial building has been delinked by: " + p.getName());
                    p.sendRawMessage(ChatColor.GREEN + "Delinked trial building for " + target.getName());
                    p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                    return true;
                }

                p.sendRawMessage(ChatColor.RED + "Player is not in a trial and does not have a trial building");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
        }

        if (TrialManager.getTrialBuildingConfig() == null) {
            File file = new File(SurvivalSkills.getInstance().getDataFolder(), "trialbuilding.yml");
            if (!file.exists()) {
                p.sendRawMessage(ChatColor.RED + "No trial building saved in trialbuilding.yml");
                p.sendRawMessage(ChatColor.YELLOW + "Admins need to use /storetrialbuilding to save a trial building");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
            TrialManager.setTrialBuildingConfig(YamlConfiguration.loadConfiguration(file));
        }

        // Check if a player is already in a trial
        for (Trial trial : TrialManager.getTrials()) {
            if (trial.getPlayers().contains(p)) {
                p.sendRawMessage(ChatColor.RED + "You are already in a trial");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
        }

        // Check if the player's inventory is empty
        if (checkInventory(p)) return true;

        // Set the player's gamemode to survival
        if (p.getGameMode().equals(GameMode.CREATIVE)) p.setGameMode(org.bukkit.GameMode.SURVIVAL);

        // Strip potion effects from the player
        p.getActivePotionEffects().forEach(effect -> p.removePotionEffect(effect.getType()));

        // Disable autotrash and permatrash
        plugin.getFishingListener().getDisabledAutoTrash().add(p);

        // Disable peaceful miner and bloody domain if active
        disableSkills(p);

        // Open the GUI to select the trial
        TrialUtils.openTrialTypeSelection(p);
        return true;
    }

    public boolean itemsInInventory(Player p) {
        for (ItemStack item : p.getInventory().getContents()) {
            if (item != null) return true;
        }

        for (ItemStack item : p.getInventory().getArmorContents()) {
            if (item != null) return true;
        }

        for (ItemStack item : p.getInventory().getExtraContents()) {
            if (item != null) return true;
        }

        return false;
    }

    public boolean checkInventory(Player p) {
        if (itemsInInventory(p)) {
            p.sendRawMessage(ChatColor.RED + "Your inventory is not empty");
            p.sendRawMessage(ChatColor.YELLOW + "Clear your inventory before starting the trial");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }
        return false;
    }

    public void disableSkills(Player p) {
        SurvivalSkills.getInstance().getMiningListener().getPeacefulMiners().remove(p);
        HashMap<Player, BloodyDomain> bloodyDomainTracker = SurvivalSkills.getInstance().getAbilityManager().getBloodyDomainTracker();
        if (bloodyDomainTracker.containsKey(p)) {
            bloodyDomainTracker.get(p).cancel();
            bloodyDomainTracker.remove(p);
        }
    }
}
