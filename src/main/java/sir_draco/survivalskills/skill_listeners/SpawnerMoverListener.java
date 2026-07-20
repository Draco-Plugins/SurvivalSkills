package sir_draco.survivalskills.skill_listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
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
        ItemStack mover = getItemInHand(player, hand);
        if (!SpawnerMover.isSpawnerMover(mover)) {
            return;
        }

        if (mover.getAmount() != 1) {
            event.setCancelled(true);
            sendFailure(player, "Spawner Movers cannot be stacked.");
            return;
        }

        event.setCancelled(true);
        Optional<CreatureSpawner> storedSpawner = SpawnerMover.getStoredSpawner(mover);
        if (!storedSpawner.isPresent()) {
            pickUpSpawner(event.getClickedBlock(), player, hand);
        } else {
            placeSpawner(event, hand, storedSpawner.get());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawnerMoverPlace(BlockPlaceEvent event) {
        ItemStack mover = event.getItemInHand();
        if (!SpawnerMover.isSpawnerMover(mover)) {
            return;
        }

        event.setCancelled(true);
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

    void placeSpawner(PlayerInteractEvent interactEvent, EquipmentSlot hand,
                      CreatureSpawner storedSpawner) {
        Player player = interactEvent.getPlayer();
        Block clickedBlock = interactEvent.getClickedBlock();
        if (clickedBlock == null) return;
        Block target = clickedBlock.getRelative(interactEvent.getBlockFace());
        if (!target.isEmpty()) {
            sendFailure(player, "There is no room to place the stored spawner there.");
            return;
        }
        if (!canChangeBlock(player, target, true)) {
            sendFailure(player, "You cannot place a spawner here.");
            return;
        }

        if (!SpawnerMover.placeSpawner(storedSpawner, target.getLocation())) {
            sendFailure(player, "The stored spawner could not be placed.");
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

    private static ItemStack getItemInHand(Player player, EquipmentSlot hand) {
        return hand.equals(EquipmentSlot.OFF_HAND)
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
    }

    private static void sendFailure(Player player, String message) {
        player.sendRawMessage(ChatColor.RED + message);
        player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
    }
}
