package sir_draco.survivalskills.GodQuestline;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.BoundingBox;
import sir_draco.survivalskills.GodQuestline.TrialMobs.*;
import sir_draco.survivalskills.SurvivalSkills;

import java.io.File;
import java.util.*;

public class TrialManager implements Listener {

    private static final ArrayList<Trial> trials = new ArrayList<>();
    private static final ArrayList<Inventory> rewardInventories = new ArrayList<>();
    private static final HashMap<UUID, ProtectedArea> protectedAreas = new HashMap<>();
    private static final HashMap<Integer, Wave> waves = new HashMap<>();
    private static final HashMap<Integer, TrialLootTable> lootTables = new HashMap<>();
    private static final NamespacedKey trialObjectKey = new NamespacedKey(SurvivalSkills.getInstance(), "trialobject");
    private static final HashMap<Player, Location> spectatingPlayers = new HashMap<>();
    private static final HashMap<Player, Player> target = new HashMap<>();
    private static final HashMap<Player, Scoreboard> trialScoreboards = new HashMap<>();

    public TrialManager() {
        createWaves();
        createRewards();
    }

    @EventHandler
    public void onTrialBuildingBreak(BlockBreakEvent e) {
        if (protectedAreas.isEmpty()) return;
        for (ProtectedArea protectedArea : protectedAreas.values()) {
            if (!protectedArea.world().equals(e.getBlock().getWorld())) continue;
            if (protectedArea.boundingBox().contains(e.getBlock().getLocation().toVector())) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onExplosionBreakBlock(EntityExplodeEvent e) {
        if (e.getEntity().hasMetadata("trialmob") && e.getEntity() instanceof Creeper) {
            for (Trial trial : trials) {
                Wave wave = trial.getWave();
                if (wave == null) continue;

                if (wave.getWaveMobs().isEmpty()) continue;
                WaveMob waveMob = wave.getWaveMob(e.getEntity());
                if (waveMob == null) continue;
                wave.removeWaveMob(waveMob);
                e.blockList().clear();
                return;
            }
        }

        if (protectedAreas.isEmpty()) return;
        for (ProtectedArea protectedArea : protectedAreas.values()) {
            for (Block block : e.blockList()) {
                if (!protectedArea.world().equals(block.getWorld())) continue;
                if (protectedArea.boundingBox().contains(block.getLocation().toVector())) {
                    e.blockList().clear();
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onEndermanBlockTake(EntityChangeBlockEvent e) {
        if (protectedAreas.isEmpty()) return;
        if (!e.getEntity().getType().equals(EntityType.ENDERMAN)) return;
        for (ProtectedArea protectedArea : protectedAreas.values()) {
            if (!protectedArea.world().equals(e.getBlock().getWorld())) continue;
            if (protectedArea.boundingBox().contains(e.getBlock().getLocation().toVector())) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onTrialBuildingPlace(BlockPlaceEvent e) {
        if (protectedAreas.isEmpty()) return;
        for (ProtectedArea protectedArea : protectedAreas.values()) {
            if (!protectedArea.world().equals(e.getBlock().getWorld())) continue;
            if (protectedArea.boundingBox().contains(e.getBlock().getLocation().toVector())) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void playerSendCommand(PlayerCommandPreprocessEvent e) {
        if (spectatingPlayers.containsKey(e.getPlayer())) {
            if (e.getMessage().toLowerCase().startsWith("/godtrial")) return;
            e.setCancelled(true);
            return;
        }

        if (trials.isEmpty()) return;
        for (Trial trial : trials) {
            if (!trial.getPlayer().equals(e.getPlayer())) continue;
            // Check if the first letters of the message are "/godtrial"
            if (e.getMessage().toLowerCase().startsWith("/godtrial")) return;
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onTrialMobDeath(EntityDeathEvent e) {
        if (e.getEntity() instanceof Player) return;
        if (!e.getEntity().hasMetadata("trialmob") && !e.getEntity().hasMetadata("trialboss")) return;
        if (trials.isEmpty()) return;
        for (Trial trial : trials) {
            Wave wave = trial.getWave();
            if (wave == null) continue;

            int scoreMultiplier = (trial.getWaveNumber() / 5) + 1;

            if (wave.isBossWave()) {
                e.getDrops().clear();
                if (e.getEntity().hasMetadata("spawned")) {
                    trial.changeScore(scoreMultiplier * 10);
                    return;
                }

                if (!e.getEntity().hasMetadata("trialboss")) return;

                wave.getBoss().death();
                trial.changeScore(scoreMultiplier * 1000);

                if (trial.getWaveNumber() == trial.getMaxWave()) {
                    trial.completeTrial();
                    return;
                }

                trial.endWave();
                return;
            }

            if (wave.getWaveMobs().isEmpty()) {
                if (!trial.isActiveWave()) continue;
                trial.endWave();
                continue;
            }

            if (e.getEntity().hasMetadata("extramob")) {
                wave.removeExtraMob(e.getEntity());
                trial.changeScore(scoreMultiplier * 10);
            }
            else {
                WaveMob waveMob = wave.getWaveMob(e.getEntity());
                if (waveMob == null) continue;
                e.getDrops().clear();
                waveMob.dropItems();
                wave.removeWaveMob(waveMob);
                trial.changeScore(scoreMultiplier * 100);
            }

            if (wave.getMobsLeft() == 0) {
                if (trial.getWaveNumber() == trial.getMaxWave()) {
                    trial.completeTrial();
                    return;
                }
                trial.endWave();
            }
        }
    }

    @EventHandler
    public void onTrialMobDamage(EntityDamageEvent e) {
        if (trials.isEmpty()) return;

        // Check if it is a trial mob
        if (!e.getEntity().hasMetadata("trialmob") && !(e.getEntity().hasMetadata("trialmob"))) return;

        // See if a player damaged the mob
        if (!(e instanceof EntityDamageByEntityEvent)) e.setCancelled(true);
    }

    @EventHandler
    public void onTrialMobDamageByPlayer(EntityDamageByEntityEvent e) {
        if (trials.isEmpty()) return;

        // Check if it is a trial mob
        if (!e.getEntity().hasMetadata("trialmob") && !(e.getEntity().hasMetadata("trialboss"))) return;

        // Prevent fireball damage
        if (e.getDamager() instanceof Fireball) {
            e.setCancelled(true);
            return;
        }

        Player p = null;
        if (e.getDamager() instanceof Arrow arrow) {
            if (!(arrow.getShooter() instanceof Player)) {
                e.setCancelled(true);
                return;
            }
            p = (Player) arrow.getShooter();
        }
        else if (e.getDamager() instanceof Trident trident) {
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

        for (Trial trial : trials) {
            Wave wave = trial.getWave();
            if (wave == null) continue;
            if (wave.getWaveMobs().isEmpty()) continue;
            WaveMob waveMob = wave.getWaveMob(e.getEntity());
            if (waveMob == null) continue;
            if (!trial.getPlayer().equals(p)) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onTrialMobCombust(EntityCombustEvent e) {
        if (trials.isEmpty()) return;

        // Check if it is a trial mob
        if (!e.getEntity().hasMetadata("trialmob")) return;

        e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (trials.isEmpty()) return;
        for (Trial trial : trials) {
            if (!trial.getPlayer().equals(e.getEntity())) continue;
            trial.endTrial();
            e.getEntity().getInventory().clear();

            Player target = e.getEntity();
            for (Map.Entry<Player, Player> spectator : TrialManager.getTarget().entrySet()) {
                if (!spectator.getValue().equals(target)) continue;
                Player p = spectator.getKey();
                spectator.getValue().showPlayer(SurvivalSkills.getInstance(), p); // Show the spectator to the world
                if (p.getGameMode().equals(GameMode.SPECTATOR))
                    p.setSpectatorTarget(null);
                p.teleport(TrialManager.getSpectatingPlayers().get(p));
                TrialManager.getSpectatingPlayers().remove(p);
                TrialManager.getTarget().remove(p);
                p.setGameMode(GameMode.SURVIVAL);
                p.sendRawMessage(ChatColor.GREEN + "You are no longer spectating");
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                return;
            }
            return;
        }
    }

    @EventHandler
    public void onPlayerHurt(EntityDamageEvent e) {
        if (trials.isEmpty()) return;
        if (!(e.getEntity() instanceof Player p)) return;

        // Get the amount of damage and subtract it from the player's trial score
        double damage = e.getDamage();
        Trial trial = null;
        for (Trial t : trials) {
            if (!t.getPlayer().equals(p)) continue;
            trial = t;
        }
        if (trial == null) return;
        trial.changeScore((int) -damage);

        // Check if they were hit by a snowball
        if (!e.getCause().equals(EntityDamageEvent.DamageCause.PROJECTILE)) return;
        if (!(e instanceof EntityDamageByEntityEvent entityDamageByEntityEvent)) return;
        if (!(entityDamageByEntityEvent.getDamager() instanceof Snowball)) return;

        if (trial.getWave() == null) return;
        if (!trial.getWave().isBossWave()) return;

        // Freeze the player for 2 seconds
        float walkSpeed = p.getWalkSpeed();
        if (walkSpeed == 0) return;
        p.setWalkSpeed(0);
        Bukkit.getScheduler().runTaskLater(SurvivalSkills.getInstance(), () -> p.setWalkSpeed(walkSpeed), 40);
    }

    @EventHandler
    public void onRewardGUIClick(InventoryClickEvent e) {
        if (rewardInventories.contains(e.getView().getTopInventory()) && !rewardInventories.contains(e.getInventory())) {
            e.setCancelled(true);
            return;
        }
        if (!rewardInventories.contains(e.getInventory())) return;

        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;
        e.getWhoClicked().getInventory().addItem(e.getCurrentItem());
        e.getWhoClicked().closeInventory();
    }

    @EventHandler
    public void onUseEnchantedBook(InventoryClickEvent e) {
        if (e.getCurrentItem() == null) return;
        if (e.getCursor() == null) return;

        ItemStack item = e.getCurrentItem();
        ItemStack book = e.getCursor();
        if (!book.getType().equals(Material.ENCHANTED_BOOK)) return;
        if (item.getItemMeta() == null
                || !item.getItemMeta().getPersistentDataContainer().has(trialObjectKey, PersistentDataType.STRING))
            return;
        if (book.getItemMeta() == null
                || !item.getItemMeta().getPersistentDataContainer().has(trialObjectKey, PersistentDataType.STRING))
            return;

        if (item.getType().equals(Material.ENCHANTED_BOOK)) {
            EnchantmentStorageMeta itemMeta = (EnchantmentStorageMeta) item.getItemMeta();
            if (itemMeta == null) return;
            EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) book.getItemMeta();
            if (bookMeta == null) return;

            // Apply the book enchant to the item
            for (Map.Entry<Enchantment, Integer> enchant : bookMeta.getStoredEnchants().entrySet()) {
                if (item.getEnchantments().containsKey(enchant.getKey())
                        && Objects.equals(item.getEnchantments().get(enchant.getKey()), enchant.getValue())) {
                    itemMeta.removeStoredEnchant(enchant.getKey());
                    itemMeta.addEnchant(enchant.getKey(), enchant.getValue() + 1, true);
                    continue;
                }
                itemMeta.addStoredEnchant(enchant.getKey(), enchant.getValue(), true);
            }
            item.setItemMeta(itemMeta);
            e.setCancelled(true);
            e.getWhoClicked().setItemOnCursor(null);
            return;
        }

        EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) book.getItemMeta();
        if (bookMeta == null) return;

        // Apply the book enchant to the item
        for (Map.Entry<Enchantment, Integer> enchant : bookMeta.getStoredEnchants().entrySet()) {
            if (item.getEnchantments().containsKey(enchant.getKey())
                    && Objects.equals(item.getEnchantments().get(enchant.getKey()), enchant.getValue())) {
                item.addEnchantment(enchant.getKey(), enchant.getValue() + 1);
                return;
            }
            item.addEnchantment(enchant.getKey(), enchant.getValue());
        }
        e.setCancelled(true);
        e.getWhoClicked().setItemOnCursor(null);
    }

    @EventHandler
    public void onRewardGUIDrag(InventoryDragEvent e) {
        if (rewardInventories.contains(e.getView().getTopInventory())) {
            e.setCancelled(true);
            return;
        }
        if (!rewardInventories.contains(e.getInventory())) return;

        e.setCancelled(true);
        e.getWhoClicked().getInventory().addItem(e.getOldCursor());
        e.getWhoClicked().closeInventory();
    }

    @EventHandler
    public void onRewardGUIClose(InventoryCloseEvent e) {
        if (!rewardInventories.contains(e.getInventory())) return;
        rewardInventories.remove(e.getInventory());
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;

        ItemStack item = e.getItem().getItemStack();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        boolean playerInTrial = false;
        for (Trial trial : trials) {
            if (!trial.getPlayer().equals(p)) continue;
            playerInTrial = true;
        }

        // If a player is not in a trial, they cannot pick up trial items
        if (!playerInTrial) {
            if (meta.getPersistentDataContainer().has(trialObjectKey, PersistentDataType.STRING)) e.setCancelled(true);
            return;
        }

        // If a player is in a trial, they can only pick up trial items
        if (!meta.getPersistentDataContainer().has(trialObjectKey, PersistentDataType.STRING)) e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerLeaveTrialArea(PlayerMoveEvent e) {
        if (trials.isEmpty()) return;
        for (Trial trial : trials) {
            if (!trial.getPlayer().equals(e.getPlayer())) continue;
            if (trial.getProtectedArea().boundingBox().contains(e.getPlayer().getLocation().toVector())) continue;
            if (!trial.isBuildingCreated()) continue;
            trial.endTrial();
            return;
        }
    }

    @EventHandler
    public void onBeaconEffect(EntityPotionEffectEvent e) {
        if (trials.isEmpty()) return;

        if (!(e.getEntity() instanceof Player p)) return;
        EntityPotionEffectEvent.Cause cause = e.getCause();
        if (!cause.equals(EntityPotionEffectEvent.Cause.BEACON)
                && !cause.equals(EntityPotionEffectEvent.Cause.COMMAND)
                && !cause.equals(EntityPotionEffectEvent.Cause.PLUGIN)
                && !cause.equals(EntityPotionEffectEvent.Cause.POTION_SPLASH)) return;

        for (Trial trial : trials) {
            if (!trial.getPlayer().equals(p)) continue;
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onSlimeSplit(SlimeSplitEvent e) {
        if (!e.getEntity().hasMetadata("trialmob")) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent e) {
        Entity entity = e.getEntity();

        if (!entity.hasMetadata("trialmob")) return;

        // Ensure that the mobs can only target the trial player near them
        if (!(e.getTarget() instanceof Player)) {
            e.setCancelled(true);
            return;
        }

        // Ensure they are targeting the right trial player
        for (Trial trial : trials)
            if (trial.getPlayer().equals(e.getTarget())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onEvokerSpellCast(EntitySpellCastEvent e) {
        // Check if the event is triggered by an Evoker
        Entity caster = e.getEntity();
        if (caster instanceof Evoker) {
            if (!caster.hasMetadata("trialmob")) return;
            // Loop through spawned entities
            for (Entity spawnedEntity : caster.getWorld().getEntities()) {
                spawnedEntity.setMetadata("trialmob", new FixedMetadataValue(SurvivalSkills.getInstance(), true));
                spawnedEntity.setMetadata("extramob", new FixedMetadataValue(SurvivalSkills.getInstance(), true));

                // Find the trial the mob belongs to
                for (Trial trial : trials) {
                    if (trial.getWave() == null) continue;
                    if (trial.getWave().getWaveMob(spawnedEntity) == null) continue;
                    // Add the mob to the extra entities of the wave
                    trial.getWave().addExtraMob(spawnedEntity);
                }
            }
        }
    }

    @EventHandler
    public void onEntityTransform(EntityTransformEvent e) {
        if (e.getEntity().hasMetadata("trialmob")) e.setCancelled(true);
        // Preserve the held item
        LivingEntity entity = (LivingEntity) e.getEntity();
        if (entity.getEquipment() == null) return;
        ItemStack mainHandItem = entity.getEquipment().getItemInMainHand();

        new BukkitRunnable() {
            @Override
            public void run() {
                entity.getEquipment().setItemInMainHand(mainHandItem);
            }
        }.runTaskLater(SurvivalSkills.getInstance(), 1);
    }

    @EventHandler
    public void onEndermanTeleport(EntityTeleportEvent e) {
        // Check if the entity is an Enderman
        if (!e.getEntity().hasMetadata("trialmob")) return;
        if (e.getEntity() instanceof Enderman) e.setCancelled(true);
    }

    public static boolean isTrialPlayer(Player p) {
        for (Trial trial : trials) if (trial.getPlayer().equals(p)) return true;
        return false;
    }

    public static void loadProtectedAreas() {
        File file = new File(SurvivalSkills.getInstance().getDataFolder(), "godquests.yml");
        if (!file.exists()) SurvivalSkills.getInstance().saveResource("godquests.yml", true);
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection section = data.getConfigurationSection("");
        if (section == null) return;
        if (section.getKeys(false).isEmpty()) return;

        for (String key : section.getKeys(false)) {
            // Get the min and max boundaries of the bounding box
            if (!data.contains(key + ".ProtectedArea")) continue;
            String minString = (String) data.get(key + ".ProtectedArea.Min");
            if (minString == null) continue;
            String maxString = (String) data.get(key + ".ProtectedArea.Max");
            if (maxString == null) continue;
            String[] minLocation = minString.split(":");
            String[] maxLocation = maxString.split(":");
            if (minLocation.length != 3 || maxLocation.length != 3) continue;

            BoundingBox box = new BoundingBox();
            box.resize(Double.parseDouble(minLocation[0]), Double.parseDouble(minLocation[1]), Double.parseDouble(minLocation[2]),
                    Double.parseDouble(maxLocation[0]), Double.parseDouble(maxLocation[1]), Double.parseDouble(maxLocation[2]));

            String worldString = (String) data.get(key + ".ProtectedArea.World");
            if (worldString == null) continue;
            World world = Bukkit.getWorld(UUID.fromString(worldString));

            ProtectedArea protectedArea = new ProtectedArea(box, world);

            UUID uuid = UUID.fromString(key);
            protectedAreas.put(uuid, protectedArea);
        }
    }

    public static void handleTrials() {
        // End existing trials
        if (!trials.isEmpty()) {
            ArrayList<Trial> trials = new ArrayList<>(TrialManager.trials);
            for (Trial trial : trials) trial.endTrial();
        }

        if (protectedAreas.isEmpty()) return;

        // Save the protected areas to the config
        File file = new File(SurvivalSkills.getInstance().getDataFolder(), "godquests.yml");
        if (!file.exists()) SurvivalSkills.getInstance().saveResource("godquests.yml", true);
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);

        for (Map.Entry<UUID, ProtectedArea> protectedArea : protectedAreas.entrySet()) {
            String minLocation = protectedArea.getValue().boundingBox().getMinX() + ":"
                    + protectedArea.getValue().boundingBox().getMinY() + ":" + protectedArea.getValue().boundingBox().getMinZ();
            String maxLocation = protectedArea.getValue().boundingBox().getMaxX() + ":"
                    + protectedArea.getValue().boundingBox().getMaxY() + ":" + protectedArea.getValue().boundingBox().getMaxZ();
            data.set(protectedArea.getKey().toString() + ".ProtectedArea.Min", minLocation);
            data.set(protectedArea.getKey().toString() + ".ProtectedArea.Max", maxLocation);
            data.set(protectedArea.getKey().toString() + ".ProtectedArea.World", protectedArea.getValue().world().getUID().toString());
        }

        try {
            data.save(file);
        } catch (Exception e) {
            SurvivalSkills.getInstance().getLogger().warning("Failed to save protected areas to godquests.yml");
        }
    }

    public static Wave spawnWave(int wave, ArrayList<Location> spawningSpots, Player target) {
        if (waves.get(wave) == null) return null;
        Wave waveObject = waves.get(wave).duplicate();
        if (waveObject == null) {
            SurvivalSkills.getInstance().getLogger().warning("Wave " + wave + " does not exist");
            return null;
        }

        if (waveObject.isBossWave()) {
            TrialBoss boss = waveObject.getBoss();

            boss.setSpawnLocations(spawningSpots);
            boss.spawn(spawningSpots.get((int) (Math.random() * spawningSpots.size())));
            boss.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);

            return waveObject;
        }

        for (WaveMob waveMob : waveObject.getWaveMobs()) {
            Location location = getRandomSpawningSpot(spawningSpots);
            waveObject.spawnMob(location, waveMob, target);
        }

        return waveObject;
    }

    public static Location getRandomSpawningSpot(ArrayList<Location> spawningSpots) {
        return spawningSpots.get((int) (Math.random() * spawningSpots.size()));
    }

    public static void openRewardGUI(Player p, int wave) {
        // Open a GUI with rewards
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.GOLD + "Wave " + wave + " Rewards");
        int stage = (wave / 5) + 1;
        if (!lootTables.containsKey(stage)) {
            Bukkit.getLogger().warning("No loot table found for stage " + stage);
            return;
        }
        ArrayList<ItemStack> rewards = lootTables.get(stage).getItems(3);
        inv.setItem(2, rewards.get(0));
        inv.setItem(4, rewards.get(1));
        inv.setItem(6, rewards.get(2));

        rewardInventories.add(inv);
        p.openInventory(inv);
    }

    public void createWaves() {
        // Wave 1
        Wave wave1 = new Wave();
        HashMap<ItemStack, Double> zombieDrops = new HashMap<>();
        zombieDrops.put(getTrialItem(Material.STONE_SWORD, 1), 0.1);
        WaveMob zombie = new WaveMob(ChatColor.GREEN + "Zombie", EntityType.ZOMBIE, 15, 3,
                0.3, 1, null, null, zombieDrops);
        wave1.addWaveMob(zombie.duplicate());
        wave1.addWaveMob(zombie.duplicate());
        wave1.addWaveMob(zombie.duplicate());
        waves.put(1, wave1);

        // Wave 2
        Wave wave2 = new Wave();
        HashMap<ItemStack, Double> skeletonDrops = new HashMap<>();
        skeletonDrops.put(getTrialItem(Material.BOW, 1), 0.1);
        skeletonDrops.put(getTrialItem(Material.ARROW, 8), 0.1);
        WaveMob weakSkeleton = new WaveMob(ChatColor.GRAY + "Weak Skeleton", EntityType.SKELETON, 10, 5,
                0.2, 1, new ItemStack(Material.BOW), null, skeletonDrops);
        wave2.addWaveMob(weakSkeleton.duplicate(), 3);
        waves.put(2, wave2);

        // Wave 3
        Wave wave3 = new Wave();
        WaveMob spider = new WaveMob(ChatColor.BLACK + "Spider", EntityType.SPIDER, 20, 5,
                0.3, 1.25, null, null, null);
        wave3.addWaveMob(spider.duplicate(), 5);
        waves.put(3, wave3);

        // Wave 4
        Wave wave4 = new Wave();
        WaveMob weakZombie = new WaveMob(ChatColor.GREEN + "Weak Zombie", EntityType.ZOMBIE, 10, 3,
                0.2, 1, null, null, zombieDrops);
        WaveMob skeleton = new WaveMob(ChatColor.GRAY + "Skeleton", EntityType.SKELETON, 15, 2,
                0.2, 1, new ItemStack(Material.BOW), null, skeletonDrops);
        wave4.addWaveMob(weakZombie.duplicate(), 3);
        wave4.addWaveMob(skeleton.duplicate(), 2);
        waves.put(4, wave4);

        // Wave 5
        Wave wave5 = new Wave();
        HashMap<ItemStack, Double> knightDrops = new HashMap<>();
        knightDrops.put(getTrialItem(Material.IRON_SWORD, 1), 0.1);
        knightDrops.put(getTrialItem(Material.IRON_BOOTS, 1), 0.1);
        knightDrops.put(getTrialItem(Material.IRON_LEGGINGS, 1), 0.1);
        knightDrops.put(getTrialItem(Material.IRON_CHESTPLATE, 1), 0.1);
        knightDrops.put(getTrialItem(Material.IRON_HELMET, 1), 0.1);
        ItemStack[] armor = new ItemStack[4];
        armor[0] = getTrialItem(Material.IRON_BOOTS, 1);
        armor[1] = getTrialItem(Material.IRON_LEGGINGS, 1);
        armor[2] = getTrialItem(Material.IRON_CHESTPLATE, 1);
        armor[3] = getTrialItem(Material.IRON_HELMET, 1);
        WaveMob zombieBoss = new WaveMob(ChatColor.GREEN + "Zombie Knight", EntityType.ZOMBIE, 25, 5,
                0.2, 1.25, getTrialItem(Material.IRON_SWORD, 1), armor, knightDrops);
        wave5.addWaveMob(zombieBoss);
        wave5.addWaveMob(zombie.duplicate(), 3);
        waves.put(5, wave5);

        // Wave 6
        Wave wave6 = new Wave();
        WaveMob stray = new WaveMob(ChatColor.GRAY + "Stray", EntityType.STRAY, 20, 5,
                0.2, 1, new ItemStack(Material.BOW), null, skeletonDrops);
        wave6.addWaveMob(stray.duplicate(), 3);
        wave6.addWaveMob(spider.duplicate(), 3);
        waves.put(6, wave6);

        // Wave 7
        Wave wave7 = new Wave();
        WaveMob husk = new WaveMob(ChatColor.GOLD + "Husk", EntityType.HUSK, 30, 9,
                0.2, 1, null, null, null);
        wave7.addWaveMob(husk.duplicate(), 6);
        waves.put(7, wave7);

        // Wave 8
        Wave wave8 = new Wave();
        HashMap<ItemStack, Double> creeperDrops = new HashMap<>();
        creeperDrops.put(getTrialItem(Material.BREAD, 4), 0.25);
        WaveMob creeper = new WaveMob(ChatColor.GREEN + "Creeper", EntityType.CREEPER, 20, 5,
                0.3, 1, null, null, creeperDrops);
        wave8.addWaveMob(creeper.duplicate(), 4);
        wave8.addWaveMob(weakSkeleton.duplicate(), 2);
        waves.put(8, wave8);

        // Wave 9
        Wave wave9 = new Wave();
        HashMap<ItemStack, Double> weaponBooks = new HashMap<>();
        weaponBooks.put(getEBook(Enchantment.SHARPNESS), 0.1);
        weaponBooks.put(getEBook(Enchantment.SMITE), 0.1);
        weaponBooks.put(getEBook(Enchantment.BANE_OF_ARTHROPODS), 0.1);
        weaponBooks.put(getEBook(Enchantment.SWEEPING_EDGE), 0.1);
        weaponBooks.put(getEBook(Enchantment.KNOCKBACK), 0.1);
        weaponBooks.put(getEBook(Enchantment.FIRE_ASPECT), 0.1);
        weaponBooks.put(getEBook(Enchantment.POWER), 0.1);
        WaveMob silverfish = new WaveMob(ChatColor.GRAY + "Silverfish", EntityType.SILVERFISH, 7, 3,
                0.3, 1, null, null, weaponBooks);
        WaveMob endermite = new WaveMob(ChatColor.DARK_PURPLE + "Endermite", EntityType.ENDERMITE, 7, 3,
                0.3, 1, null, null, weaponBooks);
        wave9.addWaveMob(silverfish.duplicate(), 6);
        wave9.addWaveMob(endermite.duplicate(), 6);
        waves.put(9, wave9);

        // Wave 10
        Wave wave10 = new Wave();
        wave10.setBossWave(true);
        HashMap<ItemStack, Double> armorBooks = new HashMap<>();
        armorBooks.put(getEBook(Enchantment.PROTECTION), 0.1);
        armorBooks.put(getEBook(Enchantment.FIRE_PROTECTION), 0.1);
        armorBooks.put(getEBook(Enchantment.BLAST_PROTECTION), 0.1);
        armorBooks.put(getEBook(Enchantment.PROJECTILE_PROTECTION), 0.1);
        BlazingGhast ghast = new BlazingGhast(armorBooks);
        wave10.setBoss(ghast);
        waves.put(10, wave10);

        // Wave 11
        Wave wave11 = new Wave();
        HashMap<ItemStack, Double> crossbowDrops = new HashMap<>();
        crossbowDrops.put(getTrialItem(Material.CROSSBOW, 1), 0.1);
        crossbowDrops.put(getTrialItem(Material.ARROW, 16), 0.1);
        HashMap<ItemStack, Double> vindicatorDrops = new HashMap<>();
        vindicatorDrops.put(getTrialItem(Material.IRON_AXE, 1), 0.2);
        WaveMob pillager = new WaveMob(ChatColor.GRAY + "Pillager", EntityType.PILLAGER, 20, 5,
                0.2, 1, new ItemStack(Material.CROSSBOW), null, crossbowDrops);
        WaveMob vindicator = new WaveMob(ChatColor.DARK_GREEN + "Vindicator", EntityType.VINDICATOR, 25, 7,
                0.3, 1, new ItemStack(Material.IRON_AXE), null, vindicatorDrops);
        wave11.addWaveMob(pillager.duplicate(), 4);
        wave11.addWaveMob(vindicator.duplicate(), 2);
        waves.put(11, wave11);

        // Wave 12
        Wave wave12 = new Wave();
        HashMap<ItemStack, Double> tridentDrops = new HashMap<>();
        tridentDrops.put(getTrialItem(Material.TRIDENT, 1), 0.1);
        WaveMob drowned = new WaveMob(ChatColor.DARK_BLUE + "Drowned", EntityType.DROWNED, 25, 7,
                0.2, 1, new ItemStack(Material.TRIDENT), null, tridentDrops);
        wave12.addWaveMob(drowned.duplicate(), 3);
        waves.put(12, wave12);

        // Wave 13
        Wave wave13 = new Wave();
        WaveMob evoker = new WaveMob(ChatColor.LIGHT_PURPLE + "Evoker", EntityType.EVOKER, 30, 9,
                0.2, 1, null, null, armorBooks);
        WaveMob caveSpider = new WaveMob(ChatColor.DARK_GREEN + "Cave Spider", EntityType.CAVE_SPIDER, 20, 5,
                0.3, 1, null, null, null);
        wave13.addWaveMob(evoker.duplicate());
        wave13.addWaveMob(caveSpider.duplicate(), 4);
        wave13.addWaveMob(pillager.duplicate(), 2);
        waves.put(13, wave13);

        // Wave 14
        Wave wave14 = new Wave();
        WaveMob phantom = new WaveMob(ChatColor.DARK_PURPLE + "Phantom", EntityType.PHANTOM, 20, 5,
                0.3, 1, null, null, null);
        wave14.addWaveMob(phantom.duplicate(), 8);
        waves.put(14, wave14);

        // Wave 15
        Wave wave15 = new Wave();
        FrostRevenant frostRevenant = new FrostRevenant(weaponBooks);
        wave15.setBossWave(true);
        wave15.setBoss(frostRevenant);
        waves.put(15, wave15);

        // Wave 16
        Wave wave16 = new Wave();
        HashMap<ItemStack, Double> meatDrops = new HashMap<>();
        meatDrops.put(getTrialItem(Material.COOKED_BEEF, 4), 0.1);
        WaveMob witherSkeleton = new WaveMob(ChatColor.BLACK + "Wither Skeleton", EntityType.WITHER_SKELETON, 20, 7,
                0.35, 1, new ItemStack(Material.STONE_SWORD), null, null);
        WaveMob piglinBrute = new WaveMob(ChatColor.GOLD + "Piglin Brute", EntityType.PIGLIN_BRUTE, 30, 9,
                0.3, 1, new ItemStack(Material.GOLDEN_AXE), null, meatDrops);
        wave16.addWaveMob(witherSkeleton.duplicate(), 3);
        wave16.addWaveMob(piglinBrute.duplicate(), 3);
        waves.put(16, wave16);

        // Wave 17
        Wave wave17 = new Wave();
        WaveMob ravager = new WaveMob(ChatColor.DARK_RED + "Ravager", EntityType.RAVAGER, 40, 11,
                0.1, 1, null, null, meatDrops);
        wave17.addWaveMob(ravager.duplicate(), 2);
        wave17.addWaveMob(pillager.duplicate(), 4);
        waves.put(17, wave17);

        // Wave 18
        Wave wave18 = new Wave();
        WaveMob enderman = new WaveMob(ChatColor.DARK_PURPLE + "Enderman", EntityType.ENDERMAN, 30, 9,
                0.25, 0.75, null, null, null);
        wave18.addWaveMob(enderman.duplicate(), 4);
        waves.put(18, wave18);

        // Wave 19
        Wave wave19 = new Wave();
        WaveMob blaze = new WaveMob(ChatColor.RED + "Blaze", EntityType.BLAZE, 25, 7,
                0.3, 1, null, null, null);
        wave19.addWaveMob(blaze.duplicate(), 4);
        wave19.addWaveMob(witherSkeleton.duplicate(), 2);
        wave19.addWaveMob(evoker.duplicate());
        waves.put(19, wave19);

        // Wave 20
        Wave wave20 = new Wave();
        wave20.setBossWave(true);
        HellsGatekeeper gatekeeper = new HellsGatekeeper(null);
        wave20.setBoss(gatekeeper);
        waves.put(20, wave20);
    }

    public void createRewards() {
        // Stage 1 Rewards
        TrialLootTable stage1Rewards = new TrialLootTable();
        stage1Rewards.addItem(getTrialItem(Material.WOODEN_SWORD, 1), 3);
        stage1Rewards.addItem(getTrialItem(Material.STONE_SWORD, 1), 2);
        stage1Rewards.addItem(getTrialItem(Material.IRON_SWORD, 1), 1);
        stage1Rewards.addItem(getTrialItem(Material.WOODEN_AXE, 1), 2);
        stage1Rewards.addItem(getTrialItem(Material.COOKED_BEEF, 3), 4);
        stage1Rewards.addItem(getTrialItem(Material.LEATHER_BOOTS, 1), 2);
        stage1Rewards.addItem(getTrialItem(Material.LEATHER_CHESTPLATE, 1), 2);
        stage1Rewards.addItem(getTrialItem(Material.LEATHER_HELMET, 1), 2);
        stage1Rewards.addItem(getTrialItem(Material.LEATHER_LEGGINGS, 1), 2);
        stage1Rewards.addItem(getTrialItem(Material.GOLDEN_APPLE, 2), 1);
        stage1Rewards.addItem(getTrialItem(Material.SHIELD, 1), 1);
        stage1Rewards.addItem(getTrialItem(Material.ARROW, 32), 1);
        lootTables.put(1, stage1Rewards);

        // Stage 2 Rewards
        TrialLootTable stage2Rewards = new TrialLootTable();
        stage2Rewards.addItem(getTrialItem(Material.STONE_SWORD, 1), 3);
        stage2Rewards.addItem(getTrialItem(Material.IRON_SWORD, 1), 2);
        stage2Rewards.addItem(getTrialItem(Material.DIAMOND_SWORD, 1), 1);
        stage2Rewards.addItem(getTrialItem(Material.STONE_AXE, 1), 3);
        stage2Rewards.addItem(getTrialItem(Material.IRON_AXE, 1), 1);
        stage2Rewards.addItem(getTrialItem(Material.COOKED_BEEF, 5), 4);
        stage2Rewards.addItem(getTrialItem(Material.CHAINMAIL_BOOTS, 1), 3);
        stage2Rewards.addItem(getTrialItem(Material.CHAINMAIL_CHESTPLATE, 1), 3);
        stage2Rewards.addItem(getTrialItem(Material.CHAINMAIL_HELMET, 1), 3);
        stage2Rewards.addItem(getTrialItem(Material.CHAINMAIL_LEGGINGS, 1), 3);
        stage2Rewards.addItem(getTrialItem(Material.IRON_HELMET, 1), 1);
        stage2Rewards.addItem(getTrialItem(Material.IRON_CHESTPLATE, 1), 1);
        stage2Rewards.addItem(getTrialItem(Material.IRON_LEGGINGS, 1), 1);
        stage2Rewards.addItem(getTrialItem(Material.IRON_BOOTS, 1), 1);
        stage2Rewards.addItem(getTrialItem(Material.GOLDEN_APPLE, 3), 1);
        stage2Rewards.addItem(getTrialItem(Material.SHIELD, 1), 2);
        stage2Rewards.addItem(getTrialItem(Material.ARROW, 64), 1);
        stage2Rewards.addItem(getTrialItem(Material.BOW, 1), 1);
        lootTables.put(2, stage2Rewards);

        // Stage 3 Rewards
        TrialLootTable stage3Rewards = new TrialLootTable();
        stage3Rewards.addItem(getTrialItem(Material.IRON_SWORD, 1), 3);
        stage3Rewards.addItem(getTrialItem(Material.DIAMOND_SWORD, 1), 2);
        stage3Rewards.addItem(getTrialItem(Material.NETHERITE_SWORD, 1), 1);
        stage3Rewards.addItem(getTrialItem(Material.IRON_AXE, 1), 3);
        stage3Rewards.addItem(getTrialItem(Material.DIAMOND_AXE, 1), 1);
        stage3Rewards.addItem(getTrialItem(Material.COOKED_BEEF, 7), 3);
        stage3Rewards.addItem(getTrialItem(Material.IRON_BOOTS, 1), 3);
        stage3Rewards.addItem(getTrialItem(Material.IRON_CHESTPLATE, 1), 3);
        stage3Rewards.addItem(getTrialItem(Material.IRON_HELMET, 1), 3);
        stage3Rewards.addItem(getTrialItem(Material.IRON_LEGGINGS, 1), 3);
        stage3Rewards.addItem(getTrialItem(Material.DIAMOND_BOOTS, 1), 1);
        stage3Rewards.addItem(getTrialItem(Material.DIAMOND_CHESTPLATE, 1), 1);
        stage3Rewards.addItem(getTrialItem(Material.DIAMOND_HELMET, 1), 1);
        stage3Rewards.addItem(getTrialItem(Material.DIAMOND_LEGGINGS, 1), 1);
        stage3Rewards.addItem(getTrialItem(Material.CROSSBOW, 1), 2);
        stage3Rewards.addItem(getTrialItem(Material.BOW, 1), 3);
        stage3Rewards.addItem(getTrialItem(Material.ARROW, 64), 2);
        stage3Rewards.addItem(getTrialItem(Material.SHIELD, 2), 2);
        stage3Rewards.addItem(getTrialItem(Material.GOLDEN_APPLE, 5), 1);
        lootTables.put(3, stage3Rewards);

        // Stage 4 Rewards
        TrialLootTable stage4Rewards = new TrialLootTable();
        stage4Rewards.addItem(getTrialItem(Material.DIAMOND_SWORD, 1), 3);
        stage4Rewards.addItem(getTrialItem(Material.NETHERITE_SWORD, 1), 2);
        stage4Rewards.addItem(getTrialItem(Material.DIAMOND_AXE, 1), 3);
        stage4Rewards.addItem(getTrialItem(Material.NETHERITE_AXE, 1), 1);
        stage4Rewards.addItem(getTrialItem(Material.COOKED_BEEF, 16), 3);
        stage4Rewards.addItem(getTrialItem(Material.DIAMOND_BOOTS, 1), 3);
        stage4Rewards.addItem(getTrialItem(Material.DIAMOND_CHESTPLATE, 1), 3);
        stage4Rewards.addItem(getTrialItem(Material.DIAMOND_HELMET, 1), 3);
        stage4Rewards.addItem(getTrialItem(Material.DIAMOND_LEGGINGS, 1), 3);
        stage4Rewards.addItem(getTrialItem(Material.NETHERITE_BOOTS, 1), 1);
        stage4Rewards.addItem(getTrialItem(Material.NETHERITE_CHESTPLATE, 1), 1);
        stage4Rewards.addItem(getTrialItem(Material.NETHERITE_HELMET, 1), 1);
        stage4Rewards.addItem(getTrialItem(Material.NETHERITE_LEGGINGS, 1), 1);
        stage4Rewards.addItem(getTrialItem(Material.CROSSBOW, 1), 3);
        stage4Rewards.addItem(getTrialItem(Material.BOW, 1), 3);
        stage4Rewards.addItem(getTrialItem(Material.ARROW, 64), 3);
        stage4Rewards.addItem(getTrialItem(Material.GOLDEN_APPLE, 8), 1);
        stage4Rewards.addItem(getTrialItem(Material.ENCHANTED_GOLDEN_APPLE, 1), 1);
        lootTables.put(4, stage4Rewards);
    }

    public static boolean isInTrial(Player p) {
        if (trials.isEmpty()) return false;
        for (Trial trial : trials) if (trial.getPlayer().equals(p)) return true;
        return false;
    }

    public static ItemStack getTrialItem(Material mat, int amount) {
        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.getPersistentDataContainer().set(trialObjectKey, PersistentDataType.STRING, "trialitem");
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack getEBook(Enchantment ench) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
        if (meta == null) return item;
        meta.addStoredEnchant(ench, 1, true);
        meta.getPersistentDataContainer().set(trialObjectKey, PersistentDataType.STRING, "trialitem");
        item.setItemMeta(meta);
        return item;
    }

    public static ArrayList<Trial> getTrials() {
        return trials;
    }

    public static HashMap<UUID, ProtectedArea> getProtectedAreas() {
        return protectedAreas;
    }

    public static NamespacedKey getTrialObjectKey() {
        return trialObjectKey;
    }

    public static HashMap<Player, Location> getSpectatingPlayers() {
        return spectatingPlayers;
    }

    public static HashMap<Player, Player> getTarget() {
        return target;
    }

    public static HashMap<Player, Scoreboard> getTrialScoreboards() {
        return trialScoreboards;
    }
}
