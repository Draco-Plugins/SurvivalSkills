package sir_draco.survivalskills.god_questline.trial;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import sir_draco.survivalskills.SurvivalSkills;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Builds and owns the per-stage trial reward loot tables, and handles the reward GUI. Reward
 * definitions are data-driven: they live in {@code trialrewards.yml} so balance can be tuned
 * without recompiling.
 */
public class TrialRewardManager {

    private static final String REWARDS_FILE = "trialrewards.yml";
    private static final int REWARD_COUNT = 3;

    private static final TrialRewardManager INSTANCE = new TrialRewardManager();

    private final Map<Integer, TrialLootTable> lootTables = new HashMap<>();
    private final Set<Inventory> rewardInventories = new HashSet<>();
    private final NamespacedKey trialObjectKey =
            new NamespacedKey(SurvivalSkills.getInstance(), "trialobject");

    private TrialRewardManager() {
    }

    public static TrialRewardManager getInstance() {
        return INSTANCE;
    }

    public NamespacedKey getTrialObjectKey() {
        return trialObjectKey;
    }

    // --- Reward table loading -------------------------------------------

    public void loadRewards() {
        File file = new File(SurvivalSkills.getInstance().getDataFolder(), REWARDS_FILE);
        if (!file.exists())
            SurvivalSkills.getInstance().saveResource(REWARDS_FILE, true);

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        lootTables.clear();

        ConfigurationSection stages = config.getConfigurationSection("stages");
        if (stages == null) {
            Bukkit.getLogger().warning("[SurvivalSkills] No 'stages' section found in " + REWARDS_FILE);
            return;
        }

        for (String stageKey : stages.getKeys(false)) {
            int stage;
            try {
                stage = Integer.parseInt(stageKey);
            } catch (NumberFormatException e) {
                continue;
            }
            TrialLootTable table = new TrialLootTable();
            for (Map<?, ?> entry : stages.getMapList(stageKey)) {
                Object materialName = entry.get("material");
                Object amountObj = entry.get("amount");
                Object weightObj = entry.get("weight");
                if (materialName == null || amountObj == null || weightObj == null)
                    continue;
                Material material = Material.matchMaterial(String.valueOf(materialName));
                if (material == null) {
                    Bukkit.getLogger().warning("[SurvivalSkills] Unknown material in " + REWARDS_FILE + ": " + materialName);
                    continue;
                }
                int amount = ((Number) amountObj).intValue();
                double weight = ((Number) weightObj).doubleValue();
                table.addItem(getTrialItem(material, amount), weight);
            }
            lootTables.put(stage, table);
        }

        if (lootTables.isEmpty())
            Bukkit.getLogger().warning("[SurvivalSkills] Loaded no trial reward stages from " + REWARDS_FILE);
    }

    // --- Trial item factory ---------------------------------------------

    public ItemStack getTrialItem(Material material, int amount) {
        ItemStack item = new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return item;
        meta.getPersistentDataContainer().set(trialObjectKey, PersistentDataType.STRING, "trialitem");
        item.setItemMeta(meta);
        return item;
    }

    // --- Reward GUI ------------------------------------------------------

    public void openRewardGUI(Player p, int wave) {
        Inventory inv = Bukkit.createInventory(null, 9, ChatColor.GOLD + "Wave " + wave + " Rewards");
        int stage = (wave / 5) + 1;
        TrialLootTable table = lootTables.get(stage);
        if (table == null) {
            Bukkit.getLogger().warning("[SurvivalSkills] No loot table found for stage " + stage);
            return;
        }

        PlayerTrialUpgrades upgrades = PlayerTrialUpgrades.getPlayerUpgrades(p);
        double enchantChance = TrialTree.getEnchantChance(upgrades);

        ArrayList<ItemStack> rewards = table.getItems(REWARD_COUNT);
        rewards.replaceAll(item -> TrialTree.applyRandomEnchantments(item, enchantChance));

        inv.setItem(2, rewards.get(0));
        inv.setItem(4, rewards.get(1));
        inv.setItem(6, rewards.get(2));

        rewardInventories.add(inv);
        p.openInventory(inv);
    }

    public boolean isRewardInventory(Inventory inv) {
        return rewardInventories.contains(inv);
    }

    public void handleRewardClick(InventoryClickEvent e) {
        // Block shift-clicks out of the reward inventory into the player inventory.
        if (isRewardInventory(e.getView().getTopInventory()) && !isRewardInventory(e.getInventory())) {
            e.setCancelled(true);
            return;
        }
        if (!isRewardInventory(e.getInventory()))
            return;

        e.setCancelled(true);
        if (e.getCurrentItem() == null)
            return;
        e.getWhoClicked().getInventory().addItem(e.getCurrentItem());
        e.getWhoClicked().closeInventory();
    }

    public void handleRewardDrag(InventoryDragEvent e) {
        if (isRewardInventory(e.getView().getTopInventory())) {
            e.setCancelled(true);
            return;
        }
        if (!isRewardInventory(e.getInventory()))
            return;
        e.setCancelled(true);
        e.getWhoClicked().getInventory().addItem(e.getOldCursor());
        e.getWhoClicked().closeInventory();
    }

    public void handleRewardClose(InventoryCloseEvent e) {
        removeInventory(e.getInventory());
    }

    public void removeInventory(Inventory inventory) {
        rewardInventories.remove(inventory);
    }

    public void clearAll() {
        rewardInventories.clear();
        lootTables.clear();
    }

}
