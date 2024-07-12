package sir_draco.survivalskills.Bosses;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;

public class MusicPhrase {

    private final HashMap<Integer, Float> harp = new HashMap<>();
    private final HashMap<Integer, Float> doubleBass = new HashMap<>();
    private final HashMap<Integer, Float> baseDrum = new HashMap<>();
    private final HashMap<Integer, Float> snareDrum = new HashMap<>();
    private final HashMap<Integer, Float> click = new HashMap<>();
    private final HashMap<Integer, Float> guitar = new HashMap<>();
    private final HashMap<Integer, Float> flute = new HashMap<>();
    private final HashMap<Integer, Float> bell = new HashMap<>();
    private final HashMap<Integer, Float> chime = new HashMap<>();
    private final HashMap<Integer, Float> xylophone = new HashMap<>();
    private final HashMap<Integer, Float> ironXylophone = new HashMap<>();
    private final HashMap<Integer, Float> cowBell = new HashMap<>();
    private final HashMap<Integer, Float> didgeridoo = new HashMap<>();
    private final HashMap<Integer, Float> bit = new HashMap<>();
    private final HashMap<Integer, Float> banjo = new HashMap<>();
    private final HashMap<Integer, Float> pling = new HashMap<>();

    public MusicPhrase() {}

    public void addNote(int tick, float pitch, Sound sound) {
        if (sound.equals(Sound.BLOCK_NOTE_BLOCK_HARP)) harp.put(tick, pitch);
        else if (sound.equals(Sound.BLOCK_NOTE_BLOCK_BASS)) doubleBass.put(tick, pitch);
        else if (sound.equals(Sound.BLOCK_NOTE_BLOCK_BASEDRUM)) baseDrum.put(tick, pitch);
        else if (sound.equals(Sound.BLOCK_NOTE_BLOCK_SNARE)) snareDrum.put(tick, pitch);
        else if (sound.equals(Sound.BLOCK_NOTE_BLOCK_HAT)) click.put(tick, pitch);
        else if (sound.equals(Sound.BLOCK_NOTE_BLOCK_GUITAR)) guitar.put(tick, pitch);
        else if (sound.equals(Sound.BLOCK_NOTE_BLOCK_FLUTE)) flute.put(tick, pitch);
        else if (sound.equals(Sound.BLOCK_NOTE_BLOCK_BELL)) bell.put(tick, pitch);
        else if (sound.equals(Sound.BLOCK_NOTE_BLOCK_CHIME)) chime.put(tick, pitch);
        else if (sound.equals(Sound.BLOCK_NOTE_BLOCK_XYLOPHONE)) xylophone.put(tick, pitch);
        else if (sound.equals(Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE)) ironXylophone.put(tick, pitch);
        else if (sound.equals(Sound.BLOCK_NOTE_BLOCK_COW_BELL)) cowBell.put(tick, pitch);
        else if (sound.equals(Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO)) didgeridoo.put(tick, pitch);
        else if (sound.equals(Sound.BLOCK_NOTE_BLOCK_BIT)) bit.put(tick, pitch);
        else if (sound.equals(Sound.BLOCK_NOTE_BLOCK_BANJO)) banjo.put(tick, pitch);
        else if (sound.equals(Sound.BLOCK_NOTE_BLOCK_PLING)) pling.put(tick, pitch);
    }

    public void playNotes(int tick, Player p) {
        if (harp.containsKey(tick)) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_HARP, 1, harp.get(tick));
        if (doubleBass.containsKey(tick)) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BASS, 1, doubleBass.get(tick));
        if (baseDrum.containsKey(tick)) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1, baseDrum.get(tick));
        if (snareDrum.containsKey(tick)) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_SNARE, 1, snareDrum.get(tick));
        if (click.containsKey(tick)) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_HAT, 1, click.get(tick));
        if (guitar.containsKey(tick)) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_GUITAR, 1, guitar.get(tick));
        if (flute.containsKey(tick)) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1, flute.get(tick));
        if (bell.containsKey(tick)) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BELL, 1, bell.get(tick));
        if (chime.containsKey(tick)) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_CHIME, 1, chime.get(tick));
        if (xylophone.containsKey(tick)) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, 1, xylophone.get(tick));
        if (ironXylophone.containsKey(tick)) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_IRON_XYLOPHONE, 1, ironXylophone.get(tick));
        if (cowBell.containsKey(tick)) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_COW_BELL, 1, cowBell.get(tick));
        if (didgeridoo.containsKey(tick)) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 1, didgeridoo.get(tick));
        if (bit.containsKey(tick)) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1, bit.get(tick));
        if (banjo.containsKey(tick)) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BANJO, 1, banjo.get(tick));
        if (pling.containsKey(tick)) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1, pling.get(tick));
    }

    public void playBass(int tick, Player p) {
        if (doubleBass.containsKey(tick)) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BASS, 1, doubleBass.get(tick));
    }

    public void playDrums(int tick, Player p) {
        if (baseDrum.containsKey(tick)) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1, baseDrum.get(tick));
        if (snareDrum.containsKey(tick)) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_SNARE, 1, snareDrum.get(tick));
    }

    public void playGuitar(int tick, Player p) {
        if (guitar.containsKey(tick)) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_GUITAR, 1, guitar.get(tick));
    }

    public void playFlute(int tick, Player p) {
        if (flute.containsKey(tick)) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_FLUTE, 1, flute.get(tick));
    }

    public void playBit(int tick, Player p) {
        if (bit.containsKey(tick)) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BIT, 1, bit.get(tick));
    }
}
