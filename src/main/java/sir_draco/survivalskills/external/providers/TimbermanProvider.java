package sir_draco.survivalskills.external.providers;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

public final class TimbermanProvider {

    private static final String PLUGIN_NAME = "Timberman";

    public boolean isAvailable() {
        Server server = Bukkit.getServer();
        if (server == null) return false;

        Plugin timberman = server.getPluginManager().getPlugin(PLUGIN_NAME);
        return timberman != null && timberman.isEnabled();
    }
}
