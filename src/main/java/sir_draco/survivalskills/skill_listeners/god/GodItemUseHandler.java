package sir_draco.survivalskills.skill_listeners.god;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import sir_draco.survivalskills.skill_listeners.god.items.GodItemAction;
import sir_draco.survivalskills.skill_listeners.god.items.GodItemActions;
import sir_draco.survivalskills.utils.items.ItemModelData;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Dispatches right-click activation of god items to per-item
 * {@link GodItemAction} strategies, and hosts the adjacent god-item event
 * handlers (revive zombie, explosion immunity, power laser cooldowns).
 */
public class GodItemUseHandler implements Listener {

    // Custom model data identifiers
    private static final int MODEL_WEB_SHOOTER = ItemModelData.WEB_SHOOTER.getId();
    private static final int MODEL_VILLAGER_REVIVAL = ItemModelData.VILLAGER_REVIVAL_ARTIFACT.getId();
    private static final int MODEL_ENDER_ESSENCE = ItemModelData.ENDER_ESSENCE.getId();
    private static final int MODEL_CREEPER_ESSENCE = ItemModelData.CREEPER_ESSENCE.getId();
    private static final int MODEL_CHARGED_CREEPER_ESSENCE = ItemModelData.CHARGED_CREEPER_ESSENCE.getId();
    private static final int MODEL_POTION_BAG = ItemModelData.POTION_BAG.getId();
    private static final int MODEL_WIND_CHARGE = ItemModelData.MAGIC_BAG_OF_WIND.getId();
    private static final int MODEL_DRAGON_FIREBALL = ItemModelData.DRAGON_BREATH_CANNON.getId();
    private static final int MODEL_WITHER_ROSE = ItemModelData.UNLIMITED_WITHER_ROSE.getId();
    private static final int MODEL_TRIDENT_LAUNCHER = ItemModelData.TRIDENT_LAUNCHER.getId();
    private static final int MODEL_SPONGE = ItemModelData.UNLIMITED_SPONGE.getId();
    private static final int MODEL_POWER_SWORD = ItemModelData.POWER_SWORD.getId();
    private static final int MODEL_POWER_LASER = ItemModelData.POWER_LASER.getId();
    private static final int MODEL_SNOWBALL_CANNON = ItemModelData.SNOWBALL_CANNON.getId();
    private static final int MODEL_RAVAGER_DASH = ItemModelData.RAVAGER_DASH.getId();
    private static final int MODEL_BIOME_FINDER = ItemModelData.BIOME_FINDER.getId();
    private static final int MODEL_WITHER_SKULL_CANNON = ItemModelData.WITHER_SKULL_CANNON.getId();
    private static final int MODEL_FIREBALL_CANNON = ItemModelData.FIREBALL_CANNON.getId();

    private static final float CREEPER_ESSENCE_EXPLOSION_POWER = 5.0f;
    private static final float CHARGED_CREEPER_ESSENCE_EXPLOSION_POWER = 10.0f;

    // Villager revival
    private static final int CONVERSION_TIME_TICKS = 40;
    private static final int CURE_PARTICLE_COUNT = 30;

    private final Map<Integer, GodItemAction> itemActions = new LinkedHashMap<>();
    private final Set<Player> powerLaserCooldowns = new HashSet<>();

    public GodItemUseHandler(PotionBagListener potionBagListener, BiomeFinderListener biomeFinderListener) {
        register(MODEL_WEB_SHOOTER, new GodItemActions.WebShooterItemAction());
        register(MODEL_ENDER_ESSENCE, new GodItemActions.EnderEssenceItemAction());
        register(MODEL_CREEPER_ESSENCE,
                new GodItemActions.CreeperEssenceItemAction(CREEPER_ESSENCE_EXPLOSION_POWER));
        register(MODEL_CHARGED_CREEPER_ESSENCE,
                new GodItemActions.CreeperEssenceItemAction(CHARGED_CREEPER_ESSENCE_EXPLOSION_POWER));
        register(MODEL_POTION_BAG, new GodItemActions.PotionBagItemAction(potionBagListener));
        register(MODEL_WIND_CHARGE, new GodItemActions.WindChargeItemAction());
        register(MODEL_DRAGON_FIREBALL, new GodItemActions.DragonFireballItemAction());
        register(MODEL_WITHER_ROSE, new GodItemActions.WitherRoseItemAction());
        register(MODEL_TRIDENT_LAUNCHER, new GodItemActions.TridentLauncherItemAction());
        register(MODEL_SPONGE, new GodItemActions.SpongeItemAction());
        register(MODEL_POWER_SWORD, new GodItemActions.PowerSwordItemAction());
        register(MODEL_POWER_LASER, new GodItemActions.PowerLaserItemAction(this));
        register(MODEL_SNOWBALL_CANNON, new GodItemActions.SnowballCannonItemAction());
        register(MODEL_RAVAGER_DASH, new GodItemActions.RavagerDashItemAction());
        register(MODEL_BIOME_FINDER, new GodItemActions.BiomeFinderItemAction(biomeFinderListener));
        register(MODEL_WITHER_SKULL_CANNON, new GodItemActions.WitherSkullCannonItemAction());
        register(MODEL_FIREBALL_CANNON, new GodItemActions.FireballCannonItemAction());
    }

    private void register(int modelData, GodItemAction action) {
        itemActions.put(modelData, action);
    }

    @EventHandler
    public void onUseGodItem(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack hand = e.getItem();
        if (!ItemStackGeneratorUtils.isCustomItem(hand))
            return;
        if (e.getHand() == null)
            return;
        if (!isRightClick(e.getAction()))
            return;
        ItemMeta meta = hand.getItemMeta();
        if (meta == null)
            return;

        for (Map.Entry<Integer, GodItemAction> entry : itemActions.entrySet()) {
            if (ItemStackGeneratorUtils.hasCustomModelData(meta, entry.getKey())) {
                entry.getValue().execute(p, hand, meta, e);
                return;
            }
        }
    }

    private static boolean isRightClick(Action action) {
        return Action.RIGHT_CLICK_AIR.equals(action) || Action.RIGHT_CLICK_BLOCK.equals(action);
    }

    @EventHandler
    public void reviveZombie(PlayerInteractEntityEvent e) {
        if (!org.bukkit.inventory.EquipmentSlot.HAND.equals(e.getHand()))
            return;
        if (!org.bukkit.entity.EntityType.ZOMBIE_VILLAGER.equals(e.getRightClicked().getType()))
            return;
        Player p = e.getPlayer();
        ItemStack mainHand = p.getInventory().getItemInMainHand();
        if (!ItemStackGeneratorUtils.isCustomItem(mainHand, MODEL_VILLAGER_REVIVAL))
            return;

        ZombieVillager zombie = (ZombieVillager) e.getRightClicked();
        zombie.setConversionTime(CONVERSION_TIME_TICKS);
        Location loc = e.getRightClicked().getLocation();
        if (loc.getWorld() == null)
            return;
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1, 1);
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, CURE_PARTICLE_COUNT,
                Math.random(), Math.random(), Math.random());
    }

    @EventHandler
    public void handleGodDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p))
            return;
        ItemStack mainHand = p.getInventory().getItemInMainHand();
        if (!ItemStackGeneratorUtils.isCustomItem(mainHand, MODEL_CREEPER_ESSENCE)
                && !ItemStackGeneratorUtils.isCustomItem(mainHand, MODEL_CHARGED_CREEPER_ESSENCE))
            return;
        if (!EntityDamageEvent.DamageCause.BLOCK_EXPLOSION.equals(e.getCause())
                && !EntityDamageEvent.DamageCause.ENTITY_EXPLOSION.equals(e.getCause()))
            return;
        e.setCancelled(true);
    }

    // --- Power laser cooldown management (targeted access for PowerLaser) ---

    public boolean isOnPowerLaserCooldown(Player player) {
        return powerLaserCooldowns.contains(player);
    }

    public void addPowerLaserCooldown(Player player) {
        powerLaserCooldowns.add(player);
    }

    public void removePowerLaserCooldown(Player player) {
        powerLaserCooldowns.remove(player);
    }
}
