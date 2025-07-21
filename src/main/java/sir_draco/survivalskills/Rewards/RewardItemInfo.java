package sir_draco.survivalskills.Rewards;

import org.bukkit.inventory.ItemStack;

public record RewardItemInfo(ItemStack item, String skillName, int level) {

    public boolean isItem(ItemStack item) {
        return this.item.isSimilar(item);
    }
}
