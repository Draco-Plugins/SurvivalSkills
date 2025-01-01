package sir_draco.survivalskills.GodQuestline;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.Utils.TrialUtils;

import java.util.ArrayList;

public class Trial extends BukkitRunnable {

    private final Player p;
    private final ProtectedArea protectedArea;
    private final Location centerLocation;

    private boolean buildingCreated = false;
    private int cycle;
    private int timer = 30;
    private int wave = 1;

    public Trial(ArrayList<RelativeBlock> building, Player p, ProtectedArea protectedArea, Location centerLocation) {
        this.p = p;
        this.protectedArea = protectedArea;
        this.centerLocation = centerLocation;
        loadBuilding(building);
    }

    public Trial(Player p, ProtectedArea protectedArea, Location centerLocation) {
        this.p = p;
        this.protectedArea = protectedArea;
        this.centerLocation = centerLocation;
        startTrial();
    }

    @Override
    public void run() {
        if (!buildingCreated) return;
        if (cycle % 20 == 0) timer--;
        cycle++;
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

    public void startTrial() {
        buildingCreated = true;
        p.teleport(centerLocation.clone().add(0.5, 1, 0.5));
    }

    public void restartTrial() {
        p.teleport(centerLocation.clone().add(0.5, 1, 0.5));
        timer = 0;
    }

    public void endTrial() {
        TrialManager.getTrials().remove(this);
        cancel();
    }

    public void deleteTrial() {
        TrialUtils.clearTrialBuilding(centerLocation);
        TrialManager.getTrials().remove(this);
        TrialManager.getProtectedAreas().remove(p.getUniqueId());
        cancel();
    }

    public Player getPlayer() {
        return p;
    }

    public ProtectedArea getProtectedArea() {
        return protectedArea;
    }
}
