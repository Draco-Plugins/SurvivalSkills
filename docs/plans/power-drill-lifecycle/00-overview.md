# Overview

> **Issue:** Power drill concurrent-task lifecycle safety
> **Classification Type:** T3
> **Severity:** High

## Goal

Allow a player to run several power drills concurrently without synthetic events recursively starting drills or vein miners, while guaranteeing task/state cleanup on completion, failure, insufficient hunger, and disconnect.

## Approach

Replace the shared per-player block list with a concurrent per-player set of owned drill tasks. Mark synthetic drill events for the duration of event dispatch, run all Bukkit access on the main thread, cancel all owned tasks on quit, and centralize idempotent cleanup. Harden vein miner so power-drill use is excluded and all termination paths clear its transient state.

## Key Files

| File | Purpose |
| --- | --- |
| `src/main/java/sir_draco/survivalskills/abilities/items/PowerDrillTask.java` | Owned synchronous drill runner and synthetic-event marker |
| `src/main/java/sir_draco/survivalskills/skill_listeners/god/PowerOreChallengeListener.java` | Concurrent task registry and disconnect cancellation |
| `src/main/java/sir_draco/survivalskills/skill_listeners/MiningSkill.java` | Reject drill-originated and power-drill-held vein mining |
| `src/main/java/sir_draco/survivalskills/abilities/VeinMinerAsync.java` | Guaranteed vein-miner cleanup |
| `src/test/java/sir_draco/survivalskills/skill_listeners/MiningSkillTest.java` | Regression coverage for drill/vein exclusion |

## Dependencies / Prerequisites

- Uses the findings in `docs/research-power-drill-lifecycle.md`.
- Synthetic `BlockBreakEvent`s must continue through Bukkit's plugin manager for cancellation compatibility.

## Risks / Open Questions

- Scheduler behavior is difficult to unit test without a Bukkit server harness; lifecycle verification will include compilation/tests plus documented manual scenarios.

