package sir_draco.survivalskills.Abilities;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;

public class HarvesterTimer extends BukkitRunnable {

    private final SurvivalSkills plugin;
    private final Player p;
    private int timer;

    public HarvesterTimer(SurvivalSkills plugin, Player p, int timer) {
        this.plugin = plugin;
        this.timer = timer;
        this.p = p;
    }

    @Override
    public void run() {
        this.timer--;
        if (this.timer > 0) return;
        plugin.getFarmingListener().getHarvesterCooldowns().remove(p);
        p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
        this.cancel();
    }
}
