package sir_draco.survivalskills.abilities.items;

import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PowerSwordTest {

    @Test
    void selectsNearbyEnemyOnlyOncePerDash() {
        Enemy enemy = mock(Enemy.class);
        when(enemy.getEntityId()).thenReturn(1);
        Set<Integer> damagedEntityIds = new HashSet<>();

        List<Enemy> firstPass = PowerSword.findNewEnemies(List.of((Entity) enemy), damagedEntityIds);
        List<Enemy> secondPass = PowerSword.findNewEnemies(List.of((Entity) enemy), damagedEntityIds);

        assertEquals(List.of(enemy), firstPass);
        assertTrue(secondPass.isEmpty());
    }

    @Test
    void ignoresNonEnemyMobs() {
        Mob passiveMob = mock(Mob.class);

        List<Enemy> enemies = PowerSword.findNewEnemies(List.of(passiveMob), new HashSet<>());

        assertTrue(enemies.isEmpty());
    }
}
