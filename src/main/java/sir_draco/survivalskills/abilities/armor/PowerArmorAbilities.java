package sir_draco.survivalskills.abilities.armor;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.skill_listeners.ArmorListener;
import sir_draco.survivalskills.skill_listeners.ArmorListener.ArmorType;

public final class PowerArmorAbilities extends BukkitRunnable {

    private final Player player;

    public PowerArmorAbilities(Player player) {
        this.player = player;
    }

    @Override
    public void run() {
        if (!ArmorListener.isWearingArmor(player.getUniqueId(), ArmorType.POWER)) {
            ArmorListener.resetAdventurerStepHeight(player);
            cancel();
            return;
        }

        ArmorListener.updateAdventurerStepHeight(player, player.isSneaking());
        ArmorListener.giveSpeedPotionEffect(player, 2);
    }
}
