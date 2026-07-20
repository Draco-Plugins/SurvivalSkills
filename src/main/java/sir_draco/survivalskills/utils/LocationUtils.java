package sir_draco.survivalskills.utils;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.Optional;

public class LocationUtils {

    public static Optional<Location> getSurfaceBlock(Location location) {
        World world = location.getWorld();
        if (world == null) return Optional.empty();

        Block surfaceBlock = world.getHighestBlockAt(location);
        Block feetBlock = surfaceBlock.getRelative(BlockFace.UP);
        Block headBlock = feetBlock.getRelative(BlockFace.UP);
        if (surfaceBlock.isPassable() || !feetBlock.isPassable() || !headBlock.isPassable())
            return Optional.empty();

        return Optional.of(surfaceBlock.getLocation().add(0.5, 1, 0.5));
    }
}
