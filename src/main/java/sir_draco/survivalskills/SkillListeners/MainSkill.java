package sir_draco.survivalskills.SkillListeners;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.Abilities.DeathLocationTimer;
import sir_draco.survivalskills.Abilities.Grave;
import sir_draco.survivalskills.Rewards.PlayerRewards;
import sir_draco.survivalskills.SurvivalSkills;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainSkill implements Listener {

    private final SurvivalSkills plugin;
    private final HashMap<Player, ArrayList<Location>> deathLocations = new HashMap<>();
    private final HashMap<Location, Grave> graves = new HashMap<>();
    private final HashMap<Inventory, Grave> openGraves = new HashMap<>();
    private final int graveLifespan;
    private final File graveFile;
    private final FileConfiguration grave;
    private int nextGraveID;

    public MainSkill(SurvivalSkills plugin) {
        this.plugin = plugin;
        graveFile = new File(plugin.getDataFolder(), "graves.yml");
        if (!graveFile.exists()) plugin.saveResource("graves.yml", true);
        grave = YamlConfiguration.loadConfiguration(graveFile);
        graveLifespan = plugin.getTrueConfig().getInt("GraveLifespan");
        nextGraveID = grave.getInt("NextGraveID");
        loadGraves();
    }

    @EventHandler (priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);

        if (deathLocations.containsKey(p)) deathLocations.get(p).add(p.getLocation());
        else {
            ArrayList<Location> locations = new ArrayList<>();
            locations.add(p.getLocation());
            deathLocations.put(p, locations);
        }

        DeathLocationTimer timer = new DeathLocationTimer(plugin, p, p.getLocation(), graveLifespan);
        timer.runTaskTimerAsynchronously(plugin, 0, 20);

        if (rewards.getReward("Main", "KeepExperience").isApplied()) {
            e.setKeepLevel(true);
            e.setDroppedExp(0);
        }
        if (Boolean.TRUE.equals(e.getEntity().getWorld().getGameRuleValue(GameRule.KEEP_INVENTORY))) return;
        if (rewards.getReward("Main", "KeepInventory").isApplied()) {
            e.setKeepInventory(true);
            e.getDrops().clear();
            return;
        }
        if (e.getKeepInventory()) return;

        if (e.getDrops().isEmpty()) return;
        if (!rewards.getReward("Main", "Gravestone").isApplied()) return;
        Grave grave = new Grave(nextGraveID, p.getUniqueId(), p.getLocation(), new ArrayList<>(e.getDrops()), graveLifespan, plugin);
        graves.put(grave.getLocation(), grave);
        e.getDrops().clear();
        nextGraveID++;
    }

    @EventHandler
    public void graveOpen(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getHand() != EquipmentSlot.HAND) return;

        Block block = e.getClickedBlock();
        if (block == null) return;
        if (block.getType() != Material.CHEST) return;

        Location location = block.getLocation();
        if (!graves.containsKey(location)) return;
        e.setCancelled(true);
        Grave grave = graves.get(location);
        Player p = e.getPlayer();

        if (!grave.getUUID().equals(p.getUniqueId())) {
            p.sendRawMessage(ChatColor.RED + "This is not your grave!");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }
        p.openInventory(grave.getInventory());
        p.playSound(p, Sound.BLOCK_CHEST_OPEN, 1, 1);
        openGraves.put(grave.getInventory(), grave);
    }

    @EventHandler
    public void graveClose(InventoryCloseEvent e) {
        if (openGraves.isEmpty()) return;
        Inventory inv = e.getInventory();
        if (!openGraves.containsKey(inv)) return;

        Grave grave = openGraves.get(inv);
        if (inv.isEmpty()) grave.claimedGrave((Player) e.getPlayer());
        else grave.refreshItems();
        openGraves.remove(inv);
    }

    @EventHandler
    public void graveDestroy(BlockBreakEvent e) {
        Block block = e.getBlock();
        Location location = block.getLocation();
        if (!graves.containsKey(location)) return;
        e.setCancelled(true);

        Player p = e.getPlayer();
        p.sendRawMessage(ChatColor.RED + "You cannot break a grave!");
        p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    }

    @EventHandler
    public void placeFireworkCannon(BlockPlaceEvent e) {
        if (!e.getBlockPlaced().getType().equals(Material.CAMPFIRE)) return;
        ItemMeta meta = e.getItemInHand().getItemMeta();
        if (meta == null) return;
        if (!meta.hasCustomModelData()) return;
        if (meta.getCustomModelData() != 15) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void useFireworkCannon(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR) return;
        if (e.getHand() != EquipmentSlot.HAND) return;
        ItemStack hand = e.getItem();
        if (hand == null) return;
        ItemMeta meta = hand.getItemMeta();
        if (meta == null) return;
        if (!meta.hasCustomModelData()) return;
        if (meta.getCustomModelData() != 18) return;
        e.setCancelled(true);
        Color color = Color.fromRGB((int) (Math.random() * 255), (int) (Math.random() * 255), (int) (Math.random() * 255));
        Vector vector = e.getPlayer().getLocation().getDirection();
        Location loc = e.getPlayer().getLocation().clone().add(vector.multiply(5));
        spawnFirework(loc, 1, FireworkEffect.Type.BALL, color, false, false, null);
    }

    public HashMap<Player, ArrayList<Location>> getDeathLocations() {
        return deathLocations;
    }

    public HashMap<Location, Grave> getGraves() {
        return graves;
    }

    public void saveGraves() throws IOException {
        if (graves.isEmpty()) return;
        for (Map.Entry<Location, Grave> unclaimedGrave : graves.entrySet()) {
            unclaimedGrave.getValue().saveGrave(grave, graveFile);
        }
    }

    public void loadGraves() {
        // Grave Loading
        ConfigurationSection graveConfig = grave.getConfigurationSection("Graves");
        if (graveConfig == null) return;
        ArrayList<Integer> removeIDs = new ArrayList<>();
        graveConfig.getKeys(false).forEach(id -> {
            int intID = Integer.parseInt(id);
            String uuidString = grave.getString("Graves." + id + ".UUID");
            if (uuidString == null) uuidString = "00000000-0000-0000-0000-000000000000";
            UUID player = UUID.fromString(uuidString);
            Location location = getLocationFromConfig(Integer.parseInt(id));
            ArrayList<ItemStack> items = new ArrayList<>();
            ConfigurationSection section = grave.getConfigurationSection("Graves." + id + ".Inventory");
            if (section == null) return;
            section.getKeys(false).forEach(slot ->
                    items.add(grave.getItemStack("Graves." + id + ".Inventory." + slot)));

            removeIDs.add(intID);
            if (location != null) {
                Grave grave = new Grave(intID, player, location, items, graveLifespan, plugin);
                graves.put(location, grave);
            }
        });

        if (!removeIDs.isEmpty()) for (int id : removeIDs) grave.set("Graves." + id, null);
    }

    public Location getLocationFromConfig(int id) {
        String locationString = grave.getString("Graves." + id + ".Location");
        if (locationString == null) return null;
        String[] locationSplit = locationString.split(":");
        String worldName = grave.getString("Graves." + id + ".World");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        double x = Double.parseDouble(locationSplit[0]);
        double y = Double.parseDouble(locationSplit[1]);
        double z = Double.parseDouble(locationSplit[2]);
        return new Location(world, x, y, z);
    }

    public void spawnFirework(Location loc, int power, FireworkEffect.Type type, Color color, boolean trail, boolean flicker, ArrayList<Color> fadeColors) {
        World world = loc.getWorld();
        if (world == null) return;
        Firework fw = (Firework) world.spawnEntity(loc, EntityType.FIREWORK_ROCKET);
        FireworkMeta fwm = fw.getFireworkMeta();

        FireworkEffect.Builder build = FireworkEffect.builder();
        build.with(type);
        build.withColor(color);
        if (trail) build.withTrail();
        if (flicker) build.withFlicker();
        if (fadeColors != null) build.withFade(fadeColors);

        fwm.addEffect(build.build());
        fwm.setPower(power);
        fw.setMetadata("nodamage", new FixedMetadataValue(plugin, true));
        fw.setFireworkMeta(fwm);
        fw.detonate();
    }
}
