package sir_draco.survivalskills.pipes;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.rewards.PlayerRewards;
import sir_draco.survivalskills.rewards.Reward;
import sir_draco.survivalskills.skills.SkillCategory;

public final class PipeRewardGate {
    public static final String REWARD_NAME = "ItemTransferPipes";
    public static final int DEFAULT_UNLOCK_LEVEL = 38;

    private PipeRewardGate() {
        // Utility class
    }

    public static boolean isUnlocked(SurvivalSkills plugin, Player player) {
        PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(player);
        if (rewards == null) return false;
        Reward reward = rewards.getReward(SkillCategory.CRAFTING, REWARD_NAME);
        return reward != null && reward.isApplied();
    }

    public static void sendLockedMessage(SurvivalSkills plugin, Player player) {
        Reward reward = plugin.getSkillManager().getDefaultPlayerRewards()
                .getReward(SkillCategory.CRAFTING, REWARD_NAME);
        int level = reward == null ? DEFAULT_UNLOCK_LEVEL : reward.getLevel();
        player.sendRawMessage(ChatColor.RED + "Item Transfer Pipes unlock at crafting level "
                + ChatColor.AQUA + level + ChatColor.RED + ".");
        player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    }
}
