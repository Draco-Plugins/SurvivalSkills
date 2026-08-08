## Title: Co-op difficulty gate checks the wrong value against the true-difficulty list and misreports the required solo clear

## Tags

Complexity Classification: T1
Severity: Medium
Reason: Bug is fully diagnosed and verified in `TrialUtils.java`. The co-op gate at line 581 and the party-join gate at line 721 both compute `(trueDifficulty + 1) / 2` (a display difficulty, 1-5) and compare it against the completed-gamemodes list, which stores true difficulties (confirmed via `Trial.completeTrial()` line 674 storing `trueDifficulty`, and the solo gate at line 595 correctly using `trueDifficulty - 2`). The error message at lines 584-585 also misreports names via `WaveGenerator.getDifficultyName()` on the mismatched value. Fix surface is small (one file, two formulas, message text), but it touches a stateful progression gate with a semantic design question.
Needs research before implementation: Yes - confirm the intended gate semantics: should co-op difficulty N require beating the solo version of the same named difficulty only (`trueDifficulty - 1`, as this issue asserts), or should a co-op completion of the previous named difficulty also count? Also confirm the beaten-list display (line 588) should map true difficulties to correct display names.

## Summary

The co-op difficulty gate in `TrialUtils.partyDifficulty()` (and the member gate in `joinParty()`) computes a display difficulty (1-5) via `(trueDifficulty + 1) / 2` and compares it against `gamemodesBeaten`, which stores true difficulties (solo clears are odd: 1, 3, 5, 7, 9). Because the two namespaces collide, co-op difficulties require the wrong solo clear (e.g., co-op Hard requires solo Medium), and the error message names the wrong difficulty.

## Steps to Reproduce Context

1. Player A completes only solo Easy (true difficulty 1 recorded). Player B completes only solo Medium (true difficulty 3 recorded).
2. Both run `/godtrial`, choose Co-op, then "New", and select Hard.
3. Player A is blocked, but the message says "You have not beaten solo mode on this difficulty: Hard" - even though the gate actually checked for value 3 (solo Medium).
4. Player B is allowed to start co-op Hard even though they have never beaten solo Hard (true difficulty 5), which is the intended prerequisite.

## Expected Behavior

A co-op difficulty should require having beaten the solo version of the same named difficulty. E.g., co-op Hard (true difficulty 6) should require solo Hard (true difficulty 5). Error messages and the beaten-list display should name the actual required solo difficulty.

## Actual Behavior

- `partyDifficulty()` line 581 computes `difficultyToBeat = (trueDifficulty + 1) / 2`, which for co-op true difficulties (even: 2, 4, 6, 8, 10) yields the display difficulty (1-5), not a true difficulty.
- Line 583 compares this against `gamemodesBeaten`, which stores true difficulties, so the check resolves to the wrong solo clear:
  - Co-op Easy (2) -> 1 (solo Easy) - correct only by coincidence.
  - Co-op Medium (4) -> 2 (a co-op Easy clear, not a solo value).
  - Co-op Hard (6) -> 3 (solo Medium), not solo Hard (5).
  - Co-op God (8) -> 4 (a co-op Medium clear, not a solo value).
  - Co-op Death (10) -> 5 (solo Hard), not solo Death (9).
- The error message (lines 584-585) uses `WaveGenerator.getDifficultyName(difficultyToBeat)`, so for co-op Hard it prints "Hard" while the gate actually requires solo Medium - the message lies.
- The same broken formula is duplicated in `joinParty()` line 721 for members joining an existing co-op party.

## Requirements for completed issue

1. Co-op difficulty gating (leader selecting a difficulty in `partyDifficulty()` and members joining via `joinParty()`) operates in the same true-difficulty namespace as the completed-gamemodes list, such that a co-op difficulty requires the solo clear of the same named difficulty (e.g., co-op Hard requires solo Hard).
2. Error messages and the "You have beaten" list display correct solo difficulty names/values consistent with what the gate actually requires.
3. Existing solo progression behavior (the `trueDifficulty - 2` chain in `partyDifficulty()` and the unlock logic in `openDifficultySelection()`) remains unchanged and stays consistent with the corrected co-op gate.

## Context

- Files:
  - `src/main/java/sir_draco/survivalskills/utils/TrialUtils.java` - co-op gate (`partyDifficulty` ~lines 569-609), member gate (`joinParty` ~lines 702-728), unlock GUI (`openDifficultySelection` ~lines 509-537), `getDifficultyFromName` lines 678-685
  - `src/main/java/sir_draco/survivalskills/god_questline/trial/Trial.java` - `difficulty = (trueDifficulty + 1) / 2` lines 95/107; `addCompletedGamemode(p, trueDifficulty)` line 674
  - `src/main/java/sir_draco/survivalskills/god_questline/trial/TrialRegistry.java` - completed-gamemodes storage lines 105-118
  - `src/main/java/sir_draco/survivalskills/god_questline/trial/TrialDataPersistence.java` - `loadCompletedTrials` lines 164-195 (persists the true-difficulty ints)
  - `src/main/java/sir_draco/survivalskills/god_questline/trial/WaveGenerator.java` - difficulty constants lines 17-21, `getDifficultyName` lines 367-376
  - `src/main/java/sir_draco/survivalskills/god_questline/GodTrophyQuest.java` - "Trial of the Gods" capstone message lines 475-484

- Code Snippets:

  `TrialUtils.partyDifficulty` - co-op gate and error message:
  ```java
  int trueDifficulty = difficulty * 2;
  if (trial.isSolo()) trueDifficulty -= 1;

  if (!trial.isSolo()) {
      if (!TrialManager.hasCompletedGamemodes(p)) {
          sendError(p, "You have not beaten any solo mode trials");
          return;
      }
      int difficultyToBeat = (trueDifficulty + 1) / 2;
      ArrayList<Integer> gamemodesBeaten = TrialManager.getCompletedGamemodes(p);
      if (!gamemodesBeaten.contains(difficultyToBeat)) {
          p.sendMessage(ChatColor.RED + String.format("You have not beaten solo mode on this difficulty: %s",
                  WaveGenerator.getDifficultyName(difficultyToBeat)));
          p.sendMessage(ChatColor.YELLOW + "You have beaten:");
          for (int beaten : gamemodesBeaten)
              p.sendMessage(ChatColor.GRAY + "- " + WaveGenerator.getDifficultyName(beaten));
          p.playSound(p, ERROR_SOUND, 1, 1);
          return;
      }
  }
  ```

  `TrialUtils.partyDifficulty` - solo gate (correctly uses true difficulties):
  ```java
  if (trial.isSolo() && trueDifficulty != 1 && TrialManager.hasCompletedGamemodes(p)
          && !TrialManager.getCompletedGamemodes(p).contains(trueDifficulty - 2)) {
      sendError(p, "You have not beaten the previous difficulty");
      return;
  }
  ```

  `TrialUtils.joinParty` - duplicate broken formula:
  ```java
  ArrayList<Integer> beaten = TrialManager.getCompletedGamemodes(p);
  if (beaten == null || !beaten.contains((trial.getTrialDifficulty() + 1) / 2)) {
      sendError(p, "You have not beaten solo mode on this difficulty and can't join this party");
      return;
  }
  ```

  `Trial.completeTrial` - what is stored in the completed list:
  ```java
  for (Player p : players) {
      TrialManager.addCompletedGamemode(p, trueDifficulty);
  ```

  `WaveGenerator` - constants and name mapping:
  ```java
  public static final int EASY = 1;
  public static final int MEDIUM = 2;
  public static final int HARD = 3;
  public static final int GOD = 4;
  public static final int DEATH = 5;
  ...
  public static String getDifficultyName(int difficulty) {
      return switch (difficulty) {
          case EASY -> "Easy";
          case MEDIUM -> "Medium";
          case HARD -> "Hard";
          case GOD -> "God";
          case DEATH -> "Death";
          default -> "Unknown";
      };
  }
  ```

## Notes

- The trial system is the "Trial of the Gods" capstone of the god questline (`GodTrophyQuest.java`); `TrialUtils.createTrial()` line 401 additionally gates true difficulty 7 (solo God) behind `completedGodQuest`. The mismatch described here is in the co-op difficulty gate specifically.
- The unlock GUI `openDifficultySelection()` (lines 529-534) unlocks via solo true difficulties (1, 3, 5, 7), which is consistent with the solo ladder but the same GUI is used for both solo and co-op selection; whether that is correct for co-op should be confirmed during implementation research.
- No existing tests cover these gates (`partyDifficulty`/`joinParty`), so verification will likely require Bukkit-heavy mocking, extracting the formula into a pure function, or live-server testing.
