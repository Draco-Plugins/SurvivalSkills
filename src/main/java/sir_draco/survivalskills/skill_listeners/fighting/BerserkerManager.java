package sir_draco.survivalskills.skill_listeners.fighting;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.abilities.AbilityManager;
import sir_draco.survivalskills.abilities.AbilityTimer;
import sir_draco.survivalskills.abilities.BerserkerEffects;
import sir_draco.survivalskills.rewards.PlayerRewards;
import sir_draco.survivalskills.skills.SkillCategory;

import java.util.List;
import java.util.Objects;

/**
 * Berserker ability lifecycle and on-hit bonus. Previously the FightingSkill
 * maintained a parallel {@code activeBerserkers} list that desynced from the real
 * ability timers in {@link AbilityManager}. AbilityManager is now the single
 * source of truth: a player is "currently berserking" iff AbilityManager reports
 * an active "Berserker" timer for them.
 */
public class BerserkerManager {

    public static final String ABILITY_NAME = "Berserker";

    private static final double BERSERKER_DAMAGE_MULTIPLIER = 1.5;
    private static final int BERSERKER_PARTICLE_COUNT = 5;
    private static final double BERSERKER_HEALTH_THRESHOLD_RATIO = 0.25;
    private static final double BERSERKER_HEALTH_COST_RATIO = 0.25;

    private static final List<Material> VALID_WEAPONS = List.of(
            Material.DIAMOND_SWORD, Material.GOLDEN_SWORD, Material.IRON_SWORD,
            Material.NETHERITE_SWORD, Material.STONE_SWORD, Material.TRIDENT, Material.WOODEN_SWORD
    );

    private record BerserkerTier(String rewardName, int resetTime, int activeTime) {
        static final BerserkerTier MAX = new BerserkerTier(null, 15, 10);
    }

    private static final List<BerserkerTier> BERSERKER_TIERS = List.of(
            new BerserkerTier("BerserkerII", 120, 3),
            new BerserkerTier("BerserkerIII", 90, 5),
            new BerserkerTier("BerserkerIV", 60, 5),
            new BerserkerTier("BerserkerV", 45, 5),
            new BerserkerTier("BerserkerVI", 45, 8),
            new BerserkerTier("BerserkerVII", 30, 8),
            BerserkerTier.MAX
    );

    private final SurvivalSkills plugin;
    private final AbilityManager abilityManager;

    public BerserkerManager(SurvivalSkills plugin, AbilityManager abilityManager) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.abilityManager = Objects.requireNonNull(abilityManager, "abilityManager");
    }

    /** True iff the player currently has an actively-running Berserker ability. */
    public boolean isBerserkerActive(Player p) {
        return abilityManager.isAbilityActive(p, ABILITY_NAME);
    }

    /** Whether {@code item} is one of the weapons that can trigger berserker. */
    public boolean holdingWeapon(ItemStack item) {
        return VALID_WEAPONS.contains(item.getType());
    }

    /**
     * Handle the interact-to-berserk flow: report cooldown if on cooldown, otherwise
     * activate. The interact listener is responsible for the sneaking/weapon/hand
     * guards before calling this.
     */
    public void tryActivate(Player p) {
        AbilityTimer timer = abilityManager.getAbility(p, ABILITY_NAME);
        if (timer != null) {
            if (!timer.isActive()) {
                p.sendRawMessage(ChatColor.RED + "You can use Berserker in " + ChatColor.AQUA
                        + timer.getTimeTillReset() + ChatColor.RED + " seconds");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            }
            return;
        }
        newBerserker(p);
    }

    /**
     * Apply the berserker on-hit damage bonus and particle effect. Caller is
     * responsible for first checking {@link #isBerserkerActive(Player)}.
     */
    public void applyBerserkerDamageBonus(Player p, EntityDamageByEntityEvent e) {
        e.setDamage(e.getDamage() * BERSERKER_DAMAGE_MULTIPLIER);
        Entity target = e.getEntity();
        for (int i = 0; i < BERSERKER_PARTICLE_COUNT; i++) {
            p.getWorld().spawnParticle(Particle.DUST, target.getLocation(), 1, Math.random(), 0.5,
                    Math.random(), new Particle.DustOptions(Color.RED, 1));
        }
    }

    /**
     * Attempt to activate berserker for {@code p}. Caller must already confirm no
     * existing Berserker timer (handled by the interact listener).
     */
    public void newBerserker(Player p) {
        AttributeInstance health = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (health == null)
            return;
        if (p.getHealth() < health.getValue() * BERSERKER_HEALTH_THRESHOLD_RATIO) {
            p.sendRawMessage(ChatColor.RED + "Not enough health for berserker!");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }

        PlayerRewards reward = plugin.getSkillManager().getPlayerRewards(p);
        if (!reward.getReward(SkillCategory.FIGHTING, "BerserkerI").isApplied()) {
            p.sendRawMessage(ChatColor.RED + "Berserker is unlocked at level: " + ChatColor.AQUA
                    + plugin.getSkillManager().getDefaultPlayerRewards().getReward(SkillCategory.FIGHTING, "BerserkerI")
                            .getLevel());
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }

        BerserkerTier tier = getBerserkerTier(reward);
        AbilityTimer timer = new AbilityTimer(plugin, ABILITY_NAME, p, tier.activeTime(), tier.resetTime());
        timer.runTaskTimerAsynchronously(plugin, 0, 20);
        abilityManager.addAbility(p, timer);
        BerserkerEffects berserker = new BerserkerEffects(p, tier.activeTime());
        berserker.runTaskTimer(plugin, 0, 5);
        p.sendRawMessage(ChatColor.GREEN + "Berserker is active!");
        p.playSound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 1, 1);
        p.damage(health.getValue() * BERSERKER_HEALTH_COST_RATIO);
    }

    /**
     * End the berserker ability when the player dies, so the on-hit bonus stops.
     * Previously this was a side-effect of removing the player from a parallel
     * "active berserkers" list; AbilityManager is now the single source of truth.
     */
    public void endBerserkerOnDeath(Player p) {
        abilityManager.removeAbility(p, ABILITY_NAME);
    }

    private static BerserkerTier getBerserkerTier(PlayerRewards rewards) {
        for (BerserkerTier tier : BERSERKER_TIERS) {
            if (tier.rewardName() == null || !rewards.getReward(SkillCategory.FIGHTING, tier.rewardName()).isApplied())
                return tier;
        }
        return BerserkerTier.MAX;
    }
}
