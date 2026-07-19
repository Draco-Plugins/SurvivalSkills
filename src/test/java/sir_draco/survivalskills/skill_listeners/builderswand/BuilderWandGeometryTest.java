package sir_draco.survivalskills.skill_listeners.builderswand;

import org.bukkit.block.BlockFace;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuilderWandGeometryTest {

    private static final BuilderWandPosition ORIGIN = new BuilderWandPosition(0, 0, 0);

    @Test
    void isolatedBlockPlacesOneBlockOnLookedAtFace() {
        Set<BuilderWandPosition> referenceBlocks = Set.of(ORIGIN);

        List<BuilderWandPosition> placements = find(referenceBlocks, BlockFace.EAST, 5, Set.of());

        assertEquals(List.of(new BuilderWandPosition(1, 0, 0)), placements);
    }

    @Test
    void pillarExtendsEveryAlignedBlockAlongLookedAtFace() {
        Set<BuilderWandPosition> referenceBlocks = Set.of(
                new BuilderWandPosition(0, -1, 0), ORIGIN, new BuilderWandPosition(0, 1, 0));

        List<BuilderWandPosition> placements = find(referenceBlocks, BlockFace.WEST, 5, Set.of());

        assertEquals(Set.of(
                new BuilderWandPosition(-1, -1, 0),
                new BuilderWandPosition(-1, 0, 0),
                new BuilderWandPosition(-1, 1, 0)), new HashSet<>(placements));
    }

    @Test
    void diagonalBlocksRemainConnectedInThePlacementPlane() {
        Set<BuilderWandPosition> referenceBlocks = Set.of(
                new BuilderWandPosition(-1, -1, 0), ORIGIN, new BuilderWandPosition(1, 1, 0));

        List<BuilderWandPosition> placements = find(referenceBlocks, BlockFace.NORTH, 5, Set.of());

        assertEquals(Set.of(
                new BuilderWandPosition(-1, -1, -1),
                new BuilderWandPosition(0, 0, -1),
                new BuilderWandPosition(1, 1, -1)), new HashSet<>(placements));
    }

    @Test
    void modeLimitSelectsNearestBlocksAroundLookedAtBlock() {
        Set<BuilderWandPosition> referenceBlocks = new HashSet<>();
        for (int x = -5; x <= 5; x++) {
            referenceBlocks.add(new BuilderWandPosition(x, 0, 0));
        }

        List<BuilderWandPosition> placements = find(referenceBlocks, BlockFace.UP, 5, Set.of());

        assertEquals(Set.of(
                new BuilderWandPosition(-2, 1, 0),
                new BuilderWandPosition(-1, 1, 0),
                new BuilderWandPosition(0, 1, 0),
                new BuilderWandPosition(1, 1, 0),
                new BuilderWandPosition(2, 1, 0)), new HashSet<>(placements));
    }

    @Test
    void blockedTargetDoesNotPreventTraversalToFurtherAlignedBlocks() {
        Set<BuilderWandPosition> referenceBlocks = Set.of(
                new BuilderWandPosition(-1, 0, 0), ORIGIN, new BuilderWandPosition(1, 0, 0));
        Set<BuilderWandPosition> blockedTargets = Set.of(new BuilderWandPosition(0, 1, 0));

        List<BuilderWandPosition> placements = find(referenceBlocks, BlockFace.UP, 5, blockedTargets);

        assertEquals(Set.of(new BuilderWandPosition(-1, 1, 0), new BuilderWandPosition(1, 1, 0)),
                new HashSet<>(placements));
    }

    private static List<BuilderWandPosition> find(Set<BuilderWandPosition> referenceBlocks, BlockFace face,
            int blockLimit, Set<BuilderWandPosition> blockedTargets) {
        return BuilderWandGeometry.findPlacements(ORIGIN, face, blockLimit, referenceBlocks::contains,
                (BuilderWandPosition position) -> !blockedTargets.contains(position));
    }
}
