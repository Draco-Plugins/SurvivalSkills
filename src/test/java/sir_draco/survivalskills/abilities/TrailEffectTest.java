package sir_draco.survivalskills.abilities;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrailEffectTest {

    @Test
    void dragonTrailSuppliesRequiredFloatData() {
        Player player = mock(Player.class);
        World world = mock(World.class);
        Location playerLocation = new Location(world, 1, 2, 3);
        Location particleLocation = playerLocation.clone().add(0, 0.3, 0);

        when(player.getLocation()).thenReturn(playerLocation);
        when(player.getWorld()).thenReturn(world);

        TrailEffect trailEffect = new TrailEffect(player, Particle.DRAGON_BREATH, 1, "Dragon");

        trailEffect.run();

        verify(world).spawnParticle(Particle.DRAGON_BREATH, particleLocation, 0, 0., 0., 0., Float.valueOf(1.0F));
    }
}
