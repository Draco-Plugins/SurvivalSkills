package sir_draco.survivalskills.skill_listeners.fishing;

/**
 * The rarity tier of an enchanted-book drop. Replaces the pair of boolean
 * flags {@code (isEpic, isLegendary)} previously passed to {@code getEnchantedBook},
 * which could not represent the invalid {@code (true, true)} combination and did
 * not document themselves at call sites.
 *
 * <ul>
 *   <li>{@link #LEGENDARY} &mdash; the stored level is applied verbatim.</li>
 *   <li>{@link #EPIC} &mdash; the level is randomized up to the configured max,
 *       with a floor of 3 when the max is at least 4.</li>
 *   <li>{@link #RARE} &mdash; the level is randomized up to the configured max
 *       (floored at 1).</li>
 * </ul>
 */
public enum EnchantTier {
    RARE,
    EPIC,
    LEGENDARY
}