package sir_draco.survivalskills.god_questline.powerore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PowerOreChickenHerdingGameTest {

    @Test
    void startsWithFortyChickensAndSixtySeconds() {
        PowerOreChickenHerdingGame game = new PowerOreChickenHerdingGame();

        assertEquals(50, game.getChickensRemaining());
        assertEquals(60, game.getSecondsRemaining());
        assertFalse(game.isComplete());
        assertFalse(game.isExpired());
    }

    @Test
    void completesOnlyAfterAllChickensAreDelivered() {
        PowerOreChickenHerdingGame game = new PowerOreChickenHerdingGame();

        game.deliverChickens(49);
        assertEquals(1, game.getChickensRemaining());
        assertFalse(game.isComplete());

        game.deliverChickens(1);
        assertEquals(0, game.getChickensRemaining());
        assertTrue(game.isComplete());
    }

    @Test
    void expiresAfterExactlySixtySeconds() {
        PowerOreChickenHerdingGame game = new PowerOreChickenHerdingGame();

        game.advanceTimer(PowerOreChickenHerdingGame.DURATION_TICKS - 1);
        assertEquals(1, game.getSecondsRemaining());
        assertFalse(game.isExpired());

        game.advanceTimer(1);
        assertEquals(0, game.getSecondsRemaining());
        assertTrue(game.isExpired());
    }

    @Test
    void rejectsNegativeProgressAndElapsedTime() {
        PowerOreChickenHerdingGame game = new PowerOreChickenHerdingGame();

        assertThrows(IllegalArgumentException.class, () -> game.deliverChickens(-1));
        assertThrows(IllegalArgumentException.class, () -> game.advanceTimer(-1));
    }
}
