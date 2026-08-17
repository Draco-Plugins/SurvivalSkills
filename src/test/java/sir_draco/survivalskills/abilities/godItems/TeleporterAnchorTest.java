package sir_draco.survivalskills.abilities.godItems;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import sir_draco.survivalskills.SurvivalSkills;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class TeleporterAnchorTest {

    @Test
    void crossWorldLocationsHaveNoMeasurableDistance() {
        World overworld = mock(World.class);
        World nether = mock(World.class);
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        when(plugin.getName()).thenReturn("SurvivalSkills");

        try (MockedStatic<SurvivalSkills> survivalSkills = mockStatic(SurvivalSkills.class)) {
            survivalSkills.when(SurvivalSkills::getInstance).thenReturn(plugin);

            assertTrue(TeleporterAnchor.calculateDistance(
                    new Location(nether, 0, 64, 0),
                    new Location(overworld, 100, 64, 100)).isEmpty());
        }
    }
}
