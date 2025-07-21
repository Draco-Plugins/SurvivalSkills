package sir_draco.survivalskills.commands.AdminCommands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.abilities.AbilityTimer;
import sir_draco.survivalskills.SurvivalSkills;

import java.io.File;

@SuppressWarnings("NullableProblems")
public class SkillsMultiplierCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public SkillsMultiplierCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("skillsmultiplier");
        if (command == null) return;
        command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (strings.length < 2) {
            if (sender instanceof Player p) {
                p.sendRawMessage(ChatColor.RED + "Usage: /skillsmultiplier <player/all> <multiplier>");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            }
            return false;
        }

        if (strings[0].equalsIgnoreCase("all")) {
            double multiplier;
            try {
                multiplier = Double.parseDouble(strings[1]);
            }
            catch (NumberFormatException e) {
                if (sender instanceof Player p) {
                    p.sendRawMessage(ChatColor.RED + "Invalid multiplier");
                    p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                }
                return false;
            }

            plugin.getSkillManager().setMultiplier(multiplier);
            if (sender instanceof Player p) {
                p.sendRawMessage(ChatColor.GREEN + "Skills multiplier set to " + multiplier);
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            }

            File file = new File(plugin.getDataFolder(), "config.yml");
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            config.set("SkillXPMultiplier", multiplier);

            try {
                config.save(file);
            } catch (Exception e) {
                throw new RuntimeException("Failed to save config file", e);
            }
        }
        else {
            if (strings.length < 3) {
                if (sender instanceof Player p) {
                    p.sendRawMessage(ChatColor.RED + "Usage: /skillsmultiplier player <player> <multiplier> [time]");
                    p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                }
                return false;
            }

            // Find the player
            Player p = null;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.getName().equalsIgnoreCase(strings[1])) continue;
                p = player;
                break;
            }

            if (p == null) {
                if (sender instanceof Player player) {
                    player.sendRawMessage(ChatColor.RED + "Player not found");
                    player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                }
                return false;
            }

            double multiplier;
            try {
                multiplier = Double.parseDouble(strings[2]);
            }
            catch (NumberFormatException e) {
                if (sender instanceof Player player) {
                    player.sendRawMessage(ChatColor.RED + "Invalid multiplier");
                    player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                }
                return false;
            }

            // Remove existing multipliers
            AbilityTimer timer = plugin.getAbilityManager().getAbility(p, "XPVoucher");
            if (timer != null) {
                timer.cancel();
                plugin.getAbilityManager().removeAbility(p, "XPVoucher");
            }

            int activeTime = 3600;
            if (strings.length >= 4) {
                try {
                    activeTime = Integer.parseInt(strings[3]);
                }
                catch (NumberFormatException e) {
                    if (sender instanceof Player player) {
                        player.sendRawMessage(ChatColor.RED + "Invalid multiplier");
                        player.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                    }
                    return false;
                }
            }

            int minutes = activeTime / 60;

            // Add the new multiplier
            plugin.getSkillManager().setPlayerMultiplier(p, multiplier);
            AbilityTimer newTimer = new AbilityTimer(plugin, "XPVoucher", p, activeTime, 0);
            newTimer.runTaskTimerAsynchronously(plugin, 0, 20);
            plugin.getAbilityManager().addAbility(p, newTimer);
            p.sendRawMessage(ChatColor.GREEN + "Skills multiplier set to " + ChatColor.AQUA + multiplier + ChatColor.GREEN + " for "
                    + ChatColor.AQUA + minutes + ChatColor.GREEN + " minutes");

            if (sender instanceof Player player) {
                player.sendRawMessage(ChatColor.GREEN + "Skills multiplier set to " + multiplier + " for " + p.getName() +
                        " for " + minutes + " minutes");
                player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            }
        }


        return true;
    }
}
