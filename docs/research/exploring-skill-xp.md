# Research: Exploring Skill XP Is Not Awarded Properly

## Overview

Exploring XP is accumulated by the `ExploringSkill` movement listener and awarded through the shared `SkillManager.experienceEvent` pipeline in batches of 100 traveled blocks. The current batching discards distance beyond the first threshold, while the quit path applies remaining XP through a separate lower-level path and leaves the listener state cached.

## Issue Context

- User/requested outcome: Fix exploring skill XP so that it is awarded properly.
- Current pain or bug: Traveled distance can be lost at an XP threshold, and remaining distance can be applied inconsistently or repeatedly across player sessions.
- Scope classification, if known: T2

## Current Behavior

- `src/main/java/sir_draco/survivalskills/skill_listeners/ExploringSkill.java:78` tracks the player's last block location and an integer distance counter.
- `src/main/java/sir_draco/survivalskills/skill_listeners/ExploringSkill.java:98` awards exactly 100 blocks of XP when the counter reaches the threshold, then resets the counter to zero. Any remainder and any additional complete 100-block batches are discarded.
- `src/main/java/sir_draco/survivalskills/skill_listeners/PlayerListener.java:96` invokes a separate quit-time update before saving player data.
- `src/main/java/sir_draco/survivalskills/skills/SkillManager.java:341` applies quit-time exploring XP directly to `Skill.changeExperience`, bypassing global/voucher multipliers, notifications, rewards, main-skill synchronization, leaderboards, and scoreboards. The listener's cached counter and location are not cleared afterward.

## Relevant Files And Entry Points

- `src/main/java/sir_draco/survivalskills/skill_listeners/ExploringSkill.java:65` - Vehicle movement entry point.
- `src/main/java/sir_draco/survivalskills/skill_listeners/ExploringSkill.java:73` - Player movement entry point.
- `src/main/java/sir_draco/survivalskills/skill_listeners/ExploringSkill.java:78` - Shared distance accumulation and XP batching.
- `src/main/java/sir_draco/survivalskills/skill_listeners/PlayerListener.java:94` - Player quit entry point.
- `src/main/java/sir_draco/survivalskills/skills/SkillManager.java:58` - Standard skill XP award pipeline.
- `src/main/resources/config.yml:9` - Default per-block exploring XP value.

## Data Flow Or Control Flow

1. A `PlayerMoveEvent` or occupied `VehicleMoveEvent` calls `ExploringSkill.handleMovement`.
2. The current block location is compared with the last tracked location; same-block events return without adding distance.
3. Cross-block distance is rounded up and added to the player's in-memory counter.
4. At 100 tracked blocks, `SkillManager.experienceEvent` applies configured exploring XP and shared multipliers, then updates level-related side effects.
5. On quit, residual tracked blocks are currently applied directly to the exploring `Skill`, after which player data is saved.

## Important Contracts And Constraints

- Same-block movement events must not add exploring distance.
- The configured `ExploringXP` value is XP per tracked block; batching is an implementation detail and must not lose accumulated distance.
- All awards should use the shared XP pipeline so caps, multipliers, rewards, main-skill synchronization, leaderboards, and UI updates remain consistent.
- Existing unrelated magnet edits in `ExploringSkill.java` are user-owned working-tree changes and must be preserved.

## Existing Tests And Validation

- `docs/to-test.md:2` explicitly lists same-block exploring XP as a manual validation item.
- No automated exploring movement/XP tests currently exist.
- Maven test and package phases are the repository's available automated validation surfaces.

## Risks, Edge Cases, And Unknowns

- One movement update may cross multiple 100-block thresholds, such as fast vehicle movement or teleport-like movement.
- Switching between distinct worlds with the same environment must not call Bukkit's cross-world `Location.distance` operation.
- Cached residual distance must be removed after it is flushed at logout to prevent repeated awards and stale-location distance on rejoin.
- It is unknown whether command/plugin teleports are intended to count as exploration; this research does not expand scope to redesign teleport handling.

## Downstream Guidance

- Requirements should account for: preserving all tracked distance, ignoring same-block events, consistently applying the standard XP pipeline, and clearing session state on quit.
- Planning should consider: extracting deterministic batch/remainder calculation for unit coverage and manually validating ordinary movement, large movement increments, logout residuals, and world changes.
- Do not include: unrelated movement, swimming, magnet, reward, or teleport-system refactors.
