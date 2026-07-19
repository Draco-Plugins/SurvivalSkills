package sir_draco.survivalskills.commands.admin_commands;

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

    private final SurvivalSkills plugin;

    public SkillsMultiplierCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        Objects.requireNonNull(plugin.getCommand("skillsmultiplier")).setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (strings.length < 2) {
            sendUsage(sender);
            return false;
        }

        if (strings[0].equalsIgnoreCase("all")) {
            Optional<Double> multiplier = parseDouble(strings[1]);
            if (multiplier.isEmpty()) {
                sendError(sender, "Invalid multiplier");
                return false;
            }

            applyGlobalMultiplier(sender, multiplier.get());
            return true;
        }

        boolean explicitPlayerTarget = strings[0].equalsIgnoreCase("player");
        int targetIndex = explicitPlayerTarget ? 1 : 0;
        int multiplierIndex = explicitPlayerTarget ? 2 : 1;
        int durationIndex = explicitPlayerTarget ? 3 : 2;
        if (strings.length <= multiplierIndex) {
            sendUsage(sender);
            return false;
        }

        Optional<Double> multiplier = parseDouble(strings[multiplierIndex]);
        if (multiplier.isEmpty()) {
            sendError(sender, "Invalid multiplier");
            return false;
        }

        int durationSeconds = DEFAULT_DURATION_SECONDS;
        if (strings.length > durationIndex) {
            Optional<Integer> seconds = parseInt(strings[durationIndex]);
            if (seconds.isEmpty()) {
                sendError(sender, "Invalid time");
                return false;
            }
            durationSeconds = seconds.get();
        }

        applyPlayerMultiplier(sender, strings[targetIndex], multiplier.get(), durationSeconds);
        return true;
    }

    private void applyGlobalMultiplier(CommandSender sender, double multiplier) {
        plugin.getSkillManager().saveGlobalMultiplier(multiplier);
        sendSuccess(sender, "Skills multiplier set to " + multiplier);
    }

    private void applyPlayerMultiplier(CommandSender sender, String targetName, double multiplier,
                                       int durationSeconds) {
        Player target = findPlayer(targetName);
        if (target == null) {
            sendError(sender, "Player not found");
            return;
        }

        removeExistingMultiplier(target);

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

    private Player findPlayer(String name) {
        return plugin.getServer().getOnlinePlayers().stream()
                .filter((Player player) -> player.getName().equalsIgnoreCase(name))
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
        sender.sendMessage(ChatColor.GREEN + message);
        if (sender instanceof Player p) {
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        }
    }

    private void sendError(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.RED + message);
        if (sender instanceof Player p) {
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        }
    }

    private void sendUsage(CommandSender sender) {
        sendError(sender, "Usage: /skillsmultiplier all <multiplier> | "
                + "/skillsmultiplier player <name> <multiplier> [seconds]");
    }
}
