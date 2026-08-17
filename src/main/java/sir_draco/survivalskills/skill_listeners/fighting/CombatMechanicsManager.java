package sir_draco.survivalskills.skill_listeners.fighting;

import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.rewards.PlayerRewards;

/**
 * On-hit combat rewards for the fighting skill: critical hits and lifesteal.
 * Extracted from the monolithic damage listener so the fighting damage handler
 * is a thin delegator. Behaviour is unchanged from the original inline logic.
 */
public class CombatMechanicsManager {

    private static final double CRITICAL_DAMAGE_MULTIPLIER = 2.0;
    private static final int CRITICAL_PARTICLE_COUNT = 5;
    private static final double LIFESTEAL_HEAL_AMOUNT = 1.0;

    private final SurvivalSkills plugin;

    public CombatMechanicsManager(SurvivalSkills plugin) {
        this.plugin = plugin;
    }

    /** Roll a critical hit; if it lands, double damage and play the crit effect. */
    public void applyCritical(Player p, EntityDamageByEntityEvent e) {
        PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
        if (rewards == null)
            return;
        double criticalChance = rewards.getCriticalChance();
        if (criticalChance == 0 || Math.random() >= criticalChance)
            return;
        e.setDamage(e.getDamage() * CRITICAL_DAMAGE_MULTIPLIER);
        p.sendRawMessage(ChatColor.GOLD + "Critical Hit!");
        p.playSound(p, Sound.BLOCK_ANVIL_HIT, 1, 1);
        Entity target = e.getEntity();
        for (int i = 0; i < CRITICAL_PARTICLE_COUNT; i++) {
            p.getWorld().spawnParticle(Particle.CRIT, target.getLocation(), 1, Math.random(), 0.5,
                    Math.random());
        }
    }

    /** Roll lifesteal; if it lands and the player is below max health, heal them. */
    public void applyLifesteal(Player p, EntityDamageByEntityEvent e) {
        PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
        if (rewards == null)
            return;
        double lifesteal = rewards.getLifesteal();
        if (lifesteal == 0 || Math.random() >= lifesteal)
            return;
        AttributeInstance healthAttribute = p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        if (healthAttribute == null)
            return;
        double health = p.getHealth();
        if (health >= healthAttribute.getValue())
            return;
        p.setHealth(Math.min(healthAttribute.getValue(), health + LIFESTEAL_HEAL_AMOUNT));
        p.playSound(p, Sound.ENTITY_GENERIC_DRINK, 1, 1);
        p.sendRawMessage(ChatColor.GREEN + "Lifesteal Activated!");
    }
}
