package sir_draco.survivalskills.abilities.armor;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.skill_listeners.ArmorListener;
import sir_draco.survivalskills.skill_listeners.ArmorListener.ArmorType;

public class MiningArmor extends BukkitRunnable {

    private final Player p;

    public MiningArmor(Player p) {
        this.p = p;
    }

    @Override
    public void run() {
        if (!ArmorListener.isWearingArmor(p.getUniqueId(), ArmorType.MINING)) {
            cancel();
            return;
        }

        ArmorListener.giveHastePotionEffect(p, 1);

        Location loc = p.getLocation();
        if (loc.getWorld() == null || loc.getBlockY() >= 64)
            return;

        ArmorListener.giveSpeedPotionEffect(p, 0);
        ArmorListener.giveFireResistancePotionEffect(p);
    }
}
