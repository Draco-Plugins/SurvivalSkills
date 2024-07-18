package sir_draco.survivalskills.Boards;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import sir_draco.survivalskills.Rewards.PlayerRewards;
import sir_draco.survivalskills.Skills.Skill;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Leaderboard {

    public static LeaderboardPlayer createLeaderboardPlayer(SurvivalSkills plugin, Player p) {
        int score = getLeaderboardScore(plugin, p, "All");
        int buildingScore = getLeaderboardScore(plugin, p, "Building");
        int craftingScore = getLeaderboardScore(plugin, p, "Crafting");
        int exploringScore = getLeaderboardScore(plugin, p, "Exploring");
        int farmingScore = getLeaderboardScore(plugin, p, "Farming");
        int fightingScore = getLeaderboardScore(plugin, p, "Fighting");
        int fishingScore = getLeaderboardScore(plugin, p, "Fishing");
        int miningScore = getLeaderboardScore(plugin, p, "Mining");
        int mainScore = getLeaderboardScore(plugin, p, "Main");
        int deathScore = getLeaderboardScore(plugin, p, "Deaths");

        return new LeaderboardPlayer(p.getDisplayName(), score, buildingScore, craftingScore, exploringScore,
                farmingScore, fightingScore, fishingScore, miningScore, mainScore, deathScore);
    }

    public static int getLeaderboardScore(SurvivalSkills plugin, Player p, String skillName) {
        if (skillName.equalsIgnoreCase("All")) {
            int score = 0;
            if (!plugin.getSkillManager().getPlayerSkills().containsKey(p.getUniqueId())) return score;
            for (Skill skill : plugin.getSkillManager().getPlayerSkills().get(p.getUniqueId())) score += skill.getLevel();
            return score;
        }
        else if (skillName.equals("Building")) {
            int score = 0;
            if (!plugin.getSkillManager().getPlayerSkills().containsKey(p.getUniqueId())) return score;
            return plugin.getSkillManager().getSkill(p.getUniqueId(), "Building").getLevel();
        }
        else if (skillName.equals("Crafting")) {
            int score = 0;
            if (!plugin.getSkillManager().getPlayerSkills().containsKey(p.getUniqueId())) return score;
            return plugin.getSkillManager().getSkill(p.getUniqueId(), "Crafting").getLevel();
        }
        else if (skillName.equals("Exploring")) {
            int score = 0;
            if (!plugin.getSkillManager().getPlayerSkills().containsKey(p.getUniqueId())) return score;
            return plugin.getSkillManager().getSkill(p.getUniqueId(), "Exploring").getLevel();
        }
        else if (skillName.equals("Farming")) {
            int score = 0;
            if (!plugin.getSkillManager().getPlayerSkills().containsKey(p.getUniqueId())) return score;
            return plugin.getSkillManager().getSkill(p.getUniqueId(), "Farming").getLevel();
        }
        else if (skillName.equals("Mining")) {
            int score = 0;
            if (!plugin.getSkillManager().getPlayerSkills().containsKey(p.getUniqueId())) return score;
            return plugin.getSkillManager().getSkill(p.getUniqueId(), "Mining").getLevel();
        }
        else if (skillName.equals("Fighting")) {
            int score = 0;
            if (!plugin.getSkillManager().getPlayerSkills().containsKey(p.getUniqueId())) return score;
            return plugin.getSkillManager().getSkill(p.getUniqueId(), "Fighting").getLevel();
        }
        else if (skillName.equals("Fishing")) {
            int score = 0;
            if (!plugin.getSkillManager().getPlayerSkills().containsKey(p.getUniqueId())) return score;
            return plugin.getSkillManager().getSkill(p.getUniqueId(), "Fishing").getLevel();
        }
        else if (skillName.equals("Main")) {
            int score = 0;
            if (!plugin.getSkillManager().getPlayerSkills().containsKey(p.getUniqueId())) return score;
            return plugin.getSkillManager().getSkill(p.getUniqueId(), "Main").getLevel();
        }
        else if (skillName.equalsIgnoreCase("Deaths")) {
            if (plugin.getLeaderboardData().get(p.getUniqueId().toString()) == null) {
                ScoreboardManager manager = Bukkit.getScoreboardManager();
                if (manager == null) return 0;
                Scoreboard mainBoard = manager.getMainScoreboard();
                Objective objective = mainBoard.getObjective("deaths");
                if (objective == null) return 0;
                Score score = objective.getScore(p.getName());
                return score.getScore();
            }
            int deaths = plugin.getLeaderboardData().getInt(p.getUniqueId() + ".Deaths");
            if (deaths == 0) {
                ScoreboardManager manager = Bukkit.getScoreboardManager();
                if (manager == null) return 0;
                Scoreboard mainBoard = manager.getMainScoreboard();
                Objective objective = mainBoard.getObjective("deaths");
                if (objective == null) return 0;
                Score score = objective.getScore(p.getName());
                return score.getScore();
            }
            return deaths;
        }

        return 0;
    }

    public static ArrayList<String> sortLeaderboard(SurvivalSkills plugin, String skillName) {
        // Get the 10 highest scores from the leaderboard
        ArrayList<String> sorted = new ArrayList<>();
        HashMap<UUID, LeaderboardPlayer> sortedPlayers = new HashMap<>();
        for (int i = 1; i <= plugin.getLeaderboardTracker().size(); i++) {
            UUID topPlayer = null;
            String name = "";
            double topScore = 0;
            if (skillName.equalsIgnoreCase("deaths")) topScore = Integer.MAX_VALUE;
            for (Map.Entry<UUID, LeaderboardPlayer> entry : plugin.getLeaderboardTracker().entrySet()) {
                if (sortedPlayers.containsKey(entry.getKey())) continue;

                if (skillName.equalsIgnoreCase("all") && entry.getValue().getScore() > topScore) {
                    topPlayer = entry.getKey();
                    topScore = entry.getValue().getScore();
                    name = entry.getValue().getName();
                }
                else if (skillName.equalsIgnoreCase("building") && entry.getValue().getBuildingScore() > topScore) {
                    topPlayer = entry.getKey();
                    topScore = entry.getValue().getBuildingScore();
                    name = entry.getValue().getName();
                }
                else if (skillName.equalsIgnoreCase("crafting") && entry.getValue().getCraftingScore() > topScore) {
                    topPlayer = entry.getKey();
                    topScore = entry.getValue().getCraftingScore();
                    name = entry.getValue().getName();
                }
                else if (skillName.equalsIgnoreCase("exploring") && entry.getValue().getExploringScore() > topScore) {
                    topPlayer = entry.getKey();
                    topScore = entry.getValue().getExploringScore();
                    name = entry.getValue().getName();
                }
                else if (skillName.equalsIgnoreCase("farming") && entry.getValue().getFarmingScore() > topScore) {
                    topPlayer = entry.getKey();
                    topScore = entry.getValue().getFarmingScore();
                    name = entry.getValue().getName();
                }
                else if (skillName.equalsIgnoreCase("fighting") && entry.getValue().getFightingScore() > topScore) {
                    topPlayer = entry.getKey();
                    topScore = entry.getValue().getFightingScore();
                    name = entry.getValue().getName();
                }
                else if (skillName.equalsIgnoreCase("fishing") && entry.getValue().getFishingScore() > topScore) {
                    topPlayer = entry.getKey();
                    topScore = entry.getValue().getFishingScore();
                    name = entry.getValue().getName();
                }
                else if (skillName.equalsIgnoreCase("mining") && entry.getValue().getMiningScore() > topScore) {
                    topPlayer = entry.getKey();
                    topScore = entry.getValue().getMiningScore();
                    name = entry.getValue().getName();
                }
                else if (skillName.equalsIgnoreCase("main") && entry.getValue().getMainScore() > topScore) {
                    topPlayer = entry.getKey();
                    topScore = entry.getValue().getMainScore();
                    name = entry.getValue().getName();
                }
                else if (skillName.equalsIgnoreCase("deaths") && entry.getValue().getDeathScore() < topScore) {
                    topPlayer = entry.getKey();
                    topScore = entry.getValue().getDeathScore();
                    name = entry.getValue().getName();
                }
            }
            if (topPlayer == null) break;
            sortedPlayers.put(topPlayer, plugin.getLeaderboardTracker().get(topPlayer));
            if (skillName.equalsIgnoreCase("deaths"))
                sorted.add(ChatColor.AQUA.toString() + i + ". " + ChatColor.GOLD + name + ChatColor.AQUA + " - " + ChatColor.GREEN + ((int) topScore));
            else
                sorted.add(ChatColor.AQUA.toString() + i + ". " + ChatColor.GOLD + name + ChatColor.AQUA + " - " + ChatColor.GREEN + topScore);
        }
        return sorted;
    }

    public static ArrayList<String> getTopTen(SurvivalSkills plugin) {
        // Get the 10 highest scores from the leaderboard
        ArrayList<String> topTen = new ArrayList<>();
        HashMap<UUID, LeaderboardPlayer> topTenPlayers = new HashMap<>();
        for (int i = 1; i <= 10; i++) {
            UUID topPlayer = null;
            String name = "";
            double topScore = 0;
            for (Map.Entry<UUID, LeaderboardPlayer> entry : plugin.getLeaderboardTracker().entrySet()) {
                if (topTenPlayers.containsKey(entry.getKey())) continue;
                if (entry.getValue().getScore() > topScore) {
                    topPlayer = entry.getKey();
                    topScore = entry.getValue().getScore();
                    name = entry.getValue().getName();
                }
            }
            if (topPlayer == null) break;
            topTenPlayers.put(topPlayer, plugin.getLeaderboardTracker().get(topPlayer));
            topTen.add(ChatColor.AQUA.toString() + i + ": " + ChatColor.GOLD + name + ChatColor.AQUA + " - " + ChatColor.GREEN + topScore);
        }
        return topTen;
    }

    public static void printLeaderboard(SurvivalSkills plugin, Player p, String skillName, int page, int maxPage) {
        ArrayList<String> leaderboard = sortLeaderboard(plugin, skillName);

        p.sendRawMessage(ChatColor.YELLOW + "Page: " + ChatColor.AQUA + page + ChatColor.YELLOW + "/" + ChatColor.AQUA + maxPage);
        printRank(plugin, p, skillName);
        for (int i = (page * 10) - 10; i <= (page * 10) - 1; i++) {
            if (i >= leaderboard.size()) break;
            p.sendRawMessage(leaderboard.get(i));
        }
        p.sendRawMessage(ChatColor.YELLOW + "Type " + ChatColor.AQUA + "/skills leaderboard " + skillName + " <page>" + ChatColor.YELLOW + " to view more pages");
        p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
    }

    public static void printRank(SurvivalSkills plugin, Player p, String skillName) {
        int skillScore = getLeaderboardScore(plugin, p, skillName);

        int rank = 1;
        for (Map.Entry<UUID, LeaderboardPlayer> entry : plugin.getLeaderboardTracker().entrySet()) {
            if (entry.getKey().equals(p.getUniqueId())) continue;
            if (skillName.equalsIgnoreCase("all") && entry.getValue().getScore() > skillScore) rank++;
            else if (skillName.equalsIgnoreCase("building") && entry.getValue().getBuildingScore() > skillScore) rank++;
            else if (skillName.equalsIgnoreCase("crafting") && entry.getValue().getCraftingScore() > skillScore) rank++;
            else if (skillName.equalsIgnoreCase("exploring") && entry.getValue().getExploringScore() > skillScore) rank++;
            else if (skillName.equalsIgnoreCase("farming") && entry.getValue().getFarmingScore() > skillScore) rank++;
            else if (skillName.equalsIgnoreCase("fighting") && entry.getValue().getFightingScore() > skillScore) rank++;
            else if (skillName.equalsIgnoreCase("fishing") && entry.getValue().getFishingScore() > skillScore) rank++;
            else if (skillName.equalsIgnoreCase("mining") && entry.getValue().getMiningScore() > skillScore) rank++;
            else if (skillName.equalsIgnoreCase("main") && entry.getValue().getMainScore() > skillScore) rank++;
        }
        p.sendRawMessage(ChatColor.GREEN + "You are currently ranked " + ChatColor.GOLD + rank + ChatColor.GREEN + " in "
                + ChatColor.GOLD + skillName + ChatColor.GREEN + " out of " + ChatColor.GOLD + plugin.getLeaderboardTracker().size() + ChatColor.GREEN + "!");
    }

    public static void leaderboardJoin(SurvivalSkills plugin, Player p) {
        plugin.getLeaderboardTracker().put(p.getUniqueId(), createLeaderboardPlayer(plugin, p));
        int deaths = getLeaderboardScore(plugin, p, "Deaths");
        if (deaths >= 40)
            p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false, true));
        if (deaths >= 50) {
            PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
            rewards.setProtectionPercentage(rewards.getProtectionPercentage() + 0.1);
            rewards.setAddedDeathResistance(true);
        }

        SkillScoreboard.updateScoreboard(plugin, p, "Main");
    }
}
