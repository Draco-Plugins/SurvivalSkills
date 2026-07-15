# Research: Power Drill Infinite-Run And Lifecycle Risks

## Overview

The current power drill implementation converts a player's block break into an asynchronous preparation task and then a synchronous, one-tick batch runner. A single isolated runner is bounded by a finite block list and an index that always advances, but the surrounding event/tracker design still permits overlapping drill and vein-miner tasks to lose ownership of their synthetic block-break events. Those collisions can recursively create more scheduled work and are the strongest remaining explanation for a drill that appears to continue indefinitely.

## Issue Context

- User/requested outcome: Analyze the refactored power-drill code for potential issues or leaked state/tasks that could reproduce the historical infinite-drill behavior.
- Current pain or bug: A prior power drill sometimes continued indefinitely; it is unclear whether subsequent refactors fully removed the underlying lifecycle and cross-ability races.
- Scope classification, if known: T4 (deep event/scheduler lifecycle bug spanning power drill, vein miner, player state, and Bukkit task execution).

## Current Behavior

- `PowerOreChallengeListener.onBreakBlock` examines every `BlockBreakEvent`, including synthetic events created by this plugin, and starts a drill when the held item is the power drill, the block is not found in the player's current drill list, no vein-miner guard is active, and the reward is unlocked (`src/main/java/sir_draco/survivalskills/skill_listeners/god/PowerOreChallengeListener.java:80`).
- The handler creates an empty per-player tracker entry before validating vein-miner state, reward availability, or unlock state (`PowerOreChallengeListener.java:118`). An eligible break schedules `PowerDrillTask` asynchronously (`PowerOreChallengeListener.java:163`).
- `PowerDrillTask.run` reads player, inventory, location, world, and block state asynchronously; creates a finite ordered block list; replaces the player's tracker list; and schedules a synchronous repeating `DrillBatchRunner` (`src/main/java/sir_draco/survivalskills/abilities/items/PowerDrillTask.java:41`).
- `getBlocks` performs exactly 19 forward steps and stores blocks in a `LinkedHashSet`, so the list has a finite upper bound of the origin plus 19 groups of at most 27 blocks (`PowerDrillTask.java:60`).
- `DrillBatchRunner` processes at most nine entries per tick, increments its index by nine on every successful invocation, then unregisters the player's tracker and cancels itself on the invocation after the list is exhausted (`PowerDrillTask.java:110`). A single runner therefore has no index-based infinite loop.
- Every non-air, non-forbidden target generates a new `BlockBreakEvent`. The power-drill listener treats that event as internal only when the target exists in the one tracker list currently stored for that player, removes the target, and returns (`PowerDrillTask.java:81`; `PowerOreChallengeListener.java:121`).
- Inference: If two drills overlap, the later async preparation replaces the earlier task's tracker list. Synthetic events from the earlier runner that are absent from the replacement list are then indistinguishable from player-originated events and can start additional drills. Completion of either runner removes the shared tracker entry even if it belongs to the other runner (`PowerOreChallengeListener.java:360`; `PowerDrillTask.java:112`). This permits recursively expanding scheduled work even though every individual runner is finite.
- Mining's normal-priority block-break handler runs vein-miner detection for synthetic drill events too; it has no drill-event marker or drill-active guard (`src/main/java/sir_draco/survivalskills/skill_listeners/MiningSkill.java:80`; `MiningSkill.java:393`). When the player is sneaking and vein miner is enabled, drill-broken ores can therefore start `VeinMinerAsync` tasks.
- `VeinMinerAsync` also replaces one per-player block list, then emits synthetic `BlockBreakEvent`s one block per tick (`src/main/java/sir_draco/survivalskills/abilities/VeinMinerAsync.java:39`). Inference: multiple vein miners started by a drill can overwrite one another's tracker and cause their synthetic events to start still more vein miners. The active flag is consulted by the power-drill listener, but not by `MiningSkill.veinminerChecker`, so it does not prevent this vein-to-vein recursion (`MiningSkill.java:393`; `PowerOreChallengeListener.java:146`).
- The historical commit `f842d6c` explicitly describes issue `#284` as a power-drill/vein-miner storage race causing infinite loops. It introduced metadata and a global vein-miner-active guard. The current code retains those cross-ability checks, but it still allows drill events to start vein miners and allows multiple tasks of either type to overwrite their own one-list-per-player trackers.
- Quitting removes the player's drill tracker entry but does not cancel the active batch runner (`PowerOreChallengeListener.java:368`). The runner has no online check, so it can continue firing events and breaking blocks after the player disconnects (`PowerDrillTask.java:110`).
- The drill tracker is removed only on normal batch completion or player quit. Early returns and exceptions do not use a cleanup/finally path (`PowerDrillTask.java:41`; `PowerDrillTask.java:110`).
- Vein-miner activity is set before its async task starts and is cleared only at normal batch completion (`VeinMinerAsync.java:28`; `VeinMinerAsync.java:75`). The insufficient-hunger return removes the block tracker but does not clear the active flag (`VeinMinerAsync.java:45`). This leaked UUID state blocks later drill starts rather than extending them.

## Relevant Files And Entry Points

- `src/main/java/sir_draco/survivalskills/skill_listeners/god/PowerOreChallengeListener.java:80` - Power-drill event entry point, synthetic-event discrimination, task launch, shared drill tracker, and quit cleanup.
- `src/main/java/sir_draco/survivalskills/abilities/items/PowerDrillTask.java:41` - Asynchronous preparation, finite block discovery, synchronous batching, synthetic block breaks, and normal completion.
- `src/main/java/sir_draco/survivalskills/skill_listeners/MiningSkill.java:80` - Receives drill-generated block-break events and can start vein miner for them.
- `src/main/java/sir_draco/survivalskills/skill_listeners/MiningSkill.java:393` - Vein-miner tracker discrimination and async launch; does not reject a second active vein-miner task.
- `src/main/java/sir_draco/survivalskills/abilities/VeinMinerAsync.java:28` - Vein-miner active state, shared block tracker, synthetic events, and incomplete abnormal cleanup.
- `src/main/java/sir_draco/survivalskills/SurvivalSkills.java:184` - Listener registration order: mining is registered before the God listener's sub-listeners.
- `src/main/java/sir_draco/survivalskills/skill_listeners/GodListener.java:45` - Registers `PowerOreChallengeListener` and establishes its plugin lifetime.
- `pom.xml:106` - Spigot API plus JUnit/Mockito dependencies; no server scheduler/event test harness is present.

## Data Flow Or Control Flow

1. A player breaks a block while holding the custom-model-data power drill. Bukkit dispatches the event to `MiningSkill` and `PowerOreChallengeListener`.
2. Mining may independently launch `VeinMinerAsync` if the player is sneaking, has vein miner enabled, and broke an ore. The power-drill listener creates/fetches an empty tracker, checks whether the block belongs to the current drill list, applies vein-miner guards, checks the unlock, and schedules `PowerDrillTask` asynchronously.
3. `PowerDrillTask` reads the player's current direction and held tool, discovers a finite tunnel-shaped block list, stores that list under the player key, and schedules `DrillBatchRunner` synchronously every tick.
4. Each batch runner sends up to nine synthetic `BlockBreakEvent`s through the full plugin event system and calls `breakNaturally` when an event is not cancelled.
5. A synthetic event is suppressed as an internal drill event only if its block is still present in the single list currently mapped to that player. Otherwise, it can follow the normal launch path again.
6. Mining also sees each synthetic event and may launch vein miner. Vein miner repeats a similar tracker/event cycle for ore blocks.
7. On the normal path, the drill index reaches the finite list size, the shared player tracker entry is removed, and the runner cancels. On disconnect, only shared map state is removed; scheduled runners continue. On early return or abnormal runner termination, cleanup is not guaranteed.

## Important Contracts And Constraints

- Synthetic block breaks intentionally traverse Bukkit's `BlockBreakEvent` pipeline so other plugin behavior and cancellation can apply (`PowerDrillTask.java:85`). Internal-event discrimination must therefore remain correct across every concurrently active producer.
- The current implicit invariant is “one drill tracker list per `Player`,” but the code does not enforce “one active drill task per player.” Registration uses unconditional `Map.put`, and cleanup uses unconditional `Map.remove` (`PowerOreChallengeListener.java:360`).
- `drillTracker` and mining's `veinTracker` are `HashMap`s, while their preparation tasks mutate them asynchronously and event runners read/mutate them synchronously (`PowerOreChallengeListener.java:72`; `MiningSkill.java:64`). This violates the maps' single-threaded access contract and makes visibility/corruption behavior undefined.
- Most Bukkit player, inventory, world, location-derived block, and scheduler interactions are expected to occur on the server thread. The drill preparation task performs these operations from an asynchronous scheduler invocation (`PowerOreChallengeListener.java:163`; `PowerDrillTask.java:41`).
- The task's captured `ItemStack` and direction are snapshots taken after the original event, not necessarily the tool/direction from the event instant (`PowerDrillTask.java:45`).
- Per-block cancellation prevents `breakNaturally`, but `breakBlock` still returns `true`, so cancelled blocks count as activity for the batch sound (`PowerDrillTask.java:81`).
- `PowerOreChallengeListener.onBreakBlock` does not use `ignoreCancelled = true` or explicitly stop when the originating event is already cancelled. A protected/cancelled initial break can therefore still launch drill work depending on event priority/order (`PowerOreChallengeListener.java:80`).
- Bukkit cancels plugin-owned scheduled tasks when the plugin is disabled, but the listener does not explicitly clear its player-keyed transient maps during disable. Player quit is the only explicit drill cleanup event in this listener.

## Existing Tests And Validation

- No unit or integration test references `PowerDrillTask`, `PowerOreChallengeListener`, `VeinMinerAsync`, drill tracking, or synthetic drill events under `src/test`.
- `src/test/java/sir_draco/survivalskills/skill_listeners/MiningSkillTest.java:1` covers selected mining-listener behavior but not vein-miner scheduling or drill interaction.
- `docs/to-test.md` contains no power-drill or vein-miner scenario.
- The repository uses JUnit 5 and Mockito but has no MockBukkit-style server event/scheduler harness (`pom.xml:136`). Existing unit tests cannot validate repeating-task ownership, listener recursion, disconnect cleanup, or cross-plugin cancellation without additional test infrastructure or purpose-built fakes.
- High-value manual validation surfaces are: rapid consecutive drill starts by one player; drilling ores while sneaking with vein miner enabled; disconnecting during a drill; cancelled/protected-region blocks; switching the held item mid-run; and injecting a failing block-break listener to observe cleanup.

## Risks, Edge Cases, And Unknowns

- **High — overlapping drill cascade:** A second drill overwrites the first tracker; either runner can then misclassify the other's events and can remove the other's tracker. This is a direct remaining route to self-replenishing drill tasks.
- **High — drill/vein-miner cascade:** Drill-generated ore events can start several vein miners, whose one-list tracker can be overwritten. Their synthetic events can then recursively start more vein miners. This may be observed by users as the power drill continuing forever even when the active work is largely vein-miner work.
- **High — async Bukkit access and non-thread-safe maps:** Preparation runs off-thread while accessing Bukkit objects and writing `HashMap` state that the main thread consumes. Race timing can change which tracker owns subsequent events and can make the cascade intermittent.
- **Medium — disconnect does not stop work:** Quit cleanup deletes discrimination state before stopping the producer, while the producer is never cancelled. Remaining synthetic events can be misclassified, create no-op async drill tasks for the offline player, or continue changing the world.
- **Medium — abnormal cleanup is incomplete:** Drill and vein-miner state cleanup is tied to normal completion. Early return, scheduling rejection, or runtime failure can retain player/block references, metadata, or active UUIDs.
- **Medium — leaked vein-miner active flag:** Insufficient hunger returns without clearing `activeVeinMinerPlayers`; exceptions and disconnects have the same risk. The resulting symptom is future drills being silently skipped, not infinite execution.
- **Low — empty tracker retention:** Any held-power-drill event creates a `Player` key before unlock/reward/vein checks. Failed starts retain the entry until quit, though repeated events reuse it.
- **Low — cancelled break behavior:** Cancelled synthetic breaks remain in the finite batch progression, but cancelled initial breaks can still launch a drill. This is primarily a protection/behavior issue rather than an infinite-run cause.
- Unknown: The exact historical reproduction steps for issue `#284` are not stored in the repository; only the commit message and diff are locally available.
- Unknown: Other installed server plugins may synthesize, cancel, or re-dispatch `BlockBreakEvent`s in ways that widen the tracker-ownership race.

## Downstream Guidance

- Requirements should account for: explicit ownership of every synthetic drill/vein event; a defined concurrency policy per player; guaranteed cleanup on completion, failure, disconnect, and disable; main-thread Bukkit access; preservation of per-block event cancellation; and a hard bound on total spawned work, not merely each individual runner.
- Planning should consider: power drill and vein miner together as one event-production graph; tests that count active tasks and emitted events under overlap; ownership-aware cleanup so one task cannot remove another task's state; disconnect/error tests; and protected-region cancellation behavior.
- Do not include: unrelated Power Ore challenge/persistence refactors, recipe/item-model changes, or broad cleanup of other scheduled abilities.
