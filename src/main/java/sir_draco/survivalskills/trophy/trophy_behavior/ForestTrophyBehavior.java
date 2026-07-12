package sir_draco.survivalskills.trophy.trophy_behavior;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.trophy.TrophyEffects;

import java.util.ArrayList;

public class ForestTrophyBehavior implements TrophyEffectBehavior {

    private static final int CYCLE_RESET = 40;
    private static final int MAX_ITEMS = 4;

    private final ArrayList<Item> itemList = new ArrayList<>();

    @Override
    public int tick(TrophyEffects effects, int cycle, World world, Location loc) {
        if (cycle % 3 == 0) {
            effects.floorParticles(Particle.WITCH);
        }

        if (cycle % 5 != 0) {
            return cycle;
        }

        ItemStack item = switch (cycle) {
            case 5 -> effects.addPersistentDataContainer(new ItemStack(Material.OAK_SAPLING));
            case 10 -> effects.addPersistentDataContainer(new ItemStack(Material.SPRUCE_SAPLING));
            case 15 -> effects.addPersistentDataContainer(new ItemStack(Material.ACACIA_SAPLING));
            case 20 -> effects.addPersistentDataContainer(new ItemStack(Material.BIRCH_SAPLING));
            case 25 -> effects.addPersistentDataContainer(new ItemStack(Material.CHERRY_SAPLING));
            case 30 -> effects.addPersistentDataContainer(new ItemStack(Material.JUNGLE_SAPLING));
            case 35 -> effects.addPersistentDataContainer(new ItemStack(Material.MANGROVE_PROPAGULE));
            case 40 -> effects.addPersistentDataContainer(new ItemStack(Material.DARK_OAK_SAPLING));
            default -> new ItemStack(Material.AIR);
        };

        if (cycle == CYCLE_RESET) {
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
        for (Item item : itemList) {
            item.remove();
        }
        itemList.clear();
    }
}
