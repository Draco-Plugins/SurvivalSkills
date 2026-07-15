package sir_draco.survivalskills.god_questline.powerore;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Common interface for active and completed Power Ore challenges.
 */
public sealed interface PowerOreChallengeHandle
        permits PowerOreChallenge, CompletedPowerOreChallenge {

    Location getOreLocation();

    UUID getUniqueId();

    PowerOreChallenge.Status getStatus();

    boolean isRewardDropped();

    /** Drops the ore reward and sends a success message to the player. */
    void reward(Player player);

    /** Starts the visual particle effect. */
    void startVisuals();

    /** Stops the visual particle effect. */
    void stopVisuals();
}
