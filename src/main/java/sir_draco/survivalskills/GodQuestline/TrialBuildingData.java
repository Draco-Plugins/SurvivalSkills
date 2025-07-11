package sir_draco.survivalskills.GodQuestline;

import org.bukkit.Location;

import java.util.UUID;

public record TrialBuildingData(UUID owner, long lastUsed, Location location) {
    public boolean isExpired() {
        return System.currentTimeMillis() - lastUsed > 60 * 60 * 1000L; // 1 hour in milliseconds
    }
}
