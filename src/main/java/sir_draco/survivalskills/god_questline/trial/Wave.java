package sir_draco.survivalskills.god_questline.trial;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import sir_draco.survivalskills.god_questline.trial_mobs.TrialBoss;
import sir_draco.survivalskills.god_questline.trial_mobs.WaveMob;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        for (int i = 0; i < count; i++) {
            waveMobs.add(waveMob.duplicate());
        }
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

    public List<WaveMob> getWaveMobs() {
        return List.copyOf(waveMobs);
    }

    public Optional<WaveMob> getWaveMob(Entity entity) {
        return waveMobs.stream()
                .filter(waveMob -> waveMob.getEntity() == entity)
                .findFirst();
    }

    public Wave duplicate() {
        Wave wave = new Wave();
        for (WaveMob waveMob : waveMobs) {
            wave.addWaveMob(waveMob.duplicate());
        }
        if (boss != null) {
            wave.setBoss(boss.duplicate());
        }
        return wave;
    }

    public void scaleWaveMobs(int scale) {
        ArrayList<WaveMob> mobsToAdd = new ArrayList<>();
        for (int i = 0; i < scale; i++) {
            for (WaveMob waveMob : waveMobs) {
                mobsToAdd.add(waveMob.duplicate());
            }
        }
        waveMobs.addAll(mobsToAdd);
    }

    public int getMobsLeft() {
        int mobsLeft = 0;
        for (WaveMob waveMob : waveMobs) {
            if (waveMob.getEntity() != null && !waveMob.getEntity().isDead()) {
                mobsLeft++;
            }
        }
        for (Entity entity : extraMobs) {
            if (!entity.isDead()) {
                mobsLeft++;
            }
        }
        return mobsLeft;
    }

    public void setBoss(TrialBoss boss) {
        this.boss = boss;
        this.bossWave = (boss != null);
    }

    public boolean isBossWave() {
        return bossWave;
    }

    public TrialBoss getBoss() {
        return boss;
    }

    public List<Entity> getExtraMobs() {
        return List.copyOf(extraMobs);
    }
}
