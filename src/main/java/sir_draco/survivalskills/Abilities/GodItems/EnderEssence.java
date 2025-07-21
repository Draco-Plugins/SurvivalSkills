package sir_draco.survivalskills.Abilities.GodItems;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;

public class EnderEssence extends BukkitRunnable {

    private final Player p;
    private final Location loc;

    public EnderEssence(Player p, Location loc) {
        this.p = p;
        this.loc = loc;
    }

    @Override
    public void run() {
        Location safeLocation = findSafeTeleportLocation(loc);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (p.getLocation().distance(safeLocation) > 6) {
                    p.sendRawMessage(ChatColor.RED + "Can't find a safe location in range!");
                    p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                }
                else p.teleport(safeLocation);
            }
        }.runTask(SurvivalSkills.getInstance());
    }

    public boolean isSafeTeleportLocation(Location location) {
        return !location.getBlock().getType().isSolid()
                && !location.getBlock().getRelative(0, 1, 0).getType().isSolid();
    }

    public Location findSafeTeleportLocation(Location location) {
        if (isSafeTeleportLocation(location)) return location;
        return findSafeTeleportLocation(location.add(0, 1, 0));
    }
}
