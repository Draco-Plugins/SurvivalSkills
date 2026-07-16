package sir_draco.survivalskills.bosses.attacks;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import sir_draco.survivalskills.SurvivalSkills;

public class DeathRainProjectile {

    private Location projectileLoc;
    private Vector finalVec;

    public DeathRainProjectile(Location startLocation, Vector trajectory) {
        projectileLoc = startLocation;
        finalVec = trajectory;
    }

    public void launch() {
        new BukkitRunnable() {
            private final World world = projectileLoc.getWorld();

            @Override
            public void run() {
                if (projectileLoc.getY() <= -10) {
                    cancel();
                    return;
                }
                if (!projectileLoc.getBlock().isEmpty()) {
                    // Set the block above on fire
                    projectileLoc.add(0, 1, 0).getBlock().setType(Material.FIRE);
                    if (projectileLoc.getWorld() != null)
                        projectileLoc.getWorld().createExplosion(projectileLoc, 4, false, false);
                    cancel();
                    return;
                }

                world.spawnParticle(Particle.DUST, projectileLoc, 1, new Particle.DustOptions(Color.PURPLE, 1));
                projectileLoc.add(finalVec);

                // Slow down the movement in each direction
                if (finalVec.getX() < 0 && Math.abs(finalVec.getX()) >= 0.01)
                    finalVec.setX(finalVec.getX() + 0.01);
                if (finalVec.getX() >= 0.01)
                    finalVec.setX(finalVec.getX() - 0.01);
                if (finalVec.getZ() < 0 && Math.abs(finalVec.getZ()) >= 0.01)
                    finalVec.setZ(finalVec.getZ() + 0.01);
                if (finalVec.getZ() >= 0.01)
                    finalVec.setZ(finalVec.getZ() - 0.01);
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);
    }
}
