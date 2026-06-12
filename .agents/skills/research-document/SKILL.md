---
name: research-document
description: Research codebase behavior and format findings into a handoff document for requirements and planning. Use when an issue needs codebase research before requirements, planning, classification, or implementation, especially T3/T4 work, cross-file behavior, data model/permissions/event system/infra changes, data format changes, deep bugs, or any task where current behavior and blast radius are unclear.
---

# Research Document Skill

Use this skill to explain what the codebase currently does and how the requested issue relates to it. The research document is a handoff artifact for the requirements agent and plan architect, not an implementation plan.

The goal here is to reduce uncertainty. Capture facts, relevant file paths, current behavior, contracts, dependencies, and open questions.

## Agentic Flow Context

Research documents are normally created for:

- T3 issues: cross-file or cross-system changes.
- T4 issues: data model, permissions, event system, infra, data format changes, security-sensitive behavior, or deep bugs.
- T2 issues when classification or implementation risk is unclear.
- Any issue where a requirements or planning agent would otherwise need to rediscover current behavior.

The document is consumed by:

- Requirements agents, which convert issue + research into exact requirements.
- Plan architects, which identify implementation chunks, tests, and validation.
- Review agents, which compare final changes against known current behavior and risk areas.
- Developers, who want to understand the current codebase behavior before making changes.

Optimize for these downstream readers. Include enough context that they can reason without rereading every file, but keep the document focused on the issue.

## Research Principles

- Describe current behavior, do not propose changes.
- Anchor important claims to file paths, class names, method names, command names, event types, tests, or config keys.
- Prefer concrete references like `src/main/java/.../Foo.java:42` over broad references like "the command layer."
- Follow the actual runtime/data flow from entry point to persistence, side effects, and user-visible output.
- Separate facts from inferences. Label inferred behavior when code does not make it explicit.
- Capture unknowns instead of silently filling gaps.
- Include tests and validation surfaces that already exist.
- Do not include opportunistic refactors or implementation tasks unless documenting that they are tempting but out of scope.

## Inputs

Start with the best available inputs:

- Issue description, user request, bug report, or feature request.
- Existing classification, if present.
- Existing docs, plans, or prior research for the same area.
- Relevant branch/diff context, if the research is about pending changes.

If the issue is ambiguous, research the codebase first. Ask the user only when the ambiguity blocks useful research or creates multiple incompatible interpretations.

## Research Process

1. Restate the issue in one or two sentences for yourself.
2. Identify likely search terms from command names, permission nodes, event names, skill types, config keys, data file paths, errors, and domain terms.
3. Use LSP for code search. It is more accurate, efficient, and context-aware than keyword search. Use it to find definitions, references, implementations, tests, logical flows, and docs.
4. Read outward from the entry points:
   - Bukkit event listeners and command registration in `plugin.yml` and the main plugin class.
   - Command executors, tab completers, and event handler methods.
   - Permission checks, feature gating, and configuration toggles.
   - Player data storage (YAML/JSON files), config files, scheduled tasks, and side effects.
   - Existing tests and fixtures.
5. Trace the main flow and important edge or failure paths.
6. Identify blast radius: files, modules, data contracts, user roles, background jobs, integrations, and tests likely affected.
7. Record unresolved questions, risky assumptions, and areas not inspected.
8. Write the research document using the required format below.

## Research Depth

Scale depth to risk:

- T1/T2: identify the immediate files, current behavior, and tests. Keep the document short.
- T3: trace the full cross-file flow, key dependencies, and likely side effects.
- T4: include data model details, permission or region checks, rollback/reversibility concerns for data changes, concurrency or security risks, and validation gaps. This should be a deep dive, not a broad survey.

Stop when the downstream agent can confidently create requirements or a plan. Do not keep expanding into unrelated systems just because they share a helper or component.

## Required Output Order

Always create the research document with sections in this order:

1. Overview
2. Issue Context
3. Current Behavior
4. Relevant Files And Entry Points
5. Data Flow Or Control Flow
6. Important Contracts And Constraints
7. Existing Tests And Validation
8. Risks, Edge Cases, And Unknowns
9. Downstream Guidance

If a section has no known items, include the section and write `None identified.`

## Required Format

```markdown
# Research: <Issue Title>

## Overview

<2-4 sentence summary of the relevant current system behavior and how it relates to the issue.>

## Issue Context

- User/requested outcome: <what the issue asks for, without inventing extra requirements>
- Current pain or bug: <what is failing, missing, or unclear>
- Scope classification, if known: <T0|T1|T2|T3|T4 or "Unknown">

## Current Behavior

- <Fact about how the code currently behaves, with file reference>
- <Fact about current user-visible behavior, API behavior, data behavior, or side effect>
- <Inference, labeled if not directly proven by code>

## Relevant Files And Entry Points

- `<path>:<line>` - <route/component/function/table/test and why it matters>
- `<path>:<line>` - <route/component/function/table/test and why it matters>

## Data Flow Or Control Flow

1. <Step from user action/request/job to first code entry point>
2. <Validation/auth/state transition>
3. <Persistence, external call, or side effect>
4. <Response, UI update, event, or downstream behavior>

## Important Contracts And Constraints

- <Data model, permission rule, config key, feature toggle, or player state invariant>
- <Existing behavior that downstream requirements/planning must preserve>

## Existing Tests And Validation

- `<path>:<line>` - <what this test covers>
- <Missing coverage or manual validation surface>

## Risks, Edge Cases, And Unknowns

- <Known risk, edge case, race, data format concern, permission concern, or integration concern>
- <Unknown that should be resolved before implementation, requirements, or planning>

## Downstream Guidance

- Requirements should account for: <facts that should become requirements or constraints>
- Planning should consider: <likely affected areas, sequencing concerns, test strategy, or validation commands>
- Do not include: <out-of-scope tempting changes or risky shortcuts>
```

## Section Guidance

### Overview

Summarize the current system, not the solution. Mention the major modules involved and why this area is being researched.

### Issue Context

Preserve the user's request and known classification. Do not upgrade "the user wants X" into "the system must implement Y and Z" unless those requirements are explicit.

### Current Behavior

Explain what happens today. Include relevant happy paths, failure paths, permission behavior, loading states, persistence behavior, and external side effects.

### Relevant Files And Entry Points

List the files a downstream agent is most likely to need. Prefer fewer high-signal entries over every file touched by search results.

### Data Flow Or Control Flow

Trace the path in order. For gameplay changes, start with player action and the triggering Bukkit event. For command work, start with the command executor. For data work, continue through data file serialization, config loading, scheduled tasks, and downstream effects.

### Important Contracts And Constraints

Capture boundaries that must not be broken: data file formats, permission checks, role behavior, config keys, feature toggles, task scheduling invariants, and compatibility with existing tests or fixtures.

### Existing Tests And Validation

Document tests that already cover the area, tests that look related but do not cover the issue, and obvious missing coverage. Include known commands only when discovered from the repo or docs.

### Risks, Edge Cases, And Unknowns

Use this section to prevent false confidence. Include uninspected paths, ambiguous product behavior, suspected coupling, data format reversibility, data cleanup needs, and security-sensitive assumptions.

### Downstream Guidance

Give requirements and planning agents the key handoff notes. This is not a task list. Keep it focused on decisions they must preserve, likely affected surfaces, and work that should stay out of scope.

## Quality Checklist

Before finishing, verify that:

- The document explains current behavior more than proposed changes.
- Important claims have concrete references.
- The main flow and at least one relevant failure or edge path are covered.
- Data, permissions, event system, infra, data format, or integration concerns are included when relevant.
- Existing tests and validation surfaces are identified.
- Unknowns and assumptions are explicit.
- Downstream guidance helps requirements and planning without becoming an implementation plan.
