package sir_draco.survivalskills.commands.admin_commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.abilities.AbilityTimer;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.Objects;
import java.util.Optional;

public class SkillsMultiplierCommand implements CommandExecutor {

    private static final String XPVOUCHER = "XPVoucher";
    private static final int DEFAULT_DURATION_SECONDS = 3600;
    private static final Sound SUCCESS_SOUND = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
    private static final Sound ERROR_SOUND = Sound.ENTITY_ENDERMAN_TELEPORT;

    private final SurvivalSkills plugin;

    public SkillsMultiplierCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        Objects.requireNonNull(plugin.getCommand("skillsmultiplier")).setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (strings.length < 2) {
            sendError(sender, "Usage: /skillsmultiplier <player/all> <multiplier> [seconds]");
            return false;
        }

        Optional<Double> multiplier = parseDouble(strings[1]);
        if (multiplier.isEmpty()) {
            sendError(sender, "Invalid multiplier");
            return false;
        }

        if (strings[0].equalsIgnoreCase("all")) {
            applyGlobalMultiplier(sender, multiplier.get());
        } else {
            applyPlayerMultiplier(sender, strings[0], multiplier.get(), strings);
        }

        return true;
    }

    private void applyGlobalMultiplier(CommandSender sender, double multiplier) {
        plugin.getSkillManager().saveGlobalMultiplier(multiplier);
        sendSuccess(sender, "Skills multiplier set to " + multiplier);
    }

    private void applyPlayerMultiplier(CommandSender sender, String targetName, double multiplier, String[] strings) {
        Player target = findPlayer(targetName);
        if (target == null) {
            sendError(sender, "Player not found");
            return;
        }

        removeExistingMultiplier(target);

        int durationSeconds = DEFAULT_DURATION_SECONDS;
        if (strings.length >= 3) {
            Optional<Integer> seconds = parseInt(strings[2]);
            if (seconds.isEmpty()) {
                sendError(sender, "Invalid time");
                return;
            }
            durationSeconds = seconds.get();
        }

        int minutes = durationSeconds / 60;

        plugin.getSkillManager().setPlayerMultiplier(target, multiplier);
        AbilityTimer newTimer = new AbilityTimer(plugin, XPVOUCHER, target, durationSeconds, 0);
        newTimer.runTaskTimerAsynchronously(plugin, 0, 20);
        plugin.getAbilityManager().addAbility(target, newTimer);

        target.sendRawMessage(ChatColor.GREEN + "Skills multiplier set to " + ChatColor.AQUA + multiplier
                + ChatColor.GREEN + " for " + ChatColor.AQUA + minutes + ChatColor.GREEN + " minutes");
        sendSuccess(sender, "Skills multiplier set to " + multiplier + " for " + target.getName()
                + " for " + minutes + " minutes");
    }

    private void removeExistingMultiplier(Player target) {
        AbilityTimer timer = plugin.getAbilityManager().getAbility(target, XPVOUCHER);
        if (timer != null) {
            timer.cancel();
            plugin.getAbilityManager().removeAbility(target, XPVOUCHER);
        }
    }

    private static Player findPlayer(String name) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private static Optional<Double> parseDouble(String arg) {
        try {
            return Optional.of(Double.parseDouble(arg));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<Integer> parseInt(String arg) {
        try {
            return Optional.of(Integer.parseInt(arg));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private void sendSuccess(CommandSender sender, String message) {
        if (sender instanceof Player p) {
            p.sendRawMessage(ChatColor.GREEN + message);
            p.playSound(p, SUCCESS_SOUND, 1, 1);
        }
    }

    private void sendError(CommandSender sender, String message) {
        if (sender instanceof Player p) {
            p.sendRawMessage(ChatColor.RED + message);
            p.playSound(p, ERROR_SOUND, 1, 1);
        }
    }
}
