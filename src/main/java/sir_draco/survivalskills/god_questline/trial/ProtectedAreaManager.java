package sir_draco.survivalskills.god_questline.trial;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Owns the protected trial areas and the trial-building creation cooldowns. All bounding-box
 * containment checks (block break/place, explosions, enderman pickup) route through
 * {@link #isLocationProtected} so the logic lives in exactly one place.
 */
public class ProtectedAreaManager {

    private static final ProtectedAreaManager INSTANCE = new ProtectedAreaManager();

    private final Map<UUID, ProtectedArea> protectedAreas = new HashMap<>();
    private final Map<UUID, Long> trialBuildingCreationCooldowns = new HashMap<>();

    private ProtectedAreaManager() {
    }

    public static ProtectedAreaManager getInstance() {
        return INSTANCE;
    }

    // --- Protected areas --------------------------------------------------

    public void putProtectedArea(UUID ownerId, ProtectedArea area) {
        protectedAreas.put(ownerId, area);
    }

    public ProtectedArea getProtectedArea(UUID ownerId) {
        return protectedAreas.get(ownerId);
    }

    public boolean hasProtectedArea(UUID ownerId) {
        return protectedAreas.containsKey(ownerId);
    }

    public void removeProtectedArea(UUID ownerId) {
        protectedAreas.remove(ownerId);
    }

    public Map<UUID, ProtectedArea> getProtectedAreas() {
        return Collections.unmodifiableMap(protectedAreas);
    }

    public boolean hasProtectedAreas() {
        return !protectedAreas.isEmpty();
    }

    /**
     * Returns true if {@code location} falls inside any registered protected area within its
     * own world. This consolidates the four near-identical event-handler checks.
     */
    public boolean isLocationProtected(Location location) {
        if (protectedAreas.isEmpty())
            return false;
        World world = location.getWorld();
        Vector point = location.toVector();
        for (ProtectedArea area : protectedAreas.values()) {
            if (!Objects.equals(area.world(), world))
                continue;
            if (area.boundingBox().contains(point))
                return true;
        }
        return false;
    }

    public boolean isBlockProtected(Block block) {
        return isLocationProtected(block.getLocation());
    }

    // --- Building creation cooldowns -------------------------------------

    public Long getBuildingCreationCooldown(UUID ownerId) {
        return trialBuildingCreationCooldowns.get(ownerId);
    }

    public void setBuildingCreationCooldown(UUID ownerId, long timestamp) {
        trialBuildingCreationCooldowns.put(ownerId, timestamp);
    }

    public void removeBuildingCreationCooldown(UUID ownerId) {
        trialBuildingCreationCooldowns.remove(ownerId);
    }

    public Map<UUID, Long> getTrialBuildingCreationCooldowns() {
        return Collections.unmodifiableMap(trialBuildingCreationCooldowns);
    }
}