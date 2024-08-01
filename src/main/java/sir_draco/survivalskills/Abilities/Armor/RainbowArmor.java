package sir_draco.survivalskills.Abilities.Armor;

import org.bukkit.Color;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import static sir_draco.survivalskills.SkillListeners.ArmorListener.beaconEffects;
import static sir_draco.survivalskills.SkillListeners.ArmorListener.playersWearingBeaconArmor;

public class RainbowArmor extends BukkitRunnable {

    private final Player p;

    private int count = 0;

    public RainbowArmor(Player p) {
        this.p = p;
    }

    public void run() {
        PlayerInventory playerInventory = p.getInventory();

        if (!playersWearingBeaconArmor.contains(p.getUniqueId())) {
            cancel();
            return;
        }

        Color armorColor = convertCountToRGB(count);
        setArmor(playerInventory, armorColor);

        if (count % 40 == 0) handleBeaconEffect();

        // Reset count
        count += 1;
        if (count >= 500) count = 0;
    }

    private void setArmor(PlayerInventory inv, Color color) {
        if (inv.getBoots() == null) {
            playersWearingBeaconArmor.remove(p.getUniqueId());
            cancel();
            return;
        }
        inv.setBoots(colorArmor(inv.getBoots(), color));

        if (inv.getLeggings() == null) {
            playersWearingBeaconArmor.remove(p.getUniqueId());
            cancel();
            return;
        }
        inv.setLeggings(colorArmor(inv.getLeggings(), color));

        if (inv.getChestplate() == null) {
            playersWearingBeaconArmor.remove(p.getUniqueId());
            cancel();
            return;
        }
        inv.setChestplate(colorArmor(inv.getChestplate(), color));

        if (inv.getHelmet() == null) {
            playersWearingBeaconArmor.remove(p.getUniqueId());
            cancel();
            return;
        }
        inv.setHelmet(colorArmor(inv.getHelmet(), color));
    }

    private ItemStack colorArmor(ItemStack armor, Color armorColor) {
        if (armor.getItemMeta() == null) {
            playersWearingBeaconArmor.remove(p.getUniqueId());
            return armor;
        }
        if (!(armor.getItemMeta() instanceof LeatherArmorMeta)) {
            playersWearingBeaconArmor.remove(p.getUniqueId());
            return armor;
        }
        LeatherArmorMeta meta = (LeatherArmorMeta) armor.getItemMeta();
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
        for (PotionEffect effect : beaconEffects) p.addPotionEffect(effect);
        for (Entity entity : p.getNearbyEntities(10, 10, 10)) {
            if (!(entity instanceof Player)) continue;
            Player player = (Player) entity;
            for (PotionEffect effect : beaconEffects) player.addPotionEffect(effect);
        }
    }
}
