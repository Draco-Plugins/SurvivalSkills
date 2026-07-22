# Research: Wardrobe Armor Set GUI

## Overview

SurvivalSkills registers player commands centrally and uses Bukkit inventory listeners for interactive menus. Player skill levels are held in memory by `SkillManager`, while item-backed features persist Bukkit `ItemStack` values to YAML during player lifecycle events and plugin shutdown.

## Issue Context

- User/requested outcome: Add `/wardrobe`, with three fighting-level-gated armor-set storage areas, clearly identified armor positions, one-click set swapping, restricted inventory interaction, and YAML persistence on quit and shutdown.
- Current pain or bug: No wardrobe command, armor-set GUI, or wardrobe persistence exists.
- Scope classification, if known: T3

## Current Behavior

- `src/main/java/sir_draco/survivalskills/utils/CommandRegistry.java:13` constructs every command executor during plugin startup.
- `src/main/resources/plugin.yml:10` declares commands and assigns the default player permission through `survivalskills.default`.
- `src/main/java/sir_draco/survivalskills/skills/SkillManager.java:450` exposes a player's current level by exact `SkillCategory`.
- `src/main/java/sir_draco/survivalskills/super_enchanting/SuperEnchantingGui.java:56` identifies its GUI with a custom `InventoryHolder`, distinguishes top and bottom inventory clicks, restricts shift-click and drag behavior, and returns retained input on close.
- `src/main/java/sir_draco/survivalskills/skill_listeners/ArmorListener.java:130` refreshes custom armor effects after native armor-slot and shift-click interactions. Programmatic armor replacement is not currently an entry path into this listener.
- `src/main/java/sir_draco/survivalskills/skill_listeners/PlayerListener.java:109` saves player data before removing the player's in-memory state on quit.
- `src/main/java/sir_draco/survivalskills/SurvivalSkills.java:157` closes online inventories and persists other item-storage features during plugin shutdown.

## Relevant Files And Entry Points

- `src/main/java/sir_draco/survivalskills/SurvivalSkills.java:87` - Plugin startup, listener construction, and shutdown persistence.
- `src/main/java/sir_draco/survivalskills/utils/CommandRegistry.java:13` - Central command executor registration.
- `src/main/resources/plugin.yml:10` - Bukkit command and permission declarations.
- `src/main/java/sir_draco/survivalskills/skills/SkillManager.java:450` - Fighting-level lookup.
- `src/main/java/sir_draco/survivalskills/super_enchanting/SuperEnchantingGui.java:45` - Current holder-based GUI interaction pattern.
- `src/main/java/sir_draco/survivalskills/skill_listeners/ArmorListener.java:206` - Custom armor activation and removal tracking.
- `src/main/java/sir_draco/survivalskills/skill_listeners/god/PotionBagListener.java:120` - Existing Bukkit `ItemStack` YAML serialization pattern.
- `src/main/java/sir_draco/survivalskills/skill_listeners/PlayerListener.java:109` - Player quit persistence lifecycle.

## Data Flow Or Control Flow

1. Bukkit resolves a declared player command to an executor registered by `CommandRegistry`.
2. The executor reads the player's `SkillCategory.FIGHTING` level from `SkillManager` and opens a custom-holder inventory.
3. Inventory listeners validate top-inventory slots, click actions, shift-clicks, and drags before changing stored items.
4. Armor changes made through the GUI must be reflected into `PlayerInventory` and then reconciled with `ArmorListener`'s custom armor trackers.
5. Stored Bukkit `ItemStack` values can be written directly into `YamlConfiguration`, following existing item-container persistence.
6. Quit and disable lifecycle hooks save cached wardrobe state before it is discarded.

## Important Contracts And Constraints

- The unlock checks use `SkillCategory.FIGHTING`, not Minecraft experience levels or main skill level.
- Inventory item ownership must remain singular across cursor, player inventory, equipped armor, GUI rendering, and YAML state to avoid duplication or loss.
- All top-inventory click variants, hotbar swaps, collection actions, shift-clicks, and drags need explicit handling because a GUI renders stored items.
- Bukkit armor contents use their own equipment ordering; named helmet, chestplate, leggings, and boots accessors avoid order mistakes.
- Programmatic armor swaps need an explicit armor-effect refresh because existing `ArmorListener` event paths only observe native interactions.
- Bukkit `YamlConfiguration` supports direct serialization of `ItemStack` metadata, enchantments, and custom model data.

## Existing Tests And Validation

- `src/test/java/sir_draco/survivalskills/super_enchanting/SuperEnchantingRulesTest.java:1` demonstrates JUnit 5 conventions for GUI-adjacent rules, but does not exercise Bukkit inventory events.
- `src/test/java/sir_draco/survivalskills/skill_listeners/ArmorUpgradeCraftingListenerTest.java:1` covers armor-related crafting only.
- No existing test covers a player-owned multi-slot item GUI, armor-set swapping, or per-player item YAML persistence.
- The Maven test suite and package build are the discovered automated validation surfaces.

## Risks, Edge Cases, And Unknowns

- Empty armor positions need visual labels without placing removable placeholder items into the actual storage cells.
- Locked sets must remain inaccessible even if the GUI was opened before a level change.
- A full equipped set and a fully empty saved set are both valid swap states.
- Custom armor effects may remain stale unless every armor type is re-evaluated after an atomic swap.
- Failure to save YAML must be logged without deleting the in-memory set.
- The request does not specify whether curse-of-binding semantics should block wardrobe swaps.

## Downstream Guidance

- Requirements should account for: exact level thresholds 28/42/58, exact armor-type validation for each named slot, safe handling of all inventory actions, atomic swaps, and both quit and shutdown persistence.
- Planning should consider: a dedicated wardrobe state/persistence component, a custom-holder GUI listener, command registration, lifecycle wiring, armor-effect refresh, and focused unit tests for immutable set/layout rules.
- Do not include: unrelated reward configuration changes, changes to global skill progression, or refactors of existing inventory systems.
