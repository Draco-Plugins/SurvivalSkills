# Research: Null PlayerRewards For Online Players

## Overview

Player skill state is held in the static `SkillManager.playerSkills` map. Each `SkillsHolder` stores a non-null `PlayerRewards` instance, which is created from the configured default rewards when player data is loaded. Gameplay listeners and commands then retrieve that instance through `SkillManager.getPlayerRewards`, which now repairs missing state for online players.

## Issue Context

- User/requested outcome: Find suspicious code that could make `PlayerRewards` null for online players and strengthen it where necessary.
- Current pain or bug: Several gameplay paths assume the lookup is non-null, while the lookup returns null when a player UUID is absent from the in-memory skills map.
- Scope classification, if known: T3

## Current Behavior

- `FileUtils.loadData` creates a `SkillsHolder` with a new `PlayerRewards` for new players and calls `SkillManager.loadPlayerSkills` for existing players (`src/main/java/sir_draco/survivalskills/utils/FileUtils.java:254-294`).
- `SurvivalSkills.playerJoin` calls `FileUtils.loadData` before synchronizing skills, enabling rewards, and applying death effects (`src/main/java/sir_draco/survivalskills/SurvivalSkills.java:237-260`). Under the normal join path, the holder and rewards object therefore exist before most gameplay work runs.
- Before hardening, `SkillManager.getPlayerRewards` returned null whenever `playerSkills` had no entry for the player's UUID (`src/main/java/sir_draco/survivalskills/skills/SkillManager.java:458-466`). It now restores persisted skill data for an online player, or creates a fresh level-1 holder when no persisted record exists.
- `SkillsHolder` now rejects null skill and reward dependencies at construction (`src/main/java/sir_draco/survivalskills/skills/SkillsHolder.java:15-18`).
- `SurvivalSkills.playerQuit` removes the UUID from `SkillManager.playerSkills` (`src/main/java/sir_draco/survivalskills/SurvivalSkills.java:213-236`). The map is static, and `ResetAllCommand` can clear it while players remain online (`src/main/java/sir_draco/survivalskills/commands/admin_commands/ResetAllCommand.java:80-95`).
- Many listeners and commands dereference the lookup immediately, including `ArmorListener`, `MiningSkill`, fishing managers, farming managers, combat managers, and skill commands. A few newer paths check for null, but the contract is inconsistent.
- `PlayerRewards.handleDeathSkillEffects` now handles a missing lookup defensively and skips already-applied death resistance (`src/main/java/sir_draco/survivalskills/rewards/PlayerRewards.java:65-80`). This method is invoked during player join after data loading (`src/main/java/sir_draco/survivalskills/SurvivalSkills.java:255-260`).

## Relevant Files And Entry Points

- `src/main/java/sir_draco/survivalskills/skills/SkillManager.java:201-258` - Loads player rewards and restores or creates missing per-player holders.
- `src/main/java/sir_draco/survivalskills/skills/SkillManager.java:458-480` - Exposes the reward lookup, restores missing online-player state, and clears lifecycle state.
- `src/main/java/sir_draco/survivalskills/skills/SkillsHolder.java:7-39` - Stores the skills and rewards references.
- `src/main/java/sir_draco/survivalskills/utils/FileUtils.java:254-294` - Creates or loads the player's in-memory skill holder during join.
- `src/main/java/sir_draco/survivalskills/SurvivalSkills.java:213-267` - Removes player state on quit and loads it on join.
- `src/main/java/sir_draco/survivalskills/rewards/PlayerRewards.java:51-74` - Applies death-based effects and performs an unchecked reward lookup.
- `src/main/java/sir_draco/survivalskills/commands/admin_commands/ResetAllCommand.java:80-95` - Clears in-memory player state while online players remain connected.

## Data Flow Or Control Flow

1. Bukkit fires `PlayerJoinEvent`, and `PlayerListener` delegates to `SurvivalSkills.playerJoin` (`src/main/java/sir_draco/survivalskills/skill_listeners/PlayerListener.java:89-103`).
2. `playerJoin` loads `playerdata.yml` through `FileUtils.loadData`.
3. `loadData` creates a holder with copied configured rewards for new or existing players and stores it by UUID.
4. `playerJoin` synchronizes Main skill state, enables the holder's rewards, initializes leaderboard state, and applies death effects.
5. Gameplay event handlers and commands later call `SkillManager.getPlayerRewards` and generally use the returned object without checking it.
6. On quit, the holder is removed. A reset command can remove all holders without disconnecting players; subsequent online-player lookups now restore persisted state or create a safe fresh holder.

## Important Contracts And Constraints

- Player reward instances must be per-player copies because reward application state is mutable; `SkillManager.getNewPlayerRewards` copies the configured default reward list (`src/main/java/sir_draco/survivalskills/skills/SkillManager.java:438-440`).
- Player skill data is persisted in `playerdata.yml`; recovery must not replace a missing in-memory holder with zeroed data when persisted data is available.
- Normal join ordering currently depends on `FileUtils.loadData` completing before reward consumers run.
- The project uses Java 25 and existing code favors records, immutable collections, exact enum types, and explicit null handling.

## Existing Tests And Validation

- `src/test/java/sir_draco/survivalskills/rewards/RewardDataTest.java` and `RewardEffectsTest.java` cover reward registration and effect behavior, but not player-holder lifecycle.
- `src/test/java/sir_draco/survivalskills/rewards/PlayerRewardsTest.java` covers missing reward categories and null reward-list rejection.
- `src/test/java/sir_draco/survivalskills/skills/SkillsHolderTest.java` covers null dependency rejection.
- No test directly exercises Bukkit-backed recovery from `playerdata.yml`; that remains an integration/manual validation surface.
- `mvn test` currently runs 111 tests with one pre-existing failure in `RewardDataTest.epicLootNonLinearPercentages` (`src/test/java/sir_draco/survivalskills/rewards/RewardDataTest.java:248`); the failure is unrelated to the player reward lookup.

## Risks, Edge Cases, And Unknowns

- The exact historical reproduction is not encoded in a test or issue artifact, so the original trigger is uncertain.
- Clearing `playerSkills` while players remain online is a confirmed path to a null lookup.
- Static player state survives plugin object replacement during a reload, which can retain stale holders; clearing it at disable and reloading online players would make lifecycle ownership explicit.
- A fallback holder must preserve persisted skill levels and must not silently grant enabled rewards before the normal loading sequence completes.
- Missing reward categories or reward names are a separate nullability problem: `PlayerRewards.getReward` and `getLevelReward` can also return null, and some callers dereference those results.

## Downstream Guidance

- Requirements should account for: a non-null reward object for a fully loaded online player, safe handling when in-memory state is unexpectedly absent, preservation of persisted skill data, and protection of death-effect application.
- Planning should consider: centralizing holder recovery or loading, making holder construction reject null dependencies, covering reset/reload/online-player paths, and clearing static state at plugin shutdown.
- Do not include: broad migration of every reward lookup to `Optional` or unrelated reward-name/category cleanup unless a focused test demonstrates it is part of this bug.
