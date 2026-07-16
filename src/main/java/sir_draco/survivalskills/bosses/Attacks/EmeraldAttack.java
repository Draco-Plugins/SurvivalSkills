package sir_draco.survivalskills.bosses.attacks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.ProjectileCalculator;
import sir_draco.survivalskills.utils.Utils;

public class EmeraldAttack {

    private Item item;

    public EmeraldAttack(Location loc, Player target) {
        ItemStack emerald = new ItemStack(Material.EMERALD);
        item = loc.getWorld().dropItem(loc, emerald);
        item.setGravity(false);
        item.setOwner(Utils.DEFAULT_UUID);
        item.setVelocity(ProjectileCalculator.getNoGravityVector(loc, target.getLocation().clone().add(0, 1, 0), 2));
        item.setCustomName("Emerald Bullet");
        target.playSound(target, Sound.ENTITY_SHULKER_SHOOT, 1, 1);
    }

    public void emeraldProjectile() {
        if (item == null) {
            throw new IllegalStateException("[Survival Skills] Emerald projectile is null");
        }
        // Create a bukkit runnable that checks if the emerald is inside the target's hit-box
        new BukkitRunnable() {
            private int count = 0;
            private final Set<UUID> alreadyHitPlayers = new HashSet<>();

            @Override
            public void run() {
                if (count == 100) {
                    item.remove();
                    cancel();
                    return;
                }
                List<Entity> hitPlayers = item.getNearbyEntities(3, 3, 3);
                if (!hitPlayers.isEmpty()) {
                    for (Entity ent : hitPlayers) {
                        if (!(ent instanceof Player p))
                            continue;

                        double distance = Math.sqrt(Math.pow(p.getLocation().getX() - item.getLocation().getX(), 2) +
                                Math.pow(p.getLocation().getZ() - item.getLocation().getZ(), 2));
                        double yDist = Math.abs(p.getLocation().getY() + 1 - item.getLocation().getY());

                        if (distance > 0.85 || yDist > 1.5) continue;
                        if (alreadyHitPlayers.contains(p.getUniqueId())) continue;
                        alreadyHitPlayers.add(p.getUniqueId());
                        p.damage(40, item);
                        p.playSound(p, Sound.BLOCK_ANVIL_HIT, 1, 1);
                    }
                }
                count++;
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);
    }
    
}
