package sir_draco.survivalskills.abilities.items;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import sir_draco.survivalskills.utils.items.ItemModelData;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;

import java.util.Objects;

public final class GiantSword {

    static final double DROP_CHANCE = 0.1;
    private static final int EXPERIENCE_MULTIPLIER = 2;

    private GiantSword() {}

    public static boolean shouldDrop(double roll) {
        return roll >= 0 && roll < DROP_CHANCE;
    }

    public static void applyExperienceBonus(EntityDeathEvent event, Player killer) {
        if (!(event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent damageEvent))
            return;
        if (!(damageEvent.getDamager() instanceof Player attackingPlayer))
            return;
        if (!Objects.equals(attackingPlayer, killer))
            return;
        if (!isGiantSword(killer.getInventory().getItemInMainHand()))
            return;
        event.setDroppedExp(doubleExperience(event.getDroppedExp()));
    }

    static int doubleExperience(int experience) {
        long doubledExperience = (long) experience * EXPERIENCE_MULTIPLIER;
        return (int) Math.min(Integer.MAX_VALUE, doubledExperience);
    }

    public static boolean isGiantSword(ItemStack item) {
        return ItemStackGeneratorUtils.isCustomItem(item, ItemModelData.GIANT_SWORD.getId());
    }
}
