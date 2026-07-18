package sir_draco.survivalskills.commands.skill_commands;

import java.util.Objects;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.abilities.AbilityTimer;
import sir_draco.survivalskills.rewards.Reward;
import sir_draco.survivalskills.rewards.RewardNotifications;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.SurvivalSkills;

public class FlightCommand implements CommandExecutor {

    public static final String FLIGHT_I = "FlightI";
    public static final String FLIGHT_II = "FlightII";
    public static final String FLIGHT_III = "FlightIII";
    public static final String FLIGHT_IV = "FlightIV";
    private static final float BASE_FLY_SPEED = 0.025f;
    private static final Sound SOUND_PICKUP = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
    private static final Sound SOUND_DENY = Sound.ENTITY_ENDERMAN_TELEPORT;

    private final SurvivalSkills plugin;

    public FlightCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("flight");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (!(sender instanceof Player p)) return false;

        // Check if the ability is enabled on this server
        if (!isFlightEnabled()) {
            p.sendRawMessage(ChatColor.RED + "Flight is not enabled on this server");
            return false;
        }

        // Resolve the current flight timer (if any). Players with Flight IV applied bypass
        // the timed-ability cooldown flow entirely.
        AbilityTimer timer = plugin.getAbilityManager().getAbility(p, "Flight");
        if (timer != null && !isFlightApplied(p, FLIGHT_IV)) return handleFlightTimer(args, p, timer);
        if (p.getAllowFlight()) return disableFlightToggle(p);

        if (!canUseFlight(p)) return true;

        FlightTier tier = resolveTier(p);
        if (!tier.isTimed()) return enableUnlimitedFlight(p, tier.speedMultiplier());
        startFlightTimer(p, tier);
        return true;
    }

    // ---------------- Flight tiers ----------------

    /**
     * Self-documenting flight power levels. Each timed tier pairs an active window with a
     * cooldown; {@link #UNLIMITED} grants permanent flight with no timer at all.
     */
    public enum FlightTier {
        BASE(1, 300, 3600),        // Flight I  -> 5m active / 60m reset
        ENHANCED(2, 900, 1800),     // Flight II -> 15m active / 30m reset
        EXTENDED(3, 1800, 1800),    // Flight III -> 30m active / 30m reset
        UNLIMITED(4, 0, 0);          // Flight IV -> no timer

        private final int speedMultiplier;
        private final int activeTime;
        private final int resetTime;

        FlightTier(int speedMultiplier, int activeTime, int resetTime) {
            this.speedMultiplier = speedMultiplier;
            this.activeTime = activeTime;
            this.resetTime = resetTime;
        }

        public int speedMultiplier() { return speedMultiplier; }
        public int activeTime() { return activeTime; }
        public int resetTime() { return resetTime; }
        public boolean isTimed() { return this != UNLIMITED; }
    }

    /** Picks the highest tier the player has earned (reward configured and applied). */
    private FlightTier resolveTier(Player p) {
        if (!hasFlightReward(p, FLIGHT_II)) return FlightTier.BASE;
        if (!hasFlightReward(p, FLIGHT_III)) return FlightTier.ENHANCED;
        if (!hasFlightReward(p, FLIGHT_IV)) return FlightTier.EXTENDED;
        return FlightTier.UNLIMITED;
    }

    // ---------------- Activation ----------------

    private void startFlightTimer(Player p, FlightTier tier) {
        float flySpeed = BASE_FLY_SPEED * tier.speedMultiplier();
        p.setAllowFlight(true);
        p.setFlying(true);
        p.setFlySpeed(flySpeed);

        AbilityTimer timer = new AbilityTimer(plugin, "Flight", p, tier.activeTime(), tier.resetTime());
        timer.setFlightSpeed(flySpeed);
        configureFlightTimer(timer);
        timer.runTaskTimer(plugin, 0, 20);
        plugin.getAbilityManager().addAbility(p, timer);

        p.sendRawMessage(ChatColor.GREEN + "You have enabled your flight for "
                + RewardNotifications.cooldown(tier.activeTime()));
        p.playSound(p, SOUND_PICKUP, 1, 1);
    }

    private boolean enableUnlimitedFlight(Player p, int speedMultiplier) {
        p.setAllowFlight(true);
        p.setFlying(true);
        p.setFlySpeed(BASE_FLY_SPEED * speedMultiplier);
        p.sendRawMessage(ChatColor.GREEN + "You have enabled your flight!");
        p.playSound(p, SOUND_PICKUP, 1, 1);
        return true;
    }

    // ---------------- Existing timer interactions ----------------

    /** Handles the three possible states when a flight timer already exists. */
    private boolean handleFlightTimer(String[] args, Player p, AbilityTimer timer) {
        if (!timer.isActive()) return notifyCooldown(p, timer);
        if (hasLeftArg(args)) return showTimeLeft(p, timer);
        return deactivateFlight(p, timer);
    }

    private boolean showTimeLeft(Player p, AbilityTimer timer) {
        p.sendRawMessage(ChatColor.GREEN + "You have " + ChatColor.AQUA
                + RewardNotifications.cooldown(timer.getActiveTimeLeft()) + ChatColor.GREEN + " minutes of flight time left!");
        p.playSound(p, SOUND_PICKUP, 1, 1);
        return true;
    }

    private boolean deactivateFlight(Player p, AbilityTimer timer) {
        p.sendRawMessage(ChatColor.YELLOW + "You have deactivated your flight!");
        p.playSound(p, SOUND_PICKUP, 1, 1);
        removeFlight(p);
        timer.endAbility();
        return true;
    }

    private boolean notifyCooldown(Player p, AbilityTimer timer) {
        p.sendRawMessage(ChatColor.RED + "You can use flight again in: "
                + RewardNotifications.cooldown(timer.getTimeTillReset()));
        p.playSound(p, SOUND_DENY, 1, 1);
        return false;
    }

    private boolean disableFlightToggle(Player p) {
        p.setAllowFlight(false);
        p.setFlying(false);
        p.sendRawMessage(ChatColor.YELLOW + "You have disabled your flight!");
        p.playSound(p, SOUND_PICKUP, 1, 1);
        return true;
    }

    private boolean canUseFlight(Player p) {
        if (hasFlightReward(p, FLIGHT_I)) return true;
        p.sendRawMessage(ChatColor.RED + "You need to be building level " + ChatColor.AQUA
                + getFlightRequiredLevel() + ChatColor.RED + " to use Flight");
        p.playSound(p, SOUND_DENY, 1, 1);
        return false;
    }

    private boolean hasLeftArg(String[] args) {
        return args.length > 0 && args[0].equalsIgnoreCase("left");
    }

    // ---------------- Flight timer hooks ----------------

    /**
     * Wires the flight-specific countdown warnings and expiry behavior onto a generic
     * {@link AbilityTimer}, so no separate {@code FlyingTimer} task is required. Shared with
     * {@link AbilityManager} for offline-restored flight.
     */
    public static void configureFlightTimer(AbilityTimer timer) {
        timer.setOnActiveTick(FlightCommand::onFlightTick);
        timer.setOnExpire(FlightCommand::removeFlight);
    }

    /** Per-second warnings while flight is still active (mirrors the old FlyingTimer thresholds). */
    private static void onFlightTick(Player p, Integer secondsLeft) {
        int remainingSeconds = Objects.requireNonNull(secondsLeft, "secondsLeft");
        if (!p.isOnline()) return;
        if (remainingSeconds == 60) {
            p.sendRawMessage(ChatColor.YELLOW + "You have 1 minute left of flight time!");
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1);
        } else if (remainingSeconds == 30) {
            p.sendRawMessage(ChatColor.YELLOW + "You have 30 seconds left of flight time!");
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 0);
        } else if (remainingSeconds == 10) {
            p.sendRawMessage(ChatColor.YELLOW + "You have 10 seconds left of flight time!");
        } else if (remainingSeconds <= 3) {
            p.playSound(p, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1, 1);
        }
    }

    /** Disables flight and notifies the player (used both on natural expiry and manual deactivation). */
    private static void removeFlight(Player p) {
        if (!p.isOnline()) return;
        p.setAllowFlight(false);
        p.setFlying(false);
        p.sendRawMessage(ChatColor.YELLOW + "Your flight time has expired!");
        p.playSound(p, Sound.ENTITY_SHEEP_SHEAR, 1, 1);
    }

    // ---------------- Reward lookups (with null safety) ----------------

    /** A reward counts as "earned" only when it is both enabled (configured) and applied to the player. */
    private boolean hasFlightReward(Player p, String rewardName) {
        Reward reward = playerReward(p, rewardName);
        return reward != null && reward.isEnabled() && reward.isApplied();
    }

    private boolean isFlightApplied(Player p, String rewardName) {
        Reward reward = playerReward(p, rewardName);
        return reward != null && reward.isApplied();
    }

    private boolean isFlightEnabled() {
        Reward reward = defaultReward(FLIGHT_I);
        return reward != null && reward.isEnabled();
    }

    private int getFlightRequiredLevel() {
        Reward reward = defaultReward(FLIGHT_I);
        return reward != null ? reward.getLevel() : 0;
    }

    private Reward playerReward(Player p, String rewardName) {
        return plugin.getSkillManager().getPlayerRewards(p).getReward(SkillCategory.BUILDING, rewardName);
    }

    private Reward defaultReward(String rewardName) {
        return plugin.getSkillManager().getDefaultPlayerRewards().getReward(SkillCategory.BUILDING, rewardName);
    }
}
