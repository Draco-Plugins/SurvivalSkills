## Title: God Quest potion steps reject extended, upgraded, and splash potions

## Tags

Complexity Classification: T1
Severity: Medium
Reason: Bug-fix-style logic change confined to a single file (`GodTrophyQuest.java`). The over-strict exact `ItemStack.equals()` matching in `handleItemCheck(Player, ItemStack, String)` rejects extended (`LONG_*`), upgraded/high-quality (`STRONG_*`), and splash/lingering potions for the Knowledge potion hand-in steps. Blast radius is limited to `potionStep`; the shared exact-match `handleItemCheck` overload is also used for non-potion hand-ins (e.g., Music Knowledge Disc) and must keep its current behavior.
Needs research before implementation: No

## Summary

The God Quest Knowledge phase potion hand-in steps require an exact base potion — a regular `Material.POTION` of an exact `PotionType`. Extended versions, high-quality (upgraded) versions, and splash potions should count as well, but are currently rejected with the "wrong item" dialogue.

## Steps to Reproduce Context

1. Progress the God Quest to a potion step (e.g., "bring me a potion of swiftness", Knowledge phase).
2. Hold a splash potion of swiftness, an extended potion of swiftness (brewed with redstone), or an upgraded potion of swiftness (brewed with glowstone) in the main hand and interact with the quest NPC.
3. Observe the "You do not have the right item" dialogue; the step does not complete.

## Expected Behavior

Extended versions (`LONG_*`), upgraded/high-quality versions (`STRONG_*`), and splash/lingering potions of the required base potion should be accepted for the potion hand-in steps and complete the step.

## Actual Behavior

Only a regular potion (`Material.POTION`) of the exact base `PotionType` is accepted. `handleItemCheck(Player, ItemStack, String)` requires `hand.equals(item)`, which compares the `Material` and full `PotionMeta`, so `LONG_SWIFTNESS`, `STRONG_SWIFTNESS`, and splash/lingering potions are rejected with the "wrong item" dialogue.

## Requirements for completed issue

1. The God Quest potion hand-in steps accept any variant of the required base potion type, including extended (`LONG_*`), upgraded/high-quality (`STRONG_*`), splash, and lingering potions.
2. Non-potion hand-in steps (e.g., the Music Knowledge Disc) continue to require the exact item; the existing exact `ItemStack.equals()` behavior is preserved.
3. Potions of a different base effect are still rejected.

## Context

- Files:
  - `src/main/java/sir_draco/survivalskills/god_questline/GodTrophyQuest.java`
- Code Snippets:

  Potion hand-in steps (Knowledge phase) — `checkKnowledgeQuest`, lines 342-373:
  ```java
  case 2 -> potionStep(p, PotionType.SWIFTNESS, "Potion of Swiftness", ...);
  case 3 -> potionStep(p, PotionType.FIRE_RESISTANCE, "Potion of Fire Resistance", ...);
  // ... through case 17
  ```

  `potionStep` wrapper — line 705:
  ```java
  private void potionStep(Player p, PotionType type, String friendlyName, List<String> successMessages,
          boolean updateParticles) {
      if (handleItemCheck(p, getPotion(type), friendlyName))
          return;
      onHandInComplete(p, successMessages, updateParticles);
  }
  ```

  `getPotion` — line 566 (builds a plain `Material.POTION` with only the base type set):
  ```java
  public ItemStack getPotion(PotionType type) {
      ItemStack potion = new ItemStack(Material.POTION);
      PotionMeta meta = (PotionMeta) potion.getItemMeta();
      if (meta == null)
          return potion;
      meta.setBasePotionType(type);
      potion.setItemMeta(meta);
      return potion;
  }
  ```

  Exact-match `handleItemCheck` overload — line 155:
  ```java
  public boolean handleItemCheck(Player p, ItemStack item, String itemName) {
      ItemStack hand = p.getInventory().getItemInMainHand();
      if (hand.getType().isAir())
          return emptyHandItemStackDialogue(p, itemName);
      if (!hand.equals(item))
          return wrongItemDialogue(p, itemName);
      playSuccessSound(p);
      return false;
  }
  ```

## Notes

- The final potion step (case 18, Wind Charged) uses `handInMaterialStep` instead and only checks for `Material.POTION`, so it behaves differently from the other potion steps.
- Bukkit `PotionType` (spigot-api 26.2) includes base, `LONG_` (extended), and `STRONG_` (upgraded) variants; `PotionMeta.hasBasePotionType()` / `getBasePotionType()` can be used to compare the base effect regardless of variant.
- The `ItemStack` overload of `handleItemCheck` is also used by `handInItemStackStep` (e.g., Music Knowledge Disc), so any relaxed matching should only apply to potion items.
