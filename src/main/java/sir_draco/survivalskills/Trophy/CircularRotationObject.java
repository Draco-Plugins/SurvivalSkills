package sir_draco.survivalskills.Trophy;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public class CircularRotationObject {

    private final ArrayList<Location> locationList = new ArrayList<>();
    private final int orbitals;
    private double radius;
    private Location center;

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
            if (radians > 2 * Math.PI) radians = radians % (2 * Math.PI);
            locationList.add(new Location(world, x + (Math.cos(radians) * radius), y, z + (Math.sin(radians) * radius)));
        }
    }

    public void setCenter(Location loc) {
        center = loc.clone();
        createLocations(0); // Reset locations
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
        double angle = 0;
        double xDist = x - cx;
        double zDist = z - cz;
        // 90, 270 degrees
        if (xDist == 0) {
            if (zDist > 0) angle = Math.PI / 2;
            else angle = 3 * (Math.PI) / 2;
        }
        // 0, 180 degrees
        else if (zDist == 0) {
            if (xDist > 0) angle = 0;
            else angle = Math.PI;
        }
        else if (xDist < 0) {
            angle = Math.tan(zDist / xDist) + Math.PI;
        }
        else {
            if (zDist > 0) angle = Math.tan(zDist / xDist);
            else angle = (2 * Math.PI) + Math.tan(zDist / xDist);
        }
        return angle % (2 * Math.PI);
    }

    public Location getLocation(int slot) {
        return locationList.get(slot);
    }

    public boolean tooFar(Location orbital) {
        double xDist = orbital.getX() - center.getX();
        double zDist = orbital.getZ() - center.getZ();
        double dist = Math.sqrt((xDist * xDist) + (zDist * zDist));
        return dist > (radius + (radius * 0.15));
    }
}
