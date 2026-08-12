# Validation

## Automated Checks

```bash
mvn test
mvn package "-Dshade.skip=true"
```

## Manual Verification Steps

1. Start a first dragon fight and a respawned dragon fight with flight-capable players.
    - Expected: all four mechanics can target participants in both fight types.
2. Allow an explosive endermite to approach within three blocks.
    - Expected: a small block-safe explosion removes it, movement/jumping is suppressed for one second, and the action bar says `You have been stunned!`.
3. Force/repeat attacks until a ground slam occurs.
    - Expected: the dragon dives, a large visible shockwave occurs on impact, in-range participants take 30 damage and knockback, and out-of-range players are untouched.
4. Reduce the dragon to 50% health while flight is active and attempt `/flight` during Wrath.
    - Expected: the announcement plays once, flight immediately stops and cannot remain enabled; legitimate pre-fight flight flags return after dragon death.
5. Force/repeat attacks until a regenerator appears.
    - Expected: it is visibly smaller, has 75 health and 10 damage, no duplicate appears while alive, and the dragon heals 1% every five seconds without exceeding max health.
6. Kill or otherwise end the encounter while helpers remain.
    - Expected: helper mobs are removed, healing stops, and suppressed flight state is restored.

## Build / Compilation

```bash
mvn package "-Dshade.skip=true"
```

## Common Pitfalls

- `mvn package` shades to an external test-server path unless shading is skipped.
- Velocity must be refreshed during the slam because vanilla dragon AI can override a one-time vector.
- Flight restoration must never run continuously while Wrath is active.
