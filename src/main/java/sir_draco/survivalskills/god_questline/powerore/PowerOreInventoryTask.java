package sir_draco.survivalskills.god_questline.powerore;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

/** Common event contract for Power Ore challenges presented in an inventory. */
public interface PowerOreInventoryTask extends PowerOreTask {

    boolean ownsInventory(Inventory inventory);

    void handleClick(InventoryClickEvent event);

    void handleClose(InventoryCloseEvent event);

    void handleDrag(InventoryDragEvent event);
}
