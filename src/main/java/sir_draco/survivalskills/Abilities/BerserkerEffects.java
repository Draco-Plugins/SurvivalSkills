package sir_draco.survivalskills.Abilities;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class BerserkerEffects extends BukkitRunnable {

    private final Player p;
    private final Particle.DustOptions red = new Particle.DustOptions(org.bukkit.Color.RED, 1);
    private int duration;

    public BerserkerEffects(Player p, int duration) {
        this.p = p;
        this.duration = duration * 4;
    }

    @Override
    public void run() {
        duration--;
        if (duration <= 0) {
            p.playSound(p, Sound.BLOCK_LAVA_EXTINGUISH, 1, 1);
            cancel();
            return;
        }

        World world = p.getWorld();
        for (int i = 0; i < 5; i++) {
            // Create a random direction for the particles to go
            double x = Math.random() - 0.5;
            double y = (Math.random() + 1.0) * 0.5;
            double z = Math.random() - 0.5;

            // Create the particle effect using red particles with the random directions for velocity from above
            world.spawnParticle(org.bukkit.Particle.DUST, p.getLocation(), 1, x, y, z, 1, red);
        }
    }
}
