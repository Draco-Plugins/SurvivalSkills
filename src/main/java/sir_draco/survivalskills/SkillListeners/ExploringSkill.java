package sir_draco.survivalskills.SkillListeners;

import org.bukkit.Location;
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
import org.bukkit.util.Vector;
import sir_draco.survivalskills.Abilities.CaveFinderAsync;
import sir_draco.survivalskills.Abilities.Magnet;
import sir_draco.survivalskills.Utils.ItemStackGenerator;
import sir_draco.survivalskills.Rewards.PlayerRewards;
import sir_draco.survivalskills.Skills.Skill;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class ExploringSkill implements Listener {
    public static final ArrayList<Player> activeMagnets = new ArrayList<>();

    private final SurvivalSkills plugin;
    private final HashMap<UUID, Location> locationTracker = new HashMap<>();
    private final HashMap<UUID, Integer> stepCounter = new HashMap<>();

    public ExploringSkill(SurvivalSkills plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void vehicleMoveEvent(VehicleMoveEvent e) {
        if (e.getVehicle().getPassengers().isEmpty()) return;
        if (e.getVehicle().getPassengers().get(0).getType() != EntityType.PLAYER) return;
        Player p = (Player) e.getVehicle().getPassengers().get(0);
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
        if (steps < 100) {
            stepCounter.put(p.getUniqueId(), steps);
            return;
        }
        else stepCounter.put(p.getUniqueId(), steps - 100);

        Skill.experienceEvent(plugin, p, plugin.getSkillManager().getExploringXP() * 100, "Exploring");
        stepCounter.put(uuid, 0);
    }

    @EventHandler
    public void onTravel(PlayerMoveEvent e) {
        Player p = e.getPlayer();
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
        if (steps < 100) {
            stepCounter.put(p.getUniqueId(), steps);
            return;
        }
        else stepCounter.put(p.getUniqueId(), steps - 100);

        Skill.experienceEvent(plugin, p, plugin.getSkillManager().getExploringXP() * 100, "Exploring");
        stepCounter.put(uuid, 0);
    }

    @EventHandler
    public void exploringSkill(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (p.isSwimming()) setSwimSpeed(p);
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (ArmorListener.playersWearingAdventurerArmor.contains(p.getUniqueId())) {
            e.setCancelled(true);
            return;
        }
        if (ArmorListener.playersWearingJumpingBoots.contains(p.getUniqueId())) {
            e.setCancelled(true);
            return;
        }

        if (!rewards.getReward("Exploring", "FallI").isApplied()) return;
        if (rewards.getReward("Exploring", "FallII").isApplied()) e.setDamage(e.getDamage() / 2);
        else e.setDamage(e.getDamage() / 4);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (e.getHand() != EquipmentSlot.HAND) return;
        PlayerInventory inv = p.getInventory();
        if (!ItemStackGenerator.isCustomItem(inv.getItemInMainHand(), 6)
                && !ItemStackGenerator.isCustomItem(inv.getItemInOffHand(), 6)) return;
        if (!plugin.getSkillManager().getPlayerRewards(p).getReward("Exploring", "CaveFinder").isApplied()) return;
        CaveFinderAsync task = new CaveFinderAsync(p, plugin);
        task.runTaskAsynchronously(plugin);
    }

    @EventHandler
    public void onItemHandChange(PlayerItemHeldEvent e) {
        Player p = e.getPlayer();
        ItemStack mainHand = p.getInventory().getItem(e.getNewSlot());
        if (!ItemStackGenerator.isCustomItem(mainHand, 32)) {
            activeMagnets.remove(p);
            return;
        }

        if (activeMagnets.contains(p)) return;

        if (!plugin.getSkillManager().getPlayerRewards(p).getReward("Exploring", "Magnet").isApplied()) return;
        activeMagnets.add(p);
        new Magnet(p).runTaskTimer(plugin, 0, 5);
    }

    public int getPlayerSteps(UUID uuid) {
        if (!stepCounter.containsKey(uuid)) return 0;
        return stepCounter.get(uuid);
    }

    public boolean sameBlock(Location loc1, Location loc2) {
        if (loc1.getWorld() == null) return true;
        if (loc2.getWorld() == null) return true;
        if (!loc1.getWorld().getEnvironment().equals(loc2.getWorld().getEnvironment())) return false;
        if (loc1.getBlockX() != loc2.getBlockX()) return false;
        if (loc1.getBlockY() != loc2.getBlockY()) return false;
        return (loc1.getBlockZ() == loc2.getBlockZ());
    }

    public void setSwimSpeed(Player p) {
        double speed = plugin.getSkillManager().getPlayerRewards(p).getSwimSpeed();
        if (speed == 0) return;

        if (ArmorListener.playersWearingGillArmor.contains(p.getUniqueId())) {
            speed *= 0.6;
            Vector v = p.getLocation().getDirection();
            p.setVelocity(v.multiply(speed));
        }
        else {
            speed *= 0.2;
            Vector v = p.getLocation().getDirection();
            p.setVelocity(v.multiply(speed));
        }
    }
}
