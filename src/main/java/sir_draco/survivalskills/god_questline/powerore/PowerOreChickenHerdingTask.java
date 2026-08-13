package sir_draco.survivalskills.god_questline.powerore;

import org.bukkit.ChatColor;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Herding task: lure the spawned chickens onto the converting obsidian before time expires. */
public final class PowerOreChickenHerdingTask extends BukkitRunnable implements PowerOreTask {

    private static final int SPAWN_RADIUS = 25;
    private static final int MAX_SPAWN_ATTEMPTS = PowerOreChickenHerdingGame.TOTAL_CHICKENS * 50;
    private static final double MOVEMENT_SPEED_MULTIPLIER = 2.0;
    private static final double BLOCK_CENTER_OFFSET = 0.5;
    private static final double SPAWN_HEIGHT_OFFSET = 1.0;
    private static final long INITIAL_DELAY_TICKS = 1L;
    private static final long TASK_PERIOD_TICKS = 1L;
    private static final String CHICKEN_NAME = ChatColor.GOLD + "Power Ore Herding Chicken";
    private static final String SEED_NAME = ChatColor.YELLOW + "Chicken Herding Seeds";

    private final PowerOreChallenge challenge;
    private final Player player;
    private final Location oreLocation;
    private final Random random;
    private final PowerOreChickenHerdingGame game = new PowerOreChickenHerdingGame();
    private final List<Chicken> chickens = new ArrayList<>();

    public PowerOreChickenHerdingTask(PowerOreChallenge challenge, Player player, Location oreLocation) {
        this(challenge, player, oreLocation, new Random());
    }

    PowerOreChickenHerdingTask(PowerOreChallenge challenge, Player player, Location oreLocation, Random random) {
        this.challenge = challenge;
        this.player = player;
        this.oreLocation = oreLocation.clone();
        this.random = random;
    }

    @Override
    public void start() {
        World world = oreLocation.getWorld();
        if (world == null) {
            challenge.fail("World unloaded");
            return;
        }

        spawnChickens(world);
        if (chickens.size() != PowerOreChickenHerdingGame.TOTAL_CHICKENS) {
            challenge.fail("Could not find enough safe locations for the chickens");
            return;
        }

        giveOrDropSeeds(world);
        player.sendMessage(ChatColor.AQUA + "Chicken Herding: " + ChatColor.YELLOW
                + "Bring all " + PowerOreChickenHerdingGame.TOTAL_CHICKENS
                + " named chickens back to the converting obsidian. You have 60 seconds!");
        sendProgress();
        runTaskTimer(SurvivalSkills.getInstance(), INITIAL_DELAY_TICKS, TASK_PERIOD_TICKS);
    }

    private void spawnChickens(World world) {
        Set<Location> spawnBlocks = new HashSet<>();
        int attempts = 0;
        while (chickens.size() < PowerOreChickenHerdingGame.TOTAL_CHICKENS
                && attempts < MAX_SPAWN_ATTEMPTS) {
            attempts++;
            int offsetX = random.nextInt((SPAWN_RADIUS * 2) + 1) - SPAWN_RADIUS;
            int offsetZ = random.nextInt((SPAWN_RADIUS * 2) + 1) - SPAWN_RADIUS;
            if ((offsetX * offsetX) + (offsetZ * offsetZ) > SPAWN_RADIUS * SPAWN_RADIUS)
                continue;

            int x = oreLocation.getBlockX() + offsetX;
            int z = oreLocation.getBlockZ() + offsetZ;
            Block ground = world.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            Location groundLocation = ground.getLocation();
            if (!ground.getType().isSolid() || !spawnBlocks.add(groundLocation))
                continue;
            if (!ground.getRelative(0, 1, 0).isPassable() || !ground.getRelative(0, 2, 0).isPassable())
                continue;

            Location spawnLocation = groundLocation.clone().add(
                    BLOCK_CENTER_OFFSET, SPAWN_HEIGHT_OFFSET, BLOCK_CENTER_OFFSET);
            chickens.add(spawnChicken(world, spawnLocation));
        }
    }

    private void configureChicken(Chicken chicken) {
        chicken.setCustomName(CHICKEN_NAME);
        chicken.setCustomNameVisible(true);
        chicken.setRemoveWhenFarAway(false);
        AttributeInstance movementSpeed = chicken.getAttribute(Attribute.MOVEMENT_SPEED);
        if (movementSpeed != null)
            movementSpeed.setBaseValue(movementSpeed.getBaseValue() * MOVEMENT_SPEED_MULTIPLIER);
    }

    private void giveOrDropSeeds(World world) {
        ItemStack seeds = new ItemStack(Material.WHEAT_SEEDS);
        ItemMeta meta = seeds.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(SEED_NAME);
            seeds.setItemMeta(meta);
        }
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(seeds);
        overflow.values().forEach((ItemStack overflowSeeds) ->
                world.dropItemNaturally(player.getLocation(), overflowSeeds));
    }

    @Override
    public void run() {
        if (!PowerOreChallenge.Status.RUNNING.equals(challenge.getStatus())) {
            cancel();
            return;
        }

        processChickens();
        if (game.isComplete()) {
            challenge.complete();
            return;
        }

        game.advanceTimer((int) TASK_PERIOD_TICKS);
        sendProgress();
        if (game.isExpired())
            challenge.fail("Time ran out before every chicken reached the Power Ore");
    }

    private void processChickens() {
        respawnDeadChickens();
        Block oreBlock = oreLocation.getBlock();
        List<Chicken> delivered = chickens.stream()
                .filter((Chicken chicken) -> chicken.isValid() && !chicken.isDead())
                .filter((Chicken chicken) -> chicken.getLocation().getBlock().getRelative(0, -1, 0).equals(oreBlock))
                .toList();
        delivered.forEach((Chicken chicken) -> chicken.remove());
        chickens.removeAll(delivered);
        game.deliverChickens(delivered.size());
        if (!delivered.isEmpty())
            player.playSound(oreLocation, Sound.ENTITY_CHICKEN_EGG, 1, 1.3f);
    }

    private void respawnDeadChickens() {
        List<Chicken> deadChickens = chickens.stream()
                .filter((Chicken chicken) -> chicken.isDead())
                .toList();
        deadChickens.forEach((Chicken deadChicken) -> {
            Location spawnLocation = deadChicken.getLocation().clone();
            World world = spawnLocation.getWorld();
            if (world == null) {
                challenge.fail("World unloaded");
                return;
            }
            chickens.remove(deadChicken);
            chickens.add(spawnChicken(world, spawnLocation));
        });
    }

    private Chicken spawnChicken(World world, Location spawnLocation) {
        return world.spawn(spawnLocation, Chicken.class, this::configureChicken);
    }

    private void sendProgress() {
        if (!player.isOnline())
            return;
        Utils.sendActionBarMessage(player, ChatColor.GOLD + "Chicken Herding" + ChatColor.DARK_GRAY + " | "
                + ChatColor.YELLOW + "Chickens Left: " + ChatColor.WHITE + game.getChickensRemaining()
                + ChatColor.DARK_GRAY + " | " + ChatColor.YELLOW + "Time: " + ChatColor.WHITE
                + game.getSecondsRemaining() + "s");
    }

    @Override
    public void cleanup() {
        try {
            cancel();
        } catch (IllegalStateException ignored) {
            // Task was not scheduled or was already cancelled
        }
        chickens.stream()
                .filter((Chicken chicken) -> chicken.isValid())
                .forEach((Chicken chicken) -> chicken.remove());
        chickens.clear();
        if (player.isOnline())
            Utils.sendActionBarMessage(player, "");
    }

    @Override
    public String name() {
        return "Chicken Herding";
    }

    @Override
    public Player getPlayer() {
        return player;
    }
}
