# Research: God Trophy Crystal Lifecycle

## Overview

God trophy visuals are driven by a repeating `TrophyEffects` task and delegated to `GodTrophyEffects`, which owns the NPC and end-crystal references. The current recovery code tries to compensate for lost entity references with proximity searches, but the crystal has no persistent trophy identity and terminal cleanup does not cover its full orbit.

## Issue Context

- User/requested outcome: Find and thoroughly fix god trophy end crystals duplicating or remaining after their visual effect should stop.
- Current pain or bug: Crystals can survive pause, trophy removal, plugin shutdown, or reload, and subsequent recovery may remove or recreate the wrong nearby crystal.
- Scope classification, if known: T3

## Current Behavior

- `Trophy.spawnTrophy` creates one repeating `TrophyEffects` task per placed or loaded trophy (`src/main/java/sir_draco/survivalskills/trophy/Trophy.java:33`).
- A loaded god trophy starts at cycle 122, deliberately skipping its placement animation and its cycle-121 NPC/crystal spawn event (`src/main/java/sir_draco/survivalskills/trophy/TrophyEffects.java:71`).
- `GodTrophyBehavior.tick` creates a new `GodTrophyEffects` whenever its in-memory reference is absent (`src/main/java/sir_draco/survivalskills/trophy/trophy_behavior/GodTrophyBehavior.java:11`).
- God trophy crystals orbit 2.5 blocks from the trophy center, while `removeCrystal` searches only 1.25 blocks for untracked stragglers (`src/main/java/sir_draco/survivalskills/god_questline/GodTrophyEffects.java:48`, `src/main/java/sir_draco/survivalskills/god_questline/GodTrophyEffects.java:458`).
- Spawn/recovery searches a wider area but removes every end crystal except the current in-memory reference; it does not check whether the entity belongs to this trophy (`src/main/java/sir_draco/survivalskills/god_questline/GodTrophyEffects.java:442`, `src/main/java/sir_draco/survivalskills/god_questline/GodTrophyEffects.java:491`).
- The `trophy` marker is Bukkit metadata, which is attached on spawn but is not a persistent entity-data identity (`src/main/java/sir_draco/survivalskills/god_questline/GodTrophyEffects.java:453`).
- Pausing removes the entire `GodTrophyEffects` reference. `resumeTrophy` checks that now-null reference before trying to respawn the NPC, so the subsequently recreated effect enters its idle cycle without running the original spawn transition (`src/main/java/sir_draco/survivalskills/trophy/TrophyEffects.java:205`, `src/main/java/sir_draco/survivalskills/trophy/Trophy.java:69`).
- Inference: An untracked crystal on the 2.5-block orbit survives terminal 1.25-block cleanup, and a later effect instance spawns another crystal, producing the reported stay/duplicate behavior.

## Relevant Files And Entry Points

- `src/main/java/sir_draco/survivalskills/trophy/Trophy.java:33` - Starts, pauses, resumes, breaks, and shuts down each trophy task.
- `src/main/java/sir_draco/survivalskills/trophy/TrophyEffects.java:71` - Initializes loaded versus freshly placed trophies and owns the current god-effect reference.
- `src/main/java/sir_draco/survivalskills/trophy/trophy_behavior/GodTrophyBehavior.java:11` - Creates and ticks `GodTrophyEffects`.
- `src/main/java/sir_draco/survivalskills/god_questline/GodTrophyEffects.java:109` - Runs placement transitions and idle NPC/crystal effects.
- `src/main/java/sir_draco/survivalskills/god_questline/GodTrophyEffects.java:442` - Spawns, moves, and proximity-cleans end crystals.
- `src/main/java/sir_draco/survivalskills/trophy/TrophyManager.java:58` - Reconstructs trophies on plugin enable.
- `src/main/java/sir_draco/survivalskills/trophy/TrophyManager.java:162` - Shuts trophy effects down on plugin disable.
- `src/main/java/sir_draco/survivalskills/trophy/TrophyListener.java:148` - Breaks trophies and protects marked crystals from damage.

## Data Flow Or Control Flow

1. A player places a god trophy, or `TrophyManager.loadTrophies` restores one from `trophydata.yml`.
2. `Trophy.spawnTrophy` schedules `TrophyEffects`; fresh trophies begin at cycle 1 and loaded trophies at cycle 122.
3. `GodTrophyBehavior` creates `GodTrophyEffects`. A fresh placement spawns its NPC/crystal at cycle 121; loaded and recreated idle effects skip that transition.
4. Idle ticks move the tracked crystal. If the reference is absent/dead, recovery removes nearby end crystals and spawns a replacement.
5. When no players are nearby, or the trophy/plugin stops, `TrophyEffects.removeItem` calls `GodTrophyEffects.remove`; only the tracked crystal plus untracked crystals within 1.25 blocks are removed.

## Important Contracts And Constraints

- Trophy ID is already a persisted, unique identifier in `trophydata.yml` and is available from the owning `Trophy` object.
- A crystal must remain invulnerable, hide its bottom, beam toward the trophy NPC position, and be protected by `TrophyListener`.
- Cleanup must not remove ordinary end crystals or another nearby god trophy's crystal.
- Recovery must work when Java entity references are lost across pause/resume, chunk lifecycle, or plugin reload.
- Fresh placement animation must remain distinct from loaded idle-state initialization.

## Existing Tests And Validation

- `mvn test` - Baseline suite passes 96 tests, but no existing test covers trophy entity lifecycle.
- `docs/to-test.md` - Contains a manual God Trophy Effects validation section.
- Missing coverage: persistent entity ownership, duplicate selection/removal, full-orbit terminal cleanup, loaded idle initialization, and pause/resume initialization.

## Risks, Edge Cases, And Unknowns

- Legacy crystals created before persistent ownership is added require narrowly scoped migration; indiscriminate removal would risk unrelated end crystals.
- Two god trophies may be close enough for their cleanup volumes to overlap.
- Unloaded chunks do not expose their entities to a world proximity query; normal trophy pause/shutdown should therefore remove tracked entities before the chunk unloads, while persistent identity must support later reconciliation.
- Citizens NPC IDs are tracked only in memory. NPC persistence is related but separate from crystal ownership; the idle initialization gap must not create additional NPCs.

## Downstream Guidance

- Requirements should account for: one persistently owned crystal per trophy, idempotent idle initialization, complete owned-crystal cleanup, legacy migration, and isolation from nearby unrelated crystals.
- Planning should consider: pass the persisted trophy ID into god effects, reconcile rather than blindly respawn, use the full orbit bounds for every cleanup path, and add focused Mockito tests where Bukkit interfaces permit it.
- Do not include: global deletion of end crystals, trophy-data format changes, or unrelated visual/quest refactors.
