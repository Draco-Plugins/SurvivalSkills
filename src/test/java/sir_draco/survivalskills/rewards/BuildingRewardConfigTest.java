package sir_draco.survivalskills.rewards;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildingRewardConfigTest {

    @Test
    void buildersWandTiersAreEnabledAtTheirSkillTreeLevels() throws IOException {
        YamlConfiguration configuration;
        try (InputStream inputStream = Objects.requireNonNull(
                getClass().getResourceAsStream("/config.yml"), "config.yml is missing");
                InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            configuration = YamlConfiguration.loadConfiguration(reader);
        }

        assertReward(configuration, "BuildersWand", 32, "Crafting");
        assertReward(configuration, "BuildersWandII", 48, "State");
        assertReward(configuration, "BuildersWandIII", 55, "State");
    }

    private static void assertReward(YamlConfiguration configuration, String rewardName, int level,
            String rewardType) {
        String path = "Building." + rewardName;
        assertTrue(configuration.getBoolean(path + ".Enabled"), rewardName + " should be enabled");
        assertEquals(level, configuration.getInt(path + ".Level"));
        assertEquals(rewardType, configuration.getString(path + ".Type"));
    }
}
