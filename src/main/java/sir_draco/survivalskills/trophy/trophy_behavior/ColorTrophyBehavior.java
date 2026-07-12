package sir_draco.survivalskills.trophy.trophy_behavior;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Particle;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.trophy.ParticleColumnThread;
import sir_draco.survivalskills.trophy.TrophyEffects;
import sir_draco.survivalskills.utils.ColorParser;

import java.util.ArrayList;
import java.util.List;

public class ColorTrophyBehavior implements TrophyEffectBehavior {

    private final ArrayList<Color> colorList = new ArrayList<>();

    @Override
    public int tick(TrophyEffects effects, int cycle, World world, Location loc) {
        floorColor(cycle, loc, effects);

        if (cycle == 8 || cycle == 16) {
            double chance = Math.random();
            if (chance < 0.5) {
                Location newLoc = loc.clone().add(
                        0.5 + (Math.random() - 0.5),
                        2.0 + Math.random(),
                        0.5 + (Math.random() - 0.5)
                );
                Color color = colorList.get((int) (Math.random() * colorList.size()));
                effects.spawnFireworkEffect(newLoc, color, 1.5, 0.3);
            }
            if (cycle == 16) {
                return 0;
            }
        }

        return cycle;
    }

    @Override
    public void start(TrophyEffects effects, Location loc, World world, SurvivalSkills plugin) {
        if (!colorList.isEmpty()) {
            colorList.clear();
        }
        List<List<String>> colors = new ArrayList<>();
        colors.add(ColorParser.generateGradient("#FF0000", "#00FF00", 6));
        colors.add(ColorParser.generateGradient("#00FF00", "#0000FF", 6));
        colors.add(ColorParser.generateGradient("#0000FF", "#FF00FF", 5));
        List<String> hexColors = ColorParser.gradientConnector(colors);
        for (String hex : hexColors) {
            colorList.add(ColorParser.hexToColor(hex));
        }
    }

    @Override
    public void cleanup() {
        colorList.clear();
    }

    private void floorColor(int column, Location loc, TrophyEffects effects) {
        World world = loc.getWorld();
        if (world == null) {
            return;
        }

        Particle.DustOptions dust = new Particle.DustOptions(colorList.get(column), 1f);
        Location newLoc;

        if (column <= 4) {
            newLoc = loc.clone().add(column / 4.0, 0.0, 0.0);
        } else if (column <= 8) {
            newLoc = loc.clone().add(1.0, 0.0, (column - 4) / 4.0);
        } else if (column <= 12) {
            newLoc = loc.clone().add(1.0 + (-1 * (column - 8) / 4.0), 0.0, 1.0);
        } else {
            newLoc = loc.clone().add(0.0, 0.0, 1.0 + (-1 * (column - 12) / 4.0));
        }

        new ParticleColumnThread(newLoc, dust, 1.0, 0.2).runTaskTimer(effects.getPlugin(), 0, 2);
    }
}
