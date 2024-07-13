package sir_draco.survivalskills.SkillListeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import sir_draco.survivalskills.Abilities.FlyingTimer;
import sir_draco.survivalskills.Rewards.Reward;
import sir_draco.survivalskills.Skill;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.*;

public class BuildingSkill implements Listener {

    private final SurvivalSkills plugin;
    private final HashMap<Player, FlyingTimer> flyingPlayers = new HashMap<>();
    private final HashSet<Material> bannedReturns = new HashSet<>();
    private final HashSet<String> brokenBlocks = new HashSet<>();

    private double xp; // XP per block placed

    public BuildingSkill(SurvivalSkills plugin, double xp) {
        this.plugin = plugin;
        this.xp = xp;
        createBannedReturns();
        createBrokenBlocks();
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();

        if (e.getBlock().getType().toString().contains("WALL_SIGN")) return;
        if (brokenBlocks.contains(e.getBlock().getType().toString())) return;
        if (plugin.getFarmingList().contains(e.getBlock().getType())) return;
        if (p.getInventory().getItemInMainHand().getType().toString().contains("SHOVEL")) return;

        Skill.experienceEvent(plugin, p, xp, "Building");

        // Handle block return
        if (isBannedReturn(e.getBlock().getType())) return;
        if (plugin.getPlayerRewards(p).getBlockBlackChance() == 0.0) return;
        if (Math.random() < plugin.getPlayerRewards(p).getBlockBlackChance()) {
            p.getInventory().addItem(new ItemStack(e.getBlock().getType(), 1));
        }
    }

    @EventHandler
    public void useSortWand(PlayerInteractEvent e) {
        // Make sure they have all necessary requirements to use the sort wand
        Player p = e.getPlayer();
        if (!isSortWand(p.getInventory().getItemInMainHand())) return;
        Reward reward = plugin.getPlayerRewards(p).getReward("Building", "AutoSortWand");
        if (!reward.isEnabled()) return;
        if (!reward.isApplied() && !p.isOp()) {
            p.sendRawMessage(ChatColor.RED + "You have to be building level: " + ChatColor.AQUA + reward.getLevel()
                    + ChatColor.RED + " to use this ability.");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }
        if (e.getHand() == null) return;
        if (!e.getHand().equals(EquipmentSlot.HAND)) return;
        if (!e.getAction().equals(Action.LEFT_CLICK_BLOCK)) return;
        Block block = e.getClickedBlock();
        if (block == null) return;
        if (!block.getType().equals(Material.CHEST) && !block.getType().equals(Material.TRAPPED_CHEST)) return;

        // Sort the chest
        // Get the inventory of the chest
        BlockState state = block.getState();
        if (!(state instanceof Chest)) return;
        Chest chest = (Chest) state;
        Inventory chestInventory;
        if (chest.getInventory().getHolder() instanceof DoubleChest) {
            DoubleChest doubleChest = (DoubleChest) chest.getInventory().getHolder();
            chestInventory = doubleChest.getInventory();
        } else {
            chestInventory = chest.getInventory();
        }

        // Make sure no one is viewing the inventory
        for (HumanEntity human : chestInventory.getViewers()) {
            if (!(human instanceof Player)) continue;
            Player player = (Player) human;
            if (player.equals(p)) continue;
            p.closeInventory();
            return;
        }

        // Sort the chest and add the sorted items to the chest
        ArrayList<ItemStack> items = getSortedItems(chestInventory.getContents().clone());
        ArrayList<ItemStack> enchantedBooks = getSortedEnchantedBooks(chestInventory.getContents().clone());

        chestInventory.clear();
        if (!items.isEmpty())
            for (ItemStack item : items) if (item != null) chestInventory.addItem(item);
        if (!enchantedBooks.isEmpty())
            for (ItemStack item : enchantedBooks) if (item != null) chestInventory.addItem(item);

        p.sendRawMessage(ChatColor.GREEN + "Chest sorted!");
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
    }

    public ArrayList<ItemStack> getSortedItems(ItemStack[] itemsRaw) {
        ArrayList<ItemStack> items = new ArrayList<>(Arrays.asList(itemsRaw));
        ArrayList<ItemStack> sortedItems = new ArrayList<>();
        int itemsSize = items.size();

        int addedItemsCounter = 0;
        while (addedItemsCounter < itemsSize) {
            ItemStack itemToAdd = null;
            String itemType = null;
            for (ItemStack item : items) {
                if (item == null) continue;
                if (item.getType().equals(Material.ENCHANTED_BOOK)) continue;
                if (itemToAdd == null) {
                    itemToAdd = item;
                    itemType = item.getType().toString();
                    continue;
                }

                // check if the item type is alphabetically before the current itemType
                if (item.getType().toString().compareTo(itemType) < 0) {
                    itemToAdd = item;
                    itemType = item.getType().toString();
                }
            }

            items.remove(itemToAdd);
            sortedItems = condenseList(sortedItems, itemToAdd);
            addedItemsCounter++;
        }

        return sortedItems;
    }

    public ArrayList<ItemStack> condenseList(ArrayList<ItemStack> items, ItemStack itemToAdd) {
        if (itemToAdd == null) return items;
        if (items.isEmpty()) {
            items.add(itemToAdd);
            return items;
        }

        // Try to add the ItemStack to the last ItemStack in the list
        ItemStack lastItem = items.get(items.size() - 1);
        if (lastItem == null) {
            items.add(itemToAdd);
            return items;
        }

        if (itemToAdd.getMaxStackSize() == 1) {
            items.add(itemToAdd);
            return items;
        }

        ItemMeta meta = itemToAdd.getItemMeta();
        if (meta != null && meta.hasCustomModelData()) {
            items.add(itemToAdd);
            return items;
        }

        if (!lastItem.isSimilar(itemToAdd) || lastItem.getAmount() == 64) {
            items.add(itemToAdd);
            return items;
        }

        if (lastItem.getAmount() + itemToAdd.getAmount() <= 64) {
            lastItem.setAmount(lastItem.getAmount() + itemToAdd.getAmount());
            return items;
        }
        else {
            int amountToAdd = 64 - lastItem.getAmount();
            lastItem.setAmount(64);
            itemToAdd.setAmount(itemToAdd.getAmount() - amountToAdd);
            items.add(itemToAdd);
        }

        return items;
    }

    public ArrayList<ItemStack> getSortedEnchantedBooks(ItemStack[] itemsRaw) {
        ArrayList<ItemStack> enchantedBooks = new ArrayList<>();
        for (ItemStack item : itemsRaw) {
            if (item == null) continue;
            if (!item.getType().equals(Material.ENCHANTED_BOOK)) continue;
            enchantedBooks.add(item);
        }

        if (enchantedBooks.isEmpty()) return enchantedBooks;

        // Sort the enchanted books by enchantment name
        enchantedBooks.sort((book1, book2) -> {
            String enchantmentName1 = getFirstEnchantmentName(book1);
            String enchantmentName2 = getFirstEnchantmentName(book2);
            if (enchantmentName1.equals(enchantmentName2))
                return Integer.compare(getEnchantLevel(book1), getEnchantLevel(book2));
            return enchantmentName1.compareTo(enchantmentName2);
        });

        return enchantedBooks;
    }

    private String getFirstEnchantmentName(ItemStack book) {
        if (book == null) return "";
        ItemMeta meta = book.getItemMeta();
        if (meta == null) return "";
        if (!(meta instanceof EnchantmentStorageMeta)) return "";
        EnchantmentStorageMeta enchantmentMeta = (EnchantmentStorageMeta) meta;
        Map<Enchantment, Integer> enchantments = enchantmentMeta.getStoredEnchants();
        if (enchantments.isEmpty()) return "";
        Enchantment enchant = enchantments.keySet().iterator().next();
        if (enchant == null) return "";
        return enchant.getKey().toString();
    }

    private int getEnchantLevel(ItemStack book) {
        if (book == null) return 0;
        ItemMeta meta = book.getItemMeta();
        if (meta == null) return 0;
        if (!(meta instanceof EnchantmentStorageMeta)) return 0;
        EnchantmentStorageMeta enchantmentMeta = (EnchantmentStorageMeta) meta;
        Map<Enchantment, Integer> enchantments = enchantmentMeta.getStoredEnchants();
        if (enchantments.isEmpty()) return 0;
        Enchantment enchant = enchantments.keySet().iterator().next();
        if (enchant == null) return 0;
        return enchantments.get(enchant);
    }

    public HashMap<Player, FlyingTimer> getFlyingPlayers() {
        return flyingPlayers;
    }

    public boolean isBannedReturn(Material material) {
        return bannedReturns.contains(material);
    }

    public boolean isSortWand(ItemStack item) {
        if (!item.getType().equals(Material.BLAZE_ROD)) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        if (!meta.hasCustomModelData()) return false;
        return meta.getCustomModelData() == 16;
    }

    public void createBannedReturns() {
        bannedReturns.add(Material.ANCIENT_DEBRIS);
        bannedReturns.add(Material.ANVIL);
        bannedReturns.add(Material.ARMOR_STAND);
        bannedReturns.add(Material.BARREL);
        bannedReturns.add(Material.BEACON);
        bannedReturns.add(Material.BELL);
        bannedReturns.add(Material.BLACK_SHULKER_BOX);
        bannedReturns.add(Material.BLAST_FURNACE);
        bannedReturns.add(Material.BLUE_SHULKER_BOX);
        bannedReturns.add(Material.BREWING_STAND);
        bannedReturns.add(Material.BROWN_SHULKER_BOX);
        bannedReturns.add(Material.CAMPFIRE);
        bannedReturns.add(Material.CARTOGRAPHY_TABLE);
        bannedReturns.add(Material.CHIPPED_ANVIL);
        bannedReturns.add(Material.COAL_BLOCK);
        bannedReturns.add(Material.COAL_ORE);
        bannedReturns.add(Material.COMPOSTER);
        bannedReturns.add(Material.CRAFTING_TABLE);
        bannedReturns.add(Material.CYAN_SHULKER_BOX);
        bannedReturns.add(Material.DAMAGED_ANVIL);
        bannedReturns.add(Material.DEEPSLATE_COAL_ORE);
        bannedReturns.add(Material.DEEPSLATE_DIAMOND_ORE);
        bannedReturns.add(Material.DEEPSLATE_EMERALD_ORE);
        bannedReturns.add(Material.DEEPSLATE_GOLD_ORE);
        bannedReturns.add(Material.DEEPSLATE_IRON_ORE);
        bannedReturns.add(Material.DEEPSLATE_LAPIS_ORE);
        bannedReturns.add(Material.DEEPSLATE_REDSTONE_ORE);
        bannedReturns.add(Material.DIAMOND_BLOCK);
        bannedReturns.add(Material.DIAMOND_ORE);
        bannedReturns.add(Material.DRAGON_EGG);
        bannedReturns.add(Material.EMERALD_BLOCK);
        bannedReturns.add(Material.EMERALD_ORE);
        bannedReturns.add(Material.ENCHANTING_TABLE);
        bannedReturns.add(Material.ENDER_CHEST);
        bannedReturns.add(Material.END_CRYSTAL);
        bannedReturns.add(Material.FLETCHING_TABLE);
        bannedReturns.add(Material.FURNACE);
        bannedReturns.add(Material.GOLD_BLOCK);
        bannedReturns.add(Material.GOLD_ORE);
        bannedReturns.add(Material.GRAY_SHULKER_BOX);
        bannedReturns.add(Material.GREEN_SHULKER_BOX);
        bannedReturns.add(Material.GRINDSTONE);
        bannedReturns.add(Material.IRON_BLOCK);
        bannedReturns.add(Material.IRON_ORE);
        bannedReturns.add(Material.LAPIS_BLOCK);
        bannedReturns.add(Material.LAPIS_ORE);
        bannedReturns.add(Material.LIGHT_BLUE_SHULKER_BOX);
        bannedReturns.add(Material.LIGHT_GRAY_SHULKER_BOX);
        bannedReturns.add(Material.LIME_SHULKER_BOX);
        bannedReturns.add(Material.MAGENTA_SHULKER_BOX);
        bannedReturns.add(Material.MOVING_PISTON);
        bannedReturns.add(Material.NETHERITE_BLOCK);
        bannedReturns.add(Material.NETHER_GOLD_ORE);
        bannedReturns.add(Material.NETHER_QUARTZ_ORE);
        bannedReturns.add(Material.ORANGE_SHULKER_BOX);
        bannedReturns.add(Material.PINK_SHULKER_BOX);
        bannedReturns.add(Material.PISTON_HEAD);
        bannedReturns.add(Material.PURPLE_SHULKER_BOX);
        bannedReturns.add(Material.QUARTZ_BLOCK);
        bannedReturns.add(Material.REDSTONE_BLOCK);
        bannedReturns.add(Material.REDSTONE_ORE);
        bannedReturns.add(Material.RED_SHULKER_BOX);
        bannedReturns.add(Material.SHULKER_BOX);
        bannedReturns.add(Material.SLIME_BLOCK);
        bannedReturns.add(Material.SMITHING_TABLE);
        bannedReturns.add(Material.SMOKER);
        bannedReturns.add(Material.SOUL_CAMPFIRE);
        bannedReturns.add(Material.STICKY_PISTON);
        bannedReturns.add(Material.STONECUTTER);
        bannedReturns.add(Material.TORCH);
        bannedReturns.add(Material.WALL_TORCH);
        bannedReturns.add(Material.WHITE_SHULKER_BOX);
        bannedReturns.add(Material.WITHER_SKELETON_SKULL);
        bannedReturns.add(Material.YELLOW_SHULKER_BOX);
    }

    public void setXp(double xp) {
        this.xp = xp;
    }

    public void createBrokenBlocks() {
        brokenBlocks.add("CAVE_VINES");
        brokenBlocks.add("BAMBOO_SAPLING");
        brokenBlocks.add("REDSTONE_WIRE");
        brokenBlocks.add("SOUL_FIRE");
        brokenBlocks.add("PITCHER_CROP");
        brokenBlocks.add("SWEET_BERRY_BUSH");
        brokenBlocks.add("WEEPING_VINES");
        brokenBlocks.add("TORCHFLOWER_CROP");
        brokenBlocks.add("GLOW_LICHEN");
        brokenBlocks.add("CAVE_VINES_PLANT");
        brokenBlocks.add("GLOW_LICHEN_PLANT");
        brokenBlocks.add("GLOW_BERRIES");
        brokenBlocks.add("BUBBLE_CORAL_WALL_FAN");
    }
}
