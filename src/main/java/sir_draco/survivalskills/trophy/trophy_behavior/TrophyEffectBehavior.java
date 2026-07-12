package sir_draco.survivalskills.trophy.trophy_behavior;

import org.bukkit.Location;
import org.bukkit.World;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.trophy.TrophyEffects;

public interface TrophyEffectBehavior {

    int tick(TrophyEffects effects, int cycle, World world, Location loc);

    void start(TrophyEffects effects, Location loc, World world, SurvivalSkills plugin);

    void cleanup();
}
