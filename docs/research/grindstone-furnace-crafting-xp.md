# Research: Grindstone And Furnace Crafting XP

## Overview

Crafting XP is awarded by `CraftingSkill` for `CraftItemEvent` interactions with the crafting grid. Stonecutter and furnace result extraction use inventory click events instead, so they currently bypass that XP path.

## Issue Context

- User/requested outcome: Award `CRAFTING_XP` per item crafted in a Stonecutter and when a player takes smelted items from a furnace result slot.
- Current pain or bug: Stonecutter and furnace result extraction do not award crafting XP.
- Scope classification, if known: T3

## Current Behavior

- `CraftingSkill.onCraftEvent` awards the configured crafting XP for normal and shift-click crafting-grid operations.
- `SurvivalSkills.loadListeners` registers one `CraftingSkill` listener, making it the existing entry point for crafting-related XP.
- The listener handles furnace-family result extraction at top-inventory slot 2, but Stonecutter output is at top-inventory slot 1 and is therefore currently skipped.
- Stonecutter shift-clicks expose only the current single-item result stack before Bukkit processes the transfer, so using that stack amount alone cannot represent all items actually crafted or inventory-capacity limits.

## Relevant Files And Entry Points

- `src/main/java/sir_draco/survivalskills/skill_listeners/CraftingSkill.java` - Existing crafting XP listener and shared access to the configured `CraftingXP` multiplier.
- `src/main/java/sir_draco/survivalskills/SurvivalSkills.java` - Registers `CraftingSkill` with Bukkit.
- `src/main/java/sir_draco/survivalskills/skills/SkillManager.java` - Provides `getCraftingXP()` and applies XP through `experienceEvent`.
- `src/main/resources/config.yml` - Defines the default `CraftingXP` value.

## Data Flow Or Control Flow

1. A player crafts in the crafting grid, or extracts an output from a Stonecutter or furnace-family inventory.
2. Crafting-grid actions trigger `CraftItemEvent`; processing output extraction triggers `InventoryClickEvent`.
3. `CraftingSkill` reads `SkillManager.getCraftingXP()` and calls `SkillManager.experienceEvent` for `SkillCategory.CRAFTING`.
4. `experienceEvent` applies configured multipliers, updates the player's crafting skill, and refreshes related UI/state.

## Important Contracts And Constraints

- XP must use the existing `SkillManager.experienceEvent` path so multipliers, caps, level-ups, and notifications remain consistent.
- XP should be based on the number of output items removed from the result slot, or the input items actually consumed for a Stonecutter shift-click.
- Cancelled clicks and clicks that do not remove an output must not award XP.
- Existing crafting-grid behavior, including shift-click quantity handling and custom-item restrictions, must remain unchanged.

## Existing Tests And Validation

- `src/test/java/sir_draco/survivalskills/skill_listeners/MiningSkillTest.java` demonstrates the repository's listener-test style with Mockito.
- There is no existing `CraftingSkill` test covering inventory result extraction.
- Maven compilation and the test suite are the available validation surfaces.

## Risks, Edge Cases, And Unknowns

- Furnace-family inventories have a result slot at slot 2, while Stonecutters have a result slot at slot 1; the implementation must distinguish those top-inventory slots from player-inventory slots.
- Furnace shift-clicks expose the full output stack, while Stonecutter shift-clicks may craft repeatedly from the input stack. The Stonecutter input delta after Bukkit processes the click reflects the number of recipes actually crafted and naturally excludes items blocked by inventory capacity; that delta must be multiplied by the pre-click result amount to count every output item.
- Automated coverage is unit-level; live-server behavior for unusual click actions remains a manual validation surface.

## Downstream Guidance

- Requirements should account for Stonecutter and furnace-family result extraction, output quantities, Stonecutter shift-click input consumption, and cancelled/non-removing click behavior.
- Planning should keep the change within `CraftingSkill` where the listener is already registered and reuse `SkillManager.experienceEvent`.
- Do not alter XP configuration, player-data formats, or unrelated crafting rewards.
