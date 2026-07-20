package sir_draco.survivalskills.skill_listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.abilities.items.SpawnerMover;
import sir_draco.survivalskills.utils.Utils;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;

import java.util.Optional;

public final class SpawnerMoverListener implements Listener {

    private final SurvivalSkills plugin;

    public SpawnerMoverListener(SurvivalSkills plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawnerMoverUse(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getHand() == null
                || event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        ItemStack mover = ItemStackGeneratorUtils.getItemInHand(player, hand);
        if (!SpawnerMover.isSpawnerMover(mover)) return;

        event.setCancelled(true);
        if (mover.getAmount() != 1) {
            sendFailure(player, "Spawner Movers cannot be stacked.");
            return;
        }

        Optional<CreatureSpawner> storedSpawner = SpawnerMover.getStoredSpawner(mover);
        if (storedSpawner.isPresent()) {
            placeSpawner(event, hand, mover, storedSpawner.get());
        } else {
            pickUpSpawner(event.getClickedBlock(), player, hand);
        }
    }

    private void pickUpSpawner(Block block, Player player, EquipmentSlot hand) {
        if (!(block.getState() instanceof CreatureSpawner spawner)) {
            sendFailure(player, "This Spawner Mover is empty. Right click a spawner to pick it up.");
            return;
        }
        if (!canChangeBlock(player, block, false)) {
            sendFailure(player, "You cannot pick up a spawner here.");
            return;
        }

        ItemStack carryingMover = SpawnerMover.createCarrying(spawner);
        setItemInHand(player, hand, carryingMover);
        block.setType(Material.AIR, false);
        player.playSound(player, Sound.BLOCK_PISTON_CONTRACT, 1.0f, 1.0f);
        player.sendRawMessage(ChatColor.GREEN + "Spawner picked up. Place it before picking up another.");
    }

    private void placeSpawner(PlayerInteractEvent interactEvent, EquipmentSlot hand, ItemStack mover,
                              CreatureSpawner storedSpawner) {
        Player player = interactEvent.getPlayer();
        Block clickedBlock = interactEvent.getClickedBlock();
        if (clickedBlock == null) return;
        Block target = clickedBlock.getRelative(interactEvent.getBlockFace());
        if (!target.getType().isAir()) {
            sendFailure(player, "There is no room to place the stored spawner there.");
            return;
        }
        if (!canChangeBlock(player, target, true)) {
            sendFailure(player, "You cannot place a spawner here.");
            return;
        }

        BlockState replacedState = target.getState();
        if (!SpawnerMover.placeSpawner(storedSpawner, target.getLocation())) {
            sendFailure(player, "The stored spawner could not be placed.");
            return;
        }

        BlockPlaceEvent placeEvent = new BlockPlaceEvent(target, replacedState, clickedBlock, mover,
                player, true, hand);
        plugin.getServer().getPluginManager().callEvent(placeEvent);
        if (placeEvent.isCancelled() || !placeEvent.canBuild()) {
            replacedState.update(true, false);
            sendFailure(player, "You cannot place a spawner here.");
            return;
        }

        setItemInHand(player, hand, SpawnerMover.createEmpty());
        player.playSound(player, Sound.BLOCK_METAL_PLACE, 1.0f, 1.0f);
        player.sendRawMessage(ChatColor.GREEN + "Spawner placed. The Spawner Mover is empty again.");
    }

    private boolean canChangeBlock(Player player, Block block, boolean placing) {
        if (plugin.isGriefPreventionEnabled() && Utils.checkForClaim(player, block.getLocation())) {
            return false;
        }
        if (!plugin.isWorldGuardEnabled() || plugin.getWorldGuardProvider() == null) {
            return true;
        }
        if (placing) {
            return plugin.getWorldGuardProvider().canPlaceBlockInRegion(player, block.getLocation());
        }
        return plugin.getWorldGuardProvider().canBreakBlockInRegion(player, block.getLocation());
    }

    private static void setItemInHand(Player player, EquipmentSlot hand, ItemStack item) {
        if (hand.equals(EquipmentSlot.OFF_HAND)) {
            player.getInventory().setItemInOffHand(item);
        } else {
            player.getInventory().setItemInMainHand(item);
        }
    }

    private static void sendFailure(Player player, String message) {
        player.sendRawMessage(ChatColor.RED + message);
        player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
    }
}
