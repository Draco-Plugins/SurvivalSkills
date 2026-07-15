package sir_draco.survivalskills.trophy;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.god_questline.GodTrophyEffects;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrophyEffectsGodLifecycleTest {

    @Test
    void cleanupRetainsGodEffectReferenceForResumeAndTerminalReconciliation() {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        when(plugin.getName()).thenReturn("SurvivalSkills");
        Trophy trophy = mock(Trophy.class);
        TrophyEffects effects = new TrophyEffects(plugin, new Location(mock(World.class), 0, 64, 0),
                TrophyType.GOD, trophy, "Player", UUID.randomUUID());
        GodTrophyEffects godTrophyEffects = mock(GodTrophyEffects.class);
        effects.setGodTrophy(godTrophyEffects);

        effects.removeItem();

        verify(godTrophyEffects).remove();
        assertSame(godTrophyEffects, effects.getGodTrophy());
    }
}
