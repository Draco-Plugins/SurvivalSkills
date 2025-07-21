package sir_draco.survivalskills.SkillListeners;

import org.bukkit.Material;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.Abilities.Armor.*;
import sir_draco.survivalskills.Utils.ItemStackGenerator;
import sir_draco.survivalskills.Rewards.PlayerRewards;
import sir_draco.survivalskills.Rewards.Reward;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

public class ArmorListener implements Listener {

    private final SurvivalSkills plugin;
    public static final ArrayList<UUID> playersWearingJumpingBoots = new ArrayList<>();
    public static final ArrayList<UUID> playersWearingWandererArmor = new ArrayList<>();
    public static final ArrayList<UUID> playersWearingTravelerArmor = new ArrayList<>();
    public static final ArrayList<UUID> playersWearingGillArmor = new ArrayList<>();
    public static final ArrayList<UUID> playersWearingAdventurerArmor = new ArrayList<>();
    public static final ArrayList<UUID> playersWearingBeaconArmor = new ArrayList<>();
    public static final ArrayList<PotionEffect> beaconEffects = new ArrayList<>();

    public ArmorListener(SurvivalSkills plugin) {
        this.plugin = plugin;
        createBeaconEffects();
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent e) {
        // Check what armor they are wearing
        Player p = e.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                checkHealthRegen(p);
                playerWearingJumpingBoots(p, p.getInventory().getArmorContents());
                playerWearingWandererArmor(p, p.getInventory().getArmorContents());
                playerWearingTravelerArmor(p, p.getInventory().getArmorContents());
                playerWearingGillArmor(p, p.getInventory().getArmorContents());
                playerWearingAdventurerArmor(p, p.getInventory().getArmorContents());
                playerWearingBeaconArmor(p, p.getInventory().getArmorContents());
            }
        }.runTaskLater(plugin, 20);
    }

    @EventHandler
    public void playerLeave(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        removeArmors(p.getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();

        if (!e.getKeepInventory()) {
            removeArmors(p.getUniqueId());
            return;
        }

        // Check if they have the keep inventory skill
        Reward reward = plugin.getSkillManager().getPlayerRewards(p).getReward("Main", "KeepInventory");
        if (reward.isApplied()) return;

        removeArmors(p.getUniqueId());
    }

    @EventHandler
    public void armorSwap(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK) && !e.getAction().equals(Action.RIGHT_CLICK_AIR)) return;
        if (!isArmor(hand.getType())) return;

        if (ItemStackGenerator.isCustomItem(hand, 4)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    playerWearingJumpingBoots(p, p.getInventory().getArmorContents());
                }
            }.runTaskLater(plugin, 1);
        }
        else if (ItemStackGenerator.isCustomItem(hand, 5)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    playerWearingWandererArmor(p, p.getInventory().getArmorContents());
                }
            }.runTaskLater(plugin, 1);
        }
        else if (ItemStackGenerator.isCustomItem(hand, 7)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    playerWearingTravelerArmor(p, p.getInventory().getArmorContents());
                }
            }.runTaskLater(plugin, 1);
        }
        else if (ItemStackGenerator.isCustomItem(hand, 19)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    playerWearingGillArmor(p, p.getInventory().getArmorContents());
                }
            }.runTaskLater(plugin, 1);
        }
        else if (ItemStackGenerator.isCustomItem(hand, 8)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    playerWearingAdventurerArmor(p, p.getInventory().getArmorContents());
                }
            }.runTaskLater(plugin, 1);
        }
        else if (ItemStackGenerator.isCustomItem(hand, 29)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    playerWearingBeaconArmor(p, p.getInventory().getArmorContents());
                }
            }.runTaskLater(plugin, 1);
        }
    }

    @EventHandler
    public void inventoryClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        Inventory clickedInventory = e.getClickedInventory();

        if (clickedInventory == null) return;

        // Check if the player puts a beacon armor piece in their armor slot
        if (isPlayerInventory(clickedInventory) && e.getSlotType().equals(InventoryType.SlotType.ARMOR)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    playerWearingJumpingBoots(p, p.getInventory().getArmorContents());
                    playerWearingWandererArmor(p, p.getInventory().getArmorContents());
                    playerWearingTravelerArmor(p, p.getInventory().getArmorContents());
                    playerWearingGillArmor(p, p.getInventory().getArmorContents());
                    playerWearingAdventurerArmor(p, p.getInventory().getArmorContents());
                    playerWearingBeaconArmor(p, p.getInventory().getArmorContents());
                }
            }.runTaskLater(plugin, 1);
            return;
        }

        // Check if armor is shift clicked onto the player
        if (e.isShiftClick()) {
            ItemStack currentItem = e.getCurrentItem();
            if (currentItem == null) return;

            if (ItemStackGenerator.isCustomItem(currentItem, 4)) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        playerWearingJumpingBoots(p, p.getInventory().getArmorContents());
                    }
                }.runTaskLater(plugin, 1);
            }
            else if (ItemStackGenerator.isCustomItem(currentItem, 5)) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        playerWearingWandererArmor(p, p.getInventory().getArmorContents());
                    }
                }.runTaskLater(plugin, 1);
            }
            else if (ItemStackGenerator.isCustomItem(currentItem, 7)) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        playerWearingTravelerArmor(p, p.getInventory().getArmorContents());
                    }
                }.runTaskLater(plugin, 1);
            }
            else if (ItemStackGenerator.isCustomItem(currentItem, 19)) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        playerWearingGillArmor(p, p.getInventory().getArmorContents());
                    }
                }.runTaskLater(plugin, 1);
            }
            else if (ItemStackGenerator.isCustomItem(currentItem, 8)) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        playerWearingAdventurerArmor(p, p.getInventory().getArmorContents());
                    }
                }.runTaskLater(plugin, 1);
            }
            else if (ItemStackGenerator.isCustomItem(currentItem, 29)) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        playerWearingBeaconArmor(p, p.getInventory().getArmorContents());
                    }
                }.runTaskLater(plugin, 1);
            }
        }
    }

    public boolean isArmor(Material mat) {
        return Objects.nonNull(mat)
                && mat.name().endsWith("_HELMET")
                || mat.name().endsWith("_CHESTPLATE")
                || mat.name().endsWith("_LEGGINGS")
                || mat.name().endsWith("_BOOTS");
    }

    public void playerWearingJumpingBoots(Player p, ItemStack[] armor) {
        boolean found = false;
        for (ItemStack item : armor) {
            if (!ItemStackGenerator.isCustomItem(item, 4)) continue;
            found = true;
        }
        if (!found) {
            playersWearingJumpingBoots.remove(p.getUniqueId());
            return;
        }

        if (playersWearingJumpingBoots.contains(p.getUniqueId())) return;

        Reward reward = SurvivalSkills.getInstance().getSkillManager().getPlayerRewards(p).getReward("Exploring", "JumpingBoots");
        if (!reward.isApplied()) return;
        playersWearingJumpingBoots.add(p.getUniqueId());
        new JumpingBoots(p).runTaskTimer(SurvivalSkills.getInstance(), 0, 20);
    }

    public void playerWearingWandererArmor(Player p, ItemStack[] armor) {
        for (ItemStack item : armor) {
            if (ItemStackGenerator.isCustomItem(item, 5)) continue;
            playersWearingWandererArmor.remove(p.getUniqueId());
            return;
        }
        if (playersWearingWandererArmor.contains(p.getUniqueId())) return;

        Reward reward = SurvivalSkills.getInstance().getSkillManager().getPlayerRewards(p).getReward("Exploring", "WandererArmor");
        if (!reward.isApplied()) return;
        playersWearingWandererArmor.add(p.getUniqueId());
        new WandererArmor(p).runTaskTimer(SurvivalSkills.getInstance(), 0, 20);
    }

    public void playerWearingTravelerArmor(Player p, ItemStack[] armor) {
        for (ItemStack item : armor) {
            if (ItemStackGenerator.isCustomItem(item, 7)) continue;
            playersWearingTravelerArmor.remove(p.getUniqueId());
            return;
        }
        if (playersWearingTravelerArmor.contains(p.getUniqueId())) return;

        Reward reward = SurvivalSkills.getInstance().getSkillManager().getPlayerRewards(p).getReward("Exploring", "TravelerArmor");
        if (!reward.isApplied()) return;
        playersWearingTravelerArmor.add(p.getUniqueId());
        new TravelerArmor(p).runTaskTimer(SurvivalSkills.getInstance(), 0, 20);
    }

    public void playerWearingGillArmor(Player p, ItemStack[] armor) {
        for (ItemStack item : armor) {
            if (ItemStackGenerator.isCustomItem(item, 19)) continue;
            playersWearingGillArmor.remove(p.getUniqueId());
            return;
        }
        if (playersWearingGillArmor.contains(p.getUniqueId())) return;

        Reward reward = SurvivalSkills.getInstance().getSkillManager().getPlayerRewards(p).getReward("Exploring", "GillArmor");
        if (!reward.isApplied()) return;
        playersWearingGillArmor.add(p.getUniqueId());
        new GillArmor(p).runTaskTimer(SurvivalSkills.getInstance(), 0, 20);
    }

    public void playerWearingAdventurerArmor(Player p, ItemStack[] armor) {
        for (ItemStack item : armor) {
            if (ItemStackGenerator.isCustomItem(item, 8)) continue;
            playersWearingAdventurerArmor.remove(p.getUniqueId());
            return;
        }
        if (playersWearingAdventurerArmor.contains(p.getUniqueId())) return;

        Reward reward = SurvivalSkills.getInstance().getSkillManager().getPlayerRewards(p).getReward("Exploring", "AdventurerArmor");
        if (!reward.isApplied()) return;
        playersWearingAdventurerArmor.add(p.getUniqueId());
        new AdventurerArmor(p).runTaskTimer(SurvivalSkills.getInstance(), 0, 20);
    }

    public void playerWearingBeaconArmor(Player p, ItemStack[] armor) {
        for (ItemStack item : armor) {
            if (ItemStackGenerator.isCustomItem(item, 29)) continue;
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

    public void removeArmors(UUID uuid) {
        playersWearingJumpingBoots.remove(uuid);
        playersWearingWandererArmor.remove(uuid);
        playersWearingTravelerArmor.remove(uuid);
        playersWearingGillArmor.remove(uuid);
        playersWearingAdventurerArmor.remove(uuid);
        playersWearingBeaconArmor.remove(uuid);
    }

    public void checkHealthRegen(Player p) {
        PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
        if (rewards == null) return;
        if (!rewards.getReward("Exploring", "HealthRegen").isApplied()) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                giveRegenPotionEffect(p);
            }
        }.runTaskTimer(plugin, 0, 20);
    }

    public static void giveJumpPotionEffect(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 100, 2, false, false, true));
    }

    public static void giveSpeedPotionEffect(Player p, int level) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 100, level, false, false, true));
    }

    public static void giveRegenPotionEffect(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, false, false, true));
    }

    public static void giveWaterBreathingPotionEffect(Player p) {
        p.addPotionEffect(new PotionEffect(PotionEffectType.WATER_BREATHING, 100, 0, false, false, true));
    }
}
