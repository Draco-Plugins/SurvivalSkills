package sir_draco.survivalskills.skill_listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.skills.SkillManager;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.EnumSet;
import java.util.Set;

public class CraftingSkill implements Listener {

    private static final int FURNACE_RESULT_SLOT = 2;
    private static final int STONECUTTER_RESULT_SLOT = 1;
    private static final Set<InventoryAction> RESULT_REMOVAL_ACTIONS = Set.of(
            InventoryAction.PICKUP_ALL,
            InventoryAction.PICKUP_SOME,
            InventoryAction.PICKUP_HALF,
            InventoryAction.PICKUP_ONE,
            InventoryAction.MOVE_TO_OTHER_INVENTORY,
            InventoryAction.HOTBAR_MOVE_AND_READD,
            InventoryAction.HOTBAR_SWAP,
            InventoryAction.DROP_ALL_SLOT,
            InventoryAction.DROP_ONE_SLOT);

    private static final int MAX_STACK_SIZE = 64;

    private final SurvivalSkills plugin;
    private final Set<Material> disallowedCraftingSkillMaterials = EnumSet.of(
            Material.DIAMOND, Material.DIAMOND_BLOCK,
            Material.EMERALD, Material.EMERALD_BLOCK,
            Material.GOLD_INGOT, Material.GOLD_BLOCK, Material.GOLD_NUGGET, Material.RAW_GOLD_BLOCK,
            Material.IRON_INGOT, Material.IRON_BLOCK, Material.IRON_NUGGET, Material.RAW_IRON_BLOCK,
            Material.NETHERITE_INGOT, Material.NETHERITE_BLOCK,
            Material.LAPIS_LAZULI, Material.LAPIS_BLOCK,
            Material.REDSTONE, Material.REDSTONE_BLOCK,
            Material.COAL, Material.COAL_BLOCK,
            Material.COPPER_INGOT, Material.COPPER_BLOCK, Material.RAW_COPPER_BLOCK, Material.RAW_COPPER,
            Material.QUARTZ,
            Material.RAW_IRON, Material.RAW_GOLD,
            Material.BONE_MEAL, Material.BONE_BLOCK,
            Material.GLOWSTONE, Material.GLOWSTONE_DUST,
            Material.HAY_BLOCK,
            Material.WHEAT
    );

    public CraftingSkill(SurvivalSkills plugin) {
        this.plugin = plugin;
    }

    @EventHandler (ignoreCancelled = true)
    public void onCraftEvent(CraftItemEvent e) {
        if (e.getClickedInventory() == null) return;
        Player p = (Player) e.getWhoClicked();

        // Do not consume the dragon egg when crafting
        if (e.getClickedInventory().contains(Material.DRAGON_EGG)) {
            p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.DRAGON_EGG));
        }

        if (e.getClick().isShiftClick()) {
            int smallestStack = MAX_STACK_SIZE;
            for (ItemStack item : e.getClickedInventory().getContents()) {
                if (item != null && item.getAmount() < smallestStack && item.getAmount() != 0 
                    && !item.getType().equals(e.getRecipe().getResult().getType()))  {
                        smallestStack = item.getAmount();
                }
            }

            SkillManager.experienceEvent(plugin, p, plugin.getSkillManager().getCraftingXP() * smallestStack, SkillCategory.CRAFTING);
            handleCraftingSkills(p, e.getClickedInventory().getContents(), smallestStack, e.getRecipe().getResult());
            return;
        }

        SkillManager.experienceEvent(plugin, p, plugin.getSkillManager().getCraftingXP(), SkillCategory.CRAFTING);
        if (noSpaceOnCursor(e.getCursor(), e.getRecipe().getResult(), e.getClick())) return;
        handleCraftingSkills(p, e.getClickedInventory().getContents(), 1, e.getRecipe().getResult());
    }

    @EventHandler (ignoreCancelled = true)
    public void onProcessingResultTaken(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !RESULT_REMOVAL_ACTIONS.contains(e.getAction())) return;

        InventoryType inventoryType = e.getView().getTopInventory().getType();
        if (e.getRawSlot() != resultSlot(inventoryType)) return;

        ItemStack result = e.getCurrentItem();
        if (result == null || result.getType().isAir()) return;

        Inventory topInventory = e.getView().getTopInventory();
        if (inventoryType == InventoryType.STONECUTTER && e.isShiftClick()) {
            int initialInputAmount = itemAmount(topInventory.getItem(0));
            int resultAmount = result.getAmount();
            plugin.getServer().getScheduler().runTask(plugin,
                    () -> awardStonecutterShiftClickXp(p, topInventory, initialInputAmount, resultAmount));
            return;
        }

        awardCraftingXp(p, result.getAmount());
    }

    private void awardStonecutterShiftClickXp(Player player, Inventory stonecutter,
                                              int initialInputAmount, int resultAmount) {
        int craftedAmount = initialInputAmount - itemAmount(stonecutter.getItem(0));
        awardCraftingXp(player, craftedAmount * resultAmount);
    }

    private void awardCraftingXp(Player player, int itemAmount) {
        if (itemAmount <= 0) return;
        SkillManager.experienceEvent(plugin, player,
                plugin.getSkillManager().getCraftingXP() * itemAmount, SkillCategory.CRAFTING);
    }

    private int itemAmount(ItemStack item) {
        return item == null || item.getType().isAir() ? 0 : item.getAmount();
    }

    private int resultSlot(InventoryType inventoryType) {
        return switch (inventoryType) {
            case STONECUTTER -> STONECUTTER_RESULT_SLOT;
            case FURNACE, BLAST_FURNACE, SMOKER -> FURNACE_RESULT_SLOT;
            default -> -1;
        };
    }

    private boolean noSpaceOnCursor(ItemStack cursor, ItemStack result, ClickType click) {
        // Shift clicks are handled by later logic
        if (click.equals(ClickType.SHIFT_LEFT)) return false;
        if (cursor == null) return false;
        if (cursor.getType().equals(Material.AIR)) return false;
        if (result == null) return false;
        if (result.getType().isAir()) return false;
        if (result.getMaxStackSize() == 1) return true;
        return cursor.getAmount() + result.getAmount() > MAX_STACK_SIZE;
    }

    private void handleCraftingSkills(Player p, ItemStack[] items, int smallestStack, ItemStack result) {
        handleMaterialsBack(p, items, smallestStack, result);
        handleExtraOutput(p, result, smallestStack);
    }

    private void handleMaterialsBack(Player p, ItemStack[] items, int smallestStack, ItemStack result) {
        for (ItemStack item : items) {
            if (item == null) continue;
            if (item.getType() == Material.SHULKER_BOX) return;
        }
        if (!canApplyCraftingEffect(result, plugin.getSkillManager().getPlayerRewards(p).getMaterialsBack())) return;
        for (ItemStack item : items) {
            if (item == null || item.equals(result)) continue;
            p.getInventory().addItem(new ItemStack(item.getType(), smallestStack));
        }
        p.sendRawMessage(ChatColor.GREEN + "You got your materials back!");
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_FLUTE, 1, 1);
    }

    private void handleExtraOutput(Player p, ItemStack result, int smallestStack) {
        if (!canApplyCraftingEffect(result, plugin.getSkillManager().getPlayerRewards(p).getExtraOutput())) return;
        ItemStack extraOutput = new ItemStack(result.getType(), result.getAmount() * smallestStack);
        if (extraOutput.getAmount() > MAX_STACK_SIZE) extraOutput.setAmount(MAX_STACK_SIZE);
        p.getInventory().addItem(extraOutput);
        p.sendRawMessage(ChatColor.GREEN + "Your crafting recipe was doubled!");
        p.playSound(p, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1, 1);
    }

    private boolean canApplyCraftingEffect(ItemStack result, double chance) {
        if (ItemStackGeneratorUtils.isCustomItem(result)) return false;
        if (disallowedCraftingSkillMaterials.contains(result.getType())) return false;
        if (chance == 0) return false;
        return Math.random() < chance;
    }
}
