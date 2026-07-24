package sir_draco.survivalskills.abilities;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.skill_listeners.MiningSkill;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class VeinMinerAsync extends BukkitRunnable {

    public static final String VEIN_MINER_BREAK_METADATA = "survivalskills_veinminer_break";

    private final SurvivalSkills plugin;
    private final Player player;
    private final MiningSkill skill;
    private final Block originBlock;
    private final Material material;
    private final int blocksPerHunger;
    private final AtomicBoolean cleanedUp = new AtomicBoolean();

    public VeinMinerAsync(SurvivalSkills plugin, Player player, MiningSkill skill, Block block, Material material,
            int blocksPerHunger) {
        this.plugin = plugin;
        this.player = player;
        this.skill = skill;
        this.originBlock = block;
        this.material = material;
        this.blocksPerHunger = blocksPerHunger;
        this.skill.setVeinMinerActive(player, true);
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            cleanup();
            return;
        }

        try {
            ArrayList<Block> blocks = getVeinBlocks(originBlock);
            skill.getVeinTracker().put(player, new ArrayList<>(blocks));
            if (!applyHungerCost(blocks.size())) {
                cleanup();
                return;
            }

            ItemStack pickaxe = player.getInventory().getItemInMainHand().clone();
            try {
                new VeinBatchRunner(blocks, pickaxe).runTaskTimer(plugin, 0, 1);
            } catch (RuntimeException exception) {
                fail("could not schedule block breaking", exception);
            }
        } catch (RuntimeException exception) {
            fail("failed while preparing blocks", exception);
        }
    }

    public void cleanupAfterFailure() {
        cleanup();
    }

    private boolean applyHungerCost(int blockCount) {
        if (!Boolean.FALSE.equals(skill.getVeinminerTracker().get(player))) return true;

        int food = player.getFoodLevel();
        int newFood = food - (blockCount / blocksPerHunger);
        if (newFood < 0) {
            player.sendRawMessage(ChatColor.RED + "You don't have enough hunger to mine the whole ore vein with");
            player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return false;
        }

        FoodLevelChangeEvent event = new FoodLevelChangeEvent(player, newFood);
        player.setFoodLevel(newFood);
        Bukkit.getServer().getPluginManager().callEvent(event);
        return true;
    }

    private void cleanup() {
        if (!cleanedUp.compareAndSet(false, true)) return;

        skill.getVeinTracker().remove(player);
        try {
            player.removeMetadata(VEIN_MINER_BREAK_METADATA, plugin);
        } finally {
            skill.setVeinMinerActive(player, false);
        }
    }

    private void fail(String message, RuntimeException exception) {
        cleanup();
        Bukkit.getLogger().log(Level.SEVERE,
                String.format("[SurvivalSkills] Vein miner for %s %s", player.getName(), message), exception);
    }

    public ArrayList<Block> getVeinBlocks(Block startBlock) {
        ArrayList<Block> blocks = new ArrayList<>();
        blocks.add(startBlock);
        int iterations = 0;
        blocks = getVeinBlockHelper(material, startBlock, new ArrayList<>(), blocks, iterations);
        blocks.remove(startBlock);
        return blocks;
    }

    public ArrayList<Block> getVeinBlockHelper(Material type, Block startBlock, ArrayList<Block> checkedBlocks,
            ArrayList<Block> blocks, int iterations) {
        iterations++;
        if (iterations > 10000 || blocks.size() >= 100)
            return blocks;
        Block left = startBlock.getRelative(-1, 0, 0);
        Block right = startBlock.getRelative(1, 0, 0);
        Block front = startBlock.getRelative(0, 0, 1);
        Block back = startBlock.getRelative(0, 0, -1);
        Block up = startBlock.getRelative(0, 1, 0);
        Block down = startBlock.getRelative(0, -1, 0);

        if (type.equals(left.getType()) && !blocks.contains(left))
            blocks.add(left);
        if (type.equals(right.getType()) && !blocks.contains(right))
            blocks.add(right);
        if (type.equals(front.getType()) && !blocks.contains(front))
            blocks.add(front);
        if (type.equals(back.getType()) && !blocks.contains(back))
            blocks.add(back);
        if (type.equals(up.getType()) && !blocks.contains(up))
            blocks.add(up);
        if (type.equals(down.getType()) && !blocks.contains(down))
            blocks.add(down);

        checkedBlocks.add(startBlock);
        if (checkedBlocks.size() == blocks.size())
            return blocks;
        for (Block block : blocks) {
            if (checkedBlocks.contains(block))
                continue;
            return getVeinBlockHelper(type, block, checkedBlocks, blocks, iterations);
        }
        return blocks;
    }

    private final class VeinBatchRunner extends BukkitRunnable {
        private final ArrayList<Block> blocks;
        private final ItemStack pickaxe;
        private int blockIndex;

        private VeinBatchRunner(ArrayList<Block> blocks, ItemStack pickaxe) {
            this.blocks = blocks;
            this.pickaxe = pickaxe;
        }

        @Override
        public void run() {
            if (!player.isOnline() || blockIndex >= blocks.size()) {
                stop();
                return;
            }

            try {
                Block blockToBreak = blocks.get(blockIndex);
                blockIndex++;
                if (blockToBreak.getType().isAir()) return;

                player.setMetadata(VEIN_MINER_BREAK_METADATA, new FixedMetadataValue(plugin, true));
                try {
                    BlockBreakEvent event = new BlockBreakEvent(blockToBreak, player);
                    Bukkit.getServer().getPluginManager().callEvent(event);
                    if (!event.isCancelled()) breakBlock(blockToBreak, pickaxe, event.isDropItems());
                } finally {
                    player.removeMetadata(VEIN_MINER_BREAK_METADATA, plugin);
                }
            } catch (RuntimeException exception) {
                try {
                    fail("failed while breaking blocks", exception);
                } finally {
                    cancel();
                }
            }
        }

        private void stop() {
            cleanup();
            cancel();
        }
    }

    static void breakBlock(Block block, ItemStack pickaxe, boolean dropItems) {
        if (dropItems) {
            block.breakNaturally(pickaxe);
            return;
        }

        block.setType(Material.AIR);
    }
}
