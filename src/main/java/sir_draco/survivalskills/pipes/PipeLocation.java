package sir_draco.survivalskills.pipes;

import java.util.UUID;

public record PipeLocation(UUID worldUuid, int x, int y, int z) {
    public PipeLocation {
        java.util.Objects.requireNonNull(worldUuid);
    }

    public int chunkX() {
        return x >> 4;
    }

    public int chunkZ() {
        return z >> 4;
    }

    public long squaredDistance(PipeLocation other) {
        if (!worldUuid.equals(other.worldUuid)) return Long.MAX_VALUE;
        long deltaX = (long) x - other.x;
        long deltaY = (long) y - other.y;
        long deltaZ = (long) z - other.z;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
    }
}
