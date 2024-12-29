package sir_draco.survivalskills.Commands.AdminCommands;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;

@SuppressWarnings("NullableProblems")
public class StoreTrialBuildingCommand implements CommandExecutor {

    public StoreTrialBuildingCommand(SurvivalSkills plugin) {
        PluginCommand command = plugin.getCommand("storetrialbuilding");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player p)) return false;

        // Get blocks in 50x50x30 area
        Location location = p.getLocation().getBlock().getLocation();
        ArrayList<Block> blocks = getBlocks(location, 50, 50, 30);
        Bukkit.getLogger().info("Blocks: " + blocks);

        // Store blocks in config

        return true;
    }

    public ArrayList<Block> getBlocks(Location location, int x, int y, int z) {
        ArrayList<Block> blocks = new ArrayList<>();
        for (int i = -(x/2); i <= x/2; i++) {
            for (int j = 0; j <= y; j++) {
                for (int k = -(z/2); k <= z; k++) {
                    Block block = location.clone().add(i, j, k).getBlock();
                    if (block.getType().isAir()) continue;
                    blocks.add(block);
                }
            }
        }
        return blocks;
    }
}
