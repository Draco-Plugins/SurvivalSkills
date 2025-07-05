package sir_draco.survivalskills.Utils;

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
import sir_draco.survivalskills.GodQuestline.*;
import sir_draco.survivalskills.SurvivalSkills;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

public class TrialUtils {

    public static ArrayList<RelativeBlock> trialBuildingBlocks = new ArrayList<>();

    public static ArrayList<Block> getBlocks(Location location, int x, int y, int z) {
        ArrayList<Block> blocks = new ArrayList<>();
        for (int i = -(x/2); i <= x/2; i++) {
            for (int j = -1; j < y; j++) {
                for (int k = -(z/2); k <= z/2; k++) {
                    Block block = location.clone().add(i, j, k).getBlock();
                    if (block.getType().isAir()) continue;
                    blocks.add(block);
                }
            }
        }
        return blocks;
    }

    public static void storeTrialBuilding(Location relativeLocation, ArrayList<Block> blocks) {
        // Store blocks in config
        File file = new File(SurvivalSkills.getInstance().getDataFolder(), "trialbuilding.yml");
        if (!file.exists()) SurvivalSkills.getInstance().saveResource("trialbuilding.yml", true);
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("Blocks", null);

        int i = 1;
        for (Block block : blocks) {
            Location loc = block.getLocation();
            // Locations relative to the player
            config.set("Blocks." + i + ".X", loc.getBlockX() - relativeLocation.getBlockX());
            config.set("Blocks." + i + ".Y", loc.getBlockY() - relativeLocation.getBlockY());
            config.set("Blocks." + i + ".Z", loc.getBlockZ() - relativeLocation.getBlockZ());
            config.set("Blocks." + i + ".Type", block.getType().name());
            config.set("Blocks." + i + ".Data", block.getBlockData().getAsString());
            i++;
        }

        try {
            config.save(file);
        } catch (Exception e) {
            Bukkit.getLogger().warning("Failed to save trial building to trialbuilding.yml");
        }
    }

    public static ArrayList<RelativeBlock> loadTrialBuilding(FileConfiguration config) {
        if (!trialBuildingBlocks.isEmpty()) return trialBuildingBlocks;

        ArrayList<RelativeBlock> blocks = new ArrayList<>();
        ConfigurationSection section = config.getConfigurationSection("Blocks");
        if (section == null) return blocks;
        if (section.getKeys(false).isEmpty()) {
            Bukkit.getLogger().warning("No trial building saved in trialbuilding.yml");
            return blocks;
        }

        for (String key : section.getKeys(false)) {
            int x = config.getInt("Blocks." + key + ".X");
            int y = config.getInt("Blocks." + key + ".Y");
            int z = config.getInt("Blocks." + key + ".Z");
            Material material = Material.valueOf(config.getString("Blocks." + key + ".Type"));
            String blockDataString = config.getString("Blocks." + key + ".Data");
            if (blockDataString == null) continue;
            BlockData data = Bukkit.createBlockData(blockDataString);
            blocks.add(new RelativeBlock(x, y, z, data, material));
        }

        if (blocks.isEmpty()) {
            Bukkit.getLogger().warning("No trial building blocks found in trialbuilding.yml");
        } else {
            Bukkit.getLogger().info("Loaded " + blocks.size() + " trial building blocks from trialbuilding.yml");
            trialBuildingBlocks.addAll(blocks);
        }

        return blocks;
    }

    // Takes a block, finds the new relative location for the block, and copies the data into the new block
    public static void convertBlockToRelative(RelativeBlock block, Location location) {
        Location loc = new Location(location.getWorld(),
                location.getBlockX() + block.x(), location.getBlockY() + block.y(), location.getBlockZ() + block.z());
        Block relativeBlock = loc.getBlock();
        relativeBlock.setType(block.material());
        relativeBlock.setBlockData(block.data());
        relativeBlock.getState().update();
    }

    public static RelativeBlock getRandomBlock(ArrayList<RelativeBlock> building) {
        RelativeBlock block = building.get((int) (Math.random() * building.size()));
        building.remove(block);
        return block;
    }

    public static void clearTrialBuilding(Location centerLocation) {
        for (int i = -25; i <= 25; i++) {
            for (int j = -1; j <= 30; j++) {
                for (int k = -25; k <= 25; k++) {
                    Block block = centerLocation.clone().add(i, j, k).getBlock();
                    if (block.getType().isAir()) continue;
                    block.setType(Material.AIR);
                }
            }
        }
    }

    public static void removeProtectedArea(ProtectedArea protectedArea) {
        for (int i = (int) protectedArea.boundingBox().getMinX(); i <= protectedArea.boundingBox().getMaxX(); i++) {
            for (int j = (int) protectedArea.boundingBox().getMinY(); j <= protectedArea.boundingBox().getMaxY(); j++) {
                for (int k = (int) protectedArea.boundingBox().getMinZ(); k <= protectedArea.boundingBox().getMaxZ(); k++) {
                    Block block = new Location(protectedArea.world(), i, j, k).getBlock();
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
                File file = new File(SurvivalSkills.getInstance().getDataFolder(), "trialdata.yml");
                if (!file.exists()) SurvivalSkills.getInstance().saveResource("trialdata.yml", true);
                FileConfiguration data = YamlConfiguration.loadConfiguration(file);

                data.set(uuid.toString() + ".ProtectedArea", null);

                try {
                    data.save(file);
                } catch (Exception e) {
                    SurvivalSkills.getInstance().getLogger().warning("Failed to save protected areas to trialdata.yml");
                }
            }
        }.runTaskAsynchronously(SurvivalSkills.getInstance());
    }

    public static void removeGroundItemsInProtectedArea(ProtectedArea protectedArea) {
        // Get all entities in the protected area
        protectedArea.world().getNearbyEntities(protectedArea.boundingBox(), entity -> true).forEach(entity -> {
            if (entity instanceof Item) {
                if (isTrialItem((Item) entity)) entity.remove();
            }
        });
    }

    public static void saveCompletedTrials(Player p) {
        new BukkitRunnable() {
            @Override
            public void run() {
                File file = new File(SurvivalSkills.getInstance().getDataFolder(), "trialdata.yml");
                if (!file.exists()) SurvivalSkills.getInstance().saveResource("trialdata.yml", true);
                FileConfiguration data = YamlConfiguration.loadConfiguration(file);

                ArrayList<Integer> completedTrials = TrialManager.getPlayerGamemodesBeaten().get(p);
                if (completedTrials == null) return;
                if (completedTrials.isEmpty()) return;
                data.set(p.getUniqueId() + ".CompletedTrials", completedTrials);

                try {
                    data.save(file);
                } catch (Exception e) {
                    SurvivalSkills.getInstance().getLogger().warning("Failed to save completed trials to trialdata.yml");
                }
            }
        }.runTaskAsynchronously(SurvivalSkills.getInstance());
    }

    public static boolean isTrialItem(Item item) {
        ItemStack itemStack = item.getItemStack();
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(TrialManager.getTrialObjectKey(), PersistentDataType.STRING);
    }

    public static boolean addTrialSpectator(Player p, Player target) {
        for (Trial trial : TrialManager.getTrials()) {
            if (!trial.getPlayers().contains(target)) continue;
            TrialManager.getSpectatingPlayers().put(p, p.getLocation());
            TrialManager.getSpectatorTargets().put(p, target);
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

    public static boolean removeTrialSpectator(Player p, Player target) {
        if (TrialManager.getSpectatingPlayers().containsKey(p)) {
            target.showPlayer(SurvivalSkills.getInstance(), p); // Show the spectator to the world
            if (p.getGameMode().equals(GameMode.SPECTATOR))
                p.setSpectatorTarget(null);
            p.teleport(TrialManager.getSpectatingPlayers().get(p));
            TrialManager.getSpectatingPlayers().remove(p);
            TrialManager.getSpectatorTargets().remove(p);
            p.setGameMode(GameMode.SURVIVAL);
            p.sendRawMessage(ChatColor.GREEN + "You are no longer spectating");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

            for (Trial trial : TrialManager.getTrials()) {
                if (!trial.getPlayers().contains(target)) continue;
                trial.removeSpectator(p);
            }
            return true;
        }
        return false;
    }

    public static void initializeTrial(Player p, Location pLocation) {
        // Check if the player has a pre-existing structure
        if (previousStructure(p, pLocation)) return;

        // Check if the player has an empty 50x50x30 area around them
        if (emptyArea(p, pLocation)) return;

        // Load the default trial building
        ArrayList<RelativeBlock> blocks = TrialUtils.loadTrialBuilding(TrialManager.getTrialBuildingConfig());

        // Create bounding box around the trial building
        ProtectedArea protectedArea = createProtectedArea(pLocation);
        TrialManager.getProtectedAreas().put(p.getUniqueId(), protectedArea);

        // Get pending trial
        PendingTrial trial = TrialManager.getPendingTrials().get(p);
        if (trial == null) {
            p.sendRawMessage(ChatColor.RED + "You do not have a pending trial");
            p.sendRawMessage(ChatColor.YELLOW + "Use /trial to start a trial");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }

        // Create the Trial
        createTrial(trial, blocks, protectedArea, pLocation);
    }

    public static boolean previousStructure(Player p, Location pLocation) {
        if (TrialManager.getProtectedAreas().containsKey(p.getUniqueId())) {
            // Get the center block of the trial building from the protected area
            ProtectedArea area = TrialManager.getProtectedAreas().get(p.getUniqueId());
            if (!p.getWorld().equals(area.world())) {
                p.sendRawMessage(ChatColor.RED + "You are in the wrong world to start the trial");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }

            BoundingBox box = area.boundingBox();
            Location centerLocation = new Location(pLocation.getWorld(), box.getCenterX(), box.getMinY() + 1, box.getCenterZ());

            // Get pending trial
            PendingTrial trial = TrialManager.getPendingTrials().get(p);
            if (trial == null) {
                p.sendRawMessage(ChatColor.RED + "You do not have a pending trial");
                p.sendRawMessage(ChatColor.YELLOW + "Use /trial to start a trial");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }

            // Create the Trial
            createTrial(trial, null, area, centerLocation);
            return true;
        }
        return false;
    }

    public static boolean emptyArea(Player p, Location pLocation) {
        for (int i = -25; i <= 25; i++) {
            for (int j = 0; j <= 30; j++) {
                for (int k = -25; k <= 25; k++) {
                    if (!pLocation.clone().add(i, j, k).getBlock().getType().isAir()) {
                        p.sendRawMessage(ChatColor.RED + "You do not have enough space to start the trial");
                        p.sendRawMessage(ChatColor.YELLOW + "Stand in the middle of an empty 50x50x30 area");
                        p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                        return true;
                    }

                    // Check if there are any existing claims nearby
                    if (SurvivalSkills.getInstance().isGriefPreventionEnabled()
                            && SurvivalSkills.getInstance().checkForClaim(p, pLocation.clone().add(i, j, k))) {
                        p.sendRawMessage(ChatColor.RED + "You are in a claim");
                        p.sendRawMessage(ChatColor.YELLOW + "Stand in an unclaimed area to start the trial");
                        p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static ProtectedArea createProtectedArea(Location pLocation) {
        BoundingBox box = new BoundingBox();
        box.resize(pLocation.getX() - 25, pLocation.getY() - 1, pLocation.getZ() - 25,
                pLocation.getX() + 26, pLocation.getY() + 29, pLocation.getZ() + 26);
        return new ProtectedArea(box, pLocation.getWorld());
    }

    public static void createTrial(PendingTrial pendingTrial, ArrayList<RelativeBlock> blocks, ProtectedArea area, Location centerLocation) {
        if (pendingTrial.getTrialDifficulty() == 4 && !completedGodQuest(pendingTrial.getTrialMaster())) return;

        Trial trial;
        if (blocks == null) trial = new Trial(pendingTrial.getTrialMaster(), area, centerLocation, pendingTrial.getTrialDifficulty());
        else trial = new Trial(blocks, pendingTrial.getTrialMaster(), area, centerLocation, pendingTrial.getTrialDifficulty());
        TrialManager.registerTrialBuilding(pendingTrial.getTrialMaster().getUniqueId(), centerLocation);

        for (Player p : pendingTrial.getPlayers()) trial.getPlayers().add(p);
        trial.initializeScoreboards();
        trial.setSolo(pendingTrial.isSolo());
        trial.setMaxWave(pendingTrial.getTrialDifficulty() * 5);
        TrialManager.getPendingTrials().remove(pendingTrial.getTrialMaster());
        TrialManager.getTrials().add(trial);
        trial.runTaskTimer(SurvivalSkills.getInstance(), 60, 1);
    }

    public static boolean completedGodQuest(Player p) {
        // Check if the player has an active god quest
        GodTrophyQuest quest = null;
        if (SurvivalSkills.getInstance().getTrophyManager().getPlayerGodQuestData().containsKey(p.getUniqueId()))
            quest = SurvivalSkills.getInstance().getTrophyManager().getPlayerGodQuestData().get(p.getUniqueId());
        else if (!p.hasPermission("survivalskills.op")){
            p.sendRawMessage(ChatColor.RED + "You do not have an active god quest");
            p.sendRawMessage(ChatColor.YELLOW + "Complete the god questline to unlock the god trial");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return false;
        }

        if (quest == null) {
            p.sendRawMessage(ChatColor.RED + "You do not have an active god quest");
            p.sendRawMessage(ChatColor.YELLOW + "Reach main level " + ChatColor.AQUA + 100 + ChatColor.YELLOW
                    + " and craft the god trophy to start the god quest");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return false;
        }

        // Check if they have unlocked the god trial
        if (!p.hasPermission("survivalskills.op") && quest.getPhase() != quest.getMaxPhase()) {
            p.sendRawMessage(ChatColor.RED + "You have not unlocked the god trial");
            p.sendRawMessage(ChatColor.YELLOW + "Complete the god questline to unlock the god trial");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return false;
        }
        return true;
    }

    public static void openPartyTypeSelection(Player p) {
        Inventory inv = Bukkit.createInventory(null, 9, "Party Type Selection");

        ItemStack newChoice = new ItemStack(Material.OAK_SAPLING);
        ItemMeta newMeta = newChoice.getItemMeta();
        if (newMeta == null) return;
        newMeta.setDisplayName(ChatColor.GREEN + "New");
        newChoice.setItemMeta(newMeta);

        ItemStack existingChoice = new ItemStack(Material.OAK_LOG);
        ItemMeta existingMeta = existingChoice.getItemMeta();
        if (existingMeta == null) return;
        existingMeta.setDisplayName(ChatColor.YELLOW + "Existing");
        existingChoice.setItemMeta(existingMeta);

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
        TrialManager.getTrialSelectionInventories().add(inv);
        p.openInventory(inv);
    }

    public static void openTrialTypeSelection(Player p) {
        Inventory inv = Bukkit.createInventory(null, 9, "Party Selection");

        ItemStack solo = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta soloMeta = solo.getItemMeta();
        if (soloMeta == null) return;
        soloMeta.setDisplayName(ChatColor.GREEN + "Solo");
        solo.setItemMeta(soloMeta);

        ItemStack coop = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta coopMeta = coop.getItemMeta();
        if (coopMeta == null) return;
        coopMeta.setDisplayName(ChatColor.GREEN + "Co-op");
        coop.setItemMeta(coopMeta);

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
        TrialManager.getTrialSelectionInventories().add(inv);
        p.openInventory(inv);
    }

    public static void openPartySelection(Player p) {
        Inventory inv = Bukkit.createInventory(null, 9, "Party Selection");

        // Get all available parties where the difficulty has been chosen and
        ArrayList<ItemStack> availableParties = new ArrayList<>();
        for (PendingTrial trial : TrialManager.getPendingTrials().values()) {
            if (trial.isSolo()) continue;
            if (!trial.isChosenDifficulty()) continue;
            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;
            meta.setDisplayName(ChatColor.GREEN + trial.getTrialMaster().getName());
            item.setItemMeta(meta);
            availableParties.add(item);
        }
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);

        // Set the inventory
        int i = 0;
        for (ItemStack item : availableParties) {
            inv.setItem(i, item);
            i++;
        }
        for (int j = i; j < 9; j++) inv.setItem(j, filler);

        TrialManager.getTrialSelectionInventories().add(inv);
        p.openInventory(inv);
    }

    public static void openDifficultySelection(Player p) {
        Inventory inv = Bukkit.createInventory(null, 9, "Difficulty Selection");

        ItemStack easy = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta easyMeta = easy.getItemMeta();
        if (easyMeta == null) return;
        easyMeta.setDisplayName(ChatColor.GREEN + "Easy");
        easy.setItemMeta(easyMeta);

        ItemStack medium = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta mediumMeta = medium.getItemMeta();
        if (mediumMeta == null) return;
        mediumMeta.setDisplayName(ChatColor.YELLOW + "Medium");
        medium.setItemMeta(mediumMeta);

        ItemStack hard = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta hardMeta = hard.getItemMeta();
        if (hardMeta == null) return;
        hardMeta.setDisplayName(ChatColor.RED + "Hard");
        hard.setItemMeta(hardMeta);

        ItemStack god = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta godMeta = god.getItemMeta();
        if (godMeta == null) return;
        godMeta.setDisplayName(ChatColor.GOLD.toString() + ChatColor.BOLD + "God");
        god.setItemMeta(godMeta);

        ItemStack death = new ItemStack(Material.BARRIER);
        ItemMeta deathMeta = death.getItemMeta();
        if (deathMeta == null) return;
        deathMeta.setDisplayName(ChatColor.MAGIC.toString() + ChatColor.RED + "Death");
        death.setItemMeta(deathMeta);

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);

        inv.setItem(0, filler);
        inv.setItem(1, easy);
        inv.setItem(2, filler);

        if (TrialManager.getPlayerGamemodesBeaten().get(p).contains(1)) inv.setItem(3, medium);
        else inv.setItem(3, filler);

        inv.setItem(4, filler);

        if (TrialManager.getPlayerGamemodesBeaten().get(p).contains(3)) inv.setItem(5, hard);
        else inv.setItem(5, filler);

        inv.setItem(6, filler);

        if (TrialManager.getPlayerGamemodesBeaten().get(p).contains(5)) inv.setItem(7, god);
        else inv.setItem(7, filler);

        if (TrialManager.getPlayerGamemodesBeaten().get(p).contains(7)) inv.setItem(8, death);
        else inv.setItem(8, filler);
        TrialManager.getTrialSelectionInventories().add(inv);
        p.openInventory(inv);
    }

    public static void partyDifficulty(Player p, int difficulty) {
        PendingTrial trial = TrialManager.getPendingTrials().get(p);
        // Check if they can attempt this trial difficulty
        int trueDifficulty = difficulty * 2;
        if (trial.isSolo()) trueDifficulty -= 1;

        // If co-op, check if they have beaten the solo version of this difficulty
        if (!trial.isSolo()) {
            if (TrialManager.getPlayerGamemodesBeaten().get(p).contains(trueDifficulty - 1)) {
                p.sendRawMessage(ChatColor.RED + "You have not beaten solo mode on this difficulty");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return;
            }
        }

        if (trial.isSolo() && trueDifficulty != 1
                && !TrialManager.getPlayerGamemodesBeaten().get(p).contains(trueDifficulty - 2)) {
            p.sendRawMessage(ChatColor.RED + "You have not beaten the previous difficulty");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }

        trial.setTrialDifficulty(trueDifficulty);
        trial.setChosenDifficulty(true);

        if (trial.isSolo()) initializeTrial(p, p.getLocation());
        else trial.updatePlayerManager();
    }

    public static void handleTrialSelectionClick(Inventory inv, Player p, ItemStack item) {
        if (item == null || item.getType().equals(Material.AIR)) return;
        if (item.getItemMeta() == null) return;
        TrialManager.getTrialSelectionInventories().remove(inv);
        String name = item.getItemMeta().getDisplayName();
        Material type = item.getType();

        if (name.equals(ChatColor.GREEN + "Solo")) {
            PendingTrial trial = new PendingTrial(p);
            TrialManager.getPendingTrials().put(p, trial);
            openDifficultySelection(p);
        } else if (name.equals(ChatColor.GREEN + "Co-op")) {
            openPartyTypeSelection(p);
        } else if (name.equals(ChatColor.GREEN + "New")) {
            PendingTrial trial = new PendingTrial(p);
            trial.setSolo(false);
            TrialManager.getPendingTrials().put(p, trial);
            openDifficultySelection(p);
        } else if (name.equals(ChatColor.YELLOW + "Existing")) {
            openPartySelection(p);
        } else if (name.equalsIgnoreCase(ChatColor.GREEN + "Confirm Party")) {
            PendingTrial trial = TrialManager.getPendingTrials().get(p);
            if (trial == null) {
                p.sendRawMessage(ChatColor.RED + "You do not have a pending trial anymore");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return;
            }

            initializeTrial(p, p.getLocation());
        }
        else if (name.equalsIgnoreCase(ChatColor.GREEN + "Easy")) {
            partyDifficulty(p, 1);
        } else if (name.equalsIgnoreCase(ChatColor.YELLOW + "Medium")) {
            partyDifficulty(p, 2);
        } else if (name.equalsIgnoreCase(ChatColor.RED + "Hard")) {
            partyDifficulty(p, 3);
        } else if (name.equalsIgnoreCase(ChatColor.GOLD.toString() + ChatColor.BOLD + "God")) {
            partyDifficulty(p, 4);
        } else if (name.equalsIgnoreCase(ChatColor.MAGIC.toString() + ChatColor.RED + "Death")) {
            partyDifficulty(p, 5);
        }
        else if (type.equals(Material.PLAYER_HEAD)) {
            // Check if they are the Party Manager
            if (!TrialManager.getPendingTrials().containsKey(p)) {
                PendingTrial trial = TrialManager.getPendingTrials().get(p);

                // Block the player selected from joining this party
                Player target = Bukkit.getPlayer(ChatColor.stripColor(name));
                if (target == null) {
                    trial.handleOfflinePlayerRemoval(p);
                    return;
                }

                // Block the player
                trial.handleBlockPartyMember(target);
                return;
            }

            // Check if the player can join the party
            Player target = Bukkit.getPlayer(ChatColor.stripColor(name));
            PendingTrial trial = TrialManager.getPendingTrials().get(target);
            if (trial == null) {
                p.sendRawMessage(ChatColor.RED + "The party leader does not have a pending trial anymore");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                inv.remove(item);
                p.closeInventory();
                openPartySelection(p);
                return;
            }

            // Check if the player is blocked from joining the party
            if (trial.getBlockedPlayers().contains(p)) {
                p.sendRawMessage(ChatColor.RED + "You are blocked from joining this party");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return;
            }

            // Check if they have beaten solo mode on this difficulty
            if (!TrialManager.getPlayerGamemodesBeaten().get(p).contains(trial.getTrialDifficulty() * 2 - 1)) {
                p.sendRawMessage(ChatColor.RED + "You have not beaten solo mode on this difficulty and can't join this party");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return;
            }

            // Add the player to the selected party
            trial.handleNewPartyMember(p);
        }
    }
}
