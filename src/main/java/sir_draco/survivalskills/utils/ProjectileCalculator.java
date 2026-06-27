package sir_draco.survivalskills.utils;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

public final class ProjectileCalculator {

    private static final double GRAVITY = -0.08;
    private static final double ITEM_GRAVITY = -0.04;
    private static final double DRAG = 0.91;
    private static final double ITEM_VERTICAL_DRAG = 0.98;
    private static final double LIVING_ENTITY_VERTICAL_DRAG = 0.98;
    private static final double BLOCK_SLIPPERINESS = 0.6;
    private static final double LIVING_ENTITY_MINIMUM_VERTICAL_SPEED = 0.51;
    private static final double LOW_VERTICAL_DAMPING_THRESHOLD = 0.60;
    private static final double LOW_VERTICAL_HORIZONTAL_DAMPING = 0.16;
    private static final double LOW_VERTICAL_INITIAL_HORIZONTAL_TICKS = 2.0;
    private static final int MAX_FLIGHT_TICKS = 200;

    private ProjectileCalculator() {}

    /**
     * Calculates the initial velocity vector needed for a living entity projectile
     * to travel from {@code start} to {@code end} at the given {@code magnitude},
     * accounting for gravity, drag, block slipperiness, and minimum vertical velocity
     * constraints specific to living entities.
     *
     * @param start     the starting location
     * @param end       the target location
     * @param magnitude the scalar speed of the projectile
     * @return a {@link Vector} with the initial velocity components
     */
    public static Vector getLivingEntityProjectileVector(Location start, Location end, double magnitude, boolean friction) {
        if (magnitude <= 0) {
            throw new IllegalArgumentException("[Survival Skills] Magnitude must be greater than 0");
        }

        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double dz = end.getZ() - start.getZ();

        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        if (horizontalDistance < 1) {
            return new Vector(0, Math.log(Math.abs(dy)), 0);
        }

        // Use whole ticks because Minecraft movement is applied discretely.
        int ticks = Math.max(1, (int) Math.ceil(horizontalDistance / magnitude));
        ticks = getMinimumVerticalVelocityTicks(dy, ticks);

        double vy = getRequiredVerticalVelocity(dy, ticks, GRAVITY);
        double dragSumAfterFirstTick = geometricSum(DRAG, ticks - 1);

        /*
         * Horizontal motion with drag:
         *
         * displacement = v0 * (1 + drag + drag^2 + ... + drag^(ticks - 1))
         *
         * so:
         *
         * v0 = displacement / dragSum
         * 
         * Accounting for BLOCK_SLIPPERINESS as on tick 1 the player is touching the ground so surface friction is applied initially
         */
        double frictionCoefficent = friction ? BLOCK_SLIPPERINESS * DRAG : DRAG;
        double horizontalDragSum = getHorizontalDragSum(dragSumAfterFirstTick, vy, frictionCoefficent);
        double vx = dx / horizontalDragSum;
        double vz = dz / horizontalDragSum;

        /*
         * Vertical recurrence:
         *
         * y += vy
         * vy = (vy + gravity) * drag
         *
         * Total vertical displacement after n ticks:
         *
         * dy = vy0 * dragSum + gravity * gravitySum
         *
         * where:
         *
         * gravitySum = sum from i=0 to n-1 of sum from j=0 to i-1 drag^j
         *
         * Simplified:
         *
         * gravitySum = (n - dragSum) / (1 - drag)
         */
        return new Vector(vx, vy, vz);
    }

    public static Vector getItemProjectileVector(Location start, Location end, double magnitude) {
        if (magnitude <= 0) {
            throw new IllegalArgumentException("[Survival Skills] Magnitude must be greater than 0");
        }

        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double dz = end.getZ() - start.getZ();

        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        if (horizontalDistance < 1) {
            return new Vector(0, Math.log(Math.abs(dy)), 0);
        }

        // Use whole ticks because Minecraft movement is applied discretely.
        int ticks = Math.max(1, (int) Math.ceil(horizontalDistance / magnitude));

        double vy = getRequiredVerticalVelocity(dy, ticks, ITEM_GRAVITY, ITEM_VERTICAL_DRAG);

        /*
         * Horizontal motion with drag:
         *
         * displacement = v0 * (1 + drag + drag^2 + ... + drag^(ticks - 1))
         *
         * so:
         *
         * v0 = displacement / dragSum
         */
        double horizontalDragSum = geometricSum(ITEM_VERTICAL_DRAG, ticks);
        double vx = dx / horizontalDragSum;
        double vz = dz / horizontalDragSum;

        /*
         * Vertical recurrence:
         *
         * y += vy
         * vy = (vy + gravity) * drag
         *
         * Total vertical displacement after n ticks:
         *
         * dy = vy0 * dragSum + gravity * gravitySum
         *
         * where:
         *
         * gravitySum = sum from i=0 to n-1 of sum from j=0 to i-1 drag^j
         *
         * Simplified:
         *
         * gravitySum = (n - dragSum) / (1 - drag)
         */
        return new Vector(vx, vy, vz);
    }

    public static Vector getNoGravityVector(Location start, Location end, double magnitude) {
        double dx = (end.getX() - start.getX());
        double dy = (end.getY() - start.getY());
        double dz = (end.getZ() - start.getZ());
        double distance = Math.sqrt((dx * dx) + (dz * dz));
        if (distance == 0 && dy == 0) return new Vector(0, 0, 0);
        if (distance == 0) return new Vector(0, Math.signum(dy) * magnitude, 0);

        // Normalize the direction vector
        double vx = magnitude * dx / distance;
        double vy = magnitude * dy / distance;
        double vz = magnitude * dz / distance;
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


    private static double geometricSum(double drag, int ticks) {
        if (ticks <= 0) return 0.0D;
        if (Math.abs(drag - 1.0D) < 1.0E-9) return ticks;
        return (1.0D - Math.pow(drag, ticks)) / (1.0D - drag);
    }

    private static int getMinimumVerticalVelocityTicks(double dy, int initialTicks) {
        int ticks = initialTicks;
        while (ticks < MAX_FLIGHT_TICKS
                && getRequiredVerticalVelocity(dy, ticks, GRAVITY) < LIVING_ENTITY_MINIMUM_VERTICAL_SPEED) {
            ticks++;
        }
        return ticks;
    }

    private static double getRequiredVerticalVelocity(double dy, int ticks, double gravity) {
        return getRequiredVerticalVelocity(dy, ticks, gravity, LIVING_ENTITY_VERTICAL_DRAG);
    }

    private static double getRequiredVerticalVelocity(double dy, int ticks, double gravity, double drag) {
        double verticalDragSum = geometricSum(drag, ticks);
        double terminalVelocity = getTerminalVelocity(gravity, drag);
        return (dy + terminalVelocity * ticks) / verticalDragSum - terminalVelocity;
    }

    private static double getTerminalVelocity(double gravity, double drag) {
        return -gravity * drag / (1.0D - drag);
    }

    private static double getHorizontalDragSum(double dragSumAfterFirstTick, double verticalVelocity, double drag) {
        if (verticalVelocity >= LOW_VERTICAL_DAMPING_THRESHOLD) {
            return 1.0D + drag * dragSumAfterFirstTick;
        }

        return LOW_VERTICAL_INITIAL_HORIZONTAL_TICKS
                + LOW_VERTICAL_HORIZONTAL_DAMPING * drag * dragSumAfterFirstTick;
    }
}
