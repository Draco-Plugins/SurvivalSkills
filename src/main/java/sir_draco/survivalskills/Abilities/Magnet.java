package sir_draco.survivalskills.Abilities;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.Utils.ItemStackGenerator;
import sir_draco.survivalskills.Utils.ProjectileCalculator;

import static sir_draco.survivalskills.SkillListeners.ExploringSkill.activeMagnets;

public class Magnet extends BukkitRunnable {

    private final Player p;

    public Magnet(Player p) {
        this.p = p;
    }

    @Override
    public void run() {
        if (!activeMagnets.contains(p)) {
            cancel();
            return;
        }

        for (Entity ent : p.getNearbyEntities(10, 10, 10)) {
            if (!(ent instanceof Item)) continue;
            ItemStack item = ((Item) ent).getItemStack();
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            String type = meta.getPersistentDataContainer().get(ItemStackGenerator.skillsItemKey, PersistentDataType.STRING);
            if (type == null) continue;
            if (type.equals("Trophy")) continue;

            Vector toPlayer = ProjectileCalculator.getNoGravityVector(ent.getLocation(), p.getLocation(),
                    1 / ent.getLocation().distance(p.getLocation()));
            ent.setVelocity(toPlayer);
        }
    }
}
