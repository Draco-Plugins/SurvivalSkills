package sir_draco.survivalskills.boards;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.skills.Skill;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.skills.SkillManager;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class Leaderboard {

    /**
     * Creates a new leaderboard player object, loading in the player's current skill levels
     * @param p
     * @return a LeaderboardPlayer object
     */
    public static LeaderboardPlayer createLeaderboardPlayer(Player p) {
        EnumMap<SkillCategory, Integer> scores = new EnumMap<>(SkillCategory.class);
        scores.put(SkillCategory.BUILDING, getLeaderboardScore(p, SkillCategory.BUILDING));
        scores.put(SkillCategory.CRAFTING, getLeaderboardScore(p, SkillCategory.CRAFTING));
        scores.put(SkillCategory.EXPLORING, getLeaderboardScore(p, SkillCategory.EXPLORING));
        scores.put(SkillCategory.FARMING, getLeaderboardScore(p, SkillCategory.FARMING));
        scores.put(SkillCategory.FIGHTING, getLeaderboardScore(p, SkillCategory.FIGHTING));
        scores.put(SkillCategory.FISHING, getLeaderboardScore(p, SkillCategory.FISHING));
        scores.put(SkillCategory.MINING, getLeaderboardScore(p, SkillCategory.MINING));
        scores.put(SkillCategory.MAIN, getLeaderboardScore(p, SkillCategory.MAIN));
        scores.put(SkillCategory.DEATHS, getLeaderboardScore(p, SkillCategory.DEATHS));
        scores.put(SkillCategory.SOLO_TRIALS, getLeaderboardScore(p, SkillCategory.SOLO_TRIALS));
        scores.put(SkillCategory.COOP_TRIALS, getLeaderboardScore(p, SkillCategory.COOP_TRIALS));
        return new LeaderboardPlayer(p.getDisplayName(), scores);
    }

    /**
     * Gets the score for a specific skill for a player
     * @param p
     * @param skillCategory
     * @return the score for the skill as an int
     */
    public static int getLeaderboardScore(Player p, SkillCategory skillCategory) {
        SurvivalSkills plugin = SurvivalSkills.getInstance();
        if (!plugin.getLeaderboardData().contains(p.getUniqueId().toString())) return 0;
        if (skillCategory == SkillCategory.ALL) {
            int score = 0;
            for (Skill skill : plugin.getSkillManager().getPlayerSkills().get(p.getUniqueId()).getSkills()) score += skill.getLevel();
            return score;
        }
        else if (skillCategory == SkillCategory.DEATHS) {
            return plugin.getLeaderboardData().getInt(p.getUniqueId() + ".Deaths");
        }
        else if (skillCategory == SkillCategory.SOLO_TRIALS) {
            return plugin.getLeaderboardData().getInt(p.getUniqueId() + ".SoloTrials");
        }
        else if (skillCategory == SkillCategory.COOP_TRIALS) {
            return plugin.getLeaderboardData().getInt(p.getUniqueId() + ".CoopTrials");
        }
        else {
            return SkillManager.getSkill(p.getUniqueId(), skillCategory).getLevel();
        }
    }


    /**
     * Sorts the leaderboard for a specific skill
     * @param skillCategory
     * @param limit the maximum number of players to return, set as -1 to return all players
     * @return ArrayList<String>
     */
    public static ArrayList<String> sortLeaderboard(SkillCategory skillCategory, int limit) {
        SurvivalSkills plugin = SurvivalSkills.getInstance();
        boolean isDeaths = (skillCategory == SkillCategory.DEATHS);

        ArrayList<Map.Entry<UUID, LeaderboardPlayer>> entries = new ArrayList<>(plugin.getLeaderboardTracker().entrySet());
        entries.sort((a, b) -> {
            int scoreA = a.getValue().getScore(skillCategory);
            int scoreB = b.getValue().getScore(skillCategory);
            return isDeaths ? Integer.compare(scoreA, scoreB) : Integer.compare(scoreB, scoreA);
        });

        int finalLimit = limit == -1 ? entries.size() : Math.min(entries.size(), limit);
        ArrayList<String> sorted = new ArrayList<>(finalLimit);

        for (int i = 0; i < finalLimit; i++) {
            LeaderboardPlayer player = entries.get(i).getValue();
            sorted.add(ChatColor.AQUA.toString() + (i + 1) + ". " + ChatColor.GOLD + player.getName()
                    + ChatColor.AQUA + " - " + ChatColor.GREEN + player.getScore(skillCategory));
        }
        return sorted;
    }


    /**
     * Prints the leaderboard for a skill for a specific page on the leaderboard
     * @param p the player
     * @param skillCategory the skill
     * @param page the page to display
     * @param maxPage the maximum page number
     */
    public static void printLeaderboard(Player p, SkillCategory skillCategory, int page, int maxPage) {
        int maxEntriesPerPage = 10;
        ArrayList<String> leaderboard = sortLeaderboard(skillCategory, page * maxEntriesPerPage);

        p.sendRawMessage(ChatColor.YELLOW + "Page: " + ChatColor.AQUA + page + ChatColor.YELLOW + "/" + ChatColor.AQUA + maxPage);
        printRank(p, skillCategory);
        for (int i = (page * maxEntriesPerPage) - maxEntriesPerPage; i <= (page * maxEntriesPerPage) - 1; i++) {
            if (i >= leaderboard.size()) break;
            p.sendRawMessage(leaderboard.get(i));
        }
        p.sendRawMessage(ChatColor.YELLOW + "Type " + ChatColor.AQUA + "/skills leaderboard " + skillCategory + " <page>" + ChatColor.YELLOW + " to view more pages");
        p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
    }


    /**
     * Prints the rank of a player in the leaderboard for a specific skill
     * @param p the player
     * @param skillCategory the skill
     */
    public static void printRank(Player p, SkillCategory skillCategory) {
        SurvivalSkills plugin = SurvivalSkills.getInstance();
        int skillScore = getLeaderboardScore(p, skillCategory);

        // Find the rank for the player
        boolean isDeaths = (skillCategory == SkillCategory.DEATHS);
        int rank = 1;
        for (Map.Entry<UUID, LeaderboardPlayer> entry : plugin.getLeaderboardTracker().entrySet()) {
            if (entry.getKey().equals(p.getUniqueId())) continue;
            int score = entry.getValue().getScore(skillCategory);
            if (isDeaths ? score < skillScore : score > skillScore) rank++;
        }
        p.sendRawMessage(ChatColor.GREEN + "You are currently ranked " + ChatColor.GOLD + rank + ChatColor.GREEN + " in "
                + ChatColor.GOLD + skillCategory + ChatColor.GREEN + " out of " + ChatColor.GOLD + plugin.getLeaderboardTracker().size() + ChatColor.GREEN + "!");
    }


    /**
     * Initializes the leaderboard for a player
     * @param p the player
     * @return leaderboardPlayer A LeaderboardPlayer instance
     */
    public static LeaderboardPlayer initializeLeaderboardForPlayer(Player p) {
        SurvivalSkills plugin = SurvivalSkills.getInstance();
        LeaderboardPlayer leaderboardPlayer = plugin.getLeaderboardTracker().get(p.getUniqueId());
        if (leaderboardPlayer == null) {
            leaderboardPlayer = createLeaderboardPlayer(p);
            plugin.getLeaderboardTracker().put(p.getUniqueId(), leaderboardPlayer);
        }

        return leaderboardPlayer;
    }
}
