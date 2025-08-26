package sir_draco.survivalskills.abilities.items;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.abilities.AbilityManager;

import java.util.HashSet;
import java.util.Set;

/**
 * Handles the Power Sword right click special ability.
 * Behaviour:
 * - Player dashes forward (riptide style visual) for a short duration
 * - Each tick hostile mobs within a 2 block radius take the player's normal
 * sword damage once
 */
public class PowerSword extends BukkitRunnable {

    private static final double AOE_RADIUS = 2.0;
    private static final int DURATION_TICKS = 12; // Short, snappy dash (~0.6s)
    private static final double FORWARD_MULTIPLIER = 2.2; // Initial push strength
    private static final double CONTINUOUS_FORCE = 0.55; // Additional force each tick to sustain dash

    private final Player player;
    private final Set<Integer> damagedEntityIds = new HashSet<>();
    private int ticks = 0;

    public PowerSword(Player player) {
        this.player = player;
    }

    @Override
    public void run() {
        // Safety: stop if player invalid
        if (!player.isOnline() || player.isDead()) {
            player.setRiptiding(false);
            cancel();
            return;
        }

        if (ticks == 0) {
            // Initial burst + sound similar to riptide
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 1.0f);
            dashForward(FORWARD_MULTIPLIER);
        } else {
            dashForward(CONTINUOUS_FORCE);
        }

        // Particles to sell motion (use sweep + crit mix)
        if (player.getWorld() != null) {
            player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, player.getLocation().add(0, 1.0, 0), 2, 0.2, 0.2,
                    0.2, 0.01);
            player.getWorld().spawnParticle(Particle.CRIT, player.getLocation(), 6, 0.3, 0.3, 0.3, 0.05);
        }

        // Damage nearby hostile mobs once
        for (Entity ent : player.getNearbyEntities(AOE_RADIUS, AOE_RADIUS, AOE_RADIUS)) {
            if (!(ent instanceof LivingEntity living))
                continue;
            if (ent.getType() == EntityType.PLAYER)
                continue; // Skip players
            if (!AbilityManager.getDomainMobs().contains(ent.getType()))
                continue; // Only hostile / domain mobs
            if (damagedEntityIds.contains(ent.getEntityId()))
                continue; // Already damaged this dash

            living.damage(15, player);
            damagedEntityIds.add(ent.getEntityId());
            // Add a lightning-ish crack sound without actual strike for feedback
            player.getWorld().playSound(living.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.2f, 1.6f);
        }

        ticks++;
        if (ticks >= DURATION_TICKS) {
            player.setRiptiding(false);
            cancel();
        }
    }

    private void dashForward(double multiplier) {
        Vector direction = player.getLocation().getDirection().normalize();
        // Blend current velocity to keep motion smooth
        Vector newVel = player.getVelocity().clone().multiply(0.3).add(direction.multiply(multiplier));
        player.setVelocity(newVel);
        player.setRiptiding(true);
    }

    /**
     * Utility to trigger the ability. Schedules the runnable instantly.
     */
    public static void activate(Player player) {
        new PowerSword(player).runTaskTimer(SurvivalSkills.getInstance(), 0L, 1L);
    }
}
