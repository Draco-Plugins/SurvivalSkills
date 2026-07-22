package sir_draco.survivalskills.wardrobe;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.rewards.PlayerRewards;
import sir_draco.survivalskills.rewards.Reward;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.skills.SkillManager;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class WardrobeGui implements Listener {

    private static final int INVENTORY_SIZE = 54;
    private static final List<Integer> SET_COLUMNS = List.of(1, 4, 7);
    private static final List<String> WARDROBE_REWARD_NAMES = List.of("WardrobeI", "WardrobeII", "WardrobeIII");
    private static final String INVENTORY_TITLE = ChatColor.DARK_AQUA + "Wardrobe";

    private final SurvivalSkills plugin;
    private final WardrobeManager wardrobeManager;

    public WardrobeGui(SurvivalSkills plugin, WardrobeManager wardrobeManager) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
        this.wardrobeManager = Objects.requireNonNull(wardrobeManager, "Wardrobe manager cannot be null");
    }

    public void open(Player player) {
        Objects.requireNonNull(player, "Player cannot be null");
        WardrobeInventoryHolder holder = new WardrobeInventoryHolder(player.getUniqueId());
        refresh(player, holder);
        player.openInventory(holder.getInventory());
        player.playSound(player, Sound.ITEM_ARMOR_EQUIP_LEATHER, 1, 1);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        if (!(event.getView().getTopInventory().getHolder() instanceof WardrobeInventoryHolder holder))
            return;
        if (!holder.playerId().equals(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            if (!isArmorOrEmpty(event.getCursor()))
                event.setCancelled(true);
            return;
        }
        if (clickedInventory.equals(holder.getInventory())) {
            handleTopInventoryClick(event, player, holder);
            return;
        }
        handlePlayerInventoryClick(event, player, holder);
    }

    private void handleTopInventoryClick(InventoryClickEvent event, Player player,
                                         WardrobeInventoryHolder holder) {
        event.setCancelled(true);
        Optional<WardrobeGuiSlot> wardrobeSlot = getWardrobeSlot(event.getRawSlot());
        if (wardrobeSlot.isPresent()) {
            handleArmorSlotClick(event, player, holder, wardrobeSlot.get());
            return;
        }

        Optional<Integer> setIndex = getSwapButtonSetIndex(event.getRawSlot());
        if (setIndex.isEmpty() || (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT))
            return;
        if (!isSetUnlocked(player, setIndex.get())) {
            sendLockedMessage(player, setIndex.get());
            return;
        }

        wardrobeManager.swapWithEquipped(player, setIndex.get());
        plugin.getArmorListener().refreshPlayerArmor(player);
        refresh(player, holder);
        player.playSound(player, Sound.ITEM_ARMOR_EQUIP_IRON, 1, 1);
        player.sendMessage(ChatColor.GREEN + "Swapped with wardrobe set " + (setIndex.get() + 1) + ".");
    }

    private void handleArmorSlotClick(InventoryClickEvent event, Player player,
                                      WardrobeInventoryHolder holder, WardrobeGuiSlot guiSlot) {
        if (!isSetUnlocked(player, guiSlot.setIndex())) {
            sendLockedMessage(player, guiSlot.setIndex());
            return;
        }

        if (event.isShiftClick()) {
            moveStoredItemToPlayerInventory(player, holder, guiSlot);
            return;
        }
        if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT)
            return;

        ItemStack cursor = event.getCursor();
        Optional<ItemStack> storedItem = wardrobeManager.getItem(
                player.getUniqueId(), guiSlot.setIndex(), guiSlot.armorSlot());
        if (WardrobeArmorSlot.isEmpty(cursor)) {
            storedItem.ifPresent((ItemStack item) -> {
                player.setItemOnCursor(item);
                wardrobeManager.setItem(player.getUniqueId(), guiSlot.setIndex(),
                        guiSlot.armorSlot(), Optional.empty());
                refresh(player, holder);
            });
            return;
        }

        if (!guiSlot.armorSlot().accepts(cursor)) {
            player.sendMessage(ChatColor.RED + "Only " + guiSlot.armorSlot().getDisplayName().toLowerCase()
                    + " armor can go in that slot.");
            player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }

        wardrobeManager.setItem(player.getUniqueId(), guiSlot.setIndex(), guiSlot.armorSlot(), Optional.of(cursor));
        player.setItemOnCursor(storedItem.orElseGet(() -> new ItemStack(Material.AIR)));
        refresh(player, holder);
    }

    private void moveStoredItemToPlayerInventory(Player player, WardrobeInventoryHolder holder,
                                                  WardrobeGuiSlot guiSlot) {
        Optional<ItemStack> storedItem = wardrobeManager.getItem(
                player.getUniqueId(), guiSlot.setIndex(), guiSlot.armorSlot());
        if (storedItem.isEmpty())
            return;
        Map<Integer, ItemStack> leftovers = player.getInventory().addItem(storedItem.get());
        if (!leftovers.isEmpty()) {
            player.sendMessage(ChatColor.RED + "Your inventory is full.");
            player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }
        wardrobeManager.setItem(player.getUniqueId(), guiSlot.setIndex(), guiSlot.armorSlot(), Optional.empty());
        refresh(player, holder);
    }

    private void handlePlayerInventoryClick(InventoryClickEvent event, Player player,
                                            WardrobeInventoryHolder holder) {
        if (event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        if (event.isShiftClick()) {
            event.setCancelled(true);
            movePlayerArmorToWardrobe(event, player, holder);
            return;
        }

        ItemStack currentItem = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        if (!isArmorOrEmpty(currentItem) || !isArmorOrEmpty(cursor)) {
            event.setCancelled(true);
            return;
        }

        if (event.getClick() == ClickType.NUMBER_KEY) {
            ItemStack hotbarItem = player.getInventory().getItem(event.getHotbarButton());
            if (!isArmorOrEmpty(hotbarItem))
                event.setCancelled(true);
        } else if (event.getClick() == ClickType.SWAP_OFFHAND
                && !isArmorOrEmpty(player.getInventory().getItemInOffHand())) {
            event.setCancelled(true);
        }
    }

    private void movePlayerArmorToWardrobe(InventoryClickEvent event, Player player,
                                           WardrobeInventoryHolder holder) {
        ItemStack selectedItem = event.getCurrentItem();
        Optional<WardrobeArmorSlot> armorSlot = WardrobeArmorSlot.fromItem(selectedItem);
        if (armorSlot.isEmpty())
            return;

        for (int setIndex = 0; setIndex < WardrobeManager.SET_COUNT; setIndex++) {
            if (!isSetUnlocked(player, setIndex))
                continue;
            if (wardrobeManager.getItem(player.getUniqueId(), setIndex, armorSlot.get()).isPresent())
                continue;
            wardrobeManager.setItem(player.getUniqueId(), setIndex, armorSlot.get(), Optional.of(selectedItem));
            event.setCurrentItem(new ItemStack(Material.AIR));
            refresh(player, holder);
            return;
        }

        player.sendMessage(ChatColor.RED + "Every unlocked " + armorSlot.get().getDisplayName().toLowerCase()
                + " slot is occupied.");
        player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof WardrobeInventoryHolder))
            return;
        if (event.getRawSlots().stream().anyMatch(rawSlot -> rawSlot < INVENTORY_SIZE)) {
            event.setCancelled(true);
            return;
        }
        if (WardrobeArmorSlot.fromItem(event.getOldCursor()).isEmpty())
            event.setCancelled(true);
    }

    private void refresh(Player player, WardrobeInventoryHolder holder) {
        Inventory inventory = holder.getInventory();
        inventory.clear();
        ItemStack background = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", List.of());
        for (int slot = 0; slot < INVENTORY_SIZE; slot++)
            inventory.setItem(slot, background);

        for (int setIndex = 0; setIndex < WardrobeManager.SET_COUNT; setIndex++) {
            boolean unlocked = isSetUnlocked(player, setIndex);
            renderHeader(player, inventory, setIndex, unlocked);
            for (WardrobeArmorSlot armorSlot : WardrobeArmorSlot.values()) {
                renderArmorSlot(player, inventory, setIndex, armorSlot, unlocked);
            }
            renderSwapButton(player, inventory, setIndex, unlocked);
        }
    }

    private void renderHeader(Player player, Inventory inventory, int setIndex, boolean unlocked) {
        int requiredLevel = getRequiredLevel(player, setIndex);
        Material material = unlocked ? Material.ARMOR_STAND : Material.BARRIER;
        ChatColor color = unlocked ? ChatColor.AQUA : ChatColor.RED;
        String status = unlocked ? ChatColor.GREEN + "Unlocked"
                : ChatColor.RED + "Requires Fighting level " + requiredLevel;
        inventory.setItem(SET_COLUMNS.get(setIndex), createItem(material,
                color + "Armor Set " + (setIndex + 1), List.of(status)));
    }

    private void renderArmorSlot(Player player, Inventory inventory, int setIndex,
                                 WardrobeArmorSlot armorSlot, boolean unlocked) {
        int storageSlot = getStorageSlot(setIndex, armorSlot);
        inventory.setItem(storageSlot - 1, createItem(armorSlot.getIconMaterial(),
                ChatColor.YELLOW + armorSlot.getDisplayName() + " Slot " + ChatColor.GRAY + "→",
                List.of(ChatColor.GRAY + "Store one " + armorSlot.getDisplayName().toLowerCase()
                        + " in the slot to the right.")));

        if (!unlocked) {
            inventory.setItem(storageSlot, createItem(Material.RED_STAINED_GLASS_PANE,
                    ChatColor.RED + "Locked", List.of(ChatColor.GRAY + "Unlocks at Fighting level "
                            + getRequiredLevel(player, setIndex) + ".")));
            return;
        }

        Optional<ItemStack> storedItem = wardrobeManager.getItem(player.getUniqueId(), setIndex, armorSlot);
        inventory.setItem(storageSlot, storedItem.orElseGet(() -> createItem(Material.BLACK_STAINED_GLASS_PANE,
                ChatColor.GRAY + "Empty " + armorSlot.getDisplayName() + " Slot",
                List.of(ChatColor.GRAY + "Click with a " + armorSlot.getDisplayName().toLowerCase() + " to store it."))));
    }

    private void renderSwapButton(Player player, Inventory inventory, int setIndex, boolean unlocked) {
        int buttonSlot = getSwapButtonSlot(setIndex);
        if (!unlocked) {
            inventory.setItem(buttonSlot, createItem(Material.RED_CONCRETE,
                    ChatColor.RED + "Set " + (setIndex + 1) + " Locked",
                    List.of(ChatColor.GRAY + "Requires Fighting level "
                            + getRequiredLevel(player, setIndex) + ".")));
            return;
        }
        inventory.setItem(buttonSlot, createItem(Material.LIME_CONCRETE,
                ChatColor.GREEN + "Swap Armor Set " + (setIndex + 1),
                List.of(ChatColor.GRAY + "Click to exchange this set", ChatColor.GRAY + "with your equipped armor.")));
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;
        meta.setDisplayName(name);
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isSetUnlocked(Player player, int setIndex) {
        PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(player);
        if (rewards != null) {
            Reward reward = rewards.getReward(SkillCategory.FIGHTING, WARDROBE_REWARD_NAMES.get(setIndex));
            if (reward != null)
                return reward.isEnabled() && reward.isApplied();
        }
        int fightingLevel = SkillManager.getSkillLevel(player.getUniqueId(), SkillCategory.FIGHTING);
        return fightingLevel >= WardrobeManager.UNLOCK_LEVELS.get(setIndex);
    }

    private void sendLockedMessage(Player player, int setIndex) {
        player.sendMessage(ChatColor.RED + "Wardrobe set " + (setIndex + 1) + " unlocks at Fighting level "
                + getRequiredLevel(player, setIndex) + ".");
        player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    }

    private int getRequiredLevel(Player player, int setIndex) {
        PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(player);
        if (rewards == null)
            return WardrobeManager.UNLOCK_LEVELS.get(setIndex);
        Reward reward = rewards.getReward(SkillCategory.FIGHTING, WARDROBE_REWARD_NAMES.get(setIndex));
        return reward == null ? WardrobeManager.UNLOCK_LEVELS.get(setIndex) : reward.getLevel();
    }

    private static boolean isArmorOrEmpty(ItemStack item) {
        return WardrobeArmorSlot.isEmpty(item) || WardrobeArmorSlot.fromItem(item).isPresent();
    }

    private static Optional<WardrobeGuiSlot> getWardrobeSlot(int rawSlot) {
        for (int setIndex = 0; setIndex < WardrobeManager.SET_COUNT; setIndex++) {
            for (WardrobeArmorSlot armorSlot : WardrobeArmorSlot.values()) {
                if (getStorageSlot(setIndex, armorSlot) == rawSlot)
                    return Optional.of(new WardrobeGuiSlot(setIndex, armorSlot));
            }
        }
        return Optional.empty();
    }

    private static Optional<Integer> getSwapButtonSetIndex(int rawSlot) {
        for (int setIndex = 0; setIndex < WardrobeManager.SET_COUNT; setIndex++) {
            if (getSwapButtonSlot(setIndex) == rawSlot)
                return Optional.of(setIndex);
        }
        return Optional.empty();
    }

    static int getStorageSlot(int setIndex, WardrobeArmorSlot armorSlot) {
        return ((armorSlot.ordinal() + 1) * 9) + SET_COLUMNS.get(setIndex);
    }

    static int getSwapButtonSlot(int setIndex) {
        return 45 + SET_COLUMNS.get(setIndex);
    }

    private record WardrobeGuiSlot(int setIndex, WardrobeArmorSlot armorSlot) {}

    private static final class WardrobeInventoryHolder implements InventoryHolder {
        private final UUID playerId;
        private final Inventory inventory;

        private WardrobeInventoryHolder(UUID playerId) {
            this.playerId = Objects.requireNonNull(playerId);
            inventory = Bukkit.createInventory(this, INVENTORY_SIZE, INVENTORY_TITLE);
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        private UUID playerId() {
            return playerId;
        }
    }
}
