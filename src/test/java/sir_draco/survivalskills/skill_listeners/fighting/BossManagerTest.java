package sir_draco.survivalskills.skill_listeners.fighting;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.junit.jupiter.api.Test;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.bosses.Boss;
import sir_draco.survivalskills.bosses.GiantBoss;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BossManagerTest {

    @Test
    void trackedBossIsRecognizedBeforeMetadataIsApplied() {
        BossManager manager = createManager();
        Player summoner = mock(Player.class);
        Boss boss = mock(Boss.class);
        LivingEntity entity = mock(LivingEntity.class);
        when(boss.getBoss()).thenReturn(entity);

        manager.addBoss(summoner, boss);

        assertTrue(manager.isBoss(entity));
    }

    @Test
    void summonerCanDamageTrackedBossDirectly() {
        BossManager manager = createManager();
        Player summoner = mock(Player.class);
        LivingEntity entity = trackBoss(manager, summoner);
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(entity);
        when(event.getDamager()).thenReturn(summoner);

        manager.handleBossDamageByCorrectPlayer(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    void anotherPlayerCannotDamageTrackedBoss() {
        BossManager manager = createManager();
        Player summoner = mock(Player.class);
        Player otherPlayer = mock(Player.class);
        LivingEntity entity = trackBoss(manager, summoner);
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(entity);
        when(event.getDamager()).thenReturn(otherPlayer);

        manager.handleBossDamageByCorrectPlayer(event);

        verify(event).setCancelled(true);
    }

    @Test
    void summonerProjectileCanDamageTrackedBoss() {
        BossManager manager = createManager();
        Player summoner = mock(Player.class);
        LivingEntity entity = trackBoss(manager, summoner);
        Arrow arrow = mock(Arrow.class);
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(arrow.getShooter()).thenReturn(summoner);
        when(event.getEntity()).thenReturn(entity);
        when(event.getDamager()).thenReturn(arrow);

        manager.handleBossDamageByCorrectPlayer(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    void environmentalDamageCannotDamageTrackedBoss() {
        BossManager manager = createManager();
        Player summoner = mock(Player.class);
        LivingEntity entity = trackBoss(manager, summoner);
        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(entity);

        manager.handleBossDamage(event);

        verify(event).setCancelled(true);
    }

    @Test
    void bossKillClearsSummonerInsteadOfDifferentKiller() {
        BossManager manager = createManager();
        Player summoner = mock(Player.class);
        Player killer = mock(Player.class);
        GiantBoss slainBoss = mock(GiantBoss.class);
        LivingEntity slainEntity = mock(LivingEntity.class);
        Boss killersBoss = mock(Boss.class);
        LivingEntity killersEntity = mock(LivingEntity.class);
        EntityDeathEvent event = mock(EntityDeathEvent.class);
        Location deathLocation = mock(Location.class);
        when(slainBoss.getBoss()).thenReturn(slainEntity);
        when(killersBoss.getBoss()).thenReturn(killersEntity);
        when(killersEntity.isDead()).thenReturn(false);
        when(killersEntity.isValid()).thenReturn(true);
        when(event.getEntity()).thenReturn(slainEntity);
        when(slainEntity.getLocation()).thenReturn(deathLocation);

        manager.addBoss(summoner, slainBoss);
        manager.addBoss(killer, killersBoss);
        manager.handleBossKill(killer, event);

        assertFalse(manager.hasActiveBoss(summoner));
        assertTrue(manager.hasActiveBoss(killer));
        verify(slainBoss).death();
    }

    @Test
    void unnaturalBossDeathClearsSummonerByEntity() {
        BossManager manager = createManager();
        Player summoner = mock(Player.class);
        GiantBoss boss = mock(GiantBoss.class);
        LivingEntity entity = mock(LivingEntity.class);
        EntityDeathEvent event = mock(EntityDeathEvent.class);
        when(boss.getBoss()).thenReturn(entity);
        when(event.getEntity()).thenReturn(entity);
        manager.addBoss(summoner, boss);

        manager.handleUnnaturalBossDeath(event);

        assertFalse(manager.hasActiveBoss(summoner));
        verify(boss).death();
        verify(summoner).sendRawMessage(ChatColor.YELLOW + "Your boss died unnaturally");
    }

    @Test
    void staleDeadBossDoesNotBlockAnotherSummon() {
        BossManager manager = createManager();
        Player summoner = mock(Player.class);
        GiantBoss staleBoss = mock(GiantBoss.class);
        LivingEntity deadEntity = mock(LivingEntity.class);
        when(staleBoss.getBoss()).thenReturn(deadEntity);
        when(deadEntity.isDead()).thenReturn(true);
        manager.addBoss(summoner, staleBoss);

        assertFalse(manager.hasActiveBoss(summoner));
        assertTrue(manager.getGiants().isEmpty());
        verify(staleBoss).cleanup();
    }

    @Test
    void liveBossStillBlocksAnotherSummon() {
        BossManager manager = createManager();
        Player summoner = mock(Player.class);
        Boss boss = mock(Boss.class);
        LivingEntity entity = mock(LivingEntity.class);
        when(boss.getBoss()).thenReturn(entity);
        when(entity.isDead()).thenReturn(false);
        when(entity.isValid()).thenReturn(true);
        manager.addBoss(summoner, boss);

        assertTrue(manager.hasActiveBoss(summoner));
        verify(boss, never()).cleanup();
    }

    private static BossManager createManager() {
        return new BossManager(mock(SurvivalSkills.class), mock(DragonManager.class),
                new ArrayList<>(), new ArrayList<>());
    }

    private static LivingEntity trackBoss(BossManager manager, Player summoner) {
        Boss boss = mock(Boss.class);
        LivingEntity entity = mock(LivingEntity.class);
        when(boss.getBoss()).thenReturn(entity);
        when(entity.getType()).thenReturn(EntityType.ZOMBIE);
        manager.addBoss(summoner, boss);
        return entity;
    }
}
