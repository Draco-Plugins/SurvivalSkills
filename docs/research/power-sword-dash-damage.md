# Research: Power Sword Dash Damage

## Overview

The Power Sword right-click action starts a synchronous, tick-based dash that propels the player and searches nearby entities for damage targets. Its damage filter is coupled to the Bloody Domain entity-type whitelist rather than Bukkit's hostile-entity contract, so valid enemies absent from that separate ability's list receive no damage.

## Issue Context

- User/requested outcome: Make the Power Sword dash damage enemies passed during the dash.
- Current pain or bug: The dash movement and effects occur, but valid nearby enemies can receive no damage.
- Scope classification, if known: T2

## Current Behavior

- `GodItemActions.PowerSwordItemAction` checks the Power Ore reward, cancels the right-click interaction, and activates the dash (`src/main/java/sir_draco/survivalskills/skill_listeners/god/items/GodItemActions.java:158`).
- `PowerSword.run()` moves the player and searches a two-block box around the player's current position once per tick (`src/main/java/sir_draco/survivalskills/abilities/items/PowerSword.java:38`).
- The damage path accepts living, non-player entities only when their `EntityType` appears in `AbilityManager.getDomainMobs()` (`src/main/java/sir_draco/survivalskills/abilities/items/PowerSword.java:62`).
- The shared list is explicitly the whitelist for Bloody Domain and is manually populated with selected types, so it is not a complete contract for Power Sword targets (`src/main/java/sir_draco/survivalskills/abilities/AbilityManager.java:37`, `src/main/java/sir_draco/survivalskills/abilities/AbilityManager.java:325`).
- Each accepted entity is damaged for 15 points and recorded by entity ID so it cannot be damaged twice during the same dash (`src/main/java/sir_draco/survivalskills/abilities/items/PowerSword.java:71`).

## Relevant Files And Entry Points

- `src/main/java/sir_draco/survivalskills/skill_listeners/god/GodItemUseHandler.java:76` - Registers the Power Sword custom-model-data action.
- `src/main/java/sir_draco/survivalskills/skill_listeners/god/items/GodItemActions.java:158` - Gates and activates the special ability.
- `src/main/java/sir_draco/survivalskills/abilities/items/PowerSword.java:24` - Owns dash movement, nearby-entity detection, and damage.
- `src/main/java/sir_draco/survivalskills/abilities/AbilityManager.java:325` - Builds the unrelated Bloody Domain type whitelist used by the faulty filter.
- `src/main/java/sir_draco/survivalskills/abilities/godItems/RavagerDash.java:45` - Existing working dash precedent that filters using a Bukkit entity interface rather than a type list.

## Data Flow Or Control Flow

1. A player with the Power Sword right-clicks, and `GodItemUseHandler` dispatches by custom model data.
2. `PowerSwordItemAction` verifies the Power Ore reward and calls `PowerSword.activate()`.
3. The scheduled runnable applies velocity and riptide visuals every tick for 12 ticks.
4. Each tick queries nearby entities, filters damage targets, applies 15 damage attributed to the player, and remembers hit entity IDs.
5. The runnable clears riptiding and cancels at the end or when the player becomes invalid.

## Important Contracts And Constraints

- The dash deliberately excludes players and should affect hostile entities only.
- A target may be damaged at most once per activation.
- Damage remains attributed to the activating player through Bukkit's two-argument `damage` method.
- Bukkit entity operations and the scheduled runnable execute on the main server thread.

## Existing Tests And Validation

- No existing test covers `PowerSword` target selection or damage.
- `src/test/java/sir_draco/survivalskills/abilities/items/GiantSwordTest.java:1` demonstrates focused ability tests in the same package.
- Maven test and package commands are defined by `pom.xml`.

## Risks, Edge Cases, And Unknowns

- Entity IDs are reused only after entities are removed; the short 12-tick lifetime makes reuse during one dash negligible.
- Bukkit damage events may still be cancelled by region/protection plugins; that is external to target detection.
- The two-block per-tick query is not a swept-volume collision check, but the configured dash speed does not create a gap larger than the query radius.

## Downstream Guidance

- Requirements should account for: Bukkit-defined hostile enemies, no player or passive-mob damage, player-attributed damage, and one hit per target per dash.
- Planning should consider: removing the Bloody Domain coupling and adding focused target-selection regression tests.
- Do not include: balance changes to damage, radius, speed, duration, PvP behavior, or Bloody Domain's whitelist.
