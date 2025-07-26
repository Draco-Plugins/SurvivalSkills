package sir_draco.survivalskills.god_questline;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.boards.SkillScoreboard;
import sir_draco.survivalskills.god_questline.TrialMobs.WaveMob;
import sir_draco.survivalskills.rewards.RewardNotifications;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.TrialGUI;
import sir_draco.survivalskills.utils.TrialUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Trial extends BukkitRunnable {

    private final ArrayList<Player> players = new ArrayList<>();
    private final ProtectedArea protectedArea;
    private final Location centerLocation;
    private final ArrayList<Location> spawningSpots = new ArrayList<>();
    private final HashMap<Integer, Wave> waves;
    private final HashMap<Player, ArrayList<Player>> spectators = new HashMap<>();
    private final Player trialMaster;
    private final int trueDifficulty;
    private final int difficulty;
    private final boolean existingStructure;
    private final int maxWave;

    private boolean solo = true;
    private boolean buildingCreated = false;
    private boolean spawningSpotsGenerated = false;
    private boolean activeWave = false;
    private boolean waveSpawned = false;
    private boolean waveEnded = false;
    private int cycle;
    private int timeCycle;
    private int timer = 15;
    private int timeSpent = 0;
    private int waveNumber = 1;
    private int score = 0;
    private int playerCount = 1;
    private Wave wave = null;

    public Trial(ArrayList<RelativeBlock> building, Player p, ProtectedArea protectedArea, Location centerLocation, int trueDifficulty) {
        trialMaster = p;
        this.protectedArea = protectedArea;
        this.centerLocation = centerLocation;
        this.trueDifficulty = trueDifficulty;
        this.difficulty = (int) Math.max(1, (double) ((trueDifficulty + 1) / 2));
        this.maxWave = difficulty * 5;
        this.existingStructure = false;
        waves = TrialManager.getWaveGenerator().getWavesForDifficulty(difficulty);
        loadBuilding(building);
        // Remove generateSpawningSpots() from here - it will be called after building is complete
    }

    public Trial(Player p, ProtectedArea protectedArea, Location centerLocation, int trueDifficulty) {
        trialMaster = p;
        this.protectedArea = protectedArea;
        this.centerLocation = centerLocation;
        this.trueDifficulty = trueDifficulty;
        this.difficulty = (int) Math.max(1, (double) ((trueDifficulty + 1) / 2));
        this.maxWave = difficulty * 5;
        this.existingStructure = true;
        waves = TrialManager.getWaveGenerator().getWavesForDifficulty(difficulty);
        generateSpawningSpots(); // Keep this for existing structures since they're already built
    }

    @Override
    public void run() {
        if (!buildingCreated) return;
        if (!spawningSpotsGenerated) return;

        updateScoreboards();
        if (activeWave) {
            if (!waveSpawned) spawnWave();
            timeCycle++;
            if (timeCycle % 20 == 0)
                timeSpent++;

            // Make sure all mobs are targeting players
            if (wave != null) {
                if (!wave.getWaveMobs().isEmpty()) {
                    for (WaveMob mob : wave.getWaveMobs()) {
                        if (mob.getEntity() == null || mob.getEntity().isDead()) continue;
                        if (!(mob.getEntity() instanceof Mob waveMob)) continue;

                        // Target a random player
                        if (waveMob.getTarget() == null) {
                            Player target = getClosestPlayer(waveMob.getLocation());
                            if (target != null) waveMob.setTarget(target);
                        }
                    }
                }
            }
        }

        if (!activeWave) {
            if (cycle % 20 == 0) {
                timer--;

                String message = ChatColor.GRAY + "Wave " + ChatColor.AQUA + waveNumber + ChatColor.GRAY + " in " +
                        ChatColor.YELLOW + timer + ChatColor.GRAY + " seconds";
                for (Player p : players)
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(message));

                if (timer <= 3 && timer != 0)
                    for (Player p : players)
                        p.playSound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);

                if (timer == 0) {
                    cycle++;
                    activeWave = true;
                    for (Player p : players) {
                        p.playSound(p, Sound.ENTITY_WOLF_GROWL, 1, 1);
                        p.sendTitle(ChatColor.GRAY + "Wave " + ChatColor.AQUA + waveNumber, "", 5, 30, 5);
                    }
                }
            }
            cycle++;
        }

        if (activeWave && wave != null) {
            int mobsLeft = wave.getMobsLeft();
            ChatColor color;
            if (mobsLeft > 1) color = ChatColor.YELLOW;
            else color = ChatColor.RED;
            String message = ChatColor.GRAY + "Mobs Left: " + color + mobsLeft;
            for (Player p : players)
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(message));

            if (mobsLeft == 0 && !wave.isBossWave() && !waveEnded) {
                endWave();
                return;
            }

            if (wave.isBossWave() && wave.getBoss() != null)
                wave.getBoss().setTarget(getClosestPlayer(wave.getBoss().getBoss().getLocation()));

            if (cycle % 40 == 0) keepMobsInCage();
        }
    }

    public void loadBuilding(ArrayList<RelativeBlock> building) {
        int increment = building.size() / 120;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (building.isEmpty()) {
                    // Generate spawning spots after building is complete
                    generateSpawningSpots();
                    startTrial();
                    cancel();
                    return;
                }

                int amount = Math.min(increment, building.size());
                for (int i = 0; i < amount; i++) {
                    if (building.isEmpty()) continue;
                    RelativeBlock block = TrialUtils.getRandomBlock(building);
                    TrialUtils.convertBlockToRelative(block, centerLocation);
                }
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);
    }

    public void generateSpawningSpots() {
        // Generate spawning spots for the mobs at 25 locations around the centerLocation
        for (int x = -15; x <= 15; x += 5) {
            for (int z = -15; z <= 15; z += 5) {
                if (x == 0 && z == 0) continue;
                // Make sure the spawning spot has 3 air blocks above it
                Location spot = validateSpawningSpot(centerLocation.clone().add(x, 0, z));
                if (spot == null) continue;
                spawningSpots.add(spot);
            }
        }
        spawningSpotsGenerated = true;
    }

    public Location validateSpawningSpot(Location location) {
        // Check if the location is a valid spawning spot
        while (location.getY() < centerLocation.getY() + 27) {
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
        waveEnded = false;
        waveSpawned = true;
        activeWave = true;

        if (waveNumber > waves.size()) {
            Bukkit.getLogger().warning("No more waves available for wave number: " + waveNumber);
            endTrial();
            return;
        }

        // Add validation for spawning spots
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

        // Additional safety check
        if (wave == null) {
            Bukkit.getLogger().warning("Failed to spawn wave " + waveNumber + "! Ending trial.");
            endTrial();
            return;
        }

        for (ArrayList<Player> spectatorList : spectators.values()) {
            for (Player player : spectatorList) {
                player.sendRawMessage(ChatColor.GREEN + "Wave " + waveNumber + " started");
                player.playSound(player, Sound.ENTITY_WOLF_GROWL, 1, 1);
            }
        }
    }

    public void endWave() {
        if (waveEnded) return;
        if (!activeWave) return;
        waveEnded = true;
        removeWaveMobs();

        for (Player p : players) {
            p.sendTitle("Wave " + waveNumber + " Complete", "", 5, 30, 5);
            p.playSound(p, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1, 1);
        }

        for (ArrayList<Player> spectatorList : spectators.values()) {
            for (Player player : spectatorList) {
                player.sendRawMessage(ChatColor.GREEN + "Wave " + waveNumber + " completed");
                player.playSound(player, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1, 1);
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                removeWaveMobs();
                activeWave = false;
                waveSpawned = false;
                for (Player p : players)
                    TrialManager.openRewardGUI(p, waveNumber);
                waveNumber++;
                timer = 15;

                String message = ChatColor.GRAY + "Wave " + ChatColor.AQUA + waveNumber + ChatColor.GRAY + " in " +
                        ChatColor.YELLOW + timer + ChatColor.GRAY + " seconds";
                for (Player p : players)
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(message));
            }
        }.runTaskLater(SurvivalSkills.getInstance(), 40);
    }

    public void keepMobsInCage() {
        for (WaveMob mob : wave.getWaveMobs()) {
            if (mob.getEntity() == null) continue;
            if (mob.getEntity().isDead()) continue;
            if (protectedArea.boundingBox().contains(mob.getEntity().getLocation().toVector())) continue;
            mob.getEntity().teleport(centerLocation.clone().add(0.5, 1, 0.5));
        }

        for (Entity entity : wave.getExtraMobs()) {
            if (entity.isDead()) continue;
            if (protectedArea.boundingBox().contains(entity.getLocation().toVector())) continue;
            entity.teleport(centerLocation.clone().add(0.5, 1, 0.5));
        }
    }

    public void removeWaveMobs() {
        if (wave == null) return;

        if (wave.isBossWave()) {
            wave.getBoss().death();
            return;
        }

        if (!wave.getWaveMobs().isEmpty())
            for (WaveMob mob : wave.getWaveMobs())
                if (mob.getEntity() != null) mob.getEntity().remove();

        if (!wave.getExtraMobs().isEmpty())
            for (Entity entity : wave.getExtraMobs())
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

            // heal all players with their max health
            AttributeInstance maxHealthAttribute = p.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttribute != null) p.setHealth(maxHealthAttribute.getValue());

            // Get player upgrades and apply starting items
            PlayerTrialUpgrades upgrades = PlayerTrialUpgrades.getPlayerUpgrades(p);
            List<ItemStack> startingItems = TrialTree.getStartingItems(upgrades);

            for (ItemStack item : startingItems) {
                p.getInventory().addItem(item);
            }

            p.teleport(centerLocation.clone().add(0.5, 1, 0.5));
            p.playSound(p, Sound.ENTITY_WOLF_GROWL, 1, 1);
            p.sendTitle(ChatColor.GRAY + "Wave " + ChatColor.AQUA + waveNumber, "", 5, 30, 5);

            p.setWalkSpeed((float) (0.3 * TrialTree.getSpeedMultiplier(TrialUpgradeManager.getPlayerUpgrades(p))));
        }

        playerCount = players.size();
        buildingCreated = true;
        activeWave = true;
        TrialUtils.removeGroundItemsInProtectedArea(protectedArea);
    }

    public void restartTrial() {
        cycle = 0;
        timer = 15;
        waveNumber = 1;
        waveSpawned = false;

        for (Player p : players) {
            p.getInventory().clear();
            AttributeInstance maxHealthAttribute = p.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealthAttribute != null) p.setHealth(maxHealthAttribute.getValue());
            p.setFoodLevel(20);
        }
        removeWaveMobs();
        wave = null;
        TrialUtils.removeGroundItemsInProtectedArea(protectedArea);

        startTrial();
    }

    public void endTrial() {
        // Remove spectators first while the trial is still in the list
        if (!spectators.isEmpty()) {
            for (Map.Entry<Player, ArrayList<Player>> specatorLists : spectators.entrySet()) {
                if (specatorLists.getValue() == null || specatorLists.getValue().isEmpty()) continue;
                List<Player> spectatorsToRemove = new ArrayList<>(specatorLists.getValue());
                for (Player spectator : spectatorsToRemove)
                    TrialUtils.removeTrialSpectator(spectator, specatorLists.getKey());
            }
        }

        // Now remove the trial from the manager
        TrialManager.getTrials().remove(this);
        TrialUtils.removeGroundItemsInProtectedArea(protectedArea);
        removeWaveMobs();
        cancel();

        for (Player p : players) {
            SurvivalSkills.getInstance().getFishingListener().getDisabledAutoTrash().remove(p);
            p.getInventory().clear();
            TrialManager.getTrialScoreboards().remove(p);
            SkillScoreboard.updateScoreboard(SurvivalSkills.getInstance(), p);
        }
    }

    public void deleteTrial() {
        // Remove spectators first while the trial is still in the list
        for (Map.Entry<Player, ArrayList<Player>> specatorLists : spectators.entrySet())
            for (Player spectator : specatorLists.getValue())
                TrialUtils.removeTrialSpectator(spectator, specatorLists.getKey());

        // Now remove the trial and clean up
        TrialUtils.clearTrialBuilding(centerLocation);
        TrialUtils.removeGroundItemsInProtectedArea(protectedArea);
        TrialManager.getTrials().remove(this);
        removeWaveMobs();

        if (trialMaster != null) TrialManager.getProtectedAreas().remove(trialMaster.getUniqueId());
        for (Player p : players) p.getInventory().clear();
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
        p.getInventory().clear();
        p.teleport(p.getBedLocation());
        p.sendRawMessage(ChatColor.RED + "You have left the trial");
        p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
        TrialManager.getTrialScoreboards().remove(p);
        SkillScoreboard.updateScoreboard(SurvivalSkills.getInstance(), p);
    }

    public void completeTrial() {
        // Remove spectators first while the trial is still in the list
        if (!spectators.isEmpty()) {
            for (Map.Entry<Player, ArrayList<Player>> specatorLists : spectators.entrySet()) {
                if (specatorLists.getValue() == null || specatorLists.getValue().isEmpty()) continue;
                List<Player> spectatorsToRemove = new ArrayList<>(specatorLists.getValue());
                for (Player spectator : spectatorsToRemove)
                    TrialUtils.removeTrialSpectator(spectator, specatorLists.getKey());
            }
        }

        // Now remove the trial from the manager
        TrialManager.getTrials().remove(this);
        TrialUtils.removeGroundItemsInProtectedArea(protectedArea);
        removeWaveMobs();

        for (Player p : players) {
            p.getInventory().clear();
            TrialManager.getTrialScoreboards().remove(p);
            SkillScoreboard.updateScoreboard(SurvivalSkills.getInstance(), p);
        }

        int timeBonus = (3600 * difficulty) - timeSpent;
        if (timeBonus > 0) changeScore(timeBonus);
        cancel();

        if (solo && trueDifficulty == 7) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + trialMaster.getName()
                    + " permission set survivalskills.creative true");
            trialMaster.sendTitle(ChatColor.GOLD + "Trial Complete", ChatColor.GRAY + "You can now use creative mode", 5, 30, 5);
            Bukkit.broadcastMessage(ChatColor.GOLD + "Congratulations to " + ChatColor.AQUA + trialMaster.getName() + ChatColor.GOLD
                    + " for becoming a " + ChatColor.AQUA + "God" + ChatColor.GOLD + "!");
            for (Player player : Bukkit.getOnlinePlayers()) player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
        }

        for (Player p : players) {
            // Add the difficulty to the player's completed gamemodes
            if (!TrialManager.getPlayerGamemodesBeaten().containsKey(p)) {
                TrialManager.getPlayerGamemodesBeaten().put(p, new ArrayList<>());
            }

            if (!TrialManager.getPlayerGamemodesBeaten().get(p).contains(trueDifficulty))
                TrialManager.getPlayerGamemodesBeaten().get(p).add(trueDifficulty);

            boolean newHighScore;
            if (solo) {
                if (SurvivalSkills.getInstance().getLeaderboardTracker().get(p.getUniqueId()).getTrialScore() < score / playerCount) {
                    newHighScore = true;
                    SurvivalSkills.getInstance().getLeaderboardTracker().get(p.getUniqueId()).setTrialScore(score / playerCount);
                } else newHighScore = false;
            }
            else {
                if (SurvivalSkills.getInstance().getLeaderboardTracker().get(p.getUniqueId()).getCoopTrialScore() < score / playerCount) {
                    newHighScore = true;
                    SurvivalSkills.getInstance().getLeaderboardTracker().get(p.getUniqueId()).setCoopTrialScore(score / playerCount);
                } else newHighScore = false;
            }

            new BukkitRunnable() {
                final int time = timeSpent;
                @Override
                public void run() {
                    p.sendTitle(ChatColor.YELLOW + "Final Score", ChatColor.GRAY + "" + (score / playerCount), 5, 50,
                                5);
                    String timeSpent = RewardNotifications.cooldown(time);
                    p.sendRawMessage(ChatColor.YELLOW + "Trial completed in " + timeSpent);

                    if (newHighScore) {
                        String type;
                        if (solo) type = "Solo";
                        else type = "Co-op";
                        p.sendRawMessage(ChatColor.YELLOW + "New " + type + " High Score: " + ChatColor.AQUA
                                                 + (score / playerCount) + ChatColor.YELLOW + "!");
                        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                    }
                }
            }.runTaskLater(SurvivalSkills.getInstance(), 60);

            new BukkitRunnable() {
                @Override
                public void run() {
                    int pointsAwarded = Math.max(100, score / (playerCount * 10));
                    for (Player p : players) {
                        TrialUpgradeManager.awardTrialPoints(p, pointsAwarded);
                        p.sendMessage(ChatColor.GREEN + "You earned " + pointsAwarded + " trial points!");
                        TrialGUI.openUpgradeGUI(p);
                    }
                }
            }.runTaskLater(SurvivalSkills.getInstance(), 100);
        }
    }

    public Player getClosestPlayer(Location location) {
        Player closest = null;
        double distance = 1000;
        for (Player p : players) {
            double newDistance = p.getLocation().distance(location);
            if (closest == null) {
                closest = p;
                distance = newDistance;
                continue;
            }

            if (newDistance >= distance) continue;
            closest = p;
            distance = newDistance;
        }
        return closest;
    }

    public void changeScore(int amount) {
        score += amount;
    }

    public void initializeScoreboards() {
        if (players.isEmpty()) return;
        for (Player p : players)
            SkillScoreboard.initializeTrialScoreboard(p);
    }

    public void updateScoreboards() {
        if (activeWave)
            for (Player p : players)
                SkillScoreboard.updateTrialScoreboard(p, score, timeSpent);

        if (spectators.isEmpty()) return;
        for (Map.Entry<Player, ArrayList<Player>> spectatorList : spectators.entrySet())
            for (Player player : spectatorList.getValue())
                SkillScoreboard.updateTrialSpectatorScoreboard(player, spectatorList.getKey().getDisplayName(),
                        spectatorList.getKey().getHealth(), spectatorList.getKey().getFoodLevel(), score, timeSpent);
    }

    public void addSpectator(Player player, Player target) {
        if (spectators.get(target) == null) {
            ArrayList<Player> players = new ArrayList<>();
            players.add(player);
            spectators.put(target, players);
        }
        else spectators.get(target).add(player);
    }

    public void removeSpectator(Player player) {
        for (ArrayList<Player> spectatorLists : spectators.values())
            if (spectatorLists.contains(player)) {
                spectatorLists.remove(player);
                return;
            }
    }

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
        return buildingCreated;
    }

    public int getMaxWave() {
        return maxWave;
    }

    public boolean isActiveWave() {
        return activeWave;
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
