# Requirements: Ender Dragon Fight Buffs

## Things To Implement

- Add an Ender Dragon attack that spawns explosive endermite minions. Each minion must detonate when an encounter participant comes within three blocks, create a low-power explosion that does not damage blocks, and be removed after detonating.
- Apply a one-second stun to encounter participants caught within the endermite's three-block detonation radius and show `You have been stunned!` in each affected player's action bar. The stun must prevent normal movement and jumping for its duration.
- Add a rare ground-slam attack with a 5% attack-selection chance. The dragon must visibly dive toward a participant's ground position and emit a large shockwave on impact; encounter participants in the shockwave radius must take exactly 30 damage and be knocked away from the impact.
- At or below 50% dragon health, trigger Dragon's Wrath once per encounter, announce that the dragon has stripped the players' powers, immediately disable flight for encounter participants, continuously prevent other systems from re-enabling it, and deny `/flight` without starting or consuming an ability timer while Wrath and the fight remain active.
- Preserve and restore each affected player's pre-Wrath allow-flight, flying, and flight-speed state when the dragon dies or the encounter task otherwise ends, provided the player remains online and in a game mode where that state is applicable.
- Roll an independent 5% regenerator spawn chance whenever a normal dragon attack is dispatched. Do not spawn a second regenerator while the current one remains alive.
- Spawn the regenerator as a custom-named enderman with 75 maximum/current health, 10 attack damage, 0.7 scale, persistent encounter ownership, and a participant target.
- While the regenerator remains alive, valid, and in the same world as the dragon, heal the dragon for 1% of its maximum health every five seconds without exceeding maximum health.
- Remove surviving encounter-owned endermites and the regenerator and terminate all associated behavior when the dragon encounter ends.
- Register online players already in the End as participants when the first dragon is attached so target-restricted mechanics work for both first and respawned dragons.

## Tests To Create Or Update

- For `explosive endermite minions`:
  - Verify the three-block boundary used for proximity detonation and stun eligibility.
  - Verify the stun duration and action-bar text through a focused helper or mocked player behavior.
- For `rare ground slam`:
  - Verify its configured chance boundary and exact 30-damage constant.
  - Verify shockwave eligibility excludes players outside the configured radius or world.
- For `Dragon's Wrath flight suppression`:
  - Verify Wrath activates once at 50% health, disables current flight, enforces disabled flags, and restores captured state at cleanup.
- For `regenerator enderman`:
  - Verify the 1%-of-maximum healing calculation, maximum-health cap, five-second interval constant, 75 health, 10 damage, and 0.7 scale.
  - Verify a living regenerator blocks duplicate spawns.
- For `first-dragon participant initialization`:
  - Verify players collected from the End are added without marking the encounter as a respawn.

## Important Background Information

- The existing `DragonBoss` is a synchronous one-tick `BukkitRunnable`, which is the safe owner for entity mutations and bounded encounter state.
- Attack targeting is restricted to the boss's `players` list; the first-dragon attach path currently leaves that list empty.
- Flight can be enabled through command, login restoration, and respawn restoration, so a command-only prohibition would not satisfy continuous suppression.
- Dragon health scales with player count. Regeneration therefore uses the live maximum-health attribute rather than the fixed base health.
- Research details are in `docs/research/ender-dragon-fight-buffs.md`.

## Things To Ensure Are Not Done

- Do not allow endermite explosions to break blocks or deal high explosion damage.
- Do not globally affect unrelated endermites, endermen, dragons, or players outside the active encounter.
- Do not permanently remove earned flight or consume/reset active flight timers because Dragon's Wrath temporarily suppresses Bukkit flight flags.
- Do not stack multiple regenerator endermen or leave helper mobs/tasks alive after encounter cleanup.
- Do not replace existing cannon, death-rain, angry-enderman, lightning, crystal reset, XP, or dragon death behavior.
- Do not modify `config.yml` or increment the plugin/config version because no new skill reward or configuration entry is being added.

## User Decisions Made During Requirement Creation

| Decision Needed | Answer | Reason |
| --------------- | ------ | ------ |
| How should the approximate 5% regenerator chance interact with attacks? | Roll independently once per dispatched normal attack. | This matches "5% chance per attack" and preserves the selected attack. |
| How rare should the unspecified ground slam be? | 5% of attack selections. | It keeps a 30-damage move meaningfully rare and provides a concrete testable rate. |
| May multiple regenerators stack? | No; only one living regenerator at a time. | The request describes a singular healer and stacking 1% heals could become disproportionate. |
| What does a one-second stun mean? | Prevent walking and jumping for 20 ticks while showing an action-bar notice. | This makes the requested status mechanically observable without adding persistent state. |
| What happens to flight after the fight? | Restore captured pre-Wrath Bukkit flight state where applicable. | Dragon's Wrath is explicitly scoped to the fight and should not erase earned powers. |
