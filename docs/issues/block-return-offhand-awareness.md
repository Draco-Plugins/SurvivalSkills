## Title: Block return does not respect offhand placement

## Tags

Complexity Classification: T0
Severity: Low
Reason: Single-file, single-line change in BuildingSkill.java onBlockPlace method (line 79). The fix replaces a generic `p.getInventory().addItem()` with hand-aware logic, using `e.getHand()` which is already imported and used in the same method. No other files, data models, configs, or systems affected. Total score = 4 (Blast Radius=0, Uncertainty=0, Behavior=2, Testing=1, Reversibility=1).
Needs research before implementation: No

## Summary

When a player places a block from their offhand and the block return skill triggers, the returned item is always added to the main inventory instead of the offhand slot, inconsistent with the expectation that the item should return to where it was placed from.

## Steps to Reproduce Context

1. Place a block in the offhand slot
2. Place that block (the player must have a high enough Building level to trigger block return)
3. Observe that the returned block item goes to the main inventory rather than the offhand

## Expected Behavior

If a block is placed from the offhand, the returned item should be placed back into the offhand slot if possible. If the offhand slot is occupied or cannot accept the item, it should fall back to the main inventory.

## Actual Behavior

The returned item is always added to the main inventory via `PlayerInventory.addItem()`, regardless of which hand was used to place the block.

## Requirements for completed issue

1. When block return triggers for a block placed from the offhand, the returned item should be placed in the offhand slot if the slot is empty or contains an identical stackable item
2. If the offhand slot cannot accept the item (occupied by a different item or full), the item should fall back to the main inventory via the existing `addItem()` logic
3. Main-hand placement behavior must remain unchanged

## Context

- Files: `src/main/java/sir_draco/survivalskills/skill_listeners/BuildingSkill.java`
- Code Snippets:

The block return logic at line 79 always adds to the main inventory:

```java
// Give the player the item
p.getInventory().addItem(item);
```

The `BlockPlaceEvent` provides `e.getHand()` for determining which hand was used, and this is already used earlier in the same method (lines 54-59) for shovel and hoe offhand checks:

```java
if (e.getHand().equals(EquipmentSlot.OFF_HAND)
        && p.getInventory().getItemInMainHand().getType().toString().contains("SHOVEL"))
    return;
if (e.getHand().equals(EquipmentSlot.OFF_HAND)
        && p.getInventory().getItemInMainHand().getType().toString().contains("HOE"))
    return;
```

## Notes

The fix should check `e.getHand()` and, if it is `EquipmentSlot.OFF_HAND`, check whether the offhand slot is empty. If empty, set the returned item directly into the offhand slot via `p.getInventory().setItemInOffHand(item)`. Otherwise, fall back to `p.getInventory().addItem(item)`.
