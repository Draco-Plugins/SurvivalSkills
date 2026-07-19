package sir_draco.survivalskills.skill_listeners.god.items;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Trident;
import org.bukkit.entity.WindCharge;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.abilities.godItems.EnderEssence;
import sir_draco.survivalskills.abilities.items.PowerLaser;
import sir_draco.survivalskills.abilities.items.PowerSword;
import sir_draco.survivalskills.abilities.godItems.RavagerDash;
import sir_draco.survivalskills.skill_listeners.god.BiomeFinderListener;
import sir_draco.survivalskills.skill_listeners.god.GodItemUseHandler;
import sir_draco.survivalskills.skill_listeners.god.PotionBagListener;
import sir_draco.survivalskills.utils.Utils;

/**
 * Concrete {@link GodItemAction} implementations, one per god item. Kept in a
 * single file because each class is tiny and they are only referenced from
 * {@link GodItemUseHandler}.
 */
public final class GodItemActions {
    private GodItemActions() {
    }

    /** Custom model data 33 - fires a falling cobweb in the player's aim direction. */
    public static final class WebShooterItemAction implements GodItemAction {
        @Override
        public void execute(Player p, ItemStack item, ItemMeta meta, PlayerInteractEvent e) {
            Vector velocity = p.getLocation().getDirection().multiply(2);
            FallingBlock cobweb = p.getWorld().spawnFallingBlock(
                    p.getLocation().clone().add(0, 1, 0), Material.COBWEB.createBlockData());
            cobweb.setHurtEntities(false);
            cobweb.setVelocity(velocity);
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EGG_THROW, 1, 1);
        }
    }

    /** Custom model data 36 - teleports the player toward where they are looking. */
    public static final class EnderEssenceItemAction implements GodItemAction {
        @Override
        public void execute(Player p, ItemStack item, ItemMeta meta, PlayerInteractEvent e) {
            e.setCancelled(true);
            Location loc = p.getLocation().clone().add(p.getLocation().getDirection().multiply(5));
            new EnderEssence(p, loc).runTaskAsynchronously(SurvivalSkills.getInstance());
        }
    }

    /** Creates an explosion centred on the player. */
    public static final class CreeperEssenceItemAction implements GodItemAction {
        private final float explosionPower;

        public CreeperEssenceItemAction(float explosionPower) {
            this.explosionPower = explosionPower;
        }

        @Override
        public void execute(Player p, ItemStack item, ItemMeta meta, PlayerInteractEvent e) {
            p.getWorld().createExplosion(p.getLocation(), explosionPower, false, true, p);
        }
    }

    /** Custom model data 39 - launches a wind charge. */
    public static final class WindChargeItemAction implements GodItemAction {
        @Override
        public void execute(Player p, ItemStack item, ItemMeta meta, PlayerInteractEvent e) {
            e.setCancelled(true);
            p.launchProjectile(WindCharge.class, p.getLocation().getDirection().multiply(2));
        }
    }

    /** Custom model data 53 - launches a snowball without consuming the cannon. */
    public static final class SnowballCannonItemAction implements GodItemAction {
        @Override
        public void execute(Player p, ItemStack item, ItemMeta meta, PlayerInteractEvent e) {
            e.setCancelled(true);
            p.launchProjectile(Snowball.class, p.getLocation().getDirection().multiply(2));
        }
    }

    /** Custom model data 65 - launches a fireball without consuming the cannon. */
    public static final class FireballCannonItemAction implements GodItemAction {
        @Override
        public void execute(Player p, ItemStack item, ItemMeta meta, PlayerInteractEvent e) {
            e.setCancelled(true);
            p.launchProjectile(Fireball.class, p.getLocation().getDirection().multiply(2));
        }
    }

    /** Custom model data 61 - charges forward and damages mobs in the player's path. */
    public static final class RavagerDashItemAction implements GodItemAction {
        @Override
        public void execute(Player p, ItemStack item, ItemMeta meta, PlayerInteractEvent e) {
            e.setCancelled(true);
            RavagerDash.activate(p);
        }
    }

    /** Custom model data 62 - opens the biome selection picker. */
    public static final class BiomeFinderItemAction implements GodItemAction {
        private final BiomeFinderListener biomeFinderListener;

        public BiomeFinderItemAction(BiomeFinderListener biomeFinderListener) {
            this.biomeFinderListener = biomeFinderListener;
        }

        @Override
        public void execute(Player p, ItemStack item, ItemMeta meta, PlayerInteractEvent e) {
            e.setCancelled(true);
            biomeFinderListener.openBiomePicker(p, 0);
        }
    }

    /** Custom model data 40 - launches a dragon fireball. */
    public static final class DragonFireballItemAction implements GodItemAction {
        @Override
        public void execute(Player p, ItemStack item, ItemMeta meta, PlayerInteractEvent e) {
            e.setCancelled(true);
            p.launchProjectile(DragonFireball.class, p.getLocation().getDirection().multiply(2));
        }
    }

    /** Custom model data 64 - launches a wither skull. */
    public static final class WitherSkullCannonItemAction implements GodItemAction {
        @Override
        public void execute(Player p, ItemStack item, ItemMeta meta, PlayerInteractEvent e) {
            e.setCancelled(true);
            p.launchProjectile(WitherSkull.class, p.getLocation().getDirection().multiply(2));
        }
    }

    /** Custom model data 43 - launches a non-pickable trident. */
    public static final class TridentLauncherItemAction implements GodItemAction {
        @Override
        public void execute(Player p, ItemStack item, ItemMeta meta, PlayerInteractEvent e) {
            e.setCancelled(true);
            Trident trident = p.launchProjectile(Trident.class, p.getLocation().getDirection().multiply(2));
            trident.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        }
    }

    /** Custom model data 47 - spinning dash attack; requires Power Ore. */
    public static final class PowerSwordItemAction implements GodItemAction {
        @Override
        public void execute(Player p, ItemStack item, ItemMeta meta, PlayerInteractEvent e) {
            if (!PowerOreGate.hasPowerOreReward(p)) {
                PowerOreGate.sendLockedMessage(p, "power swords special ability");
                return;
            }
            e.setCancelled(true);
            PowerSword.activate(p);
        }
    }

    /** Custom model data 50 - fires a warden sonic boom laser; requires Power Ore. */
    public static final class PowerLaserItemAction implements GodItemAction {
        private final GodItemUseHandler handler;

        public PowerLaserItemAction(GodItemUseHandler handler) {
            this.handler = handler;
        }

        @Override
        public void execute(Player p, ItemStack item, ItemMeta meta, PlayerInteractEvent e) {
            e.setCancelled(true);
            if (handler.isOnPowerLaserCooldown(p))
                return;
            if (!PowerOreGate.hasPowerOreReward(p)) {
                PowerOreGate.sendLockedMessage(p, "power laser");
                return;
            }
            handler.addPowerLaserCooldown(p);
            PowerLaser laser = new PowerLaser(p, handler);
            laser.runTaskTimer(SurvivalSkills.getInstance(), 0, 1);
        }
    }

    /** Custom model data 38 - opens the player's potion bag. */
    public static final class PotionBagItemAction implements GodItemAction {
        private final PotionBagListener potionBagListener;

        public PotionBagItemAction(PotionBagListener potionBagListener) {
            this.potionBagListener = potionBagListener;
        }

        @Override
        public void execute(Player p, ItemStack item, ItemMeta meta, PlayerInteractEvent e) {
            potionBagListener.openPotionBag(p, item);
        }
    }

    /**
     * Shared behaviour for god items that place a block against the clicked
     * face (wither rose and sponge). Both perform the same claim/region/space
     * checks and only differ in the placed material.
     */
    public abstract static class BlockPlacingGodItemAction implements GodItemAction {
        @Override
        public void execute(Player p, ItemStack item, ItemMeta meta, PlayerInteractEvent e) {
            e.setCancelled(true);
            if (e.getHand() == null)
                return;
            if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
                return;
            if (e.getClickedBlock() == null)
                return;

            if (SurvivalSkills.getInstance().isGriefPreventionEnabled()
                    && Utils.checkForClaim(p, e.getClickedBlock().getLocation()))
                return;
            if (SurvivalSkills.getInstance().isWorldGuardEnabled()) {
                boolean canPlace = SurvivalSkills.getInstance().getWorldGuardProvider()
                        .canPlaceBlockInRegion(p, e.getClickedBlock().getLocation());
                if (!canPlace)
                    return;
            }

            Block desiredBlock = e.getClickedBlock().getRelative(e.getBlockFace());
            if (!desiredBlock.getType().isAir() && !desiredBlock.getType().equals(Material.WATER))
                return;
            BlockState state = desiredBlock.getState();
            state.setType(getBlockType());
            desiredBlock.setType(getBlockType());
            state.update(true);
        }

        protected abstract Material getBlockType();
    }

    /** Custom model data 41 - places a wither rose. */
    public static final class WitherRoseItemAction extends BlockPlacingGodItemAction {
        @Override
        protected Material getBlockType() {
            return Material.WITHER_ROSE;
        }
    }

    /** Custom model data 46 - places a sponge. */
    public static final class SpongeItemAction extends BlockPlacingGodItemAction {
        @Override
        protected Material getBlockType() {
            return Material.SPONGE;
        }
    }
}
