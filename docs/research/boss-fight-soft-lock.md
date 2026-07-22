# Research: Boss Fight Summoner Soft Lock

## Overview

Egg-spawned bosses are created and registered to a summoning player by `BossManager`, while `FightingSkill` routes Bukkit damage and death events back to that manager. The lock is an in-memory `Map<Player, Boss>` entry; cleanup currently depends on the death event taking a path that removes the correct player key.

## Issue Context

- User/requested outcome: Only the summoner should damage their boss, every boss death should release the summoner's lock, and a later summon attempt should recover if its tracked boss is already dead.
- Current pain or bug: A boss killed by another player, and potentially by environmental damage such as fire ticks, can leave the summoner unable to summon another boss until restart.
- Scope classification, if known: T3

## Current Behavior

- `BossManager` stores one active boss per `Player` in `summonTracker` and rejects another summon whenever the player is still a key in that map (`src/main/java/sir_draco/survivalskills/skill_listeners/fighting/BossManager.java:72`, `:240`).
- A normal boss kill removes `summonTracker.remove(p)`, where `p` is the entity's Bukkit-reported killer rather than the player whose tracked boss entity died (`BossManager.java:152`). A different player's kill therefore removes the wrong key and leaves the summoner locked.
- The non-player-killer path searches the tracker by boss entity, but mutates the map directly while iterating its entries (`BossManager.java:194`).
- Summoner ownership is enforced only for villager bosses. Giant and BroodMother damage events pass through `handleBossDamageByCorrectPlayer` without an ownership check (`BossManager.java:371`, `:387`).
- Environmental damage is selectively blocked for a few entity type/cause combinations; fire and most other non-player causes are not generally blocked (`BossManager.java:299`).
- Boss runnables cancel themselves when their entity is dead, but Giant and BroodMother do not notify `BossManager` or clear `summonTracker` (`src/main/java/sir_draco/survivalskills/bosses/GiantBoss.java:51`, `src/main/java/sir_draco/survivalskills/bosses/BroodMotherBoss.java:41`).
- Boss metadata is applied on a delayed task in `Boss.applyAttributes`, while `BossManager.isBoss` currently recognizes only that metadata. Inference: a death before the delayed metadata task runs may not enter boss cleanup (`src/main/java/sir_draco/survivalskills/bosses/Boss.java:93`, `BossManager.java:102`).

## Relevant Files And Entry Points

- `src/main/java/sir_draco/survivalskills/skill_listeners/FightingSkill.java:95` - Routes entity death events based on killer presence and boss recognition.
- `src/main/java/sir_draco/survivalskills/skill_listeners/FightingSkill.java:239` - Routes generic and entity-caused boss damage events.
- `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/BossManager.java:72` - Owns the per-player summon lock and boss registries.
- `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/BossManager.java:136` - Removes a boss wrapper from its type registry and runs boss death cleanup.
- `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/BossManager.java:152` - Handles rewarded boss deaths.
- `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/BossManager.java:194` - Handles deaths without a player killer.
- `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/BossManager.java:240` - Rejects or creates an egg-spawned boss.
- `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/BossManager.java:299` - Applies environmental damage immunities.
- `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/BossManager.java:371` - Resolves direct and projectile player attackers.
- `src/main/java/sir_draco/survivalskills/bosses/Boss.java:226` - Removes the entity, boss bar, and runnable during cleanup.

## Data Flow Or Control Flow

1. A player right-clicks with a custom boss summon item; `FightingSkill.onPlayerInteract` resolves the `BossType` and calls `BossManager.spawnBoss`.
2. `spawnBoss` checks `summonTracker.containsKey(player)`, creates the boss entity, starts its runnable, registers it in both the tracker and type registry, and consumes the item.
3. Damage events pass through `handleBossDamage` and `handleBossDamageByCorrectPlayer`; only the villager/Exiled One currently compares the attacker against `summonTracker`.
4. `FightingSkill.onKillEntity` gets the Bukkit killer. A null killer uses `handleUnnaturalBossDeath`; a recognized boss with a killer uses `handleBossKill`.
5. `handleBossKill` removes the killer's tracker key, grants drops/XP, and removes the boss wrapper. If killer and summoner differ, the summoner's map entry survives even though its entity is dead.
6. A subsequent summon checks only map membership and refuses the summon without validating the tracked entity's liveness.

## Important Contracts And Constraints

- Each summoning player may have at most one active egg-spawned boss.
- Ender Dragon lifecycle and damage rules are managed separately by `DragonManager` and are not backed by `summonTracker`.
- Successful boss kills grant the existing boss item, optional special drops, broadcast, Fighting XP, and zero vanilla dropped XP.
- Player death cleanup must retain the Exiled One music/minion/slowness handling in `handlePlayerBossCleanup`.
- Projectile ownership must continue to resolve the player shooter rather than treating the projectile entity as the attacker.

## Existing Tests And Validation

- No existing tests target `BossManager`, summon tracking, boss damage ownership, or boss death cleanup.
- Maven uses JUnit 5 and Mockito (`pom.xml`); focused manager tests can mock Bukkit entities/events without a live server for state-only paths.
- Existing validation surface: `mvn test` and `mvn package` as defined by the Maven project.

## Risks, Edge Cases, And Unknowns

- A tracked entity can be dead, removed/invalid, or absent from metadata before delayed initialization; boss recognition and stale-lock cleanup should not rely only on metadata.
- Removing an entry while iterating `summonTracker.entrySet()` should use the iterator or a lookup followed by removal outside traversal.
- Non-player entity damage, projectile damage without a player shooter, and environmental damage all need a consistent policy under the summoner-only requirement.
- Unknown: external plugins may directly modify health without a cancellable Bukkit damage event. Death cleanup therefore still needs to be correct independently of damage gating.

## Downstream Guidance

- Requirements should account for: Entity-based owner lookup, summoner-only direct/projectile damage, cancellation of non-summoner and environmental damage, cleanup independent of killer identity, and stale tracker recovery before rejecting a summon.
- Planning should consider: Centralizing tracker removal by boss entity, preserving separate Ender Dragon behavior, and testing state transitions directly in `BossManager`.
- Do not include: Changes to boss rewards, boss combat balance, Ender Dragon ownership, summon item recipes, or persistent storage.
