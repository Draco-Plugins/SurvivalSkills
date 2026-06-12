---
description: Validate a plan file for missing requirements, tests, bugs, security, performance, and style issues. Usage - validate-plan <plan-directory>
---

Validate the plan at $1.

Check for the following categories of issues:

- **Missing requirements** — Are any requirements from the requirements doc not addressed in the plan?
- **Missing tests** — Does the plan skip test files, edge cases, or verification steps?
- **Bugs** — Are there logic errors, incorrect file paths, wrong function names, or broken assumptions in the plan?
- **Performance issues** — Does the plan make unnecessary database queries, miss pagination, or ignore caching opportunities?
- **Style issues** — Does the plan deviate from the codebase conventions (file organization, naming patterns, framework usage)?

Produce a validation report in `docs/reviews/` using the `plan-validation` skill. Include pass/fail for each check category, blocking and non-blocking issues with file/section references, evidence for each issue, concrete fix recommendations, and a final recommendation to proceed or fix blocking issues first.
