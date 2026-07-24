package sir_draco.survivalskills.pipes;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.Utils;
import sir_draco.survivalskills.utils.items.ItemStackGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PipeListenerTest {
    @Test
    void nonOwnerCanAccessPipeWhenGriefPreventionIsDisabled() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
                MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            PipeInteraction interaction = pipeInteraction(bukkit);
            when(interaction.plugin().isGriefPreventionEnabled()).thenReturn(false);

            interaction.listener().interactWithChest(interaction.event(), interaction.player(), interaction.block());

            verify(interaction.event()).setCancelled(true);
            verify(interaction.manager()).status(interaction.pipeUuid());
            utils.verifyNoInteractions();
        }
    }

    @Test
    void playerWithoutClaimBuildAccessCannotAccessPipe() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
                MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            PipeInteraction interaction = pipeInteraction(bukkit);
            when(interaction.plugin().isGriefPreventionEnabled()).thenReturn(true);
            utils.when(() -> Utils.checkForClaim(interaction.player(), interaction.location())).thenReturn(true);

            interaction.listener().interactWithChest(interaction.event(), interaction.player(), interaction.block());

            verify(interaction.event()).setCancelled(true);
            verify(interaction.manager(), never()).getPipe(interaction.pipeLocation());
            verify(interaction.player()).sendMessage(ChatColor.RED
                    + "You do not have build access to pipes in this claim.");
        }
    }

    @Test
    void nonOwnerWithClaimBuildAccessCanAccessPipe() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
                MockedStatic<Utils> utils = mockStatic(Utils.class)) {
            PipeInteraction interaction = pipeInteraction(bukkit);
            when(interaction.plugin().isGriefPreventionEnabled()).thenReturn(true);
            utils.when(() -> Utils.checkForClaim(interaction.player(), interaction.location())).thenReturn(false);

            interaction.listener().interactWithChest(interaction.event(), interaction.player(), interaction.block());

            verify(interaction.event()).setCancelled(true);
            verify(interaction.manager()).status(interaction.pipeUuid());
        }
    }

    @Test
    void senderRemovalRequiresASecondSneakLeftClick() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class);
                MockedStatic<Utils> utils = mockStatic(Utils.class);
                MockedStatic<ItemStackGenerator> itemStackGenerator = mockStatic(ItemStackGenerator.class)) {
            PipeInteraction interaction = pipeInteraction(bukkit);
            ItemStack transferPipe = mock(ItemStack.class);
            itemStackGenerator.when(ItemStackGenerator::getTransferPipe).thenReturn(transferPipe);
            when(interaction.plugin().isGriefPreventionEnabled()).thenReturn(false);
            when(interaction.event().getAction()).thenReturn(Action.LEFT_CLICK_BLOCK);
            when(interaction.player().isSneaking()).thenReturn(true);
            when(interaction.manager().remove(interaction.pipeUuid())).thenReturn(Optional.of(interaction.sender()));

            interaction.listener().interactWithChest(interaction.event(), interaction.player(), interaction.block());

            verify(interaction.manager(), never()).remove(interaction.pipeUuid());
            verify(interaction.player()).sendMessage(ChatColor.RED
                    + "Are you sure you want to remove this sender pipe? Shift-left-click it again to confirm.");

            interaction.listener().interactWithChest(interaction.event(), interaction.player(), interaction.block());

            verify(interaction.manager()).remove(interaction.pipeUuid());
            verify(interaction.player()).sendMessage(ChatColor.GREEN + "Pipe removed.");
            verify(interaction.inventory()).addItem(transferPipe);
        }
    }

    private static PipeInteraction pipeInteraction(MockedStatic<Bukkit> bukkit) {
        SurvivalSkills plugin = mock(SurvivalSkills.class);
        PipeManager manager = mock(PipeManager.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        PipeConfiguration configuration = new PipeConfiguration(20, 250, 60, 62_500, 1_200);
        PipeListener listener = new PipeListener(plugin, manager, configuration);

        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        Block block = mock(Block.class);
        Location location = mock(Location.class);
        World world = mock(World.class);
        UUID worldUuid = UUID.randomUUID();
        UUID pipeUuid = UUID.randomUUID();
        UUID ownerUuid = UUID.randomUUID();
        UUID playerUuid = UUID.randomUUID();
        PipeLocation pipeLocation = new PipeLocation(worldUuid, 10, 64, 20);
        PipeRecord sender = new PipeRecord(pipeUuid, ownerUuid, pipeLocation, PipeType.SENDER,
                Optional.empty(), List.of(), Set.of());

        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(player.getUniqueId()).thenReturn(playerUuid);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.addItem(org.mockito.ArgumentMatchers.any(ItemStack.class))).thenReturn(new HashMap<>());
        when(block.getType()).thenReturn(Material.CHEST);
        when(block.getLocation()).thenReturn(location);
        when(block.getWorld()).thenReturn(world);
        when(block.getX()).thenReturn(pipeLocation.x());
        when(block.getY()).thenReturn(pipeLocation.y());
        when(block.getZ()).thenReturn(pipeLocation.z());
        when(world.getUID()).thenReturn(worldUuid);
        when(manager.getPipe(pipeLocation)).thenReturn(Optional.of(sender));
        when(manager.status(pipeUuid)).thenReturn(new PipeManager.SenderStatus(0, 0, PipeManager.Activity.ACTIVE));

        return new PipeInteraction(plugin, listener, manager, event, player, inventory, block, location,
                pipeLocation, pipeUuid, sender);
    }

    private record PipeInteraction(SurvivalSkills plugin, PipeListener listener, PipeManager manager,
            PlayerInteractEvent event, Player player, PlayerInventory inventory, Block block, Location location,
            PipeLocation pipeLocation, UUID pipeUuid, PipeRecord sender) {}
}
