package sir_draco.survivalskills.Abilities;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TrailEffect extends BukkitRunnable {

    private final Player p;
    private final Particle particle;
    private final int dustType;
    private final String trailName;

    private Location location = null;
    private int count = 0;


    public TrailEffect(Player p, Particle particle, int dustType, String trailName) {
        this.p = p;
        this.particle = particle;
        this.dustType = dustType;
        this.trailName = trailName;
    }

    @Override
    public void run() {
        if (location != null && location.equals(p.getLocation())) return;
        location = p.getLocation();
        Location loc = location.clone().add(0, 0.3, 0);
        if (dustType == 1) p.getWorld().spawnParticle(particle, loc, 0, 0., 0., 0.);
        else if (dustType == 2) p.getWorld().spawnParticle(particle, loc, 1, 0, 0, 0, new Particle.DustOptions(Color.WHITE, 1));
        else {
            Color color = getNextColor();
            p.getWorld().spawnParticle(particle, loc, 2, 0, 0, 0, new Particle.DustOptions(color, 2));
            count++;
            if (count >= 500) count = 0;
        }
    }

    public Color getNextColor() {
        int red = (int) (Math.sin(count * 0.01) * 127 + 128);
        int green = (int) (Math.sin(count * 0.01 + 2) * 127 + 128);
        int blue = (int) (Math.sin(count * 0.01 + 4) * 127 + 128);
        return Color.fromRGB(red, green, blue);
    }

    public String getTrailName() {
        return trailName;
    }
}
