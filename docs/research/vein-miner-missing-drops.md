# Research: Vein Miner Missing Ore Drops

## Overview

Vein miner captures the tool used to start the ability and later breaks connected ores in a tick-based task. Each scheduled break fires a synthetic `BlockBreakEvent`, but the mining reward listener calculates custom drops from the player's current main-hand item instead of the captured tool, allowing a hotbar change during the task to turn valid ore drops into an empty collection.

## Issue Context

- User/requested outcome: Prevent vein-mined ore drops from disappearing, including while a magnet is held during an active vein-mining task.
- Current pain or bug: Drops are rarely lost during normal vein mining and consistently lost with the reported magnet/active-task reproduction.
- Scope classification, if known: T2

## Current Behavior

- `VeinMinerAsync.run()` clones the player's main-hand item when the task is prepared and passes that captured item to the scheduled batch runner (`src/main/java/sir_draco/survivalskills/abilities/VeinMinerAsync.java:58`).
- Each batch iteration marks the player with boolean vein-miner metadata, fires a synthetic `BlockBreakEvent`, and then uses the captured pickaxe for the actual natural block break (`src/main/java/sir_draco/survivalskills/abilities/VeinMinerAsync.java:175`).
- `MiningSkill.doubleOre()` handles that synthetic event and independently queries drops using whatever item is currently in the player's main hand (`src/main/java/sir_draco/survivalskills/skill_listeners/MiningSkill.java:386`).
- When the fortune reward succeeds, `doubleOre()` disables event drops and manually spawns the queried stacks (`src/main/java/sir_draco/survivalskills/skill_listeners/MiningSkill.java:402`).
- `VeinMinerAsync.breakBlock()` interprets disabled event drops as confirmation that a listener already handled them and changes the block directly to air (`src/main/java/sir_draco/survivalskills/abilities/VeinMinerAsync.java:198`).
- Inference from the combined flow: if the player switches from the captured pickaxe to a magnet or another invalid mining tool, `Block.getDrops(currentItem)` can be empty; a successful double-drop roll then disables natural drops, manually spawns nothing, and causes vein miner to replace the ore with air.
- The magnet task itself only changes nearby item velocities and does not remove item entities (`src/main/java/sir_draco/survivalskills/abilities/items/Magnet.java:31`).

## Relevant Files And Entry Points

- `src/main/java/sir_draco/survivalskills/skill_listeners/MiningSkill.java:82` - Main block-break listener that applies mining rewards and starts vein miner.
- `src/main/java/sir_draco/survivalskills/skill_listeners/MiningSkill.java:386` - Double-ore drop replacement logic and the current-main-hand lookup.
- `src/main/java/sir_draco/survivalskills/skill_listeners/MiningSkill.java:428` - Vein-miner admission and task scheduling.
- `src/main/java/sir_draco/survivalskills/abilities/VeinMinerAsync.java:45` - Captures the starting tool and schedules the batch.
- `src/main/java/sir_draco/survivalskills/abilities/VeinMinerAsync.java:153` - Fires synthetic block-break events and performs scheduled breaks.
- `src/main/java/sir_draco/survivalskills/abilities/items/Magnet.java:24` - Periodic magnet behavior; relevant only because holding it replaces the current main-hand pickaxe.
- `src/test/java/sir_draco/survivalskills/abilities/VeinMinerAsyncTest.java:21` - Existing cleanup and drop-enabled/drop-disabled vein-miner tests.
- `src/test/java/sir_draco/survivalskills/skill_listeners/MiningSkillTest.java:35` - Existing mining reward and vein-miner admission tests.

## Data Flow Or Control Flow

1. A sneaking player breaks an ore with vein miner enabled, and `MiningSkill.onBlockBreak()` applies rewards and calls `veinminerChecker()`.
2. `VeinMinerAsync` discovers connected blocks and clones the current main-hand tool before scheduling one block break per tick.
3. The player can switch hotbar slots while the batch remains active; selecting the magnet changes the current main-hand item but not the batch's captured pickaxe.
4. For each connected ore, the batch fires a synthetic `BlockBreakEvent`; `MiningSkill.doubleOre()` derives possible drops from the current main hand.
5. On a successful double-drop roll, the listener disables event drops and manually spawns its calculated collection.
6. Vein miner sees event drops disabled and sets the block to air without calling `breakNaturally()`, relying on the listener's manual drops.

## Important Contracts And Constraints

- All drop calculations for one scheduled vein-miner break need to use the same captured tool as the final `Block.breakNaturally(tool)` call.
- `VEIN_MINER_BREAK_METADATA` is also used by `PowerOreChallengeListener` as a presence-only guard; its presence and cleanup lifetime must remain intact.
- A cancelled synthetic block-break event must continue to preserve the block.
- When another listener deliberately disables event drops and handles them, vein miner must not create duplicate natural drops.
- The existing batch cleanup removes metadata and active-player/tracked-vein state after completion or failure.

## Existing Tests And Validation

- `src/test/java/sir_draco/survivalskills/abilities/VeinMinerAsyncTest.java:46` verifies that enabled event drops use the captured tool with `breakNaturally()`.
- `src/test/java/sir_draco/survivalskills/abilities/VeinMinerAsyncTest.java:57` verifies that disabled event drops change the block to air without duplicating drops.
- `src/test/java/sir_draco/survivalskills/skill_listeners/MiningSkillTest.java:133` covers which ore drops are eligible for doubling but does not cover a hotbar change during vein mining.
- Missing coverage: double-ore calculation should use the captured vein-miner tool when the player's current main hand is a magnet or another non-tool item.

## Risks, Edge Cases, And Unknowns

- The magnet is a deterministic reproduction aid, not a direct deletion path; switching to any item that cannot mine the ore can trigger the same failure when the double-drop chance succeeds.
- Third-party listeners may inspect the player's live inventory during the synthetic event; changing SurvivalSkills' own tool context will not alter their behavior.
- Metadata with the same key can be owned by multiple plugins, so any stored tool lookup must select SurvivalSkills' metadata value.
- The reported 100% reproduction implies the affected player likely has a guaranteed fortune reward; lower fortune chances explain the rare normal occurrence.

## Downstream Guidance

- Requirements should account for: consistent captured-tool drop semantics throughout a vein-mining batch, preserved event cancellation/drop suppression behavior, and cleanup of per-event context.
- Planning should consider: carrying the captured tool in the existing per-break metadata, consuming only plugin-owned metadata in `doubleOre()`, and adding a focused regression test for a different current main-hand item.
- Do not include: magnet movement changes, vein discovery changes, fortune balance changes, or duplicate-drop fallback behavior that would ignore intentional `setDropItems(false)` calls.
