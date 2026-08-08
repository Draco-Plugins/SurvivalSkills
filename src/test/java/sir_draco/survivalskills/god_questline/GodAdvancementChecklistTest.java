package sir_draco.survivalskills.god_questline;

import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GodAdvancementChecklistTest {

    @Test
    void identifiesOnlyAdvancementPhase() {
        assertFalse(GodTrophyQuest.isAdvancementPhase(57));
        assertTrue(GodTrophyQuest.isAdvancementPhase(58));
        assertFalse(GodTrophyQuest.isAdvancementPhase(59));
    }

    @Test
    void filtersCompletedAndRecipeAdvancements() {
        Advancement incomplete = advancement("story/mine_stone");
        Advancement completed = advancement("adventure/kill_a_mob");
        Advancement recipe = advancement("recipes/building_blocks/oak_planks");
        Player player = mock(Player.class);
        setCompletion(player, incomplete, false);
        setCompletion(player, completed, true);

        List<Advancement> result = GodTrophyQuest.getIncompleteAdvancements(
                List.of(incomplete, completed, recipe).iterator(), player);

        assertEquals(List.of(incomplete), result);
    }

    @Test
    void returnsImmutableAdvancementsInIteratorOrder() {
        Advancement first = advancement("nether/find_bastion");
        Advancement second = advancement("end/kill_dragon");
        Player player = mock(Player.class);
        setCompletion(player, first, false);
        setCompletion(player, second, false);

        List<Advancement> result = GodTrophyQuest.getIncompleteAdvancements(
                List.of(first, second).iterator(), player);

        assertEquals(List.of(first, second), result);
        assertThrows(UnsupportedOperationException.class, () -> result.add(advancement("story/root")));
    }

    @Test
    void calculatesChecklistPageCounts() {
        assertEquals(1, GodAdvancementUI.calculatePageCount(0));
        assertEquals(1, GodAdvancementUI.calculatePageCount(1));
        assertEquals(1, GodAdvancementUI.calculatePageCount(27));
        assertEquals(2, GodAdvancementUI.calculatePageCount(28));
        assertEquals(2, GodAdvancementUI.calculatePageCount(54));
        assertEquals(3, GodAdvancementUI.calculatePageCount(55));
        assertThrows(IllegalArgumentException.class, () -> GodAdvancementUI.calculatePageCount(-1));
    }

    @Test
    void formatsFallbackAdvancementTitles() {
        assertEquals("Adventure Kill A Mob",
                GodAdvancementUI.formatAdvancementName(new NamespacedKey("minecraft", "adventure/kill_a_mob")));
    }

    private static Advancement advancement(String key) {
        Advancement advancement = mock(Advancement.class);
        when(advancement.getKey()).thenReturn(new NamespacedKey("minecraft", key));
        return advancement;
    }

    private static void setCompletion(Player player, Advancement advancement, boolean completed) {
        AdvancementProgress progress = mock(AdvancementProgress.class);
        when(progress.isDone()).thenReturn(completed);
        when(player.getAdvancementProgress(advancement)).thenReturn(progress);
    }
}
