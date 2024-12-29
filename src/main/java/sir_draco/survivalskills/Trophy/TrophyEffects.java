package sir_draco.survivalskills.Trophy;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.GodQuestline.GodTrophyEffects;
import sir_draco.survivalskills.Utils.ColorParser;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.Utils.ItemStackGenerator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TrophyEffects extends BukkitRunnable {

    private final SurvivalSkills plugin;
    private final Location loc;
    private final int type;
    private final ArrayList<Item> itemList = new ArrayList<>();
    private final ArrayList<Color> colorList = new ArrayList<>();
    private final Trophy trophy;

    private int cycle = 1;
    private int playerCheckTimer = 1;
    private Item entity;
    private Entity mob;
    private CircularRotationObject mobTrophyOrbital = null;
    private boolean run = true;
    private boolean citizensEnabled = false;
    private GodTrophyEffects godTrophy;
    private String playerName;
    private UUID playerUUID;

    public TrophyEffects(SurvivalSkills plugin, Location loc, int type, Trophy trophy, String playerName, UUID playerUUID) {
        this.plugin = plugin;
        this.loc = loc;
        this.type = type;
        this.trophy = trophy;
        if (type == 10) {
            this.playerName = playerName;
            this.playerUUID = playerUUID;
            if (plugin.getServer().getPluginManager().getPlugin("Citizens") == null) {
                plugin.getLogger().warning("Citizens not found, disabling God Trophy");
                return;
            }
            if (plugin.getServer().getPluginManager().isPluginEnabled("Citizens")) citizensEnabled = true;
            return;
        }
        spawnItem(0.5, 1.0, 0.5);
        typeSpecificStart();
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

        switch (type) {
            case 1:
                caveParticles();
                break;
            case 2:
                forestParticles();
                break;
            case 3:
                farmingParticles();
                break;
            case 4:
                oceanParticles();
                break;
            case 5:
                fishingParticles();
                break;
            case 6:
                colorParticles();
                break;
            case 7:
                netherParticles();
                break;
            case 8:
                endParticles();
                break;
            case 9:
                championParticles();
                break;
            case 10:
                try {
                    godParticles();
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
                break;
            default:
                return;
        }
        cycle++;
        playerCheckTimer++;
    }

    public void spawnItem(double x, double y, double z) {
        if (entity != null) {
            entity.remove();
            entity = null;
        }

        // Create the item
        Material mat = getMaterial();
        checkForDuplicate(mat);
        ItemStack trophy = new ItemStack(mat);
        trophy.addUnsafeEnchantment(Enchantment.KNOCKBACK, 5);
        trophy = addPersistentDataContainer(trophy);
        // Spawn the item - ensure no one can pick it up, it won't de-spawn, and it floats in the air
        World world = loc.getWorld();
        if (world == null) return;
        Location newLoc = loc.clone().add(x, y, z);
        entity = (Item) world.spawnEntity(newLoc, EntityType.ITEM);
        entity.setItemStack(trophy);
        setFloatingItemProperties(entity);
    }

    public ItemStack addPersistentDataContainer(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.getPersistentDataContainer().set(ItemStackGenerator.skillsItemKey, PersistentDataType.STRING, "Trophy");
        item.setItemMeta(meta);
        return item;
    }

    public void checkForDuplicate(Material mat) {
        World world = Bukkit.getWorld("world");
        if (world != null) {
            for (Entity ent : world.getEntities()) {
                if (!ent.getType().equals(EntityType.ITEM)) continue;
                Item item = (Item) ent;
                if (item.getOwner() == null) continue;
                if (!item.getItemStack().getType().equals(mat)) continue;
                if (item.getLocation().getWorld() == null) continue;
                if (loc.getWorld() == null) continue;
                if (!item.getLocation().getWorld().equals(loc.getWorld())) continue;
                if (item.getLocation().distance(loc) > 5) continue;
                if (item.getOwner().equals(UUID.fromString("00000000-0000-0000-0000-000000000000"))) item.remove();
            }
        }
    }

    public void setRun(boolean go) {
        run = go;
    }

    public void checkForPlayers() {
        // If no players are online, stop the trophy effects
        if (plugin.getServer().getOnlinePlayers().isEmpty() && run) trophy.restartTrophy(false);
        // If no players are online and the trophy is already stopped, return
        if (plugin.getServer().getOnlinePlayers().isEmpty() && !run) return;
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            // If a player is within 50 blocks of the trophy, and the trophy is stopped, restart it
            if (!p.getWorld().equals(loc.getWorld())) continue;
            if (p.getLocation().distance(loc) > 50) continue;
            if (!run) trophy.restartTrophy(true);
            // If the player is withing 50 blocks and the trophy is running, do nothing
            return;
        }
        // If no players are within 50 blocks of the trophy, and the trophy is running, stop it
        if (run) trophy.restartTrophy(false);
    }

    private Material getMaterial() {
        return switch (type) {
            case 1 -> Material.DIAMOND_PICKAXE;
            case 2 -> Material.OAK_SAPLING;
            case 3 -> Material.GOLDEN_CARROT;
            case 4 -> Material.TRIDENT;
            case 5 -> Material.FISHING_ROD;
            case 6 -> Material.SHEARS;
            case 7 -> Material.NETHERRACK;
            case 8 -> Material.END_STONE;
            case 9 -> Material.DIAMOND_SWORD;
            case 10 -> Material.GRASS_BLOCK;
            default -> Material.AIR;
        };
    }

    public void removeItem() {
        if (entity != null) {
            entity.remove();
            entity = null;
        }
        if (mob != null) {
            mob.remove();
            mob = null;
        }
        if (godTrophy != null) godTrophy.remove();
        if (itemList.isEmpty()) return;
        for (Item item : itemList) item.remove();
        itemList.clear();
    }

    public void typeSpecificStart() {
        if (type == 6) createColorList();
        else if (type == 9) {
            mobTrophyOrbital = new CircularRotationObject(loc, 1, 7);
            populateItemList();
            resetLocation();
        }
    }

    public void floorParticles(Particle particle) {
        World world = loc.getWorld();
        if (world == null) return;
        double length = 1.0;
        double step = 0.25;
        // Left
        for (double x = 0.0; x <= length; x += step) {
            for (double y = 0.0; y <= step; y += step) {
                Location next = loc.clone().add(x, y, 0.);
                world.spawnParticle(particle, next, 0, 0., 0., 0.);
            }
        }
        // Right
        for (double x = 0.0; x <= length; x += step) {
            for (double y = 0.0; y <= step; y += step) {
                Location next = loc.clone().add(x, y, 1.);
                world.spawnParticle(particle, next, 0, 0., 0., 0.);
            }
        }
        // Front
        for (double y = 0.0; y <= step; y += step) {
            for (double z = 0.0; z <= length; z += step) {
                Location next = loc.clone().add(0., y, z);
                world.spawnParticle(particle, next, 0, 0., 0., 0.);
            }
        }
        // Back
        for (double y = 0.0; y <= step; y += step) {
            for (double z = 0.0; z <= length; z += step) {
                Location next = loc.clone().add(1., y, z);
                world.spawnParticle(particle, next, 0, 0., 0., 0.);
            }
        }
    }

    public void caveParticles() {
        double xOffset = 0.25 + ((Math.random() - 0.5) * 2.0);
        double yOffset = Math.random() + 1.5;
        double zOffset = 0.25 + ((Math.random() - 0.5) * 2.0);
        if (cycle == 10) {
            Location location = new Location(loc.getWorld(), loc.getX() + xOffset, loc.getY() + yOffset, loc.getZ() + zOffset);
            drawCube(location, randomCaveColor(), 0.5, 0.1);
        }
        if (cycle % 3 == 0) floorParticles(Particle.WITCH);
        if (cycle == 10) cycle = 1;
    }

    public Color randomCaveColor() {
        Color color;
        int type = (int) Math.ceil(Math.random() * 9);
        color = switch (type) {
            case 1 -> Color.GRAY;
            case 2 -> Color.RED;
            case 3 -> Color.BLACK;
            case 4 -> Color.GREEN;
            case 5 -> Color.BLUE;
            case 6 -> Color.WHITE;
            case 7 -> Color.fromRGB(156, 120, 2);
            case 8 -> Color.fromRGB(255, 255, 0);
            default -> Color.fromRGB(0, 255, 255);
        };
        return color;
    }

    public void drawCube(Location location, Color color, double length, double step) {
        World world = location.getWorld();
        if (world == null) return;
        // Top
        Particle.DustOptions dust = new Particle.DustOptions(color, 1f);
        for (double x = 0.0; x <= length; x += step) {
            for (double z = 0.0; z <= length; z += step) {
                Location next = new Location(world, location.getX() + x, location.getY() + length, location.getZ() + z);
                world.spawnParticle(Particle.DUST, next, 1, dust);
            }
        }
        // Bottom
        for (double x = 0.0; x <= length; x += step) {
            for (double z = 0.0; z <= length; z += step) {
                Location next = new Location(world, location.getX() + x, location.getY(), location.getZ() + z);
                world.spawnParticle(Particle.DUST, next, 1, dust);
            }
        }
        // Left
        for (double x = 0.0; x <= length; x += step) {
            for (double y = 0.0; y <= length; y += step) {
                Location next = new Location(world, location.getX() + x, location.getY() + y, location.getZ());
                world.spawnParticle(Particle.DUST, next, 1, dust);
            }
        }
        // Right
        for (double x = 0.0; x <= length; x += step) {
            for (double y = 0.0; y <= length; y += step) {
                Location next = new Location(world, location.getX() + x, location.getY() + y, location.getZ() + length);
                world.spawnParticle(Particle.DUST, next, 1, dust);
            }
        }
        // Front
        for (double y = 0.0; y <= length; y += step) {
            for (double z = 0.0; z <= length; z += step) {
                Location next = new Location(world, location.getX(), location.getY() + y, location.getZ() + z);
                world.spawnParticle(Particle.DUST, next, 1, dust);
            }
        }
        // Back
        for (double y = 0.0; y <= length; y += step) {
            for (double z = 0.0; z <= length; z += step) {
                Location next = new Location(world, location.getX() + length, location.getY() + y, location.getZ() + z);
                world.spawnParticle(Particle.DUST, next, 1, dust);
            }
        }
    }

    public void forestParticles() {
        World world = loc.getWorld();
        if (world == null) return;
        if (cycle % 3 == 0) floorParticles(Particle.WITCH);
        ItemStack sapling = switch (cycle) {
            case 5 -> addPersistentDataContainer(new ItemStack(Material.OAK_SAPLING));
            case 10 -> addPersistentDataContainer(new ItemStack(Material.SPRUCE_SAPLING));
            case 15 -> addPersistentDataContainer(new ItemStack(Material.ACACIA_SAPLING));
            case 20 -> addPersistentDataContainer(new ItemStack(Material.BIRCH_SAPLING));
            case 25 -> addPersistentDataContainer(new ItemStack(Material.CHERRY_SAPLING));
            case 30 -> addPersistentDataContainer(new ItemStack(Material.JUNGLE_SAPLING));
            case 35 -> addPersistentDataContainer(new ItemStack(Material.MANGROVE_PROPAGULE));
            case 40 -> addPersistentDataContainer(new ItemStack(Material.DARK_OAK_SAPLING));
            default -> new ItemStack(Material.AIR);
        };

        if (cycle % 5 != 0) return;
        if (cycle == 40) cycle = 1;
        if (itemList.size() > 4) {
            itemList.getFirst().remove();
            itemList.removeFirst();
        }
        Location newLoc = loc.clone().add(0.5, 1.0, 0.5);
        Item saplingEnt = (Item) world.spawnEntity(newLoc, EntityType.ITEM);
        saplingEnt.setItemStack(sapling);
        double directionX = (Math.random() - 0.5) * 0.25;
        double directionY = (Math.random() * 0.5) + 0.1;
        double directionZ = (Math.random() - 0.5) * 0.25;
        saplingEnt.setVelocity(new Vector(directionX, directionY, directionZ));
        saplingEnt.setOwner(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        itemList.add(saplingEnt);
    }

    public void farmingParticles() {
        World world = loc.getWorld();
        if (world == null) return;
        if (cycle == 3) {
            floorParticles(Particle.HAPPY_VILLAGER);
            rainfall();
            cycle = 1;
        }
    }

    public void rainfall() {
        World world = loc.getWorld();
        if (world == null) return;
        Location location = loc.clone().add(0., 2.0, 0.);
        for (double x = 0.0; x <= 1.0; x += 0.2) {
            for (double z = 0.0; z <= 1.0; z += 0.2) {
                Location finalLoc = location.clone().add(x, 0., z);
                world.spawnParticle(Particle.DRIPPING_WATER, finalLoc, 1, 0.0, -0.1, 0.0);
                finalLoc.add(0., 0.25, 0.);
                world.spawnParticle(Particle.CLOUD, finalLoc, 0, 0.0, 0.0, 0.0);
            }
        }
    }

    public void oceanParticles() {
        World world = loc.getWorld();
        if (world == null) return;
        if (cycle % 3 == 0) {
            floorParticles(Particle.SPLASH);
            bubbles();
        }
        if (cycle == 10) {
            for (int i = 1; i <= 5; i++) {
                double xOff = (Math.random() - 0.5) * 0.5;
                double yOff = (Math.random() - 0.5) * 0.5;
                double zOff = (Math.random() - 0.5) * 0.5;
                Location newLoc = loc.clone().add(0.5, 1.0, 0.5);
                world.spawnParticle(Particle.CRIT, newLoc, 1, xOff, yOff, zOff);
            }
            cycle = 1;
        }
    }

    public void bubbles() {
        World world = loc.getWorld();
        if (world == null) return;
        for (int i = 1; i <= 30; i++) {
            double xOff = Math.random();
            double yOff = Math.random() * 3.0;
            double zOff = Math.random();
            Location newLoc = loc.clone().add(xOff, yOff, zOff);
            world.spawnParticle(Particle.BUBBLE_COLUMN_UP, newLoc, 1);
        }
    }

    public void fishingParticles() {
        World world = loc.getWorld();
        if (world == null) return;
        if (cycle % 3 == 0) floorParticles(Particle.WITCH);
        ItemStack sapling = switch (cycle) {
            case 5 -> addPersistentDataContainer(new ItemStack(Material.COD));
            case 10 -> addPersistentDataContainer(new ItemStack(Material.SALMON));
            case 15 -> addPersistentDataContainer(new ItemStack(Material.PUFFERFISH));
            case 20 -> addPersistentDataContainer(new ItemStack(Material.TROPICAL_FISH));
            default -> addPersistentDataContainer(new ItemStack(Material.AIR));
        };

        if (cycle % 5 != 0) return;
        if (cycle == 20) {
            cycle = 1;
            double chance = Math.random();
            if (mob != null && chance < 0.2) {
                mob.remove();
                mob = null;
            }
            else if (mob == null && chance < 0.2) {
                mob = world.spawnEntity(loc.clone().add(0.5, 1.0, 0.5), EntityType.SQUID);
                Squid squid = (Squid) mob;
                squid.setPersistent(true);
                squid.setGravity(false);
                squid.setVelocity(new Vector((Math.random() - 0.5) * 0.5, Math.random() * 0.25, (Math.random() - 0.5) * 0.5));
                squid.setCollidable(false);
                squid.setInvulnerable(true);
            }
        }
        if (itemList.size() > 8) {
            itemList.getFirst().remove();
            itemList.removeFirst();
        }
        Location newLoc = loc.clone().add(0.5, 1.0, 0.5);
        Item saplingEnt = (Item) world.spawnEntity(newLoc, EntityType.ITEM);
        saplingEnt.setItemStack(sapling);
        double directionX = (Math.random() - 0.5) * 0.25;
        double directionY = (Math.random() * 0.5) + 0.1;
        double directionZ = (Math.random() - 0.5) * 0.25;
        saplingEnt.setVelocity(new Vector(directionX, directionY, directionZ));
        saplingEnt.setOwner(UUID.fromString("00000000-0000-0000-0000-000000000000"));
        itemList.add(saplingEnt);
    }

    public void colorParticles() {
        World world = loc.getWorld();
        if (world == null) return;
        floorColor(cycle);
        if (cycle == 8 || cycle == 16) {
            double chance = Math.random();
            if (chance < 0.5) {
                Location newLoc = loc.clone().add(0.5 + (Math.random() - 0.5), 2.0 + (Math.random()), 0.5 + (Math.random() - 0.5));
                Color color = colorList.get((int) Math.floor(Math.random() * colorList.size()));
                spawnFireworkEffect(newLoc, color, 1.5, 0.3);
            }
            if (cycle == 16) cycle = 0;
        }
    }

    public void floorColor(int column) {
        World world = loc.getWorld();
        if (world == null) return;
        if (column <= 4) {
            Particle.DustOptions dust = new Particle.DustOptions(colorList.get(column), 1f);
            Location newLoc = loc.clone().add(column / 4.0, 0.0, 0.0);
            new ParticleColumnThread(newLoc, dust, 1.0, 0.2).runTaskTimer(plugin, 0, 2);
        }
        else if (column <= 8) {
            Particle.DustOptions dust = new Particle.DustOptions(colorList.get(column), 1f);
            Location newLoc = loc.clone().add(1.0, 0.0, (column - 4) / 4.0);
            new ParticleColumnThread(newLoc, dust, 1.0, 0.2).runTaskTimer(plugin, 0, 2);
        }
        else if (column <= 12) {
            Particle.DustOptions dust = new Particle.DustOptions(colorList.get(column), 1f);
            Location newLoc = loc.clone().add(1.0 + (-1 * (column - 8) / 4.0), 0.0, 1.0);
            new ParticleColumnThread(newLoc, dust, 1.0, 0.2).runTaskTimer(plugin, 0, 2);
        }
        else {
            Particle.DustOptions dust = new Particle.DustOptions(colorList.get(column), 1f);
            Location newLoc = loc.clone().add(0.0, 0.0, 1.0 + (-1 * (column - 12) / 4.0));
            new ParticleColumnThread(newLoc, dust, 1.0, 0.2).runTaskTimer(plugin, 0, 2);
        }
    }

    public void createColorList() {
        if (!colorList.isEmpty()) colorList.clear();
        List<List<String>> colors = new ArrayList<>();
        colors.add(ColorParser.generateGradient("#FF0000", "#00FF00", 6));
        colors.add(ColorParser.generateGradient("#00FF00", "#0000FF", 6));
        colors.add(ColorParser.generateGradient("#0000FF", "#FF00FF", 5));
        List<String> hexColors = ColorParser.gradientConnector(colors);
        for (String hex : hexColors) colorList.add(ColorParser.hexToColor(hex));
    }

    public void spawnFireworkEffect(Location loc, Color color, double radius, double step) {
        // Spawn a sphere of dust particles of the color of the firework
        World world = loc.getWorld();
        if (world == null) return;
        Particle.DustOptions dust = new Particle.DustOptions(color, 1f);
        for (double x = -radius; x <= radius; x += step) {
            for (double y = -radius; y <= radius; y += step) {
                for (double z = -radius; z <= radius; z += step) {
                    double distance = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
                    if (distance > radius || distance < radius - step) continue;
                    Location newLoc = loc.clone().add(x, y, z);
                    world.spawnParticle(Particle.DUST, newLoc, 1, 1, 1, 1, dust);
                }
            }
        }
    }

    public void netherParticles() {
        World world = loc.getWorld();
        if (world == null) return;
        if (cycle == 3) {
            floorParticles(Particle.FLAME);
            world.spawnParticle(Particle.LAVA, loc.clone().add(0.5, 0.0, 0.5), 3);
            cycle = 1;
        }
    }

    public void endParticles() {
        World world = loc.getWorld();
        if (world == null) return;
        if (mob != null) {
            mob.setRotation((float) (Math.random() * 360), (float) (Math.random() * 180));
        }
        if (cycle % 3 == 0) {
            floorParticles(Particle.WITCH);
            Location tp = loc.clone().add(Math.random(), 1.0 + (Math.random() - 0.5), Math.random());
            entity.teleport(tp);
        }

        if (cycle == 18) {
            cycle = 1;
            double chance = Math.random();
            if (mob != null && chance < 0.4) {
                mob.remove();
                mob = null;
            }
            else if (mob == null && chance < 0.1) {
                mob = world.spawnEntity(loc.clone().add(0.5, 1.0, 0.5), EntityType.ENDERMAN);
                Enderman eman = (Enderman) mob;
                eman.setPersistent(true);
                eman.setGravity(false);
                eman.setVelocity(new Vector((Math.random() - 0.5) * 0.5, 0.1, (Math.random() - 0.5) * 0.5));
                eman.setCollidable(false);
                eman.setInvulnerable(true);
                eman.setAI(false);
            }
        }
    }

    public void championParticles() {
        if (cycle == 360) cycle = 1;
        moveChampionItems();
    }

    public void resetLocation() {
        mobTrophyOrbital.createLocations(mobTrophyOrbital.getAngle(itemList.getFirst().getLocation()) + 0.01);
        for (int i = 0; i < itemList.size(); i++) {
            Item item = itemList.get(i);
            item.teleport(mobTrophyOrbital.getLocation(i));
        }
    }

    public void moveChampionItems() {
        for (Item item : itemList) {
            item.setVelocity(mobTrophyOrbital.getVelocityVector(item.getLocation(), 0.04));
            if (mobTrophyOrbital.tooFar(item.getLocation())) {
                resetLocation();
                break;
            }
        }
    }

    public void populateItemList() {
        World world = loc.getWorld();
        if (world == null) return;
        Item giant = (Item) world.spawnEntity(loc, EntityType.ITEM);
        giant.setItemStack(addPersistentDataContainer(new ItemStack(Material.ZOMBIE_HEAD)));
        setFloatingItemProperties(giant);
        itemList.add(giant);

        Item guardian = (Item) world.spawnEntity(loc, EntityType.ITEM);
        guardian.setItemStack(addPersistentDataContainer(new ItemStack(Material.ENDER_EYE)));
        setFloatingItemProperties(guardian);
        itemList.add(guardian);

        Item wither = (Item) world.spawnEntity(loc, EntityType.ITEM);
        wither.setItemStack(addPersistentDataContainer(new ItemStack(Material.NETHER_STAR)));
        setFloatingItemProperties(wither);
        itemList.add(wither);

        Item warden = (Item) world.spawnEntity(loc, EntityType.ITEM);
        warden.setItemStack(addPersistentDataContainer(new ItemStack(Material.ECHO_SHARD)));
        setFloatingItemProperties(warden);
        itemList.add(warden);

        Item dragon = (Item) world.spawnEntity(loc, EntityType.ITEM);
        dragon.setItemStack(addPersistentDataContainer(new ItemStack(Material.DRAGON_HEAD)));
        setFloatingItemProperties(dragon);
        itemList.add(dragon);

        Item brood = (Item) world.spawnEntity(loc, EntityType.ITEM);
        brood.setItemStack(addPersistentDataContainer(new ItemStack(Material.COBWEB)));
        setFloatingItemProperties(brood);
        itemList.add(brood);

        Item villager = (Item) world.spawnEntity(loc, EntityType.ITEM);
        villager.setItemStack(addPersistentDataContainer(new ItemStack(Material.PLAYER_HEAD)));
        setFloatingItemProperties(villager);
        itemList.add(villager);
    }

    public void setFloatingItemProperties(Item item) {
        item.setGravity(false);
        item.setUnlimitedLifetime(true);
        item.setVelocity(new Vector(0, 0, 0));
        item.setOwner(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    public void godParticles() throws IOException, InterruptedException {
        if (!citizensEnabled) return;
        if (cycle == 1) godTrophy = new GodTrophyEffects(loc);

        if (cycle < 122) {
            // Spawn grass block item and send it to the sky
            switch (cycle) {
                case 1:
                    godTrophy.startAnimation();
                    break;
                case 5:
                    godTrophy.scaleDisplay(0.3f);
                    break;
                case 6:
                    godTrophy.scaleDisplay(0.4f);
                    break;
                case 7:
                    godTrophy.scaleDisplay(0.5f);
                    break;
                case 8:
                    godTrophy.scaleDisplay(0.6f);
                    break;
                case 9:
                    godTrophy.scaleDisplay(0.7f);
                    godTrophy.playSound(Sound.BLOCK_NOTE_BLOCK_CHIME);
                    break;
            }

            if (cycle >= 18 && cycle < 60) {
                godTrophy.rotateDisplay(0, 0.05f, 0);
                godTrophy.floatingEffect(cycle);
            }
            else if (cycle == 60) {
                godTrophy.spawnBlackHole();
                godTrophy.playSound(Sound.ENTITY_WITHER_SPAWN);
            }
            else if (cycle > 60 && cycle < 100) {
                godTrophy.rotateDisplay(0, 0.05f, 0);
                godTrophy.teleport(0, -0.05, 0);
            }

            if (cycle == 100) godTrophy.removeDisplay();

            if (cycle >= 100 && cycle < 105) godTrophy.changeBlackHoleSize(0.20f);
            else if (cycle == 105) {
                godTrophy.playSound(Sound.ENTITY_ENDERMAN_TELEPORT);
                godTrophy.removeBlackHole();
            }
            // Have lightning crash down spawning player NPC and explosion particle effects
            else if (cycle == 120) {
                if (loc.getWorld() == null) return;
                loc.getWorld().strikeLightningEffect(loc.clone().add(0.5, 0, 0.5));
                godTrophy.playSound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER);
            }
            else if (cycle == 121) {
                godTrophy.spawnPlayer(playerName, playerUUID);
                godTrophy.spawnCrystal(0.5, 1.5, 0.5);
            }
        }

        if (cycle > 121) {
            if (cycle % 2 == 0) {
                World world = loc.getWorld();
                if (world == null) return;
                world.spawnParticle(Particle.ENCHANT, loc.clone().add(0.5, 0, 0.5), 40);
            }
            if (cycle % 4 == 0) godTrophy.moveCrystal();
            godTrophy.questParticleEffect();
        }
    }

    public GodTrophyEffects getGodTrophy() {
        return godTrophy;
    }

    public int getType() {
        return type;
    }

    public int getCycle() {
        return cycle;
    }
}
