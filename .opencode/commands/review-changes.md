---
description: Review unstaged changes for bugs, security, performance, and style issues
---

Run `git diff` to get unstaged changes. If none exist, inform the user and stop.

Otherwise, follow the agent's standard operating procedure (Phase 2–5) to inspect every changed line. Cover these areas:

- **Logic & bugs** — off-by-one, wrong conditions, missing null checks, incorrect state transitions.
- **Type safety** — mismatched types, unsafe casts, missing type narrowing.
- **Security** — missing permission checks, unsanitized input, exposed secrets, SQL injection.
- **Performance** — unnecessary re-renders, expensive operations in loops, missing memoization.
- **Style & maintainability** — dead code, inconsistent patterns, overly complex expressions, missing error handling.

Produce a final report in `docs/reviews/` using the `review-document` skill, or tell the user there are no issues.
