package sir_draco.survivalskills.Abilities;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class FlyingTimer extends BukkitRunnable {

    private final Player p;
    private int timeLeft;

    public FlyingTimer(Player p, int totalTime) {
        this.p = p;
        this.timeLeft = totalTime;
    }

    @Override
    public void run() {
        timeLeft--;
        if (timeLeft == 0) removeFlight(p);
        else if (timeLeft == 60) {
            p.sendRawMessage(ChatColor.YELLOW + "You have 1 minute left of flight time!");
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1);
        }
        else if (timeLeft == 30) {
            p.sendRawMessage(ChatColor.YELLOW + "You have 30 seconds left of flight time!");
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 0);
        }
        else if (timeLeft == 10) {
            p.sendRawMessage(ChatColor.YELLOW + "You have 10 seconds left of flight time!");
        }
        else if (timeLeft == 3) {
            p.playSound(p, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1, 1);
        }
        else if (timeLeft == 2) {
            p.playSound(p, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1, 1);
        }
        else if (timeLeft == 1) {
            p.playSound(p, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1, 1);
        }
    }

    public void removeFlight(Player p) {
        if (!p.isOnline()) {
            cancel();
            return;
        }

        p.setAllowFlight(false);
        p.setFlying(false);
        p.sendRawMessage(ChatColor.YELLOW + "Your flight time has expired!");
        p.playSound(p, Sound.ENTITY_SHEEP_SHEAR, 1, 1);
        cancel();
    }
}
