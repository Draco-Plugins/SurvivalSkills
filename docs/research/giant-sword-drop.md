# Research: Giant Sword Drop

## Overview

Giant boss deaths are routed from the fighting listener to `BossManager`, which owns boss-specific loot, cleanup, announcements, and Fighting skill XP. Normal mob deaths return to `FightingSkill`, where vanilla death drops remain available and Fighting skill XP is awarded separately through `MobXPManager`. Custom items are generated centrally and identified by SurvivalSkills persistent data plus custom model data.

## Issue Context

- User/requested outcome: Add a 10% Giant boss drop that is a diamond sword, doubles enchanting experience from mobs killed with it, and displays larger because it comes from the Giant.
- Current pain or bug: The Giant has no rare weapon drop or weapon-specific vanilla XP behavior, and “display size” is not represented by an existing item API abstraction in the repository.
- Scope classification, if known: T3

## Current Behavior

- `src/main/java/sir_draco/survivalskills/skill_listeners/FightingSkill.java:94` routes non-player deaths by killer and boss status; bosses exit through `BossManager`, while ordinary mobs proceed to special vanilla-boss drops and Fighting skill XP.
- `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/BossManager.java:151` handles tracked boss kills. The Giant branch currently gives the guaranteed Giant Head, removes the boss, broadcasts the kill, and awards boss Fighting XP.
- `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/BossManager.java:188` suppresses vanilla XP for handled boss deaths.
- `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/MobXPManager.java:68` awards SurvivalSkills Fighting XP; it does not change `EntityDeathEvent` vanilla dropped XP.
- `src/main/java/sir_draco/survivalskills/utils/items/ItemStackGenerator.java:429` creates the existing Giant Head through the shared custom-item builder.
- `src/main/java/sir_draco/survivalskills/utils/items/ItemStackGeneratorUtils.java:103` identifies a custom item only when both the SurvivalSkills persistent-data marker and the requested custom-model-data float are present.
- Inference: “enchanting experience” refers to vanilla XP orbs (`EntityDeathEvent#getDroppedExp`) rather than the plugin’s Fighting skill XP because vanilla XP is the resource consumed for enchanting.

## Relevant Files And Entry Points

- `src/main/java/sir_draco/survivalskills/SurvivalSkills.java:229` - registers the singleton `FightingSkill` event listener.
- `src/main/java/sir_draco/survivalskills/skill_listeners/FightingSkill.java:94` - primary entity-death entry point and ordinary-mob XP flow.
- `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/BossManager.java:151` - boss death and loot entry point.
- `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/BossType.java:25` - maps Zombie boss entities to the Giant boss type.
- `src/main/java/sir_draco/survivalskills/utils/items/ItemStackGenerator.java:429` - shared boss/custom item construction surface.
- `src/main/java/sir_draco/survivalskills/utils/items/ItemModelData.java:1` - stable custom model data registry.
- `src/main/java/sir_draco/survivalskills/utils/items/ItemStackGeneratorUtils.java:103` - custom-item identity contract.

## Data Flow Or Control Flow

1. Bukkit sends an `EntityDeathEvent` to `FightingSkill.onKillEntity`.
2. For a tracked Giant killed by a player, `BossManager.handleBossKill` resolves `BossType.GIANT`, builds loot, cleans up the boss, broadcasts the kill, awards Fighting XP, drops overflow loot, and clears vanilla XP.
3. For an ordinary mob killed by a player, the event remains in `FightingSkill`; the listener can inspect the mob’s final damage event and the killer’s held item while the original vanilla dropped-XP value is still available.
4. `MobXPManager` independently awards SurvivalSkills Fighting XP after item/drop processing.

## Important Contracts And Constraints

- Custom item identity requires both the SurvivalSkills PDC boolean and a dedicated custom-model-data value; material or display name alone must not activate behavior.
- Giant rare loot should use the existing `drops` fallback so a full inventory or absent killer does not destroy the item.
- A sword kill should require a direct player final hit; merely holding the sword when a projectile or other damage source kills the mob must not grant double XP.
- The Spigot item metadata API exposes item-model selection but not an inline held-item render scale. Larger rendering therefore depends on the custom item model/resource-pack display transforms associated with a dedicated model-data value.
- Boss deaths intentionally set dropped vanilla XP to zero and exit before the ordinary-mob flow.

## Existing Tests And Validation

- `pom.xml:137` and `pom.xml:143` provide JUnit 5 and Mockito test support.
- No existing focused tests cover `BossManager`, Giant loot probabilities, custom boss weapon identity, or weapon-dependent `EntityDeathEvent` XP.
- Maven test/package is the repository’s build validation surface.

## Risks, Edge Cases, And Unknowns

- Random drop behavior needs a deterministic seam for a reliable unit test.
- XP multiplication must avoid integer overflow even though normal mob XP values are small.
- The visible larger sword model requires matching client resource-pack model data; this repository does not contain a resource pack to define the display transforms.
- If product intent meant SurvivalSkills Fighting XP rather than vanilla enchanting XP, the multiplier would belong in `MobXPManager`; current terminology supports vanilla XP.

## Downstream Guidance

- Requirements should account for: exactly a 10% Giant-only chance, diamond sword material, robust custom-item identity, direct melee-kill qualification, doubled vanilla dropped XP, and a dedicated model slot for oversized resource-pack rendering.
- Planning should consider: central item generation, deterministic loot-roll logic, death-event integration before Fighting XP, focused unit tests, and Maven validation.
- Do not include: changes to global XP multipliers, boss XP suppression, unrelated boss loot, or player-scale attributes that would resize the wielder instead of the item.
