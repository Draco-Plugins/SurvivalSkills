package sir_draco.survivalskills.skill_listeners;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import sir_draco.survivalskills.abilities.items.CaveFinderAsync;
import sir_draco.survivalskills.abilities.items.Magnet;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.skills.SkillManager;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;
import sir_draco.survivalskills.rewards.PlayerRewards;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.skill_listeners.ArmorListener.ArmorType;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class ExploringSkill implements Listener {
    private static final int STEP_XP_THRESHOLD = 100;
    private static final double SWIM_SPEED_GILL_MULTIPLIER = 0.6;
    private static final double SWIM_SPEED_BOOTS_MULTIPLIER = 0.2;
    private static final int SWIM_SPEED_TASK_INTERVAL = 3;

    private static final Set<UUID> activeMagnets = new HashSet<>();
    private final HashMap<UUID, BukkitTask> magnetTasks = new HashMap<>();
    private final HashMap<UUID, BukkitTask> swimTasks = new HashMap<>();

    private final SurvivalSkills plugin;
    private final HashMap<UUID, Location> locationTracker = new HashMap<>();
    private final HashMap<UUID, Integer> stepCounter = new HashMap<>();

    public static Set<UUID> getActiveMagnetPlayers() {
        return Collections.unmodifiableSet(activeMagnets);
    }

    public ExploringSkill(SurvivalSkills plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void vehicleMoveEvent(VehicleMoveEvent e) {
        if (e.getVehicle().getPassengers().isEmpty()) return;
        if (e.getVehicle().getPassengers().getFirst().getType() != EntityType.PLAYER) return;
        handleMovement((Player) e.getVehicle().getPassengers().getFirst());
    }

    @EventHandler
    public void onTravel(PlayerMoveEvent e) {
        handleMovement(e.getPlayer());
    }

    private void handleMovement(Player p) {
        UUID uuid = p.getUniqueId();
        Location loc = p.getLocation().getBlock().getLocation();
        if (!locationTracker.containsKey(uuid)) locationTracker.put(uuid, loc);
        if (!stepCounter.containsKey(uuid)) stepCounter.put(uuid, 0);
        if (sameBlock(locationTracker.get(uuid), loc)) return;

        if (loc.getWorld() == null) return;
        World locTrackerWorld = locationTracker.get(uuid).getWorld();
        if (locTrackerWorld == null) {
            locationTracker.put(uuid, loc);
            return;
        }
        if (!loc.getWorld().getEnvironment().equals(locTrackerWorld.getEnvironment())) {
            locationTracker.put(uuid, loc);
            return;
        }

        double distanceCovered = Math.ceil(loc.distance(locationTracker.get(uuid)));
        locationTracker.put(uuid, loc);
        int steps = stepCounter.get(uuid) + (int) distanceCovered;
        if (steps < STEP_XP_THRESHOLD) {
            stepCounter.put(uuid, steps);
            return;
        }

        SkillManager.experienceEvent(plugin, p,
                plugin.getSkillManager().getExploringXP() * STEP_XP_THRESHOLD, SkillCategory.EXPLORING);
        stepCounter.put(uuid, 0);
    }

    @EventHandler
    public void exploringSkill(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (p.isSwimming() && !swimTasks.containsKey(p.getUniqueId())
                && plugin.getSkillManager().getPlayerRewards(p).getSwimSpeed() > 0) {
            startSwimTask(p);
        }
        if (!p.hasPotionEffect(PotionEffectType.REGENERATION)) {
            if (!plugin.getSkillManager().getPlayerRewards(p).getReward(SkillCategory.EXPLORING, "HealthRegen").isApplied()) return;
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 0, true, false));
        }
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (ArmorListener.isWearingArmor(p.getUniqueId(), ArmorType.ADVENTURER)) {
            e.setCancelled(true);
            return;
        }
        if (ArmorListener.isWearingArmor(p.getUniqueId(), ArmorType.JUMPING_BOOTS)) {
            e.setCancelled(true);
            return;
        }

        if (!rewards.getReward(SkillCategory.EXPLORING, "FallI").isApplied()) return;
        if (rewards.getReward(SkillCategory.EXPLORING, "FallII").isApplied()) {
            e.setDamage(e.getDamage() / 2); // 50% reduction
        } else {
            e.setDamage(e.getDamage() - e.getDamage() / 4); // 25% reduction
        }
    }

    @EventHandler
    public void useCaveFinder(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (e.getHand() != EquipmentSlot.HAND) return;
        PlayerInventory inv = p.getInventory();
        if (!ItemStackGeneratorUtils.isCustomItem(inv.getItemInMainHand(), 6)
                && !ItemStackGeneratorUtils.isCustomItem(inv.getItemInOffHand(), 6))
            return;
        if (!plugin.getSkillManager().getPlayerRewards(p).getReward(SkillCategory.EXPLORING, "CaveFinder").isApplied()) {
            p.sendRawMessage(ChatColor.RED + "You must be exploring level " + ChatColor.AQUA
                    + plugin.getSkillManager().getDefaultPlayerRewards().getReward(SkillCategory.EXPLORING, "CaveFinder")
                            .getLevel());
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }
        CaveFinderAsync task = new CaveFinderAsync(p, plugin);
        task.runTaskAsynchronously(plugin);
    }

    @EventHandler
    public void onItemHandChange(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        ItemStack mainHand = p.getInventory().getItem(e.getNewSlot());
        UUID uuid = p.getUniqueId();

        if (!ItemStackGeneratorUtils.isCustomItem(mainHand, 32)) {
            activeMagnets.remove(uuid);
            cancelMagnetTask(uuid);
            return;
        }

        if (activeMagnets.contains(uuid)) return;

        if (!plugin.getSkillManager().getPlayerRewards(p).getReward(SkillCategory.EXPLORING, "Magnet").isApplied()) {
            p.sendRawMessage(ChatColor.RED + "You must be exploring level " + ChatColor.AQUA
                    + plugin.getSkillManager().getDefaultPlayerRewards().getReward(SkillCategory.EXPLORING, "Magnet").getLevel());
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }
        activeMagnets.add(uuid);
        cancelMagnetTask(uuid);
        BukkitTask task = new Magnet(p).runTaskTimer(plugin, 0, 5);
        magnetTasks.put(uuid, task);
    }

    private void cancelMagnetTask(UUID uuid) {
        BukkitTask existingTask = magnetTasks.remove(uuid);
        if (existingTask != null) {
            existingTask.cancel();
        }
    }

    public int getPlayerSteps(UUID uuid) {
        if (!stepCounter.containsKey(uuid)) return 0;
        return stepCounter.get(uuid);
    }

    public boolean sameBlock(Location loc1, Location loc2) {
        return loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockY() == loc2.getBlockY()
                && loc1.getBlockZ() == loc2.getBlockZ()
                && Objects.equals(loc1.getWorld(), loc2.getWorld());
    }

    private void startSwimTask(Player p) {
        UUID uuid = p.getUniqueId();
        swimTasks.put(uuid, new BukkitRunnable() {
            @Override
            public void run() {
                double speed = plugin.getSkillManager().getPlayerRewards(p).getSwimSpeed();
                if (!p.isSwimming() || !p.isOnline() || speed == 0) {
                    cancel();
                    swimTasks.remove(uuid);
                    return;
                }

                double multiplier = ArmorListener.isWearingArmor(p.getUniqueId(), ArmorType.GILL)
                        ? SWIM_SPEED_GILL_MULTIPLIER
                        : SWIM_SPEED_BOOTS_MULTIPLIER;

                p.setVelocity(p.getLocation().getDirection().multiply(speed * multiplier));
            }
        }.runTaskTimer(plugin, 0, SWIM_SPEED_TASK_INTERVAL));
    }
}
