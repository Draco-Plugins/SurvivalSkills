# Requirements: Ender Dragon Encounter Scaling

## Things To Implement

- Treat the undefeated initial Ender Dragon as a server-wide encounter: every player-originated direct or projectile attack may damage it, regardless of the encounter participant snapshot.
- Calculate the initial dragon's maximum health as 250 health per online server player at attachment time, with a minimum of 250 health.
- Initialize the initial encounter with all players online at attachment time so custom targeting and encounter effects recognize the server-wide group; continue recognizing later End entrants without changing the initial health snapshot.
- For each respawned dragon, take one participant snapshot containing only online players in the exact End world containing the dragon and no more than 200 blocks from its spawn location, including players exactly 200 blocks away.
- Calculate a respawned dragon's maximum health as 250 health per player in that nearby participant snapshot, with a minimum of 250 health.
- Permit player-originated direct or projectile damage to a respawned dragon only when the attacking player belongs to that respawn's participant snapshot.

## Tests To Create Or Update

- For `Treat the undefeated initial Ender Dragon as a server-wide encounter`:
  - Verify an arbitrary player is authorized to damage an initial dragon even when absent from the participant list.
  - Verify a respawn continues to reject a player absent from its participant list and accepts a listed participant.
- For `Calculate the initial dragon's maximum health as 250 health per online server player`:
  - Verify zero counted players produces 250 health and multiple players multiply the 250-health unit exactly.
- For `Initialize the initial encounter with all players online at attachment time`:
  - Verify the initial attachment path passes the all-online-player snapshot to both health calculation and participant initialization through focused helper coverage and code review; full Bukkit scheduler integration is not available in the unit-test harness.
- For `Take one participant snapshot within the exact End world and 200 blocks`:
  - Verify players inside and exactly on the radius are included.
  - Verify players beyond the radius, in a different End world, and outside the End are excluded.
- For `Calculate a respawned dragon's maximum health from the nearby snapshot`:
  - Verify the selected participant count is the input to the shared health calculation and an empty snapshot retains the 250-health minimum.
- For `Permit respawn damage only for snapshot participants`:
  - Verify member and non-member authorization independently from distance after the snapshot is created.

## Important Background Information

- Research is recorded in `docs/research/ender-dragon-encounter-scaling.md`.
- `DragonManager` owns spawn attachment, participant selection, health calculation, and damage authorization; `DragonBoss` stores the resulting participant snapshot.
- `DragonBoss` also uses participants for custom target selection, area attacks, and Dragon's Wrath, so respawn health and participants must come from the same selection.
- The existing damage route already supports direct player and player-projectile attacks through `BossManager.getAttackingPlayer`.

## Things To Ensure Are Not Done

- Do not dynamically increase or decrease a dragon's maximum health after its encounter snapshot is created.
- Do not add players who arrive after a respawn has spawned to that respawn's participant snapshot.
- Do not include players merely because they are in another world with the End environment.
- Do not change dragon attack balance, explosion/lightning immunity, death animation, rewards, XP, first-kill metadata, or non-dragon boss ownership rules.
- Do not add configuration or version changes; this request changes runtime encounter rules only.

## User Decisions Made During Requirement Creation

| Decision Needed | Answer | Reason |
| --------------- | ------ | ------ |
| Who can damage the initial dragon? | Any player who can attack it | The user defined the initial spawn as a server-wide fight. |
| Which players count for initial health? | All online server players at initial attachment time | The user explicitly requested server-wide player scaling. |
| Which players count for a respawn? | Online players in the dragon's exact End world within an inclusive 200-block radius at spawn time | The user specified both End presence and 200-block proximity and described respawns as fights for a single player or nearby handful. |
| Should encounter health change as players move or join? | No; use a spawn/attachment snapshot | This preserves the existing encounter model and prevents exploitable mid-fight health changes. |
| What happens when no respawn participant qualifies? | Use the 250-health minimum and keep an empty authorized group | This preserves valid dragon health without granting the fight to unrelated players. |
