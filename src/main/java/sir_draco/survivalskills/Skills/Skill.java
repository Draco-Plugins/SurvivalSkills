package sir_draco.survivalskills.Skills;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.Boards.Leaderboard;
import sir_draco.survivalskills.Boards.LeaderboardPlayer;
import sir_draco.survivalskills.Boards.SkillScoreboard;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.HashMap;
import java.util.UUID;

public class Skill {
    private static final double scalar = 2749.22119298367;
    private static String skillName = "";

    private final int maxLevel = 100;
    private final int maxExperience = 1000000;

    private double experience;
    private int expSoFarInLevel = 0;
    private int rawExperienceForNextLevel;
    private int level;
    private boolean currentMaxMessage = false;

    public static final String MINING = "Mining";
    public static final String EXPLORING = "Exploring";
    public static final String FARMING = "Farming";
    public static final String BUILDING = "Building";
    public static final String FIGHTING = "Fighting";
    public static final String FISHING = "Fishing";
    public static final String CRAFTING = "Crafting";
    public static final String MAIN = "Main";

    public Skill(double experience, int level, String name) {
        this.experience = experience;
        this.level = level;
        skillName = name;
        // Store the total XP to go from the current level to the next level
        setExperienceSoFarInLevel();
        setRawExperienceForNextLevel();

    }

    public static void experienceEvent(SurvivalSkills plugin, Player p, double xp, String skillName) {
        // Handle multipliers
        xp = xp * plugin.getSkillManager().getMultiplier();
        if (plugin.getAbilityManager().getAbility(p, "XPVoucher") != null)
            xp *= plugin.getSkillManager().getPlayerMultiplier(p);

        UUID uuid = p.getUniqueId();
        Skill skill = plugin.getSkillManager().getSkill(uuid, skillName);
        if (skill.getLevel() >= plugin.getTrophyManager().playerMaxSkillLevel(uuid)) {
            SkillScoreboard.updateScoreboard(plugin, p, "Main");
            if (skill.getLevel() == 100 || skill.isCurrentMaxMessage()) return;
            if (!plugin.getSkillManager().isMaxSkillMessageEnabled(p)) return;
            p.sendRawMessage(ChatColor.DARK_BLUE + "You have reached your current max level for: " + ChatColor.AQUA + skillName);
            p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
            skill.setCurrentMaxMessage(true);
            return;
        }

        if (skill.getLevel() >= 100) return;
        xp = checkXPCap(skill.getExperience(), xp, plugin.getTrophyManager().playerMaxSkillLevel(uuid));

        if (plugin.getToggledScoreboard().containsKey(p.getUniqueId()) && plugin.getToggledScoreboard().get(p.getUniqueId())
                && xp != 0)
            sendActionBarMessage(p, ChatColor.GRAY + skillName + ChatColor.YELLOW + " (+" + xp + ")");
        if (skill.changeExperience(xp, plugin.getTrophyManager().playerMaxSkillLevel(uuid))) {
            skill.levelUpNotification(p);
            plugin.getSkillManager().getPlayerRewards(p).handleReward(plugin, p, skill, skillName, true);
            if (plugin.getLeaderboardTracker().containsKey(p.getUniqueId())) {
                LeaderboardPlayer player = plugin.getLeaderboardTracker().get(p.getUniqueId());
                setScore(player, p, plugin, skill.getSkillName());
                plugin.getLeaderboardTracker().put(p.getUniqueId(), player);
            }
            else {
                plugin.getLeaderboardTracker().put(p.getUniqueId(), Leaderboard.createLeaderboardPlayer(plugin, p));
                setScore(plugin.getLeaderboardTracker().get(p.getUniqueId()), p, plugin, skill.getSkillName());
            }
        }

        // Check the main skill
        Skill main = plugin.getSkillManager().getSkill(uuid, "Main");
        if (main.getLevel() >= plugin.getTrophyManager().playerMaxSkillLevel(uuid)) {
            plugin.getSkillManager().checkMainXP(p);
            SkillScoreboard.updateScoreboard(plugin, p, skillName);
            return;
        }
        if (main.changeExperience(xp / 7.0, plugin.getTrophyManager().playerMaxSkillLevel(uuid))) {
            main.levelUpNotification(p);
            plugin.getSkillManager().getPlayerRewards(p).handleReward(plugin, p, main, "Main", true);
            if (main.getLevel() == 100) {
                HashMap<String, Boolean> trophies = plugin.getTrophyManager().getTrophyTracker().get(p.getUniqueId());
                trophies.put("GodTrophy", true);
                plugin.getTrophyManager().getTrophyTracker().put(p.getUniqueId(), trophies);

                // Add the god trophy to the player's inventory, if their inventory is full drop it
                if (!p.getInventory().addItem(plugin.getTrophyManager().getTrophyItem(10)).isEmpty())
                    p.getWorld().dropItem(p.getLocation(), plugin.getTrophyManager().getTrophyItem(10));

                plugin.getServer().broadcastMessage(ChatColor.AQUA + p.getName() + " has maxed out all of their skills!");
                plugin.getServer().broadcastMessage(ChatColor.GREEN + "Congratulate the hard work they put in!");
                for (Player player : Bukkit.getOnlinePlayers())
                    player.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
                p.sendRawMessage(ChatColor.GREEN + "You have been awarded a trophy for maxing out all of your skills!");
            }

            if (plugin.getLeaderboardTracker().containsKey(p.getUniqueId())) {
                LeaderboardPlayer player = plugin.getLeaderboardTracker().get(p.getUniqueId());
                setScore(player, p, plugin, main.getSkillName());
                plugin.getLeaderboardTracker().put(p.getUniqueId(), player);
            }
            else {
                plugin.getLeaderboardTracker().put(p.getUniqueId(), Leaderboard.createLeaderboardPlayer(plugin, p));
                setScore(plugin.getLeaderboardTracker().get(p.getUniqueId()), p, plugin, main.getSkillName());
            }
        }
        SkillScoreboard.updateScoreboard(plugin, p, skillName);
        if (xp != 0) experienceEvent(plugin, p, 0, skillName);
    }

    /**
     * Ensures that the experience is in the appropriate range and updates the level accordingly
     */
    public void setExperience(int experience) {

        if (experience > maxExperience) this.experience = maxExperience;
        else if (experience < 1) this.experience = 1;
        else this.experience = experience + 1;

        int expectedLevel = getExpectedLevel((int) this.experience);
        if (level != expectedLevel) {
            setLevel(Math.min(expectedLevel, maxLevel));
        }
    }

    public double getExperience() {
        return experience;
    }

    /**
     * Prevents experience from being added if already at max level
     * Changes the level if appropriate
     * @param experience The amount of experience to change
     * @param currentMaxLevel The max level the skill can be
     * @return True if there is a level up, false otherwise
     */
    public boolean changeExperience(double experience, int currentMaxLevel) {
        if (level >= maxLevel) return false;
        if (level >= currentMaxLevel) return false;

        if (this.experience + experience > maxExperience) this.experience = maxExperience;
        else this.experience += experience;

        if (this.experience < 1) {
            this.experience = 1;
            return false;
        }

        boolean changeLevel = false;
        if (level < getExpectedLevel((int) this.experience)) changeLevel = changeLevel(1);
        setExperienceSoFarInLevel();
        return changeLevel;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Ensures that the level is in the appropriate range and updates the experience accordingly
     */
    public void setLevel(int level) {
        if (level > maxLevel) this.level = maxLevel;
        else this.level = Math.max(level, 1);

        setRawExperienceForNextLevel();

        if (experience != totalExperienceForLevel(this.level)) setExperience(totalExperienceForLevel(this.level));
    }

    /**
     * Changes the level and updates how much XP is needed for the next level
     * If XP is under what is required it manually changes the XP
     * @param level The amount of levels to change by
     * @return True if the level changes, false otherwise
     */
    public boolean changeLevel(int level) {
        if (this.level + level > maxLevel || this.level + level < 0) {
            this.level = maxLevel;
            return false;
        }
        else this.level += level;

        setRawExperienceForNextLevel();

        if (experience < totalExperienceForLevel(this.level)) changeExperience(totalExperienceForNextLevel(this.level), this.level + 1);
        return true;
    }

    public String getSkillName() {
        return skillName;
    }

    /**
     * Returns the amount of XP needed for all levels up to
     * and including the level inputted
     */
    public static int totalExperienceForLevel(int level) {
        double sum = 0;
        for (int i = 1; i <= level; i++) sum += Math.log(i) * scalar;

        if (skillName.equals("Main")) {
            if (level >= 100) return 1000000;
            return (int) Math.floor(sum) + 1;
        }
        return (int) Math.floor(sum);
    }

    public void setRawExperienceForNextLevel() {
        rawExperienceForNextLevel = experienceForNextLevel(level);
    }

    public int getRawExperienceForNextLevel() {
        return rawExperienceForNextLevel;
    }

    /**
     * Returns the XP needed to go from the current level to
     * the next level regardless of current XP
     */
    public int experienceForNextLevel(int currentLevel) {
        return totalExperienceForLevel(currentLevel + 1) - totalExperienceForLevel(currentLevel);
    }

    /**
     * Returns the XP needed to go from the current level to
     * the next level taking into account how much XP
     * this skill currently has
     */
    public int totalExperienceForNextLevel(int currentLevel) {
        if (currentLevel == 1) return experienceForNextLevel(currentLevel);
        int soFar = (int) experience - totalExperienceForLevel(currentLevel);
        if (soFar < 0) return experienceForNextLevel(currentLevel);
        return experienceForNextLevel(currentLevel) - soFar;
    }

    public void setExperienceSoFarInLevel() {
        expSoFarInLevel = (int) experience - totalExperienceForLevel(level);
    }

    public int getExperienceSoFarInLevel() {
        return expSoFarInLevel;
    }

    /**
     * Ensures that the XP cap is not exceeded
     */
    public static double checkXPCap(double totalXP, double xp, int levelCap) {
        double xpForLevel = totalExperienceForLevel(levelCap);
        if (totalXP + xp > xpForLevel) return xpForLevel - totalXP;
        return xp;
    }

    /**
     * Returns the level that a skill would be
     * given a specific amount of total XP
     */
    public int getExpectedLevel(int exp) {
        if (exp < 1) return 1;
        int level = 1;
        double sum = 0;
        while (level <= 100) {
            sum += Math.log(level + 1) * scalar;
            int sumCheck = (int) Math.floor(sum);
            if (sumCheck > exp) break;
            level++;
        }
        return Math.min(level, maxLevel);
    }

    public void printStats(Player p, boolean isPlayer) {
        p.sendRawMessage(ChatColor.AQUA.toString() + ChatColor.BOLD + skillName + ChatColor.WHITE + ":");
        p.sendRawMessage(ChatColor.WHITE + "Level: " + ChatColor.GREEN + level);
        if (!isPlayer) {
            p.sendRawMessage("");
            return;
        }

        int soFar = getExperienceSoFarInLevel();
        int total = getRawExperienceForNextLevel();

        String percentString = String.format("%.2f", (double) soFar / total * 100);
        String xp = ChatColor.YELLOW.toString() + soFar + ChatColor.WHITE + "/"
                + ChatColor.YELLOW + total;
        if (level == maxLevel) {
            p.sendRawMessage(ChatColor.WHITE + "Experience: " + ChatColor.YELLOW + "MAX");
        }
        else if (soFar == 0) {
            p.sendRawMessage(ChatColor.WHITE + "Experience: " + ChatColor.LIGHT_PURPLE + "Interact with this skill first!");
        }
        else {
            p.sendRawMessage(ChatColor.WHITE + "Experience: " + xp + ChatColor.WHITE
                    + " (" + ChatColor.LIGHT_PURPLE + percentString + "%" + ChatColor.WHITE + ")");
        }
        p.sendRawMessage("");
    }

    public void levelUpNotification(Player p) {
        p.sendRawMessage(ChatColor.GREEN + "Skill " + ChatColor.AQUA + skillName + ChatColor.GREEN + " has leveled up to level: " + ChatColor.GOLD + level);
        p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
    }

    public static void sendActionBarMessage(Player p, String message) {
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(message));
    }

    public static void setScore(LeaderboardPlayer player, Player p, SurvivalSkills plugin, String skillName) {
        player.setScore(Leaderboard.getLeaderboardScore(plugin, p, "All"));
        int skillScore = Leaderboard.getLeaderboardScore(plugin, p, skillName);
        switch (skillName) {
            case "Building":
                player.setBuildingScore(skillScore);
                break;
            case "Mining":
                player.setMiningScore(skillScore);
                break;
            case "Farming":
                player.setFarmingScore(skillScore);
                break;
            case "Fighting":
                player.setFightingScore(skillScore);
                break;
            case "Fishing":
                player.setFishingScore(skillScore);
                break;
            case "Crafting":
                player.setCraftingScore(skillScore);
                break;
            case "Exploring":
                player.setExploringScore(skillScore);
                break;
            case "Main":
                player.setMainScore(skillScore);
                break;
            case "Death":
                player.setDeathScore(skillScore);
                break;
        }
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public boolean isCurrentMaxMessage() {
        return currentMaxMessage;
    }

    public void setCurrentMaxMessage(boolean currentMaxMessage) {
        this.currentMaxMessage = currentMaxMessage;
    }
}
