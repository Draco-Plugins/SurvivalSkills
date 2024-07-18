package sir_draco.survivalskills.Rewards;

import org.bukkit.inventory.ItemStack;

public class RewardItemInfo {

    private final ItemStack item;
    private final String skillName;
    private final int level;

    public RewardItemInfo(ItemStack item, String skillName, int level) {
        this.item = item;
        this.skillName = skillName;
        this.level = level;
    }

    public ItemStack getItem() {
        return item;
    }

    public String getSkillName() {
        return skillName;
    }

    public int getLevel() {
        return level;
    }

    public boolean isItem(ItemStack item) {
        return this.item.isSimilar(item);
    }
}
