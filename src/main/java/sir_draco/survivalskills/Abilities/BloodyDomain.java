package sir_draco.survivalskills.Abilities;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.Bosses.ProjectileCalculator;

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
            if (!(ent instanceof LivingEntity)) continue;
            LivingEntity livingEnt = (LivingEntity) ent;
            entities.add(livingEnt);
        }

        for (LivingEntity ent : entities) {
            ent.damage(ent.getHealth(), p);
            ProjectileCalculator.particleLine(p.getLocation(), ent.getLocation(), Particle.DUST, Color.RED);
        }
    }
}
