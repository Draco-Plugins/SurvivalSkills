package sir_draco.survivalskills.abilities.items;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.skill_listeners.GodListener;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;
import java.util.Objects;

public class PowerDrillAsync extends BukkitRunnable {

    private final SurvivalSkills plugin;
    private final Player p;
    private final GodListener listener;
    private final Block block;
    private final Vector direction;

    public PowerDrillAsync(SurvivalSkills plugin, Player p, GodListener listener, Block block) {
        this.plugin = plugin;
        this.p = p;
        this.listener = listener;
        this.block = block;
        this.direction = p.getLocation().getDirection();
    }

    @Override
    public void run() {
        // Get the blocks in the vein and remove hunger appropriately
        ArrayList<Block> blocks = getBlocks(block);
        ArrayList<Block> eventBlockTrackingList = new ArrayList<>(blocks);
        listener.getDrillTracker().put(p, eventBlockTrackingList);
        ItemStack pickaxe = p.getInventory().getItemInMainHand();

        // Break all the blocks around a block in the list 1 tick at a time
        new BukkitRunnable() {
            int i = 0;

            @Override
            public void run() {
                if (i >= blocks.size()) {
                    cancel();
                    return;
                }

                for (int j = 0; j <= 8; j++) {
                    if (i + j >= blocks.size())
                        break;
                    if (j == 0)
                        Objects.requireNonNull(block.getLocation().getWorld()).playSound(block.getLocation(),
                                Sound.BLOCK_ANVIL_FALL, 1, 1);
                    Block block = blocks.get(i + j);
                    breakBlock(block, p, pickaxe);
                }
                i += 9;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public ArrayList<Block> getBlocks(Block block) {
        ArrayList<Block> blocks = new ArrayList<>();
        blocks.add(block);
        Location startingLocation = block.getLocation().clone().add(0.5, 0.5, 0.5);
        // Move 19 blocks forward in the direction of the player
        for (int i = 1; i <= 19; i++) {
            startingLocation.add(direction);
            Block nextBlock = startingLocation.getBlock();
            if (blocks.contains(nextBlock))
                continue;
            blocks.add(nextBlock);
            // Add the blocks surrounding the block too
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        Block neighbor = nextBlock.getRelative(x, y, z);
                        if (blocks.contains(neighbor))
                            continue;
                        blocks.add(neighbor);
                    }
                }
            }
        }
        return blocks;
    }

    public void breakBlock(Block block, Player p, ItemStack tool) {
        if (block.getType().isAir())
            return;
        BlockBreakEvent event = new BlockBreakEvent(block, p);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled())
            block.breakNaturally(tool);
    }
}
