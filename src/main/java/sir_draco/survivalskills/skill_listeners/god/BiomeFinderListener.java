package sir_draco.survivalskills.skill_listeners.god;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.BiomeSearchResult;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/** Owns the Biome Finder picker and resolves selected biomes in the player's current world. */
public final class BiomeFinderListener implements Listener {

    private static final int INVENTORY_SIZE = 54;
    private static final int BIOMES_PER_PAGE = 45;
    private static final int PREVIOUS_PAGE_SLOT = 48;
    private static final int PAGE_DISPLAY_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 50;
    private static final int SEARCH_RADIUS = 10_000;
    private static final String INVENTORY_TITLE = "Biome Finder - Page ";

    @SuppressWarnings("deprecation")
    private final List<Biome> biomes = Registry.BIOME.stream()
            .filter((Biome biome) -> !Biome.CUSTOM.equals(biome))
            .sorted(Comparator.comparing((Biome biome) -> biome.getKey().toString()))
            .toList();
    private final Set<UUID> activeSearches = new HashSet<>();

    public void openBiomePicker(Player player, int requestedPage) {
        int totalPages = calculateTotalPages(biomes.size());
        int page = Math.max(0, Math.min(requestedPage, totalPages - 1));
        BiomePickerHolder holder = new BiomePickerHolder(page);
        Inventory inventory = holder.getInventory();
        int startIndex = page * BIOMES_PER_PAGE;

        for (int index = startIndex; index < Math.min(startIndex + BIOMES_PER_PAGE, biomes.size()); index++) {
            Biome biome = biomes.get(index);
            inventory.setItem(index - startIndex, createBiomeItem(biome));
        }
        if (page > 0) {
            inventory.setItem(PREVIOUS_PAGE_SLOT, createNavigationItem(Material.ARROW, "Previous Page"));
        }
        inventory.setItem(PAGE_DISPLAY_SLOT, createPageItem(page, totalPages));
        if (page + 1 < totalPages) {
            inventory.setItem(NEXT_PAGE_SLOT, createNavigationItem(Material.ARROW, "Next Page"));
        }
        player.openInventory(inventory);
    }

    @EventHandler
    public void onBiomePickerClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getView().getTopInventory().getHolder() instanceof BiomePickerHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!event.getView().getTopInventory().equals(event.getClickedInventory())) {
            return;
        }

        int slot = event.getRawSlot();
        if (slot == PREVIOUS_PAGE_SLOT) {
            openBiomePicker(player, holder.getPage() - 1);
            return;
        }
        if (slot == NEXT_PAGE_SLOT) {
            openBiomePicker(player, holder.getPage() + 1);
            return;
        }
        if (slot < 0 || slot >= BIOMES_PER_PAGE) {
            return;
        }

        int biomeIndex = holder.getPage() * BIOMES_PER_PAGE + slot;
        if (biomeIndex >= biomes.size()) {
            return;
        }
        locateBiome(player, biomes.get(biomeIndex));
    }

    @EventHandler
    public void onBiomePickerDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof BiomePickerHolder) {
            event.setCancelled(true);
        }
    }

    @SuppressWarnings("deprecation")
    private void locateBiome(Player player, Biome biome) {
        UUID playerId = player.getUniqueId();
        if (!activeSearches.add(playerId)) {
            player.sendMessage(ChatColor.YELLOW + "A biome search is already in progress.");
            return;
        }

        Location origin = player.getLocation().clone();
        World world = player.getWorld();
        String worldName = world.getName();
        String biomeName = formatBiomeName(biome.getKey());
        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + "Searching for the closest " + ChatColor.AQUA + biomeName
                + ChatColor.YELLOW + "...");

        Bukkit.getScheduler().runTaskAsynchronously(SurvivalSkills.getInstance(), () -> {
            try {
                Optional<BiomeSearchResult> result = Optional.ofNullable(
                        world.locateNearestBiome(origin, SEARCH_RADIUS, biome));
                finishSearch(playerId, biomeName, worldName, result);
            } catch (RuntimeException exception) {
                Bukkit.getLogger().log(Level.WARNING,
                        String.format("[SurvivalSkills] Failed to locate biome %s for player %s",
                                biome.getKey(), playerId), exception);
                finishFailedSearch(playerId);
            }
        });
    }

    private void finishSearch(UUID playerId, String biomeName, String searchedWorldName,
                              Optional<BiomeSearchResult> result) {
        Bukkit.getScheduler().runTask(SurvivalSkills.getInstance(), () -> {
            activeSearches.remove(playerId);
            Optional.ofNullable(Bukkit.getPlayer(playerId)).ifPresent((Player player) -> {
                if (result.isEmpty()) {
                    player.sendMessage(ChatColor.RED + "No " + biomeName + " biome was found within "
                            + SEARCH_RADIUS + " blocks in " + searchedWorldName + ".");
                    return;
                }
                Location location = result.orElseThrow().getLocation();
                player.sendMessage(ChatColor.GREEN + "Closest " + ChatColor.AQUA + biomeName + ChatColor.GREEN
                        + " biome: " + ChatColor.YELLOW + "(" + location.getBlockX() + ", "
                        + location.getBlockY() + ", " + location.getBlockZ() + ")"
                        + ChatColor.GRAY + " in " + location.getWorld().getName());
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1);
            });
        });
    }

    private void finishFailedSearch(UUID playerId) {
        Bukkit.getScheduler().runTask(SurvivalSkills.getInstance(), () -> {
            activeSearches.remove(playerId);
            Optional.ofNullable(Bukkit.getPlayer(playerId)).ifPresent((Player player) ->
                    player.sendMessage(ChatColor.RED + "The biome search failed. Please try again."));
        });
    }

    @SuppressWarnings("deprecation")
    private static ItemStack createBiomeItem(Biome biome) {
        ItemStack item = new ItemStack(getBiomeMaterial(biome.getKey().getKey()));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(ChatColor.AQUA + formatBiomeName(biome.getKey()));
        meta.setLore(List.of(ChatColor.GRAY + "Click to find the closest match"));
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createNavigationItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(ChatColor.YELLOW + name);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack createPageItem(int page, int totalPages) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.setDisplayName(ChatColor.GOLD + "Page " + (page + 1) + " of " + totalPages);
        item.setItemMeta(meta);
        return item;
    }

    static int calculateTotalPages(int biomeCount) {
        return Math.max(1, (biomeCount + BIOMES_PER_PAGE - 1) / BIOMES_PER_PAGE);
    }

    static String formatBiomeName(NamespacedKey biomeKey) {
        String[] words = biomeKey.getKey().split("_");
        String displayName = java.util.Arrays.stream(words)
                .map((String word) -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(java.util.stream.Collectors.joining(" "));
        if (NamespacedKey.MINECRAFT.equals(biomeKey.getNamespace())) {
            return displayName;
        }
        return displayName + " (" + biomeKey.getNamespace() + ")";
    }

    private static Material getBiomeMaterial(String biomeKey) {
        if (biomeKey.contains("ocean") || biomeKey.contains("river")) {
            return Material.WATER_BUCKET;
        }
        if (biomeKey.contains("nether") || biomeKey.contains("crimson") || biomeKey.contains("warped")
                || biomeKey.contains("soul_sand") || biomeKey.contains("basalt")) {
            return Material.NETHERRACK;
        }
        if (biomeKey.contains("end") || biomeKey.equals("the_void")) {
            return Material.END_STONE;
        }
        if (biomeKey.contains("snow") || biomeKey.contains("frozen") || biomeKey.contains("ice")
                || biomeKey.contains("grove") || biomeKey.contains("peak")) {
            return Material.SNOW_BLOCK;
        }
        if (biomeKey.contains("desert") || biomeKey.contains("badlands") || biomeKey.contains("beach")) {
            return Material.SAND;
        }
        if (biomeKey.contains("jungle") || biomeKey.contains("bamboo")) {
            return Material.JUNGLE_SAPLING;
        }
        if (biomeKey.contains("swamp")) {
            return Material.LILY_PAD;
        }
        if (biomeKey.contains("mushroom")) {
            return Material.RED_MUSHROOM_BLOCK;
        }
        if (biomeKey.contains("cherry")) {
            return Material.CHERRY_SAPLING;
        }
        if (biomeKey.contains("pale_garden")) {
            return Material.PALE_OAK_SAPLING;
        }
        if (biomeKey.contains("dark_forest")) {
            return Material.DARK_OAK_SAPLING;
        }
        if (biomeKey.contains("birch")) {
            return Material.BIRCH_SAPLING;
        }
        if (biomeKey.contains("forest")) {
            return Material.OAK_SAPLING;
        }
        if (biomeKey.contains("taiga")) {
            return Material.SPRUCE_SAPLING;
        }
        if (biomeKey.contains("savanna")) {
            return Material.ACACIA_SAPLING;
        }
        if (biomeKey.contains("cave") || biomeKey.contains("deep_dark")) {
            return Material.SCULK;
        }
        return Material.GRASS_BLOCK;
    }

    private static final class BiomePickerHolder implements InventoryHolder {
        private final int page;
        private final Inventory inventory;

        private BiomePickerHolder(int page) {
            this.page = page;
            this.inventory = Bukkit.createInventory(this, INVENTORY_SIZE, INVENTORY_TITLE + (page + 1));
        }

        private int getPage() {
            return page;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
