# Overview

> **Issue:** Ender Dragon fight buffs
> **Classification Type:** T3
> **Severity:** Medium

## Goal

Extend the custom Ender Dragon encounter with explosive stunning endermites, a rare high-damage ground slam, a 50%-health flight-suppression phase, and a non-stacking regenerator enderman, while cleaning up all temporary encounter state safely.

## Approach

Keep per-tick combat state and spawned helpers inside `DragonBoss`, where entity operations already run synchronously. Correct first-fight participant initialization in `DragonManager`, expose small deterministic helpers/constants for unit coverage, and validate the entity-heavy presentation manually on a test server.

## Key Files

| File | Purpose |
| ---- | ------- |
| `src/main/java/sir_draco/survivalskills/bosses/DragonBoss.java` | Own attack selection, slam state, minions, regeneration, Wrath, and cleanup. |
| `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/DragonManager.java` | Initialize first-dragon participants. |
| `src/main/java/sir_draco/survivalskills/skill_listeners/FightingSkill.java` | Expose active Wrath suppression to commands. |
| `src/main/java/sir_draco/survivalskills/commands/skill_commands/FlightCommand.java` | Deny flight activation during Wrath without consuming a timer. |
| `src/test/java/sir_draco/survivalskills/bosses/DragonBossTest.java` | Cover deterministic thresholds, healing, and encounter state helpers. |

## Dependencies / Prerequisites

- Implement participant initialization before relying on target-restricted attacks during the first fight.
- Preserve the existing local first-dragon attribute change in `DragonManager.java`.

## Risks / Open Questions

- Vanilla dragon phase AI may fight velocity, so the slam requires per-tick velocity guidance plus impact/timeout bounds.
- Entity visuals and exact feel require manual server validation even when deterministic calculations are unit tested.
- Players added after Wrath starts must have their flight state captured before suppression so cleanup remains reversible.
