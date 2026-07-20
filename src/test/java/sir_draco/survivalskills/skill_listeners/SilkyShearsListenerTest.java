package sir_draco.survivalskills.skill_listeners;

import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.rewards.PlayerRewards;
import sir_draco.survivalskills.rewards.Reward;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.skills.SkillManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SilkyShearsListenerTest {

    private final SurvivalSkills plugin = mock(SurvivalSkills.class);
    private final SkillManager skillManager = mock(SkillManager.class);
    private final PlayerRewards playerRewards = mock(PlayerRewards.class);
    private final Reward reward = mock(Reward.class);
    private final Player player = mock(Player.class);
    private final Sheep sheep = mock(Sheep.class);
    private final Entity nonSheep = mock(Entity.class);
    private final World world = mock(World.class);
    private final Location location = mock(Location.class);
    private final ItemStack shears = mock(ItemStack.class);
    private final PlayerShearEntityEvent event = mock(PlayerShearEntityEvent.class);

    @Test
    void dropsTwoAdditionalCopiesOfTheRolledWoolForUnlockedPlayer() {
        prepareUnlockedSheep();
        when(sheep.getColor()).thenReturn(DyeColor.BLUE);
        SilkyShearsListener listener = new SilkyShearsListener(plugin, () -> 3, item -> true);

        listener.onShearSheep(event);

        ArgumentCaptor<ItemStack> dropCaptor = ArgumentCaptor.forClass(ItemStack.class);
        verify(world).dropItemNaturally(eq(location), dropCaptor.capture());
        assertEquals(Material.BLUE_WOOL, dropCaptor.getValue().getType());
        assertEquals(6, dropCaptor.getValue().getAmount());
    }

    @Test
    void doesNotMultiplyDropsBeforeRewardIsUnlocked() {
        prepareSheep();
        when(reward.isApplied()).thenReturn(false);
        SilkyShearsListener listener = new SilkyShearsListener(plugin, () -> 3, item -> true);

        listener.onShearSheep(event);

        verify(world, never()).dropItemNaturally(location, new ItemStack(Material.WHITE_WOOL));
    }

    @Test
    void ignoresEntitiesOtherThanSheep() {
        when(event.getEntity()).thenReturn(nonSheep);
        SilkyShearsListener listener = new SilkyShearsListener(plugin, () -> 3, item -> true);

        listener.onShearSheep(event);

        verify(event, never()).getItem();
    }

    private void prepareUnlockedSheep() {
        prepareSheep();
        when(reward.isApplied()).thenReturn(true);
        when(sheep.getWorld()).thenReturn(world);
        when(sheep.getLocation()).thenReturn(location);
    }

    private void prepareSheep() {
        when(event.getEntity()).thenReturn(sheep);
        when(event.getItem()).thenReturn(shears);
        when(event.getPlayer()).thenReturn(player);
        when(plugin.getSkillManager()).thenReturn(skillManager);
        when(skillManager.getPlayerRewards(player)).thenReturn(playerRewards);
        when(playerRewards.getReward(SkillCategory.CRAFTING, SilkyShearsListener.REWARD_NAME))
                .thenReturn(reward);
    }
}
