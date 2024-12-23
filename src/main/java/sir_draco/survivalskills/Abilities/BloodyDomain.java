package sir_draco.survivalskills.Abilities;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.Utils.ProjectileCalculator;

import java.util.ArrayList;

public class BloodyDomain extends BukkitRunnable {

    private final Player p;

    public BloodyDomain(Player p) {
        this.p = p;
    }

    @Override
    public void run() {
        ArrayList<LivingEntity> entities = new ArrayList<>();

        for (Entity ent : p.getNearbyEntities(10, 10, 10)) {
            if (!AbilityManager.getDomainMobs().contains(ent.getType())) continue;
            if (!(ent instanceof LivingEntity livingEnt)) continue;
            entities.add(livingEnt);
        }

        for (LivingEntity ent : entities) {
            AttributeInstance attribute = ent.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            if (attribute == null) return;
            double maxHealth = attribute.getValue();
            ent.damage(maxHealth, p);
            ProjectileCalculator.particleLine(p.getLocation(), ent.getLocation(), Particle.DUST, Color.RED);
        }
    }
}
