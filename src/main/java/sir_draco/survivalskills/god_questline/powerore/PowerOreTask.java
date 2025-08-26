package sir_draco.survivalskills.god_questline.powerore;

import org.bukkit.entity.Player;

/**
 * Represents a single Power Ore challenge task.
 * Tasks must call complete() or fail() on their parent challenge when finished.
 */
public interface PowerOreTask {

    /** Start the task logic. */
    void start();

    /** Force cancel any running logic (runnables, entities, etc.). */
    void cleanup();

    /** @return display name of task for messaging */
    String name();

    /** @return player doing the task */
    Player getPlayer();
}
