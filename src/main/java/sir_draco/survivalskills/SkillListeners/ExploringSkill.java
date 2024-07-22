package sir_draco.survivalskills.SkillListeners;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.Abilities.CaveFinderAsync;
import sir_draco.survivalskills.ItemStackGenerator;
import sir_draco.survivalskills.Rewards.PlayerRewards;
import sir_draco.survivalskills.Skills.Skill;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.HashMap;
import java.util.UUID;

public class ExploringSkill implements Listener {

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
        PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
        if (rewards == null) return;

        if (p.isSwimming()) setSwimSpeed(p);
        checkJumpBoots(p, rewards);
        checkWandererArmor(p, rewards);
        checkHealthRegen(p, rewards);
        checkTravelerArmor(p, rewards);
        checkAdventurerArmor(p, rewards);
        checkGillArmor(p, rewards);
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (isAdventurerArmor(p.getInventory()) && (rewards.getReward("Exploring", "AdventurerArmor").isApplied() || p.hasPermission("survivalskills.op"))) {
            e.setCancelled(true);
            return;
        }
        if (isJumpingBoots(p)) {
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

        if (checkGillSwim(p, plugin.getSkillManager().getPlayerRewards(p))) {
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

    public void giveJumpPotionEffect(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 100, 2, false, false, true));
    }

    public boolean isWandererArmor(PlayerInventory inv) {
        if (!ItemStackGenerator.isCustomItem(inv.getBoots(), 5)) return false;
        if (!ItemStackGenerator.isCustomItem(inv.getLeggings(), 5)) return false;
        if (!ItemStackGenerator.isCustomItem(inv.getChestplate(), 5)) return false;
        return ItemStackGenerator.isCustomItem(inv.getHelmet(), 5);
    }

    public boolean isTravelerArmor(PlayerInventory inv) {
        if (!ItemStackGenerator.isCustomItem(inv.getBoots(), 7)) return false;
        if (!ItemStackGenerator.isCustomItem(inv.getLeggings(), 7)) return false;
        if (!ItemStackGenerator.isCustomItem(inv.getChestplate(), 7)) return false;
        return ItemStackGenerator.isCustomItem(inv.getHelmet(), 7);
    }

    public boolean isGillArmor(PlayerInventory inv) {
        if (!ItemStackGenerator.isCustomItem(inv.getBoots(), 19)) return false;
        if (!ItemStackGenerator.isCustomItem(inv.getLeggings(), 19)) return false;
        if (!ItemStackGenerator.isCustomItem(inv.getChestplate(), 19)) return false;
        return ItemStackGenerator.isCustomItem(inv.getHelmet(), 19);
    }

    public boolean checkGillSwim(Player p, PlayerRewards rewards) {
        if (!rewards.getReward("Exploring", "GillArmor").isApplied() && !p.hasPermission("survivalskills.op")) return false;
        return isGillArmor(p.getInventory());
    }

    public boolean isAdventurerArmor(PlayerInventory inv) {
        if (!ItemStackGenerator.isCustomItem(inv.getBoots(), 8)) return false;
        if (!ItemStackGenerator.isCustomItem(inv.getLeggings(), 8)) return false;
        if (!ItemStackGenerator.isCustomItem(inv.getChestplate(), 8)) return false;
        return ItemStackGenerator.isCustomItem(inv.getHelmet(), 8);
    }

    public boolean isJumpingBoots(Player p) {
        if (!plugin.getSkillManager().getPlayerRewards(p).getReward("Exploring", "JumpingBoots").isApplied()
                && !p.hasPermission("survivalskills.op")) return false;
        return ItemStackGenerator.isCustomItem(p.getInventory().getBoots(), 4);
    }

    public void giveSpeedPotionEffect(Player p, int level) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, level, false, false, true));
    }

    public void giveRegenPotionEffect(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, false, false, true));
    }

    public void giveWaterBreathingPotionEffect(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 100, 0, false, false, true));
    }

    public void checkJumpBoots(Player p, PlayerRewards rewards) {
        if (!rewards.getReward("Exploring", "JumpingBoots").isApplied() && !p.hasPermission("survivalskills.op")) return;
        if (ItemStackGenerator.isCustomItem(p.getInventory().getBoots(), 4)) giveJumpPotionEffect(p);
    }

    public void checkWandererArmor(Player p, PlayerRewards rewards) {
        if (!rewards.getReward("Exploring", "WandererArmor").isApplied() && !p.hasPermission("survivalskills.op")) return;
        if (isWandererArmor(p.getInventory())) giveSpeedPotionEffect(p, 0);
    }

    public void checkTravelerArmor(Player p, PlayerRewards rewards) {
        if (!rewards.getReward("Exploring", "TravelerArmor").isApplied() && !p.hasPermission("survivalskills.op")) return;
        if (isTravelerArmor(p.getInventory())) giveSpeedPotionEffect(p, 1);
    }

    public void checkGillArmor(Player p, PlayerRewards rewards) {
        if (!rewards.getReward("Exploring", "GillArmor").isApplied() && !p.hasPermission("survivalskills.op")) return;
        if (isGillArmor(p.getInventory())) {
            giveWaterBreathingPotionEffect(p);
        }
    }

    public void checkAdventurerArmor(Player p, PlayerRewards rewards) {
        if (!rewards.getReward("Exploring", "AdventurerArmor").isApplied() && !p.hasPermission("survivalskills.op")) return;
        if (isAdventurerArmor(p.getInventory())) giveSpeedPotionEffect(p, 2);
    }

    public void checkHealthRegen(Player p, PlayerRewards rewards) {
        if (!rewards.getReward("Exploring", "HealthRegen").isApplied()) return;
        giveRegenPotionEffect(p);
    }
}
