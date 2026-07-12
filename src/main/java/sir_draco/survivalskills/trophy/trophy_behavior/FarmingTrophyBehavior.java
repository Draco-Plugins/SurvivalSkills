package sir_draco.survivalskills.trophy.trophy_behavior;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.trophy.TrophyEffects;

public class FarmingTrophyBehavior implements TrophyEffectBehavior {

    @Override
    public int tick(TrophyEffects effects, int cycle, World world, Location loc) {
        if (cycle == 3) {
            effects.floorParticles(Particle.HAPPY_VILLAGER);
            rainfall(world, loc);
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

    private void rainfall(World world, Location loc) {
        Location location = loc.clone().add(0., 2.0, 0.);
        for (double x = 0.0; x <= 1.0; x += 0.2) {
            for (double z = 0.0; z <= 1.0; z += 0.2) {
                Location finalLoc = location.clone().add(x, 0., z);
                world.spawnParticle(Particle.DRIPPING_WATER, finalLoc, 1, 0.0, -0.1, 0.0);
                finalLoc.add(0., 0.25, 0.);
                world.spawnParticle(Particle.CLOUD, finalLoc, 0, 0.0, 0.0, 0.0);
            }
        }
    }
}
