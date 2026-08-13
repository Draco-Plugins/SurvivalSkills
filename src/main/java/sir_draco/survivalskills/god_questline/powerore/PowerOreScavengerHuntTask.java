package sir_draco.survivalskills.god_questline.powerore;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Scavenger hunt: player must find 3 heads within radius and right-click them.
 */
public class PowerOreScavengerHuntTask extends BukkitRunnable implements PowerOreTask {

    private static final int HEAD_COUNT = 3;
    private static final int SPAWN_RADIUS = 50;
    private static final int MAX_SPAWN_ATTEMPTS = 500;
    private static final int ANNOUNCE_INTERVAL_SECONDS = 60;
    private static final int FINAL_COUNTDOWN_SECONDS = 10;
    private static final long DURATION_MS = 300_000L;

    private final PowerOreChallenge challenge;
    private final Player player;
    private final Location origin;
    private final List<Location> headLocations = new ArrayList<>();
    private final Set<Location> found = new HashSet<>();
    private final long startTime = System.currentTimeMillis();

    public PowerOreScavengerHuntTask(PowerOreChallenge challenge, Player player, Location origin) {
        this.challenge = challenge;
        this.player = player;
        this.origin = origin.clone();
    }

    @Override
    public void start() {
        spawnHeads();
        if (headLocations.isEmpty()) {
            challenge.fail("Failed to generate head locations");
            return;
        }
        player.sendMessage(ChatColor.AQUA
                + "Scavenger Hunt: " + HEAD_COUNT + " of your head were spawned on the surface in a "
                + SPAWN_RADIUS + " block radius. Find and right-click all " + HEAD_COUNT
                + " heads! You have " + (DURATION_MS / 60000) + " minutes.");
        runTaskTimer(SurvivalSkills.getInstance(), 20, 20);
    }

    private void spawnHeads() {
        World world = origin.getWorld();
        if (world == null)
            return;
        int attempts = 0;
        while (headLocations.size() < HEAD_COUNT && attempts < MAX_SPAWN_ATTEMPTS) {
            attempts++;
            // Randomize location within the spawn radius
            int dx = ThreadLocalRandom.current().nextInt(-SPAWN_RADIUS, SPAWN_RADIUS + 1);
            int dz = ThreadLocalRandom.current().nextInt(-SPAWN_RADIUS, SPAWN_RADIUS + 1);

            if (Math.sqrt(dx * dx + dz * dz) > SPAWN_RADIUS)
                continue;

            int x = origin.getBlockX() + dx;
            int z = origin.getBlockZ() + dz;

            // Get a surface block
            Block top = world.getHighestBlockAt(x, z);
            if (top.getType().isAir())
                continue;
            Block place = top.getRelative(0, 1, 0);
            if (!place.getType().isAir())
                continue;

            // Place the player's head
            Location loc = place.getLocation();
            place.setType(Material.PLAYER_HEAD);
            if (place.getState() instanceof Skull skull) {
                PlayerProfile profile = player.getPlayerProfile();
                skull.setOwnerProfile(profile);
                skull.update();
            }
            headLocations.add(loc);
        }
    }

    public boolean handleInteract(Block block) {
        if (challenge.getStatus() != PowerOreChallenge.Status.RUNNING)
            return false;
        for (Location head : headLocations) {
            if (!head.getBlock().equals(block))
                continue;
            if (found.contains(head))
                return true;
            found.add(head);
            block.setType(Material.AIR);
            player.playSound(head, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            player.sendMessage(ChatColor.GREEN + "Head found (" + found.size() + "/" + HEAD_COUNT + ")");
            if (found.size() == HEAD_COUNT)
                challenge.complete();
            return true;
        }
        return false;
    }

    @Override
    public void run() {
        if (challenge.getStatus() != PowerOreChallenge.Status.RUNNING) {
            cancel();
            return;
        }
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= DURATION_MS) {
            challenge.fail("Time ran out");
            cancel();
            return;
        }
        long remainingSeconds = (DURATION_MS - elapsed) / 1000;
        if (remainingSeconds % ANNOUNCE_INTERVAL_SECONDS == 0 || remainingSeconds <= FINAL_COUNTDOWN_SECONDS)
            player.sendMessage(ChatColor.YELLOW + "Time left: " + remainingSeconds + "s");
    }

    @Override
    public void cleanup() {
        try {
            cancel();
        } catch (IllegalStateException ignored) {
            // Task was not scheduled or already cancelled
        }
        for (Location loc : headLocations) {
            if (found.contains(loc))
                continue;
            World world = loc.getWorld();
            if (world == null)
                continue;
            if (!world.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4))
                continue;
            if (loc.getBlock().getType() == Material.PLAYER_HEAD)
                loc.getBlock().setType(Material.AIR);
        }
    }

    @Override
    public String name() {
        return "Scavenger Hunt";
    }

    @Override
    public Player getPlayer() {
        return player;
    }
}
