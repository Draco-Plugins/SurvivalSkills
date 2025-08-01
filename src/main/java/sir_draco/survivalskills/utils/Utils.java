package sir_draco.survivalskills.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.SurvivalSkills;

public class Utils {
    private Utils() {
        // Prevent instantiation
    }

    /**
     * Returns true if there is a claim there
     */
    public static boolean checkForClaim(Player p, Location loc) {
        String noBuildReason = GriefPrevention.instance.allowBuild(p, loc);
        return (noBuildReason != null);
    }

    public static boolean canPlaceBlockInRegion(Player p, Location loc) {
        World world = loc.getWorld();
        if (world == null) return true;
        RegionManager regions = SurvivalSkills.getInstance().getContainer().get(BukkitAdapter.adapt(world));
        if (regions == null) return true;

        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        ApplicableRegionSet applicableRegions = regions.getApplicableRegions(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()));
        for (ProtectedRegion protectedRegion : applicableRegions) {
            if (protectedRegion == null || !protectedRegion.contains(BlockVector3.at(x, y, z))) continue;
            StateFlag.State state = protectedRegion.getFlag(Flags.BLOCK_PLACE);
            boolean allowed = StateFlag.test(state);
            if (p.hasPermission("worldguard.region.bypass." + protectedRegion.getId()) || p.isOp()) allowed = true;
            if (!allowed) return false;
        }

        return true;
    }
}
