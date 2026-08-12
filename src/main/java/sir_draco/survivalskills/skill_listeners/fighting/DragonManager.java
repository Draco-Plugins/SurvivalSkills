package sir_draco.survivalskills.skill_listeners.fighting;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.bosses.DragonBoss;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.skills.SkillManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Dragon-boss lifecycle: world attach/respawn, End-portal gating, the unique
 * first-kill vs subsequent-kill XP, the per-player damage gate, and the
 * life-steal triggered when a player dies in the End. All dragon state lives
 * here instead of being sprinkled across the monolithic fighting listener.
 */
public class DragonManager {

    private static final String KILLED_FIRST_DRAGON_META = "killedfirstdragon";
    private static final double DRAGON_FIRST_KILL_XP_MULTIPLIER = 500.0;
    private static final double DRAGON_SUBSEQUENT_KILL_XP_MULTIPLIER = 100.0;
    private static final int DRAGON_BASE_HEALTH = 250;
    private static final int DRAGON_ATTACH_DELAY_TICKS = 20;

    private final SurvivalSkills plugin;
    private DragonBoss dragonBoss;

    public DragonManager(SurvivalSkills plugin) {
        this.plugin = plugin;
    }

    public DragonBoss getDragonBoss() {
        return dragonBoss;
    }

    public boolean isFlightSuppressed(Player player) {
        return dragonBoss != null && dragonBoss.isFlightSuppressed(player);
    }

    // ---- Damage gates -----------------------------------------------------

    /** Cancel explosion/lightning damage to the dragon (matches the original immunity list). */
    void handleDragonDamageImmunity(EntityDamageEvent e) {
        EntityDamageEvent.DamageCause cause = e.getCause();
        if (cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                || cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                || cause == EntityDamageEvent.DamageCause.LIGHTNING)
            e.setCancelled(true);
    }

    /** Only the dragon's own fight participants may damage it. */
    void handleDragonDamageByCorrectPlayer(EntityDamageByEntityEvent e, Player p) {
        if (dragonBoss == null) return;
        if (!dragonBoss.getBoss().equals(e.getEntity())) e.setCancelled(true);
        if (!dragonBoss.isRespawn()) return;
        if (dragonBoss.getPlayers().contains(p)) return;
        e.setCancelled(true);
    }

    // ---- Death & XP -------------------------------------------------------

    /**
     * Award the dragon boss-item drop, run its death animation and award XP to End
     * players. Ordering is deliberately identical to the original: the death
     * animation runs before the metadata check, so {@code killedfirstdragon} is
     * already set by the animation when the XP loop reads it (preserving the
     * original behaviour exactly).
     */
    void handleDragonKill(Player killer, World world, List<ItemStack> drops) {
        if (dragonBoss == null) {
            Bukkit.getLogger().warning("Error finding custom dragon");
            return;
        }
        BossManager.giveBossItemOrDrop(killer, BossType.ENDER_DRAGON.getBossItem(), drops);
        dragonBoss.deathAnimation();
        dragonBoss = null;

        double base = plugin.getSkillManager().getFightingXP();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().getEnvironment().equals(World.Environment.THE_END))
                continue;
            if (!world.hasMetadata(KILLED_FIRST_DRAGON_META))
                SkillManager.experienceEvent(plugin, player, base * DRAGON_FIRST_KILL_XP_MULTIPLIER, SkillCategory.FIGHTING);
            else
                SkillManager.experienceEvent(plugin, player, base * DRAGON_SUBSEQUENT_KILL_XP_MULTIPLIER, SkillCategory.FIGHTING);
        }

        if (!world.hasMetadata(KILLED_FIRST_DRAGON_META))
            world.setMetadata(KILLED_FIRST_DRAGON_META, new FixedMetadataValue(plugin, true));
    }

    /** The dragon feeds when a player dies in the End. */
    public void onPlayerDeathCleanup(Player p) {
        if (p.getWorld().getEnvironment().equals(World.Environment.THE_END) && dragonBoss != null)
            dragonBoss.lifeSteal();
    }

    // ---- Spawn / respawn --------------------------------------------------

    public void teleportToEnd(PlayerTeleportEvent e) {
        if (!e.getCause().equals(PlayerTeleportEvent.TeleportCause.END_PORTAL)) return;
        if (dragonBoss != null) {
            if (dragonBoss.isRespawn() && dragonBoss.getPlayers().isEmpty()) {
                dragonBoss.addPlayer(e.getPlayer());
            } else if (!dragonBoss.isRespawn() && !dragonBoss.getPlayers().contains(e.getPlayer())) {
                dragonBoss.addPlayer(e.getPlayer());
            } else {
                e.getPlayer().sendRawMessage(ChatColor.LIGHT_PURPLE + ChatColor.BOLD.toString() + "Ender Dragon: "
                        + ChatColor.RESET + ChatColor.DARK_AQUA + "So you wish to die again " + e.getPlayer().getName()
                        + "?");
                e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.AMBIENT_CAVE, 1, 1);
                return;
            }
        }

        // Get the ender dragon if it is alive
        if (e.getTo() == null) return;
        World world = e.getTo().getWorld();
        if (world == null) return;
        if (world.getEnvironment().equals(World.Environment.NORMAL)) {
            world = Bukkit.getWorld(world.getName() + "_the_end");
        }
        if (world == null) return;
        if (world.hasMetadata(KILLED_FIRST_DRAGON_META)) return;

        World finalWorld = world;
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity entity : finalWorld.getEntities()) {
                    if (!entity.getType().equals(EntityType.ENDER_DRAGON)) continue;
                    LivingEntity ent = (LivingEntity) entity;
                    dragonBoss = DragonBoss.attachToDragon("dragon", 0, 0,
                            DRAGON_BASE_HEALTH * Bukkit.getOnlinePlayers().size(), 20, 5, 1, ent);
                    List<Player> players = Bukkit.getOnlinePlayers().stream()
                            .filter((Player player) -> player.getWorld().getEnvironment()
                                    .equals(World.Environment.THE_END))
                            .map((Player player) -> player)
                            .toList();
                    dragonBoss.initializePlayers(players);
                    dragonBoss.runTaskTimer(plugin, 0, 1);
                    return;
                }
                // No dragon present: the first dragon was already defeated.
                finalWorld.setMetadata(KILLED_FIRST_DRAGON_META, new FixedMetadataValue(plugin, true));
            }
        }.runTaskLater(plugin, DRAGON_ATTACH_DELAY_TICKS);
    }

    public void dragonSpawnEvent(EntitySpawnEvent e) {
        // Check if this is a dragon re-spawn
        if (!e.getEntity().getType().equals(EntityType.ENDER_DRAGON)) return;
        World world = e.getLocation().getWorld();
        if (world == null) return;
        if (!world.getEnvironment().equals(World.Environment.THE_END)) return;
        if (!world.hasMetadata(KILLED_FIRST_DRAGON_META)) return;

        // Get the players in the end to determine dragon health
        List<Player> players = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().getEnvironment().equals(World.Environment.THE_END)) continue;
            players.add(player);
        }
        int health = players.isEmpty() ? DRAGON_BASE_HEALTH : DRAGON_BASE_HEALTH * players.size();

        dragonBoss = DragonBoss.attachToDragon("dragon", 0, 0, health, 0, 0, 0,
                (LivingEntity) e.getEntity());
        dragonBoss.runTaskTimer(plugin, 0, 1);
        dragonBoss.respawnDragonInitPlayers(players);
    }
}
