package sir_draco.survivalskills.skill_listeners;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import sir_draco.survivalskills.abilities.SpelunkerAbilitySync;
import sir_draco.survivalskills.abilities.VeinMinerAsync;
import sir_draco.survivalskills.abilities.items.PowerDrillTask;
import sir_draco.survivalskills.rewards.PlayerRewards;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.skills.SkillManager;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.Utils;
import sir_draco.survivalskills.utils.items.ItemModelData;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class MiningSkill implements Listener {

    private static final int CUSTOM_ITEM_UNLIMITED_TORCH = ItemModelData.UNLIMITED_TORCH.getId();
    private static final int CUSTOM_ITEM_MINING_ARMOR = ItemModelData.MINING_ARMOR.getId();
    private static final int CUSTOM_ITEM_ZAP_WAND = ItemModelData.ZAP_WAND.getId();
    private static final int CUSTOM_ITEM_POWER_DRILL = ItemModelData.POWER_DRILL.getId();

    private final SurvivalSkills plugin;
    private final NamespacedKey unlimitedTorchDataKey;
    // Use EnumSet for O(1) contains() and minimal memory footprint. They are
    // immutable after construction.
    private Set<Material> ores = EnumSet.noneOf(Material.class);
    private Set<Material> commonOres = EnumSet.noneOf(Material.class);
    private Set<Material> uncommonOres = EnumSet.noneOf(Material.class);
    private Set<Material> rareOres = EnumSet.noneOf(Material.class);
    private final ArrayList<Player> peacefulMiners = new ArrayList<>();
    private final ArrayList<EntityType> peacefulMobList = new ArrayList<>();
    private final Set<Material> acceptableTools = createAcceptableTools();
    private final Map<Player, SpelunkerAbilitySync> spelunkerTracker = new ConcurrentHashMap<>();
    private final Map<Player, Boolean> veinminerTracker = new ConcurrentHashMap<>(); // false = takes hunger, true = doesn't
    private final Map<Player, List<Block>> veinTracker = new ConcurrentHashMap<>();
    private final HashMap<Player, Inventory> toolBelts = new HashMap<>();
    private final int blocksPerHunger;
    private final Set<UUID> activeVeinMinerPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public MiningSkill(SurvivalSkills plugin, int blocksPerHunger) {
        this.plugin = plugin;
        unlimitedTorchDataKey = new NamespacedKey(plugin, "unlimited_torches");
        this.blocksPerHunger = blocksPerHunger;
        setOres();
        setCommonOres();
        setUncommonOres();
        setRareOres();
        setPeacefulMobList();
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();

        if (plugin.getFarmingList().contains(e.getBlock().getType())) return;
        if (e.getBlock().getType().toString().contains("LOG")) return;

        // Handle Glowing Blocks
        removeGlow(e.getBlock());

        // Handle XP
        double multiplier = getMultiplier(e.getBlock().getType());
        double xpAmount = plugin.getSkillManager().getMiningXP() * multiplier;

        SkillManager.experienceEvent(plugin, p, xpAmount, SkillCategory.MINING);

        // Handle double ore chance
        doubleOre(p, e);

        // Handle veinminer
        veinminerChecker(p, e);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void preventUnlimitedTorchDrops(BlockBreakEvent e) {
        Block block = e.getBlock();
        if (!removeUnlimitedTorchData(block)) return;

        e.setDropItems(false);
    }

    @EventHandler
    public void placeTorch(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);

        if (rewards == null) {
            Bukkit.getLogger().warning("Player " + p.getName() + " does not have a PlayerRewards object");
            return;
        }

        if (!rewards.getReward(SkillCategory.MINING, "UnlimitedTorch").isApplied()) {
            if (ItemStackGeneratorUtils.isCustomItem(p.getInventory().getItemInMainHand(), CUSTOM_ITEM_UNLIMITED_TORCH)
                    || ItemStackGeneratorUtils.isCustomItem(p.getInventory().getItemInOffHand(), CUSTOM_ITEM_UNLIMITED_TORCH)) {
                e.setCancelled(true);
                p.sendRawMessage(ChatColor.RED + "Unlimited Torch unlocks at mining level "
                        + ChatColor.AQUA + rewards.getReward(SkillCategory.MINING, "UnlimitedTorch").getLevel());
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            }
            return;
        }

        if (e.getHand() == null) return;
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;

        if (e.getHand().equals(EquipmentSlot.OFF_HAND)
                && !ItemStackGeneratorUtils.isCustomItem(p.getInventory().getItemInOffHand(), CUSTOM_ITEM_UNLIMITED_TORCH))
            return;
        else if (e.getHand().equals(EquipmentSlot.HAND)
                && !ItemStackGeneratorUtils.isCustomItem(p.getInventory().getItemInMainHand(), CUSTOM_ITEM_UNLIMITED_TORCH))
            return;
        e.setCancelled(true);

        if (e.getClickedBlock() == null) return;

        // Check if it is in a claim
        if (plugin.isGriefPreventionEnabled() && Utils.checkForClaim(p, e.getClickedBlock().getLocation())) return;
        // Check if they are in spawn
        if (plugin.isWorldGuardEnabled()) {
            boolean canPlace = SurvivalSkills.getInstance().getWorldGuardProvider().canPlaceBlockInRegion(p, e.getClickedBlock().getLocation());
            if (!canPlace) return;
        }

        // Place torch if possible
        Block desiredBlock = e.getClickedBlock().getRelative(e.getBlockFace());
        if (!desiredBlock.isEmpty()) return;
        if (e.getBlockFace().equals(BlockFace.UP) || e.getBlockFace().equals(BlockFace.DOWN)) {
            desiredBlock.setType(Material.TORCH);
        } else {
            Directional torch = (Directional) Material.WALL_TORCH.createBlockData();
            torch.setFacing(e.getBlockFace());
            desiredBlock.setBlockData(torch);
        }
        desiredBlock.getState().update(true);
        markUnlimitedTorch(desiredBlock);
    }

    public void markUnlimitedTorch(Block block) {
        PersistentDataContainer data = block.getChunk().getPersistentDataContainer();
        long blockPosition = encodeBlockPosition(block);
        long[] positions = data.getOrDefault(unlimitedTorchDataKey, PersistentDataType.LONG_ARRAY, new long[0]);
        if (Arrays.stream(positions).anyMatch((long position) -> position == blockPosition)) return;

        long[] updatedPositions = Arrays.copyOf(positions, positions.length + 1);
        updatedPositions[positions.length] = blockPosition;
        data.set(unlimitedTorchDataKey, PersistentDataType.LONG_ARRAY, updatedPositions);
    }

    private boolean removeUnlimitedTorchData(Block block) {
        PersistentDataContainer data = block.getChunk().getPersistentDataContainer();
        long[] positions = data.get(unlimitedTorchDataKey, PersistentDataType.LONG_ARRAY);
        if (positions == null) return false;

        long blockPosition = encodeBlockPosition(block);
        long[] remainingPositions = Arrays.stream(positions)
                .filter((long position) -> position != blockPosition)
                .toArray();
        if (remainingPositions.length == positions.length) return false;

        if (remainingPositions.length == 0) data.remove(unlimitedTorchDataKey);
        else data.set(unlimitedTorchDataKey, PersistentDataType.LONG_ARRAY, remainingPositions);
        return true;
    }

    private static long encodeBlockPosition(Block block) {
        return ((long) (block.getX() & 0xF) << 36)
                | ((long) (block.getZ() & 0xF) << 32)
                | (block.getY() & 0xFFFFFFFFL);
    }

    @EventHandler
    public void useZapWand(PlayerInteractEvent e) {
        if (e.getHand() == null) return;
        if (e.getHand().equals(EquipmentSlot.OFF_HAND)) return;
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        if (e.getClickedBlock() == null) return;
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!ItemStackGeneratorUtils.isCustomItem(hand, CUSTOM_ITEM_ZAP_WAND)) return;

        PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
        if (rewards == null) return;

        if (!rewards.getReward(SkillCategory.MINING, "ZapWand").isApplied()) {
            e.setCancelled(true);
            p.sendRawMessage(ChatColor.RED + "Zap Wand unlocks at mining level " + ChatColor.AQUA +
                    rewards.getReward(SkillCategory.MINING, "ZapWand").getLevel());
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }

        World world = e.getClickedBlock().getWorld();
        Location loc = e.getClickedBlock().getLocation();
        world.strikeLightning(loc);
        e.setCancelled(true);
    }

    @EventHandler
    public void toolDamage(PlayerItemDamageEvent e) {
        Player p = e.getPlayer();
        if (!plugin.getSkillManager().getPlayerRewards(p).isUnbreakableTools()) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void playerHurt(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
        if (rewards == null) return;
        double reductionPercentage = rewards.getProtectionPercentage();
        if (reductionPercentage == 0) return;
        double newDamage = e.getDamage() * (1 - reductionPercentage);
        e.setDamage(newDamage);
    }

    @EventHandler
    public void playerMove(PlayerMoveEvent e) {
        // Make sure they are wearing the armor
        if (!isMiningArmor(e.getPlayer().getInventory())) return;

        // Add the potion effects
        e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 100, 1, false, false));

        Location loc = e.getPlayer().getLocation();
        if (loc.getWorld() == null || !loc.getWorld().getEnvironment().equals(World.Environment.NORMAL)
                || loc.getBlockY() >= 64)
            return;
        e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, 0, false, false));
        e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 100, 0, false, false));
    }

    @EventHandler
    public void onMobSpawn(EntitySpawnEvent e) {
        if (e.getLocation().getBlockY() >= 64) return;
        if (!peacefulMobList.contains(e.getEntityType())) return;
        for (Player p : peacefulMiners) {
            if (!p.getWorld().getEnvironment().equals(e.getEntity().getWorld().getEnvironment())) continue;
            if (p.getLocation().distance(e.getEntity().getLocation()) > 150) continue;
            e.setCancelled(true);
            e.getEntity().remove();
            return;
        }
    }

    @EventHandler
    public void onToolBeltClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;
        Player p = (Player) e.getWhoClicked();
        Inventory inv = e.getClickedInventory();
        Inventory top = e.getView().getTopInventory();
        if (!toolBelts.containsKey(p)) return;
        Inventory toolBelt = toolBelts.get(p);
        if (!toolBelt.equals(inv) && !toolBelt.equals(top)) return;
        // Prevent hotbar swaps
        if (e.getAction().equals(InventoryAction.HOTBAR_SWAP)
                || e.getAction().equals(InventoryAction.HOTBAR_MOVE_AND_READD)) {
            e.setCancelled(true);
            return;
        }
        if (e.getCurrentItem() == null) return;

        // Check if the clicked item is a tool
        if (inv.equals(top)) return;
        if (acceptableTools.contains(e.getCurrentItem().getType())) return;
        p.sendRawMessage(ChatColor.RED + "You can only put tools in the tool belt");
        p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        e.setCancelled(true);
    }

    @EventHandler
    public void onToolBeltDrag(InventoryDragEvent e) {
        Inventory inv = e.getInventory();
        if (!toolBelts.containsValue(inv)) return;
        Player p = (Player) e.getWhoClicked();

        // Check if the clicked item is a tool
        if (!acceptableTools.contains(e.getOldCursor().getType())) {
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            e.setCancelled(true);
            return;
        }

        for (ItemStack item : e.getNewItems().values()) {
            if (acceptableTools.contains(item.getType())) continue;
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onToolBeltClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        if (!toolBelts.containsKey(p)) return;

        // Save the changes to the tool belt
        plugin.getAbilityManager().saveToolBelt(p, toolBelts.get(p));
    }

    public double getMultiplier(Material mat) {
        return switch (mat) {
            case DEEPSLATE -> 2.0;
            case COAL_ORE, DEEPSLATE_COAL_ORE, COPPER_ORE, DEEPSLATE_COPPER_ORE -> 3.0;
            case IRON_ORE, DEEPSLATE_IRON_ORE, NETHER_QUARTZ_ORE, NETHER_GOLD_ORE -> 4.0;
            case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE, LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> 5.0;
            case GOLD_ORE, DEEPSLATE_GOLD_ORE -> 7.0;
            case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> 10.0;
            case EMERALD_ORE, DEEPSLATE_EMERALD_ORE, ANCIENT_DEBRIS -> 20.0;
            case OBSIDIAN -> 30.0;
            default -> 1.0;
        };
    }

    public String getOreTeam(Material mat) {
        if (commonOres.contains(mat)) return "common";
        else if (uncommonOres.contains(mat)) return ChatColor.GREEN + "uncommon";
        else if (rareOres.contains(mat)) return ChatColor.BLUE + "rare";
        else return "none";
    }

    public void endSpelunkerAll() {
        if (spelunkerTracker.isEmpty()) return;
        for (Map.Entry<Player, SpelunkerAbilitySync> spelunker : spelunkerTracker.entrySet())
            spelunker.getValue().endThread();
    }

    public void removeGlow(Block block) {
        if (spelunkerTracker.isEmpty()) return;
        for (Map.Entry<Player, SpelunkerAbilitySync> tracker : spelunkerTracker.entrySet()) {
            if (!tracker.getValue().containsBlock(block)) continue;
            tracker.getValue().removeGlow(block);
        }
    }

    public void hideGlowForPlayer(Player p) {
        if (spelunkerTracker.isEmpty()) return;
        for (Map.Entry<Player, SpelunkerAbilitySync> hide : spelunkerTracker.entrySet())
            hide.getValue().hideAllGlowForPlayer(p);
    }

    public void doubleOre(Player p, BlockBreakEvent e) {
        // Defensive: rewards object can theoretically be null if player data not fully
        // loaded yet.
        PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
        if (rewards == null) return;
        double fortuneChance = rewards.getFortuneChance();
        if (fortuneChance <= 0) return;
        Material brokenType = e.getBlock().getType();
        if (!ores.contains(brokenType)) return;
        if (p.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.SILK_TOUCH) > 0) return; // Respect silk touch

        if (Math.random() >= fortuneChance) return; // Chance failed

        // Replace default drops with doubled stacks (capped at 64)
        e.setDropItems(false);
        for (ItemStack drop : e.getBlock().getDrops(p.getInventory().getItemInMainHand())) {
            if (drop == null || drop.getType().isAir()) continue;
            int newAmount = Math.min(drop.getAmount() * 2, 64);
            drop.setAmount(newAmount);
            e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), drop);
        }
    }

    public void veinminerChecker(Player p, BlockBreakEvent e) {
        if (p.hasMetadata(PowerDrillTask.DRILL_BREAK_METADATA)) return;
        boolean holdingPowerDrill = ItemStackGeneratorUtils.isCustomItem(
                p.getInventory().getItemInMainHand(), CUSTOM_ITEM_POWER_DRILL);
        if (shouldSkipVeinMiner(holdingPowerDrill, isVeinMinerActive(p))) return;

        // Make sure the player has the ability to vein mine
        if (!veinminerTracker.containsKey(p)) return;
        if (!p.isSneaking()) return;
        Material material = e.getBlock().getType();
        if (!ores.contains(material)) return;

        // Make sure this block isn't part of a previous vein mine
        veinTracker.computeIfAbsent(p, k -> new ArrayList<>());
        if (veinTracker.get(p).contains(e.getBlock())) {
            veinTracker.get(p).remove(e.getBlock());
            return;
        }

        VeinMinerAsync veinMiner = new VeinMinerAsync(plugin, p, this, e.getBlock(), material, blocksPerHunger);
        try {
            veinMiner.runTask(plugin);
        } catch (RuntimeException exception) {
            veinMiner.cleanupAfterFailure();
            Bukkit.getLogger().log(Level.SEVERE,
                    String.format("[SurvivalSkills] Failed to schedule vein miner for %s", p.getName()), exception);
        }
    }

    static boolean shouldSkipVeinMiner(boolean holdingPowerDrill, boolean veinMinerActive) {
        return holdingPowerDrill || veinMinerActive;
    }

    public boolean isMiningArmor(PlayerInventory inv) {
        if (!ItemStackGeneratorUtils.isCustomItem(inv.getBoots(), CUSTOM_ITEM_MINING_ARMOR)) return false;
        if (!ItemStackGeneratorUtils.isCustomItem(inv.getLeggings(), CUSTOM_ITEM_MINING_ARMOR)) return false;
        if (!ItemStackGeneratorUtils.isCustomItem(inv.getChestplate(), CUSTOM_ITEM_MINING_ARMOR)) return false;
        return ItemStackGeneratorUtils.isCustomItem(inv.getHelmet(), CUSTOM_ITEM_MINING_ARMOR);
    }

    public void setOres() {
        ores = Collections.unmodifiableSet(EnumSet.of(
                Material.COAL_ORE,
                Material.DEEPSLATE_COAL_ORE,
                Material.IRON_ORE,
                Material.DEEPSLATE_IRON_ORE,
                Material.COPPER_ORE,
                Material.GOLD_ORE,
                Material.DEEPSLATE_GOLD_ORE,
                Material.LAPIS_ORE,
                Material.NETHER_GOLD_ORE,
                Material.NETHER_QUARTZ_ORE,
                Material.DEEPSLATE_LAPIS_ORE,
                Material.DIAMOND_ORE,
                Material.DEEPSLATE_DIAMOND_ORE,
                Material.EMERALD_ORE,
                Material.DEEPSLATE_EMERALD_ORE,
                Material.ANCIENT_DEBRIS,
                Material.REDSTONE_ORE,
                Material.DEEPSLATE_REDSTONE_ORE,
                Material.OBSIDIAN));
    }

    public void setCommonOres() {
        commonOres = Collections.unmodifiableSet(EnumSet.of(
                Material.COAL_ORE,
                Material.DEEPSLATE_COAL_ORE,
                Material.IRON_ORE,
                Material.DEEPSLATE_IRON_ORE,
                Material.COPPER_ORE));
    }

    public void setUncommonOres() {
        uncommonOres = Collections.unmodifiableSet(EnumSet.of(
                Material.GOLD_ORE,
                Material.DEEPSLATE_GOLD_ORE,
                Material.LAPIS_ORE,
                Material.NETHER_GOLD_ORE,
                Material.NETHER_QUARTZ_ORE,
                Material.DEEPSLATE_LAPIS_ORE,
                Material.REDSTONE_ORE,
                Material.DEEPSLATE_REDSTONE_ORE));
    }

    public void setRareOres() {
        rareOres = Collections.unmodifiableSet(EnumSet.of(
                Material.DIAMOND_ORE,
                Material.DEEPSLATE_DIAMOND_ORE,
                Material.EMERALD_ORE,
                Material.DEEPSLATE_EMERALD_ORE,
                Material.ANCIENT_DEBRIS));
    }

    public void setPeacefulMobList() {
        peacefulMobList.add(EntityType.ZOMBIE);
        peacefulMobList.add(EntityType.SKELETON);
        peacefulMobList.add(EntityType.SPIDER);
        peacefulMobList.add(EntityType.CAVE_SPIDER);
        peacefulMobList.add(EntityType.ENDERMAN);
        peacefulMobList.add(EntityType.CREEPER);
        peacefulMobList.add(EntityType.SILVERFISH);
    }

    private static Set<Material> createAcceptableTools() {
        return Collections.unmodifiableSet(EnumSet.of(
                Material.WOODEN_PICKAXE,
                Material.STONE_PICKAXE,
                Material.IRON_PICKAXE,
                Material.GOLDEN_PICKAXE,
                Material.DIAMOND_PICKAXE,
                Material.NETHERITE_PICKAXE,
                Material.WOODEN_SHOVEL,
                Material.STONE_SHOVEL,
                Material.IRON_SHOVEL,
                Material.GOLDEN_SHOVEL,
                Material.DIAMOND_SHOVEL,
                Material.NETHERITE_SHOVEL,
                Material.WOODEN_AXE,
                Material.STONE_AXE,
                Material.IRON_AXE,
                Material.GOLDEN_AXE,
                Material.DIAMOND_AXE,
                Material.NETHERITE_AXE,
                Material.WOODEN_HOE,
                Material.STONE_HOE,
                Material.IRON_HOE,
                Material.GOLDEN_HOE,
                Material.DIAMOND_HOE,
                Material.NETHERITE_HOE,
                Material.SHEARS,
                Material.BUCKET,
                Material.WATER_BUCKET,
                Material.LAVA_BUCKET,
                Material.FLINT_AND_STEEL,
                Material.CLOCK,
                Material.COMPASS,
                Material.FISHING_ROD,
                Material.CARROT_ON_A_STICK,
                Material.WARPED_FUNGUS_ON_A_STICK,
                Material.SPYGLASS,
                Material.TROPICAL_FISH_BUCKET,
                Material.PUFFERFISH_BUCKET,
                Material.SALMON_BUCKET,
                Material.COD_BUCKET,
                Material.AXOLOTL_BUCKET,
                Material.ELYTRA,
                Material.RECOVERY_COMPASS,
                Material.BRUSH,
                Material.TADPOLE_BUCKET,
                Material.MILK_BUCKET,
                Material.POWDER_SNOW_BUCKET,
                Material.WIND_CHARGE,
                Material.FIREWORK_ROCKET,
                Material.TOTEM_OF_UNDYING,
                Material.BONE_MEAL,
                Material.LEAD,
                Material.FIRE_CHARGE,
                Material.WRITABLE_BOOK,
                Material.MAP,
                Material.ENDER_PEARL,
                Material.ENDER_EYE,
                Material.SHIELD,
                Material.TORCH));
    }

    public Map<Player, SpelunkerAbilitySync> getSpelunkerTracker() {
        return spelunkerTracker;
    }

    public Set<Material> getOres() {
        return ores;
    }

    public Map<Player, Boolean> getVeinminerTracker() {
        return veinminerTracker;
    }

    public Map<Player, List<Block>> getVeinTracker() {
        return veinTracker;
    }

    public ArrayList<Player> getPeacefulMiners() {
        return peacefulMiners;
    }

    public HashMap<Player, Inventory> getToolBelts() {
        return toolBelts;
    }

    // Vein miner activity flag helpers
    public void setVeinMinerActive(Player p, boolean active) {
        if (active) activeVeinMinerPlayers.add(p.getUniqueId());
        else activeVeinMinerPlayers.remove(p.getUniqueId());
    }

    public boolean isVeinMinerActive(Player p) {
        return activeVeinMinerPlayers.contains(p.getUniqueId());
    }
}
