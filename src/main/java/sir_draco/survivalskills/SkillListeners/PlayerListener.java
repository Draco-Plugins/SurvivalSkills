package sir_draco.survivalskills.SkillListeners;

import org.bukkit.*;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.Abilities.AbilityTimer;
import sir_draco.survivalskills.ItemStackGenerator;
import sir_draco.survivalskills.Boards.LeaderboardPlayer;
import sir_draco.survivalskills.Rewards.PlayerRewards;
import sir_draco.survivalskills.Rewards.RewardNotifications;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;
import java.util.HashMap;

public class PlayerListener implements Listener {

    private final SurvivalSkills plugin;
    private final HashMap<Player, ArrayList<Inventory>> customInventories = new HashMap<>();
    private final HashMap<Player, Inventory> openInventory = new HashMap<>();
    private final HashMap<Player, Location> deathLocations = new HashMap<>();

    public PlayerListener(SurvivalSkills plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.playerJoin(p, false);
            }
        }.runTaskLater(plugin, 60);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        plugin.getSkillManager().updateExploringStats(p.getUniqueId());
        plugin.savePlayerData(p);
        plugin.savePermaTrash(p);
        plugin.playerQuit(p);
    }

    @EventHandler
    public void bucket(PlayerBucketEmptyEvent e) {
        Player p = e.getPlayer();
        if (!e.getHand().equals(EquipmentSlot.HAND)) return;
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getItemMeta() == null) return;
        if (!hand.getItemMeta().hasCustomModelData()) return;
        if (hand.getItemMeta().getCustomModelData() == 17) {
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
        if (e.getDamager() instanceof Firework) {
            Firework fw = (Firework) e.getDamager();
            if (fw.hasMetadata("nodamage")) e.setCancelled(true);
        }
    }

    @EventHandler
    public void playerCraftEvent(CraftItemEvent e) {
        if (e.getClickedInventory() == null) return;
        ItemStack result = e.getRecipe().getResult();
        if (!ItemStackGenerator.isCustomItem(result)) return;
        ItemMeta meta = result.getItemMeta();
        if (meta == null) return;

        Player p = (Player) e.getWhoClicked();
        int modelData = meta.getCustomModelData();
        PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
        switch (modelData) {
            case 1:
                if (rewards.getReward("Mining", "UnlimitedTorch").isApplied()) return;
                e.setCancelled(true);
                p.sendRawMessage(ChatColor.RED + "You need to be mining level " + ChatColor.AQUA
                        + rewards.getReward("Mining", "UnlimitedTorch").getLevel() + ChatColor.RED + " to craft this");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                break;
            case 3:
                if (rewards.getReward("Mining", "MiningArmor").isApplied()) return;
                e.setCancelled(true);
                p.sendRawMessage(ChatColor.RED + "You need to be mining level " + ChatColor.AQUA
                        + rewards.getReward("Mining", "MiningArmor").getLevel() + ChatColor.RED + " to craft this");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                break;
            case 4:
                if (rewards.getReward("Exploring", "JumpingBoots").isApplied()) return;
                e.setCancelled(true);
                p.sendRawMessage(ChatColor.RED + "You need to be exploring level " + ChatColor.AQUA
                        + rewards.getReward("Exploring", "JumpingBoots").getLevel() + ChatColor.RED + " to craft this");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                break;
            case 5:
                if (rewards.getReward("Exploring", "WandererArmor").isApplied()) return;
                e.setCancelled(true);
                p.sendRawMessage(ChatColor.RED + "You need to be exploring level " + ChatColor.AQUA
                        + rewards.getReward("Exploring", "WandererArmor").getLevel() + ChatColor.RED + " to craft this");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                break;
            case 6:
                if (rewards.getReward("Exploring", "CaveFinder").isApplied()) return;
                e.setCancelled(true);
                p.sendRawMessage(ChatColor.RED + "You need to be exploring level " + ChatColor.AQUA
                        + rewards.getReward("Exploring", "CaveFinder").getLevel() + ChatColor.RED + " to craft this");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                break;
            case 7:
                if (rewards.getReward("Exploring", "TravelerArmor").isApplied()) return;
                e.setCancelled(true);
                p.sendRawMessage(ChatColor.RED + "You need to be exploring level " + ChatColor.AQUA
                        + rewards.getReward("Exploring", "TravelerArmor").getLevel() + ChatColor.RED + " to craft this");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                break;
            case 8:
                if (rewards.getReward("Exploring", "AdventurerArmor").isApplied()) return;
                e.setCancelled(true);
                p.sendRawMessage(ChatColor.RED + "You need to be exploring level " + ChatColor.AQUA
                        + rewards.getReward("Exploring", "AdventurerArmor").getLevel() + ChatColor.RED + " to craft this");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                break;
            case 9:
                if (rewards.getReward("Farming", "WateringCan").isApplied()) return;
                e.setCancelled(true);
                p.sendRawMessage(ChatColor.RED + "You need to be exploring level " + ChatColor.AQUA
                        + rewards.getReward("Exploring", "WateringCan").getLevel() + ChatColor.RED + " to craft this");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                break;
            case 10:
                if (rewards.getReward("Farming", "UnlimitedBoneMeal").isApplied()) return;
                e.setCancelled(true);
                p.sendRawMessage(ChatColor.RED + "You need to be exploring level " + ChatColor.AQUA
                        + rewards.getReward("Exploring", "UnlimitedBoneMeal").getLevel() + ChatColor.RED + " to craft this");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                break;
            case 11:
                if (rewards.getReward("Farming", "Harvester").isApplied()) return;
                e.setCancelled(true);
                p.sendRawMessage(ChatColor.RED + "You need to be exploring level " + ChatColor.AQUA
                        + rewards.getReward("Exploring", "Harvester").getLevel() + ChatColor.RED + " to craft this");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                break;
            case 12:
                if (rewards.getReward("Fighting", "GiantSummon").isApplied()) return;
                e.setCancelled(true);
                p.sendRawMessage(ChatColor.RED + "You need to be exploring level " + ChatColor.AQUA
                        + rewards.getReward("Exploring", "GiantSummon").getLevel() + ChatColor.RED + " to craft this");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                break;
            case 13:
                if (rewards.getReward("Fighting", "BroodMotherSummon").isApplied()) return;
                e.setCancelled(true);
                p.sendRawMessage(ChatColor.RED + "You need to be exploring level " + ChatColor.AQUA
                        + rewards.getReward("Exploring", "BroodMotherSummon").getLevel() + ChatColor.RED + " to craft this");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                break;
            case 14:
                if (rewards.getReward("Fighting", "TheExiledOneSummon").isApplied()) return;
                e.setCancelled(true);
                p.sendRawMessage(ChatColor.RED + "You need to be exploring level " + ChatColor.AQUA
                        + rewards.getReward("Exploring", "TheExiledOneSummon").getLevel() + ChatColor.RED + " to craft this");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                break;
            case 16:
                if (rewards.getReward("Building", "AutoSortWand").isApplied()) return;
                e.setCancelled(true);
                p.sendRawMessage(ChatColor.RED + "You need to be building level " + ChatColor.AQUA
                        + rewards.getReward("Building", "AutoSortWand").getLevel() + ChatColor.RED + " to craft this");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            case 18:
                if (rewards.getReward("Main", "FireworkCannon").isApplied()) return;
                e.setCancelled(true);
                p.sendRawMessage(ChatColor.RED + "You need to be main level " + ChatColor.AQUA
                        + rewards.getReward("Main", "FireworkCannon").getLevel() + ChatColor.RED + " to craft this");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            case 19:
                if (rewards.getReward("Exploring", "GillArmor").isApplied()) return;
                e.setCancelled(true);
                p.sendRawMessage(ChatColor.RED + "You need to be exploring level " + ChatColor.AQUA
                        + rewards.getReward("Exploring", "GillArmor").getLevel() + ChatColor.RED + " to craft this");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            case 27:
                if (rewards.getReward("Mining", "ZapWand").isApplied()) return;
                e.setCancelled(true);
                p.sendRawMessage(ChatColor.RED + "You need to be mining level " + ChatColor.AQUA
                        + rewards.getReward("Mining", "ZapWand").getLevel() + ChatColor.RED + " to craft this");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            case 999:
                if (result.getType().equals(Material.WHITE_WOOL)) return;
                if (result.getType().equals(Material.BLACK_WOOL)) return;
                HashMap<String, Boolean> trophies = plugin.getTrophyManager().getTrophyTracker().get(p.getUniqueId());
                switch (result.getType()) {
                    case DIAMOND_PICKAXE:
                        trophies = enterTrophy(trophies, "CaveTrophy");
                        Bukkit.broadcastMessage(ChatColor.GOLD + p.getName() + " has crafted the " + ChatColor.AQUA + "Cave Trophy");
                        break;
                    case OAK_SAPLING:
                        trophies = enterTrophy(trophies, "ForestTrophy");
                        Bukkit.broadcastMessage(ChatColor.GOLD + p.getName() + " has crafted the " + ChatColor.AQUA + "Forest Trophy");
                        break;
                    case SHEARS:
                        trophies = enterTrophy(trophies, "ColorTrophy");
                        Bukkit.broadcastMessage(ChatColor.GOLD + p.getName() + " has crafted the " + ChatColor.AQUA + "Color Trophy");
                        break;
                    case GOLDEN_CARROT:
                        trophies = enterTrophy(trophies, "FarmingTrophy");
                        Bukkit.broadcastMessage(ChatColor.GOLD + p.getName() + " has crafted the " + ChatColor.AQUA + "Farming Trophy");
                        break;
                    case TRIDENT:
                        trophies = enterTrophy(trophies, "OceanTrophy");
                        Bukkit.broadcastMessage(ChatColor.GOLD + p.getName() + " has crafted the " + ChatColor.AQUA + "Ocean Trophy");
                        break;
                    case FISHING_ROD:
                        trophies = enterTrophy(trophies, "FishingTrophy");
                        Bukkit.broadcastMessage(ChatColor.GOLD + p.getName() + " has crafted the " + ChatColor.AQUA + "Fishing Trophy");
                        break;
                    case NETHERRACK:
                        trophies = enterTrophy(trophies, "NetherTrophy");
                        Bukkit.broadcastMessage(ChatColor.GOLD + p.getName() + " has crafted the " + ChatColor.AQUA + "Nether Trophy");
                        break;
                    case END_STONE:
                        trophies = enterTrophy(trophies, "EndTrophy");
                        Bukkit.broadcastMessage(ChatColor.GOLD + p.getName() + " has crafted the " + ChatColor.AQUA + "End Trophy");
                        break;
                    case DIAMOND_SWORD:
                        trophies = enterTrophy(trophies, "ChampionTrophy");
                        Bukkit.broadcastMessage(ChatColor.GOLD + p.getName() + " has crafted the " + ChatColor.AQUA + "Champion Trophy");
                        break;
                }
                plugin.getTrophyManager().getTrophyTracker().put(p.getUniqueId(), trophies);
                p.sendRawMessage(ChatColor.GREEN + "Your level cap has been changed to: " + ChatColor.AQUA
                        + (plugin.getTrophyManager().playerMaxSkillLevel(p.getUniqueId())));
                p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
                break;
        }
    }

    @EventHandler
    public void inventoryClickEvent(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;
        if (e.getCurrentItem() == null) return;
        Player p = (Player) e.getWhoClicked();
        if (!customInventories.containsKey(p)) return;

        e.setCancelled(true);
        ItemStack arrow = e.getCurrentItem();
        ItemMeta meta = arrow.getItemMeta();
        if (!arrow.getType().equals(Material.ARROW)) return;

        int currentInv = 0;
        for (int i = 0; i < customInventories.get(p).size(); i++) {
            if (!customInventories.get(p).get(i).equals(e.getClickedInventory())) continue;
            currentInv = i;
            break;
        }
        if (meta != null && meta.hasCustomModelData()) {
            if (currentInv + 1 >= customInventories.get(p).size()) currentInv = -1;
            Inventory inv = customInventories.get(p).get(currentInv + 1);
            openInventory.put(p, inv);
            p.openInventory(inv);
        }
        else {
            if (currentInv - 1 < 0) currentInv = customInventories.get(p).size();
            Inventory inv = customInventories.get(p).get(currentInv - 1);
            openInventory.put(p, inv);
            p.openInventory(inv);
        }
    }

    @EventHandler
    public void inventoryDragEvent(InventoryDragEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (!customInventories.containsKey(p)) return;

        e.setCancelled(true);
        ItemStack arrow = e.getOldCursor();
        ItemMeta meta = arrow.getItemMeta();
        if (!arrow.getType().equals(Material.ARROW)) return;

        int currentInv = 0;
        for (int i = 0; i < customInventories.get(p).size(); i++) {
            if (!customInventories.get(p).get(i).equals(e.getInventory())) continue;
            currentInv = i;
            break;
        }
        if (meta != null && meta.hasCustomModelData()) {
            if (currentInv + 1 >= customInventories.get(p).size()) currentInv = -1;
            Inventory inv = customInventories.get(p).get(currentInv + 1);
            openInventory.put(p, inv);
            p.openInventory(inv);
        }
        else {
            if (currentInv - 1 < 0) currentInv = customInventories.get(p).size();
            Inventory inv = customInventories.get(p).get(currentInv - 1);
            openInventory.put(p, inv);
            p.openInventory(inv);
        }
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
        LeaderboardPlayer player = plugin.getLeaderboardTracker().get(e.getEntity().getUniqueId());
        if (player == null) return;
        int deaths = player.getDeathScore() + 1;
        player.setDeathScore(deaths);

        if (deaths >= 75) deathLocations.put(e.getEntity(), e.getEntity().getLocation());
    }

    @EventHandler
    public void respawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        int deaths = plugin.getLeaderboardTracker().get(p.getUniqueId()).getDeathScore();

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
                    p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, Integer.MAX_VALUE, 0, false, false, true));
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
        if (!ItemStackGenerator.isCustomItem(hand, 26)) return;

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
        plugin.getSkillManager().getSkillMultipliers().put(p, 2.0);
        p.sendRawMessage(ChatColor.GREEN + "You have activated an XP Voucher");
        p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);

        hand.setAmount(hand.getAmount() - 1);
        p.getInventory().setItemInMainHand(hand);
    }

    @EventHandler
    public void useUnlimitedRocket(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!p.isGliding()) return;

        ItemStack item = e.getItem();
        if (!ItemStackGenerator.isCustomItem(item, 31)) return;
        e.setCancelled(true);
        // Apply the speed boost to the player
        p.setVelocity(p.getLocation().getDirection().multiply(1.5));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
    }

    public HashMap<String, Boolean> enterTrophy(HashMap<String, Boolean> trophies, String trophyName) {
        if (trophies == null) {
            trophies = new HashMap<>();
            trophies.put(trophyName, true);
        }
        else trophies.put(trophyName, true);
        return trophies;
    }

    public HashMap<Player, ArrayList<Inventory>> getCustomInventories() {
        return customInventories;
    }

    public HashMap<Player, Location> getDeathLocations() {
        return deathLocations;
    }
}
