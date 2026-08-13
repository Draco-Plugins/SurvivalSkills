package sir_draco.survivalskills.god_questline.powerore;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static sir_draco.survivalskills.god_questline.powerore.PowerOreMemoryMatchGame.SelectionOutcome.FIRST_REVEALED;
import static sir_draco.survivalskills.god_questline.powerore.PowerOreMemoryMatchGame.SelectionOutcome.GAME_COMPLETE;
import static sir_draco.survivalskills.god_questline.powerore.PowerOreMemoryMatchGame.SelectionOutcome.IGNORED;
import static sir_draco.survivalskills.god_questline.powerore.PowerOreMemoryMatchGame.SelectionOutcome.MATCH;
import static sir_draco.survivalskills.god_questline.powerore.PowerOreMemoryMatchGame.SelectionOutcome.MISMATCH;
import static sir_draco.survivalskills.god_questline.powerore.PowerOreMemoryMatchGame.SelectionOutcome.ROUND_COMPLETE;

class PowerOreMemoryMatchGameTest {

    @Test
    void roundsUseRequestedDimensions() {
        assertEquals(List.of(
                new PowerOreMemoryMatchGame.RoundDefinition(3, 4),
                new PowerOreMemoryMatchGame.RoundDefinition(5, 4)),
                PowerOreMemoryMatchGame.getRoundDefinitions());
        assertEquals(List.of(12, 20), PowerOreMemoryMatchGame.getRoundDefinitions().stream()
                .map((PowerOreMemoryMatchGame.RoundDefinition definition) -> definition.tileCount())
                .toList());
    }

    @Test
    void eachRoundContainsExactlyTwoOfEveryPair() {
        PowerOreMemoryMatchGame game = new PowerOreMemoryMatchGame(new Random(42));

        for (int roundIndex = 0; roundIndex < PowerOreMemoryMatchGame.getRoundDefinitions().size(); roundIndex++) {
            Map<Integer, List<Integer>> positionsByPair = positionsByPair(game);
            assertEquals(game.getRoundDefinition().tileCount() / 2, positionsByPair.size());
            assertTrue(positionsByPair.values().stream()
                    .allMatch((List<Integer> positions) -> positions.size() == 2));
            completeRound(game);
            if (roundIndex < PowerOreMemoryMatchGame.getRoundDefinitions().size() - 1)
                game.advanceRound();
        }
    }

    @Test
    void invalidSelectionsDoNotChangeState() {
        PowerOreMemoryMatchGame game = new PowerOreMemoryMatchGame(new Random(7));
        List<Integer> matchingPositions = positionsByPair(game).values().iterator().next();
        int firstPosition = matchingPositions.get(0);
        int secondPosition = matchingPositions.get(1);

        assertEquals(IGNORED, game.select(firstPosition).outcome());
        game.beginMatching();
        assertEquals(IGNORED, game.select(-1).outcome());
        assertEquals(IGNORED, game.select(game.getRoundDefinition().tileCount()).outcome());
        assertEquals(FIRST_REVEALED, game.select(firstPosition).outcome());
        assertEquals(IGNORED, game.select(firstPosition).outcome());
        assertEquals(MATCH, game.select(secondPosition).outcome());
        assertTrue(game.isMatched(firstPosition));
        assertTrue(game.isMatched(secondPosition));
        assertEquals(IGNORED, game.select(firstPosition).outcome());
        assertEquals(3, game.getLivesRemaining());

        List<Integer> mismatch = findMismatch(game);
        assertEquals(FIRST_REVEALED, game.select(mismatch.get(0)).outcome());
        assertEquals(MISMATCH, game.select(mismatch.get(1)).outcome());
        assertEquals(IGNORED, game.select(mismatch.get(0)).outcome());
        assertEquals(2, game.getLivesRemaining());
        game.resolveMismatch();
        assertEquals(FIRST_REVEALED, game.select(mismatch.get(0)).outcome());
    }

    @Test
    void livesResetForEachRound() {
        PowerOreMemoryMatchGame game = new PowerOreMemoryMatchGame(new Random(12));
        game.beginMatching();

        makeMismatch(game, MISMATCH);
        assertEquals(2, game.getLivesRemaining());
        game.resolveMismatch();
        completeRound(game);
        assertEquals(0, game.getRoundIndex());
        game.advanceRound();
        game.beginMatching();

        makeMismatch(game, MISMATCH);
        assertEquals(2, game.getLivesRemaining());
        game.resolveMismatch();
        completeRound(game);
    }

    @Test
    void matchingAllRoundsCompletesGame() {
        PowerOreMemoryMatchGame game = new PowerOreMemoryMatchGame(new Random(99));

        game.beginMatching();
        assertEquals(ROUND_COMPLETE, completeRound(game));
        game.advanceRound();
        assertEquals(1, game.getRoundIndex());
        assertEquals(3, game.getLivesRemaining());

        game.beginMatching();
        assertEquals(GAME_COMPLETE, completeRound(game));
        assertEquals(IGNORED, game.select(0).outcome());
        assertThrows(IllegalStateException.class, game::advanceRound);
    }

    private static PowerOreMemoryMatchGame.SelectionOutcome completeRound(PowerOreMemoryMatchGame game) {
        game.beginMatching();
        PowerOreMemoryMatchGame.SelectionOutcome lastOutcome = IGNORED;
        for (List<Integer> positions : positionsByPair(game).values()) {
            if (game.isMatched(positions.get(0)))
                continue;
            assertEquals(FIRST_REVEALED, game.select(positions.get(0)).outcome());
            lastOutcome = game.select(positions.get(1)).outcome();
            assertTrue(MATCH.equals(lastOutcome)
                    || ROUND_COMPLETE.equals(lastOutcome)
                    || GAME_COMPLETE.equals(lastOutcome));
        }
        return lastOutcome;
    }

    private static void makeMismatch(PowerOreMemoryMatchGame game,
                                     PowerOreMemoryMatchGame.SelectionOutcome expectedOutcome) {
        List<Integer> mismatch = findMismatch(game);
        assertEquals(FIRST_REVEALED, game.select(mismatch.get(0)).outcome());
        assertEquals(expectedOutcome, game.select(mismatch.get(1)).outcome());
        assertFalse(game.isMatched(mismatch.get(0)));
        assertFalse(game.isMatched(mismatch.get(1)));
    }

    private static List<Integer> findMismatch(PowerOreMemoryMatchGame game) {
        for (int firstPosition = 0; firstPosition < game.getRoundDefinition().tileCount(); firstPosition++) {
            if (game.isMatched(firstPosition))
                continue;
            for (int secondPosition = firstPosition + 1;
                 secondPosition < game.getRoundDefinition().tileCount(); secondPosition++) {
                if (!game.isMatched(secondPosition)
                        && game.getPairIdentifier(firstPosition) != game.getPairIdentifier(secondPosition))
                    return List.of(firstPosition, secondPosition);
            }
        }
        throw new IllegalStateException("No mismatched positions remain");
    }

    private static Map<Integer, List<Integer>> positionsByPair(PowerOreMemoryMatchGame game) {
        Map<Integer, List<Integer>> positions = new HashMap<>();
        for (int position = 0; position < game.getRoundDefinition().tileCount(); position++) {
            positions.computeIfAbsent(game.getPairIdentifier(position), (Integer ignored) -> new ArrayList<>())
                    .add(position);
        }
        return positions;
    }
}
