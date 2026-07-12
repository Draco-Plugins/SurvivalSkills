package sir_draco.survivalskills.trophy;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public class Trophy {

    private static final int GOD_TROPHY_CYCLE_THRESHOLD = 130;

    private final Location loc;
    private final UUID uuid;
    private final TrophyType type;
    private final int id;
    private final String playerName;
    private TrophyEffects effects;

    public Trophy(Location loc, UUID uuid, String type, int id, String playerName) {
        this.loc = loc;
        this.uuid = uuid;
        this.type = TrophyType.fromName(type);
        this.id = id;
        this.playerName = playerName;
    }

    public void spawnTrophy(SurvivalSkills plugin, boolean freshPlacement) {
        Block block = loc.getBlock();
        block.setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
        block.getState().update();

        effects = new TrophyEffects(plugin, loc, type, this, playerName, uuid);
        effects.init(freshPlacement);
        effects.runTaskTimer(plugin, 20, 2);
    }

    public void breakTrophy(ItemStack item) {
        if (loc.getWorld() == null) {
            return;
        }
        loc.getWorld().dropItemNaturally(loc, item);
        if (effects != null && effects.getGodTrophy() != null) {
            effects.getGodTrophy().destroyPlayer();
        }
        cleanupTrophyBlock();
    }

    public void shutdownTrophy() {
        if (loc.getWorld() == null) {
            return;
        }
        cleanupTrophyBlock();
    }

    public void pauseTrophy() {
        if (effects == null) {
            return;
        }
        effects.removeItem();
        effects.setRun(false);
    }

    public void resumeTrophy() {
        if (effects == null) {
            return;
        }
        if (type != TrophyType.GOD) {
            effects.spawnItem(0.5, 1.0, 0.5);
        }
        if (effects.getGodTrophy() != null && effects.getCycle() > GOD_TROPHY_CYCLE_THRESHOLD) {
            try {
                effects.getGodTrophy().spawnPlayer(playerName, uuid);
            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING, "[SurvivalSkills] Failed to spawn player for trophy: " + id);
            }
        }
        effects.behaviorStart();
        effects.setRun(true);
    }

    public boolean canBreakTrophy(UUID player) {
        return player.equals(uuid);
    }

    public int getTrophyType() {
        return type.getId();
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getType() {
        return type.getName();
    }

    public int getID() {
        return id;
    }

    public Optional<TrophyEffects> getEffects() {
        return Optional.ofNullable(effects);
    }

    public String getPlayerName() {
        return playerName;
    }

    private void cleanupTrophyBlock() {
        loc.getBlock().setType(Material.AIR);
        loc.getBlock().getState().update();
        if (effects != null) {
            effects.removeItem();
            effects.cancel();
        }
    }
}
