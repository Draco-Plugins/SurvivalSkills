package sir_draco.survivalskills.skill_listeners;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public enum AutoEatMode {
    INVENTORY_ORDER("Inventory Order", "Uses food in inventory slot order."),
    BEST_SATURATION_FIRST("Best Saturation First", "Uses foods with the best saturation ratio first."),
    WORST_FOOD_FIRST("Worst Food First", "Uses low-nutrition foods first."),
    BALANCED("Balanced", "Chooses food that most closely fills the hunger gap.");

    private final String displayName;
    private final String description;

    AutoEatMode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public List<ItemStack> order(List<ItemStack> items, int hungerGap,
            Map<Material, Integer> nutrition, Map<Material, Float> saturation) {
        List<ItemStack> orderedItems = new ArrayList<>(items);
        comparator(hungerGap, nutrition, saturation).ifPresent(orderedItems::sort);
        return orderedItems;
    }

    private java.util.Optional<Comparator<ItemStack>> comparator(int hungerGap,
            Map<Material, Integer> nutrition, Map<Material, Float> saturation) {
        return switch (this) {
            case INVENTORY_ORDER -> java.util.Optional.empty();
            case BEST_SATURATION_FIRST -> java.util.Optional.of(Comparator
                    .comparingDouble((ItemStack item) -> saturation.getOrDefault(item.getType(), 0f)).reversed());
            case WORST_FOOD_FIRST -> java.util.Optional.of(Comparator
                    .comparingInt((ItemStack item) -> nutrition.getOrDefault(item.getType(), 0)));
            case BALANCED -> java.util.Optional.of(Comparator
                    .comparingInt((ItemStack item) -> Math.abs(hungerGap - nutrition.getOrDefault(item.getType(), 0)))
                    .thenComparingInt((ItemStack item) -> nutrition.getOrDefault(item.getType(), 0)));
        };
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
