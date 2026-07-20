package sir_draco.survivalskills.skill_listeners.fishing;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import sir_draco.survivalskills.abilities.AutoTrash;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Owns the auto-trash / perma-trash feature: the player-to-trash registries, the
 * session "viewing-trash" guard list, the auto-trash disable toggle, and all four
 * related inventory/pickup event handlers.
 *
 * <p>The previously duplicated trash-resolution block (copy-pasted between the
 * click and drag handlers) is collapsed into {@link #resolveTrashForPlayer}, and
 * the previously duplicated enchanted-book / material keep-vs-trash check is
 * unified by {@link #shouldTrashItem}. The unified keep rule is: an enchanted book
 * is kept whenever the trash's enchant list is empty or does not contain the
 * book's enchantment; a plain item is kept unless its material is registered.</p>
 */
public class TrashManager {

    private final HashMap<Player, AutoTrash> trashInventories = new HashMap<>();
    private final HashMap<Player, AutoTrash> permaTrash = new HashMap<>();
    private final ArrayList<Player> openTrashInventories = new ArrayList<>();
    private final ArrayList<Player> disabledAutoTrash = new ArrayList<>();

    // =================================================================
    // Public registry API (delegated through FishingSkill).
    // Live concrete references are returned to preserve the original contract:
    // callers (FileUtils, ResetAllCommand, AutoTrashCommand, ...) mutate these
    // collections directly (.put/.clear/.remove/.add).
    // =================================================================

    public HashMap<Player, AutoTrash> getTrashInventories() {
        return trashInventories;
    }

    public HashMap<Player, AutoTrash> getPermaTrash() {
        return permaTrash;
    }

    public ArrayList<Player> getOpenTrashInventories() {
        return openTrashInventories;
    }

    public ArrayList<Player> getDisabledAutoTrash() {
        return disabledAutoTrash;
    }

    public void addTrashInventory(Player p, AutoTrash trash) {
        trashInventories.put(p, trash);
    }

    public void markTrashInventoryOpen(Player p) {
        if (!openTrashInventories.contains(p))
            openTrashInventories.add(p);
    }

    public void removePlayer(Player p) {
        trashInventories.remove(p);
        permaTrash.remove(p);
        disabledAutoTrash.removeIf((Player player) -> Objects.equals(player, p));
        openTrashInventories.removeIf((Player player) -> Objects.equals(player, p));
    }

    // =================================================================
    // Event handlers
    // =================================================================

    public void onTrashClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        if (!openTrashInventories.contains(p))
            return;
        if (resolveTrashForPlayer(p, e.getInventory(), e.getInventory()) == null)
            return;
        openTrashInventories.removeIf((Player player) -> Objects.equals(player, p));
    }

    public void onClickTrashInventory(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (!isPlayerViewingTrash(p))
            return;

        AutoTrash trash = resolveTrashForPlayer(p, e.getInventory(),
                e.getClick().isShiftClick() ? p.getOpenInventory().getTopInventory() : e.getInventory());
        if (trash == null)
            return;

        if (e.getClick().equals(ClickType.DOUBLE_CLICK)) {
            e.setCancelled(true);
            return;
        }

        if (e.getRawSlot() < 0)
            return;
        if (e.getRawSlot() >= trash.getTrashInventory().getSize() && !e.isShiftClick())
            return;

        if (e.isShiftClick()) {
            if (e.getCurrentItem() == null) {
                e.setCancelled(true);
                return;
            }
            if (e.getRawSlot() >= trash.getTrashInventory().getSize()) {
                ItemStack item = e.getCurrentItem();
                if (item == null)
                    return;
                e.setCancelled(true);
                int slot = trash.findOpenSlot();
                if (slot == -1)
                    return;
                trash.addTrashItem(item, slot);
            } else {
                e.setCancelled(true);
                trash.removeTrashItem(e.getCurrentItem(), e.getRawSlot());
            }
        } else {
            if (e.getCurrentItem() != null) {
                e.setCancelled(true);
                trash.removeTrashItem(e.getCurrentItem(), e.getRawSlot());
            } else {
                ItemStack item = e.getCursor();
                if (item == null)
                    return;
                e.setCancelled(true);
                trash.addTrashItem(item, e.getRawSlot());
            }
        }
    }

    public void onDragTrashInventory(InventoryDragEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (!isPlayerViewingTrash(p))
            return;

        AutoTrash trash = resolveTrashForPlayer(p, e.getInventory(), e.getInventory());
        if (trash == null)
            return;
        e.setCancelled(true);

        int slot = e.getRawSlots().iterator().next();
        if (slot >= trash.getTrashInventory().getSize() || slot < 0)
            return;
        if (trash.getTrashInventory().getItem(slot) != null && e.getCursor() != null)
            trash.removeTrashItem(e.getCursor(), slot);
        else {
            ItemStack item = e.getOldCursor();
            if (item.getType().isAir())
                return;
            trash.addTrashItem(item, slot);
        }
    }

    public void onItemPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p))
            return;
        if (disabledAutoTrash.contains(p))
            return;
        if (!trashInventories.containsKey(p) && !permaTrash.containsKey(p))
            return;

        ItemStack item = e.getItem().getItemStack();
        if (item.getItemMeta() != null && ItemStackGeneratorUtils.hasCustomModelData(item.getItemMeta()))
            return;
        if (item.getItemMeta() != null
                && item.getItemMeta().getPersistentDataContainer().has(ItemStackGeneratorUtils.skillsItemKey))
            return;

        if (trashIfApplicable(trashInventories, p, e, item))
            return;
        trashIfApplicable(permaTrash, p, e, item);
    }

    // =================================================================
    // Shared helpers
    // =================================================================

    /**
     * Returns true if {@code p} is currently viewing a trash inventory, cleaning
     * up the stale "open" flag if the underlying registry entries have vanished.
     * Replaces the duplicated guard at the top of the click and drag handlers.
     */
    private boolean isPlayerViewingTrash(Player p) {
        if (!openTrashInventories.contains(p))
            return false;
        if (!trashInventories.containsKey(p) && !permaTrash.containsKey(p)) {
            openTrashInventories.remove(p);
            return false;
        }
        return true;
    }

    /**
     * Resolves which trash instance the player is interacting with. Session trash
     * is matched against {@code sessionCompareInventory} (the top inventory when
     * shift-clicking, the event inventory otherwise); perma-trash is always
     * matched against the event inventory. Collapses the previously duplicated
     * ownership-resolution block from the click and drag handlers.
     */
    private AutoTrash resolveTrashForPlayer(Player p, Inventory eventInventory, Inventory sessionCompareInventory) {
        if (trashInventories.containsKey(p)) {
            AutoTrash check = trashInventories.get(p);
            if (Objects.equals(sessionCompareInventory, check.getTrashInventory()))
                return check;
        }
        if (permaTrash.containsKey(p)) {
            AutoTrash check = permaTrash.get(p);
            if (Objects.equals(eventInventory, check.getTrashInventory()))
                return check;
        }
        return null;
    }

    /**
     * Decides whether {@code item} should be auto-trashed by {@code trash}.
     * Unifies the previously duplicated and subtly divergent enchanted-book
     * checks that existed separately for session trash and perma-trash. The keep
     * rule is: an enchanted book is kept when the trash's enchant list is empty
     * or does not contain the book's enchantment; any other item is kept unless
     * its material is registered. Returns {@code false} when input is malformed
     * (no meta, not an enchant book, empty stored enchants, or null trash).
     */
    private boolean shouldTrashItem(AutoTrash trash, ItemStack item) {
        if (trash == null)
            return false;
        if (item.getType().equals(Material.ENCHANTED_BOOK)) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null)
                return false;
            if (!(meta instanceof EnchantmentStorageMeta enchantMeta))
                return false;
            if (enchantMeta.getStoredEnchants().isEmpty())
                return false;
            Enchantment enchant =
                    enchantMeta.getStoredEnchants().keySet().iterator().next();
            // Keep when there are no trash enchants or this enchant is not trashed.
            if (trash.getEnchants().isEmpty() || !trash.getEnchants().contains(enchant))
                return false;
            return true;
        }
        return trash.getTrashMaterials().contains(item.getType());
    }

    /**
     * Cancels the pickup and removes the dropped item if {@code registry} holds a
     * trash for {@code p} that wants {@code item}. Returns true when handled.
     */
    private boolean trashIfApplicable(Map<Player, AutoTrash> registry, Player p,
                                      EntityPickupItemEvent e, ItemStack item) {
        AutoTrash trash = registry.get(p);
        if (!shouldTrashItem(trash, item))
            return false;
        e.setCancelled(true);
        e.getItem().remove();
        return true;
    }
}
