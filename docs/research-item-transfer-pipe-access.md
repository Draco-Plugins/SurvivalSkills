# Research: Item Transfer Pipe Access

## Overview

Item transfer pipe interactions enter through `PipeListener`, while pipe linking and persistence are handled by `PipeManager`. The current implementation uses the placing player's stored UUID as an authorization boundary and does not consult the project's existing GriefPrevention build-access helper during pipe wrench interactions.

## Issue Context

- User/requested outcome: Allow every player to access pipes unless GriefPrevention is enabled and denies that player build access at the pipe.
- Current pain or bug: Players other than the placing player are rejected even where claim permissions would otherwise allow them to interact.
- Scope classification, if known: T3

## Current Behavior

- `src/main/java/sir_draco/survivalskills/pipes/PipeListener.java:45` handles wrench interactions with pipe-bearing chests.
- `src/main/java/sir_draco/survivalskills/pipes/PipeListener.java:66` rejects right-click access when the interacting player's UUID differs from the stored pipe owner UUID.
- `src/main/java/sir_draco/survivalskills/pipes/PipeListener.java:131`, `:143`, and `:156` repeat the owner restriction for relinking, sender selection, and removal.
- `src/main/java/sir_draco/survivalskills/pipes/PipeManager.java:101` also requires the actor UUID to own a receiver before relinking it.
- `src/main/java/sir_draco/survivalskills/pipes/PipeManager.java:349` rejects sender/receiver links whose stored owner UUIDs differ.
- `src/main/java/sir_draco/survivalskills/utils/Utils.java:40` already exposes the GriefPrevention `allowBuild` result as a boolean denial check, but `PipeListener` does not use it.

## Relevant Files And Entry Points

- `src/main/java/sir_draco/survivalskills/pipes/PipeListener.java:45` - Player wrench interaction entry point and current owner checks.
- `src/main/java/sir_draco/survivalskills/pipes/PipeManager.java:82` - Pipe creation, linking, relinking, removal, and persisted ownership metadata.
- `src/main/java/sir_draco/survivalskills/utils/Utils.java:40` - Existing GriefPrevention build-access integration.
- `src/main/java/sir_draco/survivalskills/utils/DependencyChecker.java:16` - Enables the GriefPrevention integration flag only when the plugin is installed and enabled.
- `src/main/resources/plugin.yml:7` - Declares GriefPrevention as a soft dependency.
- `src/test/java/sir_draco/survivalskills/pipes/PipeManagerTest.java:19` - Existing pipe tests cover double-chest discovery but not access control.

## Data Flow Or Control Flow

1. A player uses the pipe wrench and `PipeListener.interact` validates the hand, wrench item, and crafting reward unlock.
2. For a clicked chest, the listener resolves any pipe record from `PipeManager`.
3. Right-click opens a receiver filter or displays sender status only when the actor owns the pipe; left-click operations delegate to owner-checked selection, relinking, or removal methods.
4. New and restored links pass through `PipeManager.validateLink`, which currently requires matching owner UUIDs.
5. Mutations mark pipe state dirty and the existing save flow persists the owner field and link state to `pipedata.yml`.

## Important Contracts And Constraints

- GriefPrevention is optional; its API must only be called when `SurvivalSkills.isGriefPreventionEnabled()` is true.
- `Utils.checkForClaim` is named as a claim check but semantically returns whether GriefPrevention's `allowBuild` denied the player at the location. Wilderness and claims where the player has build access therefore return false.
- Existing `owner` values in `pipedata.yml` are required by the current loader. Keeping that metadata avoids a data migration even when ownership no longer controls access.
- The crafting reward gate is independent of pipe ownership and currently runs before all wrench interactions.

## Existing Tests And Validation

- `src/test/java/sir_draco/survivalskills/pipes/PipeRecordTest.java:15` covers immutable pipe record collections and receiver sender references.
- `src/test/java/sir_draco/survivalskills/pipes/PipeManagerTest.java:19` covers double-chest location indexing.
- No existing test covers non-owner access, GriefPrevention denial, or GriefPrevention-authorized access.
- Maven's `test` lifecycle is the repository's available automated validation surface.

## Risks, Edge Cases, And Unknowns

- Removing only listener checks would leave relinking and cross-owner linking partially restricted inside `PipeManager`.
- A player may select a sender in one location and attach or relink a receiver elsewhere, so build access needs to be evaluated for each clicked chest operation.
- Pipe filter inventories do not currently re-evaluate permissions after opening if claim trust changes mid-session.
- Block breaking and explosions remove attached pipe records through separate event paths and rely on the relevant protection plugin/event cancellation behavior.

## Downstream Guidance

- Requirements should account for: no owner-based interaction restrictions; deny pipe wrench operations only when the enabled GriefPrevention integration reports no build access; preserve behavior when GriefPrevention is absent, in wilderness, or grants build access.
- Planning should consider: the listener boundary, manager relink/link invariants, persisted owner compatibility, and tests for all three GriefPrevention states.
- Do not include: removal or migration of persisted owner metadata, unrelated reward-gate changes, or broader claim behavior outside pipe interactions.
