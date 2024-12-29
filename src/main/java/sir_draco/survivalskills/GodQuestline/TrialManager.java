package sir_draco.survivalskills.GodQuestline;

import org.bukkit.event.Listener;

import java.util.ArrayList;

public class TrialManager implements Listener {

    private static final ArrayList<Trial> trials = new ArrayList<>();

    public static ArrayList<Trial> getTrials() {
        return trials;
    }
}
