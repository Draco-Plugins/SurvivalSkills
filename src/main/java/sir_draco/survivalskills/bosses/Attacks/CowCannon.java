package sir_draco.survivalskills.bosses.Attacks;

import org.bukkit.Location;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.ProjectileCalculator;

public class CowCannon {

    private Cow cow;
    private Player target;
    
    public CowCannon(Location loc, Player target) {
        cow = loc.getWorld().spawn(loc, Cow.class);
        this.target = target;
        cow.setGravity(false);
        Vector direction = ProjectileCalculator.getNoGravityVector(loc, target.getLocation().clone().add(0, 1, 0),
                2.5);
        cow.setVelocity(direction);
    }

    public void cowCannon() {
        if (cow == null) {
            throw new IllegalStateException("[Survival Skills] Cow cannon entity has not spawned");
        }

        if (target == null) {
            throw new IllegalStateException("[Survival Skills] Cow cannon has no target");
        }

        new BukkitRunnable() {
            private final Cow cowCannon = cow;
            private int count = 0;

            @Override
            public void run() {
                if (count >= 80) {
                    cowCannon.getWorld().createExplosion(cowCannon.getLocation(), 8);
                    cowCannon.remove();
                    cancel();
                    return;
                }

                // If the cow hits a player or the ground, explode
                cowCannon.setGravity(false);
                count++;
                if (cowCannon.getLocation().distance(target.getLocation()) > 2 && !cowCannon.isOnGround())
                    return;
                cowCannon.getWorld().createExplosion(cowCannon.getLocation(), 8);
                cowCannon.remove();
                cancel();
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);
    }
}
