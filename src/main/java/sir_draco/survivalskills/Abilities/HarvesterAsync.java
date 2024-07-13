package sir_draco.survivalskills.Abilities;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;

public class HarvesterAsync extends BukkitRunnable {

    private final SurvivalSkills plugin;
    private final Block block;
    private final Player p;
    private final Material type;

    public HarvesterAsync(SurvivalSkills plugin, Block block, Player p, Material type) {
        this.plugin = plugin;
        this.block = block;
        this.p = p;
        this.type = type;
    }

    @Override
    public void run() {
        // Get the blocks to be harvested
        ArrayList<Block> blocks = getHarvestedCrops();

        // Add the blocks to the harvestedBlocks list using a duplicate list of the blocks
        plugin.getFarmingListener().getHarvestedBlocks().put(p, new ArrayList<>(blocks));

        // Break the blocks and replace them with seeds
        new BukkitRunnable() {
            @Override
            public void run() {
                World world = block.getWorld();
                for (Block b : blocks) {
                    Block block = world.getBlockAt(b.getLocation());
                    BlockBreakEvent event = new BlockBreakEvent(block, p);
                    Bukkit.getServer().getPluginManager().callEvent(event);
                }

                // Replace the crops with the appropriate seeds
                blocks.add(block);
                for (Block b : blocks) {
                    Block seedBlock = world.getBlockAt(b.getLocation());
                    if (type.equals(Material.WHEAT)) {
                        seedBlock.setType(Material.WHEAT);
                        Ageable ageable = (Ageable) seedBlock.getBlockData();
                        ageable.setAge(0);
                        seedBlock.setBlockData(ageable);
                    } else if (type.equals(Material.POTATOES)) {
                        seedBlock.setType(Material.POTATOES);
                        Ageable ageable = (Ageable) seedBlock.getBlockData();
                        ageable.setAge(0);
                        seedBlock.setBlockData(ageable);
                    } else if (type.equals(Material.CARROTS)) {
                        seedBlock.setType(Material.CARROTS);
                        Ageable ageable = (Ageable) seedBlock.getBlockData();
                        ageable.setAge(0);
                        seedBlock.setBlockData(ageable);
                    } else if (type.equals(Material.BEETROOTS)) {
                        seedBlock.setType(Material.BEETROOTS);
                        Ageable ageable = (Ageable) seedBlock.getBlockData();
                        ageable.setAge(0);
                        seedBlock.setBlockData(ageable);
                    } else if (type.equals(Material.NETHER_WART)) {
                        seedBlock.setType(Material.NETHER_WART);
                        Ageable ageable = (Ageable) seedBlock.getBlockData();
                        ageable.setAge(0);
                        seedBlock.setBlockData(ageable);
                    }
                    seedBlock.getState().update();
                }
            }
        }.runTask(plugin);
    }

    public ArrayList<Block> getHarvestedCrops() {
        ArrayList<Block> blocks = new ArrayList<>();
        blocks.add(block);
        blocks = getNearbyCrops(blocks, new ArrayList<>(), block);
        blocks.remove(block);
        return blocks;
    }

    public ArrayList<Block> getNearbyCrops(ArrayList<Block> blocks, ArrayList<Block> blocksChecked, Block block) {
        if (blocks.size() >= 200) return blocks;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Block b = block.getRelative(x, 0, z);
                if (b.equals(block)) continue;
                if (!b.getType().equals(type)) continue;
                if (blocks.contains(b)) continue;
                if (block.getBlockData() instanceof Ageable) {
                    Ageable ageable = (Ageable) block.getBlockData();
                    if (ageable.getAge() != ageable.getMaximumAge()) continue;
                }
                blocks.add(b);
            }
        }
        blocksChecked.add(block);
        if (blocksChecked.size() == blocks.size()) return blocks;
        for (Block b : blocks) {
            if (blocksChecked.contains(b)) continue;
            return getNearbyCrops(blocks, blocksChecked, b);
        }
        return blocks;
    }
}
