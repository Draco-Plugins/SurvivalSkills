package sir_draco.survivalskills.Abilities;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;

public class CaveFinderAsync extends BukkitRunnable {

    private final SurvivalSkills plugin;
    private final Player p;
    private boolean trueCave = false;

    public CaveFinderAsync(Player p, SurvivalSkills plugin) {
        this.p = p;
        this.plugin = plugin;
    }

    @Override
    public void run() {
        ArrayList<Location> locations = getParticleLocations(p.getLocation(), 10);
        if (locations == null || locations.isEmpty()) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    p.sendRawMessage(ChatColor.YELLOW + "No cave found");
                }
            }.runTask(plugin);
            return;
        }

        new BukkitRunnable() {
            private int count = 0;
            @Override
            public void run() {
                Particle.DustOptions dust = new Particle.DustOptions(org.bukkit.Color.RED, 1);
                for (Location loc : locations) {
                    p.spawnParticle(org.bukkit.Particle.DUST, loc, 1, 0, 0, 0, 0, dust);
                }

                if (count == 0) {
                    p.sendRawMessage(ChatColor.GREEN + "Cave found at ("
                            + locations.get(0).getBlockX() + ", " + locations.get(0).getBlockY() + ", "
                            + locations.get(0).getBlockZ() + ")");
                    if (trueCave) {
                        p.sendRawMessage(ChatColor.AQUA + "This is a true cave!");
                    } else {
                        p.sendRawMessage(ChatColor.YELLOW + "This could be a random dark spot");
                    }
                }
                count++;
                if (count > 20) cancel();
            }
        }.runTaskTimer(plugin, 0, 10);
    }

    public ArrayList<Location> getParticleLocations(Location loc, int radius) {
        Location locStart = findCaveAir(loc, radius);
        if (locStart == null) return null;
        return tracePathThroughLocations(locStart, loc.clone().add(0, 2, 0));
    }

    public Location findCaveAir(Location loc, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius * 2; y <= radius * 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Location locCheck = loc.clone().add(x, y, z);
                    Block block = locCheck.getBlock();
                    if (block.getType().equals(Material.CAVE_AIR)) {
                        trueCave = true;
                        return locCheck;
                    }
                    if (!block.isEmpty() && !block.getType().isAir()) continue;
                    if (block.getLightFromSky() > 0) continue;
                    if (block.getLightLevel() == 0) return locCheck;
                }
            }
        }
        return null;
    }

    public ArrayList<Location> tracePathThroughLocations(Location startLoc, Location endLoc) {
        ArrayList<Location> locations = new ArrayList<>();

        // Create a list of locations from the start location to the end location
        double distance = startLoc.distance(endLoc);
        double increment = 0.2;
        double x = startLoc.getX();
        double y = startLoc.getY();
        double z = startLoc.getZ();
        double dx = endLoc.getX() - startLoc.getX();
        double dy = endLoc.getY() - startLoc.getY();
        double dz = endLoc.getZ() - startLoc.getZ();

        double incrementX = (dx / distance) * increment;
        double incrementY = (dy / distance) * increment;
        double incrementZ = (dz / distance) * increment;

        for (double i = 0; i < distance; i += increment) {
            x += incrementX;
            y += incrementY;
            z += incrementZ;
            locations.add(new Location(startLoc.getWorld(), x, y, z));
        }

        return locations;
    }
}
