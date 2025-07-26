package sir_draco.survivalskills.god_questline;

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
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.BoundingBox;
import sir_draco.survivalskills.god_questline.TrialMobs.*;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.TrialUtils;

import java.io.File;
import java.util.*;

public class TrialManager implements Listener {

    private static final HashMap<Player, PendingTrial> pendingTrials = new HashMap<>();
    private static final ArrayList<Trial> trials = new ArrayList<>();
    private static final ArrayList<Inventory> rewardInventories = new ArrayList<>();
    private static final ArrayList<Inventory> trialSelectionInventories = new ArrayList<>();
    private static final HashMap<UUID, ProtectedArea> protectedAreas = new HashMap<>();
    private static final HashMap<Integer, TrialLootTable> lootTables = new HashMap<>();
    private static final HashMap<Player, Location> spectatingPlayers = new HashMap<>();
    private static final HashMap<Player, Player> spectatorTargets = new HashMap<>();
    private static final HashMap<Player, Scoreboard> trialScoreboards = new HashMap<>();
    private static final HashMap<Player, Scoreboard> spectatorScoreboards = new HashMap<>();
    private static final HashMap<Player, ArrayList<Integer>> playerGamemodesBeaten = new HashMap<>();
    private static final NamespacedKey trialObjectKey = new NamespacedKey(SurvivalSkills.getInstance(), "trialobject");
    private static final WaveGenerator waveGenerator = new WaveGenerator();
    private static final HashMap<UUID, TrialBuildingData> trialBuildingOwnership = new HashMap<>();
    private static final HashMap<UUID, Long> trialBuildingCreationCooldownList = new HashMap<>();

    private static FileConfiguration trialBuildingConfig = null;
    private static FileConfiguration trialDataConfig = null;

    private static final long CLEANUP_INTERVAL = 60 * 20L; // 1 minute in ticks

    public TrialManager() {
        createRewards();
        loadTrialBuildingData();
        startCleanupTask();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        loadCompletedTrials(e.getPlayer());
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
        if (!isTrialPlayer(e.getPlayer())) return;
        // Check if the first letters of the message are "/godtrial"
        if (e.getMessage().toLowerCase().startsWith("/godtrial")) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onSpectatorSneak(PlayerToggleSneakEvent e) {
        // Prevent spectators from crouching which would exit first person view
        if (spectatingPlayers.containsKey(e.getPlayer()) && e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            e.setCancelled(true);

            // Ensure they remain locked to their spectator target
            Player target = spectatorTargets.get(e.getPlayer());
            if (target != null) {
                e.getPlayer().setSpectatorTarget(target);
            }
        }
    }

    @EventHandler
    public void onSpectatorGameModeChange(PlayerGameModeChangeEvent e) {
        // Prevent spectators from changing game modes while spectating
        if (spectatingPlayers.containsKey(e.getPlayer()) && e.getNewGameMode() != GameMode.SPECTATOR) {
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
        if (e.getEntity() instanceof Player) return;

        // Check if it is a trial mob
        if (!e.getEntity().hasMetadata("trialmob") && !e.getEntity().hasMetadata("trialboss")) return;

        // See if a player damaged the mob
        if (!(e instanceof EntityDamageByEntityEvent)) e.setCancelled(true);
    }

    @EventHandler
    public void onTrialMobDamageByPlayer(EntityDamageByEntityEvent e) {
        if (trials.isEmpty()) return;
        if (e.getEntity() instanceof Player) return;

        // Check if it is a trial mob
        if (!e.getEntity().hasMetadata("trialmob") && !(e.getEntity().hasMetadata("trialboss"))) return;

        // Prevent fireball damage
        if (e.getDamager() instanceof Fireball) {
            e.setCancelled(true);
            return;
        }

        Player p = null;
        switch (e.getDamager()) {
            case Arrow arrow -> {
                if (!(arrow.getShooter() instanceof Player)) {
                    e.setCancelled(true);
                    return;
                }
                p = (Player) arrow.getShooter();
            }
            case Trident trident -> {
                if (!(trident.getShooter() instanceof Player)) {
                    e.setCancelled(true);
                    return;
                }
                p = (Player) trident.getShooter();
            }
            case Player player -> p = player;
            default -> {}
        }

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
            if (!trial.getPlayers().contains(p)) {
                e.setCancelled(true);
                return;
            }
        }

        // Apply damage multiplier based on trial upgrades
        e.setDamage(e.getDamage() * TrialTree.getDamageMultiplier(TrialUpgradeManager.getPlayerUpgrades(p)));
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
            if (!trial.getPlayers().contains(e.getEntity())) continue;
            trial.endTrial();
            e.getEntity().getInventory().clear();
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
            if (!t.getPlayers().contains(p)) continue;
            trial = t;
            break;
        }
        if (trial == null) return;
        trial.changeScore((int) -damage * 2);
        if (Math.random() < TrialTree.getDodgeChance(TrialUpgradeManager.getPlayerUpgrades(p))) {
            e.setCancelled(true);
            return;
        }

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
                Enchantment enchantment = enchant.getKey();
                if (item.getEnchantments().containsKey(enchantment)
                        && Objects.equals(item.getEnchantments().get(enchantment), enchant.getValue())) {
                    itemMeta.removeStoredEnchant(enchantment);
                    itemMeta.addEnchant(enchantment, enchant.getValue() + 1, true);
                    continue;
                }
                itemMeta.addStoredEnchant(enchantment, enchant.getValue(), true);
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
        e.getWhoClicked().setItemOnCursor(null);
        e.setCancelled(true);
    }

    @EventHandler
    public void onTrialSelectionClick(InventoryClickEvent e) {
        if (trialSelectionInventories.contains(e.getView().getTopInventory())
                && !trialSelectionInventories.contains(e.getInventory())) {
            e.setCancelled(true);
            return;
        }

        if (!trialSelectionInventories.contains(e.getInventory())) return;
        e.setCancelled(true);
        TrialUtils.handleTrialSelectionClick(e.getClickedInventory(), (Player) e.getWhoClicked(), e.getCurrentItem());
    }

    @EventHandler
    public void onTrialSelectionDrag(InventoryDragEvent e) {
        if (trialSelectionInventories.contains(e.getView().getTopInventory())
                && !trialSelectionInventories.contains(e.getInventory())) {
            e.setCancelled(true);
            return;
        }

        if (!trialSelectionInventories.contains(e.getInventory())) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onTrialPartyInventoryClose(InventoryCloseEvent e) {
        if (pendingTrials.isEmpty()) return;
        Player p = (Player) e.getPlayer();
        if (!pendingTrials.containsKey(p)) return;

        new BukkitRunnable() {

            @Override
            public void run() {
                // Check if the player has any inventory open
                if (p.getOpenInventory().getTopInventory().getSize() != 9) {
                    if (pendingTrials.containsKey(p)) pendingTrials.get(p).endPendingTrial();
                    pendingTrials.remove(p);
                }
            }
        }.runTaskLater(SurvivalSkills.getInstance(), 40);
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
            if (!trial.getPlayers().contains(p)) continue;
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
            if (!trial.getPlayers().contains(e.getPlayer())) continue;
            if (trial.getProtectedArea().boundingBox().contains(e.getPlayer().getLocation().toVector())) continue;
            if (!trial.isBuildingCreated()) continue;
            trial.quitTrial(e.getPlayer());
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
            if (!trial.getPlayers().contains(p)) continue;
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
        if (!(e.getTarget() instanceof Player target)) {
            e.setCancelled(true);
            return;
        }

        // Ensure they are targeting the right trial player
        for (Trial trial : trials)
            if (trial.getPlayers().contains(target)) {
                // Pick a random player to target
                ArrayList<Player> players = new ArrayList<>(trial.getPlayers());
                e.setTarget(players.get((int) (Math.random() * players.size())));
                return;
            }
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

    @EventHandler
    public void playerQuit(PlayerQuitEvent e) {
        TrialUtils.saveCompletedTrials(e.getPlayer(), trialDataConfig, false);
    }

    public static boolean isTrialPlayer(Player p) {
        for (Trial trial : trials) if (trial.getPlayers().contains(p)) return true;
        return false;
    }

    public static void loadProtectedAreas() {
        if (trialDataConfig == null) {
            File file = new File(SurvivalSkills.getInstance().getDataFolder(), "trialdata.yml");
            if (!file.exists()) SurvivalSkills.getInstance().saveResource("trialdata.yml", true);
            trialDataConfig = YamlConfiguration.loadConfiguration(file);
        }

        ConfigurationSection section = trialDataConfig.getConfigurationSection("");
        if (section == null) return;
        if (section.getKeys(false).isEmpty()) return;

        for (String key : section.getKeys(false)) {
            // Get the min and max boundaries of the bounding box
            if (!trialDataConfig.contains(key + ".ProtectedArea")) continue;
            String minString = (String) trialDataConfig.get(key + ".ProtectedArea.Min");
            if (minString == null) continue;
            String maxString = (String) trialDataConfig.get(key + ".ProtectedArea.Max");
            if (maxString == null) continue;
            String[] minLocation = minString.split(":");
            String[] maxLocation = maxString.split(":");
            if (minLocation.length != 3 || maxLocation.length != 3) continue;

            BoundingBox box = new BoundingBox();
            box.resize(Double.parseDouble(minLocation[0]), Double.parseDouble(minLocation[1]), Double.parseDouble(minLocation[2]),
                    Double.parseDouble(maxLocation[0]), Double.parseDouble(maxLocation[1]), Double.parseDouble(maxLocation[2]));

            String worldString = (String) trialDataConfig.get(key + ".ProtectedArea.World");
            if (worldString == null) continue;
            // Use world name instead of UUID for reliability
            World world = Bukkit.getWorld(worldString);

            ProtectedArea protectedArea = new ProtectedArea(box, world);

            UUID uuid = UUID.fromString(key);
            protectedAreas.put(uuid, protectedArea);
        }
    }

    public static void loadCompletedTrials(Player p) {
        if (trialDataConfig == null) {
            File file = new File(SurvivalSkills.getInstance().getDataFolder(), "trialdata.yml");
            if (!file.exists()) SurvivalSkills.getInstance().saveResource("trialdata.yml", true);
            trialDataConfig = YamlConfiguration.loadConfiguration(file);
        }

        if (trialDataConfig.contains(p.getUniqueId() + ".CompletedTrials")) {
            ArrayList<Integer> gamemodesBeaten = new ArrayList<>();
            for (String gamemode : trialDataConfig.getStringList(p.getUniqueId() + ".CompletedTrials"))
                gamemodesBeaten.add(Integer.parseInt(gamemode));
            playerGamemodesBeaten.put(p, gamemodesBeaten);
        }
        else playerGamemodesBeaten.put(p, new ArrayList<>());
    }

    public static void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpiredTrialBuildings();
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), CLEANUP_INTERVAL, CLEANUP_INTERVAL);
    }

    private static void cleanupExpiredTrialBuildings() {
        ArrayList<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, TrialBuildingData> entry : trialBuildingOwnership.entrySet()) {
            if (entry.getValue().isExpired()) {
                UUID buildingId = entry.getKey();
                TrialBuildingData buildingData = entry.getValue();
                Location buildingLocation = buildingData.location();

                // Ensure the world is still available before attempting cleanup
                if (buildingLocation.getWorld() == null) {
                    SurvivalSkills.getInstance().getLogger().warning(
                            "World no longer exists for expired trial building owned by " +
                            Bukkit.getOfflinePlayer(buildingData.owner()).getName() + ". Removing from records only.");
                    toRemove.add(buildingId);
                    continue;
                }

                // Load the chunk where the building is located to ensure safe removal
                World world = buildingLocation.getWorld();
                int chunkX = buildingLocation.getBlockX() >> 4;
                int chunkZ = buildingLocation.getBlockZ() >> 4;

                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    world.loadChunk(chunkX, chunkZ);
                }

                // Remove the protected area (chunks will be loaded within removeProtectedArea method)
                ProtectedArea protectedArea = protectedAreas.get(buildingId);
                if (protectedArea != null) {
                    TrialUtils.removeProtectedArea(protectedArea);
                    protectedAreas.remove(buildingId);
                }

                // Remove building data
                toRemove.add(buildingId);

                SurvivalSkills.getInstance().getLogger().info("Removed expired trial building owned by " +
                                                                      Bukkit.getOfflinePlayer(buildingData.owner()).getName());
            }
        }

        for (UUID id : toRemove) trialBuildingOwnership.remove(id);
        if (!toRemove.isEmpty()) saveTrialBuildingData(true);
    }

    public static void updateTrialUsage(UUID trialBuildingId) {
        TrialBuildingData data = trialBuildingOwnership.get(trialBuildingId);
        if (data != null) {
            trialBuildingOwnership.put(trialBuildingId, new TrialBuildingData(data.owner(), System.currentTimeMillis(),
                    data.location()));
        }
    }

    public static void registerTrialBuilding(UUID userId, Location location) {
        if (trialBuildingOwnership.containsKey(userId)) {
            updateTrialUsage(userId);
            return;
        }

        trialBuildingOwnership.put(userId, new TrialBuildingData(userId, System.currentTimeMillis(), location));
    }

    private static void saveTrialBuildingData(boolean saveFile) {
        if (trialDataConfig == null) {
            File file = new File(SurvivalSkills.getInstance().getDataFolder(), "trialdata.yml");
            if (!file.exists()) SurvivalSkills.getInstance().saveResource("trialdata.yml", true);
            trialDataConfig = YamlConfiguration.loadConfiguration(file);
        }

        // Get a list of all currently stored UUIDs
        List<String> existingKeys = new ArrayList<>(trialDataConfig.getKeys(false));
        // Remove keys that are no longer in use
        for (String key : existingKeys) {
            try {
                UUID uuid = UUID.fromString(key);
                if (!trialBuildingOwnership.containsKey(uuid))
                    trialDataConfig.set(key, null);
            } catch (IllegalArgumentException e) {
                // Not a UUID, skip
            }
        }

        for (Map.Entry<UUID, TrialBuildingData> entry : trialBuildingOwnership.entrySet()) {
            String key = entry.getKey().toString();
            TrialBuildingData data = entry.getValue();

            trialDataConfig.set(key + ".Owner", data.owner().toString());
            trialDataConfig.set(key + ".LastUsed", data.lastUsed());
            trialDataConfig.set(key + ".Location", locationToString(data.location()));
        }

        if (!saveFile) return;

        try {
            File file = new File(SurvivalSkills.getInstance().getDataFolder(), "trialdata.yml");
            trialDataConfig.save(file);
        } catch (Exception e) {
            SurvivalSkills.getInstance().getLogger().warning("Failed to save trial building data: " + e.getMessage());
        }
    }

    public static void removeTrialBuilding(UUID buildingId) {
        trialBuildingOwnership.remove(buildingId);
        trialDataConfig.set(buildingId.toString(), null);
    }

    private static void loadTrialBuildingData() {
        if (trialDataConfig == null) {
            File file = new File(SurvivalSkills.getInstance().getDataFolder(), "trialdata.yml");
            if (!file.exists()) {
                Bukkit.getLogger().warning("No trial building saved in trialdata.yml");
                return;
            }
            trialDataConfig = YamlConfiguration.loadConfiguration(file);
        }

        ConfigurationSection section = trialDataConfig.getConfigurationSection("");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            if (!trialDataConfig.contains(key + ".Owner")) continue;

            try {
                UUID buildingId = UUID.fromString(key);
                UUID owner = UUID.fromString(Objects.requireNonNull(trialDataConfig.getString(key + ".Owner")));
                long lastUsed = trialDataConfig.getLong(key + ".LastUsed", System.currentTimeMillis());
                String locationStr = trialDataConfig.getString(key + ".Location");

                if (locationStr != null) {
                    Location location = stringToLocation(locationStr);
                    trialBuildingOwnership.put(buildingId, new TrialBuildingData(owner, lastUsed, location));
                }
            } catch (Exception e) {
                SurvivalSkills.getInstance().getLogger().warning("Failed to load trial building data for " + key);
            }
        }
    }

    private static String locationToString(Location loc) {
        return Objects.requireNonNull(loc.getWorld()).getUID() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ();
    }

    private static Location stringToLocation(String str) {
        String[] parts = str.split(":");
        if (parts.length != 4) return null;

        World world = Bukkit.getWorld(UUID.fromString(parts[0]));
        if (world == null) return null;

        return new Location(world, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
    }

    public static void handleTrials() {
        // End existing trials
        if (!trials.isEmpty()) {
            ArrayList<Trial> trials = new ArrayList<>(TrialManager.trials);
            for (Trial trial : trials) trial.endTrial();
        }

        if (protectedAreas.isEmpty()) return;

        for (Map.Entry<UUID, ProtectedArea> protectedArea : protectedAreas.entrySet()) {
            String minLocation = protectedArea.getValue().boundingBox().getMinX() + ":"
                    + protectedArea.getValue().boundingBox().getMinY() + ":" + protectedArea.getValue().boundingBox().getMinZ();
            String maxLocation = protectedArea.getValue().boundingBox().getMaxX() + ":"
                    + protectedArea.getValue().boundingBox().getMaxY() + ":" + protectedArea.getValue().boundingBox().getMaxZ();
            trialDataConfig.set(protectedArea.getKey().toString() + ".ProtectedArea.Min", minLocation);
            trialDataConfig.set(protectedArea.getKey().toString() + ".ProtectedArea.Max", maxLocation);
            // Save world name instead of UUID for reliability
            trialDataConfig.set(protectedArea.getKey().toString() + ".ProtectedArea.World", protectedArea.getValue().world().getName());
        }

        saveTrialBuildingData(false);

        for (Map.Entry<Player, ArrayList<Integer>> entry : playerGamemodesBeaten.entrySet()) {
            TrialUtils.saveCompletedTrials(entry.getKey(), trialDataConfig, true);
        }

        try {
            File file = new File(SurvivalSkills.getInstance().getDataFolder(), "trialdata.yml");
            trialDataConfig.save(file);
        } catch (Exception e) {
            SurvivalSkills.getInstance().getLogger().warning("Failed to save protected areas to trialdata.yml");
        }
    }

    public static Wave spawnWave(Wave wave, ArrayList<Location> spawningSpots, ArrayList<Player> players, int playerCount) {
        if (spawningSpots == null || spawningSpots.isEmpty()) {
            SurvivalSkills.getInstance().getLogger().warning("No spawning spots provided for wave " + wave);
            return null;
        }

        if (wave == null) return null;
        Wave waveObject = wave.duplicate();
        if (waveObject == null) {
            SurvivalSkills.getInstance().getLogger().warning("Wave " + wave + " does not exist");
            return null;
        }

        if (waveObject.isBossWave()) {
            TrialBoss boss = waveObject.getBoss();
            boss.setSpawnLocations(spawningSpots);
            boss.spawn(spawningSpots.get((int) (Math.random() * spawningSpots.size())));

            boss.scaleHealth(playerCount);
            boss.startScript();
            return waveObject;
        }

        if (playerCount > 1) waveObject.scaleMobs(playerCount);

        for (WaveMob waveMob : waveObject.getWaveMobs()) {
            Location location = getRandomSpawningSpot(spawningSpots);
            waveObject.spawnMob(location, waveMob, players);
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

        PlayerTrialUpgrades upgrades = PlayerTrialUpgrades.getPlayerUpgrades(p);
        double enchantChance = TrialTree.getEnchantChance(upgrades);

        ArrayList<ItemStack> rewards = lootTables.get(stage).getItems(3);

        // Apply enchantments based on upgrade level
        rewards.replaceAll(item -> TrialTree.applyRandomEnchantments(item, enchantChance));

        inv.setItem(2, rewards.get(0));
        inv.setItem(4, rewards.get(1));
        inv.setItem(6, rewards.get(2));

        rewardInventories.add(inv);
        p.openInventory(inv);
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

        // Stage 5 Rewards
        TrialLootTable stage5Rewards = new TrialLootTable();
        stage5Rewards.addItem(getTrialItem(Material.NETHERITE_SWORD, 1), 2);
        stage5Rewards.addItem(getTrialItem(Material.NETHERITE_AXE, 1), 2);
        stage5Rewards.addItem(getTrialItem(Material.COOKED_BEEF, 32), 2);
        stage5Rewards.addItem(getTrialItem(Material.NETHERITE_BOOTS, 1), 3);
        stage5Rewards.addItem(getTrialItem(Material.NETHERITE_CHESTPLATE, 1), 4);
        stage5Rewards.addItem(getTrialItem(Material.NETHERITE_HELMET, 1), 4);
        stage5Rewards.addItem(getTrialItem(Material.NETHERITE_LEGGINGS, 1), 3);
        stage5Rewards.addItem(getTrialItem(Material.CROSSBOW, 1), 2);
        stage5Rewards.addItem(getTrialItem(Material.BOW, 1), 2);
        stage5Rewards.addItem(getTrialItem(Material.ARROW, 64), 3);
        stage5Rewards.addItem(getTrialItem(Material.ENCHANTED_GOLDEN_APPLE, 3), 1);
        lootTables.put(5, stage5Rewards);
    }

    public static boolean isInTrial(Player p) {
        if (trials.isEmpty()) return false;
        for (Trial trial : trials) if (trial.getPlayers().contains(p)) return true;
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

    public static HashMap<Player, Player> getSpectatorTargets() {
        return spectatorTargets;
    }

    public static HashMap<Player, Scoreboard> getTrialScoreboards() {
        return trialScoreboards;
    }

    public static HashMap<Player, Scoreboard> getSpectatorScoreboards() {
        return spectatorScoreboards;
    }

    public static ArrayList<Inventory> getTrialSelectionInventories() {
        return trialSelectionInventories;
    }

    public static FileConfiguration getTrialBuildingConfig() {
        return trialBuildingConfig;
    }

    public static void setTrialBuildingConfig(FileConfiguration trialBuildingConfig) {
        TrialManager.trialBuildingConfig = trialBuildingConfig;
    }

    public static HashMap<Player, PendingTrial> getPendingTrials() {
        return pendingTrials;
    }

    public static HashMap<Player, ArrayList<Integer>> getPlayerGamemodesBeaten() {
        return playerGamemodesBeaten;
    }

    public static WaveGenerator getWaveGenerator() {
        return waveGenerator;
    }

    public static HashMap<UUID, Long> getTrialBuildingCreationCooldownList() {
        return trialBuildingCreationCooldownList;
    }
}
