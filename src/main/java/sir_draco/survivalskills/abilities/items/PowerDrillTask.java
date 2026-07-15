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
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.skill_listeners.god.PowerOreChallengeListener;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public final class PowerDrillTask extends BukkitRunnable {

    public static final String DRILL_BREAK_METADATA = "survivalskills_power_drill_break";

    private static final int MAX_BLOCKS_PER_TICK = 9;
    private static final long MAX_BATCH_NANOS = 1_500_000L;
    private static final int MAX_DRILL_DEPTH = 19;
    private static final double BLOCK_CENTER_OFFSET = 0.5;
    private static final int INITIAL_DELAY = 0;
    private static final int TICK_PERIOD = 1;

    private final SurvivalSkills plugin;
    private final Player drillPlayer;
    private final UUID drillPlayerId;
    private final PowerOreChallengeListener listener;
    private final List<BlockPosition> blockPositions;
    private final World world;
    private final Location originLocation;
    private final ItemStack pickaxe;
    private final AtomicBoolean stopped = new AtomicBoolean();

    private int blockIndex;
    private boolean scheduled;

    public PowerDrillTask(SurvivalSkills plugin, Player player, PowerOreChallengeListener listener, Block originBlock) {
        this.plugin = plugin;
        this.drillPlayer = player;
        this.drillPlayerId = player.getUniqueId();
        this.listener = listener;
        this.world = originBlock.getWorld();
        this.originLocation = originBlock.getLocation();
        Vector direction = player.getLocation().getDirection().clone();
        this.blockPositions = getBlockPositions(
                originBlock.getX(), originBlock.getY(), originBlock.getZ(),
                direction.getX(), direction.getY(), direction.getZ());
        this.pickaxe = player.getInventory().getItemInMainHand().clone();
    }

    public void start() {
        listener.registerDrillTask(drillPlayerId, this);
        try {
            runTaskTimer(plugin, INITIAL_DELAY, TICK_PERIOD);
            scheduled = true;
        } catch (RuntimeException exception) {
            stop(false);
            logFailure("could not be scheduled", exception);
        }
    }

    @Override
    public void run() {
        if (!drillPlayer.isOnline()) {
            stop(true);
            return;
        }

        try {
            if (blockIndex >= blockPositions.size()) {
                stop(true);
                return;
            }

            boolean anyBroken = false;
            int processedBlocks = 0;
            long deadline = System.nanoTime() + MAX_BATCH_NANOS;
            while (blockIndex < blockPositions.size()
                    && processedBlocks < MAX_BLOCKS_PER_TICK
                    && System.nanoTime() < deadline) {
                BlockPosition position = blockPositions.get(blockIndex);
                blockIndex++;
                processedBlocks++;
                if (!world.isChunkLoaded(position.chunkX(), position.chunkZ())) continue;

                Block targetBlock = world.getBlockAt(position.x(), position.y(), position.z());
                if (breakBlock(targetBlock, drillPlayer, pickaxe)) anyBroken = true;
            }

            if (anyBroken) world.playSound(originLocation, Sound.BLOCK_ANVIL_FALL, 1, 1);
            if (blockIndex >= blockPositions.size()) stop(true);
        } catch (RuntimeException exception) {
            logFailure("failed while breaking blocks", exception);
            stop(true);
        }
    }

    public void cancelForDisconnect() {
        stop(true);
    }

    public static List<BlockPosition> getBlockPositions(int originX, int originY, int originZ,
            double directionX, double directionY, double directionZ) {
        LinkedHashSet<BlockPosition> positions = new LinkedHashSet<>();
        positions.add(new BlockPosition(originX, originY, originZ));
        double cursorX = originX + BLOCK_CENTER_OFFSET;
        double cursorY = originY + BLOCK_CENTER_OFFSET;
        double cursorZ = originZ + BLOCK_CENTER_OFFSET;

        for (int i = 1; i <= MAX_DRILL_DEPTH; i++) {
            cursorX += directionX;
            cursorY += directionY;
            cursorZ += directionZ;
            int nextX = (int) Math.floor(cursorX);
            int nextY = (int) Math.floor(cursorY);
            int nextZ = (int) Math.floor(cursorZ);

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        positions.add(new BlockPosition(nextX + x, nextY + y, nextZ + z));
                    }
                }
            }
        }
        return List.copyOf(positions);
    }

    public boolean breakBlock(Block block, Player player, ItemStack tool) {
        Material type = block.getType();
        if (type.isAir() || type == Material.BEDROCK || type == Material.BARRIER || type == Material.STRUCTURE_VOID)
            return false;

        player.setMetadata(DRILL_BREAK_METADATA, new FixedMetadataValue(plugin, true));
        try {
            BlockBreakEvent event = new BlockBreakEvent(block, player);
            Bukkit.getServer().getPluginManager().callEvent(event);
            if (!event.isCancelled()) block.breakNaturally(tool);
            return true;
        } finally {
            player.removeMetadata(DRILL_BREAK_METADATA, plugin);
        }
    }

    private void stop(boolean cancelScheduledTask) {
        if (!stopped.compareAndSet(false, true)) return;

        listener.unregisterDrillTask(drillPlayerId, this);
        if (cancelScheduledTask && scheduled) cancel();
    }

    private void logFailure(String message, RuntimeException exception) {
        Bukkit.getLogger().log(Level.SEVERE,
                String.format("[SurvivalSkills] Power drill task for %s %s", drillPlayer.getName(), message),
                exception);
    }

    public record BlockPosition(int x, int y, int z) {

        private int chunkX() {
            return x >> 4;
        }

        private int chunkZ() {
            return z >> 4;
        }
    }
}
