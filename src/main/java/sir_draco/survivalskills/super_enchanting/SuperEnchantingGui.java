package sir_draco.survivalskills.super_enchanting;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.super_enchanting.SuperEnchantingRules.UpgradeCost;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class SuperEnchantingGui implements Listener {

    private static final int INVENTORY_SIZE = 54;
    private static final int INPUT_SLOT = 13;
    private static final int STATUS_SLOT = 4;
    private static final int INSTRUCTIONS_SLOT = 10;
    private static final List<Integer> ENCHANTMENT_SLOTS = List.of(
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43);

    private final SurvivalSkills plugin;

    public SuperEnchantingGui(SurvivalSkills plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        SuperEnchantingInventoryHolder holder = new SuperEnchantingInventoryHolder(player.getUniqueId());
        decorate(holder.getInventory());
        refresh(player, holder);
        player.openInventory(holder.getInventory());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (!(event.getView().getTopInventory().getHolder() instanceof SuperEnchantingInventoryHolder holder))
            return;
        if (!holder.playerId().equals(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null)
            return;

        if (clickedInventory.equals(holder.getInventory())) {
            handleTopInventoryClick(event, player, holder);
            return;
        }

        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        if (!event.isShiftClick())
            return;
        event.setCancelled(true);
        if (holder.getInventory().getItem(INPUT_SLOT) != null)
            return;
        ItemStack selectedItem = event.getCurrentItem();
        if (selectedItem == null || selectedItem.getType().isAir())
            return;
        holder.getInventory().setItem(INPUT_SLOT, selectedItem.clone());
        event.setCurrentItem(null);
        refresh(player, holder);
    }

    private void handleTopInventoryClick(InventoryClickEvent event, Player player,
                                         SuperEnchantingInventoryHolder holder) {
        int slot = event.getSlot();
        if (slot == INPUT_SLOT) {
            scheduleRefresh(player, holder);
            return;
        }

        event.setCancelled(true);
        Enchantment enchantment = holder.getEnchantment(slot);
        if (enchantment != null)
            upgradeEnchantment(player, holder, enchantment);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (!(event.getView().getTopInventory().getHolder() instanceof SuperEnchantingInventoryHolder holder))
            return;

        boolean touchesTopInventory = event.getRawSlots().stream().anyMatch(slot -> slot < INVENTORY_SIZE);
        if (!touchesTopInventory)
            return;
        boolean onlyTouchesInput = event.getRawSlots().stream()
                .filter(slot -> slot < INVENTORY_SIZE)
                .allMatch(slot -> slot == INPUT_SLOT);
        if (!onlyTouchesInput) {
            event.setCancelled(true);
            return;
        }
        scheduleRefresh(player, holder);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player))
            return;
        if (!(event.getInventory().getHolder() instanceof SuperEnchantingInventoryHolder holder))
            return;

        ItemStack input = holder.getInventory().getItem(INPUT_SLOT);
        holder.getInventory().setItem(INPUT_SLOT, null);
        holder.clearEnchantments();
        if (input == null || input.getType().isAir())
            return;
        giveOrDrop(player, input);
    }

    private void upgradeEnchantment(Player player, SuperEnchantingInventoryHolder holder,
                                    Enchantment enchantment) {
        ItemStack input = holder.getInventory().getItem(INPUT_SLOT);
        if (input == null || input.getType().isAir())
            return;

        Map<Enchantment, Integer> enchantments = getEnchantments(input);
        Integer currentLevel = enchantments.get(enchantment);
        if (currentLevel == null) {
            refresh(player, holder);
            return;
        }

        Optional<UpgradeCost> costOptional = SuperEnchantingRules.getUpgradeCost(
                enchantment.getMaxLevel(), currentLevel);
        if (costOptional.isEmpty()) {
            player.sendMessage(ChatColor.RED + "That enchantment cannot be super upgraded yet.");
            player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }

        UpgradeCost cost = costOptional.get();
        int fragments = countTrialFragments(player);
        if (fragments < cost.fragmentCost()) {
            player.sendMessage(ChatColor.RED + "You need " + cost.fragmentCost() + " trial fragments.");
            player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }
        if (player.getLevel() < cost.levelCost()) {
            player.sendMessage(ChatColor.RED + "You need " + cost.levelCost() + " experience levels.");
            player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }

        if (!removeTrialFragments(player, cost.fragmentCost()))
            return;
        player.giveExpLevels(-cost.levelCost());
        setEnchantmentLevel(input, enchantment, currentLevel + 1);
        holder.getInventory().setItem(INPUT_SLOT, input);
        player.sendMessage(ChatColor.GREEN + formatEnchantmentName(enchantment) + " upgraded to level "
                + (currentLevel + 1) + "!");
        player.playSound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, 1.2f);
        refresh(player, holder);
    }

    @SuppressWarnings({ "deprecation" })
    private void refresh(Player player, SuperEnchantingInventoryHolder holder) {
        if (!player.isOnline())
            return;
        Inventory inventory = holder.getInventory();
        holder.clearEnchantments();
        ENCHANTMENT_SLOTS.forEach(slot -> inventory.setItem(slot, null));
        inventory.setItem(STATUS_SLOT, createStatusItem(player));

        ItemStack input = inventory.getItem(INPUT_SLOT);
        if (input == null || input.getType().isAir()) {
            inventory.setItem(INSTRUCTIONS_SLOT, createInformationItem(Material.BOOK,
                    ChatColor.AQUA + "Insert an enchanted item",
                    List.of(ChatColor.GRAY + "Place an item in the empty slot above.",
                            ChatColor.GRAY + "Its enchantments will appear below.")));
            return;
        }

        Map<Enchantment, Integer> enchantments = getEnchantments(input);
        if (enchantments.isEmpty()) {
            inventory.setItem(INSTRUCTIONS_SLOT, createInformationItem(Material.BARRIER,
                    ChatColor.RED + "No enchantments found",
                    List.of(ChatColor.GRAY + "Only existing enchantments can be upgraded.")));
            return;
        }

        inventory.setItem(INSTRUCTIONS_SLOT, createInformationItem(Material.ENCHANTING_TABLE,
                ChatColor.LIGHT_PURPLE + "Choose an enchantment",
                List.of(ChatColor.GRAY + "Click an enchantment below to upgrade it.")));

        List<Map.Entry<Enchantment, Integer>> sortedEnchantments = enchantments.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().getKey().toString()))
                .toList();
        int renderedEnchantments = Math.min(sortedEnchantments.size(), ENCHANTMENT_SLOTS.size());
        for (int index = 0; index < renderedEnchantments; index++) {
            Map.Entry<Enchantment, Integer> entry = sortedEnchantments.get(index);
            int slot = ENCHANTMENT_SLOTS.get(index);
            inventory.setItem(slot, createEnchantmentItem(player, entry.getKey(), entry.getValue()));
            holder.putEnchantment(slot, entry.getKey());
        }
    }

    private ItemStack createStatusItem(Player player) {
        return createInformationItem(Material.EXPERIENCE_BOTTLE,
                ChatColor.GOLD + "Super Enchanting Resources",
                List.of(ChatColor.LIGHT_PURPLE + "Trial Fragments: " + countTrialFragments(player),
                        ChatColor.GREEN + "Experience Levels: " + player.getLevel()));
    }

    private ItemStack createEnchantmentItem(Player player, Enchantment enchantment, int currentLevel) {
        int baseMaximum = enchantment.getMaxLevel();
        int superMaximum = SuperEnchantingRules.getMaximumLevel(baseMaximum);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current level: " + ChatColor.WHITE + currentLevel);
        lore.add(ChatColor.GRAY + "Super maximum: " + ChatColor.WHITE + superMaximum);
        lore.add("");

        Optional<UpgradeCost> costOptional = SuperEnchantingRules.getUpgradeCost(baseMaximum, currentLevel);
        if (costOptional.isEmpty()) {
            if (currentLevel < baseMaximum)
                lore.add(ChatColor.YELLOW + "Reach vanilla level " + baseMaximum + " first.");
            else
                lore.add(ChatColor.GREEN + "Maximum level reached");
        } else {
            UpgradeCost cost = costOptional.get();
            lore.add(ChatColor.GOLD + "Upgrade to level " + (currentLevel + 1));
            lore.add(ChatColor.LIGHT_PURPLE + "Cost: " + cost.fragmentCost() + " trial fragments");
            lore.add(ChatColor.GREEN + "Cost: " + cost.levelCost() + " experience levels");
            if (countTrialFragments(player) >= cost.fragmentCost() && player.getLevel() >= cost.levelCost())
                lore.add(ChatColor.AQUA + "Click to upgrade");
            else
                lore.add(ChatColor.RED + "Missing required resources");
        }

        return createInformationItem(Material.ENCHANTED_BOOK,
                ChatColor.AQUA + formatEnchantmentName(enchantment), lore);
    }

    private ItemStack createInformationItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private void decorate(Inventory inventory) {
        ItemStack border = createInformationItem(Material.PURPLE_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < INVENTORY_SIZE; slot++)
            inventory.setItem(slot, border);
        inventory.setItem(INPUT_SLOT, null);
        ENCHANTMENT_SLOTS.forEach(slot -> inventory.setItem(slot, null));
    }

    private void scheduleRefresh(Player player, SuperEnchantingInventoryHolder holder) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.getOpenInventory().getTopInventory().getHolder() == holder)
                refresh(player, holder);
        });
    }

    private Map<Enchantment, Integer> getEnchantments(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta storageMeta)
            return Map.copyOf(storageMeta.getStoredEnchants());
        if (meta == null)
            return Map.of();
        return Map.copyOf(meta.getEnchants());
    }

    private void setEnchantmentLevel(ItemStack item, Enchantment enchantment, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            storageMeta.addStoredEnchant(enchantment, level, true);
            item.setItemMeta(storageMeta);
            return;
        }
        if (meta == null)
            return;
        meta.addEnchant(enchantment, level, true);
        item.setItemMeta(meta);
    }

    private int countTrialFragments(Player player) {
        int fragments = 0;
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (SuperEnchantingItems.isTrialFragment(item, plugin))
                fragments += item.getAmount();
        }
        return fragments;
    }

    private boolean removeTrialFragments(Player player, int amount) {
        if (countTrialFragments(player) < amount)
            return false;
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < contents.length && remaining > 0; slot++) {
            ItemStack item = contents[slot];
            if (!SuperEnchantingItems.isTrialFragment(item, plugin))
                continue;
            int removed = Math.min(remaining, item.getAmount());
            item.setAmount(item.getAmount() - removed);
            if (item.getAmount() == 0)
                player.getInventory().setItem(slot, null);
            remaining -= removed;
        }
        return remaining == 0;
    }

    @SuppressWarnings("deprecation")
    private String formatEnchantmentName(Enchantment enchantment) {
        String[] words = enchantment.getKey().toString().split("_");
        StringBuilder name = new StringBuilder();
        for (String word : words) {
            if (!name.isEmpty())
                name.append(' ');
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return name.toString();
    }

    private void giveOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(item);
        leftovers.values().forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private static final class SuperEnchantingInventoryHolder implements InventoryHolder {
        private final UUID playerId;
        private final Inventory inventory;
        private final Map<Integer, Enchantment> enchantmentsBySlot = new HashMap<>();

        private SuperEnchantingInventoryHolder(UUID playerId) {
            this.playerId = playerId;
            this.inventory = Bukkit.createInventory(this, INVENTORY_SIZE,
                    ChatColor.DARK_PURPLE + "Super Enchanting");
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        private UUID playerId() {
            return playerId;
        }

        private Enchantment getEnchantment(int slot) {
            return enchantmentsBySlot.get(slot);
        }

        private void putEnchantment(int slot, Enchantment enchantment) {
            enchantmentsBySlot.put(slot, enchantment);
        }

        private void clearEnchantments() {
            enchantmentsBySlot.clear();
        }
    }
}
