package sir_draco.survivalskills.GodQuestline.TrialMobs;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.Utils.ProjectileCalculator;

import java.util.HashMap;

public class FrostRevenant extends TrialBoss{

    private final int maxCooldown = 20 * 3;

    private int cooldown = maxCooldown;
    private int summonCooldown = 0;

    private Stray stray = null;
    private boolean firstSpawn = false;

    public FrostRevenant(HashMap<ItemStack, Double> drops) {
        super(ChatColor.LIGHT_PURPLE + "Frost Revenant", 200, 10, 0, 0.35,
                1.5, EntityType.STRAY, drops);
    }

    public FrostRevenant(HashMap<ItemStack, Double> drops, double healthMultiplier, double damageMultiplier) {
        super(ChatColor.LIGHT_PURPLE + "Frost Revenant", 200 * healthMultiplier, 10 * damageMultiplier,
                0, 0.35, 1.5, EntityType.STRAY, drops);
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
        if (!firstSpawn) {
            firstSpawn = true;
            spawnAngryWolves();
        }

        // Handle attacks
        if (cooldown > 0) cooldown--;
        else {
            cooldown = maxCooldown;
            attack();
        }

    }

    @Override
    public void attack() {
        double chance = Math.random();
        if (chance < 0.75) freezingSnowBall();
        else if (summonCooldown == 0) {
            spawnAngryWolves();
            summonCooldown = 20 * 20;
        }
    }

    @Override
    public void handleTypeSpecificSpawn() {
        this.stray = (Stray) getBoss();
    }

    public void freezingSnowBall() {
        new BukkitRunnable() {
            Snowball snowball = null;
            @Override
            public void run() {
                if (snowball == null) {
                    if (stray.getTarget() == null) return;
                    snowball = stray.launchProjectile(Snowball.class,
                            ProjectileCalculator.getNoGravityVector(stray.getEyeLocation(),
                                    stray.getTarget().getEyeLocation(), 1.5));
                }
            }
        }.runTaskTimer(SurvivalSkills.getPlugin(SurvivalSkills.class), 0, 1);
    }

    public void spawnAngryWolves() {
        if (getSpawnLocations() == null) return;
        for (int i = 0; i <= 4; i++) {
            Location randomSpawnLocation = getSpawnLocations().get((int) (Math.random() * getSpawnLocations().size()));
            if (randomSpawnLocation.getWorld() == null) continue;
            Wolf wolf = (Wolf) randomSpawnLocation.getWorld().spawnEntity(randomSpawnLocation, EntityType.WOLF);
            wolf.setMetadata("trialmob", new FixedMetadataValue(SurvivalSkills.getInstance(), true));
            wolf.setMetadata("spawned", new FixedMetadataValue(SurvivalSkills.getInstance(), true));
            wolf.setCustomName(ChatColor.RED + "Revenant Wolf");
            wolf.setCustomNameVisible(true);
            wolf.setAngry(true);
            if (stray != null)
                wolf.setTarget(stray.getTarget());
            getSummons().add(wolf);

            AttributeInstance healthAttribute = wolf.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            if (healthAttribute != null) healthAttribute.setBaseValue(10);
            wolf.setHealth(10);
        }
    }

    @Override
    public FrostRevenant duplicate(double healthMultiplier, double damageMultiplier) {
        return new FrostRevenant(getDrops(), healthMultiplier, damageMultiplier);
    }
}
