package sir_draco.survivalskills.skill_listeners.god;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.abilities.VeinMinerAsync;
import sir_draco.survivalskills.abilities.items.PowerDrillTask;
import sir_draco.survivalskills.god_questline.powerore.CompletedPowerOreChallenge;
import sir_draco.survivalskills.god_questline.powerore.PowerOreChallenge;
import sir_draco.survivalskills.god_questline.powerore.PowerOreChallengeHandle;
import sir_draco.survivalskills.god_questline.powerore.PowerOreInventoryTask;
import sir_draco.survivalskills.god_questline.powerore.PowerOreScavengerHuntTask;
import sir_draco.survivalskills.rewards.PlayerRewards;
import sir_draco.survivalskills.utils.items.ItemModelData;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;
import sir_draco.survivalskills.skill_listeners.god.items.PowerOreGate;
import sir_draco.survivalskills.skills.SkillCategory;

import java.io.File;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Owns the Power Ore conversion lifecycle: lightning-triggered challenges,
 * charged-ore mining, task interactions, inventory challenge GUIs, and the
 * power drill ability.
 */
public class PowerOreChallengeListener implements Listener {

    // Power ore item / ability model data
    private static final int MODEL_POWER_ORE = ItemModelData.POWER_ORE.getId();
    private static final int MODEL_POWER_DRILL = ItemModelData.POWER_DRILL.getId();

    // Conversion requirements
    private static final int POWER_ORE_LEVEL_REQUIREMENT = 50;
    private static final int POWER_ORE_UNLOCK_LEVEL = 95;
    private static final long CONVERSION_COOLDOWN_TICKS = 20L;

    private static final String POWER_ORE_CONVERSIONS_FILE = "poweroreconversions.yml";
    private static final String YAML_CHARGED_KEY = "ChargedPowerOres";
    private static final double BLOCK_CENTER_OFFSET = 0.5;

    private final ChallengeRegistry powerOreRegistry = new ChallengeRegistry();
    private final Set<Player> conversionCooldowns = new HashSet<>();
    private final Map<UUID, Set<PowerDrillTask>> drillTasks = new ConcurrentHashMap<>();

    public PowerOreChallengeListener() {
        this(true);
    }

    PowerOreChallengeListener(boolean loadConversions) {
        if (loadConversions) loadPowerOreConversions();
    }

    // --- Block break ---

    @EventHandler
    public void onBreakBlock(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block block = e.getBlock();
        Location loc = block.getLocation();

        handlePowerOreBreak(e, p, loc);
        handlePowerDrillBreak(p, block);
    }

    private void handlePowerOreBreak(BlockBreakEvent e, Player p, Location loc) {
        if (!Material.OBSIDIAN.equals(e.getBlock().getType()))
            return;
        if (!powerOreRegistry.hasLocation(loc))
            return;

        PowerOreChallengeHandle challenge = powerOreRegistry.atLocation(loc);
        e.setCancelled(true); // Cancel no matter what since drops are handled manually
        if (!challenge.getUniqueId().equals(p.getUniqueId())) {
            p.sendMessage(ChatColor.RED + "This Power Ore belongs to another player");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }
        if (challenge.getStatus() == PowerOreChallenge.Status.RUNNING) {
            p.sendMessage(ChatColor.RED + "The ore is still charging. Complete your task first.");
            return;
        }
        if (challenge.getStatus() == PowerOreChallenge.Status.SUCCESS && !challenge.isRewardDropped()) {
            e.getBlock().setType(Material.AIR);
            challenge.reward(p);
            powerOreRegistry.unregister(loc, p.getUniqueId());
        }
    }

    private void handlePowerDrillBreak(Player p, Block block) {
        if (!ItemStackGeneratorUtils.isCustomItem(p.getInventory().getItemInMainHand(), MODEL_POWER_DRILL))
            return;
        if (p.hasMetadata(PowerDrillTask.DRILL_BREAK_METADATA))
            return;
        if (p.hasMetadata(VeinMinerAsync.VEIN_MINER_BREAK_METADATA))
            return;

        // Make sure the player has the Power Ore ability unlocked
        PlayerRewards rewards = SurvivalSkills.getInstance().getSkillManager().getPlayerRewards(p);
        if (rewards == null) {
            Bukkit.getLogger().log(Level.WARNING,
                    String.format("[SurvivalSkills] Player rewards not found for %s", p.getName()));
            return;
        }
        if (!hasPowerOreAbilityUnlocked(p)) {
            PowerOreGate.sendLockedMessage(p, "drill");
            return;
        }

        launchPowerDrill(p, block);
    }

    private boolean hasPowerOreAbilityUnlocked(Player p) {
        PlayerRewards rewards = SurvivalSkills.getInstance().getSkillManager().getPlayerRewards(p);
        return rewards != null && rewards.getReward(SkillCategory.MINING,
                PowerOreGate.POWER_ORE_REWARD).isApplied();
    }

    private void launchPowerDrill(Player p, Block block) {
        PowerDrillTask drillTask = new PowerDrillTask(SurvivalSkills.getInstance(), p, this, block);
        drillTask.start();
    }

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent e) {
        if (ItemStackGeneratorUtils.isCustomItem(e.getItemInHand(), MODEL_POWER_ORE))
            e.setCancelled(true);
    }

    // --- Scavenger hunt head interaction ---

    @EventHandler
    public void onRightClickScavengerHunt(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null)
            return;
        Material type = e.getClickedBlock().getType();
        if (!Material.PLAYER_HEAD.equals(type) && !Material.PLAYER_WALL_HEAD.equals(type))
            return;

        PowerOreChallengeHandle handle = powerOreRegistry.forPlayer(e.getPlayer().getUniqueId());
        if (!(handle instanceof PowerOreChallenge challenge)
                || !(challenge.getTask() instanceof PowerOreScavengerHuntTask huntTask))
            return;

        boolean handled = huntTask.handleInteract(e.getClickedBlock());
        if (handled)
            e.setCancelled(true); // Prevent other plugins from consuming
    }

    // --- Power Ore challenge GUIs ---

    private Optional<PowerOreInventoryTask> getInventoryTask(Player p) {
        PowerOreChallengeHandle handle = powerOreRegistry.forPlayer(p.getUniqueId());
        if (!(handle instanceof PowerOreChallenge challenge)
                || !challenge.getStatus().equals(PowerOreChallenge.Status.RUNNING)
                || !(challenge.getTask() instanceof PowerOreInventoryTask inventoryTask))
            return Optional.empty();
        return Optional.of(inventoryTask);
    }

    @EventHandler
    public void onGUIClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p))
            return;
        getInventoryTask(p).ifPresent((PowerOreInventoryTask inventoryTask) -> inventoryTask.handleClick(e));
    }

    @EventHandler
    public void onGUIClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p))
            return;
        getInventoryTask(p).ifPresent((PowerOreInventoryTask inventoryTask) -> inventoryTask.handleClose(e));
    }

    @EventHandler
    public void onGUIDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p))
            return;
        getInventoryTask(p).ifPresent((PowerOreInventoryTask inventoryTask) -> inventoryTask.handleDrag(e));
    }

    // --- Conversion ---

    public void handleZapWandPowerOreForge(Player p, Block clickedBlock) {
        Location clickedLocation = clickedBlock.getLocation();
        Block blockBelowPlayer = p.getLocation().getBlock().getRelative(0, -1, 0);
        PlayerRewards rewards = SurvivalSkills.getInstance().getSkillManager().getPlayerRewards(p);
        boolean powerOreUnlocked = rewards != null
                && rewards.getReward(SkillCategory.MINING, PowerOreGate.POWER_ORE_REWARD) != null
                && rewards.getReward(SkillCategory.MINING, PowerOreGate.POWER_ORE_REWARD).isApplied();
        ForgePrerequisite prerequisite = firstMissingForgePrerequisite(
                clickedLocation.equals(blockBelowPlayer.getLocation()),
                powerOreUnlocked,
                p.getLevel() >= POWER_ORE_LEVEL_REQUIREMENT,
                clickedLocation.getWorld() != null
                        && World.Environment.NORMAL.equals(clickedLocation.getWorld().getEnvironment()),
                powerOreRegistry.hasPlayer(p.getUniqueId()));

        if (!ForgePrerequisite.READY.equals(prerequisite)) {
            sendForgeHint(p, prerequisite, rewards);
            clickedBlock.getWorld().strikeLightning(clickedLocation);
            return;
        }

        clickedBlock.getWorld().strikeLightning(clickedLocation);
        tryPowerOreConversion(p, clickedLocation);
    }

    static ForgePrerequisite firstMissingForgePrerequisite(boolean standingOnClickedObsidian,
                                                            boolean powerOreUnlocked,
                                                            boolean enoughExperience,
                                                            boolean inOverworld,
                                                            boolean hasActiveChallenge) {
        if (!standingOnClickedObsidian) return ForgePrerequisite.STAND_ON_CLICKED_OBSIDIAN;
        if (!powerOreUnlocked) return ForgePrerequisite.UNLOCK_POWER_ORE;
        if (!enoughExperience) return ForgePrerequisite.EXPERIENCE_LEVELS;
        if (!inOverworld) return ForgePrerequisite.OVERWORLD;
        if (hasActiveChallenge) return ForgePrerequisite.ACTIVE_CHALLENGE;
        return ForgePrerequisite.READY;
    }

    private void sendForgeHint(Player p, ForgePrerequisite prerequisite, PlayerRewards rewards) {
        switch (prerequisite) {
            case STAND_ON_CLICKED_OBSIDIAN -> p.sendRawMessage(ChatColor.RED
                    + "Stand on the obsidian block you zap to forge Power Ore");
            case UNLOCK_POWER_ORE -> {
                int unlockLevel = POWER_ORE_UNLOCK_LEVEL;
                if (rewards != null
                        && rewards.getReward(SkillCategory.MINING, PowerOreGate.POWER_ORE_REWARD) != null) {
                    unlockLevel = rewards.getReward(SkillCategory.MINING, PowerOreGate.POWER_ORE_REWARD).getLevel();
                }
                p.sendRawMessage(ChatColor.RED + "Power Ore forging unlocks at mining level "
                        + ChatColor.AQUA + unlockLevel);
            }
            case EXPERIENCE_LEVELS -> p.sendRawMessage(ChatColor.RED
                    + "You need 50 levels of experience to forge Power Ore");
            case OVERWORLD -> p.sendRawMessage(ChatColor.RED
                    + "Power Ore can only be forged in the Overworld");
            case ACTIVE_CHALLENGE -> {
                PowerOreChallengeHandle existing = powerOreRegistry.forPlayer(p.getUniqueId());
                p.sendRawMessage(ChatColor.RED + "You are already attempting a Power Ore challenge at "
                        + existing.getOreLocation().getBlockX() + ", " + existing.getOreLocation().getBlockY() + ", "
                        + existing.getOreLocation().getBlockZ());
            }
            case READY -> {
                return;
            }
        }
        p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    }

    private void tryPowerOreConversion(Player p, Location expectedForgeLocation) {
        if (conversionCooldowns.contains(p))
            return;

        conversionCooldowns.add(p);
        scheduleCooldownExpiry(p);

        if (!hasPowerOreAbilityUnlocked(p))
            return;

        Location loc = validateConversionPrerequisites(p, expectedForgeLocation);
        if (loc == null)
            return;

        createAndStartChallenge(loc, p);
    }

    private void scheduleCooldownExpiry(Player p) {
        new BukkitRunnable() {
            @Override
            public void run() {
                conversionCooldowns.remove(p);
            }
        }.runTaskLaterAsynchronously(SurvivalSkills.getInstance(), CONVERSION_COOLDOWN_TICKS);
    }

    private Location validateConversionPrerequisites(Player p, Location expectedForgeLocation) {
        Block block = p.getLocation().getBlock().getRelative(0, -1, 0);
        if (!Material.OBSIDIAN.equals(block.getType()))
            return null;

        Location loc = block.getLocation();
        if (!expectedForgeLocation.equals(loc))
            return null;
        if (loc.getWorld() == null || loc.getWorld().getEnvironment() != World.Environment.NORMAL)
            return null;

        if (p.getLevel() < POWER_ORE_LEVEL_REQUIREMENT) {
            p.sendRawMessage(ChatColor.RED + "You need 50 levels of experience to power the ore");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return null;
        }

        if (powerOreRegistry.hasPlayer(p.getUniqueId())) {
            PowerOreChallengeHandle existing = powerOreRegistry.forPlayer(p.getUniqueId());
            p.sendMessage(ChatColor.RED + "You are already attempting a Power Ore challenge at "
                    + existing.getOreLocation().getBlockX() + ", " + existing.getOreLocation().getBlockY() + ", "
                    + existing.getOreLocation().getBlockZ());
            return null;
        }

        return loc;
    }

    private void createAndStartChallenge(Location loc, Player p) {
        p.setLevel(p.getLevel() - POWER_ORE_LEVEL_REQUIREMENT);

        PowerOreChallenge.TaskType taskType = PowerOreChallenge.randomTask(new Random());
        PowerOreChallenge challenge = new PowerOreChallenge(loc, p, taskType);
        powerOreRegistry.register(loc, challenge);
        challenge.start();
    }

    // --- Persistence ---

    public void savePowerOreConversions(FileConfiguration data) {
        // Only persist charged (completed) ores awaiting pickup.
        data.set(YAML_CHARGED_KEY, null);

        int i = 1;
        for (Map.Entry<Location, PowerOreChallengeHandle> entry : powerOreRegistry.entries()) {
            Location loc = entry.getKey();
            if (loc.getWorld() == null)
                continue;
            if (entry.getValue().getStatus() != PowerOreChallenge.Status.SUCCESS)
                continue;

            data.set(YAML_CHARGED_KEY + "." + i + ".Location", loc);
            data.set(YAML_CHARGED_KEY + "." + i + ".Player", entry.getValue().getUniqueId().toString());
            i++;
        }
    }

    private Map.Entry<Location, PowerOreChallengeHandle> loadSingleConversion(
            FileConfiguration data, String key) {
        Location loc = data.getLocation(YAML_CHARGED_KEY + "." + key + ".Location");
        String playerUUID = data.getString(YAML_CHARGED_KEY + "." + key + ".Player");
        if (loc == null || playerUUID == null)
            return null;
        try {
            return new AbstractMap.SimpleEntry<>(
                    loc, new CompletedPowerOreChallenge(loc, UUID.fromString(playerUUID)));
        } catch (IllegalArgumentException e) {
            Bukkit.getLogger().log(Level.WARNING,
                    "[SurvivalSkills] Invalid UUID in power ore conversions: " + playerUUID);
            return null;
        }
    }

    public void loadPowerOreConversions() {
        File file = new File(SurvivalSkills.getInstance().getDataFolder(), POWER_ORE_CONVERSIONS_FILE);
        if (!file.exists())
            return;
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);

        if (!data.contains(YAML_CHARGED_KEY))
            return;
        ConfigurationSection section = data.getConfigurationSection(YAML_CHARGED_KEY);
        if (section == null)
            return;

        section.getKeys(false).stream()
                .map(key -> loadSingleConversion(data, key))
                .filter(Objects::nonNull)
                .forEach(entry -> powerOreRegistry.register(entry.getKey(), entry.getValue()));
    }

    public void removeOreConversion(UUID uuid, Location loc) {
        powerOreRegistry.unregister(loc, uuid);
    }

    // --- Active drill task ownership ---

    public void registerDrillTask(UUID playerId, PowerDrillTask drillTask) {
        drillTasks.compute(playerId, (UUID ignored, Set<PowerDrillTask> playerTasks) -> {
            Set<PowerDrillTask> updatedTasks = playerTasks == null
                    ? ConcurrentHashMap.newKeySet()
                    : playerTasks;
            updatedTasks.add(drillTask);
            return updatedTasks;
        });
    }

    public void unregisterDrillTask(UUID playerId, PowerDrillTask drillTask) {
        drillTasks.computeIfPresent(playerId, (UUID ignored, Set<PowerDrillTask> playerTasks) -> {
            playerTasks.remove(drillTask);
            return playerTasks.isEmpty() ? null : playerTasks;
        });
    }

    private void cancelDrillTasks(UUID playerId) {
        Set<PowerDrillTask> playerTasks = drillTasks.remove(playerId);
        if (playerTasks == null) return;
        Set.copyOf(playerTasks).forEach((PowerDrillTask drillTask) -> drillTask.cancelForDisconnect());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        cancelDrillTasks(e.getPlayer().getUniqueId());
        conversionCooldowns.remove(e.getPlayer());
    }

    /**
     * Finds a safe location near {@code loc} for a downward strike: the first
     * solid block with air above it, searching down then up as a fallback.
     */
    public Location getSafeNearbyLocation(Location loc) {
        if (loc.getWorld() == null)
            return loc;
        World world = loc.getWorld();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();

        Block base = loc.getBlock();
        // Find first non-air block searching downward
        Block search = base;
        while (search.getY() > minY && search.getType().isAir())
            search = search.getRelative(0, -1, 0);

        // Search downward first, then upward as fallback
        Location found = findFirstSolidWithAirAbove(search, -1, minY);
        if (found != null) return found;

        found = findFirstSolidWithAirAbove(base, 1, maxY);
        if (found != null) return found;

        return loc;
    }

    private Location findFirstSolidWithAirAbove(Block start, int stepDir, int boundY) {
        Block search = start;
        while (stepDir < 0 ? search.getY() > boundY : search.getY() < boundY) {
            if (search.getType().isSolid() && search.getRelative(0, 1, 0).getType().isAir())
                return search.getLocation().add(BLOCK_CENTER_OFFSET, 1, BLOCK_CENTER_OFFSET);
            search = search.getRelative(0, stepDir, 0);
        }
        return null;
    }

    // --- Challenge Registry ---

    /**
     * Thread-safe container for the two-map {@code Location→Challenge} /
     * {@code UUID→Challenge} relationship, guaranteeing atomic synchronization
     * between the two indexes.
     */
    private static class ChallengeRegistry {
        private final Map<Location, PowerOreChallengeHandle> byLocation = new HashMap<>();
        private final Map<UUID, PowerOreChallengeHandle> byPlayer = new HashMap<>();

        void register(Location loc, PowerOreChallengeHandle challenge) {
            byLocation.put(loc, challenge);
            byPlayer.put(challenge.getUniqueId(), challenge);
        }

        void unregister(Location loc, UUID playerId) {
            byLocation.remove(loc);
            byPlayer.remove(playerId);
        }

        PowerOreChallengeHandle atLocation(Location loc) {
            return byLocation.get(loc);
        }

        PowerOreChallengeHandle forPlayer(UUID playerId) {
            return byPlayer.get(playerId);
        }

        boolean hasLocation(Location loc) {
            return byLocation.containsKey(loc);
        }

        boolean hasPlayer(UUID playerId) {
            return byPlayer.containsKey(playerId);
        }

        Set<Map.Entry<Location, PowerOreChallengeHandle>> entries() {
            return byLocation.entrySet();
        }
    }

    enum ForgePrerequisite {
        STAND_ON_CLICKED_OBSIDIAN,
        UNLOCK_POWER_ORE,
        EXPERIENCE_LEVELS,
        OVERWORLD,
        ACTIVE_CHALLENGE,
        READY
    }
}
