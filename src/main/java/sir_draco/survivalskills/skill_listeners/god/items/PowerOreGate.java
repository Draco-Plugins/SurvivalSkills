package sir_draco.survivalskills.skill_listeners.god.items;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.rewards.PlayerRewards;
import sir_draco.survivalskills.skills.SkillCategory;

/**
 * Shared gatekeeper logic for god items that require the "Power Ore" mining
 * reward to be unlocked before they can be used.
 */
public final class PowerOreGate {

    /** Reward key used to look up the Power Ore unlock across the skill system. */
    public static final String POWER_ORE_REWARD = "PowerOre";

    private PowerOreGate() {
        // Utility class
    }

    public static boolean hasPowerOreReward(Player player) {
        PlayerRewards rewards = SurvivalSkills.getInstance().getSkillManager().getPlayerRewards(player);
        return rewards != null
                && rewards.getReward(SkillCategory.MINING, POWER_ORE_REWARD).isApplied();
    }

    public static int getDefaultPowerOreLevel() {
        return SurvivalSkills.getInstance().getSkillManager().getDefaultPlayerRewards()
                .getReward(SkillCategory.MINING, POWER_ORE_REWARD).getLevel();
    }

    public static void sendLockedMessage(Player player, String abilityDescription) {
        player.sendRawMessage(ChatColor.RED + "Unlock power ore to use the " + abilityDescription
                + " at mining level: " + ChatColor.AQUA + getDefaultPowerOreLevel());
        player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    }
}