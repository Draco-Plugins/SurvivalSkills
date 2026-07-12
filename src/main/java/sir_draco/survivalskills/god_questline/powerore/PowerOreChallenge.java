package sir_draco.survivalskills.god_questline.powerore;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.items.ItemStackGenerator;

import java.util.Random;
import java.util.UUID;

/**
 * Handles a single Power Ore conversion attempt using a randomly selected task.
 */
public class PowerOreChallenge {

    public enum Status {
        RUNNING, SUCCESS, FAILED
    }

    /** Future extension: add weights or conditions per task type here */
    public enum TaskType {
        MINI_BOSS, SCAVENGER_HUNT, SIMON_SAYS
    }

    private final Location oreLocation; // Obsidian block location
    private final Player player;
    private final UUID uuid;
    private final PowerOreTask task;
    private Status status = Status.RUNNING;
    private BukkitRunnable visualTask;
    private int tick = 0;
    private boolean rewardDropped = false;

    public PowerOreChallenge(Location oreLocation, Player player, TaskType type) {
        this.oreLocation = oreLocation;
        this.player = player;
        this.uuid = player.getUniqueId();
        this.task = switch (type) {
            case MINI_BOSS -> new PowerOreMiniBossTask(this, player, oreLocation);
            case SCAVENGER_HUNT -> new PowerOreScavengerHuntTask(this, player, oreLocation);
            case SIMON_SAYS -> new PowerOreSimonSaysTask(this, player, oreLocation);
        };
    }

    /**
     * Only used if the conversion is already completed
     * 
     * @param oreLocation
     * @param uuid
     */
    public PowerOreChallenge(Location oreLocation, UUID uuid) {
        this.oreLocation = oreLocation;
        this.uuid = uuid;
        this.player = null;
        this.task = null;
        status = Status.SUCCESS;

        startVisuals();
    }

    public void start() {
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Power Ore Challenge started: " + ChatColor.GOLD + task.name());
        startVisuals();
        task.start();
    }

    public void complete() {
        if (status != Status.RUNNING)
            return;
        status = Status.SUCCESS;
        task.cleanup();
        player.sendMessage(ChatColor.GREEN + "Your Power Ore is now charged! Mine it to claim the ore.");
    }

    public void fail(String reason) {
        if (status != Status.RUNNING)
            return;
        status = Status.FAILED;
        task.cleanup();
        if (reason != null && !reason.isBlank())
            player.sendMessage(ChatColor.RED + "Challenge failed: " + reason);
        else
            player.sendMessage(ChatColor.RED + "Power Ore Challenge failed.");
        player.playSound(player, Sound.BLOCK_GLASS_BREAK, 1, 0.6f);
        stopVisuals();
        SurvivalSkills.getInstance().getGodListener().removeOreConversion(getUniqueId(), getOreLocation());
    }

    public void reward() {
        if (rewardDropped)
            return;
        World world = oreLocation.getWorld();
        if (world == null)
            return;
        world.dropItem(oreLocation.clone().add(0.5, 0.5, 0.5), ItemStackGenerator.getPowerOre());
        world.playSound(oreLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1, 1);
        player.sendMessage(ChatColor.GREEN + "You successfully converted the Power Ore!");
        rewardDropped = true;
        stopVisuals();
    }

    private void startVisuals() {
        stopVisuals();
        visualTask = new BukkitRunnable() {
            @Override
            public void run() {
                World world = oreLocation.getWorld();
                if (world == null) {
                    cancel();
                    return;
                }
                if (status == Status.FAILED) {
                    cancel();
                    return;
                }
                if (oreLocation.getBlock().getType() != Material.OBSIDIAN) {
                    cancel();
                    return;
                }
                tick++;
                double angle = (tick % 40) / 40.0 * 2 * Math.PI;
                double x = oreLocation.getX() + 0.5 + Math.cos(angle);
                double z = oreLocation.getZ() + 0.5 + Math.sin(angle);
                double y = oreLocation.getY() + 1.1;
                if (status == Status.RUNNING) {
                    world.spawnParticle(Particle.DUST, x, y, z, 1, new Particle.DustOptions(Color.RED, 1));
                } else if (status == Status.SUCCESS) {
                    world.spawnParticle(Particle.DUST, x, y, z, 1, new Particle.DustOptions(Color.GREEN, 1));
                    // Upward electric spark occasionally
                    if (tick % 5 == 0) {
                        world.spawnParticle(Particle.ELECTRIC_SPARK, oreLocation.getX() + 0.5, oreLocation.getY() + 1.2,
                                oreLocation.getZ() + 0.5, 3, 0.1, 0.2, 0.1, 0.01);
                    }
                }
            }
        };
        visualTask.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);
    }

    private void stopVisuals() {
        if (visualTask != null) {
            try {
                visualTask.cancel();
            } catch (Exception ignored) {
            }
        }
    }

    public Location getOreLocation() {
        return oreLocation;
    }

    public Player getPlayer() {
        return player;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public Status getStatus() {
        return status;
    }

    public PowerOreTask getTask() {
        return task;
    }

    public static TaskType randomTask(Random random) {
        TaskType[] values = TaskType.values();
        return values[random.nextInt(values.length)];
    }

    public boolean isRewardDropped() {
        return rewardDropped;
    }
}
