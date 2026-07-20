package sir_draco.survivalskills.god_questline.trial;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

final class TrialCursorItemManager {

    private TrialCursorItemManager() {
    }

    static void returnToInventory(Player player) {
        ItemStack cursorItem = player.getItemOnCursor();
        if (cursorItem.getType().equals(Material.AIR))
            return;

        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(cursorItem.clone());
        for (ItemStack leftover : leftovers.values())
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        player.setItemOnCursor(new ItemStack(Material.AIR));
    }
}
