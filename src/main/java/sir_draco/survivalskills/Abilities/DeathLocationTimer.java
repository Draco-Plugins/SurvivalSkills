package sir_draco.survivalskills.Abilities;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;

public class DeathLocationTimer extends BukkitRunnable {

    private final SurvivalSkills plugin;
    private final Player p;
    private final Location loc;
    private int time;

    public DeathLocationTimer(SurvivalSkills plugin, Player p, Location loc, int time) {
        this.plugin = plugin;
        this.p = p;
        this.loc = loc;
        this.time = time;
    }

    @Override
    public void run() {
        time--;
        if (time <= 0) {
            plugin.getMainListener().getDeathLocations().get(p).remove(loc);
            cancel();
        }
    }
}
