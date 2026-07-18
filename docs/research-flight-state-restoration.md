# Research: Timed Flight Returns After Login

## Overview

Timed flight uses an in-memory `AbilityTimer`, persists that timer to `playerdata.yml`, and restores it during player login. Bukkit also persists the player's own flight flags, creating a second state source that the login path did not reconcile when no active timed flight existed.

## Issue Context

- User/requested outcome: Expired timed flight must remain disabled after logging out and back in.
- Current pain or bug: A player can regain flight on login, and `/flight` then identifies that stale state as enabled and toggles it off.
- Scope classification, if known: T3

## Current Behavior

- `FlightCommand` enables both Bukkit flight flags and starts a timed `AbilityTimer` for Flight I-III.
- Natural expiry disables Bukkit flight through the timer's expiry callback.
- Logout saves a remaining timer or removes its YAML section when the timer has completed.
- Login restores an active saved timer, but previously made no change to Bukkit flight flags when there was no timer to restore.
- Inference: Bukkit player persistence can restore an earlier `allowFlight` value, leaving the command's timer state and the player's flight state inconsistent.

## Relevant Files And Entry Points

- `src/main/java/sir_draco/survivalskills/commands/skill_commands/FlightCommand.java` - Enables, toggles, expires, and schedules flight.
- `src/main/java/sir_draco/survivalskills/abilities/AbilityTimer.java` - Transitions active abilities into cooldown and invokes flight callbacks.
- `src/main/java/sir_draco/survivalskills/abilities/AbilityManager.java` - Saves and restores flight timer state.
- `src/main/java/sir_draco/survivalskills/skill_listeners/PlayerListener.java` - Orders save-before-cleanup on quit and triggers player loading on join.
- `src/main/java/sir_draco/survivalskills/utils/FileUtils.java` - Reads and writes the flight section in `playerdata.yml`.

## Data Flow Or Control Flow

1. `/flight` sets `allowFlight`, starts flying, and registers a timer.
2. Timer expiry invokes the flight callback and transitions the timer into cooldown.
3. Once cooldown completes, the timer is removed; logout consequently removes the persisted flight section.
4. Login loads rewards and then asks `AbilityManager` to restore flight.
5. With no saved timer, the old implementation returned without reconciling Bukkit's player flight flags.

## Important Contracts And Constraints

- Active timed flight must be restored with its saved remaining time and speed.
- Flight IV is an unlimited user-controlled toggle and has no timer to restore.
- Creative and spectator game modes own their native flight state and must not be disabled by timed-flight cleanup.
- Bukkit entity and player mutations must run on the server thread.

## Existing Tests And Validation

- No existing tests covered flight login state reconciliation.
- Maven's JUnit and Mockito setup supports isolated player-state tests.

## Risks, Edge Cases, And Unknowns

- Existing affected player data may already contain stale Bukkit flight flags; login reconciliation must heal those players.
- A valid active timer must re-enable flight after stale state is cleared.
- Unlimited-flight and native game-mode flight must remain untouched.

## Downstream Guidance

- Requirements should account for: one authoritative timed-flight state on login while preserving Flight IV and native flight modes.
- Planning should consider: login tests for absent timers and protected flight cases, plus main-thread timer scheduling.
- Do not include: changes to unrelated ability timers or redesign of the complete ability persistence format.
