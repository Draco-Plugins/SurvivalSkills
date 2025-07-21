package sir_draco.survivalskills.godQuestline;

import org.bukkit.inventory.ItemStack;

import java.util.*;

public class TrialLootTable {

    private final NavigableMap<Double, ItemStack> lootTable = new TreeMap<>();
    private double totalWeight = 0.0;

    // Add an item to the loot table
    public void addItem(ItemStack item, double weight) {
        if (weight <= 0) throw new IllegalArgumentException("Weight must be positive");
        totalWeight += weight;
        lootTable.put(totalWeight, item);
    }

    // Drop exactly 3 unique items
    public ArrayList<ItemStack> getItems(int count) {
        if (count > lootTable.size()) {
            throw new IllegalArgumentException("Not enough unique items in the loot table");
        }

        ArrayList<ItemStack> result = new ArrayList<>();
        Random random = new Random();

        while (result.size() < count) {
            double randValue = random.nextDouble() * totalWeight;
            ItemStack selectedItem = lootTable.higherEntry(randValue).getValue();

            // Ensure no duplicates
            if (!result.contains(selectedItem)) result.add(selectedItem);
        }

        return result;
    }
}
