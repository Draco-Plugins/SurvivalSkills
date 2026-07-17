package sir_draco.survivalskills.pipes;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;

import java.util.logging.Level;

public record PipeConfiguration(int transferIntervalTicks, int maxRangeBlocks,
        int linkingTimeSeconds, long maxRangeSquared, long linkingTimeTicks) {
    private static final int DEFAULT_INTERVAL = 20;
    private static final int DEFAULT_RANGE = 250;
    private static final int DEFAULT_LINKING_TIME = 60;

    public static PipeConfiguration load(Plugin plugin, FileConfiguration configuration) {
        int interval = positive(plugin, configuration.getInt("Pipes.TransferIntervalTicks", DEFAULT_INTERVAL),
                DEFAULT_INTERVAL, "transfer interval");
        int range = nonNegative(plugin, configuration.getInt("Pipes.MaxRangeBlocks", DEFAULT_RANGE),
                DEFAULT_RANGE, "maximum range");
        int linkingTime = positive(plugin, configuration.getInt("Pipes.LinkingTimeSeconds", DEFAULT_LINKING_TIME),
                DEFAULT_LINKING_TIME, "linking time");
        return new PipeConfiguration(interval, range, linkingTime,
                (long) range * range, (long) linkingTime * 20);
    }

    private static int positive(Plugin plugin, int value, int fallback, String name) {
        return value > 0 ? value : warn(plugin, fallback, name);
    }

    private static int nonNegative(Plugin plugin, int value, int fallback, String name) {
        return value >= 0 ? value : warn(plugin, fallback, name);
    }

    private static int warn(Plugin plugin, int fallback, String name) {
        plugin.getLogger().log(Level.WARNING, "[SurvivalSkills] Invalid pipe {0}; using {1}",
                new Object[]{name, fallback});
        return fallback;
    }
}
