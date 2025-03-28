package sir_draco.survivalskills.GodQuestline.TrialMobs;

import org.bukkit.*;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.GodQuestline.TrialManager;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.Utils.ColorParser;

import java.util.HashMap;
import java.util.Random;

public class HellsGatekeeper extends TrialBoss {

    private final int maxCooldown = 20 * 3;

    private int cooldown = maxCooldown;
    private int summonCooldown = 0;

    private WitherSkeleton witherSkeleton = null;

    public HellsGatekeeper(HashMap<ItemStack, Double> drops) {
        super(ColorParser.colorizeString("Hell's Gatekeeper",
                ColorParser.generateGradient("#EC6000", "#FB0808", 17), true),
                166, 10, 0, 0.3, 2, EntityType.WITHER_SKELETON, drops);
    }

    public HellsGatekeeper(HashMap<ItemStack, Double> drops, double healthMultiplier, double damageMultiplier) {
        super(ColorParser.colorizeString("Hell's Gatekeeper",
                ColorParser.generateGradient("#EC6000", "#FB0808", 17), true),
                166 * healthMultiplier, 10 * damageMultiplier, 0, 0.3, 2, EntityType.WITHER_SKELETON, drops);
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
    }

    @Override
    public void attack() {
        double chance = Math.random();
        if (chance < 0.5) fireStorm();
        else if (summonCooldown == 0) {
            if (chance < 0.65) spawnMagmaCubes();
            else if (chance < 0.85) spawnBlazes();
            else spawnPiglinBrutes();
            summonCooldown = 20 * 20;
        }
    }

    @Override
    public void handleTypeSpecificSpawn() {
        this.witherSkeleton = (WitherSkeleton) getBoss();
        ItemStack[] armor = new ItemStack[4];
        armor[0] = null;
        armor[1] = null;
        armor[2] = TrialManager.getTrialItem(Material.NETHERITE_CHESTPLATE, 1);
        armor[3] = null;
        if (witherSkeleton.getEquipment() != null)
            witherSkeleton.getEquipment().setArmorContents(armor);
    }

    public void fireStorm() {
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks >= 20 * 5) {
                    cancel();
                    return;
                }

                Random random = new Random();
                Location bossLocation = witherSkeleton.getLocation();
                World world = bossLocation.getWorld();
                if (world == null) {
                    cancel();
                    return;
                }

                // Generate fire particles and deal damage
                for (int i = 0; i < 10; i++) { // Spawn 10 particle clusters per tick
                    double angle = random.nextDouble() * 2 * Math.PI;
                    double distance = random.nextDouble() * 3;
                    double x = bossLocation.getX() + distance * Math.cos(angle);
                    double z = bossLocation.getZ() + distance * Math.sin(angle);
                    double y = bossLocation.getY() + random.nextDouble() * 5; // Slightly above the ground

                    Location particleLocation = new Location(world, x, y, z);

                    // Display fire particles
                    world.spawnParticle(Particle.FLAME, particleLocation, 20, 0.5, 0.5, 0.5, 0.01);

                    // Damage players near the particle location
                    for (Player player : world.getPlayers())
                        if (player.getLocation().distance(particleLocation) <= 2) {
                            player.damage(4);
                            player.setFireTicks(40);
                        }
                }
                ticks++;
            }
        }.runTaskTimer(SurvivalSkills.getPlugin(SurvivalSkills.class), 0, 1);
    }

    public void spawnMagmaCubes() {
        if (getSpawnLocations() == null) return;
        for (int i = 0; i <= 4; i++) {
            Location randomSpawnLocation = getSpawnLocations().get((int) (Math.random() * getSpawnLocations().size()));
            if (randomSpawnLocation.getWorld() == null) continue;
            MagmaCube magmaCube = (MagmaCube) randomSpawnLocation.getWorld().spawnEntity(randomSpawnLocation, EntityType.MAGMA_CUBE);
            magmaCube.setMetadata("trialmob", new FixedMetadataValue(SurvivalSkills.getInstance(), true));
            magmaCube.setMetadata("spawned", new FixedMetadataValue(SurvivalSkills.getInstance(), true));
            magmaCube.setCustomName(ChatColor.RED + "Hell's Cube");
            magmaCube.setCustomNameVisible(true);
            magmaCube.setSize(3);
            getSummons().add(magmaCube);

            AttributeInstance healthAttribute = magmaCube.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            if (healthAttribute != null) healthAttribute.setBaseValue(10);
            magmaCube.setHealth(10);
        }
    }

    public void spawnBlazes() {
        if (getSpawnLocations() == null) return;
        for (int i = 0; i <= 4; i++) {
            Location randomSpawnLocation = getSpawnLocations().get((int) (Math.random() * getSpawnLocations().size()));
            if (randomSpawnLocation.getWorld() == null) continue;
            Blaze blaze = (Blaze) randomSpawnLocation.getWorld().spawnEntity(randomSpawnLocation, EntityType.BLAZE);
            blaze.setMetadata("trialmob", new FixedMetadataValue(SurvivalSkills.getInstance(), true));
            blaze.setMetadata("spawned", new FixedMetadataValue(SurvivalSkills.getInstance(), true));
            blaze.setCustomName(ChatColor.RED + "Hell's Blaze");
            blaze.setCustomNameVisible(true);
            getSummons().add(blaze);
        }
    }

    public void spawnPiglinBrutes() {
        if (getSpawnLocations() == null) return;
        for (int i = 0; i <= 4; i++) {
            Location randomSpawnLocation = getSpawnLocations().get((int) (Math.random() * getSpawnLocations().size()));
            if (randomSpawnLocation.getWorld() == null) continue;
            PiglinBrute piglinBrute = (PiglinBrute) randomSpawnLocation.getWorld().spawnEntity(randomSpawnLocation, EntityType.PIGLIN_BRUTE);
            piglinBrute.setMetadata("trialmob", new FixedMetadataValue(SurvivalSkills.getInstance(), true));
            piglinBrute.setMetadata("spawned", new FixedMetadataValue(SurvivalSkills.getInstance(), true));
            piglinBrute.setCustomName(ChatColor.RED + "Hell's Brute");
            piglinBrute.setCustomNameVisible(true);
            getSummons().add(piglinBrute);
        }
    }

    @Override
    public HellsGatekeeper duplicate(double healthMultiplier, double damageMultiplier) {
        return new HellsGatekeeper(getDrops(), healthMultiplier, damageMultiplier);
    }
}
