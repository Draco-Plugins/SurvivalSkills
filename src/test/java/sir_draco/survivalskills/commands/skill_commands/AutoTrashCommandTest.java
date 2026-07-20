package sir_draco.survivalskills.commands.skill_commands;

import org.bukkit.command.Command;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.abilities.AutoTrash;
import sir_draco.survivalskills.rewards.PlayerRewards;
import sir_draco.survivalskills.rewards.Reward;
import sir_draco.survivalskills.skill_listeners.FishingSkill;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.skills.SkillManager;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutoTrashCommandTest {

    @Test
    void appliedSizeRewardUpgradesExistingSmallTrashBeforeOpening() {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        SkillManager skillManager = mock(SkillManager.class);
        PlayerRewards playerRewards = mock(PlayerRewards.class);
        FishingSkill fishingSkill = mock(FishingSkill.class);
        Player player = mock(Player.class);
        Command command = mock(Command.class);
        Reward autoTrashReward = mock(Reward.class);
        Reward sizeReward = mock(Reward.class);
        AutoTrash trash = mock(AutoTrash.class);
        HashMap<Player, AutoTrash> trashInventories = new HashMap<>();
        trashInventories.put(player, trash);

        when(plugin.getCommand("autotrash")).thenReturn(null);
        when(plugin.getSkillManager()).thenReturn(skillManager);
        when(skillManager.getPlayerRewards(player)).thenReturn(playerRewards);
        when(playerRewards.getReward(SkillCategory.FISHING, "AutoTrashI")).thenReturn(autoTrashReward);
        when(playerRewards.getReward(SkillCategory.FISHING, "AutoTrashII")).thenReturn(sizeReward);
        when(autoTrashReward.isEnabled()).thenReturn(true);
        when(autoTrashReward.isApplied()).thenReturn(true);
        when(sizeReward.isEnabled()).thenReturn(true);
        when(sizeReward.isApplied()).thenReturn(true);
        when(plugin.getFishingListener()).thenReturn(fishingSkill);
        when(fishingSkill.getTrashInventories()).thenReturn(trashInventories);
        when(trash.isBig()).thenReturn(false);
        AutoTrashCommand executor = new AutoTrashCommand(plugin);

        boolean handled = executor.onCommand(player, command, "autotrash", new String[0]);

        assertTrue(handled);
        verify(trash).upgradeTrashSize();
        verify(fishingSkill).markTrashInventoryOpen(player);
        verify(trash).openTrashInventory(player);
    }
}
