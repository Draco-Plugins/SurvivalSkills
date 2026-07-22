package sir_draco.survivalskills.skill_listeners.fighting;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Trident;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.inventory.ItemStack;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.abilities.items.GiantSword;
import sir_draco.survivalskills.bosses.Boss;
import sir_draco.survivalskills.bosses.BroodMotherBoss;
import sir_draco.survivalskills.bosses.GiantBoss;
import sir_draco.survivalskills.bosses.VillagerBoss;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.skills.SkillManager;
import sir_draco.survivalskills.utils.items.ItemStackGenerator;
import sir_draco.survivalskills.utils.items.ItemModelData;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;
import sir_draco.survivalskills.utils.music.ExiledBossMusic;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Boss summoning, tracking, damage gating, death handling and cleanup for the
 * egg-spawned bosses. Previously a tangle of per-type switch/if-else chains and
 * three near-identical {@code remove*} methods lived in the monolithic fighting
 * listener; here boss handling is driven by {@link BossType} and a single generic
 * remove walks a shared registry.
 */
public class BossManager {

    // BroodMother bonus drop
    private static final double BROODING_SILK_DROP_CHANCE = 0.2;
    // Spider-bite self-heal
    private static final double SPIDER_BITE_HEAL_RATIO = 0.5;
    private static final int SPIDER_BITE_PARTICLE_COUNT = 10;
    private static final int SPIDER_BITE_PARTICLES_PER = 2;
    // Night window for the Giant spawn
    private static final long NIGHT_START_TIME = 13000;
    private static final long NIGHT_END_TIME = 23000;

    private final SurvivalSkills plugin;
    private final DragonManager dragonManager;
    private final List<Player> noBossMusic;
    private final List<Player> fixSlownessEffect;

    private final List<GiantBoss> giants = new ArrayList<>();
    private final List<BroodMotherBoss> broodMothers = new ArrayList<>();
    private final List<VillagerBoss> villagers = new ArrayList<>();
    private final Map<Player, Boss> summonTracker = new HashMap<>();
    private final Map<BossType, List<? extends Boss>> bossRegistry = new EnumMap<>(BossType.class);

    public BossManager(SurvivalSkills plugin, DragonManager dragonManager,
                       List<Player> noBossMusic, List<Player> fixSlownessEffect) {
        this.plugin = plugin;
        this.dragonManager = dragonManager;
        this.noBossMusic = noBossMusic;
        this.fixSlownessEffect = fixSlownessEffect;
        bossRegistry.put(BossType.GIANT, giants);
        bossRegistry.put(BossType.BROOD_MOTHER, broodMothers);
        bossRegistry.put(BossType.EXILED_ONE, villagers);
    }

    // ---------------------------------------------------------------------
    // Public state accessors (used externally via FightingSkill)
    // ---------------------------------------------------------------------

    public List<GiantBoss> getGiants() {
        return giants;
    }

    public List<BroodMotherBoss> getBroodMothers() {
        return broodMothers;
    }

    public List<VillagerBoss> getVillagerBosses() {
        return villagers;
    }

    public boolean isBoss(Entity entity) {
        return entity.hasMetadata("boss") || findSummoner(entity).isPresent();
    }

    /** Register a freshly spawned boss for cross-listener summon tracking. */
    public void addBoss(Player p, Boss boss) {
        summonTracker.put(p, boss);
        if (boss instanceof GiantBoss g)
            giants.add(g);
        else if (boss instanceof BroodMotherBoss b)
            broodMothers.add(b);
        else if (boss instanceof VillagerBoss v)
            villagers.add(v);
    }

    /** Remove a tracked boss from its collection (admin tooling). */
    public void removeBoss(Boss boss) {
        if (boss instanceof GiantBoss g)
            giants.remove(g);
        else if (boss instanceof BroodMotherBoss b)
            broodMothers.remove(b);
        else if (boss instanceof VillagerBoss v)
            villagers.remove(v);
    }

    // ---------------------------------------------------------------------
    // Generic remove (replaces removeGiant / removeBroodMother / removeVillager)
    // ---------------------------------------------------------------------

    /**
     * Find and cleanly kill whatever boss is backed by {@code entity}, regardless of
     * its concrete type. Walks the type registry so new boss types are supported
     * automatically instead of needing a copy-pasted {@code removeXxx} method.
     */
    private void removeBossEntity(LivingEntity entity) {
        for (List<? extends Boss> bosses : bossRegistry.values()) {
            bosses.removeIf(b -> {
                if (Objects.equals(b.getBoss(), entity)) {
                    b.death();
                    return true;
                }
                return false;
            });
        }
    }

    private Optional<Player> findSummoner(Entity entity) {
        return summonTracker.entrySet().stream()
                .filter(entry -> Objects.equals(entry.getValue().getBoss(), entity))
                .map((Map.Entry<Player, Boss> entry) -> entry.getKey())
                .findFirst();
    }

    private Optional<Player> cleanupBossEntity(LivingEntity entity) {
        Optional<Player> summoner = removeSummonTracker(entity);
        removeBossEntity(entity);
        return summoner;
    }

    private Optional<Player> removeSummonTracker(Entity entity) {
        Iterator<Map.Entry<Player, Boss>> iterator = summonTracker.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Player, Boss> entry = iterator.next();
            if (!Objects.equals(entry.getValue().getBoss(), entity))
                continue;
            Player summoner = entry.getKey();
            iterator.remove();
            return Optional.of(summoner);
        }
        return Optional.empty();
    }

    // ---------------------------------------------------------------------
    // Death handlers
    // ---------------------------------------------------------------------

    public void handleBossKill(Player p, EntityDeathEvent e) {
        cleanupBossEntity(e.getEntity());

        Location entLocation = e.getEntity().getLocation();
        if (entLocation.getWorld() == null)
            return;
        List<ItemStack> drops = new ArrayList<>();

        BossType type = BossType.fromEntityType(e.getEntity().getType());
        if (type != null) {
            switch (type) {
                case GIANT -> {
                    giveBossItemOrDrop(p, type.getBossItem(), drops);
                    if (GiantSword.shouldDrop(Math.random()))
                        drops.add(ItemStackGenerator.getGiantSword());
                    broadcastSlain(type);
                    awardBossXP(p, type.getXpMultiplier());
                }
                case BROOD_MOTHER -> {
                    giveBossItemOrDrop(p, type.getBossItem(), drops);
                    if (Math.random() < BROODING_SILK_DROP_CHANCE)
                        drops.add(ItemStackGenerator.getBroodingSilk());
                    broadcastSlain(type);
                    awardBossXP(p, type.getXpMultiplier());
                }
                case EXILED_ONE -> {
                    giveBossItemOrDrop(p, type.getBossItem(), drops);
                    broadcastSlain(type);
                    awardBossXP(p, type.getXpMultiplier());
                }
                case ENDER_DRAGON -> dragonManager.handleDragonKill(p, entLocation.getWorld(), drops);
            }
        }

        for (ItemStack drop : drops)
            entLocation.getWorld().dropItemNaturally(entLocation, drop);
        e.setDroppedExp(0);
    }

    public void handleUnnaturalBossDeath(EntityDeathEvent e) {
        if (!isBoss(e.getEntity()))
            return;
        cleanupBossEntity(e.getEntity()).ifPresent((Player player) ->
                player.sendRawMessage(ChatColor.YELLOW + "Your boss died unnaturally"));
    }

    /** Called when the summoning player dies: tear down their boss broadcast the defeat. */
    public void handlePlayerBossCleanup(Player p) {
        if (!summonTracker.containsKey(p))
            return;
        Boss boss = summonTracker.get(p);
        if (boss != null) {
            LivingEntity entity = boss.getBoss();
            if (entity instanceof Villager) {
                for (VillagerBoss villager : villagers) {
                    if (!villager.getBoss().equals(entity))
                        continue;
                    ExiledBossMusic music = villager.getMusic();
                    if (music != null)
                        music.setDead(true);
                    villager.removeBossSummonedMobs();
                    fixSlownessEffect.add(p);
                }
            }
            boss.cleanup();
            Bukkit.broadcastMessage(
                    ChatColor.LIGHT_PURPLE + p.getDisplayName() + ChatColor.RED + ChatColor.BOLD
                            + " was bested by " + ChatColor.DARK_PURPLE + ChatColor.BOLD + boss.getName());
        }
        summonTracker.remove(p);
    }

    // ---------------------------------------------------------------------
    // Spawning
    // ---------------------------------------------------------------------

    /** Generic boss spawn driven by {@link BossType}, replacing the per-type switch. */
    public void spawnBoss(BossType type, Location loc, Player p, ItemStack mainHand) {
        if (hasActiveBoss(p)) {
            p.sendRawMessage(ChatColor.RED + "You can only summon one boss at a time");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }
        if (type.requiresNight() && !isNight(p.getWorld())) {
            p.sendRawMessage(ChatColor.RED + "You can only spawn the " + type.getSpawnName() + " at night");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }
        Boss boss = type.create(loc, p, noBossMusic.contains(p));
        if (boss == null) {
            p.sendRawMessage(ChatColor.RED + "Not enough space to spawn the " + type.getSpawnName());
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }
        boss.runTaskTimer(plugin, 0, 1);
        addBoss(p, boss);

        if (mainHand.getAmount() == 1)
            p.getInventory().setItemInMainHand(null);
        else
            mainHand.setAmount(mainHand.getAmount() - 1);
    }

    boolean hasActiveBoss(Player player) {
        Boss trackedBoss = summonTracker.get(player);
        if (trackedBoss == null) {
            summonTracker.remove(player);
            return false;
        }

        LivingEntity trackedEntity = trackedBoss.getBoss();
        if (trackedEntity != null && !trackedEntity.isDead() && trackedEntity.isValid())
            return true;

        summonTracker.remove(player);
        removeBoss(trackedBoss);
        trackedBoss.cleanup();
        return false;
    }

    public static boolean isSummoningBoss(ItemStack item) {
        if (ItemStackGeneratorUtils.isCustomItem(item, ItemModelData.GIANT_SUMMON.getId()))
            return true;
        if (ItemStackGeneratorUtils.isCustomItem(item, ItemModelData.BROOD_MOTHER_SUMMON.getId()))
            return true;
        return ItemStackGeneratorUtils.isCustomItem(item, ItemModelData.THE_EXILED_ONE_SUMMON.getId());
    }

    static boolean isNight(World world) {
        long time = world.getTime();
        return time > NIGHT_START_TIME && time < NIGHT_END_TIME;
    }

    // ---------------------------------------------------------------------
    // Boss-egg detection / item drop helper
    // ---------------------------------------------------------------------

    /**
     * Give {@code item} to {@code player}; if it does not fully fit (or there is no
     * player), keep it in {@code drops} so it can be scattered at the death location.
     */
    static void giveBossItemOrDrop(Player player, ItemStack item, List<ItemStack> drops) {
        Map<Integer, ItemStack> leftover = null;
        if (player != null)
            leftover = player.getInventory().addItem(item);
        if (leftover == null || !leftover.isEmpty())
            drops.add(item);
    }

    // ---------------------------------------------------------------------
    // Damage listeners
    // ---------------------------------------------------------------------

    public void handleBossDamage(EntityDamageEvent e) {
        if (!isBoss(e.getEntity()))
            return;
        if (e.getEntity().getType().equals(EntityType.ENDER_DRAGON)) {
            dragonManager.handleDragonDamageImmunity(e);
            return;
        }
        if (!(e instanceof EntityDamageByEntityEvent)) {
            e.setCancelled(true);
            return;
        }

        EntityDamageEvent.DamageCause cause = e.getCause();
        EntityType type = e.getEntity().getType();
        if (type.equals(EntityType.VILLAGER)) {
            if (cause.equals(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION))
                e.setCancelled(true);
            if (cause.equals(EntityDamageEvent.DamageCause.LIGHTNING))
                e.setCancelled(true);
            return;
        }
        if ((type.equals(EntityType.ZOMBIE) || type.equals(EntityType.SPIDER))
                && cause.equals(EntityDamageEvent.DamageCause.FALL))
            e.setCancelled(true);
    }

    public void handleExiledDamage(EntityDamageByEntityEvent e) {
        if (!isBoss(e.getEntity()))
            return;
        if (!e.getEntity().getType().equals(EntityType.VILLAGER))
            return;
        VillagerBoss boss = null;
        for (VillagerBoss villager : villagers) {
            if (!villager.getBoss().equals(e.getEntity()))
                continue;
            boss = villager;
            break;
        }
        if (boss == null)
            return;

        Player p = getAttackingPlayer(e).orElse(null);
        if (p == null && (e.getDamager() instanceof Arrow || e.getDamager() instanceof Trident)) {
            e.setCancelled(true);
            return;
        }

        if (boss.isHitPhase())
            return;
        if (boss.isHealing()) {
            if (p == null)
                return;
            if (e.getDamager() instanceof Arrow)
                boss.incrementArrow();
            e.setDamage(1);
            return;
        }

        if (p == null)
            return;
        e.setCancelled(true);
        p.sendRawMessage(
                ChatColor.YELLOW.toString() + ChatColor.BOLD + "The Exiled One's magic shield prevents damage!");
        p.playSound(p, Sound.BLOCK_ANVIL_LAND, 1, 1);
    }

    public void scaleExiledDamage(EntityDamageEvent e) {
        if (!isBoss(e.getEntity()) || !e.getEntity().getType().equals(EntityType.VILLAGER))
            return;
        for (VillagerBoss villager : villagers) {
            if (!villager.getBoss().equals(e.getEntity()))
                continue;
            e.setDamage(villager.scaleIncomingDamage(e.getDamage()));
            return;
        }
    }

    public void handleBossDamageByCorrectPlayer(EntityDamageByEntityEvent e) {
        if (!isBoss(e.getEntity()))
            return;
        Optional<Player> attacker = getAttackingPlayer(e);
        if (e.getEntity().getType().equals(EntityType.ENDER_DRAGON)) {
            if (attacker.isEmpty()) {
                e.setCancelled(true);
                return;
            }
            dragonManager.handleDragonDamageByCorrectPlayer(e, attacker.get());
            return;
        }

        Optional<Player> summoner = findSummoner(e.getEntity());
        if (attacker.isEmpty() || summoner.isEmpty()
                || !Objects.equals(attacker.get(), summoner.get()))
            e.setCancelled(true);
    }

    public void handleVillagerTransform(EntityTransformEvent e) {
        if (!(e.getEntity() instanceof Villager))
            return;
        if (!isBoss(e.getEntity()))
            return;
        if (e.getTransformReason() == EntityTransformEvent.TransformReason.LIGHTNING)
            e.setCancelled(true);
    }

    public void handleSpiderBite(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player))
            return;
        if (!isBoss(e.getDamager()))
            return;
        if (!e.getDamager().getType().equals(EntityType.SPIDER))
            return;
        Spider spider = (Spider) e.getDamager();
        AttributeInstance healthAttribute = spider.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (healthAttribute == null)
            return;
        double maxHealth = healthAttribute.getValue();
        spider.setHealth(Math.min(maxHealth, spider.getHealth() + (SPIDER_BITE_HEAL_RATIO * e.getDamage())));

        for (int i = 0; i < SPIDER_BITE_PARTICLE_COUNT; i++) {
            spider.getWorld().spawnParticle(Particle.DUST, spider.getLocation(), SPIDER_BITE_PARTICLES_PER,
                    Math.random(), 0.5, Math.random(), 0, new Particle.DustOptions(Color.RED, 1));
        }
    }

    // ---------------------------------------------------------------------
    // Attacker resolution
    // ---------------------------------------------------------------------

    /**
     * Resolve the attacking player behind a damage event, unwrapping projectiles
     * to their shooter. Returns {@link Optional#empty()} when there is no player
     * attacker, so callers cannot forget the absent case.
     */
    static Optional<Player> getAttackingPlayer(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player player)
                return Optional.of(player);
        } else if (e.getDamager() instanceof Player player) {
            return Optional.of(player);
        }
        return Optional.empty();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private void broadcastSlain(BossType type) {
        Bukkit.broadcastMessage(
                ChatColor.AQUA + type.getDisplayName() + ChatColor.LIGHT_PURPLE + " has been slain!");
    }

    private void awardBossXP(Player p, double multiplier) {
        SkillManager.experienceEvent(plugin, p, plugin.getSkillManager().getFightingXP() * multiplier,
                SkillCategory.FIGHTING);
    }
}
