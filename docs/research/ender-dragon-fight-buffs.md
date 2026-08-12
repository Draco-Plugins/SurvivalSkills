# Research: Ender Dragon Fight Buffs

## Overview

The custom Ender Dragon encounter is managed by `DragonManager` for lifecycle and participant tracking and by the tick-based `DragonBoss` task for stages, cooldowns, and attacks. Flight is owned by separate ability, command, login, and respawn paths, so suppressing it during the encounter crosses combat and ability systems. The requested minions, ground slam, mid-fight power suppression, and healing add do not currently exist.

## Issue Context

- User/requested outcome: Add proximity-exploding endermites that stun players, a rare 30-damage ground slam, Dragon's Wrath flight suppression at 50% health, and an occasional scaled regenerator enderman that heals the dragon.
- Current pain or bug: The existing custom dragon only uses lightning, cannon, death-rain, and angry-enderman attacks and does not suppress player powers.
- Scope classification, if known: T3

## Current Behavior

- `src/main/java/sir_draco/survivalskills/skill_listeners/FightingSkill.java:266` routes End portal teleports and dragon spawns into a dedicated `DragonManager`.
- `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/DragonManager.java:111` attaches a `DragonBoss` to the first dragon after a one-second delay; the current first-dragon path does not add the players already in the End to the boss participant list.
- `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/DragonManager.java:155` attaches respawned dragons immediately and initializes their participant list from online players in the End.
- `src/main/java/sir_draco/survivalskills/bosses/DragonBoss.java:65` runs every tick, updates phase transitions and cooldowns, and dispatches lightning and one normal attack when their counters expire.
- `src/main/java/sir_draco/survivalskills/bosses/DragonBoss.java:131` selects cannon or death rain in stages one and two; stage three also allows five combat-focused "Dragon Worshipper" endermen.
- `src/main/java/sir_draco/survivalskills/bosses/DragonBoss.java:91` delegates health thresholds to the shared three-stage `Boss` implementation and only performs special work on entry to stage three.
- `src/main/java/sir_draco/survivalskills/commands/skill_commands/FlightCommand.java:35` can grant timed or unlimited flight, while `AbilityManager.loadFlight` and `FlightRespawnListener` can restore active flight independently.
- Inference: enforcing Dragon's Wrath only in `FlightCommand` would be incomplete because login and respawn restoration can set the Bukkit flight flags without invoking the command.

## Relevant Files And Entry Points

- `src/main/java/sir_draco/survivalskills/bosses/DragonBoss.java:65` - Per-tick encounter state, attack selection, stage transitions, targeting, and death cleanup.
- `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/DragonManager.java:111` - First-dragon attachment, respawn attachment, participant ownership, and boss reference lifecycle.
- `src/main/java/sir_draco/survivalskills/skill_listeners/FightingSkill.java:266` - Bukkit listener entry points for the dragon lifecycle.
- `src/main/java/sir_draco/survivalskills/commands/skill_commands/FlightCommand.java:35` - Player-triggered flight activation and toggling.
- `src/main/java/sir_draco/survivalskills/abilities/AbilityManager.java:129` - Login restoration of persisted timed flight.
- `src/main/java/sir_draco/survivalskills/skill_listeners/FlightRespawnListener.java:19` - Respawn restoration of active timed flight.
- `src/main/java/sir_draco/survivalskills/SurvivalSkills.java:214` - Construction and listener registration for the relevant systems.

## Data Flow Or Control Flow

1. A player uses an End portal or a dragon respawns, and `FightingSkill` forwards the Bukkit event to `DragonManager`.
2. `DragonManager` finds the dragon, scales its health, attaches a `DragonBoss`, starts its one-tick scheduler, and, on respawns, records current End players.
3. `DragonBoss.run()` enforces altitude, initializes crystal locations, evaluates stage changes, decrements cooldowns, and triggers attacks.
4. Attacks directly spawn entities/projectiles or mutate the End world; target lookup is limited to players recorded by the boss.
5. Dragon death flows through `BossManager` into `DragonManager.handleDragonKill`, which runs `DragonBoss.deathAnimation()`, cancels the task, clears the manager reference, and awards XP.

## Important Contracts And Constraints

- Bukkit entity and world mutations must run on the main server thread; `DragonBoss` is currently a synchronous `BukkitRunnable`.
- Attack targeting is intended to be restricted to encounter participants, especially for respawned dragons where nonparticipants cannot damage the boss.
- The boss health is dynamically scaled by player count, so percentage-based healing must use the dragon's configured maximum health rather than a fixed base value.
- Existing flight may be timed, unlimited, creative, or spectator-owned. Encounter suppression must prevent re-enabling while avoiding permanent loss of legitimate post-fight flight state.
- Spawned helper mobs and child tasks need explicit cleanup when the dragon dies or its task ends to prevent orphaned healing or explosion behavior.
- The working tree already changes first-dragon combat attributes in `DragonManager`; that user-owned change must remain intact.

## Existing Tests And Validation

- `src/test/java/sir_draco/survivalskills/skill_listeners/fighting/BossManagerTest.java:1` covers generic boss recognition, damage gates, death cleanup, and stale boss state but not dragon attacks.
- `src/test/java/sir_draco/survivalskills/abilities/AbilityManagerFlightTest.java:1` covers login flight cleanup and preservation for creative/unlimited flight.
- `src/test/java/sir_draco/survivalskills/skill_listeners/FlightRespawnListenerTest.java:1` covers active timed-flight restoration after respawn.
- No existing test directly covers `DragonBoss` attack probabilities, spawned minion attributes, percentage healing, participant initialization, or encounter flight suppression.
- Maven/JUnit is the discovered repository validation surface through `pom.xml`.

## Risks, Edge Cases, And Unknowns

- A velocity-only dive may be overridden by the vanilla dragon phase controller; the slam needs a bounded state and landing/timeout detection.
- A Bukkit explosion alone does not identify exactly which players should receive a custom stun; proximity should be evaluated at detonation and terrain damage should remain disabled.
- Minecraft has no single generic "stunned" flag, so immobilization requires a defined combination of effects or movement enforcement for one second.
- The phrase "5% chance per attack" is ambiguous as to whether the regenerator replaces the selected attack or is an independent additional spawn roll.
- Fight participation for the initial dragon is currently incomplete, which would prevent proximity-targeted mechanics from finding the player unless corrected.
- Players joining, leaving, dying, changing game mode, or disconnecting while Wrath is active require restoration and cleanup decisions.

## Downstream Guidance

- Requirements should account for: independent 5% regenerator rolls per normal attack, low-damage block-safe endermite explosions, exact one-second stun feedback, one-time Wrath activation at 50%, continuous flight enforcement, post-fight restoration, maximum-health-based healing, and child-entity cleanup.
- Planning should consider: keeping encounter-owned behavior in `DragonBoss`, fixing participant initialization in `DragonManager`, using testable package-private helpers for chance/health calculations, and adding focused Mockito tests without requiring a live server.
- Do not include: unrelated boss refactors, general flight-system redesign, configuration version changes (no skill reward or `config.yml` entry is requested), or changes to non-dragon endermen/endermites.
