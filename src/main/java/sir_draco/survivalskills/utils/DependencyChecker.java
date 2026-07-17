package sir_draco.survivalskills.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.external.providers.WorldGuardProvider;

/**
 * Checks for the presence and enabled state of optional plugin dependencies
 * at startup, setting the corresponding flags on the main plugin instance.
 */
public final class DependencyChecker {

    private DependencyChecker() {}

    public static void check() {
        SurvivalSkills plugin = SurvivalSkills.getInstance();

        Plugin griefPrevention = Bukkit.getServer().getPluginManager().getPlugin("GriefPrevention");
        if (griefPrevention != null && griefPrevention.isEnabled())
            plugin.setGriefPreventionEnabled(true);

        Plugin worldGuard = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
        if (worldGuard != null && worldGuard.isEnabled()) {
            plugin.setWorldGuardProvider(new WorldGuardProvider());
            plugin.setWorldGuardEnabled(true);
        }

        Plugin citizens = Bukkit.getServer().getPluginManager().getPlugin("Citizens");
        if (citizens != null && citizens.isEnabled())
            plugin.setCitizensEnabled(true);
    }
}
