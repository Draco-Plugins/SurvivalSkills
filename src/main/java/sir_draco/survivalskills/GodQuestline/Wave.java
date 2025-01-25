package sir_draco.survivalskills.GodQuestline;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.GodQuestline.TrialMobs.TrialBoss;
import sir_draco.survivalskills.GodQuestline.TrialMobs.WaveMob;

import java.util.ArrayList;

public class Wave {

    private final ArrayList<WaveMob> waveMobs = new ArrayList<>();
    private final ArrayList<Entity> extraMobs = new ArrayList<>();

    private TrialBoss boss = null;
    private boolean bossWave = false;

    public Wave() {}

    public void addWaveMob(WaveMob waveMob) {
        waveMobs.add(waveMob);
    }

    public void addWaveMob(WaveMob waveMob, int count) {
        for (int i = 0; i < count; i++) waveMobs.add(waveMob.duplicate());
    }

    public void addExtraMob(Entity entity) {
        extraMobs.add(entity);
    }

    public void removeExtraMob(Entity entity) {
        extraMobs.remove(entity);
    }

    public void removeWaveMob(WaveMob waveMob) {
        waveMobs.remove(waveMob);
    }

    public void spawnMob(Location location, WaveMob waveMob, ArrayList<Player> players) {
        waveMob.spawnMob(location, players);
    }

    public ArrayList<WaveMob> getWaveMobs() {
        return waveMobs;
    }

    public WaveMob getWaveMob(Entity entity) {
        for (WaveMob waveMob : waveMobs) if (waveMob.getEntity() == entity) return waveMob;
        return null;
    }

    public Wave duplicate() {
        Wave wave = new Wave();
        if (bossWave) {
            wave.setBossWave(true);
            wave.setBoss(boss.duplicate());
            return wave;
        }

        for (WaveMob waveMob : waveMobs) wave.addWaveMob(waveMob.duplicate());
        return wave;
    }

    public void scaleMobs(int scale) {
        ArrayList<WaveMob> mobsToAdd = new ArrayList<>();
        for (int i = 0; i < scale; i++)
            for (WaveMob waveMob : waveMobs) mobsToAdd.add(waveMob.duplicate());
        waveMobs.addAll(mobsToAdd);
    }

    public int getMobsLeft() {
        int mobsLeft = 0;
        for (WaveMob waveMob : waveMobs)
            if (waveMob.getEntity() != null && !waveMob.getEntity().isDead()) mobsLeft++;
        for (Entity entity : extraMobs) if (!entity.isDead()) mobsLeft++;
        return mobsLeft;
    }

    public void setBossWave(boolean bossWave) {
        this.bossWave = bossWave;
    }

    public boolean isBossWave() {
        return bossWave;
    }

    public void setBoss(TrialBoss boss) {
        this.boss = boss;
    }

    public TrialBoss getBoss() {
        return boss;
    }

    public ArrayList<Entity> getExtraMobs() {
        return extraMobs;
    }
}
