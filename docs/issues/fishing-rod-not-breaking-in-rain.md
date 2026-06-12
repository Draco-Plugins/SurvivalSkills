## Title: Fishing rods do not break in rain - they stay at zero durability

## Tags

Complexity Classification: T0
Severity: Medium
Reason: Single method in a single file, well-understood bug with a straightforward fix. Blast Radius=0, Uncertainty=0, Behavior=2, Testing=1, Reversibility=0. Total=3.
Needs research before implementation: No

## Summary

When fishing in the rain, the plugin manually handles rod durability via `handleDurability()`. This method increments the rod's damage value but never checks whether the damage has exceeded the rod's maximum durability (64 for a fishing rod). As a result, the rod stays in the player's hand at 0 durability instead of breaking.

## Steps to Reproduce Context

1. Cast a fishing rod while in rain (or use a Weather Artifact to summon a storm)
2. Wait for the rain auto-catch mechanism to trigger (the `BukkitRunnable` in the rain path)
3. Repeat until the fishing rod's damage reaches or exceeds 64
4. Observe that the rod remains in hand showing 0 durability instead of breaking

## Expected Behavior

The fishing rod should break (be destroyed/removed from the inventory) when its accumulated damage reaches the rod's maximum durability, just as it does during normal (non-rain) fishing.

## Actual Behavior

`handleDurability()` increments `meta.setDamage(meta.getDamage() + 1)` without validating whether the new damage equals or exceeds the fishing rod's maximum durability (64). The rod stays in the player's inventory with a damage value >= 64, appearing at 0 durability but never breaking. The player can continue using it indefinitely.

## Requirements for completed issue

1. Fishing rods should break (be removed from inventory or have their stack count decremented) when their damage reaches or exceeds the maximum durability in the rain fishing path
2. Edge cases should be handled: Unbreaking enchantment chance rolls, and the possibility that the player switches items between casting and the delayed rain catch

## Context

- Files:
  - `src/main/java/sir_draco/survivalskills/skill_listeners/FishingSkill.java`
- Code Snippets:

  **`handleDurability()` method (lines 728–744):**
  ```java
  public void handleDurability(ItemStack fishingRod) {
      if (fishingRod == null)
          return;
      if (fishingRod.getType().equals(Material.FISHING_ROD)) {
          Damageable meta = (Damageable) fishingRod.getItemMeta();
          if (meta == null)
              return;

          if (fishingRod.containsEnchantment(Enchantment.UNBREAKING)) {
              double chance = Math.random() * 100;
              int level = fishingRod.getEnchantmentLevel(Enchantment.UNBREAKING);
              if (chance <= (100f / (level + 1)))
                  meta.setDamage(meta.getDamage() + 1);
          } else
              meta.setDamage(meta.getDamage() + 1);
          fishingRod.setItemMeta(meta);
      }
  }
  ```

  **Call site in the rain path (line 134):**
  ```java
  handleDurability(rod);
  ```

## Notes

- The method does not use `fishingRod.getType().getMaxDurability()` (or the equivalent) to check whether the item should break.
- The `rod` variable (`p.getInventory().getItemInMainHand()`) is captured when the `PlayerFishEvent` fires (state `FISHING`) and used later inside the delayed `BukkitRunnable`. If the player switches items in hand during the delay, durability would be applied to the wrong `ItemStack` reference. This is a pre-existing concern separate from the break logic.
