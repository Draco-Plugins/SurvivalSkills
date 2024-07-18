package sir_draco.survivalskills.Commands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.SurvivalSkills;

import java.io.File;

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
        if (strings.length < 1) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                p.sendRawMessage(ChatColor.RED + "Usage: /skillsmultiplier <multiplier>");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            }
            return false;
        }
        double multiplier;
        try {
            multiplier = Double.parseDouble(strings[0]);
        }
        catch (NumberFormatException e) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                p.sendRawMessage(ChatColor.RED + "Invalid multiplier");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            }
            return false;
        }

        plugin.getSkillManager().setMultiplier(multiplier);
        if (sender instanceof Player) {
            Player p = (Player) sender;
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
        return true;
    }
}
