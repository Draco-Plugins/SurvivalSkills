package sir_draco.survivalskills.Abilities;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;
import java.util.HashMap;

public class SpelunkerAbilityAsync extends BukkitRunnable {

    private final Block playerBlock;
    private final int radius;
    private final ArrayList<Material> ores;
    private final ArrayList<Block> currentGlows = new ArrayList<>();
    private final ArrayList<Block> glowsToAdd = new ArrayList<>();

    private ArrayList<Block> glowsToRemove;
    private Block closestLocation = null;

    public SpelunkerAbilityAsync(SurvivalSkills plugin, Block playerBlock, int radius, HashMap<Block, ItemDisplay> currentGlowsRaw) {
        this.playerBlock = playerBlock;
        this.radius = radius;
        ores = plugin.getMiningListener().getOres();
        currentGlows.addAll(currentGlowsRaw.keySet());
    }

    @Override
    public void run() {
        // Loop through all blocks
        ArrayList<Block> blocks = getNearBlocks(playerBlock);
        Block closestBlock = null;
        double closestDistance = Double.MAX_VALUE;
        for (Block block : blocks) {
            if (!ores.contains(block.getType())) continue; // Check for an ore
            double distance = playerBlock.getLocation().distance(block.getLocation());
            if (distance < closestDistance) {
                closestBlock = block;
                closestDistance = distance;
            }
            if (currentGlows.contains(block)) currentGlows.remove (block); // If it already exists ignore it
            else glowsToAdd.add(block); // Otherwise add a glow block
        }
        // Any leftover blocks will be removed
        glowsToRemove = currentGlows;

        if (closestBlock != null) closestLocation = closestBlock;
    }

    public ArrayList<Block> getGlowsToAdd() {
        return glowsToAdd;
    }

    public ArrayList<Block> getGlowsToRemove() {
        return glowsToRemove;
    }

    public ArrayList<Block> getNearBlocks(Block block) {
        // Bottom left corner of the cube
        Block startBlock = block.getRelative(-radius, -radius, -radius);
        ArrayList<Block> blocks = getBlockRowX(startBlock);
        for (int i = 1; i <= radius * 2; i++) blocks.addAll(getBlockRowX(startBlock.getRelative(0, 0, i)));
        return blocks;
    }

    public ArrayList<Block> getBlockRowX(Block block) {
        ArrayList<Block> blocks = getBlockColumn(block);
        // Assume the block given has the smallest X value
        for (int i = 1; i <= radius * 2; i++) blocks.addAll(getBlockColumn(block.getRelative(i, 0, 0)));
        return blocks;
    }

    public ArrayList<Block> getBlockColumn(Block block) {
        ArrayList<Block> blocks = new ArrayList<>();
        blocks.add(block);
        // Assume the block given is the bottom of the column
        for (int i = 1; i <= radius * 2; i ++) blocks.add(block.getRelative(0, i, 0));
        return blocks;
    }

    public Block getClosestLocation() {
        return closestLocation;
    }
}
