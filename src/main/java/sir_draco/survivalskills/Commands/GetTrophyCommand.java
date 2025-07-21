package sir_draco.survivalskills.Commands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import sir_draco.survivalskills.SurvivalSkills;

public class GetTrophyCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public GetTrophyCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        plugin.getCommand("gettrophy").setExecutor(this);
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;
        if (strings.length != 1) {
            p.sendRawMessage(ChatColor.RED + "Please specify the trophy id you want");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }
        int type;
        try {
            type = Integer.parseInt(strings[0]);
        } catch (Exception e) {
            type = 1;
        }
        ItemStack item = plugin.getTrophyItem(type);
        p.getInventory().addItem(item);
        p.sendRawMessage(ChatColor.GREEN + "You have received the trophy");
        p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        return true;
    }
}
