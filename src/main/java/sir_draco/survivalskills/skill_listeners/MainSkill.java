package sir_draco.survivalskills.skill_listeners;

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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.abilities.DeathLocationTimer;
import sir_draco.survivalskills.abilities.Grave;
import sir_draco.survivalskills.rewards.PlayerRewards;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.utils.items.ItemModelData;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;
import sir_draco.survivalskills.SurvivalSkills;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainSkill implements Listener {

    private final SurvivalSkills plugin;
    private final Map<Player, List<Location>> deathLocations = new ConcurrentHashMap<>();
    private final Map<Location, Grave> graves = new HashMap<>();
    private final Map<Inventory, Grave> openGraves = new HashMap<>();
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

        trackDeathLocation(p);
        startDeathTimer(p);
        handleDeathInventory(e, rewards);
    }

    private void trackDeathLocation(Player player) {
        deathLocations.computeIfAbsent(player, k -> new CopyOnWriteArrayList<>()).add(player.getLocation());
    }

    private void startDeathTimer(Player player) {
        DeathLocationTimer timer = new DeathLocationTimer(plugin, player, player.getLocation(), graveLifespan);
        timer.runTaskTimerAsynchronously(plugin, 0, 20);
    }

    private void handleDeathInventory(PlayerDeathEvent e, PlayerRewards rewards) {
        if (rewards.getReward(SkillCategory.MAIN, "KeepExperience").isApplied()) {
            e.setKeepLevel(true);
            e.setDroppedExp(0);
        }
        if (Boolean.TRUE.equals(e.getEntity().getWorld().getGameRuleValue(GameRule.KEEP_INVENTORY))) return;
        if (rewards.getReward(SkillCategory.MAIN, "KeepInventory").isApplied()) {
            e.setKeepInventory(true);
            e.getDrops().clear();
            return;
        }
        if (e.getKeepInventory()) return;
        if (e.getDrops().isEmpty()) return;
        if (!rewards.getReward(SkillCategory.MAIN, "Gravestone").isApplied()) return;

        Grave grave = new Grave(nextGraveID, e.getEntity().getUniqueId(), e.getEntity().getLocation(),
                                new ArrayList<>(e.getDrops()), graveLifespan, plugin);
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
    public void placeSkillsItem(BlockPlaceEvent e) {
        if (!ItemStackGeneratorUtils.isCustomItem(e.getItemInHand())) return;
        ItemMeta meta = e.getItemInHand().getItemMeta();
        if (meta == null) return;
        if (!ItemStackGeneratorUtils.hasCustomModelData(meta, ItemModelData.DENSE_WOOL.getId())
                && !ItemStackGeneratorUtils.hasCustomModelData(meta, ItemModelData.MAGNET.getId())) return;
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
        if (!ItemStackGeneratorUtils.hasCustomModelData(meta, ItemModelData.FIREWORK_CANNON.getId())) return;
        e.setCancelled(true);
        Color color = randomColor();
        Vector vector = e.getPlayer().getLocation().getDirection();
        Location loc = e.getPlayer().getLocation().clone().add(vector.multiply(5));
        spawnFirework(loc, 1, FireworkEffect.Type.BALL, color, false, false, List.of());
    }

    public Map<Player, List<Location>> getDeathLocations() {
        return deathLocations;
    }

    public Map<Location, Grave> getGraves() {
        return graves;
    }

    public void saveGraves() throws IOException {
        if (graves.isEmpty()) return;
        for (Map.Entry<Location, Grave> unclaimedGrave : graves.entrySet()) {
            unclaimedGrave.getValue().saveGrave(grave, graveFile);
        }
    }

    public void loadGraves() {
        ConfigurationSection graveConfig = grave.getConfigurationSection("Graves");
        if (graveConfig == null) return;

        List<Integer> removeIDs = new ArrayList<>();
        for (String id : graveConfig.getKeys(false)) {
            int intID = Integer.parseInt(id);
            removeIDs.add(intID);
            loadSingleGrave(intID);
        }

        for (int id : removeIDs) {
            grave.set("Graves." + id, null);
        }
    }

    private void loadSingleGrave(int intID) {
        String uuidString = grave.getString("Graves." + intID + ".UUID");
        if (uuidString == null) {
            uuidString = "00000000-0000-0000-0000-000000000000";
        }
        UUID playerUUID = UUID.fromString(uuidString);
        List<ItemStack> items = loadGraveItems(intID);
        Optional<Location> locationOpt = getLocationFromConfig(intID);

        locationOpt.ifPresent(location -> {
            Grave grave = new Grave(intID, playerUUID, location, new ArrayList<>(items), graveLifespan, plugin);
            graves.put(location, grave);
        });
    }

    private List<ItemStack> loadGraveItems(int intID) {
        ConfigurationSection section = grave.getConfigurationSection("Graves." + intID + ".Inventory");
        if (section == null) return List.of();

        List<ItemStack> items = new ArrayList<>();
        for (String slot : section.getKeys(false)) {
            items.add(grave.getItemStack("Graves." + intID + ".Inventory." + slot));
        }
        return items;
    }

    public Optional<Location> getLocationFromConfig(int id) {
        String locationString = grave.getString("Graves." + id + ".Location");
        if (locationString == null) return Optional.empty();
        String[] locationSplit = locationString.split(":");
        String worldName = grave.getString("Graves." + id + ".World");
        if (worldName == null) return Optional.empty();
        World world = Bukkit.getWorld(worldName);
        if (world == null) return Optional.empty();
        double x = Double.parseDouble(locationSplit[0]);
        double y = Double.parseDouble(locationSplit[1]);
        double z = Double.parseDouble(locationSplit[2]);
        return Optional.of(new Location(world, x, y, z));
    }

    public void spawnFirework(Location loc, int power, FireworkEffect.Type type, Color color, boolean trail, boolean flicker, List<Color> fadeColors) {
        World world = loc.getWorld();
        if (world == null) return;
        Firework fw = (Firework) world.spawnEntity(loc, EntityType.FIREWORK_ROCKET);
        FireworkMeta fwm = fw.getFireworkMeta();

        FireworkEffect.Builder build = FireworkEffect.builder();
        build.with(type);
        build.withColor(color);
        if (trail) build.withTrail();
        if (flicker) build.withFlicker();
        if (!fadeColors.isEmpty()) build.withFade(fadeColors);

        fwm.addEffect(build.build());
        fwm.setPower(power);
        fw.setMetadata("nodamage", new FixedMetadataValue(plugin, true));
        fw.setFireworkMeta(fwm);
        fw.detonate();
    }

    private Color randomColor() {
        return Color.fromRGB((int) (Math.random() * 255),
                             (int) (Math.random() * 255),
                             (int) (Math.random() * 255));
    }
}
