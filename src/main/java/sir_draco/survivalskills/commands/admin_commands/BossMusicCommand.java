package sir_draco.survivalskills.commands.admin_commands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.utils.ExiledBossMusic;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.Utils;

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
        if (!(sender instanceof Player p)) return false;

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
            // Look up the named player (case-insensitive). A missing player falls
            // through to the sender fallback below, matching the original behaviour
            // where getPlayer() returned null instead of throwing.
            songPlayer = Utils.findPlayer(strings[1]).orElse(null);
            if (exiledMusic.containsKey(songPlayer)) {
                exiledMusic.get(songPlayer).setDead(true);
                exiledMusic.remove(songPlayer);
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
