package sir_draco.survivalskills.Abilities;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

public class GraveTimer extends BukkitRunnable {

    private final Grave grave;
    private int timeLeft;

    public GraveTimer(Grave grave) {
        this.grave = grave;
        timeLeft = grave.getLifespan();
    }

    @Override
    public void run() {
        Block block = grave.getLocation().getBlock();
        if (!block.getType().equals(Material.CHEST)) {
            block.setType(Material.CHEST);
            block.getState().setType(Material.CHEST);
            block.getState().update();
        }
        if (timeLeft > 0) {
            timeLeft--;
            if (timeLeft % 2 == 0) spawnParticles();
            if (timeLeft == 1800) grave.graveWarning(timeLeft);
            else if (timeLeft == 600) grave.graveWarning(timeLeft);
            else if (timeLeft == 300) grave.graveWarning(timeLeft);
            else if (timeLeft == 60) grave.graveWarning(timeLeft);
            return;
        }

        grave.removeGrave(true);
        cancel();
    }

    public void spawnParticles() {
        // Spawn a ring of particles emitting from the center of the grave location
        World world = grave.getLocation().getWorld();
        if (world == null) return;
        double radius = 1.0;
        int numParticles = 20;

        for (int i = 0; i < numParticles; i++) {
            double angle = 2 * Math.PI * i / numParticles;
            world.spawnParticle(Particle.HAPPY_VILLAGER, grave.getLocation().clone().add(0.5, 0.5, 0.5), 1,
                    radius * Math.cos(angle), 0, radius * Math.sin(angle));
        }
    }
}
