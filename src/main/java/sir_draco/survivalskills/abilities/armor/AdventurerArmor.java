package sir_draco.survivalskills.abilities.armor;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.skill_listeners.ArmorListener;
import sir_draco.survivalskills.skill_listeners.ArmorListener.ArmorType;

public class AdventurerArmor extends BukkitRunnable {

    private final Player p;

    public AdventurerArmor(Player p) {
        this.p = p;
    }

    @Override
    public void run() {
        if (!ArmorListener.isWearingArmor(p.getUniqueId(), ArmorType.ADVENTURER)) {
            cancel();
            return;
        }

        ArmorListener.giveSpeedPotionEffect(p, 2);
    }
}
