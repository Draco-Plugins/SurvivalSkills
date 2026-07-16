package sir_draco.survivalskills.commands.admin_commands;

import org.bukkit.Bukkit;
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
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;

public class ResetAllCommand implements CommandExecutor {

    private static final String TROPHY_DATA_FILE = "trophydata.yml";
    private static final String LEADERBOARD_FILE = "leaderboard.yml";
    private static final String PERMA_TRASH_FILE = "permatrash.yml";
    private static final String TOOL_BELT_FILE = "toolbelt.yml";
    private static final String GOD_QUEST_FILE = "godquests.yml";
    private static final String GRAVES_FILE = "graves.yml";
    private static final String POTION_BAGS_FILE = "potionbags.yml";
    private static final String PLAYER_DATA_FILE = "playerdata.yml";

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

        resetDataFiles();
        resetInMemoryState();

        p.sendRawMessage(ChatColor.AQUA + "All player data has been reset!");
        p.sendRawMessage(ChatColor.RED + "Please reset the server to apply changes.");
        p.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
        return true;
    }

    private void resetDataFiles() {
        resetDataFile(TROPHY_DATA_FILE, false, plugin.getTrophyData());
        resetDataFile(LEADERBOARD_FILE, false, plugin.getLeaderboardData());
        resetDataFile(PERMA_TRASH_FILE, false, plugin.getPermaTrashData());
        resetDataFile(TOOL_BELT_FILE, false, plugin.getToolBeltData());
        resetDataFile(GOD_QUEST_FILE, true, null);
        resetDataFile(GRAVES_FILE, true, null);
        resetDataFile(POTION_BAGS_FILE, true, null);
        resetDataFile(PLAYER_DATA_FILE, true, null);
    }

    private void resetDataFile(String fileName, boolean shouldLoadFresh, FileConfiguration existingData) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) plugin.saveResource(fileName, true);

        FileConfiguration data = shouldLoadFresh ? YamlConfiguration.loadConfiguration(file) : existingData;
        data.set("", null);
        try {
            data.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.WARNING,
                    String.format("[SurvivalSkills] Failed to reset %s", fileName), e);
        }
    }

    private void resetInMemoryState() {
        plugin.getLeaderboardTracker().clear();
        plugin.getScoreboardTracker().clear();
        plugin.getSkillManager().clearPlayerData();
        plugin.getTrophyManager().disableTrophies();
        plugin.getTrophyManager().getTrophyTracker().clear();
        plugin.getTrophyManager().getPlayerGodQuestData().clear();
        plugin.getTrophyManager().getTrophies().clear();

        for (Map.Entry<Location, Grave> grave : plugin.getMainListener().getGraves().entrySet()) {
            grave.getValue().removeGrave(false);
        }
        plugin.getMainListener().getGraves().clear();
        plugin.getGodListener().clearPotionBags();
        plugin.getFishingListener().getPermaTrash().clear();
    }
}
