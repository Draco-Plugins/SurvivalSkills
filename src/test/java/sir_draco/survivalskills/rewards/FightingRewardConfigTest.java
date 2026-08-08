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

class FightingRewardConfigTest {

    @Test
    void toolBeltUpgradeIsEnabledAtFightingLevelSixtyTwo() throws IOException {
        YamlConfiguration configuration;
        try (InputStream inputStream = Objects.requireNonNull(
                getClass().getResourceAsStream("/config.yml"), "config.yml is missing");
                InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            configuration = YamlConfiguration.loadConfiguration(reader);
        }

        String path = "Fighting.ToolBeltII";
        assertTrue(configuration.getBoolean(path + ".Enabled"));
        assertEquals(62, configuration.getInt(path + ".Level"));
        assertEquals("State", configuration.getString(path + ".Type"));
    }
}
