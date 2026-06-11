package sir_draco.survivalskills.external.providers;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;

import sir_draco.survivalskills.utils.FileUtils;

public class WorldGuardProvider {

    private RegionContainer container = null;
    private ProtectedRegion spawnRegion = null;

    public WorldGuardProvider() {
        container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        World world = Bukkit.getWorld(Bukkit.getWorlds().get(0).getName());
        if (world == null) {
            Bukkit.getLogger().warning("Could not find world for worldguard");
        } else {
            RegionManager regions = container.get(BukkitAdapter.adapt(world));
            if (regions == null) {
                Bukkit.getLogger().warning("Could not find region manager for worldguard");
            } else {
                String spawnRegionName = FileUtils.loadSpawnRegionName();
                spawnRegion = regions.getRegion(spawnRegionName);
                if (!ProtectedRegion.isValidId(spawnRegionName)) {
                    Bukkit.getLogger().log(Level.WARNING, "Spawn region is invalid. Please create a region called 'spawn'.");
                }
                if (spawnRegion == null) {
                    Bukkit.getLogger().log(Level.WARNING, "Could not find spawn region in worldguard. Please create a region called 'spawn'.");
                }
            }
        }
    }

    
    public boolean canPlaceBlockInRegion(Player p, Location loc) {
        World world = loc.getWorld();
        if (world == null) return true;
        if (container == null) return true;
        RegionManager regions = container.get(BukkitAdapter.adapt(world));
        if (regions == null) return true;

        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        ApplicableRegionSet applicableRegions = regions
                .getApplicableRegions(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()));
        for (ProtectedRegion protectedRegion : applicableRegions) {
            if (protectedRegion == null || !protectedRegion.contains(BlockVector3.at(x, y, z)))
                continue;
            StateFlag.State state = protectedRegion.getFlag(Flags.BLOCK_PLACE);
            boolean allowed = StateFlag.test(state);
            if (p.hasPermission("worldguard.region.bypass." + protectedRegion.getId()) || p.isOp())
                allowed = true;
            if (!allowed)
                return false;
        }

        return true;
    }

    public boolean locationInRegion(double x, double y, double z) {
        if (spawnRegion == null) return false;
        return spawnRegion.contains(BlockVector3.at(x, y, z));
    }

    public RegionContainer getContainer() {
        return container;
    }

    public ProtectedRegion getSpawnRegion() {
        return spawnRegion;
    }

    public void setContainer(RegionContainer container) {
        this.container = container;
    }
    
    public void setSpawnRegion(ProtectedRegion region) {
        this.spawnRegion = region;
    }
}
