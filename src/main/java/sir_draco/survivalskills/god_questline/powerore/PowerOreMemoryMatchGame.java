package sir_draco.survivalskills.god_questline.powerore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

/** Owns the deterministic rules and state for a Power Ore Memory Match game. */
public final class PowerOreMemoryMatchGame {

    private static final int STARTING_LIVES = 3;
    private static final List<RoundDefinition> ROUND_DEFINITIONS = List.of(
            new RoundDefinition(3, 4),
            new RoundDefinition(5, 4));

    public enum SelectionOutcome {
        IGNORED,
        FIRST_REVEALED,
        MATCH,
        MISMATCH,
        ROUND_COMPLETE,
        GAME_COMPLETE,
        GAME_FAILED
    }

    private enum Phase {
        PREVIEW,
        MATCHING,
        RESOLVING_MISMATCH,
        ROUND_TRANSITION,
        COMPLETE,
        FAILED
    }

    public record RoundDefinition(int columns, int rows) {
        public RoundDefinition {
            if (columns < 1 || rows < 1 || (columns * rows) % 2 != 0)
                throw new IllegalArgumentException("Memory Match rounds require a positive, even number of tiles");
        }

        public int tileCount() {
            return columns * rows;
        }
    }

    public record SelectionResult(SelectionOutcome outcome, List<Integer> revealedPositions, int livesRemaining) {
        public SelectionResult {
            Objects.requireNonNull(outcome);
            revealedPositions = List.copyOf(revealedPositions);
        }
    }

    private final Random random;
    private final Set<Integer> matchedPositions = new HashSet<>();
    private int roundIndex;
    private int livesRemaining = STARTING_LIVES;
    private List<Integer> pairIdentifiers = List.of();
    private OptionalInt firstSelectedPosition = OptionalInt.empty();
    private Phase phase = Phase.PREVIEW;

    public PowerOreMemoryMatchGame(Random random) {
        this.random = Objects.requireNonNull(random);
        initializeRound();
    }

    public static List<RoundDefinition> getRoundDefinitions() {
        return ROUND_DEFINITIONS;
    }

    public RoundDefinition getRoundDefinition() {
        return ROUND_DEFINITIONS.get(roundIndex);
    }

    public int getRoundIndex() {
        return roundIndex;
    }

    public int getLivesRemaining() {
        return livesRemaining;
    }

    public int getPairIdentifier(int position) {
        return pairIdentifiers.get(position);
    }

    public boolean isMatched(int position) {
        return matchedPositions.contains(position);
    }

    public void beginMatching() {
        if (Phase.PREVIEW.equals(phase))
            phase = Phase.MATCHING;
    }

    public SelectionResult select(int position) {
        if (!Phase.MATCHING.equals(phase) || position < 0 || position >= pairIdentifiers.size())
            return ignoredResult();
        if (matchedPositions.contains(position))
            return ignoredResult();
        if (firstSelectedPosition.isPresent() && firstSelectedPosition.getAsInt() == position)
            return ignoredResult();

        if (firstSelectedPosition.isEmpty()) {
            firstSelectedPosition = OptionalInt.of(position);
            return result(SelectionOutcome.FIRST_REVEALED, List.of(position));
        }

        int firstPosition = firstSelectedPosition.getAsInt();
        firstSelectedPosition = OptionalInt.empty();
        List<Integer> revealedPositions = List.of(firstPosition, position);
        if (pairIdentifiers.get(firstPosition).equals(pairIdentifiers.get(position))) {
            matchedPositions.addAll(revealedPositions);
            if (matchedPositions.size() == pairIdentifiers.size()) {
                if (roundIndex == ROUND_DEFINITIONS.size() - 1) {
                    phase = Phase.COMPLETE;
                    return result(SelectionOutcome.GAME_COMPLETE, revealedPositions);
                }
                phase = Phase.ROUND_TRANSITION;
                return result(SelectionOutcome.ROUND_COMPLETE, revealedPositions);
            }
            return result(SelectionOutcome.MATCH, revealedPositions);
        }

        livesRemaining--;
        if (livesRemaining == 0) {
            phase = Phase.FAILED;
            return result(SelectionOutcome.GAME_FAILED, revealedPositions);
        }
        phase = Phase.RESOLVING_MISMATCH;
        return result(SelectionOutcome.MISMATCH, revealedPositions);
    }

    public void resolveMismatch() {
        if (Phase.RESOLVING_MISMATCH.equals(phase))
            phase = Phase.MATCHING;
    }

    public void advanceRound() {
        if (!Phase.ROUND_TRANSITION.equals(phase))
            throw new IllegalStateException("The current Memory Match round is not complete");
        roundIndex++;
        initializeRound();
    }

    private void initializeRound() {
        int pairCount = getRoundDefinition().tileCount() / 2;
        ArrayList<Integer> shuffledPairs = IntStream.range(0, pairCount)
                .boxed()
                .flatMap((Integer pairIdentifier) -> List.of(pairIdentifier, pairIdentifier).stream())
                .collect(ArrayList::new,
                        (ArrayList<Integer> pairs, Integer pairIdentifier) -> pairs.add(pairIdentifier),
                        (ArrayList<Integer> firstPairs, ArrayList<Integer> secondPairs) -> firstPairs.addAll(secondPairs));
        Collections.shuffle(shuffledPairs, random);
        pairIdentifiers = List.copyOf(shuffledPairs);
        matchedPositions.clear();
        firstSelectedPosition = OptionalInt.empty();
        livesRemaining = STARTING_LIVES;
        phase = Phase.PREVIEW;
    }

    private SelectionResult ignoredResult() {
        return result(SelectionOutcome.IGNORED, List.of());
    }

    private SelectionResult result(SelectionOutcome outcome, List<Integer> revealedPositions) {
        return new SelectionResult(outcome, revealedPositions, livesRemaining);
    }
}
