package sir_draco.survivalskills.GodQuestline;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.Trophy.ParticleColumnThread;
import sir_draco.survivalskills.Utils.ItemStackGenerator;

import java.util.UUID;

public class PowerOreConversion extends BukkitRunnable {

    private final Location loc;
    private final UUID uuid;

    private int secondsLeft;
    private int cycle = 20;
    private boolean isComplete = false;
    private boolean display = true;

    public PowerOreConversion(int secondsLeft, Location loc, UUID uuid) {
        this.secondsLeft = secondsLeft;
        this.loc = loc;
        this.uuid = uuid;
    }

    @Override
    public void run() {
        if (cycle <= 1) {
            if (secondsLeft > 0) secondsLeft--;
            else isComplete = true;
            cycle = 20;
            checkForPlayers();
        }
        else cycle--;

        if (!isComplete && display) powerOreParticles();
        if (isComplete && display) finishedParticles();
    }

    public void powerOreParticles() {
        // Display purple particles based on how many hours are left
        int hoursLeft = secondsLeft / 3600;
        double chanceNeeded = (Math.pow(2, ((double) 2 / hoursLeft + 1))) * ((double) 1 / 20);

        World world = loc.getWorld();
        if (world == null) return;

        // Randomly spawn a new particle column
        if (Math.random() < chanceNeeded) spawnParticleColumn();
    }

    public void spawnParticleColumn() {
        ParticleColumnThread column = new ParticleColumnThread(loc.clone().add(Math.random(), Math.random() * 0.75 + 1.1, Math.random()),
                new Particle.DustOptions(Color.PURPLE, 1), 1.5, 0.1);
        column.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);
    }

    public void finishedParticles() {
        World world = loc.getWorld();
        if (world == null) return;

        // Display a green ring of particles
        double i = ((double) cycle / 20) * (2 * Math.PI);
        double x = Math.cos(i);
        double z = Math.sin(i);
        world.spawnParticle(Particle.DUST, loc.getX() + x + 0.5, loc.getY() + 1.1, loc.getZ() + z + 0.5, 1, new Particle.DustOptions(Color.GREEN, 1));
    }

    public void breakOre() {
        this.cancel();
        World world = loc.getWorld();
        if (world == null) return;
        world.dropItem(loc.clone().add(0.5, 0.5, 0.5), ItemStackGenerator.getPowerOre());
        world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1, 1);

        // Spawn particles nearby
        for (int i = 0; i < 20; i++) {
            double x = (Math.random() - 0.5) * 2.5;
            double y = Math.random() * 1.5;
            double z = (Math.random() - 0.5) * 2.5;
            world.spawnParticle(Particle.HAPPY_VILLAGER, loc.getX() + x + 0.5, loc.getY() + y + 1, loc.getZ() + z + 0.5, 1, 0, 0, 0, 1);
        }
    }

    public void checkForPlayers() {
        // If no players are online, stop the ore effects
        if (SurvivalSkills.getInstance().getServer().getOnlinePlayers().isEmpty()) {
            if (display) display = false;
            return;
        }

        for (Player p : SurvivalSkills.getInstance().getServer().getOnlinePlayers()) {
            // If a player is within 50 blocks of the ore, ensure particles are showing
            if (!p.getWorld().equals(loc.getWorld())) continue;
            if (p.getLocation().distance(loc) > 50) continue;
            if (!display) display = true;
            // If the player is withing 50 blocks and the ore particles are showing, do nothing
            return;
        }

        // If no players are within 50 blocks of the ore, and the particles are showing, stop it
        if (display) display = false;
    }

    public int getSecondsLeft() {
        return secondsLeft;
    }

    public Location getLocation() {
        return loc;
    }

    public UUID getUUID() {
        return uuid;
    }
}
