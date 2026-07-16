package sir_draco.survivalskills.external.providers;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TimbermanProviderTest {

    @Test
    void isUnavailableWithoutBukkitServer() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(null);

            assertFalse(new TimbermanProvider().isAvailable());
        }
    }

    @Test
    void isUnavailableWhenTimbermanIsMissing() {
        Server server = mock(Server.class);
        PluginManager pluginManager = mock(PluginManager.class);
        when(server.getPluginManager()).thenReturn(pluginManager);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);

            assertFalse(new TimbermanProvider().isAvailable());
            verify(pluginManager).getPlugin("Timberman");
        }
    }

    @Test
    void isUnavailableWhenTimbermanIsDisabled() {
        Server server = mock(Server.class);
        PluginManager pluginManager = mock(PluginManager.class);
        Plugin timberman = mock(Plugin.class);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(pluginManager.getPlugin("Timberman")).thenReturn(timberman);
        when(timberman.isEnabled()).thenReturn(false);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);

            assertFalse(new TimbermanProvider().isAvailable());
        }
    }

    @Test
    void isAvailableWhenTimbermanIsEnabled() {
        Server server = mock(Server.class);
        PluginManager pluginManager = mock(PluginManager.class);
        Plugin timberman = mock(Plugin.class);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(pluginManager.getPlugin("Timberman")).thenReturn(timberman);
        when(timberman.isEnabled()).thenReturn(true);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getServer).thenReturn(server);

            assertTrue(new TimbermanProvider().isAvailable());
        }
    }
}
