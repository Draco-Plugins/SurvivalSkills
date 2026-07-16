package sir_draco.survivalskills.bosses.attacks;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Fireball;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import sir_draco.survivalskills.SurvivalSkills;

public class Meteor {

    private Fireball fireball;

    public Meteor(Location loc) {
        loc.setY(loc.getY() + 30);
        Vector direction = new Vector(0, -1, 0);
        // Launch a fireball projectile from the location
        fireball = loc.getWorld().spawn(loc, Fireball.class);
        fireball.setDirection(direction);
    }
    
    public void launchMeteor() {
        if (fireball == null) {
            throw new IllegalStateException("[Survival Skills] Fireball projectile for meteor is null");
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                if (fireball.isDead()) {
                    cancel();
                    return;
                }

                // If the fireball hits a block or player, explode
                if (fireball.isOnGround() || !fireball.getNearbyEntities(1, 1, 1).isEmpty()
                        || !fireball.getLocation().getBlock().getType().isAir()) {
                    fireball.getWorld().createExplosion(fireball.getLocation(), 6, false, false, fireball);
                    fireball.remove();
                    cancel();
                    return;
                }

                // Spawn explosion particles as the fireball falls
                Location particleLoc = fireball.getLocation().clone().add(0, 1, 0);
                fireball.getWorld().spawnParticle(Particle.EXPLOSION, particleLoc, 3);
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);
    }
}
