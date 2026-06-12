---
name: requirement-document
description: Convert an issue and its research into a clear set of requirements
---

# Requirement Document Skill

Use this skill when converting an issue, bug report, feature request, or research notes into an implementation-ready requirements document.

The output must be clear, specific, and easy to scan. It should tell an implementer exactly what needs to be built, how each item should be tested, what context matters, and what boundaries must not be crossed.

## Required Output Order

Always create the requirements document with sections in this order:

1. Things To Implement
2. Tests To Create Or Update
3. Important Background Information
4. Things To Ensure Are Not Done
5. User Decisions Made During Requirement Creation

Do not reorder these sections. If a section has no known items, include the section and write `None identified.`

## Writing Rules

- Write requirements as concrete, verifiable statements.
- Prefer specific implementation outcomes over vague goals.
- Keep each bullet focused on one behavior, data change, UI change, or contract.
- Include enough detail that a planner can turn each requirement into tasks without rereading the full issue thread.
- Tie tests directly to the implementation items they cover.
- Preserve issue constraints, edge cases, and decisions from the research.
- Avoid implementation plans, file-by-file task breakdowns, or code snippets unless they are essential to clarify a requirement.
- Do not include unrelated cleanup, refactors, or opportunistic improvements.

## Required Format (Example)

```markdown
# Requirements: <Issue Title>

## Things To Implement

- <Specific implementation requirement 1>
- <Specific implementation requirement 2>
- <Specific implementation requirement 3>

## Tests To Create Or Update

- For `<implementation requirement 1>`:
  - <Test that proves the expected behavior works>
  - <Regression or edge-case test, if needed>
- For `<implementation requirement 2>`:
  - <Test that proves the expected behavior works>
- For `<implementation requirement 3>`:
  - <Test that proves the expected behavior works>

## Important Background Information

- <Relevant issue context, current behavior, user workflow, system constraint, data model fact, or prior decision>
- <Any known edge case, dependency, data format concern, permission constraint, or gameplay constraint that affects implementation>
- <Links or references to research documents, issue comments, existing behavior, or related bugs when available>

## Things To Ensure Are Not Done

- <Explicit non-goal, forbidden behavior, out-of-scope change, or risky shortcut to avoid>
- <Existing behavior that must not regress>
- <Architecture, security, data, or UX constraint that must not be violated>

## User Decisions Made During Requirement Creation

| Decision Needed | Answer | Reason |
| --------------- | ------ | ------ |
```

## Section Guidance

### Things To Implement

List the required product and technical outcomes. Each bullet should be specific enough to verify after implementation.

Good bullets:

- Add validation that prevents setting a skill level below 0 in the config file.
- Send a chat message to the player when they level up a skill.
- Keep existing XP gain behavior unchanged for all skill types.

Avoid bullets like:

- Fix validation.
- Improve the skill system.
- Make the rewards better.

### Tests To Create Or Update

Group tests by implementation item using `For '<implementation requirement>':` so coverage is traceable. Include unit, integration, or other test levels as appropriate for the behavior and risk.

Each implementation item should have at least one listed test unless testing is genuinely not applicable. When a requirement does not need a test, state why.

### Important Background Information

Capture only context that helps implementation decisions. Include current behavior, user impact, existing code paths, data model details, platform constraints, related issues, and decisions already made.

This section should explain why the requirements exist without becoming a full research document.

### Things To Ensure Are Not Done

Make non-goals and guardrails explicit. Include scope boundaries, risky shortcuts, behavior that must not change, security or data constraints, and any tempting work that should be left out.

This section should help prevent accidental scope creep and regressions.

### User Decisions Made During Requirement Creation

This prevents the agent that sees this requirements docuement from forgetting "why" certain requirements exist or why certain trade-offs were made. It also helps future readers understand the rationale behind the requirements without needing to read through the entire issue and research thread. You should always frame this as "what was the decision needed to be made", "what was the answer to that decision", and "why was that answer chosen". You should list these in a table format

## Quality Checklist

Before finishing, verify that:

- The five required sections are present and in the required order.
- The implementation list is specific and complete.
- Every implementation item has matching test guidance or an explicit reason no test is needed.
- Background information is relevant to implementation decisions.
- The not-done section clearly defines non-goals and constraints.
- The user decisions section documents the rationale behind requirements and trade-offs.
- The document avoids planning tasks, speculative refactors, and unrelated improvements.
