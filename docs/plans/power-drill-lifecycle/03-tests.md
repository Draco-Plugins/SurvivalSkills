# Tests

## Test Strategy

- Add focused listener tests proving drill metadata and a held power drill prevent vein-miner launch.
- Retain the full Maven suite for regression coverage.
- Manually validate concurrent scheduler ownership because the repository has no Bukkit scheduler harness.

## Requirement Coverage

| Requirement / Acceptance Criteria | Test Coverage | Notes / Gaps |
| --- | --- | --- |
| Drill breaks cannot start vein miner | `MiningSkillTest` regression cases | Mockito-based listener test |
| Multiple drills remain independent | Manual scheduler scenario | No MockBukkit dependency |
| Disconnect cancels all drills | Manual scheduler scenario | No MockBukkit dependency |
| Insufficient hunger clears flags | Code-path review and full suite | Scheduler harness absent |

## New Tests

| Test File | Test Name | Test Type | Requirement / Risk Covered | Key Assertions |
| --- | --- | --- | --- | --- |
| `src/test/java/sir_draco/survivalskills/skill_listeners/MiningSkillTest.java` | `drillGeneratedBreakDoesNotStartVeinMiner` | regression | Synthetic drill isolation | Scheduler is never invoked |
| Same | `powerDrillIsRejectedByVeinMinerAdmissionPolicy` | unit | Initial drill action isolation | Admission policy rejects the drill |
| `src/test/java/sir_draco/survivalskills/abilities/VeinMinerAsyncTest.java` | `cleanupAfterFailureClearsAllTransientState` | regression | Failure cleanup | Tracker, metadata, and active flag are cleared idempotently |
| `src/test/java/sir_draco/survivalskills/abilities/items/PowerDrillTaskTest.java` | coordinate generation tests | unit | Lightweight drill plan | Coordinates are unique, immutable, and correctly floored |

## Modified Tests

None identified.

## Test Setup / Fixtures

| Fixture / Mock / Seed Data | Used By | Setup Details | Cleanup / Isolation |
| --- | --- | --- | --- |
| Mock plugin/player/block event | Mining listener cases | Enabled vein miner and ore block | Per-test mocks |

## Test Data

| Data Shape | Valid Examples | Invalid / Boundary Examples |
| --- | --- | --- |
| Block-break provenance | Normal ore | Drill metadata, held power drill |

## Test Cases per Feature

### Feature: Vein-miner admission

| Scenario | Preconditions | Action | Expected Outcome | Assertions |
| --- | --- | --- | --- | --- |
| Synthetic drill ore | Vein miner enabled | Call checker | No task starts | Scheduler untouched |
| Power drill initial ore | Vein miner enabled | Call checker | No task starts | Scheduler untouched |

## Regression / Edge Coverage

- Run all existing mining and reward tests.
- Manually start overlapping drills and disconnect mid-run.

## Test Execution

```bash
mvn test
```

## Not Covered / Deferred

- Automated Bukkit scheduler/event integration requires infrastructure not currently present.
