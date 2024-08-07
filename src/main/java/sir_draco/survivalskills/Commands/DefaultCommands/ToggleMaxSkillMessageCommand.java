package sir_draco.survivalskills.Commands.DefaultCommands;

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
import sir_draco.survivalskills.SurvivalSkills;

import java.io.File;
import java.io.IOException;

public class ToggleMaxSkillMessageCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public ToggleMaxSkillMessageCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("togglemaxskillmessage");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;

        if (!plugin.getSkillManager().getMaxSkillMessage().containsKey(p)) {
            p.sendRawMessage(ChatColor.GREEN + "You will now receive a message when you reach the maximum level of a skill.");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            plugin.getSkillManager().getMaxSkillMessage().put(p, true);
            saveStatus(p.getUniqueId().toString(), true);
            return true;
        }

        boolean toggle = plugin.getSkillManager().getMaxSkillMessage().get(p);
        if (toggle) {
            p.sendRawMessage(ChatColor.GREEN + "You will no longer receive a message when you reach the maximum level of a skill.");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            plugin.getSkillManager().getMaxSkillMessage().put(p, false);
        } else {
            p.sendRawMessage(ChatColor.GREEN + "You will now receive a message when you reach the maximum level of a skill.");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            plugin.getSkillManager().getMaxSkillMessage().put(p, true);
        }
        saveStatus(p.getUniqueId().toString(), !toggle);

        return true;
    }

    public void saveStatus(String uuid, boolean status) {
        File dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) plugin.saveResource("playerdata.yml", true);
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);

        data.set(uuid + ".MaxSkillMessage", status);
        try {
            data.save(dataFile);
        } catch (IOException e) {
            Bukkit.getLogger().warning("Could not save data file");
        }
    }
}
