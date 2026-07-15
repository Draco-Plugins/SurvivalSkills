# Task: Isolate and clean up vein miner

## Classification

Type: T2: moderate cross-listener state fix
Reasoning: Two files coordinate metadata, held-item detection, active-state admission, and cleanup. Blast Radius=2, Uncertainty=1, Behavior=3, Testing=2, Reversibility=1. Total=9.

## Goal

Prevent drill-related breaks from starting vein miner and ensure insufficient hunger or failures never retain vein-miner state that blocks later drills.

## Files to Modify

| File | Action (create/update/delete) |
| --- | --- |
| `src/main/java/sir_draco/survivalskills/skill_listeners/MiningSkill.java` | update |
| `src/main/java/sir_draco/survivalskills/abilities/VeinMinerAsync.java` | update |

## Step-by-Step Instructions

### 1. Reject drill-related vein-miner starts

**File:** `src/main/java/sir_draco/survivalskills/skill_listeners/MiningSkill.java`

- Return before vein-miner launch when the drill synthetic-event metadata exists or the held item is the power drill.
- Reject a second active vein-miner task for the same UUID.
- Schedule vein preparation synchronously and clean up if scheduling fails.

### 2. Centralize vein state cleanup

**File:** `src/main/java/sir_draco/survivalskills/abilities/VeinMinerAsync.java`

- Use one idempotent cleanup method for the tracker, metadata, and active UUID.
- Call cleanup for insufficient hunger, preparation failure, batch failure, and normal completion.
- Scope vein synthetic-event metadata with `try/finally`.

## Edge Cases to Handle

- Insufficient hunger before a runner exists.
- Empty veins.
- Exceptions during event dispatch.
- Repeated cleanup calls.

## Related Files (read-only context)

- `src/main/java/sir_draco/survivalskills/commands/skill_commands/VeinminerCommand.java`

