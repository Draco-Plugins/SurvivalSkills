package sir_draco.survivalskills.skill_listeners.fishing;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;

import java.util.List;

/**
 * Owns the fishing "artifact" items (the weather and time artifacts): their
 * right-click activation, the persistent-data cooldown tracking, and the legacy
 * lore-based migration for items created before the PDC was introduced.
 *
 * <p>The previously duplicated cooldown-check + message-format block at the top
 * of each artifact branch is extracted into {@link #checkArtifactCooldown}, which
 * is invoked once before branching on the held item's material.</p>
 */
public class ArtifactManager {

    private static final long COOLDOWN_MS = 3600000L;     // 1 hour
    private static final int COOLDOWN_SECS = 3600;
    private static final int SECS_PER_MIN = 60;

    // Day/night boundary used by the time artifact.
    private static final long NIGHT_START_TICK = 13000L;
    private static final long DAY_TICK = 0L;

    private final NamespacedKey artifactLastUsedKey;

    public ArtifactManager(SurvivalSkills plugin) {
        this.artifactLastUsedKey = new NamespacedKey(plugin, "artifact_last_used");
    }

    // =================================================================
    // Artifact activation
    // =================================================================

    public void onArtifactUse(PlayerInteractEvent e) {
        if (e.getHand() == null || e.getHand().equals(EquipmentSlot.OFF_HAND))
            return;
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)
                && !e.getAction().equals(Action.RIGHT_CLICK_AIR))
            return;
        ItemStack hand = e.getPlayer().getInventory().getItemInMainHand();
        if (!ItemStackGeneratorUtils.isCustomItem(hand))
            return;

        Player p = e.getPlayer();

        // Single shared cooldown gate; the old code duplicated this block inside
        // both the BREEZE_ROD and CLOCK branches.
        if (checkArtifactCooldown(p, hand))
            return;

        if (hand.getType().equals(Material.BREEZE_ROD))
            activateWeatherArtifact(p, hand);
        else if (hand.getType().equals(Material.CLOCK))
            activateTimeArtifact(p, hand);
    }

    /**
     * Returns {@code true} (and plays the rejection feedback) when {@code hand}
     * is still on cooldown, {@code false} when the artifact may be used. Centralises
     * the cooldown formatting/sound that was previously duplicated per-branch.
     */
    private boolean checkArtifactCooldown(Player p, ItemStack hand) {
        if (!itemIsOnCooldown(hand))
            return false;

        int time = getSecondsTillCooldown(hand);
        int minutes = time / SECS_PER_MIN;
        int seconds = time % SECS_PER_MIN;
        String formattedTime = String.format("%02d:%02d", minutes, seconds);
        p.sendRawMessage(ChatColor.RED + "This item is on cooldown for " + formattedTime + "!");
        p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        return true;
    }

    private void activateWeatherArtifact(Player p, ItemStack hand) {
        World world = p.getWorld();
        if (world.hasStorm()) {
            p.sendRawMessage(ChatColor.RED + "It is already raining!");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }

        world.setStorm(true);
        BukkitBroadcast.summonedStorm(p);
        p.sendRawMessage(ChatColor.GREEN + "You have summoned a storm!");
        p.playSound(p, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1, 1);

        setArtifactLastUsed(hand, System.currentTimeMillis());
    }

    private void activateTimeArtifact(Player p, ItemStack hand) {
        World world = p.getWorld();
        if (world.getTime() >= NIGHT_START_TICK) {
            world.setTime(DAY_TICK);
            BukkitBroadcast.setTimeDay(p);
        } else {
            world.setTime(NIGHT_START_TICK);
            BukkitBroadcast.setTimeNight(p);
        }
        setArtifactLastUsed(hand, System.currentTimeMillis());
    }

    // =================================================================
    // Cooldown persistence
    // =================================================================

    public boolean itemIsOnCooldown(ItemStack hand) {
        if (hand == null)
            return true;
        ItemMeta meta = hand.getItemMeta();
        if (meta == null)
            return true;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Backwards compatibility: migrate old lore-based timestamp if present.
        if (!pdc.has(artifactLastUsedKey, PersistentDataType.LONG))
            migrateLegacyLoreTimestamp(hand, meta);

        Long time = pdc.get(artifactLastUsedKey, PersistentDataType.LONG);
        if (time == null || time == 0L)
            return false; // never used or legacy 'never'
        return (System.currentTimeMillis() - time) < COOLDOWN_MS;
    }

    public int getSecondsTillCooldown(ItemStack hand) {
        if (hand == null)
            return 0;
        ItemMeta meta = hand.getItemMeta();
        if (meta == null)
            return 0;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Long time = pdc.get(artifactLastUsedKey, PersistentDataType.LONG);
        if (time == null || time == 0L)
            return 0;
        long elapsed = System.currentTimeMillis() - time;
        if (elapsed >= COOLDOWN_MS)
            return 0;
        return COOLDOWN_SECS - (int) (elapsed / 1000L);
    }

    private void setArtifactLastUsed(ItemStack item, long timestamp) {
        if (item == null)
            return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;
        meta.getPersistentDataContainer().set(artifactLastUsedKey, PersistentDataType.LONG, timestamp);
        item.setItemMeta(meta);
    }

    /**
     * Migrates a pre-PDC artifact by reading its "Last Used:" lore line into the
     * persistent data container. Malformed legacy values are silently ignored.
     */
    private void migrateLegacyLoreTimestamp(ItemStack hand, ItemMeta meta) {
        if (!meta.hasLore())
            return;
        List<String> lore = meta.getLore();
        if (lore == null)
            return;
        for (String line : lore) {
            if (!line.contains("Last Used:"))
                continue;
            String[] split = line.split(":");
            if (split.length < 2)
                return;
            String timeString = split[1].replace(" ", "");
            if (timeString.equalsIgnoreCase("never"))
                return;
            try {
                long legacyTime = Long.parseLong(timeString);
                setArtifactLastUsed(hand, legacyTime);
            } catch (NumberFormatException ignored) {
                // ignore malformed legacy value
            }
            return;
        }
    }

    // =================================================================
    // Small broadcast helper to keep the artifact branches readable
    // =================================================================

    private static final class BukkitBroadcast {
        static void summonedStorm(Player p) {
            Bukkit.broadcastMessage(ChatColor.GRAY.toString() + ChatColor.ITALIC + "[Server] "
                    + ChatColor.RESET + ChatColor.GOLD + p.getName() + ChatColor.YELLOW
                    + " has summoned a storm!");
        }

        static void setTimeDay(Player p) {
            Bukkit.broadcastMessage(ChatColor.GRAY.toString() + ChatColor.ITALIC + "[Server] "
                    + ChatColor.RESET + ChatColor.GOLD + p.getName() + ChatColor.YELLOW
                    + " has set the time to day!");
        }

        static void setTimeNight(Player p) {
            Bukkit.broadcastMessage(ChatColor.GRAY.toString() + ChatColor.ITALIC + "[Server] "
                    + ChatColor.RESET + ChatColor.GOLD + p.getName() + ChatColor.YELLOW
                    + " has set the time to night!");
        }
    }
}