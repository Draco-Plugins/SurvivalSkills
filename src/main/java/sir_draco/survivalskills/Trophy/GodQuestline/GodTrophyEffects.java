package sir_draco.survivalskills.Trophy.GodQuestline;

import net.citizensnpcs.api.CitizensAPI;
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
import sir_draco.survivalskills.Trophy.TrophyManager;
import sir_draco.survivalskills.SurvivalSkills;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class GodTrophyEffects {

    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final int npcID;
    private final Location trophyLoc;

    private ItemDisplay display;
    private ItemDisplay blackHole;
    private EnderCrystal crystal;
    private NPC npcPlayer;
    private boolean movingUp = false;
    private double crystalRadians = 0;
    private int questParticles = 0;

    public GodTrophyEffects(Location trophyLoc) {
        this.trophyLoc = trophyLoc;
        centerX = trophyLoc.getX() + 0.5;
        centerY = trophyLoc.getY() + 1.5;
        centerZ = trophyLoc.getZ() + 0.5;
        this.npcID = -1;
    }

    public GodTrophyEffects(Location trophyLoc, int npcID) {
        this.trophyLoc = trophyLoc;
        centerX = trophyLoc.getX() + 0.5;
        centerY = trophyLoc.getY() + 1.5;
        centerZ = trophyLoc.getZ() + 0.5;
        this.npcID = npcID;
        // loadParticlePhase();
    }

    public void startAnimation() {
        if (trophyLoc.getWorld() == null) return;
        display = trophyLoc.getWorld().spawn(trophyLoc.clone().add(0.5, 2, 0.5), ItemDisplay.class);
        display.setItemStack(new ItemStack(Material.GRASS_BLOCK));
        Transformation transformation = display.getTransformation();
        transformation.getScale().set(0.2);
        display.setTransformation(transformation);
        //display.setBillboard(Billboard.CENTER) // rotates automatically
    }

    public void floatingEffect(int cycle) {
        if (cycle % 12 == 0) movingUp = !movingUp;
        if (movingUp) teleport(0, 0.05, 0);
        else teleport(0, -0.05, 0);
    }

    public void rotateDisplay(float x, float y, float z) {
        if (display == null) {
            Bukkit.getLogger().warning("No display");
            return;
        }
        Transformation transformation = display.getTransformation();
        Quaternionf quaternion = new Quaternionf();
        quaternion.rotateX(x);
        quaternion.rotateY(y);
        quaternion.rotateZ(z);

        transformation.getLeftRotation().mul(quaternion);

        display.setTransformation(transformation);
    }

    public void teleport(double xChange, double yChange, double zChange) {
        display.teleport(display.getLocation().clone().add(xChange, yChange, zChange));
    }

    public void scaleDisplay(float scale) {
        if (display == null) {
            Bukkit.getLogger().warning("No display");
            return;
        }
        Transformation transformation = display.getTransformation();
        transformation.getScale().set(scale);
        display.setTransformation(transformation);
    }

    public void playSound(Sound sound) {
        if (display == null) {
            Bukkit.getLogger().warning("No display");
            return;
        }
        World world = trophyLoc.getWorld();
        if (world == null) return;
        world.playSound(trophyLoc, sound, 1, 1);
    }

    public void removeDisplay() {
        if (display != null) display.remove();
    }

    public void remove() {
        removeDisplay();
        removeBlackHole();
        removePlayer();
        removeCrystal();
    }

    public void spawnBlackHole() {
        World world = trophyLoc.getWorld();
        if (world == null) return;
        blackHole = world.spawn(trophyLoc.clone().add(0.5, 2, 0.5), ItemDisplay.class);
        blackHole.setItemStack(new ItemStack(Material.AIR));
        blackHole.setShadowRadius(1.5f);
        blackHole.setShadowStrength(5f);
    }

    public void changeBlackHoleSize(float change) {
        blackHole.setShadowRadius(blackHole.getShadowRadius() - change);
    }

    public void removeBlackHole() {
        if (blackHole != null) blackHole.remove();
    }

    public void spawnPlayer(String name) {
        if (npcID != -1) {
            npcPlayer = CitizensAPI.getNPCRegistry().getById(npcID);
            if (npcPlayer == null) return;
            npcPlayer.spawn(trophyLoc.clone().add(0.5, 2.0, 0.5));
            return;
        }

        npcPlayer = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, TrophyManager.npcName);
        if (npcPlayer == null) return;
        npcPlayer.spawn(trophyLoc.clone().add(0.5, 2.0, 0.5));
        if (npcPlayer.getEntity() == null) return;
        npcPlayer.setProtected(true);

        Gravity gravity = npcPlayer.getOrAddTrait(Gravity.class);
        gravity.toggle();

        SkinTrait skin = npcPlayer.getOrAddTrait(SkinTrait.class);
        skin.setSkinName(name, false);

        new BukkitRunnable() {
            @Override
            public void run() {
                getText(name);
                LookClose look = npcPlayer.getOrAddTrait(LookClose.class);
                look.lookClose(true);
            }
        }.runTaskLater(SurvivalSkills.getPlugin(SurvivalSkills.class), 20);
    }

    private void getText(String name) {
        Text text = npcPlayer.getOrAddTrait(Text.class);
        text.toggleTalkClose();
        text.setRange(10.0);
        text.setDelay(20 * 15);
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
        text.add(ChatColor.GOLD + "Right click me to start the " + ChatColor.BOLD + "God Quest");

        // Get the current day of the week
        LocalDate currentDate = LocalDate.now();
        DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
        String day = dayOfWeek.name().toLowerCase();
        day = day.substring(0, 1).toUpperCase() + day.substring(1);
        text.add(ChatColor.AQUA + "Happy " + day);

        // Get the playtime of the player
        Player player = Bukkit.getPlayer(name);
        if (player != null) {
            long playtime = player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20 / 60;
            text.add(ChatColor.GREEN + name + " has played for " + ChatColor.AQUA + playtime + " minutes!");
        }
    }

    public void removePlayer() {
        if (npcPlayer == null) return;
        npcPlayer.despawn();
    }

    public void spawnCrystal(double x, double y, double z) {
        World world = trophyLoc.getWorld();
        if (world == null) return;
        crystal = (EnderCrystal) world.spawnEntity(new Location(trophyLoc.getWorld(), x, y, z), EntityType.END_CRYSTAL);
        crystal.setBeamTarget(trophyLoc.clone().add(0.5, 2, 0.5));
        crystal.setMetadata("trophy", new FixedMetadataValue(SurvivalSkills.getPlugin(SurvivalSkills.class), true));
        crystal.setShowingBottom(false);
        crystal.setInvulnerable(true);
    }

    public void removeCrystal() {
        if (crystal == null) return;
        crystal.remove();
    }

    public void moveCrystal() {
        crystal.remove();
        double radians = crystalRadians;
        spawnCrystal(centerX + (Math.cos(radians) * 2.5), centerY, centerZ + (Math.sin(radians) * 2.5));
        crystalRadians += 0.2;
    }

    public void questParticleEffect() {

    }

    public NPC getNPCPlayer() {
        return npcPlayer;
    }

    public void increaseQuestParticles() {
        questParticles++;
    }
}
