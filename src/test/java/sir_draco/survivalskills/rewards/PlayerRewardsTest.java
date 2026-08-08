package sir_draco.survivalskills.rewards;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;
import sir_draco.survivalskills.skills.SkillCategory;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerRewardsTest {

    @Test
    void missingRewardCategoryReturnsNullWithoutThrowing() {
        PlayerRewards rewards = new PlayerRewards();

        assertNull(rewards.getReward(SkillCategory.MINING, "MissingReward"));
        assertNull(rewards.getLevelReward(SkillCategory.MINING, 1));
    }

    @Test
    void nullRewardListIsRejected() {
        HashMap<SkillCategory, ArrayList<Reward>> rewardList = null;

        assertThrows(NullPointerException.class, () -> new PlayerRewards(rewardList));
    }

    @Test
    void onlyTheMiningPowerOreRewardGrantsTheGuide() {
        assertTrue(PlayerRewards.grantsPowerOreGuide(SkillCategory.MINING, "PowerOre"));
        assertFalse(PlayerRewards.grantsPowerOreGuide(SkillCategory.MINING, "ZapWand"));
        assertFalse(PlayerRewards.grantsPowerOreGuide(SkillCategory.EXPLORING, "PowerOre"));
    }

    @Test
    void fullInventoryDropsTheGuideAtThePlayersFeet() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack guide = mock(ItemStack.class);
        World world = mock(World.class);
        Location location = mock(Location.class);
        when(player.getInventory()).thenReturn(inventory);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(location);
        HashMap<Integer, ItemStack> leftovers = new HashMap<>();
        leftovers.put(0, guide);
        when(inventory.addItem(guide)).thenReturn(leftovers);

        PlayerRewards.giveOrDropItem(player, guide);

        verify(world).dropItemNaturally(location, guide);
    }
}
