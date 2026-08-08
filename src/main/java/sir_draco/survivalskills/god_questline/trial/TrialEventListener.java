package sir_draco.survivalskills.god_questline.trial;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpellCastEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.god_questline.trial_mobs.WaveMob;
import sir_draco.survivalskills.utils.TrialUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Central event listener for the trial system. All state mutations are delegated to the focused
 * managers ({@link TrialRegistry}, {@link ProtectedAreaManager}, {@link TrialSpectatorManager},
 * {@link TrialRewardManager}); this class only wires Bukkit events to them.
 */
public class TrialEventListener implements Listener {

    // Two sneak-start (toggle-to-sneaking) events within this window let a trial participant
    // skip the countdown between waves. Renamed from lastCrouchTimes to make it clear we are
    // counting consecutive sneak-starts, not a literal held double-tap.
    private static final long DOUBLE_SNEAK_MS = 600;
    private static final HashMap<UUID, Long> lastSneakStartTimes = new HashMap<>();
    private static final String GOD_TRIAL_COMMAND = "/godtrial";

    static void clearRuntimeState() {
        lastSneakStartTimes.clear();
    }

    @EventHandler
    public void onPlayerSneakToggle(PlayerToggleSneakEvent e) {
        // Only react when the player begins sneaking; toggling back to standing is ignored.
        if (!e.isSneaking())
            return;

        Player p = e.getPlayer();
        Optional<Trial> trialOpt = TrialRegistry.getInstance().getPlayerTrial(p);
        if (trialOpt.isEmpty())
            return;

        Trial trial = trialOpt.get();
        UUID id = p.getUniqueId();
        long now = System.currentTimeMillis();
        Long last = lastSneakStartTimes.remove(id);

        // A second sneak-start within the window skips the between-wave countdown. A single
        // sneak-start on its own does nothing (it is simply recorded here).
        if (last != null && now - last <= DOUBLE_SNEAK_MS) {
            trial.attemptSkipCountdown(p);
            return;
        }
        lastSneakStartTimes.put(id, now);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        TrialDataPersistence.getInstance().loadCompletedTrials(e.getPlayer(), TrialRegistry.getInstance());
    }

    // --- Protected-area handlers (consolidated via ProtectedAreaManager) ---

    @EventHandler
    public void onTrialBuildingBreak(BlockBreakEvent e) {
        if (ProtectedAreaManager.getInstance().isBlockProtected(e.getBlock()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onTrialBuildingPlace(BlockPlaceEvent e) {
        if (ProtectedAreaManager.getInstance().isBlockProtected(e.getBlock()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onEndermanBlockTake(EntityChangeBlockEvent e) {
        if (!e.getEntity().getType().equals(EntityType.ENDERMAN))
            return;
        if (ProtectedAreaManager.getInstance().isBlockProtected(e.getBlock()))
            e.setCancelled(true);
    }

    @EventHandler
    public void onExplosionBreakBlock(EntityExplodeEvent e) {
        // Trial creepers should not destroy terrain: hand their block list off to the wave so
        // any reference to them is cleaned up.
        if (e.getEntity().hasMetadata("trialmob") && e.getEntity() instanceof Creeper) {
            for (Trial trial : TrialRegistry.getInstance().getTrials()) {
                Wave wave = trial.getWave();
                if (wave == null || wave.getWaveMobs().isEmpty())
                    continue;
                Optional<WaveMob> creeperMob = wave.getWaveMob(e.getEntity());
                if (creeperMob.isEmpty())
                    continue;
                wave.removeWaveMob(creeperMob.get());
                e.blockList().clear();
                return;
            }
        }

        // Otherwise strip any blocks that fall inside a protected area.
        ProtectedAreaManager areas = ProtectedAreaManager.getInstance();
        if (!areas.hasProtectedAreas())
            return;
        for (org.bukkit.block.Block block : new ArrayList<>(e.blockList())) {
            if (areas.isBlockProtected(block)) {
                e.blockList().clear();
                return;
            }
        }
    }

    // --- Spectator restrictions -----------------------------------------

    @EventHandler
    public void onSpectatorSneak(PlayerToggleSneakEvent e) {
        // Prevent spectators from crouching (which would exit first-person view).
        TrialSpectatorManager spectators = TrialSpectatorManager.getInstance();
        if (spectators.isSpectating(e.getPlayer()) && e.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            e.setCancelled(true);
            Player target = spectators.getSpectatorTarget(e.getPlayer());
            if (target != null)
                e.getPlayer().setSpectatorTarget(target);
        }
    }

    @EventHandler
    public void onSpectatorGameModeChange(PlayerGameModeChangeEvent e) {
        if (TrialSpectatorManager.getInstance().isSpectating(e.getPlayer())
                && e.getNewGameMode() != GameMode.SPECTATOR)
            e.setCancelled(true);
    }

    // --- Command gating -------------------------------------------------

    @EventHandler
    public void playerSendCommand(PlayerCommandPreprocessEvent e) {
        TrialSpectatorManager spectators = TrialSpectatorManager.getInstance();
        TrialRegistry registry = TrialRegistry.getInstance();

        if (spectators.isSpectating(e.getPlayer())) {
            if (!e.getMessage().toLowerCase().startsWith(GOD_TRIAL_COMMAND))
                e.setCancelled(true);
            return;
        }

        if (registry.hasActiveTrials() && registry.isInTrial(e.getPlayer())
                && !e.getMessage().toLowerCase().startsWith(GOD_TRIAL_COMMAND))
            e.setCancelled(true);
    }

    // --- Trial mob death ------------------------------------------------

    @EventHandler
    public void onTrialMobDeath(EntityDeathEvent e) {
        if (e.getEntity() instanceof Player)
            return;
        if (!isTrialMob(e.getEntity()))
            return;
        TrialRegistry registry = TrialRegistry.getInstance();
        if (!registry.hasActiveTrials())
            return;

        for (Trial trial : registry.getTrials()) {
            Wave wave = trial.getWave();
            if (wave == null)
                continue;

            int scoreMultiplier = (trial.getWaveNumber() / 5) + 1;
            if (wave.isBossWave()) {
                handleBossDeath(trial, e, scoreMultiplier);
                return;
            }

            if (handleRegularMobDeath(trial, wave, e, scoreMultiplier))
                return;
        }
    }

    private void handleBossDeath(Trial trial, EntityDeathEvent e, int scoreMultiplier) {
        e.getDrops().clear();
        if (e.getEntity().hasMetadata("spawned")) {
            trial.changeScore(scoreMultiplier * 10);
            return;
        }
        if (!e.getEntity().hasMetadata("trialboss"))
            return;

        trial.getWave().getBoss().death();
        trial.changeScore(scoreMultiplier * 1000);

        if (trial.getWaveNumber() == trial.getMaxWave()) {
            trial.completeTrial();
            return;
        }
        trial.endWave();
    }

    /**
     * Processes a non-boss mob death. Returns true if the trial was completed and the caller
     * should stop iterating (i.e. return from the event handler).
     */
    private boolean handleRegularMobDeath(Trial trial, Wave wave, EntityDeathEvent e, int scoreMultiplier) {
        if (wave.getWaveMobs().isEmpty()) {
            if (!trial.isActiveWave())
                return false;
            trial.endWave();
            return false;
        }

        if (e.getEntity().hasMetadata("extramob")) {
            wave.removeExtraMob(e.getEntity());
            trial.changeScore(scoreMultiplier * 10);
        } else {
            Optional<WaveMob> waveMob = wave.getWaveMob(e.getEntity());
            if (waveMob.isEmpty())
                return false;
            e.getDrops().clear();
            waveMob.get().dropItems();
            wave.removeWaveMob(waveMob.get());
            trial.changeScore(scoreMultiplier * 100);
        }

        return checkWaveCompletion(trial, wave);
    }

    /**
     * Ends the wave (or completes the trial) once every mob in it is gone. Returns true if the
     * trial was completed so the caller can short-circuit.
     */
    private boolean checkWaveCompletion(Trial trial, Wave wave) {
        if (wave.getMobsLeft() != 0)
            return false;
        if (trial.getWaveNumber() == trial.getMaxWave()) {
            trial.completeTrial();
            return true;
        }
        trial.endWave();
        return false;
    }

    private boolean isTrialMob(Entity entity) {
        return entity.hasMetadata("trialmob") || entity.hasMetadata("trialboss");
    }

    // --- Trial mob damage ------------------------------------------------

    @EventHandler
    public void onTrialMobDamage(EntityDamageEvent e) {
        if (TrialRegistry.getInstance().getTrials().isEmpty())
            return;
        if (e.getEntity() instanceof Player)
            return;
        if (!isTrialMob(e.getEntity()))
            return;
        // Only damage dealt by another entity is allowed; everything else is cancelled.
        if (!(e instanceof EntityDamageByEntityEvent))
            e.setCancelled(true);
    }

    @EventHandler
    public void onTrialMobDamageByPlayer(EntityDamageByEntityEvent e) {
        TrialRegistry registry = TrialRegistry.getInstance();
        if (!registry.hasActiveTrials())
            return;
        if (e.getEntity() instanceof Player)
            return;
        if (!isTrialMob(e.getEntity()))
            return;

        // Trial mobs are immune to fireball splash damage.
        if (e.getDamager() instanceof Fireball) {
            e.setCancelled(true);
            return;
        }

        Player p = resolveDamagingPlayer(e);
        if (p == null) {
            e.setCancelled(true);
            return;
        }

        for (Trial trial : registry.getTrials()) {
            Wave wave = trial.getWave();
            if (wave == null || wave.getWaveMobs().isEmpty())
                continue;
            Optional<WaveMob> waveMob = wave.getWaveMob(e.getEntity());
            if (waveMob.isEmpty())
                continue;
            if (!trial.getPlayers().contains(p)) {
                e.setCancelled(true);
                return;
            }
            // Confirmed: the player belongs to the trial that owns this mob — stop searching.
            break;
        }

        e.setDamage(e.getDamage() * TrialTree.getDamageMultiplier(TrialUpgradeManager.getPlayerUpgrades(p)));
    }

    private Player resolveDamagingPlayer(EntityDamageByEntityEvent e) {
        return switch (e.getDamager()) {
            case Arrow arrow -> arrow.getShooter() instanceof Player shooter ? shooter : null;
            case Trident trident -> trident.getShooter() instanceof Player shooter ? shooter : null;
            case Player player -> player;
            default -> null;
        };
    }

    @EventHandler
    public void onTrialMobCombust(EntityCombustEvent e) {
        if (!TrialRegistry.getInstance().hasActiveTrials())
            return;
        if (!e.getEntity().hasMetadata("trialmob"))
            return;
        e.setCancelled(true);
    }

    // --- Player death / hurt --------------------------------------------

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Optional<Trial> trialOpt = TrialRegistry.getInstance().getPlayerTrial(e.getEntity());
        if (trialOpt.isEmpty())
            return;
        e.getDrops().clear();
        e.setDroppedExp(0);
        trialOpt.get().endTrial();
        e.getEntity().getInventory().clear();
    }

    @EventHandler
    public void onPlayerHurt(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p))
            return;
        Optional<Trial> trialOpt = TrialRegistry.getInstance().getPlayerTrial(p);
        if (trialOpt.isEmpty())
            return;
        Trial trial = trialOpt.get();

        double damage = e.getDamage();
        trial.changeScore((int) -damage * 2);

        if (Math.random() < TrialTree.getDodgeChance(TrialUpgradeManager.getPlayerUpgrades(p))) {
            e.setCancelled(true);
            return;
        }

        // Boss-wave snowball hits briefly freeze the player.
        if (!e.getCause().equals(EntityDamageEvent.DamageCause.PROJECTILE))
            return;
        if (!(e instanceof EntityDamageByEntityEvent entityDamageByEntityEvent))
            return;
        if (!(entityDamageByEntityEvent.getDamager() instanceof Snowball))
            return;
        if (trial.getWave() == null || !trial.getWave().isBossWave())
            return;

        float walkSpeed = p.getWalkSpeed();
        if (walkSpeed == 0)
            return;
        p.setWalkSpeed(0);
        Bukkit.getScheduler().runTaskLater(SurvivalSkills.getInstance(), () -> p.setWalkSpeed(walkSpeed), 40);
    }

    // --- Reward + selection inventories ---------------------------------

    private final NamespacedKey trialObjectKey = TrialRewardManager.getInstance().getTrialObjectKey();

    @EventHandler
    public void onRewardGUIClick(InventoryClickEvent e) {
        TrialRewardManager.getInstance().handleRewardClick(e);
    }

    @EventHandler
    public void onRewardGUIDrag(InventoryDragEvent e) {
        TrialRewardManager.getInstance().handleRewardDrag(e);
    }

    @EventHandler
    public void onRewardGUIClose(InventoryCloseEvent e) {
        TrialRewardManager.getInstance().handleRewardClose(e);
    }

    @EventHandler
    public void onUseEnchantedBook(InventoryClickEvent e) {
        if (e.isCancelled()
                || TrialRewardManager.getInstance().isRewardInventory(e.getView().getTopInventory()))
            return;
        if (e.getCurrentItem() == null || e.getCursor() == null)
            return;

        ItemStack item = e.getCurrentItem();
        ItemStack book = e.getCursor();
        if (item.getType().equals(Material.AIR) || !book.getType().equals(Material.ENCHANTED_BOOK))
            return;
        if (!hasTrialObjectKey(item) || !hasTrialObjectKey(book))
            return;

        boolean applied;
        if (item.getType().equals(Material.ENCHANTED_BOOK)) {
            applied = applyBookToBook(item, book);
        } else {
            applied = applyBookToItem(item, book);
        }

        if (!applied)
            return;
        e.setCancelled(true);
        e.getWhoClicked().setItemOnCursor(new ItemStack(Material.AIR));
    }

    private boolean hasTrialObjectKey(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta != null
                && meta.getPersistentDataContainer().has(trialObjectKey, PersistentDataType.STRING);
    }

    private boolean applyBookToBook(ItemStack item, ItemStack book) {
        if (!(item.getItemMeta() instanceof EnchantmentStorageMeta itemMeta)
                || !(book.getItemMeta() instanceof EnchantmentStorageMeta bookMeta))
            return false;

        boolean changed = false;
        for (Map.Entry<Enchantment, Integer> enchant : bookMeta.getStoredEnchants().entrySet()) {
            Enchantment enchantment = enchant.getKey();
            int currentLevel = itemMeta.getStoredEnchantLevel(enchantment);
            int combinedLevel = getCombinedEnchantmentLevel(
                    currentLevel, enchant.getValue(), enchantment.getMaxLevel());
            if (combinedLevel <= currentLevel)
                continue;
            itemMeta.addStoredEnchant(enchantment, combinedLevel, true);
            changed = true;
        }
        if (!changed)
            return false;
        item.setItemMeta(itemMeta);
        return true;
    }

    private boolean applyBookToItem(ItemStack item, ItemStack book) {
        if (!(book.getItemMeta() instanceof EnchantmentStorageMeta bookMeta))
            return false;

        Map<Enchantment, Integer> enchantmentsToApply = new HashMap<>();
        for (Map.Entry<Enchantment, Integer> enchant : bookMeta.getStoredEnchants().entrySet()) {
            Enchantment enchantment = enchant.getKey();
            if (!enchantment.canEnchantItem(item))
                return false;
            int currentLevel = item.getEnchantmentLevel(enchantment);
            int combinedLevel = getCombinedEnchantmentLevel(
                    currentLevel, enchant.getValue(), enchantment.getMaxLevel());
            if (combinedLevel > currentLevel)
                enchantmentsToApply.put(enchantment, combinedLevel);
        }

        if (enchantmentsToApply.isEmpty())
            return false;
        for (Map.Entry<Enchantment, Integer> enchantment : enchantmentsToApply.entrySet())
            item.addEnchantment(enchantment.getKey(), enchantment.getValue());
        return true;
    }

    static int getCombinedEnchantmentLevel(int currentLevel, int bookLevel, int maximumLevel) {
        int combinedLevel = currentLevel == bookLevel ? currentLevel + 1 : Math.max(currentLevel, bookLevel);
        return Math.min(combinedLevel, maximumLevel);
    }

    @EventHandler
    public void onTrialSelectionClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player))
            return;
        TrialRegistry registry = TrialRegistry.getInstance();
        Inventory top = e.getView().getTopInventory();
        Inventory clicked = e.getInventory();

        // Shift-clicks moving items out of the selection GUI into the player inventory.
        if (registry.isTrialSelectionInventory(player, top)
                && !registry.isTrialSelectionInventory(player, clicked)) {
            e.setCancelled(true);
            return;
        }
        if (!registry.isTrialSelectionInventory(player, clicked))
            return;
        e.setCancelled(true);
        TrialUtils.handleTrialSelectionClick(e.getClickedInventory(), player, e.getCurrentItem());
    }

    @EventHandler
    public void onTrialSelectionDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player player))
            return;
        TrialRegistry registry = TrialRegistry.getInstance();
        Inventory top = e.getView().getTopInventory();
        if (registry.isTrialSelectionInventory(player, top)
                && !registry.isTrialSelectionInventory(player, e.getInventory())) {
            e.setCancelled(true);
            return;
        }
        if (!registry.isTrialSelectionInventory(player, e.getInventory()))
            return;
        e.setCancelled(true);
    }

    @EventHandler
    public void onTrialPartyInventoryClose(InventoryCloseEvent e) {
        TrialRegistry registry = TrialRegistry.getInstance();
        registry.unregisterTrialSelectionInventory(e.getInventory());
        if (registry.getPendingTrials().isEmpty())
            return;
        Player p = (Player) e.getPlayer();
        if (!registry.hasPendingTrial(p))
            return;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (p.getOpenInventory().getTopInventory().getSize() != 9) {
                    if (registry.hasPendingTrial(p))
                        registry.getPendingTrial(p).endPendingTrial();
                    registry.removePendingTrial(p);
                }
            }
        }.runTaskLater(SurvivalSkills.getInstance(), 40);
    }

    // --- Pickup / movement / beacon -------------------------------------

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player p))
            return;

        ItemStack item = e.getItem().getItemStack();
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;
        boolean isTrialItem = meta.getPersistentDataContainer().has(trialObjectKey, PersistentDataType.STRING);
        boolean playerInTrial = TrialRegistry.getInstance().isInTrial(p);

        // Players outside a trial cannot pick up trial items; players inside one can only pick
        // up trial items.
        if (!playerInTrial) {
            if (isTrialItem)
                e.setCancelled(true);
            return;
        }
        if (!isTrialItem)
            e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerLeaveTrialArea(PlayerMoveEvent e) {
        Optional<Trial> trialOpt = TrialRegistry.getInstance().getPlayerTrial(e.getPlayer());
        if (trialOpt.isEmpty())
            return;
        Trial trial = trialOpt.get();
        if (!trial.isBuildingCreated())
            return;
        if (trial.getProtectedArea().boundingBox().contains(e.getPlayer().getLocation().toVector()))
            return;
        trial.quitTrial(e.getPlayer());
    }

    @EventHandler
    public void onBeaconEffect(EntityPotionEffectEvent e) {
        if (!TrialRegistry.getInstance().hasActiveTrials())
            return;
        if (!(e.getEntity() instanceof Player p))
            return;
        EntityPotionEffectEvent.Cause cause = e.getCause();
        if (cause != EntityPotionEffectEvent.Cause.BEACON
                && cause != EntityPotionEffectEvent.Cause.COMMAND
                && cause != EntityPotionEffectEvent.Cause.PLUGIN
                && cause != EntityPotionEffectEvent.Cause.POTION_SPLASH)
            return;
        if (TrialRegistry.getInstance().isInTrial(p))
            e.setCancelled(true);
    }

    @SuppressWarnings("deprecation")
    @EventHandler
    public void onSlimeSplit(SlimeSplitEvent e) {
        if (e.getEntity().hasMetadata("trialmob"))
            e.setCancelled(true);
    }

    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent e) {
        if (!e.getEntity().hasMetadata("trialmob"))
            return;

        if (!(e.getTarget() instanceof Player target)) {
            e.setCancelled(true);
            return;
        }

        Optional<Trial> trialOpt = TrialRegistry.getInstance().getPlayerTrial(target);
        if (trialOpt.isEmpty()) {
            e.setCancelled(true);
            return;
        }

        // Redirect to the nearest participant so mobs always chase a nearby trial player
        // rather than a random (possibly distant) one.
        Player nearest = trialOpt.get().getClosestPlayer(e.getEntity().getLocation());
        if (nearest != null)
            e.setTarget(nearest);
    }

    @EventHandler
    public void onEvokerSpellCast(EntitySpellCastEvent e) {
        if (!(e.getEntity() instanceof Evoker caster))
            return;
        if (!caster.hasMetadata("trialmob"))
            return;

        // Newly summoned entities become tagged as extra trial mobs.
        for (Entity spawnedEntity : caster.getWorld().getEntities()) {
            spawnedEntity.setMetadata("trialmob", new FixedMetadataValue(SurvivalSkills.getInstance(), true));
            spawnedEntity.setMetadata("extramob", new FixedMetadataValue(SurvivalSkills.getInstance(), true));

            for (Trial trial : TrialRegistry.getInstance().getTrials()) {
                Wave wave = trial.getWave();
                if (wave == null || wave.getWaveMob(spawnedEntity).isEmpty())
                    continue;
                wave.addExtraMob(spawnedEntity);
            }
        }
    }

    @EventHandler
    public void onEntityTransform(EntityTransformEvent e) {
        if (!e.getEntity().hasMetadata("trialmob"))
            return;
        e.setCancelled(true);

        // Preserve the held item the transforming entity was carrying.
        if (!(e.getEntity() instanceof org.bukkit.entity.LivingEntity entity))
            return;
        if (entity.getEquipment() == null)
            return;
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
        if (e.getEntity() instanceof Enderman && e.getEntity().hasMetadata("trialmob"))
            e.setCancelled(true);
    }

    @EventHandler
    public void playerQuit(PlayerQuitEvent e) {
        Player player = e.getPlayer();
        UUID playerId = player.getUniqueId();
        TrialRegistry registry = TrialRegistry.getInstance();

        TrialDataPersistence.getInstance().saveCompletedTrialsOnQuit(player, registry);
        lastSneakStartTimes.remove(playerId);

        TrialSpectatorManager spectatorManager = TrialSpectatorManager.getInstance();
        if (spectatorManager.isSpectating(player))
            TrialUtils.removeTrialSpectator(player, spectatorManager.getSpectatorTarget(player));

        for (Trial trial : registry.getTrials()) {
            trial.removeSpectator(player);
            trial.removeSpectatorsForTarget(player);
        }

        TrialGUI.removePlayer(playerId);
        TrialRewardManager.getInstance().removeInventory(player.getOpenInventory().getTopInventory());
        spectatorManager.removePlayer(player);
        registry.cleanupPlayer(player);
    }
}
