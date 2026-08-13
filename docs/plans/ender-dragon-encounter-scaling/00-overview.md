# Overview

> **Issue:** Ender Dragon encounter scaling and damage scope
> **Classification Type:** T2
> **Severity:** High

## Goal

Make the initial Ender Dragon reliably server-wide while limiting each respawn to the players in its End world within 200 blocks at spawn, with health and damage authorization derived from the appropriate encounter group.

## Approach

Centralize player-count health math, nearby respawn participant selection, and initial-versus-respawn damage authorization in `DragonManager`. Use all online players for initial attachment, reuse the exact nearby snapshot for respawn health and participants, and add focused unit tests for boundaries and authorization.

## Key Files

| File | Purpose |
| ---- | ------- |
| `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/DragonManager.java` | Defines encounter membership, health scaling, and dragon damage authorization. |
| `src/test/java/sir_draco/survivalskills/skill_listeners/fighting/DragonManagerTest.java` | Provides regression coverage for encounter rules. |

## Dependencies / Prerequisites

- Preserve `BossManager`'s existing direct/projectile attacker resolution.
- Preserve `DragonBoss`'s participant storage and respawn marker contract.

## Risks / Open Questions

- Bukkit scheduler/event integration is not available in the existing unit harness; pure helper tests and a full Maven build will validate the extracted rules.
- An empty respawn snapshot intentionally has base health but no authorized players.
