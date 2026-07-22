package sir_draco.survivalskills.pipes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public final class PipeManager {
    public enum Activity { IDLE_UNPOWERED, ACTIVE, BLOCKED }
    public record SenderStatus(long lastProcessedTick, int amountMoved, Activity activity) {}
    public record ChunkKey(UUID worldUuid, int x, int z) {}
    public record PipeSnapshot(long version, List<PersistedPipe> pipes) {}
    public record PersistedPipe(UUID pipeUuid, UUID ownerUuid, PipeLocation location, String worldName,
            PipeType type, Optional<UUID> senderUuid, List<UUID> receiverUuids, Set<String> whitelist) {
        public PersistedPipe {
            receiverUuids = List.copyOf(receiverUuids);
            whitelist = Set.copyOf(whitelist);
        }
    }

    private final JavaPlugin plugin;
    private final PipeConfiguration configuration;
    private final File dataFile;
    private final Map<UUID, PipeRecord> byUuid = new HashMap<>();
    private final Map<PipeLocation, UUID> byLocation = new HashMap<>();
    private final Map<UUID, Set<PipeLocation>> coveredLocations = new HashMap<>();
    private final Map<ChunkKey, Set<UUID>> chunkIndex = new HashMap<>();
    private final Set<UUID> loadedSenders = new HashSet<>();
    private final Map<UUID, SenderStatus> statuses = new HashMap<>();
    private final Map<UUID, Integer> receiverCursors = new HashMap<>();
    private final Map<UUID, Integer> sourceCursors = new HashMap<>();
    private final List<LinkedHashSet<UUID>> workBuckets;
    private final Map<UUID, Integer> bucketBySender = new HashMap<>();
    private long tick;
    private long version;
    private boolean dirty;
    private CompletableFuture<Boolean> saveInFlight;
    private boolean saveRequested;

    public PipeManager(JavaPlugin plugin, PipeConfiguration configuration) {
        this.plugin = Objects.requireNonNull(plugin);
        this.configuration = Objects.requireNonNull(configuration);
        this.dataFile = new File(plugin.getDataFolder(), "pipedata.yml");
        this.workBuckets = java.util.stream.IntStream.range(0, configuration.transferIntervalTicks())
                .mapToObj(ignored -> new LinkedHashSet<UUID>()).toList();
    }

    public Optional<PipeRecord> getPipe(UUID pipeUuid) {
        return Optional.ofNullable(byUuid.get(pipeUuid));
    }

    public Optional<PipeRecord> getPipe(PipeLocation location) {
        return Optional.ofNullable(byLocation.get(location)).map(byUuid::get);
    }

    public Collection<PipeRecord> pipes() {
        return List.copyOf(byUuid.values());
    }

    public Set<UUID> pipesInChunk(ChunkKey key) {
        return Set.copyOf(chunkIndex.getOrDefault(key, Set.of()));
    }

    public Set<PipeLocation> coveredLocations(UUID pipeUuid) {
        return Set.copyOf(coveredLocations.getOrDefault(pipeUuid, Set.of()));
    }

    public PipeRecord attach(UUID owner, Block clickedBlock, PipeType type, Optional<UUID> senderUuid) {
        requireMainThread();
        Set<PipeLocation> locations = chestLocations(clickedBlock)
                .orElseThrow(() -> new IllegalArgumentException("Only loaded chests can have pipes"));
        if (locations.stream().anyMatch(byLocation::containsKey)) {
            throw new IllegalStateException("Chest already has a pipe");
        }
        PipeLocation canonical = locations.stream().min(Comparator.comparingInt((PipeLocation location) -> location.x())
                .thenComparingInt((PipeLocation location) -> location.y())
                .thenComparingInt((PipeLocation location) -> location.z())).orElseThrow();
        PipeRecord record = new PipeRecord(UUID.randomUUID(), owner, canonical, type,
                type == PipeType.RECEIVER ? senderUuid : Optional.empty(), List.of(), Set.of());
        if (record.senderUuid().isPresent()) validateLink(record, byUuid.get(record.senderUuid().orElseThrow()));
        index(record, locations);
        if (record.senderUuid().isPresent()) addReceiverToSender(record.senderUuid().orElseThrow(), record.pipeUuid());
        changed();
        return byUuid.get(record.pipeUuid());
    }

    public boolean relink(UUID receiverUuid, UUID senderUuid) {
        requireMainThread();
        PipeRecord receiver = byUuid.get(receiverUuid);
        PipeRecord sender = byUuid.get(senderUuid);
        if (receiver == null || sender == null || receiver.type() != PipeType.RECEIVER
                || receiver.senderUuid().isPresent()) return false;
        validateLink(receiver, sender);
        replace(new PipeRecord(receiver.pipeUuid(), receiver.ownerUuid(), receiver.location(), receiver.type(),
                Optional.of(senderUuid), List.of(), receiver.whitelist()));
        addReceiverToSender(senderUuid, receiverUuid);
        changed();
        return true;
    }

    public Optional<PipeRecord> remove(UUID pipeUuid) {
        requireMainThread();
        PipeRecord removed = byUuid.remove(pipeUuid);
        if (removed == null) return Optional.empty();
        deindex(pipeUuid);
        if (removed.type() == PipeType.SENDER) {
            for (UUID receiverUuid : removed.receiverUuids()) {
                PipeRecord receiver = byUuid.get(receiverUuid);
                if (receiver != null) replace(new PipeRecord(receiver.pipeUuid(), receiver.ownerUuid(),
                        receiver.location(), receiver.type(), Optional.empty(), List.of(), receiver.whitelist()));
            }
        } else {
            removed.senderUuid().ifPresent(sender -> removeReceiverFromSender(sender, pipeUuid));
        }
        changed();
        return Optional.of(removed);
    }

    public void setWhitelist(UUID receiverUuid, Set<PipeFilter> whitelist) {
        requireMainThread();
        PipeRecord receiver = Objects.requireNonNull(byUuid.get(receiverUuid));
        if (receiver.type() != PipeType.RECEIVER) throw new IllegalArgumentException("Not a receiver");
        Set<PipeFilter> immutable = Set.copyOf(whitelist);
        if (receiver.whitelist().equals(immutable)) return;
        replace(new PipeRecord(receiver.pipeUuid(), receiver.ownerUuid(), receiver.location(), receiver.type(),
                receiver.senderUuid(), List.of(), immutable));
        changed();
    }

    public boolean isWithinRange(PipeLocation first, PipeLocation second) {
        return first.squaredDistance(second) <= configuration.maxRangeSquared();
    }

    public Optional<Inventory> resolveInventory(PipeRecord record) {
        World world = Bukkit.getWorld(record.location().worldUuid());
        if (world == null) return Optional.empty();
        Set<PipeLocation> locations = coveredLocations.getOrDefault(record.pipeUuid(), Set.of(record.location()));
        if (locations.stream().anyMatch(location -> !world.isChunkLoaded(location.chunkX(), location.chunkZ()))) {
            return Optional.empty();
        }
        Block block = world.getBlockAt(record.location().x(), record.location().y(), record.location().z());
        if (!(block.getState() instanceof Chest chest)) return Optional.empty();
        return Optional.of(chest.getInventory().getHolder() instanceof DoubleChest doubleChest
                ? doubleChest.getInventory() : chest.getInventory());
    }

    public void processTick() {
        requireMainThread();
        int bucket = (int) (tick % configuration.transferIntervalTicks());
        for (UUID senderUuid : List.copyOf(workBuckets.get(bucket))) processSender(senderUuid);
        tick++;
    }

    public SenderStatus status(UUID senderUuid) {
        return statuses.getOrDefault(senderUuid, new SenderStatus(0, 0, Activity.IDLE_UNPOWERED));
    }

    private void processSender(UUID senderUuid) {
        PipeRecord sender = byUuid.get(senderUuid);
        if (sender == null || !loadedSenders.contains(senderUuid)) return;
        World world = Bukkit.getWorld(sender.location().worldUuid());
        Optional<Inventory> sourceOptional = resolveInventory(sender);
        if (world == null || sourceOptional.isEmpty()) return;
        Block block = world.getBlockAt(sender.location().x(), sender.location().y(), sender.location().z());
        boolean powered = block.isBlockPowered() || block.isBlockIndirectlyPowered();
        if (!powered) {
            statuses.put(senderUuid, new SenderStatus(tick, 0, Activity.IDLE_UNPOWERED));
            return;
        }
        Inventory source = sourceOptional.orElseThrow();
        ItemStack[] contents = source.getContents();
        List<UUID> receivers = sender.receiverUuids();
        int moved = 0;
        int startReceiver = receivers.isEmpty() ? 0 : receiverCursors.getOrDefault(senderUuid, 0) % receivers.size();
        int startSlot = contents.length == 0 ? 0 : sourceCursors.getOrDefault(senderUuid, 0) % contents.length;
        for (int offset = 0; offset < receivers.size(); offset++) {
            PipeRecord receiver = byUuid.get(receivers.get((startReceiver + offset) % receivers.size()));
            if (receiver == null || receiver.senderUuid().filter(senderUuid::equals).isEmpty()) continue;
            Optional<Inventory> target = resolveInventory(receiver);
            if (target.isEmpty()) continue;
            moved += transferOneStack(source, contents, target.orElseThrow(), receiver.whitelist(), startSlot);
        }
        if (!receivers.isEmpty()) receiverCursors.put(senderUuid, (startReceiver + 1) % receivers.size());
        if (contents.length > 0) sourceCursors.put(senderUuid, (startSlot + 1) % contents.length);
        statuses.put(senderUuid, new SenderStatus(tick, moved, moved > 0 ? Activity.ACTIVE : Activity.BLOCKED));
    }

    static int transferOneStack(Inventory source, ItemStack[] contents, Inventory target,
            Set<PipeFilter> whitelist, int startSlot) {
        for (int offset = 0; offset < contents.length; offset++) {
            int slot = (startSlot + offset) % contents.length;
            ItemStack item = contents[slot];
            if (item == null || item.getType().isAir()
                    || (!whitelist.isEmpty() && whitelist.stream().noneMatch(filter -> filter.matches(item)))) continue;
            ItemStack offered = item.clone();
            offered.setAmount(Math.min(item.getAmount(), item.getMaxStackSize()));
            Map<Integer, ItemStack> leftovers = target.addItem(offered);
            int rejected = leftovers.values().stream().mapToInt((ItemStack itemStack) -> itemStack.getAmount()).sum();
            int accepted = offered.getAmount() - rejected;
            if (accepted <= 0) continue;
            item.setAmount(item.getAmount() - accepted);
            source.setItem(slot, item.getAmount() == 0 ? null : item);
            contents[slot] = item.getAmount() == 0 ? null : item;
            return accepted;
        }
        return 0;
    }

    public void refreshChunk(World world, int chunkX, int chunkZ) {
        requireMainThread();
        for (UUID pipeUuid : chunkIndex.getOrDefault(new ChunkKey(world.getUID(), chunkX, chunkZ), Set.of())) {
            PipeRecord record = byUuid.get(pipeUuid);
            if (record != null && record.type() == PipeType.SENDER) updateEligibility(record);
        }
    }

    public void load() {
        requireMainThread();
        if (!dataFile.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = yaml.getConfigurationSection("pipes");
        if (section == null) return;
        Map<UUID, PipeRecord> parsed = new LinkedHashMap<>();
        Map<UUID, String> worldNames = new HashMap<>();
        for (String key : section.getKeys(false)) {
            try {
                ConfigurationSection value = Objects.requireNonNull(section.getConfigurationSection(key));
                UUID id = UUID.fromString(key);
                UUID owner = UUID.fromString(Objects.requireNonNull(value.getString("owner")));
                UUID storedWorld = UUID.fromString(Objects.requireNonNull(value.getString("world-uuid")));
                String worldName = value.getString("world-name", "");
                World resolved = Optional.ofNullable(Bukkit.getWorld(storedWorld)).orElse(Bukkit.getWorld(worldName));
                UUID worldUuid = resolved == null ? storedWorld : resolved.getUID();
                PipeType type = PipeType.valueOf(Objects.requireNonNull(value.getString("type")));
                Optional<UUID> sender = Optional.ofNullable(value.getString("sender")).map(UUID::fromString);
                Set<PipeFilter> whitelist = value.getStringList("whitelist").stream()
                        .map((String serializedFilter) -> PipeFilter.deserialize(serializedFilter))
                        .flatMap((Optional<PipeFilter> filter) -> filter.stream())
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());
                PipeRecord record = new PipeRecord(id, owner,
                        new PipeLocation(worldUuid, value.getInt("x"), value.getInt("y"), value.getInt("z")),
                        type, type == PipeType.RECEIVER ? sender : Optional.empty(), List.of(), whitelist);
                parsed.put(id, record);
                worldNames.put(id, worldName);
            } catch (RuntimeException exception) {
                Bukkit.getLogger().log(Level.WARNING, "[SurvivalSkills] Invalid pipe record " + key, exception);
            }
        }
        byUuid.putAll(parsed);
        for (PipeRecord record : List.copyOf(parsed.values())) {
            Set<PipeLocation> locations = discoverLoadedChestLocations(record.location()).orElse(Set.of(record.location()));
            indexLocations(record, locations);
            updateEligibility(record);
        }
        for (PipeRecord receiver : List.copyOf(byUuid.values())) {
            if (receiver.type() != PipeType.RECEIVER || receiver.senderUuid().isEmpty()) continue;
            PipeRecord sender = byUuid.get(receiver.senderUuid().orElseThrow());
            try {
                validateLink(receiver, sender);
                addReceiverToSender(sender.pipeUuid(), receiver.pipeUuid());
            } catch (RuntimeException exception) {
                replace(new PipeRecord(receiver.pipeUuid(), receiver.ownerUuid(), receiver.location(),
                        receiver.type(), Optional.empty(), List.of(), receiver.whitelist()));
                Bukkit.getLogger().log(Level.WARNING, "[SurvivalSkills] Discarded invalid pipe link for "
                        + receiver.pipeUuid(), exception);
            }
        }
        dirty = false;
    }

    public void requestSave() {
        requireMainThread();
        if (!dirty) return;
        if (saveInFlight != null && !saveInFlight.isDone()) {
            saveRequested = true;
            return;
        }
        PipeSnapshot snapshot = snapshot();
        saveInFlight = CompletableFuture.supplyAsync(() -> write(snapshot));
        saveInFlight.thenAccept(success -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (success && version == snapshot.version()) dirty = false;
            boolean repeat = saveRequested || dirty && version != snapshot.version();
            saveRequested = false;
            if (repeat) requestSave();
        }));
    }

    public void saveNow() {
        requireMainThread();
        if (saveInFlight != null) saveInFlight.join();
        if (dirty) {
            PipeSnapshot snapshot = snapshot();
            if (write(snapshot) && version == snapshot.version()) dirty = false;
        }
    }

    public boolean isDirty() { return dirty; }

    private PipeSnapshot snapshot() {
        List<PersistedPipe> pipes = byUuid.values().stream().map(record -> {
            World world = Bukkit.getWorld(record.location().worldUuid());
            Set<String> whitelist = record.whitelist().stream().map((PipeFilter filter) -> filter.serialize())
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());
            return new PersistedPipe(record.pipeUuid(), record.ownerUuid(), record.location(),
                    world == null ? "" : world.getName(), record.type(), record.senderUuid(),
                    record.receiverUuids(), whitelist);
        }).toList();
        return new PipeSnapshot(version, pipes);
    }

    private boolean write(PipeSnapshot snapshot) {
        YamlConfiguration yaml = new YamlConfiguration();
        for (PersistedPipe pipe : snapshot.pipes()) {
            String path = "pipes." + pipe.pipeUuid();
            yaml.set(path + ".owner", pipe.ownerUuid().toString());
            yaml.set(path + ".type", pipe.type().name());
            yaml.set(path + ".world-uuid", pipe.location().worldUuid().toString());
            yaml.set(path + ".world-name", pipe.worldName());
            yaml.set(path + ".x", pipe.location().x());
            yaml.set(path + ".y", pipe.location().y());
            yaml.set(path + ".z", pipe.location().z());
            if (pipe.type() == PipeType.SENDER) yaml.set(path + ".receivers",
                    pipe.receiverUuids().stream().map((UUID uuid) -> uuid.toString()).toList());
            else {
                pipe.senderUuid().ifPresent(sender -> yaml.set(path + ".sender", sender.toString()));
                yaml.set(path + ".whitelist", pipe.whitelist().stream().sorted().toList());
            }
        }
        try {
            yaml.save(dataFile);
            return true;
        } catch (IOException exception) {
            Bukkit.getLogger().log(Level.SEVERE, "[SurvivalSkills] Failed to save pipe data", exception);
            return false;
        }
    }

    private void validateLink(PipeRecord receiver, PipeRecord sender) {
        if (sender == null || sender.type() != PipeType.SENDER) throw new IllegalArgumentException("Missing sender");
        if (!isWithinRange(receiver.location(), sender.location())) throw new IllegalArgumentException("Out-of-range link");
    }

    private void addReceiverToSender(UUID senderUuid, UUID receiverUuid) {
        PipeRecord sender = Objects.requireNonNull(byUuid.get(senderUuid));
        if (sender.receiverUuids().contains(receiverUuid)) return;
        List<UUID> receivers = new ArrayList<>(sender.receiverUuids());
        receivers.add(receiverUuid);
        replace(new PipeRecord(sender.pipeUuid(), sender.ownerUuid(), sender.location(), sender.type(),
                Optional.empty(), receivers, Set.of()));
    }

    private void removeReceiverFromSender(UUID senderUuid, UUID receiverUuid) {
        PipeRecord sender = byUuid.get(senderUuid);
        if (sender == null) return;
        replace(new PipeRecord(sender.pipeUuid(), sender.ownerUuid(), sender.location(), sender.type(),
                Optional.empty(), sender.receiverUuids().stream().filter(id -> !id.equals(receiverUuid)).toList(), Set.of()));
    }

    private void index(PipeRecord record, Set<PipeLocation> locations) {
        byUuid.put(record.pipeUuid(), record);
        indexLocations(record, locations);
        updateEligibility(record);
    }

    private void indexLocations(PipeRecord record, Set<PipeLocation> locations) {
        coveredLocations.put(record.pipeUuid(), Set.copyOf(locations));
        for (PipeLocation location : locations) {
            byLocation.put(location, record.pipeUuid());
            chunkIndex.computeIfAbsent(new ChunkKey(location.worldUuid(), location.chunkX(), location.chunkZ()),
                    ignored -> new HashSet<>()).add(record.pipeUuid());
        }
    }

    private void deindex(UUID pipeUuid) {
        for (PipeLocation location : coveredLocations.remove(pipeUuid)) {
            byLocation.remove(location);
            ChunkKey key = new ChunkKey(location.worldUuid(), location.chunkX(), location.chunkZ());
            Set<UUID> pipes = chunkIndex.get(key);
            if (pipes != null) {
                pipes.remove(pipeUuid);
                if (pipes.isEmpty()) chunkIndex.remove(key);
            }
        }
        loadedSenders.remove(pipeUuid);
        Integer bucket = bucketBySender.remove(pipeUuid);
        if (bucket != null) workBuckets.get(bucket).remove(pipeUuid);
        statuses.remove(pipeUuid);
    }

    private void replace(PipeRecord record) { byUuid.put(record.pipeUuid(), record); }

    private void updateEligibility(PipeRecord record) {
        if (record.type() != PipeType.SENDER) return;
        boolean loaded = coveredLocations.getOrDefault(record.pipeUuid(), Set.of()).stream().allMatch(location -> {
            World world = Bukkit.getWorld(location.worldUuid());
            return world != null && world.isChunkLoaded(location.chunkX(), location.chunkZ());
        });
        if (loaded && loadedSenders.add(record.pipeUuid())) assignBucket(record.pipeUuid());
        if (!loaded && loadedSenders.remove(record.pipeUuid())) {
            Integer bucket = bucketBySender.remove(record.pipeUuid());
            if (bucket != null) workBuckets.get(bucket).remove(record.pipeUuid());
        }
    }

    private void assignBucket(UUID senderUuid) {
        if (bucketBySender.containsKey(senderUuid)) return;
        int least = java.util.stream.IntStream.range(0, workBuckets.size()).boxed()
                .min(Comparator.comparingInt(index -> workBuckets.get(index).size())).orElse(0);
        workBuckets.get(least).add(senderUuid);
        bucketBySender.put(senderUuid, least);
    }

    private Optional<Set<PipeLocation>> chestLocations(Block block) {
        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST) return Optional.empty();
        return discoverLoadedChestLocations(new PipeLocation(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ()));
    }

    private Optional<Set<PipeLocation>> discoverLoadedChestLocations(PipeLocation location) {
        World world = Bukkit.getWorld(location.worldUuid());
        if (world == null || !world.isChunkLoaded(location.chunkX(), location.chunkZ())) return Optional.empty();
        Block block = world.getBlockAt(location.x(), location.y(), location.z());
        if (!(block.getState() instanceof Chest chest)) return Optional.empty();
        if (!(chest.getInventory().getHolder() instanceof DoubleChest doubleChest)) return Optional.of(Set.of(location));
        return doubleChestLocations(doubleChest)
                .filter(locations -> locations.stream()
                        .allMatch(value -> world.isChunkLoaded(value.chunkX(), value.chunkZ())));
    }

    static Optional<Set<PipeLocation>> doubleChestLocations(DoubleChest doubleChest) {
        Set<PipeLocation> locations = java.util.stream.Stream.of(
                        doubleChest.getLeftSide(), doubleChest.getRightSide())
                .flatMap((InventoryHolder holder) -> holder instanceof Chest chest
                        ? java.util.stream.Stream.of(chest.getBlock())
                        : java.util.stream.Stream.empty())
                .map((Block halfBlock) -> new PipeLocation(halfBlock.getWorld().getUID(),
                        halfBlock.getX(), halfBlock.getY(), halfBlock.getZ()))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return locations.size() == 2 ? Optional.of(locations) : Optional.empty();
    }

    private void changed() { dirty = true; version++; }

    private void requireMainThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("Pipe state is main-thread confined");
    }
}
