package sir_draco.survivalskills.Abilities;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

public class AutoTrash {

    private final ArrayList<Enchantment> enchants = new ArrayList<>();
    private final ArrayList<Material> trashMaterials = new ArrayList<>();

    private Boolean big = false;

    private Inventory trashInventory;

    public AutoTrash(boolean big) {
        this.big = big;
        if (big) trashInventory = Bukkit.createInventory(null, 54, "Auto Trash");
        else trashInventory = Bukkit.createInventory(null, 27, "Auto Trash");
    }

    public void addTrashItem(ItemStack item, int slot) {
        if (slot < 0 || slot >= trashInventory.getSize()) return;
        ItemStack clone = item.clone();
        clone.setAmount(1);
        if (clone.getType().equals(Material.ENCHANTED_BOOK)) {
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;
            if (!(meta instanceof EnchantmentStorageMeta)) return;
            EnchantmentStorageMeta enchantMeta = (EnchantmentStorageMeta) meta;
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
            if (!(meta instanceof EnchantmentStorageMeta)) return;
            EnchantmentStorageMeta enchantMeta = (EnchantmentStorageMeta) meta;
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
            if (!(meta instanceof EnchantmentStorageMeta)) return;
            EnchantmentStorageMeta enchantMeta = (EnchantmentStorageMeta) meta;
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
        Inventory newTrashInventory = Bukkit.createInventory(null, 54, "Auto Trash");
        trashInventory.clear();
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

        trashInventory = newTrashInventory;
        big = true;
    }

    public void openTrashInventory(Player p) {
        p.openInventory(trashInventory);
    }

    public ArrayList<Enchantment> getEnchants() {
        return enchants;
    }

    public ArrayList<Material> getTrashMaterials() {
        return trashMaterials;
    }

    public Inventory getTrashInventory() {
        return trashInventory;
    }

    public Boolean isBig() {
        return big;
    }
}
