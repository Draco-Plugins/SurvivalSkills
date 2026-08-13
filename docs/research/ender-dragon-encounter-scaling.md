# Research: Ender Dragon Encounter Scaling

## Overview

The custom Ender Dragon lifecycle is coordinated by `DragonManager`, while `DragonBoss` stores the encounter participant list used for respawn damage authorization and boss targeting. Initial dragons and respawned dragons already take different paths, but participant selection is inconsistent: the initial encounter starts with only players already in an End world, while respawns include every player anywhere in the End without a distance limit.

## Issue Context

- User/requested outcome: Make the initial Ender Dragon a server-wide fight whose health scales with all online players, while respawned dragons are limited to players in the dragon's End world within 200 blocks and scale health from that nearby group.
- Current pain or bug: Players joining the initial fight after the first entrant have experienced being unable to damage the dragon, and respawn scope is not geographically limited.
- Scope classification, if known: T2

## Current Behavior

- Initial-dragon attachment is scheduled 20 ticks after an End-portal teleport and sets maximum health to 250 times `Bukkit.getOnlinePlayers().size()` (`src/main/java/sir_draco/survivalskills/skill_listeners/fighting/DragonManager.java:131`).
- The initial participant list contains only online players whose current world has the End environment, even though the health calculation uses all online players (`src/main/java/sir_draco/survivalskills/skill_listeners/fighting/DragonManager.java:150`).
- End-portal teleports add later players to an initial encounter, but a respawn only accepts the first portal entrant when its participant list is empty (`src/main/java/sir_draco/survivalskills/skill_listeners/fighting/DragonManager.java:115`).
- Direct and projectile player damage is routed through `BossManager`, which rejects non-player-originated dragon damage before delegating participant authorization (`src/main/java/sir_draco/survivalskills/skill_listeners/fighting/BossManager.java:410`).
- `DragonManager` applies participant-based damage authorization only to respawned dragons; initial-dragon player damage is intended to be unrestricted (`src/main/java/sir_draco/survivalskills/skill_listeners/fighting/DragonManager.java:67`).
- Respawn spawning gathers every online player in any End-environment world, calculates health as 250 times that count with a one-player minimum, and stores the same group as participants (`src/main/java/sir_draco/survivalskills/skill_listeners/fighting/DragonManager.java:165`).
- `DragonBoss` uses its participant list for attack target selection, area attacks, flight suppression, and respawn damage authorization through `DragonManager` (`src/main/java/sir_draco/survivalskills/bosses/DragonBoss.java:685`).

## Relevant Files And Entry Points

- `src/main/java/sir_draco/survivalskills/skill_listeners/FightingSkill.java:266` - Routes portal teleports and dragon spawn events into the lifecycle manager.
- `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/DragonManager.java:67` - Enforces dragon player-damage eligibility.
- `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/DragonManager.java:115` - Updates encounter membership around End-portal travel and attaches to the initial dragon.
- `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/DragonManager.java:165` - Creates the custom respawn encounter and calculates its health.
- `src/main/java/sir_draco/survivalskills/bosses/DragonBoss.java:685` - Marks encounters as respawns and stores participants.
- `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/BossManager.java:410` - Resolves the attacking player and invokes the dragon-specific gate.
- `src/test/java/sir_draco/survivalskills/bosses/DragonBossTest.java:20` - Existing focused coverage for DragonBoss helpers, but not encounter selection or damage authorization.

## Data Flow Or Control Flow

1. A player travels through an End portal, and `FightingSkill.teleportToEnd` delegates the event to `DragonManager.teleportToEnd`.
2. For an undefeated initial dragon, a delayed task finds the vanilla entity, attaches `DragonBoss`, calculates server-count health, initializes participants, and starts its tick task.
3. For a later vanilla dragon spawn in a world marked `killedfirstdragon`, `dragonSpawnEvent` immediately attaches `DragonBoss`, selects End players, calculates health, marks the encounter as a respawn, and starts its tick task.
4. When a player or player-fired projectile damages the dragon, `BossManager` resolves the player and delegates to `DragonManager`; respawn damage is cancelled when that player is absent from the stored participant list.
5. During the encounter, `DragonBoss` uses the stored participants to choose targets and apply custom attacks and Dragon's Wrath effects.

## Important Contracts And Constraints

- The initial dragon's health unit is 250 per online server player and must retain at least the vanilla custom base of 250.
- Respawn participant selection and health scaling must use the same snapshot so damage permissions, targets, and health describe one encounter group.
- Players must be in the exact End world containing the respawned dragon as well as within the requested 200-block radius; another End world must not qualify.
- The 200-block boundary is inclusive.
- Existing explosion and lightning immunities, custom attacks, death behavior, rewards, and XP handling are outside this change.

## Existing Tests And Validation

- `src/test/java/sir_draco/survivalskills/bosses/DragonBossTest.java:75` verifies duplicate-safe participant merging but not how participants are selected.
- `src/test/java/sir_draco/survivalskills/skill_listeners/fighting/BossManagerTest.java:28` verifies summoner-based damage gates for non-dragon bosses only.
- No test currently proves initial-dragon global player damage, respawn participant-only damage, 200-block selection, or player-count health scaling.
- Maven provides unit-test, compile, and package validation through `pom.xml`.

## Risks, Edge Cases, And Unknowns

- A respawn can theoretically have no qualifying nearby player at its spawn event; retaining the existing one-player minimum avoids zero health, while its empty participant snapshot prevents unrelated players from damaging it.
- Players who connect after the initial health snapshot should be allowed to join and damage the server-wide initial encounter, but should not retroactively increase its maximum health.
- Players who approach a respawn only after its spawn snapshot are inferred to remain outside that encounter, matching the existing stored-participant model and the request that respawns be fought by the nearby single player or handful that spawned them.
- Bukkit event timing is difficult to reproduce in a unit test, so pure encounter-selection, scaling, and authorization helpers are the practical regression surface.

## Downstream Guidance

- Requirements should account for: unrestricted player damage to initial dragons, all-online-player initial health scaling, exact-world and inclusive 200-block respawn selection, matching respawn health/participant snapshots, and a one-player health floor.
- Planning should consider: consolidating health math and respawn selection in `DragonManager`, then testing the pure rules without requiring a running Bukkit server.
- Do not include: dynamic mid-fight health rescaling, broader dragon balance changes, reward/XP changes, or changes to non-dragon boss ownership.
