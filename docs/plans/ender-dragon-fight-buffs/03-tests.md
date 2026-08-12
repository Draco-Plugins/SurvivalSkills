# Tests

## Test Strategy

- Unit-test deterministic chance, radius, damage, duration, and healing helpers without a live Minecraft server.
- Use Mockito for player/entity state transitions where Bukkit static server state is unnecessary.
- Preserve existing ability and boss regression suites, then manually validate dragon AI presentation.

## Requirement Coverage

| Requirement / Acceptance Criteria | Test Coverage | Notes / Gaps |
| --------------------------------- | ------------- | ------------ |
| Endermite radius/stun | `DragonBossTest` boundary/helper tests | Explosion visuals need manual validation. |
| Ground slam rarity/damage | `DragonBossTest` constants/chance and eligibility tests | Dragon velocity/phase feel needs manual validation. |
| Wrath suppresses/restores flight | `DragonBossTest` mocked flight-state tests | Login during an active fight is manually checked. |
| Regenerator attributes/healing | `DragonBossTest` healing/cap and spawn-state tests | Entity spawning visuals need manual validation. |
| First-fight participants | `DragonBossTest` initialization tests | Delayed Bukkit runnable is manually integrated. |

## New Tests

| Test File | Test Name | Test Type | Requirement / Risk Covered | Key Assertions |
| --------- | --------- | --------- | -------------------------- | -------------- |
| `src/test/java/sir_draco/survivalskills/bosses/DragonBossTest.java` | `regenerationHealsOnePercentOfMaximumHealth` | unit | scaled healing | delta is exactly 1% and caps at max |
| same | `proximityBoundaryIncludesThreeBlocks` | unit | detonation/stun radius | squared distance at boundary is eligible, outside is not |
| same | `participantMergeDoesNotAddDuplicates` | unit | first fight targeting | players are unique after repeated merge calls |
| same | `wrathFlightStateRoundTripsWhenGameModeIsUnchanged` | unit | temporary suppression | flags are disabled then restored |
| same | `wrathDoesNotRestoreStateAfterGameModeChange` | unit | safe restoration | captured flags are not applied to a changed game mode |

## Modified Tests

None identified.

## Test Setup / Fixtures

| Fixture / Mock / Seed Data | Used By | Setup Details | Cleanup / Isolation |
| -------------------------- | ------- | ------------- | ------------------- |
| Mock `Player` | `DragonBossTest` | Stub game mode, flight flags, speed, and online state | New mocks per test |

## Test Data

| Data Shape | Valid Examples | Invalid / Boundary Examples |
| ---------- | -------------- | --------------------------- |
| health | max=500, current=250 | current=499 and heal cap=500 |
| distance | 0, 2.99, 3.0 | 3.01, other world |
| chance | 0.0 through below 0.05 | exactly 0.05 and above |

## Test Cases per Feature

### Feature: Dragon attacks

| Scenario | Preconditions | Action | Expected Outcome | Assertions |
| -------- | ------------- | ------ | ---------------- | ---------- |
| Slam roll is rare | deterministic roll at boundary | classify attack | slam only below 0.05 | exact boundary assertions |
| Endermite enters proximity | same-world participant at 3 blocks | evaluate proximity | eligible for detonation/stun | true at boundary, false outside |

### Feature: Wrath and regeneration

| Scenario | Preconditions | Action | Expected Outcome | Assertions |
| -------- | ------------- | ------ | ---------------- | ---------- |
| Wrath ends | player had flight before trigger | cleanup | prior state returns | allow/flying/speed restored |
| Heal would exceed maximum | dragon near max health | apply healing calculation | cap at maximum | result equals max |

## Regression / Edge Coverage

- Duplicate participant additions remain idempotent.
- A living regenerator blocks another spawn.
- Existing flight login/respawn tests continue to pass because their systems are not permanently mutated.

## Test Execution

```bash
mvn test
```

## Not Covered / Deferred

- Vanilla dragon phase/velocity behavior, particles, sounds, and perceived balance require a live Paper/Spigot test server.
- `/flight` denial during Wrath is manually validated because loading `FlightCommand` initializes registry-backed Bukkit `Sound` constants, and the repository's Mockito-only unit environment has no Bukkit server registry.
