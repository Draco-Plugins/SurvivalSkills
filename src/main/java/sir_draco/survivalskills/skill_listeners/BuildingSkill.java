package sir_draco.survivalskills.skill_listeners;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.skills.SkillManager;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BuildingSkill implements Listener {

    private static final Map<Material, Material> WALL_FAN_TO_FAN = Map.of(
            Material.BUBBLE_CORAL_WALL_FAN, Material.BUBBLE_CORAL_FAN,
            Material.TUBE_CORAL_WALL_FAN, Material.TUBE_CORAL_FAN,
            Material.BRAIN_CORAL_WALL_FAN, Material.BRAIN_CORAL_FAN,
            Material.FIRE_CORAL_WALL_FAN, Material.FIRE_CORAL_FAN,
            Material.HORN_CORAL_WALL_FAN, Material.HORN_CORAL_FAN
    );

    private final SurvivalSkills plugin;
    private final Set<Material> bannedReturns = new HashSet<>();
    private final Set<UUID> wandPlacementPlayers = new HashSet<>();

    public BuildingSkill(SurvivalSkills plugin) {
        this.plugin = plugin;
        createBannedReturns();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        Material blockType = e.getBlock().getType();

        if (blockType.toString().contains("WALL_SIGN")) {
            return;
        }
        if (plugin.getFarmingList().contains(blockType)) {
            return;
        }
        if (!wandPlacementPlayers.contains(p.getUniqueId()) && isHoldingToolInOffHand(e, p)) {
            return;
        }

        if (!wandPlacementPlayers.contains(p.getUniqueId())) {
            SkillManager.experienceEvent(plugin, p, plugin.getSkillManager().getBuildingXP(), SkillCategory.BUILDING);
        }

        handleBlockReturn(e, p, blockType);
    }

    private void handleBlockReturn(BlockPlaceEvent e, Player p, Material blockType) {
        if (isBannedReturn(blockType)) {
            return;
        }
        double blockBackChance = plugin.getSkillManager().getPlayerRewards(p).getBlockBlackChance();
        if (blockBackChance == 0.0) {
            return;
        }
        if (Math.random() < blockBackChance) {
            ItemStack item = createReturnItem(blockType);
            if (item == null) {
                return;
            }

            // Prioritize returning items to the off hand when placing from off hand
            if (e.getHand().equals(EquipmentSlot.OFF_HAND)) {
                ItemStack offHand = p.getInventory().getItemInOffHand();
                if (offHand == null) {
                    p.getInventory().setItemInOffHand(item);
                    return;
                }
                if (offHand.getType().equals(item.getType())) {
                    return;
                }
            }

            p.getInventory().addItem(item);
        }
    }

    public void beginWandPlacement(Player player) {
        wandPlacementPlayers.add(player.getUniqueId());
    }

    public void endWandPlacement(Player player) {
        wandPlacementPlayers.remove(player.getUniqueId());
    }

    public void awardWandExperience(Player player) {
        SkillManager.experienceEvent(plugin, player, plugin.getSkillManager().getBuildingXP(), SkillCategory.BUILDING);
    }

    private boolean isHoldingToolInOffHand(BlockPlaceEvent e, Player p) {
        if (!e.getHand().equals(EquipmentSlot.OFF_HAND)) {
            return false;
        }
        Material mainHand = p.getInventory().getItemInMainHand().getType();
        return Tag.ITEMS_SHOVELS.isTagged(mainHand) || Tag.ITEMS_HOES.isTagged(mainHand);
    }

    private ItemStack createReturnItem(Material material) {
        Material mappedMaterial = WALL_FAN_TO_FAN.get(material);
        if (mappedMaterial != null) {
            return new ItemStack(mappedMaterial, 1);
        }
        if (material.isItem()) {
            return new ItemStack(material, 1);
        }
        return null;
    }

    public boolean isBannedReturn(Material material) {
        return Tag.SHULKER_BOXES.isTagged(material) || bannedReturns.contains(material);
    }

    private void createBannedReturns() {
        // Ores
        bannedReturns.addAll(Set.of(
                Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE,
                Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE,
                Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE,
                Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE,
                Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE,
                Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE,
                Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE,
                Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE,
                Material.NETHER_GOLD_ORE, Material.NETHER_QUARTZ_ORE,
                Material.ANCIENT_DEBRIS
        ));
        // Storage blocks
        bannedReturns.addAll(Set.of(
                Material.DIAMOND_BLOCK, Material.EMERALD_BLOCK,
                Material.IRON_BLOCK, Material.GOLD_BLOCK,
                Material.REDSTONE_BLOCK, Material.LAPIS_BLOCK,
                Material.COPPER_BLOCK, Material.QUARTZ_BLOCK,
                Material.COAL_BLOCK, Material.NETHERITE_BLOCK,
                Material.SLIME_BLOCK, Material.RAW_COPPER_BLOCK
        ));
        // Utility blocks
        bannedReturns.addAll(Set.of(
                Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL,
                Material.BEACON, Material.ENCHANTING_TABLE, Material.ENDER_CHEST,
                Material.DRAGON_EGG, Material.END_CRYSTAL, Material.END_PORTAL_FRAME,
                Material.BREWING_STAND, Material.SMITHING_TABLE, Material.GRINDSTONE,
                Material.STONECUTTER, Material.CARTOGRAPHY_TABLE, Material.FLETCHING_TABLE,
                Material.SMOKER, Material.BLAST_FURNACE, Material.FURNACE,
                Material.COMPOSTER, Material.CRAFTING_TABLE, Material.BARREL,
                Material.BELL, Material.CAMPFIRE, Material.SOUL_CAMPFIRE,
                Material.TORCH, Material.WALL_TORCH, Material.FARMLAND
        ));
        // Special
        bannedReturns.addAll(Set.of(
                Material.ARMOR_STAND, Material.WITHER_SKELETON_SKULL,
                Material.PISTON_HEAD, Material.MOVING_PISTON,
                Material.COPPER_INGOT
        ));
    }
}
