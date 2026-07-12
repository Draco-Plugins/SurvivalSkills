package sir_draco.survivalskills.trophy.trophy_behavior;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.trophy.TrophyEffects;

public class CaveTrophyBehavior implements TrophyEffectBehavior {

    private static final Color[] CAVE_COLORS = {
            Color.fromRGB(0, 255, 255),
            Color.GRAY,
            Color.RED,
            Color.BLACK,
            Color.GREEN,
            Color.BLUE,
            Color.WHITE,
            Color.fromRGB(156, 120, 2),
            Color.fromRGB(255, 255, 0)
    };

    @Override
    public int tick(TrophyEffects effects, int cycle, World world, Location loc) {
        double xOffset = 0.25 + ((Math.random() - 0.5) * 2.0);
        double yOffset = Math.random() + 1.5;
        double zOffset = 0.25 + ((Math.random() - 0.5) * 2.0);

        if (cycle == 10) {
            Location location = new Location(loc.getWorld(), loc.getX() + xOffset,
                    loc.getY() + yOffset, loc.getZ() + zOffset);
            effects.drawCube(location, randomCaveColor(), 0.5, 0.1);
        }
        if (cycle % 3 == 0) {
            effects.floorParticles(Particle.WITCH);
        }
        if (cycle == 10) {
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

    private Color randomCaveColor() {
        return CAVE_COLORS[(int) (Math.random() * CAVE_COLORS.length)];
    }
}
