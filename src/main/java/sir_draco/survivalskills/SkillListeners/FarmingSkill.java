package sir_draco.survivalskills.SkillListeners;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.Abilities.HarvesterAsync;
import sir_draco.survivalskills.Abilities.HarvesterTimer;
import sir_draco.survivalskills.Bosses.ProjectileCalculator;
import sir_draco.survivalskills.ItemStackGenerator;
import sir_draco.survivalskills.Skill;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;
import java.util.HashMap;

public class FarmingSkill implements Listener {

    private final SurvivalSkills plugin;
    private final HashMap<Player, ArrayList<Block>> harvestedBlocks = new HashMap<>();
    private final HashMap<Player, HarvesterTimer> harvesterCooldowns = new HashMap<>();
    private final ArrayList<Material> leafBlocks = new ArrayList<>();
    private final ArrayList<Player> autoEat = new ArrayList<>();

    private double xp; // XP per crop harvested

    public FarmingSkill(SurvivalSkills plugin, double xp) {
        this.plugin = plugin;
        this.xp = xp;
        createLeafList();
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.LOW)
    public void onBlockBreakEvent(BlockBreakEvent e) {
        Block block = e.getBlock();
        Player p = e.getPlayer();

        if (leafBlocks.contains(block.getType())) {
            boolean isHarvested = isHarvestedBlock(p, block);
            handleHarvester(p, block, isHarvested, block.getType(), e);
            doubleCrops(p, e, isHarvested);
            return;
        }
        if (!plugin.getFarmingList().contains(block.getType()) && !block.getType().toString().contains("LOG")) return;

        if (!block.getType().equals(Material.SUGAR_CANE) && !block.getType().equals(Material.CACTUS)) {
            if (block.getState().getBlockData() instanceof Ageable) {
                Ageable age = (Ageable) block.getState().getBlockData();
                if (age.getAge() != age.getMaximumAge()) return;
            }
        }

        int doubleXP = 1;
        if (e.getBlock().getType().equals(Material.SUGAR_CANE) || e.getBlock().getType().equals(Material.CACTUS)) {
            Material above = e.getBlock().getLocation().clone().add(0, 1, 0).getBlock().getType();
            if (above.equals(Material.SUGAR_CANE) || above.equals(Material.CACTUS)) doubleXP = 2;
        }

        Skill.experienceEvent(plugin, p, xp * doubleXP, "Farming");
        if (block.getType().toString().contains("LOG")) return;

        boolean isHarvested = isHarvestedBlock(p, block);
        handleHarvester(p, block, isHarvested, block.getType(), e);
        doubleCrops(p, e, isHarvested);
    }

    @EventHandler
    public void onClickEvent(PlayerInteractEvent e) {
        if (!e.hasBlock()) return;

        Player p = e.getPlayer();
        Block block = e.getClickedBlock();
        if (block == null) return;

        // Check if it is in a claim
        if (plugin.isGriefPreventionEnabled() && plugin.checkForClaim(p, block.getLocation())) return;

        handleWateringCan(p, block);
        if (block.getType().equals(Material.DIRT)) return;
        handleUnlimitedBoneMeal(p, block, e);

        if (!(block.getState().getBlockData() instanceof Ageable)) return;
        if (!block.getType().equals(Material.SWEET_BERRY_BUSH)) return;
        Ageable age = (Ageable) block.getState().getBlockData();
        if (age.getAge() != age.getMaximumAge()) return;
        Skill.experienceEvent(plugin, p, xp, "Farming");
        plugin.updateScoreboard(p, "Farming");
    }

    @EventHandler (ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();

        if (!plugin.getFarmingList().contains(e.getBlock().getType())) return;

        Skill.experienceEvent(plugin, p, xp * 0.5, "Farming");
    }

    @EventHandler
    public void playerHungerManager(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (autoEat.contains(p) && p.getFoodLevel() < 20) {
            // Get the first food item in the player's inventory
            for (ItemStack item : p.getInventory().getContents()) {
                if (item == null) continue;
                if (item.getType().isEdible()) {
                    // Get the amount of food the item will restore
                    int foodRestore = getFoodLevelRestorationAmount(item.getType());
                    p.setFoodLevel(Math.min(p.getFoodLevel() + foodRestore, 20));
                    p.setSaturation(Math.min(p.getSaturation() + foodRestore * 0.6f, 20));

                    if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
                    else p.getInventory().remove(item);
                    return;
                }
            }
            p.sendRawMessage(ChatColor.RED + "You have no food to eat!");
            autoEat.remove(p);
            p.sendRawMessage(ChatColor.YELLOW + "Auto Eat has been disabled.");
            p.playSound(p, Sound.ENTITY_PANDA_EAT, 1, 1);
            return;
        }

        if (!plugin.getPlayerRewards(p).getReward("Farming", "NoHunger").isApplied()) return;
        if (p.getFoodLevel() < 20) p.setFoodLevel(20);
    }

    public void handleWateringCan(Player p, Block block) {
        if (!ItemStackGenerator.isCustomItem(p.getInventory().getItemInMainHand(), 9)) return;
        if (!plugin.getPlayerRewards(p).getReward("Farming", "WateringCan").isApplied() && !p.hasPermission("survivalskills.op")) return;
        growNearbyCrops(block);
    }

    public void createLeafList() {
        leafBlocks.add(Material.OAK_LEAVES);
        leafBlocks.add(Material.SPRUCE_LEAVES);
        leafBlocks.add(Material.BIRCH_LEAVES);
        leafBlocks.add(Material.JUNGLE_LEAVES);
        leafBlocks.add(Material.ACACIA_LEAVES);
        leafBlocks.add(Material.DARK_OAK_LEAVES);
        leafBlocks.add(Material.AZALEA_LEAVES);
        leafBlocks.add(Material.FLOWERING_AZALEA_LEAVES);
    }

    public void growNearbyCrops(Block block) {
        Location particleStart = block.getLocation().clone().add(0.5, 1, 0.5);
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Block b = block.getRelative(x, 0, z);
                spawnWateringCanParticle(particleStart, b.getLocation().clone().add(0.5, 0, 0.5));
                if (plugin.getFarmingList().contains(b.getType())) {
                    Ageable age = (Ageable) b.getState().getBlockData();
                    if (age.getAge() != age.getMaximumAge()) {
                        double chance = Math.random();
                        if (chance < 0.1) continue;
                        age.setAge(age.getAge() + 1);
                        b.setBlockData(age);
                        b.getState().update();
                    }
                }
                else if (b.getType().equals(Material.DIRT)) {
                    b.setType(Material.GRASS_BLOCK);
                    b.getState().update();
                }
            }
        }
    }

    public void spawnWateringCanParticle(Location from, Location to) {
        if (from.getWorld() == null) return;
        Vector direction = ProjectileCalculator.getDirectionVector(from, to);
        // Spawn a water particles that spray out with random variation in the direction of the 'to' location
        for (int i = 0; i < 3; i++) {
            // Get the direction of the 'to' location with some random variation
            double x = direction.getX();
            double y = Math.abs(direction.getY());
            double z = direction.getZ();
            from.getWorld().spawnParticle(Particle.SPLASH, from, 1, x, y, z, 0);
        }
    }

    public void handleUnlimitedBoneMeal(Player p, Block block, PlayerInteractEvent e) {
        if (!ItemStackGenerator.isCustomItem(p.getInventory().getItemInMainHand(), 10)) return;
        e.setCancelled(true);
        if (!plugin.getPlayerRewards(p).getReward("Farming", "UnlimitedBoneMeal").isApplied()) return;

        if (block.getType().equals(Material.GRASS_BLOCK)) block.applyBoneMeal(BlockFace.UP);
        else if (block.getType().equals(Material.MOSS_BLOCK)) block.applyBoneMeal(BlockFace.UP);
        else if (block.getType().toString().contains("SAPLING")) block.applyBoneMeal(BlockFace.UP);
        else {
            BlockData state = block.getState().getBlockData();
            if (!(state instanceof Ageable)) return;
            Ageable age = (Ageable) block.getState().getBlockData();
            if (age.getAge() == age.getMaximumAge()) return;
            age.setAge(age.getMaximumAge());
            block.setBlockData(age);
            block.getState().update();
        }
    }

    public void doubleCrops(Player p, BlockBreakEvent e, boolean harvester) {
        if (harvester) {
            e.setDropItems(false);
            boolean doubleCrops = false;
            double chance = Math.random();
            if (chance < plugin.getPlayerRewards(p).getCropDoubleChance()) doubleCrops = true;
            ItemStack[] drops = e.getBlock().getDrops(p.getInventory().getItemInMainHand()).toArray(new ItemStack[0]);
            for (ItemStack drop : drops) {
                if (isSeed(drop.getType())) continue;
                if (doubleCrops) drop.setAmount(drop.getAmount() * 2);
                e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), drop);
            }
            return;
        }

        if (plugin.getPlayerRewards(p).getCropDoubleChance() == 0) return;
        double chance = Math.random();
        if (chance >= plugin.getPlayerRewards(p).getCropDoubleChance()) return;
        e.setDropItems(false);
        ItemStack[] drops = e.getBlock().getDrops(p.getInventory().getItemInMainHand()).toArray(new ItemStack[0]);
        for (ItemStack drop : drops) {
            if (isSeed(drop.getType())) continue;
            drop.setAmount(drop.getAmount() * 2);
            e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), drop);
        }
    }

    public boolean isSeed(Material type) {
        switch (type) {
            case WHEAT_SEEDS:
            case BEETROOT_SEEDS:
            case PUMPKIN_SEEDS:
            case MELON_SEEDS:
                return true;
        }
        return false;
    }

    public void handleHarvester(Player p, Block block, boolean isHarvested, Material type, BlockBreakEvent e) {
        if (isHarvested) return;
        if (!ItemStackGenerator.isCustomItem(p.getInventory().getItemInMainHand(), 11)) return;
        if (!plugin.getPlayerRewards(p).getReward("Farming", "Harvester").isApplied() && !p.hasPermission("survivalskills.op")) return;
        if (harvesterCooldowns.containsKey(p)) {
            e.setCancelled(true);
            p.sendRawMessage(ChatColor.RED + "Slow down there!");
            return;
        }

        HarvesterTimer timer = new HarvesterTimer(plugin, p, 1);
        harvesterCooldowns.put(p, timer);
        timer.runTaskTimerAsynchronously(plugin, 20, 20);

        HarvesterAsync harvester = new HarvesterAsync(plugin, block, p, type);
        harvester.runTaskAsynchronously(plugin);
    }

    public boolean isHarvestedBlock(Player p, Block block) {
        if (!harvestedBlocks.containsKey(p)) return false;
        for (Block b : harvestedBlocks.get(p)) if (b.equals(block)) {
            harvestedBlocks.get(p).remove(b);
            if (harvestedBlocks.get(p).isEmpty()) harvestedBlocks.remove(p);
            return true;
        }
        return false;
    }

    public int getFoodLevelRestorationAmount(Material mat) {
        switch (mat) {
            case BEETROOT:
            case DRIED_KELP:
            case POTATO:
            case PUFFERFISH:
            case TROPICAL_FISH:
                return 1;
            case COOKIE:
            case CHICKEN:
            case COD:
            case MUTTON:
            case MELON_SLICE:
            case SALMON:
                return 2;
            case CARROT:
            case BEEF:
            case PORKCHOP:
            case RABBIT:
                return 3;
            case APPLE:
            case CHORUS_FRUIT:
            case ENCHANTED_GOLDEN_APPLE:
            case GOLDEN_APPLE:
                return 4;
            case BAKED_POTATO:
            case COOKED_COD:
            case BREAD:
            case COOKED_RABBIT:
                return 5;
            case BEETROOT_SOUP:
            case COOKED_MUTTON:
            case COOKED_CHICKEN:
            case COOKED_SALMON:
            case GOLDEN_CARROT:
            case HONEY_BOTTLE:
            case MUSHROOM_STEW:
                return 6;
            case COOKED_BEEF:
            case COOKED_PORKCHOP:
            case PUMPKIN_PIE:
                return 8;
            case RABBIT_STEW:
                return 10;
            default:
                return 0;
        }
    }

    public HashMap<Player, HarvesterTimer> getHarvesterCooldowns() {
        return harvesterCooldowns;
    }

    public HashMap<Player, ArrayList<Block>> getHarvestedBlocks() {
        return harvestedBlocks;
    }

    public ArrayList<Player> getAutoEat() {
        return autoEat;
    }

    public void setXp(double xp) {
        this.xp = xp;
    }
}
