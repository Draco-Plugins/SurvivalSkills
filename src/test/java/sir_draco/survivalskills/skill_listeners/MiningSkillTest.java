package sir_draco.survivalskills.skill_listeners;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.abilities.AbilityManager;
import sir_draco.survivalskills.abilities.VeinMinerAsync;
import sir_draco.survivalskills.abilities.items.PowerDrillTask;
import sir_draco.survivalskills.rewards.PlayerRewards;
import sir_draco.survivalskills.skills.SkillManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MiningSkillTest {

    @Test
    void unlimitedTorchRunsAfterSpawnerMoverAndIgnoresCancelledInteractions() throws NoSuchMethodException {
        EventHandler eventHandler = MiningSkill.class
                .getMethod("placeTorch", PlayerInteractEvent.class)
                .getAnnotation(EventHandler.class);

        assertEquals(EventPriority.HIGHEST, eventHandler.priority());
        assertTrue(eventHandler.ignoreCancelled());
    }

    @Test
    void cancelledInteractionCannotPlaceUnlimitedTorch() {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        when(plugin.getName()).thenReturn("SurvivalSkills");
        MiningSkill miningSkill = new MiningSkill(plugin, 0);
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.useInteractedBlock()).thenReturn(Result.DENY);

        miningSkill.placeTorch(event);

        verify(event, never()).getPlayer();
    }

    @Test
    void spawnerItemCannotBeClassifiedAsUnlimitedTorch() {
        ItemStack spawnerMover = mock(ItemStack.class);
        when(spawnerMover.getType()).thenReturn(Material.SPAWNER);

        assertFalse(MiningSkill.isUnlimitedTorch(spawnerMover));
    }

    @Test
    void unlimitedTorchDoesNotDropWhenBroken() {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        when(plugin.getName()).thenReturn("SurvivalSkills");
        MiningSkill miningSkill = new MiningSkill(plugin, 0);
        BlockBreakEvent event = mock(BlockBreakEvent.class);
        Block block = mock(Block.class);
        Chunk chunk = mock(Chunk.class);
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        when(event.getBlock()).thenReturn(block);
        when(block.getChunk()).thenReturn(chunk);
        when(chunk.getPersistentDataContainer()).thenReturn(data);
        when(block.getX()).thenReturn(1);
        when(block.getY()).thenReturn(64);
        when(block.getZ()).thenReturn(2);
        when(data.get(any(NamespacedKey.class), eq(PersistentDataType.LONG_ARRAY)))
                .thenReturn(new long[] { ((long) 1 << 36) | ((long) 2 << 32) | 64 });

        miningSkill.preventUnlimitedTorchDrops(event);

        verify(event).setDropItems(false);
        verify(data).remove(any(NamespacedKey.class));
    }

    @Test
    void ordinaryTorchKeepsItsDrops() {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        when(plugin.getName()).thenReturn("SurvivalSkills");
        MiningSkill miningSkill = new MiningSkill(plugin, 0);
        BlockBreakEvent event = mock(BlockBreakEvent.class);
        Block block = mock(Block.class);
        Chunk chunk = mock(Chunk.class);
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        when(event.getBlock()).thenReturn(block);
        when(block.getChunk()).thenReturn(chunk);
        when(chunk.getPersistentDataContainer()).thenReturn(data);

        miningSkill.preventUnlimitedTorchDrops(event);

        verify(event, never()).setDropItems(false);
        verify(data, never()).remove(any(NamespacedKey.class));
    }

    @Test
    void unlimitedTorchPositionIsStoredInChunkData() {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        when(plugin.getName()).thenReturn("SurvivalSkills");
        MiningSkill miningSkill = new MiningSkill(plugin, 0);
        Block block = mock(Block.class);
        Chunk chunk = mock(Chunk.class);
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        when(block.getChunk()).thenReturn(chunk);
        when(chunk.getPersistentDataContainer()).thenReturn(data);
        when(block.getX()).thenReturn(1);
        when(block.getY()).thenReturn(64);
        when(block.getZ()).thenReturn(2);
        when(data.getOrDefault(any(NamespacedKey.class), eq(PersistentDataType.LONG_ARRAY), any(long[].class)))
                .thenReturn(new long[0]);

        miningSkill.markUnlimitedTorch(block);

        verify(data).set(any(NamespacedKey.class), eq(PersistentDataType.LONG_ARRAY),
                eq(new long[] { ((long) 1 << 36) | ((long) 2 << 32) | 64 }));
    }

    @Test
    void naturallyGeneratedAncientDebrisCanBeDoubled() {
        assertTrue(MiningSkill.canDoubleOreDrops(
                Material.ANCIENT_DEBRIS, List.of(new ItemStack(Material.ANCIENT_DEBRIS)), false));
    }

    @Test
    void playerPlacedAncientDebrisCannotBeDoubled() {
        assertFalse(MiningSkill.canDoubleOreDrops(
                Material.ANCIENT_DEBRIS, List.of(new ItemStack(Material.ANCIENT_DEBRIS)), true));
    }

    @Test
    void coalCanBeDoubledWhenCoalOreReturnsCoal() {
        assertTrue(MiningSkill.canDoubleOreDrops(
                Material.COAL_ORE, List.of(new ItemStack(Material.COAL)), false));
    }

    @Test
    void coalOreCannotBeDoubledWhenSilkTouchReturnsTheBlock() {
        assertFalse(MiningSkill.canDoubleOreDrops(
                Material.COAL_ORE, List.of(new ItemStack(Material.COAL_ORE)), false));
    }

    @Test
    void veinMinerDoubleOreUsesCapturedToolAfterHeldItemChanges() {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        SkillManager skillManager = mock(SkillManager.class);
        PlayerRewards rewards = mock(PlayerRewards.class);
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        BlockBreakEvent event = mock(BlockBreakEvent.class);
        Block block = mock(Block.class);
        World world = mock(World.class);
        Location location = mock(Location.class);
        MetadataValue metadataValue = mock(MetadataValue.class);
        ItemStack pickaxe = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemStack magnet = new ItemStack(Material.HOPPER);
        ItemStack diamond = mock(ItemStack.class);
        Material diamondMaterial = mock(Material.class);
        when(plugin.getName()).thenReturn("SurvivalSkills");
        when(plugin.getSkillManager()).thenReturn(skillManager);
        when(skillManager.getPlayerRewards(player)).thenReturn(rewards);
        when(rewards.getFortuneChance()).thenReturn(1.0);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getItemInMainHand()).thenReturn(magnet);
        when(player.getMetadata(VeinMinerAsync.VEIN_MINER_BREAK_METADATA)).thenReturn(List.of(metadataValue));
        when(metadataValue.getOwningPlugin()).thenReturn(plugin);
        when(metadataValue.value()).thenReturn(pickaxe);
        when(event.getBlock()).thenReturn(block);
        when(block.getType()).thenReturn(Material.DIAMOND_ORE);
        when(block.getDrops(pickaxe)).thenReturn(List.of(diamond));
        when(diamond.getType()).thenReturn(diamondMaterial);
        when(diamond.getAmount()).thenReturn(1);
        when(diamondMaterial.isAir()).thenReturn(false);
        when(block.getWorld()).thenReturn(world);
        when(block.getLocation()).thenReturn(location);
        MiningSkill miningSkill = new MiningSkill(plugin, 1);

        miningSkill.doubleOre(player, event);

        verify(block).getDrops(pickaxe);
        verify(block, never()).getDrops(magnet);
        verify(event).setDropItems(false);
        verify(diamond).setAmount(2);
        verify(world).dropItemNaturally(location, diamond);
    }

    @Test
    void playerPlacedAncientDebrisIsTrackedInChunkData() {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        when(plugin.getName()).thenReturn("SurvivalSkills");
        MiningSkill miningSkill = new MiningSkill(plugin, 0);
        BlockPlaceEvent event = mock(BlockPlaceEvent.class);
        Block block = mock(Block.class);
        Chunk chunk = mock(Chunk.class);
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        when(event.getBlockPlaced()).thenReturn(block);
        when(block.getType()).thenReturn(Material.ANCIENT_DEBRIS);
        when(block.getChunk()).thenReturn(chunk);
        when(chunk.getPersistentDataContainer()).thenReturn(data);
        when(block.getX()).thenReturn(1);
        when(block.getY()).thenReturn(64);
        when(block.getZ()).thenReturn(2);
        when(data.getOrDefault(any(NamespacedKey.class), eq(PersistentDataType.LONG_ARRAY), any(long[].class)))
                .thenReturn(new long[0]);

        miningSkill.trackPlayerPlacedAncientDebris(event);

        verify(data).set(eq(new NamespacedKey(plugin, "player_placed_ancient_debris")),
                eq(PersistentDataType.LONG_ARRAY),
                eq(new long[] { ((long) 1 << 36) | ((long) 2 << 32) | 64 }));
    }

    @Test
    void drillGeneratedBreakDoesNotStartVeinMiner() {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        when(plugin.getName()).thenReturn("SurvivalSkills");
        MiningSkill miningSkill = new MiningSkill(plugin, 1);
        Player player = mock(Player.class);
        BlockBreakEvent event = mock(BlockBreakEvent.class);
        when(player.hasMetadata(PowerDrillTask.DRILL_BREAK_METADATA)).thenReturn(true);

        miningSkill.veinminerChecker(player, event);

        verifyNoInteractions(event);
    }

    @Test
    void powerDrillIsRejectedByVeinMinerAdmissionPolicy() {
        assertTrue(MiningSkill.shouldSkipVeinMiner(true, false));
    }

    @Test
    void weaponsRequireTheToolBeltUpgrade() {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        AbilityManager abilityManager = mock(AbilityManager.class);
        Player player = mock(Player.class);
        when(plugin.getName()).thenReturn("SurvivalSkills");
        when(plugin.getAbilityManager()).thenReturn(abilityManager);
        MiningSkill miningSkill = new MiningSkill(plugin, 0);

        when(abilityManager.hasToolBeltUpgrade(player)).thenReturn(false);
        assertFalse(miningSkill.isAcceptableToolBeltItem(player, new ItemStack(Material.DIAMOND_SWORD)));

        when(abilityManager.hasToolBeltUpgrade(player)).thenReturn(true);
        assertTrue(miningSkill.isAcceptableToolBeltItem(player, new ItemStack(Material.DIAMOND_SWORD)));
    }

    @Test
    void toolBeltUpgradeAcceptsEverySupportedWeaponType() {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        AbilityManager abilityManager = mock(AbilityManager.class);
        Player player = mock(Player.class);
        when(plugin.getName()).thenReturn("SurvivalSkills");
        when(plugin.getAbilityManager()).thenReturn(abilityManager);
        when(abilityManager.hasToolBeltUpgrade(player)).thenReturn(true);
        MiningSkill miningSkill = new MiningSkill(plugin, 0);

        List<Material> supportedWeapons = List.of(
                Material.WOODEN_SWORD,
                Material.STONE_SWORD,
                Material.IRON_SWORD,
                Material.GOLDEN_SWORD,
                Material.DIAMOND_SWORD,
                Material.NETHERITE_SWORD,
                Material.BOW,
                Material.CROSSBOW,
                Material.MACE,
                Material.TRIDENT);

        assertTrue(supportedWeapons.stream().allMatch((Material material) ->
                miningSkill.isAcceptableToolBeltItem(player, new ItemStack(material))));
    }

    @Test
    void toolsRemainAcceptedWithoutTheToolBeltUpgrade() {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        Player player = mock(Player.class);
        when(plugin.getName()).thenReturn("SurvivalSkills");
        MiningSkill miningSkill = new MiningSkill(plugin, 0);

        assertTrue(miningSkill.isAcceptableToolBeltItem(player, new ItemStack(Material.DIAMOND_PICKAXE)));
    }
}
