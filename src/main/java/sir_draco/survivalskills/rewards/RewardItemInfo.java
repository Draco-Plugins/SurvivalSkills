package sir_draco.survivalskills.rewards;

import org.bukkit.inventory.ItemStack;

import sir_draco.survivalskills.skills.SkillCategory;

public record RewardItemInfo(ItemStack item, SkillCategory skillCategory, int level) {

    public boolean isItem(ItemStack item) {
        return this.item.isSimilar(item);
    }
}
