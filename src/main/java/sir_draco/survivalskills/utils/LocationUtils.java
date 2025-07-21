package sir_draco.survivalskills.utils;

import org.bukkit.Location;

public class LocationUtils {

    public static Location getSurfaceBlock(Location loc) {
        if (loc.getWorld() == null) return loc;
        return loc.getWorld().getHighestBlockAt(loc).getLocation().clone().add(0, 1, 0);
    }
}
