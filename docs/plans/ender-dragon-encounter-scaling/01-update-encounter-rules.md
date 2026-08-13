# Task: Update Dragon Encounter Rules

## Classification

Type: T2: moderate state and event-lifecycle change
Reasoning: The task changes one production coordinator but affects spawn state, combat authorization, and downstream boss targeting. Blast Radius=2, Uncertainty=1, Behavior=3, Testing=2, Reversibility=1. Total=9.

## Goal

Define explicit and testable initial-versus-respawn participant, health, and damage rules in the dragon lifecycle coordinator.

## Files to Modify

| File | Action (create/update/delete) |
| ---- | ----------------------------- |
| `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/DragonManager.java` | update |

## Step-by-Step Instructions

### 1. Centralize dragon health scaling

**File:** `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/DragonManager.java`

- Add a package-visible pure helper that returns `DRAGON_BASE_HEALTH * max(1, playerCount)`.
- Use it for both initial attachment and respawn attachment.

### 2. Select respawn participants by exact world and radius

**File:** `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/DragonManager.java`

- Add a 200-block respawn radius constant.
- Add a package-visible pure helper accepting the dragon spawn `Location` and online player collection.
- Keep players only when they are in the exact world, that world is an End environment, and their squared distance is at most the squared radius.
- Return an immutable snapshot with `Stream.toList()`.
- Pass that same snapshot to health calculation and `respawnDragonInitPlayers`.

### 3. Make initial encounter scope server-wide

**File:** `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/DragonManager.java`

- Snapshot all online players in the delayed initial attachment task.
- Use its size for health and pass it to `DragonBoss.initializePlayers`.
- Keep later initial-fight End entrants addable without changing maximum health.
- Do not add later portal entrants to respawn snapshots.

### 4. Express damage authorization explicitly

**File:** `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/DragonManager.java`

- Add a package-visible pure helper that always permits player damage for a non-respawn and requires participant membership for a respawn.
- Use the helper from `handleDragonDamageByCorrectPlayer`.
- Return immediately after rejecting an event for a dragon other than the tracked entity.

## Edge Cases to Handle

- Zero online players or zero nearby respawn players still yields 250 maximum health.
- A player exactly 200 blocks away is included.
- A nearby player in another End world is excluded.
- A player in a non-End world is excluded.
- A player who was not in the respawn snapshot cannot join it through a later portal teleport.

## Related Files (read-only context)

- `src/main/java/sir_draco/survivalskills/bosses/DragonBoss.java` - stores participants and marks respawn encounters.
- `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/BossManager.java` - resolves player attackers before delegating authorization.
