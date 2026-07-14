package sir_draco.survivalskills.skill_listeners.god;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Owns all potion bag state and interaction handling: creation, opening,
 * filtering (only potions may be stored), destruction and persistence.
 */
public class PotionBagListener implements Listener {

    public static final NamespacedKey POTION_BAG_KEY =
            new NamespacedKey(SurvivalSkills.getInstance(), "potion_bag");

    private static final int POTION_BAG_SIZE = 9;
    private static final String POTION_BAGS_FILE = "potionbags.yml";

    private final Map<Integer, Inventory> potionBags = new HashMap<>();
    private final Set<Inventory> openPotionBags = new HashSet<>();

    /**
     * Opens (creating if necessary) the potion bag bound to the given item,
     * loading its contents from disk on first access.
     */
    public void openPotionBag(Player player, ItemStack hand) {
        int id = getPotionBagID(hand);

        Inventory existing = potionBags.get(id);
        if (existing != null) {
            openPotionBags.add(existing);
            player.openInventory(existing);
            return;
        }

        File file = new File(SurvivalSkills.getInstance().getDataFolder(), POTION_BAGS_FILE);
        if (!file.exists())
            SurvivalSkills.getInstance().saveResource(POTION_BAGS_FILE, false);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (potionBagExists(id, config)) {
            Inventory loaded = loadPotionBag(id, config);
            openPotionBags.add(loaded);
            player.openInventory(loaded);
            return;
        }

        Inventory potionBag = Bukkit.createInventory(null, POTION_BAG_SIZE, "Potion Bag");
        potionBags.put(id, potionBag);
        openPotionBags.add(potionBag);
        player.openInventory(potionBag);
    }

    @EventHandler
    public void potionBagClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (e.getClick().equals(ClickType.DOUBLE_CLICK)) {
            if (!openPotionBags.contains(p.getOpenInventory().getTopInventory()))
                return;
            if (e.getCurrentItem() == null)
                return;
            if (isNotPotion(e.getCurrentItem().getType())) {
                e.setCancelled(true);
                return;
            }
        }

        if (!openPotionBags.contains(e.getInventory()))
            return;
        if (e.getCursor() == null)
            return;
        if (isNotPotion(e.getCursor().getType())) {
            e.setCancelled(true);
            p.sendRawMessage(ChatColor.RED + "Only potions can go in this bag");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        }
    }

    @EventHandler
    public void potionDrag(InventoryDragEvent e) {
        if (!openPotionBags.contains(e.getInventory()))
            return;
        Player p = (Player) e.getWhoClicked();
        if (isNotPotion(e.getOldCursor().getType())) {
            e.setCancelled(true);
            p.sendRawMessage(ChatColor.RED + "Only potions can go in this bag");
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        }
    }

    @EventHandler
    public void onPotionBagDestroy(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Item item))
            return;
        ItemStack stack = item.getItemStack();
        if (!isPotionBag(stack))
            return;

        int id = getPotionBagID(stack);
        item.remove();
        removePotionBag(id);
    }

    private boolean isPotionBag(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null)
            return false;
        return meta.getPersistentDataContainer().has(POTION_BAG_KEY, PersistentDataType.INTEGER);
    }

    public boolean isNotPotion(Material mat) {
        return !mat.equals(Material.POTION)
                && !mat.equals(Material.SPLASH_POTION)
                && !mat.equals(Material.LINGERING_POTION);
    }

    public void savePotionBags(FileConfiguration config) {
        for (Map.Entry<Integer, Inventory> bag : potionBags.entrySet()) {
            config.set(bag.getKey() + ".Items", null);
            if (bag.getValue().isEmpty()) {
                config.set(bag.getKey().toString(), false);
                continue;
            }
            config.set(bag.getKey().toString(), true);

            int i = 0;
            for (ItemStack item : bag.getValue().getContents()) {
                config.set(bag.getKey() + ".Items." + i, item);
                i++;
            }
        }
    }

    public boolean potionBagExists(int id, FileConfiguration config) {
        return config.contains(String.valueOf(id));
    }

    public Inventory loadPotionBag(int id, FileConfiguration config) {
        Inventory bag = Bukkit.createInventory(null, POTION_BAG_SIZE, "Potion Bag");
        potionBags.put(id, bag);
        if (!config.getBoolean(String.valueOf(id)))
            return bag;
        if (!config.contains(id + ".Items"))
            return bag;

        ConfigurationSection section = config.getConfigurationSection(id + ".Items");
        if (section == null)
            return bag;

        section.getKeys(false).forEach(key -> {
            ItemStack item = config.getItemStack(id + ".Items." + key);
            if (item == null)
                return;
            bag.addItem(item);
        });
        return bag;
    }

    public void removePotionBag(int id) {
        Inventory removed = potionBags.remove(id);
        if (removed != null)
            openPotionBags.remove(removed);
        deletePotionBagFromFile(id);
    }

    private void deletePotionBagFromFile(int id) {
        File file = new File(SurvivalSkills.getInstance().getDataFolder(), POTION_BAGS_FILE);
        if (!file.exists())
            SurvivalSkills.getInstance().saveResource(POTION_BAGS_FILE, true);
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        data.set(String.valueOf(id), null);
        try {
            data.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.WARNING,
                    String.format("[SurvivalSkills] Failed to persist removal of potion bag %d", id), e);
        }
    }

    public int getPotionBagID(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return 0;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        int id = container.getOrDefault(POTION_BAG_KEY, PersistentDataType.INTEGER,
                ItemStackGeneratorUtils.nextPotionBagId());
        item.setItemMeta(meta);
        return id;
    }

    /** Removes all in-memory potion bags (used by the reset-all command). */
    public void clearPotionBags() {
        potionBags.clear();
        openPotionBags.clear();
    }
}