# Validation

## Automated Checks

```bash
mvn compile
mvn test
mvn test -Dtest="MiningSkillTest"
```

## Manual Verification Steps

1. Rapidly start several drills with one player.
    - Expected: All finite tunnels complete independently with no recursively spawned drills.
2. Drill ore while sneaking with vein miner enabled.
    - Expected: No vein-miner expansion occurs.
3. Disconnect while several drills are active.
    - Expected: All drill work stops immediately.
4. Attempt vein miner without enough hunger, then use the drill.
    - Expected: The drill starts normally and no active flag remains.

## Build / Compilation

```bash
mvn package
```

## Common Pitfalls

- Removing all tasks when one task completes instead of unregistering by identity.
- Leaving event metadata set after an exception.
- Calling Bukkit world/player APIs from an async task.
