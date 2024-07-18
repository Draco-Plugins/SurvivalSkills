package sir_draco.survivalskills.Boards;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;
import sir_draco.survivalskills.Skills.Skill;
import sir_draco.survivalskills.SurvivalSkills;

public class SkillScoreboard {

    public static void initializeScoreboard(SurvivalSkills plugin, Player p) {
        // Make a new scoreboard
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;
        Scoreboard board = manager.getNewScoreboard();
        p.setScoreboard(board);
        plugin.getScoreboardTracker().put(p, board);

        // Register the main and death objectives
        if (board.getObjective("Main") == null) {
            Objective main = board.registerNewObjective("Main", Criteria.DUMMY, "Main");
            Objective deaths = board.registerNewObjective("Deaths", Criteria.DUMMY, "Deaths");
            main.setDisplaySlot(DisplaySlot.SIDEBAR);
            deaths.setDisplaySlot(DisplaySlot.PLAYER_LIST);
            updateScoreboard(plugin, p, "Main");
        }
        else updateScoreboard(plugin, p, "Main");

        // Add the scoreboard to the tracker
        p.setScoreboard(board);
        plugin.getScoreboardTracker().put(p, board);
    }

    /**
     * Creates an empty scoreboard to hide an existing scoreboard
     */
    public static void hideScoreboard(SurvivalSkills plugin, Player p) {
        // Remove the objective from the display slot
        p.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);

        // Try to put a new empty scoreboard in place
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;
        Scoreboard empty = manager.getNewScoreboard();
        p.setScoreboard(empty);
        plugin.getScoreboardTracker().remove(p);
    }

    /**
     * Changes a player's scoreboard to represent a change in the XP of a skill
     */
    public static void updateScoreboard(SurvivalSkills plugin, Player p, String skillName) {
        // If the player has never toggled the scoreboard, initialize it
        if (plugin.getToggledScoreboard().get(p.getUniqueId()) == null) {
            plugin.getToggledScoreboard().put(p.getUniqueId(), true);
            initializeScoreboard(plugin, p);
            return;
        }
        if (!plugin.getToggledScoreboard().get(p.getUniqueId())) return;
        Scoreboard board = plugin.getScoreboardTracker().get(p);
        if (board == null) {
            initializeScoreboard(plugin, p);
            return;
        }

        // Get main skill and death objectives
        Skill mainSkill = plugin.getSkillManager().getSkill(p.getUniqueId(), "Main");
        Objective main = board.getObjective("Main");
        Objective deaths = board.getObjective("Deaths");
        if (main == null) return;
        if (deaths == null) return;
        int playerLevel = mainSkill.getLevel();

        // Color the main level in the scoreboard display
        ChatColor mainColor = getChatColor(mainSkill);
        String mainString;
        if (mainSkill.getLevel() == 100) mainString = ChatColor.BOLD.toString() + mainColor;
        else mainString = mainColor.toString();
        main.setDisplayName(ChatColor.GOLD + "Player Main Level " + mainString + "(" + playerLevel + ")");

        // Deaths
        for (Player player : Bukkit.getOnlinePlayers()) {
            LeaderboardPlayer leaderboardPlayer = plugin.getLeaderboardTracker().get(player.getUniqueId());
            if (leaderboardPlayer != null) deaths.getScore(player.getName()).setScore(leaderboardPlayer.getDeathScore());
            else deaths.getScore(player.getName()).setScore(0);
        }

        // Player NameTags
        for (Player player : Bukkit.getOnlinePlayers()) {
            Skill playerMainSkill = plugin.getSkillManager().getSkill(player.getUniqueId(), "Main");
            ChatColor color = getChatColor(playerMainSkill);
            String colorString;
            if (playerMainSkill.getLevel() == 100) colorString = ChatColor.BOLD.toString() + color;
            else colorString = color.toString();
            Team team = board.getTeam(player.getName());
            if (team == null) {
                team = board.registerNewTeam(player.getName());
                team.addEntry(player.getName());
                team.setPrefix(colorString + "(" + playerMainSkill.getLevel() + ") ");
            }
            else team.setPrefix(colorString + "(" + playerMainSkill.getLevel() + ") ");
        }

        // Handle another skill being displayed
        if (skillName.equals("Main")) return;
        Skill sideSkill = plugin.getSkillManager().getSkill(p.getUniqueId(), skillName);
        int skillLevel = sideSkill.getLevel();
        String skillXPNext;
        if (skillLevel < 100) {
            double ratio = (double) sideSkill.getExperienceSoFarInLevel() / sideSkill.getRawExperienceForNextLevel();
            int newRatio = (int) (ratio * 100);
            skillXPNext = "Progress: " + ChatColor.AQUA + "(" + newRatio + "％)";
        }
        else skillXPNext = "Progress: MAX";

        // Create a new scoreboard line for each string displayed
        newTeam(board, "SkillXPNext", ChatColor.BLUE.toString(), ChatColor.GRAY + skillXPNext, 1);
        newTeam(board, "Skill", ChatColor.GRAY.toString(), ChatColor.GOLD + skillName + " Level " + ChatColor.AQUA + "(" + skillLevel + ")", 2);
        newTeam(board, "Empty", ChatColor.DARK_PURPLE.toString(), ChatColor.GRAY + "----------------", 3);
        p.setScoreboard(board);
    }

    /**
     * Creates a new team for the scoreboard
     */
    public static void newTeam(Scoreboard board, String name, String holder, String display, int score) {
        Team team = board.getTeam(name);
        if (team == null) {
            team = board.registerNewTeam(name);
            team.setPrefix(display);
            team.addEntry(holder);
            Objective obj = board.getObjective(DisplaySlot.SIDEBAR);
            if (obj != null) obj.getScore(holder).setScore(score);
        }
        else team.setPrefix(display);
    }

    /**
     * Returns the ChatColor of a player's main skill level
     */
    private static ChatColor getChatColor(Skill playerMainSkill) {
        int playerMainLevel = playerMainSkill.getLevel();
        ChatColor color;
        if (playerMainLevel < 10) color = ChatColor.GRAY;
        else if (playerMainLevel < 20) color = ChatColor.DARK_GRAY;
        else if (playerMainLevel < 30) color = ChatColor.GREEN;
        else if (playerMainLevel < 40) color = ChatColor.DARK_GREEN;
        else if (playerMainLevel < 50) color = ChatColor.AQUA;
        else if (playerMainLevel < 60) color = ChatColor.DARK_AQUA;
        else if (playerMainLevel < 70) color = ChatColor.LIGHT_PURPLE;
        else if (playerMainLevel < 80) color = ChatColor.DARK_PURPLE;
        else if (playerMainLevel < 90) color = ChatColor.RED;
        else if (playerMainLevel < 100) color = ChatColor.DARK_RED;
        else color = ChatColor.GOLD;
        return color;
    }
}
