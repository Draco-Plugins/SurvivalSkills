package sir_draco.survivalskills.GodQuestline;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.GodQuestline.TrialMobs.WaveMob;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.Utils.TrialUtils;

import java.util.ArrayList;

public class Trial extends BukkitRunnable {

    private final Player p;
    private final ProtectedArea protectedArea;
    private final Location centerLocation;
    private final ArrayList<Location> spawningSpots = new ArrayList<>();
    private final int maxWave;

    private boolean buildingCreated = false;
    private boolean spawningSpotsGenerated = false;
    private boolean activeWave = false;
    private boolean waveSpawned = false;
    private boolean waveEnded = false;
    private int cycle;
    private int timer = 15;
    private int waveNumber = 1;
    private Wave wave = null;

    public Trial(ArrayList<RelativeBlock> building, Player p, ProtectedArea protectedArea, Location centerLocation, int maxWave) {
        this.p = p;
        this.protectedArea = protectedArea;
        this.centerLocation = centerLocation;
        this.maxWave = maxWave;
        generateSpawningSpots();
        loadBuilding(building);
    }

    public Trial(Player p, ProtectedArea protectedArea, Location centerLocation, int maxWave) {
        this.p = p;
        this.protectedArea = protectedArea;
        this.centerLocation = centerLocation;
        this.maxWave = maxWave;
        generateSpawningSpots();
        startTrial();
    }

    @Override
    public void run() {
        if (!buildingCreated) return;
        if (!spawningSpotsGenerated) return;

        if (activeWave && !waveSpawned) spawnWave();

        if (!activeWave) {
            if (cycle % 20 == 0) {
                String message = ChatColor.GRAY + "Wave " + ChatColor.AQUA + waveNumber + ChatColor.GRAY + " in " +
                        ChatColor.YELLOW + timer + ChatColor.GRAY + " seconds";
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(message));

                if (timer <= 3 && timer != 0) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);

                timer--;
            }
            cycle++;
            if (timer == 0) {
                activeWave = true;
                p.playSound(p, Sound.ENTITY_WOLF_GROWL, 1, 1);
                p.sendTitle(ChatColor.GRAY + "Wave " + ChatColor.AQUA + waveNumber, "", 5, 30, 5);
            }
        }

        if (activeWave && wave != null) {
            int mobsLeft = wave.getMobsLeft();
            ChatColor color;
            if (mobsLeft > 1) color = ChatColor.YELLOW;
            else color = ChatColor.RED;
            String message = ChatColor.GRAY + "Mobs Left: " + color + mobsLeft;
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(message));

            if (mobsLeft == 0 && !wave.isBossWave() && !waveEnded) {
                endWave();
                return;
            }

            if (wave.isBossWave() && wave.getBoss() != null)
                wave.getBoss().setTarget(p);
        }
    }

    public void loadBuilding(ArrayList<RelativeBlock> building) {
        int increment = building.size() / 120;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (building.isEmpty()) {
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
        new BukkitRunnable() {
            @Override
            public void run() {
                // Generate spawning spots for the mobs at 25 locations around the centerLocation
                for (int x = -15; x <= 15; x += 5) {
                    for (int z = -15; z <= 15; z += 5) {
                        if (x == 0 && z == 0) continue;
                        // Make sure the spawning spot has 3 air blocks above it
                        Location spot = validateSpawningSpot(centerLocation.clone().add(x, 0, z));
                        spawningSpots.add(spot);
                    }
                }
                spawningSpotsGenerated = true;
            }
        }.runTaskAsynchronously(SurvivalSkills.getInstance());
    }

    public Location validateSpawningSpot(Location location) {
        // Make sure it isn't too high
        if (location.getY() >= centerLocation.getY() + 27) return validateSpawningSpot(location.clone().add(1, -27, 1));

        // Check if the location is a valid spawning spot
        if (location.getBlock().getType().isAir()
                && location.clone().add(0, 1, 0).getBlock().getType().isAir()
                && location.clone().add(0, 2, 0).getBlock().getType().isAir()) {
            return location;
        }
        return validateSpawningSpot(location.clone().add(0, 1, 0));
    }

    public void spawnWave() {
        waveEnded = false;
        waveSpawned = true;
        activeWave = true;
        wave = TrialManager.spawnWave(waveNumber, spawningSpots, p);
    }

    public void endWave() {
        if (waveEnded) return;
        if (!activeWave) return;
        waveEnded = true;
        removeWaveMobs();

        p.sendTitle("Wave " + waveNumber + " Complete", "", 5, 30, 5);
        p.playSound(p, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1, 1);

        new BukkitRunnable() {
            @Override
            public void run() {
                removeWaveMobs();
                activeWave = false;
                waveSpawned = false;
                waveNumber++;
                timer = 15;
                TrialManager.openRewardGUI(p, waveNumber);

                String message = ChatColor.GRAY + "Wave " + ChatColor.AQUA + waveNumber + ChatColor.GRAY + " in " +
                        ChatColor.YELLOW + timer + ChatColor.GRAY + " seconds";
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacy(message));
            }
        }.runTaskLater(SurvivalSkills.getInstance(), 40);
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
        p.setAllowFlight(false);
        p.setFlying(false);

        p.getInventory().addItem(TrialManager.getTrialItem(Material.WOODEN_SWORD, 1));
        p.getInventory().addItem(TrialManager.getTrialItem(Material.COOKED_BEEF, 2));
        p.teleport(centerLocation.clone().add(0.5, 1, 0.5));
        p.playSound(p, Sound.ENTITY_WOLF_GROWL, 1, 1);
        p.sendTitle(ChatColor.GRAY + "Wave " + ChatColor.AQUA + waveNumber, "", 5, 30, 5);

        buildingCreated = true;
        activeWave = true;
    }

    public void restartTrial() {
        cycle = 0;
        timer = 15;
        waveNumber = 1;
        waveSpawned = false;

        p.getInventory().clear();
        removeWaveMobs();
        wave = null;
        TrialUtils.removeGroundItemsInProtectedArea(protectedArea);

        startTrial();
    }

    public void endTrial() {
        TrialManager.getTrials().remove(this);
        TrialUtils.removeGroundItemsInProtectedArea(protectedArea);
        removeWaveMobs();
        p.getInventory().clear();
        cancel();
    }

    public void deleteTrial() {
        TrialUtils.clearTrialBuilding(centerLocation);
        TrialUtils.removeGroundItemsInProtectedArea(protectedArea);
        TrialManager.getTrials().remove(this);
        removeWaveMobs();
        TrialManager.getProtectedAreas().remove(p.getUniqueId());
        p.getInventory().clear();
        cancel();
    }

    public void completeTrial() {
        TrialManager.getTrials().remove(this);
        TrialUtils.removeGroundItemsInProtectedArea(protectedArea);
        removeWaveMobs();
        p.getInventory().clear();
        cancel();

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp user " + p.getName()
                + " permission set survivalskills.creative true");
        p.sendTitle(ChatColor.GOLD + "Trial Complete", ChatColor.GRAY + "You can now use creative mode", 5, 30, 5);
        Bukkit.broadcastMessage(ChatColor.GOLD + "Congratulations to " + ChatColor.AQUA + p.getName() + ChatColor.GOLD
                + " for becoming a " + ChatColor.AQUA + "God" + ChatColor.GOLD + "!");
        for (Player player : Bukkit.getOnlinePlayers()) player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1, 1);
    }

    public Player getPlayer() {
        return p;
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
}
