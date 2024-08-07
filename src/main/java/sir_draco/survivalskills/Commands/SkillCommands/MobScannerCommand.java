package sir_draco.survivalskills.Commands.SkillCommands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;

public class MobScannerCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public MobScannerCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("mobscanner");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;
        if (!plugin.getSkillManager().getDefaultPlayerRewards().getReward("Fighting", "MobScanner").isEnabled()) {
            p.sendRawMessage(ChatColor.RED + "Mob Scanner is not enabled on this server");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return false;
        }

        if (!plugin.getSkillManager().getPlayerRewards(p).getReward("Fighting", "MobScanner").isApplied() && !plugin.isForced(p, strings)) {
            if (p.hasPermission("survivalskills.op")) {
                p.sendRawMessage(ChatColor.RED + "To force Mob Scanner use: " + ChatColor.AQUA + "/mobscanner force");
            }
            p.sendRawMessage(ChatColor.RED + "You need to be fighting level " + ChatColor.AQUA
                    + plugin.getSkillManager().getDefaultPlayerRewards().getReward("Fighting", "MobScanner").getLevel()
                    + ChatColor.RED + " to use Mob Scanner");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        // Find all mobs in a 50 block radius of the player and make sure they are glowing
        ArrayList<Entity> glowingEntities = new ArrayList<>();
        for (Entity ent : p.getWorld().getNearbyEntities(p.getLocation(), 50, 50, 50)) {
            if (ent instanceof Player) continue;
            if (!(ent instanceof LivingEntity)) continue;
            if (ent.getType().equals(EntityType.ITEM)) continue;
            ent.setGlowing(true);
            glowingEntities.add(ent);
        }

        // Add the mobs to the list of scanned mobs
        plugin.getAbilityManager().addScannedMobs(glowingEntities);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Entity ent : glowingEntities) ent.setGlowing(false);
                plugin.getAbilityManager().removeScannedMobs(glowingEntities);
            }
        }.runTaskLater(plugin, 20 * 60);

        p.sendRawMessage(ChatColor.GREEN + "Nearby Mobs Glowing for 60 seconds");
        p.playSound(p, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1, 1);
        return true;
    }
}
