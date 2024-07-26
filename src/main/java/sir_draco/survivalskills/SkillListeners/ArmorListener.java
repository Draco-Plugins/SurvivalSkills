package sir_draco.survivalskills.SkillListeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.Abilities.RainbowArmor;
import sir_draco.survivalskills.ItemStackGenerator;
import sir_draco.survivalskills.Rewards.Reward;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

public class ArmorListener implements Listener {

    private final SurvivalSkills plugin;
    public static final ArrayList<UUID> playersWearingBeaconArmor = new ArrayList<>();
    public static final ArrayList<PotionEffect> beaconEffects = new ArrayList<>();

    public ArmorListener(SurvivalSkills plugin) {
        this.plugin = plugin;
        createBeaconEffects();
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent e) {
        // If all their armor is beacon armor add them to the list
        Player p = e.getPlayer();
        playerWearingBeaconArmor(p, p.getInventory().getArmorContents());
    }

    @EventHandler
    public void playerLeave(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        playersWearingBeaconArmor.remove(p.getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();

        if (e.getKeepInventory()) {
            playersWearingBeaconArmor.remove(p.getUniqueId());
            return;
        }

        // Check if they have the keep inventory skill
        Reward reward = plugin.getSkillManager().getPlayerRewards(p).getReward("Main", "KeepInventory");
        if (reward.isApplied()) return;

        playersWearingBeaconArmor.remove(p.getUniqueId());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!isBeaconArmorPiece(hand) && !playersWearingBeaconArmor.contains(p.getUniqueId())) return;
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK) && !e.getAction().equals(Action.RIGHT_CLICK_AIR)) return;
        if (!isArmor(hand.getType())) return;

        e.setCancelled(true);
        p.sendRawMessage(ChatColor.RED + "You can not quick swap beacon armor pieces");
        p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    }

    @EventHandler
    public void inventoryClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        Inventory clickedInventory = e.getClickedInventory();

        if (clickedInventory == null) return;

        // Check if the player puts a beacon armor piece in their armor slot
        if (isPlayerInventory(clickedInventory) && e.getSlotType().equals(InventoryType.SlotType.ARMOR)) {
            PlayerInventory playerInventory = p.getInventory();
            playerWearingBeaconArmor(p, playerInventory.getArmorContents());
            return;
        }

        // Check if armor is shift clicked onto the player
        if (e.isShiftClick()) {
            ItemStack currentItem = e.getCurrentItem();
            if (currentItem == null) return;
            if (!isBeaconArmorPiece(currentItem)) return;
            new BukkitRunnable() {
                @Override
                public void run() {
                    playerWearingBeaconArmor(p, p.getInventory().getArmorContents());
                }
            }.runTaskLater(plugin, 1);
        }
    }

    public boolean isArmor(Material mat) {
        return Objects.nonNull(mat)
                && mat.name().endsWith("_HELMET")
                || mat.name().endsWith("_CHESTPLATE")
                || mat.name().endsWith("_LEGGINGS")
                || mat.name().endsWith("_BOOTS");
    }

    public void playerWearingBeaconArmor(Player p, ItemStack[] armor) {
        for (ItemStack item : armor) {
            if (isBeaconArmorPiece(item)) continue;
            playersWearingBeaconArmor.remove(p.getUniqueId());
            return;
        }
        if (playersWearingBeaconArmor.contains(p.getUniqueId())) return;
        Reward reward = SurvivalSkills.getInstance().getSkillManager().getPlayerRewards(p).getReward("Mining", "BeaconArmor");
        if (!reward.isApplied()) return;
        playersWearingBeaconArmor.add(p.getUniqueId());
        new RainbowArmor(p).runTaskTimer(SurvivalSkills.getInstance(), 0, 1);
    }

    private static boolean isPlayerInventory(Inventory inventory) {
        return inventory.getType().equals(InventoryType.PLAYER);
    }

    public static boolean isBeaconArmorPiece(ItemStack item) {
        if (item == null) return false;
        return ItemStackGenerator.isCustomItem(item, 29);
    }

    public void createBeaconEffects() {
        PotionEffect regen = new PotionEffect(PotionEffectType.REGENERATION, 80, 0);
        PotionEffect resistance = new PotionEffect(PotionEffectType.RESISTANCE, 80, 0);
        PotionEffect speed = new PotionEffect(PotionEffectType.SPEED, 80, 0);
        PotionEffect strength = new PotionEffect(PotionEffectType.STRENGTH, 80, 0);
        PotionEffect jump = new PotionEffect(PotionEffectType.JUMP_BOOST, 80, 0);
        PotionEffect haste = new PotionEffect(PotionEffectType.HASTE, 80, 0);

        beaconEffects.add(regen);
        beaconEffects.add(resistance);
        beaconEffects.add(speed);
        beaconEffects.add(strength);
        beaconEffects.add(jump);
        beaconEffects.add(haste);
    }
}
