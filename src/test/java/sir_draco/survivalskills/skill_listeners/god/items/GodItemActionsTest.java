package sir_draco.survivalskills.skill_listeners.god.items;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GodItemActionsTest {

    @Test
    void snowballCannonCancelsInteractionAndLaunchesSnowball() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        Location location = new Location(world, 0, 0, 0, 0, 0);
        ItemStack item = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(player.getLocation()).thenReturn(location);

        new GodItemActions.SnowballCannonItemAction().execute(player, item, meta, event);

        verify(event).setCancelled(true);
        verify(player).launchProjectile(eq(Snowball.class), eq(new Vector(0, 0, 2)));
    }

    @Test
    void witherSkullCannonCancelsInteractionAndLaunchesWitherSkull() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        Location location = new Location(world, 0, 0, 0, 0, 0);
        ItemStack item = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(player.getLocation()).thenReturn(location);

        new GodItemActions.WitherSkullCannonItemAction().execute(player, item, meta, event);

        verify(event).setCancelled(true);
        verify(player).launchProjectile(eq(WitherSkull.class), eq(new Vector(0, 0, 2)));
    }

    @Test
    void fireballCannonCancelsInteractionAndLaunchesFireball() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        Location location = new Location(world, 0, 0, 0, 0, 0);
        ItemStack item = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(player.getLocation()).thenReturn(location);

        new GodItemActions.FireballCannonItemAction().execute(player, item, meta, event);

        verify(event).setCancelled(true);
        verify(player).launchProjectile(eq(Fireball.class), eq(new Vector(0, 0, 2)));
    }
}
