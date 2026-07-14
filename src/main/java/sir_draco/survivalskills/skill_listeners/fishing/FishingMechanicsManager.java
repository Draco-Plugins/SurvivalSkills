package sir_draco.survivalskills.skill_listeners.fishing;

import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.skills.SkillManager;
import sir_draco.survivalskills.trophy.TrophyType;
import sir_draco.survivalskills.utils.ProjectileCalculator;
import sir_draco.survivalskills.utils.items.ItemStackGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Owns the fishing event pipeline: lure-time calculation, the rain-fishing
 * speedup runnable, item-drop projection, experience/durability accounting and
 * the fishing-boss spawn gate. It delegates drop selection to
 * {@link FishingLootManager} and exposes {@link #spawnFishingBoss} for the admin
 * command API.
 *
 * <p>Previously all of this lived in the {@code onPlayerFish} God method in
 * {@code FishingSkill}, including a duplicated boss-spawn branch whose only
 * difference was the spawn probability (1% before the trophy, 0.4% after).</p>
 */
public class FishingMechanicsManager {

    // Lure-time tuning
    private static final int MAX_LURE_LEVEL = 3;
    private static final int LURE_MIN_TICK_REDUCTION = 10;
    private static final int LURE_MAX_TICK_REDUCTION = 20;
    private static final int MIN_TICK_SPEED_FLOOR = 1;
    private static final int MIN_TICK_SPEED_CAP = 79;
    private static final int MAX_TICK_SPEED_OFFSET = 5;
    private static final int SKY_INFLUENCED_MULTIPLIER = 2;

    // Rain-fishing speedup
    private static final int RAIN_MIN_SPEED_BONUS = 10;
    private static final int RAIN_MIN_SPEED_FLOOR = 10;

    // Projectiles
    private static final double ITEM_PROJECTILE_SPEED = 0.5;
    private static final double ENTITY_PROJECTILE_SPEED = 1.0;

    // Durability
    private static final double UNBREAKING_CHANCE_BASE = 100.0;

    // Fishing XP orb
    private static final int FISHING_XP_ROLL_BOUND = 6;

    // Fishing boss
    private static final String FISHING_BOSS_METADATA = "fishingboss";
    private static final double BOSS_HEALTH = 100.0;
    private static final double BOSS_MOVEMENT_SPEED = 0.35;
    private static final int BOSS_THORNS_LEVEL = 6;
    private static final int BOSS_IMPALING_LEVEL = 10;
    private static final double BOSS_SPAWN_CHANCE_NO_TROPHY = 0.01;
    private static final double BOSS_SPAWN_CHANCE_WITH_TROPHY = 0.004;

    private final SurvivalSkills plugin;
    private final FishingLootManager lootManager;

    // Tracks the active rain-fishing runnable per player so a stale hook can be
    // removed if the player recasts. The id is atomically incremented so a future
    // async refactor cannot race the counter.
    private final AtomicInteger rainFisherId = new AtomicInteger(1);
    private final Map<Player, Integer> rainFishers = new HashMap<>();

    public FishingMechanicsManager(SurvivalSkills plugin, FishingLootManager lootManager) {
        this.plugin = plugin;
        this.lootManager = lootManager;
    }

    // =================================================================
    // Fishing event pipeline
    // =================================================================

    public void onPlayerFish(PlayerFishEvent e) {
        if (e.getState() == PlayerFishEvent.State.REEL_IN
                || e.getState() == PlayerFishEvent.State.FAILED_ATTEMPT)
            rainFishers.remove(e.getPlayer());

        if (e.getState() == PlayerFishEvent.State.FISHING) {
            handleFishingState(e);
            return;
        }

        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH)
            return;
        if (e.getCaught() == null)
            return;

        Player p = e.getPlayer();

        // The fishing boss spawn gate consumes the caught entity before loot rolls.
        if (trySpawnFishingBoss(p, e))
            return;

        ItemStack rod = p.getInventory().getItemInMainHand();
        int luckLevel = getLuckOfTheSeaLevel(rod);
        ArrayList<ItemStack> items = lootManager.getItemsToDrop(p, luckLevel);

        Location loc = e.getHook().getLocation();
        World world = e.getHook().getWorld();
        Vector velocity = ProjectileCalculator.getItemProjectileVector(loc, p.getLocation(), ITEM_PROJECTILE_SPEED);
        e.getCaught().remove();

        dropItems(items, world, loc, velocity);
        SkillManager.experienceEvent(plugin, p, plugin.getSkillManager().getFishingXP(), SkillCategory.FISHING);
    }

    private void handleFishingState(PlayerFishEvent e) {
        FishHook hook = e.getHook();
        Player p = e.getPlayer();
        ItemStack rod = p.getInventory().getItemInMainHand();

        int lureLevel = 0;
        if (rod.containsEnchantment(Enchantment.LURE))
            lureLevel = Math.min(MAX_LURE_LEVEL, rod.getEnchantmentLevel(Enchantment.LURE));

        int minSpeed = plugin.getSkillManager().getPlayerRewards(p).getFishingMinTickSpeed();
        int maxSpeed = plugin.getSkillManager().getPlayerRewards(p).getFishingMaxTickSpeed();
        minSpeed = Math.min(Math.max(MIN_TICK_SPEED_FLOOR, minSpeed - (lureLevel * LURE_MIN_TICK_REDUCTION)), MIN_TICK_SPEED_CAP);
        maxSpeed = Math.max(minSpeed + MAX_TICK_SPEED_OFFSET, maxSpeed - (lureLevel * LURE_MAX_TICK_REDUCTION));

        if (!hook.isSkyInfluenced()) {
            minSpeed *= SKY_INFLUENCED_MULTIPLIER;
            maxSpeed *= SKY_INFLUENCED_MULTIPLIER;
        } else if (hook.getWorld().hasStorm()) {
            scheduleRainFishing(p, hook, rod, minSpeed);
            return;
        }

        hook.setMinLureTime(minSpeed);
        hook.setMaxLureTime(maxSpeed);
    }

    private void scheduleRainFishing(Player p, FishHook hook, ItemStack rod, int minSpeed) {
        int pID = rainFisherId.getAndIncrement();
        rainFishers.put(p, pID);
        int speed = Math.max(RAIN_MIN_SPEED_FLOOR, minSpeed + RAIN_MIN_SPEED_BONUS);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!hook.getLocation().getBlock().getType().equals(Material.WATER))
                    return;
                if (!rainFishers.containsKey(p) || rainFishers.get(p) != pID) {
                    hook.remove();
                    return;
                }

                int luckLevel = getLuckOfTheSeaLevel(rod);
                ArrayList<ItemStack> items = lootManager.getItemsToDrop(p, luckLevel);

                Location loc = hook.getLocation();
                World world = hook.getWorld();
                Vector velocity = ProjectileCalculator.getItemProjectileVector(loc, p.getLocation(), ITEM_PROJECTILE_SPEED);
                dropItems(items, world, loc, velocity);

                handleFishingExperience(p);
                handleDurability(p, rod);
                SkillManager.experienceEvent(plugin, p, plugin.getSkillManager().getFishingXP(), SkillCategory.FISHING);
                hook.remove();
                rainFishers.remove(p);
            }
        }.runTaskLater(plugin, speed);
    }

    private void dropItems(ArrayList<ItemStack> items, World world, Location loc, Vector velocity) {
        if (items.isEmpty())
            return;
        for (ItemStack item : items)
            world.dropItem(loc, item).setVelocity(velocity);
    }

    private int getLuckOfTheSeaLevel(ItemStack rod) {
        if (!rod.containsEnchantment(Enchantment.LUCK_OF_THE_SEA))
            return 0;
        return rod.getEnchantmentLevel(Enchantment.LUCK_OF_THE_SEA);
    }

    // =================================================================
    // Fishing boss
    // =================================================================

    /**
     * Spawns the fishing boss for {@code p} based on the {@code FishingKing}
     * reward. The spawn probability depends on whether the player has built the
     * fishing trophy, but the spawn sequence itself is identical, so the two
     * previously duplicated branches collapse into a single threshold lookup.
     * Returns {@code true} if a boss was spawned (and the caller should stop).
     */
    private boolean trySpawnFishingBoss(Player p, PlayerFishEvent e) {
        if (!plugin.getSkillManager().getPlayerRewards(p)
                .getReward(SkillCategory.FIGHTING, "FishingKing").isApplied())
            return false;

        boolean hasTrophy = plugin.getTrophyManager().getTrophyTracker()
                .get(p.getUniqueId()).get(TrophyType.FISHING);
        double threshold = hasTrophy ? BOSS_SPAWN_CHANCE_WITH_TROPHY : BOSS_SPAWN_CHANCE_NO_TROPHY;

        if (Math.random() > threshold)
            return false;

        Location loc = e.getHook().getLocation();
        World world = e.getHook().getWorld();
        Vector velocity = ProjectileCalculator.getLivingEntityProjectileVector(loc, p.getLocation(), ENTITY_PROJECTILE_SPEED, true);
        e.getCaught().remove();
        spawnFishingBoss(world, loc, velocity);
        return true;
    }

    /**
     * Spawns an armoured drowned "fishing boss" at {@code loc} launched with
     * {@code velocity}. Public so the admin boss command can spawn it directly.
     */
    public void spawnFishingBoss(World world, Location loc, Vector velocity) {
        Drowned boss = (Drowned) world.spawnEntity(loc, EntityType.DROWNED);
        boss.setVelocity(velocity);
        boss.setMetadata(FISHING_BOSS_METADATA, new FixedMetadataValue(plugin, true));

        ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
        ItemStack chestplate = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemStack leggings = new ItemStack(Material.DIAMOND_LEGGINGS);
        ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
        ItemStack trident = new ItemStack(Material.TRIDENT);

        helmet.addUnsafeEnchantment(Enchantment.THORNS, BOSS_THORNS_LEVEL);
        chestplate.addUnsafeEnchantment(Enchantment.THORNS, BOSS_THORNS_LEVEL);
        leggings.addUnsafeEnchantment(Enchantment.THORNS, BOSS_THORNS_LEVEL);
        boots.addUnsafeEnchantment(Enchantment.THORNS, BOSS_THORNS_LEVEL);
        trident.addUnsafeEnchantment(Enchantment.IMPALING, BOSS_IMPALING_LEVEL);

        if (boss.getEquipment() == null)
            return;
        boss.getEquipment().setHelmet(helmet);
        boss.getEquipment().setChestplate(chestplate);
        boss.getEquipment().setLeggings(leggings);
        boss.getEquipment().setBoots(boots);
        boss.getEquipment().setItemInMainHand(trident);

        AttributeInstance healthAttribute = boss.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttribute != null)
            healthAttribute.setBaseValue(BOSS_HEALTH);
        boss.setHealth(BOSS_HEALTH);
        AttributeInstance speedInstance = boss.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedInstance != null)
            speedInstance.setBaseValue(BOSS_MOVEMENT_SPEED);
    }

    /**
     * Replaces the drowned's drops with the fishing-boss trophy item.
     */
    public void killFishingBoss(EntityDeathEvent e) {
        if (!e.getEntity().hasMetadata(FISHING_BOSS_METADATA))
            return;
        e.getDrops().clear();
        e.setDroppedExp(0);
        e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(),
                ItemStackGenerator.getFishingBossItem());
    }

    // =================================================================
    // Experience + durability
    // =================================================================

    /**
     * Applies the skill experience multiplier to all vanilla XP orbs a player
     * picks up.
     */
    public void modifyExperience(PlayerExpChangeEvent e) {
        Player p = e.getPlayer();
        double xpMultiplier = plugin.getSkillManager().getPlayerRewards(p).getExperienceMultiplier();
        e.setAmount((int) (e.getAmount() * xpMultiplier));
    }

    public void handleFishingExperience(Player p) {
        int xp = (int) (Math.random() * FISHING_XP_ROLL_BOUND);
        int xpMultiplier = (int) plugin.getSkillManager().getPlayerRewards(p).getExperienceMultiplier();
        if (xp != 0)
            p.getWorld().spawn(p.getLocation(), ExperienceOrb.class).setExperience(xp * xpMultiplier);
    }

    public void handleDurability(Player p, ItemStack fishingRod) {
        if (fishingRod == null)
            return;
        if (!fishingRod.getType().equals(Material.FISHING_ROD))
            return;
        Damageable meta = (Damageable) fishingRod.getItemMeta();
        if (meta == null)
            return;

        if (fishingRod.containsEnchantment(Enchantment.UNBREAKING)) {
            double chance = Math.random() * UNBREAKING_CHANCE_BASE;
            int level = fishingRod.getEnchantmentLevel(Enchantment.UNBREAKING);
            if (chance <= (UNBREAKING_CHANCE_BASE / (level + 1)))
                meta.setDamage(meta.getDamage() + 1);
        } else if (fishingRod.getType().getMaxDurability() <= meta.getDamage()) {
            p.playEffect(EntityEffect.BREAK_EQUIPMENT_MAIN_HAND);
            fishingRod.setAmount(0);
            return;
        } else {
            meta.setDamage(meta.getDamage() + 1);
        }
        fishingRod.setItemMeta(meta);
    }
}