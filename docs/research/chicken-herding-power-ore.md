# Research: Chicken Herding Power Ore Mini-Game

## Overview

Power Ore conversion creates one randomly selected `PowerOreTask`, registers it by ore location and player, and delegates completion, failure, and cleanup to `PowerOreChallenge`. Existing tasks include entity combat, a world scavenger hunt, and two inventory games; no existing task tracks passive mobs crossing the converting obsidian.

## Issue Context

- User/requested outcome: Add a Chicken Herding conversion game that spawns 40 named chickens within 25 blocks, makes them move 50% faster, supplies wheat seeds, tracks chickens remaining in the action bar, removes chickens reaching the converting obsidian, and fails after 60 seconds.
- Current pain or bug: This mini-game does not exist.
- Scope classification, if known: T3

## Current Behavior

- `PowerOreChallenge` selects a concrete task with a switch over `TaskType`; the random selection uses every enum value with equal probability (`src/main/java/sir_draco/survivalskills/god_questline/powerore/PowerOreChallenge.java:23`).
- Starting a challenge sends the selected task name, starts the common ore visual effect, and calls `PowerOreTask.start()` (`src/main/java/sir_draco/survivalskills/god_questline/powerore/PowerOreChallenge.java:51`).
- Completion and failure both call the task's cleanup method. Failure also stops visuals and unregisters the conversion, while success leaves the charged obsidian registered until its owner mines it (`src/main/java/sir_draco/survivalskills/god_questline/powerore/PowerOreChallenge.java:57`).
- The entity-based sentinel task owns and removes its spawned entity and scheduled work during cleanup (`src/main/java/sir_draco/survivalskills/god_questline/powerore/PowerOreMiniBossTask.java:155`).
- The scavenger hunt demonstrates surface-location generation around the ore and a wall-clock timeout, but it uses chat messages rather than an action bar (`src/main/java/sir_draco/survivalskills/god_questline/powerore/PowerOreScavengerHuntTask.java:39`).
- `Utils.sendActionBarMessage` is the repository's existing action-bar integration (`src/main/java/sir_draco/survivalskills/utils/Utils.java:100`).

## Relevant Files And Entry Points

- `src/main/java/sir_draco/survivalskills/skill_listeners/god/PowerOreChallengeListener.java:331` - Deducts levels, randomly selects a task, registers it, and starts it.
- `src/main/java/sir_draco/survivalskills/god_questline/powerore/PowerOreChallenge.java:23` - Declares task types and constructs task implementations.
- `src/main/java/sir_draco/survivalskills/god_questline/powerore/PowerOreTask.java:9` - Defines the lifecycle contract for conversion tasks.
- `src/main/java/sir_draco/survivalskills/god_questline/powerore/PowerOreMiniBossTask.java:51` - Existing entity-owning task and cleanup precedent.
- `src/main/java/sir_draco/survivalskills/god_questline/powerore/PowerOreScavengerHuntTask.java:39` - Existing radius-based surface placement and timeout precedent.
- `src/main/java/sir_draco/survivalskills/utils/Utils.java:100` - Existing utility for legacy-colored action-bar messages.
- `src/test/java/sir_draco/survivalskills/god_questline/powerore/PowerOreChallengeTest.java:13` - Verifies enum task types construct the intended implementation.

## Data Flow Or Control Flow

1. A qualified player zaps the obsidian beneath them with the Zap Wand; the listener validates the forge and spends 50 experience levels.
2. The listener chooses a random `TaskType`, creates a `PowerOreChallenge`, registers it by location and player UUID, and calls `start()`.
3. The concrete task creates its world entities or inventory and owns any scheduled task state.
4. The concrete task calls `PowerOreChallenge.complete()` or `fail(reason)`; both paths invoke concrete-task cleanup.
5. Success leaves charged obsidian for the owner to mine. Failure unregisters the conversion immediately.

## Important Contracts And Constraints

- A task must call its parent challenge's completion or failure method and must make cleanup safe when scheduled work is already cancelled.
- Spawned gameplay entities must be owned by the task and removed on both success and failure so they do not survive plugin gameplay state.
- Power Ore successes remain registered and persisted only as completed ore; running task state is not persisted across shutdown.
- Bukkit world/entity/inventory operations and scheduled gameplay work run on the server thread.
- The working tree already contains an uncommitted Memory Match task and enum entry that must remain intact.

## Existing Tests And Validation

- `src/test/java/sir_draco/survivalskills/god_questline/powerore/PowerOreChallengeTest.java:13` - Covers construction for the in-progress Memory Match task only.
- `src/test/java/sir_draco/survivalskills/god_questline/powerore/PowerOreMemoryMatchGameTest.java:23` - Covers pure Memory Match game state, unrelated to world entity handling.
- Missing coverage: chicken task construction, countdown/progress state, and task-owned entity cleanup.
- Maven is the discovered build and test surface through `pom.xml`.

## Risks, Edge Cases, And Unknowns

- Random surface candidates can be unsuitable for spawning; setup needs a bounded retry policy and must fail cleanly if all 40 chickens cannot be created.
- Chickens may die, unload, or otherwise disappear before reaching the ore. The requested outcome only defines success on reaching the obsidian and timeout failure, so missing chickens should not count as delivered.
- Player logout does not currently fail general Power Ore challenges; a running task remains registered until it resolves by its own lifecycle.
- Ordinary wheat seeds are indistinguishable from a player's existing seeds, so cleanup cannot safely reclaim only the supplied seed unless it is custom-tagged.
- It is unspecified whether the action bar should show time as well as chicken count. Showing both makes the explicitly requested running timer observable without changing success rules.

## Downstream Guidance

- Requirements should account for: exactly 40 identifiable task chickens, radius-bounded surface spawns, a wheat-seed handoff with inventory overflow handling, action-bar count and time, delivery only at the converting obsidian, 60-second failure, and full entity/task cleanup.
- Planning should consider: a dedicated `PowerOreTask`, one new `TaskType` switch arm, pure helper methods for unit-testable progress/countdown rules, and focused Maven tests.
- Do not include: changes to conversion cost, reward behavior, persistence format, or unrelated existing mini-games.
