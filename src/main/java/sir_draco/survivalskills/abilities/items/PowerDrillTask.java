package sir_draco.survivalskills.abilities.items;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.skill_listeners.god.PowerOreChallengeListener;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class PowerDrillTask extends BukkitRunnable {

    private static final int BATCH_SIZE = 9;
    private static final int MAX_DRILL_DEPTH = 19;
    private static final double BLOCK_CENTER_OFFSET = 0.5;
    private static final int INITIAL_DELAY = 0;
    private static final int TICK_PERIOD = 1;

    private final SurvivalSkills plugin;
    private final Player drillPlayer;
    private final PowerOreChallengeListener listener;
    private final Block originBlock;

    public PowerDrillTask(SurvivalSkills plugin, Player p, PowerOreChallengeListener listener, Block block) {
        this.plugin = plugin;
        this.drillPlayer = p;
        this.listener = listener;
        this.originBlock = block;
    }

    @Override
    public void run() {
        if (!drillPlayer.isOnline()) return;

        Location playerLoc = drillPlayer.getLocation();
        if (playerLoc == null || playerLoc.getWorld() == null) return;

        World world = originBlock.getLocation().getWorld();
        if (world == null) return;

        List<Block> blocks = getBlocks(originBlock, playerLoc.getDirection());
        List<Block> eventBlockTrackingList = new ArrayList<>(blocks);
        listener.registerDrillBlocks(drillPlayer, eventBlockTrackingList);
        ItemStack pickaxe = drillPlayer.getInventory().getItemInMainHand();

        new DrillBatchRunner(blocks, world, originBlock.getLocation(), drillPlayer, pickaxe, listener)
                .runTaskTimer(plugin, INITIAL_DELAY, TICK_PERIOD);
    }

    public List<Block> getBlocks(Block originBlock, Vector direction) {
        LinkedHashSet<Block> blockSet = new LinkedHashSet<>();
        blockSet.add(originBlock);
        Location cursor = originBlock.getLocation().clone().add(BLOCK_CENTER_OFFSET, BLOCK_CENTER_OFFSET, BLOCK_CENTER_OFFSET);

        for (int i = 1; i <= MAX_DRILL_DEPTH; i++) {
            cursor.add(direction);
            Block nextBlock = cursor.getBlock();
            blockSet.add(nextBlock);

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        blockSet.add(nextBlock.getRelative(x, y, z));
                    }
                }
            }
        }
        return new ArrayList<>(blockSet);
    }

    public static boolean breakBlock(Block block, Player player, ItemStack tool) {
        Material type = block.getType();
        if (type.isAir() || type == Material.BEDROCK || type == Material.BARRIER || type == Material.STRUCTURE_VOID)
            return false;
        BlockBreakEvent event = new BlockBreakEvent(block, player);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (!event.isCancelled()) block.breakNaturally(tool);
        return true;
    }

    private static class DrillBatchRunner extends BukkitRunnable {
        private int i = 0;
        private final List<Block> blocks;
        private final World world;
        private final Location originLocation;
        private final Player player;
        private final ItemStack pickaxe;
        private final PowerOreChallengeListener listener;

        DrillBatchRunner(List<Block> blocks, World world, Location originLocation, Player player,
                         ItemStack pickaxe, PowerOreChallengeListener listener) {
            this.blocks = blocks;
            this.world = world;
            this.originLocation = originLocation;
            this.player = player;
            this.pickaxe = pickaxe;
            this.listener = listener;
        }

        @Override
        public void run() {
            if (i >= blocks.size()) {
                listener.unregisterDrillBlocks(player);
                cancel();
                return;
            }

            boolean anyBroken = false;
            for (int j = 0; j < BATCH_SIZE; j++) {
                if (i + j >= blocks.size()) break;
                Block targetBlock = blocks.get(i + j);
                if (breakBlock(targetBlock, player, pickaxe)) anyBroken = true;
            }
            i += BATCH_SIZE;

            if (anyBroken) {
                world.playSound(originLocation, Sound.BLOCK_ANVIL_FALL, 1, 1);
            }
        }
    }
}
