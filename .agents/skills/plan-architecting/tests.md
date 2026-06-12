# Tests

## Test Strategy

<!--
Summarize the testing approach in 2-4 bullets. Explain which risks require coverage,
which test levels are appropriate, and why. Prefer the smallest reliable test level
that proves the behavior.
-->

- <!-- Primary behavior or risk to prove -->
- <!-- Test level choice: unit, integration, regression, performance, etc. -->
- <!-- Existing coverage to reuse or extend -->

## Requirement Coverage

| Requirement / Acceptance Criteria   | Test Coverage                                            | Notes / Gaps                                            |
| ----------------------------------- | -------------------------------------------------------- | ------------------------------------------------------- |
| <!-- requirement id or behavior --> | <!-- test file + test name, or "new coverage needed" --> | <!-- edge cases, limitations, or reason not covered --> |
| <!-- requirement id or behavior --> | <!-- test file + test name, or "new coverage needed" --> | <!-- edge cases, limitations, or reason not covered --> |

## New Tests

Test types include: unit test, integration test, end-to-end test, regression test, performance test, etc.

| Test File     | Test Name     | Test Type     | Requirement / Risk Covered   | Key Assertions                     |
| ------------- | ------------- | ------------- | ---------------------------- | ---------------------------------- |
| <!-- path --> | <!-- name --> | <!-- type --> | <!-- requirement or risk --> | <!-- exact expected assertions --> |
| <!-- path --> | <!-- name --> | <!-- type --> | <!-- requirement or risk --> | <!-- exact expected assertions --> |

## Modified Tests

| Test File     | Existing Test Name | Change                  | Why It Must Change                               |
| ------------- | ------------------ | ----------------------- | ------------------------------------------------ |
| <!-- path --> | <!-- name -->      | <!-- what to update --> | <!-- changed behavior or regression coverage --> |

## Test Setup / Fixtures

| Fixture / Mock / Seed Data                                 | Used By                        | Setup Details                                  | Cleanup / Isolation                                    |
| ---------------------------------------------------------- | ------------------------------ | ---------------------------------------------- | ------------------------------------------------------ |
| <!-- e.g. Mock player, mocked event, test config file --> | <!-- test file or scenario --> | <!-- exact values or builder/helper to use --> | <!-- reset, cleanup, unique ids, fake timers, etc. --> |

## Test Data

| Data Shape                                           | Valid Examples           | Invalid / Boundary Examples                                            |
| ---------------------------------------------------- | ------------------------ | ---------------------------------------------------------------------- |
| <!-- e.g. exercise input, plan day, user setting --> | <!-- concrete values --> | <!-- empty, null, unauthorized, max/min, duplicate, timezone, etc. --> |

## Test Cases per Feature

### Feature: <!-- name -->

| Scenario      | Preconditions                   | Action                      | Expected Outcome | Assertions                                               |
| ------------- | ------------------------------- | --------------------------- | ---------------- | -------------------------------------------------------- |
| <!-- desc --> | <!-- state, permissions, test data --> | <!-- user/system action --> | <!-- result -->  | <!-- exact assertions, including negative assertions --> |
| <!-- desc --> | <!-- state, permissions, test data --> | <!-- user/system action --> | <!-- result -->  | <!-- exact assertions, including negative assertions --> |

### Feature: <!-- name -->

| Scenario      | Preconditions                   | Action                      | Expected Outcome | Assertions                                               |
| ------------- | ------------------------------- | --------------------------- | ---------------- | -------------------------------------------------------- |
| <!-- desc --> | <!-- state, permissions, test data --> | <!-- user/system action --> | <!-- result -->  | <!-- exact assertions, including negative assertions --> |
| <!-- desc --> | <!-- state, permissions, test data --> | <!-- user/system action --> | <!-- result -->  | <!-- exact assertions, including negative assertions --> |

## Regression / Edge Coverage

- <!-- Previously broken behavior or likely regression -->
- <!-- Empty/loading/error/permission/offline/state transition case -->
- <!-- Cross-cutting case: plugin reload, player quit during save, config corruption, async task timing, etc. -->

## Test Execution

```bash
# Targeted tests for this plan
mvn test
```

## Not Covered / Deferred

<!--
List anything intentionally not covered by automated tests and why. Include manual
verification instructions only when automation is impractical.
-->

- <!-- gap + reason + any manual verification needed -->
