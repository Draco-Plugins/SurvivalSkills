package sir_draco.survivalskills.Commands.SkillCommands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import sir_draco.survivalskills.Rewards.Reward;
import sir_draco.survivalskills.SurvivalSkills;

public class ToolBeltCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public ToolBeltCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("toolbelt");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;

        // Check if the tool belt reward
        Reward reward = plugin.getSkillManager().getPlayerRewards(p).getReward("Mining", "ToolBelt");
        if (reward == null) {
            p.sendRawMessage(ChatColor.RED + "Tool Belts are not enabled");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            Bukkit.getLogger().warning("Could not find the tool belt reward");
            return true;
        }
        if (!reward.isEnabled()) {
            p.sendRawMessage(ChatColor.RED + "Tool Belts are not enabled");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }
        if (!reward.isApplied()) {
            p.sendRawMessage(ChatColor.RED + "You need to be mining level " + ChatColor.AQUA +
                    reward.getLevel() + ChatColor.RED + " to use Tool Belts");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        // Check if the player has a tool belt
        if (!plugin.getMiningListener().getToolBelts().containsKey(p)) {
            Inventory toolBelt = plugin.getAbilityManager().loadToolBelt(p);
            if (toolBelt == null) toolBelt = Bukkit.createInventory(p, 9, "Tool Belt");
            plugin.getMiningListener().getToolBelts().put(p, toolBelt);
            p.openInventory(toolBelt);
            p.playSound(p, Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);
        }
        else {
            p.openInventory(plugin.getMiningListener().getToolBelts().get(p));
            p.playSound(p, Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);
        }
        return true;
    }
}
