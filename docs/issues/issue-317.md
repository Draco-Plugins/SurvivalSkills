## Title: Boost exotic mob drop chance during the god quest mob item phase

## Tags

Complexity Classification: T1
Severity: Low
Reason: The change is confined to a single listener method (`dropGodWeapon` in `GodItemDropListener.java`) which currently never resolves the killing player. It requires adding a killer lookup, a null-safe check of the killer's quest phase via `TrophyManager.getPlayerGodQuestData()` + `GodTrophyQuest.getPhase()`, and using 0.004 in place of `GENERIC_DROP_CHANCE` (0.001) when the killer is in the MOB_ITEM phase group (phases 49-57). Only the generic 0.1% path is affected; BOSS (10%) and SPECIAL (1%) drop chances stay untouched.
Needs research before implementation: No

## Summary

The exotic mob drop section of the god questline is tedious because god items drop from mobs at a flat 0.1% chance (`GENERIC_DROP_CHANCE = 0.001`). While a player is in the exotic mob drop phase of the god quest (the MOB_ITEM phase group, phases 49-57), the drop chance should be raised to 0.4% (0.004). For all other cases the chance stays 0.1%.

## Steps to Reproduce Context

1. Progress the god quest to the MOB_ITEM phase group (phases 49-57), where the player must hand in god items dropped by specific mobs (e.g., the web shooter from a spider).
2. Kill a god-item mob (e.g., a spider) as the questing player.
3. Observe that the god item drops with a flat 0.1% chance (`GENERIC_DROP_CHANCE = 0.001` in `GodItemDropListener`), making this phase very grindy.

## Expected Behavior

When the player who killed the god-item mob is in the exotic mob drop phase of the god quest (MOB_ITEM, phases 49-57), god items drop at a 0.4% chance. For players not in that phase, and for kills with no player killer, the drop chance remains 0.1%.

## Actual Behavior

The god item drop chance is a flat 0.1% (`GENERIC_DROP_CHANCE = 0.001`) for all god-item mob deaths, regardless of whether the killer is currently in the exotic mob drop phase of the god quest. `dropGodWeapon` never consults the killing player's quest state.

## Requirements for completed issue

1. When the player who killed a god-item mob is in the exotic mob drop phase of the god quest (MOB_ITEM phase group, phases 49-57), the god item drop chance is 0.4% instead of 0.1%.
2. For all other cases (killer not in that phase, or no player killer), the god item drop chance remains 0.1%.
3. No other drop paths (`BOSS_DROP_CHANCE` / `SPECIAL_DROP_CHANCE`) are changed.

## Context

- Files:
  - `src/main/java/sir_draco/survivalskills/skill_listeners/god/GodItemDropListener.java`
  - `src/main/java/sir_draco/survivalskills/god_questline/GodTrophyQuest.java`
  - `src/main/java/sir_draco/survivalskills/trophy/TrophyManager.java`
- Code Snippets:

  Drop chances and the drop handler — `GodItemDropListener.java` lines 34-95:
  ```java
  private static final double BOSS_DROP_CHANCE = 0.1;
  private static final double SPECIAL_DROP_CHANCE = 0.01;
  private static final double GENERIC_DROP_CHANCE = 0.001;
  ...
  @EventHandler
  public void dropGodWeapon(EntityDeathEvent e) {
      EntityType type = e.getEntityType();
      if (!godItems.containsKey(type))
          return;

      double chance = Math.random();
      ...
      // Rest of the mobs
      if (chance > GENERIC_DROP_CHANCE)
          return;
      ...
  }
  ```

  The MOB_ITEM phase group definition and the phase accessor — `GodTrophyQuest.java` lines 49, 596-598:
  ```java
  MOB_ITEM(49, 57, 0),
  ...
  public int getPhase() {
      return phase;
  }
  ```

  Per-player quest state access — `TrophyManager.java` lines 40 and 265-267:
  ```java
  private final HashMap<UUID, GodTrophyQuest> playerGodQuestData = new HashMap<>();
  ...
  public Map<UUID, GodTrophyQuest> getPlayerGodQuestData() {
      return playerGodQuestData;
  }
  ```

  `dropGodWeapon` currently determines neither the killer nor any quest state; the killing player would need to be resolved from the `EntityDeathEvent` (e.g., `e.getEntity().getKiller()`) and looked up in `getPlayerGodQuestData()`.

## Notes

- The 0.1% chance referenced in the request corresponds to `GENERIC_DROP_CHANCE = 0.001` in `GodItemDropListener`. Boss (10%) and special (1%) drop paths are separate and are not part of this change.
- `playerGodQuestData` may not contain an entry for a player who never started the god quest (entries are created on first join/max-level in `GodListener.onPlayerJoin` and `SurvivalSkills.java` lines 344-349), so the quest lookup must be null-safe.
- No existing test covers `GodItemDropListener`; the randomized drop makes deterministic testing difficult without injecting the chance value.
