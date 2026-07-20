package sir_draco.survivalskills.skill_listeners;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.abilities.AutoTrash;
import sir_draco.survivalskills.skill_listeners.fishing.ArtifactManager;
import sir_draco.survivalskills.skill_listeners.fishing.FishingAbilityManager;
import sir_draco.survivalskills.skill_listeners.fishing.FishingLootManager;
import sir_draco.survivalskills.skill_listeners.fishing.FishingMechanicsManager;
import sir_draco.survivalskills.skill_listeners.fishing.TrashManager;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Lightweight coordinator for the fishing skill.
 *
 * <p>Originally this single class handled loot tables, the fishing pipeline,
 * boss spawning, water breathing, rain broadcasts, the auto-trash inventories,
 * artifact cooldowns and the custom buckets &mdash; over a thousand lines of
 * unrelated concerns. Each concern now lives in a focused collaborator in
 * {@code skill_listeners.fishing}; this class only wires them together, routes
 * the Bukkit events, and exposes the public API used by commands and the plugin
 * core (the trash registries, water-breather list, and {@link #spawnFishingBoss}).</p>
 *
 * <p>This mirrors the structure already adopted by {@link FightingSkill}, which
 * delegates to focused managers in {@code skill_listeners.fighting}.</p>
 */
public class FishingSkill implements Listener {

    // Focused collaborators (constructed once, in dependency order)
    private final FishingLootManager lootManager;
    private final FishingMechanicsManager mechanicsManager;
    private final TrashManager trashManager;
    private final ArtifactManager artifactManager;
    private final FishingAbilityManager abilityManager;

    public FishingSkill(SurvivalSkills plugin) {
        this.lootManager = new FishingLootManager(plugin);
        this.mechanicsManager = new FishingMechanicsManager(plugin, lootManager);
        this.trashManager = new TrashManager();
        this.artifactManager = new ArtifactManager(plugin);
        this.abilityManager = new FishingAbilityManager(plugin);
    }

    // =================================================================
    // Fishing pipeline
    // =================================================================

    @EventHandler
    public void onPlayerFish(PlayerFishEvent e) {
        mechanicsManager.onPlayerFish(e);
    }

    @EventHandler
    public void experienceEvent(PlayerExpChangeEvent e) {
        mechanicsManager.modifyExperience(e);
    }

    @EventHandler
    public void killFishingBoss(EntityDeathEvent e) {
        mechanicsManager.killFishingBoss(e);
    }

    // =================================================================
    // Water breathing / rain / buckets
    // =================================================================

    @EventHandler
    public void playerMoveInWater(PlayerMoveEvent e) {
        abilityManager.playerMoveInWater(e);
    }

    @EventHandler
    public void rainEvent(WeatherChangeEvent e) {
        abilityManager.rainEvent(e);
    }

    @EventHandler
    public void onBucketPickup(PlayerBucketFillEvent e) {
        abilityManager.onBucketPickup(e);
    }

    @EventHandler
    public void onBucketUse(PlayerBucketEmptyEvent e) {
        abilityManager.onBucketUse(e);
    }

    // =================================================================
    // Auto-trash / perma-trash
    // =================================================================

    @EventHandler
    public void onTrashClose(InventoryCloseEvent e) {
        trashManager.onTrashClose(e);
    }

    @EventHandler
    public void onClickTrashInventory(InventoryClickEvent e) {
        trashManager.onClickTrashInventory(e);
    }

    @EventHandler
    public void onDragTrashInventory(InventoryDragEvent e) {
        trashManager.onDragTrashInventory(e);
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent e) {
        trashManager.onItemPickup(e);
    }

    // =================================================================
    // Artifacts
    // =================================================================

    @EventHandler
    public void onArtifactUse(PlayerInteractEvent e) {
        artifactManager.onArtifactUse(e);
    }

    // =================================================================
    // Public API (used by commands and the plugin core).
    // Live references are returned to preserve the original contract: callers
    // (FileUtils, ResetAllCommand, AutoTrashCommand, WaterBreathingCommand, ...)
    // mutate these collections directly (.put/.clear/.remove/.add).
    // =================================================================

    public ArrayList<Player> getWaterBreathers() {
        return abilityManager.getWaterBreathers();
    }

    public ArrayList<Player> getOpenTrashInventories() {
        return trashManager.getOpenTrashInventories();
    }

    public HashMap<Player, AutoTrash> getTrashInventories() {
        return trashManager.getTrashInventories();
    }

    public HashMap<Player, AutoTrash> getPermaTrash() {
        return trashManager.getPermaTrash();
    }

    public ArrayList<Player> getDisabledAutoTrash() {
        return trashManager.getDisabledAutoTrash();
    }

    public void addTrashInventory(Player p, AutoTrash trash) {
        trashManager.addTrashInventory(p, trash);
    }

    public void markTrashInventoryOpen(Player p) {
        trashManager.markTrashInventoryOpen(p);
    }

    public void removePlayerTrashData(Player p) {
        trashManager.removePlayer(p);
    }

    public void spawnFishingBoss(World world, Location loc, Vector velocity) {
        mechanicsManager.spawnFishingBoss(world, loc, velocity);
    }
}
