package sir_draco.survivalskills.god_questline.powerore;

import org.bukkit.*;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.function.Supplier;

/**
 * Manages the orbiting particle effect around a Power Ore block.
 * The effect color and extra spark behaviour depend on the challenge status
 * supplied by {@code statusSupplier}.
 */
public class PowerOreVisualEffect {

    private static final int ORBIT_PERIOD_TICKS = 40;
    private static final double ORBIT_RADIUS = 0.5;
    private static final double PARTICLE_Y_OFFSET = 1.1;
    private static final double HOVER_Y_OFFSET = 1.2;
    private static final int ELECTRIC_SPARK_INTERVAL = 5;
    private static final int SPARK_COUNT = 3;
    private static final double SPARK_OFFSET = 0.1;
    private static final double SPARK_Y_SPREAD = 0.2;
    private static final double SPARK_SPEED = 0.01;
    private static final double BLOCK_CENTER_OFFSET = 0.5;

    private final Location oreLocation;
    private final Supplier<PowerOreChallenge.Status> statusSupplier;
    private BukkitRunnable task;

    public PowerOreVisualEffect(Location oreLocation,
                                Supplier<PowerOreChallenge.Status> statusSupplier) {
        this.oreLocation = oreLocation;
        this.statusSupplier = statusSupplier;
    }

    /** Starts the repeating particle task. */
    public void start() {
        stop();
        task = new BukkitRunnable() {
            private int tick;

            @Override
            public void run() {
                World world = oreLocation.getWorld();
                if (world == null) {
                    cancel();
                    return;
                }

                PowerOreChallenge.Status status = statusSupplier.get();
                if (status == PowerOreChallenge.Status.FAILED) {
                    cancel();
                    return;
                }

                if (oreLocation.getBlock().getType() != Material.OBSIDIAN) {
                    cancel();
                    return;
                }

                tick++;
                double angle = (tick % ORBIT_PERIOD_TICKS) / (double) ORBIT_PERIOD_TICKS * 2 * Math.PI;
                double cx = oreLocation.getX() + BLOCK_CENTER_OFFSET;
                double cz = oreLocation.getZ() + BLOCK_CENTER_OFFSET;
                double x = cx + ORBIT_RADIUS * Math.cos(angle);
                double z = cz + ORBIT_RADIUS * Math.sin(angle);
                double y = oreLocation.getY() + PARTICLE_Y_OFFSET;

                if (status == PowerOreChallenge.Status.RUNNING) {
                    world.spawnParticle(Particle.DUST, x, y, z, 1,
                            new Particle.DustOptions(Color.RED, 1));
                } else if (status == PowerOreChallenge.Status.SUCCESS) {
                    world.spawnParticle(Particle.DUST, x, y, z, 1,
                            new Particle.DustOptions(Color.GREEN, 1));
                    if (tick % ELECTRIC_SPARK_INTERVAL == 0) {
                        world.spawnParticle(Particle.ELECTRIC_SPARK,
                                cx, oreLocation.getY() + HOVER_Y_OFFSET, cz,
                                SPARK_COUNT, SPARK_OFFSET, SPARK_Y_SPREAD, SPARK_OFFSET, SPARK_SPEED);
                    }
                }
            }
        };
        task.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);
    }

    /** Stops the repeating particle task if it is running. */
    public void stop() {
        if (task != null) {
            try {
                task.cancel();
            } catch (IllegalStateException e) {
                // Expected when the Bukkit scheduler has already been shut down
                // (e.g. during server reload / shutdown).
            }
        }
        task = null;
    }
}
