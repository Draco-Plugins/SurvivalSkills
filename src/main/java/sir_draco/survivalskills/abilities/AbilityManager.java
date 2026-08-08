package sir_draco.survivalskills.abilities;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import sir_draco.survivalskills.commands.skill_commands.FlightCommand;
import sir_draco.survivalskills.rewards.PlayerRewards;
import sir_draco.survivalskills.rewards.Reward;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.SurvivalSkills;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;

/**
 * Central manager for runtime player abilities and visual effects.
 *
 * Responsibilities:
 * - Load/save per-player ability state (flight, spelunker, tool belt)
 * - Start/stop timers and background tasks per ability
 * - Track transient entities/effects (trails, glowing mobs, bloody domain)
 *
 * Note: Bukkit requires nearly all entity/world interactions to run on the main server thread.
 * Timers marked as asynchronous must not directly mutate Bukkit API state; this class delegates
 * such work to concrete timers that follow that rule. Public API is preserved to avoid breaking
 * existing command/listener code.
 */
public class AbilityManager {

    // Whitelist of entity types affected by Bloody Domain. Kept static for quick lookups.
    private static final ArrayList<EntityType> domainMobs = new ArrayList<>();
    public static final String TOOL_BELT_UPGRADE_REWARD = "ToolBeltII";
    public static final String TOOL_BELT_TITLE = "Tool Belt";
    public static final int TOOL_BELT_SIZE = 9;
    public static final int UPGRADED_TOOL_BELT_SIZE = 18;
    public static final String FLIGHT = ".Flight";
    public static final String SPELUNKER = ".Spelunker";
    public static final String LAST_USED_SUFFIX = ".LastUsedTimestamp";
    private static final String ACTIVE_TIME_KEY = ".ActiveTime";
    private static final String COOLDOWN_TIME_KEY = ".CooldownTime";

    private final SurvivalSkills plugin;
    private final HashMap<Player, ArrayList<AbilityTimer>> timerTracker = new HashMap<>();
    private final HashMap<Player, TrailEffect> trailTracker = new HashMap<>();
    private final HashMap<String, Particle> trails = new HashMap<>();
    private final HashMap<Player, BloodyDomain> bloodyDomainTracker = new HashMap<>();
    private final ArrayList<Entity> mobsScanned = new ArrayList<>();

    public AbilityManager(SurvivalSkills plugin) {
        this.plugin = plugin;
        createTrails();
        createDomainMobs();
    }

    /**
     * Rebuild a player's Tool Belt inventory from saved data.
     * Returns null if no data is found for the player.
     */
    public Inventory loadToolBelt(Player p) {
        FileConfiguration data = plugin.getToolBeltData();
        if (!data.contains(p.getUniqueId().toString())) return null;

        ConfigurationSection section = data.getConfigurationSection(p.getUniqueId().toString());
        if (section == null) return null;
        List<ItemStack> items = section.getKeys(false).stream()
                .map((String key) -> section.getItemStack(key))
                .filter(Objects::nonNull)
                .toList();
        return createToolBelt(p, getToolBeltSize(p), items);
    }

    /**
     * Rebuild a cached Tool Belt at the size currently available to the player.
     * Existing contents are copied so upgrading from nine to eighteen slots cannot
     * discard items.
     */
    public Inventory resizeToolBelt(Player p, Inventory toolBelt) {
        int size = getToolBeltSize(p);
        if (toolBelt.getSize() == size) return toolBelt;

        List<ItemStack> items = Arrays.stream(toolBelt.getContents())
                .filter(Objects::nonNull)
                .map((ItemStack item) -> item.clone())
                .toList();
        return createToolBelt(p, size, items);
    }

    public int getToolBeltSize(Player p) {
        return hasToolBeltUpgrade(p) ? UPGRADED_TOOL_BELT_SIZE : TOOL_BELT_SIZE;
    }

    public boolean hasToolBeltUpgrade(Player p) {
        PlayerRewards playerRewards = plugin.getSkillManager().getPlayerRewards(p);
        if (playerRewards == null) return false;
        Reward reward = playerRewards.getReward(SkillCategory.FIGHTING, TOOL_BELT_UPGRADE_REWARD);
        return reward != null && reward.isEnabled() && reward.isApplied();
    }

    private static Inventory createToolBelt(Player p, int size, List<ItemStack> items) {
        Inventory toolBelt = Bukkit.createInventory(p, size, TOOL_BELT_TITLE);
        Map<Integer, ItemStack> overflow = toolBelt.addItem(items.toArray(ItemStack[]::new));
        if (overflow.isEmpty()) return toolBelt;

        Map<Integer, ItemStack> playerOverflow = p.getInventory()
                .addItem(overflow.values().toArray(ItemStack[]::new));
        playerOverflow.values().forEach((ItemStack item) ->
                p.getWorld().dropItemNaturally(p.getLocation(), item));
        return toolBelt;
    }

    /**
     * Restore the Flight ability for a player, including handling of offline cooldown.
     */
    public void loadFlight(Player p, FileConfiguration data) {
        clearStaleFlightState(p);
        if (!data.contains(p.getUniqueId() + FLIGHT)) return;
        final String base = p.getUniqueId() + FLIGHT;

        int activeTime = data.getInt(base + ACTIVE_TIME_KEY);
        int cooldownTime = data.getInt(base + COOLDOWN_TIME_KEY);
        float speed = (float) data.getDouble(base + ".Speed");

        // Apply offline cooldown if ability was in cooldown mode
        cooldownTime = adjustCooldownFromOffline(data, base, activeTime, cooldownTime, p, "Flight");
        if (cooldownTime == 0 && activeTime <= 0) return; // ability fully reset while offline

        AbilityTimer timer = new AbilityTimer(plugin, "Flight", p, activeTime, cooldownTime);
        timer.setFlightSpeed(speed);
        FlightCommand.configureFlightTimer(timer);
        timer.runTaskTimer(plugin, 0, 20);
        addAbility(p, timer);

        if (activeTime <= 0) return;
        p.setAllowFlight(true);
        p.setFlying(true);
        p.setFlySpeed(speed);
        int minutes = activeTime / 60;
        int seconds = activeTime % 60;
        p.sendRawMessage(ChatColor.GREEN + "Your flight will end in " + ChatColor.AQUA + minutes +
                ChatColor.GREEN + " minutes " + ChatColor.AQUA + seconds + ChatColor.GREEN + " seconds");
    }

    /**
     * Restore the Spelunker ability for a player, including handling of offline cooldown.
     */
    public void loadSpelunker(Player p, FileConfiguration data) {
        if (!data.contains(p.getUniqueId() + SPELUNKER)) return;
        final String base = p.getUniqueId() + SPELUNKER;

        int activeTime = data.getInt(base + ACTIVE_TIME_KEY);
        int cooldownTime = data.getInt(base + COOLDOWN_TIME_KEY);
        int radius = data.getInt(base + ".Radius");

        // Apply offline cooldown if ability was in cooldown mode
        cooldownTime = adjustCooldownFromOffline(data, base, activeTime, cooldownTime, p, "Spelunker");
        if (cooldownTime == 0 && activeTime <= 0) return; // ability fully reset while offline

        AbilityTimer timer = new AbilityTimer(plugin, "Spelunker", p, activeTime, cooldownTime);
        timer.runTaskTimerAsynchronously(plugin, 0, 20);
        addAbility(p, timer);

        if (activeTime <= 0) return;

        // Restart the spelunker ability if it was active
        SpelunkerAbilitySync spelunker = new SpelunkerAbilitySync(plugin, radius, p);
        spelunker.runTaskTimer(plugin, 0, 10);
        plugin.getMiningListener().getSpelunkerTracker().put(p, spelunker);

        int minutes = activeTime / 60;
        int seconds = activeTime % 60;
        p.sendRawMessage(ChatColor.GREEN + "Your spelunker will end in " + ChatColor.AQUA + minutes +
                ChatColor.GREEN + " minutes " + ChatColor.AQUA + seconds + ChatColor.GREEN + " seconds");
    }

    /**
     * Persist a player's Tool Belt inventory to disk. No-op while viewers are open.
     */
    public void saveToolBelt(Player p, Inventory inv) {
        if (!inv.getViewers().isEmpty()) return;

        FileConfiguration data = plugin.getToolBeltData();
        int slot = 0;
        data.set(p.getUniqueId().toString(), null);
        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) continue;
            data.set(p.getUniqueId() + "." + slot, inv.getItem(i));
            slot++;
        }
    }

    /**
     * Save the provided FileConfiguration to the given file with guarded logging.
     */
    public void saveToolBeltFile(File file, FileConfiguration data) {
        try {
            data.save(file);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, String.format("Failed to save Tool Belts: %s", e.getMessage()));
        }
    }

    public void saveToolBelts() {
        File file = plugin.getToolBeltFile();
        FileConfiguration data = plugin.getToolBeltData();
        for (Map.Entry<Player, Inventory> toolBelt : plugin.getMiningListener().getToolBelts().entrySet()) {
            if (toolBelt.getKey() == null || toolBelt.getValue() == null) continue;
            saveToolBelt(toolBelt.getKey(), toolBelt.getValue());
        }

        saveToolBeltFile(file, data);
    }

    /**
     * Persist the Flight timer state for a player. Removes the section if no timer exists.
     */
    public void saveFlightTimer(Player p, FileConfiguration data) {
        AbilityTimer timer = getAbility(p, "Flight");
        if (timer == null) {
            if (data.contains(p.getUniqueId() + FLIGHT)) data.set(p.getUniqueId() + FLIGHT, null);
            return;
        }

        final String base = p.getUniqueId() + FLIGHT;
        data.set(base + ACTIVE_TIME_KEY, timer.getActiveTimeLeft());
        data.set(base + COOLDOWN_TIME_KEY, timer.getTimeTillReset());
        data.set(base + ".Speed", timer.getFlightSpeed());

        // Save the current timestamp for offline cooldown calculation
        if (!timer.isActive() && timer.getTimeTillReset() > 0) {
            data.set(base + LAST_USED_SUFFIX, System.currentTimeMillis());
        }
    }

    /**
     * Persist the Spelunker timer state for a player. Removes the section if no timer exists.
     */
    public void saveSpelunkerTimer(Player p, FileConfiguration data) {
        AbilityTimer timer = getAbility(p, "Spelunker");
        if (timer == null) {
            if (data.contains(p.getUniqueId() + SPELUNKER)) data.set(p.getUniqueId() + SPELUNKER, null);
            return;
        }

        final String base = p.getUniqueId() + SPELUNKER;
        data.set(base + ACTIVE_TIME_KEY, timer.getActiveTimeLeft());
        data.set(base + COOLDOWN_TIME_KEY, timer.getTimeTillReset());

        // Save the current timestamp for offline cooldown calculation
        if (!timer.isActive() && timer.getTimeTillReset() > 0) {
            data.set(base + LAST_USED_SUFFIX, System.currentTimeMillis());
        }

        // Save the radius from the active spelunker or use default based on player's rewards
        int radius = 5; // Default radius
        SpelunkerAbilitySync activeSpelunker = plugin.getMiningListener().getSpelunkerTracker().get(p);
        if (activeSpelunker != null) {
            radius = activeSpelunker.getRadius();
        } else {
            // Determine radius based on player's spelunker level if no active spelunker
            if (plugin.getSkillManager().getPlayerRewards(p).getReward(SkillCategory.MINING, "SpelunkerIII").isApplied()) {
                radius = 15;
            } else if (plugin.getSkillManager().getPlayerRewards(p).getReward(SkillCategory.MINING, "SpelunkerII").isApplied()) {
                radius = 10;
            }
        }
        data.set(base + ".Radius", radius);
    }

    /**
     * Begin tracking Bloody Domain for a player if they own the reward.
     */
    public void startBloodyDomain(Player p) {
        Reward reward = plugin.getSkillManager().getPlayerRewards(p).getReward(SkillCategory.FIGHTING, "BloodyDomain");
        if (reward == null || !reward.isApplied()) return;

        BloodyDomain domain = new BloodyDomain(p);
        domain.runTaskTimer(plugin, 0, 20);
        bloodyDomainTracker.put(p, domain);
    }

    /**
     * End and clear all ability timers for the provided player.
     */
    public void endPlayerTimers(Player p) {
        if (!timerTracker.containsKey(p)) return;
        for (AbilityTimer timer : timerTracker.get(p)) timer.endAbility();
        timerTracker.remove(p);
    }

    /** Add a new tracked ability timer for a player. */
    public void addAbility(Player p, AbilityTimer timer) {
        timerTracker.computeIfAbsent(p, k -> new ArrayList<>());
        timerTracker.get(p).add(timer);
    }

    /** Remove a specific ability timer by name for a player, if present. */
    public void removeAbility(Player p, String ability) {
        if (!timerTracker.containsKey(p)) return;
        timerTracker.get(p).removeIf(timer -> timer.getName().equalsIgnoreCase(ability));
    }

/** Get a tracked ability timer by name for a player; returns null if not found. */
    public AbilityTimer getAbility(Player p, String ability) {
        if (!timerTracker.containsKey(p)) return null;
        for (AbilityTimer timer : timerTracker.get(p)) if (timer.getName().equalsIgnoreCase(ability)) return timer;
        return null;
    }

    /**
     * True iff the named ability exists for the player and is currently within its
     * active window. Lets callers treat AbilityManager as the single source of
     * truth for "is this ability currently in effect" instead of mirroring timer
     * state in parallel lists that can drift out of sync.
     */
    public boolean isAbilityActive(Player p, String ability) {
        AbilityTimer timer = getAbility(p, ability);
        return timer != null && timer.isActive();
    }

    public boolean saveAbilityTimerState(Player p, String ability, FileConfiguration data, String configPath) {
        AbilityTimer timer = getAbility(p, ability);
        if (timer == null) {
            data.set(configPath, null);
            return false;
        }
        data.set(configPath, timer.getActiveTimeLeft());
        return true;
    }

    /** Add entities to the current scanned list (used for glow highlighting). */
    public void addScannedMobs(List<Entity> entities) {
        if (entities == null || entities.isEmpty()) return;
        mobsScanned.addAll(entities);
    }

    /** Remove entities from the current scanned list. */
    public void removeScannedMobs(List<Entity> entities) {
        if (entities == null || entities.isEmpty()) return;
        mobsScanned.removeAll(entities);
    }

    /** Remove the glow effect from all scanned mobs and clear the list. */
    public void removeGlowFromScannedMobs() {
        if (mobsScanned.isEmpty()) return;
        for (Entity entity : mobsScanned) {
            if (entity != null) entity.setGlowing(false);
        }
        mobsScanned.clear();
    }

    /** Initialize available particle trails. Idempotent. */
    public void createTrails() {
        trails.clear();
        trails.put("Dust", Particle.DUST);
        trails.put("Water", Particle.SPLASH);
        trails.put("Happy", Particle.HAPPY_VILLAGER);
        trails.put("Dragon", Particle.DRAGON_BREATH);
        trails.put("Electric", Particle.ELECTRIC_SPARK);
        trails.put("Enchantment", Particle.ENCHANT);
        trails.put("Ominous", Particle.TRIAL_OMEN);
        trails.put("Love", Particle.HEART);
        trails.put("Flame", Particle.FLAME);
        trails.put("BlueFlame", Particle.SOUL_FIRE_FLAME);
        trails.put("Cherry", Particle.CHERRY_LEAVES);
        trails.put("Rainbow", Particle.DUST);
    }

    /** Initialize the set of mobs affected by Bloody Domain. Idempotent. */
    public void createDomainMobs() {
        if (!domainMobs.isEmpty()) {
            domainMobs.clear();
        }
        domainMobs.add(EntityType.BLAZE);
        domainMobs.add(EntityType.BOGGED);
        domainMobs.add(EntityType.BREEZE);
        domainMobs.add(EntityType.CAVE_SPIDER);
        domainMobs.add(EntityType.CREEPER);
        domainMobs.add(EntityType.DROWNED);
        domainMobs.add(EntityType.ENDERMAN);
        domainMobs.add(EntityType.ENDERMITE);
        domainMobs.add(EntityType.EVOKER);
        domainMobs.add(EntityType.GHAST);
        domainMobs.add(EntityType.GUARDIAN);
        domainMobs.add(EntityType.HOGLIN);
        domainMobs.add(EntityType.HUSK);
        domainMobs.add(EntityType.ILLUSIONER);
        domainMobs.add(EntityType.MAGMA_CUBE);
        domainMobs.add(EntityType.PHANTOM);
        domainMobs.add(EntityType.PIGLIN);
        domainMobs.add(EntityType.PIGLIN_BRUTE);
        domainMobs.add(EntityType.PILLAGER);
        domainMobs.add(EntityType.RAVAGER);
        domainMobs.add(EntityType.SHULKER);
        domainMobs.add(EntityType.SILVERFISH);
        domainMobs.add(EntityType.SKELETON);
        domainMobs.add(EntityType.SLIME);
        domainMobs.add(EntityType.SPIDER);
        domainMobs.add(EntityType.STRAY);
        domainMobs.add(EntityType.VEX);
        domainMobs.add(EntityType.VINDICATOR);
        domainMobs.add(EntityType.WITCH);
        domainMobs.add(EntityType.WITHER_SKELETON);
        domainMobs.add(EntityType.ZOGLIN);
        domainMobs.add(EntityType.ZOMBIE);
        domainMobs.add(EntityType.ZOMBIE_VILLAGER);
    }

    /** Get the defined trail particle mapping (read-only). */
    public Map<String, Particle> getTrails() {
        return Collections.unmodifiableMap(trails);
    }

    public Map<Player, TrailEffect> getTrailTracker() {
        return trailTracker;
    }

    public Map<Player, ArrayList<AbilityTimer>> getTimerTracker() {
        return timerTracker;
    }

    /** Get the list of entity types affected by Bloody Domain (read-only). */
    public static List<EntityType> getDomainMobs() {
        return Collections.unmodifiableList(domainMobs);
    }

    public Map<Player, BloodyDomain> getBloodyDomainTracker() {
        return bloodyDomainTracker;
    }

    // --------------------------
    // Internal helpers
    // --------------------------

    /**
     * Apply offline cooldown reduction if the ability was cooling down when the player logged off.
     *
     * @param data         backing config
     * @param basePath     full path prefix (uuid + ".Ability")
     * @param activeTime   seconds remaining on active state when saved
     * @param cooldownTime seconds remaining on cooldown when saved
     * @param p            player to notify on reset
     * @param abilityName  for messaging
     * @return updated cooldown time (0 if fully reset). If 0 is returned and activeTime <= 0, callers should stop.
     */
    private int adjustCooldownFromOffline(FileConfiguration data, String basePath, int activeTime, int cooldownTime,
                                          Player p, String abilityName) {
        if (activeTime > 0 || cooldownTime <= 0 || !data.contains(basePath + LAST_USED_SUFFIX)) return cooldownTime;

        long lastUsedTimestamp = data.getLong(basePath + LAST_USED_SUFFIX);
        long elapsedSeconds = Math.max(0, (System.currentTimeMillis() - lastUsedTimestamp) / 1000);
        int updatedCooldown = Math.max(0, cooldownTime - (int) elapsedSeconds);

        if (updatedCooldown == 0) {
            // Cooldown finished while offline; clear ability section and notify.
            p.sendRawMessage(ChatColor.GREEN + "Your " + abilityName + " ability has reset while you were offline!");
            data.set(basePath, null);
        }

        return updatedCooldown;
    }

    /**
     * Clear Bukkit's persisted flight flags before restoring a timed flight timer. This prevents
     * a stale allow-flight value from granting flight after the plugin timer has expired. Creative,
     * spectator, and unlimited-flight players own their flight state outside the timed timer.
     */
    private void clearStaleFlightState(Player p) {
        GameMode gameMode = p.getGameMode();
        if (gameMode == GameMode.CREATIVE || gameMode == GameMode.SPECTATOR || hasUnlimitedFlight(p)) return;

        p.setFlying(false);
        p.setAllowFlight(false);
    }

    private boolean hasUnlimitedFlight(Player p) {
        PlayerRewards playerRewards = plugin.getSkillManager().getPlayerRewards(p);
        if (playerRewards == null) return false;
        Reward reward = playerRewards.getReward(SkillCategory.BUILDING, FlightCommand.FLIGHT_IV);
        return reward != null && reward.isEnabled() && reward.isApplied();
    }
}
