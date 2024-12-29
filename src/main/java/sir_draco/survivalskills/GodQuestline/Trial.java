package sir_draco.survivalskills.GodQuestline;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.Utils.TrialUtils;

import java.util.ArrayList;

public class Trial extends BukkitRunnable {

    private final Player p;
    private final BoundingBox boundingBox;
    private final Location centerLocation;

    private boolean buildingCreated = false;

    public Trial(ArrayList<RelativeBlock> building, Player p, BoundingBox boundingBox, Location centerLocation) {
        this.p = p;
        this.boundingBox = boundingBox;
        this.centerLocation = centerLocation;
        loadBuilding(building);
    }

    @Override
    public void run() {
        if (!buildingCreated) return;
    }

    public void loadBuilding(ArrayList<RelativeBlock> building) {
        int increment = building.size() / 60;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (building.isEmpty()) {
                    buildingCreated = true;
                    p.teleport(centerLocation.clone().add(0.5, 1, 0.5));
                    cancel();
                    return;
                }

                int amount = Math.max(increment, building.size());
                for (int i = 0; i < amount; i++) {
                    if (building.isEmpty()) continue;
                    RelativeBlock block = TrialUtils.getRandomBlock(building);
                    TrialUtils.convertBlockToRelative(block, centerLocation);
                }
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);
    }

    public void endTrial() {
        TrialUtils.clearTrialBuilding(centerLocation);
        TrialManager.getTrials().remove(this);
        cancel();
    }

    public Player getPlayer() {
        return p;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }
}
