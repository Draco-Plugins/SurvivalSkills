package sir_draco.survivalskills.commands.default_commands;

import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.god_questline.GodRecipeUI;
import sir_draco.survivalskills.god_questline.GodTrophyQuest;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class GodQuestCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public GodQuestCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        Objects.requireNonNull(plugin.getCommand("godquest")).setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(ChatColor.RED + "This command can only be used by a player.");
            return true;
        }

        GodTrophyQuest quest = plugin.getTrophyManager().getPlayerGodQuestData().get(p.getUniqueId());
        if (quest == null) {
            sendQuestError(p);
            return true;
        }

        Optional<Integer> stage = quest.getStage();
        if (stage.isEmpty()) {
            sendNoRecipeError(p);
            return true;
        }

        GodRecipeUI ui = new GodRecipeUI(getRecipeList(stage.get()));
        plugin.getGodListener().registerGodRecipeUI(p, ui);
        ui.open(p);
        return true;
    }

    private ArrayList<NamespacedKey> getRecipeList(int stage) {
        return plugin.getGodRecipeKeys().entrySet().stream()
                .filter(e -> e.getValue() == stage)
                .map((Map.Entry<NamespacedKey, Integer> entry) -> entry.getKey())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void sendQuestError(Player p) {
        p.sendRawMessage(ChatColor.RED + "You have not started the god quest yet");
        p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    }

    private void sendNoRecipeError(Player p) {
        p.sendRawMessage(ChatColor.RED + "There are no recipes for this stage of the God Quest");
        p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    }
}
