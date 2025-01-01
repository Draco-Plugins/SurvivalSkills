package sir_draco.survivalskills.SkillListeners;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.Abilities.AbilityManager;
import sir_draco.survivalskills.Abilities.GodItems.EnderEssence;
import sir_draco.survivalskills.Abilities.PowerDrillAsync;
import sir_draco.survivalskills.Abilities.PowerLaser;
import sir_draco.survivalskills.Rewards.RewardNotifications;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.GodQuestline.GodRecipeUI;
import sir_draco.survivalskills.GodQuestline.GodTrophyQuest;
import sir_draco.survivalskills.GodQuestline.PowerOreConversion;
import sir_draco.survivalskills.Utils.ItemStackGenerator;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GodListener implements Listener {

    public static final NamespacedKey potionBagKey = new NamespacedKey(SurvivalSkills.getInstance(), "potion_bag");
    public static int previousPotionBagID = 0;

    private final HashMap<EntityType, ItemStack> godItems = new HashMap<>();
    private final HashMap<Integer, Inventory> potionBags = new HashMap<>();
    private final HashMap<Player, GodRecipeUI> openGodRecipeUI = new HashMap<>();
    private final HashMap<Location, PowerOreConversion> powerOreConversions = new HashMap<>();
    private final HashMap<Player, ArrayList<Block>> drillTracker = new HashMap<>();
    private final ArrayList<Player> powerLaserCooldowns = new ArrayList<>();
    private final ArrayList<Player> conversionCooldowns = new ArrayList<>();
    private final ArrayList<PotionEffectType> potionEffects = new ArrayList<>();
    private final ArrayList<Inventory> openPotionBags = new ArrayList<>();

    public GodListener() {
        createGodWeaponMap();
        createPotionList();
        loadPowerOreConversions();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (p.hasPlayedBefore()) return;

        if (!SurvivalSkills.getInstance().getTrophyManager().getPlayerGodQuestData().containsKey(p.getUniqueId())) {
            GodTrophyQuest quest = new GodTrophyQuest(p.getUniqueId());
            SurvivalSkills.getInstance().getTrophyManager().getPlayerGodQuestData().put(p.getUniqueId(), quest);
        }
    }

    @EventHandler
    public void dropGodWeapon(EntityDeathEvent e) {
        EntityType type = e.getEntityType();
        double chance = Math.random();
        if (!godItems.containsKey(type)) return;

        // Special cases
        if (type.equals(EntityType.ENDER_DRAGON) && chance <= 0.1) {
            e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), godItems.get(type));
            e.getEntity().getWorld().playSound(e.getEntity().getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1, 1);
        }
        else if (type.equals(EntityType.CREEPER)) {
            Creeper creeper = (Creeper) e.getEntity();
            if (creeper.isPowered() && chance <= 0.01) {
                e.getDrops().add(godItems.get(type));
                e.getEntity().getWorld().playSound(e.getEntity().getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1, 1);
            }
            else if (chance <= 0.001) {
                e.getDrops().add(godItems.get(type));
                e.getEntity().getWorld().playSound(e.getEntity().getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1, 1);
            }
        }
        else if (type.equals(EntityType.BREEZE) && chance <= 0.01) {
            e.getDrops().add(godItems.get(type));
            e.getEntity().getWorld().playSound(e.getEntity().getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1, 1);
        }

        // Rest of the mobs
        if (chance > 0.001) return;

        if (type.equals(EntityType.WITCH)) {
            e.getEntity().getWorld().playSound(e.getEntity().getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1, 1);
            e.getDrops().add(ItemStackGenerator.getPotionBag(previousPotionBagID++));
        }
        else {
            e.getDrops().add(godItems.get(type));
            e.getEntity().getWorld().playSound(e.getEntity().getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1, 1);
        }
    }

    @EventHandler
    public void onUseGodItem(PlayerInteractEvent e) {
        if (e.getHand() == null || !e.getHand().equals(EquipmentSlot.HAND)) return;
        if (!e.getAction().equals(Action.RIGHT_CLICK_AIR) && !e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        Player p = e.getPlayer();
        ItemStack mainHand = p.getInventory().getItemInMainHand();

        if (!ItemStackGenerator.isCustomItem(mainHand)) return;
        ItemMeta meta = mainHand.getItemMeta();
        if (meta == null) return;
        if (!meta.hasCustomModelData()) return;
        int modelData = meta.getCustomModelData();

        if (modelData == 33) {
            Vector velocity = p.getLocation().getDirection().multiply(2);
            FallingBlock cobweb = p.getWorld().spawnFallingBlock(p.getLocation().clone().add(0, 1, 0),
                    Material.COBWEB.createBlockData());
            cobweb.setHurtEntities(false);
            cobweb.setVelocity(velocity);
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EGG_THROW, 1, 1);
        }
        else if (modelData == 36) {
            e.setCancelled(true);
            Location loc = p.getLocation().clone().add(p.getLocation().getDirection().multiply(5));
            new EnderEssence(p, loc).runTaskAsynchronously(SurvivalSkills.getInstance());
        }
        else if (modelData == 37) {
            p.getWorld().createExplosion(p.getLocation(), 5, false, true, p);
        }
        else if (modelData == 38) {
            int id = getPotionBagID(mainHand);

            if (potionBags.containsKey(id)) {
                openPotionBags.add(potionBags.get(id));
                p.openInventory(potionBags.get(id));
                return;
            }

            // Try to get the bag data from config
            File file = new File(SurvivalSkills.getInstance().getDataFolder(), "potionbags.yml");
            if (!file.exists()) SurvivalSkills.getInstance().saveResource("potionbags.yml", false);
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            if (potionBagExists(id, config)) {
                Inventory bag = loadPotionBag(id, config);
                openPotionBags.add(bag);
                p.openInventory(bag);
                return;
            }

            Inventory potionBag = Bukkit.createInventory(null, 9, "Potion Bag");
            potionBags.put(id, potionBag);
            openPotionBags.add(potionBag);
            p.openInventory(potionBag);
        }
        else if (modelData == 39) {
            e.setCancelled(true);
            //noinspection UnstableApiUsage
            p.launchProjectile(WindCharge.class, p.getLocation().getDirection().multiply(2));
        }
        else if (modelData == 40) {
            e.setCancelled(true);
            p.launchProjectile(DragonFireball.class, p.getLocation().getDirection().multiply(2));
        }
        else if (modelData == 41) {
            e.setCancelled(true);
            if (e.getHand() == null) return;
            if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;

            if (e.getClickedBlock() == null) return;

            // Check if it is in a claim
            if (SurvivalSkills.getInstance().isGriefPreventionEnabled() && SurvivalSkills.getInstance().checkForClaim(p, e.getClickedBlock().getLocation())) return;
            // Check if they are in spawn
            if (SurvivalSkills.getInstance().isWorldGuardEnabled()) {
                boolean canPlace = SurvivalSkills.getInstance().canPlaceBlockInRegion(p, e.getClickedBlock().getLocation());
                if (!canPlace) return;
            }

            // Place sponge if possible
            Block desiredBlock = e.getClickedBlock().getRelative(e.getBlockFace());
            if (!desiredBlock.getType().isAir() && !desiredBlock.getType().equals(Material.WATER)) return;
            BlockState state = desiredBlock.getState();
            state.setType(Material.WITHER_ROSE);
            desiredBlock.setType(Material.WITHER_ROSE);
            state.update(true);
        }
        else if (modelData == 43) {
            // Handle trident launcher
            e.setCancelled(true);
            Trident trident = p.launchProjectile(Trident.class, p.getLocation().getDirection().multiply(2));
            trident.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        }
        else if (modelData == 46) {
            e.setCancelled(true);
            if (e.getHand() == null) return;
            if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;

            if (e.getClickedBlock() == null) return;

            // Check if it is in a claim
            if (SurvivalSkills.getInstance().isGriefPreventionEnabled() && SurvivalSkills.getInstance().checkForClaim(p, e.getClickedBlock().getLocation())) return;
            // Check if they are in spawn
            if (SurvivalSkills.getInstance().isWorldGuardEnabled()) {
                boolean canPlace = SurvivalSkills.getInstance().canPlaceBlockInRegion(p, e.getClickedBlock().getLocation());
                if (!canPlace) return;
            }

            // Place sponge if possible
            Block desiredBlock = e.getClickedBlock().getRelative(e.getBlockFace());
            if (!desiredBlock.getType().isAir() && !desiredBlock.getType().equals(Material.WATER)) return;
            BlockState state = desiredBlock.getState();
            state.setType(Material.SPONGE);
            desiredBlock.setType(Material.SPONGE);
            state.update(true);
        }
        else if (modelData == 47) {
            if (!SurvivalSkills.getInstance().getSkillManager().getPlayerRewards(p).getReward("Mining", "PowerOre").isApplied()) {
                p.sendRawMessage(ChatColor.RED + "Unlock power ore to use the power swords special ability at mining level: " + ChatColor.AQUA +
                        SurvivalSkills.getInstance().getSkillManager().getDefaultPlayerRewards().getReward("Mining", "PowerOre").getLevel());
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return;
            }

            // Throw lightning bolts down near the player
            e.setCancelled(true);
            Location loc = p.getLocation();
            if (loc.getWorld() == null) return;
            for (int i = 0; i < 8; i++) loc.getWorld().strikeLightning(getSafeNearbyLocation(loc));
        }
        else if (modelData == 50) {
            e.setCancelled(true);
            if (powerLaserCooldowns.contains(p)) return;

            if (!SurvivalSkills.getInstance().getSkillManager().getPlayerRewards(p).getReward("Mining", "PowerOre").isApplied()) {
                p.sendRawMessage(ChatColor.RED + "Unlock power ore to use the power laser at mining level: " + ChatColor.AQUA +
                        SurvivalSkills.getInstance().getSkillManager().getDefaultPlayerRewards().getReward("Mining", "PowerOre").getLevel());
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return;
            }

            powerLaserCooldowns.add(p);
            PowerLaser laser = new PowerLaser(p, this);
            laser.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);
        }
    }

    @EventHandler
    public void reviveZombie(PlayerInteractEntityEvent e) {
        if (!e.getHand().equals(EquipmentSlot.HAND)) return;
        if (!e.getRightClicked().getType().equals(EntityType.ZOMBIE_VILLAGER)) return;
        Player p = e.getPlayer();
        ItemStack mainHand = p.getInventory().getItemInMainHand();

        if (ItemStackGenerator.isCustomItem(mainHand, 35)) {
            ZombieVillager zombie = (ZombieVillager) e.getRightClicked();
            zombie.setConversionTime(40);
            Location loc = e.getRightClicked().getLocation();
            if (loc.getWorld() == null) return;
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1, 1);
            loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 30, Math.random(), Math.random(), Math.random());
        }
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        ItemStack arrow = e.getConsumable();
        if (!ItemStackGenerator.isCustomItem(arrow, 34)) return;

        Arrow oldArrow = (Arrow) e.getProjectile();
        Arrow projectile = p.launchProjectile(Arrow.class, oldArrow.getVelocity());
        e.setCancelled(true);
        projectile.addCustomEffect(getRandomPotionEffect(), true);
        projectile.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
        projectile.setColor(Color.fromRGB(new Random().nextInt(256), new Random().nextInt(256), new Random().nextInt(256)));
    }

    @EventHandler
    public void handleGodDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;

        if (ItemStackGenerator.isCustomItem(p.getInventory().getItemInMainHand(), 47)) {
            if (!e.getCause().equals(EntityDamageEvent.DamageCause.LIGHTNING)) return;
            e.setCancelled(true);
            return;
        }

        if (ItemStackGenerator.isCustomItem(p.getInventory().getItemInMainHand(), 37)) {
            if (!e.getCause().equals(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) &&
                    !e.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)) return;
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void potionBagClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (e.getClick().equals(ClickType.DOUBLE_CLICK)) {
            if (!openPotionBags.contains(p.getOpenInventory().getTopInventory())) return;
            if (e.getCurrentItem() == null) return;
            if (isNotPotion(e.getCurrentItem().getType())) {
                e.setCancelled(true);
                return;
            }
        }

        if (!openPotionBags.contains(e.getInventory())) return;
        if (e.getCursor() == null) return;
        if (isNotPotion(e.getCursor().getType())) {
            e.setCancelled(true);
            p.sendRawMessage(ChatColor.RED + "Only potions can go in this bag");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT , 1, 1);
        }
    }

    @EventHandler
    public void potionDrag(InventoryDragEvent e) {
        if (!openPotionBags.contains(e.getInventory())) return;
        Player p = (Player) e.getWhoClicked();
        if (isNotPotion(e.getOldCursor().getType())) {
            e.setCancelled(true);
            p.sendRawMessage(ChatColor.RED + "Only potions can go in this bag");
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT , 1, 1);
        }
    }

    @EventHandler
    public void onPotionBagDestroy(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Item item)) return;
        if (!ItemStackGenerator.isCustomItem(item.getItemStack(), 38)) return;

        int id = getPotionBagID(item.getItemStack());
        item.remove();
        removePotionBag(id);
    }

    @EventHandler
    public void onPlayerDamageByLightning(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!e.getCause().equals(EntityDamageEvent.DamageCause.LIGHTNING)) return;
        if (conversionCooldowns.contains(p)) return;
        tryPowerOreConversion(p);
    }

    @EventHandler
    public void onBreakBlock(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block block = e.getBlock();
        Location loc = block.getLocation();
        if (block.getType().equals(Material.OBSIDIAN) && powerOreConversions.containsKey(loc)) {
            PowerOreConversion conversion = powerOreConversions.get(loc);
            if (!conversion.getUUID().equals(p.getUniqueId())) {
                e.setCancelled(true);
                p.sendRawMessage(ChatColor.RED + "This is not your ore to break");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return;
            }
            e.setDropItems(false);
            conversion.breakOre();
            powerOreConversions.remove(loc);
            return;
        }

        if (ItemStackGenerator.isCustomItem(p.getInventory().getItemInMainHand(), 48)) {
            // Make sure the player has the ability to drill
            if (!SurvivalSkills.getInstance().getSkillManager().getPlayerRewards(p).getReward("Mining", "PowerOre").isApplied()) {
                p.sendRawMessage(ChatColor.RED + "Unlock power ore to use the drill at level: " + ChatColor.AQUA +
                        SurvivalSkills.getInstance().getSkillManager().getDefaultPlayerRewards().getReward("Mining", "PowerOre").getLevel());
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return;
            }
            // Make sure this block isn't part of a previous drill task
            if (!drillTracker.containsKey(p)) drillTracker.put(p, new ArrayList<>());
            if (drillTracker.get(p).contains(e.getBlock())) {
                drillTracker.get(p).remove(e.getBlock());
                return;
            }
            // Prevent drill crossover by ignoring air blocks
            if (block.getType().equals(Material.AIR)) return;
            PowerDrillAsync drillTask = new PowerDrillAsync(SurvivalSkills.getInstance(), p, this, e.getBlock());
            drillTask.runTaskAsynchronously(SurvivalSkills.getInstance());
        }
        }

    @EventHandler
    public void onPlayerClickPowerOreConversion(PlayerInteractEvent e) {
        if (e.getHand() == null || !e.getHand().equals(EquipmentSlot.HAND)) return;
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        Player p = e.getPlayer();
        Block block = e.getClickedBlock();
        if (block == null) return;
        Location loc = block.getLocation();
        if (!powerOreConversions.containsKey(loc)) return;
        PowerOreConversion conversion = powerOreConversions.get(loc);
        if (!conversion.getUUID().equals(p.getUniqueId())) return;

        // Tell the player how much time is left
        p.sendRawMessage(ChatColor.YELLOW + "Time left: " + RewardNotifications.cooldown(conversion.getSecondsLeft()));
        p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
    }

    @EventHandler
    public void godRecipeClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (!openGodRecipeUI.containsKey(p)) return;

        e.setCancelled(true);
        openGodRecipeUI.get(p).handleClick(e);
    }

    @EventHandler
    public void godRecipeDrag(InventoryDragEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (!openGodRecipeUI.containsKey(p)) return;

        e.setCancelled(true);
        openGodRecipeUI.get(p).handleDrag(e);
    }

    @EventHandler
    public void godRecipeClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        if (!openGodRecipeUI.containsKey(p)) return;
        if (!openGodRecipeUI.get(p).getInventories().get(openGodRecipeUI.get(p).getCurrentInv()).equals(e.getInventory()))
            return;
        openGodRecipeUI.remove(p);
    }

    @EventHandler
    public void powerSwordAttack(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!ItemStackGenerator.isCustomItem(p.getInventory().getItemInMainHand(), 47)) return;
        if (!SurvivalSkills.getInstance().getSkillManager().getPlayerRewards(p).getReward("Mining", "PowerOre").isApplied()) return;
        if (!(e.getEntity() instanceof LivingEntity)) return;
        for (Entity ent : p.getNearbyEntities(10, 10, 10)) {
            if (!AbilityManager.getDomainMobs().contains(ent.getType())) continue;
            if (!(ent instanceof LivingEntity livingEnt)) continue;
            livingEnt.getWorld().strikeLightning(livingEnt.getLocation());
        }
    }

    public PotionEffect getRandomPotionEffect() {
        return new PotionEffect(potionEffects.get((int) Math.floor(Math.random() * potionEffects.size())),
                160, 0);
    }

    public boolean isNotPotion(Material mat) {
        return !mat.equals(Material.POTION)
                && !mat.equals(Material.SPLASH_POTION)
                && !mat.equals(Material.LINGERING_POTION);
    }

    public void savePotionBags(FileConfiguration config) {
        for (Map.Entry<Integer, Inventory> bag : potionBags.entrySet()) {
            config.set(bag.getKey() + ".Items", null);
            if (bag.getValue().isEmpty()) {
                config.set(bag.getKey().toString(), false);
                continue;
            }
            else config.set(bag.getKey().toString(), true);

            int i = 0;
            for (ItemStack item : bag.getValue().getContents()) {
                config.set(bag.getKey() + ".Items." + i, item);
                i++;
            }
        }
    }

    public boolean potionBagExists(int id, FileConfiguration config) {
        return config.contains(String.valueOf(id));
    }

    public Inventory loadPotionBag(int id, FileConfiguration config) {
        Inventory bag = Bukkit.createInventory(null, 9, "Potion Bag");
        potionBags.put(id, bag);
        if (!config.getBoolean(String.valueOf(id))) return bag;
        if (!config.contains(id + ".Items")) return bag;

        ConfigurationSection section = config.getConfigurationSection(id + ".Items");
        if (section == null) return bag;

        section.getKeys(false).forEach(key -> {
            ItemStack item = config.getItemStack(id + ".Items." + key);
            if (item == null) return;
            bag.addItem(item);
        });
        return bag;
    }

    public void removePotionBag(int id) {
        potionBags.remove(id);

        File potionBagFile = new File(SurvivalSkills.getInstance().getDataFolder(), "potionbags.yml");
        if (!potionBagFile.exists()) SurvivalSkills.getInstance().saveResource("potionbags.yml", true);
        FileConfiguration potionBagData = YamlConfiguration.loadConfiguration(potionBagFile);

        potionBagData.set(String.valueOf(id), null);

        try {
            potionBagData.save(potionBagFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getPotionBagID(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        int id = container.getOrDefault(potionBagKey, PersistentDataType.INTEGER, previousPotionBagID++);
        item.setItemMeta(meta);

        return id;
    }

    public void savePowerOreConversions(FileConfiguration data) {
        // Empty the file
        data.set("PowerOreConversions", null);

        int i = 1;
        for (Map.Entry<Location, PowerOreConversion> entry : powerOreConversions.entrySet()) {
            Location loc = entry.getKey();
            PowerOreConversion conversion = entry.getValue();
            if (loc.getWorld() == null) continue;
            data.set("PowerOreConversions." + i + ".Location", loc);
            data.set("PowerOreConversions." + i + ".SecondsLeft", conversion.getSecondsLeft());
            data.set("PowerOreConversions." + i + ".Player", conversion.getUUID().toString());
            i++;
        }
    }

    public void loadPowerOreConversions() {
        File file = new File(SurvivalSkills.getInstance().getDataFolder(), "poweroreconversions.yml");
        if (!file.exists()) return;
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);

        if (!data.contains("PowerOreConversions")) return;
        ConfigurationSection section = data.getConfigurationSection("PowerOreConversions");
        if (section == null) return;

        section.getKeys(false).forEach(key -> {
            Location loc = data.getLocation("PowerOreConversions." + key + ".Location");
            int secondsLeft = data.getInt("PowerOreConversions." + key + ".SecondsLeft");
            String playerUUIDString = data.getString("PowerOreConversions." + key + ".Player");
            if (loc == null || playerUUIDString == null) return;
            UUID uuid = UUID.fromString(playerUUIDString);
            PowerOreConversion conversion = new PowerOreConversion(secondsLeft, loc, uuid);
            powerOreConversions.put(loc, conversion);
            conversion.runTaskTimer(SurvivalSkills.getInstance(), 20, 1);
        });
    }

    public void tryPowerOreConversion(Player p) {
        // Add cooldown for player
        conversionCooldowns.add(p);
        new BukkitRunnable() {
            @Override
            public void run() {
                conversionCooldowns.remove(p);
            }
        }.runTaskLaterAsynchronously(SurvivalSkills.getInstance(), 20);

        // Check if player has unlocked power ore
        if (!SurvivalSkills.getInstance().getSkillManager().getPlayerRewards(p).getReward("Mining", "PowerOre").isApplied()) return;


        // Check if there is obsidian below the player
        Block block = p.getLocation().getBlock().getRelative(0, -1, 0);
        Location loc = block.getLocation();
        if (loc.getWorld() == null) return;
        if (!loc.getWorld().getEnvironment().equals(World.Environment.NORMAL)) return;
        if (!block.getType().equals(Material.OBSIDIAN)) return;

        // Check if the player has 50 XP levels
        if (p.getLevel() < 50) {
            p.sendRawMessage(ChatColor.RED + "You need 50 levels of experience to power the ore");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }

        // Check if the player is already converting an ore
        for (PowerOreConversion conversion : powerOreConversions.values()) {
            if (conversion.getUUID().equals(p.getUniqueId())) {
                p.sendRawMessage(ChatColor.RED + "Your life force can only power one ore at a time");
                p.sendRawMessage(ChatColor.YELLOW + "Your ore is at: " + conversion.getLocation().getBlockX() + ", " +
                        conversion.getLocation().getBlockY() + ", " + conversion.getLocation().getBlockZ());
                return;
            }
        }

        // Take the levels and add a power ore conversion object to the list
        p.setLevel(p.getLevel() - 50);
        PowerOreConversion conversion = new PowerOreConversion(3600 * 6, loc, p.getUniqueId());
        powerOreConversions.put(loc, conversion);
        conversion.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);

        p.sendRawMessage(ChatColor.GREEN + "The ore conversion will take 6 hours");
        p.sendRawMessage(ChatColor.GREEN + "The ore will have a green circle above it when it is done");
    }

    public void createGodWeaponMap() {
        // God Quest Mapping
        godItems.put(EntityType.SPIDER, ItemStackGenerator.getWebShooter());
        godItems.put(EntityType.SKELETON, ItemStackGenerator.getUnlimitedTippedArrow());
        godItems.put(EntityType.ZOMBIE, ItemStackGenerator.getVillagerRevivalArtifact());
        godItems.put(EntityType.ENDERMAN, ItemStackGenerator.getEnderEssence());
        godItems.put(EntityType.CREEPER, ItemStackGenerator.getCreeperEssence());
        godItems.put(EntityType.WITCH, ItemStackGenerator.getPotionBag(previousPotionBagID++));
        godItems.put(EntityType.DROWNED, ItemStackGenerator.getTridentLauncher());
        godItems.put(EntityType.BREEZE, ItemStackGenerator.getMagicBagOfWind());
        godItems.put(EntityType.ENDER_DRAGON, ItemStackGenerator.getDragonBreathCannon());
        godItems.put(EntityType.GUARDIAN, ItemStackGenerator.getUnlimitedSponge());
    }

    public void createPotionList() {
        for (PotionEffectType type : Registry.EFFECT)
            potionEffects.add(type);
    }

    public Location getSafeNearbyLocation(Location loc) {
        Location newLoc = loc.clone();
        double xOff = (Math.random() - 0.5) * 20;
        double zOff = (Math.random() - 0.5) * 20;
        if (Math.abs(xOff) < 5) xOff = 5 * Math.signum(xOff);
        else if (Math.abs(zOff) < 5) zOff = 5 * Math.signum(zOff);
        newLoc.add(xOff, 0, zOff);
        // Touch ground
        while (newLoc.getBlock().getType().isAir()) newLoc.add(0, -1, 0);
        return newLoc;
    }

    public HashMap<Player, GodRecipeUI> getOpenGodRecipeUI() {
        return openGodRecipeUI;
    }

    public HashMap<Integer, Inventory> getPotionBags() {
        return potionBags;
    }

    public HashMap<Player, ArrayList<Block>> getDrillTracker() {
        return drillTracker;
    }

    public ArrayList<Player> getPowerLaserCooldowns() {
        return powerLaserCooldowns;
    }
}
