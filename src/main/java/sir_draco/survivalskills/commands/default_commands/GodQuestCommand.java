package sir_draco.survivalskills.commands.default_commands;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.god_questline.GodRecipeUI;
import sir_draco.survivalskills.god_questline.GodTrophyQuest;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;
import java.util.Map;

@SuppressWarnings("NullableProblems")
public class GodQuestCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public GodQuestCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("godquest");
        if (command != null)
            command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player p))
            return false;

        // Check if the player has an active god quest
        if (!plugin.getTrophyManager().getPlayerGodQuestData().containsKey(p.getUniqueId())) {
            p.sendRawMessage(ChatColor.RED + "You have not started the god quest yet");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        GodTrophyQuest quest = plugin.getTrophyManager().getPlayerGodQuestData().get(p.getUniqueId());
        if (quest == null) {
            p.sendRawMessage(ChatColor.RED + "You have not started the god quest yet");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        // Check what stage they are in
        // TODO: Find a better way so that this is not hardcoded
        int stage;
        if (quest.getPhase() >= 22 && quest.getPhase() <= 26)
            stage = 1;
        else if (quest.getPhase() >= 27 && quest.getPhase() <= 44)
            stage = 2;
        else if (quest.getPhase() == 45 || quest.getPhase() == 46)
            stage = 3;
        else if (quest.getPhase() == 48)
            stage = 4;
        else {
            p.sendRawMessage(ChatColor.RED + "There are no recipes for this stage of the God Quest");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        // Build the Recipe UI and open it
        GodRecipeUI ui = new GodRecipeUI(getRecipeList(stage));
        plugin.getGodListener().getOpenGodRecipeUI().put(p, ui);
        ui.open(p);
        return true;
    }

    public ArrayList<NamespacedKey> getRecipeList(int stage) {
        ArrayList<NamespacedKey> list = new ArrayList<>();
        for (Map.Entry<NamespacedKey, Integer> recipe : plugin.getGodRecipeKeys().entrySet()) {
            if (recipe.getValue() != stage)
                continue;
            list.add(recipe.getKey());
        }
        return list;
    }
}
