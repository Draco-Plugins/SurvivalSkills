## Title: Add god quest progress to the /godquest GUI and broadcast phase-group completions

## Tags

Complexity Classification: T2
Severity: Medium
Reason: Two-part feature spanning the god_questline subsystem. Blast Radius=2 (3-4 files: GodRecipeUI.java, GodQuestCommand.java, GodTrophyQuest.java, possibly a new step-metadata representation). Uncertainty=2 (per-step goal and next-step values are only hard-coded inside each phase handler's `bulkStep(...)` calls and success-dialogue strings, so deriving them generically for the GUI requires restructuring; the completion-percentage definition for the broadcast is also open). Behavior=3 (UI state rendering, step-metadata derivation, phase-transition detection for the broadcast). Testing=2 (GUI rendering is hard to unit-test without a server; refactoring step data risks breaking quest progression). Reversibility=1 (additive UI and broadcast; godquests.yml schema unchanged). Total=10.
Needs research before implementation: Yes
Research needed: (1) How to generically expose per-step "goal", "count", and "next step" for the GUI — the values currently exist only as hard-coded arguments in each phase handler's `bulkStep()`/`handIn*Step()` calls and in the success-dialogue strings, so a structured step-metadata representation must be designed and refactored into GodTrophyQuest. (2) How the broadcast's completion percentage is computed — per group (`countCompletedGroups()` / `GOD_QUEST_GROUPS.size()`) vs. per phase (`phase`/`maxPhase`) — and which `PhaseGroup` boundaries (FARMING 1-12, ORE 13-21, etc.) trigger a broadcast.

## Summary

The existing `/godquest` command opens a GUI that only renders the crafting recipes for the current stage — it gives the player no sense of their quest progress. This issue extends that GUI to show the current step, current count, goal, and next step, and adds a server broadcast whenever a player completes a full phase group of the god quest (e.g., all Farming phases), announcing the player's completion percentage.

## Steps to Reproduce Context

1. Start a server, begin the god quest, and run `/godquest`.
2. Observe that the opened GUI ("God Recipes: Page x/y") shows only crafting recipes for the current stage.
3. Complete a full phase group (e.g., all 12 Farming phases, phases 1-12) and note that nothing is broadcast to the server.

## Expected Behavior

- The `/godquest` GUI shows quest progress: the current step (e.g., the item being collected), the current collected count vs. the goal, and the next step that follows.
- When a player completes a full phase group of the god quest (e.g., all Farming phases are done), a server broadcast announces that the player has completed Y% of the god quest.
- The existing recipe display remains fully functional.

## Actual Behavior

- The `/godquest` GUI (`GodRecipeUI`) renders only recipes for the current stage with no progress information. `GodQuestCommand` builds it solely from the stage's recipe keys and passes no quest state (`GodQuestCommand.java` lines 48-51).
- No broadcast is sent when a phase group is completed; `GodTrophyQuest` advances `phase` and updates particles but never notifies the server.

## Requirements for completed issue

1. The `/godquest` GUI displays the player's current step, current count, goal, and next step for their active god quest, in addition to the recipes.
2. Completing a full god quest phase group (e.g., all Farming phases) triggers a server broadcast stating that the player has completed Y% of the god quest; the percentage must be correct for every phase group.
3. Existing behavior is unaffected: recipe rendering still works, quest progression/phase advancement is unchanged, and godquests.yml persistence is unchanged.

## Context

- Files:
    - `src/main/java/sir_draco/survivalskills/commands/default_commands/GodQuestCommand.java` — `/godquest` executor; opens the recipe UI without passing quest progress state.
    - `src/main/java/sir_draco/survivalskills/god_questline/GodRecipeUI.java` — the existing paginated recipe GUI; no progress information.
    - `src/main/java/sir_draco/survivalskills/god_questline/GodTrophyQuest.java` — holds all quest state (`phase`, `currentItemCount`, `maxPhase`, `PhaseGroup`) and all phase-completion paths.
    - `src/main/java/sir_draco/survivalskills/skill_listeners/GodListener.java` — owns the `openGodRecipeUI` session map and routes GUI click/drag/close events.
- Code Snippets:

    `/godquest` only passes recipe keys to the UI — no quest state — `GodQuestCommand.java` lines 36-51:

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

    The GUI builds only recipe pages — `GodRecipeUI.java` line 107:

    ```java
    Inventory inv = Bukkit.createInventory(null, 36, "God Recipes: Page " + currPage + "/" + totalPages);
    ```

    Quest state available for progress display — `GodTrophyQuest.java`:

    ```java
    private int currentItemCount = 0;          // line 36
    private int phase = 0;                     // line 38
    private final int maxPhase = 59;           // line 33

    public int getCurrentItemCount() { ... }   // line 592
    public int getPhase() { ... }              // line 596
    public Optional<Integer> getStage() { ... }// line 600
    ```

    `PhaseGroup` definitions (each group = one broadcast trigger) — `GodTrophyQuest.java` lines 40-69:

    ```java
    INTRO(0, 0, 0),
    FARMING(1, 12, 0),
    ORE(13, 21, 0),
    CREATURE(22, 26, 1),
    KNOWLEDGE(27, 44, 2),
    RELIC(45, 46, 3),
    VILLAGER(47, 47, 0),
    COMBAT(48, 48, 4),
    MOB_ITEM(49, 57, 0),
    ADVANCEMENT(58, 58, 0);
    ```

    Per-step goal and next-step values are hard-coded inside each phase handler, e.g. `checkFarmingQuest` — `GodTrophyQuest.java` lines 216-220:

    ```java
    public void checkFarmingQuest(Player p, int cropType) {
        switch (cropType) {
            case 1 -> bulkStep(p, 2000, Material.BREAD, "bread",
                    lines("Excellent Work!", "Now bring me " + ChatColor.AQUA + "5,000 " + ChatColor.WHITE + "carrots"),
                    false);
    ```

    Existing group-completion detection used for trophy particles — `countCompletedGroups`, `GodTrophyQuest.java` line 556:

    ```java
    private static int countCompletedGroups(int phase) {
        return (int) GOD_QUEST_GROUPS.stream()
                .filter((PhaseGroup group) -> phase > group.end)
                .count();
    }
    ```

    Phase-completion paths where a group-completion broadcast would hook — `GodTrophyQuest.java` lines 656-674:

    ```java
    private void onBulkComplete(Player p, List<String> messages, boolean updateParticles) {
        currentItemCount = 0;
        dialogue(p, messages);
        phase++;
        if (updateParticles)
            updateGodTrophyParticles();
    }

    private void onHandInComplete(Player p, List<String> messages, boolean updateParticles) {
        removeItemFromMainHand(p);
        currentItemCount = 0;
        dialogue(p, messages);
        phase++;
        if (updateParticles)
            updateGodTrophyParticles();
    }
    ```

    Existing server-broadcast patterns — `SkillManager.java` line 405 and `PlayerListener.java` line 355:

    ```java
    plugin.getServer().broadcastMessage(ChatColor.AQUA + p.getName() + " has maxed out all of their skills!");
    // ...
    Bukkit.broadcastMessage(ChatColor.GOLD + p.getName() + " has crafted the " + ChatColor.AQUA + trophyType.getName());
    ```

## Notes

- The quest NPC dialogue already tells players to "use /godquest to see the recipe", so the command is the natural home for the progress display.
- `PhaseGroup.INTRO` (phase 0) should not be a broadcast trigger; only the eight quest groups in `GOD_QUEST_GROUPS` represent real milestones.
- Per-phase goals are embedded in each handler's `bulkStep(p, <goal>, ...)` call, and the next step's goal is only present in the success-dialogue text (e.g., "Now bring me 5,000 carrots"). Exposing "goal" and "next step" generically will require extracting this into structured metadata rather than parsing dialogue strings.
