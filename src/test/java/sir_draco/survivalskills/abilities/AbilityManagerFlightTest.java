package sir_draco.survivalskills.abilities;

import org.bukkit.GameMode;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.commands.skill_commands.FlightCommand;
import sir_draco.survivalskills.rewards.PlayerRewards;
import sir_draco.survivalskills.rewards.Reward;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.skills.SkillManager;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbilityManagerFlightTest {

    @Test
    void loadFlightClearsPersistedFlightWithoutTrackedTimer() {
        FlightTestContext context = createContext(GameMode.SURVIVAL, false);

        context.abilityManager().loadFlight(context.player(), new YamlConfiguration());

        verify(context.player()).setFlying(false);
        verify(context.player()).setAllowFlight(false);
    }

    @Test
    void loadFlightPreservesCreativeFlightWithoutTrackedTimer() {
        FlightTestContext context = createContext(GameMode.CREATIVE, false);

        context.abilityManager().loadFlight(context.player(), new YamlConfiguration());

        verify(context.player(), never()).setFlying(false);
        verify(context.player(), never()).setAllowFlight(false);
    }

    @Test
    void loadFlightPreservesUnlimitedFlightWithoutTrackedTimer() {
        FlightTestContext context = createContext(GameMode.SURVIVAL, true);

        context.abilityManager().loadFlight(context.player(), new YamlConfiguration());

        verify(context.player(), never()).setFlying(false);
        verify(context.player(), never()).setAllowFlight(false);
    }

    private static FlightTestContext createContext(GameMode gameMode, boolean unlimitedFlight) {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        SkillManager skillManager = mock(SkillManager.class);
        PlayerRewards playerRewards = mock(PlayerRewards.class);
        Player player = mock(Player.class);
        Reward reward = mock(Reward.class);

        when(plugin.getSkillManager()).thenReturn(skillManager);
        when(skillManager.getPlayerRewards(player)).thenReturn(playerRewards);
        when(playerRewards.getReward(SkillCategory.BUILDING, FlightCommand.FLIGHT_IV)).thenReturn(reward);
        when(reward.isEnabled()).thenReturn(unlimitedFlight);
        when(reward.isApplied()).thenReturn(unlimitedFlight);
        when(player.getGameMode()).thenReturn(gameMode);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        return new FlightTestContext(new AbilityManager(plugin), player);
    }

    private record FlightTestContext(AbilityManager abilityManager, Player player) {}
}
