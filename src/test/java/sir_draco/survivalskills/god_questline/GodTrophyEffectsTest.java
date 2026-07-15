package sir_draco.survivalskills.god_questline;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GodTrophyEffectsTest {

    private static final int TROPHY_ID = 42;
    private static final double CLEANUP_RADIUS = 3.5;

    private SurvivalSkills plugin;
    private World world;
    private Location trophyLocation;

    @BeforeEach
    void setUp() {
        plugin = mock(SurvivalSkills.class);
        when(plugin.getName()).thenReturn("SurvivalSkills");
        world = mock(World.class);
        trophyLocation = new Location(world, 10, 64, 20);
    }

    @Test
    void removeCleansOwnedCrystalAcrossFullOrbit() {
        EnderCrystal ownedCrystal = crystalWithOwner(TROPHY_ID);
        when(world.getNearbyEntities(any(Location.class), eq(CLEANUP_RADIUS), eq(CLEANUP_RADIUS),
                eq(CLEANUP_RADIUS))).thenReturn(List.of(ownedCrystal));

        createEffects().remove();

        verify(ownedCrystal).remove();
        verify(world).getNearbyEntities(any(Location.class), eq(CLEANUP_RADIUS), eq(CLEANUP_RADIUS),
                eq(CLEANUP_RADIUS));
    }

    @Test
    void removeDoesNotTouchAnotherTrophysCrystal() {
        EnderCrystal otherCrystal = crystalWithOwner(TROPHY_ID + 1);
        when(world.getNearbyEntities(any(Location.class), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(otherCrystal));

        createEffects().remove();

        verify(otherCrystal, never()).remove();
    }

    @Test
    void removeMigratesLegacyCrystalByBeamAndVisualSignature() {
        EnderCrystal legacyCrystal = crystalWithOwner(null);
        when(legacyCrystal.isInvulnerable()).thenReturn(true);
        when(legacyCrystal.isShowingBottom()).thenReturn(false);
        when(legacyCrystal.getBeamTarget()).thenReturn(trophyLocation.clone().add(0.5, 2, 0.5));
        when(world.getNearbyEntities(any(Location.class), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(legacyCrystal));

        createEffects().remove();

        verify(legacyCrystal).remove();
    }

    @Test
    void idleInitializationRetainsOneOwnedCrystalAndRemovesDuplicates() {
        EnderCrystal retainedCrystal = crystalWithOwner(TROPHY_ID);
        EnderCrystal duplicateCrystal = crystalWithOwner(TROPHY_ID);
        when(world.getNearbyEntities(any(Location.class), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(retainedCrystal, duplicateCrystal));

        createEffects().tickTrophy(122);

        verify(retainedCrystal, never()).remove();
        verify(duplicateCrystal).remove();
        verify(retainedCrystal).setPersistent(true);
        verify(world, never()).spawnEntity(any(Location.class), eq(EntityType.END_CRYSTAL));
    }

    @Test
    void idleInitializationSpawnsAndConfiguresExactlyOneCrystal() {
        EnderCrystal spawnedCrystal = crystalWithOwner(null);
        when(world.getNearbyEntities(any(Location.class), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of());
        when(world.spawnEntity(any(Location.class), eq(EntityType.END_CRYSTAL))).thenReturn(spawnedCrystal);
        GodTrophyEffects effects = createEffects();

        effects.tickTrophy(122);
        effects.tickTrophy(123);

        ArgumentCaptor<Location> locationCaptor = ArgumentCaptor.forClass(Location.class);
        verify(world, times(1)).spawnEntity(locationCaptor.capture(), eq(EntityType.END_CRYSTAL));
        assertEquals(10.5, locationCaptor.getValue().getX());
        assertEquals(65.5, locationCaptor.getValue().getY());
        assertEquals(20.5, locationCaptor.getValue().getZ());
        verify(spawnedCrystal).setPersistent(true);
        verify(spawnedCrystal.getPersistentDataContainer()).set(any(NamespacedKey.class),
                eq(PersistentDataType.INTEGER), eq(TROPHY_ID));
    }

    @Test
    void startupCleanupRemovesOwnedAndLegacyOrphansWithoutTouchingActiveOrOrdinaryCrystals() {
        EnderCrystal ownedOrphan = crystalWithOwner(TROPHY_ID + 1);
        EnderCrystal activeCrystal = crystalWithOwner(TROPHY_ID);
        when(activeCrystal.getLocation()).thenReturn(trophyLocation.clone().add(3, 1.5, 0.5));
        EnderCrystal legacyOrphan = crystalWithOwner(null);
        when(legacyOrphan.isInvulnerable()).thenReturn(true);
        when(legacyOrphan.isShowingBottom()).thenReturn(false);
        when(legacyOrphan.getBeamTarget()).thenReturn(new Location(world, 100.5, 70, 100.5));
        EnderCrystal ordinaryCrystal = crystalWithOwner(null);
        when(world.getEntities()).thenReturn(List.of(ownedOrphan, activeCrystal, legacyOrphan, ordinaryCrystal));

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getWorlds).thenReturn(List.of(world));
            GodTrophyEffects.removeOrphanedCrystals(plugin, Map.of(TROPHY_ID, trophyLocation));
        }

        verify(ownedOrphan).remove();
        verify(legacyOrphan).remove();
        verify(activeCrystal, never()).remove();
        verify(ordinaryCrystal, never()).remove();
    }

    private GodTrophyEffects createEffects() {
        return new GodTrophyEffects(plugin, trophyLocation, TROPHY_ID, "Player", UUID.randomUUID());
    }

    private EnderCrystal crystalWithOwner(Integer ownerId) {
        EnderCrystal crystal = mock(EnderCrystal.class);
        PersistentDataContainer persistentDataContainer = mock(PersistentDataContainer.class);
        when(crystal.getPersistentDataContainer()).thenReturn(persistentDataContainer);
        when(persistentDataContainer.get(any(NamespacedKey.class), eq(PersistentDataType.INTEGER)))
                .thenReturn(ownerId);
        return crystal;
    }
}
