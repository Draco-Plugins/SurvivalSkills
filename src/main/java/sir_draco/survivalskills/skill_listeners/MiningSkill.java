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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import sir_draco.survivalskills.abilities.SpelunkerAbilitySync;
import sir_draco.survivalskills.abilities.VeinMinerAsync;
import sir_draco.survivalskills.rewards.PlayerRewards;
import sir_draco.survivalskills.skills.SkillManager;
import sir_draco.survivalskills.utils.ItemStackGenerator;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.Utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MiningSkill implements Listener {

    private final SurvivalSkills plugin;
    // Use EnumSet for O(1) contains() and minimal memory footprint. They are
    // immutable after construction.
    private Set<Material> ores = EnumSet.noneOf(Material.class);
    private Set<Material> commonOres = EnumSet.noneOf(Material.class);
    private Set<Material> uncommonOres = EnumSet.noneOf(Material.class);
    private Set<Material> rareOres = EnumSet.noneOf(Material.class);
    private final ArrayList<Player> peacefulMiners = new ArrayList<>();
    private final ArrayList<EntityType> peacefulMobList = new ArrayList<>();
    private final ArrayList<Material> acceptableTools = new ArrayList<>();
    private final HashMap<Player, SpelunkerAbilitySync> spelunkerTracker = new HashMap<>();
    private final HashMap<Player, Integer> veinminerTracker = new HashMap<>(); // Integer is 0 (takes hunger) or 1
                                                                               // (doesn't)
    private final HashMap<Player, ArrayList<Block>> veinTracker = new HashMap<>();
    private final HashMap<Player, Inventory> toolBelts = new HashMap<>();
    private final int blocksPerHunger;
    private final Set<UUID> activeVeinMinerPlayers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public MiningSkill(SurvivalSkills plugin, int blocksPerHunger) {
        this.plugin = plugin;
        this.blocksPerHunger = blocksPerHunger;
        setOres();
        setCommonOres();
        setUncommonOres();
        setRareOres();
        setPeacefulMobList();
        setAcceptableTools();
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();

        if (plugin.getFarmingList().contains(e.getBlock().getType()))
            return;
        if (e.getBlock().getType().toString().contains("LOG"))
            return;

        // Handle Glowing Blocks
        removeGlow(e.getBlock());

        // Handle XP
        double multiplier = getMultiplier(e.getBlock().getType());
        double xpAmount = plugin.getSkillManager().getMiningXP() * multiplier;

        SkillManager.experienceEvent(plugin, p, xpAmount, "Mining");

        // Handle double ore chance
        doubleOre(p, e);

        // Handle veinminer
        veinminerChecker(p, e);
    }

    @EventHandler
    public void placeTorch(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);

        if (rewards == null && plugin.isCitizensEnabled())
            return;
        else if (rewards == null) {
            Bukkit.getLogger().warning("Player " + p.getName() + " does not have a PlayerRewards object");
            return;
        }

        if (!rewards.getReward("Mining", "UnlimitedTorch").isApplied()) {
            if (ItemStackGenerator.isCustomItem(p.getInventory().getItemInMainHand(), 1)
                    || ItemStackGenerator.isCustomItem(p.getInventory().getItemInOffHand(), 1)) {
                e.setCancelled(true);
                p.sendRawMessage(ChatColor.RED + "Unlimited Torch unlocks at mining level "
                        + ChatColor.AQUA + rewards.getReward("Mining", "UnlimitedTorch").getLevel());
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            }
            return;
        }

        if (e.getHand() == null) return;
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;

        if (e.getHand().equals(EquipmentSlot.OFF_HAND)
                && !ItemStackGenerator.isCustomItem(p.getInventory().getItemInOffHand(), 1))
            return;
        else if (e.getHand().equals(EquipmentSlot.HAND)
                && !ItemStackGenerator.isCustomItem(p.getInventory().getItemInMainHand(), 1))
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
        if (!desiredBlock.isEmpty() && !desiredBlock.getType().isAir()) return;
        if (e.getBlockFace().equals(BlockFace.UP) || e.getBlockFace().equals(BlockFace.DOWN)) {
            desiredBlock.setType(Material.TORCH);
            desiredBlock.getState().setType(Material.TORCH);
        } else {
            Directional torch = (Directional) Material.WALL_TORCH.createBlockData();
            torch.setFacing(e.getBlockFace());
            desiredBlock.setBlockData(torch);
        }
        desiredBlock.getState().update(true);
    }

    @EventHandler
    public void useZapWand(PlayerInteractEvent e) {
        if (e.getHand() == null)
            return;
        if (e.getHand().equals(EquipmentSlot.OFF_HAND))
            return;
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
            return;
        if (e.getClickedBlock() == null)
            return;
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!ItemStackGenerator.isCustomItem(hand, 27))
            return;
        if (!plugin.getSkillManager().getPlayerRewards(p).getReward("Mining", "ZapWand").isApplied()) {
            e.setCancelled(true);
            p.sendRawMessage(ChatColor.RED + "Zap Wand unlocks at mining level " + ChatColor.AQUA +
                    plugin.getSkillManager().getPlayerRewards(p).getReward("Mining", "ZapWand").getLevel());
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
        if (!plugin.getSkillManager().getPlayerRewards(p).isUnbreakableTools())
            return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void playerHurt(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p))
            return;
        double reductionPercentage = plugin.getSkillManager().getPlayerRewards(p).getProtectionPercentage();
        if (reductionPercentage == 0)
            return;
        double newDamage = e.getDamage() * (1 - reductionPercentage);
        e.setDamage(newDamage);
    }

    @EventHandler
    public void playerMove(PlayerMoveEvent e) {
        // Make sure they are wearing the armor
        if (!isMiningArmor(e.getPlayer().getInventory()))
            return;

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
        if (e.getLocation().getBlockY() >= 64)
            return;
        if (!peacefulMobList.contains(e.getEntityType()))
            return;
        for (Player p : peacefulMiners) {
            if (!p.getWorld().getEnvironment().equals(e.getEntity().getWorld().getEnvironment()))
                continue;
            if (p.getLocation().distance(e.getEntity().getLocation()) > 150)
                continue;
            e.setCancelled(true);
            e.getEntity().remove();
            return;
        }
    }

    @EventHandler
    public void onToolBeltClick(InventoryClickEvent e) {
        if (e.getClickedInventory() == null)
            return;
        Player p = (Player) e.getWhoClicked();
        Inventory inv = e.getClickedInventory();
        Inventory top = e.getView().getTopInventory();
        if (!toolBelts.containsKey(p))
            return;
        Inventory toolBelt = toolBelts.get(p);
        if (!toolBelt.equals(inv) && !toolBelt.equals(top))
            return;
        // Prevent hotbar swaps
        if (e.getAction().equals(InventoryAction.HOTBAR_SWAP)
                || e.getAction().equals(InventoryAction.HOTBAR_MOVE_AND_READD)) {
            e.setCancelled(true);
            return;
        }
        if (e.getCurrentItem() == null)
            return;

        // Check if the clicked item is a tool
        if (!inv.equals(top)) {
            if (!acceptableTools.contains(e.getCurrentItem().getType())) {
                p.sendRawMessage(ChatColor.RED + "You can only put tools in the tool belt");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onToolBeltDrag(InventoryDragEvent e) {
        Inventory inv = e.getInventory();
        if (!toolBelts.containsValue(inv))
            return;
        Player p = (Player) e.getWhoClicked();

        // Check if the clicked item is a tool
        if (!acceptableTools.contains(e.getOldCursor().getType())) {
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            e.setCancelled(true);
            return;
        }

        for (ItemStack item : e.getNewItems().values()) {
            if (!acceptableTools.contains(item.getType())) {
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                e.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onToolBeltClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        if (!toolBelts.containsKey(p))
            return;

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
        if (commonOres.contains(mat))
            return "common";
        else if (uncommonOres.contains(mat))
            return ChatColor.GREEN + "uncommon";
        else if (rareOres.contains(mat))
            return ChatColor.BLUE + "rare";
        else
            return "none";
    }

    public void endSpelunkerAll() {
        if (spelunkerTracker.isEmpty())
            return;
        for (Map.Entry<Player, SpelunkerAbilitySync> spelunker : spelunkerTracker.entrySet())
            spelunker.getValue().endThread();
    }

    public void removeGlow(Block block) {
        if (spelunkerTracker.isEmpty())
            return;
        for (Map.Entry<Player, SpelunkerAbilitySync> tracker : spelunkerTracker.entrySet()) {
            if (!tracker.getValue().containsBlock(block))
                continue;
            tracker.getValue().removeGlow(block);
        }
    }

    public void hideGlowForPlayer(Player p) {
        if (spelunkerTracker.isEmpty())
            return;
        for (Map.Entry<Player, SpelunkerAbilitySync> hide : spelunkerTracker.entrySet())
            hide.getValue().hideAllGlowForPlayer(p);
    }

    public void doubleOre(Player p, BlockBreakEvent e) {
        // Defensive: rewards object can theoretically be null if player data not fully
        // loaded yet.
        PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
        if (rewards == null)
            return;
        double fortuneChance = rewards.getFortuneChance();
        if (fortuneChance <= 0)
            return;
        Material brokenType = e.getBlock().getType();
        if (!ores.contains(brokenType))
            return;
        if (p.getInventory().getItemInMainHand().containsEnchantment(Enchantment.SILK_TOUCH))
            return; // Respect silk touch

        if (Math.random() >= fortuneChance)
            return; // Chance failed

        // Replace default drops with doubled stacks (capped at 64)
        e.setDropItems(false);
        for (ItemStack drop : e.getBlock().getDrops(p.getInventory().getItemInMainHand())) {
            if (drop == null || drop.getType().isAir())
                continue;
            int newAmount = Math.min(drop.getAmount() * 2, 64);
            drop.setAmount(newAmount);
            e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), drop);
        }
    }

    public void veinminerChecker(Player p, BlockBreakEvent e) {
        // Make sure the player has the ability to vein mine
        if (!veinminerTracker.containsKey(p))
            return;
        if (!p.isSneaking())
            return;
        Material material = e.getBlock().getType();
        if (!ores.contains(material))
            return;

        // Make sure this block isn't part of a previous vein mine
        if (!veinTracker.containsKey(p))
            veinTracker.put(p, new ArrayList<>());
        if (veinTracker.get(p).contains(e.getBlock())) {
            veinTracker.get(p).remove(e.getBlock());
            return;
        }

        VeinMinerAsync veinMiner = new VeinMinerAsync(plugin, p, this, e.getBlock(), material,
                blocksPerHunger);
        veinMiner.runTaskAsynchronously(plugin);
    }

    public boolean isMiningArmor(PlayerInventory inv) {
        if (!ItemStackGenerator.isCustomItem(inv.getBoots(), 3))
            return false;
        if (!ItemStackGenerator.isCustomItem(inv.getLeggings(), 3))
            return false;
        if (!ItemStackGenerator.isCustomItem(inv.getChestplate(), 3))
            return false;
        return ItemStackGenerator.isCustomItem(inv.getHelmet(), 3);
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

    public void setAcceptableTools() {
        acceptableTools.add(Material.WOODEN_PICKAXE);
        acceptableTools.add(Material.STONE_PICKAXE);
        acceptableTools.add(Material.IRON_PICKAXE);
        acceptableTools.add(Material.GOLDEN_PICKAXE);
        acceptableTools.add(Material.DIAMOND_PICKAXE);
        acceptableTools.add(Material.NETHERITE_PICKAXE);
        acceptableTools.add(Material.WOODEN_SHOVEL);
        acceptableTools.add(Material.STONE_SHOVEL);
        acceptableTools.add(Material.IRON_SHOVEL);
        acceptableTools.add(Material.GOLDEN_SHOVEL);
        acceptableTools.add(Material.DIAMOND_SHOVEL);
        acceptableTools.add(Material.NETHERITE_SHOVEL);
        acceptableTools.add(Material.WOODEN_AXE);
        acceptableTools.add(Material.STONE_AXE);
        acceptableTools.add(Material.IRON_AXE);
        acceptableTools.add(Material.GOLDEN_AXE);
        acceptableTools.add(Material.DIAMOND_AXE);
        acceptableTools.add(Material.NETHERITE_AXE);
        acceptableTools.add(Material.WOODEN_HOE);
        acceptableTools.add(Material.STONE_HOE);
        acceptableTools.add(Material.IRON_HOE);
        acceptableTools.add(Material.GOLDEN_HOE);
        acceptableTools.add(Material.DIAMOND_HOE);
        acceptableTools.add(Material.NETHERITE_HOE);
        acceptableTools.add(Material.SHEARS);
        acceptableTools.add(Material.BUCKET);
        acceptableTools.add(Material.WATER_BUCKET);
        acceptableTools.add(Material.LAVA_BUCKET);
        acceptableTools.add(Material.FLINT_AND_STEEL);
        acceptableTools.add(Material.CLOCK);
        acceptableTools.add(Material.COMPASS);
        acceptableTools.add(Material.FISHING_ROD);
        acceptableTools.add(Material.CARROT_ON_A_STICK);
        acceptableTools.add(Material.WARPED_FUNGUS_ON_A_STICK);
        acceptableTools.add(Material.SPYGLASS);
        acceptableTools.add(Material.TROPICAL_FISH_BUCKET);
        acceptableTools.add(Material.PUFFERFISH_BUCKET);
        acceptableTools.add(Material.SALMON_BUCKET);
        acceptableTools.add(Material.COD_BUCKET);
        acceptableTools.add(Material.AXOLOTL_BUCKET);
        acceptableTools.add(Material.ELYTRA);
        acceptableTools.add(Material.RECOVERY_COMPASS);
        acceptableTools.add(Material.BRUSH);
        acceptableTools.add(Material.TADPOLE_BUCKET);
        acceptableTools.add(Material.MILK_BUCKET);
        acceptableTools.add(Material.POWDER_SNOW_BUCKET);
        acceptableTools.add(Material.WIND_CHARGE);
        acceptableTools.add(Material.FIREWORK_ROCKET);
        acceptableTools.add(Material.TOTEM_OF_UNDYING);
        acceptableTools.add(Material.BONE_MEAL);
        acceptableTools.add(Material.LEAD);
        acceptableTools.add(Material.FIRE_CHARGE);
        acceptableTools.add(Material.SPYGLASS);
        acceptableTools.add(Material.WRITABLE_BOOK);
        acceptableTools.add(Material.MAP);
        acceptableTools.add(Material.ENDER_PEARL);
        acceptableTools.add(Material.ENDER_EYE);
    }

    public HashMap<Player, SpelunkerAbilitySync> getSpelunkerTracker() {
        return spelunkerTracker;
    }

    public Set<Material> getOres() {
        return ores;
    }

    public HashMap<Player, Integer> getVeinminerTracker() {
        return veinminerTracker;
    }

    public HashMap<Player, ArrayList<Block>> getVeinTracker() {
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
        if (active) {
            activeVeinMinerPlayers.add(p.getUniqueId());
        } else {
            activeVeinMinerPlayers.remove(p.getUniqueId());
        }
    }

    public boolean isVeinMinerActive(Player p) {
        return activeVeinMinerPlayers.contains(p.getUniqueId());
    }
}
