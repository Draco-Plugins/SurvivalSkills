package sir_draco.survivalskills.commands.admin_commands;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.abilities.Grave;
import sir_draco.survivalskills.SurvivalSkills;

import java.io.File;
import java.util.Map;

@SuppressWarnings("NullableProblems")
public class ResetAllCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public ResetAllCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("ssresetall");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player p)) return false;
        if (!p.isOp()) return false;

        // Reset all player data

        // Reset files
        File trophyFile = new File(plugin.getDataFolder(), "trophydata.yml");
        if (!trophyFile.exists()) plugin.saveResource("trophydata.yml", true);
        plugin.getTrophyData().set("", null);
        try {
            plugin.getTrophyData().save(trophyFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        File leaderboardFile = new File(plugin.getDataFolder(), "leaderboard.yml");
        if (!leaderboardFile.exists()) plugin.saveResource("leaderboard.yml", true);
        plugin.getLeaderboardData().set("", null);
        try {
            plugin.getLeaderboardData().save(leaderboardFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        File permaTrashFile = new File(plugin.getDataFolder(), "permatrash.yml");
        if (!permaTrashFile.exists()) plugin.saveResource("permatrash.yml", true);
        plugin.getPermaTrashData().set("", null);
        try {
            plugin.getPermaTrashData().save(permaTrashFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        File toolBeltFile = new File(plugin.getDataFolder(), "toolbelt.yml");
        if (!toolBeltFile.exists()) plugin.saveResource("toolbelt.yml", true);
        plugin.getToolBeltData().set("", null);
        try {
            plugin.getToolBeltData().save(toolBeltFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        File godQuestFile = new File(plugin.getDataFolder(), "godquests.yml");
        if (!godQuestFile.exists()) plugin.saveResource("godquests.yml", true);
        FileConfiguration godQuestData = YamlConfiguration.loadConfiguration(godQuestFile);
        godQuestData.set("", null);
        try {
            godQuestData.save(godQuestFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        File gravesFile = new File(plugin.getDataFolder(), "graves.yml");
        if (!gravesFile.exists()) plugin.saveResource("graves.yml", true);
        FileConfiguration gravesData = YamlConfiguration.loadConfiguration(gravesFile);
        gravesData.set("", null);
        try {
            gravesData.save(gravesFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        File potionBagsFile = new File(plugin.getDataFolder(), "potionbags.yml");
        if (!potionBagsFile.exists()) plugin.saveResource("potionbags.yml", true);
        FileConfiguration potionBagsData = YamlConfiguration.loadConfiguration(potionBagsFile);
        potionBagsData.set("", null);
        try {
            potionBagsData.save(potionBagsFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        File dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) plugin.saveResource("playerdata.yml", true);
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        data.set("", null);
        try {
            data.save(dataFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Reset trackers
        plugin.getLeaderboardTracker().clear();
        plugin.getScoreboardTracker().clear();
        plugin.getSkillManager().getPlayerSkills().clear();
        plugin.getTrophyManager().disableTrophies();
        plugin.getTrophyManager().getTrophyTracker().clear();
        plugin.getTrophyManager().getPlayerGodQuestData().clear();
        plugin.getTrophyManager().getTrophies().clear();

        for (Map.Entry<Location, Grave> grave : plugin.getMainListener().getGraves().entrySet()) {
            grave.getValue().removeGrave(false);
        }
        plugin.getMainListener().getGraves().clear();
        plugin.getGodListener().getPotionBags().clear();
        plugin.getFishingListener().getPermaTrash().clear();

        p.sendRawMessage(ChatColor.AQUA + "All player data has been reset!");
        p.sendRawMessage(ChatColor.RED + "Please reset the server to apply changes.");
        p.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
        return true;
    }
}
