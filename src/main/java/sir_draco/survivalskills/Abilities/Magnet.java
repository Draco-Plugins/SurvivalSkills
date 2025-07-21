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

        for (Entity ent : p.getNearbyEntities(20, 20, 20)) {
            if (!(ent instanceof Item)) continue;
            ItemStack item = ((Item) ent).getItemStack();
            if (p.getLocation().distance(ent.getLocation()) < 1) return;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            if (!meta.getPersistentDataContainer().isEmpty()
                    && meta.getPersistentDataContainer().has(ItemStackGenerator.skillsItemKey)
                    && meta.getPersistentDataContainer().has(ItemStackGenerator.skillsItemKey, PersistentDataType.STRING)) {
                String type = meta.getPersistentDataContainer().get(ItemStackGenerator.skillsItemKey, PersistentDataType.STRING);
                if (type != null)
                    if (type.equals("Trophy")) continue;
            }

            Vector toPlayer = ProjectileCalculator.getNoGravityVector(ent.getLocation(), p.getLocation(),
                    ent.getLocation().distance(p.getLocation()) / 2);
            ent.setVelocity(toPlayer);
        }
    }
}
