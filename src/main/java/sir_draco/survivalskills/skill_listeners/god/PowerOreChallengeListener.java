package sir_draco.survivalskills.skill_listeners.god;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.abilities.items.PowerDrillAsync;
import sir_draco.survivalskills.god_questline.powerore.PowerOreChallenge;
import sir_draco.survivalskills.god_questline.powerore.PowerOreScavengerHuntTask;
import sir_draco.survivalskills.god_questline.powerore.PowerOreSimonSaysTask;
import sir_draco.survivalskills.rewards.PlayerRewards;
import sir_draco.survivalskills.skill_listeners.MiningSkill;
import sir_draco.survivalskills.skill_listeners.god.items.PowerOreGate;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Owns the Power Ore conversion lifecycle: lightning-triggered challenges,
 * charged-ore mining, the scavenger hunt and Simon Says GUI interactions, and
 * the power drill ability.
 */
public class PowerOreChallengeListener implements Listener {

    // Power ore item / ability model data
    private static final int MODEL_POWER_ORE = 44;
    private static final int MODEL_POWER_DRILL = 48;

    // Conversion requirements
    private static final int POWER_ORE_LEVEL_REQUIREMENT = 50;
    private static final long CONVERSION_COOLDOWN_TICKS = 20L;

    private static final String POWER_ORE_CONVERSIONS_FILE = "poweroreconversions.yml";

    private final Map<Location, PowerOreChallenge> powerOreChallenges = new HashMap<>();
    private final Map<UUID, PowerOreChallenge> playerChallenges = new HashMap<>();
    private final Set<Player> conversionCooldowns = new HashSet<>();
    private final Map<Player, List<Block>> drillTracker = new HashMap<>();

    public PowerOreChallengeListener() {
        loadPowerOreConversions();
    }

    // --- Block break ---

    @EventHandler
    public void onBreakBlock(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block block = e.getBlock();
        Location loc = block.getLocation();

        handlePowerOreBreak(e, p, loc);
        handlePowerDrillBreak(e, p, block);
    }

    private void handlePowerOreBreak(BlockBreakEvent e, Player p, Location loc) {
        if (!Material.OBSIDIAN.equals(e.getBlock().getType()))
            return;
        if (!powerOreChallenges.containsKey(loc))
            return;

        PowerOreChallenge challenge = powerOreChallenges.get(loc);
        e.setCancelled(true);
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
            challenge.reward();
            powerOreChallenges.remove(loc);
            playerChallenges.remove(p.getUniqueId());
        }
    }

    private void handlePowerDrillBreak(BlockBreakEvent e, Player p, Block block) {
        if (!sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils
                .isCustomItem(p.getInventory().getItemInMainHand(), MODEL_POWER_DRILL))
            return;

        // Make sure the player is trackable
        drillTracker.computeIfAbsent(p, k -> new ArrayList<>());

        // Make sure this block isn't part of a previous drill task
        if (drillTracker.get(p).contains(block)) {
            drillTracker.get(p).remove(block);
            return;
        }

        // Skip if this break is part of an active vein-miner task
        MiningSkill mining = SurvivalSkills.getInstance().getMiningListener();
        if (mining != null) {
            if (p.hasMetadata("survivalskills_veinminer_break"))
                return;
            if (mining.isVeinMinerActive(p))
                return;
            if (mining.getVeinTracker().containsKey(p) && mining.getVeinTracker().get(p).contains(block))
                return;
        }

        // Make sure the player has the Power Ore ability unlocked
        PlayerRewards rewards = SurvivalSkills.getInstance().getSkillManager().getPlayerRewards(p);
        if (rewards == null) {
            Bukkit.getLogger().log(Level.WARNING,
                    String.format("[SurvivalSkills] Player rewards not found for %s", p.getName()));
            return;
        }
        if (!rewards.getReward(sir_draco.survivalskills.skills.SkillCategory.MINING,
                PowerOreGate.POWER_ORE_REWARD).isApplied()) {
            PowerOreGate.sendLockedMessage(p, "drill");
            return;
        }

        PowerDrillAsync drillTask = new PowerDrillAsync(SurvivalSkills.getInstance(), p, this, block);
        drillTask.runTaskAsynchronously(SurvivalSkills.getInstance());
    }

    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent e) {
        if (sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils
                .isCustomItem(e.getItemInHand(), MODEL_POWER_ORE))
            e.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDamageByLightning(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p))
            return;
        if (!EntityDamageEvent.DamageCause.LIGHTNING.equals(e.getCause()))
            return;
        if (conversionCooldowns.contains(p))
            return;
        tryPowerOreConversion(p);
    }

    // --- Scavenger hunt head interaction ---

    @EventHandler
    public void onRightClickScavengerHunt(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null)
            return;
        Material type = e.getClickedBlock().getType();
        if (!Material.PLAYER_HEAD.equals(type) && !Material.PLAYER_WALL_HEAD.equals(type))
            return;

        PowerOreChallenge challenge = playerChallenges.get(e.getPlayer().getUniqueId());
        if (challenge == null
                || !(challenge.getTask() instanceof PowerOreScavengerHuntTask huntTask))
            return;

        boolean handled = huntTask.handleInteract(e.getClickedBlock());
        if (handled)
            e.setCancelled(true); // Prevent other plugins from consuming
    }

    // --- Simon Says GUI ---

    @EventHandler
    public void onGUIClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p))
            return;
        PowerOreChallenge challenge = playerChallenges.get(p.getUniqueId());
        if (challenge == null
                || !challenge.getStatus().equals(PowerOreChallenge.Status.RUNNING)
                || !(challenge.getTask() instanceof PowerOreSimonSaysTask simonTask))
            return;
        simonTask.handleClick(e);
    }

    @EventHandler
    public void onGUIClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p))
            return;
        PowerOreChallenge challenge = playerChallenges.get(p.getUniqueId());
        if (challenge == null
                || !challenge.getStatus().equals(PowerOreChallenge.Status.RUNNING)
                || !(challenge.getTask() instanceof PowerOreSimonSaysTask simonTask))
            return;
        simonTask.handleClose();
    }

    @EventHandler
    public void onGUIDrag(InventoryDragEvent e) {
        if (e.getView().getTitle().contains("Simon Says"))
            e.setCancelled(true);
    }

    // --- Conversion ---

    public void tryPowerOreConversion(Player p) {
        conversionCooldowns.add(p);
        new BukkitRunnable() {
            @Override
            public void run() {
                conversionCooldowns.remove(p);
            }
        }.runTaskLaterAsynchronously(SurvivalSkills.getInstance(), CONVERSION_COOLDOWN_TICKS);

        PlayerRewards rewards = SurvivalSkills.getInstance().getSkillManager().getPlayerRewards(p);
        if (rewards == null || !rewards.getReward(
                sir_draco.survivalskills.skills.SkillCategory.MINING, PowerOreGate.POWER_ORE_REWARD).isApplied())
            return;

        Block block = p.getLocation().getBlock().getRelative(0, -1, 0);
        if (!Material.OBSIDIAN.equals(block.getType()))
            return;
        Location loc = block.getLocation();
        if (loc.getWorld() == null || loc.getWorld().getEnvironment() != World.Environment.NORMAL)
            return;
        if (p.getLevel() < POWER_ORE_LEVEL_REQUIREMENT) {
            p.sendRawMessage(ChatColor.RED + "You need 50 levels of experience to power the ore");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }
        if (playerChallenges.containsKey(p.getUniqueId())) {
            PowerOreChallenge existing = playerChallenges.get(p.getUniqueId());
            p.sendMessage(ChatColor.RED + "You are already attempting a Power Ore challenge at "
                    + existing.getOreLocation().getBlockX() + ", " + existing.getOreLocation().getBlockY() + ", "
                    + existing.getOreLocation().getBlockZ());
            return;
        }

        p.setLevel(p.getLevel() - POWER_ORE_LEVEL_REQUIREMENT);

        PowerOreChallenge.TaskType taskType = PowerOreChallenge.randomTask(new Random());
        PowerOreChallenge challenge = new PowerOreChallenge(loc, p, taskType);
        powerOreChallenges.put(loc, challenge);
        playerChallenges.put(p.getUniqueId(), challenge);
        challenge.start();
    }

    // --- Persistence ---

    public void savePowerOreConversions(org.bukkit.configuration.file.FileConfiguration data) {
        // Only persist charged (completed) ores awaiting pickup.
        data.set("ChargedPowerOres", null);

        int i = 1;
        for (Map.Entry<Location, PowerOreChallenge> entry : powerOreChallenges.entrySet()) {
            Location loc = entry.getKey();
            if (loc.getWorld() == null)
                continue;
            if (entry.getValue().getStatus() != PowerOreChallenge.Status.SUCCESS)
                continue;

            data.set("ChargedPowerOres." + i + ".Location", loc);
            data.set("ChargedPowerOres." + i + ".Player", entry.getValue().getUniqueId().toString());
            i++;
        }
    }

    public void loadPowerOreConversions() {
        File file = new File(SurvivalSkills.getInstance().getDataFolder(), POWER_ORE_CONVERSIONS_FILE);
        if (!file.exists())
            return;
        org.bukkit.configuration.file.FileConfiguration data =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);

        if (!data.contains("ChargedPowerOres"))
            return;
        org.bukkit.configuration.ConfigurationSection section = data.getConfigurationSection("ChargedPowerOres");
        if (section == null)
            return;

        section.getKeys(false).forEach(key -> {
            Location loc = data.getLocation("ChargedPowerOres." + key + ".Location");
            String playerUUID = data.getString("ChargedPowerOres." + key + ".Player");
            if (loc == null || playerUUID == null)
                return;
            try {
                PowerOreChallenge challenge = new PowerOreChallenge(loc, UUID.fromString(playerUUID));
                powerOreChallenges.put(loc, challenge);
            } catch (IllegalArgumentException ignored) {
                Bukkit.getLogger().log(Level.WARNING,
                        "[SurvivalSkills] Invalid UUID in power ore conversions: " + playerUUID);
            }
        });
    }

    public void removeOreConversion(UUID uuid, Location loc) {
        powerOreChallenges.remove(loc);
        playerChallenges.remove(uuid);
    }

    // --- Drill tracker (targeted access for PowerDrillAsync) ---

    public void registerDrillBlocks(Player player, List<Block> blocks) {
        drillTracker.put(player, blocks);
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
        Block search = base;
        while (search.getY() > minY && search.getType().isAir())
            search = search.getRelative(0, -1, 0);

        Block candidate = search;
        while (candidate.getY() > minY) {
            if (candidate.getType().isSolid() && candidate.getRelative(0, 1, 0).getType().isAir())
                return candidate.getLocation().add(0.5, 1, 0.5); // strike just above the block center
            candidate = candidate.getRelative(0, -1, 0);
        }

        Block upSearch = base;
        while (upSearch.getY() < maxY) {
            if (upSearch.getType().isSolid() && upSearch.getRelative(0, 1, 0).getType().isAir())
                return upSearch.getLocation().add(0.5, 1, 0.5);
            upSearch = upSearch.getRelative(0, 1, 0);
        }

        return loc;
    }
}