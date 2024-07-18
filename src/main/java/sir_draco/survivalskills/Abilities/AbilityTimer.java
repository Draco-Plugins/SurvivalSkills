package sir_draco.survivalskills.Abilities;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;

public class AbilityTimer extends BukkitRunnable {

    private final SurvivalSkills plugin;
    private final String name;
    private final Player p;
    private int activeTimeLeft;
    private boolean active = true;
    private int timeTillReset;

    public AbilityTimer(SurvivalSkills plugin, String name, Player p, int activeTimeLeft, int timeTillReset) {
        this.plugin = plugin;
        this.name = name;
        this.p = p;
        this.activeTimeLeft = activeTimeLeft;
        this.timeTillReset = timeTillReset;
    }

    @Override
    public void run() {
        if (plugin.getAbilityManager().getAbility(p, name) == null) {
            this.cancel();
            return;
        }

        if (active) activeTimeLeft--;
        else timeTillReset--;

        if (activeTimeLeft == 0) {
            active = false;
            activeTimeLeft--;
        }
        if (timeTillReset == 0) {
            plugin.getAbilityManager().removeAbility(p, name);
            this.cancel();
        }
    }

    public String getName() {
        return name;
    }

    public int getTimeTillReset() {
        return timeTillReset;
    }

    public int getActiveTimeLeft() {
        return activeTimeLeft;
    }

    public boolean isActive() {
        return active;
    }

    public void endAbility() {
        activeTimeLeft = 0;
        active = false;
    }

    public void endCooldown() {
        timeTillReset = 0;
        this.cancel();
    }
}
