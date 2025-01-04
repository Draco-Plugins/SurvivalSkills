package sir_draco.survivalskills.Utils;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.GodQuestline.ProtectedArea;
import sir_draco.survivalskills.GodQuestline.RelativeBlock;
import sir_draco.survivalskills.GodQuestline.Trial;
import sir_draco.survivalskills.GodQuestline.TrialManager;
import sir_draco.survivalskills.SurvivalSkills;

import java.io.File;
import java.util.ArrayList;
import java.util.UUID;

public class TrialUtils {

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
                File file = new File(SurvivalSkills.getInstance().getDataFolder(), "godquests.yml");
                if (!file.exists()) SurvivalSkills.getInstance().saveResource("godquests.yml", true);
                FileConfiguration data = YamlConfiguration.loadConfiguration(file);

                data.set(uuid.toString() + ".ProtectedArea", null);

                try {
                    data.save(file);
                } catch (Exception e) {
                    SurvivalSkills.getInstance().getLogger().warning("Failed to save protected areas to godquests.yml");
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

    public static boolean isTrialItem(Item item) {
        ItemStack itemStack = item.getItemStack();
        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(TrialManager.getTrialObjectKey(), PersistentDataType.STRING);
    }

    public static boolean addTrialSpectator(Player p, Player target) {
        for (Trial trial : TrialManager.getTrials()) {
            if (!trial.getPlayer().equals(target)) continue;
            TrialManager.getSpectatingPlayers().put(p, p.getLocation());
            TrialManager.getSpectatorTargets().put(p, target);
            trial.addSpectator(p);
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
                if (!trial.getPlayer().equals(target)) continue;
                trial.removeSpectator(p);
            }
            return true;
        }
        return false;
    }
}
