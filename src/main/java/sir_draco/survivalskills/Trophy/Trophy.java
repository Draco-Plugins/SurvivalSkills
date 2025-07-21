package sir_draco.survivalskills.Trophy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.UUID;

public class Trophy {

    private final Location loc;
    private final UUID uuid;
    private final String type;
    private final int id;
    private final String playerName;
    private TrophyEffects effects;

    public Trophy(Location loc, UUID uuid, String type, int id, String playerName) {
        this.loc = loc;
        this.uuid = uuid;
        this.type = type;
        this.id = id;
        this.playerName = playerName;
    }

    public void spawnTrophy(SurvivalSkills plugin) {
        // Block
        Block block = loc.getBlock();
        block.setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
        block.getState().update();

        int type = getTrophyType();
        effects = new TrophyEffects(plugin, loc, type, this, playerName, uuid);
        effects.runTaskTimer(plugin, 20, 2);
    }

    public void breakTrophy(ItemStack item) {
        if (loc.getWorld() == null) return;
        loc.getWorld().dropItemNaturally(loc, item);
        loc.getBlock().setType(Material.AIR);
        loc.getBlock().getState().update();

        if (effects != null) {
            if (effects.getGodTrophy() != null) effects.getGodTrophy().destroyPlayer();
            effects.removeItem();
            effects.cancel();
        }
    }

    public void shutdownTrophy() {
        if (loc.getWorld() == null) return;
        loc.getBlock().setType(Material.AIR);
        loc.getBlock().getState().update();
        if (effects != null) {
            effects.removeItem();
            effects.cancel();
        }
    }

    public void restartTrophy(boolean run) {
        if (effects == null) return;
        if (!run) {
            effects.removeItem();
            effects.setRun(false);
            return;
        }

        if (effects.getType() != 10) effects.spawnItem(0.5, 1.0, 0.5);
        if (effects.getGodTrophy() != null && effects.getCycle() > 130) {
            try {
                effects.getGodTrophy().spawnPlayer(playerName, uuid);
            } catch (Exception e) {
                Bukkit.getLogger().warning("Failed to spawn player for trophy: " + id);
            }
        }
        effects.typeSpecificStart();
        effects.setRun(true);
    }

    public boolean canBreakTrophy(UUID player) {
        return player.equals(uuid);
    }

    public int getTrophyType() {
        return switch (type) {
            case "CaveTrophy" -> 1;
            case "ForestTrophy" -> 2;
            case "FarmingTrophy" -> 3;
            case "OceanTrophy" -> 4;
            case "FishingTrophy" -> 5;
            case "ColorTrophy" -> 6;
            case "NetherTrophy" -> 7;
            case "EndTrophy" -> 8;
            case "ChampionTrophy" -> 9;
            default -> 10;
        };
    }

    public UUID getUUID() {
        return uuid;
    }

    public String getType() {
        return type;
    }

    public int getID() {
        return id;
    }

    public TrophyEffects getEffects() {
        return effects;
    }

    public String getPlayerName() {
        return playerName;
    }
}
