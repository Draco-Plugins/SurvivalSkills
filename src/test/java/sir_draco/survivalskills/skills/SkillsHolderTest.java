package sir_draco.survivalskills.skills;

import org.junit.jupiter.api.Test;
import sir_draco.survivalskills.rewards.PlayerRewards;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertThrows;

class SkillsHolderTest {

    @Test
    void nullPlayerRewardsAreRejected() {
        ArrayList<Skill> skills = new ArrayList<>();

        assertThrows(NullPointerException.class, () -> new SkillsHolder(skills, null));
    }

    @Test
    void nullSkillsAreRejected() {
        PlayerRewards rewards = new PlayerRewards();

        assertThrows(NullPointerException.class, () -> new SkillsHolder(null, rewards));
    }
}
