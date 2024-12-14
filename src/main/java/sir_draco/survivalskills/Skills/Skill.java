package sir_draco.survivalskills.Skills;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class Skill {
    private final int maxLevel = 100;
    private final int maxExperience = 1000000;

    private String skillName = "";
    private double experience;
    private int expSoFarInLevel = 0;
    private int rawExperienceForNextLevel;
    private int level;
    private boolean currentMaxMessage = false;

    public Skill(double experience, int level, String name) {
        this.experience = experience;
        this.level = level;
        this.skillName = name;
        // Store the total XP to go from the current level to the next level
        setExperienceSoFarInLevel();
        setRawExperienceForNextLevel();

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

        int xp = SkillManager.totalExperienceForLevel(this.level, skillName);
        if (experience != xp) setExperience(xp);
    }

    /**
     * Changes the level and updates how much XP is needed for the next level
     * If XP is under what is required it manually changes the XP
     * @param level The amount of levels to change by
     * @return True if the level changes, false otherwise
     */
    public boolean changeLevel(int level) {
        if (this.level + level > maxLevel) {
            this.level = maxLevel;
            return false;
        }
        else if (this.level + level < 1) {
            this.level = 1;
            return false;
        }
        else this.level += level;

        setRawExperienceForNextLevel();

        if (experience < SkillManager.totalExperienceForLevel(this.level, skillName)) changeExperience(totalExperienceForNextLevel(this.level), this.level + 1);
        return true;
    }

    public String getSkillName() {
        return skillName;
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
        return SkillManager.totalExperienceForLevel(currentLevel + 1, skillName) - SkillManager.totalExperienceForLevel(currentLevel, skillName);
    }

    /**
     * Returns the XP needed to go from the current level to
     * the next level taking into account how much XP
     * this skill currently has
     */
    public int totalExperienceForNextLevel(int currentLevel) {
        if (currentLevel == 1) return experienceForNextLevel(currentLevel);
        int soFar = (int) experience - SkillManager.totalExperienceForLevel(currentLevel, skillName);
        if (soFar < 0) return experienceForNextLevel(currentLevel);
        return experienceForNextLevel(currentLevel) - soFar;
    }

    public void setExperienceSoFarInLevel() {
        expSoFarInLevel = (int) experience - SkillManager.totalExperienceForLevel(level, skillName);
    }

    public int getExperienceSoFarInLevel() {
        return expSoFarInLevel;
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
            sum += Math.log(level + 1) * SkillManager.scalar;
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

    public int getMaxLevel() {
        return maxLevel;
    }

    public boolean isCurrentMaxMessage() {
        return currentMaxMessage;
    }

    public void setCurrentMaxMessage(boolean currentMaxMessage) {
        this.currentMaxMessage = currentMaxMessage;
    }

    public String toString() {
        return "Skill {" +
                "experience=" + experience +
                ", expSoFarInLevel=" + expSoFarInLevel +
                ", rawExperienceForNextLevel=" + rawExperienceForNextLevel +
                ", level=" + level +
                ", currentMaxMessage=" + currentMaxMessage +
                ", maxLevel=" + maxLevel +
                ", skillName='" + skillName + '\'' +
                '}';
    }
}
