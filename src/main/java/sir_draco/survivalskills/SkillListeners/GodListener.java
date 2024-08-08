package sir_draco.survivalskills.SkillListeners;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.Utils.ItemStackGenerator;

import java.util.HashMap;

public class GodListener implements Listener {

    private final SurvivalSkills plugin;
    private final HashMap<EntityType, ItemStack> godWeapons = new HashMap<>();

    public GodListener(SurvivalSkills plugin) {
        this.plugin = plugin;
        createGodWeaponMap();
    }

    @EventHandler
    public void dropGodWeapon(EntityDeathEvent e) {
        EntityType type = e.getEntityType();
        double chance = Math.random();
        if (!godWeapons.containsKey(type)) return;

        if (type.equals(EntityType.ENDER_DRAGON) && chance <= 0.1)
            e.getDrops().add(godWeapons.get(type));
        else if (chance <= 0.001)
            e.getDrops().add(godWeapons.get(type));
    }

    @EventHandler
    public void onUseGodWeapon(PlayerInteractEvent e) {
        if (e.getHand() == null || !e.getHand().equals(EquipmentSlot.HAND)) return;
        if (!e.getAction().equals(Action.RIGHT_CLICK_AIR) && !e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        Player p = e.getPlayer();
        ItemStack mainHand = p.getInventory().getItemInMainHand();

        if (ItemStackGenerator.isCustomItem(mainHand, 33)) {
            Vector velocity = p.getLocation().getDirection().multiply(2);
            FallingBlock cobweb = p.getWorld().spawnFallingBlock(p.getLocation().clone().add(0, 1, 0),
                    Material.COBWEB.createBlockData());
            cobweb.setHurtEntities(false);
            cobweb.setVelocity(velocity);
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EGG_THROW, 1, 1);
        }
    }

    public void createGodWeaponMap() {
        godWeapons.put(EntityType.SPIDER, ItemStackGenerator.getWebShooter());
    }
}
