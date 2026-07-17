package sir_draco.survivalskills.god_questline.trial;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import sir_draco.survivalskills.boards.SkillScoreboard;
import sir_draco.survivalskills.god_questline.trial_mobs.WaveMob;
import sir_draco.survivalskills.rewards.RewardNotifications;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.super_enchanting.SuperEnchantingItems;
import sir_draco.survivalskills.super_enchanting.SuperEnchantingRules;
import sir_draco.survivalskills.utils.TrialUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Trial extends BukkitRunnable {

    // --- Constants ---
    private static final int TICKS_PER_SECOND = 20;
    private static final int WAVE_COUNTDOWN_SECONDS = 15;
    private static final int COUNTDOWN_SOUND_THRESHOLD = 3;
    private static final int WAVES_PER_DIFFICULTY = 5;
    private static final int BUILDING_BATCH_DIVISOR = 120;
    private static final int SPAWNING_SPOT_RADIUS = 15;
    private static final int SPAWNING_SPOT_STEP = 5;
    private static final int VERTICAL_SPAWN_LIMIT = 27;
    private static final int POST_WAVE_DELAY_TICKS = 40;
    private static final double BASE_WALK_SPEED = 0.3;
    private static final int TIME_BONUS_BASE = 3600;
    private static final int CONTAINMENT_INTERVAL = 40;
    private static final double PHANTOM_CAGE_MARGIN = 1.5;
    private static final double PHANTOM_RECOVERY_HEIGHT = 5.0;
    private static final double PHANTOM_RECOVERY_SPEED = 0.35;
    private static final int MIN_POINTS_AWARDED = 100;
    private static final int POINTS_DIVISOR = 10;
    private static final int MAX_CLOSEST_PLAYER_DISTANCE = 1000;
    private static final int REWARD_DISPLAY_DELAY_TICKS = 60;
    private static final int UPGRADE_GUI_DELAY_TICKS = 100;

    enum TrialState {
        BUILDING,
        COUNTDOWN,
        WAVE_ACTIVE,
        COMPLETED
    }

    private final ArrayList<Player> players = new ArrayList<>();
    private final ProtectedArea protectedArea;
    private final Location centerLocation;
    private final ArrayList<Location> spawningSpots = new ArrayList<>();
    private final Map<Integer, Wave> waves;
    private final HashMap<UUID, ArrayList<UUID>> spectators = new HashMap<>();
    private final Player trialMaster;
    private final int trueDifficulty;
    private final int difficulty;
    private final boolean existingStructure;
    private final int maxWave;

    private TrialState state = TrialState.BUILDING;
    private boolean waveEnding = false;
    private boolean solo = true;
    private int cycle;
    private int timeCycle;
    private int timer = WAVE_COUNTDOWN_SECONDS;
    private int timeSpent;
    private int waveNumber = 1;
    private int score;
    private int playerCount = 1;
    private Wave wave;

    public Trial(ArrayList<RelativeBlock> building, Player p, ProtectedArea protectedArea,
            Location centerLocation, int trueDifficulty) {
        trialMaster = p;
        this.protectedArea = protectedArea;
        this.centerLocation = centerLocation;
        this.trueDifficulty = trueDifficulty;
        this.difficulty = (int) Math.max(1, (double) ((trueDifficulty + 1) / 2));
        this.maxWave = this.difficulty * WAVES_PER_DIFFICULTY;
        this.existingStructure = false;
        this.waves = TrialManager.getWaveGenerator().getWavesForDifficulty(this.difficulty);
        loadBuilding(building);
    }

    public Trial(Player p, ProtectedArea protectedArea, Location centerLocation, int trueDifficulty) {
        trialMaster = p;
        this.protectedArea = protectedArea;
        this.centerLocation = centerLocation;
        this.trueDifficulty = trueDifficulty;
        this.difficulty = (int) Math.max(1, (double) ((trueDifficulty + 1) / 2));
        this.maxWave = this.difficulty * WAVES_PER_DIFFICULTY;
        this.existingStructure = true;
        this.waves = TrialManager.getWaveGenerator().getWavesForDifficulty(this.difficulty);
        generateSpawningSpots();
    }

    @Override
    public void run() {
        if (state == TrialState.BUILDING || state == TrialState.COMPLETED)
            return;

        updateScoreboards();

        if (state == TrialState.WAVE_ACTIVE) {
            handleActiveWave();
        } else {
            handleCountdown();
        }
    }

    private void handleActiveWave() {
        timeCycle++;
        if (timeCycle % TICKS_PER_SECOND == 0)
            timeSpent++;

        handleMobTargeting();
        keepPhantomsInCage();

        if (tryEndWave())
            return;

        if (cycle % CONTAINMENT_INTERVAL == 0)
            keepMobsInCage();
    }

    private void handleCountdown() {
        if (cycle % TICKS_PER_SECOND == 0) {
            timer--;

            String message = ChatColor.GRAY + "Wave " + ChatColor.AQUA + waveNumber
                    + ChatColor.GRAY + " in " + ChatColor.YELLOW + timer + ChatColor.GRAY + " seconds";
            for (Player p : players)
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(message));

            if (timer <= COUNTDOWN_SOUND_THRESHOLD && timer != 0)
                for (Player p : players)
                    p.playSound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);

            if (timer == 0) {
                cycle++;
                state = TrialState.WAVE_ACTIVE;
                spawnWave();
                for (Player p : players) {
                    p.playSound(p, Sound.ENTITY_WOLF_GROWL, 1, 1);
                    p.sendTitle(ChatColor.GRAY + "Wave " + ChatColor.AQUA + waveNumber, "", 5, 30, 5);
                }
            }
        }
        cycle++;
    }

    private void handleMobTargeting() {
        if (wave == null || wave.getWaveMobs().isEmpty())
            return;

        for (WaveMob mob : wave.getWaveMobs()) {
            if (mob.getEntity() == null || mob.getEntity().isDead())
                continue;
            if (!(mob.getEntity() instanceof Mob waveMob))
                continue;

            if (!isValidTrialTarget(waveMob.getTarget())) {
                Player target = getClosestPlayer(waveMob.getLocation());
                if (target != null)
                    waveMob.setTarget(target);
            }

            if (waveMob instanceof Phantom phantom) {
                Player target = getClosestPlayer(phantom.getLocation());
                if (target != null && !target.equals(phantom.getTarget()))
                    phantom.setTarget(target);
                phantom.setFireTicks(0);
            }
        }
    }

    private boolean isValidTrialTarget(Entity target) {
        return target instanceof Player player
                && player.isOnline()
                && !player.isDead()
                && players.contains(player);
    }

    private boolean tryEndWave() {
        if (wave == null)
            return false;

        int mobsLeft = wave.getMobsLeft();
        ChatColor color = mobsLeft > 1 ? ChatColor.YELLOW : ChatColor.RED;
        String message = ChatColor.GRAY + "Mobs Left: " + color + mobsLeft;
        for (Player p : players)
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(message));

        if (mobsLeft == 0 && !wave.isBossWave() && !waveEnding) {
            endWave();
            return true;
        }

        if (wave.isBossWave() && wave.getBoss() != null)
            wave.getBoss().setTarget(getClosestPlayer(wave.getBoss().getBoss().getLocation()));

        return false;
    }

    /**
     * Attempt to skip the countdown between waves for this trial. Returns true
     * if the skip was accepted and the next wave will start (or is already
     * starting). Only players who are part of the trial may trigger this.
     */
    public synchronized boolean attemptSkipCountdown(Player p) {
        if (!players.contains(p))
            return false;
        if (state != TrialState.COUNTDOWN)
            return false;
        if (waveNumber > maxWave)
            return false;
        if (timer <= 0)
            return false;

        timer = 1;
        return true;
    }

    public void loadBuilding(ArrayList<RelativeBlock> building) {
        int increment = Math.max(1, building.size() / BUILDING_BATCH_DIVISOR);
        new BukkitRunnable() {
            @Override
            public void run() {
                if (building.isEmpty()) {
                    generateSpawningSpots();
                    startTrial();
                    cancel();
                    return;
                }

                int amount = Math.min(increment, building.size());
                for (int i = 0; i < amount; i++) {
                    if (building.isEmpty())
                        continue;
                    RelativeBlock block = TrialUtils.popRandomBlock(building);
                    TrialUtils.convertBlockToRelative(block, centerLocation);
                }
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);
    }

    public void generateSpawningSpots() {
        for (int x = -SPAWNING_SPOT_RADIUS; x <= SPAWNING_SPOT_RADIUS; x += SPAWNING_SPOT_STEP) {
            for (int z = -SPAWNING_SPOT_RADIUS; z <= SPAWNING_SPOT_RADIUS; z += SPAWNING_SPOT_STEP) {
                if (x == 0 && z == 0)
                    continue;
                Location spot = validateSpawningSpot(centerLocation.clone().add(x, 0, z));
                if (spot == null)
                    continue;
                spawningSpots.add(spot);
            }
        }
    }

    public Location validateSpawningSpot(Location location) {
        while (location.getY() < centerLocation.getY() + VERTICAL_SPAWN_LIMIT) {
            Block block = location.getBlock();
            if (block.getType().isAir()
                    && block.getRelative(0, 1, 0).getType().isAir()
                    && block.getRelative(0, 2, 0).getType().isAir()
                    && block.getRelative(0, -1, 0).getType().isSolid()) {
                return location;
            }
            location.add(0, 1, 0);
        }
        return null;
    }

    public void spawnWave() {
        waveEnding = false;

        if (waveNumber > waves.size()) {
            Bukkit.getLogger().warning("No more waves available for wave number: " + waveNumber);
            endTrial();
            return;
        }

        if (spawningSpots.isEmpty()) {
            Bukkit.getLogger().warning("No valid spawning spots found for trial! Regenerating spawning spots...");
            generateSpawningSpots();

            if (spawningSpots.isEmpty()) {
                Bukkit.getLogger().severe("Failed to generate spawning spots for trial! Ending trial.");
                endTrial();
                return;
            }
        }

        wave = TrialManager.spawnWave(waves.get(waveNumber), spawningSpots, players, playerCount);

        if (wave == null) {
            Bukkit.getLogger().warning("Failed to spawn wave " + waveNumber + "! Ending trial.");
            endTrial();
            return;
        }

        notifySpectatorsOfWaveStart();
    }

    private void notifySpectatorsOfWaveStart() {
        if (spectators.isEmpty())
            return;
        for (ArrayList<UUID> spectatorList : spectators.values()) {
            for (UUID spectatorId : spectatorList) {
                Player spectator = Bukkit.getPlayer(spectatorId);
                if (spectator != null) {
                    spectator.sendRawMessage(ChatColor.GREEN + "Wave " + waveNumber + " started");
                    spectator.playSound(spectator, Sound.ENTITY_WOLF_GROWL, 1, 1);
                }
            }
        }
    }

    public void endWave() {
        if (waveEnding)
            return;
        if (state != TrialState.WAVE_ACTIVE)
            return;

        waveEnding = true;
        removeWaveMobs();

        for (Player p : players) {
            p.sendTitle("Wave " + waveNumber + " Complete", "", 5, 30, 5);
            p.playSound(p, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1, 1);
        }

        notifySpectatorsOfWaveEnd();

        new BukkitRunnable() {
            @Override
            public void run() {
                removeWaveMobs();
                state = TrialState.COUNTDOWN;
                for (Player p : players)
                    TrialManager.openRewardGUI(p, waveNumber);
                waveNumber++;
                timer = WAVE_COUNTDOWN_SECONDS;

                String message = ChatColor.GRAY + "Wave " + ChatColor.AQUA + waveNumber
                        + ChatColor.GRAY + " in " + ChatColor.YELLOW + timer + ChatColor.GRAY + " seconds";
                for (Player p : players)
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(message));
            }
        }.runTaskLater(SurvivalSkills.getInstance(), POST_WAVE_DELAY_TICKS);
    }

    private void notifySpectatorsOfWaveEnd() {
        if (spectators.isEmpty())
            return;
        for (ArrayList<UUID> spectatorList : spectators.values()) {
            for (UUID spectatorId : spectatorList) {
                Player spectator = Bukkit.getPlayer(spectatorId);
                if (spectator != null) {
                    spectator.sendRawMessage(ChatColor.GREEN + "Wave " + waveNumber + " completed");
                    spectator.playSound(spectator, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1, 1);
                }
            }
        }
    }

    public void keepMobsInCage() {
        List<WaveMob> waveMobs = wave.getWaveMobs();
        for (WaveMob mob : waveMobs) {
            if (mob.getEntity() == null || mob.getEntity().isDead())
                continue;

            if (mob.getEntity() instanceof Phantom) {
                continue;
            }

            if (protectedArea.boundingBox().contains(mob.getEntity().getLocation().toVector()))
                continue;
            mob.getEntity().teleport(centerLocation.clone().add(0.5, 1, 0.5));
        }

        for (Entity entity : wave.getExtraMobs()) {
            if (entity.isDead())
                continue;
            if (protectedArea.boundingBox().contains(entity.getLocation().toVector()))
                continue;
            entity.teleport(centerLocation.clone().add(0.5, 1, 0.5));
        }
    }

    /**
     * Phantoms move quickly enough to cross the cage between the slower generic containment
     * checks. Recover them every tick as soon as their hit position leaves an inset of the
     * arena bounds.
     */
    private void keepPhantomsInCage() {
        if (wave == null)
            return;

        BoundingBox cage = protectedArea.boundingBox();
        for (WaveMob waveMob : wave.getWaveMobs()) {
            if (!(waveMob.getEntity() instanceof Phantom phantom) || phantom.isDead())
                continue;

            if (containsPhantom(cage, phantom.getLocation()))
                continue;

            Player target = getClosestPlayer(phantom.getLocation());
            Location recoveryLocation = getPhantomRecoveryLocation(cage, target);
            phantom.teleport(recoveryLocation);
            if (target != null) {
                phantom.setTarget(target);
                Vector direction = target.getEyeLocation().toVector()
                        .subtract(recoveryLocation.toVector());
                if (direction.lengthSquared() > 0)
                    phantom.setVelocity(direction.normalize().multiply(PHANTOM_RECOVERY_SPEED));
            } else {
                phantom.setVelocity(new Vector());
            }
        }
    }

    static boolean containsPhantom(BoundingBox cage, Location location) {
        double minimumX = cage.getMinX() + PHANTOM_CAGE_MARGIN;
        double maximumX = cage.getMaxX() - PHANTOM_CAGE_MARGIN;
        double minimumY = cage.getMinY() + PHANTOM_CAGE_MARGIN;
        double maximumY = cage.getMaxY() - PHANTOM_CAGE_MARGIN;
        double minimumZ = cage.getMinZ() + PHANTOM_CAGE_MARGIN;
        double maximumZ = cage.getMaxZ() - PHANTOM_CAGE_MARGIN;
        return location.getX() >= minimumX && location.getX() <= maximumX
                && location.getY() >= minimumY && location.getY() <= maximumY
                && location.getZ() >= minimumZ && location.getZ() <= maximumZ;
    }

    private Location getPhantomRecoveryLocation(BoundingBox cage, Player target) {
        Location preferred = target == null
                ? centerLocation.clone().add(0.5, PHANTOM_RECOVERY_HEIGHT, 0.5)
                : target.getLocation().clone().add(0, PHANTOM_RECOVERY_HEIGHT, 0);
        double minimumX = cage.getMinX() + PHANTOM_CAGE_MARGIN;
        double maximumX = cage.getMaxX() - PHANTOM_CAGE_MARGIN;
        double minimumY = cage.getMinY() + PHANTOM_CAGE_MARGIN;
        double maximumY = cage.getMaxY() - PHANTOM_CAGE_MARGIN;
        double minimumZ = cage.getMinZ() + PHANTOM_CAGE_MARGIN;
        double maximumZ = cage.getMaxZ() - PHANTOM_CAGE_MARGIN;
        preferred.setX(clamp(preferred.getX(), minimumX, maximumX));
        preferred.setY(clamp(preferred.getY(), minimumY, maximumY));
        preferred.setZ(clamp(preferred.getZ(), minimumZ, maximumZ));
        return preferred;
    }

    private static double clamp(double value, double minimum, double maximum) {
        if (minimum > maximum)
            return (minimum + maximum) / 2;
        return Math.max(minimum, Math.min(maximum, value));
    }

    public void removeWaveMobs() {
        if (wave == null)
            return;

        if (wave.isBossWave()) {
            wave.getBoss().death();
            return;
        }

        List<WaveMob> waveMobs = wave.getWaveMobs();
        if (!waveMobs.isEmpty())
            for (WaveMob mob : waveMobs)
                if (mob.getEntity() != null)
                    mob.getEntity().remove();

        List<Entity> extraMobs = wave.getExtraMobs();
        if (!extraMobs.isEmpty())
            for (Entity entity : extraMobs)
                entity.remove();
    }

    public void setWave(int waveNumber) {
        this.waveNumber = waveNumber;
    }

    public void startTrial() {
        if (players.isEmpty()) {
            Bukkit.getLogger().warning("Cannot start trial with no players!");
            return;
        }

        for (Player p : players) {
            p.setAllowFlight(false);
            p.setFlying(false);
            p.setFoodLevel(20);

            AttributeInstance maxHealthAttribute = p.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttribute != null)
                p.setHealth(maxHealthAttribute.getValue());

            PlayerTrialUpgrades upgrades = PlayerTrialUpgrades.getPlayerUpgrades(p);
            List<ItemStack> startingItems = TrialTree.getStartingItems(upgrades);

            for (ItemStack item : startingItems)
                p.getInventory().addItem(item);

            p.teleport(centerLocation.clone().add(0.5, 1, 0.5));
            p.playSound(p, Sound.ENTITY_WOLF_GROWL, 1, 1);
            p.sendTitle(ChatColor.GRAY + "Wave " + ChatColor.AQUA + waveNumber, "", 5, 30, 5);

            p.setWalkSpeed((float) (BASE_WALK_SPEED * TrialTree.getSpeedMultiplier(
                    TrialUpgradeManager.getPlayerUpgrades(p))));
        }

        playerCount = players.size();
        state = TrialState.WAVE_ACTIVE;
        spawnWave();
        TrialUtils.removeGroundItemsInProtectedArea(protectedArea);
    }

    public void restartTrial() {
        cycle = 0;
        timer = WAVE_COUNTDOWN_SECONDS;
        waveNumber = 1;
        waveEnding = false;

        for (Player p : players) {
            p.getInventory().clear();
            AttributeInstance maxHealthAttribute = p.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttribute != null)
                p.setHealth(maxHealthAttribute.getValue());
            p.setFoodLevel(20);
        }
        removeWaveMobs();
        wave = null;
        TrialUtils.removeGroundItemsInProtectedArea(protectedArea);

        startTrial();
    }

    private void removeAllSpectators() {
        if (spectators.isEmpty())
            return;

        // Copy entries to avoid ConcurrentModification during removal
        List<Map.Entry<UUID, ArrayList<UUID>>> entries = new ArrayList<>(spectators.entrySet());
        for (Map.Entry<UUID, ArrayList<UUID>> entry : entries) {
            Player target = Bukkit.getPlayer(entry.getKey());
            for (UUID spectatorId : new ArrayList<>(entry.getValue())) {
                Player spectator = Bukkit.getPlayer(spectatorId);
                if (spectator != null)
                    TrialUtils.removeTrialSpectator(spectator, target);
            }
        }
    }

    public void endTrial() {
        removeAllSpectators();
        state = TrialState.COMPLETED;
        TrialManager.unregisterTrial(this);
        TrialUtils.removeGroundItemsInProtectedArea(protectedArea);
        removeWaveMobs();
        cancel();

        for (Player p : players) {
            SurvivalSkills.getInstance().getFishingListener().getDisabledAutoTrash().remove(p);
            p.getInventory().clear();
            TrialManager.removeTrialScoreboard(p);
            SkillScoreboard.updateScoreboard(p);
        }
    }

    public void deleteTrial() {
        removeAllSpectators();
        state = TrialState.COMPLETED;
        TrialUtils.clearTrialBuilding(centerLocation);
        TrialUtils.removeGroundItemsInProtectedArea(protectedArea);
        TrialManager.unregisterTrial(this);
        removeWaveMobs();

        if (trialMaster != null)
            TrialManager.removeProtectedArea(trialMaster.getUniqueId());

        for (Player p : players)
            p.getInventory().clear();
        cancel();
    }

    public void quitTrial(Player p) {
        if (trialMaster.equals(p)) {
            endTrial();
            return;
        }

        if (players.size() == 1) {
            endTrial();
            return;
        }

        players.remove(p);
        // Keep playerCount in sync when a player leaves mid-trial so scoring
        // remains proportional. In co-op trials with 3+ players, this ensures
        // remaining players aren't penalized for someone else quitting.
        // Note: this concession doesn't cover players who quit before the
        // first wave starts, but in practice startTrial is called before
        // any player interaction during the trial.
        if (playerCount > 1)
            playerCount--;

        // The trial stays active for the remaining players, so drop only this player
        // from the fast player->trial lookup index.
        TrialManager.removePlayerFromTrialIndex(p);

        p.getInventory().clear();
        p.teleport(p.getBedLocation());
        p.sendRawMessage(ChatColor.RED + "You have left the trial");
        p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        TrialManager.removeTrialScoreboard(p);
        SkillScoreboard.updateScoreboard(p);
    }

    public void completeTrial() {
        removeAllSpectators();
        state = TrialState.COMPLETED;
        TrialManager.unregisterTrial(this);
        TrialUtils.removeGroundItemsInProtectedArea(protectedArea);
        removeWaveMobs();

        for (Player p : players) {
            p.getInventory().clear();
            TrialManager.removeTrialScoreboard(p);
            SkillScoreboard.updateScoreboard(p);
        }

        int timeBonus = (TIME_BONUS_BASE * difficulty) - timeSpent;
        if (timeBonus > 0)
            changeScore(timeBonus);
        cancel();

        if (solo && trueDifficulty == 7) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + trialMaster.getName()
                    + " permission set survivalskills.creative true");
            trialMaster.sendTitle(ChatColor.GOLD + "Trial Complete",
                    ChatColor.GRAY + "You can now use creative mode",
                    5, 30, 5);
            Bukkit.broadcastMessage(
                    ChatColor.GOLD + "Congratulations to " + ChatColor.AQUA + trialMaster.getName() + ChatColor.GOLD
                            + " for becoming a " + ChatColor.AQUA + "God" + ChatColor.GOLD + "!");
            for (Player player : Bukkit.getOnlinePlayers())
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
        }

        for (Player p : players) {
            TrialManager.addCompletedGamemode(p, trueDifficulty);
            awardTrialFragments(p);

            int finalScore = score / Math.max(1, playerCount);
            boolean newHighScore;
            if (solo) {
                newHighScore = updateLeaderboardIfBetter(p, SkillCategory.SOLO_TRIALS, finalScore);
            } else {
                newHighScore = updateLeaderboardIfBetter(p, SkillCategory.COOP_TRIALS, finalScore);
            }

            scheduleRewardDisplay(p, finalScore, newHighScore);
            scheduleUpgradeGUI(p, finalScore);
        }
    }

    private void awardTrialFragments(Player player) {
        int fragmentAmount = SuperEnchantingRules.getTrialFragmentReward(difficulty);
        ItemStack fragments = SuperEnchantingItems.createTrialFragment(
                SurvivalSkills.getInstance(), fragmentAmount);
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(fragments);
        leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        player.sendMessage(ChatColor.LIGHT_PURPLE + "You earned " + fragmentAmount + " trial fragment"
                + (fragmentAmount == 1 ? "!" : "s!"));
    }

    private boolean updateLeaderboardIfBetter(Player player, SkillCategory category, int finalScore) {
        int currentScore = SurvivalSkills.getInstance().getLeaderboardTracker()
                .get(player.getUniqueId()).getScore(category);
        if (currentScore < finalScore) {
            SurvivalSkills.getInstance().getLeaderboardTracker()
                    .get(player.getUniqueId()).setScore(category, finalScore);
            return true;
        }
        return false;
    }

    private void scheduleRewardDisplay(Player p, int finalScore, boolean newHighScore) {
        final int time = timeSpent;
        new BukkitRunnable() {
            @Override
            public void run() {
                p.sendTitle(ChatColor.YELLOW + "Final Score",
                        ChatColor.GRAY + "" + finalScore, 5, 50, 5);
                String timeString = RewardNotifications.cooldown(time);
                p.sendRawMessage(ChatColor.YELLOW + "Trial completed in " + timeString);

                if (newHighScore) {
                    String type = solo ? "Solo" : "Co-op";
                    p.sendRawMessage(ChatColor.YELLOW + "New " + type + " High Score: "
                            + ChatColor.AQUA + finalScore + ChatColor.YELLOW + "!");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                }
            }
        }.runTaskLater(SurvivalSkills.getInstance(), REWARD_DISPLAY_DELAY_TICKS);
    }

    private void scheduleUpgradeGUI(Player p, int finalScore) {
        new BukkitRunnable() {
            @Override
            public void run() {
                int pointsAwarded = Math.max(MIN_POINTS_AWARDED, finalScore / POINTS_DIVISOR);
                for (Player player : players) {
                    TrialUpgradeManager.awardTrialPoints(player, pointsAwarded);
                    player.sendMessage(ChatColor.GREEN + "You earned " + pointsAwarded + " trial points!");
                    TrialGUI.openUpgradeGUI(player);
                }
            }
        }.runTaskLater(SurvivalSkills.getInstance(), UPGRADE_GUI_DELAY_TICKS);
    }

    public Player getClosestPlayer(Location location) {
        Player closest = null;
        double distance = MAX_CLOSEST_PLAYER_DISTANCE;
        for (Player p : players) {
            double newDistance = p.getLocation().distance(location);
            if (closest == null) {
                closest = p;
                distance = newDistance;
                continue;
            }
            if (newDistance >= distance)
                continue;
            closest = p;
            distance = newDistance;
        }
        return closest;
    }

    public void changeScore(int amount) {
        score += amount;
    }

    public void initializeScoreboards() {
        if (players.isEmpty())
            return;
        for (Player p : players)
            SkillScoreboard.initializeTrialScoreboard(p);
    }

    public void updateScoreboards() {
        if (state == TrialState.WAVE_ACTIVE || state == TrialState.COUNTDOWN) {
            for (Player p : players)
                SkillScoreboard.updateTrialScoreboard(p, score, timeSpent);
        }

        if (spectators.isEmpty())
            return;

        for (Map.Entry<UUID, ArrayList<UUID>> entry : spectators.entrySet()) {
            Player target = Bukkit.getPlayer(entry.getKey());
            if (target == null)
                continue;
            String displayName = target.getDisplayName();
            double health = target.getHealth();
            int foodLevel = target.getFoodLevel();
            for (UUID spectatorId : entry.getValue()) {
                Player spectator = Bukkit.getPlayer(spectatorId);
                if (spectator != null)
                    SkillScoreboard.updateTrialSpectatorScoreboard(
                            spectator, displayName, health, foodLevel, score, timeSpent);
            }
        }
    }

    public void addSpectator(Player player, Player target) {
        spectators.computeIfAbsent(target.getUniqueId(), k -> new ArrayList<>()).add(player.getUniqueId());
    }

    public void removeSpectator(Player player) {
        UUID playerId = player.getUniqueId();
        for (ArrayList<UUID> spectatorList : spectators.values())
            if (spectatorList.remove(playerId))
                return;
    }

    // --- Getters and Setters ---

    public ArrayList<Player> getPlayers() {
        return players;
    }

    public ProtectedArea getProtectedArea() {
        return protectedArea;
    }

    public Wave getWave() {
        return wave;
    }

    public int getWaveNumber() {
        return waveNumber;
    }

    public boolean isBuildingCreated() {
        return state != TrialState.BUILDING;
    }

    public int getMaxWave() {
        return maxWave;
    }

    public boolean isActiveWave() {
        return state == TrialState.WAVE_ACTIVE;
    }

    public Player getTrialMaster() {
        return trialMaster;
    }

    public void setSolo(boolean solo) {
        this.solo = solo;
    }

    public boolean isExistingStructure() {
        return existingStructure;
    }
}
