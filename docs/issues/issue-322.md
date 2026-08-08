## Title: Add a checklist of remaining advancements to the /godquest GUI

## Tags

Complexity Classification: T2
Severity: Medium
Reason: Feature spanning the god_questline subsystem. Blast Radius=2 (4-5 files: GodQuestCommand.java, a new checklist UI modeled on GodRecipeUI.java, GodListener.java, GodTrophyQuest.java). Uncertainty=2 (the structure is clear, but the "cross them off" semantics are ambiguous and GodListener's session map is typed `Map<Player, GodRecipeUI>` so it must be generalized or paralleled). Behavior=3 (paged checklist GUI with navigation plus advancement filtering/state logic). Testing=1 (pure code addition, no data consequences; existing GUI test precedent available). Reversibility=1 (additive UI; godquests.yml schema unchanged). Total=9.
Needs research before implementation: Yes
Research needed: (1) Clarify "cross them off" semantics — does the checklist reflect actual in-game advancement completion (derived from `AdvancementProgress.isDone()`, no persistence) or require manual toggling with per-player persistence (which escalates the scope)? (2) Whether GodListener's session handling should be generalized to a common UI interface or gain a separate session map for the second UI type. (3) Whether the open checklist should refresh live while open (requires listening for advancement-completion events) or only on the next time it is opened. (4) How to detect the ADVANCEMENT phase in GodQuestCommand — `getStage()` currently returns empty for the ADVANCEMENT group (stage 0), which routes into the "no recipes" error path.

## Summary

The `/godquest` command opens a GUI that only renders the crafting recipes for the current quest stage. During the final phase of the god quest (phase 58, `ADVANCEMENT` group), where the player must complete all Minecraft advancements, `/godquest` simply reports "There are no recipes for this stage of the God Quest". This issue adds a checklist view to the `/godquest` GUI that lists all the advancements the player has not yet completed, so they can work through them one by one and cross them off.

## Steps to Reproduce Context

1. Start a server with `AllAdvancements: true` in config.yml, begin the god quest, and progress to the final ADVANCEMENT phase (phase 58).
2. Run `/godquest`.
3. Observe that the command reports "There are no recipes for this stage of the God Quest" and provides no way to see which advancements still remain.

## Expected Behavior

- Running `/godquest` shows a checklist GUI listing every Minecraft advancement the player has not yet completed.
- The player can track their remaining advancements through the checklist and cross them off as they are completed.
- The existing recipe display functionality remains fully functional.

## Actual Behavior

- `/godquest` opens `GodRecipeUI`, which only renders recipes for the current stage (`GodQuestCommand.java` lines 48-51). During the ADVANCEMENT phase, `quest.getStage()` returns empty (the `ADVANCEMENT` `PhaseGroup` is declared with stage 0, `GodTrophyQuest.java` line 50), so the command calls `sendNoRecipeError(p)` ("There are no recipes for this stage of the God Quest", lines 66-69) and no checklist is shown.
- The only advancement-related logic is `GodTrophyQuest.hasAllAdvancements(Player)` (lines 576-590), which returns a boolean; there is no way to enumerate the player's remaining advancements for display.

## Requirements for completed issue

1. The `/godquest` GUI provides a checklist view listing all Minecraft advancements the player has not yet completed, excluding `recipes/` advancements (consistent with the existing `hasAllAdvancements` check).
2. The player can track their remaining advancements through the checklist and see each one crossed off once it is completed.
3. Existing behavior is unaffected: recipe rendering still works, quest progression/phase advancement is unchanged, and godquests.yml persistence is unchanged.

## Context

- Files:
    - `src/main/java/sir_draco/survivalskills/commands/default_commands/GodQuestCommand.java` — `/godquest` executor; opens the recipe UI or errors out for stages without recipes.
    - `src/main/java/sir_draco/survivalskills/god_questline/GodRecipeUI.java` — the existing paginated recipe GUI whose paging/navigation pattern can be reused.
    - `src/main/java/sir_draco/survivalskills/god_questline/GodTrophyQuest.java` — holds quest state (`phase`, `PhaseGroup`) and the `hasAllAdvancements` advancement iteration logic.
    - `src/main/java/sir_draco/survivalskills/skill_listeners/GodListener.java` — owns the `openGodRecipeUI` session map and routes GUI click/drag/close events.
    - `src/main/resources/config.yml` — `AllAdvancements` option (line 38) that toggles whether the god quest requires all advancements.
- Code Snippets:

    `/godquest` routes only to the recipe UI and errors for recipe-less stages — `GodQuestCommand.java` lines 36-69:

    ```java
    GodTrophyQuest quest = plugin.getTrophyManager().getPlayerGodQuestData().get(p.getUniqueId());
    if (quest == null) {
        sendQuestError(p);
        return true;
    }

    Optional<Integer> stage = quest.getStage();
    if (stage.isEmpty()) {
        sendNoRecipeError(p);
        return true;
    }

    GodRecipeUI ui = new GodRecipeUI(getRecipeList(stage.get()));
    plugin.getGodListener().registerGodRecipeUI(p, ui);
    ui.open(p);
    ```

    The `ADVANCEMENT` phase group has stage 0, so `getStage()` is empty during the final phase — `GodTrophyQuest.java` lines 40-69 and 600-605:

    ```java
    INTRO(0, 0, 0),
    ...
    MOB_ITEM(49, 57, 0),
    ADVANCEMENT(58, 58, 0);
    ```

    ```java
    public Optional<Integer> getStage() {
        PhaseGroup group = PHASE_TO_GROUP.get(phase);
        if (group != null && group.stage > 0)
            return Optional.of(group.stage);
        return Optional.empty();
    }
    ```

    Existing advancement-completion logic that the checklist can mirror — `GodTrophyQuest.java` lines 576-590:

    ```java
    public boolean hasAllAdvancements(Player p) {
        Iterator<Advancement> advancementIterator = Bukkit.advancementIterator();

        while (advancementIterator.hasNext()) {
            Advancement advancement = advancementIterator.next();
            // Skip recipe unlocks — they auto-complete and don't count as
            // visible advancements for the player
            if (advancement.getKey().getKey().startsWith("recipes/"))
                continue;
            if (!p.getAdvancementProgress(advancement).isDone())
                return false;
        }

        return true;
    }
    ```

    The checklist UI is invoked when the player reaches the ADVANCEMENT phase — `GodTrophyQuest.java` line 466:

    ```java
    public void checkAdvancementsQuest(Player p) {
        if (hasAllAdvancements(p) || !allAdvancements) {
            onBulkComplete(p, getGodTrialCompletionMessage(), true);
        } else {
            dialogue(p, lines("You have not yet achieved all advancements.",
                    "Keep working hard and you will get there!"));
        }
    }
    ```

    Existing paged-GUI navigation pattern to reuse — `GodRecipeUI.java` lines 33-63:

    ```java
    public void open(Player p) {
        currentInv = 0;
        p.openInventory(inventories.get(currentInv));
    }

    public void handleClick(InventoryClickEvent e) {
        ...
        if (ItemStackGeneratorUtils.hasCustomModelData(meta)) {
            if (currentInv + 1 >= inventories.size()) currentInv = -1;
            currentInv += 1;
            Inventory inv = inventories.get(currentInv);
            p.openInventory(inv);
        }
        ...
    }
    ```

## Notes

- The ADVANCEMENT phase (58) has stage 0, so it currently falls into the "no recipes" error path; the checklist GUI is the natural replacement for that phase, and `GodQuestCommand` will need a branch that detects the ADVANCEMENT phase and opens the checklist instead.
- Modern Minecraft has a large number of advancements, so the checklist GUI will need pagination; the `GodRecipeUI` paging pattern (36-slot inventories with Next/Back arrows) can be reused.
- "Cross them off" is ambiguous: either (a) the checklist reflects actual in-game advancement completion via `AdvancementProgress.isDone()` and updates accordingly, or (b) the player manually toggles items in the GUI — interpretation (b) would require per-player persistence and escalate the scope. Research is needed to confirm the intended behavior.
- `GodListener` registers UI sessions as `Map<Player, GodRecipeUI>`; supporting a second GUI type requires generalizing that session handling or adding a parallel map.
