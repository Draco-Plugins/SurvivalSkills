package sir_draco.survivalskills.skill_listeners.fighting;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.junit.jupiter.api.Test;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.skills.SkillManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CombatMechanicsManagerTest {

    @Test
    void missingPlayerRewardsSkipCombatRewardEffects() {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        SkillManager skillManager = mock(SkillManager.class);
        Player player = mock(Player.class);
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(plugin.getSkillManager()).thenReturn(skillManager);
        when(skillManager.getPlayerRewards(player)).thenReturn(null);
        CombatMechanicsManager combatMechanicsManager = new CombatMechanicsManager(plugin);

        combatMechanicsManager.applyCritical(player, event);
        combatMechanicsManager.applyLifesteal(player, event);

        verifyNoInteractions(event);
    }
}
