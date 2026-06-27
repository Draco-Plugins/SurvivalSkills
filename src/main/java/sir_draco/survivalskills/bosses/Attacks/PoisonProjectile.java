package sir_draco.survivalskills.bosses.Attacks;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.Utils;

public class PoisonProjectile {

    private Item item;
    private Player target;

    public PoisonProjectile(Location loc, Player target) {
        this.target = target;
        loc.getWorld().playSound(loc, Sound.BLOCK_SLIME_BLOCK_PLACE, 10, 1);
        double x = Math.random() - 0.5;
        double y = Math.random() - 0.5;
        double z = Math.random() - 0.5;
        Vector v = new Vector(x, y, z);
        item = loc.getWorld().dropItem(loc, new ItemStack(Material.SLIME_BALL));
        item.setOwner(Utils.DEFAULT_UUID);
        item.setGravity(false);
        item.setVelocity(v.multiply(0.35));
        item.setCustomName("Poison Projectile");
    }
    
    public void poisonProjectile() {
        if (item == null) {
            throw new IllegalStateException("[Survival Skills] Poison projectile is null");
        }

        if (target == null) {
            throw new IllegalStateException("[Survival Skills] Poison projectile has no target");
        }


        new BukkitRunnable() {
            private int count = 0;

            @Override
            public void run() {
                if (count == 120) {
                    item.remove();
                    cancel();
                    return;
                }

                count++;
                double distance = item.getLocation().distance(target.getLocation());
                if (distance > 2.0) return;
                target.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 4));
                target.damage(60, item);
                target.playSound(target, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, 1, 1);
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);
    }
}
