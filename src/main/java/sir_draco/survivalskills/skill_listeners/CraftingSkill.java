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

import java.util.ArrayList;
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

    private final SurvivalSkills plugin;
    private final ArrayList<Material> disallowedCraftingSkillMaterials = new ArrayList<>();

    public CraftingSkill(SurvivalSkills plugin) {
        this.plugin = plugin;
        createDisallowedCraftingSkillMaterials();
    }

    @EventHandler (ignoreCancelled = true)
    public void onCraftEvent(CraftItemEvent e) {
        if (e.getClickedInventory() == null) return;
        Player p = (Player) e.getWhoClicked();

        if (e.getClickedInventory().contains(Material.DRAGON_EGG)) {
            p.getWorld().dropItem(p.getLocation(), new ItemStack(Material.DRAGON_EGG));
        }

        if (e.getClick().isShiftClick()) {
            int smallestStack = 64;
            for (ItemStack item : e.getClickedInventory().getContents())
                if (item.getAmount() < smallestStack && item.getAmount() != 0 && !item.getType().equals(e.getRecipe().getResult().getType())) smallestStack = item.getAmount();

            SkillManager.experienceEvent(plugin, p, plugin.getSkillManager().getCraftingXP() * smallestStack, SkillCategory.CRAFTING);
            handleCraftingSkills(p, e.getClickedInventory().getContents(), smallestStack, e.getRecipe().getResult());
            return;
        }

        SkillManager.experienceEvent(plugin, p, plugin.getSkillManager().getCraftingXP(), SkillCategory.CRAFTING);
        if (cannotGetResult(e.getCursor(), e.getRecipe().getResult(), e.getClick())) return;
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

    public boolean cannotGetResult(ItemStack cursor, ItemStack result, ClickType click) {
        if (click.equals(ClickType.SHIFT_LEFT)) return false;
        if (cursor == null) return false;
        if (cursor.getType().equals(Material.AIR)) return false;
        if (result == null) return false;
        if (result.getType().isAir()) return false;
        if (result.getMaxStackSize() == 1) return true;
        return cursor.getAmount() + result.getAmount() > 64;
    }

    public void handleCraftingSkills(Player p, ItemStack[] items, int smallestStack, ItemStack result) {
        handleMaterialsBack(p, items, smallestStack, result);
        handleExtraOutput(p, result, smallestStack);
    }

    public void handleMaterialsBack(Player p, ItemStack[] items, int smallestStack, ItemStack result) {
        if (ItemStackGeneratorUtils.isCustomItem(result)) return;
        for (ItemStack item : items) {
            if (item == null) continue;
            if (item.getType().equals(Material.SHULKER_BOX)) return;
        }
        double chance = plugin.getSkillManager().getPlayerRewards(p).getMaterialsBack();
        if (chance == 0) return;
        if (disallowedCraftingSkillMaterials.contains(result.getType())) return;
        if (Math.random() >= chance) return;
        for (ItemStack item : items) {
            if (item == null || item.equals(result)) continue;
            ItemStack itemBack = new ItemStack(item.getType(), smallestStack);
            p.getInventory().addItem(itemBack);
        }
        p.sendRawMessage(ChatColor.GREEN + "You got your materials back!");
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_FLUTE, 1, 1);
    }

    public void handleExtraOutput(Player p, ItemStack result, int smallestStack) {
        if (ItemStackGeneratorUtils.isCustomItem(result)) return;
        double chance = plugin.getSkillManager().getPlayerRewards(p).getExtraOutput();
        if (chance == 0) return;
        if (disallowedCraftingSkillMaterials.contains(result.getType())) return;
        if (Math.random() >= chance) return;
        ItemStack extraOutput = new ItemStack(result.getType(), result.getAmount() * smallestStack);
        if (extraOutput.getAmount() > 64) extraOutput.setAmount(64);
        p.getInventory().addItem(extraOutput);
        p.sendRawMessage(ChatColor.GREEN + "Your crafting recipe was doubled!");
        p.playSound(p, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1, 1);
    }

    public void createDisallowedCraftingSkillMaterials() {
        disallowedCraftingSkillMaterials.add(Material.DIAMOND);
        disallowedCraftingSkillMaterials.add(Material.DIAMOND_BLOCK);
        disallowedCraftingSkillMaterials.add(Material.EMERALD);
        disallowedCraftingSkillMaterials.add(Material.EMERALD_BLOCK);
        disallowedCraftingSkillMaterials.add(Material.GOLD_INGOT);
        disallowedCraftingSkillMaterials.add(Material.GOLD_BLOCK);
        disallowedCraftingSkillMaterials.add(Material.GOLD_NUGGET);
        disallowedCraftingSkillMaterials.add(Material.RAW_GOLD_BLOCK);
        disallowedCraftingSkillMaterials.add(Material.IRON_INGOT);
        disallowedCraftingSkillMaterials.add(Material.IRON_BLOCK);
        disallowedCraftingSkillMaterials.add(Material.IRON_NUGGET);
        disallowedCraftingSkillMaterials.add(Material.RAW_IRON_BLOCK);
        disallowedCraftingSkillMaterials.add(Material.NETHERITE_INGOT);
        disallowedCraftingSkillMaterials.add(Material.NETHERITE_BLOCK);
        disallowedCraftingSkillMaterials.add(Material.LAPIS_LAZULI);
        disallowedCraftingSkillMaterials.add(Material.LAPIS_BLOCK);
        disallowedCraftingSkillMaterials.add(Material.REDSTONE);
        disallowedCraftingSkillMaterials.add(Material.REDSTONE_BLOCK);
        disallowedCraftingSkillMaterials.add(Material.COAL);
        disallowedCraftingSkillMaterials.add(Material.COAL_BLOCK);
        disallowedCraftingSkillMaterials.add(Material.COPPER_INGOT);
        disallowedCraftingSkillMaterials.add(Material.COPPER_BLOCK);
        disallowedCraftingSkillMaterials.add(Material.RAW_COPPER_BLOCK);
        disallowedCraftingSkillMaterials.add(Material.RAW_COPPER);
        disallowedCraftingSkillMaterials.add(Material.QUARTZ);
        disallowedCraftingSkillMaterials.add(Material.RAW_IRON);
        disallowedCraftingSkillMaterials.add(Material.RAW_GOLD);
        disallowedCraftingSkillMaterials.add(Material.BONE_MEAL);
        disallowedCraftingSkillMaterials.add(Material.BONE_BLOCK);
        disallowedCraftingSkillMaterials.add(Material.GLOWSTONE);
        disallowedCraftingSkillMaterials.add(Material.HAY_BLOCK);
        disallowedCraftingSkillMaterials.add(Material.GLOWSTONE_DUST);
        disallowedCraftingSkillMaterials.add(Material.WHEAT);
    }
}
