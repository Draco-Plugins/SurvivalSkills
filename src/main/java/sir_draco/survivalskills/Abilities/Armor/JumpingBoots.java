package sir_draco.survivalskills.Abilities.Armor;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SkillListeners.ArmorListener;

import static sir_draco.survivalskills.SkillListeners.ArmorListener.playersWearingJumpingBoots;

public class JumpingBoots extends BukkitRunnable {

    private final Player p;

    public JumpingBoots(Player p) {
        this.p = p;
    }

    @Override
    public void run() {
        if (!playersWearingJumpingBoots.contains(p.getUniqueId())) {
            cancel();
            return;
        }

        ArmorListener.giveJumpPotionEffect(p);
    }
}
