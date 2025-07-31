package sir_draco.survivalskills.commands.skill_commands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.abilities.AbilityTimer;
import sir_draco.survivalskills.abilities.FlyingTimer;
import sir_draco.survivalskills.rewards.RewardNotifications;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("NullableProblems")
public class FlightCommand implements CommandExecutor {

    public static final String BUILDING = "Building";
    public static final String FLIGHT_I = "FlightI";
    public static final String FLIGHT_IV = "FlightIV";
    private final SurvivalSkills plugin;
    private final HashMap<Player, FlyingTimer> flyingTimers = new HashMap<>();

    public FlightCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("flight");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player p)) return false;

        // Check if the player can use the command
        if (!plugin.getSkillManager().getDefaultPlayerRewards().getReward(BUILDING, FLIGHT_I).isEnabled()) {
            p.sendRawMessage(ChatColor.RED + "Flight is not enabled on this server");
            return false;
        }

        // Check if the player has a cooldown
        AbilityTimer timer = plugin.getAbilityManager().getAbility(p, "Flight");
        if (timer != null && !plugin.getSkillManager().getPlayerRewards(p).getReward(BUILDING, FLIGHT_IV).isApplied()) {
            return checkForActiveTimer(strings, p, timer);
        }
        else if (p.getAllowFlight()) {
            p.setAllowFlight(false);
            p.setFlying(false);
            p.sendRawMessage(ChatColor.YELLOW + "You have disabled your flight!");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            return true;
        }

        // Give flight
        int resetTime;
        int activeTime;
        int speed;
        float baseSpeed = 0.025f;
        if (canUseFlight(p)) return true;

        if (!plugin.getSkillManager().getPlayerRewards(p).getReward(BUILDING, "FlightII").isEnabled()
                || !plugin.getSkillManager().getPlayerRewards(p).getReward(BUILDING, "FlightII").isApplied()) {
            resetTime = 3600; // 60 minutes
            activeTime = 300; // 5 minutes
            speed = 1;
        }
        else if (!plugin.getSkillManager().getPlayerRewards(p).getReward(BUILDING, "FlightIII").isEnabled() ||
                !plugin.getSkillManager().getPlayerRewards(p).getReward(BUILDING, "FlightIII").isApplied()) {
            resetTime = 1800; // 30 minutes
            activeTime = 900; // 15 minutes
            speed = 2;
        }
        else if (!plugin.getSkillManager().getPlayerRewards(p).getReward(BUILDING, FLIGHT_IV).isEnabled() ||
                !plugin.getSkillManager().getPlayerRewards(p).getReward(BUILDING, FLIGHT_IV).isApplied()){
            resetTime = 1800; // 30 minutes
            activeTime = 1800; // 30 minutes
            speed = 3;
        }
        else {
            p.setAllowFlight(true);
            p.setFlying(true);
            speed = 4;
            p.setFlySpeed(baseSpeed * speed);
            p.sendRawMessage(ChatColor.GREEN + "You have enabled your flight!");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            return true;
        }

        startFlightTimer(p, baseSpeed, speed, activeTime, resetTime);
        return true;
    }

    private void startFlightTimer(Player p, float baseSpeed, int speed, int activeTime, int resetTime) {
        p.setAllowFlight(true);
        p.setFlying(true);
        p.setFlySpeed(baseSpeed * speed);
        AbilityTimer abilityTimer = new AbilityTimer(plugin, "Flight", p, activeTime, resetTime);
        abilityTimer.setFlightSpeed(baseSpeed * speed);
        abilityTimer.runTaskTimerAsynchronously(plugin, 0, 20);
        plugin.getAbilityManager().addAbility(p, abilityTimer);
        FlyingTimer flyingTimer = new FlyingTimer(p, activeTime);
        flyingTimers.put(p, flyingTimer);
        flyingTimer.runTaskTimerAsynchronously(plugin, 0, 20);
        p.sendRawMessage(ChatColor.GREEN + "You have enabled your flight for " + ChatColor.AQUA
                + (activeTime / 60) + ChatColor.GREEN + " minutes!");
        p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
    }

    private boolean canUseFlight(Player p) {
        if (!plugin.getSkillManager().getPlayerRewards(p).getReward(BUILDING, FLIGHT_I).isEnabled()
            || !plugin.getSkillManager().getPlayerRewards(p).getReward(BUILDING, FLIGHT_I).isApplied()) {
            p.sendRawMessage(ChatColor.RED + "You need to be building level " + ChatColor.AQUA
                    + plugin.getSkillManager().getDefaultPlayerRewards().getReward(BUILDING, FLIGHT_I).getLevel()
                    + ChatColor.RED + " to use Flight");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }
        return false;
    }

    private boolean checkForActiveTimer(String[] strings, Player p, AbilityTimer timer) {
        if (timer.isActive()) {
            if (strings.length > 0 && strings[0].equalsIgnoreCase("left")) {
                p.sendRawMessage(ChatColor.GREEN + "You have " + ChatColor.AQUA
                        + RewardNotifications.cooldown(timer.getActiveTimeLeft()) + ChatColor.GREEN + " minutes of flight time left!");
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                return true;
            }

            // Inform the player flight is being deactivated
            p.sendRawMessage(ChatColor.YELLOW + "You have deactivated your flight!");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);

            if (flyingTimers.containsKey(p)) {
                flyingTimers.get(p).removeFlight(p);
                flyingTimers.remove(p);
            }
            timer.endAbility();
        }
        else {
            p.sendRawMessage(ChatColor.RED + "You can use flight again in: " + RewardNotifications.cooldown(timer.getTimeTillReset()));
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return false;
        }
        return true;
    }

    public Map<Player, FlyingTimer> getFlyingTimers() {
        return flyingTimers;
    }
}
