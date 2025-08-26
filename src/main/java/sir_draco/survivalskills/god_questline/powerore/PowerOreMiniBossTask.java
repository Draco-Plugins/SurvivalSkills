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

    private static final int MAX_HEALTH = 250;
    private static final int ATTACK_DAMAGE = 30;
    private static final float SPEED = 0.4f;

    private final PowerOreChallenge challenge;
    private final Player player;
    private final Location oreLoc;
    private IronGolem golem;
    private BukkitRunnable shooter;
    private final double maxDistance = 60d; // configurable later

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
        golem = (IronGolem) world.spawnEntity(oreLoc.clone().add(0.5, 1, 0.5), EntityType.IRON_GOLEM);
        golem.setCustomName(ChatColor.DARK_RED + "Power Ore Sentinel");
        golem.setCustomNameVisible(true);
        handleAttributes();
        golem.setPlayerCreated(false);
        golem.setTarget(player);
        shooter = new BukkitRunnable() {
            @Override
            public void run() {
                if (golem == null || golem.isDead() || challenge.getStatus() != PowerOreChallenge.Status.RUNNING) {
                    cancel();
                    return;
                }
                if (!player.isOnline())
                    return;
                launchIngot();
            }
        };
        shooter.runTaskTimer(SurvivalSkills.getInstance(), 40, 60);
        runTaskTimer(SurvivalSkills.getInstance(), 0, 20);
        player.sendMessage(ChatColor.RED + "Defeat the Power Ore Sentinel to complete the conversion!");
    }

    private void launchIngot() {
        if (golem == null)
            return;
        World world = golem.getWorld();
        Location hand = golem.getLocation().clone().add(0, 2.2, 0);
        Item ingot = world.dropItem(hand, new ItemStack(Material.IRON_INGOT));
        ingot.setPickupDelay(Integer.MAX_VALUE);
        Vector dir = player.getLocation().toVector().subtract(hand.toVector()).normalize();
        ingot.setVelocity(dir.multiply(1.2));
        new BukkitRunnable() {
            int life = 40;

            @Override
            public void run() {
                if (life-- <= 0 || ingot.isDead()) {
                    ingot.remove();
                    cancel();
                    return;
                }
                if (player.getWorld().equals(ingot.getWorld())
                        && player.getLocation().distanceSquared(ingot.getLocation()) < 1.2) {
                    player.damage(18, golem);
                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 1, 1);
                    ingot.remove();
                    cancel();
                }
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), 1, 2);
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
                || player.getLocation().distanceSquared(golem.getLocation()) > maxDistance * maxDistance) {
            challenge.fail("You abandoned the Sentinel.");
            cancel();
        }
    }

    @Override
    public void cleanup() {
        try {
            if (shooter != null)
                shooter.cancel();
        } catch (Exception ignored) {
        }
        try {
            cancel();
        } catch (Exception ignored) {
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

    public void handleAttributes() {
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
