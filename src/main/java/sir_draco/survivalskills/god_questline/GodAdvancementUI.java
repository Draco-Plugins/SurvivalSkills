package sir_draco.survivalskills.god_questline;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class GodAdvancementUI {

    private static final int INVENTORY_SIZE = 36;
    private static final int ADVANCEMENTS_PER_PAGE = 27;
    private static final int FOOTER_START_SLOT = 27;
    private static final int BACK_SLOT = 30;
    private static final int PROGRESS_SLOT = 31;
    private static final int NEXT_SLOT = 32;

    private final List<Inventory> inventories;
    private int currentInventoryIndex;

    public GodAdvancementUI(List<Advancement> incompleteAdvancements,
            GodTrophyQuest.QuestProgress questProgress) {
        Objects.requireNonNull(incompleteAdvancements);
        Objects.requireNonNull(questProgress);
        inventories = createInventories(List.copyOf(incompleteAdvancements), questProgress);
    }

    public void open(Player player) {
        Objects.requireNonNull(player);
        currentInventoryIndex = 0;
        player.openInventory(inventories.get(currentInventoryIndex));
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null || !clickedInventory.equals(inventories.get(currentInventoryIndex)))
            return;

        if (event.getRawSlot() == BACK_SLOT)
            openPreviousPage((Player) event.getWhoClicked());
        else if (event.getRawSlot() == NEXT_SLOT)
            openNextPage((Player) event.getWhoClicked());
    }

    public void handleDrag(InventoryDragEvent event) {
        event.setCancelled(true);
    }

    private List<Inventory> createInventories(List<Advancement> incompleteAdvancements,
            GodTrophyQuest.QuestProgress questProgress) {
        int totalPages = calculatePageCount(incompleteAdvancements.size());
        List<Inventory> pages = new ArrayList<>(totalPages);
        for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
            Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE,
                    String.format("God Advancements: Page %d/%d", pageIndex + 1, totalPages));
            addAdvancements(inventory, incompleteAdvancements, pageIndex);
            decorateFooter(inventory, questProgress, pageIndex > 0, pageIndex + 1 < totalPages);
            pages.add(inventory);
        }

        if (incompleteAdvancements.isEmpty())
            pages.getFirst().setItem(13, createEmptyStateItem());
        return List.copyOf(pages);
    }

    private void addAdvancements(Inventory inventory, List<Advancement> incompleteAdvancements, int pageIndex) {
        int firstAdvancementIndex = pageIndex * ADVANCEMENTS_PER_PAGE;
        int pageEnd = Math.min(firstAdvancementIndex + ADVANCEMENTS_PER_PAGE, incompleteAdvancements.size());
        for (int advancementIndex = firstAdvancementIndex; advancementIndex < pageEnd; advancementIndex++)
            inventory.setItem(advancementIndex - firstAdvancementIndex,
                    createAdvancementItem(incompleteAdvancements.get(advancementIndex)));
    }

    private ItemStack createAdvancementItem(Advancement advancement) {
        AdvancementDisplay display = advancement.getDisplay();
        ItemStack item = display == null ? new ItemStack(Material.PAPER) : display.getIcon().clone();
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;

        String title = display == null ? formatAdvancementName(advancement.getKey()) : display.getTitle();
        meta.setDisplayName(ChatColor.RED + title);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.RED + "Status: Incomplete");
        if (display != null && !display.getDescription().isBlank())
            lore.add(ChatColor.GRAY + display.getDescription());
        lore.add(ChatColor.DARK_GRAY + advancement.getKey().toString());
        meta.setLore(List.copyOf(lore));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEmptyStateItem() {
        ItemStack item = new ItemStack(Material.LIME_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;
        meta.setDisplayName(ChatColor.GREEN + "No Advancements Remaining");
        meta.setLore(List.of(ChatColor.GRAY + "All required advancements are complete."));
        item.setItemMeta(meta);
        return item;
    }

    private void decorateFooter(Inventory inventory, GodTrophyQuest.QuestProgress questProgress,
            boolean showBack, boolean showNext) {
        ItemStack footer = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        for (int slot = FOOTER_START_SLOT; slot < INVENTORY_SIZE; slot++)
            inventory.setItem(slot, footer);

        inventory.setItem(PROGRESS_SLOT, createProgressItem(questProgress));
        if (showBack)
            inventory.setItem(BACK_SLOT, createArrow(ChatColor.RED + "Back", false));
        if (showNext)
            inventory.setItem(NEXT_SLOT, createArrow(ChatColor.BLUE + "Next", true));
    }

    private ItemStack createProgressItem(GodTrophyQuest.QuestProgress questProgress) {
        ItemStack progressItem = new ItemStack(Material.COMPASS);
        ItemMeta meta = progressItem.getItemMeta();
        if (meta == null)
            return progressItem;
        meta.setDisplayName(ChatColor.GOLD + "God Quest Progress");
        meta.setLore(List.of(
                ChatColor.YELLOW + "Current Step: " + ChatColor.WHITE + questProgress.currentStep(),
                ChatColor.YELLOW + "Progress: " + ChatColor.AQUA + questProgress.currentCount()
                        + ChatColor.WHITE + " / " + ChatColor.AQUA + questProgress.goal(),
                ChatColor.YELLOW + "Next Step: " + ChatColor.WHITE + questProgress.nextStep()));
        progressItem.setItemMeta(meta);
        return progressItem;
    }

    private ItemStack createArrow(String displayName, boolean pointsForward) {
        ItemStack arrow = new ItemStack(Material.ARROW);
        ItemMeta meta = arrow.getItemMeta();
        if (meta == null)
            return arrow;
        meta.setDisplayName(displayName);
        if (pointsForward)
            ItemStackGeneratorUtils.setCustomModelData(meta, 1);
        arrow.setItemMeta(meta);
        return arrow;
    }

    private void openPreviousPage(Player player) {
        if (currentInventoryIndex <= 0)
            return;
        currentInventoryIndex--;
        player.openInventory(inventories.get(currentInventoryIndex));
    }

    private void openNextPage(Player player) {
        if (currentInventoryIndex + 1 >= inventories.size())
            return;
        currentInventoryIndex++;
        player.openInventory(inventories.get(currentInventoryIndex));
    }

    static int calculatePageCount(int advancementCount) {
        if (advancementCount < 0)
            throw new IllegalArgumentException("Advancement count cannot be negative");
        return Math.max(1, Math.ceilDiv(advancementCount, ADVANCEMENTS_PER_PAGE));
    }

    static String formatAdvancementName(NamespacedKey key) {
        Objects.requireNonNull(key);
        return String.join(" ", Arrays.stream(key.getKey().replace('/', ' ').replace('_', ' ').split(" "))
                .filter((String word) -> !word.isBlank())
                .map((String word) -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .toList());
    }

    public List<Inventory> getInventories() {
        return inventories;
    }

    public int getCurrentInventoryIndex() {
        return currentInventoryIndex;
    }
}
