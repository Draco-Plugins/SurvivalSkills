package sir_draco.survivalskills.Commands.AdminCommands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.DragonBattle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.SurvivalSkills;

public class DragonStatusCommand implements CommandExecutor {

    public DragonStatusCommand() {
        PluginCommand command = SurvivalSkills.getInstance().getCommand("dragonstatus");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;

        World world = p.getWorld();
        boolean dragonKilled = world.hasMetadata("killedfirstdragon");
        p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        p.sendRawMessage(ChatColor.GREEN + "Dragon Killed: " + ChatColor.AQUA + dragonKilled);

        if (!world.getEnvironment().equals(World.Environment.THE_END)) return true;

        DragonBattle battle = world.getEnderDragonBattle();
        if (battle == null) {
            p.sendRawMessage(ChatColor.GREEN + "No Ender Dragon Alive");
            return true;
        }

        EnderDragon dragon = battle.getEnderDragon();
        if (dragon == null) {
            p.sendRawMessage(ChatColor.GREEN + "No Ender Dragon Alive");
            return true;
        }
        if (dragon.isDead()) {
            p.sendRawMessage(ChatColor.GREEN + "No Ender Dragon Alive");
            return true;
        }
        p.sendRawMessage(ChatColor.GREEN + "Ender Dragon Health: " + ChatColor.AQUA + dragon.getHealth());
        return true;
    }
}
