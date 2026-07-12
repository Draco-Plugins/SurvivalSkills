package sir_draco.survivalskills.trophy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class CircularRotationObject {

    private static final double CENTER_OFFSET_X = 0.5;
    private static final double CENTER_OFFSET_Y = 1.0;
    private static final double CENTER_OFFSET_Z = 0.5;
    private static final double VELOCITY_EPSILON = 0.01;
    private static final double DISTANCE_TOLERANCE = 0.15;

    private final List<Location> locationList = new ArrayList<>();
    private final int orbitals;
    private final Location center;

    private double radius;

    public CircularRotationObject(Location center, double radius, int orbitals) {
        this.center = center.clone().add(CENTER_OFFSET_X, CENTER_OFFSET_Y, CENTER_OFFSET_Z);
        this.radius = radius;
        this.orbitals = orbitals;
        createLocations(0);
    }

    public void createLocations(double angleOffset) {
        if (!locationList.isEmpty()) locationList.clear();

        World world = center.getWorld();
        double increment = 2.0 * Math.PI / orbitals;
        double x = center.getX();
        double y = center.getY();
        double z = center.getZ();
        for (int i = 0; i < orbitals; i++) {
            double angle = angleOffset + (i * increment);
            locationList.add(new Location(world, x + (Math.cos(angle) * radius), y, z + (Math.sin(angle) * radius)));
        }
    }

    public void setRadius(double newRadius) {
        radius = newRadius;
    }

    public Vector getVelocityVector(Location loc, double scale) {
        double zVelocity = (loc.getZ() - center.getZ()) * scale;
        if (Math.abs(zVelocity) < VELOCITY_EPSILON) zVelocity = 0;
        double xVelocity = (loc.getX() - center.getX()) * scale;
        if (Math.abs(xVelocity) < VELOCITY_EPSILON) xVelocity = 0;
        return new Vector(-1 * zVelocity, 0.0, xVelocity);
    }

    public Location getLocation(int slot) {
        if (slot >= locationList.size()) {
            Bukkit.getLogger().warning("CircularRotationObject: Slot " + slot + " is out of bounds. Resetting to 0.");
            slot = 0;
        }
        return locationList.get(slot);
    }

    public boolean tooFar(Location orbital) {
        double xDist = orbital.getX() - center.getX();
        double zDist = orbital.getZ() - center.getZ();
        double threshold = radius * (1.0 + DISTANCE_TOLERANCE);
        return (xDist * xDist) + (zDist * zDist) > threshold * threshold;
    }

    public static double getAngle(Location loc, Location center) {
        double xDist = loc.getX() - center.getX();
        double zDist = loc.getZ() - center.getZ();
        double angle = Math.atan2(zDist, xDist);
        if (angle < 0) angle += 2 * Math.PI;
        return angle;
    }

    public Location getCenter() {
        return center;
    }
}
