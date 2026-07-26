# Research: Trophy Display Item Restart Cleanup

## Overview

Trophies are persisted in `trophydata.yml` and reconstructed by `TrophyManager` during plugin startup. Each non-God trophy recreates a floating `Item` entity through `TrophyEffects`; the entity has runtime metadata and an enchanted item stack. The reported restart bug occurs because runtime metadata is not a reliable persisted identifier for an entity loaded from disk.

## Issue Context

- User/requested outcome: Remove a stale trophy display item near its expected spawn location after a restart.
- Current pain or bug: A previous display item can remain when its runtime trophy metadata is missing after restart.
- Scope classification, if known: T2

## Current Behavior

- `TrophyManager.loadTrophies()` loads persisted trophies and calls `Trophy.spawnTrophy(plugin, false)` (`src/main/java/sir_draco/survivalskills/trophy/TrophyManager.java:60-85`).
- `Trophy.spawnTrophy()` creates `TrophyEffects`, initializes it, and starts its repeating task (`src/main/java/sir_draco/survivalskills/trophy/Trophy.java:29-37`).
- Non-God initialization calls `TrophyEffects.spawnItem(0.5, 1.0, 0.5)` (`src/main/java/sir_draco/survivalskills/trophy/TrophyEffects.java:73-94`).
- The primary display item uses the trophy material and Knockback enchantment, while `setFloatingItemProperties()` adds runtime metadata and persistent entity data (`src/main/java/sir_draco/survivalskills/trophy/TrophyEffects.java:122-151`, `:331-347`).
- Before this fix, duplicate cleanup enumerated all world entities, required `TROPHY_ITEM` metadata, and used a five-block distance from the trophy block. That metadata is runtime-only and can be absent after restart.

## Relevant Files And Entry Points

- `src/main/java/sir_draco/survivalskills/trophy/TrophyManager.java:60` - Loads persisted trophies during startup.
- `src/main/java/sir_draco/survivalskills/trophy/Trophy.java:29` - Recreates the trophy block and effects.
- `src/main/java/sir_draco/survivalskills/trophy/TrophyEffects.java:73` - Initializes display effects for loaded and newly placed trophies.
- `src/main/java/sir_draco/survivalskills/trophy/TrophyEffects.java:122` - Creates the primary display item and invokes duplicate cleanup.
- `src/test/java/sir_draco/survivalskills/trophy/TrophyEffectsDisplayCleanupTest.java` - Validates the cleanup query radius and rejection of an unenchanted nearby item.

## Data Flow Or Control Flow

1. `TrophyManager` reads a persisted trophy location and type.
2. `Trophy.spawnTrophy()` creates `TrophyEffects` and invokes initialization.
3. `TrophyEffects.spawnItem()` determines the trophy material, checks for an existing display item, and spawns the replacement at the block center plus one block vertically.
4. The display item receives its stack, floating properties, runtime metadata, and persistent entity trophy ID.

## Important Contracts And Constraints

- The primary display location is `loc + (0.5, 1.0, 0.5)`.
- Display item identity is based on the trophy material and Knockback enchantment; runtime metadata may not survive a restart.
- Champion trophies have additional orbiting items and a separate persisted cleanup path (`TrophyEffects.removePersistedChampionItems()`).
- Decorative Forest and Fishing items do not use the primary display item’s enchantment signature.

## Existing Tests And Validation

- `src/test/java/sir_draco/survivalskills/trophy/TrophyEffectsDisplayCleanupTest.java` - Checks that duplicate lookup uses a 0.5 block radius and that a same-material item without the display enchantment is retained.
- `src/test/java/sir_draco/survivalskills/trophy/TrophyEffectsGodLifecycleTest.java` - Covers effect cleanup lifecycle behavior.
- `mvn test` - Full test suite passes after the change.

## Risks, Edge Cases, And Unknowns

- A genuinely dropped, enchanted item with the same material at the exact display location will match the requested signature and be removed.
- Entity lookup depends on the trophy’s world and nearby loaded entities; no cross-world scan is involved.
- God trophies do not use the standard display-item spawn path.

## Downstream Guidance

- Requirements should preserve the exact display center and the material-plus-enchantment identity check.
- Planning should keep Champion’s separate orbit cleanup and decorative item behavior intact.
- Do not rely on runtime metadata as the sole restart-safe identifier for the primary display item.
