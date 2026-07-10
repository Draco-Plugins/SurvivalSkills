package sir_draco.survivalskills.utils;

import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.KeyedBossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.god_questline.trial.TrialManager;

import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class Utils {
    public static final UUID DEFAULT_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private Utils() {
        // Prevent instantiation
    }

    /**
     * Returns true if there is a claim there
     */
    public static boolean checkForClaim(Player p, Location loc) {
        String noBuildReason = GriefPrevention.instance.allowBuild(p, loc);
        return (noBuildReason != null);
    }

    public static void tryRemovingTrophyItem(Entity ent) {
        if (!ent.getType().equals(EntityType.ITEM))
            return;
        Item item = (Item) ent;

        ItemStack itemStack = item.getItemStack();
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null)
            return;
        if (meta.getPersistentDataContainer().has(ItemStackGenerator.skillsItemKey)) {
            try {
                PersistentDataContainer container = meta.getPersistentDataContainer();
                if (container == null)
                    return;
                if (!container.has(ItemStackGenerator.skillsItemKey, PersistentDataType.STRING))
                    return;

                String itemContainer = container.get(ItemStackGenerator.skillsItemKey, PersistentDataType.STRING);
                if (itemContainer == null)
                    return;

                if (itemContainer.equals("Trophy"))
                    ent.remove();
                else
                    Bukkit.getLogger().log(Level.INFO, "Item with unknown skills item key: " + itemContainer);

            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "Failed to remove trophy item", e);
            }

        }
    }

    public static void loadOnlinePlayers(SurvivalSkills plugin) {
        if (!Bukkit.getServer().getOnlinePlayers().isEmpty()) {
            for (Player p : Bukkit.getServer().getOnlinePlayers()) {
                plugin.playerJoin(p);
                TrialManager.loadCompletedTrials(p);
            }
            // Try to fix boss bars
            for (Iterator<KeyedBossBar> it = Bukkit.getBossBars(); it.hasNext();) {
                KeyedBossBar bar = it.next();
                bar.removeAll();
            }
        }
    }

    /**
     * Finds an online player by name (case-insensitive match), mirroring
     * Bukkit.getPlayer(String) but null-safe.
     */
    public static Optional<Player> findPlayer(String name) {
        if (name == null) return Optional.empty();
        return Optional.ofNullable(Bukkit.getPlayer(name));
    }

    public static void sendActionBarMessage(Player p, String message) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(message));
    }

    public static void updateEntityAttributeInstance(LivingEntity entity, Attribute attribute, double value) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance == null) {
            throw new IllegalStateException("[Survival Skills] Updating attribute instance for " + entity.toString() + " failed");
        }
        instance.setBaseValue(value);
    }
}
