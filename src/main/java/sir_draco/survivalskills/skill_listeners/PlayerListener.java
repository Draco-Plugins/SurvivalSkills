package sir_draco.survivalskills.skill_listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.abilities.AbilityTimer;
import sir_draco.survivalskills.skills.Skill;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.skills.SkillManager;
import sir_draco.survivalskills.utils.FileUtils;
import sir_draco.survivalskills.utils.items.ItemModelData;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;
import sir_draco.survivalskills.boards.Leaderboard;
import sir_draco.survivalskills.boards.LeaderboardPlayer;
import sir_draco.survivalskills.boards.SkillScoreboard;
import sir_draco.survivalskills.rewards.PlayerRewards;
import sir_draco.survivalskills.rewards.RewardNotifications;
import sir_draco.survivalskills.trophy.TrophyType;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

public class PlayerListener implements Listener {

    private final SurvivalSkills plugin;
    private final Map<Player, List<Inventory>> customInventories = new HashMap<>();
    private final HashMap<Player, Inventory> openInventory = new HashMap<>();
    private final HashMap<Player, Location> deathLocations = new HashMap<>();

    private record CraftRestriction(SkillCategory category, String rewardName) {}

    private static final Map<Integer, CraftRestriction> CRAFT_RESTRICTIONS = Map.ofEntries(
            Map.entry(ItemModelData.UNLIMITED_TORCH.getId(), new CraftRestriction(SkillCategory.MINING, "UnlimitedTorch")),
            Map.entry(ItemModelData.MINING_ARMOR.getId(), new CraftRestriction(SkillCategory.MINING, "MiningArmor")),
            Map.entry(ItemModelData.JUMPING_BOOTS.getId(), new CraftRestriction(SkillCategory.EXPLORING, "JumpingBoots")),
            Map.entry(ItemModelData.WANDERER_ARMOR.getId(), new CraftRestriction(SkillCategory.EXPLORING, "WandererArmor")),
            Map.entry(ItemModelData.CAVE_FINDER.getId(), new CraftRestriction(SkillCategory.EXPLORING, "CaveFinder")),
            Map.entry(ItemModelData.TRAVELER_ARMOR.getId(), new CraftRestriction(SkillCategory.EXPLORING, "TravelerArmor")),
            Map.entry(ItemModelData.ADVENTURER_ARMOR.getId(), new CraftRestriction(SkillCategory.EXPLORING, "AdventurerArmor")),
            Map.entry(ItemModelData.WATERING_CAN.getId(), new CraftRestriction(SkillCategory.FARMING, "WateringCan")),
            Map.entry(ItemModelData.UNLIMITED_BONE_MEAL.getId(), new CraftRestriction(SkillCategory.FARMING, "UnlimitedBoneMeal")),
            Map.entry(ItemModelData.HARVESTER.getId(), new CraftRestriction(SkillCategory.FARMING, "Harvester")),
            Map.entry(ItemModelData.GIANT_SUMMON.getId(), new CraftRestriction(SkillCategory.FIGHTING, "GiantSummon")),
            Map.entry(ItemModelData.BROOD_MOTHER_SUMMON.getId(), new CraftRestriction(SkillCategory.FIGHTING, "BroodMotherSummon")),
            Map.entry(ItemModelData.THE_EXILED_ONE_SUMMON.getId(), new CraftRestriction(SkillCategory.FIGHTING, "TheExiledOneSummon")),
            Map.entry(ItemModelData.SORT_WAND.getId(), new CraftRestriction(SkillCategory.BUILDING, "AutoSortWand")),
            Map.entry(ItemModelData.FIREWORK_CANNON.getId(), new CraftRestriction(SkillCategory.MAIN, "FireworkCannon")),
            Map.entry(ItemModelData.GILL_ARMOR.getId(), new CraftRestriction(SkillCategory.EXPLORING, "GillArmor")),
            Map.entry(ItemModelData.ZAP_WAND.getId(), new CraftRestriction(SkillCategory.MINING, "ZapWand")),
            Map.entry(ItemModelData.MAGNET.getId(), new CraftRestriction(SkillCategory.EXPLORING, "Magnet"))
    );

    public PlayerListener(SurvivalSkills plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        plugin.playerJoin(p);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        plugin.getExploringListener().flushPlayerSteps(p);
        plugin.getSkillManager().clearMaxLevelMessages(p.getUniqueId());
        FileUtils.savePlayerData(p);
        FileUtils.savePermaTrash(p, plugin.getPermaTrashData(), plugin.getPermaTrashFile());
        plugin.playerQuit(p);
    }

    @EventHandler
    public void bucket(PlayerBucketEmptyEvent e) {
        Player p = e.getPlayer();
        ItemStack hand = ItemStackGeneratorUtils.getItemInHand(p, e.getHand());
        if (hand == null) return;
        ItemMeta meta = hand.getItemMeta();
        if (meta == null) return;
        if (ItemStackGeneratorUtils.hasCustomModelData(meta, ItemModelData.UNLIMITED_TROPICAL_FISH_BUCKET.getId())) {
            e.setCancelled(true);

            // Spawn a tropical fish
            Location loc = e.getBlock().getLocation().clone().add(0.5, 1, 0.5);
            World world = loc.getWorld();
            if (world == null) return;
            world.spawnEntity(loc, EntityType.TROPICAL_FISH);
        }
    }

    @EventHandler
    public void playerDamageEvent(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Firework fw)) return;
        if (!fw.hasMetadata("nodamage")) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void playerCraftEvent(CraftItemEvent e) {
        if (e.getClickedInventory() == null) return;
        ItemStack result = e.getRecipe().getResult();
        if (!ItemStackGeneratorUtils.isCustomItem(result)) return;
        ItemMeta meta = result.getItemMeta();
        if (meta == null) return;

        Player p = (Player) e.getWhoClicked();
        if (!meta.hasCustomModelDataComponent()) return;
        int modelData = extractModelData(meta);

        PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
        if (rewards == null) {
            e.setCancelled(true);
            p.sendRawMessage(ChatColor.RED + "Your player information is missing. Please rejoin the server.");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            Bukkit.getLogger().log(Level.WARNING, "Player rewards are not loaded for " + p.getName());
            return;
        }

        CraftRestriction restriction = CRAFT_RESTRICTIONS.get(modelData);
        if (restriction != null) {
            handleCraftRestriction(e, p, rewards, restriction);
            return;
        }

        if (modelData == ItemModelData.TROPHY.getId()) {
            handleTrophyCraft(e, p, rewards, result);
        }
    }

    @EventHandler
    public void inventoryClickEvent(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;
        if (e.getCurrentItem() == null) return;
        Player p = (Player) e.getWhoClicked();
        List<Inventory> playerInvs = customInventories.get(p);
        if (playerInvs == null) return;

        e.setCancelled(true);
        ItemStack arrow = e.getCurrentItem();
        navigateCustomInventory(p, arrow, e.getClickedInventory(), playerInvs);
    }

    @EventHandler
    public void inventoryDragEvent(InventoryDragEvent e) {
        Player p = (Player) e.getWhoClicked();
        List<Inventory> playerInvs = customInventories.get(p);
        if (playerInvs == null) return;

        e.setCancelled(true);
        ItemStack arrow = e.getOldCursor();
        navigateCustomInventory(p, arrow, e.getInventory(), playerInvs);
    }

    @EventHandler
    public void inventoryCloseEvent(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        if (openInventory.containsKey(p) && !openInventory.get(p).equals(e.getInventory())) return;
        customInventories.remove(p);
        openInventory.remove(p);
    }

    @EventHandler
    public void deathEvent(PlayerDeathEvent e) {
        Player p = e.getEntity();
        LeaderboardPlayer leaderboardPlayer = plugin.getLeaderboardTracker().get(p.getUniqueId());
        if (leaderboardPlayer == null) {
            Bukkit.getLogger().log(Level.WARNING, "Leaderboard tracker is missing for %s. Adding trac", p.getName());
            leaderboardPlayer = Leaderboard.initializeLeaderboardForPlayer(p);
        }

        int deaths = leaderboardPlayer.getScore(SkillCategory.DEATHS) + 1;
        leaderboardPlayer.setScore(SkillCategory.DEATHS, deaths);
        SkillScoreboard.updateScoreboardDeaths(e.getEntity(), deaths);

        // Death return skill
        if (deaths >= 75) {
            deathLocations.put(e.getEntity(), e.getEntity().getLocation());
        }
    }

    @EventHandler
    public void respawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        int deaths = plugin.getLeaderboardTracker().get(p.getUniqueId()).getScore(SkillCategory.DEATHS);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (deaths >= 10)
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 1200, 2));
                if (deaths >= 20)
                    p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 1200, 2));
                if (deaths >= 30)
                    p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 1200, 2));
                if (deaths >= 40)
                    p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false,
                            false, true));
                if (deaths >= 50) {
                    PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
                    if (!rewards.isAddedDeathResistance()) {
                        rewards.setProtectionPercentage(rewards.getProtectionPercentage() + 0.1);
                        rewards.setAddedDeathResistance(true);
                    }
                }
                if (deaths >= 75) {
                    p.sendRawMessage(ChatColor.GOLD + "Use " + ChatColor.AQUA + "/deathreturn" + ChatColor.GOLD
                            + " to return to your death location");
                }
            }
        }.runTaskLater(plugin, 1);
    }

    @EventHandler
    public void useXPVoucher(PlayerInteractEvent e) {
        if (e.getHand() == null) return;
        if (e.getHand().equals(EquipmentSlot.OFF_HAND)) return;
        if (e.getAction().equals(Action.RIGHT_CLICK_BLOCK) || e.getAction().equals(Action.RIGHT_CLICK_AIR)) return;
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!ItemStackGeneratorUtils.isCustomItem(hand, ItemModelData.XP_VOUCHER.getId())) return;

        AbilityTimer timer = plugin.getAbilityManager().getAbility(p, "XPVoucher");
        if (timer != null) {
            p.sendRawMessage(ChatColor.RED + "You already have an XP Voucher active");
            p.sendRawMessage(ChatColor.RED + "You have: " + ChatColor.AQUA
                    + RewardNotifications.cooldown(timer.getActiveTimeLeft()) + ChatColor.RED + " seconds left");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }

        timer = new AbilityTimer(plugin, "XPVoucher", p, 3600, 0);
        timer.runTaskTimerAsynchronously(plugin, 0, 20);
        plugin.getAbilityManager().addAbility(p, timer);
        plugin.getSkillManager().setPlayerMultiplier(p, 2.0);
        p.sendRawMessage(ChatColor.GREEN + "You have activated an XP Voucher");
        p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);

        hand.setAmount(hand.getAmount() - 1);
        p.getInventory().setItemInMainHand(hand);
    }

    @EventHandler
    public void useUnlimitedRocket(PlayerInteractEvent e) {
        Player p = e.getPlayer();

        ItemStack item = e.getItem();
        if (!ItemStackGeneratorUtils.isCustomItem(item, ItemModelData.UNLIMITED_ROCKET.getId())) return;
        e.setCancelled(true);
        // Apply the speed boost to the player
        p.setVelocity(p.getLocation().getDirection().multiply(1.5));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
    }

    private void navigateCustomInventory(Player p, ItemStack arrow, Inventory clickedInventory,
                                          List<Inventory> playerInvs) {
        ItemMeta meta = arrow.getItemMeta();
        if (!arrow.getType().equals(Material.ARROW)) return;
        if (meta == null) return;
        if (meta.getDisplayName().equalsIgnoreCase("arrow")) return;

        int currentInv = 0;
        for (int i = 0; i < playerInvs.size(); i++) {
            if (!playerInvs.get(i).equals(clickedInventory)) continue;
            currentInv = i;
            break;
        }
        if (ItemStackGeneratorUtils.hasCustomModelData(meta)) {
            if (currentInv + 1 >= playerInvs.size()) currentInv = -1;
            Inventory inv = playerInvs.get(currentInv + 1);
            openInventory.put(p, inv);
            p.openInventory(inv);
        } else {
            if (currentInv - 1 < 0)
                currentInv = playerInvs.size();
            Inventory inv = playerInvs.get(currentInv - 1);
            openInventory.put(p, inv);
            p.openInventory(inv);
        }
    }

    private void handleCraftRestriction(CraftItemEvent e, Player p, PlayerRewards rewards, CraftRestriction restriction) {
        if (rewards.getReward(restriction.category(), restriction.rewardName()).isApplied())
            return;
        e.setCancelled(true);
        p.sendRawMessage(ChatColor.RED + "You need to be " + restriction.category().getDisplayName().toLowerCase()
                + " level " + ChatColor.AQUA
                + rewards.getReward(restriction.category(), restriction.rewardName()).getLevel()
                + ChatColor.RED + " to craft this");
        p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    }

    private void handleTrophyCraft(CraftItemEvent e, Player p, PlayerRewards rewards, ItemStack result) {
        if (result.getType().equals(Material.WHITE_WOOL))
            return;
        if (result.getType().equals(Material.BLACK_WOOL))
            return;

        Map<TrophyType, Boolean> trophies = plugin.getTrophyManager().getTrophyTracker().get(p.getUniqueId());
        Optional<TrophyType> matchedTrophy = TrophyType.fromMaterial(result.getType());
        if (matchedTrophy.isEmpty()) return;

        TrophyType trophyType = matchedTrophy.get();
        if (trophyType == TrophyType.GOD) {
            if (SkillManager.getSkillLevel(p.getUniqueId(), SkillCategory.MAIN) != Skill.MAX_LEVEL) {
                e.setCancelled(true);
                p.sendRawMessage(ChatColor.RED + "You need to be main level " + ChatColor.AQUA + Skill.MAX_LEVEL
                        + ChatColor.RED + " to craft this");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return;
            }
        } else {
            trophies = enterTrophy(trophies, trophyType);
            Bukkit.broadcastMessage(
                    ChatColor.GOLD + p.getName() + " has crafted the " + ChatColor.AQUA + trophyType.getName());
        }

        plugin.getTrophyManager().getTrophyTracker().put(p.getUniqueId(), trophies);
        p.sendRawMessage(ChatColor.GREEN + "Your level cap has been changed to: " + ChatColor.AQUA
                + (plugin.getTrophyManager().playerMaxSkillLevel(p.getUniqueId())));
        p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
    }

    private static int extractModelData(ItemMeta meta) {
        CustomModelDataComponent customModelData = meta.getCustomModelDataComponent();
        return customModelData.getFloats().stream()
                .filter(modelDataValue -> modelDataValue == Math.round(modelDataValue))
                .map((Float modelDataValue) -> Math.round(modelDataValue))
                .findFirst()
                .orElse(0);
    }

    public Map<TrophyType, Boolean> enterTrophy(Map<TrophyType, Boolean> trophies, TrophyType trophyType) {
        if (trophies == null) {
            trophies = new HashMap<>();
        }
        trophies.put(trophyType, true);
        return trophies;
    }

    public Map<Player, List<Inventory>> getCustomInventories() {
        return customInventories;
    }

    public HashMap<Player, Location> getDeathLocations() {
        return deathLocations;
    }
}
