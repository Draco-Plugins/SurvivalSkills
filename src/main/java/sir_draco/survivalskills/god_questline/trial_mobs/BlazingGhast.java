package sir_draco.survivalskills.god_questline.trial_mobs;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.MagmaCube;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.ProjectileCalculator;

import java.util.HashMap;

public class BlazingGhast extends TrialBoss {

    private final int maxCooldown = 20 * 3;

    private int cooldown = maxCooldown;
    private int summonCooldown = 0;

    private Ghast ghast = null;

    public BlazingGhast(HashMap<ItemStack, Double> drops) {
        super("blazingGhast", ChatColor.RED + "Blazing Ghast", 100, 10, 0, 0.2, 1,
                EntityType.GHAST, drops);
    }

    public BlazingGhast(HashMap<ItemStack, Double> drops, double healthMultiplier, double damageMultiplier) {
        super("blazingGhast", ChatColor.RED + "Blazing Ghast", 100 * healthMultiplier, 10 * damageMultiplier,
                0, 0.2, 1, EntityType.GHAST, drops);
    }

    @Override
    public void run() {
        if (getBoss() == null || getBoss().isDead()) {
            cancel();
            return;
        }

        updateBossBar();
        manageBossBarPlayers();

        if (summonCooldown > 0) summonCooldown--;

        // Handle attacks
        if (cooldown > 0) cooldown--;
        else {
            cooldown = maxCooldown;
            attack();
        }

        checkTooFar();
    }

    @Override
    public void attack() {
        double chance = Math.random();
        if (chance < 0.2) perch();
        else if (chance < 0.5) blazeFireballs();
        else if (chance < 0.8) chargePlayer();
        else if (summonCooldown == 0) {
            spawnMagmaCubes();
            summonCooldown = 20 * 20;
        }
    }

    @Override
    public void handleTypeSpecificSpawn() {
        this.ghast = (Ghast) getBoss();
    }

    public void startScript() {
        this.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);
    }

    public void checkTooFar() {
        if (ghast == null) return;
        if (ghast.getTarget() == null) return;
        if (ghast.getLocation().distance(ghast.getTarget().getLocation()) > 30) {
            ghast.teleport(ghast.getTarget().getLocation().clone().add(0, 7, 0));
        }
    }

    public void perch() {
        if (ghast == null) return;
        ghast.getWorld().playSound(ghast.getLocation(), Sound.ENTITY_GHAST_WARN, 1, 1);
        if (ghast.getTarget() == null) return;
        ghast.teleport(ghast.getTarget().getLocation().clone().add(0, 1, 0));
        ghast.setAI(false);
        ghast.setVelocity(new Vector(0, 0, 0));
        new BukkitRunnable() {
            @Override
            public void run() {
                ghast.setAI(true);
            }
        }.runTaskLater(SurvivalSkills.getPlugin(SurvivalSkills.class), 20 * 3);
    }

    public void blazeFireballs() {
        new BukkitRunnable() {
            int fireballCount = 0;
            @Override
            public void run() {
                if (fireballCount >= 3) {
                    cancel();
                    return;
                }
                if (ghast.getTarget() == null) return;
                Location target = ghast.getTarget().getLocation().clone();
                Fireball fireball = ghast.getWorld().spawn(ghast.getLocation(), Fireball.class);
                fireball.setAcceleration(ProjectileCalculator.getNoGravityVector(ghast.getLocation(), target, 0.5));
                fireballCount++;
            }
        }.runTaskTimer(SurvivalSkills.getPlugin(SurvivalSkills.class), 0, 20);
    }

    public void chargePlayer() {
        if (ghast == null) return;
        ghast.setCharging(true);
    }

    public void spawnMagmaCubes() {
        if (getSpawnLocations() == null) return;
        for (int i = 0; i <= 4; i++) {
            Location randomSpawnLocation = getSpawnLocations().get((int) (Math.random() * getSpawnLocations().size()));
            if (randomSpawnLocation.getWorld() == null) continue;
            MagmaCube cube = (MagmaCube) randomSpawnLocation.getWorld().spawnEntity(randomSpawnLocation, EntityType.MAGMA_CUBE);
            cube.setMetadata("trialmob", new FixedMetadataValue(SurvivalSkills.getInstance(), true));
            cube.setMetadata("spawned", new FixedMetadataValue(SurvivalSkills.getInstance(), true));
            cube.setSize(3);
            cube.setCustomName(ChatColor.RED + "Blazing Magma Cube");
            cube.setCustomNameVisible(true);
            getSummons().add(cube);

            AttributeInstance healthAttribute = cube.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            if (healthAttribute != null) healthAttribute.setBaseValue(10);
            cube.setHealth(10);
        }
    }

    public BlazingGhast duplicate(double healthMultiplier, double damageMultiplier) {
        return new BlazingGhast(getDrops(), healthMultiplier, damageMultiplier);
    }

    public BlazingGhast duplicate() {
        return new BlazingGhast(getDrops());
    }
}
