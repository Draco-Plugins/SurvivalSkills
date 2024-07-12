package sir_draco.survivalskills.Abilities;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;

public class WaterBreathingTimer extends BukkitRunnable {

    private final SurvivalSkills plugin;
    private int timeLeft;
    private final Player p;

    public WaterBreathingTimer(SurvivalSkills plugin, int timeLeft, Player p) {
        this.plugin = plugin;
        this.timeLeft = timeLeft;
        this.p = p;
    }

    @Override
    public void run() {
        timeLeft--;
        if (timeLeft <= 0) {
            plugin.getFishingListener().getWaterBreathers().remove(p);
            p.sendRawMessage(ChatColor.YELLOW + "Water Breathing has worn off");
            p.playSound(p, Sound.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 1, 1);
            cancel();
        }
    }
}
