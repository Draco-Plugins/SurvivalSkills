package sir_draco.survivalskills.trophy.trophy_behavior;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Squid;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.trophy.TrophyEffects;

import java.util.ArrayList;

public class FishingTrophyBehavior implements TrophyEffectBehavior {

    private static final int CYCLE_RESET = 20;
    private static final int MAX_ITEMS = 8;

    private final ArrayList<Item> itemList = new ArrayList<>();
    private Entity mob;

    @Override
    public int tick(TrophyEffects effects, int cycle, World world, Location loc) {
        if (cycle % 3 == 0) {
            effects.floorParticles(Particle.WITCH);
        }

        ItemStack item = switch (cycle) {
            case 5 -> effects.addPersistentDataContainer(new ItemStack(Material.COD));
            case 10 -> effects.addPersistentDataContainer(new ItemStack(Material.SALMON));
            case 15 -> effects.addPersistentDataContainer(new ItemStack(Material.PUFFERFISH));
            case 20 -> effects.addPersistentDataContainer(new ItemStack(Material.TROPICAL_FISH));
            default -> effects.addPersistentDataContainer(new ItemStack(Material.AIR));
        };

        if (cycle % 5 != 0) {
            return cycle;
        }

        if (cycle == CYCLE_RESET) {
            double chance = Math.random();
            if (mob != null && chance < 0.2) {
                mob.remove();
                mob = null;
            } else if (mob == null && chance < 0.2) {
                mob = world.spawnEntity(loc.clone().add(0.5, 1.0, 0.5), EntityType.SQUID);
                Squid squid = (Squid) mob;
                squid.setPersistent(true);
                squid.setGravity(false);
                squid.setVelocity(
                        new Vector((Math.random() - 0.5) * 0.5, Math.random() * 0.25, (Math.random() - 0.5) * 0.5));
                squid.setCollidable(false);
                squid.setInvulnerable(true);
            }
            effects.spawnDecorativeItem(item, itemList, MAX_ITEMS);
            return 1;
        }

        effects.spawnDecorativeItem(item, itemList, MAX_ITEMS);
        return cycle;
    }

    @Override
    public void start(TrophyEffects effects, Location loc, World world, SurvivalSkills plugin) {
    }

    @Override
    public void cleanup() {
        if (mob != null) {
            mob.remove();
            mob = null;
        }
        for (Item item : itemList) {
            item.remove();
        }
        itemList.clear();
    }
}
