package sir_draco.survivalskills.rewards;

import org.junit.jupiter.api.Test;
import sir_draco.survivalskills.skills.SkillCategory;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
