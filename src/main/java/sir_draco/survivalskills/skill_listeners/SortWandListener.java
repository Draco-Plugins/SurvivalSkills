package sir_draco.survivalskills.skill_listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.rewards.Reward;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;

import java.util.*;
import java.util.stream.Collectors;

public class SortWandListener implements Listener {

    private static final int SORT_WAND_MODEL_DATA = 16;
    private static final int FIRST_MAIN_SLOT = 9;
    private static final int LAST_MAIN_SLOT = 35;
    private static final int MAX_STACK_SIZE = 64;

    private final SurvivalSkills plugin;

    public SortWandListener(SurvivalSkills plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void useSortWand(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (isUnableToSort(e, p)) {
            return;
        }
        trySortingPlayerInventory(e, p);

        if (!e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            return;
        }
        Block block = e.getClickedBlock();
        if (block == null) {
            return;
        }
        if (!block.getType().equals(Material.CHEST)
                && !block.getType().equals(Material.TRAPPED_CHEST)
                && !isShulkerBox(block.getType())) {
            return;
        }

        BlockState state = block.getState();
        Inventory containerInventory = getContainerInventory(state);
        if (containerInventory == null) {
            return;
        }

        if (isInventoryInUseByAnotherPlayer(containerInventory, p)) {
            return;
        }

        sortChestInventory(containerInventory.getContents(), containerInventory);
        String containerType = isShulkerBox(block.getType()) ? "Shulker box" : "Chest";
        p.sendRawMessage(ChatColor.GREEN + containerType + " sorted!");
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
    }

    private Inventory getContainerInventory(BlockState state) {
        if (state instanceof Chest chest) {
            if (chest.getInventory().getHolder() instanceof DoubleChest doubleChest) {
                return doubleChest.getInventory();
            }
            return chest.getInventory();
        }
        if (state.getBlock().getState() instanceof ShulkerBox shulkerBox) {
            return shulkerBox.getInventory();
        }
        return null;
    }

    private boolean isInventoryInUseByAnotherPlayer(Inventory containerInventory, Player p) {
        for (HumanEntity human : containerInventory.getViewers()) {
            if (!(human instanceof Player player) || player.equals(p)) {
                continue;
            }
            p.closeInventory();
            return true;
        }
        return false;
    }

    private void trySortingPlayerInventory(PlayerInteractEvent e, Player p) {
        if (!e.getAction().equals(Action.RIGHT_CLICK_AIR) || !p.isSneaking()) {
            return;
        }
        // Only main inventory slots (9-35), preserving hotbar (0-8)
        ItemStack[] items = new ItemStack[27];
        for (int i = FIRST_MAIN_SLOT; i <= LAST_MAIN_SLOT; i++) {
            ItemStack item = p.getInventory().getItem(i);
            if (item == null) {
                continue;
            }
            items[i - FIRST_MAIN_SLOT] = item;
        }
        sortPlayerInventory(items, p.getInventory());
        p.sendRawMessage(ChatColor.GREEN + "Inventory sorted!");
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1);
    }

    private boolean isUnableToSort(PlayerInteractEvent e, Player p) {
        if (!ItemStackGeneratorUtils.isCustomItem(p.getInventory().getItemInMainHand(), SORT_WAND_MODEL_DATA)) {
            return true;
        }
        Reward reward = plugin.getSkillManager().getPlayerRewards(p)
                .getReward(SkillCategory.BUILDING, "AutoSortWand");
        if (!reward.isEnabled()) {
            return true;
        }
        if (!reward.isApplied() && !p.isOp()) {
            p.sendRawMessage(ChatColor.RED + "You have to be building level: " + ChatColor.AQUA + reward.getLevel()
                    + ChatColor.RED + " to use this ability.");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }
        if (e.getHand() == null) {
            return true;
        }
        return !e.getHand().equals(EquipmentSlot.HAND);
    }

    private boolean isShulkerBox(Material material) {
        return Tag.SHULKER_BOXES.isTagged(material);
    }

    public void sortChestInventory(ItemStack[] inventoryItems, Inventory chest) {
        chest.clear();
        for (ItemStack item : getSortedItemList(inventoryItems)) {
            if (item != null) {
                chest.addItem(item);
            }
        }
    }

    public void sortPlayerInventory(ItemStack[] inventoryItems, Inventory playerInventory) {
        // Only clear the main inventory slots (9-35), preserve hotbar (0-8)
        for (int i = FIRST_MAIN_SLOT; i <= LAST_MAIN_SLOT; i++) {
            playerInventory.setItem(i, null);
        }
        int currentSlot = FIRST_MAIN_SLOT;
        for (ItemStack item : getSortedItemList(inventoryItems)) {
            if (item != null && currentSlot <= LAST_MAIN_SLOT) {
                playerInventory.setItem(currentSlot, item);
                currentSlot++;
            }
        }
    }

    private List<ItemStack> getSortedItemList(ItemStack[] inventoryItems) {
        List<ItemStack> items = getSortedItems(inventoryItems);
        List<ItemStack> enchantedBooks = getSortedEnchantedBooks(inventoryItems);
        List<ItemStack> result = new ArrayList<>(items.size() + enchantedBooks.size());
        result.addAll(items);
        result.addAll(enchantedBooks);
        return result;
    }

    public List<ItemStack> getSortedItems(ItemStack[] itemsRaw) {
        // Filter out nulls & enchanted books to avoid duplication (books handled separately)
        List<ItemStack> filtered = streamNonNullItems(itemsRaw)
                .filter(i -> i.getType() != Material.ENCHANTED_BOOK)
                .sorted(java.util.Comparator
                        .comparing((ItemStack i) -> i.getType().toString())
                        .thenComparing(i -> {
                            ItemMeta meta = i.getItemMeta();
                            if (meta != null && meta.hasDisplayName()) {
                                return ChatColor.stripColor(meta.getDisplayName());
                            }
                            return "";
                        }))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            return Collections.emptyList();
        }

        // Condense consecutive similar stacks (sorting groups identical items together)
        List<ItemStack> condensed = new ArrayList<>();
        for (ItemStack item : filtered) {
            condenseList(condensed, item.clone());
        }
        return condensed;
    }

    public List<ItemStack> getSortedEnchantedBooks(ItemStack[] itemsRaw) {
        List<ItemStack> enchantedBooks = streamNonNullItems(itemsRaw)
                .filter(i -> i.getType() == Material.ENCHANTED_BOOK)
                .sorted(java.util.Comparator
                        .comparing(this::buildEnchantmentSortKey)
                        .thenComparingInt(this::getTotalEnchantmentLevels))
                .toList();

        return enchantedBooks;
    }

    private void condenseList(List<ItemStack> items, ItemStack itemToAdd) {
        if (itemToAdd == null) {
            return;
        }
        if (items.isEmpty()) {
            items.add(itemToAdd);
            return;
        }

        ItemStack lastItem = items.getLast();
        if (lastItem == null) {
            items.add(itemToAdd);
            return;
        }

        if (itemToAdd.getMaxStackSize() == 1) {
            items.add(itemToAdd);
            return;
        }

        ItemMeta meta = itemToAdd.getItemMeta();
        if (meta != null && ItemStackGeneratorUtils.hasCustomModelData(meta)) {
            items.add(itemToAdd);
            return;
        }

        if (!lastItem.isSimilar(itemToAdd) || lastItem.getAmount() == MAX_STACK_SIZE) {
            items.add(itemToAdd);
            return;
        }

        int combined = lastItem.getAmount() + itemToAdd.getAmount();
        if (combined <= MAX_STACK_SIZE) {
            lastItem.setAmount(combined);
        } else {
            int amountToAdd = MAX_STACK_SIZE - lastItem.getAmount();
            lastItem.setAmount(MAX_STACK_SIZE);
            itemToAdd.setAmount(itemToAdd.getAmount() - amountToAdd);
            items.add(itemToAdd);
        }
    }

    @SuppressWarnings("deprecation")
    private String buildEnchantmentSortKey(ItemStack book) {
        if (book == null) {
            return "";
        }
        ItemMeta meta = book.getItemMeta();
        if (!(meta instanceof EnchantmentStorageMeta enchantmentMeta)) {
            return "";
        }
        Map<Enchantment, Integer> enchants = enchantmentMeta.getStoredEnchants();
        if (enchants.isEmpty()) {
            return "";
        }
        return enchants.entrySet().stream()
                .sorted(java.util.Comparator.comparing(e -> e.getKey().getKey().toString()))
                .map(e -> e.getKey().getKey() + ":" + e.getValue())
                .collect(Collectors.joining("|"));
    }

    private int getTotalEnchantmentLevels(ItemStack book) {
        if (book == null) {
            return 0;
        }
        ItemMeta meta = book.getItemMeta();
        if (!(meta instanceof EnchantmentStorageMeta enchantmentMeta)) {
            return 0;
        }
        return enchantmentMeta.getStoredEnchants().values().stream()
                .mapToInt(level -> level)
                .sum();
    }

    private static java.util.stream.Stream<ItemStack> streamNonNullItems(ItemStack[] items) {
        return Arrays.stream(items).filter(Objects::nonNull);
    }
}
