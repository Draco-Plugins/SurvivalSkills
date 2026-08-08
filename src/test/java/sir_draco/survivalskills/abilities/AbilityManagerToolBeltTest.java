package sir_draco.survivalskills.abilities;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.rewards.PlayerRewards;
import sir_draco.survivalskills.rewards.Reward;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.skills.SkillManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbilityManagerToolBeltTest {

    @Test
    void toolBeltUsesNineSlotsBeforeUpgradeIsEarned() {
        ToolBeltTestContext context = createContext(false, false);

        assertEquals(AbilityManager.TOOL_BELT_SIZE,
                context.abilityManager().getToolBeltSize(context.player()));
    }

    @Test
    void toolBeltUsesEighteenSlotsAfterUpgradeIsEarned() {
        ToolBeltTestContext context = createContext(true, true);

        assertEquals(AbilityManager.UPGRADED_TOOL_BELT_SIZE,
                context.abilityManager().getToolBeltSize(context.player()));
    }

    @Test
    void disabledUpgradeDoesNotExpandTheToolBelt() {
        ToolBeltTestContext context = createContext(false, true);

        assertEquals(AbilityManager.TOOL_BELT_SIZE,
                context.abilityManager().getToolBeltSize(context.player()));
    }

    private static ToolBeltTestContext createContext(boolean enabled, boolean applied) {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        SkillManager skillManager = mock(SkillManager.class);
        PlayerRewards playerRewards = mock(PlayerRewards.class);
        Reward reward = mock(Reward.class);
        Player player = mock(Player.class);

        when(plugin.getSkillManager()).thenReturn(skillManager);
        when(skillManager.getPlayerRewards(player)).thenReturn(playerRewards);
        when(playerRewards.getReward(SkillCategory.FIGHTING, AbilityManager.TOOL_BELT_UPGRADE_REWARD))
                .thenReturn(reward);
        when(reward.isEnabled()).thenReturn(enabled);
        when(reward.isApplied()).thenReturn(applied);

        return new ToolBeltTestContext(new AbilityManager(plugin), player);
    }

    private record ToolBeltTestContext(AbilityManager abilityManager, Player player) {}
}
