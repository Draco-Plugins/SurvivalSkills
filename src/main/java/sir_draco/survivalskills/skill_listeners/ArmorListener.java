package sir_draco.survivalskills.skill_listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.abilities.armor.*;
import sir_draco.survivalskills.rewards.PlayerRewards;
import sir_draco.survivalskills.rewards.Reward;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.utils.items.ItemModelData;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public class ArmorListener implements Listener {

    private static final double DEFAULT_STEP_HEIGHT = 0.6;
    private static final double ADVENTURER_STEP_HEIGHT = 1.0;

    public enum ArmorType {
        JUMPING_BOOTS(ItemModelData.JUMPING_BOOTS.getId(), SkillCategory.EXPLORING, "JumpingBoots", JumpingBoots::new, 20, false),
        WANDERER(ItemModelData.WANDERER_ARMOR.getId(), SkillCategory.EXPLORING, "WandererArmor", WandererArmor::new, 20, true),
        TRAVELER(ItemModelData.TRAVELER_ARMOR.getId(), SkillCategory.EXPLORING, "TravelerArmor", TravelerArmor::new, 20, true),
        GILL(ItemModelData.GILL_ARMOR.getId(), SkillCategory.EXPLORING, "GillArmor", GillArmor::new, 20, true),
        ADVENTURER(ItemModelData.ADVENTURER_ARMOR.getId(), SkillCategory.EXPLORING, "AdventurerArmor", AdventurerArmor::new, 20, true),
        BEACON(ItemModelData.BEACON_ARMOR.getId(), SkillCategory.MINING, "BeaconArmor", BeaconArmor::new, 1, true),
        POWER(ItemModelData.POWER_ARMOR.getId(), SkillCategory.MINING, "PowerOre", null, 0, true);

        private final int modelData;
        private final SkillCategory category;
        private final String rewardName;
        private final Function<Player, ? extends BukkitRunnable> taskFactory;
        private final int tickInterval;
        private final boolean requiresFullSet;

        ArmorType(int modelData, SkillCategory category, String rewardName,
                  Function<Player, ? extends BukkitRunnable> taskFactory,
                  int tickInterval, boolean requiresFullSet) {
            this.modelData = modelData;
            this.category = category;
            this.rewardName = rewardName;
            this.taskFactory = taskFactory;
            this.tickInterval = tickInterval;
            this.requiresFullSet = requiresFullSet;
        }

        public int getModelData() { return modelData; }
        public SkillCategory getCategory() { return category; }
        public String getRewardName() { return rewardName; }
        public int getTickInterval() { return tickInterval; }
        public boolean requiresFullSet() { return requiresFullSet; }
    }

    private static final Map<ArmorType, Set<UUID>> armorPlayers;
    private static final Set<UUID> regenTaskPlayers = new HashSet<>();
    private final Map<UUID, Long> lastSneakTime = new HashMap<>();

    public static final List<PotionEffect> beaconEffects = List.of(
        new PotionEffect(PotionEffectType.REGENERATION, 80, 0),
        new PotionEffect(PotionEffectType.RESISTANCE, 80, 0),
        new PotionEffect(PotionEffectType.SPEED, 80, 0),
        new PotionEffect(PotionEffectType.STRENGTH, 80, 0),
        new PotionEffect(PotionEffectType.JUMP_BOOST, 80, 0),
        new PotionEffect(PotionEffectType.HASTE, 80, 0)
    );

    static {
        armorPlayers = new EnumMap<>(ArmorType.class);
        for (ArmorType type : ArmorType.values()) {
            armorPlayers.put(type, new HashSet<>());
        }
    }

    public ArmorListener() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            armorCheck(p);
        }
    }

    public static boolean isWearingArmor(UUID playerId, ArmorType type) {
        return armorPlayers.getOrDefault(type, Collections.emptySet()).contains(playerId);
    }

    public static void removeArmor(UUID playerId, ArmorType type) {
        Set<UUID> players = armorPlayers.get(type);
        if (players != null) {
            players.remove(playerId);
        }
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent e) {
        armorCheck(e.getPlayer());
    }

    @EventHandler
    public void playerLeave(PlayerQuitEvent e) {
        resetAdventurerStepHeight(e.getPlayer());
        removeArmors(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        resetAdventurerStepHeight(p);

        if (!e.getKeepInventory()) {
            removeArmors(p.getUniqueId());
            return;
        }

        Reward reward = SurvivalSkills.getInstance().getSkillManager().getPlayerRewards(p)
                .getReward(SkillCategory.MAIN, "KeepInventory");
        if (reward.isApplied())
            return;

        removeArmors(p.getUniqueId());
    }

    @EventHandler
    public void armorSwap(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK) && !e.getAction().equals(Action.RIGHT_CLICK_AIR))
            return;
        if (!isArmor(hand.getType()))
            return;

        for (ArmorType armorType : ArmorType.values()) {
            if (!ItemStackGeneratorUtils.isCustomItem(hand, armorType.getModelData()))
                continue;
            scheduleArmorCheck(p, armorType);
            break;
        }
    }

    @EventHandler
    public void inventoryClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        Inventory clickedInventory = e.getClickedInventory();

        if (clickedInventory == null)
            return;

        if (isPlayerInventory(clickedInventory) && e.getSlotType().equals(InventoryType.SlotType.ARMOR)) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    ItemStack[] armor = p.getInventory().getArmorContents();
                    for (ArmorType type : ArmorType.values()) {
                        playerWearingArmor(p, armor, type);
                    }
                }
            }.runTaskLater(SurvivalSkills.getInstance(), 1);
            return;
        }

        if (e.isShiftClick()) {
            ItemStack currentItem = e.getCurrentItem();
            if (currentItem == null)
                return;

            for (ArmorType armorType : ArmorType.values()) {
                if (!ItemStackGeneratorUtils.isCustomItem(currentItem, armorType.getModelData()))
                    continue;
                scheduleArmorCheck(p, armorType);
                break;
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p))
            return;
        if (!isWearingArmor(p.getUniqueId(), ArmorType.POWER))
            return;
        double incoming = e.getFinalDamage();
        double absorbed = PowerArmor.addStoredDamage(p, incoming);
        if (absorbed > 0) {
            if (absorbed >= incoming) {
                e.setCancelled(true);
            } else {
                double remaining = incoming - absorbed;
                e.setDamage(remaining * 2);
            }
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        if (isWearingArmor(p.getUniqueId(), ArmorType.ADVENTURER))
            updateAdventurerStepHeight(p, e.isSneaking());

        if (!e.isSneaking())
            return;
        if (!isWearingArmor(p.getUniqueId(), ArmorType.POWER))
            return;
        long now = System.currentTimeMillis();
        long last = lastSneakTime.getOrDefault(p.getUniqueId(), 0L);
        lastSneakTime.put(p.getUniqueId(), now);
        if (now - last < 1000) {
            if (PowerArmor.getStoredDamage(p) <= 0)
                return;
            PowerArmor.releaseDamage(p);
            p.sendMessage(ChatColor.GOLD + "Shockwave released!");
        }
    }

    private void armorCheck(Player p) {
        new BukkitRunnable() {
            @Override
            public void run() {
                checkHealthRegen(p);
                ItemStack[] armor = p.getInventory().getArmorContents();
                for (ArmorType type : ArmorType.values()) {
                    playerWearingArmor(p, armor, type);
                }
            }
        }.runTaskLater(SurvivalSkills.getInstance(), 20);
    }

    public static boolean isArmor(Material mat) {
        return mat != null && (mat.name().endsWith("_HELMET")
                || mat.name().endsWith("_CHESTPLATE")
                || mat.name().endsWith("_LEGGINGS")
                || mat.name().endsWith("_BOOTS"));
    }

    public void playerWearingArmor(Player p, ItemStack[] armor, ArmorType type) {
        UUID uuid = p.getUniqueId();
        Set<UUID> players = armorPlayers.get(type);

        if (type.requiresFullSet()) {
            boolean allMatch = true;
            for (ItemStack item : armor) {
                if (!ItemStackGeneratorUtils.isCustomItem(item, type.getModelData())) {
                    allMatch = false;
                    break;
                }
            }
            if (!allMatch) {
                players.remove(uuid);
                if (type == ArmorType.ADVENTURER)
                    resetAdventurerStepHeight(p);
                return;
            }
        } else {
            boolean found = false;
            for (ItemStack item : armor) {
                if (ItemStackGeneratorUtils.isCustomItem(item, type.getModelData())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                players.remove(uuid);
                if (type == ArmorType.ADVENTURER)
                    resetAdventurerStepHeight(p);
                return;
            }
        }

        if (players.contains(uuid))
            return;

        Reward reward = SurvivalSkills.getInstance().getSkillManager().getPlayerRewards(p)
                .getReward(type.getCategory(), type.getRewardName());
        if (!reward.isApplied())
            return;

        players.add(uuid);

        if (type == ArmorType.ADVENTURER)
            updateAdventurerStepHeight(p, p.isSneaking());

        if (type.taskFactory != null) {
            BukkitRunnable task = type.taskFactory.apply(p);
            if (task != null) {
                task.runTaskTimer(SurvivalSkills.getInstance(), 0, type.getTickInterval());
            }
        }
    }

    public void refreshPlayerArmor(Player player) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (ArmorType armorType : ArmorType.values()) {
            playerWearingArmor(player, armor, armorType);
        }
    }

    private static boolean isPlayerInventory(Inventory inventory) {
        return inventory.getType().equals(InventoryType.PLAYER);
    }

    private void scheduleArmorCheck(Player p, ArmorType type) {
        new BukkitRunnable() {
            @Override
            public void run() {
                playerWearingArmor(p, p.getInventory().getArmorContents(), type);
            }
        }.runTaskLater(SurvivalSkills.getInstance(), 1);
    }

    public void removeArmors(UUID uuid) {
        for (Set<UUID> players : armorPlayers.values()) {
            players.remove(uuid);
        }
        lastSneakTime.remove(uuid);
    }

    public static void updateAdventurerStepHeight(Player p, boolean sneaking) {
        setAdventurerStepHeight(p, sneaking ? DEFAULT_STEP_HEIGHT : ADVENTURER_STEP_HEIGHT);
    }

    public static void resetAdventurerStepHeight(Player p) {
        setAdventurerStepHeight(p, DEFAULT_STEP_HEIGHT);
    }

    private static void setAdventurerStepHeight(Player p, double stepHeight) {
        AttributeInstance attribute = p.getAttribute(Attribute.STEP_HEIGHT);
        if (attribute != null)
            attribute.setBaseValue(stepHeight);
    }

    public void checkHealthRegen(Player p) {
        if (regenTaskPlayers.contains(p.getUniqueId()))
            return;

        PlayerRewards rewards = SurvivalSkills.getInstance().getSkillManager().getPlayerRewards(p);
        if (rewards == null)
            return;
        if (!rewards.getReward(SkillCategory.EXPLORING, "HealthRegen").isApplied())
            return;

        regenTaskPlayers.add(p.getUniqueId());
        new BukkitRunnable() {
            @Override
            public void run() {
                giveRegenPotionEffect(p);
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), 0, 20);
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
