package sir_draco.survivalskills.skill_listeners.builderswand;

public record BuilderWandPosition(int x, int y, int z) {

    public BuilderWandPosition add(BuilderWandPosition other) {
        return new BuilderWandPosition(x + other.x(), y + other.y(), z + other.z());
    }

    public long distanceSquared(BuilderWandPosition other) {
        long deltaX = x - other.x();
        long deltaY = y - other.y();
        long deltaZ = z - other.z();
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
    }
}
