package sir_draco.survivalskills.pipes;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.Utils;
import sir_draco.survivalskills.utils.items.ItemModelData;
import sir_draco.survivalskills.utils.items.ItemStackGenerator;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;

import java.util.*;

public final class PipeListener implements Listener {
    private static final String FILTER_TITLE = "Pipe Filter";
    private static final long[] THRESHOLDS = {600, 200, 60, 0};
    private record LinkingSession(UUID senderUuid, long deadline, Set<Long> sentThresholds) {}

    private final SurvivalSkills plugin;
    private final PipeManager manager;
    private final PipeConfiguration configuration;
    private final Map<UUID, LinkingSession> sessions = new HashMap<>();
    private final Map<UUID, UUID> openFilters = new HashMap<>();
    private long tick;

    public PipeListener(SurvivalSkills plugin, PipeManager manager, PipeConfiguration configuration) {
        this.plugin = plugin;
        this.manager = manager;
        this.configuration = configuration;
        Bukkit.getScheduler().runTaskTimer(plugin, this::timerTick, 1, 1);
    }

    @EventHandler
    public void interact(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !isWrench(event.getPlayer().getInventory().getItemInMainHand())) return;
        Player player = event.getPlayer();
        if (!PipeRewardGate.isUnlocked(plugin, player)) {
            event.setCancelled(true);
            PipeRewardGate.sendLockedMessage(plugin, player);
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_AIR) {
            event.setCancelled(true);
            showLinkingHud(player);
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || !isChest(block)) return;
        interactWithChest(event, player, block);
    }

    void interactWithChest(PlayerInteractEvent event, Player player, Block block) {
        if (!hasPipeAccess(player, block)) {
            event.setCancelled(true);
            return;
        }
        PipeLocation clicked = location(block);
        Optional<PipeRecord> pipe = manager.getPipe(clicked);
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (pipe.isEmpty()) return;
            event.setCancelled(true);
            PipeRecord record = pipe.orElseThrow();
            if (record.type() == PipeType.RECEIVER) {
                openFilter(player, record);
            } else {
                showSenderHud(player, record, block);
            }
            return;
        }
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        event.setCancelled(true);
        if (player.isSneaking()) {
            if (pipe.isPresent()) remove(player, pipe.orElseThrow());
            else {
                Optional<PipeRecord> sender = activeSender(player);
                if (sender.isPresent()) attachReceiver(player, block, sender.orElseThrow());
                else attachSender(player, block);
            }
            return;
        }
        if (pipe.isPresent()) {
            PipeRecord record = pipe.orElseThrow();
            if (record.type() == PipeType.SENDER) selectSender(player, record);
            else if (record.senderUuid().isEmpty() && activeSender(player).isPresent()) relink(player, record);
            return;
        }
        activeSender(player).ifPresent(sender -> attachReceiver(player, block, sender));
    }

    private void attachSender(Player player, Block block) {
        if (!consumePipe(player)) {
            player.sendMessage(ChatColor.RED + "You need a Transfer Pipe.");
            return;
        }
        try {
            PipeRecord sender = manager.attach(player.getUniqueId(), block, PipeType.SENDER, Optional.empty());
            resetSession(player.getUniqueId(), sender.pipeUuid());
            player.sendMessage(ChatColor.GREEN + "Sender pipe attached. Linking mode active for "
                    + configuration.linkingTimeSeconds() + " seconds.");
        } catch (RuntimeException exception) {
            givePipe(player);
            player.sendMessage(ChatColor.RED + exception.getMessage());
        }
    }

    private void attachReceiver(Player player, Block block, PipeRecord sender) {
        if (!manager.isWithinRange(sender.location(), location(block))) {
            player.sendMessage(ChatColor.RED + "That chest is outside the pipe range.");
            return;
        }
        if (!consumePipe(player)) {
            player.sendMessage(ChatColor.RED + "You need a Transfer Pipe.");
            return;
        }
        try {
            PipeRecord receiver = manager.attach(player.getUniqueId(), block, PipeType.RECEIVER,
                    Optional.of(sender.pipeUuid()));
            resetSession(player.getUniqueId(), sender.pipeUuid());
            openFilter(player, receiver);
        } catch (RuntimeException exception) {
            givePipe(player);
            player.sendMessage(ChatColor.RED + exception.getMessage());
        }
    }

    private void relink(Player player, PipeRecord receiver) {
        PipeRecord sender = activeSender(player).orElseThrow();
        if (manager.relink(receiver.pipeUuid(), sender.pipeUuid())) {
            resetSession(player.getUniqueId(), sender.pipeUuid());
            player.sendMessage(ChatColor.GREEN + "Receiver linked.");
        } else player.sendMessage(ChatColor.RED + "The receiver could not be linked.");
    }

    private void selectSender(Player player, PipeRecord sender) {
        if (!manager.isWithinRange(sender.location(), location(player.getLocation().getBlock()))) {
            player.sendMessage(ChatColor.RED + "That sender is too far away.");
            return;
        }
        resetSession(player.getUniqueId(), sender.pipeUuid());
        player.sendMessage(ChatColor.GREEN + "Sender selected; linking timer reset.");
    }

    private void remove(Player player, PipeRecord record) {
        manager.remove(record.pipeUuid());
        givePipe(player);
        sessions.entrySet().removeIf(entry -> entry.getValue().senderUuid().equals(record.pipeUuid()));
        player.sendMessage(ChatColor.GREEN + "Pipe removed.");
    }

    @EventHandler
    public void breakPipe(BlockBreakEvent event) {
        removeBrokenPipe(event.getBlock());
    }

    @EventHandler
    public void explode(BlockExplodeEvent event) {
        event.blockList().forEach(this::removeBrokenPipe);
    }

    @EventHandler
    public void explode(EntityExplodeEvent event) {
        event.blockList().forEach(this::removeBrokenPipe);
    }

    private void removeBrokenPipe(Block block) {
        manager.getPipe(location(block)).flatMap(record -> manager.remove(record.pipeUuid())).ifPresent(removed ->
                block.getWorld().dropItemNaturally(block.getLocation().add(.5, .5, .5), ItemStackGenerator.getTransferPipe()));
    }

    @EventHandler
    public void chunkLoad(ChunkLoadEvent event) {
        manager.refreshChunk(event.getWorld(), event.getChunk().getX(), event.getChunk().getZ());
    }

    @EventHandler
    public void chunkUnload(ChunkUnloadEvent event) {
        Bukkit.getScheduler().runTask(plugin, () -> manager.refreshChunk(event.getWorld(),
                event.getChunk().getX(), event.getChunk().getZ()));
    }

    @EventHandler
    public void quit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
        openFilters.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void heldItem(PlayerItemHeldEvent event) {
        ItemStack next = event.getPlayer().getInventory().getItem(event.getNewSlot());
        if (!isWrench(next)) sessions.remove(event.getPlayer().getUniqueId());
    }

    private void openFilter(Player player, PipeRecord receiver) {
        Inventory inventory = Bukkit.createInventory(null, 9, FILTER_TITLE);
        receiver.whitelist().stream().limit(9).forEach(filter -> inventory.addItem(filter.toDisplayItem()));
        openFilters.put(player.getUniqueId(), receiver.pipeUuid());
        player.openInventory(inventory);
    }

    @EventHandler
    public void filterClick(InventoryClickEvent event) {
        if (!FILTER_TITLE.equals(event.getView().getTitle()) || !(event.getWhoClicked() instanceof Player player)) return;
        event.setCancelled(true);
        UUID receiverUuid = openFilters.get(player.getUniqueId());
        if (receiverUuid == null) return;
        Set<PipeFilter> whitelist = new HashSet<>(manager.getPipe(receiverUuid)
                .map((PipeRecord record) -> record.whitelist()).orElse(Set.of()));
        if (event.getClickedInventory() == event.getView().getTopInventory()) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && !clicked.getType().isAir()) whitelist.remove(PipeFilter.fromItem(clicked));
        } else if (event.isShiftClick()) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && !clicked.getType().isAir() && whitelist.size() < 9) {
                whitelist.add(PipeFilter.fromItem(clicked));
            }
        }
        manager.setWhitelist(receiverUuid, whitelist);
        manager.getPipe(receiverUuid).ifPresent(record -> {
            event.getView().getTopInventory().clear();
            record.whitelist().forEach(filter -> event.getView().getTopInventory().addItem(filter.toDisplayItem()));
        });
    }

    @EventHandler
    public void filterClose(InventoryCloseEvent event) {
        if (FILTER_TITLE.equals(event.getView().getTitle())) openFilters.remove(event.getPlayer().getUniqueId());
    }

    private void timerTick() {
        tick++;
        Iterator<Map.Entry<UUID, LinkingSession>> iterator = sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, LinkingSession> entry = iterator.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            LinkingSession session = entry.getValue();
            if (player == null || !isWrench(player.getInventory().getItemInMainHand())) {
                iterator.remove();
                continue;
            }
            Optional<PipeRecord> sender = manager.getPipe(session.senderUuid());
            if (sender.isEmpty() || !manager.isWithinRange(sender.orElseThrow().location(), location(player.getLocation().getBlock()))) {
                player.sendMessage(ChatColor.RED + "Pipe linking mode ended.");
                iterator.remove();
                continue;
            }
            long remaining = Math.max(0, session.deadline() - tick);
            for (long threshold : THRESHOLDS) {
                if (remaining <= threshold && session.sentThresholds().add(threshold)) {
                    player.sendMessage(threshold == 0 ? ChatColor.RED + "Pipe linking mode ended."
                            : ChatColor.YELLOW + "Pipe linking: " + threshold / 20 + " seconds remaining.");
                }
            }
            if (remaining == 0) iterator.remove();
        }
    }

    private Optional<PipeRecord> activeSender(Player player) {
        LinkingSession session = sessions.get(player.getUniqueId());
        if (session == null || session.deadline() <= tick) return Optional.empty();
        return manager.getPipe(session.senderUuid()).filter(record -> record.type() == PipeType.SENDER)
                .filter(record -> manager.isWithinRange(record.location(), location(player.getLocation().getBlock())));
    }

    private void resetSession(UUID playerUuid, UUID senderUuid) {
        sessions.put(playerUuid, new LinkingSession(senderUuid, tick + configuration.linkingTimeTicks(), new HashSet<>()));
    }

    private void showLinkingHud(Player player) {
        Optional<PipeRecord> sender = activeSender(player);
        if (sender.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Pipe linking mode: inactive");
            return;
        }
        LinkingSession session = sessions.get(player.getUniqueId());
        PipeRecord record = sender.orElseThrow();
        player.sendMessage(ChatColor.AQUA + "Pipe linking mode: active");
        player.sendMessage(ChatColor.GRAY + "Time: " + Math.max(0, session.deadline() - tick) / 20 + "s; Sender: "
                + format(record.location()) + "; Receivers: " + record.receiverUuids().size());
    }

    private void showSenderHud(Player player, PipeRecord sender, Block block) {
        long withinRange = sender.receiverUuids().stream().map((UUID pipeUuid) -> manager.getPipe(pipeUuid))
                .flatMap((Optional<PipeRecord> optionalRecord) -> optionalRecord.stream())
                .filter(receiver -> manager.isWithinRange(sender.location(), receiver.location())).count();
        boolean powered = block.isBlockPowered() || block.isBlockIndirectlyPowered();
        player.sendMessage(ChatColor.BLUE + "Sender Pipe " + ChatColor.GRAY + format(sender.location()));
        player.sendMessage(ChatColor.GRAY + "Receivers: " + sender.receiverUuids().size() + " (" + withinRange
                + " in range); Power: " + (powered ? "powered" : "unpowered") + "; Activity: "
                + manager.status(sender.pipeUuid()).activity());
        player.sendMessage(ChatColor.DARK_GRAY + "Left-click to select; sneak+left-click to remove.");
    }

    private boolean consumePipe(Player player) {
        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack item = player.getInventory().getItem(slot);
            if (!ItemStackGeneratorUtils.isCustomItem(item, ItemModelData.TRANSFER_PIPE.getId())) continue;
            item.setAmount(item.getAmount() - 1);
            player.getInventory().setItem(slot, item.getAmount() == 0 ? null : item);
            return true;
        }
        return false;
    }

    private void givePipe(Player player) {
        player.getInventory().addItem(ItemStackGenerator.getTransferPipe()).values()
                .forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
    }

    private static boolean isWrench(ItemStack item) {
        return ItemStackGeneratorUtils.isCustomItem(item, ItemModelData.WRENCH.getId());
    }

    private static boolean isChest(Block block) {
        return block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST;
    }

    private static PipeLocation location(Block block) {
        return new PipeLocation(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    private boolean hasPipeAccess(Player player, Block block) {
        if (!plugin.isGriefPreventionEnabled() || !Utils.checkForClaim(player, block.getLocation())) return true;
        player.sendMessage(ChatColor.RED + "You do not have build access to pipes in this claim.");
        return false;
    }

    private static String format(PipeLocation location) {
        return location.x() + ", " + location.y() + ", " + location.z();
    }

}
