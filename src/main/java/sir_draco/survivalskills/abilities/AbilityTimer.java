package sir_draco.survivalskills.abilities;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class AbilityTimer extends BukkitRunnable {

    private final SurvivalSkills plugin;
    private final String name;
    private final Player p;

    private boolean active = true;
    private int activeTimeLeft;
    private int timeTillReset;
    private float flightSpeed;

    // Optional hooks so concrete abilities (e.g. Flight) can react to the
    // countdown without spawning a separate BukkitRunnable.
    private Consumer<Player> onExpire;
    private BiConsumer<Player, Integer> onActiveTick;

    public AbilityTimer(SurvivalSkills plugin, String name, Player p, int activeTimeLeft, int timeTillReset) {
        this.plugin = plugin;
        this.name = name;
        this.p = p;
        this.activeTimeLeft = activeTimeLeft;
        this.timeTillReset = timeTillReset;
    }

    @Override
    public void run() {
        if (plugin.getAbilityManager().getAbility(p, name) == null) {
            this.cancel();
            return;
        }

        // While the ability is active, count down the active window and fire
        // the optional per-second hook (only while time remains). The expire
        // hook is invoked at the exact organic transition to cooldown, so it
        // is never re-triggered by a manual endAbility() (which sets active=false).
        if (active) {
            activeTimeLeft--;
            if (activeTimeLeft > 0 && onActiveTick != null) onActiveTick.accept(p, activeTimeLeft);
            if (activeTimeLeft == 0) {
                if (onExpire != null) onExpire.accept(p);
                active = false;
                activeTimeLeft--;
            }
        } else {
            timeTillReset--;
        }

        if (!active && timeTillReset == 0) {
            if (p.isOnline()) {
                if (name.equals("XPVoucher")) {
                    p.sendRawMessage(ChatColor.RED + "Your XP Voucher has expired!");
                    p.playSound(p, Sound.ENTITY_SHEEP_SHEAR, 1, 1);
                }
                else {
                    p.sendRawMessage(ChatColor.RED + "Your " + name + " ability has reset!");
                    p.playSound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
                }
            }
            plugin.getAbilityManager().removeAbility(p, name);
            this.cancel();
        }
    }

    public String getName() {
        return name;
    }

    public int getTimeTillReset() {
        return timeTillReset;
    }

    public int getActiveTimeLeft() {
        return activeTimeLeft;
    }

    public boolean isActive() {
        return active;
    }

    public void endAbility() {
        activeTimeLeft = 0;
        active = false;
    }

    public void endCooldown() {
        timeTillReset = 0;
        this.cancel();
        plugin.getAbilityManager().removeAbility(p, name);
    }

    public void setFlightSpeed(float flightSpeed) {
        this.flightSpeed = flightSpeed;
    }

    public float getFlightSpeed() {
        return flightSpeed;
    }

    /** Runs when the active window organically reaches zero (player may be offline). */
    public void setOnExpire(Consumer<Player> onExpire) {
        this.onExpire = onExpire;
    }

    /** Runs once per second while active, supplying the seconds remaining (always > 0). */
    public void setOnActiveTick(BiConsumer<Player, Integer> onActiveTick) {
        this.onActiveTick = onActiveTick;
    }
}
