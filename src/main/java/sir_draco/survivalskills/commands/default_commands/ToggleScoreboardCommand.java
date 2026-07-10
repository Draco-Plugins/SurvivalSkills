package sir_draco.survivalskills.commands.default_commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.boards.SkillScoreboard;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.FileUtils;

import java.io.File;
import java.io.IOException;


public class ToggleScoreboardCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public ToggleScoreboardCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("togglescoreboard");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player p)) return false;

        File dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) plugin.saveResource("playerdata.yml", true);
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);

        if (!plugin.getShowScoreboard().containsKey(p.getUniqueId())) {
            Bukkit.getLogger().info("Player " + p.getName() + " does not have a scoreboard status");

            FileUtils.loadScoreboardSetting(p.getUniqueId(), data);
            p.sendRawMessage(ChatColor.RED + "Try again!");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }
        boolean status = !plugin.getShowScoreboard().get(p.getUniqueId());
        plugin.getShowScoreboard().put(p.getUniqueId(), status);
        if (!status) {
            SkillScoreboard.hideScoreboard(p);
            p.sendRawMessage(ChatColor.GREEN + "The scoreboard is now hidden");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        }
        else {
            SkillScoreboard.initializeScoreboard(p);
            p.sendRawMessage(ChatColor.GREEN + "The scoreboard is visible");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        }

        data.set(p.getUniqueId() + ".Scoreboard", status);
        try {
            data.save(dataFile);
        } catch (IOException e) {
            Bukkit.getLogger().warning("Could not save data file");
        }
        return true;
    }
}
