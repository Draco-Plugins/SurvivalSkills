package sir_draco.survivalskills.super_enchanting;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.EnchantingTable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.skills.SkillManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SuperEnchantingTableManager implements Listener {

    public static final int REQUIRED_CRAFTING_LEVEL = 42;

    private static final String BOOK_DISPLAY_TAG = "survivalskills_super_enchanting_book";
    private static final double ORBIT_HEIGHT = 1.35;
    private static final double ORBIT_RADIUS = 0.55;
    private static final double ORBIT_SPEED_RADIANS = Math.toRadians(4);

    private final SurvivalSkills plugin;
    private final SuperEnchantingGui gui;
    private final Map<TablePosition, List<ItemDisplay>> orbitingBooks = new HashMap<>();
    private BukkitTask orbitTask;
    private double orbitAngle;

    public SuperEnchantingTableManager(SurvivalSkills plugin) {
        this.plugin = plugin;
        this.gui = new SuperEnchantingGui(plugin);
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getPluginManager().registerEvents(gui, plugin);
        for (World world : Bukkit.getWorlds())
            for (Chunk chunk : world.getLoadedChunks())
                loadTablesInChunk(chunk);
        orbitTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateOrbits, 1, 1);
    }

    public void shutdown() {
        if (orbitTask != null) {
            orbitTask.cancel();
            orbitTask = null;
        }
        orbitingBooks.values().forEach(this::removeDisplays);
        orbitingBooks.clear();
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSuperEnchantingTableCraft(CraftItemEvent event) {
        if (!SuperEnchantingItems.isSuperEnchantingTable(event.getRecipe().getResult(), plugin))
            return;
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (SkillManager.getSkillLevel(player.getUniqueId(), SkillCategory.CRAFTING)
                >= REQUIRED_CRAFTING_LEVEL)
            return;

        event.setCancelled(true);
        player.sendMessage(ChatColor.RED + "You need crafting level " + ChatColor.AQUA
                + REQUIRED_CRAFTING_LEVEL + ChatColor.RED + " to craft a Super Enchanting Table.");
        player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTablePlace(BlockPlaceEvent event) {
        if (!SuperEnchantingItems.isSuperEnchantingTable(event.getItemInHand(), plugin))
            return;
        Block block = event.getBlockPlaced();
        if (!SuperEnchantingItems.markSuperEnchantingTable(block, plugin))
            return;
        spawnOrbitingBooks(block);
    }

    @EventHandler(ignoreCancelled = true)
    public void onTableInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND)
            return;
        Block block = event.getClickedBlock();
        if (block == null || !SuperEnchantingItems.isSuperEnchantingTable(block, plugin))
            return;

        event.setCancelled(true);
        gui.open(event.getPlayer());
        event.getPlayer().playSound(event.getPlayer(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTableBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!SuperEnchantingItems.isSuperEnchantingTable(block, plugin))
            return;

        event.setDropItems(false);
        removeOrbitingBooks(TablePosition.from(block));
        block.getWorld().dropItemNaturally(block.getLocation(),
                SuperEnchantingItems.createSuperEnchantingTable(plugin));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplosion(BlockExplodeEvent event) {
        replaceExplodedTableDrops(event.blockList());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplosion(EntityExplodeEvent event) {
        replaceExplodedTableDrops(event.blockList());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        loadTablesInChunk(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        List<TablePosition> positions = orbitingBooks.keySet().stream()
                .filter(position -> position.isInChunk(chunk))
                .toList();
        positions.forEach(this::removeOrbitingBooks);
    }

    private void replaceExplodedTableDrops(List<Block> explodedBlocks) {
        Iterator<Block> iterator = explodedBlocks.iterator();
        List<Block> superTables = new ArrayList<>();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (!SuperEnchantingItems.isSuperEnchantingTable(block, plugin))
                continue;
            iterator.remove();
            superTables.add(block);
        }

        for (Block block : superTables) {
            removeOrbitingBooks(TablePosition.from(block));
            block.setType(Material.AIR, false);
            block.getWorld().dropItemNaturally(block.getLocation(),
                    SuperEnchantingItems.createSuperEnchantingTable(plugin));
        }
    }

    private void loadTablesInChunk(Chunk chunk) {
        for (BlockState state : chunk.getTileEntities()) {
            if (!(state instanceof EnchantingTable))
                continue;
            Block block = state.getBlock();
            if (SuperEnchantingItems.isSuperEnchantingTable(block, plugin))
                spawnOrbitingBooks(block);
        }
    }

    private void spawnOrbitingBooks(Block block) {
        TablePosition position = TablePosition.from(block);
        removeOrbitingBooks(position);
        Location center = block.getLocation().add(0.5, ORBIT_HEIGHT, 0.5);

        List<ItemDisplay> displays = new ArrayList<>();
        for (int index = 0; index < 2; index++) {
            ItemDisplay display = (ItemDisplay) block.getWorld().spawnEntity(center, EntityType.ITEM_DISPLAY);
            display.setItemStack(new ItemStack(Material.BOOK));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            display.setBillboard(ItemDisplay.Billboard.FIXED);
            display.setTeleportDuration(1);
            display.setInterpolationDuration(1);
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setGravity(false);
            display.addScoreboardTag(BOOK_DISPLAY_TAG);
            displays.add(display);
        }
        orbitingBooks.put(position, List.copyOf(displays));
    }

    private void updateOrbits() {
        orbitAngle += ORBIT_SPEED_RADIANS;
        List<Block> tablesToRespawn = new ArrayList<>();
        Iterator<Map.Entry<TablePosition, List<ItemDisplay>>> iterator = orbitingBooks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<TablePosition, List<ItemDisplay>> entry = iterator.next();
            TablePosition position = entry.getKey();
            Block block = position.getLoadedBlock();
            if (block == null || !SuperEnchantingItems.isSuperEnchantingTable(block, plugin)) {
                removeDisplays(entry.getValue());
                iterator.remove();
                continue;
            }

            Location center = block.getLocation().add(0.5, ORBIT_HEIGHT, 0.5);
            List<ItemDisplay> displays = entry.getValue();
            if (displays.stream().anyMatch(display -> !display.isValid())) {
                removeDisplays(displays);
                iterator.remove();
                tablesToRespawn.add(block);
                continue;
            }
            for (int index = 0; index < displays.size(); index++) {
                ItemDisplay display = displays.get(index);
                double angle = orbitAngle + (Math.PI * index);
                Location target = center.clone().add(
                        Math.cos(angle) * ORBIT_RADIUS,
                        Math.sin(angle * 2) * 0.08,
                        Math.sin(angle) * ORBIT_RADIUS);
                target.setYaw((float) Math.toDegrees(-angle));
                display.teleport(target);
            }
        }
        tablesToRespawn.forEach(this::spawnOrbitingBooks);
    }

    private void removeOrbitingBooks(TablePosition position) {
        List<ItemDisplay> displays = orbitingBooks.remove(position);
        if (displays != null)
            removeDisplays(displays);
    }

    private void removeDisplays(List<ItemDisplay> displays) {
        displays.forEach((ItemDisplay display) -> display.remove());
    }

    private record TablePosition(UUID worldId, int x, int y, int z) {

        private static TablePosition from(Block block) {
            return new TablePosition(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }

        private Block getLoadedBlock() {
            World world = Bukkit.getWorld(worldId);
            if (world == null || !world.isChunkLoaded(x >> 4, z >> 4))
                return null;
            return world.getBlockAt(x, y, z);
        }

        private boolean isInChunk(Chunk chunk) {
            return worldId.equals(chunk.getWorld().getUID())
                    && (x >> 4) == chunk.getX()
                    && (z >> 4) == chunk.getZ();
        }
    }
}
