package sir_draco.survivalskills.Bosses;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.Utils.ExiledBossMusic;
import sir_draco.survivalskills.Utils.ProjectileCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class VillagerBoss extends Boss {

    private final Player summoner;
    private final ArrayList<Entity> bossSummonedMobs = new ArrayList<>();

    private Villager villager;
    private boolean inAction = false;
    private boolean activeAttack = false;
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

    public VillagerBoss(Location loc, Player summoner) {
        super("The Exiled One", 3, 3, 1500, 0, 10, 0.2, EntityType.VILLAGER, loc, 5);
        this.summoner = summoner;
        if (isSpawnSuccess()) {
            villager = (Villager) getBoss();
            Biome biome = loc.getBlock().getBiome();
            switch (biome.name()) {
                case "DESERT":
                    villager.setVillagerType(Villager.Type.DESERT);
                    break;
                case "JUNGLE":
                    villager.setVillagerType(Villager.Type.JUNGLE);
                    break;
                case "SAVANNA":
                    villager.setVillagerType(Villager.Type.SAVANNA);
                    break;
                case "SNOWY":
                    villager.setVillagerType(Villager.Type.SNOW);
                    break;
                case "SWAMP":
                    villager.setVillagerType(Villager.Type.SWAMP);
                    break;
                case "TAIGA":
                    villager.setVillagerType(Villager.Type.TAIGA);
                    break;
                default:
                    villager.setVillagerType(Villager.Type.PLAINS);
                    break;
            }
            villager.setPersistent(true);
            teleportFinder(false, false, null, 10);
        }
    }

    @Override
    public void run() {
        deathTimer--;
        if (!isSpawnSuccess() || villager.isDead()) {
            cancel();
            return;
        }

        if (!summoner.isOnline()) {
            despawnBoss();
            music.setDead(true);
            cancel();
            return;
        }

        // Make sure the player doesn't get too far away
        if (summoner.getLocation().distance(villager.getLocation()) > 100) {
            // Shoot the player towards the boss
            Vector direction = ProjectileCalculator.getDirectionVector(summoner.getLocation(), villager.getLocation());
            summoner.setVelocity(direction.multiply(2.0));
            summoner.playSound(summoner, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            summoner.sendRawMessage(ChatColor.RED + "You can't leave this fight");
        }

        villager.setGravity(false);
        villager.setAI(false);
        if (teleportCooldown != 0) teleportCooldown--;
        if (attackCooldown != 0) attackCooldown--;
        checkStage(false, null);
        updateBossBar();
        manageBossBarPlayers();

        bossMusic();
        if (isDisableAttack()) return;
        attack();

        if (deathTimer <= 0) {
            summoner.damage(1000);
            summoner.sendRawMessage(ChatColor.RED.toString() + ChatColor.BOLD + "You ran out of time!");
        }
        else if (deathTimer == 200)
            summoner.sendTitle(ChatColor.RED.toString() + ChatColor.BOLD + "10 Seconds Left!", "", 10, 40, 10);
        else if (deathTimer == 1200)
            summoner.sendTitle(ChatColor.RED.toString() + ChatColor.BOLD + "1 Minute Left!", "", 10, 40, 10);
        else if (deathTimer == 6000)
            summoner.sendTitle(ChatColor.RED.toString() + ChatColor.BOLD + "5 Minutes Left!", "", 10, 40, 10);

        int minutes = deathTimer / 1200;
        int seconds = (deathTimer % 1200) / 20;
        String timeLeft = ChatColor.GRAY + String.format("%02d:%02d", minutes, seconds);
        sendActionBarMessage(summoner, timeLeft);
    }

    @Override
    public void attack() {
        if (hitPhase) {
            manaRegen();
            if (attackCooldown <= 0) {
                for (int i = 0; i <= 3; i++) launchMeteor();
                attackCooldown = defaultCooldown;
            }
            return;
        }
        else {
            shieldParticles();
            if (attackCount >= Math.max(2, 16 * (1 - getHealthPercentage()))) {
                if (inAction) return;

                // Allow the exiled one to be hit
                hitPhase = true;
                teleportFinder(true, false, null, -1);
                attackCount = 0;
                inAction = true;

                // Play a sound a send a message to all nearby players
                ArrayList<Player> players = getNearbyPlayers(50);
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
                        teleportFinder(false, false, null, 5);
                    }
                }.runTaskLater(SurvivalSkills.getPlugin(SurvivalSkills.class), 160);
                return;
            }
        }

        if (inAction) return;
        if (arrowCount >= 5) {
            arrowCount = 0;
            explosion();
            return;
        }

        // Determine if the villager will attack - the lower the health the higher the attack rate
        double attackChance = Math.random();
        double emeraldChance = Math.random();
        double teleportChance = Math.random();
        double rate = Math.max(0.01, (1 - getHealthPercentage()) * 0.1);
        if (teleportCooldown <= 0 && teleportChance < rate) {
            teleportCooldown = 60;
            teleportFinder(false, true, null, 0);
        }

        if (teleportCooldown > 60) rate = -1;
        if (!activeAttack && emeraldChance < rate) emeraldAttack(false);

        // Handle Phase
        if (attackChance > rate || attackCooldown > 0) return;
        attackCooldown = defaultCooldown;
        attackCount++;
        if (getStage() == 1) {
            double type = Math.random();
            if (type >= 0.66) cowCannon();
            else if (type >= 0.33) slowRay();
            else heal();
        }
        else if (getStage() == 2) {
            double type = Math.random();
            if (type >= 0.8) cowCannon();
            else if (type >= 0.6) slowRay();
            else if (type >= 0.4) fireChargeSpray();
            else if (type >= 0.2) emeraldAttack(true);
            else heal();
        } else if (getStage() == 3) {
            if (defaultCooldown != 10) defaultCooldown = 10;
            double type = Math.random();
            if (type >= 0.833) cowCannon();
            else if (type >= 0.666) slowRay();
            else if (type >= 0.5) fireChargeSpray();
            else if (type >= 0.333) poisonSpray();
            else if (type >= 0.166) emeraldAttack(true);
            else heal();
        } else if (getStage() == 4) {
            if (!mobAttackCalled) {
                mobAttack();
                return;
            }

            double type = Math.random();
            if (type >= 0.833) cowCannon();
            else if (type >= 0.666) slowRay();
            else if (type >= 0.5) fireChargeSpray();
            else if (type >= 0.333) poisonSpray();
            else if (type >= 0.166) emeraldAttack(true);
            else heal();
        } else {
            if (defaultCooldown != 5) defaultCooldown = 5;
            if (!mobAttackCalled) {
                mobAttack();
                return;
            }

            double type = Math.random();
            if (type >= 0.833) cowCannon();
            else if (type >= 0.666) slowRay();
            else if (type >= 0.5) fireChargeSpray();
            else if (type >= 0.333) poisonSpray();
            else if (type >= 0.166) emeraldAttack(true);
            else heal();
        }

    }

    @Override
    public void deathAnimation() {
        music.setDead(true);
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
                ArrayList<Player> players = getNearbyPlayers(20);
                for (Player p : players) {
                    Vector direction = ProjectileCalculator.getDirectionVector(dummy.getLocation(), p.getLocation());
                    p.setVelocity(direction.multiply(10));
                }

                // Spawn ender dragon light rays
                EnderDragon dragon = (EnderDragon) dummy.getWorld().spawnEntity(dummy.getLocation(), EntityType.ENDER_DRAGON);
                dragonAnimation(dragon);
                removeDeathEntitiesLater(dummy, dragon);
            }
        }.runTaskLater(SurvivalSkills.getPlugin(SurvivalSkills.class), 80);
    }

    public void emeraldAttack(boolean machineGun) {
        if (machineGun) {
            inAction = true;
            new BukkitRunnable() {
                private int count = 0;
                @Override
                public void run() {
                    if (count >= 10) {
                        inAction = false;
                        cancel();
                        return;
                    }
                    emeraldProjectile();
                    count++;
                }
            }.runTaskTimer(SurvivalSkills.getPlugin(SurvivalSkills.class), 0, 3);
        }
        else emeraldProjectile();
    }

    public void emeraldProjectile() {
        // Spawn an emerald item stack at the villager's location
        Location loc = villager.getLocation().clone().add(0, 1, 0);
        ItemStack emerald = new ItemStack(Material.EMERALD);
        Item item = villager.getWorld().dropItem(loc, emerald);
        item.setGravity(false);
        item.setOwner(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        item.setVelocity(ProjectileCalculator.getNoGravityVector(loc, summoner.getLocation().clone().add(0, 1, 0), 2));
        item.setCustomName("Emerald Bullet");
        summoner.playSound(summoner, Sound.ENTITY_SHULKER_SHOOT, 1, 1);

        // Create a bukkit runnable that checks if the emerald is inside the target's hit-box
        new BukkitRunnable() {
            private int count = 0;
            private final ArrayList<Player> alreadyHitPlayers = new ArrayList<>();

            @Override
            public void run() {
                if (count == 100) {
                    item.remove();
                    cancel();
                    return;
                }
                List<Entity> hitPlayers = item.getNearbyEntities(3, 3, 3);
                if (!hitPlayers.isEmpty()) {
                    for (Entity ent : hitPlayers) {
                        if (!(ent instanceof Player)) continue;
                        Player p = (Player) ent;

                        double distance = Math.sqrt(Math.pow(p.getLocation().getX() - item.getLocation().getX(), 2) +
                                Math.pow(p.getLocation().getZ() - item.getLocation().getZ(), 2));
                        double yDist = Math.abs(p.getLocation().getY() + 1 - item.getLocation().getY());

                        if (distance > 0.85 || yDist > 1.5) continue;
                        if (alreadyHitPlayers.contains(p)) continue;
                        alreadyHitPlayers.add(p);
                        p.damage(40, item);
                        p.playSound(p, Sound.BLOCK_ANVIL_HIT, 1, 1);
                    }
                }
                count++;
            }
        }.runTaskTimer(SurvivalSkills.getPlugin(SurvivalSkills.class), 0, 1);
    }

    public void cowCannon() {
        teleportFinder(false, true, null, 0);
        Location loc = villager.getLocation().clone().add(0, 1, 0);
        Cow cow = villager.getWorld().spawn(loc, Cow.class);
        cow.setGravity(false);
        Vector direction = ProjectileCalculator.getNoGravityVector(loc, summoner.getLocation().clone().add(0, 1, 0), 2.5);
        cow.setVelocity(direction);

        new BukkitRunnable() {
            private final Cow cowCannon = cow;
            private int count = 0;
            @Override
            public void run() {
                if (count >= 80) {
                    cowCannon.getWorld().createExplosion(cowCannon.getLocation(), 8);
                    cowCannon.remove();
                    cancel();
                    return;
                }

                // If the cow hits a player or the ground, explode
                cowCannon.setGravity(false);
                count++;
                if (cowCannon.getLocation().distance(summoner.getLocation()) > 2 && !cowCannon.isOnGround()) return;
                cowCannon.getWorld().createExplosion(cowCannon.getLocation(), 8);
                cowCannon.remove();
                cancel();
            }
        }.runTaskTimer(SurvivalSkills.getPlugin(SurvivalSkills.class), 0, 1);
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
                    if (!summoner.isOnline() || summoner.isDead()) return;
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
                    villager.getWorld().spawnParticle(Particle.DUST, particleLoc, 1, 0, 0, 0, 0, new Particle.DustOptions(Color.BLACK, 3));
                }
                count++;
            }
        }.runTaskTimer(SurvivalSkills.getPlugin(SurvivalSkills.class), 60, 1);
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
                    for (int i = 0; i < 10; i++) {
                        Location loc = villager.getLocation().clone().add(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5);
                        villager.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 1, 0, 0, 0, 0);
                    }
                    villager.getWorld().playSound(villager.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1, 1);
                    cancel();
                    return;
                }

                for (int i = 0; i < 10; i++) {
                    Location loc = villager.getLocation().clone().add(Math.random() - 0.5, Math.random() - 0.5, Math.random() - 0.5);
                    villager.getWorld().spawnParticle(Particle.HEART, loc, 2, 0, 0, 0, 0);
                }
                villager.getWorld().playSound(villager.getLocation(), Sound.ENTITY_GENERIC_DRINK, 10, 1);
                count++;
            }
        }.runTaskTimer(SurvivalSkills.getPlugin(SurvivalSkills.class), 0, 20);
    }

    public void mobAttack() {
        villager.setHealth(getMaxHealth() / 1.5);
        mobAttackCalled = true;
        World world = villager.getWorld();
        Location loc = villager.getLocation();
        world.playSound(loc, Sound.ENTITY_WITHER_SPAWN, 10, 1);

        // Spawn a bunch of mobs around the villager
        ArrayList<EntityType> mobs = new ArrayList<>();
        mobs.add(EntityType.BLAZE);
        mobs.add(EntityType.CREEPER);
        mobs.add(EntityType.DROWNED);
        mobs.add(EntityType.ENDERMAN);
        mobs.add(EntityType.EVOKER);
        mobs.add(EntityType.GHAST);
        mobs.add(EntityType.HUSK);
        mobs.add(EntityType.MAGMA_CUBE);
        mobs.add(EntityType.PHANTOM);
        mobs.add(EntityType.PILLAGER);
        mobs.add(EntityType.RAVAGER);
        mobs.add(EntityType.SKELETON);
        mobs.add(EntityType.SLIME);
        mobs.add(EntityType.SPIDER);
        mobs.add(EntityType.STRAY);
        mobs.add(EntityType.VEX);
        mobs.add(EntityType.VINDICATOR);
        mobs.add(EntityType.WITCH);
        mobs.add(EntityType.WITHER_SKELETON);
        mobs.add(EntityType.ZOMBIE);

        for (EntityType type : mobs) {
            Vector direction = new Vector((Math.random() - 0.5) * 2, (Math.random() - 0.5) * 2, (Math.random() - 0.5) * 2);
            Entity ent = world.spawnEntity(loc, type);
            ent.setVelocity(direction);
            bossSummonedMobs.add(ent);
        }
    }

    public void explosion() {
        activeAttack = true;
        attackCooldown = 200;
        teleportFinder(false, false, null, 5);
        villager.getWorld().playSound(villager.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 10, 1);
        summoner.sendTitle("", ChatColor.RED + "Your arrows pull you in", 10, 40, 10);

        // Pull players in gently for 4 seconds with a 2-second delay
        new BukkitRunnable() {
            private int count = 0;

            @Override
            public void run() {
                if (count == 40) {
                    cancel();
                    return;
                }
                Vector direction = ProjectileCalculator.getDirectionVector(summoner.getLocation(), villager.getLocation());
                Vector nudge = summoner.getVelocity().add(direction.multiply(0.4));
                summoner.setVelocity(nudge);
                ProjectileCalculator.particleLine(summoner.getLocation(), villager.getLocation(), Particle.DUST, Color.BLACK);
                count++;
            }
        }.runTaskTimer(SurvivalSkills.getPlugin(SurvivalSkills.class), 40, 2);

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
                double epsilon = 0.1;
                double step = 0.3;
                for (double x = -layer; x <= layer; x += step) {
                    for (double y = -layer; y <= layer; y += step) {
                        for (double z = -layer; z <= layer; z += step) {
                            double distance = Math.sqrt(x * x + y * y + z * z);
                            if (Math.abs(distance - layer) < epsilon) {
                                Location particleLoc = centerPoint.clone().add(x, y, z);
                                centerPoint.getWorld().spawnParticle(Particle.ENCHANT, particleLoc, 1);
                            }
                        }
                    }
                }

                layer++;
                if (layer >= 4) cancel();
            }
        }.runTaskTimer(SurvivalSkills.getPlugin(SurvivalSkills.class), 0, 20);

        // Create an explosion after 6 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                villager.getWorld().createExplosion(villager.getLocation(), 15, false, true, villager);
                activeAttack = false;
            }
        }.runTaskLater(SurvivalSkills.getPlugin(SurvivalSkills.class), 120);

        teleportLater(140, false, false, -5);
    }

    public void fireChargeSpray() {
        inAction = true;
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count >= 15) {
                    inAction = false;
                    cancel();
                    return;
                }

                villager.getWorld().playSound(villager.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 5, 1);
                double x = (Math.random() - 0.5) * 3;
                double y = (Math.random() - 0.5) * 3;
                double z = (Math.random() - 0.5) * 3;
                Vector v = new Vector(x, y, z);
                Fireball fireball = villager.launchProjectile(Fireball.class, v);
                fireball.setDirection(v);
                fireball.setIsIncendiary(true);
                fireball.setYield(3);
                count++;
            }
        }.runTaskTimer(SurvivalSkills.getPlugin(SurvivalSkills.class), 0, 3);
    }

    public void poisonSpray() {
        inAction = true;
        new BukkitRunnable() {
            int count = 0;
            @Override
            public void run() {
                if (count >= 10) {
                    inAction = false;
                    cancel();
                    return;
                }

                poisonProjectile();
                count++;
            }
        }.runTaskTimer(SurvivalSkills.getPlugin(SurvivalSkills.class), 0, 5);
    }

    public void poisonProjectile() {
        villager.getWorld().playSound(villager.getLocation(), Sound.BLOCK_SLIME_BLOCK_PLACE, 10, 1);
        double x = Math.random() - 0.5;
        double y = Math.random() - 0.5;
        double z = Math.random() - 0.5;
        Vector v = new Vector(x, y, z);
        Item item = villager.getWorld().dropItem(villager.getLocation(), new ItemStack(Material.SLIME_BALL));
        item.setOwner(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        item.setGravity(false);
        item.setVelocity(v.multiply(0.35));
        item.setCustomName("Poison Projectile");

        new BukkitRunnable() {
            private int count = 0;
            @Override
            public void run() {
                if (count == 120) {
                    item.remove();
                    cancel();
                    return;
                }

                count++;
                double distance = item.getLocation().distance(summoner.getLocation());
                if (distance > 2.0) return;
                summoner.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 200, 4));
                summoner.damage(60, item);
                summoner.playSound(summoner, Sound.BLOCK_BUBBLE_COLUMN_UPWARDS_AMBIENT, 1, 1);
            }
        }.runTaskTimer(SurvivalSkills.getPlugin(SurvivalSkills.class), 0, 1);
    }

    public void launchMeteor() {
        // Spawn fire charges in the sky that create explosion particles as they fall and when they collide with a block or player they explode
        Location loc = randomLocation();
        loc.setY(loc.getY() + 30);
        Vector direction = new Vector(0, -1, 0);
        // Launch a fireball projectile from the location
        Fireball fireball = villager.getWorld().spawn(loc, Fireball.class);
        fireball.setDirection(direction);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (fireball.isDead()) {
                    cancel();
                    return;
                }

                // If the fireball hits a block or player, explode
                if (fireball.isOnGround() || !fireball.getNearbyEntities(1, 1, 1).isEmpty()
                        || !fireball.getLocation().getBlock().getType().isAir()) {
                    fireball.getWorld().createExplosion(fireball.getLocation(), 6, false, false, fireball);
                    fireball.remove();
                    cancel();
                    return;
                }

                // Spawn explosion particles as the fireball falls
                Location particleLoc = fireball.getLocation().clone().add(0, 1, 0);
                fireball.getWorld().spawnParticle(Particle.EXPLOSION, particleLoc, 3);
            }
        }.runTaskTimer(SurvivalSkills.getPlugin(SurvivalSkills.class), 0, 1);
    }

    public void teleport(Location loc) {
        new BukkitRunnable() {
            @Override
            public void run() {
                ProjectileCalculator.particleLine(villager.getLocation(), loc, Particle.DUST, Color.AQUA);
                villager.teleport(loc);
            }
        }.runTask(SurvivalSkills.getPlugin(SurvivalSkills.class));
    }

    public boolean teleport(boolean findGround, boolean random, Location randomLoc, int relativeY) {
        if (random) {
            if (randomLoc != null) {
                Location loc = randomLoc.add(0, 1, 0);
                boolean isAir = loc.getBlock().isEmpty();
                boolean isAirAbove = loc.getBlock().getRelative(0, 1, 0).isEmpty();
                if (!isAir && isAirAbove) return teleport(false, true, loc.add(0, 1, 0), 0);
                else if (!isAir) return teleport(false, true, loc.add(0, 2, 0), 0);
                teleport(loc);
                return true;
            }

            Location loc;
            if (getStage() > 3) loc = locationNearPlayer();
            else loc = randomLocation();
            boolean isAir = loc.getBlock().isEmpty();
            boolean isAirAbove = loc.getBlock().getRelative(0, 1, 0).isEmpty();
            if (!isAir && isAirAbove) return teleport(false, true, loc.add(0, 1, 0), 0);
            else if (!isAir) return teleport(false, true, loc.add(0, 2, 0), 0);
            teleport(loc);
            return true;
        }
        else if (findGround) {
            Location loc = villager.getLocation().clone().add(0, relativeY, 0);
            if (loc.getBlock().isEmpty()) return teleport(true, false, null, relativeY - 1);
            teleport(loc.add(0, 1, 0));
            return true;
        }
        else {
            Location loc = villager.getLocation().clone().add(0, relativeY, 0);
            boolean isAir = loc.getBlock().isEmpty();
            boolean isAirAbove = loc.getBlock().getRelative(0, 1, 0).isEmpty();
            if (!isAir && isAirAbove) return teleport(false, false, null, relativeY + 1);
            else if (!isAir) return teleport(false, false, null, relativeY + 2);
            teleport(loc);
            return true;
        }
    }

    public void teleportFinder(boolean findGround, boolean random, Location randomLoc, int relativeY) {
        if (inAction) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                teleport(findGround, random, randomLoc, relativeY);
            }
        }.runTaskAsynchronously(SurvivalSkills.getPlugin(SurvivalSkills.class));
    }

    public void teleportLater(int delay, boolean findGround, boolean random, int relativeY) {
        inAction = true;
        new BukkitRunnable() {
            @Override
            public void run() {
                inAction = false;
                if (teleportCooldown != 0) return;
                else teleportCooldown = 60;
                teleportFinder(findGround, random, null, relativeY);
            }
        }.runTaskLater(SurvivalSkills.getPlugin(SurvivalSkills.class), delay);
    }

    public Location randomLocation() {
        double offsetX = (Math.random() - 0.5) * 10;
        double offsetY = (Math.random() - 0.5) * 5;
        double offsetZ = (Math.random() - 0.5) * 10;
        return villager.getLocation().clone().add(offsetX, offsetY, offsetZ);
    }

    public Location locationNearPlayer() {
        double offsetX = (Math.random() - 0.5) * 10;
        double offsetY = (Math.random() - 0.5) * 5;
        double offsetZ = (Math.random() - 0.5) * 10;
        return summoner.getLocation().clone().add(offsetX, offsetY, offsetZ);
    }

    public ArrayList<Player> getNearbyPlayers(int radius) {
        ArrayList<Player> players = new ArrayList<>();
        for (Entity entity : villager.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Player)) continue;
            double distance = entity.getLocation().distance(villager.getLocation());
            if (distance > 50) continue;
            players.add((Player) entity);
        }
        return players;
    }

    public void shieldParticles() {
        if (shieldParticleCounter != 20) {
            shieldParticleCounter++;
            return;
        }
        shieldParticleCounter = 0;

        Location loc = villager.getLocation().clone().add(0, 1, 0);
        // Create a sphere of particles with a radius of 2 blocks around this location
        double step = 0.3;
        for (double x = -2; x <= 2; x += step) {
            for (double y = -2; y <= 2; y += step) {
                for (double z = -2; z <= 2; z += step) {
                    double distance = Math.sqrt(x * x + y * y + z * z);
                    if (distance > 1.9 && distance < 2.1) {
                        Location particleLoc = loc.clone().add(x, y, z);
                        villager.getWorld().spawnParticle(Particle.DRAGON_BREATH, particleLoc, 1, 0, 0, 0, 0);
                    }
                }
            }
        }
    }

    public void dragonAnimation(EnderDragon dragon) {
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
        }.runTaskLater(SurvivalSkills.getPlugin(SurvivalSkills.class), 1);
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
        }.runTaskLater(SurvivalSkills.getPlugin(SurvivalSkills.class), 100);
    }

    public void bossMusic() {
        if (activeMusic) return;
        activeMusic = true;
        music = new ExiledBossMusic(summoner);
        music.runTaskTimerAsynchronously(SurvivalSkills.getPlugin(SurvivalSkills.class), 3, 2);
    }

    public void manaRegen() {
        // Spawn enchantment particles shooting from the villager
        for (int i = 0; i < 10; i++) {
            Location loc = villager.getLocation().clone();
            villager.getWorld().spawnParticle(Particle.ENCHANT, loc, 1, Math.random() - 0.5, Math.random() * 2.0, Math.random() - 0.5, 0);
        }
    }

    public void removeBossSummonedMobs() {
        if (bossSummonedMobs.isEmpty()) return;
        for (Entity ent : bossSummonedMobs) {
            if (ent.isDead()) continue;
            ent.remove();
        }
    }

    public static void sendActionBarMessage(Player p, String message) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(message));
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
}
