package sir_draco.survivalskills.utils;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import sir_draco.survivalskills.skills.Skill;
import sir_draco.survivalskills.skills.SkillCategory;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SkillDisplayTest {

    @Test
    void printStatsShowsProgressForPlayerSkill() {
        Player player = mock(Player.class);
        Skill skill = skill(SkillCategory.MINING, 50, 25, 100);

        var messages = inOrder(player);
        SkillDisplay.printStats(player, skill, true);

        messages.verify(player).sendRawMessage(ChatColor.AQUA.toString() + ChatColor.BOLD
                + SkillCategory.MINING + ChatColor.WHITE + ":");
        messages.verify(player).sendRawMessage(ChatColor.WHITE + "Level: " + ChatColor.GREEN + 50);
        messages.verify(player).sendRawMessage(ChatColor.WHITE + "Experience: " + ChatColor.YELLOW + "25"
                + ChatColor.WHITE + "/" + ChatColor.YELLOW + "100" + ChatColor.WHITE + " ("
                + ChatColor.LIGHT_PURPLE + "25.00%" + ChatColor.WHITE + ")");
        messages.verify(player).sendRawMessage("");
    }

    @Test
    void printStatsShowsMaximumExperienceAtMaximumLevel() {
        Player player = mock(Player.class);
        Skill skill = skill(SkillCategory.MAIN, Skill.MAX_LEVEL, 0, 0);

        SkillDisplay.printStats(player, skill, true);

        verify(player).sendRawMessage(ChatColor.WHITE + "Experience: " + ChatColor.YELLOW + "MAX");
    }

    @Test
    void printStatsShowsInteractionPromptAtStartOfLevel() {
        Player player = mock(Player.class);
        Skill skill = skill(SkillCategory.FARMING, 10, 0, 100);

        SkillDisplay.printStats(player, skill, true);

        verify(player).sendRawMessage(ChatColor.WHITE + "Experience: " + ChatColor.LIGHT_PURPLE
                + "Interact with this skill first!");
    }

    @Test
    void printStatsForAnotherPlayerOmitsExperience() {
        Player player = mock(Player.class);
        Skill skill = skill(SkillCategory.BUILDING, 20, 25, 100);

        SkillDisplay.printStats(player, skill, false);

        verify(player).sendRawMessage(ChatColor.AQUA.toString() + ChatColor.BOLD
                + SkillCategory.BUILDING + ChatColor.WHITE + ":");
        verify(player).sendRawMessage(ChatColor.WHITE + "Level: " + ChatColor.GREEN + 20);
        verify(player).sendRawMessage("");
        verify(player, never()).sendRawMessage(org.mockito.ArgumentMatchers.contains("Experience:"));
    }

    private Skill skill(SkillCategory category, int level, int experienceSoFar, int requiredExperience) {
        Skill skill = mock(Skill.class);
        when(skill.getSkillCategory()).thenReturn(category);
        when(skill.getLevel()).thenReturn(level);
        when(skill.getExperienceSoFarInLevel()).thenReturn(experienceSoFar);
        when(skill.getRawExperienceForNextLevel()).thenReturn(requiredExperience);
        return skill;
    }
}
