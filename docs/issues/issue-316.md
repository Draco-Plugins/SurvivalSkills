## Title: Rapid NPC clicking stacks overlapping dialogue runnables

## Tags

Complexity Classification: T1
Severity: Low
Reason: The fix is confined to a single file (`GodTrophyQuest.java`): track the active dialogue task and cancel it before scheduling a new one. The root cause and affected code paths are fully confirmed by direct code reading. All ~20 `dialogue()` call sites and the `CitizensTrophyListener` entry point remain unchanged. The only consideration beyond the core fix is lifecycle cleanup (e.g., cancelling the active dialogue when the player quits).
Needs research before implementation: No

## Summary

`GodTrophyQuest.dialogue()` creates a new `BukkitRunnable` timer on every call and never cancels a previously-started dialogue timer for the same player. Rapidly right-clicking the God Quest NPC fires multiple overlapping timers that run in parallel, interleaving and duplicating dialogue lines and villager sounds in chat.

## Steps to Reproduce Context

1. Progress the God Quest to any phase that shows dialogue (e.g., the intro, or any bulk-collection/quest step).
2. Right-click the assigned God NPC repeatedly in quick succession (several clicks within a few seconds).
3. Observe multiple concurrent dialogue sequences playing at the same time — the same lines are shown more than once, interleaved with each other, along with repeated villager ambient sounds.

## Expected Behavior

Only one dialogue sequence plays at a time per player. A new NPC interaction either starts a fresh dialogue cleanly or is ignored/queued, but it never runs concurrently with a previous dialogue. Chat shows each line of the dialogue sequence exactly once, in order.

## Actual Behavior

Each `NPCRightClickEvent` on the God NPC calls `quest.handleNPCInteract(p)`, which routes to a phase handler that calls `dialogue(p, messages)`. `dialogue()` (lines 177-192) creates a brand-new anonymous `BukkitRunnable` scheduled via `runTaskTimer(SurvivalSkills.getInstance(), 0, 40)` — a repeating task that sends one message every 40 ticks (2 seconds) and cancels itself only when all messages are sent. `GodTrophyQuest` has no field tracking the active dialogue runnable, and `dialogue()` never cancels a previously-started timer before starting a new one, so rapid clicking stacks several independent timers that all fire at the same time, spamming overlapping/duplicated dialogue.

## Requirements for completed issue

1. Rapid repeated interactions with the God Quest NPC must never produce overlapping, duplicated, or interleaved dialogue messages or sounds — only one dialogue sequence may play at a time per player.
2. Starting a new dialogue must not be corrupted by messages from a previously-started dialogue for the same player.
3. Quest progression behavior (phase advancement, item hand-in, phase routing) must remain unchanged.

## Context

- Files:
  - `src/main/java/sir_draco/survivalskills/god_questline/GodTrophyQuest.java`
  - `src/main/java/sir_draco/survivalskills/external/listeners/CitizensTrophyListener.java`
- Code Snippets:

  The `dialogue()` method that creates a new timer per call — `GodTrophyQuest.java` lines 177-192:
  ```java
  public void dialogue(Player p, List<String> messages) {
      new BukkitRunnable() {
          private int counter = 0;

          @Override
          public void run() {
              if (counter >= messages.size()) {
                  cancel();
                  return;
              }
              p.sendRawMessage(TrophyManager.npcName + ChatColor.WHITE + ": " + messages.get(counter));
              p.playSound(p, Sound.ENTITY_VILLAGER_AMBIENT, 1, 1);
              counter++;
          }
      }.runTaskTimer(SurvivalSkills.getInstance(), 0, 40);
  }
  ```

  The NPC click entry point — `CitizensTrophyListener.java` line 67 (inside `clickGodNPC(NPCRightClickEvent)`):
  ```java
  quest.handleNPCInteract(p);
  ```

  Phase routing into dialogue — `GodTrophyQuest.java` lines 120-140 and 194-211:
  ```java
  public void handleNPCInteract(Player p) {
      if (phase >= maxPhase)
          return;
      PhaseGroup group = PHASE_TO_GROUP.get(phase);
      if (group == null)
          return;
      switch (group) {
          case INTRO -> dialogueOpener(p);
          case FARMING -> checkFarmingQuest(p, phase);
          // ... other phase handlers
      }
  }
  ```
  ```java
  public void dialogueOpener(Player p) {
      List<String> messages = lines(...);
      dialogue(p, messages);
      phase++;
  }
  ```

  `dialogue()` call sites that all funnel into the same unsynchronized timer (each sends the full sequence independently):
  - `dialogueOpener` (line 202), `dialogueItemCount` (line 210)
  - `checkVillagerTradingQuest` (lines 402, 407), `checkAdvancementsQuest` (line 470)
  - `wrongItemDialogue` (line 627), `emptyHandItemStackDialogue` (line 640)
  - `onBulkComplete` (line 658), `onHandInComplete` (line 671)
  - All phase handlers (`checkFarmingQuest`, `checkOreQuest`, `checkCreatureQuest`, `checkKnowledgeQuest`, `checkRelicQuest`, `checkMobItemQuest`) via the above.

## Notes

- `GodTrophyQuest` currently holds no field referencing the active dialogue runnable (fields are only `uuid`, `maxPhase`, `allAdvancements`, `currentItemCount`, `phase`); a grep for `dialogueTask`/`dialogueRunnable`/`dialogues` found nothing.
- The `BukkitRunnable` already self-cancels when its sequence finishes (line 184); the missing piece is cancelling a still-running dialogue task for the same player before scheduling a new one.
- Cleanup consideration: the active dialogue task should also be cancelled when the player disconnects (the plugin currently schedules on `SurvivalSkills.getInstance()` with no player-quit handling for the dialogue task).
- No existing test covers `GodTrophyQuest`; reproducing the overlap requires mocking Bukkit's scheduler (the codebase already uses `mockStatic` for scheduler-dependent tests).
