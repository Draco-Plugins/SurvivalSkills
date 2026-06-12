---
name: issue-creation
description: Create a well-formatted issue based on user description
---

Create the issue as a markdown file at `docs/issues/<short-kebab-case-title>.md`. Assume that this folder already exists.

Use the template below. Fill every section. Omit sections only if they truly do not apply.

```markdown
## Title: <Issue Title>

## Tags

Complexity Classification: <T0, T1, T2, T3, or T4>
Severity: <Low, Medium, High, Critical>
Reason: <Classification justification>
Needs research before implementation: <Yes or No, with explanation if Yes>

## Summary

<1-3 sentence summary of the problem or feature>

## Steps to Reproduce Context

1. <step>
2. <step>
3. <step>

## Expected Behavior

<what should happen>

## Actual Behavior

<what happens instead>

## Requirements for completed issue

1. <requirement>
2. <requirement>
3. <requirement>

## Context

- Files: <relevant file references found in the codebase>
- Code Snippets: <relevant code snippets found in the codebase>

## Notes

<optional logs, screenshots, or extra context>
```
