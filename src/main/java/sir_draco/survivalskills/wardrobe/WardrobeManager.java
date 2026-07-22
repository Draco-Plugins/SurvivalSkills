package sir_draco.survivalskills.wardrobe;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import sir_draco.survivalskills.SurvivalSkills;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public final class WardrobeManager implements Listener {

    public static final int SET_COUNT = 3;
    public static final List<Integer> UNLOCK_LEVELS = List.of(28, 42, 58);
    private static final String DATA_FILE_NAME = "wardrobes.yml";

    private final File dataFile;
    private final FileConfiguration data;
    private final Map<UUID, List<WardrobeSet>> playerSets = new HashMap<>();

    public WardrobeManager(SurvivalSkills plugin) {
        Objects.requireNonNull(plugin, "Plugin cannot be null");
        dataFile = new File(plugin.getDataFolder(), DATA_FILE_NAME);
        if (!dataFile.exists())
            plugin.saveResource(DATA_FILE_NAME, false);
        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public WardrobeSet getSet(UUID playerId, int setIndex) {
        validateSetIndex(setIndex);
        return getPlayerSets(playerId).get(setIndex);
    }

    public Optional<ItemStack> getItem(UUID playerId, int setIndex, WardrobeArmorSlot armorSlot) {
        return getSet(playerId, setIndex).getItem(armorSlot);
    }

    public void setItem(UUID playerId, int setIndex, WardrobeArmorSlot armorSlot, Optional<ItemStack> item) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        Objects.requireNonNull(armorSlot, "Armor slot cannot be null");
        Objects.requireNonNull(item, "Armor item cannot be null");
        replaceSet(playerId, setIndex, getSet(playerId, setIndex).withItem(armorSlot, item));
    }

    public void swapWithEquipped(Player player, int setIndex) {
        Objects.requireNonNull(player, "Player cannot be null");
        WardrobeSet storedSet = getSet(player.getUniqueId(), setIndex);
        WardrobeSet equippedSet = WardrobeSet.fromInventory(player.getInventory());
        storedSet.equip(player.getInventory());
        replaceSet(player.getUniqueId(), setIndex, equippedSet);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        saveAndUnloadPlayer(event.getPlayer().getUniqueId());
    }

    public void saveAndUnloadPlayer(UUID playerId) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        if (!playerSets.containsKey(playerId))
            return;
        writePlayerToConfiguration(playerId);
        if (saveConfiguration())
            playerSets.remove(playerId);
    }

    public void saveAll() {
        playerSets.keySet().forEach((UUID playerId) -> writePlayerToConfiguration(playerId));
        saveConfiguration();
    }

    static String itemPath(UUID playerId, int setIndex, WardrobeArmorSlot armorSlot) {
        return playerId + ".Sets." + (setIndex + 1) + "." + armorSlot.getDisplayName();
    }

    private List<WardrobeSet> getPlayerSets(UUID playerId) {
        Objects.requireNonNull(playerId, "Player ID cannot be null");
        return playerSets.computeIfAbsent(playerId, this::loadPlayerSets);
    }

    private List<WardrobeSet> loadPlayerSets(UUID playerId) {
        List<WardrobeSet> loadedSets = new ArrayList<>();
        for (int setIndex = 0; setIndex < SET_COUNT; setIndex++) {
            WardrobeSet wardrobeSet = WardrobeSet.empty();
            for (WardrobeArmorSlot armorSlot : WardrobeArmorSlot.values()) {
                ItemStack item = data.getItemStack(itemPath(playerId, setIndex, armorSlot));
                if (item != null && armorSlot.accepts(item))
                    wardrobeSet = wardrobeSet.withItem(armorSlot, Optional.of(item));
            }
            loadedSets.add(wardrobeSet);
        }
        return List.copyOf(loadedSets);
    }

    private void replaceSet(UUID playerId, int setIndex, WardrobeSet wardrobeSet) {
        validateSetIndex(setIndex);
        List<WardrobeSet> updatedSets = new ArrayList<>(getPlayerSets(playerId));
        updatedSets.set(setIndex, Objects.requireNonNull(wardrobeSet));
        playerSets.put(playerId, List.copyOf(updatedSets));
    }

    private void writePlayerToConfiguration(UUID playerId) {
        data.set(playerId + ".Sets", null);
        List<WardrobeSet> wardrobeSets = playerSets.getOrDefault(playerId, List.of());
        for (int setIndex = 0; setIndex < wardrobeSets.size(); setIndex++) {
            WardrobeSet wardrobeSet = wardrobeSets.get(setIndex);
            for (WardrobeArmorSlot armorSlot : WardrobeArmorSlot.values()) {
                Optional<ItemStack> item = wardrobeSet.getItem(armorSlot);
                if (item.isPresent())
                    data.set(itemPath(playerId, setIndex, armorSlot), item.get());
            }
        }
    }

    private boolean saveConfiguration() {
        try {
            data.save(dataFile);
            return true;
        } catch (IOException exception) {
            Bukkit.getLogger().log(Level.SEVERE,
                    "[SurvivalSkills] Failed to save wardrobe data", exception);
            return false;
        }
    }

    private static void validateSetIndex(int setIndex) {
        if (setIndex < 0 || setIndex >= SET_COUNT)
            throw new IllegalArgumentException("Wardrobe set index must be between 0 and " + (SET_COUNT - 1));
    }
}
