package sir_draco.survivalskills.SkillListeners;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
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
import sir_draco.survivalskills.ItemStackGenerator;
import sir_draco.survivalskills.LeaderboardPlayer;
import sir_draco.survivalskills.Rewards.PlayerRewards;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.Trophy.Trophy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
        plugin.endPlayerTimers(p);
        if (plugin.getBuildingListener().getFlyingPlayers().containsKey(p))
            plugin.getBuildingListener().getFlyingPlayers().get(p).removeFlight(p);
        plugin.updateExploringStats(p.getUniqueId());
        plugin.savePlayerData(p);
        plugin.savePermaTrash(p);
        plugin.playerQuit(p);
    }

    @EventHandler
    public void blockPlaceEvent(BlockPlaceEvent e) {
        for (Map.Entry<Location, Trophy> trophy : plugin.getTrophies().entrySet()) {
            if (trophy.getKey().getBlockX() != e.getBlock().getX()) continue;
            if (trophy.getKey().getBlockZ() != e.getBlock().getZ()) continue;
            if (trophy.getKey().getBlockY() > e.getBlock().getY()) continue;
            e.setCancelled(true);
            e.getPlayer().sendRawMessage(ChatColor.RED + "There is a " + trophy.getValue().getType() + " trophy below you");
            return;
        }
    }

    @EventHandler
    public void playerPlaceTrophy(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (e.getHand() == null) return;
        if (!e.getHand().equals(EquipmentSlot.HAND)) return;
        if (hand.getItemMeta() == null) return;
        if (!hand.getItemMeta().hasCustomModelData()) return;
        if (hand.getEnchantments().containsKey(Enchantment.KNOCKBACK) && hand.getItemMeta().getCustomModelData() == 999) e.setCancelled(true);
        else return;


        // Make sure the trophy can be placed
        Block clicked = e.getClickedBlock();
        if (clicked == null) {
            p.sendRawMessage(ChatColor.RED + "You can't place a trophy there");
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            return;
        }
        else if (!e.getBlockFace().equals(BlockFace.UP)) {
            p.sendRawMessage(ChatColor.RED + "You can't place a trophy there");
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            return;
        }
        Block above = clicked.getLocation().add(0, 1, 0).getBlock();
        if (!above.getType().isAir() || !clicked.getLocation().add(0, 2, 0).getBlock().getType().isAir()) {
            p.sendRawMessage(ChatColor.RED + "You can't place a trophy there");
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            return;
        }

        // Check if it is in a claim
        if (plugin.isGriefPreventionEnabled() && plugin.checkForClaim(p, clicked.getLocation())) {
            e.setCancelled(true);
            return;
        }

        // God Trophy Outside
        if (hand.getType().equals(Material.GRASS_BLOCK)) {
            if (!blockHasSkyAccess(above)) {
                p.sendRawMessage(ChatColor.RED + "God trophies need sky access");
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                return;
            }
        }

        p.getInventory().setItemInMainHand(new ItemStack(Material.AIR));

        String playerName = p.getName();
        Trophy trophy;
        if (hand.getType().equals(Material.DIAMOND_PICKAXE)) {
            above.setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
            trophy = new Trophy(above.getLocation(), p.getUniqueId(), "CaveTrophy", plugin.generateID(), playerName);
            trophy.spawnTrophy(plugin);
            plugin.getTrophies().put(above.getLocation(), trophy);
        }
        else if (hand.getType().equals(Material.OAK_SAPLING)) {
            above.setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
            trophy = new Trophy(above.getLocation(), p.getUniqueId(), "ForestTrophy", plugin.generateID(), playerName);
            trophy.spawnTrophy(plugin);
            plugin.getTrophies().put(above.getLocation(), trophy);
        }
        else if (hand.getType().equals(Material.GOLDEN_CARROT)) {
            above.setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
            trophy = new Trophy(above.getLocation(), p.getUniqueId(), "FarmingTrophy", plugin.generateID(), playerName);
            trophy.spawnTrophy(plugin);
            plugin.getTrophies().put(above.getLocation(), trophy);
        }
        else if (hand.getType().equals(Material.TRIDENT)) {
            above.setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
            trophy = new Trophy(above.getLocation(), p.getUniqueId(), "OceanTrophy", plugin.generateID(), playerName);
            trophy.spawnTrophy(plugin);
            plugin.getTrophies().put(above.getLocation(), trophy);
        }
        else if (hand.getType().equals(Material.FISHING_ROD)) {
            above.setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
            trophy = new Trophy(above.getLocation(), p.getUniqueId(), "FishingTrophy", plugin.generateID(), playerName);
            trophy.spawnTrophy(plugin);
            plugin.getTrophies().put(above.getLocation(), trophy);
        }
        else if (hand.getType().equals(Material.SHEARS)) {
            above.setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
            trophy = new Trophy(above.getLocation(), p.getUniqueId(), "ColorTrophy", plugin.generateID(), playerName);
            trophy.spawnTrophy(plugin);
            plugin.getTrophies().put(above.getLocation(), trophy);
        }
        else if (hand.getType().equals(Material.NETHERRACK)) {
            above.setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
            trophy = new Trophy(above.getLocation(), p.getUniqueId(), "NetherTrophy", plugin.generateID(), playerName);
            trophy.spawnTrophy(plugin);
            plugin.getTrophies().put(above.getLocation(), trophy);
        }
        else if (hand.getType().equals(Material.END_STONE)) {
            above.setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
            trophy = new Trophy(above.getLocation(), p.getUniqueId(), "EndTrophy", plugin.generateID(), playerName);
            trophy.spawnTrophy(plugin);
            plugin.getTrophies().put(above.getLocation(), trophy);
        }
        else if (hand.getType().equals(Material.DIAMOND_SWORD)) {
            above.setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
            trophy = new Trophy(above.getLocation(), p.getUniqueId(), "ChampionTrophy", plugin.generateID(), playerName);
            trophy.spawnTrophy(plugin);
            plugin.getTrophies().put(above.getLocation(), trophy);
        }
        else if (hand.getType().equals(Material.GRASS_BLOCK)) {
            above.setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
            trophy = new Trophy(above.getLocation(), p.getUniqueId(), "GodTrophy", plugin.generateID(), playerName);
            trophy.spawnTrophy(plugin);
            plugin.getTrophies().put(above.getLocation(), trophy);
        }
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
    public void creeperBoom(BlockExplodeEvent e) {
        Location loc = e.getBlock().getLocation();
        if (!plugin.getTrophies().containsKey(loc.clone().add(0, 1, 0))) return;
        if (!plugin.getTrophies().containsKey(loc)) return;
        e.setCancelled(true);
        e.getBlock().setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
    }

    @EventHandler
    public void trophyExplode(EntityExplodeEvent e) {
        if (e.blockList().isEmpty()) return;
        for (Block block : e.blockList()) {
            if (!plugin.getTrophies().containsKey(block.getLocation().clone().add(0, 1, 0))) continue;
            if (!plugin.getTrophies().containsKey(block.getLocation())) continue;
            e.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void playerBreakTrophy(BlockBreakEvent e) {
        Location loc = e.getBlock().getLocation();
        if (holdingMiningTrophy(e.getPlayer())) {
            e.getPlayer().sendRawMessage(ChatColor.RED + "You can not use the mining trophy as a tool");
            e.getPlayer().playSound(e.getPlayer(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            e.setCancelled(true);
            return;
        }
        if (plugin.getTrophies().containsKey(loc.clone().add(0, 1, 0))) {
            e.getPlayer().sendRawMessage(ChatColor.RED + "There is a trophy on this block");
            e.getPlayer().playSound(e.getPlayer(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            e.setCancelled(true);
            return;
        }
        if (!plugin.getTrophies().containsKey(loc)) return;
        e.setCancelled(true);
        Trophy trophy = plugin.getTrophies().get(loc);
        if (!trophy.canBreakTrophy(e.getPlayer().getUniqueId()) && !e.getPlayer().isOp()) return;
        int type = trophy.getTrophyType();
        trophy.breakTrophy(plugin.getTrophyItem(type));
        plugin.removeTrophy(loc);
    }

    @EventHandler
    public void playerDamageEvent(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Firework) {
            Firework fw = (Firework) e.getDamager();
            if (fw.hasMetadata("nodamage")) e.setCancelled(true);
        }
    }

    @EventHandler
    public void crystalDamage(EntityDamageEvent e) {
        if (!e.getEntity().getType().equals(EntityType.END_CRYSTAL)) return;
        if (e.getEntity().hasMetadata("trophy")) e.setCancelled(true);
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
        PlayerRewards rewards = plugin.getPlayerRewards(p);
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
                if (rewards.getReward("Exploring", "TravellerArmor").isApplied()) return;
                e.setCancelled(true);
                p.sendRawMessage(ChatColor.RED + "You need to be exploring level " + ChatColor.AQUA
                        + rewards.getReward("Exploring", "TravellerArmor").getLevel() + ChatColor.RED + " to craft this");
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
            case 999:
                if (result.getType().equals(Material.WHITE_WOOL)) return;
                if (result.getType().equals(Material.BLACK_WOOL)) return;
                HashMap<String, Boolean> trophies = plugin.getTrophyTracker().get(p.getUniqueId());
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
                plugin.getTrophyTracker().put(p.getUniqueId(), trophies);
                p.sendRawMessage(ChatColor.GREEN + "Your level cap has been changed to: " + ChatColor.AQUA
                        + (plugin.getTrophyCount(p.getUniqueId()) * 10 + 10));
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
                    PlayerRewards rewards = plugin.getPlayerRewards(p);
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
    public void entityPickUpTrophy(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player) return;
        if (!e.getItem().getItemStack().containsEnchantment(Enchantment.KNOCKBACK)) return;
        if (e.getItem().getItemStack().getEnchantmentLevel(Enchantment.KNOCKBACK) != 5) return;
        e.setCancelled(true);
    }

    public HashMap<String, Boolean> enterTrophy(HashMap<String, Boolean> trophies, String trophyName) {
        if (trophies == null) {
            trophies = new HashMap<>();
            trophies.put(trophyName, true);
        }
        else trophies.put(trophyName, true);
        return trophies;
    }

    public boolean holdingMiningTrophy(Player p) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!hand.getType().equals(Material.DIAMOND_PICKAXE)) return false;
        return hand.containsEnchantment(Enchantment.KNOCKBACK);
    }

    public boolean blockHasSkyAccess(Block block) {
        if (block.getLocation().getBlockY() == 257) return true;
        Block above = block.getRelative(BlockFace.UP);
        if (above.isEmpty() || above.getType() == Material.AIR) return blockHasSkyAccess(above);
        return false;
    }

    public HashMap<Player, ArrayList<Inventory>> getCustomInventories() {
        return customInventories;
    }

    public HashMap<Player, Location> getDeathLocations() {
        return deathLocations;
    }
}
