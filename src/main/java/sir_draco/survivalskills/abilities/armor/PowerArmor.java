package sir_draco.survivalskills.abilities.armor;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.*;
import org.bukkit.entity.Boss;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;

/**
 * Utility class for Power Armor damage storage & release.
 * Damage taken while wearing the full set is absorbed (up to MAX_STORED_DAMAGE)
 * and stored on the chestplate's PersistentDataContainer. Player can double
 * sneak to unleash a shockwave dealing the stored damage to nearby hostile
 * mobs.
 */
public final class PowerArmor {

    private PowerArmor() {
    }

    public static final String POWER_ARMOR_KEY = "power_armor_damage";
    public static final int POWER_ARMOR_MODEL_DATA = 51;
    public static final double MAX_STORED_DAMAGE = 50.0;
    private static final double MAX_RADIUS = 20.0;
    // Particle rendering configuration for shockwave sphere
    private static final int MIN_PARTICLE_POINTS = 80; // ensure a decent baseline
    private static final int MAX_PARTICLE_POINTS = 1500; // cap to prevent lag spikes
    private static final double PARTICLE_DENSITY = 4.5; // points per radius^2 (tweak for visual density)
    private static final NamespacedKey DAMAGE_KEY = new NamespacedKey(SurvivalSkills.getInstance(), POWER_ARMOR_KEY);

    /**
     * Stores additional damage (cancelling event externally when fully absorbed).
     */
    public static double addStoredDamage(Player player, double incoming) {
        ItemStack chest = player.getInventory().getChestplate();
        if (chest == null)
            return 0;
        ItemMeta meta = chest.getItemMeta();
        if (meta == null)
            return 0;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        double current = pdc.getOrDefault(DAMAGE_KEY, PersistentDataType.DOUBLE, 0.0);
        double capacityLeft = MAX_STORED_DAMAGE - current;
        double absorbed = Math.min(capacityLeft, incoming);

        if (absorbed <= 0)
            return 0;

        double updated = current + absorbed;
        pdc.set(DAMAGE_KEY, PersistentDataType.DOUBLE, updated);

        // If not all the damage was absorbed then alert the player
        if (updated >= MAX_STORED_DAMAGE) {
            player.sendMessage(ChatColor.RED + "Maximum damage absorbed!");
        }

        // Update or insert the "Absorbed Damage" lore line with the total stored damage
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new ArrayList<>();
        } else {
            // make mutable copy to avoid modifying unmodifiable lists
            lore = new ArrayList<>(lore);
        }

        int updatedInt = (int) updated;
        String newLine = ChatColor.GRAY + "Absorbed Damage: " + ChatColor.BOLD + ChatColor.DARK_AQUA + updatedInt;

        int foundIndex = -1;
        for (int i = 0; i < lore.size(); i++) {
            String stripped = ChatColor.stripColor(lore.get(i));
            if (stripped != null && stripped.startsWith("Absorbed Damage:")) {
                foundIndex = i;
                break;
            }
        }

        if (foundIndex != -1) {
            lore.set(foundIndex, newLine);
        } else {
            // preserve spacing if existing lore isn't empty
            if (!lore.isEmpty() && !lore.get(lore.size() - 1).isEmpty()) {
                lore.add("");
            }
            lore.add(newLine);
        }

        meta.setLore(lore);
        chest.setItemMeta(meta);

        return absorbed;
    }

    public static double getStoredDamage(Player player) {
        ItemStack chest = player.getInventory().getChestplate();
        if (chest == null)
            return 0;
        ItemMeta meta = chest.getItemMeta();
        if (meta == null)
            return 0;
        return meta.getPersistentDataContainer().getOrDefault(DAMAGE_KEY, PersistentDataType.DOUBLE, 0.0);
    }

    public static void resetStoredDamage(Player player) {
        ItemStack chest = player.getInventory().getChestplate();
        if (chest == null)
            return;
        ItemMeta meta = chest.getItemMeta();
        if (meta == null)
            return;
        meta.getPersistentDataContainer().set(DAMAGE_KEY, PersistentDataType.DOUBLE, 0.0);

        // Also reset lore display (if present) to 0
        List<String> lore = meta.getLore();
        if (lore != null && !lore.isEmpty()) {
            List<String> newLore = new ArrayList<>(lore);
            for (int i = 0; i < newLore.size(); i++) {
                String stripped = ChatColor.stripColor(newLore.get(i));
                if (stripped != null && stripped.startsWith("Absorbed Damage:")) {
                    newLore.set(i, ChatColor.GRAY + "Absorbed Damage: " + ChatColor.BOLD + ChatColor.DARK_AQUA + 0);
                    meta.setLore(newLore);
                    break;
                }
            }
        }
        chest.setItemMeta(meta);
    }

    /** Releases the damage in a shockwave. */
    public static void releaseDamage(Player player) {
        double damage = getStoredDamage(player);
        if (damage <= 0)
            return;
        double radius = (damage / MAX_STORED_DAMAGE) * MAX_RADIUS;
        radius = Math.min(radius, MAX_RADIUS);
        performShockwave(player, damage, radius);
        resetStoredDamage(player);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 0.6f);
    }

    private static void performShockwave(Player player, double damage, double radius) {
        Location origin = player.getLocation().clone().add(0, 1, 0);
        World world = origin.getWorld();
        if (world == null)
            return;

        new BukkitRunnable() {
            @Override
            public void run() {
                // Scale particle count approximately with surface area (r^2) while capping
                int points = (int) Math.min(
                        MAX_PARTICLE_POINTS,
                        Math.max(MIN_PARTICLE_POINTS, Math.round(radius * radius * PARTICLE_DENSITY)));
                spawnSphere(world, origin, radius, points);
                // Damage hostiles within the radius
                for (Entity ent : world.getNearbyEntities(origin, radius, radius, radius)) {
                    if (ent.getLocation().distance(origin) > radius)
                        continue;
                    if (!(ent instanceof LivingEntity livingEntity))
                        continue;
                    if (livingEntity.getUniqueId().equals(player.getUniqueId()))
                        continue;
                    if (!isHostile(livingEntity))
                        continue;
                    livingEntity.damage(damage, player);
                }
            }
        }.runTask(SurvivalSkills.getInstance());
    }

    private static void spawnSphere(World world, Location center, double radius, int points) {
        // Spawn points only on the outer shell using a Fibonacci (golden-angle) sphere
        int sampleCount = Math.max(12, points);
        final double goldenAngle = Math.PI * (3 - Math.sqrt(5)); // ~2.39996
        for (int i = 0; i < sampleCount; i++) {
            double t = (double) i / (sampleCount - 1); // 0 -> 1
            double y = 1 - 2 * t; // -1 -> 1
            double r = Math.sqrt(Math.max(0.0, 1 - y * y));
            double theta = goldenAngle * i;
            double x = Math.cos(theta) * r;
            double z = Math.sin(theta) * r;

            // scale to desired radius and spawn at exact surface location
            double px = center.getX() + x * radius;
            double py = center.getY() + y * radius;
            double pz = center.getZ() + z * radius;

            world.spawnParticle(Particle.DRAGON_BREATH, px, py, pz, 1, 0, 0, 0, 0);
        }
    }

    private static boolean isHostile(LivingEntity entity) {
        return entity instanceof Monster || entity instanceof Boss;
    }
}
