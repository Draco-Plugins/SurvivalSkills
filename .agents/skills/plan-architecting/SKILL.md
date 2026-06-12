---
name: plan-architecting
description: Break down requirements into a structured implementation plan with multiple files
---

## Plan Architecting Skill

This skill outlines the proper way to architect a plan meant for implementation. The plan should be thorough and clear enough that a weaker agent could execute it without further guidance. The plan should be broken down into multiple files, each with specific instructions, expected changes, and tests. The architecting process involves analyzing the requirements and research, identifying blockers, and creating a structured plan that covers all necessary aspects of the implementation.

## Templates

---

- Overview: A high-level summary of the plan, including the main goals and the approach to be taken. [Overview Template](overview.md)
- Task Files: Detailed instructions for each change that needs to be made, including file paths, function signatures, expected changes, and any relevant code snippets. [Task File Template](task-file.md)
- Test Files: A comprehensive list of tests that need to be created or updated, including test cases, expected outcomes, and any necessary setup. [Test File Template](tests.md)
- Validation File: Instructions for validating the implementation after the changes have been made, including specific commands to run and what to look for in the results. [Validation File Template](validation.md)

## Inputs

- Requirements document
- Research document (optional)

## Process

1. Read the research and requirements documents to understand the issue, the context, and the necessary changes. Most of the necessary information should come from the requirements document, but the research document can provide additional context that may be helpful for planning.
2. Identify missing blockers. You may need to look for any potential issues or short-sighted assumptions in the requirements that could cause problems during implementation.
3. If blockers exist, ask questions before planning.
4. Create the plan. The plan should be broken out into an overview, task files, test files, and validation file.
5. Each task file must be clear enough and specific enough to be executable by a weaker implementation agent.
6. Each task file should have its own scope classification using the plan-task-scope-classification skill. If you classify a task as T4 you definitely need to break it down further. If you classify a task as T3, consider whether it can be broken down further to reduce risk and make it easier to execute.
7. Include exact file paths, function signatures, expected changes, tests, and validation commands.
8. Do not modify production code.

## Example of required output files

docs/plans/issue-123/

- 00-overview.md
- 01-<first-change>.md
- 02-<second-change>.md
- 03-tests.md
- 04-validation.md
