package sir_draco.survivalskills.Abilities.Armor;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SkillListeners.ArmorListener;

import static sir_draco.survivalskills.SkillListeners.ArmorListener.playersWearingGillArmor;

public class GillArmor extends BukkitRunnable {
    private final Player p;

    public GillArmor(Player p) {
        this.p = p;
    }

    @Override
    public void run() {
        if (!playersWearingGillArmor.contains(p.getUniqueId())) {
            cancel();
            return;
        }

        ArmorListener.giveWaterBreathingPotionEffect(p);
    }
}
