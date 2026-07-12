package sir_draco.survivalskills.trophy.trophy_behavior;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.trophy.TrophyEffects;

public class NetherTrophyBehavior implements TrophyEffectBehavior {

    @Override
    public int tick(TrophyEffects effects, int cycle, World world, Location loc) {
        if (cycle == 3) {
            effects.floorParticles(Particle.FLAME);
            world.spawnParticle(Particle.LAVA, loc.clone().add(0.5, 0.0, 0.5), 3);
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
}
