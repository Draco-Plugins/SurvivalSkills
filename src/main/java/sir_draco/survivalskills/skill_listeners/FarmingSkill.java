package sir_draco.survivalskills.skill_listeners;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import sir_draco.survivalskills.abilities.items.HarvesterAsync;
import sir_draco.survivalskills.abilities.items.HarvesterTimer;
import sir_draco.survivalskills.god_questline.trial.TrialManager;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.skills.SkillManager;
import sir_draco.survivalskills.utils.ProjectileCalculator;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.Utils;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FarmingSkill implements Listener {

    private final SurvivalSkills plugin;
    private final Map<Player, Set<Block>> harvestedBlocks = new HashMap<>();
    private final Map<Player, HarvesterTimer> harvesterCooldowns = new HashMap<>();
    private final Map<Material, Integer> foodNutritionMap = new HashMap<>();
    private final Map<Material, Float> foodSaturationMap = new HashMap<>();
    private final List<Material> leafBlocks = new ArrayList<>();
    private final List<Player> autoEat = new ArrayList<>();

    private static final int WATERING_CAN_ID = 9;
    private static final int BONEMEAL_ID = 10;
    private static final int HARVESTER_ID = 11;

    public FarmingSkill(SurvivalSkills plugin) {
        this.plugin = plugin;
        createLeafList();
        createFoodMappings();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onBlockBreakEvent(BlockBreakEvent e) {
        Block block = e.getBlock();
        Player p = e.getPlayer();
        Material type = block.getType();

        if (leafBlocks.contains(type)) {
            handleHarvesterAndDoubling(p, block, e);
            return;
        }

        if (!plugin.getFarmingList().contains(type) && !type.toString().contains("LOG"))
            return;

        if (!isMatureCrop(block)) return;

        int xpMultiplier = getSugarCaneXPMultiplier(block);
        SkillManager.experienceEvent(plugin, p, plugin.getSkillManager().getFarmingXP() * xpMultiplier, SkillCategory.FARMING);

        if (type.toString().contains("LOG")) return;

        handleHarvesterAndDoubling(p, block, e);
    }

    private boolean isMatureCrop(Block block) {
        Material type = block.getType();
        if (type == Material.SUGAR_CANE || type == Material.CACTUS) return true;
        if (block.getState().getBlockData() instanceof Ageable age)
            return age.getAge() == age.getMaximumAge();
        return true;
    }

    private int getSugarCaneXPMultiplier(Block block) {
        Material type = block.getType();
        if (type != Material.SUGAR_CANE && type != Material.CACTUS) return 1;
        Material above = block.getLocation().clone().add(0, 1, 0).getBlock().getType();
        if (above == Material.SUGAR_CANE || above == Material.CACTUS) return 2;
        return 1;
    }

    private void handleHarvesterAndDoubling(Player p, Block block, BlockBreakEvent e) {
        boolean isHarvested = isHarvestedBlock(p, block);
        triggerHarvesterAbility(p, block, isHarvested, block.getType(), e);
        doubleCrops(p, e, isHarvested);
    }

    @EventHandler
    public void onClickEvent(PlayerInteractEvent e) {
        if (!e.hasBlock()) return;

        Player p = e.getPlayer();
        Block block = e.getClickedBlock();
        if (block == null) return;

        // Check if it is in a claim
        if (plugin.isGriefPreventionEnabled() && Utils.checkForClaim(p, block.getLocation())) return;

        handleWateringCan(p, block);
        if (block.getType().equals(Material.DIRT)) return;
        handleUnlimitedBoneMeal(p, block, e);

        if (!(block.getState().getBlockData() instanceof Ageable age)) return;
        if (!block.getType().equals(Material.SWEET_BERRY_BUSH)) return;
        if (age.getAge() != age.getMaximumAge()) return;
        SkillManager.experienceEvent(plugin, p, plugin.getSkillManager().getFarmingXP(), SkillCategory.FARMING);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (!plugin.getFarmingList().contains(e.getBlock().getType())) return;
        SkillManager.experienceEvent(plugin, p, plugin.getSkillManager().getFarmingXP() * 0.5, SkillCategory.FARMING);
    }

    @EventHandler
    public void hungerEvent(FoodLevelChangeEvent e) {
        Player p = e.getEntity() instanceof Player player ? player : null;
        if (p == null) return;

        // If a player is in a trial, don't apply the no hunger effect
        if (TrialManager.isInTrial(p)) return;

        if (plugin.getSkillManager().getPlayerRewards(p).getReward(SkillCategory.FARMING, "NoHunger").isApplied()) {
            e.setCancelled(true);
            p.setFoodLevel(20);
            p.setSaturation(20);
            return;
        }

        handleAutoEat(e, p);
    }

    private void handleAutoEat(FoodLevelChangeEvent e, Player p) {
        if (!autoEat.contains(p)) return;

        // Process on next tick
        new BukkitRunnable() {
            @Override
            public void run() {
                ArrayList<ItemStack> edibleItems = findEdibleItems(p);
                if (edibleItems.isEmpty()) {
                    p.sendRawMessage(ChatColor.RED + "You have no food to eat!");
                    autoEat.remove(p);
                    p.sendRawMessage(ChatColor.YELLOW + "Auto Eat has been disabled.");
                    p.playSound(p, Sound.ENTITY_PANDA_EAT, 1, 1);
                } else {
                    for (ItemStack next : edibleItems)
                        consumeFoodItem(p, next);
                }
            }
        }.runTaskLater(plugin, 1);
    }

    private ArrayList<ItemStack> findEdibleItems(Player p) {
        ArrayList<ItemStack> edibleItems = new ArrayList<>();
        for (ItemStack item : p.getInventory().getContents()) {
            if (item == null) continue;
            if (!item.getType().isEdible()) continue;
            if (item.getType().equals(Material.ROTTEN_FLESH) || item.getType().equals(Material.POISONOUS_POTATO)
                    || item.getType().equals(Material.SPIDER_EYE))
                continue; // skip negative food
            if (item.getItemMeta() != null && ItemStackGeneratorUtils.hasCustomModelData(item.getItemMeta()))
                continue; // skip custom items (likely special tools)

            edibleItems.add(item);
        }
        return edibleItems;
    }

    private void consumeFoodItem(Player p, ItemStack stack) {
        if (stack == null) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;

        int foodAmount = foodNutritionMap.getOrDefault(stack.getType(), 0);
        float saturationAmount = foodSaturationMap.getOrDefault(stack.getType(), 0f);

        if (foodAmount == 0) return;

        // Calculate how many items it would take to fill hunger
        int itemsNeeded = (20 - p.getFoodLevel()) / foodAmount;
        if ((20 - p.getFoodLevel()) % foodAmount != 0) itemsNeeded++;

        // Remove what items are available from the stack amount
        int itemsAvailable = stack.getAmount();
        int itemsUsed = 0;
        if (itemsAvailable > itemsNeeded) {
            itemsUsed = itemsNeeded;
            stack.setAmount(itemsAvailable - itemsNeeded);
        } else {
            itemsUsed = itemsAvailable;
            p.getInventory().remove(stack);
        }

        // Update the player's food level and saturation
        p.setFoodLevel(Math.min(20, p.getFoodLevel() + (foodAmount * itemsUsed)));
        p.setSaturation(Math.min(20, p.getSaturation() + (saturationAmount * itemsUsed)));
    }

    public void handleWateringCan(Player p, Block block) {
        if (!ItemStackGeneratorUtils.isCustomItem(p.getInventory().getItemInMainHand(), WATERING_CAN_ID)) return;
        if (!plugin.getSkillManager().getPlayerRewards(p).getReward(SkillCategory.FARMING, "WateringCan").isApplied()) {
            p.sendRawMessage(ChatColor.RED + "Watering Can unlocks at Farming Level: " + ChatColor.AQUA
                    + plugin.getSkillManager().getDefaultPlayerRewards().getReward(SkillCategory.FARMING, "WateringCan").getLevel());
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }
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
                    if (!(b.getState().getBlockData() instanceof Ageable age))
                        continue;
                    increaseCropAge(age, b);
                } else if (b.getType().equals(Material.DIRT)) {
                    b.setType(Material.GRASS_BLOCK);
                    b.getState().update();
                }
            }
        }
    }

    private static void increaseCropAge(Ageable age, Block b) {
        if (age.getAge() == age.getMaximumAge()) return;
        if (Math.random() < 0.1) return;
        age.setAge(age.getAge() + 1);
        b.setBlockData(age);
        b.getState().update();
    }

    public void spawnWateringCanParticle(Location from, Location to) {
        if (from.getWorld() == null) return;
        Vector direction = ProjectileCalculator.getDirectionVector(from, to);
        // Spawn a water particles that spray out with random variation in the direction
        // of the 'to' location
        for (int i = 0; i < 3; i++) {
            // Get the direction of the 'to' location with some random variation
            double x = direction.getX();
            double y = Math.abs(direction.getY());
            double z = direction.getZ();
            from.getWorld().spawnParticle(Particle.SPLASH, from, 1, x, y, z, 0);
        }
    }

    public void handleUnlimitedBoneMeal(Player p, Block block, PlayerInteractEvent e) {
        ItemStack item = e.getItem();
        if (!ItemStackGeneratorUtils.isCustomItem(item, BONEMEAL_ID)) return;
        e.setCancelled(true);
        if (!plugin.getSkillManager().getPlayerRewards(p).getReward(SkillCategory.FARMING, "UnlimitedBoneMeal").isApplied()) {
            p.sendRawMessage(ChatColor.RED + "Unlimited Bonemeal unlocks at Farming Level: " + ChatColor.AQUA
                    + plugin.getSkillManager().getDefaultPlayerRewards().getReward(SkillCategory.FARMING, "UnlimitedBoneMeal")
                            .getLevel());
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }

        block.applyBoneMeal(e.getBlockFace());
    }

    public void doubleCrops(Player p, BlockBreakEvent e, boolean harvester) {
        if (handleHarvesterDrops(p, e, harvester)) return;

        double chance = plugin.getSkillManager().getPlayerRewards(p).getCropDoubleChance();
        if (chance == 0) return;
        if (Math.random() >= chance) return;
        applyCustomDrops(e, p, true);
    }

    private boolean handleHarvesterDrops(Player p, BlockBreakEvent e, boolean harvester) {
        if (!harvester) return false;
        boolean doubleCrops = Math.random() < plugin.getSkillManager().getPlayerRewards(p).getCropDoubleChance();
        applyCustomDrops(e, p, doubleCrops);
        return true;
    }

    private void applyCustomDrops(BlockBreakEvent e, Player p, boolean shouldDouble) {
        e.setDropItems(false);
        ItemStack[] drops = e.getBlock().getDrops(p.getInventory().getItemInMainHand()).toArray(new ItemStack[0]);
        for (ItemStack drop : drops) {
            if (isSeed(drop.getType())) continue;
            if (shouldDouble) drop.setAmount(drop.getAmount() * 2);
            e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), drop);
        }
    }

    public boolean isSeed(Material type) {
        return switch (type) {
            case WHEAT_SEEDS, BEETROOT_SEEDS, PUMPKIN_SEEDS, MELON_SEEDS -> true;
            default -> false;
        };
    }

    public void triggerHarvesterAbility(Player p, Block block, boolean isHarvested, Material type, BlockBreakEvent e) {
        if (isHarvested) return;
        if (!ItemStackGeneratorUtils.isCustomItem(p.getInventory().getItemInMainHand(), HARVESTER_ID)) return;
        if (!plugin.getSkillManager().getPlayerRewards(p).getReward(SkillCategory.FARMING, "Harvester").isApplied()
                && !p.hasPermission("survivalskills.op")) {
            p.sendRawMessage(ChatColor.RED + "Harvester unlocks at Farming Level: " + ChatColor.AQUA
                    + plugin.getSkillManager().getDefaultPlayerRewards().getReward(SkillCategory.FARMING, "Harvester").getLevel());
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }
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
        Set<Block> blocks = harvestedBlocks.get(p);
        if (blocks == null) return false;
        if (!blocks.remove(block)) return false;
        if (blocks.isEmpty()) harvestedBlocks.remove(p);
        return true;
    }

    public Map<Player, HarvesterTimer> getHarvesterCooldowns() {
        return harvesterCooldowns;
    }

    public void createFoodMappings() {
        // Basic crops / simple foods
        foodNutritionMap.put(Material.APPLE, 4);
        foodSaturationMap.put(Material.APPLE, 0.6f);
        foodNutritionMap.put(Material.BAKED_POTATO, 5);
        foodSaturationMap.put(Material.BAKED_POTATO, 1.2f);
        foodNutritionMap.put(Material.BEETROOT, 1);
        foodSaturationMap.put(Material.BEETROOT, 1.2f);
        foodNutritionMap.put(Material.BEETROOT_SOUP, 6);
        foodSaturationMap.put(Material.BEETROOT_SOUP, 1.2f);
        foodNutritionMap.put(Material.BREAD, 5);
        foodSaturationMap.put(Material.BREAD, 1.2f);
        foodNutritionMap.put(Material.CARROT, 3);
        foodSaturationMap.put(Material.CARROT, 1.2f);
        foodNutritionMap.put(Material.MELON_SLICE, 2);
        foodSaturationMap.put(Material.MELON_SLICE, 0.6f);
        foodNutritionMap.put(Material.POTATO, 1);
        foodSaturationMap.put(Material.POTATO, 0.6f);

        // Meat / fish
        foodNutritionMap.put(Material.BEEF, 3);
        foodSaturationMap.put(Material.BEEF, 0.6f);
        foodNutritionMap.put(Material.COOKED_BEEF, 8);
        foodSaturationMap.put(Material.COOKED_BEEF, 1.6f);
        foodNutritionMap.put(Material.CHICKEN, 2);
        foodSaturationMap.put(Material.CHICKEN, 0.6f);
        foodNutritionMap.put(Material.COOKED_CHICKEN, 6);
        foodSaturationMap.put(Material.COOKED_CHICKEN, 1.2f);
        foodNutritionMap.put(Material.PORKCHOP, 3);
        foodSaturationMap.put(Material.PORKCHOP, 0.6f);
        foodNutritionMap.put(Material.COOKED_PORKCHOP, 8);
        foodSaturationMap.put(Material.COOKED_PORKCHOP, 1.6f);
        foodNutritionMap.put(Material.RABBIT, 3);
        foodSaturationMap.put(Material.RABBIT, 0.6f);
        foodNutritionMap.put(Material.COOKED_RABBIT, 5);
        foodSaturationMap.put(Material.COOKED_RABBIT, 1.2f);
        foodNutritionMap.put(Material.MUTTON, 2);
        foodSaturationMap.put(Material.MUTTON, 0.6f);
        foodNutritionMap.put(Material.COOKED_MUTTON, 6);
        foodSaturationMap.put(Material.COOKED_MUTTON, 1.6f);
        foodNutritionMap.put(Material.COD, 2);
        foodSaturationMap.put(Material.COD, 0.2f);
        foodNutritionMap.put(Material.COOKED_COD, 5);
        foodSaturationMap.put(Material.COOKED_COD, 1.2f);
        foodNutritionMap.put(Material.SALMON, 2);
        foodSaturationMap.put(Material.SALMON, 0.2f);
        foodNutritionMap.put(Material.COOKED_SALMON, 6);
        foodSaturationMap.put(Material.COOKED_SALMON, 1.6f);

        // Prepared / special foods
        foodNutritionMap.put(Material.GOLDEN_APPLE, 4);
        foodSaturationMap.put(Material.GOLDEN_APPLE, 2.4f);
        foodNutritionMap.put(Material.ENCHANTED_GOLDEN_APPLE, 4);
        foodSaturationMap.put(Material.ENCHANTED_GOLDEN_APPLE, 2.4f);
        foodNutritionMap.put(Material.GOLDEN_CARROT, 6);
        foodSaturationMap.put(Material.GOLDEN_CARROT, 2.4f);
        foodNutritionMap.put(Material.HONEY_BOTTLE, 6);
        foodSaturationMap.put(Material.HONEY_BOTTLE, 0.2f);
        foodNutritionMap.put(Material.PUMPKIN_PIE, 8);
        foodSaturationMap.put(Material.PUMPKIN_PIE, 0.6f);
        foodNutritionMap.put(Material.POISONOUS_POTATO, 2);
        foodSaturationMap.put(Material.POISONOUS_POTATO, 0.6f);
        foodNutritionMap.put(Material.COOKIE, 2);
        foodSaturationMap.put(Material.COOKIE, 0.2f);
        foodNutritionMap.put(Material.MUSHROOM_STEW, 6);
        foodSaturationMap.put(Material.MUSHROOM_STEW, 1.2f);
        foodNutritionMap.put(Material.RABBIT_STEW, 10);
        foodSaturationMap.put(Material.RABBIT_STEW, 1.2f);
        foodNutritionMap.put(Material.SUSPICIOUS_STEW, 6);
        foodSaturationMap.put(Material.SUSPICIOUS_STEW, 1.2f);

        // Berries / small foods
        foodNutritionMap.put(Material.SWEET_BERRIES, 2);
        foodSaturationMap.put(Material.SWEET_BERRIES, 0.2f);
        foodNutritionMap.put(Material.GLOW_BERRIES, 2);
        foodSaturationMap.put(Material.GLOW_BERRIES, 0.2f);

        // Fish variants
        foodNutritionMap.put(Material.TROPICAL_FISH, 1);
        foodSaturationMap.put(Material.TROPICAL_FISH, 0.2f);
        foodNutritionMap.put(Material.PUFFERFISH, 1);
        foodSaturationMap.put(Material.PUFFERFISH, 0.2f);

        // Other
        // PUMPKIN_PIE already added above
        foodNutritionMap.put(Material.DRIED_KELP, 1);
        foodSaturationMap.put(Material.DRIED_KELP, 0.6f);
        foodNutritionMap.put(Material.ROTTEN_FLESH, 4);
        foodSaturationMap.put(Material.ROTTEN_FLESH, 0.2f);
        foodNutritionMap.put(Material.SPIDER_EYE, 2);
        foodSaturationMap.put(Material.SPIDER_EYE, 1.6f);
    }

    public Map<Player, Set<Block>> getHarvestedBlocks() {
        return harvestedBlocks;
    }

    public List<Player> getAutoEat() {
        return autoEat;
    }
}
