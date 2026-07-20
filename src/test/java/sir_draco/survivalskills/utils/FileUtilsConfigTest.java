package sir_draco.survivalskills.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUtilsConfigTest {

    @Test
    void currentConfigRequiresMigrationWhenBuildersWandTierIsMissing() {
        YamlConfiguration configuration = buildersWandConfiguration();
        configuration.set("Building.BuildersWandIII", null);

        assertTrue(FileUtils.requiresConfigUpdate(configuration));
    }

    @Test
    void currentConfigDoesNotRequireMigrationWhenAllBuildersWandTiersExist() {
        YamlConfiguration configuration = buildersWandConfiguration();

        assertFalse(FileUtils.requiresConfigUpdate(configuration));
    }

    private static YamlConfiguration buildersWandConfiguration() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("Version", 2.3);
        configuration.set("Building.BuildersWand.Enabled", true);
        configuration.set("Building.BuildersWandII.Enabled", true);
        configuration.set("Building.BuildersWandIII.Enabled", true);
        return configuration;
    }
}
