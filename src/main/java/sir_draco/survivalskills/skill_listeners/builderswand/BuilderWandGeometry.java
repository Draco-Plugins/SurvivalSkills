package sir_draco.survivalskills.skill_listeners.builderswand;

import org.bukkit.block.BlockFace;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

public final class BuilderWandGeometry {

    private BuilderWandGeometry() {}

    public static List<BuilderWandPosition> findPlacements(BuilderWandPosition origin, BlockFace face,
            int blockLimit, Predicate<BuilderWandPosition> matchesReference,
            Predicate<BuilderWandPosition> isValidTarget) {
        if (blockLimit < 1) {
            return List.of();
        }

        BuilderWandPosition normal = normalFor(face);
        List<BuilderWandPosition> planeDirections = planeDirections(face);
        int traversalRadius = blockLimit - 1;
        Queue<BuilderWandPosition> pending = new ArrayDeque<>();
        Set<BuilderWandPosition> visited = new HashSet<>();
        List<BuilderWandPosition> placements = new ArrayList<>();
        pending.add(origin);

        while (!pending.isEmpty()) {
            BuilderWandPosition source = pending.remove();
            if (!visited.add(source) || !withinRadius(origin, source, traversalRadius)
                    || !matchesReference.test(source)) {
                continue;
            }

            BuilderWandPosition target = source.add(normal);
            if (isValidTarget.test(target)) {
                placements.add(target);
            }
            planeDirections.stream()
                    .map(source::add)
                    .filter((BuilderWandPosition position) -> !visited.contains(position))
                    .forEach(pending::add);
        }

        return placements.stream()
                .sorted(Comparator.comparingLong((BuilderWandPosition position) -> position.distanceSquared(origin))
                        .thenComparingInt((BuilderWandPosition position) -> position.y())
                        .thenComparingInt((BuilderWandPosition position) -> position.x())
                        .thenComparingInt((BuilderWandPosition position) -> position.z()))
                .limit(blockLimit)
                .toList();
    }

    private static boolean withinRadius(BuilderWandPosition origin, BuilderWandPosition position, int radius) {
        return Math.abs(position.x() - origin.x()) <= radius
                && Math.abs(position.y() - origin.y()) <= radius
                && Math.abs(position.z() - origin.z()) <= radius;
    }

    private static BuilderWandPosition normalFor(BlockFace face) {
        return switch (face) {
            case UP -> new BuilderWandPosition(0, 1, 0);
            case DOWN -> new BuilderWandPosition(0, -1, 0);
            case NORTH -> new BuilderWandPosition(0, 0, -1);
            case SOUTH -> new BuilderWandPosition(0, 0, 1);
            case EAST -> new BuilderWandPosition(1, 0, 0);
            case WEST -> new BuilderWandPosition(-1, 0, 0);
            default -> throw new IllegalArgumentException("Builder's wand requires a cardinal block face: " + face);
        };
    }

    private static List<BuilderWandPosition> planeDirections(BlockFace face) {
        return switch (face) {
            case UP, DOWN -> combineAxes(new BuilderWandPosition(1, 0, 0),
                    new BuilderWandPosition(0, 0, 1));
            case NORTH, SOUTH -> combineAxes(new BuilderWandPosition(1, 0, 0),
                    new BuilderWandPosition(0, 1, 0));
            case EAST, WEST -> combineAxes(new BuilderWandPosition(0, 0, 1),
                    new BuilderWandPosition(0, 1, 0));
            default -> throw new IllegalArgumentException("Builder's wand requires a cardinal block face: " + face);
        };
    }

    private static List<BuilderWandPosition> combineAxes(BuilderWandPosition firstAxis,
            BuilderWandPosition secondAxis) {
        List<BuilderWandPosition> directions = new ArrayList<>();
        for (int first = -1; first <= 1; first++) {
            for (int second = -1; second <= 1; second++) {
                if (first == 0 && second == 0) {
                    continue;
                }
                directions.add(new BuilderWandPosition(
                        firstAxis.x() * first + secondAxis.x() * second,
                        firstAxis.y() * first + secondAxis.y() * second,
                        firstAxis.z() * first + secondAxis.z() * second));
            }
        }
        return List.copyOf(directions);
    }
}
