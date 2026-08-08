## Title: createTrial fails the God difficulty check without cleaning up the pending trial

## Tags

Complexity Classification: T1
Severity: Medium
Reason: Single-file bug-fix logic change in `TrialUtils.createTrial` (line 401). The fix mirrors the existing cleanup in the empty-players error path (lines 408-413): remove the pending trial and close the trial master's inventory before the early return. Minor design decisions remain (whether to also drop the `ProtectedArea` registered in `initializeTrial` and whether the co-op God difficulty-8 bypass of the check should be covered), but the change surface is small and well understood.
Needs research before implementation: No

## Summary

When a solo player selects the God difficulty without having completed the God Trophy questline, `createTrial` returns early at its `completedGodQuest` guard without removing the pending trial or closing the trial master's inventory. The player is left with a registered pending trial and, in the fresh-building flow, a registered `ProtectedArea` (plus building-creation cooldown) even though no trial building was ever constructed — a half-pending state that interferes with subsequent trial attempts.

## Steps to Reproduce Context

1. Have a player who has beaten the Death solo trial (unlocking the God option in the difficulty-selection GUI) but who has NOT completed the God Trophy questline.
2. Run `/godtrial`, choose Solo, then click the "God" difficulty item.
3. `partyDifficulty` sets the trial difficulty to 7, closes the selection inventory, and calls `initializeTrial`, which registers a `ProtectedArea` and building-creation cooldown before calling `createTrial`.
4. `createTrial` hits the `difficulty == 7 && !completedGodQuest(...)` guard. `completedGodQuest` sends the "You do not have an active god quest" / "You have not unlocked the god trial" error, then `createTrial` returns without cleanup.
5. Observe the half-pending state: the pending trial is still registered, and the player now has a `ProtectedArea` with no actual building constructed.

## Expected Behavior

When the God difficulty is rejected because the player has not completed the God Trophy questline, the trial attempt should be fully rolled back: the pending trial should be removed and the trial master's inventory closed (mirroring the empty-players cleanup at lines 408-413), and no stale `ProtectedArea`/cooldown state should remain that makes the next attempt treat the player as having an existing trial building.

## Actual Behavior

`createTrial` (line 401) returns immediately when `pendingTrial.getTrialDifficulty() == 7` and `completedGodQuest(...)` is false. `completedGodQuest` does notify the player with an error message and error sound, but the early return performs none of the cleanup the sibling error path performs: the pending trial stays in `TrialRegistry.pendingTrials`, and in the fresh-building flow the `ProtectedArea` registered in `initializeTrial` (line 309) and the building-creation cooldown (line 300) are left behind even though the building was never built. A later `/godtrial` attempt then takes the `previousStructure` path for a structure that doesn't exist.

## Requirements for completed issue

1. When the God difficulty check in `createTrial` fails, the pending trial is removed and the trial master's inventory is closed, matching the cleanup in the empty-players error path.
2. A failed God check does not leave stale trial state behind — no leftover `ProtectedArea`/building-creation cooldown that causes a later attempt to treat the player as having an existing trial building.
3. Players who legitimately completed the God Trophy questline can still start God trials (the check itself continues to function).

## Context

- Files:
  - `src/main/java/sir_draco/survivalskills/utils/TrialUtils.java`
  - `src/main/java/sir_draco/survivalskills/god_questline/trial/TrialRegistry.java`
  - `src/main/java/sir_draco/survivalskills/god_questline/trial/PendingTrial.java`
  - `src/main/java/sir_draco/survivalskills/god_questline/trial/TrialManager.java`
- Code Snippets:

  The early-returning God check — `createTrial`, `TrialUtils.java` line 401:
  ```java
  if (pendingTrial.getTrialDifficulty() == 7 && !completedGodQuest(pendingTrial.getTrialMaster())) return;
  ```

  The sibling error path that DOES clean up — lines 408-413:
  ```java
  if (pendingTrial.getPlayers().isEmpty()) {
      sendError(pendingTrial.getTrialMaster(), "You must have at least one player in your trial");
      TrialManager.removePendingTrial(pendingTrial.getTrialMaster());
      pendingTrial.getTrialMaster().closeInventory();
      return;
  }
  ```

  `completedGodQuest` sends the error but does not clean up — lines 429-449:
  ```java
  public static boolean completedGodQuest(Player p) {
      GodTrophyQuest quest = SurvivalSkills.getInstance().getTrophyManager().getPlayerGodQuestData().get(p.getUniqueId());
      if (quest == null && !p.hasPermission("survivalskills.op")) {
          sendError(p, "You do not have an active god quest", "Complete the god questline to unlock the god trial");
          return false;
      }
      // ...
      if (!p.hasPermission("survivalskills.op") && quest.getPhase() != quest.getMaxPhase()) {
          sendError(p, "You have not unlocked the god trial", "Complete the god questline to unlock the god trial");
          return false;
      }
      return true;
  }
  ```

  State registered before `createTrial` in the fresh-building flow — `initializeTrial`, lines 300-309:
  ```java
  TrialManager.setBuildingCreationCooldown(p.getUniqueId(), System.currentTimeMillis());
  // ...
  ProtectedArea protectedArea = createProtectedArea(pLocation);
  TrialManager.putProtectedArea(p.getUniqueId(), protectedArea);
  ```

## Notes

- The God difficulty maps to difficulty 4 in the selection GUI; `partyDifficulty` computes `trueDifficulty = difficulty * 2` (minus 1 for solo), so solo God is difficulty 7 and co-op God is difficulty 8. The check at line 401 only matches 7, so a co-op God trial (difficulty 8) bypasses the god-quest check entirely — the fix should consider covering difficulty 8 as well.
- `TrialRegistry.removePendingTrial` (line 93) calls `PendingTrial.dispose()`, which unregisters the trial-selection inventory and clears the player lists, so the cleanup mechanism already exists.
