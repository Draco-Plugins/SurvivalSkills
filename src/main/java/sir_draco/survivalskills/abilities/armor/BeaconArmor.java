package sir_draco.survivalskills.abilities.armor;

import org.bukkit.Color;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import sir_draco.survivalskills.skill_listeners.ArmorListener;
import sir_draco.survivalskills.skill_listeners.ArmorListener.ArmorType;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;

import static sir_draco.survivalskills.skill_listeners.ArmorListener.beaconEffects;

public class BeaconArmor extends BukkitRunnable {

    private final Player p;

    private int count = 0;

    public BeaconArmor(Player p) {
        this.p = p;
    }

    public void run() {
        PlayerInventory playerInventory = p.getInventory();

        if (!ArmorListener.isWearingArmor(p.getUniqueId(), ArmorType.BEACON)) {
            cancel();
            return;
        }

        Color armorColor = convertCountToRGB(count);
        setArmor(playerInventory, armorColor);

        if (count % 40 == 0)
            handleBeaconEffect();

        // Reset count
        count += 1;
        if (count >= 500)
            count = 0;
    }

    private void setArmor(PlayerInventory inv, Color color) {
        if (inv.getBoots() == null || !ItemStackGeneratorUtils.isCustomItem(inv.getBoots(), 29)) {
            ArmorListener.removeArmor(p.getUniqueId(), ArmorType.BEACON);
            cancel();
            return;
        }
        ItemStack boots = colorArmor(inv.getBoots(), color);
        p.sendEquipmentChange(p, EquipmentSlot.FEET, boots);

        if (inv.getLeggings() == null || !ItemStackGeneratorUtils.isCustomItem(inv.getLeggings(), 29)) {
            ArmorListener.removeArmor(p.getUniqueId(), ArmorType.BEACON);
            cancel();
            return;
        }
        ItemStack leggings = colorArmor(inv.getLeggings(), color);
        p.sendEquipmentChange(p, EquipmentSlot.LEGS, leggings);

        if (inv.getChestplate() == null || !ItemStackGeneratorUtils.isCustomItem(inv.getChestplate(), 29)) {
            ArmorListener.removeArmor(p.getUniqueId(), ArmorType.BEACON);
            cancel();
            return;
        }
        ItemStack chestplate = colorArmor(inv.getChestplate(), color);
        p.sendEquipmentChange(p, EquipmentSlot.CHEST, chestplate);

        if (inv.getHelmet() == null || !ItemStackGeneratorUtils.isCustomItem(inv.getHelmet(), 29)) {
            ArmorListener.removeArmor(p.getUniqueId(), ArmorType.BEACON);
            cancel();
            return;
        }
        ItemStack helmet = colorArmor(inv.getHelmet(), color);
        p.sendEquipmentChange(p, EquipmentSlot.HEAD, helmet);
    }

    private ItemStack colorArmor(ItemStack armor, Color armorColor) {
        armor = armor.clone();
        if (armor.getItemMeta() == null) {
            ArmorListener.removeArmor(p.getUniqueId(), ArmorType.BEACON);
            return armor;
        }
        if (!(armor.getItemMeta() instanceof LeatherArmorMeta meta)) {
            ArmorListener.removeArmor(p.getUniqueId(), ArmorType.BEACON);
            return armor;
        }
        meta.setColor(armorColor);
        armor.setItemMeta(meta);
        return armor;
    }

    private Color convertCountToRGB(int count) {
        int red = (int) (Math.sin(count * 0.01) * 127 + 128);
        int green = (int) (Math.sin(count * 0.01 + 2) * 127 + 128);
        int blue = (int) (Math.sin(count * 0.01 + 4) * 127 + 128);
        return Color.fromRGB(red, green, blue);
    }

    private void handleBeaconEffect() {
        for (PotionEffect effect : beaconEffects)
            p.addPotionEffect(effect);
        for (Entity entity : p.getNearbyEntities(10, 10, 10)) {
            if (!(entity instanceof Player player))
                continue;
            for (PotionEffect effect : beaconEffects)
                player.addPotionEffect(effect);
        }
    }
}
