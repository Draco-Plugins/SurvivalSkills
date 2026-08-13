# Tests

## Test Strategy

- Unit-test pure encounter rules in the same package as `DragonManager`.
- Cover the exact 200-block boundary and cross-world exclusions without starting a Bukkit server.
- Lock initial global damage and respawn participant-only damage as separate cases.

## Requirement Coverage

| Requirement / Acceptance Criteria | Test Coverage | Notes / Gaps |
| --------------------------------- | ------------- | ------------ |
| Initial health scales with all online players and has a one-player floor | `DragonManagerTest.dragonHealthUsesPlayerCountWithOnePlayerMinimum` | Pure health math. |
| Respawn players are in the exact End world within 200 blocks | `DragonManagerTest.respawnParticipantsRequireExactEndWorldAndRadius` | Includes boundary, beyond-boundary, other-End, and normal-world cases. |
| Initial player damage is server-wide | `DragonManagerTest.initialDragonAllowsAnyPlayerDamage` | Tests authorization rule independent of Bukkit event plumbing. |
| Respawn damage is participant-only | `DragonManagerTest.respawnDragonAllowsOnlyParticipantDamage` | Tests both allowed and rejected players. |

## New Tests

| Test File | Test Name | Test Type | Requirement / Risk Covered | Key Assertions |
| --------- | --------- | --------- | -------------------------- | -------------- |
| `src/test/java/sir_draco/survivalskills/skill_listeners/fighting/DragonManagerTest.java` | `dragonHealthUsesPlayerCountWithOnePlayerMinimum` | unit/regression | Both health-scaling paths | Counts 0, 1, and 3 yield 250, 250, and 750. |
| same | `respawnParticipantsRequireExactEndWorldAndRadius` | unit/regression | Respawn geographic scope | Only same-End-world players at distances <= 200 are returned. |
| same | `initialDragonAllowsAnyPlayerDamage` | unit/regression | Initial server-wide combat | Nonparticipant is allowed when `isRespawn` is false. |
| same | `respawnDragonAllowsOnlyParticipantDamage` | unit/regression | Respawn ownership | Participant is allowed and other player is rejected. |

## Modified Tests

None identified.

## Test Setup / Fixtures

| Fixture / Mock / Seed Data | Used By | Setup Details | Cleanup / Isolation |
| -------------------------- | ------- | ------------- | ------------------- |
| Mock End and normal Bukkit worlds | participant selection | Stub `getEnvironment()` and use `Location` values with matching world objects. | Per-test mocks only. |
| Mock Bukkit players | participant selection and damage authorization | Stub player worlds and locations; use identity-based list membership. | Per-test mocks only. |

## Test Data

| Data Shape | Valid Examples | Invalid / Boundary Examples |
| ---------- | -------------- | --------------------------- |
| Player count | 1, 3 | 0 uses one-player minimum. |
| Respawn distance | 199, 200 | 200.01 excluded. |
| Player world | Exact dragon End world | Different End world and normal world excluded. |

## Test Cases per Feature

### Feature: Encounter Health

| Scenario | Preconditions | Action | Expected Outcome | Assertions |
| -------- | ------------- | ------ | ---------------- | ---------- |
| Empty count | player count is zero | calculate health | base health | equals 250 |
| Server group | player count is three | calculate health | scaled health | equals 750 |

### Feature: Encounter Authorization

| Scenario | Preconditions | Action | Expected Outcome | Assertions |
| -------- | ------------- | ------ | ---------------- | ---------- |
| Initial fight | attacker absent from participant list | authorize damage | allowed | returns true |
| Respawn fight | one participant and one outsider | authorize each | only member allowed | true for member, false for outsider |

## Regression / Edge Coverage

- Initial fights cannot accidentally inherit respawn participant gating.
- Exact radius boundary is inclusive.
- Similar End environments do not make distinct worlds equivalent.
- Empty player counts never produce a zero-health dragon.

## Test Execution

```bash
mvn test -Dtest="DragonManagerTest"
mvn test
```

## Not Covered / Deferred

- Live multiplayer Bukkit event timing requires manual server verification because the repository has no server integration-test harness.
