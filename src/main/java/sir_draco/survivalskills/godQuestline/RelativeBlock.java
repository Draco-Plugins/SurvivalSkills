package sir_draco.survivalskills.godQuestline;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

public record RelativeBlock(int x, int y, int z, BlockData data, Material material) {}
