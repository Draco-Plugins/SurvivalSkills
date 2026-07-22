package sir_draco.survivalskills.god_questline.trial;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrialDataPersistenceTest {

    @Test
    void removeTrialBuildingDataPreservesCompletedTrials() {
        UUID buildingId = UUID.randomUUID();
        String key = buildingId.toString();
        YamlConfiguration config = new YamlConfiguration();
        config.set(key + ".Owner", buildingId.toString());
        config.set(key + ".LastUsed", 123L);
        config.set(key + ".Location", "world:1:2:3");
        config.set(key + ".ProtectedArea.Min", "1:2:3");
        config.set(key + ".ProtectedArea.Max", "4:5:6");
        config.set(key + ".ProtectedArea.World", "world");
        config.set(key + ".CompletedTrials", List.of(1, 2));

        TrialDataPersistence.removeTrialBuildingData(config, buildingId);

        assertFalse(config.contains(key + ".Owner"));
        assertFalse(config.contains(key + ".LastUsed"));
        assertFalse(config.contains(key + ".Location"));
        assertFalse(config.contains(key + ".ProtectedArea"));
        assertEquals(List.of(1, 2), config.getIntegerList(key + ".CompletedTrials"));
    }

    @Test
    void removeTrialBuildingDataRemovesEmptyPlayerSection() {
        UUID buildingId = UUID.randomUUID();
        String key = buildingId.toString();
        YamlConfiguration config = new YamlConfiguration();
        config.set(key + ".Owner", buildingId.toString());
        config.set(key + ".LastUsed", 123L);
        config.set(key + ".Location", "world:1:2:3");
        config.set(key + ".ProtectedArea.Min", "1:2:3");
        config.set(key + ".ProtectedArea.Max", "4:5:6");
        config.set(key + ".ProtectedArea.World", "world");

        TrialDataPersistence.removeTrialBuildingData(config, buildingId);

        assertTrue(config.getKeys(false).isEmpty());
    }
}
