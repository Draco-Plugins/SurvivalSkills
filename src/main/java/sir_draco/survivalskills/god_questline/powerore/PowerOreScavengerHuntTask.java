package sir_draco.survivalskills.god_questline.powerore;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.*;

/**
 * Scavenger hunt: player must find 3 heads within radius and right-click them.
 */
public class PowerOreScavengerHuntTask extends BukkitRunnable implements PowerOreTask {

    private final PowerOreChallenge challenge;
    private final Player player;
    private final Location origin;
    private final List<Location> headLocations = new ArrayList<>();
    private final Set<Location> found = new HashSet<>();
    private int secondsLeft = 300; // 5 minutes
    private final Random random = new Random();

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
                + "Scavenger Hunt: 3 of your head were spawned on the surface in a 150 block radius. Find and right-click all 3 heads! You have 5 minutes.");
        runTaskTimer(SurvivalSkills.getInstance(), 20, 20);
    }

    private void spawnHeads() {
        World world = origin.getWorld();
        if (world == null)
            return;
        int attempts = 0;
        while (headLocations.size() < 3 && attempts < 500) {
            attempts++;
            // Randomize location within a 150 block radius
            int dx = random.nextInt(301) - 150;
            int dz = random.nextInt(301) - 150;

            if (Math.sqrt(dx * dx + dz * dz) > 150)
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
            player.sendMessage(ChatColor.AQUA + "Head spawned at: " + loc);
        }
    }

    public boolean handleInteract(Block block) {
        if (challenge.getStatus() != PowerOreChallenge.Status.RUNNING)
            return false;
        Location loc = block.getLocation();
        for (Location head : headLocations) {
            if (head.getWorld().equals(loc.getWorld()) && head.distanceSquared(loc) < 0.1) {
                if (found.contains(head))
                    return true;
                found.add(head);
                block.setType(Material.AIR);
                player.playSound(head, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                player.sendMessage(ChatColor.GREEN + "Head found (" + found.size() + "/3)");
                if (found.size() == 3)
                    challenge.complete();
                return true;
            }
        }
        return false;
    }

    @Override
    public void run() {
        if (challenge.getStatus() != PowerOreChallenge.Status.RUNNING) {
            cancel();
            return;
        }
        if (secondsLeft-- <= 0) {
            challenge.fail("Time ran out");
            cancel();
            return;
        }
        if (secondsLeft % 60 == 0 || secondsLeft <= 10)
            player.sendMessage(ChatColor.YELLOW + "Time left: " + secondsLeft + "s");
    }

    @Override
    public void cleanup() {
        try {
            cancel();
        } catch (Exception ignored) {
        }
        for (Location loc : headLocations)
            if (!found.contains(loc) && loc.getBlock().getType() == Material.PLAYER_HEAD)
                loc.getBlock().setType(Material.AIR);
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
