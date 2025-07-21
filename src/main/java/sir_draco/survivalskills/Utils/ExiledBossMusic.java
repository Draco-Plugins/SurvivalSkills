package sir_draco.survivalskills.Utils;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;

public class ExiledBossMusic extends BukkitRunnable {

    private final Player p;
    private final HashMap<String, Float> noteMap = new HashMap<>();
    private final HashMap<Integer, MusicPhrase> phrases = new HashMap<>();

    private int measure = 1;
    private int tick = 0;
    private int phrase = 0;
    private boolean dead = false;

    public ExiledBossMusic(Player p) {
        this.p = p;
        createNoteMap();
        entrance1();
        entrance2();
        entrance3();
        entrance4();
        entrance5();
        entrance6();
        bridge();
        mainPhrase();
        mainPhrase2();
        mainPhrase3();
        mainPhrase4();
        end();
    }

    @Override
    public void run() {
        if (dead || !p.isOnline()) {
            this.cancel();
            return;
        }

        if (phrase < 6) {
            if (phrase == 0) {
                phrases.get(phrase).playBass(tick, p);
                phrases.get(phrase).playBit(tick, p);
                if (tick == 63) {
                    phrase++;
                    tick = -1;
                }
            }
            else if (phrase == 1) {
                phrases.get(phrase).playBass(tick, p);
                phrases.get(phrase).playBit(tick, p);
                phrases.get(phrase).playGuitar(tick, p);
                if (tick == 63) {
                    phrase++;
                    tick = -1;
                }
            }
            else if (phrase == 2) {
                phrases.get(phrase).playBass(tick, p);
                phrases.get(phrase).playBit(tick, p);
                phrases.get(phrase).playGuitar(tick, p);
                phrases.get(phrase).playDrums(tick, p);
                if (tick == 63) {
                    phrase++;
                    tick = -1;
                }
            }
            else if (phrase == 3) {
                phrases.get(phrase).playBass(tick, p);
                phrases.get(phrase).playBit(tick, p);
                phrases.get(phrase).playGuitar(tick, p);
                phrases.get(phrase).playDrums(tick, p);
                if (tick == 63) {
                    phrase++;
                    tick = -1;
                }
            }
            else if (phrase == 4) {
                phrases.get(phrase).playBass(tick, p);
                phrases.get(phrase).playDrums(tick, p);
                phrases.get(phrase).playFlute(tick, p);
                if (tick == 63) {
                    phrase++;
                    tick = -1;
                }
            }
            else if (phrase == 5) {
                phrases.get(phrase).playBass(tick, p);
                phrases.get(phrase).playDrums(tick, p);
                phrases.get(phrase).playFlute(tick, p);
                if (tick == 63) {
                    phrase++;
                    tick = -1;
                }
            }
        }
        else if (phrase == 6) {
            phrases.get(phrase).playNotes(tick, p);
            if (tick == 0) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BELL, 1, noteMap.get("C3"));
            else if (tick == 4) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, noteMap.get("C3"));
            else if (tick == 8) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BELL, 0.4f, noteMap.get("C3"));
            else if (tick == 12) p.playSound(p, Sound.BLOCK_NOTE_BLOCK_BELL, 0.1f, noteMap.get("C3"));

            if (tick == 31) {
                phrase++;
                tick = -1;
            }
        }
        else {
            if (phrase == 7) {
                phrases.get(phrase).playNotes(tick, p);
                if (tick == 63) {
                    phrase++;
                    tick = -1;
                }
            }
            else if (phrase == 8) {
                phrases.get(phrase).playNotes(tick, p);
                if (tick == 63) {
                    phrase++;
                    tick = -1;
                }
            }
            else if (phrase == 9) {
                phrases.get(phrase).playNotes(tick, p);
                if (tick == 63) {
                    phrase++;
                    tick = -1;
                }
            }
            else if (phrase == 10) {
                phrases.get(phrase).playNotes(tick, p);
                if (tick == 63) {
                    phrase++;
                    tick = -1;
                }
            }
            else {
                phrases.get(11).playNotes(tick, p);
                if (tick == 63) {
                    phrase++;
                    tick = -1;
                    if (phrase == 13) phrase = 2;
                }
            }
        }

        tick++;
        measure++;
        if (measure == 5) measure = 1;
    }

    public void createNoteMap() {
        noteMap.put("F#1", 0.5f);
        noteMap.put("G1", 0.53f);
        noteMap.put("G#1", 0.56f);
        noteMap.put("A1", 0.6f);
        noteMap.put("A#1", 0.63f);
        noteMap.put("B1", 0.67f);
        noteMap.put("C2", 0.7f);
        noteMap.put("C#2", 0.74f);
        noteMap.put("D2", 0.78f);
        noteMap.put("D#2", 0.83f);
        noteMap.put("E2", 0.88f);
        noteMap.put("F2", 0.93f);
        noteMap.put("F#2", 0.98f);
        noteMap.put("G2", 1.05f);
        noteMap.put("G#2", 1.1f);
        noteMap.put("A2", 1.17f);
        noteMap.put("A#2", 1.24f);
        noteMap.put("B2", 1.32f);
        noteMap.put("C3", 1.4f);
        noteMap.put("C#3", 1.48f);
        noteMap.put("D3", 1.57f);
        noteMap.put("D#3", 1.67f);
        noteMap.put("E3", 1.80f);
    }

    // Sections of the song
    public void entrance1() {
        MusicPhrase phrase = new MusicPhrase();
        bass(phrase);
        bitBeat(phrase);
        phrases.put(0, phrase);
    }

    public void entrance2() {
        MusicPhrase phrase = new MusicPhrase();
        bass(phrase);
        bitBeat(phrase);
        steadyGuitar(phrase);
        phrases.put(1, phrase);
    }

    public void entrance3() {
        MusicPhrase phrase = new MusicPhrase();
        bass(phrase);
        bitBeat(phrase);
        steadyGuitar(phrase);
        drums(phrase);
        phrases.put(2, phrase);
    }

    public void entrance4() {
        MusicPhrase phrase = new MusicPhrase();
        bass(phrase);
        bitBeat2(phrase);
        steadyGuitar(phrase);
        drums(phrase);
        phrases.put(3, phrase);
    }

    public void entrance5() {
        MusicPhrase phrase = new MusicPhrase();
        bass(phrase);
        drums(phrase);
        flute(phrase);
        phrases.put(4, phrase);
    }

    public void entrance6() {
        MusicPhrase phrase = new MusicPhrase();
        bass(phrase);
        drums(phrase);
        flute2(phrase);
        phrases.put(5, phrase);
    }

    public void bridge() {
        MusicPhrase phrase = new MusicPhrase();
        phrase.addNote(2, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_BASEDRUM);
        phrase.addNote(6, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_BASEDRUM);
        phrase.addNote(10, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_BASEDRUM);
        phrase.addNote(14, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_BASEDRUM);
        phrase.addNote(18, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_BASEDRUM);
        phrase.addNote(22, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_BASEDRUM);
        phrase.addNote(26, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_BASEDRUM);
        phrase.addNote(30, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_BASEDRUM);
        phrase.addNote(16, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_SNARE);
        phrase.addNote(20, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_SNARE);
        phrase.addNote(24, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_SNARE);
        phrase.addNote(26, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_SNARE);
        phrase.addNote(28, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_SNARE);
        phrase.addNote(30, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_SNARE);
        phrase.addNote(18, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_HAT);
        phrase.addNote(22, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_HAT);
        phrases.put(6, phrase);
    }

    public void mainPhrase() {
        MusicPhrase phrase = new MusicPhrase();
        bass(phrase);
        bitBeat3(phrase);
        steadyGuitar(phrase);
        drums(phrase);
        banjo(phrase);
        xylophone(phrase);
        phrases.put(7, phrase);
    }

    public void mainPhrase2() {
        MusicPhrase phrase = new MusicPhrase();
        bass(phrase);
        bitBeat3(phrase);
        steadyGuitar(phrase);
        drums(phrase);
        banjo2(phrase);
        xylophone(phrase);
        phrases.put(8, phrase);
    }

    public void mainPhrase3() {
        MusicPhrase phrase = new MusicPhrase();
        bass(phrase);
        bitBeat3(phrase);
        steadyGuitar(phrase);
        drums(phrase);
        banjo(phrase);
        xylophone(phrase);
        flute3(phrase);
        phrases.put(9, phrase);
    }

    public void mainPhrase4() {
        MusicPhrase phrase = new MusicPhrase();
        bass(phrase);
        bitBeat3(phrase);
        steadyGuitar(phrase);
        drums(phrase);
        banjo2(phrase);
        xylophone(phrase);
        flute3(phrase);
        phrases.put(10, phrase);
    }

    public void end() {
        MusicPhrase phrase = new MusicPhrase();
        bass(phrase);
        bitBeat3(phrase);
        steadyGuitar(phrase);
        drums(phrase);
        xylophone(phrase);
        flute3(phrase);
        phrases.put(11, phrase);
    }


    // Pieces of the songs
    public void bass(MusicPhrase phrase) {
        phrase.addNote(2, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BASS);
        phrase.addNote(6, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BASS);
        phrase.addNote(10, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BASS);
        phrase.addNote(18, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BASS);
        phrase.addNote(22, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BASS);
        phrase.addNote(26, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BASS);
        phrase.addNote(30, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BASS);
        phrase.addNote(34, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_BASS);
        phrase.addNote(38, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_BASS);
        phrase.addNote(42, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_BASS);
        phrase.addNote(50, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_BASS);
        phrase.addNote(54, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_BASS);
        phrase.addNote(58, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_BASS);
        phrase.addNote(62, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_BASS);
    }

    public void bitBeat(MusicPhrase phrase) {
        phrase.addNote(0, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(14, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(15, noteMap.get("F#2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(16, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(32, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(46, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(47, noteMap.get("F#2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(48, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_BIT);
    }

    public void bitBeat2(MusicPhrase phrase) {
        phrase.addNote(0, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(14, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(15, noteMap.get("F#2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(16, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(18, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(20, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(31, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(32, noteMap.get("D3"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(44, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(48, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(50, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(52, noteMap.get("F#2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(53, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(54, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(56, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(58, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(60, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(62, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_BIT);
    }

    public void bitBeat3(MusicPhrase phrase) {
        phrase.addNote(0, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(2, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(4, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(6, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(8, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(10, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(12, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(14, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(16, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(18, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(20, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(22, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(24, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(26, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(28, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(30, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(32, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(34, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(36, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(38, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(40, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(42, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(44, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(46, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(48, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(50, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(52, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(54, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(56, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(58, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(60, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_BIT);
        phrase.addNote(62, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_BIT);
    }

    public void steadyGuitar(MusicPhrase phrase) {
        phrase.addNote(2, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(3, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(6, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(7, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(10, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(11, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(14, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(15, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(18, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(19, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(22, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(23, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(26, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(27, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(30, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(31, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(34, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(35, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(38, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(39, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(42, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(43, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(46, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(47, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(50, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(51, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(54, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(55, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(58, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(59, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(62, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
        phrase.addNote(63, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_GUITAR);
    }

    public void drums(MusicPhrase phrase) {
        phrase.addNote(0, noteMap.get("F2"), Sound.BLOCK_NOTE_BLOCK_BASEDRUM);
        phrase.addNote(8, noteMap.get("F2"), Sound.BLOCK_NOTE_BLOCK_BASEDRUM);
        phrase.addNote(16, noteMap.get("F2"), Sound.BLOCK_NOTE_BLOCK_BASEDRUM);
        phrase.addNote(24, noteMap.get("F2"), Sound.BLOCK_NOTE_BLOCK_BASEDRUM);
        phrase.addNote(32, noteMap.get("F2"), Sound.BLOCK_NOTE_BLOCK_BASEDRUM);
        phrase.addNote(40, noteMap.get("F2"), Sound.BLOCK_NOTE_BLOCK_BASEDRUM);
        phrase.addNote(48, noteMap.get("F2"), Sound.BLOCK_NOTE_BLOCK_BASEDRUM);
        phrase.addNote(56, noteMap.get("F2"), Sound.BLOCK_NOTE_BLOCK_BASEDRUM);

        phrase.addNote(4, noteMap.get("F2"), Sound.BLOCK_NOTE_BLOCK_SNARE);
        phrase.addNote(12, noteMap.get("F2"), Sound.BLOCK_NOTE_BLOCK_SNARE);
        phrase.addNote(20, noteMap.get("F2"), Sound.BLOCK_NOTE_BLOCK_SNARE);
        phrase.addNote(28, noteMap.get("F2"), Sound.BLOCK_NOTE_BLOCK_SNARE);
        phrase.addNote(36, noteMap.get("F2"), Sound.BLOCK_NOTE_BLOCK_SNARE);
        phrase.addNote(44, noteMap.get("F2"), Sound.BLOCK_NOTE_BLOCK_SNARE);
        phrase.addNote(52, noteMap.get("F2"), Sound.BLOCK_NOTE_BLOCK_SNARE);
        phrase.addNote(60, noteMap.get("F2"), Sound.BLOCK_NOTE_BLOCK_SNARE);
    }

    public void flute(MusicPhrase phrase) {
        phrase.addNote(0, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(14, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(15, noteMap.get("F#2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(16, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(32, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(46, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(47, noteMap.get("F#2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(48, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
    }

    public void flute2(MusicPhrase phrase) {
        phrase.addNote(0, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(14, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(15, noteMap.get("F#2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(16, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(18, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(20, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(31, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(32, noteMap.get("D3"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(44, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(48, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(50, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(52, noteMap.get("F#2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(53, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(54, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(56, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(58, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(60, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(62, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
    }

    public void flute3(MusicPhrase phrase) {
        phrase.addNote(2, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(6, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(10, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(14, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(18, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(22, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(26, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(30, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(34, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(38, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(42, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(46, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(50, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(54, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(58, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
        phrase.addNote(62, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_FLUTE);
    }

    public void banjo(MusicPhrase phrase) {
        phrase.addNote(0, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(4, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(6, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(8, noteMap.get("E3"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(10, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(12, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(15, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(16, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(18, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(20, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(24, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(26, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(28, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(30, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(32, noteMap.get("D3"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(44, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(48, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(49, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(50, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(52, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(54, noteMap.get("D3"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(56, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(60, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_BANJO);
    }

    public void banjo2(MusicPhrase phrase) {
        phrase.addNote(0, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(2, noteMap.get("E3"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(4, noteMap.get("D3"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(8, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(12, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(18, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(20, noteMap.get("F#2"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(24, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(28, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(30, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(32, noteMap.get("E3"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(42, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(44, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(48, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(56, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_BANJO);
        phrase.addNote(60, noteMap.get("C3"), Sound.BLOCK_NOTE_BLOCK_BANJO);
    }

    public void xylophone(MusicPhrase phrase) {
        phrase.addNote(2, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(3, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(6, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(7, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(10, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(11, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(14, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(15, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(18, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(19, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(22, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(23, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(26, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(27, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(30, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(31, noteMap.get("E2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(34, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(35, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(38, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(39, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(42, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(43, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(46, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(47, noteMap.get("G2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(50, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(51, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(54, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(55, noteMap.get("A2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(58, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(59, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(62, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
        phrase.addNote(63, noteMap.get("B2"), Sound.BLOCK_NOTE_BLOCK_XYLOPHONE);
    }

    public void setDead(boolean dead) {
        this.dead = dead;
    }
}
