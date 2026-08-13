package sir_draco.survivalskills.god_questline.powerore;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.items.ItemStackGenerator;

import java.util.Random;
import java.util.UUID;

/**
 * Handles a single Power Ore conversion attempt using a randomly selected task.
 */
public final class PowerOreChallenge implements PowerOreChallengeHandle {

    private static final TaskType[] TASK_TYPES = TaskType.values();
    private static final double BLOCK_CENTER_OFFSET = 0.5;

    public enum Status {
        RUNNING, SUCCESS, FAILED
    }

    /** Future extension: add weights or conditions per task type here */
    public enum TaskType {
        MINI_BOSS, SCAVENGER_HUNT, SIMON_SAYS, MEMORY_MATCH, CHICKEN_HERDING
    }

    private final Location oreLocation; // Obsidian block location
    private final Player player;
    private final UUID uuid;
    private final PowerOreTask task;
    private final boolean testOnly;
    private Status status = Status.RUNNING;
    private PowerOreVisualEffect visualEffect;
    private boolean rewardDropped = false;

    public PowerOreChallenge(Location oreLocation, Player player, TaskType type) {
        this(oreLocation, player, type, false);
    }

    private PowerOreChallenge(Location oreLocation, Player player, TaskType type, boolean testOnly) {
        this.oreLocation = oreLocation;
        this.player = player;
        this.uuid = player.getUniqueId();
        this.testOnly = testOnly;
        this.task = switch (type) {
            case MINI_BOSS -> new PowerOreMiniBossTask(this, player, oreLocation);
            case SCAVENGER_HUNT -> new PowerOreScavengerHuntTask(this, player, oreLocation);
            case SIMON_SAYS -> new PowerOreSimonSaysTask(this, player);
            case MEMORY_MATCH -> new PowerOreMemoryMatchTask(this, player);
            case CHICKEN_HERDING -> new PowerOreChickenHerdingTask(this, player, oreLocation);
        };
    }

    /** Creates a mini-boss challenge for admin testing without starting a conversion. */
    public static PowerOreChallenge forMiniBossTest(Location location, Player player) {
        return new PowerOreChallenge(location, player, TaskType.MINI_BOSS, true);
    }

    public void start() {
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Power Ore Challenge started: " + ChatColor.GOLD + task.name());
        if (!testOnly)
            startVisuals();
        task.start();
    }

    public void complete() {
        if (status != Status.RUNNING)
            return;
        status = Status.SUCCESS;
        task.cleanup();
        if (testOnly)
            player.sendMessage(ChatColor.GREEN + "Power Ore Sentinel defeated!");
        else
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
        if (!testOnly) {
            stopVisuals();
            SurvivalSkills.getInstance().getGodListener().removeOreConversion(uuid, oreLocation);
        }
    }

    @Override
    public void reward(Player player) {
        if (rewardDropped)
            return;
        World world = oreLocation.getWorld();
        if (world == null)
            return;
        world.dropItem(oreLocation.clone().add(BLOCK_CENTER_OFFSET, BLOCK_CENTER_OFFSET, BLOCK_CENTER_OFFSET),
                ItemStackGenerator.getPowerOre());
        world.playSound(oreLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1, 1);
        player.sendMessage(ChatColor.GREEN + "You successfully converted the Power Ore!");
        rewardDropped = true;
        stopVisuals();
    }

    @Override
    public void startVisuals() {
        stopVisuals();
        visualEffect = new PowerOreVisualEffect(oreLocation, this::getStatus);
        visualEffect.start();
    }

    @Override
    public void stopVisuals() {
        if (visualEffect != null) {
            visualEffect.stop();
            visualEffect = null;
        }
    }

    @Override
    public Location getOreLocation() {
        return oreLocation;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public UUID getUniqueId() {
        return uuid;
    }

    @Override
    public Status getStatus() {
        return status;
    }

    public PowerOreTask getTask() {
        return task;
    }

    public static TaskType randomTask(Random random) {
        return TASK_TYPES[random.nextInt(TASK_TYPES.length)];
    }

    @Override
    public boolean isRewardDropped() {
        return rewardDropped;
    }
}
