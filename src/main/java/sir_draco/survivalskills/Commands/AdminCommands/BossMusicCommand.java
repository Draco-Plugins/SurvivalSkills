package sir_draco.survivalskills.Commands.AdminCommands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.Bosses.ExiledBossMusic;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.HashMap;

public class BossMusicCommand implements CommandExecutor {

    private final SurvivalSkills plugin;
    private final HashMap<Player, ExiledBossMusic> exiledMusic = new HashMap<>();

    public BossMusicCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("bossmusic");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;

        if (exiledMusic.containsKey(p)) {
            exiledMusic.get(p).setDead(true);
            exiledMusic.remove(p);
            p.sendRawMessage(ChatColor.GREEN + "Boss music stopped.");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            return true;
        }

        if (strings.length < 1) {
            p.sendRawMessage(ChatColor.RED + "Usage: /bossmusic <song>");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        String song = strings[0];
        Player songPlayer = null;
        if (strings.length > 1) {
            String player = strings[1];
            try {
                songPlayer = plugin.getServer().getPlayer(player);
                if (exiledMusic.containsKey(songPlayer)) {
                    exiledMusic.get(songPlayer).setDead(true);
                    exiledMusic.remove(songPlayer);
                }
            } catch (Exception e) {
                p.sendRawMessage(ChatColor.RED + "Player not found.");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
        }
        if (songPlayer == null) songPlayer = p;

        if (song.equalsIgnoreCase("villager")) {
            ExiledBossMusic music = new ExiledBossMusic(songPlayer);
            exiledMusic.put(songPlayer, music);
            music.runTaskTimerAsynchronously(plugin, 3, 2);
        }
        return true;
    }
}
