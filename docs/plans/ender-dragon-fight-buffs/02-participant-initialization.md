# Task: Initialize first-dragon participants

## Classification

Type: T1: limited lifecycle correction
Reasoning: A focused change in one production file and one test file using the existing participant API. Blast Radius=1, Uncertainty=0, Behavior=2, Testing=1, Reversibility=1. Total=5.

## Goal

Ensure the first custom dragon encounter records online players in the End without changing respawn-only damage-gating semantics.

## Files to Modify

| File | Action (create/update/delete) |
| ---- | ----------------------------- |
| `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/DragonManager.java` | update |
| `src/main/java/sir_draco/survivalskills/bosses/DragonBoss.java` | update |

## Step-by-Step Instructions

### 1. Add non-respawn participant initialization

**File:** `src/main/java/sir_draco/survivalskills/bosses/DragonBoss.java`

Add an idempotent `initializePlayers(List<Player> players)` method that adds missing players without setting `isRespawn`. Refactor `respawnDragonInitPlayers` to set the respawn flag and delegate to it.

### 2. Populate the first encounter

**File:** `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/DragonManager.java`

In the delayed first-dragon attach runnable, collect online players whose world environment is `THE_END` and pass the immutable result to `initializePlayers` before starting/continuing target-based combat. Preserve existing health scaling and the user-owned damage/defense/speed arguments.

## Edge Cases to Handle

- No players remain in the End when the delayed attach runs.
- Repeated portal activity must not duplicate a participant.
- Initial encounters must keep `isRespawn == false`.

## Related Files (read-only context)

- `src/main/java/sir_draco/survivalskills/skill_listeners/FightingSkill.java` - Event routing.

