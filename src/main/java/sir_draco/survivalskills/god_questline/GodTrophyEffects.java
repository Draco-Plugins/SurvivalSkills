package sir_draco.survivalskills.god_questline;

import net.citizensnpcs.api.npc.MetadataStore;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import sir_draco.survivalskills.trophy.TrophyManager;
import sir_draco.survivalskills.utils.MojangAPI;
import sir_draco.survivalskills.SurvivalSkills;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.StreamSupport;

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
    // Cleanup radius must exceed orbit radius to catch orphaned crystals from
    // prior reloads / missed removals
    private static final double CRYSTAL_CLEANUP_RADIUS = CRYSTAL_ORBIT_RADIUS + 1.0;
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
    private static final int AURA_PARTICLE_INTERVAL = 4;
    private static final int AURA_PARTICLE_COUNT = 24;
    private static final double AURA_START_RADIUS = 0.6;
    private static final double AURA_RADIUS_PER_GROUP = 0.1;
    private static final double AURA_Y_OFFSET = 0.25;
    private static final double PROGRESS_BURST_Y_OFFSET = 1.0;
    private static final int PROGRESS_BURST_PARTICLE_COUNT = 45;

    // Metadata key
    private static final String META_TROPHY = "trophy";
    private static final String CRYSTAL_OWNER_KEY = "god_trophy_crystal_owner";
    private static final String NPC_OWNER_KEY = "survival_skills_god_trophy_owner";
    private static final double LOCATION_MATCH_EPSILON_SQUARED = 0.01;
    private static final double FULL_CIRCLE_RADIANS = Math.PI * 2;

    // Center fields were previously used for orbit; logic now derives from base
    // location + HALF.
    private final SurvivalSkills plugin;
    private final Location trophyLoc;
    private final int trophyId;
    private final String playerName;
    private final UUID playerUUID;
    private final NamespacedKey crystalOwnerKey;

    private ItemDisplay display;
    private ItemDisplay blackHole;
    private EnderCrystal crystal;
    private NPC npcPlayer;
    private boolean movingUp = false;
    private boolean idleEffectsInitialized = false;
    private double crystalRadians = 0;
    private Color auraColor = Color.fromRGB(160, 160, 160);
    private int completedGroups = 0;

    public GodTrophyEffects(SurvivalSkills plugin, Location trophyLoc, int trophyId, String playerName,
                            UUID playerUUID) {
        this.plugin = Objects.requireNonNull(plugin);
        this.trophyLoc = Objects.requireNonNull(trophyLoc).clone();
        this.trophyId = trophyId;
        this.playerName = Objects.requireNonNull(playerName);
        this.playerUUID = Objects.requireNonNull(playerUUID);
        this.crystalOwnerKey = new NamespacedKey(plugin, CRYSTAL_OWNER_KEY);
    }

    public static void removeOrphanedCrystals(SurvivalSkills plugin, Map<Integer, Location> activeGodTrophies) {
        NamespacedKey ownerKey = new NamespacedKey(plugin, CRYSTAL_OWNER_KEY);
        Bukkit.getWorlds().stream()
                .map((World world) -> world.getEntities())
                .flatMap((List<org.bukkit.entity.Entity> entities) -> entities.stream())
                .<EnderCrystal>mapMulti((org.bukkit.entity.Entity entity,
                        Consumer<EnderCrystal> crystalConsumer) -> {
                    if (entity instanceof EnderCrystal enderCrystal)
                        crystalConsumer.accept(enderCrystal);
                })
                .filter((EnderCrystal enderCrystal) -> isOrphanedCrystal(
                        enderCrystal, ownerKey, activeGodTrophies))
                .forEach((EnderCrystal enderCrystal) -> enderCrystal.remove());
    }

    private static boolean isOrphanedCrystal(EnderCrystal enderCrystal, NamespacedKey ownerKey,
                                              Map<Integer, Location> activeGodTrophies) {
        Integer ownerId = enderCrystal.getPersistentDataContainer().get(ownerKey, PersistentDataType.INTEGER);
        if (ownerId != null) {
            Location trophyLocation = activeGodTrophies.get(ownerId);
            return trophyLocation == null || !isWithinCrystalBounds(enderCrystal.getLocation(), trophyLocation);
        }
        if (!enderCrystal.isInvulnerable() || enderCrystal.isShowingBottom())
            return false;
        Location beamTarget = enderCrystal.getBeamTarget();
        if (beamTarget == null || !hasGodTrophyBeamGeometry(beamTarget))
            return false;
        return activeGodTrophies.values().stream()
                .noneMatch((Location trophyLocation) -> locationsMatch(
                        beamTarget, trophyLocation.clone().add(HALF, NPC_Y_OFFSET, HALF)));
    }

    private static boolean isWithinCrystalBounds(Location crystalLocation, Location trophyLocation) {
        Location center = trophyLocation.clone().add(HALF, 0, HALF);
        return Objects.equals(crystalLocation.getWorld(), center.getWorld())
                && Math.abs(crystalLocation.getX() - center.getX()) <= CRYSTAL_CLEANUP_RADIUS
                && Math.abs(crystalLocation.getY() - center.getY()) <= CRYSTAL_CLEANUP_RADIUS
                && Math.abs(crystalLocation.getZ() - center.getZ()) <= CRYSTAL_CLEANUP_RADIUS;
    }

    private static boolean locationsMatch(Location first, Location second) {
        return Objects.equals(first.getWorld(), second.getWorld())
                && first.distanceSquared(second) <= LOCATION_MATCH_EPSILON_SQUARED;
    }

    private static boolean hasGodTrophyBeamGeometry(Location beamTarget) {
        return Math.abs(beamTarget.getX() - Math.floor(beamTarget.getX()) - HALF) <=
                LOCATION_MATCH_EPSILON_SQUARED
                && Math.abs(beamTarget.getY() - Math.rint(beamTarget.getY())) <= LOCATION_MATCH_EPSILON_SQUARED
                && Math.abs(beamTarget.getZ() - Math.floor(beamTarget.getZ()) - HALF) <=
                LOCATION_MATCH_EPSILON_SQUARED;
    }

    public void tickTrophy(int cycle) {
        if (cycle < CYCLE_EFFECTS_START) {
            growDirtBlock(cycle);
            displayDirtBlock(cycle);
            handleTransitionEvents(cycle);
            return; // All pre-122 events stop here; idle effects start at 122+
        }
        tickIdleEffects(cycle);
    }

    private void tickIdleEffects(int cycle) {
        if (cycle <= CYCLE_NPC_SPAWN)
            return;
        initializeIdleEffects();
        if (cycle % 2 == 0) {
            World world = world();
            if (world != null)
                world.spawnParticle(Particle.ENCHANT, offset(0, 0, 0), 40);
        }
        if (cycle % 4 == 0)
            moveCrystal();
        if (cycle % AURA_PARTICLE_INTERVAL == 0)
            questParticleEffect();
    }

    private void handleTransitionEvents(int cycle) {
        if (cycle == CYCLE_BLACKHOLE_SHRINK_START)
            removeDisplay();

        if (cycle >= CYCLE_BLACKHOLE_SHRINK_START && cycle < CYCLE_BLACKHOLE_SHRINK_END) {
            changeBlackHoleSize(0.20f);
        } else if (cycle == CYCLE_BLACKHOLE_REMOVE) {
            playSound(Sound.ENTITY_ENDERMAN_TELEPORT);
            removeBlackHole();
        } else if (cycle == CYCLE_LIGHTNING) {
            var w = world();
            if (w == null)
                return;
            w.strikeLightningEffect(offset(0, 0, 0));
            playSound(Sound.ENTITY_LIGHTNING_BOLT_THUNDER);
        } else if (cycle == CYCLE_NPC_SPAWN) {
            initializeIdleEffects();
        }
    }

    private void initializeIdleEffects() {
        if (idleEffectsInitialized || world() == null)
            return;
        spawnPlayer(playerName, playerUUID);
        crystal = reconcileExistingCrystals().orElseGet(() -> spawnCrystal(0, 1.5, 0));
        idleEffectsInitialized = true;
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

    private void startAnimation() {
        var w = world();
        if (w == null)
            return;
        display = w.spawn(offset(0, DISPLAY_START_Y_OFFSET, 0), ItemDisplay.class);
        display.setItemStack(new ItemStack(Material.GRASS_BLOCK));
        Transformation transformation = display.getTransformation();
        transformation.getScale().set(INITIAL_SCALE);
        display.setTransformation(transformation);
    }

    private void floatingEffect(int cycle) {
        if (cycle % FLOATING_INTERVAL == 0)
            movingUp = !movingUp;
        teleport(0, movingUp ? FLOATING_STEP : -FLOATING_STEP, 0);
    }

    private void rotateDisplay(float x, float y, float z) {
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

    private void teleport(double xChange, double yChange, double zChange) {
        if (display == null)
            return;
        display.teleport(display.getLocation().clone().add(xChange, yChange, zChange));
    }

    private void scaleDisplay(float scale) {
        if (display == null)
            return;
        Transformation transformation = display.getTransformation();
        transformation.getScale().set(scale);
        display.setTransformation(transformation);
    }

    private void playSound(Sound sound) {
        var w = world();
        if (w == null)
            return;
        w.playSound(trophyLoc, sound, 1, 1);
    }

    private void removeDisplay() {
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
        idleEffectsInitialized = false;
    }

    private void spawnBlackHole() {
        var w = world();
        if (w == null)
            return;
        blackHole = w.spawn(offset(0, DISPLAY_START_Y_OFFSET, 0), ItemDisplay.class);
        blackHole.setItemStack(new ItemStack(Material.AIR));
        blackHole.setShadowRadius(1.5f);
        blackHole.setShadowStrength(5f);
    }

    private void changeBlackHoleSize(float change) {
        if (blackHole == null)
            return;
        blackHole.setShadowRadius(Math.max(0, blackHole.getShadowRadius() - change));
    }

    private void removeBlackHole() {
        if (blackHole != null) {
            blackHole.remove();
            blackHole = null;
        }
    }

    public void spawnPlayer(String name, UUID playerUUID) {
        if (TrophyManager.getCitizensRegistryProvider() == null) return;
        if (!findOrCreateNPC()) return;
        configureAppearance(name, playerUUID);
        scheduleNpcText(name, playerUUID);
    }

    /**
     * Locates or creates the NPC for this trophy.
     * Reuses the in-memory reference, persistent Citizens ownership, or a legacy
     * location match before creating a fresh NPC.
     */
    private boolean findOrCreateNPC() {
        NPCRegistry registry = TrophyManager.getCitizensRegistryProvider().getRegistry();
        Integer storedId = plugin.getTrophyManager().getGodNPCIDs().get(playerUUID);
        if (storedId != null) {
            NPC storedNPC = registry.getById(storedId);
            if (storedNPC != null && isNPCForThisTrophy(storedNPC)) {
                npcPlayer = storedNPC;
            }
        }

        if (npcPlayer == null) {
            List<NPC> matchingNPCs = StreamSupport.stream(registry.spliterator(), false)
                    .filter((NPC candidateNPC) -> isNPCForThisTrophy(candidateNPC))
                    .toList();
            if (!matchingNPCs.isEmpty()) {
                npcPlayer = matchingNPCs.getFirst();
                matchingNPCs.stream().skip(1)
                        .forEach((NPC duplicateNPC) -> destroyNPC(registry, duplicateNPC));
            }
        }

        if (npcPlayer == null)
            npcPlayer = registry.createNPC(EntityType.PLAYER, TrophyManager.npcName);
        if (npcPlayer == null)
            return false;

        npcPlayer.data().setPersistent(NPC_OWNER_KEY, trophyId);
        npcPlayer.setProtected(true);
        plugin.getTrophyManager().getGodNPCIDs().put(playerUUID, npcPlayer.getId());
        return npcPlayer.isSpawned() || npcPlayer.spawn(offset(0, NPC_Y_OFFSET, 0));
    }

    private boolean isNPCForThisTrophy(NPC candidateNPC) {
        MetadataStore npcData = candidateNPC.data();
        if (npcData.has(NPC_OWNER_KEY)) {
            Integer ownerId = npcData.get(NPC_OWNER_KEY, Integer.MIN_VALUE);
            return ownerId.intValue() == trophyId;
        }
        Location storedLocation = candidateNPC.getStoredLocation();
        return storedLocation != null
                && Objects.equals(storedLocation.getWorld(), trophyLoc.getWorld())
                && storedLocation.distanceSquared(offset(0, NPC_Y_OFFSET, 0)) <= LOCATION_MATCH_EPSILON_SQUARED
                && isGodTrophyNPCName(candidateNPC);
    }

    private boolean isGodTrophyNPCName(NPC candidateNPC) {
        return Objects.equals(candidateNPC.getName(), TrophyManager.npcName)
                || Objects.equals(candidateNPC.getFullName(), TrophyManager.npcName)
                || Objects.equals(candidateNPC.getRawName(), TrophyManager.npcName);
    }

    private void destroyNPC(NPCRegistry registry, NPC removedNPC) {
        removedNPC.despawn();
        removedNPC.destroy();
        registry.deregister(removedNPC);
    }

    /**
     * Applies gravity and skin to the NPC.
     * Skin data is fetched asynchronously when the player is offline.
     */
    private void configureAppearance(String name, UUID playerUUID) {
        Gravity gravity = npcPlayer.getOrAddTrait(Gravity.class);
        gravity.setHasGravity(false);

        Player p = Bukkit.getPlayer(name);
        SkinTrait skin = npcPlayer.getOrAddTrait(SkinTrait.class);
        if (p != null) {
            skin.setSkinPersistent(p);
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        String[] info = MojangAPI.getSkinData(playerUUID);
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                skin.setSkinPersistent(playerUUID.toString(), info[0], info[1]);
                            }
                        }.runTask(plugin);
                    } catch (IOException | InterruptedException e) {
                        Bukkit.getLogger().log(Level.WARNING,
                                String.format("[SurvivalSkills] Failed to fetch skin data for %s", playerUUID), e);
                    }
                }
            }.runTaskAsynchronously(plugin);
        }
    }

    /**
     * Schedules NPC text and LookClose trait on a short delay after spawn.
     */
    private void scheduleNpcText(String name, UUID playerUUID) {
        NPC scheduledNPC = npcPlayer;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (npcPlayer != scheduledNPC)
                    return;
                getText(name, playerUUID);
                LookClose look = npcPlayer.getOrAddTrait(LookClose.class);
                look.lookClose(true);
            }
        }.runTaskLater(plugin, 20);
    }

    private void getText(String name, UUID uuid) {
        Text text = npcPlayer.getOrAddTrait(Text.class);
        text.toggleTalkClose();
        text.setRange(TALK_RANGE);
        text.setDelay(TALK_DELAY_TICKS);
        replaceNpcDialogue(text, name, uuid);
    }

    public void setAuraColor(Color auraColor) {
        this.auraColor = Objects.requireNonNull(auraColor);
    }

    public void updateNpcPersonality(int completedGroups) {
        this.completedGroups = Math.clamp(completedGroups, 0, 9);
        if (npcPlayer == null)
            return;
        replaceNpcDialogue(npcPlayer.getOrAddTrait(Text.class), playerName, playerUUID);
    }

    public void playProgressBurst() {
        World world = world();
        if (world == null)
            return;
        Particle.DustOptions dust = new Particle.DustOptions(auraColor, 1.35f);
        world.spawnParticle(Particle.DUST, offset(0, PROGRESS_BURST_Y_OFFSET, 0),
                PROGRESS_BURST_PARTICLE_COUNT, 0.65, 0.75, 0.65, 0.02, dust);
        world.playSound(offset(0, PROGRESS_BURST_Y_OFFSET, 0), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.2f);
    }

    private void replaceNpcDialogue(Text text, String name, UUID uuid) {
        text.getTexts().clear();
        npcDialogue(name).forEach((String line) -> text.add(line));
        if (completedGroups == 0
                && !SurvivalSkills.getInstance().getTrophyManager().getPlayerGodQuestData().containsKey(uuid)) {
            text.add(ChatColor.GOLD + "Right click me to start the " + ChatColor.BOLD + "God Quest");
        }
    }

    private List<String> npcDialogue(String name) {
        if (completedGroups >= 9) {
            return List.of(
                    ChatColor.GOLD + "You have surpassed all who came before",
                    ChatColor.LIGHT_PURPLE + "The realm of gods awaits you",
                    ChatColor.AQUA + "Carry your power with wisdom, " + name);
        }
        if (completedGroups >= 7) {
            return List.of(
                    ChatColor.LIGHT_PURPLE + "You approach the threshold of godhood",
                    ChatColor.AQUA + "Soon you will understand what I am",
                    ChatColor.GOLD + "Few have come this close, " + name);
        }
        if (completedGroups >= 3) {
            return List.of(
                    ChatColor.GOLD + "You've brought me much... but will you finish?",
                    ChatColor.AQUA + "The crops were a good start, but wealth is not power",
                    ChatColor.LIGHT_PURPLE + "I am still testing you, " + name);
        }
        return List.of(
                ChatColor.AQUA + "Who put me up here?",
                ChatColor.RED + "I will feed upon your flesh " + ChatColor.MAGIC + "and soul",
                ChatColor.AQUA + "You should definitely try to break these crystals",
                ChatColor.GOLD + "I hear there is a secret hidden in this world");
    }

    private void removePlayer() {
        if (npcPlayer == null) return;
        npcPlayer.despawn();
        npcPlayer = null;
    }

    public void destroyPlayer() {
        if (TrophyManager.getCitizensRegistryProvider() == null)
            return;
        NPCRegistry registry = TrophyManager.getCitizensRegistryProvider().getRegistry();
        StreamSupport.stream(registry.spliterator(), false)
                .filter((NPC candidateNPC) -> isNPCForThisTrophy(candidateNPC))
                .toList()
                .forEach((NPC removedNPC) -> destroyNPC(registry, removedNPC));
        plugin.getTrophyManager().getGodNPCIDs().remove(playerUUID);
        npcPlayer = null;
    }

    private EnderCrystal spawnCrystal(double x, double y, double z) {
        World world = world();
        if (world == null)
            throw new IllegalStateException("Cannot spawn a god trophy crystal without a world");
        removeCrystal();
        EnderCrystal spawnedCrystal = (EnderCrystal) world.spawnEntity(offset(x, y, z), EntityType.END_CRYSTAL);
        configureCrystal(spawnedCrystal);
        return spawnedCrystal;
    }

    private void removeCrystal() {
        if (crystal != null) {
            if (crystal.isValid() && !crystal.isDead())
                crystal.remove();
            crystal = null;
        }
        nearbyCrystals().stream()
                .filter((EnderCrystal nearbyCrystal) -> isCrystalForThisTrophy(nearbyCrystal))
                .forEach((EnderCrystal nearbyCrystal) -> nearbyCrystal.remove());
    }

    private void moveCrystal() {
        if (crystal == null || !crystal.isValid() || crystal.isDead()) {
            crystal = reconcileExistingCrystals().orElseGet(() -> spawnCrystal(
                    Math.cos(crystalRadians) * CRYSTAL_ORBIT_RADIUS, 1.5,
                    Math.sin(crystalRadians) * CRYSTAL_ORBIT_RADIUS));
        }

        double x = Math.cos(crystalRadians) * CRYSTAL_ORBIT_RADIUS;
        double z = Math.sin(crystalRadians) * CRYSTAL_ORBIT_RADIUS;
        crystal.teleport(offset(x, 1.5, z));
        crystalRadians = (crystalRadians + CRYSTAL_ANGLE_INCREMENT) % FULL_CIRCLE_RADIANS;
    }

    private Optional<EnderCrystal> reconcileExistingCrystals() {
        List<EnderCrystal> ownedCrystals = nearbyCrystals().stream()
                .filter((EnderCrystal nearbyCrystal) -> isCrystalForThisTrophy(nearbyCrystal))
                .toList();
        if (ownedCrystals.isEmpty())
            return Optional.empty();

        EnderCrystal retainedCrystal = ownedCrystals.getFirst();
        configureCrystal(retainedCrystal);
        ownedCrystals.stream().skip(1)
                .forEach((EnderCrystal duplicateCrystal) -> duplicateCrystal.remove());
        return Optional.of(retainedCrystal);
    }

    private List<EnderCrystal> nearbyCrystals() {
        World world = world();
        if (world == null)
            return List.of();
        return world.getNearbyEntities(offset(0, 0, 0), CRYSTAL_CLEANUP_RADIUS,
                        CRYSTAL_CLEANUP_RADIUS, CRYSTAL_CLEANUP_RADIUS).stream()
                .<EnderCrystal>mapMulti((org.bukkit.entity.Entity entity,
                        Consumer<EnderCrystal> crystalConsumer) -> {
                    if (entity instanceof EnderCrystal nearbyCrystal)
                        crystalConsumer.accept(nearbyCrystal);
                })
                .toList();
    }

    private boolean isCrystalForThisTrophy(EnderCrystal nearbyCrystal) {
        Integer ownerId = nearbyCrystal.getPersistentDataContainer().get(crystalOwnerKey,
                PersistentDataType.INTEGER);
        if (ownerId != null)
            return ownerId.intValue() == trophyId;
        return isLegacyCrystalForThisTrophy(nearbyCrystal);
    }

    private boolean isLegacyCrystalForThisTrophy(EnderCrystal nearbyCrystal) {
        Location beamTarget = nearbyCrystal.getBeamTarget();
        return nearbyCrystal.isInvulnerable()
                && !nearbyCrystal.isShowingBottom()
                && beamTarget != null
                && Objects.equals(beamTarget.getWorld(), trophyLoc.getWorld())
                && beamTarget.distanceSquared(offset(0, NPC_Y_OFFSET, 0)) <= LOCATION_MATCH_EPSILON_SQUARED;
    }

    private void configureCrystal(EnderCrystal configuredCrystal) {
        configuredCrystal.setBeamTarget(offset(0, NPC_Y_OFFSET, 0));
        configuredCrystal.setMetadata(META_TROPHY, new FixedMetadataValue(plugin, true));
        configuredCrystal.getPersistentDataContainer().set(crystalOwnerKey, PersistentDataType.INTEGER, trophyId);
        configuredCrystal.setShowingBottom(false);
        configuredCrystal.setInvulnerable(true);
        configuredCrystal.setPersistent(true);
    }

    private void questParticleEffect() {
        World world = world();
        if (world == null)
            return;
        double radius = AURA_START_RADIUS + (completedGroups * AURA_RADIUS_PER_GROUP);
        Particle.DustOptions dust = new Particle.DustOptions(auraColor, 1.0f);
        Location center = offset(0, AURA_Y_OFFSET, 0);
        for (int particleIndex = 0; particleIndex < AURA_PARTICLE_COUNT; particleIndex++) {
            double angle = (Math.PI * 2 * particleIndex) / AURA_PARTICLE_COUNT;
            Location particleLocation = center.clone().add(Math.cos(angle) * radius, 0,
                    Math.sin(angle) * radius);
            world.spawnParticle(Particle.DUST, particleLocation, 1, dust);
        }
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
