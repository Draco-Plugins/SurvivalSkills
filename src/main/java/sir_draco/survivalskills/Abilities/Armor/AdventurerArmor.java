package sir_draco.survivalskills.Abilities.Armor;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SkillListeners.ArmorListener;

import static sir_draco.survivalskills.SkillListeners.ArmorListener.playersWearingAdventurerArmor;

public class AdventurerArmor extends BukkitRunnable {

    private final Player p;

    public AdventurerArmor(Player p) {
        this.p = p;
    }

    @Override
    public void run() {
        if (!playersWearingAdventurerArmor.contains(p.getUniqueId())) {
            cancel();
            return;
        }

        ArmorListener.giveSpeedPotionEffect(p, 2);
    }
}
