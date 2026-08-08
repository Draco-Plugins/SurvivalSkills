package sir_draco.survivalskills.skill_listeners.god;

import org.bukkit.Color;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.god_questline.GodTrophyQuest;
import sir_draco.survivalskills.utils.items.ItemStackGenerator;
import sir_draco.survivalskills.utils.items.ItemModelData;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Handles god item drops from specific mobs and the unlimited tipped arrow
 * fired from bows.
 */
public class GodItemDropListener implements Listener {

    // Drop chances
    private static final double BOSS_DROP_CHANCE = 0.1;
    private static final double SPECIAL_DROP_CHANCE = 0.01;
    private static final double GENERIC_DROP_CHANCE = 0.001;
    private static final double MOB_ITEM_DROP_CHANCE = 0.004;
    private static final int MOB_ITEM_PHASE_START = 49;
    private static final int MOB_ITEM_PHASE_END = 57;

    // Tipped arrow effect
    private static final int ARROW_EFFECT_DURATION_TICKS = 160;
    private static final int ARROW_EFFECT_AMPLIFIER = 0;
    private static final int RGB_BOUND = 256;

    private final Map<EntityType, ItemStack> godItems = new HashMap<>();
    private final List<PotionEffectType> potionEffects = new java.util.ArrayList<>();

    public GodItemDropListener() {
        createGodWeaponMap();
        createPotionList();
    }

    @EventHandler
    public void dropGodWeapon(EntityDeathEvent e) {
        EntityType type = e.getEntityType();
        if (!godItems.containsKey(type))
            return;

        double chance = Math.random();

        // Special cases
        if (EntityType.ENDER_DRAGON.equals(type) && chance <= BOSS_DROP_CHANCE) {
            dropItemNaturally(e, type);
        } else if (EntityType.CREEPER.equals(type)) {
            Creeper creeper = (Creeper) e.getEntity();
            if (creeper.isPowered() && chance <= SPECIAL_DROP_CHANCE) {
                addToDrops(e, ItemStackGenerator.getChargedCreeperEssence());
            } else if (chance <= getGenericDropChance(e)) {
                addToDrops(e, type);
            }
            return;
        } else if (EntityType.BREEZE.equals(type) && chance <= SPECIAL_DROP_CHANCE) {
            addToDrops(e, type);
        } else if (EntityType.WITHER.equals(type)) {
            if (chance <= SPECIAL_DROP_CHANCE) {
                addToDrops(e, type);
            }
            return;
        } else if (EntityType.ELDER_GUARDIAN.equals(type)) {
            if (chance <= BOSS_DROP_CHANCE) {
                addToDrops(e, type);
            }
            return;
        }

        // Rest of the mobs
        if (chance > getGenericDropChance(e))
            return;

        if (EntityType.WITCH.equals(type)) {
            playGuitarSound(e);
            e.getDrops().add(ItemStackGenerator.getNewPotionBag());
        } else {
            addToDrops(e, type);
        }
    }

    private double getGenericDropChance(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null)
            return GENERIC_DROP_CHANCE;

        GodTrophyQuest quest = SurvivalSkills.getInstance().getTrophyManager()
                .getPlayerGodQuestData().get(killer.getUniqueId());
        if (quest == null)
            return GENERIC_DROP_CHANCE;

        int phase = quest.getPhase();
        if (phase >= MOB_ITEM_PHASE_START && phase <= MOB_ITEM_PHASE_END)
            return MOB_ITEM_DROP_CHANCE;
        return GENERIC_DROP_CHANCE;
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent e) {
        if (!(e.getEntity() instanceof Player p))
            return;
        ItemStack arrow = e.getConsumable();
        if (!ItemStackGeneratorUtils.isCustomItem(arrow, ItemModelData.UNLIMITED_TIPPED_ARROW.getId()))
            return;

        Arrow oldArrow = (Arrow) e.getProjectile();
        Arrow projectile = p.launchProjectile(Arrow.class, oldArrow.getVelocity());
        e.setCancelled(true);
        projectile.addCustomEffect(getRandomPotionEffect(), true);
        projectile.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
        Random random = new Random();
        projectile.setColor(Color.fromRGB(random.nextInt(RGB_BOUND), random.nextInt(RGB_BOUND), random.nextInt(RGB_BOUND)));
    }

    public PotionEffect getRandomPotionEffect() {
        PotionEffectType type = potionEffects.get(ThreadLocalRandom.current().nextInt(potionEffects.size()));
        return new PotionEffect(type, ARROW_EFFECT_DURATION_TICKS, ARROW_EFFECT_AMPLIFIER);
    }

    private void addToDrops(EntityDeathEvent e, EntityType type) {
        addToDrops(e, godItems.get(type));
    }

    private void addToDrops(EntityDeathEvent e, ItemStack item) {
        e.getDrops().add(item);
        playGuitarSound(e);
    }

    private void dropItemNaturally(EntityDeathEvent e, EntityType type) {
        Entity entity = e.getEntity();
        entity.getWorld().dropItemNaturally(entity.getLocation(), godItems.get(type));
        entity.getWorld().playSound(entity.getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1, 1);
    }

    private void playGuitarSound(EntityDeathEvent e) {
        e.getEntity().getWorld().playSound(e.getEntity().getLocation(), Sound.BLOCK_NOTE_BLOCK_GUITAR, 1, 1);
    }

    private void createGodWeaponMap() {
        godItems.put(EntityType.SPIDER, ItemStackGenerator.getWebShooter());
        godItems.put(EntityType.SKELETON, ItemStackGenerator.getUnlimitedTippedArrow());
        godItems.put(EntityType.ZOMBIE, ItemStackGenerator.getVillagerRevivalArtifact());
        godItems.put(EntityType.ENDERMAN, ItemStackGenerator.getEnderEssence());
        godItems.put(EntityType.CREEPER, ItemStackGenerator.getCreeperEssence());
        godItems.put(EntityType.WITCH, ItemStackGenerator.getNewPotionBag());
        godItems.put(EntityType.DROWNED, ItemStackGenerator.getTridentLauncher());
        godItems.put(EntityType.BREEZE, ItemStackGenerator.getMagicBagOfWind());
        godItems.put(EntityType.STRAY, ItemStackGenerator.getSnowballCannon());
        godItems.put(EntityType.BLAZE, ItemStackGenerator.getFireballCannon());
        godItems.put(EntityType.ENDER_DRAGON, ItemStackGenerator.getDragonBreathCannon());
        godItems.put(EntityType.GUARDIAN, ItemStackGenerator.getUnlimitedSponge());
        godItems.put(EntityType.WITHER_SKELETON, ItemStackGenerator.getUnlimitedWitherRose());
        godItems.put(EntityType.WITHER, ItemStackGenerator.getWitherSkullCannon());
        godItems.put(EntityType.RAVAGER, ItemStackGenerator.getRavagerDash());
        godItems.put(EntityType.ELDER_GUARDIAN, ItemStackGenerator.getBiomeFinder());
    }

    private void createPotionList() {
        for (PotionEffectType type : Registry.EFFECT)
            potionEffects.add(type);
    }
}
