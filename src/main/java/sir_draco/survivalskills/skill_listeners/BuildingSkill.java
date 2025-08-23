package sir_draco.survivalskills.skill_listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import sir_draco.survivalskills.abilities.AbilityTimer;
import sir_draco.survivalskills.rewards.Reward;
import sir_draco.survivalskills.skills.SkillManager;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.ItemStackGenerator;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class BuildingSkill implements Listener {

    private final SurvivalSkills plugin;
    private final HashSet<Material> bannedReturns = new HashSet<>();

    public BuildingSkill(SurvivalSkills plugin) {
        this.plugin = plugin;
        createBannedReturns();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();

        if (e.getBlock().getType().toString().contains("WALL_SIGN"))
            return;
        if (plugin.getFarmingList().contains(e.getBlock().getType()))
            return;
        if (e.getHand().equals(EquipmentSlot.OFF_HAND)
                && p.getInventory().getItemInMainHand().getType().toString().contains("SHOVEL"))
            return;
        if (e.getHand().equals(EquipmentSlot.OFF_HAND)
                && p.getInventory().getItemInMainHand().getType().toString().contains("HOE"))
            return;

        SkillManager.experienceEvent(plugin, p, plugin.getSkillManager().getBuildingXP(), "Building");

        // Handle block return
        if (isBannedReturn(e.getBlock().getType()))
            return;
        if (plugin.getSkillManager().getPlayerRewards(p).getBlockBlackChance() == 0.0)
            return;
        if (Math.random() < plugin.getSkillManager().getPlayerRewards(p).getBlockBlackChance()) {
            ItemStack item;
            Material material = e.getBlock().getType();
            if (material.equals(Material.BUBBLE_CORAL_WALL_FAN))
                item = new ItemStack(Material.BUBBLE_CORAL_FAN, 1);
            else if (material.isItem())
                item = new ItemStack(material, 1);
            else
                return;

            // Give the player the item
            p.getInventory().addItem(item);
        }
    }

    @EventHandler
    public void useSortWand(PlayerInteractEvent e) {
        // Make sure they have all necessary requirements to use the sort wand
        Player p = e.getPlayer();
        if (isValidSortAttempt(e, p))
            return;
        trySortingPlayerInventory(e, p);

        if (!e.getAction().equals(Action.LEFT_CLICK_BLOCK))
            return;
        Block block = e.getClickedBlock();
        if (block == null)
            return;
        if (!block.getType().equals(Material.CHEST) &&
                !block.getType().equals(Material.TRAPPED_CHEST) &&
                !isShulkerBox(block.getType()))
            return;

        // Get the inventory of the chest or shulker box
        BlockState state = block.getState();
        Inventory containerInventory;

        if (state instanceof Chest chest) {
            if (chest.getInventory().getHolder() instanceof DoubleChest doubleChest) {
                containerInventory = doubleChest.getInventory();
            } else {
                containerInventory = chest.getInventory();
            }
        } else if (state.getBlock().getState() instanceof ShulkerBox shulkerBox) {
            containerInventory = shulkerBox.getInventory();
        } else {
            return;
        }

        // Make sure no one is viewing the inventory
        if (preventViewingSortedInventory(containerInventory, p))
            return;

        // Sort the container and add the sorted items to it
        sortChestInventory(containerInventory.getContents().clone(), containerInventory);
        String containerType = isShulkerBox(block.getType()) ? "Shulker box" : "Chest";
        p.sendRawMessage(ChatColor.GREEN + containerType + " sorted!");
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        AbilityTimer timer = plugin.getAbilityManager().getAbility(e.getPlayer(), "Flight");
        if (timer == null || !timer.isActive())
            return;
        Player p = e.getPlayer();
        p.setAllowFlight(true);
        p.setFlying(true);
        p.setFlySpeed(timer.getFlightSpeed());
    }

    private boolean preventViewingSortedInventory(Inventory containerInventory, Player p) {
        for (HumanEntity human : containerInventory.getViewers()) {
            if (!(human instanceof Player player) || player.equals(p))
                continue;
            p.closeInventory();
            return true;
        }
        return false;
    }

    private void trySortingPlayerInventory(PlayerInteractEvent e, Player p) {
        if (e.getAction().equals(Action.RIGHT_CLICK_AIR) && p.isSneaking()) {
            // Sort the player's inventory (excluding hotbar)
            ItemStack[] items = new ItemStack[27]; // Only main inventory slots (9-35)
            for (int i = 9; i <= 35; i++) {
                ItemStack item = p.getInventory().getItem(i);
                if (item == null)
                    continue;
                items[i - 9] = item; // Adjust index for the array
            }
            sortPlayerInventory(items.clone(), p.getInventory());
            p.sendRawMessage(ChatColor.GREEN + "Inventory sorted!");
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
        }
    }

    private boolean isValidSortAttempt(PlayerInteractEvent e, Player p) {
        if (!ItemStackGenerator.isCustomItem(p.getInventory().getItemInMainHand(), 16))
            return true;
        Reward reward = plugin.getSkillManager().getPlayerRewards(p).getReward("Building", "AutoSortWand");
        if (!reward.isEnabled())
            return true;
        if (!reward.isApplied() && !p.isOp()) {
            p.sendRawMessage(ChatColor.RED + "You have to be building level: " + ChatColor.AQUA + reward.getLevel()
                    + ChatColor.RED + " to use this ability.");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }
        if (e.getHand() == null)
            return true;
        return !e.getHand().equals(EquipmentSlot.HAND);
    }

    private boolean isShulkerBox(Material material) {
        return material.toString().contains("SHULKER_BOX");
    }

    public void sortChestInventory(ItemStack[] inventoryItems, Inventory chest) {
        List<ItemStack> items = getSortedItems(inventoryItems);
        List<ItemStack> enchantedBooks = getSortedEnchantedBooks(inventoryItems);

        chest.clear();
        if (!items.isEmpty())
            for (ItemStack item : items)
                if (item != null)
                    chest.addItem(item);
        if (!enchantedBooks.isEmpty())
            for (ItemStack item : enchantedBooks)
                if (item != null)
                    chest.addItem(item);
    }

    public void sortPlayerInventory(ItemStack[] inventoryItems, Inventory playerInventory) {
        List<ItemStack> items = getSortedItems(inventoryItems);
        List<ItemStack> enchantedBooks = getSortedEnchantedBooks(inventoryItems);

        // Only clear the main inventory slots (9-35), preserve hotbar (0-8)
        for (int i = 9; i <= 35; i++)
            playerInventory.setItem(i, null);

        // Add items back starting from slot 9
        int currentSlot = 9;
        if (!items.isEmpty()) {
            for (ItemStack item : items) {
                if (item != null && currentSlot <= 35) {
                    playerInventory.setItem(currentSlot, item);
                    currentSlot++;
                }
            }
        }
        if (!enchantedBooks.isEmpty()) {
            for (ItemStack item : enchantedBooks) {
                if (item != null && currentSlot <= 35) {
                    playerInventory.setItem(currentSlot, item);
                    currentSlot++;
                }
            }
        }
    }

    public List<ItemStack> getSortedItems(ItemStack[] itemsRaw) {
        // Filter out nulls & enchanted books first to avoid duplication (books handled
        // separately)
        List<ItemStack> filtered = Arrays.stream(itemsRaw)
                .filter(Objects::nonNull)
                .filter(i -> i.getType() != Material.ENCHANTED_BOOK)
                .collect(Collectors.toList());

        if (filtered.isEmpty())
            return Collections.emptyList();

        // Sort by (1) material name (2) display name if present (strip color for
        // consistency)
        filtered.sort(java.util.Comparator
                .comparing((ItemStack i) -> i.getType().toString())
                .thenComparing(i -> {
                    ItemMeta meta = i.getItemMeta();
                    if (meta != null && meta.hasDisplayName()) {
                        return ChatColor.stripColor(meta.getDisplayName());
                    }
                    return ""; // items without name come first for that material
                }));

        // Condense consecutive similar stacks (sorting groups identical items together)
        List<ItemStack> condensed = new ArrayList<>();
        for (ItemStack item : filtered) {
            condenseList((ArrayList<ItemStack>) condensed, item.clone()); // clone to avoid mutating original references
        }
        return condensed;
    }

    public List<ItemStack> condenseList(ArrayList<ItemStack> items, ItemStack itemToAdd) {
        if (itemToAdd == null)
            return items;
        if (items.isEmpty()) {
            items.add(itemToAdd);
            return items;
        }

        // Try to add the ItemStack to the last ItemStack in the list
        ItemStack lastItem = items.getLast();
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
        } else {
            int amountToAdd = 64 - lastItem.getAmount();
            lastItem.setAmount(64);
            itemToAdd.setAmount(itemToAdd.getAmount() - amountToAdd);
            items.add(itemToAdd);
        }

        return items;
    }

    public List<ItemStack> getSortedEnchantedBooks(ItemStack[] itemsRaw) {
        List<ItemStack> enchantedBooks = Arrays.stream(itemsRaw)
                .filter(Objects::nonNull)
                .filter(i -> i.getType() == Material.ENCHANTED_BOOK)
                .collect(Collectors.toList());

        if (enchantedBooks.isEmpty())
            return enchantedBooks;

        enchantedBooks.sort(java.util.Comparator
                .comparing(this::buildEnchantmentSortKey)
                .thenComparingInt(this::getTotalEnchantmentLevels));

        return enchantedBooks;
    }

    private String buildEnchantmentSortKey(ItemStack book) {
        if (book == null)
            return "";
        ItemMeta meta = book.getItemMeta();
        if (!(meta instanceof EnchantmentStorageMeta enchantmentMeta))
            return "";
        Map<Enchantment, Integer> enchants = enchantmentMeta.getStoredEnchants();
        if (enchants.isEmpty())
            return "";
        return enchants.entrySet().stream()
                .sorted(java.util.Comparator.comparing(e -> e.getKey().getKey().toString()))
                .map(e -> e.getKey().getKey() + ":" + e.getValue())
                .collect(Collectors.joining("|"));
    }

    private int getTotalEnchantmentLevels(ItemStack book) {
        if (book == null)
            return 0;
        ItemMeta meta = book.getItemMeta();
        if (!(meta instanceof EnchantmentStorageMeta enchantmentMeta))
            return 0;
        return enchantmentMeta.getStoredEnchants().values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean isBannedReturn(Material material) {
        return bannedReturns.contains(material);
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
        bannedReturns.add(Material.COPPER_ORE);
        bannedReturns.add(Material.COPPER_INGOT);
        bannedReturns.add(Material.COPPER_BLOCK);
        bannedReturns.add(Material.RAW_COPPER_BLOCK);
        bannedReturns.add(Material.CRAFTING_TABLE);
        bannedReturns.add(Material.CYAN_SHULKER_BOX);
        bannedReturns.add(Material.DAMAGED_ANVIL);
        bannedReturns.add(Material.DEEPSLATE_COPPER_ORE);
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
        bannedReturns.add(Material.STONECUTTER);
        bannedReturns.add(Material.TORCH);
        bannedReturns.add(Material.WALL_TORCH);
        bannedReturns.add(Material.WHITE_SHULKER_BOX);
        bannedReturns.add(Material.WITHER_SKELETON_SKULL);
        bannedReturns.add(Material.YELLOW_SHULKER_BOX);
        bannedReturns.add(Material.END_PORTAL_FRAME);
        bannedReturns.add(Material.FARMLAND);
    }
}
