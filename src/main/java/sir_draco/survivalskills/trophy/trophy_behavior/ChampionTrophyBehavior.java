package sir_draco.survivalskills.trophy.trophy_behavior;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.trophy.CircularRotationObject;
import sir_draco.survivalskills.trophy.TrophyEffects;

import java.util.ArrayList;

public class ChampionTrophyBehavior implements TrophyEffectBehavior {

    private static final int CYCLE_RESET = 360;
    private static final Material[] CHAMPION_MATERIALS = {
            Material.ZOMBIE_HEAD,
            Material.ENDER_EYE,
            Material.NETHER_STAR,
            Material.ECHO_SHARD,
            Material.DRAGON_HEAD,
            Material.COBWEB,
            Material.PLAYER_HEAD
    };

    private final ArrayList<Item> itemList = new ArrayList<>();
    private CircularRotationObject mobTrophyOrbital;

    @Override
    public int tick(TrophyEffects effects, int cycle, World world, Location loc) {
        moveChampionItems(effects);
        if (cycle == CYCLE_RESET) {
            return 1;
        }
        return cycle;
    }

    @Override
    public void start(TrophyEffects effects, Location loc, World world, SurvivalSkills plugin) {
        mobTrophyOrbital = new CircularRotationObject(loc, 1, 7);
        populateItemList(world, loc, effects);
        resetLocation();
    }

    @Override
    public void cleanup() {
        for (Item item : itemList) {
            item.remove();
        }
        itemList.clear();
    }

    private void populateItemList(World world, Location loc, TrophyEffects effects) {
        for (Material material : CHAMPION_MATERIALS) {
            Item item = (Item) world.spawnEntity(loc, EntityType.ITEM);
            item.setMetadata(TrophyEffects.TROPHY_ITEM, new FixedMetadataValue(effects.getPlugin(), true));
            item.setItemStack(effects.addPersistentDataContainer(new ItemStack(material)));
            effects.setFloatingItemProperties(item);
            itemList.add(item);
        }
    }

    private void resetLocation() {
        mobTrophyOrbital.createLocations(
                CircularRotationObject.getAngle(itemList.getFirst().getLocation(), mobTrophyOrbital.getCenter()) + 0.01
        );
        for (int i = 0; i < itemList.size(); i++) {
            Item item = itemList.get(i);
            item.teleport(mobTrophyOrbital.getLocation(i));
        }
    }

    private void moveChampionItems(TrophyEffects effects) {
        for (Item item : itemList) {
            item.setVelocity(mobTrophyOrbital.getVelocityVector(item.getLocation(), 0.04));
            if (mobTrophyOrbital.tooFar(item.getLocation())) {
                resetLocation();
                break;
            }
        }
    }
}
