package sir_draco.survivalskills.abilities.godItems;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.HashSet;
import java.util.Set;

/** Charges a player forward, damaging each mob caught in the path once. */
public final class RavagerDash extends BukkitRunnable {

    private static final double DAMAGE = 8.0;
    private static final double DAMAGE_RADIUS = 1.5;
    private static final double INITIAL_SPEED = 2.0;
    private static final double SUSTAINED_SPEED = 1.2;
    private static final int DURATION_TICKS = 10;

    private final Player player;
    private final Set<Integer> damagedEntityIds = new HashSet<>();
    private int ticks;

    private RavagerDash(Player player) {
        this.player = player;
    }

    @Override
    public void run() {
        if (!player.isOnline() || player.isDead()) {
            cancel();
            return;
        }

        propelPlayer(ticks == 0 ? INITIAL_SPEED : SUSTAINED_SPEED);
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation(), 5,
                0.35, 0.15, 0.35, 0.02);
        damageNearbyMobs();

        ticks++;
        if (ticks >= DURATION_TICKS)
            cancel();
    }

    private void propelPlayer(double speed) {
        Vector direction = player.getLocation().getDirection().normalize().multiply(speed);
        player.setVelocity(direction);
    }

    private void damageNearbyMobs() {
        for (Entity entity : player.getNearbyEntities(DAMAGE_RADIUS, DAMAGE_RADIUS, DAMAGE_RADIUS)) {
            if (!(entity instanceof Mob mob) || !damagedEntityIds.add(entity.getEntityId()))
                continue;
            mob.damage(DAMAGE, player);
        }
    }

    public static void activate(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.0f, 1.0f);
        new RavagerDash(player).runTaskTimer(SurvivalSkills.getInstance(), 0L, 1L);
    }
}
