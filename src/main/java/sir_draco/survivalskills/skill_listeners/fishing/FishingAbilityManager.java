package sir_draco.survivalskills.skill_listeners.fishing;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.utils.items.ItemModelData;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;

import java.util.ArrayList;

/**
 * Groups the fishing-skill auxiliary features that do not belong to loot,
 * mechanics, trash or artifacts: the water-breathing reward, the rain broadcast,
 * and the unlimited-bucket custom items. Each is small and self-contained, but
 * kept here so {@code FishingSkill} does not own four unrelated event handlers.
 */
public class FishingAbilityManager {

    private static final int MAX_REMAINING_AIR = 300;

    /** Custom-item identifier used by the unlimited bucket pickup guard. */
    private static final int UNLIMITED_BUCKET_CUSTOM_ITEM_ID = ItemModelData.UNLIMITED_EMPTY_BUCKET.getId();
    private static final int UNLIMITED_POWDER_SNOW_BUCKET_CUSTOM_ITEM_ID =
            ItemModelData.UNLIMITED_POWDER_SNOW_BUCKET.getId();

    private final SurvivalSkills plugin;
    private final ArrayList<Player> waterBreathers = new ArrayList<>();

    public FishingAbilityManager(SurvivalSkills plugin) {
        this.plugin = plugin;
    }

    public ArrayList<Player> getWaterBreathers() {
        return waterBreathers;
    }

    // =================================================================
    // Water breathing
    // =================================================================

    public void playerMoveInWater(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!p.isSwimming() && !p.isInWater())
            return;
        if (plugin.getSkillManager().getPlayerRewards(p)
                .getReward(SkillCategory.FISHING, "WaterBreathingIII").isApplied()) {
            p.setRemainingAir(MAX_REMAINING_AIR);
            return;
        }
        if (waterBreathers.contains(p))
            p.setRemainingAir(MAX_REMAINING_AIR);
    }

    // =================================================================
    // Rain broadcast
    // =================================================================

    public void rainEvent(WeatherChangeEvent e) {
        // Bukkit fires WeatherChangeEvent before the storm state flips, so
        // "currently no storm" means a storm is about to begin.
        if (e.getWorld().hasStorm())
            return;
        Bukkit.broadcastMessage(ChatColor.YELLOW + "Rain has started falling. Fishing speeds greatly increased!");
        for (Player p : Bukkit.getOnlinePlayers())
            p.playSound(p, Sound.ENTITY_FISHING_BOBBER_SPLASH, 1, 1);
    }

    // =================================================================
    // Unlimited buckets (custom items defined by ItemStackGeneratorUtils)
    // =================================================================

    public void onBucketPickup(PlayerBucketFillEvent e) {
        ItemStack hand = ItemStackGeneratorUtils.getItemInHand(e.getPlayer(), e.getHand());
        if (!ItemStackGeneratorUtils.isCustomItem(hand, UNLIMITED_BUCKET_CUSTOM_ITEM_ID))
            return;
        Material type = e.getBlockClicked().getType();
        if (!type.equals(Material.WATER) && !type.equals(Material.LAVA))
            return;

        e.setCancelled(true);
        e.getBlockClicked().setType(Material.AIR);
    }

    public void onBucketUse(PlayerBucketEmptyEvent e) {
        ItemStack hand = ItemStackGeneratorUtils.getItemInHand(e.getPlayer(), e.getHand());
        if (!ItemStackGeneratorUtils.isCustomItem(hand))
            return;

        if (hand.getType().equals(Material.WATER_BUCKET)) {
            e.setCancelled(true);
            Block relative = e.getBlockClicked().getRelative(e.getBlockFace());
            relative.setType(Material.WATER);
        } else if (hand.getType().equals(Material.LAVA_BUCKET)) {
            e.setCancelled(true);
            Block relative = e.getBlockClicked().getRelative(e.getBlockFace());
            relative.setType(Material.LAVA);
        }
    }

    public void onBlockPlace(BlockPlaceEvent e) {
        ItemStack placedItem = e.getItemInHand();
        if (!ItemStackGeneratorUtils.isCustomItem(placedItem,
                UNLIMITED_POWDER_SNOW_BUCKET_CUSTOM_ITEM_ID))
            return;

        Player player = e.getPlayer();
        EquipmentSlot hand = e.getHand();
        int mainHandSlot = player.getInventory().getHeldItemSlot();
        ItemStack preservedBucket = placedItem.clone();
        Bukkit.getScheduler().runTask(plugin,
                () -> restorePowderSnowBucket(player, hand, mainHandSlot, preservedBucket));
    }

    static void restorePowderSnowBucket(Player player, EquipmentSlot hand,
            int mainHandSlot, ItemStack preservedBucket) {
        PlayerInventory inventory = player.getInventory();
        if (EquipmentSlot.OFF_HAND.equals(hand))
            inventory.setItemInOffHand(preservedBucket);
        else
            inventory.setItem(mainHandSlot, preservedBucket);
        player.updateInventory();
    }
}
