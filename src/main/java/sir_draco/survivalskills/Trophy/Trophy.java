package sir_draco.survivalskills.Trophy;

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
        // Item Entity
        effects = new TrophyEffects(plugin, loc, type, this, playerName);
        effects.runTaskTimer(plugin, 20, 2);
    }

    public void breakTrophy(ItemStack item) {
        if (loc.getWorld() == null) return;
        loc.getWorld().dropItemNaturally(loc, item);
        loc.getBlock().setType(Material.AIR);
        loc.getBlock().getState().update();

        effects.removeItem();
        effects.cancel();
    }

    public void shutdownTrophy() {
        if (effects.getGodTrophy() != null) effects.getGodTrophy().removePlayer();
        if (loc.getWorld() == null) return;
        loc.getBlock().setType(Material.AIR);
        loc.getBlock().getState().update();
        effects.removeItem();
        effects.cancel();
    }

    public void restartTrophy(boolean run) {
        if (!run) {
            effects.removeItem();
            effects.setRun(false);
            return;
        }
        if (effects.getType() != 10) effects.spawnItem(0.5, 1.0, 0.5);
        effects.typeSpecificStart();
        effects.setRun(true);
    }

    public boolean canBreakTrophy(UUID player) {
        return player.equals(uuid);
    }

    public int getTrophyType() {
        switch (type) {
            case "CaveTrophy":
                return 1;
            case "ForestTrophy":
                return 2;
            case "FarmingTrophy":
                return 3;
            case "OceanTrophy":
                return 4;
            case "FishingTrophy":
                return 5;
            case "ColorTrophy":
                return 6;
            case "NetherTrophy":
                return 7;
            case "EndTrophy":
                return 8;
            case "ChampionTrophy":
                return 9;
            default:
                return 10;
        }
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
