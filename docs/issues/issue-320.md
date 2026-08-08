## Title: Add a recovery recipe for a lost God Trophy Base

## Tags

Complexity Classification: T1
Severity: Medium
Reason: A small, well-patterned feature addition touching two files. Recipe registration in RecipeMaker.java follows the existing `registerGodTrophy`/`registerColorTrophy` patterns (shapeless: gold pressure plate + nether star -> `getGodTrophyBase()`, reusing the existing GOD_TROPHY_BASE model data, so no new item is needed). The level-100 gate is a small new branch in PlayerListener.java (model data 45 currently has no CRAFT_RESTRICTIONS entry and is not the TROPHY/999 path), reusing the exact `SkillManager.getSkillLevel(...) == Skill.MAX_LEVEL` pattern already present in `handleTrophyCraft`. No config.yml/pom.xml version bump (not a skill reward), no data-model/trophydata changes.
Needs research before implementation: No

## Summary

The God Trophy Base is awarded exactly once per player. If a player loses the item (e.g., lava, void) before crafting the God Trophy, non-admin players have no recovery path and the god questline is permanently soft-locked. Add a simple recovery recipe (gold pressure plate + nether star) that produces the God Trophy Base and only works when the player is main level 100.

## Steps to Reproduce Context

1. Reach main level 100 (`Skill.MAX_LEVEL`), which triggers `SkillManager.awardGodTrophy` and awards exactly one God Trophy Base.
2. Lose the God Trophy Base (e.g., drop it into lava or the void) before crafting the God Trophy.
3. Attempt to continue the god questline — there is no way to obtain another base without admin assistance (`/ssget godtrophybase`).

## Expected Behavior

A player who has lost their God Trophy Base can recover it through a new crafting recipe (gold pressure plate + nether star) gated on main level 100, so the god questline is never permanently soft-locked for non-admin players.

## Actual Behavior

The God Trophy Base is awarded only once; `TrophyType.GOD` is permanently marked true in the trophy tracker, so `awardGodTrophy` never runs again. Non-admin players who lose the item have no recovery path and the god questline is permanently soft-locked.

## Requirements for completed issue

1. Add a recipe that produces the God Trophy Base (`ItemStackGenerator.getGodTrophyBase()`) from a gold pressure plate and a nether star.
2. The recipe must only be craftable when the player's main skill level is 100 (`Skill.MAX_LEVEL`); craft attempts below that level must be rejected.
3. The recovery path must work without admin commands or permissions (must not rely on `/ssget godtrophybase`).
4. Existing behavior must be preserved: the base is still awarded once at max level, and the God Trophy craft itself is unchanged.

## Context

- Files:
    - `src/main/java/sir_draco/survivalskills/skills/SkillManager.java` (`awardGodTrophy`, lines 395-413; `syncMainSkill`, lines 347-393)
    - `src/main/java/sir_draco/survivalskills/utils/items/ItemStackGenerator.java` (`getGodTrophyBase`, lines 1019-1025)
    - `src/main/java/sir_draco/survivalskills/utils/RecipeMaker.java` (`registerGodTrophy`, lines 266-276; `trophyRecipes`, lines 167-198)
    - `src/main/java/sir_draco/survivalskills/skill_listeners/PlayerListener.java` (`playerCraftEvent`, lines 140-170; `handleTrophyCraft`, lines 334-363; `CRAFT_RESTRICTIONS`, lines 65-93)
    - `src/main/java/sir_draco/survivalskills/commands/admin_commands/SurvivalSkillsGetCommand.java` (line 128, admin recovery `/ssget godtrophybase`)
    - `src/main/java/sir_draco/survivalskills/commands/TabCompleter.java` (line 148)
    - `src/main/java/sir_draco/survivalskills/skills/Skill.java` (`MAX_LEVEL = 100`)
    - `src/main/java/sir_draco/survivalskills/utils/items/ItemModelData.java` (`GOD_TROPHY_BASE`, id 45)

- Code Snippets:

    `SkillManager.awardGodTrophy` — one-time award:

    ```java
    private void awardGodTrophy(Player p) {
        Map<TrophyType, Boolean> trophies = plugin.getTrophyManager().getTrophyTracker().get(p.getUniqueId());
        if (trophies == null || trophies.getOrDefault(TrophyType.GOD, false)) return;

        trophies.put(TrophyType.GOD, true);
        plugin.getTrophyManager().getTrophyTracker().put(p.getUniqueId(), trophies);
        if (!p.getInventory().addItem(ItemStackGenerator.getGodTrophyBase()).isEmpty()) {
            p.getWorld().dropItem(p.getLocation(), ItemStackGenerator.getGodTrophyBase());
        }
        ...
    }
    ```

    `ItemStackGenerator.getGodTrophyBase` — the item the recovery recipe should produce:

    ```java
    public static ItemStack getGodTrophyBase() {
        return new ItemStackBuilder(Material.LIGHT_WEIGHTED_PRESSURE_PLATE, 1,
                ColorParser.gradientName("God Trophy Base", "#FFFF00", "#FFFFFF", true))
                .lore(List.of(ChatColor.GRAY + "Used to craft the God Trophy"))
                .modelData(ItemModelData.GOD_TROPHY_BASE.getId())
                .build();
    }
    ```

    `RecipeMaker.registerGodTrophy` — existing God Trophy recipe (B = God Trophy Base), the pattern a new recipe would follow:

    ```java
    private static void registerGodTrophy(NamespacedKey key, Map<Integer, ItemStack> trophyItems) {
        ...
        createSmallShapedRecipe(key, godTrophy, "DAD:ABA:DAD",
                ItemStackGenerator.getPowerOre(), ItemStackGenerator.getGodTrophyBase(), null, null,
                null, null);
        trophyItems.put(10, godTrophy);
    }
    ```

    `PlayerListener.handleTrophyCraft` — existing main-level-100 gate for the God Trophy craft:

    ```java
    if (trophyType == TrophyType.GOD) {
        if (SkillManager.getSkillLevel(p.getUniqueId(), SkillCategory.MAIN) != Skill.MAX_LEVEL) {
            e.setCancelled(true);
            p.sendRawMessage(ChatColor.RED + "You need to be main level " + ChatColor.AQUA + Skill.MAX_LEVEL
                    + ChatColor.RED + " to craft this");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return;
        }
    }
    ```

## Notes

- The God Trophy Base is a `LIGHT_WEIGHTED_PRESSURE_PLATE` with custom model data `GOD_TROPHY_BASE` (id 45). Because `PlayerListener.playerCraftEvent` only handles results that pass `ItemStackGeneratorUtils.isCustomItem`, a crafted base would be caught by that handler; model data 45 currently has no `CRAFT_RESTRICTIONS` entry and is not the `TROPHY` (999) path, so any main-level gate for the new recipe would need to be wired into that flow.
- Recipe registration should follow the existing `RecipeMaker` patterns (e.g., `registerColorTrophy`) and be invoked from `trophyRecipes`.
