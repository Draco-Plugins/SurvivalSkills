package sir_draco.survivalskills.trophy.trophy_behavior;

import org.bukkit.Location;
import org.bukkit.World;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.god_questline.GodTrophyEffects;
import sir_draco.survivalskills.trophy.TrophyEffects;

public class GodTrophyBehavior implements TrophyEffectBehavior {

    @Override
    public int tick(TrophyEffects effects, int cycle, World world, Location loc) {
        GodTrophyEffects godTrophy = effects.getGodTrophy();
        if (!effects.isCitizensEnabled()) {
            return cycle;
        }
        if (cycle == 1 || godTrophy == null) {
            godTrophy = new GodTrophyEffects(loc, effects.getPlayerName(), effects.getPlayerUUID());
            effects.setGodTrophy(godTrophy);
        }
        godTrophy.tickTrophy(cycle);
        return cycle;
    }

    @Override
    public void start(TrophyEffects effects, Location loc, World world, SurvivalSkills plugin) {
    }

    @Override
    public void cleanup() {
    }
}
