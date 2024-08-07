package sir_draco.survivalskills.Commands.DefaultCommands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.Rewards.PlayerRewards;
import sir_draco.survivalskills.Rewards.Reward;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;

public class ToggleSpeedCommand implements CommandExecutor {

    private final SurvivalSkills plugin;
    private final ArrayList<Player> disabledPlayers = new ArrayList<>();

    public ToggleSpeedCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("togglespeed");
        if (command != null) command.setExecutor(this);
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;

        Player p = (Player) sender;
        if (disabledPlayers.contains(p)) {
            PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
            int level = plugin.getSkillManager().getSkill(p.getUniqueId(), "Exploring").getLevel();

            // Get the player's walk and swim speed based on the exploring level
            float walkSpeed = 0.2f;
            double swimSpeed = 0.0;
            for (Reward reward : rewards.getRewardList().get("Exploring")) {
                if (reward.getLevel() > level || !reward.isEnabled() || !reward.isApplied()) continue;
                switch (reward.getName()) {
                    case "SwimI":
                        if (1.2 > swimSpeed) swimSpeed = 1.2;
                        break;
                    case "SwimII":
                        if (1.4 > swimSpeed) swimSpeed = 1.4;
                        break;
                    case "SwimIII":
                        if (1.6 > swimSpeed) swimSpeed = 1.6;
                        break;
                    case "SwimIV":
                        if (1.8 > swimSpeed) swimSpeed = 1.8;
                        break;
                    case "SwimV":
                        if (2.0 > swimSpeed) swimSpeed = 2.0;
                        break;
                    case "SpeedI":
                        if (0.22f > walkSpeed) walkSpeed = 0.22f;
                        break;
                    case "SpeedII":
                        if (0.24f > walkSpeed) walkSpeed = 0.24f;
                        break;
                    case "SpeedIII":
                        if (0.26f > walkSpeed) walkSpeed = 0.26f;
                        break;
                    case "SpeedIV":
                        if (0.28f > walkSpeed) walkSpeed = 0.28f;
                        break;
                    case "SpeedV":
                        if (0.30f > walkSpeed) walkSpeed = 0.30f;
                        break;
                }
            }
            p.setWalkSpeed(walkSpeed);
            rewards.setSwimSpeed(swimSpeed);
            
            disabledPlayers.remove(p);
            p.sendMessage("Speed enabled");
        } else {
            p.setWalkSpeed(0.2f);
            plugin.getSkillManager().getPlayerRewards(p).setSwimSpeed(0);
            disabledPlayers.add(p);
            p.sendMessage("Speed disabled");
        }
        return true;
    }
}
