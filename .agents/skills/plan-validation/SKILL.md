---
name: plan-validation
description: Evaluate an implementation plan for completeness, correctness, and potential issues before starting implementation
---

## Plan Validation Skill

This skill validates implementation plans produced by the plan-architecting skill before coding begins. It checks format correctness, implementation soundness, codebase accuracy, test coverage, and validation completeness. The validator should be skeptical: do not assume the plan is correct just because it is detailed. Confirm claims against the requirements, research, templates, and targeted codebase context.

If any issue is found, report it clearly with the specific file, section, severity, evidence, and recommended fix so the plan can be corrected before implementation.

## Inputs

- The plan directory (typically `docs/plans/issue-<N>/`) containing:
    - `00-overview.md`
    - `01-<task>.md` (one or more task files)
    - `03-tests.md`
    - `04-validation.md`
- The requirement document used to create the plan (optional but recommended)
- The research document used to create the plan (optional)

If the plan directory or referenced requirement/research documents are not provided, ask for the missing path before validating. Do not infer a plan directory from unrelated files unless the user explicitly asks you to find likely candidates.

## Severity Rules

Use these severities consistently:

| Severity     | Meaning                                                                                                                                                                                                                                 |
| ------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Blocking     | The plan is unsafe or not executable as written. Implementation could fail, introduce a bug/security issue, miss a requirement, modify the wrong files, or lack required test/validation coverage. Must be fixed before implementation. |
| Non-blocking | The plan is mostly executable, but clarity, maintainability, specificity, or coverage should be improved. Implementation can proceed only if the owner accepts the residual risk.                                                       |
| Note         | Useful observation that does not require a plan change. Use sparingly.                                                                                                                                                                  |

Default to **blocking** for missing requirements, contradictory instructions, placeholder content, incorrect file paths/symbols, missing auth/permission handling, missing backward-compatible data handling, missing regression coverage for a bug fix, or a task classified T4 without being split.

## Validation Procedure

Validate each plan file in order, then do cross-file consistency checks.

Before scoring the files:

1. Read the plan directory listing and verify expected files exist.
2. Read the requirement document and research document in full when provided.
3. Read the relevant plan-architecting templates: `../plan-architecting/overview.md`, `../plan-architecting/task-file.md`, `../plan-architecting/tests.md`, and `../plan-architecting/validation.md`.

4. Inspect targeted source/test files referenced by the plan when needed to verify paths, class names, method signatures, command names, event types, config keys, data file fields, test helpers, scripts, and existing patterns.
5. Check `pom.xml` for plugin configuration before judging validation/test commands.

Use symbol-aware search or targeted file reads for source verification. Avoid broad codebase audits unless a concrete plan claim cannot be checked locally.

---

### 1. Validate Overview (`00-overview.md`)

Compare against the [Overview Template](../plan-architecting/overview.md).

| Check                  | What to Look For                                                                                              |
| ---------------------- | ------------------------------------------------------------------------------------------------------------- |
| Front matter           | Issue number, classification type, and severity are filled in (not placeholders).                             |
| Goal                   | Clearly states what the implementation achieves in plain language.                                            |
| Approach               | Explains the high-level strategy — what will be created, modified, or removed and why.                        |
| Key Files              | Lists every file that will be changed, with a clear purpose for each. Must be consistent with the task files. |
| Dependencies           | States prerequisites (config changes, data format changes, infra, etc.) and their status.                                      |
| Risks / Open Questions | Identifies real risks or unknowns; if none, says "None identified."                                           |

Additional overview checks:

- The overview does not hide implementation work inside vague phrases such as "update related files" or "wire up behavior".
- The approach matches the requirement/research documents and does not introduce unrequested behavior.
- Key files include tests, config defaults, data format changes, documentation, and validation artifacts when those are part of the change.
- Risks are specific to the plan, not generic boilerplate.
- Any assumption that affects behavior, data, permissions, or UX is called out as an open question or explicitly resolved by requirements/research.

---

### 2. Validate Each Task File (`01-*.md`, `02-*.md`, ...)

Compare against the [Task File Template](../plan-architecting/task-file.md).

| Check           | What to Look For                                                                                                                                                                |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Classification  | Type (T1–T4) and reasoning are present and plausible. T4 tasks must be broken down further; T3 should be examined for possible splitting.                                       |
| Goal            | One or two sentences stating exactly what this task accomplishes.                                                                                                               |
| Files to Modify | Every file listed has an action (create/update/delete). No missing files.                                                                                                       |
| Step-by-Step    | Instructions are specific enough for a weaker agent to execute without guesswork. Includes imports, type signatures, function names, and pseudocode-level logic where relevant. |
| Edge Cases      | Lists at least the obvious edge cases (empty state, error state, permission denial, etc.).                                                                                      |
| Related Files   | Lists files to read for pattern reference but not modify (optional but good practice).                                                                                          |

**Implementation soundness checks for each task:**

- Are event handler calls, data file reads/writes, and side effects properly sequenced and error-handled?
- Are permission checks applied where needed?
- Are loading, empty, error, and success states all accounted for in command feedback?
- Are data types, serialization, and nullability handled correctly?
- If the task touches async task scheduling (BukkitRunnable, etc.), are cancellation and cleanup considered?
- If the task involves player state, are quit, kick, and reload scenarios handled?
- Are there any hardcoded values, TODOs, or placeholders that should be configuration or constants?

**Codebase accuracy checks for each task:**

- Referenced files exist, or new files are clearly marked as `create`.
- Referenced classes, methods, event types, command names, config keys, data file fields, and permission nodes match the actual codebase or are explicitly introduced in the task.
- Package names and import statements are plausible for the existing module boundaries.
- The task does not modify generated files unless generation is explicitly part of the plan.
- The task does not duplicate existing helper methods or utility classes when a local pattern already exists.
- The task respects repository conventions, including package structure, utility class usage, and method length expectations.

**Config and data file checks when relevant:**

- Old data file formats remain readable during a format change (backward compatibility).
- Existing player data is handled safely — corrupted files do not crash the plugin.
- Default config values are generated on first load if the file is missing.
- Data file format (JSON, YAML, etc.) and serialization approach are specified.

**Command/listener feedback checks when relevant:**

- Player-facing messages cover success, error, permission-denied, and cooldown states.
- Feedback uses the plugin's chat formatting conventions (Bukkit ChatColor, Adventure API, etc.).
- Console logging follows the plugin's logging pattern (see AGENTS.md) for errors and warnings only.
- Event cancellation and side effects (drops, damage, block changes) are specified where applicable.

**Task sizing checks:**

- T4 tasks must be split before implementation.
- T3 tasks should be split if they combine unrelated files, unrelated behavior, or more than one risky state/data transition.
- Any task that requires broad judgment from the implementer should be marked blocking until the plan adds the missing specifics.

---

### 3. Validate Tests (`03-tests.md`)

Compare against the [Tests Template](../plan-architecting/tests.md).

| Check                      | What to Look For                                                                                                                                        |
| -------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Test Strategy              | Summarizes approach, test levels, and rationale. Missing coverage should be justified (e.g., "covered by integration tests only").                                    |
| Requirement Coverage       | Every requirement or acceptance criterion from the issue maps to at least one test. No gaps.                                                            |
| New Tests                  | Table is populated with file paths, test names, types, and key assertions. Cross-check that every code path in the task files has a corresponding test. |
| Modified Tests             | Existing tests that need updating due to changed behavior are listed with the exact change and reason.                                                  |
| Test Setup / Fixtures      | Fixtures, mocks, and test data (player state, config files, etc.) are specified with setup details and cleanup/isolation strategy.                                                         |
| Test Data                  | Valid and invalid/boundary examples are provided for every significant data shape.                                                                      |
| Test Cases per Feature     | Scenarios cover: happy path, empty/zero state, error/failure, permission denied, and edge/boundary conditions.                                          |
| Regression / Edge Coverage | Lists previously broken behaviors, cross-cutting concerns (plugin reload, async task timing, data save failure, etc.).                                              |
| Test Execution             | Commands are correct and runnable. Usually includes both targeted tests                                                                                 |
| Not Covered / Deferred     | Any intentional gaps are documented with reasoning.                                                                                                     |

**Thoroughness sweep:** For each task file's step-by-step instructions and edge cases, verify there is a corresponding test entry in the tests file. If a behavior is described in the task but has no test, that is a gap.

Additional tests checks:

- Bug fixes include regression tests that would have failed before the fix.
- Permission/auth behavior is covered at the smallest reliable level, or an integration/manual gap is justified.
- Failure paths are tested separately from success paths.
- Async task scheduling and cancellation (BukkitRunnable, BukkitScheduler) are covered when touched.
- Data file changes include backward-compatible loading for old formats where needed.
- Integration tests are only required when unit tests cannot prove the behavior, but missing integration coverage for full server workflows must be justified.
- Test names and assertions are specific enough that an implementer can write the tests without inventing acceptance criteria.
- Test commands are valid for this repository and include targeted checks

---

### 4. Validate Validation File (`04-validation.md`)

Compare against the [Validation Template](../plan-architecting/validation.md).

| Check                     | What to Look For                                                                                                                                          |
| ------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Automated Checks          | Compile, test, and build commands are present and correct for this project.                                                                       |
| Manual Verification Steps | Steps are concrete and actionable, with expected outcomes stated. They should cover anything that can't be automated (in-game behavior checks, console output inspection, etc.). |
| Build / Compilation       | Build command is present.                                                                                                                                 |
| Common Pitfalls           | Lists realistic pitfalls relevant to this specific plan (not generic boilerplate).                                                                        |

**Completeness check:** Does the validation file itself cover everything needed to verify the implementation? Consider:

- Are there data conversion steps that need manual verification?
- Are there environment variables or configuration changes to validate?
- Are there player data files or config state prerequisites?
- If there are gameplay or UI changes (GUIs, boss bars, etc.), do manual steps include checking all relevant in-game states?

Additional validation checks:

- Automated checks should use commands that work with Maven and the project's `pom.xml`
- Targeted commands should be specific to changed tests or affected suites where practical.
- Manual steps should state preconditions, actions, and expected outcomes. They should not be vague instructions like "verify it works".
- Manual server testing should include checking console for errors, in-game command behavior, and event-triggered behavior.
- Validation should mention plugin reload, data file backup, and clean build steps when data or config changes require them.
- Common pitfalls should name the concrete failure mode and the affected plan area.

---

### 5. Cross-File Consistency Checks

After validating each file individually, verify consistency across the plan:

| Check                    | What to Look For                                                                                                                                |
| ------------------------ | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| File list alignment      | The key files table in the overview and the files-to-modify in each task file agree on what needs to change.                                    |
| No orphan tasks          | Every behavior described in the overview or requirements appears in at least one task file or is explicitly deferred.                           |
| Test coverage match      | Every step, edge case, and function signature mentioned in task files has a corresponding test entry.                                           |
| Validation matches tasks | The validation file's manual steps and common pitfalls align with the actual changes described in the task files.                               |
| Numbering convention     | Files follow the `NN-<name>.md` naming convention and are ordered logically (usually: overview, tasks sorted by dependency, tests, validation). |

Additional consistency checks:

- Requirement coverage: every requirement and acceptance criterion appears in a task, a test, and validation when manual verification is needed.
- Research coverage: important constraints or existing patterns from the research document are reflected in the plan, or deviations are justified.
- Dependency ordering: tasks are ordered so data classes/helpers are created before listeners, commands, or tests that depend on them.
- File action consistency: the same file is not listed with conflicting actions across tasks.
- Terminology consistency: names for commands, events, config keys, data file fields, and permission nodes are consistent across all files.
- Deferred work consistency: any deferred behavior appears in the overview risks/open questions and in the tests file's Not Covered / Deferred section.

---

### 6. Executability Review

After cross-file checks, judge whether a weaker implementation agent could execute the plan without making product or architecture decisions.

Ask:

- Are all required decisions already made or clearly marked as blockers?
- Does each task say exactly where to edit and what behavior to produce?
- Are signatures, data shapes, state transitions, and error handling concrete enough?
- Are tests and validation strong enough to catch the likely mistakes in the implementation?
- Does the plan keep changes within the requested scope?

If the answer to any of these is no, report a blocking issue unless the missing detail is truly optional.

---

## Output

Produce a validation report containing:

- **Pass/Fail** for each file and each check category
- **Specific issues** found, with the exact file path and section reference
- **Severity** of each issue (blocking / non-blocking)
- **Evidence** for each issue, including the conflicting requirement, template expectation, codebase fact, or missing coverage
- **Recommendations** for how to fix each issue
- **Residual risks or assumptions** if relevant

Blocking issues must be resolved before implementation begins.

Use this report structure:

```markdown
## Validation Report for <plan-directory>

### Inputs Reviewed

- Plan files: <list>
- Requirements: <path or "Not provided">
- Research: <path or "Not provided">
- Codebase checks: <brief list of referenced files/scripts inspected>

### 00-overview.md - PASS/FAIL

- <summary or issues>

### Task Files - PASS/FAIL

- <per-file summary or issues>

### 03-tests.md - PASS/FAIL

- <summary or issues>

### 04-validation.md - PASS/FAIL

- <summary or issues>

### Cross-File Consistency - PASS/FAIL

- <summary or issues>

### Executability Review - PASS/FAIL

- <summary or issues>

### Summary

- Blocking issues: <count>
- Non-blocking issues: <count>
- Recommendation: <Proceed / Fix blocking issues first>
```

When there are no issues in a category, say what was checked and why it passes. Keep the report concise, but do not omit evidence for failures.

## Example

```markdown
## Validation Report for docs/plans/issue-42/

### 00-overview.md — PASS

- All fields filled. Approach is clear.

### 01-add-reward-data.md — FAIL (1 blocking, 1 non-blocking)

- [BLOCKING] Step 3: Reward notification message references a placeholder `{level}` that is not substituted in the code path. This would show raw placeholders to players.
- [NON-BLOCKING] Edge Cases: Missing case for when the player's data file is missing or corrupted on first join.

### 03-tests.md — FAIL (1 blocking)

- [BLOCKING] No test coverage for the case where a reward description returns an empty string.

### 04-validation.md — PASS

- All checks present. Pitfalls include stale compiled classes from a previous build.

### Cross-File Consistency — PASS

- No inconsistencies found.

### Summary: 2 blocking issues found. Fix before implementation.
```
