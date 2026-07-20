package sir_draco.survivalskills.abilities;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AutoTrash {

    private final ArrayList<Enchantment> enchants = new ArrayList<>();
    private final ArrayList<Material> trashMaterials = new ArrayList<>();
    private final String inventoryName;

    private boolean big;

    private Inventory trashInventory;

    public AutoTrash(boolean big, boolean permaTrash) {
        this.big = big;
        inventoryName = permaTrash ? "Perma Trash" : "Auto Trash";
        trashInventory = createTrashInventory(big);
    }

    public void addTrashItem(ItemStack item, int slot) {
        if (slot < 0 || slot >= trashInventory.getSize()) return;
        ItemStack clone = item.clone();
        clone.setAmount(1);
        if (clone.getType().equals(Material.ENCHANTED_BOOK)) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;
            if (!(meta instanceof EnchantmentStorageMeta enchantMeta)) return;
            if (enchantMeta.getStoredEnchants().isEmpty()) return;
            Enchantment enchant = enchantMeta.getStoredEnchants().keySet().iterator().next();
            if (enchants.contains(enchant)) return;
            enchants.add(enchant);
        }
        else {
            if (trashMaterials.contains(clone.getType())) return;
            trashMaterials.add(clone.getType());
        }
        trashInventory.setItem(slot, clone);
    }

    public void addTrashItem(ItemStack item) {
        ItemStack clone = item.clone();
        clone.setAmount(1);
        if (clone.getType().equals(Material.ENCHANTED_BOOK)) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;
            if (!(meta instanceof EnchantmentStorageMeta enchantMeta)) return;
            if (enchantMeta.getStoredEnchants().isEmpty()) return;
            Enchantment enchant = enchantMeta.getStoredEnchants().keySet().iterator().next();
            if (enchants.contains(enchant)) return;
            enchants.add(enchant);
        }
        else {
            if (trashMaterials.contains(clone.getType())) return;
            trashMaterials.add(clone.getType());
        }
        trashInventory.addItem(clone);
    }

    public void removeTrashItem(ItemStack item, int slot) {
        if (item.getType().equals(Material.ENCHANTED_BOOK)) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;
            if (!(meta instanceof EnchantmentStorageMeta enchantMeta)) return;
            if (enchantMeta.getStoredEnchants().isEmpty()) return;
            Enchantment enchant = enchantMeta.getStoredEnchants().keySet().iterator().next();
            enchants.remove(enchant);
        }
        else trashMaterials.remove(item.getType());
        trashInventory.setItem(slot, null);
    }

    public int findOpenSlot() {
        for (int i = 0; i < trashInventory.getSize(); i++) {
            if (trashInventory.getItem(i) == null) return i;
        }
        return -1;
    }

    public void upgradeTrashSize() {
        if (big) return;

        List<HumanEntity> viewers = List.copyOf(trashInventory.getViewers());
        Inventory newTrashInventory = createTrashInventory(true);
        trashInventory = newTrashInventory;
        if (!trashMaterials.isEmpty()) {
            for (Material mat : trashMaterials) {
                ItemStack item = new ItemStack(mat);
                int slot = findOpenSlot();
                if (slot != -1) newTrashInventory.setItem(slot, item);
            }
        }

        if (!enchants.isEmpty()) {
            for (Enchantment book : enchants) {
                int slot = findOpenSlot();
                if (slot != -1) {
                    ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
                    EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
                    if (meta == null) continue;
                    meta.addStoredEnchant(book, 1, true);
                    item.setItemMeta(meta);
                    newTrashInventory.setItem(slot, item);
                }
            }
        }
        big = true;

        for (HumanEntity viewer : viewers) {
            viewer.openInventory(newTrashInventory);
        }
    }

    private Inventory createTrashInventory(boolean useLargeInventory) {
        int size = useLargeInventory ? 54 : 27;
        return Bukkit.createInventory(null, size, inventoryName);
    }

    public void openTrashInventory(Player p) {
        if (Objects.equals(p.getOpenInventory().getTopInventory(), trashInventory)) return;
        p.openInventory(trashInventory);
    }

    public List<Enchantment> getEnchants() {
        return enchants;
    }

    public List<Material> getTrashMaterials() {
        return trashMaterials;
    }

    public Inventory getTrashInventory() {
        return trashInventory;
    }

    public boolean isBig() {
        return big;
    }
}
