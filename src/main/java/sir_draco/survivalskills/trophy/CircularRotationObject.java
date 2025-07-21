package sir_draco.survivalskills.trophy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class CircularRotationObject {

    private final ArrayList<Location> locationList = new ArrayList<>();
    private final int orbitals;
    private final Location center;

    private double radius;

    public CircularRotationObject(Location center, double radius, int orbitals) {
        this.center = center.clone().add(0.5, 1.0, 0.5);
        this.radius = radius;
        this.orbitals = orbitals;
        createLocations(0);
    }

    public void createLocations(double angleOffset) {
        if (!locationList.isEmpty()) locationList.clear();
        double degreeOffset = angleOffset * 180 / Math.PI;

        // Divide the circumference by the orbitals to determine the distance between them
        World world = center.getWorld();
        double degreeIncrement = 360.0 / orbitals;
        double x = center.getX(); // x = 0 relatively
        double y = center.getY();
        double z = center.getZ(); // assume z = 0 relatively
        for (double degrees = degreeOffset; degrees < 360.0 + degreeOffset; degrees += degreeIncrement) {
            double radians = degrees * Math.PI / 180;
            locationList.add(new Location(world, x + (Math.cos(radians) * radius), y, z + (Math.sin(radians) * radius)));
        }
    }

    public void setRadius(double newRadius) {
        radius = newRadius;
    }

    public Vector getVelocityVector(Location loc, double scale) {
        double z = (loc.getZ() - center.getZ()) * scale;
        if (Math.abs(z) < 0.01) z = 0;
        double x = (loc.getX() - center.getX()) * scale;
        if (Math.abs(x) < 0.01) x = 0;
        return new Vector(-1 * z, 0.0, x);
    }

    public double getAngle(Location loc) {
        return getAngle(loc.getX(), loc.getZ(), center.getX(), center.getZ());
    }

    public double getAngle(double x, double z, double cx, double cz) {
        // Use Math.atan2 for correct angle calculation
        double xDist = x - cx;
        double zDist = z - cz;
        double angle = Math.atan2(zDist, xDist);
        if (angle < 0) angle += 2 * Math.PI;
        return angle;
    }

    public Location getLocation(int slot) {
        if (slot >= locationList.size()) {
            slot = 0;
            Bukkit.getLogger().warning("CircularRotationObject: Slot " + slot + " is out of bounds. Resetting to 0.");
        }
        return locationList.get(slot);
    }

    public boolean tooFar(Location orbital) {
        double xDist = orbital.getX() - center.getX();
        double zDist = orbital.getZ() - center.getZ();
        double dist = Math.sqrt((xDist * xDist) + (zDist * zDist));
        return dist > (radius + (radius * 0.15));
    }
}
