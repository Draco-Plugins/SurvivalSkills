package sir_draco.survivalskills.skill_listeners.fighting;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.skills.SkillManager;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Owns the mob-to-XP-multiplier mapping for the fighting skill. The mapping is
 * loaded from {@code fightingmobxp.yml} so server admins can tune rewards without
 * recompiling. Previously this was a 37-line hardcoded {@code HashMap} populated
 * in the_listener's constructor.
 */
public class MobXPManager {

    private static final String CONFIG_FILE = "fightingmobxp.yml";
    private static final String MOB_XP_SECTION = "MobXP";
    /** Multiplier applied to the base FightingXP for mobs not present in the config. */
    private static final double DEFAULT_UNKNOWN_MOB_MULTIPLIER = 0.5;

    private final SurvivalSkills plugin;
    private final Map<EntityType, Double> mobXP = new EnumMap<>(EntityType.class);

    public MobXPManager(SurvivalSkills plugin) {
        this.plugin = plugin;
        loadMobXP();
    }

    private void loadMobXP() {
        File file = new File(plugin.getDataFolder(), CONFIG_FILE);
        if (!file.exists())
            plugin.saveResource(CONFIG_FILE, false);
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = data.getConfigurationSection(MOB_XP_SECTION);
        if (section == null) {
            Bukkit.getLogger().log(Level.WARNING,
                    "[SurvivalSkills] Missing " + MOB_XP_SECTION + " section in " + CONFIG_FILE);
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                EntityType type = EntityType.valueOf(key);
                mobXP.put(type, section.getDouble(key));
            } catch (IllegalArgumentException error) {
                Bukkit.getLogger().log(Level.WARNING,
                        String.format("[SurvivalSkills] Unknown entity type in %s: %s", CONFIG_FILE, key));
            }
        }
    }

    /**
     * Award fighting experience to {@code p} for killing {@code ent}, using the
     * configured multiplier. Player-created iron golems and absent entries use
     * the same fallback behaviour as the original implementation.
     */
    public void handleExperience(Player p, Entity ent) {
        EntityType type = ent.getType();
        if (!mobXP.containsKey(type)) {
            killExperience(p, plugin.getSkillManager().getFightingXP() * DEFAULT_UNKNOWN_MOB_MULTIPLIER);
            return;
        }
        if (type.equals(EntityType.IRON_GOLEM) && ((IronGolem) ent).isPlayerCreated())
            return;
        killExperience(p, plugin.getSkillManager().getFightingXP() * mobXP.get(type));
    }

    public void killExperience(Player p, double experience) {
        SkillManager.experienceEvent(plugin, p, experience, SkillCategory.FIGHTING);
    }
}