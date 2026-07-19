package sir_draco.survivalskills.commands.skill_commands;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import sir_draco.survivalskills.abilities.BloodyDomain;
import sir_draco.survivalskills.god_questline.trial.PendingTrial;
import sir_draco.survivalskills.god_questline.trial.ProtectedArea;
import sir_draco.survivalskills.god_questline.trial.Trial;
import sir_draco.survivalskills.god_questline.trial.TrialManager;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.TrialUtils;
import sir_draco.survivalskills.utils.Utils;

import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;


public class GodTrialCommand implements CommandExecutor {

    private static final String OP_PERMISSION = "survivalskills.op";
    private static final Sound SUCCESS_SOUND = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
    private static final Sound ERROR_SOUND = Sound.ENTITY_ENDERMAN_TELEPORT;

    private final SurvivalSkills plugin;

    public GodTrialCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("godtrial");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player p)) return false;

        // No subcommand: start a fresh trial for the sender.
        if (strings.length == 0) return startTrial(p);

        String sub = strings[0].toLowerCase(Locale.ROOT);

        // Spectate is the only subcommand available regardless of op permission.
        if (sub.equals("spectate")) {
            if (strings.length >= 2) return handleSpectate(p, strings);
            return handleSpectateToggle(p);
        }

        // Single-arg subcommands act on the sender themselves.
        if (strings.length == 1) {
            return switch (sub) {
                case "end" -> handleEndSelf(p);
                case "delete" -> handleDeleteSelf(p);
                case "restart" -> handleRestartSelf(p);
                case "quit" -> handleQuit(p);
                default -> startTrial(p);
            };
        }

        // Two-or-more arg subcommands are op-only and act on a target player.
        if (!p.hasPermission(OP_PERMISSION)) return startTrial(p);

        // Resolve the target player once for all op subcommands: the original
        // performed this lookup before dispatching on the subcommand, so a
        // missing target fails here (returning false) before any subcommand
        // logic runs.
        Optional<Player> targetOpt = findOpTarget(p, strings);
        if (targetOpt.isEmpty()) return false;
        Player target = targetOpt.get();

        switch (sub) {
            case "end" -> { return handleEndOther(p, target); }
            case "delete" -> { return handleDeleteOther(p, target); }
            case "restart" -> { return handleRestartOther(p, target); }
            case "nextwave" -> { return handleNextWaveOther(p, target); }
            case "setwave" -> {
                if (strings.length >= 3) return handleSetWaveOther(p, target, strings);
            }
            case "delink" -> { return handleDelinkOther(p, target); }
            // default and setwave-with-too-few-args fall through to startTrial
        }
        return startTrial(p);
    }

    // ---------------------------------------------------------------------
    // Self subcommand handlers
    // ---------------------------------------------------------------------

    private boolean handleSpectateToggle(Player p) {
        // No target specified: if the player is already spectating, stop it.
        if (TrialManager.isSpectating(p)) {
            TrialUtils.removeTrialSpectator(p, null);
        }
        return true;
    }

    private boolean handleSpectate(Player p, String[] strings) {
        // A player currently in a trial cannot spectate another.
        for (Trial trial : TrialManager.getTrials()) {
            if (trial.getPlayers().contains(p) && !TrialManager.isSpectating(p)) {
                sendError(p, "You cannot spectate while in a trial");
                return true;
            }
        }

        Optional<Player> targetOpt = Utils.findPlayer(strings[1]);
        if (targetOpt.isEmpty()) {
            p.sendRawMessage(ChatColor.RED + "Player not found");
            p.sendRawMessage(ChatColor.YELLOW + "Proper usage: /godtrial spectate <player>");
            p.playSound(p, ERROR_SOUND, 1, 1);
            return false;
        }
        Player target = targetOpt.get();

        // Already spectating: switch to (or stop spectating) the new target.
        if (TrialManager.isSpectating(p)) {
            TrialUtils.removeTrialSpectator(p, target);
            return true;
        }

        if (TrialUtils.addTrialSpectator(p, target)) return true;

        sendError(p, "Player: " + target.getName() + " is not in a trial");
        return true;
    }

    private boolean handleEndSelf(Player p) {
        Optional<Trial> trialOpt = findTrialForPlayer(p);
        if (trialOpt.isEmpty()) {
            sendError(p, "You are not in an active trial");
            return true;
        }

        Trial trial = trialOpt.get();
        if (!isTrialMaster(trial, p, "end")) return true;

        trial.endTrialAndTeleportPlayers();
        notifyAllTrialPlayers(trial, p, "Trial ended");
        return true;
    }

    private boolean handleRestartSelf(Player p) {
        Optional<Trial> trialOpt = findTrialForPlayer(p);
        if (trialOpt.isEmpty()) {
            sendError(p, "No active trial to restart");
            return true;
        }

        Trial trial = trialOpt.get();
        if (!isTrialMaster(trial, p, "restart")) return true;

        trial.restartTrial();
        notifyAllTrialPlayers(trial, p, "Trial restarted");
        return true;
    }

    private boolean handleDeleteSelf(Player p) {
        Optional<Trial> trialOpt = findTrialForPlayer(p);
        if (trialOpt.isPresent()) {
            Trial trial = trialOpt.get();
            if (!isTrialMaster(trial, p, "delete")) return true;
            trial.deleteTrial();
            sendSuccess(p, "Your trial has been deleted");
            return true;
        }

        // No active trial: look for a protected trial building to remove.
        ProtectedArea area = TrialManager.getProtectedArea(p.getUniqueId());
        if (area != null) {
            TrialUtils.removeProtectedArea(area);
            TrialUtils.removeSavedProtectedArea(p.getUniqueId());
            TrialManager.removeProtectedArea(p.getUniqueId());
            sendSuccess(p, "Your trial building has been deleted");
            return true;
        }

        TrialManager.removeTrialBuilding(p.getUniqueId());
        sendError(p, "No active trials or trial buildings found");
        return true;
    }

    private boolean handleQuit(Player p) {
        // Quitting an active trial.
        Optional<Trial> trialOpt = findTrialForPlayer(p);
        if (trialOpt.isPresent()) {
            trialOpt.get().quitTrial(p);
            return true;
        }

        // Quitting a pending (not-yet-started) trial.
        for (PendingTrial trial : TrialManager.getPendingTrials().values()) {
            if (trial.getTrialMaster().equals(p)) {
                trial.getPlayers().forEach(player -> {
                    player.sendRawMessage(ChatColor.GREEN + "Trial master has quit the trial");
                    player.playSound(player, ERROR_SOUND, 1, 1);
                    player.closeInventory();
                });
                trial.clearPlayers();
                TrialManager.removePendingTrial(p);
                sendSuccess(p, "You have quit the trial");
                return true;
            }

            if (!trial.getPlayers().contains(p)) continue;
            trial.removePlayer(p);
            sendSuccess(p, "You have quit the trial");
            return true;
        }

        sendError(p, "You are not in an active trial and have no trials pending");
        return true;
    }

    // ---------------------------------------------------------------------
    // Op (target-player) subcommand handlers
    // ---------------------------------------------------------------------

    private boolean handleEndOther(Player p, Player target) {
        Optional<Trial> trialOpt = findTrialForPlayer(target);
        if (trialOpt.isEmpty()) {
            sendError(p, "Player is not in a trial");
            return true;
        }

        trialOpt.get().endTrialAndTeleportPlayers();
        target.sendRawMessage(ChatColor.GREEN + "Your trial has been ended by: " + p.getName());
        sendSuccess(p, "Ended trial for " + target.getName());
        return true;
    }

    private boolean handleRestartOther(Player p, Player target) {
        Optional<Trial> trialOpt = findTrialForPlayer(target);
        if (trialOpt.isEmpty()) {
            sendError(p, "No active trial to restart");
            return true;
        }

        trialOpt.get().restartTrial();
        target.sendRawMessage(ChatColor.GREEN + "Your trial has been restarted by: " + p.getName());
        sendSuccess(p, "Restarted trial for " + target.getName());
        return true;
    }

    private boolean handleDeleteOther(Player p, Player target) {
        Optional<Trial> trialOpt = findTrialForPlayer(target);
        if (trialOpt.isEmpty()) {
            sendError(p, "Player is not in a trial");
            return true;
        }

        trialOpt.get().deleteTrial();
        TrialUtils.removeSavedProtectedArea(p.getUniqueId());
        target.sendRawMessage(ChatColor.GREEN + "Your trial has been deleted by: " + p.getName());
        sendSuccess(p, "Deleted trial for " + target.getName());
        return true;
    }

    private boolean handleNextWaveOther(Player p, Player target) {
        Optional<Trial> trialOpt = findTrialForPlayer(target);
        if (trialOpt.isEmpty()) {
            sendError(p, "No active trial to start next wave for");
            return true;
        }

        trialOpt.get().endWave();
        target.sendRawMessage(ChatColor.GREEN + "Next wave initiated by: " + p.getName());
        sendSuccess(p, "Next wave initiated for: " + target.getName());
        return true;
    }

    private boolean handleSetWaveOther(Player p, Player target, String[] strings) {
        int waveNumber;
        try {
            waveNumber = Integer.parseInt(strings[2]);
        } catch (NumberFormatException e) {
            sendError(p, "Invalid wave number");
            return false;
        }

        Optional<Trial> trialOpt = findTrialForPlayer(target);
        if (trialOpt.isEmpty()) {
            sendError(p, "No active trial to set the wave of");
            return true;
        }

        Trial trial = trialOpt.get();
        trial.endWave();
        trial.setWave(waveNumber);
        target.sendRawMessage(ChatColor.GREEN + "Your wave has been set to wave " + ChatColor.AQUA +
                waveNumber + ChatColor.GREEN + "by: " + p.getName());
        p.sendRawMessage(ChatColor.GREEN + "Wave has been set to wave " + ChatColor.AQUA +
                waveNumber + ChatColor.GREEN + "for: " + target.getName());
        p.playSound(p, SUCCESS_SOUND, 1, 1);
        return true;
    }

    private boolean handleDelinkOther(Player p, Player target) {
        Optional<Trial> trialOpt = findTrialForPlayer(target);
        if (trialOpt.isPresent()) {
            trialOpt.get().endTrial();
            TrialUtils.removeSavedProtectedArea(p.getUniqueId());
            target.sendRawMessage(ChatColor.GREEN + "Your trial has been delinked by: " + p.getName());
            sendSuccess(p, "Delinked trial for " + target.getName());
            return true;
        }

        // No active trial: look for a protected trial building to delink.
        ProtectedArea area = TrialManager.getProtectedArea(target.getUniqueId());
        if (area != null) {
            TrialUtils.removeSavedProtectedArea(target.getUniqueId());
            TrialManager.removeProtectedArea(target.getUniqueId());
            target.sendRawMessage(ChatColor.GREEN + "Your trial building has been delinked by: " + p.getName());
            sendSuccess(p, "Delinked trial building for " + target.getName());
            return true;
        }

        sendError(p, "Player is not in a trial and does not have a trial building");
        return true;
    }

    // ---------------------------------------------------------------------
    // Starting a trial (fall-through path)
    // ---------------------------------------------------------------------

    private boolean startTrial(Player p) {
        if (TrialManager.getTrialBuildingConfig() == null) {
            File file = new File(plugin.getDataFolder(), "trialbuilding.yml");
            if (!file.exists()) {
                sendErrorWithHint(p,
                        "No trial building saved in trialbuilding.yml",
                        "Admins need to use /storetrialbuilding to save a trial building");
                return true;
            }
            TrialManager.setTrialBuildingConfig(YamlConfiguration.loadConfiguration(file));
        }

        // Check if a player is in spawn (sendMessage, not sendRawMessage: preserved from original).
        Location pLocation = p.getLocation();
        if (plugin.getWorldGuardProvider() != null &&
                plugin.getWorldGuardProvider().locationInRegion(pLocation.getX(), pLocation.getY(), pLocation.getZ())) {
            p.sendMessage(ChatColor.RED + "You cannot start a trial in spawn");
            p.playSound(p, ERROR_SOUND, 1, 1);
            return true;
        }

        // Check if the player is already in a trial.
        if (findTrialForPlayer(p).isPresent()) {
            sendError(p, "You are already in a trial");
            return true;
        }

        // Check if the player's inventory is empty.
        if (checkInventory(p)) return true;

        // Set the player's gamemode to survival.
        if (p.getGameMode().equals(GameMode.CREATIVE)) p.setGameMode(GameMode.SURVIVAL);

        // Strip potion effects from the player.
        p.getActivePotionEffects().forEach(effect -> p.removePotionEffect(effect.getType()));

        // Disable peaceful miner and bloody domain if active.
        disableSkills(p);

        // Open the GUI to select the trial.
        TrialUtils.openTrialTypeSelection(p);
        return true;
    }

    // ---------------------------------------------------------------------
    // Helpers: lookup, messaging, inventory, skills
    // ---------------------------------------------------------------------

    /** Finds the active trial containing the given player, if any. */
    private Optional<Trial> findTrialForPlayer(Player target) {
        return TrialManager.getTrials().stream()
                .filter(t -> t.getPlayers().contains(target))
                .findFirst();
    }

    /** Returns true if {@code p} owns the trial, otherwise sends an error and returns false. */
    private boolean isTrialMaster(Trial trial, Player p, String action) {
        if (trial.getTrialMaster().equals(p)) return true;
        sendError(p, "Only the trial master can " + action + " the trial");
        return false;
    }

    /** Notifies every player in the trial, playing the success sound at the sender's position. */
    private void notifyAllTrialPlayers(Trial trial, Player sender, String message) {
        for (Player trialPlayer : trial.getPlayers()) {
            trialPlayer.sendRawMessage(ChatColor.GREEN + message);
            trialPlayer.playSound(sender, SUCCESS_SOUND, 1, 1);
        }
    }

    /**
     * Resolves the target player named in {@code strings[1]} for op subcommands.
     * Sends the "Player not found" error and returns an empty Optional on failure.
     */
    private Optional<Player> findOpTarget(Player admin, String[] strings) {
        Optional<Player> target = Utils.findPlayer(strings[1]);
        if (target.isEmpty()) sendError(admin, "Player not found");
        return target;
    }

    private void sendError(Player p, String message) {
        p.sendRawMessage(ChatColor.RED + message);
        p.playSound(p, ERROR_SOUND, 1, 1);
    }

    private void sendSuccess(Player p, String message) {
        p.sendRawMessage(ChatColor.GREEN + message);
        p.playSound(p, SUCCESS_SOUND, 1, 1);
    }

    private void sendErrorWithHint(Player p, String error, String hint) {
        p.sendRawMessage(ChatColor.RED + error);
        p.sendRawMessage(ChatColor.YELLOW + hint);
        p.playSound(p, ERROR_SOUND, 1, 1);
    }

    private boolean itemsInInventory(Player p) {
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

    private boolean checkInventory(Player p) {
        if (itemsInInventory(p)) {
            sendErrorWithHint(p,
                    "Your inventory is not empty",
                    "Clear your inventory before starting the trial");
            return true;
        }
        return false;
    }

    private void disableSkills(Player p) {
        plugin.getMiningListener().getPeacefulMiners().remove(p);
        Map<Player, BloodyDomain> bloodyDomainTracker = plugin.getAbilityManager().getBloodyDomainTracker();
        if (bloodyDomainTracker.containsKey(p)) {
            bloodyDomainTracker.get(p).cancel();
            bloodyDomainTracker.remove(p);
        }
        // Disable autotrash and permatrash
        plugin.getFishingListener().getDisabledAutoTrash().add(p);
    }
}
