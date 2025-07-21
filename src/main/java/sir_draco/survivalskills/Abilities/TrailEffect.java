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
    private int r = 120;
    private int g = 0;
    private int b = 0;
    private boolean r1 = true;
    private boolean g1 = false;
    private boolean b1 = false;


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
        }
    }

    public Color getNextColor() {
        // Increment Colors
        if (r1) {
            r+=10;
            if (r > 255) r = 255;
        }
        if (g1) {
            g+=10;
            if (g > 255) g = 255;
        }
        if (b1) {
            b+=10;
            if (b > 255) b = 255;
        }

        if (r == 255 && r1 && !g1 && !b1) {
            r1 = false;
            g1 = true;
            r = 0;
            g = 120;
            return Color.fromRGB(r, g, b);
        }
        if (g == 255 && !r1 && g1 && !b1) {
            g1 = false;
            b1 = true;
            g = 0;
            b = 120;
            return Color.fromRGB(r, g, b);
        }
        if (b == 255 && !r1 && !g1 && b1) {
            r1 = true;
            g1 = true;
            b1 = false;
            r = 120;
            g = 120;
            b = 0;
            return Color.fromRGB(r, g, b);
        }
        if (r == 255 && r1 && g1 && !b1) {
            r1 = false;
            b1 = true;
            r = 0;
            g = 120;
            b = 120;
            return Color.fromRGB(r, g, b);
        }
        if (g == 255 && !r1 && g1) {
            r1 = true;
            g1 = false;
            r = 120;
            g = 0;
            b = 120;
            return Color.fromRGB(r, g, b);
        }
        if (r == 255 && r1 && !g1) {
            g1 = true;
            r = 120;
            g = 120;
            b = 120;
            return Color.fromRGB(r, g, b);
        }
        if (r == 255 && r1) {
            g1 = false;
            b1 = false;
            r = 120;
            g = 0;
            b = 0;
        }

        return Color.fromRGB(r, g, b);
    }

    public String getTrailName() {
        return trailName;
    }
}
