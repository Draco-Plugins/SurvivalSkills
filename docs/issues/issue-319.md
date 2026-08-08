## Title: Dense/Bundle wool recipes show "null" in the item name instead of the correct color

## Tags

Complexity Classification: T1
Severity: Low
Reason: Single-file bug in RecipeMaker.java. The private DenseWoolColor enum has no external dependents; only the 4 affected enum values and the two display-name concatenation sites are involved. Root cause verified against the Spigot API (ChatColor.getByChar(String) reads only the first character and does not support hex colors, returning null). Impact is cosmetic (item names), no gameplay or data corruption.
Needs research before implementation: No

## Summary

For the Brown, Orange, Pink, and Cyan dense wool tiers, the crafted "Dense X Wool" and "Bundle Of Dense X Wool" items display the literal text "null" in their name where the color prefix should be. This is caused by `ChatColor.getByChar("#RRGGBB")` returning `null` for hex color strings, and that null being string-concatenated into the item display names.

## Steps to Reproduce Context

1. Start a server running the plugin and unlock the wool recipes (or craft the relevant items).
2. Craft the "Dense <color> Wool" item for Brown, Orange, Pink, or Cyan wool (the second tier of the dense wool compaction chain).
3. Craft the "Bundle Of Dense <color> Wool" item (third tier) for the same colors.
4. Inspect the resulting item names in-game.

## Expected Behavior

Every dense wool item name should show the intended color prefix, e.g. "Dense Brown Wool" and "Bundle Of Dense Brown Wool" (with the corresponding color formatting), for all 16 wool colors.

## Actual Behavior

For the Brown, Orange, Pink, and Cyan wool tiers, the display names contain the literal text "null", e.g. "nullDense Brown Wool" and "nullBundle Of Dense Brown Wool", because the color lookup returned `null` and Java string concatenation rendered it as "null".

## Requirements for completed issue

1. No dense wool item name may contain the literal text "null"; every tier (Compacted, Dense, and Bundle Of Dense) must show the correct color name for all 16 wool colors.
2. Color formatting for the four hex-based colors (Brown, Orange, Pink, Cyan) must render as a valid Minecraft color, consistent with the 12 colors that use named `ChatColor` constants.
3. Recipe matching must remain intact, since the fragment recipes match bundle items by exact ItemStack choice, not by name.

## Context

- Files:
  - `src/main/java/sir_draco/survivalskills/utils/RecipeMaker.java` — defines the `DenseWoolColor` enum and builds all dense wool items and recipes.
  - `src/main/java/sir_draco/survivalskills/utils/ColorParser.java` — contains the existing hex-to-ChatColor helper used elsewhere in the codebase.
- Code Snippets:

  `DenseWoolColor` entries that produce a null `ChatColor` (RecipeMaker.java lines 45, 48, 50, 55):
  ```java
  BROWN(Material.BROWN_WOOL, "Brown", ChatColor.getByChar("#6E2C00")),
  ORANGE(Material.ORANGE_WOOL, "Orange", ChatColor.getByChar("#FF8C00")),
  PINK(Material.PINK_WOOL, "Pink", ChatColor.getByChar("#FF00A2")),
  CYAN(Material.CYAN_WOOL, "Cyan", ChatColor.getByChar("#009696")),
  ```

  Bundle name concatenation (RecipeMaker.java line 140):
  ```java
  private static ItemStack buildDenseWoolBundle(DenseWoolColor color) {
          return new ItemStackBuilder(color.material(), 1,
                          color.chatColor() + "Bundle Of Dense " + color.displayName() + " Wool")
  ```

  Dense wool name concatenation (RecipeMaker.java line 288-293):
  ```java
  ItemStack wool2 = color == DenseWoolColor.WHITE
                          ? ItemStackGenerator.getDenseWhiteWool()
                          : new ItemStackBuilder(color.material(), 1,
                                          color.chatColor() + "Dense " + color.displayName() + " Wool")
  ```

  Existing correct hex-color helper (ColorParser.java lines 70-72):
  ```java
  private static String hexToChatColor(String hex) {
          return net.md_5.bungee.api.ChatColor.of(hex) + "";
  }
  ```

## Notes

- Verified via `javap` against `spigot-api-26.2-R0.1-SNAPSHOT`: `org.bukkit.ChatColor.getByChar(String)` only inspects `charAt(0)` against a map of legacy color codes (e.g. '0'-'9', 'a'-'f'). Since the strings passed here start with '#', the lookup returns `null`.
- The "Compacted <color> Wool" tier (RecipeMaker.java line 285) does not prepend a chat color, so it is unaffected.
- Bundle items are consumed as exact-choice ingredients in the Black/White Fragment recipes (RecipeMaker.java lines 145-148, 207, 219); matching is by item data, not by name, so the name bug does not break those recipes.
- No existing tests cover the dense wool recipes.
