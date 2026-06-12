---
name: issue-classification
description: Classify the scope of an issue based on its description and context
---

# Issue Scope Classification

Use this system to classify a GitHub issue's scope so that it can be triaged and assigned appropriately. Your job is to research the issue description and the relevant codebase context, then produce a classification output.

## Research Phase

Before classifying, you must gather context to make an informed decision:

1. Read the issue description carefully.
2. Search the codebase for relevant files, functions, data models, commands, event types, and systems referenced in the issue.
3. Identify which files, functions, or systems would need to change.
4. Assess the kind of change (text, isolated code, data model, infra, deep bug).
5. Determine test coverage and how hard it would be to verify the change.

## Categories

| Category | Description                              |
| -------- | ---------------------------------------- |
| **T0**   | Text/color/tiny isolated change          |
| **T1**   | Simple code change, low blast radius     |
| **T2**   | Medium ambiguity or unknown side effects |
| **T3**   | Cross-file/cross-system change           |
| **T4**   | Data model, infra, deep bug              |

## Scoring Dimensions

Rate each dimension on its scale, then sum the scores to determine the category.

### Blast Radius (0-5)

How many files, functions, or systems are potentially impacted?

| Score | Meaning                           |
| ----- | --------------------------------- |
| 0     | Single file, no dependents        |
| 1     | 1-2 files, limited dependents     |
| 2     | 2-5 files, some dependents        |
| 3     | 5-10 files, cross-module          |
| 4     | 10+ files, cross-system           |
| 5     | Entire app, external dependencies |

### Uncertainty (0-4)

How clear is the blast radius?

| Score | Meaning                                          |
| ----- | ------------------------------------------------ |
| 0     | Exactly known what changes and what it affects   |
| 1     | Mostly understood, minor unknowns                |
| 2     | Moderate unknowns about side effects             |
| 3     | Significant unknowns, hard to trace dependencies |
| 4     | Very unclear what all needs to change            |

### Behavior (0-5)

What kind of change is this?

| Score | Meaning                                      |
| ----- | -------------------------------------------- |
| 0     | Text, labels, colors, copy                   |
| 1     | UI layout, styling, non-functional           |
| 2     | Simple logic, utility function               |
| 3     | Complex logic, state management, API handler |
| 4     | Data model, database query, auth rule        |
| 5     | Infra, routing, security, concurrency        |

### Testing (0-3)

How hard is it to test the change, and what is the user impact if it breaks?

| Score | Meaning                                          |
| ----- | ------------------------------------------------ |
| 0     | Trivial to test, low user impact                 |
| 1     | Standard tests suffice, moderate impact          |
| 2     | Hard to test, high user impact if broken         |
| 3     | Very hard to test, critical user/business impact |

### Reversibility (0-3)

How easy is it to revert if something goes wrong?

| Score | Meaning                                   |
| ----- | ----------------------------------------- |
| 0     | Instant rollback (config change, text)    |
| 1     | Simple revert, no data consequences       |
| 2     | Revert needs data cleanup or coordination |
| 3     | Destructive or very hard to fully undo    |

## Category Thresholds

Sum the five dimension scores. The total determines the category:

| Score Range | Category |
| ----------- | -------- |
| 0-4         | **T0**   |
| 5-8         | **T1**   |
| 9-11        | **T2**   |
| 12-15       | **T3**   |
| 16-20       | **T4**   |

If any single dimension score is unusually high relative to the total (e.g., Behavior = 5 but total is 8), you may justify a higher category.

## Classification Output

You MUST output the classification in the following structured format. Do not include extra commentary.

```
Category: T0 | T1 | T2 | T3 | T4
Confidence: Low | Medium | High
Reason: <Brief explanation of the classification, including key factors that influenced the decision and the dimension scores>
Needs research before implementation: Yes | No
<If Yes, explain what needs to be researched>
```

### Examples

#### Example 1: Change a reward notification message

```
Category: T0
Confidence: High
Reason: Single-file text change in a reward data class. Blast Radius=0, Uncertainty=0, Behavior=0, Testing=0, Reversibility=0. Total=0.
Needs research before implementation: No
```

#### Example 2: Add a new command

```
Category: T2
Confidence: High
Reason: New command with executor, tab completer, permission checks, and tests. Blast Radius=2 (3 files), Uncertainty=1 (standard pattern), Behavior=3 (command logic), Testing=1 (well-covered by existing test setup), Reversibility=1 (no cross-file changes, independent) Total=8.
Needs research before implementation: No
```

#### Example 3: Change player data storage format

```
Category: T4
Confidence: Medium
Reason: Cross-plugin data format change affecting save/load and rollback. Blast Radius=4 (data layer, commands, listeners, config), Uncertainty=2 (edge cases in old format reading unknown), Behavior=4, Testing=2, Reversibility=3 (destructive data change). Total=15. Behavior=4 further justifies T4.
Needs research before implementation: Yes
Research needed: Determine if the old format can still be read during the transition.
```

## Guidance for Confidence

- **High** — You found and read the relevant files, understood the change surface, and the scores clearly map to a single category.
- **Medium** — You have reasonable understanding but some uncertainty about the blast radius or side effects.
- **Low** — You could not find or read relevant code, the issue is ambiguous, or the scores fall on a category boundary with conflicting signals.
