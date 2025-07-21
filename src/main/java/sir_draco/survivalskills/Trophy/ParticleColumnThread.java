package sir_draco.survivalskills.Trophy;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

public class ParticleColumnThread extends BukkitRunnable {

    private final Location loc;
    private final World world;
    private final Particle.DustOptions dust;
    private final double max;
    private final double step;
    private double y;

    public ParticleColumnThread(Location loc, Particle.DustOptions dust, double max, double step) {
        this.loc = loc;
        this.world = loc.getWorld();
        this.dust = dust;
        this.max = max;
        this.step = step;
    }

    @Override
    public void run() {
        if (y > max) {
            this.cancel();
            return;
        }

        Location newLoc = loc.clone().add(0.0, y, 0.0);
        world.spawnParticle(Particle.DUST, newLoc, 1, dust);
        y += step;
    }
}
