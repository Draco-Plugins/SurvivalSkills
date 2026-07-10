package sir_draco.survivalskills.commands.admin_commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.TrialUtils;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class StoreTrialBuildingCommand implements CommandExecutor {

    public StoreTrialBuildingCommand(SurvivalSkills plugin) {
        PluginCommand command = plugin.getCommand("storetrialbuilding");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player p)) return false;

        if (strings.length >= 1 && strings[0].equalsIgnoreCase("load")) {
            // Load the default configuration from the JAR
            InputStream defConfigStream = getClass().getClassLoader().getResourceAsStream("trialbuilding.yml");
            if (defConfigStream == null) {
                throw new RuntimeException("Failed to load default config");
            }
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));

            // Save the merged configuration
            File file = new File(SurvivalSkills.getInstance().getDataFolder(), "trialbuilding.yml");
            try {
                defConfig.save(file);
            } catch (Exception e) {
                throw new RuntimeException("Failed to save config file", e);
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                // Get blocks in 50x50x30 area
                Location location = p.getLocation().getBlock().getLocation();
                ArrayList<Block> blocks = TrialUtils.getBlocks(location, 50, 30, 50);

                // Store blocks in config
                TrialUtils.storeTrialBuilding(location, blocks);
                p.sendRawMessage(ChatColor.GREEN + "Trial building stored!");
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            }
        }.runTaskAsynchronously(SurvivalSkills.getInstance());
        return true;
    }
}
