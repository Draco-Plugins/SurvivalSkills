package sir_draco.survivalskills.SkillListeners;

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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import sir_draco.survivalskills.Abilities.SpelunkerAbilitySync;
import sir_draco.survivalskills.ItemStackGenerator;
import sir_draco.survivalskills.Skill;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MiningSkill implements Listener {

    private final SurvivalSkills plugin;
    private final ArrayList<Material> ores = new ArrayList<>();
    private final ArrayList<Material> commonOres = new ArrayList<>();
    private final ArrayList<Material> uncommonOres = new ArrayList<>();
    private final ArrayList<Material> rareOres = new ArrayList<>();
    private final HashMap<Player, SpelunkerAbilitySync> spelunkerTracker = new HashMap<>();
    private final HashMap<Player, Integer> veinminerTracker = new HashMap<>(); // Integer is 0 (takes hunger) or 1 (doesn't)
    private final HashMap<Player, ArrayList<Block>> veinTracker = new HashMap<>();
    private final ArrayList<Player> peacefulMiners = new ArrayList<>();
    private final ArrayList<EntityType> peacefulMobList = new ArrayList<>();
    private final int blocksPerHunger;

    private double xp; // XP per block mined

    public MiningSkill(SurvivalSkills plugin, double xp, int blocksPerHunger) {
        this.plugin = plugin;
        this.xp = xp;
        this.blocksPerHunger = blocksPerHunger;
        setOres();
        setCommonOres();
        setUncommonOres();
        setRareOres();
        setPeacefulMobList();
    }

    @EventHandler (ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();

        if (plugin.getFarmingList().contains(e.getBlock().getType())) return;
        if (e.getBlock().getType().toString().contains("LOG")) return;

        // Handle Glowing Blocks
        removeGlow(e.getBlock());

        // Handle XP
        double multiplier = getMultiplier(e.getBlock().getType());
        Skill.experienceEvent(plugin, p, xp * multiplier, "Mining");

        // Handle double ore chance
        doubleOre(p, e);

        // Handle veinminer
        veinminerChecker(p, e);
    }

    @EventHandler
    public void placeTorch(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!plugin.getPlayerRewards(p).getReward("Mining", "UnlimitedTorch").isApplied() && !p.hasPermission("survivalskills.op")) {
            if (ItemStackGenerator.isCustomItem(p.getInventory().getItemInMainHand(), 1) || ItemStackGenerator.isCustomItem(p.getInventory().getItemInOffHand(), 1)) {
                e.setCancelled(true);
                p.sendRawMessage(ChatColor.RED + "You are not a high enough level to use this item");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            }
            return;
        }

        if (e.getHand() == null) return;
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;

        if (e.getHand().equals(EquipmentSlot.OFF_HAND) && !ItemStackGenerator.isCustomItem(p.getInventory().getItemInOffHand(), 1)) return;
        else if (e.getHand().equals(EquipmentSlot.HAND) && !ItemStackGenerator.isCustomItem(p.getInventory().getItemInMainHand(), 1)) return;
        e.setCancelled(true);

        if (e.getClickedBlock() == null) return;

        // Check if it is in a claim
        if (plugin.isGriefPreventionEnabled() && plugin.checkForClaim(p, e.getClickedBlock().getLocation())) return;

        // Place torch if possible
        Block desiredBlock = e.getClickedBlock().getRelative(e.getBlockFace());
        if (!desiredBlock.isEmpty() && !desiredBlock.getType().isAir()) return;
        if (e.getBlockFace().equals(BlockFace.UP) || e.getBlockFace().equals(BlockFace.DOWN)) {
            desiredBlock.setType(Material.TORCH);
            desiredBlock.getState().setType(Material.TORCH);
        }
        else {
            Directional torch = (Directional) Material.WALL_TORCH.createBlockData();
            torch.setFacing(e.getBlockFace());
            desiredBlock.setBlockData(torch);
        }
        desiredBlock.getState().update(true);
    }

    @EventHandler
    public void toolDamage(PlayerItemDamageEvent e) {
        Player p = e.getPlayer();
        if (!plugin.getPlayerRewards(p).isUnbreakableTools()) return;
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void playerHurt(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        double reductionPercentage = plugin.getPlayerRewards(p).getProtectionPercentage();
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
        if (loc.getWorld() == null || !loc.getWorld().getEnvironment().equals(World.Environment.NORMAL) || loc.getBlockY() >= 64) return;
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

    public double getMultiplier(Material mat) {
        switch (mat) {
            case DEEPSLATE:
                return 2.0;
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE:
            case COPPER_ORE:
            case DEEPSLATE_COPPER_ORE:
                return 3.0;
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE:
            case NETHER_QUARTZ_ORE:
            case NETHER_GOLD_ORE:
                return 4.0;
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE:
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE:
                return 5.0;
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE:
                return 7.0;
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
                return 10.0;
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
            case ANCIENT_DEBRIS:
                return 20.0;
            case OBSIDIAN:
                return 30.0;
            default:
                return 1.0;
        }
    }

    public void setOres() {
        ores.add(Material.COAL_ORE);
        ores.add(Material.DEEPSLATE_COAL_ORE);
        ores.add(Material.IRON_ORE);
        ores.add(Material.DEEPSLATE_IRON_ORE);
        ores.add(Material.COPPER_ORE);
        ores.add(Material.GOLD_ORE);
        ores.add(Material.DEEPSLATE_GOLD_ORE);
        ores.add(Material.LAPIS_ORE);
        ores.add(Material.NETHER_GOLD_ORE);
        ores.add(Material.NETHER_QUARTZ_ORE);
        ores.add(Material.DEEPSLATE_LAPIS_ORE);
        ores.add(Material.DIAMOND_ORE);
        ores.add(Material.DEEPSLATE_DIAMOND_ORE);
        ores.add(Material.EMERALD_ORE);
        ores.add(Material.DEEPSLATE_EMERALD_ORE);
        ores.add(Material.ANCIENT_DEBRIS);
        ores.add(Material.REDSTONE_ORE);
        ores.add(Material.DEEPSLATE_REDSTONE_ORE);
        ores.add(Material.OBSIDIAN);
    }

    public void setCommonOres() {
        commonOres.add(Material.COAL_ORE);
        commonOres.add(Material.DEEPSLATE_COAL_ORE);
        commonOres.add(Material.IRON_ORE);
        commonOres.add(Material.DEEPSLATE_IRON_ORE);
        commonOres.add(Material.COPPER_ORE);
    }

    public void setUncommonOres() {
        uncommonOres.add(Material.GOLD_ORE);
        uncommonOres.add(Material.DEEPSLATE_GOLD_ORE);
        uncommonOres.add(Material.LAPIS_ORE);
        uncommonOres.add(Material.NETHER_GOLD_ORE);
        uncommonOres.add(Material.NETHER_QUARTZ_ORE);
        uncommonOres.add(Material.DEEPSLATE_LAPIS_ORE);
        uncommonOres.add(Material.REDSTONE_ORE);
        uncommonOres.add(Material.DEEPSLATE_REDSTONE_ORE);
    }

    public void setRareOres() {
        rareOres.add(Material.DIAMOND_ORE);
        rareOres.add(Material.DEEPSLATE_DIAMOND_ORE);
        rareOres.add(Material.EMERALD_ORE);
        rareOres.add(Material.DEEPSLATE_EMERALD_ORE);
        rareOres.add(Material.ANCIENT_DEBRIS);
    }

    public HashMap<Player, SpelunkerAbilitySync> getSpelunkerTracker() {
        return spelunkerTracker;
    }

    public ArrayList<Material> getOres() {
        return ores;
    }

    public String getOreTeam(Material mat) {
        if (commonOres.contains(mat)) return "common";
        else if (uncommonOres.contains(mat)) return ChatColor.GREEN + "uncommon";
        else if (rareOres.contains(mat)) return ChatColor.BLUE + "rare";
        else return "none";
    }

    public void endSpelunkerAll() {
        if (spelunkerTracker.isEmpty()) return;
        for (Map.Entry<Player, SpelunkerAbilitySync> spelunker : spelunkerTracker.entrySet()) spelunker.getValue().endThread();
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
        for (Map.Entry<Player, SpelunkerAbilitySync> hide : spelunkerTracker.entrySet()) hide.getValue().hideAllGlowForPlayer(p);
    }

    public HashMap<Player, Integer> getVeinminerTracker() {
        return veinminerTracker;
    }

    public void doubleOre(Player p, BlockBreakEvent e) {
        if (plugin.getPlayerRewards(p).getFortuneChance() == 0) return;
        if (!ores.contains(e.getBlock().getType())) return;
        if (p.getInventory().getItemInMainHand().containsEnchantment(Enchantment.SILK_TOUCH)) return;
        if (e.getBlock().getType().equals(Material.ANCIENT_DEBRIS)) return;
        double chance = Math.random();
        if (chance >= plugin.getPlayerRewards(p).getFortuneChance()) return;
        e.setDropItems(false);
        ItemStack[] drops = e.getBlock().getDrops(p.getInventory().getItemInMainHand()).toArray(new ItemStack[0]);
        for (ItemStack drop : drops) {
            int amount = drop.getAmount() * 2;
            if (amount > 64) amount = 64;
            drop.setAmount(amount);
            e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(), drop);
        }
    }

    public void veinminerChecker(Player p, BlockBreakEvent e) {
        // Make sure the player has the ability to vein mine
        if (!veinminerTracker.containsKey(p)) return;
        if (!p.isSneaking()) return;
        if (!ores.contains(e.getBlock().getType())) return;

        // Make sure this block isn't part of a previous vein mine
        if (!veinTracker.containsKey(p)) veinTracker.put(p, new ArrayList<>());
        if (veinTracker.get(p).contains(e.getBlock())) {
            veinTracker.get(p).remove(e.getBlock());
            return;
        }

        // Get the blocks in the vein and remove hunger appropriately
        ArrayList<Block> blocks = getVeinBlocks(e.getBlock());
        ArrayList<Block> eventBlockTrackingList = new ArrayList<>(blocks);
        veinTracker.put(p, eventBlockTrackingList);
        if (veinminerTracker.get(p) == 0) {
            int food = p.getFoodLevel();
            int newFood = food - (blocks.size() / blocksPerHunger);
            if (newFood < 0) {
                p.sendRawMessage(ChatColor.RED + "You don't have enough hunger to mine the whole ore vein with");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                veinTracker.remove(p);
                return;
            }
            p.setFoodLevel(newFood);
        }

        // Break all the blocks in the vein
        ItemStack pickaxe = p.getInventory().getItemInMainHand();
        for (Block block : blocks) {
            BlockBreakEvent event = new BlockBreakEvent(block, p);
            Bukkit.getServer().getPluginManager().callEvent(event);
            if (!event.isCancelled()) block.breakNaturally(pickaxe);
        }
    }

    public ArrayList<Block> getVeinBlocks(Block startBlock) {
        ArrayList<Block> blocks = new ArrayList<>();
        blocks.add(startBlock);
        int iterations = 0;
        getVeinBlockHelper(startBlock.getType(), startBlock, new ArrayList<>(), blocks, iterations);
        blocks.remove(startBlock);
        return blocks;
    }

    public ArrayList<Block> getVeinBlockHelper(Material type, Block startBlock, ArrayList<Block> checkedBlocks, ArrayList<Block> blocks, int iterations) {
        iterations++;
        if (iterations > 10000 || blocks.size() >= 100) return blocks;
        Block left = startBlock.getRelative(-1, 0, 0);
        Block right = startBlock.getRelative(1, 0, 0);
        Block front = startBlock.getRelative(0, 0, 1);
        Block back = startBlock.getRelative(0, 0, -1);
        Block up = startBlock.getRelative(0, 1, 0);
        Block down = startBlock.getRelative(0, -1, 0);

        if (type.equals(left.getType()) && !blocks.contains(left)) blocks.add(left);
        if (type.equals(right.getType()) && !blocks.contains(right)) blocks.add(right);
        if (type.equals(front.getType()) && !blocks.contains(front)) blocks.add(front);
        if (type.equals(back.getType()) && !blocks.contains(back)) blocks.add(back);
        if (type.equals(up.getType()) && !blocks.contains(up)) blocks.add(up);
        if (type.equals(down.getType()) && !blocks.contains(down)) blocks.add(down);

        checkedBlocks.add(startBlock);
        if (checkedBlocks.size() == blocks.size()) return blocks;
        for (Block block : blocks) {
            if (checkedBlocks.contains(block)) continue;
            return getVeinBlockHelper(type, block, checkedBlocks, blocks, iterations);
        }
        return blocks;
    }

    public boolean isMiningArmor(PlayerInventory inv) {
        if (!ItemStackGenerator.isCustomItem(inv.getBoots(), 3)) return false;
        if (!ItemStackGenerator.isCustomItem(inv.getLeggings(), 3)) return false;
        if (!ItemStackGenerator.isCustomItem(inv.getChestplate(), 3)) return false;
        return ItemStackGenerator.isCustomItem(inv.getHelmet(), 3);
    }

    public ArrayList<Player> getPeacefulMiners() {
        return peacefulMiners;
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

    public void setXp(double xp) {
        this.xp = xp;
    }
}
