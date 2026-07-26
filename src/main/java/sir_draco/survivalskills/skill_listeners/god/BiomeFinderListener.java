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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.BiomeSearchResult;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

/** Owns the Biome Finder picker and resolves selected biomes in the player's current world. */
public final class BiomeFinderListener implements Listener {

    private static final int INVENTORY_SIZE = 54;
    private static final int BIOMES_PER_PAGE = 45;
    private static final int PREVIOUS_PAGE_SLOT = 48;
    private static final int PAGE_DISPLAY_SLOT = 49;
    private static final int NEXT_PAGE_SLOT = 50;
    private static final int SEARCH_RADIUS = 10_000;
    private static final int HORIZONTAL_INTERVAL = 16;
    private static final int VERTICAL_INTERVAL = 32;
    private static final int MAX_SEARCH_ATTEMPTS = 3;
    private static final double REJECTED_RADIUS_SQUARED = 128 * 128;
    private static final long SEARCH_COOLDOWN_MILLIS = 30_000;
    private static final int MAX_CONCURRENT_SEARCHES = 3;
    private static final String INVENTORY_TITLE = "Biome Finder - Page ";

    @SuppressWarnings("deprecation")
    private final List<Biome> biomes = Registry.BIOME.stream()
            .filter((Biome biome) -> !Biome.CUSTOM.equals(biome))
            .sorted(Comparator.comparing((Biome biome) -> biome.getKey().toString()))
            .toList();
    private final Set<UUID> activeSearches = new HashSet<>();
    private final Map<UUID, Long> searchCooldowns = new HashMap<>();
    private int runningSearches;

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

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        searchCooldowns.remove(event.getPlayer().getUniqueId());
    }

    @SuppressWarnings("deprecation")
    private void locateBiome(Player player, Biome biome) {
        UUID playerId = player.getUniqueId();
        if (activeSearches.contains(playerId)) {
            player.sendMessage(ChatColor.YELLOW + "A biome search is already in progress.");
            return;
        }
        long now = System.currentTimeMillis();
        Long lastSearch = searchCooldowns.get(playerId);
        if (lastSearch != null && now - lastSearch < SEARCH_COOLDOWN_MILLIS) {
            long remainingSeconds = (SEARCH_COOLDOWN_MILLIS - (now - lastSearch) + 999) / 1000;
            player.sendMessage(ChatColor.YELLOW + "Please wait " + remainingSeconds
                    + " seconds before using the Biome Finder again.");
            return;
        }
        if (runningSearches >= MAX_CONCURRENT_SEARCHES) {
            player.sendMessage(ChatColor.YELLOW + "The Biome Finder is busy. Please try again shortly.");
            return;
        }
        activeSearches.add(playerId);
        searchCooldowns.put(playerId, now);
        runningSearches++;

        Location origin = player.getLocation().clone();
        World world = player.getWorld();
        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + "Searching for the closest " + ChatColor.AQUA
                + formatBiomeName(biome.getKey()) + ChatColor.YELLOW + "...");

        searchAsync(new BiomeSearch(playerId, world, origin, biome), origin, new CopyOnWriteArrayList<>(), 1);
    }

    @SuppressWarnings("deprecation")
    private void searchAsync(BiomeSearch search, Location searchOrigin, List<Location> rejected, int attempt) {
        Bukkit.getScheduler().runTaskAsynchronously(SurvivalSkills.getInstance(), () -> {
            try {
                Optional<BiomeSearchResult> result = Optional.ofNullable(search.world().locateNearestBiome(
                        searchOrigin, SEARCH_RADIUS, HORIZONTAL_INTERVAL, VERTICAL_INTERVAL, search.biome()));
                if (result.isPresent() && isRejected(result.orElseThrow().getLocation(), rejected)) {
                    retrySearch(search, rejected, attempt);
                    return;
                }
                verifyCandidate(search, result, rejected, attempt);
            } catch (RuntimeException exception) {
                Bukkit.getLogger().log(Level.WARNING,
                        String.format("[SurvivalSkills] Failed to locate biome %s for player %s",
                                search.biome().getKey(), search.playerId()), exception);
                finishFailedSearch(search.playerId());
            }
        });
    }

    private void verifyCandidate(BiomeSearch search, Optional<BiomeSearchResult> result,
                                 List<Location> rejected, int attempt) {
        Bukkit.getScheduler().runTask(SurvivalSkills.getInstance(), () -> {
            if (result.isEmpty()) {
                if (attempt == 1) {
                    finishNotFoundSearch(search);
                } else {
                    finishUnverifiedSearch(search);
                }
                return;
            }
            Location location = result.orElseThrow().getLocation();
            boolean verified = readStoredBiome(search.world(), location)
                    .map((Biome actual) -> actual.equals(search.biome()))
                    .orElse(true);
            if (verified) {
                finishVerifiedSearch(search, location);
                return;
            }
            rejected.add(location);
            retrySearch(search, rejected, attempt);
        });
    }

    /**
     * Reads the biome actually stored in the chunk at the candidate location. An empty result
     * means the chunk has never been generated, so it will match the search sampler when created.
     */
    private static Optional<Biome> readStoredBiome(World world, Location location) {
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            if (!world.isChunkGenerated(chunkX, chunkZ)) {
                return Optional.empty();
            }
            world.getChunkAt(chunkX, chunkZ);
        }
        return Optional.of(world.getBiome(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
    }

    private void retrySearch(BiomeSearch search, List<Location> rejected, int attempt) {
        if (attempt >= MAX_SEARCH_ATTEMPTS) {
            finishUnverifiedSearch(search);
            return;
        }
        searchAsync(search, shiftedOrigin(search.origin(), attempt), rejected, attempt + 1);
    }

    // Offsets the origin enough to change which sample points the search checks
    private static Location shiftedOrigin(Location origin, int attempt) {
        int offset = (HORIZONTAL_INTERVAL / 2 + 1) * attempt;
        return origin.clone().add(offset, 0.0, offset);
    }

    private static boolean isRejected(Location location, List<Location> rejected) {
        return rejected.stream().anyMatch((Location point) ->
                point.distanceSquared(location) < REJECTED_RADIUS_SQUARED);
    }

    @SuppressWarnings("deprecation")
    private void finishVerifiedSearch(BiomeSearch search, Location location) {
        Bukkit.getScheduler().runTask(SurvivalSkills.getInstance(), () -> {
            endSearch(search.playerId());
            Optional.ofNullable(Bukkit.getPlayer(search.playerId())).ifPresent((Player player) -> {
                player.sendMessage(ChatColor.GREEN + "Closest " + ChatColor.AQUA
                        + formatBiomeName(search.biome().getKey()) + ChatColor.GREEN + " biome: "
                        + ChatColor.YELLOW + "(" + location.getBlockX() + ", " + location.getBlockY()
                        + ", " + location.getBlockZ() + ")" + ChatColor.GRAY + " in "
                        + location.getWorld().getName());
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1);
            });
        });
    }

    @SuppressWarnings("deprecation")
    private void finishNotFoundSearch(BiomeSearch search) {
        finishSearchWithMessage(search, ChatColor.RED + "No " + formatBiomeName(search.biome().getKey())
                + " biome was found within " + SEARCH_RADIUS + " blocks in " + search.world().getName() + ".");
    }

    @SuppressWarnings("deprecation")
    private void finishUnverifiedSearch(BiomeSearch search) {
        finishSearchWithMessage(search, ChatColor.RED + "No verified " + formatBiomeName(search.biome().getKey())
                + " biome was found within " + SEARCH_RADIUS + " blocks in " + search.world().getName() + ".");
    }

    private void finishFailedSearch(UUID playerId) {
        Bukkit.getScheduler().runTask(SurvivalSkills.getInstance(), () -> {
            endSearch(playerId);
            Optional.ofNullable(Bukkit.getPlayer(playerId)).ifPresent((Player player) ->
                    player.sendMessage(ChatColor.RED + "The biome search failed. Please try again."));
        });
    }

    private void finishSearchWithMessage(BiomeSearch search, String message) {
        Bukkit.getScheduler().runTask(SurvivalSkills.getInstance(), () -> {
            endSearch(search.playerId());
            Optional.ofNullable(Bukkit.getPlayer(search.playerId()))
                    .ifPresent((Player player) -> player.sendMessage(message));
        });
    }

    private void endSearch(UUID playerId) {
        activeSearches.remove(playerId);
        runningSearches--;
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

    private record BiomeSearch(UUID playerId, World world, Location origin, Biome biome) {
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
