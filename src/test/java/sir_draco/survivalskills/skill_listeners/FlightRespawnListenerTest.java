package sir_draco.survivalskills.skill_listeners;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.abilities.AbilityManager;
import sir_draco.survivalskills.abilities.AbilityTimer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FlightRespawnListenerTest {

    @Test
    void restoresActiveFlightOnTickAfterRespawn() {
        FlightRespawnTestContext context = createContext();
        when(context.timer().isActive()).thenReturn(true);
        when(context.timer().getFlightSpeed()).thenReturn(0.2F);

        context.listener().onPlayerRespawn(context.event());

        verify(context.player(), never()).setAllowFlight(true);
        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(context.scheduler()).runTask(eq(context.plugin()), taskCaptor.capture());

        taskCaptor.getValue().run();

        verify(context.player()).setAllowFlight(true);
        verify(context.player()).setFlying(true);
        verify(context.player()).setFlySpeed(0.2F);
    }

    @Test
    void doesNotRestoreFlightIfTimerExpiresBeforeScheduledTaskRuns() {
        FlightRespawnTestContext context = createContext();
        when(context.timer().isActive()).thenReturn(true, false);

        context.listener().onPlayerRespawn(context.event());

        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(context.scheduler()).runTask(eq(context.plugin()), taskCaptor.capture());
        taskCaptor.getValue().run();

        verify(context.player(), never()).setAllowFlight(true);
        verify(context.player(), never()).setFlying(true);
        verify(context.player(), never()).setFlySpeed(anyFloat());
    }

    @Test
    void doesNotScheduleRestoreForInactiveFlight() {
        FlightRespawnTestContext context = createContext();
        when(context.timer().isActive()).thenReturn(false);

        context.listener().onPlayerRespawn(context.event());

        verify(context.scheduler(), never()).runTask(any(), any(Runnable.class));
    }

    private static FlightRespawnTestContext createContext() {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        Server server = mock(Server.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        AbilityManager abilityManager = mock(AbilityManager.class);
        AbilityTimer timer = mock(AbilityTimer.class);
        Player player = mock(Player.class);
        PlayerRespawnEvent event = mock(PlayerRespawnEvent.class);

        when(plugin.getServer()).thenReturn(server);
        when(server.getScheduler()).thenReturn(scheduler);
        when(plugin.getAbilityManager()).thenReturn(abilityManager);
        when(abilityManager.getAbility(player, "Flight")).thenReturn(timer);
        when(event.getPlayer()).thenReturn(player);

        return new FlightRespawnTestContext(plugin, scheduler, timer, player, event,
                new FlightRespawnListener(plugin));
    }

    private record FlightRespawnTestContext(SurvivalSkills plugin, BukkitScheduler scheduler, AbilityTimer timer,
                                            Player player, PlayerRespawnEvent event,
                                            FlightRespawnListener listener) {}
}
