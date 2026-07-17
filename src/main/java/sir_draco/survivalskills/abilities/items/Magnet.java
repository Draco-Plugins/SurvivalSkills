package sir_draco.survivalskills.abilities.items;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import sir_draco.survivalskills.utils.ProjectileCalculator;
import sir_draco.survivalskills.utils.items.ItemModelData;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;

import static sir_draco.survivalskills.skill_listeners.ExploringSkill.getActiveMagnetPlayers;

public class Magnet extends BukkitRunnable {

    private final Player p;
    private final Runnable deactivate;

    public Magnet(Player p, Runnable deactivate) {
        this.p = p;
        this.deactivate = deactivate;
    }

    @Override
    public void run() {
        if (!getActiveMagnetPlayers().contains(p.getUniqueId())) {
            cancel();
            return;
        }
        if (!ItemStackGeneratorUtils.isCustomItem(p.getInventory().getItemInMainHand(),
                ItemModelData.MAGNET.getId())) {
            deactivate.run();
            return;
        }

        for (Entity ent : p.getNearbyEntities(20, 20, 20)) {
            if (!(ent instanceof Item))
                continue;
            ItemStack item = ((Item) ent).getItemStack();
            if (p.getLocation().distance(ent.getLocation()) < 1)
                return;
            ItemMeta meta = item.getItemMeta();
            if (meta == null)
                continue;
            if (meta.getPersistentDataContainer().has(ItemStackGeneratorUtils.trophyItemKey,
                            PersistentDataType.STRING)) {
                String type = meta.getPersistentDataContainer().get(ItemStackGeneratorUtils.trophyItemKey,
                        PersistentDataType.STRING);
                if (type != null)
                    if (type.equals("Trophy"))
                        continue;
            }

            Vector toPlayer = ProjectileCalculator.getNoGravityVector(ent.getLocation(), p.getLocation(),
                    ent.getLocation().distance(p.getLocation()) / 2);
            ent.setVelocity(toPlayer);
        }
    }
}
