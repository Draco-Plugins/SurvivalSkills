package sir_draco.survivalskills.god_questline;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.trait.Gravity;
import net.citizensnpcs.trait.LookClose;
import net.citizensnpcs.trait.SkinTrait;
import net.citizensnpcs.trait.text.Text;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import sir_draco.survivalskills.trophy.TrophyManager;
import sir_draco.survivalskills.utils.MojangAPI;
import sir_draco.survivalskills.SurvivalSkills;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.UUID;

/**
 * Handles visual & NPC effects for a God Trophy.
 * Life-cycle (cycle ticks):
 * 1 : spawn tiny grass block display
 * 5-9: scale up (0.3 -> 0.7) with chime at 9
 * 18-59: float & rotate
 * 60: spawn black hole & wither sound
 * 61-99: rotate & descend into black hole
 * 100-104: shrink black hole
 * 105: remove black hole (teleport sound)
 * 120: lightning
 * 121: spawn player NPC + stationary crystal
 * >121: particles every 2 ticks & crystal orbit every 4 ticks
 */
public class GodTrophyEffects {

    // Spatial constants
    private static final double HALF = 0.5;
    private static final double DISPLAY_START_Y_OFFSET = 2.0;
    private static final double NPC_Y_OFFSET = 2.0;
    private static final double CRYSTAL_ORBIT_RADIUS = 2.5;
    private static final double FLOATING_STEP = 0.05;
    private static final double DESCENT_STEP = 0.05;
    private static final float ROTATION_Y = 0.05f;
    private static final int FLOATING_INTERVAL = 12;
    private static final double CRYSTAL_ANGLE_INCREMENT = 0.2;

    // Cycle boundaries
    private static final int CYCLE_FLOATING_START = 18;
    private static final int CYCLE_FLOATING_END = 60; // exclusive of 60
    private static final int CYCLE_BLACKHOLE_SPAWN = 60;
    private static final int CYCLE_DESCEND_END = 100; // exclusive of 100
    private static final int CYCLE_BLACKHOLE_SHRINK_START = 100;
    private static final int CYCLE_BLACKHOLE_SHRINK_END = 105; // exclusive 105
    private static final int CYCLE_BLACKHOLE_REMOVE = 105;
    private static final int CYCLE_LIGHTNING = 120;
    private static final int CYCLE_NPC_SPAWN = 121;
    private static final int CYCLE_EFFECTS_START = 122; // trophy idle effects begin strictly after spawn

    // Scaling
    private static final float INITIAL_SCALE = 0.2f;
    private static final float SCALE_INCREMENT = 0.1f;

    // Timings
    private static final int TALK_RANGE = 10;
    private static final int TALK_DELAY_TICKS = 20 * 15;

    // Metadata key
    private static final String META_TROPHY = "trophy";

    // Center fields were previously used for orbit; logic now derives from base
    // location + HALF.
    private final Location trophyLoc;
    private final String playerName;
    private final UUID playerUUID;

    private ItemDisplay display;
    private ItemDisplay blackHole;
    private EnderCrystal crystal;
    private NPC npcPlayer;
    private boolean movingUp = false;
    private double crystalRadians = 0;
    private int npcID = -1;

    public GodTrophyEffects(Location trophyLoc, String playerName, UUID playerUUID) {
        this.trophyLoc = trophyLoc;
        this.playerName = playerName;
        this.playerUUID = playerUUID;
    }

    public void tickTrophy(int cycle) {
        // Pre-spawn animation phase
        if (cycle < CYCLE_EFFECTS_START) {
            growDirtBlock(cycle);
            displayDirtBlock(cycle);
            if (handleTransitionEvents(cycle))
                return; // returns true if processing should stop this tick
        }
        tickIdleEffects(cycle);
    }

    private void tickIdleEffects(int cycle) {
        if (cycle <= CYCLE_NPC_SPAWN)
            return;
        if (cycle % 2 == 0) {
            var world = world();
            if (world != null)
                world.spawnParticle(Particle.ENCHANT, offset(0, 0, 0), 40);
        }
        if (cycle % 4 == 0)
            moveCrystal();
        questParticleEffect();
    }

    private boolean handleTransitionEvents(int cycle) {
        // Display removal
        if (cycle == CYCLE_BLACKHOLE_SHRINK_START)
            removeDisplay();

        // Shrink black hole
        if (cycle >= CYCLE_BLACKHOLE_SHRINK_START && cycle < CYCLE_BLACKHOLE_SHRINK_END) {
            changeBlackHoleSize(0.20f);
        } else if (cycle == CYCLE_BLACKHOLE_REMOVE) {
            playSound(Sound.ENTITY_ENDERMAN_TELEPORT);
            removeBlackHole();
        } else if (cycle == CYCLE_LIGHTNING) { // Lightning & sound
            var w = world();
            if (w == null)
                return true;
            w.strikeLightningEffect(offset(0, 0, 0));
            playSound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER);
        } else if (cycle == CYCLE_NPC_SPAWN) {
            spawnPlayer(playerName, playerUUID);
            // First crystal spawn is stationary above block center
            spawnCrystal(HALF, 1.5, HALF);
        }
        return false;
    }

    private void displayDirtBlock(int cycle) {
        if (cycle >= CYCLE_FLOATING_START && cycle < CYCLE_FLOATING_END) {
            rotateDisplay(0, ROTATION_Y, 0);
            floatingEffect(cycle);
        } else if (cycle == CYCLE_BLACKHOLE_SPAWN) {
            spawnBlackHole();
            playSound(Sound.ENTITY_WITHER_SPAWN);
        } else if (cycle > CYCLE_BLACKHOLE_SPAWN && cycle < CYCLE_DESCEND_END) {
            rotateDisplay(0, ROTATION_Y, 0);
            teleport(0, -DESCENT_STEP, 0);
        }
    }

    private void growDirtBlock(int cycle) {
        if (cycle == 1) {
            startAnimation();
            return;
        }
        if (cycle >= 5 && cycle <= 9) {
            // cycles 5..9 produce scales 0.3..0.7
            float scale = INITIAL_SCALE + ((cycle - 4) * SCALE_INCREMENT); // (5-4)*0.1 => 0.3 ... (9-4)*0.1 => 0.7
            scaleDisplay(scale);
            if (cycle == 9)
                playSound(Sound.BLOCK_NOTE_BLOCK_CHIME);
        }
    }

    public void startAnimation() {
        var w = world();
        if (w == null)
            return;
        display = w.spawn(offset(0, DISPLAY_START_Y_OFFSET, 0), ItemDisplay.class);
        display.setItemStack(new ItemStack(Material.GRASS_BLOCK));
        Transformation transformation = display.getTransformation();
        transformation.getScale().set(INITIAL_SCALE);
        display.setTransformation(transformation);
    }

    public void floatingEffect(int cycle) {
        if (cycle % FLOATING_INTERVAL == 0)
            movingUp = !movingUp;
        teleport(0, movingUp ? FLOATING_STEP : -FLOATING_STEP, 0);
    }

    public void rotateDisplay(float x, float y, float z) {
        if (display == null)
            return;
        Transformation transformation = display.getTransformation();
        Quaternionf quaternion = new Quaternionf();
        quaternion.rotateX(x);
        quaternion.rotateY(y);
        quaternion.rotateZ(z);

        transformation.getLeftRotation().mul(quaternion);

        display.setTransformation(transformation);
    }

    public void teleport(double xChange, double yChange, double zChange) {
        if (display == null)
            return;
        display.teleport(display.getLocation().clone().add(xChange, yChange, zChange));
    }

    public void scaleDisplay(float scale) {
        if (display == null)
            return;
        Transformation transformation = display.getTransformation();
        transformation.getScale().set(scale);
        display.setTransformation(transformation);
    }

    public void playSound(Sound sound) {
        var w = world();
        if (w == null)
            return;
        w.playSound(trophyLoc, sound, 1, 1);
    }

    public void removeDisplay() {
        if (display != null) {
            display.remove();
            display = null;
        }
    }

    public void remove() {
        removeDisplay();
        removeBlackHole();
        removePlayer();
        removeCrystal();
    }

    public void spawnBlackHole() {
        var w = world();
        if (w == null)
            return;
        blackHole = w.spawn(offset(0, DISPLAY_START_Y_OFFSET, 0), ItemDisplay.class);
        blackHole.setItemStack(new ItemStack(Material.AIR));
        blackHole.setShadowRadius(1.5f);
        blackHole.setShadowStrength(5f);
    }

    public void changeBlackHoleSize(float change) {
        if (blackHole == null)
            return;
        blackHole.setShadowRadius(Math.max(0, blackHole.getShadowRadius() - change));
    }

    public void removeBlackHole() {
        if (blackHole != null) {
            blackHole.remove();
            blackHole = null;
        }
    }

    public void spawnPlayer(String name, UUID playerUUID) {
        if (npcID != -1) {
            npcPlayer = TrophyManager.getRegistry().getById(npcID);
            if (npcPlayer == null)
                return;
            // Update the text
            getText(name, playerUUID);
            if (npcPlayer.isSpawned())
                return;
            npcPlayer.spawn(offset(0, NPC_Y_OFFSET, 0));
            return;
        }

        UUID uuid = UUID.randomUUID();
        npcPlayer = TrophyManager.getRegistry().createNPC(EntityType.PLAYER, uuid, TrophyManager.getNextID(),
                TrophyManager.npcName);
        if (npcPlayer == null)
            return;
        npcPlayer.spawn(offset(0, NPC_Y_OFFSET, 0));
        if (npcPlayer.getEntity() == null)
            return;
        npcPlayer.setProtected(true);
        npcID = npcPlayer.getId();
        SurvivalSkills.getInstance().getTrophyManager().getGodNPCIDs().put(playerUUID, npcID);

        Gravity gravity = npcPlayer.getOrAddTrait(Gravity.class);
        gravity.toggle();

        Player p = Bukkit.getPlayer(name);
        SkinTrait skin = npcPlayer.getOrAddTrait(SkinTrait.class);
        if (p != null)
            skin.setSkinPersistent(p);
        else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    String[] info;
                    try {
                        info = MojangAPI.getSkinData(playerUUID);
                    } catch (IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    skin.setSkinPersistent(playerUUID.toString(), info[0], info[1]);
                }
            }.runTask(SurvivalSkills.getPlugin(SurvivalSkills.class));
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                getText(name, playerUUID);
                LookClose look = npcPlayer.getOrAddTrait(LookClose.class);
                look.lookClose(true);
            }
        }.runTaskLater(SurvivalSkills.getPlugin(SurvivalSkills.class), 20);
    }

    private void getText(String name, UUID uuid) {
        Text text = npcPlayer.getOrAddTrait(Text.class);
        text.toggleTalkClose();
        text.setRange(TALK_RANGE);
        text.setDelay(TALK_DELAY_TICKS);
        text.getTexts().clear();
        text.add(ChatColor.AQUA + "What a nice day");
        text.add(ChatColor.AQUA + name + " sure is impressive");
        text.add(ChatColor.AQUA + "Who put me up here?");
        text.add(ChatColor.RED + "I will feed upon your flesh " + ChatColor.MAGIC + "and soul");
        text.add(ChatColor.AQUA + "I can't believe " + name + " had to shear all those sheep");
        text.add(ChatColor.AQUA + "I'm so high up here");
        text.add(ChatColor.AQUA + "There is a bald spot on your head you know");
        text.add(ChatColor.AQUA + "You should definitely try to break these crystals");
        text.add(ChatColor.AQUA + "Let's be honest, " + name + " is the best player on the server");
        text.add(ChatColor.GOLD + "I hear there is a secret hidden in this world");

        if (!SurvivalSkills.getInstance().getTrophyManager().getPlayerGodQuestData().containsKey(uuid))
            text.add(ChatColor.GOLD + "Right click me to start the " + ChatColor.BOLD + "God Quest");

        // Day of week
        String day = LocalDate.now().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        text.add(ChatColor.AQUA + "Happy " + day);

        // Get the playtime of the player
        Player player = Bukkit.getPlayer(name);
        if (player != null) {
            long playtime = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20 / 60;
            text.add(ChatColor.GREEN + name + " has played for " + ChatColor.AQUA + playtime + " minutes!");
        }
    }

    public void removePlayer() {
        if (npcPlayer == null)
            return;
        npcPlayer.despawn();
        npcPlayer = null;
    }

    public void destroyPlayer() {
        if (npcPlayer == null)
            return;
        npcPlayer.despawn();
        npcPlayer.destroy();
        TrophyManager.getRegistry().deregister(npcPlayer);
        npcPlayer = null;
    }

    public void spawnCrystal(double x, double y, double z) {
        // Interpret x,y,z as offsets from base trophy block for clarity / consistency
        var w = world();
        if (w == null)
            return;
        removeCrystal();
        Location spawnLoc = new Location(w, trophyLoc.getX() + x, trophyLoc.getY() + y, trophyLoc.getZ() + z);
        crystal = (EnderCrystal) w.spawnEntity(spawnLoc, EntityType.END_CRYSTAL);
        crystal.setBeamTarget(offset(0, NPC_Y_OFFSET, 0));
        crystal.setMetadata(META_TROPHY, new FixedMetadataValue(SurvivalSkills.getPlugin(SurvivalSkills.class), true));
        crystal.setShowingBottom(false);
        crystal.setInvulnerable(true);
    }

    public void removeCrystal() {
        if (crystal != null) {
            crystal.remove();
            crystal = null;
        }
    }

    public void moveCrystal() {
        if (crystal != null)
            crystal.remove();
        double radians = crystalRadians;
        // Convert from absolute intended center positions back to offsets for
        // spawnCrystal
        double offsetX = (Math.cos(radians) * CRYSTAL_ORBIT_RADIUS) + HALF;
        double offsetZ = (Math.sin(radians) * CRYSTAL_ORBIT_RADIUS) + HALF;
        double offsetY = 1.5; // keep constant height (same as initial crystal spawn y-offset)
        spawnCrystal(offsetX, offsetY, offsetZ);
        crystalRadians += CRYSTAL_ANGLE_INCREMENT;
    }

    public void questParticleEffect() {

    }

    // Helper: world reference (may be null if chunk / world unloaded)
    private World world() {
        return trophyLoc.getWorld();
    }

    // Helper: location offset from base block center
    private Location offset(double x, double y, double z) {
        return trophyLoc.clone().add(HALF + x, y, HALF + z);
    }
}
