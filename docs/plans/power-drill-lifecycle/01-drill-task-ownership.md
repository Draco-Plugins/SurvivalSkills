# Task: Add concurrent drill task ownership

## Classification

Type: T2: moderate concurrency lifecycle change
Reasoning: Two production files share a new ownership contract. Blast Radius=2, Uncertainty=1, Behavior=5, Testing=2, Reversibility=1. Total=11.

## Goal

Permit multiple concurrent drills per player while ensuring each task owns and removes only its own state and all tasks stop on disconnect.

## Files to Modify

| File | Action (create/update/delete) |
| --- | --- |
| `src/main/java/sir_draco/survivalskills/abilities/items/PowerDrillTask.java` | update |
| `src/main/java/sir_draco/survivalskills/skill_listeners/god/PowerOreChallengeListener.java` | update |

## Step-by-Step Instructions

### 1. Make the drill a single synchronous owned runner

**File:** `src/main/java/sir_draco/survivalskills/abilities/items/PowerDrillTask.java`

- Capture direction and tool on the event/main thread, then create an immutable list of lightweight integer-coordinate records without retaining Bukkit `Block` objects.
- Resolve coordinates lazily during each synchronous batch, skip unloaded chunks, and enforce both a nine-position cap and a 1.5 ms time budget per tick.
- Add a short-lived metadata marker around synthetic event dispatch and always remove it in `finally`.
- Make completion/failure/disconnect cleanup idempotent and unregister the exact task instance.
- Catch runtime failures, log them in the project format, and stop the task.

### 2. Track task sets concurrently

**File:** `src/main/java/sir_draco/survivalskills/skill_listeners/god/PowerOreChallengeListener.java`

- Replace `Map<Player, List<Block>>` with `ConcurrentHashMap<UUID, Set<PowerDrillTask>>` and concurrent key sets.
- Register before scheduling and remove only the completing task.
- Treat marked synthetic events as internal without relying on block ownership.
- On quit, remove and cancel every active task for the player's UUID.

## Edge Cases to Handle

- Two tasks finish on the same tick.
- Scheduling fails after registration.
- A player disconnects while several runners are active.
- Event dispatch or block breaking throws.

## Related Files (read-only context)

- `src/main/java/sir_draco/survivalskills/skill_listeners/GodListener.java`
