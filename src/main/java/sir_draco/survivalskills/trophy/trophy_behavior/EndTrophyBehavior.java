package sir_draco.survivalskills.trophy.trophy_behavior;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.trophy.TrophyEffects;

public class EndTrophyBehavior implements TrophyEffectBehavior {

    private Entity mob;

    @Override
    public int tick(TrophyEffects effects, int cycle, World world, Location loc) {
        if (mob != null) {
            mob.setRotation((float) (Math.random() * 360), (float) (Math.random() * 180));
        }

        if (cycle % 3 == 0) {
            effects.floorParticles(Particle.WITCH);
            Location tp = loc.clone().add(Math.random(), 1.0 + (Math.random() - 0.5), Math.random());
            effects.getDisplayItem().teleport(tp);
        }

        if (cycle == 18) {
            double chance = Math.random();
            if (mob != null && chance < 0.4) {
                mob.remove();
                mob = null;
            } else if (mob == null && chance < 0.1) {
                mob = world.spawnEntity(loc.clone().add(0.5, 1.0, 0.5), EntityType.ENDERMAN);
                Enderman eman = (Enderman) mob;
                eman.setPersistent(true);
                eman.setGravity(false);
                eman.setVelocity(new Vector((Math.random() - 0.5) * 0.5, 0.1, (Math.random() - 0.5) * 0.5));
                eman.setCollidable(false);
                eman.setInvulnerable(true);
                eman.setAI(false);
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
        if (mob != null) {
            mob.remove();
            mob = null;
        }
    }
}
