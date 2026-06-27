package sir_draco.survivalskills.bosses;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.bosses.Attacks.CowCannon;
import sir_draco.survivalskills.bosses.Attacks.EmeraldAttack;
import sir_draco.survivalskills.bosses.Attacks.Meteor;
import sir_draco.survivalskills.bosses.Attacks.PoisonProjectile;
import sir_draco.survivalskills.utils.ExiledBossMusic;
import sir_draco.survivalskills.utils.ProjectileCalculator;
import sir_draco.survivalskills.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class VillagerBoss extends Boss {

    private static final int LOWEST_BLOCK_HEIGHT = -64;
    private static final List<EntityType> MOBS = List.of(EntityType.BLAZE, EntityType.CREEPER, EntityType.DROWNED, EntityType.ENDERMAN,
        EntityType.EVOKER, EntityType.GHAST, EntityType.HUSK, EntityType.MAGMA_CUBE, EntityType.PHANTOM, EntityType.PILLAGER,
        EntityType.RAVAGER, EntityType.SKELETON, EntityType.SLIME, EntityType.SPIDER, EntityType.STRAY, EntityType.VEX, EntityType.VINDICATOR,
        EntityType.WITCH, EntityType.WITHER_SKELETON, EntityType.ZOMBIE);

    private final Player summoner;
    private final ArrayList<Entity> bossSummonedMobs = new ArrayList<>();
    private final boolean disabledMusic;

    private Villager villager;
    private boolean inAction = false;
    private boolean mobAttackCalled = false;
    private boolean hitPhase = false;
    private boolean healing = false;
    private boolean activeMusic = false;
    private int attackCount = 0;
    private int attackCooldown = 20;
    private int defaultCooldown = 20;
    private int shieldParticleCounter = 0;
    private int teleportCooldown = 80;
    private int arrowCount = 0;
    private int deathTimer = 18000;
    private ExiledBossMusic music;

    // TODO: Implement health logic so health can be 1500 instead of 1024
    private VillagerBoss(Player summoner, boolean disabledMusic) {
        super("The Exiled One", 3, 3, 1024, 0, 10, 0.2, 5);
        this.summoner = summoner;
        this.disabledMusic = disabledMusic;
    }

    public static VillagerBoss create(Location loc, Player summoner, boolean disabledMusic) {
        VillagerBoss boss = new VillagerBoss(summoner, disabledMusic);
        if (!boss.spawnBoss(EntityType.VILLAGER, loc)) return null;
        boss.villager = (Villager) boss.getBoss();
        applyBiomeType(boss.villager, loc);
        boss.teleportFinder(false, false, 10);
        return boss;
    }

    private static void applyBiomeType(Villager villager, Location loc) {
        Biome biome = loc.getBlock().getBiome();
        String biomeName;
        try {
            biomeName = biome.toString();
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.getLogger().warning("Failed to get biome key for " + loc.getBlock().getBiome());
            biomeName = "DEFAULT";
        }

        Villager.Type type = switch (biomeName) {
            // Desert-like
            case "DESERT", "DESERT_HILLS", "DESERT_LAKES" -> Villager.Type.DESERT;

            // Jungle family
            case "JUNGLE", "JUNGLE_HILLS", "JUNGLE_EDGE", "BAMBOO_JUNGLE", "BAMBOO_JUNGLE_HILLS" -> Villager.Type.JUNGLE;

            // Savanna / Badlands / Mesa
            case "SAVANNA", "SAVANNA_PLATEAU", "WINDSWEPT_SAVANNA", "MESA", "MESA_PLATEAU",
                    "BADLANDS", "BADLANDS_PLATEAU", "ERODED_BADLANDS" -> Villager.Type.SAVANNA;

            // Snowy / icy
            case "SNOWY_TUNDRA", "SNOWY_MOUNTAINS", "ICE_SPIKES", "SNOWY_BEACH", "FROZEN_RIVER", "FROZEN_OCEAN" -> Villager.Type.SNOW;

            // Taiga family
            case "TAIGA", "TAIGA_HILLS", "TAIGA_MOUNTAINS", "GIANT_TREE_TAIGA",
                    "OLD_GROWTH_PINE_TAIGA", "OLD_GROWTH_SPRUCE_TAIGA", "SNOWY_TAIGA", "TAIGA_COLD" -> Villager.Type.TAIGA;

            // Swamp / mangrove
            case "SWAMP", "MANGROVE_SWAMP" -> Villager.Type.SWAMP;

            // Plains / flower meadows / others that suit plains villagers
            case "PLAINS", "SUNFLOWER_PLAINS", "MEADOW", "FLOWER_FOREST", "GROVE", "FOREST",
                    "OLD_GROWTH_BIRCH_FOREST", "BIRCH_FOREST", "DARK_FOREST", "WOODED_HILLS", "WOODED_MOUNTAINS",
                    "MUSHROOM_FIELDS", "MUSHROOM_FIELD_SHORE", "STONE_SHORE", "RIVER", "BEACH" -> Villager.Type.PLAINS;

            // Fallback / rare or nether/end biomes map to plains as a safe default
            default -> Villager.Type.PLAINS;
        };
        villager.setVillagerType(type);
    }

    @Override
    public void run() {
        villager.setPersistent(true);
        villager.setGravity(false);
        villager.setAI(false);

        deathTimer--;
        if (boss == null || villager.isDead()) {
            if (music != null) music.setDead(true);
            cleanup();
            cancel();
            return;
        }

        if (handleSummoner()) return;

        if (teleportCooldown != 0) teleportCooldown--;
        if (attackCooldown != 0) attackCooldown--;
        checkStage(false, null);
        updateBossBar();
        manageBossBarPlayers();

        bossMusic();
        if (isDisableAttack()) return;
        attack();

        handleDeathTimer();
    }

    @Override
    public void attack() {
        if (handleHitPhase()) return;
        shieldParticles();
        if (tryEnterHitPhase()) return;
        if (inAction) return;
        if (checkForArrowExplosion()) return;

        double rate = Math.max(0.01, (1 - getHealthPercentage()) * 0.1);
        handleBasicAttack(rate);
        handleStageAttack(rate);

    }

    @Override
    public void deathAnimation() {
        if (music != null) music.setDead(true);
        Villager dummy = (Villager) villager.getWorld().spawnEntity(villager.getLocation(), EntityType.VILLAGER);
        dummy.setCanPickupItems(false);
        dummy.setAI(true);
        dummy.setInvulnerable(true);
        dummy.setGravity(false);
        dummy.setVelocity(new Vector(0, 1.0, 0));
        removeBossSummonedMobs();

        new BukkitRunnable() {
            @Override
            public void run() {
                // Knock everyone nearby away
                List<Player> players = getNearbyPlayers(20);
                for (Player p : players) {
                    Vector direction = ProjectileCalculator.getDirectionVector(dummy.getLocation(), p.getLocation());
                    p.setVelocity(direction.multiply(10));
                }

                // Spawn ender dragon light rays
                EnderDragon dragon = (EnderDragon) dummy.getWorld().spawnEntity(dummy.getLocation(),
                        EntityType.ENDER_DRAGON);
                dragonAnimation(dragon);
                removeDeathEntitiesLater(dummy, dragon);
            }
        }.runTaskLater(SurvivalSkills.getInstance(), 80);
    }

    public void emeraldAttack(boolean machineGun) {
        if (machineGun) {
            inAction = true;
            runCountedTask(0, 3, 10, () -> {createEmeraldProjectile();});
        } 
        else createEmeraldProjectile();
    }

    private void createEmeraldProjectile() {
        EmeraldAttack emerald = new EmeraldAttack(villager.getLocation().clone().add(0, 1, 0), summoner);
        emerald.emeraldProjectile();
    }

    public void slowRay() {
        Location loc = villager.getLocation().clone().add(0, 1, 0);
        teleportLater(65, false, true, 0);
        inAction = true;

        // Draw a line of particles from the villager to the player over 2 ticks
        summoner.playSound(summoner, Sound.BLOCK_BEACON_ACTIVATE, 1, 1);
        new BukkitRunnable() {
            private int count = 0;

            @Override
            public void run() {
                if (count == 0) {
                    summoner.playSound(summoner, Sound.BLOCK_BEACON_DEACTIVATE, 1, 1);
                    if (!villager.hasLineOfSight(summoner)) {
                        cancel();
                        inAction = false;
                        return;
                    }
                    if (!summoner.isOnline() || summoner.isDead())
                        return;
                    summoner.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 300, 3));
                }

                if (count >= 3) {
                    inAction = false;
                    cancel();
                    return;
                }

                Vector direction = ProjectileCalculator.getDirectionVector(loc, summoner.getLocation());
                int distance = (int) Math.ceil(loc.distance(summoner.getLocation()));
                for (int i = 0; i < distance * 2; i++) {
                    Location particleLoc = loc.clone().add(direction.clone().multiply((double) i / 2));
                    villager.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0,
                            new Particle.DustOptions(Color.BLACK, 3));
                }
                count++;
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), 60, 1);
    }

    public void heal() {
        // Spawn healing particles around the villager
        healing = true;
        inAction = true;
        new BukkitRunnable() {
            private final double startingHealth = villager.getHealth();
            private int count = 0;

            @Override
            public void run() {
                if (villager.getHealth() < startingHealth) {
                    villager.getWorld().playSound(villager.getLocation(), Sound.ENTITY_PILLAGER_AMBIENT, 10, 1);
                    healing = false;
                    inAction = false;
                    cancel();
                    return;
                }

                if (count == 3) {
                    healing = false;
                    inAction = false;
                    villager.setHealth(Math.min(villager.getHealth() + villager.getHealth() * 0.5, getMaxHealth()));
                    spawnRandomParticlesAroundBoss(Particle.HAPPY_VILLAGER, 1, 10);
                    villager.getWorld().playSound(villager.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1, 1);
                    cancel();
                    return;
                }

                spawnRandomParticlesAroundBoss(Particle.HEART, 2, 10);
                villager.getWorld().playSound(villager.getLocation(), Sound.ENTITY_GENERIC_DRINK, 10, 1);
                count++;
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), 0, 20);
    }


    public void mobAttack() {
        villager.setHealth(getMaxHealth() / 1.5);
        // One time attack
        mobAttackCalled = true;
        World world = villager.getWorld();
        Location loc = villager.getLocation();
        world.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 10, 1);

        for (EntityType type : MOBS) {
            Vector direction = new Vector((Math.random() - 0.5) * 2, (Math.random() - 0.5) * 2,
                    (Math.random() - 0.5) * 2);
            Entity ent = world.spawnEntity(loc, type);
            ent.setVelocity(direction);
            bossSummonedMobs.add(ent);
        }
    }

    public void explosion() {
        inAction = true;
        attackCooldown = 200;
        teleportFinder(false, false, 5);
        villager.getWorld().playSound(villager.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 10, 1);
        summoner.sendTitle("", ChatColor.RED + "The villager is tired of your ranged attempts", 10, 40, 10);

        // Pull players in gently for 4 seconds with a 2-second delay
        runCountedTask(40, 2, 40, () -> {
            Vector direction = ProjectileCalculator.getDirectionVector(summoner.getLocation(), villager.getLocation());
            Vector nudge = summoner.getVelocity().add(direction.multiply(0.4));
            summoner.setVelocity(nudge);
            ProjectileCalculator.particleLine(summoner.getLocation(), villager.getLocation(), Particle.DUST, Color.BLACK);
        });

        // Spawn particles in a sphere starting far away and moving inwards every second
        new BukkitRunnable() {

            final Location centerPoint = villager.getLocation().clone().add(0, 1, 0);
            int layer = 1;

            @Override
            public void run() {
                if (centerPoint.getWorld() == null) {
                    cancel();
                    return;
                }

                centerPoint.getWorld().playSound(centerPoint, Sound.BLOCK_NOTE_BLOCK_BASS, 10, (float) layer / 3);

                // Spawn particles in a sphere with a radius of 'layer' blocks
                spawnParticleSphere(centerPoint, Particle.ENCHANT, 1, 0.3, layer, null);

                layer++;
                if (layer >= 4) cancel();
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), 0, 20);

        // Create an explosion after 6 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                villager.getWorld().createExplosion(villager.getLocation(), 15, false, true, villager);
                inAction = false;
            }
        }.runTaskLater(SurvivalSkills.getInstance(), 120);

        teleportLater(140, false, false, -5);
    }

    public void fireChargeSpray() {
        inAction = true;
        runCountedTask(0, 3, 15, () -> {
            villager.getWorld().playSound(villager.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 5, 1);
            double x = (Math.random() - 0.5) * 3;
            double y = (Math.random() - 0.5) * 3;
            double z = (Math.random() - 0.5) * 3;
            Vector v = new Vector(x, y, z);
            Fireball fireball = villager.launchProjectile(Fireball.class, v);
            fireball.setDirection(v);
            fireball.setIsIncendiary(true);
            fireball.setYield(3);
        });
    }

    public void poisonSpray() {
        inAction = true;
        runCountedTask(0, 5, 10, () -> {
            PoisonProjectile poison = new PoisonProjectile(villager.getLocation(), summoner);
            poison.poisonProjectile();
        });
    }

    public void teleportSynchronously(Location loc) {
        new BukkitRunnable() {
            @Override
            public void run() {
                ProjectileCalculator.particleLine(villager.getLocation(), loc, Particle.DUST, Color.AQUA);
                villager.teleport(loc);
            }
        }.runTask(SurvivalSkills.getInstance());
    }

    public void teleportFinder(boolean findGround, boolean random, int relativeY) {
        if (inAction) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (random) {
                    teleportRandomly();
                } else if (findGround) {
                    teleportToGround(villager.getLocation().clone());
                } else {
                    teleportRelatively(villager.getLocation().clone(), relativeY);
                }
            }
        }.runTaskAsynchronously(SurvivalSkills.getInstance());
    }

    public void teleportLater(int delay, boolean findGround, boolean random, int relativeY) {
        inAction = true;
        new BukkitRunnable() {
            @Override
            public void run() {
                inAction = false;
                if (teleportCooldown != 0)
                    return;
                else
                    teleportCooldown = 60;
                teleportFinder(findGround, random, relativeY);
            }
        }.runTaskLater(SurvivalSkills.getInstance(), delay);
    }


    private void teleportRandomly() {
        Location loc;
        if (getStage() > 3) loc = locationNearPlayer();
        else loc = randomLocation(true, 10, 5, 10);

        teleportRelatively(loc, 0);
    }


    /**
     * Looks for a ground block recursively and then teleports the villager above it when it finds one
     * @param loc
     * @param relativeY
     */
    private void teleportToGround(Location loc) {
        loc.add(0, -1, 0);
        if (loc.getBlockY() < LOWEST_BLOCK_HEIGHT) {
            teleportSynchronously(summoner.getLocation());
            return;
        }
        // The villager will never start in a block, so if a block is found teleport above it
        if (!loc.getBlock().isEmpty()) {
            teleportSynchronously(loc.add(0, 1, 0));
        } else {
            teleportToGround(loc);
        }
    }


    /**
     * Looks for a space with 2 open blocks recursively relative to the initial location and initial relative Y value
     * @param loc
     * @param relativeY
     */
    private void teleportRelatively(Location loc, int relativeY) {
        loc.add(0, relativeY, 0);
        boolean isAir = loc.getBlock().isEmpty();
        boolean isAirAbove = loc.getBlock().getRelative(0, 1, 0).isEmpty();
        if (!isAir && isAirAbove) {
            teleportRelatively(loc, 1);
        } else if (!isAir) {
            teleportRelatively(loc, 2);
        } else {
            teleportSynchronously(loc);
        }
    }

    public Location randomLocation(boolean isVillager, int xRange, int yRange, int zRange) {
        double offsetX = (Math.random() - 0.5) * xRange;
        double offsetY = (Math.random() - 0.5) * yRange;
        double offsetZ = (Math.random() - 0.5) * zRange;
        Location newLoc = villager.getLocation().clone().add(offsetX, offsetY, offsetZ);
        if (isVillager && newLoc.distance(summoner.getLocation()) > 30)
            return summoner.getLocation().clone().add(offsetX, offsetY, offsetZ);
        return newLoc;
    }

    public Location locationNearPlayer() {
        double offsetX = (Math.random() - 0.5) * 10;
        double offsetY = (Math.random() - 0.5) * 5;
        double offsetZ = (Math.random() - 0.5) * 10;
        return summoner.getLocation().clone().add(offsetX, offsetY, offsetZ);
    }


    /**
     * Get nearby entities within a radius and check if their spherical radius is close enough
     * @param radius
     * @return
     */
    public List<Player> getNearbyPlayers(int radius) {
        return villager.getNearbyEntities(radius, radius, radius).stream()
            .filter(Player.class::isInstance)
            .map(Player.class::cast)
            .filter(p -> p.getLocation().distance(villager.getLocation()) <= radius)
            .toList();
    }

    public void shieldParticles() {
        if (++shieldParticleCounter % 20 != 0) return;

        Location loc = villager.getLocation().clone().add(0, 1, 0);
        spawnParticleSphere(loc, Particle.DUST, 1, 0.3, 2, new Particle.DustOptions(Color.PURPLE, 1F));
    }

    public void dragonAnimation(EnderDragon dragon) {
        // Delay to ensure that dragon entity exists
        // TODO: test dragon animation
        new BukkitRunnable() {
            @Override
            public void run() {
                dragon.setCollidable(false);
                dragon.setCanPickupItems(false);
                dragon.setInvisible(true);
                dragon.setAI(false);
                dragon.setInvulnerable(true);
                dragon.setSilent(true);
                dragon.setGravity(false);
                dragon.playEffect(EntityEffect.ENTITY_DEATH);
                dragon.getWorld().playSound(dragon.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 10, 1);
            }
        }.runTaskLater(SurvivalSkills.getInstance(), 1);
    }

    public void removeDeathEntitiesLater(Villager v, EnderDragon d) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (villager.isDead()) {
                    v.remove();
                    d.remove();
                }
            }
        }.runTaskLater(SurvivalSkills.getInstance(), 100);
    }

    public void bossMusic() {
        if (disabledMusic)
            return;
        if (activeMusic)
            return;
        activeMusic = true;
        music = new ExiledBossMusic(summoner);
        music.runTaskTimerAsynchronously(SurvivalSkills.getInstance(), 3, 2);
    }

    public void manaRegen() {
        // Spawn enchantment particles shooting from the villager
        for (int i = 0; i < 10; i++) {
            Location loc = villager.getLocation().clone();
            villager.getWorld().spawnParticle(Particle.ENCHANT, loc, 1, Math.random() - 0.5, Math.random() * 2.0,
                    Math.random() - 0.5, 0);
        }
    }

    public void removeBossSummonedMobs() {
        if (bossSummonedMobs.isEmpty())
            return;
        for (Entity ent : bossSummonedMobs) {
            if (ent.isDead())
                continue;
            ent.remove();
        }
    }

    public void incrementArrow() {
        arrowCount++;
    }

    public boolean isHitPhase() {
        return hitPhase;
    }

    public ExiledBossMusic getMusic() {
        return music;
    }

    public boolean isHealing() {
        return healing;
    }

    private void spawnParticleSphere(Location center, Particle particle, int count, double step, double radius, Object data) {
        double stepAngle = step / radius;
        for (double theta = 0; theta <= Math.PI; theta += stepAngle) {
            double y = radius * Math.cos(theta);
            double r = radius * Math.sin(theta);
            for (double phi = 0; phi <= 2 * Math.PI; phi += stepAngle) {
                double x = r * Math.cos(phi);
                double z = r * Math.sin(phi);
                Location particleLoc = center.clone().add(x, y, z);
                villager.getWorld().spawnParticle(particle, particleLoc, count, 0, 0, 0, 0, data);
            }
        }
    }


    private void performStageAttack(double cowChange, double slowChance, double fireChance, double poisonChance, double emeraldChance, boolean mobAttack) {
        if (mobAttack) {
            mobAttack();
            return;
        }

        double type = Math.random();
        if (type >= cowChange) {
            teleportFinder(false, true, 0);
            CowCannon cow = new CowCannon(villager.getLocation().clone().add(0, 1, 0), summoner);
            cow.cowCannon();
        }
        else if (type >= slowChance) slowRay();
        else if (type >= fireChance) fireChargeSpray();
        else if (type >= poisonChance) poisonSpray();
        else if (type >= emeraldChance) emeraldAttack(true);
        else heal();
    }

    
    private void handleDeathTimer() {
        if (deathTimer <= 0) {
            summoner.damage(1000);
            summoner.sendRawMessage(ChatColor.RED.toString() + ChatColor.BOLD + "You ran out of time!");
        } else if (deathTimer == 200) {
            summoner.sendTitle(ChatColor.RED.toString() + ChatColor.BOLD + "10 Seconds Left!", "", 10, 40, 10);
        } else if (deathTimer == 1200) {
            summoner.sendTitle(ChatColor.RED.toString() + ChatColor.BOLD + "1 Minute Left!", "", 10, 40, 10);
        } else if (deathTimer == 6000) {
            summoner.sendTitle(ChatColor.RED.toString() + ChatColor.BOLD + "5 Minutes Left!", "", 10, 40, 10);
        }

        int minutes = deathTimer / 1200;
        int seconds = (deathTimer % 1200) / 20;
        String timeLeft = ChatColor.GRAY + String.format("%02d:%02d", minutes, seconds);
        Utils.sendActionBarMessage(summoner, timeLeft);
    }


    private boolean handleHitPhase() {
        if (!hitPhase) return false;
        manaRegen();
        if (attackCooldown <= 0) {
            for (int i = 0; i <= 3; i++) {
                Meteor meteor = new Meteor(randomLocation(false, 10, 5, 10));
                meteor.launchMeteor();
            }
            attackCooldown = defaultCooldown;
        }
        return true;
    }


    private boolean tryEnterHitPhase() {
        if (attackCount < Math.max(2, 16 * (1 - getHealthPercentage()))) return false;
        if (inAction) return false;

        // Allow the exiled one to be hit
        hitPhase = true;
        teleportFinder(true, false, -1);
        attackCount = 0;
        inAction = true;

        // Play a sound a send a message to all nearby players
        List<Player> players = getNearbyPlayers(50);
        for (Player p : players) {
            p.playSound(p.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1, 1);
            p.sendTitle(ChatColor.LIGHT_PURPLE.toString() + ChatColor.BOLD + "Shield is Down!", "", 10, 40, 10);
        }

        // Stay on the ground for 5 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                inAction = false;
                hitPhase = false;
                teleportFinder(false, false, 5);
            }
        }.runTaskLater(SurvivalSkills.getInstance(), 160);
        return true;
    }


    private void handleStageAttack(double rate) {
        // Handle Phase
        double attackChance = Math.random();
        if (attackChance > rate || attackCooldown > 0) return;
        attackCooldown = defaultCooldown;
        attackCount++;
        int stage = getStage();
        switch (stage) {
            case 1:
                performStageAttack(0.66, 0.33, 2, 2, 2, false); 
                break;
            case 2:
                performStageAttack(0.8, 0.6, 0.4, 2, 0.2, false);
                break;
            case 3:
                if (defaultCooldown != 10) defaultCooldown = 10;
                performStageAttack(0.833, 0.666, 0.5, 0.333, 0.166, false);
                break;
            case 4:
                performStageAttack(0.833, 0.666, 0.5, 0.333, 0.166, !mobAttackCalled);
                break;
            default:
                if (defaultCooldown != 5) defaultCooldown = 5;
                performStageAttack(0.833, 0.666, 0.5, 0.333, 0.166, !mobAttackCalled);
                break;
        }
    }


    private boolean handleSummoner() {
        if (!summoner.isOnline()) {
            cleanup();
            if (music != null) music.setDead(true);
            cancel();
            return true;
        }

        // Make sure the player doesn't get too far away
        if (!summoner.getLocation().getWorld().getEnvironment().equals(villager.getLocation().getWorld().getEnvironment())) {
            summoner.teleport(villager.getLocation());
            summoner.sendRawMessage(getName() + ChatColor.RED.toString() + ChatColor.BOLD + " cast a spell bringing you back to it");
        } else if (summoner.getLocation().distance(villager.getLocation()) > 100) {
            // Shoot the player towards the boss
            Vector direction = ProjectileCalculator.getDirectionVector(summoner.getLocation(), villager.getLocation());
            summoner.setVelocity(direction.multiply(2.0));
            summoner.playSound(summoner, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            summoner.sendRawMessage(ChatColor.RED + "A powerful force prevents you from fleeing");
        }

        return false;
    }


    private void handleBasicAttack(double rate) {
        double emeraldChance = Math.random();
        double teleportChance = Math.random();
        if (teleportCooldown <= 0 && teleportChance < rate) {
            teleportCooldown = 60;
            teleportFinder(false, true, 0);
        }

        if (teleportCooldown > 60) rate = -1;
        if (emeraldChance < rate) {
            emeraldAttack(false);
        }
    }


    private boolean checkForArrowExplosion() {
        if (arrowCount < 5) return false;
        arrowCount = 0;
        explosion();
        return true;
    }


    private void runCountedTask(int delay, int period, int maxCount, Runnable onTick) {
        new BukkitRunnable() {
            private int count = 0;
            @Override
            public void run() {
                if (count >= maxCount) {
                    inAction = false;
                    cancel();
                    return;
                }
                onTick.run();
                count++;
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), delay, period);
    }


    private void spawnRandomParticlesAroundBoss(Particle particle, int particleCount, int iterations) {
        for (int i = 0; i < iterations; i++) {
            Location loc = villager.getLocation().clone().add(Math.random() - 0.5, Math.random() - 0.5,
                    Math.random() - 0.5);
            villager.getWorld().spawnParticle(particle, loc, particleCount, 0, 0, 0, 0);
        }
    }
}
