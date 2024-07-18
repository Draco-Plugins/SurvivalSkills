package sir_draco.survivalskills.Commands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import sir_draco.survivalskills.Abilities.AbilityTimer;
import sir_draco.survivalskills.Rewards.RewardNotifications;
import sir_draco.survivalskills.SurvivalSkills;

public class NightVisionCommand implements CommandExecutor {

    private final SurvivalSkills plugin;

    public NightVisionCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("ssnv");
        if (command != null) command.setExecutor(this);
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;

        // Check for level requirements reset and active times and radius
        if (!plugin.getSkillManager().getDefaultPlayerRewards().getReward("Mining", "NightVisionI").isEnabled()) {
            p.sendRawMessage(ChatColor.RED + "Night Vision is not enabled on this server");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return false;
        }

        // Check if night vision is being disabled or if there is a cooldown
        AbilityTimer timer = plugin.getAbilityManager().getAbility(p, "NightVision");
        if (p.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            p.removePotionEffect(PotionEffectType.NIGHT_VISION);
            p.sendRawMessage(ChatColor.YELLOW + "Night Vision has been disabled!");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            if (timer != null) timer.endAbility();
            return true;
        }
        else if (timer != null) {
            p.sendRawMessage(ChatColor.RED + "You can use night vision again in: " + RewardNotifications.cooldown(timer.getTimeTillReset()));
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        // Enable night vision
        if (!plugin.getSkillManager().getPlayerRewards(p).getReward("Mining", "NightVisionI").isApplied() && !plugin.isForced(p, strings)) {
            if (p.hasPermission("survivalskills.op")) {
                p.sendRawMessage(ChatColor.RED + "To force nightvision use: " + ChatColor.AQUA + "/ssnv force");
            }
            p.sendRawMessage(ChatColor.RED + "You need to be mining level " + ChatColor.AQUA
                    + plugin.getSkillManager().getDefaultPlayerRewards().getReward("Mining", "NightVisionI").getLevel()
                    + ChatColor.RED + " to use Night Vision");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }
        else if (!plugin.getSkillManager().getPlayerRewards(p).getReward("Mining", "NightVisionII").isApplied()) {
            timer = new AbilityTimer(plugin, "NightVision", p, 900, 900);
            timer.runTaskTimerAsynchronously(plugin, 0, 20);
            p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 18000, 0, false, false, false));
        }
        else p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, -1, 1, false, false, false));

        p.sendRawMessage(ChatColor.GREEN + "Night Vision has been enabled!");
        p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

        return true;
    }
}
