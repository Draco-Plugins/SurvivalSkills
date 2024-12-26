package sir_draco.survivalskills.Abilities;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SkillListeners.GodListener;

public class PowerLaser extends BukkitRunnable {

    private final Player p;
    private final GodListener listener;
    private final Location startLocation;
    private final Location endLocation;

    private int time = 0;

    public PowerLaser(Player p, GodListener listener) {
        this.p = p;
        this.listener = listener;
        startLocation = p.getEyeLocation();
        endLocation = getEndLocation();
    }

    @Override
    public void run() {
        if (time == 40) {
            cancel();
            listener.getPowerLaserCooldowns().remove(p);
            return;
        }

        if (time == 0) {
            // Create sonic boom particles from the start to the end
            for (int i = 0; i <= 49; i++) {
                Location loc = startLocation.clone().add(startLocation.getDirection().multiply(i));
                if (loc.getWorld() == null) break;
                if (startLocation.distance(loc) > startLocation.distance(endLocation)) break;

                loc.getWorld().spawnParticle(Particle.SONIC_BOOM, loc, 1);
            }
        }

        // Damage entities near the beam from the start location until the end location
        if (time == 10) {
            Location pLoc = p.getLocation();
            if (pLoc.getWorld() == null) return;
            pLoc.getWorld().playSound(pLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1, 1);

            for (int i = 0; i <= 49; i++) {
                Location loc = startLocation.clone().add(startLocation.getDirection().multiply(i));
                if (loc.getWorld() == null) break;
                if (startLocation.distance(loc) > startLocation.distance(endLocation)) break;

                for (Entity ent : loc.getWorld().getNearbyEntities(loc, 1, 1, 1)) {
                    if (!(ent instanceof LivingEntity livingEnt)) continue;
                    if (livingEnt == p) continue;
                    livingEnt.damage(25, p);
                }
            }
        }

        time++;
    }

    public Location getEndLocation() {
        // Find the next block in the direction the player is looking
        Location loc = startLocation.clone();
        for (int i = 0; i <= 49; i++) {
            loc.add(loc.getDirection());
            if (!loc.getBlock().getType().equals(Material.AIR) && !loc.getBlock().getType().equals(Material.WATER)) return loc;
        }
        return loc.add(loc.getDirection().multiply(50));
    }
}
