package sir_draco.survivalskills.god_questline.powerore;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.SurvivalSkills;

/** Mini boss task: spawn a powerful Iron Golem the player must defeat. */
public class PowerOreMiniBossTask extends BukkitRunnable implements PowerOreTask {

    // Entity stats
    private static final int MAX_HEALTH = 250;
    private static final int ATTACK_DAMAGE = 75;
    private static final float SPEED = 0.6f;
    private static final double MAX_DISTANCE = 60.0;

    // Spawn offsets
    private static final double SPAWN_OFFSET_X = 0.5;
    private static final double SPAWN_OFFSET_Y = 1.0;
    private static final double SPAWN_OFFSET_Z = 0.5;

    // Shooter task timing (ticks)
    private static final int SHOOTER_INITIAL_DELAY = 40;
    private static final int SHOOTER_PERIOD = 60;
    private static final int MAIN_TASK_PERIOD = 20;

    // Projectile properties
    private static final double HAND_OFFSET_Y = 2.2;
    private static final double PROJECTILE_SPEED = 1.2;
    private static final int PROJECTILE_LIFETIME = 40;
    private static final double COLLISION_RADIUS_SQ = 1.2;
    private static final int PROJECTILE_DAMAGE = 75;

    // Projectile tracker timing (ticks)
    private static final int TRACKER_DELAY = 1;
    private static final int TRACKER_PERIOD = 2;

    private final PowerOreChallenge challenge;
    private final Player player;
    private final Location oreLoc;
    private IronGolem golem;
    private BukkitRunnable shooter;

    public PowerOreMiniBossTask(PowerOreChallenge challenge, Player player, Location oreLoc) {
        this.challenge = challenge;
        this.player = player;
        this.oreLoc = oreLoc.clone();
    }

    @Override
    public void start() {
        World world = oreLoc.getWorld();
        if (world == null) {
            challenge.fail("World unloaded");
            return;
        }
        Location spawnLoc = oreLoc.clone().add(SPAWN_OFFSET_X, SPAWN_OFFSET_Y, SPAWN_OFFSET_Z);
        golem = (IronGolem) world.spawnEntity(spawnLoc, EntityType.IRON_GOLEM);
        if (golem == null) {
            challenge.fail("Failed to spawn the Sentinel");
            return;
        }
        golem.setCustomName(ChatColor.DARK_RED + "Power Ore Sentinel");
        golem.setCustomNameVisible(true);
        handleAttributes();
        golem.setPlayerCreated(false);
        golem.setTarget(player);
        shooter = new ShooterTask();
        shooter.runTaskTimer(SurvivalSkills.getInstance(), SHOOTER_INITIAL_DELAY, SHOOTER_PERIOD);
        runTaskTimer(SurvivalSkills.getInstance(), 0, MAIN_TASK_PERIOD);
        player.sendMessage(ChatColor.RED + "Defeat the Power Ore Sentinel to complete the conversion!");
    }

    /** @return true if the challenge is still running and the golem is alive */
    private boolean isActive() {
        return challenge.getStatus() == PowerOreChallenge.Status.RUNNING
                && golem != null && !golem.isDead();
    }

    /** Periodically launches iron ingot projectiles at the player. */
    private class ShooterTask extends BukkitRunnable {
        @Override
        public void run() {
            if (!isActive()) {
                cancel();
                return;
            }
            if (!player.isOnline()) {
                cancel();
                return;
            }
            launchIngot();
        }
    }

    private void launchIngot() {
        if (golem == null)
            return;
        World world = golem.getWorld();
        Location hand = golem.getLocation().clone().add(0, HAND_OFFSET_Y, 0);
        Item ingot = world.dropItem(hand, new ItemStack(Material.IRON_INGOT));
        ingot.setPickupDelay(Integer.MAX_VALUE);
        Vector dir = player.getLocation().toVector().subtract(hand.toVector()).normalize();
        ingot.setVelocity(dir.multiply(PROJECTILE_SPEED));
        new ProjectileTracker(ingot).runTaskTimer(SurvivalSkills.getInstance(), TRACKER_DELAY, TRACKER_PERIOD);
    }

    /** Tracks a single projectile and damages the player on collision. */
    private class ProjectileTracker extends BukkitRunnable {
        private final Item ingot;
        private int life = PROJECTILE_LIFETIME;

        ProjectileTracker(Item ingot) {
            this.ingot = ingot;
        }

        @Override
        public void run() {
            if (life-- <= 0 || ingot.isDead()) {
                ingot.remove();
                cancel();
                return;
            }
            if (player.getWorld().equals(ingot.getWorld())
                    && player.getLocation().distanceSquared(ingot.getLocation()) < COLLISION_RADIUS_SQ) {
                player.damage(PROJECTILE_DAMAGE, golem);
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 1);
                ingot.remove();
                cancel();
            }
        }
    }

    @Override
    public void run() {
        if (challenge.getStatus() != PowerOreChallenge.Status.RUNNING) {
            cancel();
            return;
        }
        if (golem == null || golem.isDead()) {
            challenge.complete();
            cancel();
            return;
        }
        if (!player.getWorld().equals(golem.getWorld())
                || player.getLocation().distanceSquared(golem.getLocation()) > MAX_DISTANCE * MAX_DISTANCE) {
            challenge.fail("You abandoned the Sentinel.");
            cancel();
        }
    }

    @Override
    public void cleanup() {
        try {
            if (shooter != null)
                shooter.cancel();
        } catch (IllegalStateException ignored) {
            // Task was already cancelled or not scheduled
        }
        try {
            cancel();
        } catch (IllegalStateException ignored) {
            // Task was already cancelled or not scheduled
        }
        if (golem != null && !golem.isDead())
            golem.remove();
    }

    @Override
    public String name() {
        return "Ore Guardian";
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    private void handleAttributes() {
        AttributeInstance health = golem.getAttribute(Attribute.MAX_HEALTH);
        if (health != null)
            health.setBaseValue(MAX_HEALTH);
        golem.setHealth(MAX_HEALTH);
        AttributeInstance attack = golem.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attack != null)
            attack.setBaseValue(ATTACK_DAMAGE);
        AttributeInstance speed = golem.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null)
            speed.setBaseValue(SPEED);
    }
}
