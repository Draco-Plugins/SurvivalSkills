package sir_draco.survivalskills.utils;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.skills.Skill;

public final class SkillDisplay {

    private SkillDisplay() {}

    public static void printStats(Player p, Skill skill, boolean isPlayer) {
        printStats(p, skill, isPlayer, Skill.MAX_LEVEL);
    }

    public static void printStats(Player p, Skill skill, boolean isPlayer, int maxLevel) {
        p.sendRawMessage(ChatColor.AQUA.toString() + ChatColor.BOLD + skill.getSkillCategory() + ChatColor.WHITE + ":");
        p.sendRawMessage(ChatColor.WHITE + "Level: " + ChatColor.GREEN + skill.getLevel());
        if (!isPlayer) {
            p.sendRawMessage("");
            return;
        }

        int soFar = skill.getExperienceSoFarInLevel();
        int total = skill.getRawExperienceForNextLevel();
        String percentString = String.format("%.2f", (double) soFar / total * 100);
        String xp = ChatColor.YELLOW.toString() + soFar + ChatColor.WHITE + "/" + ChatColor.YELLOW + total;
        if (skill.getLevel() == Skill.MAX_LEVEL) {
            p.sendRawMessage(ChatColor.WHITE + "Experience: " + ChatColor.YELLOW + "MAX");
        } else if (skill.getLevel() >= maxLevel) {
            p.sendRawMessage(ChatColor.WHITE + "Experience: " + ChatColor.LIGHT_PURPLE
                    + "You have hit your level cap!");
        } else if (soFar == 0) {
            p.sendRawMessage(ChatColor.WHITE + "Experience: " + ChatColor.LIGHT_PURPLE
                    + "Interact with this skill first!");
        } else {
            p.sendRawMessage(ChatColor.WHITE + "Experience: " + xp + ChatColor.WHITE + " ("
                    + ChatColor.LIGHT_PURPLE + percentString + "%" + ChatColor.WHITE + ")");
        }
        p.sendRawMessage("");
    }

    public static void levelUpNotification(Player p, Skill skill) {
        p.sendRawMessage(ChatColor.GREEN + "Skill " + ChatColor.AQUA + skill.getSkillCategory().getDisplayName()
                + ChatColor.GREEN + " has leveled up to level: " + ChatColor.GOLD + skill.getLevel());
        p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
    }
}
