package sir_draco.survivalskills.skill_listeners;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.bosses.Boss;
import sir_draco.survivalskills.bosses.BroodMotherBoss;
import sir_draco.survivalskills.bosses.DragonBoss;
import sir_draco.survivalskills.bosses.GiantBoss;
import sir_draco.survivalskills.bosses.VillagerBoss;
import sir_draco.survivalskills.god_questline.trial.ProtectedArea;
import sir_draco.survivalskills.god_questline.trial.TrialManager;
import sir_draco.survivalskills.skill_listeners.fighting.BerserkerManager;
import sir_draco.survivalskills.skill_listeners.fighting.BossManager;
import sir_draco.survivalskills.skill_listeners.fighting.BossType;
import sir_draco.survivalskills.skill_listeners.fighting.CombatMechanicsManager;
import sir_draco.survivalskills.skill_listeners.fighting.DragonManager;
import sir_draco.survivalskills.skill_listeners.fighting.MobXPManager;
import sir_draco.survivalskills.utils.items.ItemStackGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight coordinator for the fighting skill.
 *
 * Originally this single class handled boss summoning/tracking, the berserker
 * ability, combat mechanics (crit/lifesteal), the ender-dragon lifecycle, mob XP,
 * phantom spawning and player respawn slowness. Each concern now lives in a
 * focused collaborator in {@code skill_listeners.fighting}; this class only wires
 * them together and routes Bukkit events. The public API used by commands and the
 * plugin core (the boss/phantom/boss-music collections and the dragon boss
 * reference) is preserved verbatim.
 */
public class FightingSkill implements Listener {

    private static final int RESPAWN_SLOWNESS_DURATION_TICKS = 20;
    private static final int RESPAWN_SLOWNESS_DELAY_TICKS = 40;
    private static final int PHANTOM_PREVENTION_RADIUS = 50;

    private final SurvivalSkills plugin;

    // Player-driven toggles shared with collaborators
    private final List<Player> noBossMusic = new ArrayList<>();
    private final List<Player> noPhantomSpawns = new ArrayList<>();
    private final List<Player> fixSlownessEffect = new ArrayList<>();

    // Focused collaborators (constructed once, in dependency order)
    private final DragonManager dragonManager;
    private final BossManager bossManager;
    private final BerserkerManager berserkerManager;
    private final CombatMechanicsManager combatMechanicsManager;
    private final MobXPManager mobXPManager;

    public FightingSkill(SurvivalSkills plugin) {
        this.plugin = plugin;
        this.dragonManager = new DragonManager(plugin);
        this.bossManager = new BossManager(plugin, dragonManager, noBossMusic, fixSlownessEffect);
        this.berserkerManager = new BerserkerManager(plugin, plugin.getAbilityManager());
        this.combatMechanicsManager = new CombatMechanicsManager(plugin);
        this.mobXPManager = new MobXPManager(plugin);
    }

    // =================================================================
    // Death / XP
    // =================================================================

    @EventHandler(ignoreCancelled = true)
    public void onKillEntity(EntityDeathEvent e) {
        if (e.getEntity() instanceof Player p) {
            berserkerManager.endBerserkerOnDeath(p);
            bossManager.handlePlayerBossCleanup(p);
            dragonManager.onPlayerDeathCleanup(p);
            return;
        }

        Player p = e.getEntity().getKiller();
        if (p == null && !e.getEntity().getType().equals(EntityType.ENDER_DRAGON)) {
            bossManager.handleUnnaturalBossDeath(e);
            return;
        }

        if (bossManager.isBoss(e.getEntity()) || e.getEntity().getType().equals(EntityType.ENDER_DRAGON)) {
            bossManager.handleBossKill(p, e);
            return;
        }

        handleSpecialDrops(e);
        mobXPManager.handleExperience(p, e.getEntity());
    }

    /** Drop the special trophy items for the vanilla bosses (Warden/Elder Guardian/Dragon). */
    private void handleSpecialDrops(EntityDeathEvent e) {
        ItemStack drop = switch (e.getEntity().getType()) {
            case WARDEN -> ItemStackGenerator.getWardenBossItem();
            case ELDER_GUARDIAN -> ItemStackGenerator.getElderGuardianBossItem();
            case ENDER_DRAGON -> ItemStackGenerator.getEnderDragonBossItem();
            default -> null;
        };
        if (drop != null)
            e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), drop);
    }

    // =================================================================
    // Boss summoning + berserker activation
    // =================================================================

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();

        // A boss summoning item may never be used from the offhand.
        if (BossManager.isSummoningBoss(p.getInventory().getItemInOffHand())) {
            e.setCancelled(true);
            p.sendRawMessage(ChatColor.RED + "You cannot use a boss summoning item in your offhand");
            return;
        }

        ItemStack mainHand = p.getInventory().getItemInMainHand();
        boolean boss = BossManager.isSummoningBoss(mainHand);
        if (!berserkerManager.holdingWeapon(mainHand) && !boss)
            return;

        if (boss) {
            e.setCancelled(true);
            if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
                return;
            if (e.getHand() == null || !e.getHand().equals(EquipmentSlot.HAND))
                return;
            if (e.getClickedBlock() == null)
                return;
            Location loc = e.getClickedBlock().getLocation().clone().add(0, 1, 0);
            BossType type = BossType.fromSpawnEgg(mainHand.getType());
            if (type != null)
                bossManager.spawnBoss(type, loc, p, mainHand);
            return;
        }

        // Berserker interaction path
        if (!p.isSneaking())
            return;
        if (e.getHand() != EquipmentSlot.HAND)
            return;
        if (e.getAction().equals(Action.LEFT_CLICK_BLOCK) || e.getAction().equals(Action.LEFT_CLICK_AIR))
            return;

        berserkerManager.tryActivate(p);
    }

    // =================================================================
    // Combat mechanics
    // =================================================================

    @EventHandler
    public void handleFightingSkills(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p))
            return;
        if (e.getCause().equals(EntityDamageEvent.DamageCause.THORNS))
            return;

        if (berserkerManager.isBerserkerActive(p))
            berserkerManager.applyBerserkerDamageBonus(p, e);

        combatMechanicsManager.applyCritical(p, e);
        combatMechanicsManager.applyLifesteal(p, e);
    }

    @EventHandler
    public void spiderBite(EntityDamageByEntityEvent e) {
        bossManager.handleSpiderBite(e);
    }

    // =================================================================
    // Boss damage / transform listeners
    // =================================================================

    @EventHandler
    public void bossDamage(EntityDamageEvent e) {
        bossManager.handleBossDamage(e);
    }

    @EventHandler
    public void villagerBossLightning(EntityTransformEvent e) {
        bossManager.handleVillagerTransform(e);
    }

    @EventHandler
    public void exiledDamage(EntityDamageByEntityEvent e) {
        bossManager.handleExiledDamage(e);
    }

    @EventHandler
    public void bossDamageByCorrectPlayer(EntityDamageByEntityEvent e) {
        bossManager.handleBossDamageByCorrectPlayer(e);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void scaleExiledDamage(EntityDamageEvent e) {
        bossManager.scaleExiledDamage(e);
    }

    // =================================================================
    // Dragon lifecycle
    // =================================================================

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void teleportToEnd(PlayerTeleportEvent e) {
        dragonManager.teleportToEnd(e);
    }

    @EventHandler
    public void dragonSpawnEvent(EntitySpawnEvent e) {
        dragonManager.dragonSpawnEvent(e);
    }

    // =================================================================
    // Phantom spawn gating + respawn slowness
    // =================================================================

    @EventHandler
    public void phantomSpawn(EntitySpawnEvent e) {
        if (e.getEntity().hasMetadata("trialmob"))
            return;

        // Spawns inside a trial chamber are always allowed.
        for (ProtectedArea area : TrialManager.getProtectedAreas().values())
            if (area.boundingBox().contains(e.getLocation().toVector()))
                return;

        if (!e.getEntity().getType().equals(EntityType.PHANTOM))
            return;
        if (noPhantomSpawns.isEmpty())
            return;
        Location loc = e.getLocation();
        if (loc.getWorld() == null)
            return;
        for (Player p : noPhantomSpawns) {
            if (!loc.getWorld().getEnvironment().equals(p.getWorld().getEnvironment()))
                continue;
            if (p.getLocation().distance(loc) < PHANTOM_PREVENTION_RADIUS) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void playerRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (fixSlownessEffect.contains(p))
            return;
        fixSlownessEffect.remove(p);
        new BukkitRunnable() {
            @Override
            public void run() {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, RESPAWN_SLOWNESS_DURATION_TICKS, 0));
            }
        }.runTaskLater(plugin, RESPAWN_SLOWNESS_DELAY_TICKS);
    }

    // =================================================================
    // Public API (used by commands and the plugin core)
    // =================================================================

    public List<GiantBoss> getGiants() {
        return bossManager.getGiants();
    }

    public List<BroodMotherBoss> getBroodMothers() {
        return bossManager.getBroodMothers();
    }

    public List<VillagerBoss> getVillagerBosses() {
        return bossManager.getVillagerBosses();
    }

    public DragonBoss getDragonBoss() {
        return dragonManager.getDragonBoss();
    }

    public List<Player> getNoPhantomSpawns() {
        return noPhantomSpawns;
    }

    public List<Player> getNoBossMusic() {
        return noBossMusic;
    }

    public void addBoss(Player p, Boss boss) {
        bossManager.addBoss(p, boss);
    }

    public void removeBoss(Boss boss) {
        bossManager.removeBoss(boss);
    }
}
