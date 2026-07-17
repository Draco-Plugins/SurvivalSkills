package sir_draco.survivalskills.god_questline.trial;

import org.bukkit.Location;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrialPhantomContainmentTest {
    private static final BoundingBox CAGE = new BoundingBox(0, 50, 0, 40, 80, 40);

    @Test
    void acceptsPhantomInsideInsetCage() {
        assertTrue(Trial.containsPhantom(CAGE, new Location(null, 20, 65, 20)));
        assertTrue(Trial.containsPhantom(CAGE, new Location(null, 1.5, 51.5, 1.5)));
    }

    @Test
    void rejectsPhantomAtOrBeyondCageWalls() {
        assertFalse(Trial.containsPhantom(CAGE, new Location(null, 1.49, 65, 20)));
        assertFalse(Trial.containsPhantom(CAGE, new Location(null, 38.51, 65, 20)));
        assertFalse(Trial.containsPhantom(CAGE, new Location(null, 20, 78.51, 20)));
        assertFalse(Trial.containsPhantom(CAGE, new Location(null, 20, 65, 38.51)));
    }
}
