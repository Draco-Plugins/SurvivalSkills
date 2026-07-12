package sir_draco.survivalskills.trophy.trophy_behavior;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.trophy.TrophyEffects;

public class OceanTrophyBehavior implements TrophyEffectBehavior {

    @Override
    public int tick(TrophyEffects effects, int cycle, World world, Location loc) {
        if (cycle % 3 == 0) {
            effects.floorParticles(Particle.SPLASH);
            bubbles(world, loc);
        }

        if (cycle == 10) {
            for (int i = 1; i <= 5; i++) {
                double xOff = (Math.random() - 0.5) * 0.5;
                double yOff = (Math.random() - 0.5) * 0.5;
                double zOff = (Math.random() - 0.5) * 0.5;
                Location newLoc = loc.clone().add(0.5, 1.0, 0.5);
                world.spawnParticle(Particle.CRIT, newLoc, 1, xOff, yOff, zOff);
            }
            return 1;
        }

        return cycle;
    }

    @Override
    public void start(TrophyEffects effects, Location loc, World world, SurvivalSkills plugin) {
    }

    @Override
    public void cleanup() {
    }

    private void bubbles(World world, Location loc) {
        for (int i = 1; i <= 30; i++) {
            double xOff = Math.random();
            double yOff = Math.random() * 3.0;
            double zOff = Math.random();
            Location newLoc = loc.clone().add(xOff, yOff, zOff);
            world.spawnParticle(Particle.BUBBLE_COLUMN_UP, newLoc, 1);
        }
    }
}
