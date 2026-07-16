package sir_draco.survivalskills.boards;

import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import sir_draco.survivalskills.skills.Skill;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.skills.SkillManager;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.god_questline.trial.TrialManager;

public class SkillScoreboard {

    private static final String MAIN = SkillCategory.MAIN.getDisplayName();
    private static final String DEATHS = SkillCategory.DEATHS.getDisplayName();

    private static final NavigableMap<Integer, ChatColor> LEVEL_COLORS = new TreeMap<>();
    static {
        LEVEL_COLORS.put(0, ChatColor.GRAY);
        LEVEL_COLORS.put(10, ChatColor.DARK_GRAY);
        LEVEL_COLORS.put(20, ChatColor.GREEN);
        LEVEL_COLORS.put(30, ChatColor.DARK_GREEN);
        LEVEL_COLORS.put(40, ChatColor.AQUA);
        LEVEL_COLORS.put(50, ChatColor.DARK_AQUA);
        LEVEL_COLORS.put(60, ChatColor.LIGHT_PURPLE);
        LEVEL_COLORS.put(70, ChatColor.DARK_PURPLE);
        LEVEL_COLORS.put(80, ChatColor.RED);
        LEVEL_COLORS.put(90, ChatColor.DARK_RED);
        LEVEL_COLORS.put(100, ChatColor.GOLD);
    }

    private SkillScoreboard() {}

    /**
     * Initializes the main and death scoreboard for the player and updates the scoreboard tracker
     * @param p the player
     * @return a new scoreboard
     */
    public static Scoreboard initializeScoreboard(Player p) {
        Scoreboard board = createNewScoreboard(p, "scoreboard");

        // Register the main and death objectives
        if (board.getObjective(MAIN) == null) {
            Objective mainObjective = board.registerNewObjective(MAIN, Criteria.DUMMY, MAIN);
            Objective deathObjective = board.registerNewObjective(DEATHS, Criteria.DUMMY, DEATHS);
            mainObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
            deathObjective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
        }

        // Add the scoreboard to the player and tracker
        p.setScoreboard(board);
        SurvivalSkills.getInstance().getScoreboardTracker().put(p, board);
        // Update after the board has been set to prevent board being initialized in a loop
        updateScoreboard(p, SkillCategory.MAIN);
        loadPlayerDeaths(p);
        loadPlayerNametags(p);
        return board;
    }

    /**
     * Initializes a trial scoreboard for the player and updates the trial scoreboard tracker
     * @param p the player
     */
    public static void initializeTrialScoreboard(Player p) {
        Scoreboard board = createNewScoreboard(p, "trial scoreboard");

        // Register the main objective
        if (board.getObjective(MAIN) == null) {
            Objective main = board.registerNewObjective(MAIN, Criteria.DUMMY, MAIN);
            main.setDisplayName(ChatColor.AQUA + "Trial");
            main.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        // Add the scoreboard to the tracker
        p.setScoreboard(board);
        TrialManager.setTrialScoreboard(p, board);
    }

    /**
     * Initializes a trial spectator scoreboard for the player and updates the trial spectator scoreboard tracker
     * @param p the player
     * @param targetName the name of the player being spectated
     */
    public static void initializeTrialSpectatorScoreboard(Player p, String targetName) {
        Scoreboard board = createNewScoreboard(p, "trial spectator scoreboard");
        Objects.requireNonNull(targetName, "Spectator target name cannot be null");

        // Register the main and death objectives
        if (board.getObjective(MAIN) == null) {
            Objective main = board.registerNewObjective(MAIN, Criteria.DUMMY, MAIN);
            main.setDisplayName(ChatColor.AQUA + targetName);
            main.setDisplaySlot(DisplaySlot.SIDEBAR);
        }

        // Add the scoreboard to the tracker
        p.setScoreboard(board);
        TrialManager.setSpectatorScoreboard(p, board);
    }

    /**
     * Creates an empty scoreboard to hide an existing scoreboard
     * Removes the player from the scoreboard tracker
     * @param p the player
     */
    public static void hideScoreboard(Player p) {
        Scoreboard empty = createNewScoreboard(p, "empty scoreboard");
        // Remove the objective from the display slot
        p.getScoreboard().clearSlot(DisplaySlot.SIDEBAR);
        p.setScoreboard(empty);
        SurvivalSkills.getInstance().getScoreboardTracker().remove(p);
    }

    /**
     * Changes a player's scoreboard to represent a change in the XP of a skill
     * @param p the player
     * @param skillCategory the name of the skill
     */
    public static void updateScoreboard(Player p, SkillCategory skillCategory) {
        // Ignore trial players as they have a different scoreboard
        if (TrialManager.isTrialPlayer(p)) return;

        Scoreboard board = getScoreboard(p);
        if (board == null) return;

        updateMainSkillTitle(p, board);

        if (skillCategory == SkillCategory.MAIN) {
            p.setScoreboard(board);
            return;
        }

        // Handle another skill being displayed
        Skill sideSkill = SkillManager.getSkill(p.getUniqueId(), skillCategory);
        int skillLevel = sideSkill.getLevel();
        String skillXPNext;
        if (skillLevel < Skill.MAX_LEVEL) {
            double ratio = (double) sideSkill.getExperienceSoFarInLevel() / sideSkill.getRawExperienceForNextLevel();
            int newRatio = (int) (ratio * Skill.MAX_LEVEL);
            skillXPNext = "Progress: " + ChatColor.AQUA + "(" + newRatio + "％)"; // This unicode character is intentional
        } else {
            skillXPNext = "Progress: MAX";
        }

        // Create a new scoreboard line for each string displayed
        updateTeam(board, "SkillXPNext", ChatColor.BLUE.toString(), ChatColor.GRAY + skillXPNext, 1);
        updateTeam(board, "Skill", ChatColor.GRAY.toString(),
                ChatColor.GOLD + skillCategory.getDisplayName() + " Level " + ChatColor.AQUA + "(" + skillLevel + ")", 2);
        updateTeam(board, "Empty", ChatColor.DARK_PURPLE.toString(), ChatColor.GRAY + "----------------", 3);
    }

    /**
     * Changes a player's scoreboard without displaying any specific skill updates
     * @param p the player
     */
    public static void updateScoreboard(Player p) {
        updateScoreboard(p, SkillCategory.MAIN);
    }

    /**
     * Updates the player's trial scoreboard with the current score and time
     * @param p the player
     * @param scoreAmount the current trial score
     * @param timeAmount the current trial time
     */
    public static void updateTrialScoreboard(Player p, int scoreAmount, int timeAmount) {
        Scoreboard board = TrialManager.getTrialScoreboard(p);
        if (board == null) {
            initializeTrialScoreboard(p);
            return;
        }

        // Color the main level in the scoreboard display
        String scoreString = ChatColor.GOLD + "Score: " + ChatColor.AQUA + scoreAmount;
        String timeString = ChatColor.GOLD + "Time: " + ChatColor.AQUA + formatTime(timeAmount);
        updateTeam(board, "Score", ChatColor.GRAY.toString(), scoreString, 1);
        updateTeam(board, "Time", ChatColor.BLUE.toString(), timeString, 2);
        p.setScoreboard(board);
    }

    /**
     * Updates the spectator trial scoreboard with the target's health, food, score, and time
     * @param p the spectator player
     * @param targetName the name of the player being spectated
     * @param health the target's current health
     * @param food the target's current food level
     * @param scoreAmount the current trial score
     * @param timeAmount the current trial time
     */
    public static void updateTrialSpectatorScoreboard(Player p, String targetName, double health, int food,
            int scoreAmount, int timeAmount) {
        Scoreboard board = TrialManager.getSpectatorScoreboard(p);
        if (board == null) {
            initializeTrialSpectatorScoreboard(p, targetName);
            return;
        }

        // Color the main level in the scoreboard display
        String healthString = ChatColor.GOLD + "Health: " + ChatColor.AQUA + health;
        String foodString = ChatColor.GOLD + "Food: " + ChatColor.AQUA + food;
        String scoreString = ChatColor.GOLD + "Score: " + ChatColor.AQUA + scoreAmount;
        String timeString = ChatColor.GOLD + "Time: " + ChatColor.AQUA + formatTime(timeAmount);
        updateTeam(board, "Health", ChatColor.GRAY.toString(), healthString, 1);
        updateTeam(board, "Food", ChatColor.BLUE.toString(), foodString, 2);
        updateTeam(board, "Score", ChatColor.GRAY.toString(), scoreString, 3);
        updateTeam(board, "Time", ChatColor.BLUE.toString(), timeString, 4);
        p.setScoreboard(board);
    }


    /**
     * Updates the death counters for all players on the server for a specific player's death count
     * @param p
     * @param deaths
     */
    public static void updateScoreboardDeaths(Player p, int deaths) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard board = getScoreboard(player);
            if (board == null) continue;
            Objective deathObjective = board.getObjective(DEATHS);
            if (deathObjective == null) return;
            deathObjective.getScore(p.getName()).setScore(deaths);
        }
    }

    
    /**
     * Updates the nametag of a player for all other players to see
     * @param p
     */
    public static void updateNametags(Player p) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Scoreboard board = getScoreboard(player);
            if (board == null) continue;
            Skill playerMainSkill = SkillManager.getSkill(p.getUniqueId(), SkillCategory.MAIN);
            String colorString = getColorString(getChatColor(playerMainSkill), playerMainSkill.getLevel());
            Team team = board.getTeam(p.getName());
            if (team == null) {
                team = board.registerNewTeam(p.getName());
                team.addEntry(p.getName());
                team.setPrefix(colorString + "(" + playerMainSkill.getLevel() + ") ");
            } else {
                team.setPrefix(colorString + "(" + playerMainSkill.getLevel() + ") ");
            }
        }
    }

    /**
     * Updates the team prefix or creates a new team if one doesn't exist
     * @param board the scoreboard
     * @param name the team name
     * @param holder the holder name
     * @param display the team display
     * @param score the team score
     */
    private static void updateTeam(Scoreboard board, String name, String holder, String display, int score) {
        Team team = board.getTeam(name);
        if (team == null) {
            team = board.registerNewTeam(name);
            team.setPrefix(display);
            team.addEntry(holder);
            Objective obj = board.getObjective(DisplaySlot.SIDEBAR);
            Objects.requireNonNull(obj, String.format("No sidebar objective for %s", name));
            obj.getScore(holder).setScore(score);
        } else {
            team.setPrefix(display);
        }
    }

    /**
     * Returns the ChatColor of a player's main skill level
     * @param playerMainSkill the player's main skill object
     * @return the ChatColor
     */
    private static ChatColor getChatColor(Skill playerMainSkill) {
        return LEVEL_COLORS.floorEntry(playerMainSkill.getLevel()).getValue();
    }

    /**
     * Converts a time in seconds to a formatted MM:SS string
     * @param time the time in seconds
     * @return a formatted time string
     */
    private static String formatTime(int time) {
        int minutes = time / 60;
        int seconds = time % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * Either gets or initializes a new scoreboard
     * @param p the player
     * @return a new or existing scoreboard, null when the player has the scoreboard disabled
     */
    @Nullable
    private static Scoreboard getScoreboard(Player p) {
        // If the player has never toggled the scoreboard, initialize it
        SurvivalSkills plugin = SurvivalSkills.getInstance();
        if (plugin.getShowScoreboard().get(p.getUniqueId()) == null) {
            plugin.getShowScoreboard().put(p.getUniqueId(), true);
            return initializeScoreboard(p);
        }
        // Don't return a scoreboard if is toggled off
        if (!plugin.getShowScoreboard().get(p.getUniqueId())) return null;
        Scoreboard board = plugin.getScoreboardTracker().get(p);
        if (board == null) {
            return initializeScoreboard(p);
        }
        return board;
    }

    /**
     * Updates the main scoreboard display name
     * @param p the player
     * @param board the scoreboard to update
     */
    private static void updateMainSkillTitle(Player p, Scoreboard board) {
        // Get main skill and death objectives
        Skill mainSkill = SkillManager.getSkill(p.getUniqueId(), SkillCategory.MAIN);
        Objective main = board.getObjective(MAIN);
        if (main == null) return;
        int playerLevel = mainSkill.getLevel();

        // Color the main level in the scoreboard display
        String mainString = getColorString(getChatColor(mainSkill), mainSkill.getLevel());
        main.setDisplayName(ChatColor.GOLD + "Player Main Level " + mainString + "(" + playerLevel + ")");
    }

    /**
     * Returns the colorized string for a skill's level
     * @param color the color
     * @param mainSkillLevel the player's main skill level
     * @return
     */
    private static String getColorString(ChatColor color, int mainSkillLevel) {
        if (mainSkillLevel == Skill.MAX_LEVEL) {
            return ChatColor.BOLD.toString() + color;
        }
        return color.toString();
    }

    /**
     * Checks if a player is valid and then creates a scoreboard instance
     * @param p the player
     * @param boardType the scoreboard type name for error messages
     * @return a new scoreboard
     */
    private static Scoreboard createNewScoreboard(Player p, String boardType) {
        Objects.requireNonNull(p, String.format("Player object null when initializing %s", boardType));
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Objects.requireNonNull(manager, String.format("ScoreboardManager object null when initializing %s", boardType));
        return manager.getNewScoreboard();
    }

    /**
     * Loads the death counts of all players on the server for a single player
     * @param p
     */
    private static void loadPlayerDeaths(Player p) {
        Scoreboard board = getScoreboard(p);
        if (board == null) return;
        Objective deathObjective = board.getObjective(DEATHS);
        if (deathObjective == null) return;
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            LeaderboardPlayer leaderboardPlayer = SurvivalSkills.getInstance().getLeaderboardTracker().get(player.getUniqueId());
            if (leaderboardPlayer == null) continue;
            int deaths = leaderboardPlayer.getScore(SkillCategory.DEATHS);
            deathObjective.getScore(p.getName()).setScore(deaths);
        }
    }


    /**
     * Loads the custom nametags of all players on the server for a single player
     * @param p
     */
    private static void loadPlayerNametags(Player p) {
        // Get the player's scoreboard
        Scoreboard board = getScoreboard(p);
        if (board == null) return;
        // Get every player's main skill level and create a team for them
        for (Player player : Bukkit.getOnlinePlayers()) {
            Skill playerMainSkill = SkillManager.getSkill(player.getUniqueId(), SkillCategory.MAIN);
            String colorString = getColorString(getChatColor(playerMainSkill), playerMainSkill.getLevel());
            Team team = board.getTeam(player.getName());
            if (team == null) {
                team = board.registerNewTeam(player.getName());
                team.addEntry(player.getName());
                team.setPrefix(colorString + "(" + playerMainSkill.getLevel() + ") ");
            } else {
                team.setPrefix(colorString + "(" + playerMainSkill.getLevel() + ") ");
            }
        }
    }
}
