# Validation

## Automated Checks

```bash
# Compile
mvn compile

# Focused unit tests
mvn test -Dtest="DragonManagerTest"

# Full unit suite
mvn test
```

## Manual Verification Steps

1. Reset the first dragon, start a server with multiple online players, and have one player enter the End before the others.
    - Expected: Initial maximum health is 250 times the online server count at attachment, and every later-arriving player can damage it.

2. Respawn the dragon with one player near the center and another player in the same End world more than 200 blocks away.
    - Expected: Maximum health is 250, the nearby player can damage it, and the distant player cannot.

3. Respawn the dragon with several players within 200 blocks, including one near the boundary.
    - Expected: Maximum health is 250 times the nearby count and each snapshot participant can damage it.

4. Move a previously distant player within 200 blocks after respawn.
    - Expected: The player remains unable to damage that respawn because encounter membership is captured at spawn.

## Build / Compilation

```bash
mvn package
```

## Common Pitfalls

- Using End environment equality alone would incorrectly include players in a different End world.
- Recomputing proximity during damage would make health scaling and authorized participants describe different groups.
- Adding portal entrants to respawns would restore the prior first-arrival ownership behavior instead of preserving the spawn snapshot.
