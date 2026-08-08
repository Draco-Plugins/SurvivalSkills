package sir_draco.survivalskills.utils;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import sir_draco.survivalskills.god_questline.*;
import sir_draco.survivalskills.god_questline.trial.PendingTrial;
import sir_draco.survivalskills.god_questline.trial.ProtectedArea;
import sir_draco.survivalskills.god_questline.trial.RelativeBlock;
import sir_draco.survivalskills.god_questline.trial.Trial;
import sir_draco.survivalskills.god_questline.trial.TrialManager;
import sir_draco.survivalskills.SurvivalSkills;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class TrialUtils {

    private static final String TRIAL_DATA_FILE = "trialdata.yml";
    private static final String TRIAL_BUILDING_FILE = "trialbuilding.yml";
    private static final int TRIAL_AREA_RADIUS = 25;
    private static final int TRIAL_AREA_HEIGHT = 30;
    private static final int TRIAL_AREA_Y_OFFSET = -1;
    private static final int TRIAL_MAX_DISTANCE = 100;
    private static final int SOLO_GOD_DIFFICULTY = 7;
    private static final int COOP_GOD_DIFFICULTY = 8;
    private static final long COOLDOWN = 5 * 60 * 1000L;
    private static final int COOLDOWN_MINUTES = (int) (COOLDOWN / (60 * 1000));
    private static final Sound ERROR_SOUND = Sound.ENTITY_ENDERMAN_TELEPORT;

    // Cached trial building blocks; loaded once from config and copied per trial.
    private static List<RelativeBlock> cachedBuildingBlocks = null;

    // --- Block collection helpers ---

    public static ArrayList<Block> getBlocks(Location location, int x, int y, int z) {
        ArrayList<Block> blocks = new ArrayList<>();
        World world = location.getWorld();
        int baseX = location.getBlockX();
        int baseY = location.getBlockY();
        int baseZ = location.getBlockZ();
        for (int i = -(x / 2); i <= x / 2; i++) {
            for (int j = TRIAL_AREA_Y_OFFSET; j < y; j++) {
                for (int k = -(z / 2); k <= z / 2; k++) {
                    Block block = world.getBlockAt(baseX + i, baseY + j, baseZ + k);
                    if (block.getType().isAir()) continue;
                    blocks.add(block);
                }
            }
        }
        return blocks;
    }

    // --- Trial building persistence ---

    public static void storeTrialBuilding(Location relativeLocation, ArrayList<Block> blocks) {
        FileConfiguration config = loadConfigFile(TRIAL_BUILDING_FILE);
        config.set("Blocks", null);

        int i = 1;
        for (Block block : blocks) {
            Location loc = block.getLocation();
            // Locations relative to the player
            String path = "Blocks." + i + ".";
            config.set(path + "X", loc.getBlockX() - relativeLocation.getBlockX());
            config.set(path + "Y", loc.getBlockY() - relativeLocation.getBlockY());
            config.set(path + "Z", loc.getBlockZ() - relativeLocation.getBlockZ());
            config.set(path + "Type", block.getType().name());
            config.set(path + "Data", block.getBlockData().getAsString());
            i++;
        }

        saveConfig(config, TRIAL_BUILDING_FILE, "Failed to save trial building to " + TRIAL_BUILDING_FILE);
    }

    public static ArrayList<RelativeBlock> loadTrialBuilding(FileConfiguration config) {
        // Return a fresh copy of the cache so consumers can mutate it freely
        if (cachedBuildingBlocks != null) return new ArrayList<>(cachedBuildingBlocks);

        ArrayList<RelativeBlock> blocks = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("Blocks");
        if (section == null) return blocks;
        if (section.getKeys(false).isEmpty()) {
            Bukkit.getLogger().warning("No trial building saved in " + TRIAL_BUILDING_FILE);
            return blocks;
        }

        for (String key : section.getKeys(false)) {
            String path = "Blocks." + key + ".";
            int x = config.getInt(path + "X");
            int y = config.getInt(path + "Y");
            int z = config.getInt(path + "Z");
            Material material = Material.valueOf(config.getString(path + "Type"));
            String blockDataString = config.getString(path + "Data");
            if (blockDataString == null) continue;
            BlockData data = Bukkit.createBlockData(blockDataString);
            blocks.add(new RelativeBlock(x, y, z, data, material));
        }

        if (blocks.isEmpty()) {
            Bukkit.getLogger().warning("No trial building blocks found in " + TRIAL_BUILDING_FILE);
        } else {
            Bukkit.getLogger().info(String.format("Loaded %d trial building blocks from %s", blocks.size(), TRIAL_BUILDING_FILE));
            cachedBuildingBlocks = List.copyOf(blocks);
        }

        return blocks;
    }

    // Takes a block, finds the new relative location for the block, and copies the data into the new block
    public static void convertBlockToRelative(RelativeBlock block, Location location) {
        Block relativeBlock = new Location(location.getWorld(),
                location.getBlockX() + block.x(), location.getBlockY() + block.y(), location.getBlockZ() + block.z()).getBlock();
        relativeBlock.setType(block.material());
        relativeBlock.setBlockData(block.data());
        relativeBlock.getState().update();
    }

    public static RelativeBlock popRandomBlock(ArrayList<RelativeBlock> building) {
        RelativeBlock block = building.get((int) (Math.random() * building.size()));
        building.remove(block);
        return block;
    }

    public static void clearTrialBuilding(Location centerLocation) {
        World world = centerLocation.getWorld();
        int baseX = centerLocation.getBlockX();
        int baseY = centerLocation.getBlockY();
        int baseZ = centerLocation.getBlockZ();
        for (int i = -TRIAL_AREA_RADIUS; i <= TRIAL_AREA_RADIUS; i++) {
            for (int j = TRIAL_AREA_Y_OFFSET; j <= TRIAL_AREA_HEIGHT; j++) {
                for (int k = -TRIAL_AREA_RADIUS; k <= TRIAL_AREA_RADIUS; k++) {
                    Block block = world.getBlockAt(baseX + i, baseY + j, baseZ + k);
                    if (block.getType().isAir()) continue;
                    block.setType(Material.AIR);
                }
            }
        }
    }

    public static void removeProtectedArea(ProtectedArea protectedArea) {
        World world = protectedArea.world();
        BoundingBox box = protectedArea.boundingBox();

        // Load the chunks that contain blocks in the protected area before removing them
        int minChunkX = (int) Math.floor(box.getMinX()) >> 4;
        int maxChunkX = (int) Math.floor(box.getMaxX()) >> 4;
        int minChunkZ = (int) Math.floor(box.getMinZ()) >> 4;
        int maxChunkZ = (int) Math.floor(box.getMaxZ()) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    world.loadChunk(chunkX, chunkZ);
                }
            }
        }

        // Now safely remove blocks
        for (int i = (int) box.getMinX() - 1; i <= box.getMaxX(); i++) {
            for (int j = (int) box.getMinY(); j <= box.getMaxY(); j++) {
                for (int k = (int) box.getMinZ() - 1; k <= box.getMaxZ(); k++) {
                    Block block = world.getBlockAt(i, j, k);
                    if (block.getType().isAir()) continue;
                    block.setType(Material.AIR);
                }
            }
        }
    }

    public static void removeSavedProtectedArea(UUID uuid) {
        new BukkitRunnable() {
            @Override
            public void run() {
                FileConfiguration data = loadConfigFile(TRIAL_DATA_FILE);
                data.set(uuid.toString() + ".ProtectedArea", null);
                saveConfig(data, TRIAL_DATA_FILE, "Failed to save protected areas to " + TRIAL_DATA_FILE);
            }
        }.runTaskAsynchronously(SurvivalSkills.getInstance());
    }

    public static void removeGroundItemsInProtectedArea(ProtectedArea protectedArea) {
        protectedArea.world().getNearbyEntities(protectedArea.boundingBox(), Item.class::isInstance)
                .stream()
                .map(Item.class::cast)
                .filter(TrialUtils::isTrialItem)
                .forEach(item -> Objects.requireNonNull(item, "item").remove());
    }

    // --- Completed trial persistence ---

    public static void saveCompletedTrials(UUID playerId, List<Integer> completedTrials,
                                           FileConfiguration data, boolean disabling) {
        List<Integer> completedTrialSnapshot = List.copyOf(completedTrials);
        if (completedTrialSnapshot.isEmpty())
            return;

        if (disabling) {
            saveCompletedTrialsSync(playerId, completedTrialSnapshot, data);
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                saveCompletedTrialsSync(playerId, completedTrialSnapshot, data);
            }
        }.runTaskAsynchronously(SurvivalSkills.getInstance());
    }

    private static void saveCompletedTrialsSync(UUID playerId, List<Integer> completedTrials,
                                                FileConfiguration data) {
        data.set(playerId + ".CompletedTrials", completedTrials);
        saveConfig(data, TRIAL_DATA_FILE, "Failed to save completed trials for " + playerId);
    }

    public static boolean isTrialItem(Item item) {
        ItemMeta meta = item.getItemStack().getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(TrialManager.getTrialObjectKey(), PersistentDataType.STRING);
    }

    // --- Spectator management ---

    public static boolean addTrialSpectator(Player p, Player target) {
        for (Trial trial : TrialManager.getTrials()) {
            if (!trial.getPlayers().contains(target)) continue;
            TrialManager.addSpectator(p, target, p.getLocation());
            trial.addSpectator(p, target);
            p.teleport(target.getLocation()); // Teleport the spectator to the target
            p.setGameMode(GameMode.SPECTATOR);

            new BukkitRunnable() {
                @Override
                public void run() {
                    target.hidePlayer(SurvivalSkills.getInstance(), p);
                    p.setSpectatorTarget(target);
                }
            }.runTaskLater(SurvivalSkills.getInstance(), 20);
            return true;
        }
        return false;
    }

    public static void removeTrialSpectator(Player p, Player target) {
        if (!TrialManager.isSpectating(p)) return;

        // Show the spectator to the player
        if (target != null) target.showPlayer(SurvivalSkills.getInstance(), p);
        if (p.getGameMode().equals(GameMode.SPECTATOR)) p.setSpectatorTarget(null);
        p.teleport(TrialManager.getSpectatorOrigin(p));
        TrialManager.removeSpectator(p);
        p.setGameMode(GameMode.SURVIVAL);
        p.sendMessage(ChatColor.GREEN + "You are no longer spectating");
        p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

        for (Trial trial : TrialManager.getTrials()) {
            if (target != null && !trial.getPlayers().contains(target)) continue;
            trial.removeSpectator(p);
        }
    }

    // --- Trial initialization ---

    public static void initializeTrial(Player p, Location pLocation) {
        // Check if the player has a pre-existing structure
        if (previousStructure(p, pLocation)) return;

        // Check if the player has an empty area around them
        if (trialSpaceConflict(p, pLocation)) return;

        // Check if the player can create a new building
        if (!TrialManager.hasProtectedArea(p.getUniqueId())) {
            Long lastCreation = TrialManager.getBuildingCreationCooldown(p.getUniqueId());
            if (lastCreation != null) {
                long timeSinceLastCreation = System.currentTimeMillis() - lastCreation;
                if (timeSinceLastCreation < COOLDOWN) {
                    long timeLeft = (COOLDOWN - timeSinceLastCreation) / 1000;
                    int minutesLeft = (int) (timeLeft / 60);
                    int secondsLeft = (int) (timeLeft % 60);
                    p.sendMessage(ChatColor.RED + "You can only create a new trial building every " + COOLDOWN_MINUTES + " minutes");
                    p.sendMessage(ChatColor.YELLOW + String.format("Time left: %d minutes and %d seconds", minutesLeft, secondsLeft));
                    p.playSound(p, ERROR_SOUND, 1, 1);
                    return;
                }
            } else {
                // Store the time that a new trial building is created
                TrialManager.setBuildingCreationCooldown(p.getUniqueId(), System.currentTimeMillis());
            }
        }

        // Load the default trial building
        ArrayList<RelativeBlock> blocks = loadTrialBuilding(TrialManager.getTrialBuildingConfig());

        // Create bounding box around the trial building
        ProtectedArea protectedArea = createProtectedArea(pLocation);
        TrialManager.putProtectedArea(p.getUniqueId(), protectedArea);

        // Get pending trial
        PendingTrial trial = TrialManager.getPendingTrial(p);
        if (trial == null) {
            sendError(p, "You do not have a pending trial", "Use /trial to start a trial");
            return;
        }

        // Create the Trial
        createTrial(trial, blocks, protectedArea, pLocation);
    }

    public static boolean previousStructure(Player p, Location pLocation) {
        if (!TrialManager.hasProtectedArea(p.getUniqueId())) return false;

        // Get the center block of the trial building from the protected area
        ProtectedArea area = TrialManager.getProtectedArea(p.getUniqueId());
        if (!p.getWorld().equals(area.world())) {
            sendError(p, "You are in the wrong world to start the trial");
            return true;
        }

        BoundingBox box = area.boundingBox();
        Location centerLocation = new Location(pLocation.getWorld(), box.getCenterX(), box.getMinY() + 1, box.getCenterZ());

        // Check if the player is within range of the center of the trial building
        if (pLocation.distance(centerLocation) > TRIAL_MAX_DISTANCE) {
            sendError(p, "You are too far away from the trial building",
                    "Stand within " + TRIAL_MAX_DISTANCE + " blocks of the trial building to start the trial");
            return true;
        }

        // Get pending trial
        PendingTrial trial = TrialManager.getPendingTrial(p);
        if (trial == null) {
            sendError(p, "You do not have a pending trial", "Use /trial to start a trial");
            return true;
        }

        // Create the Trial
        createTrial(trial, null, area, centerLocation);
        return true;
    }

    public static boolean trialSpaceConflict(Player p, Location pLocation) {
        World world = pLocation.getWorld();
        int baseX = pLocation.getBlockX();
        int baseY = pLocation.getBlockY();
        int baseZ = pLocation.getBlockZ();
        boolean griefPreventionEnabled = SurvivalSkills.getInstance().isGriefPreventionEnabled();

        for (int i = -TRIAL_AREA_RADIUS; i <= TRIAL_AREA_RADIUS; i++) {
            for (int j = 0; j <= TRIAL_AREA_HEIGHT; j++) {
                for (int k = -TRIAL_AREA_RADIUS; k <= TRIAL_AREA_RADIUS; k++) {
                    Block block = world.getBlockAt(baseX + i, baseY + j, baseZ + k);

                    // The area must be empty
                    if (!block.getType().isAir()) {
                        sendError(p, "You do not have enough space to start the trial",
                                "Stand in the middle of an empty 50x50 area with sky access");
                        return true;
                    }

                    // The area must not overlap a claim
                    if (griefPreventionEnabled && Utils.checkForClaim(p, block.getLocation())) {
                        sendError(p, "You are in a claim", "Stand in an unclaimed area to start the trial");
                        p.closeInventory();
                        return true;
                    }

                    // The block must have sky access
                    if (block.getLightFromSky() == 0) {
                        sendError(p, "You do not have enough space to start the trial",
                                "Stand in the middle of an empty 50x50 area with sky access");
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static ProtectedArea createProtectedArea(Location pLocation) {
        BoundingBox box = new BoundingBox();
        box.resize(pLocation.getX() - TRIAL_AREA_RADIUS, pLocation.getY() + TRIAL_AREA_Y_OFFSET, pLocation.getZ() - TRIAL_AREA_RADIUS,
                pLocation.getX() + TRIAL_AREA_RADIUS + 1, pLocation.getY() + TRIAL_AREA_Y_OFFSET + TRIAL_AREA_HEIGHT, pLocation.getZ() + TRIAL_AREA_RADIUS + 1);
        return new ProtectedArea(box, pLocation.getWorld());
    }

    public static void createTrial(PendingTrial pendingTrial, ArrayList<RelativeBlock> blocks, ProtectedArea area, Location centerLocation) {
        if (isGodDifficulty(pendingTrial.getTrialDifficulty())
                && !completedGodQuest(pendingTrial.getTrialMaster())) {
            // A non-null block list means initializeTrial just reserved a new building area.
            // Existing structures must remain available for later attempts.
            TrialManager.cancelPendingTrial(pendingTrial.getTrialMaster(), blocks != null);
            return;
        }

        Trial trial;
        if (blocks == null) trial = new Trial(pendingTrial.getTrialMaster(), area, centerLocation, pendingTrial.getTrialDifficulty());
        else trial = new Trial(blocks, pendingTrial.getTrialMaster(), area, centerLocation, pendingTrial.getTrialDifficulty());
        TrialManager.registerTrialBuilding(pendingTrial.getTrialMaster().getUniqueId(), centerLocation);

        if (pendingTrial.getPlayers().isEmpty()) {
            sendError(pendingTrial.getTrialMaster(), "You must have at least one player in your trial");
            TrialManager.removePendingTrial(pendingTrial.getTrialMaster());
            pendingTrial.getTrialMaster().closeInventory();
            return;
        }

        for (Player p : pendingTrial.getPlayers()) trial.getPlayers().add(p);
        // Make sure the trial master is added to the players list
        if (!trial.getPlayers().contains(pendingTrial.getTrialMaster())) {
            trial.getPlayers().add(pendingTrial.getTrialMaster());
        }

        trial.initializeScoreboards();
        trial.setSolo(pendingTrial.isSolo());
        TrialManager.removePendingTrial(pendingTrial.getTrialMaster());
        TrialManager.registerTrial(trial);
        trial.runTaskTimer(SurvivalSkills.getInstance(), 60, 1);
        if (trial.isExistingStructure()) trial.startTrial();
    }

    private static boolean isGodDifficulty(int difficulty) {
        return difficulty == SOLO_GOD_DIFFICULTY || difficulty == COOP_GOD_DIFFICULTY;
    }

    public static boolean completedGodQuest(Player p) {
        // Check if the player has an active god quest
        GodTrophyQuest quest = SurvivalSkills.getInstance().getTrophyManager().getPlayerGodQuestData().get(p.getUniqueId());
        if (quest == null && !p.hasPermission("survivalskills.op")) {
            sendError(p, "You do not have an active god quest", "Complete the god questline to unlock the god trial");
            return false;
        }

        if (quest == null) {
            sendError(p, "You do not have an active god quest",
                    "Reach main level " + ChatColor.AQUA + 100 + ChatColor.YELLOW + " and craft the god trophy to start the god quest");
            return false;
        }

        // Check if they have unlocked the god trial
        if (!p.hasPermission("survivalskills.op") && quest.getPhase() != quest.getMaxPhase()) {
            sendError(p, "You have not unlocked the god trial", "Complete the god questline to unlock the god trial");
            return false;
        }
        return true;
    }

    // --- Inventory UIs ---

    public static void openPartyTypeSelection(Player p) {
        Inventory inv = Bukkit.createInventory(null, 9, "Party Type Selection");
        ItemStack newChoice = createMenuItem(Material.OAK_SAPLING, ChatColor.GREEN + "New");
        if (newChoice == null) return;
        ItemStack existingChoice = createMenuItem(Material.OAK_LOG, ChatColor.YELLOW + "Existing");
        if (existingChoice == null) return;
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);

        inv.setItem(0, filler);
        inv.setItem(1, filler);
        inv.setItem(2, filler);
        inv.setItem(3, newChoice);
        inv.setItem(4, filler);
        inv.setItem(5, existingChoice);
        inv.setItem(6, filler);
        inv.setItem(7, filler);
        inv.setItem(8, filler);
        registerAndOpen(p, inv);
    }

    public static void openTrialTypeSelection(Player p) {
        Inventory inv = Bukkit.createInventory(null, 9, "Party Selection");
        ItemStack solo = createMenuItem(Material.DIAMOND_SWORD, ChatColor.GREEN + "Solo");
        if (solo == null) return;
        ItemStack coop = createMenuItem(Material.PLAYER_HEAD, TrialSelectionClassifier.COOP_OPTION_NAME);
        if (coop == null) return;
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);

        inv.setItem(0, filler);
        inv.setItem(1, filler);
        inv.setItem(2, filler);
        inv.setItem(3, solo);
        inv.setItem(4, filler);
        inv.setItem(5, coop);
        inv.setItem(6, filler);
        inv.setItem(7, filler);
        inv.setItem(8, filler);
        registerAndOpen(p, inv);
    }

    public static void openPartySelection(Player p) {
        Inventory inv = Bukkit.createInventory(null, 9, "Party Selection");
        // Get all available parties where the difficulty has been chosen
        int slot = 0;
        for (PendingTrial trial : TrialManager.getPendingTrials().values()) {
            if (trial.isSolo() || !trial.isChosenDifficulty()) continue;
            ItemStack item = createMenuItem(Material.PLAYER_HEAD, ChatColor.GREEN + trial.getTrialMaster().getName());
            if (item == null) return;
            inv.setItem(slot++, item);
        }
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        for (int j = slot; j < 9; j++) inv.setItem(j, filler);

        registerAndOpen(p, inv);
    }

    public static void openDifficultySelection(Player p) {
        Inventory inv = Bukkit.createInventory(null, 9, "Difficulty Selection");
        ItemStack easy = createMenuItem(Material.LIME_STAINED_GLASS_PANE, ChatColor.GREEN + "Easy");
        if (easy == null) return;
        ItemStack medium = createMenuItem(Material.YELLOW_STAINED_GLASS_PANE, ChatColor.YELLOW + "Medium");
        if (medium == null) return;
        ItemStack hard = createMenuItem(Material.RED_STAINED_GLASS_PANE, ChatColor.RED + "Hard");
        if (hard == null) return;
        ItemStack god = createMenuItem(Material.GOLD_BLOCK, ChatColor.GOLD.toString() + ChatColor.BOLD + "God");
        if (god == null) return;
        ItemStack death = createMenuItem(Material.BARRIER, ChatColor.MAGIC.toString() + ChatColor.RED + "Death");
        if (death == null) return;
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);

        inv.setItem(0, filler);
        inv.setItem(1, easy);
        inv.setItem(2, filler);

        ArrayList<Integer> beaten = TrialManager.getCompletedGamemodes(p);
        if (beaten != null) {
            inv.setItem(3, beaten.contains(1) ? medium : filler);
            inv.setItem(4, filler);
            inv.setItem(5, beaten.contains(3) ? hard : filler);
            inv.setItem(6, filler);
            inv.setItem(7, beaten.contains(5) ? god : filler);
            inv.setItem(8, beaten.contains(7) ? death : filler);
        }
        registerAndOpen(p, inv);
    }

    // --- Trial selection click handling ---

    public static void handleTrialSelectionClick(Inventory inv, Player p, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        if (item.getItemMeta() == null) return;
        TrialManager.unregisterTrialSelectionInventory(inv);
        String name = item.getItemMeta().getDisplayName();
        Material type = item.getType();

        if (TrialSelectionClassifier.isPartyMemberSelection(type, name)) {
            handlePartyMemberClick(inv, p, item, name);
            return;
        }

        if (name.equals(ChatColor.GREEN + "Solo")) {
            startSoloTrial(p);
        } else if (name.equals(TrialSelectionClassifier.COOP_OPTION_NAME)) {
            openPartyTypeSelection(p);
        } else if (name.equals(ChatColor.GREEN + "New")) {
            startNewCoopTrial(p);
        } else if (name.equals(ChatColor.YELLOW + "Existing")) {
            openPartySelection(p);
        } else if (name.equalsIgnoreCase(ChatColor.GREEN + "Confirm Party")) {
            confirmParty(p);
        } else {
            int difficulty = getDifficultyFromName(name);
            if (difficulty > 0) partyDifficulty(p, difficulty);
        }
    }

    public static void partyDifficulty(Player p, int difficulty) {
        PendingTrial trial = TrialManager.getPendingTrial(p);
        // Check if they can attempt this trial difficulty
        int trueDifficulty = difficulty * 2;
        if (trial.isSolo()) trueDifficulty -= 1;

        // If co-op, check if they have beaten the solo version of this difficulty
        if (!trial.isSolo()) {
            if (!TrialManager.hasCompletedGamemodes(p)) {
                sendError(p, "You have not beaten any solo mode trials");
                return;
            }
            int difficultyToBeat = TrialDifficulty.getSoloDifficultyForCoop(trueDifficulty);
            ArrayList<Integer> gamemodesBeaten = TrialManager.getCompletedGamemodes(p);
            if (!gamemodesBeaten.contains(difficultyToBeat)) {
                p.sendMessage(ChatColor.RED + String.format("You have not beaten solo mode on this difficulty: %s",
                        TrialDifficulty.getDifficultyNameFromTrueDifficulty(difficultyToBeat)));
                p.sendMessage(ChatColor.YELLOW + "You have beaten:");
                for (int beaten : gamemodesBeaten)
                    p.sendMessage(ChatColor.GRAY + "- " + TrialDifficulty.getDifficultyNameFromTrueDifficulty(beaten));
                p.playSound(p, ERROR_SOUND, 1, 1);
                return;
            }
        }

        if (trial.isSolo() && trueDifficulty != 1 && TrialManager.hasCompletedGamemodes(p)
                && !TrialManager.getCompletedGamemodes(p).contains(trueDifficulty - 2)) {
            sendError(p, "You have not beaten the previous difficulty");
            return;
        }

        trial.setTrialDifficulty(trueDifficulty);
        trial.setChosenDifficulty(true);

        if (trial.isSolo()) {
            p.closeInventory();
            initializeTrial(p, p.getLocation());
        } else {
            trial.updatePlayerManager();
        }
    }

    // --- Internal helpers ---

    private static File getOrCreateFile(String filename) {
        File file = new File(SurvivalSkills.getInstance().getDataFolder(), filename);
        if (!file.exists()) SurvivalSkills.getInstance().saveResource(filename, true);
        return file;
    }

    private static FileConfiguration loadConfigFile(String filename) {
        return YamlConfiguration.loadConfiguration(getOrCreateFile(filename));
    }

    private static void saveConfig(FileConfiguration config, String filename, String errorMessage) {
        try {
            config.save(getOrCreateFile(filename));
        } catch (Exception e) {
            Bukkit.getLogger().warning(errorMessage);
        }
    }

    private static ItemStack createMenuItem(Material material, String displayName) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        meta.setDisplayName(displayName);
        item.setItemMeta(meta);
        return item;
    }

    private static void registerAndOpen(Player p, Inventory inv) {
        TrialManager.registerTrialSelectionInventory(inv);
        p.openInventory(inv);
    }

    private static void sendError(Player p, String message) {
        p.sendMessage(ChatColor.RED + message);
        p.playSound(p, ERROR_SOUND, 1, 1);
    }

    private static void sendError(Player p, String message, String guidance) {
        p.sendMessage(ChatColor.RED + message);
        p.sendMessage(ChatColor.YELLOW + guidance);
        p.playSound(p, ERROR_SOUND, 1, 1);
    }

    private static void startSoloTrial(Player p) {
        TrialManager.putPendingTrial(p, new PendingTrial(p));
        openDifficultySelection(p);
    }

    private static void startNewCoopTrial(Player p) {
        PendingTrial trial = new PendingTrial(p);
        trial.setSolo(false);
        TrialManager.putPendingTrial(p, trial);
        openDifficultySelection(p);
    }

    private static void confirmParty(Player p) {
        PendingTrial trial = TrialManager.getPendingTrial(p);
        if (trial == null) {
            sendError(p, "You do not have a pending trial anymore");
            return;
        }
        trial.confirmParty();
        initializeTrial(p, p.getLocation());
    }

    private static int getDifficultyFromName(String name) {
        if (name.equalsIgnoreCase(ChatColor.GREEN + "Easy")) return 1;
        if (name.equalsIgnoreCase(ChatColor.YELLOW + "Medium")) return 2;
        if (name.equalsIgnoreCase(ChatColor.RED + "Hard")) return 3;
        if (name.equalsIgnoreCase(ChatColor.GOLD.toString() + ChatColor.BOLD + "God")) return 4;
        if (name.equalsIgnoreCase(ChatColor.MAGIC.toString() + ChatColor.RED + "Death")) return 5;
        return 0;
    }

    private static void handlePartyMemberClick(Inventory inv, Player p, ItemStack item, String name) {
        // If the clicking player is a party manager, they are blocking/removing a member
        if (TrialManager.hasPendingTrial(p)) {
            PendingTrial trial = TrialManager.getPendingTrial(p);
            Player target = Bukkit.getPlayer(ChatColor.stripColor(name));
            if (target == null) {
                trial.handleOfflinePlayerRemoval(p, ChatColor.stripColor(name));
                return;
            }
            trial.handleBlockPartyMember(target);
            return;
        }
        joinParty(inv, p, item, name);
    }

    private static void joinParty(Inventory inv, Player p, ItemStack item, String name) {
        Player target = Bukkit.getPlayer(ChatColor.stripColor(name));
        PendingTrial trial = TrialManager.getPendingTrial(target);
        if (trial == null) {
            sendError(p, "The party leader does not have a pending trial anymore");
            inv.remove(item);
            p.closeInventory();
            openPartySelection(p);
            return;
        }

        // Check if the player is blocked from joining the party
        if (trial.getBlockedPlayers().contains(p)) {
            sendError(p, "You are blocked from joining this party");
            return;
        }

        // Check if they have beaten solo mode on this difficulty
        ArrayList<Integer> beaten = TrialManager.getCompletedGamemodes(p);
        if (beaten == null || !beaten.contains(TrialDifficulty.getSoloDifficultyForCoop(trial.getTrialDifficulty()))) {
            sendError(p, "You have not beaten solo mode on this difficulty and can't join this party");
            return;
        }

        // Add the player to the selected party
        trial.handleNewPartyMember(p);
    }
}
