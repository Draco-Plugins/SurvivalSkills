package sir_draco.survivalskills.Trophy;

import net.citizensnpcs.api.event.NPCClickEvent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.Trophy.GodQuestline.GodRecipeUI;
import sir_draco.survivalskills.Trophy.GodQuestline.GodTrophyQuest;

import java.util.HashMap;
import java.util.Map;

public class TrophyListener implements Listener {

    private final SurvivalSkills plugin;

    public TrophyListener(SurvivalSkills plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void blockPlaceEvent(BlockPlaceEvent e) {
        for (Map.Entry<Location, Trophy> trophy : plugin.getTrophyManager().getTrophies().entrySet()) {
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

        if (hand.getItemMeta().getCustomModelData() == 999) e.setCancelled(true);
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
            trophy = new Trophy(above.getLocation(), p.getUniqueId(), "CaveTrophy", plugin.getTrophyManager().generateTrophyID(), playerName);
            trophy.spawnTrophy(plugin);
            plugin.getTrophyManager().getTrophies().put(above.getLocation(), trophy);
        }
        else if (hand.getType().equals(Material.OAK_SAPLING)) {
            above.setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
            trophy = new Trophy(above.getLocation(), p.getUniqueId(), "ForestTrophy", plugin.getTrophyManager().generateTrophyID(), playerName);
            trophy.spawnTrophy(plugin);
            plugin.getTrophyManager().getTrophies().put(above.getLocation(), trophy);
        }
        else if (hand.getType().equals(Material.GOLDEN_CARROT)) {
            above.setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
            trophy = new Trophy(above.getLocation(), p.getUniqueId(), "FarmingTrophy", plugin.getTrophyManager().generateTrophyID(), playerName);
            trophy.spawnTrophy(plugin);
            plugin.getTrophyManager().getTrophies().put(above.getLocation(), trophy);
        }
        else if (hand.getType().equals(Material.TRIDENT)) {
            above.setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
            trophy = new Trophy(above.getLocation(), p.getUniqueId(), "OceanTrophy", plugin.getTrophyManager().generateTrophyID(), playerName);
            trophy.spawnTrophy(plugin);
            plugin.getTrophyManager().getTrophies().put(above.getLocation(), trophy);
        }
        else if (hand.getType().equals(Material.FISHING_ROD)) {
            above.setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
            trophy = new Trophy(above.getLocation(), p.getUniqueId(), "FishingTrophy", plugin.getTrophyManager().generateTrophyID(), playerName);
            trophy.spawnTrophy(plugin);
            plugin.getTrophyManager().getTrophies().put(above.getLocation(), trophy);
        }
        else if (hand.getType().equals(Material.SHEARS)) {
            above.setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
            trophy = new Trophy(above.getLocation(), p.getUniqueId(), "ColorTrophy", plugin.getTrophyManager().generateTrophyID(), playerName);
            trophy.spawnTrophy(plugin);
            plugin.getTrophyManager().getTrophies().put(above.getLocation(), trophy);
        }
        else if (hand.getType().equals(Material.NETHERRACK)) {
            above.setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
            trophy = new Trophy(above.getLocation(), p.getUniqueId(), "NetherTrophy", plugin.getTrophyManager().generateTrophyID(), playerName);
            trophy.spawnTrophy(plugin);
            plugin.getTrophyManager().getTrophies().put(above.getLocation(), trophy);
        }
        else if (hand.getType().equals(Material.END_STONE)) {
            above.setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
            trophy = new Trophy(above.getLocation(), p.getUniqueId(), "EndTrophy", plugin.getTrophyManager().generateTrophyID(), playerName);
            trophy.spawnTrophy(plugin);
            plugin.getTrophyManager().getTrophies().put(above.getLocation(), trophy);
        }
        else if (hand.getType().equals(Material.DIAMOND_SWORD)) {
            above.setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
            trophy = new Trophy(above.getLocation(), p.getUniqueId(), "ChampionTrophy", plugin.getTrophyManager().generateTrophyID(), playerName);
            trophy.spawnTrophy(plugin);
            plugin.getTrophyManager().getTrophies().put(above.getLocation(), trophy);
        }
        else if (hand.getType().equals(Material.GRASS_BLOCK)) {
            above.setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);

            if (plugin.getTrophyManager().getGodNPCIDs().containsKey(p)) {
                trophy = new Trophy(above.getLocation(), p.getUniqueId(), "GodTrophy", plugin.getTrophyManager().generateTrophyID(), playerName);
                trophy.spawnTrophy(plugin, plugin.getTrophyManager().getGodNPCIDs().get(p));
                plugin.getTrophyManager().getTrophies().put(above.getLocation(), trophy);
            }
            else {
                trophy = new Trophy(above.getLocation(), p.getUniqueId(), "GodTrophy", plugin.getTrophyManager().generateTrophyID(), playerName);
                trophy.spawnTrophy(plugin);
                plugin.getTrophyManager().getTrophies().put(above.getLocation(), trophy);
            }
        }
    }

    @EventHandler
    public void trophyBlockExplode(BlockExplodeEvent e) {
        Location loc = e.getBlock().getLocation();
        if (!plugin.getTrophyManager().getTrophies().containsKey(loc.clone().add(0, 1, 0))) return;
        if (!plugin.getTrophyManager().getTrophies().containsKey(loc)) return;
        e.setCancelled(true);
        e.getBlock().setType(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
    }

    @EventHandler
    public void trophyExplode(EntityExplodeEvent e) {
        if (e.blockList().isEmpty()) return;
        for (Block block : e.blockList()) {
            if (!plugin.getTrophyManager().getTrophies().containsKey(block.getLocation().clone().add(0, 1, 0))) continue;
            if (!plugin.getTrophyManager().getTrophies().containsKey(block.getLocation())) continue;
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

        if (plugin.getTrophyManager().getTrophies().containsKey(loc.clone().add(0, 1, 0))) {
            e.getPlayer().sendRawMessage(ChatColor.RED + "There is a trophy on this block");
            e.getPlayer().playSound(e.getPlayer(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            e.setCancelled(true);
            return;
        }

        if (!plugin.getTrophyManager().getTrophies().containsKey(loc)) return;
        e.setCancelled(true);
        Trophy trophy = plugin.getTrophyManager().getTrophies().get(loc);
        if (!trophy.canBreakTrophy(e.getPlayer().getUniqueId()) && !e.getPlayer().isOp()) return;
        int type = trophy.getTrophyType();
        trophy.breakTrophy(plugin.getTrophyManager().getTrophyItem(type));
        plugin.getTrophyManager().removeTrophy(loc);
    }

    @EventHandler
    public void crystalDamage(EntityDamageEvent e) {
        if (!e.getEntity().getType().equals(EntityType.END_CRYSTAL)) return;
        if (e.getEntity().hasMetadata("trophy")) e.setCancelled(true);
    }

    @EventHandler
    public void entityPickUpTrophy(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player) return;
        if (!e.getItem().getItemStack().containsEnchantment(Enchantment.KNOCKBACK)) return;
        if (e.getItem().getItemStack().getEnchantmentLevel(Enchantment.KNOCKBACK) != 5) return;
        e.setCancelled(true);
    }

    @EventHandler
    public void clickGodNPC(NPCClickEvent e) {
        Player p = e.getClicker();
        if (!plugin.getTrophyManager().getGodNPCIDs().containsKey(p)) {
            p.sendRawMessage(TrophyManager.npcName + ChatColor.WHITE + ": Are you expecting something?");
            p.playSound(p, Sound.ENTITY_VILLAGER_YES, 1, 1);
            return;
        }


        if (plugin.getTrophyManager().getGodNPCIDs().get(p) != e.getNPC().getId()) {
            p.sendRawMessage(TrophyManager.npcName + ChatColor.WHITE + ": You have your own god to talk to!");
            p.playSound(p, Sound.ENTITY_VILLAGER_YES, 1, 1);
            return;
        }

        // Check if the god quest is enabled
        if (!plugin.getTrophyManager().isGodQuestEnabled()) {
            p.sendRawMessage(TrophyManager.npcName + ChatColor.WHITE + ": The God Quest is not enabled on this server");
            p.playSound(p, Sound.ENTITY_VILLAGER_YES, 1, 1);
            return;
        }

        GodTrophyQuest quest;
        if (plugin.getTrophyManager().getPlayerGodQuestData().containsKey(p.getUniqueId()))
            quest = plugin.getTrophyManager().getPlayerGodQuestData().get(p.getUniqueId());
        else {
            quest = new GodTrophyQuest(p.getUniqueId());
            plugin.getTrophyManager().getPlayerGodQuestData().put(p.getUniqueId(), quest);
        }

        quest.handleNPCInteract(p);
    }

    @EventHandler
    public void villagerTradeEvent(InventoryClickEvent e) {
        // Check if the player has an active God Quest
        Player p = (Player) e.getWhoClicked();
        if (!plugin.getTrophyManager().getPlayerGodQuestData().containsKey(p.getUniqueId())) return;

        // Get the god quest
        GodTrophyQuest quest = plugin.getTrophyManager().getPlayerGodQuestData().get(p.getUniqueId());

        // Check that the inventory is a villager trade inventory
        if (e.getClickedInventory() == null) return;
        if (!e.getClickedInventory().getType().equals(InventoryType.MERCHANT)) return;

        // If it is a shift click, check how many trades took place
        int trades = 1;
        if (e.isShiftClick()) {
            MerchantInventory inv = (MerchantInventory) e.getClickedInventory();
            trades = getTradeCount(inv);
        }

        // Update the god quest progress
        quest.setCurrentItemCount(quest.getCurrentItemCount() + trades);
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

    public int getTradeCount(MerchantInventory inv) {
        // Get the recipe
        MerchantRecipe recipe = inv.getSelectedRecipe();
        if (recipe == null) return 0;
        if (recipe.getDemand() <= 0) return 0;

        // Get the items involved in the trade
        ItemStack[] ingredients = recipe.getIngredients().toArray(new ItemStack[0]);

        // Calculate the maximum number of trades based on the villager's inventory
        int maxTrades = Integer.MAX_VALUE;
        for (ItemStack ingredient : ingredients) {
            if (ingredient == null) continue;
            int villagerItemCount = getVillagerIngredientItemCount(inv, ingredient);
            int possibleTrades = (int) Math.floor((double) villagerItemCount / ingredient.getAmount());
            if (possibleTrades < maxTrades) maxTrades = possibleTrades;
        }

        return maxTrades;
    }

    private int getVillagerIngredientItemCount(MerchantInventory inv, ItemStack item) {
        int count = 0;
        for (ItemStack invItem : inv.getContents())
            if (invItem != null && invItem.isSimilar(item))
                return invItem.getAmount();
        return count;
    }
}
