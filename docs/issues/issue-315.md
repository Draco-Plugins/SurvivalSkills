## Title: God Quest villager trade count is not reset when the Villager phase completes

## Tags

Complexity Classification: T1
Severity: Low
Reason: Single-file bug-fix logic change. `checkVillagerTradingQuest` (GodTrophyQuest.java line 413) is the only step-completion path that advances `phase` without resetting the shared `currentItemCount` counter — `onBulkComplete` (line 657) and `onHandInComplete` (line 670) both reset it to 0. The fix is a one-line change confined to this method. An optional related improvement is gating `TrophyListener.villagerTradeEvent` on the Villager phase, but the core bug is well understood and self-contained.
Needs research before implementation: No

## Summary

When a player completes the God Quest Villager phase (phase 47, 1,000 trades), `checkVillagerTradingQuest` advances to the Combat phase but does not reset `currentItemCount` back to 0. The leftover count self-heals because the next phase (Combat) is a hand-in step that resets the counter on completion, but until then the stale value persists in memory and is saved to `godquests.yml`.

## Steps to Reproduce Context

1. Progress the God Quest to the Villager phase (phase 47) and reach 1,000 villager trades.
2. Interact with the quest NPC to complete the phase; the completion dialogue plays, `phase` advances to 48 (Combat), and trophy particles update.
3. Trade with villagers again during the Combat phase — `TrophyListener.villagerTradeEvent` keeps incrementing the counter because it does not check the phase.
4. Before handing in the Combat item, observe the stale count; the next hand-in step eventually resets it to 0.

## Expected Behavior

Completing the Villager phase should reset the trade count to 0 at the moment the phase advances, consistent with every other phase-completion path (`onBulkComplete` and `onHandInComplete`). Villager trades made after the phase completes should not inflate the shared `currentItemCount`.

## Actual Behavior

`checkVillagerTradingQuest` sends the completion dialogue, increments `phase`, and updates particles but leaves `currentItemCount` at 1000+. `TrophyListener.villagerTradeEvent` (which only checks that the player has an active God Quest, not the phase) continues to add to the counter, so the inflated value is carried into the Combat phase and persisted to `godquests.yml` until the Combat hand-in runs `onHandInComplete`, which resets it.

## Requirements for completed issue

1. Completing the God Quest Villager phase resets the trade count to 0 when the phase advances.
2. Villager trades made after the Villager phase completes no longer inflate the shared `currentItemCount` (e.g., the trade listener gates on the Villager phase).
3. The rest of the God Quest flow is unaffected — bulk-collection phases (Farming/Ore) and hand-in steps keep their existing reset behavior.

## Context

- Files:
  - `src/main/java/sir_draco/survivalskills/god_questline/GodTrophyQuest.java`
  - `src/main/java/sir_draco/survivalskills/trophy/TrophyListener.java`
  - `src/main/java/sir_draco/survivalskills/trophy/TrophyManager.java`
- Code Snippets:

  Villager phase completion — `checkVillagerTradingQuest`, GodTrophyQuest.java lines 400-416. Advances the phase and updates particles but never resets `currentItemCount`:
  ```java
  public void checkVillagerTradingQuest(Player p) {
      if (currentItemCount < 1000) {
          dialogue(p, List.of(
                  "You have traded with villagers " + ChatColor.AQUA + currentItemCount + ChatColor.WHITE + " times",
                  "You need to trade with villagers " + ChatColor.AQUA + (1000 - currentItemCount)
                          + ChatColor.WHITE + " more times"));
      } else {
          dialogue(p, List.of(
                  "The villagers clearly trust in you skills as a merchant",
                  "We are nearing the end of my tribulations",
                  "Soon you will need to prove yourself in a combat trial",
                  "Bring me some powerful gear to show me you know what it means to fight",
                  "You can see the recipe by using " + ChatColor.YELLOW + "/godquest"));
          phase++;
          updateGodTrophyParticles();
      }
  }
  ```

  The two completion helpers that DO reset the shared counter — `onBulkComplete` line 656 and `onHandInComplete` line 668:
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

  The trade counter increment, which is not gated on the Villager phase — `villagerTradeEvent`, TrophyListener.java lines 190-213:
  ```java
  @EventHandler
  public void villagerTradeEvent(InventoryClickEvent e) {
      // Check if a player has an active God Quest
      if (!(e.getWhoClicked() instanceof Player p)) return;
      if (!plugin.getTrophyManager().getPlayerGodQuestData().containsKey(p.getUniqueId())) return;

      // Get the god quest
      GodTrophyQuest quest = plugin.getTrophyManager().getPlayerGodQuestData().get(p.getUniqueId());
      // ...
      quest.setCurrentItemCount(quest.getCurrentItemCount() + trades);
  }
  ```

  The counter is persisted and reloaded — `savePlayerGodQuestData`, TrophyManager.java lines 163-167 and `loadProgress`, GodTrophyQuest.java line 112:
  ```java
  data.set(uuid + ".ItemCount", quest.getCurrentItemCount());
  // ...
  currentItemCount = data.getInt(uuid + ".ItemCount");
  ```

## Notes

- The Villager phase maps to `PhaseGroup.VILLAGER(47, 47, 0)` (line 47 of GodTrophyQuest.java); phase 48 is `PhaseGroup.COMBAT`, a hand-in step, which is why the bug self-heals.
- The Combat phase is followed by hand-in-style Mob Item phases, so no bulk-collection phase runs after Villager; however, the shared `currentItemCount` field is also used by the Farming and Ore bulk-collection phases (lines 145-153), so any future phase ordering that places a bulk phase after Villager would break without the reset.
- The villager trade listener increments the counter even while a player is on Farming/Ore bulk steps, which is a related fragility of the shared counter, but is out of the minimal scope of this issue.
