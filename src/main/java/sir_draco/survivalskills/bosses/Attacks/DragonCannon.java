package sir_draco.survivalskills.bosses.attacks;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.ProjectileCalculator;

public class DragonCannon {

    private Location targetLocation;
    private Location originLocation;

    public DragonCannon(Location targetLocation, Location originLocation) {
        this.targetLocation = targetLocation;
        this.originLocation = originLocation;
    }

    public void cannon() {
        new BukkitRunnable() {
            private final Vector vec = ProjectileCalculator.getNoGravityVector(originLocation, targetLocation, 1.0);;

            @Override
            public void run() {
                if (originLocation.getWorld() == null) {
                    cancel();
                    return;
                }

                if (originLocation.distance(targetLocation) < 1) {
                    originLocation.getWorld().createExplosion(originLocation, 3, false, false);
                    cancel();
                    return;
                }
                originLocation.add(vec);
                originLocation.getWorld().spawnParticle(Particle.EXPLOSION, originLocation, 3);
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);
    }
}
