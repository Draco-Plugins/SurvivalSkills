package sir_draco.survivalskills.skill_listeners.god.items;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Strategy for handling the activation of a god item identified by its
 * custom model data. Each implementation owns the behaviour for a single
 * item, replacing the long if/else chain that previously lived in
 * {@code GodListener.onUseGodItem}.
 */
public interface GodItemAction {

    /**
     * Executes the god item behaviour for the given player.
     *
     * @param player the player using the item
     * @param item   the item stack in the player's hand
     * @param meta   the item meta of {@code item}
     * @param event  the originating interact event
     */
    void execute(Player player, ItemStack item, ItemMeta meta, PlayerInteractEvent event);
}