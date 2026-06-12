---
name: plan-task-scope-classification
description: Classify the scope of individual implementation tasks within a larger issue
---

# Task Scope Classification

Use this system to classify individual task files within a plan. Each task is one focused piece of work within a larger issue. The classification informs the plan architect whether the task is appropriately scoped and helps assign it to the right implementation agent.

## Research Phase

Before classifying, you must gather context:

1. Read the task file's Goal, Files to Modify, Step-by-Step Instructions, and Edge Cases.
2. Search the codebase for the files listed in "Files to Modify" and "Related Files".
3. Identify whether the task depends on other tasks in the plan (check the plan overview and other task files).
4. Assess the kind of change and its blast radius within the broader codebase.

## Categories

| Category | Description                                                                          |
| -------- | ------------------------------------------------------------------------------------ |
| **T0**   | Text/color/tiny isolated change, single file                                         |
| **T1**   | Simple code change, limited to 1-2 files                                             |
| **T2**   | Moderate change across a few files, standard pattern                                 |
| **T3**   | Cross-file or cross-system change, may indicate insufficient decomposition           |
| **T4**   | Data model, infra, or deep architectural change — likely needs further decomposition |

## Scoring Dimensions

Rate each dimension on its scale, then sum the scores to determine the category.

### Blast Radius (0-5)

How many files or systems are impacted?

| Score | Meaning                                   |
| ----- | ----------------------------------------- |
| 0     | Single file, no dependents                |
| 1     | 1-2 files in the task, limited dependents |
| 2     | 2-5 files, some cross-module              |
| 3     | 5-10 files, cross-module                  |
| 4     | 10+ files, cross-system                   |
| 5     | Entire app, external dependencies         |

### Uncertainty (0-4)

How clear is the implementation path and blast radius?

| Score | Meaning                                                      |
| ----- | ------------------------------------------------------------ |
| 0     | Plan is fully specified, every change is known               |
| 1     | Mostly specified, minor unknowns                             |
| 2     | Moderate unknowns (e.g., exact API shape unclear)            |
| 3     | Significant unknowns, hard to trace dependencies             |
| 4     | Very unclear what needs to change — task needs more planning |

### Behavior (0-5)

What kind of change is this?

| Score | Meaning                                      |
| ----- | -------------------------------------------- |
| 0     | Text, labels, colors, copy                   |
| 1     | UI layout, styling, non-functional           |
| 2     | Simple logic, utility function               |
| 3     | Complex logic, state management, API handler |
| 4     | Data model, database query, auth rule        |
| 5     | infra, routing, security, concurrency        |

### Testing (0-3)

How hard is it to test the change?

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

The task file's `Classification` section should be filled in with the T-category and reasoning. Do NOT include additional sections or fields beyond what the template specifies. The output format for the task file is:

```
## Classification

Type: T<n>: <brief description>
Reasoning: <Brief explanation including the dimension scores and key factors>
```

### Examples

#### Example 1: Change a notification message color

```
## Classification

Type: T0: text/tiny change
Reasoning: Single-file text change in the reward data class. Blast Radius=0, Uncertainty=0, Behavior=0, Testing=0, Reversibility=0. Total=0.
```

#### Example 2: Add a new command for displaying skill stats

```
## Classification

Type: T2: moderate change, standard pattern
Reasoning: New command executor, tab completer, registration in plugin class, and tests. Blast Radius=2 (3 files), Uncertainty=1 (follows existing pattern), Behavior=3 (command logic with permission checks), Testing=1 (test setup exists), Reversibility=1 (no cross-file changes). Total=8.
```

#### Example 3: Add a new skill type with rewards and listeners

```
## Classification

Type: T3: cross-file change — consider splitting into separate data + listener tasks
Reasoning: Requires new reward entries in data class, new event listener, command updates, and config changes. Blast Radius=4, Uncertainty=2, Behavior=4, Testing=2, Reversibility=3. Total=15. Behavior=4 and Reversibility=3 further justify T3. This task may benefit from further decomposition into separate data and listener tasks.
```

## Guidance for Confidence (for the plan architect, not included in the task file)

- **High** — You have read all relevant files, the "Files to Modify" list is complete, and the scores clearly map to a single category.
- **Medium** — You have reasonable understanding but some minor unknowns about dependencies or side effects.
- **Low** — You could not find or read relevant code, the task depends on other incomplete tasks, or the scores fall on a category boundary.

## When to Decompose Further

If a task scores **T3** or **T4**, consider splitting it into smaller tasks. Suggested decomposition patterns:

- **Data model change** → separate tasks for: data class update, listener update, command update
- **Cross-system feature** → separate tasks per system boundary (e.g., listener vs. command vs. config)
- **High uncertainty** → add a research/spike task before the implementation task
