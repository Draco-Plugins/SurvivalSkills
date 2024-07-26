package sir_draco.survivalskills.Commands;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;
import sir_draco.survivalskills.Rewards.PlayerRewards;
import sir_draco.survivalskills.Rewards.Reward;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;
import java.util.Map;

public class TabCompleter implements Listener {

    private final SurvivalSkills plugin;
    private final ArrayList<String> skillNames = new ArrayList<>();

    public TabCompleter(SurvivalSkills plugin) {
        this.plugin = plugin;
        createSkillNames(plugin);
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent e) {
        String buffer = e.getBuffer();
        Player p = (Player) e.getSender();
        if (buffer.contains("/spelunker ")) handleSpelunker(buffer, p, e);
        else if (buffer.contains("/veinminer ")) handleVeinminer(buffer, p, e);
        else if (buffer.contains("/ssget ")) handleGetItem(buffer, p, e);
        else if (buffer.contains("/ssboss spawn ")) handleBossSpawn(buffer, p, e);
        else if (buffer.contains("/ssboss ")) handleBoss(buffer, p, e);
        else if (buffer.contains("/survivalskills ") || buffer.contains("/ss ")) handleSurvivalSkills(buffer, p, e);
        else if (buffer.contains("/skills leaderboard ")) handleSkillsTree(buffer, e);
        else if (buffer.contains("/skills tree ")) handleSkillsTree(buffer, e);
        else if (buffer.contains("/skills player ")) handlePlayers(buffer, e);
        else if (buffer.contains("/skills ")) handleSkills(buffer, e);
        else if (buffer.contains("/toggletrail ")) handleTrails(p, buffer, e);
        else if (buffer.contains("/bossmusic ")) handleBossMusic(buffer, p, e);
    }

    public void handleSpelunker(String buffer, Player p, TabCompleteEvent e) {
        ArrayList<String> words = new ArrayList<>();
        words.add("time");
        if (p.hasPermission("survivalskills.op")) words.add("force");
        e.setCompletions(getCompletions(buffer, words));
    }

    public void handleVeinminer(String buffer, Player p, TabCompleteEvent e) {
        if (!p.hasPermission("survivalskills.op")) return;
        ArrayList<String> words = new ArrayList<>();
        words.add("force");
        e.setCompletions(getCompletions(buffer, words));
    }

    public void handleGetItem(String buffer, Player p, TabCompleteEvent e) {
        if (!p.hasPermission("survivalskills.op")) return;
        ArrayList<String> words = new ArrayList<>();
        words.add("unlimitedtorch");
        words.add("jumpingboots");
        words.add("miningboots");
        words.add("miningleggings");
        words.add("miningchestplate");
        words.add("mininghelmet");
        words.add("wandererboots");
        words.add("wandererleggings");
        words.add("wandererchestplate");
        words.add("wandererhelmet");
        words.add("cavefinder");
        words.add("travelerboots");
        words.add("travelerleggings");
        words.add("travelerchestplate");
        words.add("travelerhelmet");
        words.add("gillhelmet");
        words.add("gillchestplate");
        words.add("gillleggings");
        words.add("gillboots");
        words.add("adventurerboots");
        words.add("adventurerleggings");
        words.add("adventurerchestplate");
        words.add("adventurerhelmet");
        words.add("wateringcan");
        words.add("unlimitedbonemeal");
        words.add("harvester");
        words.add("dragonhead");
        words.add("giantsummoner");
        words.add("broodmothersummoner");
        words.add("exiledsummoner");
        words.add("sortofstonepick");
        words.add("fireworkcannon");
        words.add("sortwand");
        words.add("unlimitedtropicalfishbucket");
        words.add("unlimitedwaterbucket");
        words.add("unlimitedlavabucket");
        words.add("weatherartifact");
        words.add("timeartifact");
        words.add("xpvoucher");
        e.setCompletions(getCompletions(buffer, words));
    }

    public void handleBoss(String buffer, Player p, TabCompleteEvent e) {
        if (!p.hasPermission("survivalskills.op")) return;
        ArrayList<String> words = new ArrayList<>();
        words.add("spawn");
        words.add("select");
        words.add("healthpercent");
        words.add("kill");
        words.add("toggleai");
        words.add("attack");
        e.setCompletions(getCompletions(buffer, words));
    }

    public void handleBossSpawn(String buffer, Player p, TabCompleteEvent e) {
        if (!p.hasPermission("survivalskills.op")) return;
        ArrayList<String> words = new ArrayList<>();
        words.add("giant");
        words.add("broodmother");
        words.add("villager");
        words.add("fishingboss");
        e.setCompletions(getCompletions(buffer, words));
    }

    public void handleSkills(String buffer, TabCompleteEvent e) {
        ArrayList<String> words = new ArrayList<>();
        words.add("tree");
        words.add("recipes");
        words.add("commands");
        words.add("trophies");
        words.add("player");
        words.add("leaderboard");
        e.setCompletions(getCompletions(buffer, words));
    }

    public void handleSkillsTree(String buffer, TabCompleteEvent e) {
        ArrayList<String> words = new ArrayList<>();
        words.add("mining");
        words.add("exploring");
        words.add("farming");
        words.add("building");
        words.add("fighting");
        words.add("fishing");
        words.add("crafting");
        words.add("main");
        words.add("deaths");
        if (buffer.contains("leaderboard")) words.add("all");
        e.setCompletions(getCompletions(buffer, words));
    }

    public void handleSS(String buffer, Player p, TabCompleteEvent e) {
        if (!p.hasPermission("survivalskills.op")) return;
        ArrayList<String> words = new ArrayList<>();
        words.add("help");
        words.add("xp");
        words.add("level");
        words.add("skillxp");
        words.add("togglereward");
        words.add("rewardlevel");
        e.setCompletions(getCompletions(buffer, words));
    }

    public void handleSSkills(String buffer, Player p, TabCompleteEvent e) {
        if (!p.hasPermission("survivalskills.op")) return;
        ArrayList<String> words = new ArrayList<>();
        words.add("mining");
        words.add("exploring");
        words.add("farming");
        words.add("building");
        words.add("fighting");
        words.add("fishing");
        words.add("crafting");
        words.add("main");
        words.add("all");
        e.setCompletions(getCompletions(buffer, words));
    }

    public void handleSSPlayerCheck(String buffer, Player p, TabCompleteEvent e) {
        if (!p.hasPermission("survivalskills.op")) return;
        ArrayList<String> words = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) words.add(player.getName());
        e.setCompletions(getCompletions(buffer, words));
    }

    public void handleSurvivalSkills(String buffer, Player p, TabCompleteEvent e) {
        String[] words = buffer.split(" ");
        if (words.length == 0) return;

        if (words[0].equalsIgnoreCase("/ss") || words[0].equalsIgnoreCase("/survivalskills")) {
            if (words.length == 2) {
                if (words[1].equalsIgnoreCase("xp") || words[1].equalsIgnoreCase("level")) {
                    handleSSPlayerCheck(buffer, p, e);
                }
                else if (words[1].equalsIgnoreCase("skillxp")) handleChangeSkillXP(buffer, p, e);
                else if (words[1].equalsIgnoreCase("togglereward") ||
                        words[1].equalsIgnoreCase("rewardlevel")) e.setCompletions(getCompletions(buffer, skillNames));
                else handleSS(buffer, p, e);
            }
            else if (words.length == 3 && !buffer.endsWith(" ") && words[1].equalsIgnoreCase("skillxp")) {
                handleChangeSkillXP(buffer, p, e);
            }
            else if (words.length == 3 && !buffer.endsWith(" ")) handleSSPlayerCheck(buffer, p, e);
            else if (words.length == 3) handleSSkills(buffer, p, e);
            else if (words.length == 4 && !buffer.endsWith(" ")) handleSSkills(buffer, p, e);
            else if (words.length > 3) handleSSChangers(buffer, p, e);
            else handleSS(buffer, p, e);
        }
    }

    public void handleSSChangers(String buffer, Player p, TabCompleteEvent e) {
        if (!p.hasPermission("survivalskills.op")) return;
        ArrayList<String> words = new ArrayList<>();
        words.add("add");
        words.add("remove");
        words.add("set");
        e.setCompletions(getCompletions(buffer, words));
    }

    public void handleChangeSkillXP(String buffer, Player p, TabCompleteEvent e) {
        if (!p.hasPermission("survivalskills.op")) return;
        ArrayList<String> words = new ArrayList<>();
        words.add("building");
        words.add("crafting");
        words.add("exploring");
        words.add("farming");
        words.add("fighting");
        words.add("fishing");
        words.add("mining");
        e.setCompletions(getCompletions(buffer, words));
    }

    public void handleTrails(Player p, String buffer, TabCompleteEvent e) {
        ArrayList<String> words = new ArrayList<>();
        PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);

        if (rewards.getReward("Main", "DustTrail").isApplied()) words.add("dust");
        if (rewards.getReward("Main", "WaterTrail").isApplied()) words.add("water");
        if (rewards.getReward("Main", "HappyTrail").isApplied()) words.add("happy");
        if (rewards.getReward("Main", "DragonTrail").isApplied()) words.add("dragon");
        if (rewards.getReward("Main", "ElectricTrail").isApplied()) words.add("electric");
        if (rewards.getReward("Main", "EnchantmentTrail").isApplied()) words.add("enchantment");
        if (rewards.getReward("Main", "OminousTrail").isApplied()) words.add("ominous");
        if (rewards.getReward("Main", "LoveTrail").isApplied()) words.add("love");
        if (rewards.getReward("Main", "FlameTrail").isApplied()) words.add("flame");
        if (rewards.getReward("Main", "BlueFlameTrail").isApplied()) words.add("blueflame");
        if (rewards.getReward("Main", "CherryTrail").isApplied()) words.add("cherry");
        if (rewards.getReward("Main", "RainbowTrail").isApplied()) words.add("rainbow");
        e.setCompletions(getCompletions(buffer, words));
    }

    public void handlePlayers(String buffer, TabCompleteEvent e) {
        ArrayList<String> words = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) words.add(player.getName());
        e.setCompletions(getCompletions(buffer, words));
    }

    public void handleBossMusic(String buffer, Player p, TabCompleteEvent e) {
        if (!p.hasPermission("survivalskills.op")) return;
        ArrayList<String> words = new ArrayList<>();
        words.add("villager");
        e.setCompletions(getCompletions(buffer, words));
    }

    /**
     * Takes in a list of words and a string. Checks to see how many words in the
     * given list match up to the string given
     * @param buffer String input
     * @param wordList List of words to check the input against
     * @return List of words that can be used in tab completion
     */
    public ArrayList<String> getCompletions(String buffer, ArrayList<String> wordList) {
        ArrayList<String> completions = new ArrayList<>();
        String[] segment = buffer.split(" ");
        if (segment.length == 1) return wordList;
        if (segment.length > 1 && buffer.endsWith(" ")) return wordList;

        for (String word : wordList) if (matchPrefix(segment[segment.length - 1], word)) completions.add(word);
        return completions;
    }

    /**
     * Takes an input string and a comparison string and checks if the input
     * string matches the comparison string.
     * If the input string is longer than the comparison string then there is no match
     * If the input string is shorter it can still be a match ("abc" matches with "abcd")
     * @param input The string that needs to be checked
     * @param name The string to compare to
     * @return boolean
     */
    public boolean matchPrefix(String input, String name) {
        // false if it is too long
        if (input.length() > name.length()) return false;
        // false if any letter doesn't match
        for (int i = 0; i < input.length(); i++) if (input.charAt(i) != name.charAt(i)) return false;
        return true;
    }

    public void createSkillNames(SurvivalSkills plugin) {
        for (Map.Entry<String, ArrayList<Reward>> rewards : plugin.getSkillManager().getDefaultPlayerRewards().getRewardList().entrySet()) {
            for (Reward reward : rewards.getValue()) skillNames.add(reward.getName());
        }
    }
}
