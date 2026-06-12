---
name: review-document
description: Format a code review document that checks the final implementation against the original issue and looks for any newly introduced bugs or risks
---

# Review Document Skill

Use this skill when producing the final artifact for a review agent. The review document is a handoff artifact for the implementer and issue owner: it should make every real concern easy to locate, understand, prioritize, and fix.

This skill is not a general review checklist. The review agent should perform its investigation first, then use this format to compile verified findings into a final markdown report.

## Agentic Flow Context

Review documents are normally created after implementation for:

- T3 issues: cross-file or cross-system changes.
- T4 issues: data model, permissions, event system, infra, data format changes, security-sensitive behavior, or deep bugs.
- Any implementation where the diff needs an independent pass before testing or merge.

The document is consumed by:

- Implementation agents, which use the findings to make targeted fixes.
- Testing agents, which use the warnings and risks to focus manual and automated validation.
- Developers, who need a compact record of what was reviewed and why a change is or is not safe.

Only create a review document when there are findings. If the review finds no issues, report that directly to the user instead of creating an empty document.

## Inputs

Start with the best available inputs:

- The final diff or unstaged changes being reviewed.
- The original issue or user request.
- Requirements, research, plan, and validation documents for the same issue when available.
- Test output, build output, or manual verification notes when available.

If an expected input is missing, continue with the available evidence unless the missing context would make the review speculative. Do not invent issue requirements or product decisions.

## Review Principles

- Findings first: prioritize bugs, regressions, broken requirements, security issues, data risks, and missing validation.
- Evidence over opinion: every finding must point to specific changed code or a concrete missing check.
- Compare against the original issue, requirements, research, and plan when available.
- Read surrounding code before flagging a diff line.
- Do not report speculative concerns as findings. Put residual uncertainty in the summary only when it matters.
- Keep one problem per finding.
- Use severity consistently and avoid inflating minor concerns.
- Include a file review table so downstream agents know the reviewed surface area.
- Do not include implementation patches in the review document unless a tiny snippet is needed to explain evidence.

## Severity Definitions

| Severity     | Definition                                                                              | Examples                                                                                                     |
| ------------ | --------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| **Critical** | Will cause runtime errors, data loss, security vulnerabilities, or broken functionality | Null reference on required field, SQL injection, missing auth check, deleted export still imported elsewhere |
| **Warning**  | Likely to cause issues in edge cases or indicates a pattern that will lead to bugs      | Missing error handling, potential race condition, type narrowing gap, missing test for changed behavior      |
| **Info**     | Code quality observation that will not break behavior but is worth noting               | Inconsistent naming, duplicated logic, unclear branch that makes future maintenance harder                   |

## Review Process

1. Identify the branch name, date, and changed-file scope.
2. Review the diff against the original issue, requirements, research, and plan when available.
3. Inspect surrounding code, callers, imports, tests, and contracts needed to verify each concern.
4. Group verified findings by severity.
5. For each changed file, record its status and issue count for the file review table.
6. If there are no findings, do not create a review document.
7. If there are findings, write the document using the required output order and format below.

## Required Output Order

Always create the review document with sections in this order:

1. Summary
2. Critical Issues
3. Warnings
4. Informational
5. Files Reviewed

If a severity section has no findings, omit that severity section. Always include Summary and Files Reviewed.

## Required Finding Format

Each finding must use this structure:

```markdown
### <Finding Title>

- **Severity**: Critical | Warning | Info
- **File(s)**: [`path/to/file.ts:42`](../../path/to/file.ts#L42)
- **Description**: <what is wrong>
- **Evidence**: <specific code, behavior, contract, or missing validation that proves the concern>
- **Impact**: <what can break or what risk remains>
- **Recommendation**: <targeted direction for fixing or validating the issue>
```

Use links that work from the review document location. If exact line numbers are not available after editing, use the closest changed line and say why.

## Required Format

```markdown
# Branch Review: [branch name]

**Date**: YYYY-MM-DD
**Scope**: Unstaged changes ([N] files changed)

## Summary

[1-3 sentence overview: how many issues found, severity breakdown]

## Critical Issues

[List all Critical severity findings here. If none, omit this section.]

## Warnings

[List all Warning severity findings here. If none, omit this section.]

## Informational

[List all Info severity findings here. If none, omit this section.]

## Files Reviewed

| File      | Status   | Issues     |
| --------- | -------- | ---------- |
| [path]... | Modified | 2 warnings |
| [path]... | Added    | None       |
```

## Section Guidance

### Summary

Write 1-3 sentences. State how many findings were found, the severity breakdown, and the most important risk theme. If the review was limited by missing issue context, generated files, or unavailable tests, mention that briefly.

### Critical Issues

Include only findings that would break functionality, corrupt data, introduce a security issue, or directly violate a must-have requirement. These should block merge or handoff until fixed.

### Warnings

Include likely edge-case bugs, missing error handling, incomplete test coverage for changed behavior, unsafe assumptions, or maintainability issues that could become bugs.

### Informational

Use this section sparingly for low-risk observations. Do not pad the review with style opinions.

### Files Reviewed

List every changed file that was reviewed. Use these statuses when practical: `Added`, `Modified`, `Deleted`, `Renamed`, or `Generated`. The Issues column should summarize counts by severity or say `None`.

## Quality Checklist

Before finishing, verify that:

- A document is created only when there is at least one finding.
- The required sections are present in the required order.
- Empty severity sections are omitted.
- Every finding has severity, file reference, description, evidence, impact, and recommendation.
- Every finding is backed by changed code, surrounding code, or a documented requirement/contract.
- The file review table includes every changed file in scope.
- Severity labels match the definitions above.
- The document avoids speculative issues, broad essays, and unrelated cleanup suggestions.
