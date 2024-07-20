package sir_draco.survivalskills.SkillListeners;

import org.bukkit.*;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.Abilities.AbilityTimer;
import sir_draco.survivalskills.Abilities.BerserkerEffects;
import sir_draco.survivalskills.Bosses.*;
import sir_draco.survivalskills.Bosses.Boss;
import sir_draco.survivalskills.ItemStackGenerator;
import sir_draco.survivalskills.Rewards.PlayerRewards;
import sir_draco.survivalskills.Skills.Skill;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;
import java.util.HashMap;

public class FightingSkill implements Listener {

    private final SurvivalSkills plugin;
    private final ArrayList<Material> validWeapons = new ArrayList<>();
    private final ArrayList<Player> activeBerserkers = new ArrayList<>();
    private final ArrayList<GiantBoss> giants = new ArrayList<>();
    private final ArrayList<BroodMotherBoss> broodMothers = new ArrayList<>();
    private final ArrayList<VillagerBoss> villagers = new ArrayList<>();
    private final ArrayList<Player> noPhantomSpawns = new ArrayList<>();
    private final HashMap<Player, Boss> summonTracker = new HashMap<>();
    private final HashMap<EntityType, Double> mobXP = new HashMap<>();

    private DragonBoss dragonBoss;

    public FightingSkill(SurvivalSkills plugin) {
        this.plugin = plugin;
        createMobXPMapping();
        createValidWeapons();
    }

    @EventHandler (ignoreCancelled = true)
    public void onKillEntity(EntityDeathEvent e) {
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            activeBerserkers.remove(p);

            if (summonTracker.containsKey(p)) {
                Boss boss = summonTracker.get(p);
                if (boss != null) {
                    boss.despawnBoss();
                    Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE + p.getDisplayName() + ChatColor.RED + ChatColor.BOLD +
                            " was bested by " + ChatColor.DARK_PURPLE + ChatColor.BOLD + boss.getName());
                }
                summonTracker.remove(p);
            }

            if (p.getWorld().getEnvironment().equals(World.Environment.THE_END) && dragonBoss != null) dragonBoss.lifeSteal();
            return;
        }

        Player p = e.getEntity().getKiller();
        if (p == null) return;

        if (isBoss(e.getEntity())) {
            ItemStack drop = new ItemStack(Material.AIR);
            switch (e.getEntity().getType()) {
                case ZOMBIE:
                    drop = ItemStackGenerator.getGiantBossItem();
                    removeGiant(e.getEntity());
                    Bukkit.broadcastMessage(ChatColor.AQUA + "The Giant" + ChatColor.LIGHT_PURPLE + " has been slain!");
                    killExperience(p, plugin.getSkillManager().getFightingXP() * 500);
                    break;
                case SPIDER:
                    drop = ItemStackGenerator.getBroodMotherBossItem();
                    removeBroodMother(e.getEntity());
                    Bukkit.broadcastMessage(ChatColor.AQUA + "The BroodMother" + ChatColor.LIGHT_PURPLE + " has been slain!");
                    killExperience(p, plugin.getSkillManager().getFightingXP() * 1000);
                    break;
                case VILLAGER:
                    drop = ItemStackGenerator.getVillagerBossItem();
                    removeVillager(e.getEntity());
                    Bukkit.broadcastMessage(ChatColor.AQUA + "The Exiled One" + ChatColor.LIGHT_PURPLE + " has been slain!");
                    killExperience(p, plugin.getSkillManager().getFightingXP() * 2500);
                    break;
                case ENDER_DRAGON:
                    if (dragonBoss != null) {
                        World world = dragonBoss.getBoss().getWorld();
                        dragonBoss.death();
                        dragonBoss = null;
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (!player.getWorld().getEnvironment().equals(World.Environment.THE_END)) continue;

                            if (!world.hasMetadata("killedfirstdragon"))
                                killExperience(player, plugin.getSkillManager().getFightingXP() * 500);
                            else killExperience(player, plugin.getSkillManager().getFightingXP() * 75);
                        }

                        if (!world.hasMetadata("killedfirstdragon"))
                            world.setMetadata("killedfirstdragon", new FixedMetadataValue(plugin, true));
                    }
                    break;
            }
            e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), drop);
            e.setDroppedExp(0);
            return;
        }

        if (e.getEntity().getType().equals(EntityType.WARDEN)) {
            e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), ItemStackGenerator.getWardenBossItem());
        } else if (e.getEntity().getType().equals(EntityType.ELDER_GUARDIAN)) {
            e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), ItemStackGenerator.getElderGuardianBossItem());
        } else if (e.getEntity().getType().equals(EntityType.ENDER_DRAGON)) {
            e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), ItemStackGenerator.getEnderDragonBossItem());
        }

        handleExperience(p, e.getEntity());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        // Check to see if a player is holding a weapon
        Player p = e.getPlayer();
        if (isSummoningBoss(p.getInventory().getItemInOffHand())) {
            e.setCancelled(true);
            p.sendRawMessage(ChatColor.RED + "You cannot use a boss summoning item in your offhand");
            return;
        }

        ItemStack mainHand = p.getInventory().getItemInMainHand();
        boolean boss = isSummoningBoss(mainHand);
        if (!holdingWeapon(mainHand) && !boss) return;

        // Handle the boss spawn if it is a boss
        if (boss) {
            e.setCancelled(true);
            if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
            if (e.getHand() == null || !e.getHand().equals(EquipmentSlot.HAND)) return;
            if (e.getClickedBlock() == null) return;
            Location loc = e.getClickedBlock().getLocation().clone().add(0, 1, 0);
            if (mainHand.getType().equals(Material.ZOMBIE_SPAWN_EGG)) spawnBoss("Giant", loc, p, mainHand);
            else if (mainHand.getType().equals(Material.SPIDER_SPAWN_EGG)) {
                e.setCancelled(true);
                spawnBoss("BroodMother", loc, p, mainHand);
            }
            else if (mainHand.getType().equals(Material.VILLAGER_SPAWN_EGG)) spawnBoss("The Exiled One", loc, p, mainHand);
            return;
        }

        // Check if a player is sneaking
        if (!p.isSneaking()) return;
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction().equals(Action.LEFT_CLICK_BLOCK) || e.getAction().equals(Action.LEFT_CLICK_AIR)) return;

        // Check if a berserker effect is active or is on cooldown
        AbilityTimer timer = plugin.getAbilityManager().getAbility(p, "Berserker");
        if (timer != null) {
            if (!timer.isActive()) {
                p.sendRawMessage(ChatColor.RED + "You can use Berserker in " + ChatColor.AQUA
                        + timer.getTimeTillReset() + ChatColor.RED + " seconds");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            }
            return;
        }

        // If not then create a new berserker effect
        newBerserker(p);
    }

    @EventHandler
    public void handleFightingSkills(EntityDamageByEntityEvent e) {
        // Check if the entity is a player
        if (!(e.getDamager() instanceof Player)) return;

        // Check if it is from thorns
        if (e.getCause().equals(EntityDamageEvent.DamageCause.THORNS)) return;

        // If it is, check if it is an active berserker
        Player p = (Player) e.getDamager();
        if (activeBerserkers.contains(p)) {
            if (!plugin.getAbilityManager().getTimerTracker().containsKey(p)) {
                activeBerserkers.remove(p);
                return;
            }
            boolean found = false;
            for (AbilityTimer timer : plugin.getAbilityManager().getTimerTracker().get(p)) {
                if (!timer.getName().equals("Berserker")) continue;
                found = true;
                break;
            }
            if (!found) {
                activeBerserkers.remove(p);
                return;
            }

            e.setDamage(e.getDamage() * 1.5);
            for (int i = 0; i < 5; i++) {
                p.getWorld().spawnParticle(Particle.DUST, e.getEntity().getLocation(), 1, Math.random(), 0.5, Math.random(), new Particle.DustOptions(Color.RED, 1));
            }
        }

        double criticalChance = plugin.getSkillManager().getPlayerRewards(p).getCriticalChance();
        if (criticalChance != 0 && Math.random() < criticalChance) {
            e.setDamage(e.getDamage() * 2.0);
            p.sendRawMessage(ChatColor.GOLD + "Critical Hit!");
            p.playSound(p, Sound.BLOCK_ANVIL_HIT, 1, 1);
            for (int i = 0; i < 5; i++) {
                p.getWorld().spawnParticle(Particle.CRIT, e.getEntity().getLocation(), 1, Math.random(), 0.5, Math.random());
            }
        }

        double lifesteal = plugin.getSkillManager().getPlayerRewards(p).getLifesteal();
        if (lifesteal != 0 && Math.random() < lifesteal) {
            double health = p.getHealth();
            AttributeInstance healthAttribute = p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
            if (healthAttribute == null) return;
            if (health < healthAttribute.getValue()) p.setHealth(Math.min(healthAttribute.getValue(), health + 1));
            else return;
            p.playSound(p, Sound.ENTITY_GENERIC_DRINK, 1, 1);
            p.sendRawMessage(ChatColor.GREEN + "Lifesteal Activated!");
        }
    }

    @EventHandler
    public void spiderBite(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        if (!isBoss(e.getDamager())) return;
        if (!e.getDamager().getType().equals(EntityType.SPIDER)) return;
        Spider spider = (Spider) e.getDamager();
        AttributeInstance healthAttribute = spider.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
        if (healthAttribute == null) return;
        double maxHealth = healthAttribute.getValue();
        spider.setHealth(Math.min(maxHealth, spider.getHealth() + (0.5 * e.getDamage())));

        // Spawn particles coming from the spider
        for (int i = 0; i < 10; i++) {
            spider.getWorld().spawnParticle(Particle.DUST, spider.getLocation(), 2, Math.random(), 0.5, Math.random(), 0,
                    new Particle.DustOptions(Color.RED, 1));
        }
    }

    @EventHandler
    public void bossDamage(EntityDamageEvent e) {
        if (!isBoss(e.getEntity())) return;
        if (e.getEntity().getType().equals(EntityType.ZOMBIE))
            if (e.getCause().equals(EntityDamageEvent.DamageCause.FALL)) e.setCancelled(true);
        else if (e.getEntity().getType().equals(EntityType.ENDER_DRAGON)) {
            if (e.getCause().equals(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION)) e.setCancelled(true);
            else if (e.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)) e.setCancelled(true);
            else if (e.getCause().equals(EntityDamageEvent.DamageCause.LIGHTNING)) e.setCancelled(true);
        }
    }

    @EventHandler
    public void bossDamageByCorrectPlayer(EntityDamageByEntityEvent e) {
        if (!isBoss(e.getEntity())) return;
        Player p = null;
        if (e.getDamager() instanceof Arrow) {
            Arrow arrow = (Arrow) e.getDamager();
            if (!(arrow.getShooter() instanceof Player)) {
                e.setCancelled(true);
                return;
            }
            p = (Player) arrow.getShooter();
        }
        else if (e.getDamager() instanceof Trident) {
            Trident trident = (Trident) e.getDamager();
            if (!(trident.getShooter() instanceof Player)) {
                e.setCancelled(true);
                return;
            }
            p = (Player) trident.getShooter();
        }
        else if (e.getDamager() instanceof Player) p = (Player) e.getDamager();

        if (p == null) {
            e.setCancelled(true);
            return;
        }

        if (e.getEntity().getType().equals(EntityType.VILLAGER)) {
            if (summonTracker.containsKey(p)) {
                Boss boss = summonTracker.get(p);
                if (boss == null) {
                    summonTracker.remove(p);
                    e.setCancelled(true);
                    return;
                }
                if (!boss.getBoss().equals(e.getEntity())) e.setCancelled(true);
            }
            else e.setCancelled(true);
        }
        else if (e.getEntity().getType().equals(EntityType.ENDER_DRAGON)) {
            if (dragonBoss == null) return;
            if (!dragonBoss.getBoss().equals(e.getEntity())) e.setCancelled(true);
            if (dragonBoss.getPlayers().contains(p)) return;
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void teleportToEnd(PlayerTeleportEvent e) {
        if (!e.getCause().equals(PlayerTeleportEvent.TeleportCause.END_PORTAL)) return;
        if (dragonBoss != null) {
            if (dragonBoss.isRespawn()) {
                if (dragonBoss.getPlayers().isEmpty()) dragonBoss.addCanAttackPlayer(e.getPlayer());
            } else {
                e.getPlayer().sendRawMessage(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Ender Dragon: "
                        + ChatColor.RESET + ChatColor.DARK_AQUA + "So you wish to die again " + e.getPlayer().getName() + "?");
                e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.AMBIENT_CAVE, 1, 1);
                return;
            }
        }


        // Get the ender dragon if it is alive
        if (e.getTo() == null) return;
        World world = e.getTo().getWorld();
        if (world == null) return;
        if (world.getEnvironment().equals(World.Environment.NORMAL)) world = Bukkit.getWorld(world.getName() + "_the_end");
        if (world == null) return;
        if (world.hasMetadata("killedfirstdragon")) return;

        World finalWorld = world;
        new BukkitRunnable() {
            @Override
            public void run() {
                boolean found = false;
                for (Entity entity : finalWorld.getEntities()) {
                    if (!entity.getType().equals(EntityType.ENDER_DRAGON)) continue;
                    found = true;
                    LivingEntity ent = (LivingEntity) entity;
                    dragonBoss = new DragonBoss("dragon", 0, 0,
                            250 * Bukkit.getOnlinePlayers().size(), 0, 0, 0, entity.getType(),
                            entity.getLocation(), ent);
                    dragonBoss.runTaskTimer(plugin, 0, 1);
                    break;
                }

                if (!found) finalWorld.setMetadata("killedfirstdragon", new FixedMetadataValue(plugin, true));
            }
        }.runTaskLater(plugin, 20);
    }

    @EventHandler
    public void phantomSpawn(EntitySpawnEvent e) {
        if (!e.getEntity().getType().equals(EntityType.PHANTOM)) return;
        if (noPhantomSpawns.isEmpty()) return;
        Location loc = e.getLocation();
        if (loc.getWorld() == null) return;
        for (Player p : noPhantomSpawns) {
            if (!loc.getWorld().getEnvironment().equals(p.getWorld().getEnvironment())) continue;
            if (p.getLocation().distance(loc) < 50) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void dragonSpawnEvent(EntitySpawnEvent e) {
        if (!e.getEntity().getType().equals(EntityType.ENDER_DRAGON)) return;
        World world = e.getLocation().getWorld();
        if (world == null) return;
        if (!world.getEnvironment().equals(World.Environment.THE_END)) return;
        if (!world.hasMetadata("killedfirstdragon")) return;

        // Spawn the dragon
        // Get the players in the end to determine dragon health
        ArrayList<Player> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().getEnvironment().equals(World.Environment.THE_END)) continue;
            players.add(player);
        }
        int health;
        if (players.isEmpty()) health = 250;
        else health = 250 * players.size();

        dragonBoss = new DragonBoss("dragon", 0, 0, health, 0, 0, 0,
                e.getEntity().getType(), e.getLocation(), (LivingEntity) e.getEntity());
        dragonBoss.runTaskTimer(plugin, 0, 1);
        dragonBoss.setCanAttackPlayers(players);
    }

    public void handleExperience(Player p, Entity ent) {
        EntityType type = ent.getType();
        if (!mobXP.containsKey(type)) {
            killExperience(p, plugin.getSkillManager().getFightingXP() * 0.5);
            return;
        }
        if (type.equals(EntityType.IRON_GOLEM)) {
            IronGolem golem = (IronGolem) ent;
            if (golem.isPlayerCreated()) return;
        }

        double exp;
        try {
            exp = plugin.getSkillManager().getFightingXP() * mobXP.get(type);
        }
        catch (Exception error) {
            Bukkit.getLogger().warning("Entity not found: " + type);
            exp = plugin.getSkillManager().getFightingXP();
        }
        killExperience(p, exp);
    }

    public void killExperience(Player p, double experience) {
        Skill.experienceEvent(plugin, p, experience, "Fighting");
    }

    public void createMobXPMapping() {
        mobXP.put(EntityType.BEE, 0.5);
        mobXP.put(EntityType.BLAZE, 1.5);
        mobXP.put(EntityType.CAVE_SPIDER, 1.0);
        mobXP.put(EntityType.CREEPER, 1.0);
        mobXP.put(EntityType.DROWNED, 1.0);
        mobXP.put(EntityType.ELDER_GUARDIAN, 100.0);
        mobXP.put(EntityType.ENDERMAN, 2.0);
        mobXP.put(EntityType.ENDERMITE, 0.5);
        mobXP.put(EntityType.EVOKER, 2.5);
        mobXP.put(EntityType.GHAST, 2.5);
        mobXP.put(EntityType.HOGLIN, 1.5);
        mobXP.put(EntityType.HUSK, 1.0);
        mobXP.put(EntityType.IRON_GOLEM, 2.0);
        mobXP.put(EntityType.MAGMA_CUBE, 0.25);
        mobXP.put(EntityType.PHANTOM, 1.5);
        mobXP.put(EntityType.PIGLIN, 1.0);
        mobXP.put(EntityType.PIGLIN_BRUTE, 1.5);
        mobXP.put(EntityType.PILLAGER, 1.5);
        mobXP.put(EntityType.POLAR_BEAR, 1.5);
        mobXP.put(EntityType.RAVAGER, 2.5);
        mobXP.put(EntityType.SHULKER, 1.5);
        mobXP.put(EntityType.SILVERFISH, 0.5);
        mobXP.put(EntityType.SKELETON, 2.0);
        mobXP.put(EntityType.SLIME, 0.25);
        mobXP.put(EntityType.SPIDER, 1.0);
        mobXP.put(EntityType.STRAY, 1.5);
        mobXP.put(EntityType.VEX, 1.5);
        mobXP.put(EntityType.VINDICATOR, 1.5);
        mobXP.put(EntityType.WARDEN, 250.0);
        mobXP.put(EntityType.WITCH, 1.5);
        mobXP.put(EntityType.WITHER, 750.0);
        mobXP.put(EntityType.WITHER_SKELETON, 1.5);
        mobXP.put(EntityType.ZOGLIN, 1.5);
        mobXP.put(EntityType.ZOMBIE, 1.0);
        mobXP.put(EntityType.ZOMBIE_VILLAGER, 1.0);
        mobXP.put(EntityType.ZOMBIFIED_PIGLIN, 1.0);
    }

    public void createValidWeapons() {
        validWeapons.add(Material.DIAMOND_SWORD);
        validWeapons.add(Material.GOLDEN_SWORD);
        validWeapons.add(Material.IRON_SWORD);
        validWeapons.add(Material.NETHERITE_SWORD);
        validWeapons.add(Material.STONE_SWORD);
        validWeapons.add(Material.TRIDENT);
        validWeapons.add(Material.WOODEN_SWORD);
    }

    public boolean holdingWeapon(ItemStack item) {
        Material hand = item.getType();
        return validWeapons.contains(hand);
    }

    public void newBerserker(Player p) {
        AttributeInstance health = p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
        if (health == null) return;
        if (p.getHealth() < health.getValue() * 0.25) {
            p.sendRawMessage(ChatColor.RED + "Not enough health for berserker!");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }

        PlayerRewards reward = plugin.getSkillManager().getPlayerRewards(p);
        int activeTime;
        int resetTime;
        if (!reward.getReward("Fighting", "BerserkerI").isApplied()) {
            p.sendRawMessage(ChatColor.RED + "Berserker is unlocked at level: " + ChatColor.AQUA
                    + plugin.getSkillManager().getDefaultPlayerRewards().getReward("Fighting", "BerserkerI").getLevel());
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }
        else if (!reward.getReward("Fighting", "BerserkerII").isApplied()) {
            resetTime = 120;
            activeTime = 3;
        }
        else if (!reward.getReward("Fighting", "BerserkerIII").isApplied()) {
            resetTime = 90;
            activeTime = 5;
        }
        else if (!reward.getReward("Fighting", "BerserkerIV").isApplied()) {
            resetTime = 60;
            activeTime = 5;
        }
        else if (!reward.getReward("Fighting", "BerserkerV").isApplied()) {
            resetTime = 45;
            activeTime = 5;
        }
        else if (!reward.getReward("Fighting", "BerserkerVI").isApplied()) {
            resetTime = 45;
            activeTime = 8;
        }
        else if (!reward.getReward("Fighting", "BerserkerVII").isApplied()) {
            resetTime = 30;
            activeTime = 8;
        }
        else {
            resetTime = 15;
            activeTime = 10;
        }

        AbilityTimer timer = new AbilityTimer(plugin, "Berserker", p, activeTime, resetTime);
        timer.runTaskTimerAsynchronously(plugin, 0, 20);
        plugin.getAbilityManager().addAbility(p, timer);
        activeBerserkers.add(p);
        BerserkerEffects berserker = new BerserkerEffects(p, activeTime);
        berserker.runTaskTimer(plugin, 0, 5);
        p.sendRawMessage(ChatColor.GREEN + "Berserker is active!");
        p.playSound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 1);
        p.damage(health.getValue() * 0.25);
    }

    public boolean isSummoningBoss(ItemStack item) {
        if (ItemStackGenerator.isCustomItem(item, 12)) return true;
        if (ItemStackGenerator.isCustomItem(item, 13)) return true;
        return ItemStackGenerator.isCustomItem(item, 14);
    }

    public void spawnBoss(String boss, Location loc, Player p, ItemStack mainHand) {
        switch (boss) {
            case "Giant":
                if (!isNight(p.getWorld())) {
                    p.sendRawMessage(ChatColor.RED + "You can only spawn the Giant at night");
                    p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                    return;
                }
                GiantBoss giant = new GiantBoss(loc);
                if (!giant.isSpawnSuccess()) {
                    p.sendRawMessage(ChatColor.RED + "Not enough space to spawn the Giant");
                    p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                    return;
                }
                giant.runTaskTimer(plugin, 0, 1);
                addBoss(p, giant);
                if (mainHand.getAmount() == 1) p.getInventory().remove(mainHand);
                else mainHand.setAmount(mainHand.getAmount() - 1);
                break;
            case "BroodMother":
                BroodMotherBoss broodMother = new BroodMotherBoss(loc);
                if (!broodMother.isSpawnSuccess()) {
                    p.sendRawMessage(ChatColor.RED + "Not enough space to spawn the BroodMother");
                    p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                    return;
                }
                broodMother.runTaskTimer(plugin, 0, 1);
                addBoss(p, broodMother);
                if (mainHand.getAmount() == 1) p.getInventory().remove(mainHand);
                else mainHand.setAmount(mainHand.getAmount() - 1);
                break;
            case "Villager":
                VillagerBoss villager = new VillagerBoss(loc, p);
                if (!villager.isSpawnSuccess()) {
                    p.sendRawMessage(ChatColor.RED + "Not enough space to spawn the exiled one");
                    p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                    return;
                }
                villager.runTaskTimer(plugin, 0, 1);
                addBoss(p, villager);
                if (mainHand.getAmount() == 1) p.getInventory().remove(mainHand);
                else mainHand.setAmount(mainHand.getAmount() - 1);
                break;
        }
    }

    public boolean isNight(World world) {
        long time = world.getTime();
        return time > 13000 && time < 23000;
    }

    public boolean isBoss(Entity entity) {
        return entity.hasMetadata("boss");
    }

    public void removeGiant(LivingEntity giant) {
        if (giants.isEmpty()) return;
        for (GiantBoss giantBoss : giants) {
            if (giantBoss.getBoss().equals(giant)) {
                giantBoss.death();
                giants.remove(giantBoss);
                return;
            }
        }
    }

    public void removeBroodMother(LivingEntity broodMother) {
        if (broodMothers.isEmpty()) return;
        for (BroodMotherBoss broodMotherBoss : broodMothers) {
            if (broodMotherBoss.getBoss().equals(broodMother)) {
                broodMotherBoss.death();
                broodMothers.remove(broodMotherBoss);
                return;
            }
        }
    }

    public void removeVillager(LivingEntity villager) {
        if (villagers.isEmpty()) return;
        for (VillagerBoss villagerBoss : villagers) {
            if (villagerBoss.getBoss().equals(villager)) {
                villagerBoss.death();
                villagers.remove(villagerBoss);
                return;
            }
        }
    }

    public ArrayList<GiantBoss> getGiants() {
        return giants;
    }

    public void addBoss(Player p, Boss boss) {
        summonTracker.put(p, boss);
        if (boss instanceof GiantBoss) giants.add((GiantBoss) boss);
        else if (boss instanceof BroodMotherBoss) broodMothers.add((BroodMotherBoss) boss);
        else if (boss instanceof VillagerBoss) villagers.add((VillagerBoss) boss);
    }

    public ArrayList<BroodMotherBoss> getBroodMothers() {
        return broodMothers;
    }

    public ArrayList<VillagerBoss> getVillagerBosses() {
        return villagers;
    }

    public DragonBoss getDragonBoss() {
        return dragonBoss;
    }

    public ArrayList<Player> getNoPhantomSpawns() {
        return noPhantomSpawns;
    }

    public ArrayList<Player> getActiveBerserkers() {
        return activeBerserkers;
    }
}
