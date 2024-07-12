package sir_draco.survivalskills.Abilities;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import sir_draco.survivalskills.SurvivalSkills;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class Grave {

    private final int id;
    private final Location location;
    private final int lifespan;
    private final UUID uuid;
    private final SurvivalSkills plugin;
    private final ArrayList<ItemStack> items;
    private Inventory inventory;
    private GraveTimer timer;

    public Grave(int id, UUID uuid, Location location, ArrayList<ItemStack> items, int graveLifespan, SurvivalSkills plugin) {
        this.id = id;
        this.uuid = uuid;
        this.location = getValidLocation(location);
        this.items = items;
        this.lifespan = graveLifespan;
        this.plugin = plugin;

        inventory = Bukkit.createInventory(null, 54);
        addItems();

        spawnGrave();
    }

    public Location getValidLocation(Location location) {
        if (location.getBlock().isEmpty() || location.getBlock().getType().isAir()) {
            return location.getBlock().getLocation();
        }
        return getValidLocation(location.clone().add(0, 1, 0));
    }

    public void addItems() {
        for (int i = 0; i < items.size(); i++) inventory.setItem(i, items.get(i));
    }

    public void spawnGrave() {
        Block block = location.getBlock();
        block.setType(Material.CHEST);
        block.getState().setType(Material.CHEST);
        block.getState().update();

        timer = new GraveTimer(this);
        timer.runTaskTimer(plugin, 0, 20);
    }

    public void removeGrave(boolean timeout) {
        Block block = location.getBlock();
        block.setType(Material.AIR);
        block.getState().setType(Material.AIR);
        block.getState().update();

        plugin.getMainListener().getGraves().remove(location);
        if (!timeout) return;
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        player.sendRawMessage(ChatColor.RED + "Your grave has expired!");
        player.playSound(player, Sound.BLOCK_ANVIL_USE, 1, 1);
    }

    public void graveWarning(int time) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return;
        player.sendRawMessage(ChatColor.YELLOW + "Your grave will expire in " + (time / 60) + " minutes!");
    }

    public void saveGrave(FileConfiguration grave, File graveFile) throws IOException {
        grave.set("Graves." + id + ".UUID", uuid.toString());
        grave.set("Graves." + id + ".Lifespan", lifespan);
        grave.set("Graves." + id + ".Location", location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ());
        grave.set("Graves." + id + ".World", location.getWorld().getName());
        refreshItems();
        for (int i = 0; i < items.size(); i++) grave.set("Graves." + id + ".Inventory." + i, items.get(i));

        grave.save(graveFile);
    }

    public Location getLocation() {
        return location;
    }

    public int getLifespan() {
        return lifespan;
    }

    public void refreshItems() {
        items.clear();
        for (ItemStack item : inventory.getContents()) {
            if (item == null) continue;
            items.add(item);
        }
    }

    public void claimedGrave(Player p) {
        timer.cancel();
        removeGrave(false);
        p.sendRawMessage(ChatColor.GREEN + "You have claimed your grave!");
        p.playSound(p, Sound.BLOCK_CHEST_CLOSE, 1, 1);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public UUID getUUID() {
        return uuid;
    }
}
