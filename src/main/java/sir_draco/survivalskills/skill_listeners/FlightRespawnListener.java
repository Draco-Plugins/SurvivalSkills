package sir_draco.survivalskills.skill_listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.abilities.AbilityTimer;

public final class FlightRespawnListener implements Listener {

    private final SurvivalSkills plugin;

    public FlightRespawnListener(SurvivalSkills plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        AbilityTimer timer = plugin.getAbilityManager().getAbility(p, "Flight");
        if (timer == null || !timer.isActive()) {
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> restoreFlight(p));
    }

    private void restoreFlight(Player p) {
        AbilityTimer timer = plugin.getAbilityManager().getAbility(p, "Flight");
        if (timer == null || !timer.isActive()) {
            return;
        }

        p.setAllowFlight(true);
        p.setFlying(true);
        p.setFlySpeed(timer.getFlightSpeed());
    }
}
