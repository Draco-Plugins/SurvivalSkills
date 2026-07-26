package sir_draco.survivalskills.trophy;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.god_questline.GodTrophyEffects;
import sir_draco.survivalskills.trophy.trophy_behavior.CaveTrophyBehavior;
import sir_draco.survivalskills.trophy.trophy_behavior.ChampionTrophyBehavior;
import sir_draco.survivalskills.trophy.trophy_behavior.ColorTrophyBehavior;
import sir_draco.survivalskills.trophy.trophy_behavior.EndTrophyBehavior;
import sir_draco.survivalskills.trophy.trophy_behavior.FarmingTrophyBehavior;
import sir_draco.survivalskills.trophy.trophy_behavior.FishingTrophyBehavior;
import sir_draco.survivalskills.trophy.trophy_behavior.ForestTrophyBehavior;
import sir_draco.survivalskills.trophy.trophy_behavior.GodTrophyBehavior;
import sir_draco.survivalskills.trophy.trophy_behavior.NetherTrophyBehavior;
import sir_draco.survivalskills.trophy.trophy_behavior.OceanTrophyBehavior;
import sir_draco.survivalskills.trophy.trophy_behavior.TrophyEffectBehavior;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

public class TrophyEffects extends BukkitRunnable {

    public static final String TROPHY_ITEM = "TrophyItem";
    public static final String NO_PICKUP = "00000000-0000-0000-0000-000000000000";

    private static final String TROPHY_EFFECT_ID_KEY = "trophy_effect_id";
    private static final String TROPHY_EFFECT_ITEM_VALUE = "Trophy";
    private static final NamespacedKey DISPLAY_ITEM_ENCHANTMENT_KEY = NamespacedKey.minecraft("knockback");
    private static final double DISPLAY_ITEM_CLEANUP_RADIUS = 0.5;
    private static final double CHAMPION_CLEANUP_HORIZONTAL_RADIUS = 1.5;
    private static final double CHAMPION_CLEANUP_VERTICAL_RADIUS = 1.0;

    private final SurvivalSkills plugin;
    private final Location loc;
    private final TrophyType type;
    private final Trophy trophy;
    private final TrophyEffectBehavior behavior;
    private final NamespacedKey trophyEffectIdKey;

    private Item displayItem;
    private int cycle = 1;
    private int playerCheckTimer = 1;
    private boolean run = true;

    // God trophy specific state
    private String playerName;
    private UUID playerUUID;
    private boolean citizensEnabled = false;
    private GodTrophyEffects godTrophy;

    public TrophyEffects(SurvivalSkills plugin, Location loc, TrophyType type, Trophy trophy,
                         String playerName, UUID playerUUID) {
        this.plugin = plugin;
        this.loc = loc;
        this.type = type;
        this.trophy = trophy;
        this.playerName = playerName;
        this.playerUUID = playerUUID;
        this.behavior = createBehavior(type);
        this.trophyEffectIdKey = new NamespacedKey(plugin, TROPHY_EFFECT_ID_KEY);
    }

    public void init(boolean freshPlacement) {
        if (type == TrophyType.GOD) {
            if (plugin.getServer().getPluginManager().getPlugin("Citizens") == null) {
                plugin.getLogger().warning("Citizens not found, disabling God Trophy");
                return;
            }
            if (plugin.getServer().getPluginManager().isPluginEnabled("Citizens")) {
                citizensEnabled = true;
            }
            if (!freshPlacement) {
                this.cycle = 122;
            }
            return;
        }

        World world = loc.getWorld();
        if (world != null && type == TrophyType.CHAMPION) {
            removePersistedChampionItems(world);
        }
        spawnItem(0.5, 1.0, 0.5);
        if (world != null && behavior != null) {
            behavior.start(this, loc, world, plugin);
        }
    }

    @Override
    public void run() {
        if (playerCheckTimer == 50) {
            checkForPlayers();
            playerCheckTimer = 1;
        }
        if (!run) {
            playerCheckTimer++;
            return;
        }

        World world = loc.getWorld();
        if (world == null) {
            playerCheckTimer++;
            return;
        }

        if (behavior != null) {
            cycle = behavior.tick(this, cycle, world, loc);
        }
        cycle++;
        playerCheckTimer++;
    }

    public void spawnItem(double x, double y, double z) {
        if (displayItem != null) {
            displayItem.remove();
            displayItem = null;
        }

        Material mat = type.getMaterial();
        checkForDuplicate(mat);
        ItemStack trophyItem = new ItemStack(mat);
        trophyItem.addUnsafeEnchantment(Enchantment.KNOCKBACK, 5);
        trophyItem = addPersistentDataContainer(trophyItem);

        World world = loc.getWorld();
        if (world == null) {
            return;
        }
        Location newLoc = loc.clone().add(x, y, z);
        displayItem = (Item) world.spawnEntity(newLoc, EntityType.ITEM);
        displayItem.setItemStack(trophyItem);
        setFloatingItemProperties(displayItem);
    }

    public ItemStack addPersistentDataContainer(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.getPersistentDataContainer().set(ItemStackGeneratorUtils.trophyItemKey,
                PersistentDataType.STRING, "Trophy");
        item.setItemMeta(meta);
        return item;
    }

    public void checkForDuplicate(Material mat) {
        World world = loc.getWorld();
        if (world == null) {
            return;
        }
        Location displayLocation = loc.clone().add(0.5, 1.0, 0.5);
        for (Entity ent : world.getNearbyEntities(displayLocation, DISPLAY_ITEM_CLEANUP_RADIUS,
                DISPLAY_ITEM_CLEANUP_RADIUS, DISPLAY_ITEM_CLEANUP_RADIUS)) {
            removeDuplicateDisplayItem(mat, ent);
        }
    }

    private void removeDuplicateDisplayItem(Material mat, Entity ent) {
        if (!(ent instanceof Item item)) {
            return;
        }
        ItemStack itemStack = item.getItemStack();
        if (!itemStack.getType().equals(mat) || !hasDisplayItemEnchantment(itemStack)) {
            return;
        }
        item.remove();
    }

    @SuppressWarnings("deprecation")
    private static boolean hasDisplayItemEnchantment(ItemStack itemStack) {
        return itemStack.getEnchantments().keySet().stream()
                .anyMatch((Enchantment enchantment) -> DISPLAY_ITEM_ENCHANTMENT_KEY.equals(enchantment.getKey()));
    }

    public void setRun(boolean go) {
        run = go;
    }

    public void checkForPlayers() {
        if (plugin.getServer().getOnlinePlayers().isEmpty() && run) {
            trophy.pauseTrophy();
        }
        if (plugin.getServer().getOnlinePlayers().isEmpty() && !run) {
            return;
        }
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (!p.getWorld().equals(loc.getWorld()) || p.getLocation().distance(loc) > 50) {
                continue;
            }
            if (!run) {
                trophy.resumeTrophy();
            }
            return;
        }
        if (run) {
            trophy.pauseTrophy();
        }
    }

    public void removeItem() {
        if (displayItem != null) {
            displayItem.remove();
            displayItem = null;
        }
        if (godTrophy != null) {
            godTrophy.remove();
        }
        if (behavior != null) {
            behavior.cleanup();
        }
    }

    public void behaviorStart() {
        World world = loc.getWorld();
        if (world != null && behavior != null) {
            behavior.start(this, loc, world, plugin);
        }
    }

    // ---- Shared utility methods used by behaviors ----

    public void floorParticles(Particle particle) {
        edgeParticlesOnAxis(particle, 0, 0, true, 1.0, 0.25);  // Left (z=0, x varies)
        edgeParticlesOnAxis(particle, 0, 1, true, 1.0, 0.25);  // Right (z=1, x varies)
        edgeParticlesOnAxis(particle, 0, 0, false, 1.0, 0.25); // Front (x=0, z varies)
        edgeParticlesOnAxis(particle, 1, 0, false, 1.0, 0.25); // Back (x=1, z varies)
    }

    private void edgeParticlesOnAxis(Particle particle, double fixedX, double fixedZ,
                                      boolean varyX, double length, double step) {
        World world = loc.getWorld();
        if (world == null) {
            return;
        }

        for (double d = 0.0; d <= length; d += step) {
            double x = varyX ? d : fixedX;
            double z = varyX ? fixedZ : d;
            for (double y = 0.0; y <= step; y += step) {
                Location next = loc.clone().add(x, y, z);
                world.spawnParticle(particle, next, 0, 0., 0., 0.);
            }
        }
    }

    public void drawCube(Location location, Color color, double length, double step) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }

        var dust = new Particle.DustOptions(color, 1f);

        double[][] faces = {
                { 0, length, 0, 0, 0, length },       // Bottom (Y=0)
                { 0, length, length, length, 0, length }, // Top (Y=length)
                { 0, 0, 0, length, 0, length },           // Front (X=0)
                { length, length, 0, length, 0, length }, // Back (X=length)
                { 0, length, 0, length, 0, 0 },           // Left (Z=0)
                { 0, length, 0, length, length, length }  // Right (Z=length)
        };

        for (double[] face : faces) {
            drawCubeFace(world, location, dust, step, face);
        }
    }

    private void drawCubeFace(World world, Location baseLocation, Particle.DustOptions dust,
                               double step, double[] coords) {
        double x1 = coords[0];
        double x2 = coords[1];
        double y1 = coords[2];
        double y2 = coords[3];
        double z1 = coords[4];
        double z2 = coords[5];

        for (double x = x1; x <= x2; x += (x1 == x2) ? 1 : step) {
            for (double y = y1; y <= y2; y += (y1 == y2) ? 1 : step) {
                for (double z = z1; z <= z2; z += (z1 == z2) ? 1 : step) {
                    var particleLocation = new Location(world,
                            baseLocation.getX() + x,
                            baseLocation.getY() + y,
                            baseLocation.getZ() + z);
                    world.spawnParticle(Particle.DUST, particleLocation, 1, dust);
                }
            }
        }
    }

    public void spawnFireworkEffect(Location loc, Color color, double radius, double step) {
        World world = loc.getWorld();
        if (world == null) {
            return;
        }
        Particle.DustOptions dust = new Particle.DustOptions(color, 1f);
        for (double x = -radius; x <= radius; x += step) {
            for (double y = -radius; y <= radius; y += step) {
                for (double z = -radius; z <= radius; z += step) {
                    double distance = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
                    if (distance > radius || distance < radius - step) {
                        continue;
                    }
                    Location newLoc = loc.clone().add(x, y, z);
                    world.spawnParticle(Particle.DUST, newLoc, 1, 1, 1, 1, dust);
                }
            }
        }
    }

    public Item spawnDecorativeItem(ItemStack item, ArrayList<Item> targetList, int maxItems) {
        World world = loc.getWorld();
        if (world == null) {
            return null;
        }

        if (targetList.size() > maxItems) {
            targetList.getFirst().remove();
            targetList.removeFirst();
        }

        Location newLoc = loc.clone().add(0.5, 1.0, 0.5);
        Item entity = (Item) world.spawnEntity(newLoc, EntityType.ITEM);
        entity.setItemStack(item);
        double directionX = (Math.random() - 0.5) * 0.25;
        double directionY = (Math.random() * 0.5) + 0.1;
        double directionZ = (Math.random() - 0.5) * 0.25;
        entity.setVelocity(new Vector(directionX, directionY, directionZ));
        entity.setOwner(java.util.UUID.fromString(NO_PICKUP));
        targetList.add(entity);
        return entity;
    }

    public void setFloatingItemProperties(Item item) {
        item.setGravity(false);
        item.setUnlimitedLifetime(true);
        item.setVelocity(new Vector(0, 0, 0));
        item.setOwner(java.util.UUID.fromString(NO_PICKUP));
        item.setMetadata(TROPHY_ITEM, new FixedMetadataValue(plugin, true));
        item.getPersistentDataContainer().set(trophyEffectIdKey, PersistentDataType.INTEGER, trophy.getID());
    }

    private void removePersistedChampionItems(World world) {
        loc.getChunk().load();
        Location center = loc.clone().add(0.5, 1.0, 0.5);
        for (Entity entity : world.getNearbyEntities(center, CHAMPION_CLEANUP_HORIZONTAL_RADIUS,
                CHAMPION_CLEANUP_VERTICAL_RADIUS, CHAMPION_CLEANUP_HORIZONTAL_RADIUS)) {
            if (entity instanceof Item item && isPersistedEffectItemForThisTrophy(item)) {
                item.remove();
            }
        }
    }

    private boolean isPersistedEffectItemForThisTrophy(Item item) {
        Integer effectTrophyId = item.getPersistentDataContainer().get(trophyEffectIdKey,
                PersistentDataType.INTEGER);
        if (effectTrophyId != null) {
            return Objects.equals(effectTrophyId, trophy.getID());
        }

        ItemMeta itemMeta = item.getItemStack().getItemMeta();
        if (itemMeta == null) {
            return false;
        }
        String itemType = itemMeta.getPersistentDataContainer().get(ItemStackGeneratorUtils.trophyItemKey,
                PersistentDataType.STRING);
        return Objects.equals(itemType, TROPHY_EFFECT_ITEM_VALUE);
    }

    // ---- Accessors for behaviors ----

    public Item getDisplayItem() {
        return displayItem;
    }

    public SurvivalSkills getPlugin() {
        return plugin;
    }

    public boolean isCitizensEnabled() {
        return citizensEnabled;
    }

    public String getPlayerName() {
        return playerName;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public void setGodTrophy(GodTrophyEffects godTrophy) {
        this.godTrophy = godTrophy;
    }

    // ---- External accessors ----

    public GodTrophyEffects getGodTrophy() {
        return godTrophy;
    }

    public TrophyType getType() {
        return type;
    }

    public int getTrophyId() {
        return trophy.getID();
    }

    // ---- Behavior factory ----

    private static TrophyEffectBehavior createBehavior(TrophyType type) {
        return switch (type) {
            case CAVE -> new CaveTrophyBehavior();
            case FOREST -> new ForestTrophyBehavior();
            case FARMING -> new FarmingTrophyBehavior();
            case OCEAN -> new OceanTrophyBehavior();
            case FISHING -> new FishingTrophyBehavior();
            case COLOR -> new ColorTrophyBehavior();
            case NETHER -> new NetherTrophyBehavior();
            case END -> new EndTrophyBehavior();
            case CHAMPION -> new ChampionTrophyBehavior();
            case GOD -> new GodTrophyBehavior();
        };
    }
}
