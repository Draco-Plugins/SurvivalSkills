package sir_draco.survivalskills.Abilities;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SkillListeners.MiningSkill;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;

public class VeinMinerAsync extends BukkitRunnable {

    private final SurvivalSkills plugin;
    private final Player p;
    private final MiningSkill skill;
    private final Block block;
    private final int blocksPerHunger;

    public VeinMinerAsync(SurvivalSkills plugin, Player p, MiningSkill skill, Block block, int blocksPerHunger) {
        this.plugin = plugin;
        this.p = p;
        this.skill = skill;
        this.block = block;
        this.blocksPerHunger = blocksPerHunger;
    }

    @Override
    public void run() {
        // Get the blocks in the vein and remove hunger appropriately
        ArrayList<Block> blocks = getVeinBlocks(block);
        ArrayList<Block> eventBlockTrackingList = new ArrayList<>(blocks);
        skill.getVeinTracker().put(p, eventBlockTrackingList);
        if (skill.getVeinminerTracker().get(p) == 0) {
            int food = p.getFoodLevel();
            int newFood = food - (blocks.size() / blocksPerHunger);
            if (newFood < 0) {
                p.sendRawMessage(ChatColor.RED + "You don't have enough hunger to mine the whole ore vein with");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                skill.getVeinTracker().remove(p);
                return;
            }
            p.setFoodLevel(newFood);
        }

        ItemStack pickaxe = p.getInventory().getItemInMainHand();

        // Break all the blocks in the vein 1 block per tick
        new BukkitRunnable() {
            int i = 0;
            @Override
            public void run() {
                if (i >= blocks.size()) {
                    cancel();
                    return;
                }

                Block blockToBreak = blocks.get(i);
                BlockBreakEvent event = new BlockBreakEvent(blockToBreak, p);
                Bukkit.getServer().getPluginManager().callEvent(event);
                if (!event.isCancelled()) blockToBreak.breakNaturally(pickaxe);
                i++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public ArrayList<Block> getVeinBlocks(Block startBlock) {
        ArrayList<Block> blocks = new ArrayList<>();
        blocks.add(startBlock);
        int iterations = 0;
        getVeinBlockHelper(startBlock.getType(), startBlock, new ArrayList<>(), blocks, iterations);
        blocks.remove(startBlock);
        return blocks;
    }

    public ArrayList<Block> getVeinBlockHelper(Material type, Block startBlock, ArrayList<Block> checkedBlocks, ArrayList<Block> blocks, int iterations) {
        iterations++;
        if (iterations > 10000 || blocks.size() >= 100) return blocks;
        Block left = startBlock.getRelative(-1, 0, 0);
        Block right = startBlock.getRelative(1, 0, 0);
        Block front = startBlock.getRelative(0, 0, 1);
        Block back = startBlock.getRelative(0, 0, -1);
        Block up = startBlock.getRelative(0, 1, 0);
        Block down = startBlock.getRelative(0, -1, 0);

        if (type.equals(left.getType()) && !blocks.contains(left)) blocks.add(left);
        if (type.equals(right.getType()) && !blocks.contains(right)) blocks.add(right);
        if (type.equals(front.getType()) && !blocks.contains(front)) blocks.add(front);
        if (type.equals(back.getType()) && !blocks.contains(back)) blocks.add(back);
        if (type.equals(up.getType()) && !blocks.contains(up)) blocks.add(up);
        if (type.equals(down.getType()) && !blocks.contains(down)) blocks.add(down);

        checkedBlocks.add(startBlock);
        if (checkedBlocks.size() == blocks.size()) return blocks;
        for (Block block : blocks) {
            if (checkedBlocks.contains(block)) continue;
            return getVeinBlockHelper(type, block, checkedBlocks, blocks, iterations);
        }
        return blocks;
    }
}
