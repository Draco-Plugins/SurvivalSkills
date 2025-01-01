package sir_draco.survivalskills.GodQuestline;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.BoundingBox;
import sir_draco.survivalskills.SurvivalSkills;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TrialManager implements Listener {

    private static final ArrayList<Trial> trials = new ArrayList<>();
    private static final HashMap<UUID, ProtectedArea> protectedAreas = new HashMap<>();

    public TrialManager() {}

    @EventHandler
    public void onTrialBuildingBreak(BlockBreakEvent e) {
        if (protectedAreas.isEmpty()) return;
        for (ProtectedArea protectedArea : protectedAreas.values()) {
            if (!protectedArea.world().equals(e.getBlock().getWorld())) continue;
            if (protectedArea.boundingBox().contains(e.getBlock().getLocation().toVector())) {
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onTrialBuildingPlace(BlockPlaceEvent e) {
        if (protectedAreas.isEmpty()) return;
        for (ProtectedArea protectedArea : protectedAreas.values()) {
            if (!protectedArea.world().equals(e.getBlock().getWorld())) continue;
            if (protectedArea.boundingBox().contains(e.getBlock().getLocation().toVector())) {
                e.setCancelled(true);
                return;
            }
        }
    }

    public static void loadProtectedAreas() {
        File file = new File(SurvivalSkills.getInstance().getDataFolder(), "godquests.yml");
        if (!file.exists()) SurvivalSkills.getInstance().saveResource("godquests.yml", true);
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);

        ConfigurationSection section = data.getConfigurationSection("");
        if (section == null) return;
        if (section.getKeys(false).isEmpty()) return;

        for (String key : section.getKeys(false)) {
            // Get the min and max boundaries of the bounding box
            if (!data.contains(key + ".ProtectedArea")) continue;
            String minString = (String) data.get(key + ".ProtectedArea.Min");
            if (minString == null) continue;
            String maxString = (String) data.get(key + ".ProtectedArea.Max");
            if (maxString == null) continue;
            String[] minLocation = minString.split(":");
            String[] maxLocation = maxString.split(":");
            if (minLocation.length != 3 || maxLocation.length != 3) continue;

            BoundingBox box = new BoundingBox();
            box.resize(Double.parseDouble(minLocation[0]), Double.parseDouble(minLocation[1]), Double.parseDouble(minLocation[2]),
                    Double.parseDouble(maxLocation[0]), Double.parseDouble(maxLocation[1]), Double.parseDouble(maxLocation[2]));

            String worldString = (String) data.get(key + ".ProtectedArea.World");
            if (worldString == null) continue;
            World world = Bukkit.getWorld(UUID.fromString(worldString));

            ProtectedArea protectedArea = new ProtectedArea(box, world);

            UUID uuid = UUID.fromString(key);
            protectedAreas.put(uuid, protectedArea);
        }
    }

    public static void handleTrials() {
        // End existing trials
        if (!trials.isEmpty()) {
            ArrayList<Trial> trials = new ArrayList<>(TrialManager.trials);
            for (Trial trial : trials) trial.endTrial();
        }

        if (protectedAreas.isEmpty()) return;

        // Save the protected areas to the config
        File file = new File(SurvivalSkills.getInstance().getDataFolder(), "godquests.yml");
        if (!file.exists()) SurvivalSkills.getInstance().saveResource("godquests.yml", true);
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);

        for (Map.Entry<UUID, ProtectedArea> protectedArea : protectedAreas.entrySet()) {
            String minLocation = protectedArea.getValue().boundingBox().getMinX() + ":"
                    + protectedArea.getValue().boundingBox().getMinY() + ":" + protectedArea.getValue().boundingBox().getMinZ();
            String maxLocation = protectedArea.getValue().boundingBox().getMaxX() + ":"
                    + protectedArea.getValue().boundingBox().getMaxY() + ":" + protectedArea.getValue().boundingBox().getMaxZ();
            data.set(protectedArea.getKey().toString() + ".ProtectedArea.Min", minLocation);
            data.set(protectedArea.getKey().toString() + ".ProtectedArea.Max", maxLocation);
            data.set(protectedArea.getKey().toString() + ".ProtectedArea.World", protectedArea.getValue().world().getUID().toString());
        }

        try {
            data.save(file);
        } catch (Exception e) {
            SurvivalSkills.getInstance().getLogger().warning("Failed to save protected areas to godquests.yml");
        }
    }

    public static ArrayList<Trial> getTrials() {
        return trials;
    }

    public static HashMap<UUID, ProtectedArea> getProtectedAreas() {
        return protectedAreas;
    }
}
