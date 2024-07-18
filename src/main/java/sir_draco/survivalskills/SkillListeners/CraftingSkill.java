package sir_draco.survivalskills.SkillListeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.inventory.ItemStack;
import sir_draco.survivalskills.ItemStackGenerator;
import sir_draco.survivalskills.Skills.Skill;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;

public class CraftingSkill implements Listener {

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

            Skill.experienceEvent(plugin, p, plugin.getSkillManager().getCraftingXP() * smallestStack, "Crafting");
            handleCraftingSkills(p, e.getClickedInventory().getContents(), smallestStack, e.getRecipe().getResult());
            return;
        }

        Skill.experienceEvent(plugin, p, plugin.getSkillManager().getCraftingXP(), "Crafting");
        if (cannotGetResult(e.getCursor(), e.getRecipe().getResult(), e.getClick())) return;
        handleCraftingSkills(p, e.getClickedInventory().getContents(), 1, e.getRecipe().getResult());
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
        if (ItemStackGenerator.isCustomItem(result)) return;
        for (ItemStack item : items) {
            if (item == null) continue;
            if (item.getType().equals(Material.SHULKER_BOX)) return;
        }
        double chance = plugin.getSkillManager().getPlayerRewards(p).getMaterialsBack();
        if (chance == 0) return;
        if (disallowedCraftingSkillMaterials.contains(result.getType())) return;
        if (Math.random() >= chance) return;
        for (ItemStack item : items) {
            if (item == null) continue;
            if (item.equals(result)) continue;
            ItemStack itemBack = new ItemStack(item.getType(), smallestStack);
            p.getInventory().addItem(itemBack);
        }
        p.sendRawMessage(ChatColor.GREEN + "You got your materials back!");
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_FLUTE, 1, 1);
    }

    public void handleExtraOutput(Player p, ItemStack result, int smallestStack) {
        if (ItemStackGenerator.isCustomItem(result)) return;
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
        disallowedCraftingSkillMaterials.add(Material.IRON_INGOT);
        disallowedCraftingSkillMaterials.add(Material.IRON_BLOCK);
        disallowedCraftingSkillMaterials.add(Material.IRON_NUGGET);
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
        disallowedCraftingSkillMaterials.add(Material.BONE_MEAL);
        disallowedCraftingSkillMaterials.add(Material.BONE_BLOCK);
        disallowedCraftingSkillMaterials.add(Material.GLOWSTONE);
        disallowedCraftingSkillMaterials.add(Material.HAY_BLOCK);
        disallowedCraftingSkillMaterials.add(Material.GLOWSTONE_DUST);
        disallowedCraftingSkillMaterials.add(Material.WHEAT);
    }
}
