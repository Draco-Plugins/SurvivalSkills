# Research: Preserve Enchantments In Armor Upgrade Recipes

## Overview

Reward armor recipes are declared as shaped Bukkit recipes and custom-item ingredients are registered with `RecipeChoice.ExactChoice`. Three armor progressions consume an earlier custom armor piece, so any enchantment added to that piece changes its metadata and prevents Bukkit from matching the recipe.

## Issue Context

- User/requested outcome: Allow existing custom armor to be upgraded while retaining its enchantments on the upgraded armor.
- Current pain or bug: Enchanted custom armor no longer matches the exact recipe ingredient.
- Scope classification, if known: T2

## Current Behavior

- `RecipeMaker.createSmallShapedRecipe` registers every non-null custom `ItemStack` ingredient as an exact choice (`src/main/java/sir_draco/survivalskills/utils/RecipeMaker.java:88`).
- Traveler recipes consume Wanderer pieces, Adventurer recipes consume Traveler pieces, and Power recipes consume Beacon pieces (`src/main/java/sir_draco/survivalskills/utils/Recipes/RewardRecipeData.java:69`, `:100`, `:158`).
- Bukkit performs recipe recognition before `CraftItemEvent`; there is currently no `PrepareItemCraftEvent` handler that could validate a relaxed ingredient or customize the preview result.
- `PlayerListener.playerCraftEvent` applies reward gates from the registered recipe result after a recipe has matched (`src/main/java/sir_draco/survivalskills/skill_listeners/PlayerListener.java:137`).

## Relevant Files And Entry Points

- `src/main/java/sir_draco/survivalskills/utils/Recipes/RewardRecipeData.java:69` - Declares all twelve armor-upgrade recipes and their exact custom inputs.
- `src/main/java/sir_draco/survivalskills/utils/RecipeMaker.java:88` - Converts declarative exact inputs into Bukkit `ExactChoice` ingredients.
- `src/main/java/sir_draco/survivalskills/SurvivalSkills.java:203` - Constructs and registers gameplay listeners.
- `src/main/java/sir_draco/survivalskills/skill_listeners/PlayerListener.java:137` - Enforces reward-level restrictions when a custom recipe is crafted.

## Data Flow Or Control Flow

1. Reward recipes are built asynchronously during plugin enable and deferred to `RecipeRegistrar` for Bukkit registration.
2. Bukkit compares the crafting matrix against each recipe, including full item metadata for exact choices.
3. An unmodified custom armor input matches and exposes the fixed upgraded result; an enchanted input does not match and exposes no result.
4. Once a valid custom result is taken, existing craft listeners award XP and enforce the relevant reward unlock.

## Important Contracts And Constraints

- Only the intended prior-tier custom armor piece may satisfy an upgrade slot; a vanilla armor item of the same material must remain invalid.
- The upgraded item must retain its own custom name, lore, model data, persistent data, unbreakable flag, color, and attribute modifiers.
- Existing reward restrictions and normal crafting ingredient consumption must continue to use Bukkit's standard craft flow.
- Only enchantments are requested for transfer; unrelated input metadata should not silently replace upgraded-item metadata.

## Existing Tests And Validation

- No existing tests cover `PrepareItemCraftEvent`, recipe recognition with changed metadata, or armor upgrade result mutation.
- Maven/JUnit is configured in `pom.xml`; repository validation is available through `mvn test` and `mvn package`/compile phases.

## Risks, Edge Cases, And Unknowns

- Relaxing an upgrade slot to a material choice without a preparation-time identity check would allow vanilla armor to be substituted.
- Validation must compare the custom item while disregarding only enchantments; disregarding all metadata would defeat the exact-choice security boundary.
- Craft-time validation should accompany preview validation so a stale or externally modified crafting result cannot bypass the identity check.
- Enchantments may include levels or combinations not normally legal on the output; preserving the actual prior item requires copying them without recalculating legality.

## Downstream Guidance

- Requirements should account for: all twelve upgrade recipes, exact custom-item identity apart from enchantments, enchantment transfer, invalid vanilla/lookalike rejection, and existing reward gates.
- Planning should consider: narrowly relaxing only upgrade slots, a preparation/craft listener, immutable upgrade descriptors, and focused unit coverage for matching and transfer behavior.
- Do not include: transferring arbitrary metadata such as renamed display names, damage, or unrelated persistent data, or relaxing other custom recipe ingredients.
