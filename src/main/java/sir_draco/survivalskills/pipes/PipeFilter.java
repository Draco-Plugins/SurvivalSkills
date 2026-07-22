package sir_draco.survivalskills.pipes;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public record PipeFilter(Material material, Set<NamespacedKey> enchantments) {
    private static final String ENCHANTMENT_SEPARATOR = ",";
    private static final String FILTER_SEPARATOR = "|";

    public PipeFilter {
        Objects.requireNonNull(material);
        enchantments = Set.copyOf(enchantments);
        if (material != Material.ENCHANTED_BOOK && !enchantments.isEmpty()) {
            throw new IllegalArgumentException("Only enchanted book filters can contain enchantments");
        }
    }

    public static PipeFilter fromItem(ItemStack item) {
        Objects.requireNonNull(item);
        if (item.getType() != Material.ENCHANTED_BOOK
                || !(item.getItemMeta() instanceof EnchantmentStorageMeta enchantmentMeta)) {
            return new PipeFilter(item.getType(), Set.of());
        }
        Set<NamespacedKey> enchantmentKeys = enchantmentMeta.getStoredEnchants().keySet().stream()
                .map(enchantment -> enchantment.getKeyOrThrow())
                .collect(Collectors.toUnmodifiableSet());
        return new PipeFilter(item.getType(), enchantmentKeys);
    }

    public boolean matches(ItemStack item) {
        Objects.requireNonNull(item);
        if (item.getType() != Material.ENCHANTED_BOOK
                || !(item.getItemMeta() instanceof EnchantmentStorageMeta enchantmentMeta)) {
            return matches(item.getType(), Set.of());
        }
        Set<NamespacedKey> enchantmentKeys = enchantmentMeta.getStoredEnchants().keySet().stream()
                .map(enchantment -> enchantment.getKeyOrThrow())
                .collect(Collectors.toUnmodifiableSet());
        return matches(item.getType(), enchantmentKeys);
    }

    boolean matches(Material itemMaterial, Set<NamespacedKey> itemEnchantments) {
        Objects.requireNonNull(itemMaterial);
        Objects.requireNonNull(itemEnchantments);
        return itemMaterial == material
                && (material != Material.ENCHANTED_BOOK || enchantments.isEmpty()
                || itemEnchantments.containsAll(enchantments));
    }

    public ItemStack toDisplayItem() {
        ItemStack item = new ItemStack(material, 1);
        if (material != Material.ENCHANTED_BOOK || enchantments.isEmpty()
                || !(item.getItemMeta() instanceof EnchantmentStorageMeta enchantmentMeta)) {
            return item;
        }
        enchantments.stream()
                .map(key -> Registry.ENCHANTMENT.get(key))
                .filter(Objects::nonNull)
                .forEach(enchantment -> enchantmentMeta.addStoredEnchant(enchantment, 1, true));
        item.setItemMeta(enchantmentMeta);
        return item;
    }

    public String serialize() {
        if (enchantments.isEmpty()) return material.name();
        String serializedEnchantments = enchantments.stream()
                .map(key -> key.toString())
                .sorted()
                .collect(Collectors.joining(ENCHANTMENT_SEPARATOR));
        return material.name() + FILTER_SEPARATOR + serializedEnchantments;
    }

    public static Optional<PipeFilter> deserialize(String serialized) {
        Objects.requireNonNull(serialized);
        String[] filterParts = serialized.split("\\|", 2);
        Optional<Material> material = Optional.ofNullable(Material.matchMaterial(filterParts[0]));
        if (material.isEmpty()) return Optional.empty();
        if (filterParts.length == 1) return Optional.of(new PipeFilter(material.orElseThrow(), Set.of()));
        if (material.orElseThrow() != Material.ENCHANTED_BOOK || filterParts[1].isBlank()) return Optional.empty();

        Set<String> serializedEnchantments = Arrays.stream(filterParts[1].split(ENCHANTMENT_SEPARATOR))
                .collect(Collectors.toUnmodifiableSet());
        Set<NamespacedKey> enchantments = serializedEnchantments.stream()
                .map((String serializedKey) -> Optional.ofNullable(NamespacedKey.fromString(serializedKey)))
                .flatMap((Optional<NamespacedKey> enchantment) -> enchantment.stream())
                .collect(Collectors.toUnmodifiableSet());
        if (enchantments.size() != serializedEnchantments.size()) return Optional.empty();
        return Optional.of(new PipeFilter(material.orElseThrow(), enchantments));
    }
}
