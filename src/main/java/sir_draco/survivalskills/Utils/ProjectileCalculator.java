package sir_draco.survivalskills.Bosses;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class ProjectileCalculator {

    private static final double gravity = -0.06;

    public static Vector getVector(Location start, Location end, double speed) {
        double dx = (end.getX() - start.getX());
        double dy = (end.getY() - start.getY());
        double dz = (end.getZ() - start.getZ());
        double distance = Math.sqrt((dx * dx) + (dz * dz));
        double time = distance / speed;

        // Normalize the direction vector
        double vx = speed * dx / distance;
        double vy = (dy - (0.5 * gravity * time * time)) / time;
        double vz = speed * dz / distance;
        return new Vector(vx, vy, vz);
    }

    public static Vector getNoGravityVector(Location start, Location end, double speed) {
        double dx = (end.getX() - start.getX());
        double dy = (end.getY() - start.getY());
        double dz = (end.getZ() - start.getZ());
        double distance = Math.sqrt((dx * dx) + (dz * dz));
        if (distance == 0) return new Vector(0, 0, 0);

        // Normalize the direction vector
        double vx = speed * dx / distance;
        double vy = speed * dy / distance;
        double vz = speed * dz / distance;
        return new Vector(vx, vy, vz);
    }

    public static Vector getDirectionVector(Location start, Location end) {
        return end.toVector().subtract(start.toVector()).normalize();
    }

    public static void particleLine(Location start, Location end, Particle particle, Color color) {
        World world = start.getWorld();
        if (world == null) return;
        Vector vector = getDirectionVector(start, end);
        Location loc = start.clone();
        int totalSteps = (int) (start.distance(end) / vector.length());
        for (int i = 0; i < totalSteps; i++) {
            if (color != null) world.spawnParticle(particle, loc, 1, new Particle.DustOptions(color, 1));
            else world.spawnParticle(particle, loc, 1);
            loc.add(vector);
        }
    }
}
