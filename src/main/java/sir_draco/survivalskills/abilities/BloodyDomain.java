package sir_draco.survivalskills.abilities;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.utils.ProjectileCalculator;

public class BloodyDomain extends BukkitRunnable {

    private final Player p;

    public BloodyDomain(Player p) {
        this.p = p;
    }

    @Override
    public void run() {
        for (Entity ent : p.getNearbyEntities(10, 10, 10)) {
            if (!AbilityManager.getDomainMobs().contains(ent.getType())) continue;
            if (!(ent instanceof LivingEntity livingEnt)) continue;
            AttributeInstance attribute = livingEnt.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            if (attribute == null) return;
            double maxHealth = attribute.getValue();
            livingEnt.damage(maxHealth, p);
            ProjectileCalculator.particleLine(p.getLocation(), ent.getLocation(), Particle.DUST, Color.RED);
        }
    }
}
