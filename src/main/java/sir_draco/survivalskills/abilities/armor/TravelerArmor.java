package sir_draco.survivalskills.abilities.armor;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.skill_listeners.ArmorListener;

import static sir_draco.survivalskills.skill_listeners.ArmorListener.playersWearingTravelerArmor;

public class TravelerArmor extends BukkitRunnable {

    private final Player p;

    public TravelerArmor(Player p) {
        this.p = p;
    }

    @Override
    public void run() {
        if (!playersWearingTravelerArmor.contains(p.getUniqueId())) {
            cancel();
            return;
        }

        ArmorListener.giveSpeedPotionEffect(p, 1);
    }
}
