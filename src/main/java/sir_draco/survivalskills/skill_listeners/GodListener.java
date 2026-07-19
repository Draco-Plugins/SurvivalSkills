package sir_draco.survivalskills.skill_listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.god_questline.GodRecipeUI;
import sir_draco.survivalskills.god_questline.GodTrophyQuest;
import sir_draco.survivalskills.skill_listeners.god.GodItemDropListener;
import sir_draco.survivalskills.skill_listeners.god.GodItemUseHandler;
import sir_draco.survivalskills.skill_listeners.god.BiomeFinderListener;
import sir_draco.survivalskills.skill_listeners.god.PotionBagListener;
import sir_draco.survivalskills.skill_listeners.god.PowerOreChallengeListener;
import sir_draco.survivalskills.skill_listeners.god.TeleportAnchorListener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Coordinator for the "god" feature set. The heavy lifting has been split into
 * focused listeners ({@link PotionBagListener}, {@link TeleportAnchorListener},
 * {@link PowerOreChallengeListener}, {@link GodItemDropListener},
 * {@link GodItemUseHandler}); this class now only wires them together, owns
 * the God Recipe UI session map, and provides a stable access surface for the
 * rest of the plugin.
 */
public class GodListener implements Listener {

    private final PotionBagListener potionBagListener = new PotionBagListener();
    private final BiomeFinderListener biomeFinderListener = new BiomeFinderListener();
    private final GodItemDropListener godItemDropListener = new GodItemDropListener();
    private final TeleportAnchorListener teleportAnchorListener = new TeleportAnchorListener();
    private final PowerOreChallengeListener powerOreChallengeListener = new PowerOreChallengeListener();
    private final GodItemUseHandler godItemUseHandler = new GodItemUseHandler(potionBagListener, biomeFinderListener);

    private final Map<Player, GodRecipeUI> openGodRecipeUI = new HashMap<>();

    public GodListener() {
    }

    /** Registers this coordinator and all of its sub-listeners with the plugin. */
    public void register(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getServer().getPluginManager().registerEvents(potionBagListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(biomeFinderListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(godItemDropListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(teleportAnchorListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(powerOreChallengeListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(godItemUseHandler, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (p.hasPlayedBefore())
            return;

        if (!SurvivalSkills.getInstance().getTrophyManager().getPlayerGodQuestData().containsKey(p.getUniqueId())) {
            GodTrophyQuest quest = new GodTrophyQuest(p.getUniqueId());
            SurvivalSkills.getInstance().getTrophyManager().getPlayerGodQuestData().put(p.getUniqueId(), quest);
        }
    }

    // --- God Recipe UI session handling ---

    public void registerGodRecipeUI(Player player, GodRecipeUI ui) {
        openGodRecipeUI.put(player, ui);
    }

    @EventHandler
    public void onGUIClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p))
            return;
        GodRecipeUI ui = openGodRecipeUI.get(p);
        if (ui == null)
            return;
        e.setCancelled(true);
        ui.handleClick(e);
    }

    @EventHandler
    public void onGUIDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p))
            return;
        GodRecipeUI ui = openGodRecipeUI.get(p);
        if (ui == null)
            return;
        e.setCancelled(true);
        ui.handleDrag(e);
    }

    @EventHandler
    public void onGUIClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p))
            return;
        GodRecipeUI ui = openGodRecipeUI.get(p);
        if (ui == null)
            return;
        if (ui.getInventories().get(ui.getCurrentInv()).equals(e.getInventory()))
            openGodRecipeUI.remove(p);
    }

    // --- Delegated access surface ---

    public PotionBagListener getPotionBagListener() {
        return potionBagListener;
    }

    public GodItemDropListener getGodItemDropListener() {
        return godItemDropListener;
    }

    public TeleportAnchorListener getTeleportAnchorListener() {
        return teleportAnchorListener;
    }

    public PowerOreChallengeListener getPowerOreChallengeListener() {
        return powerOreChallengeListener;
    }

    public GodItemUseHandler getGodItemUseHandler() {
        return godItemUseHandler;
    }

    public Map<org.bukkit.Location, sir_draco.survivalskills.abilities.godItems.TeleporterAnchor> getTeleportAnchors() {
        return teleportAnchorListener.getTeleportAnchors();
    }

    public void savePotionBags(org.bukkit.configuration.file.FileConfiguration config) {
        potionBagListener.savePotionBags(config);
    }

    public void savePowerOreConversions(org.bukkit.configuration.file.FileConfiguration data) {
        powerOreChallengeListener.savePowerOreConversions(data);
    }

    public void removeOreConversion(UUID uuid, org.bukkit.Location loc) {
        powerOreChallengeListener.removeOreConversion(uuid, loc);
    }

    public void clearPotionBags() {
        potionBagListener.clearPotionBags();
    }
}
