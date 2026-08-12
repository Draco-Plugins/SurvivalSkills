# Task: Add encounter mechanics and cleanup

## Classification

Type: T2: moderate stateful gameplay change
Reasoning: Four production classes plus focused tests, with most state concentrated in one encounter owner. Blast Radius=2, Uncertainty=2, Behavior=3, Testing=2, Reversibility=1. Total=10.

## Goal

Implement all four requested combat gimmicks in the synchronous dragon task with bounded state, participant-only effects, and reliable cleanup.

## Files to Modify

| File | Action (create/update/delete) |
| ---- | ----------------------------- |
| `src/main/java/sir_draco/survivalskills/bosses/DragonBoss.java` | update |
| `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/DragonManager.java` | update |
| `src/main/java/sir_draco/survivalskills/skill_listeners/FightingSkill.java` | update |
| `src/main/java/sir_draco/survivalskills/commands/skill_commands/FlightCommand.java` | update |

## Step-by-Step Instructions

### 1. Add immutable encounter state and constants

**File:** `src/main/java/sir_draco/survivalskills/bosses/DragonBoss.java`

Add package-private named constants for attack probabilities, radii, damage, durations, minion attributes, and heal interval so exact balance values are unit-testable. Add collections for live explosive endermites, one optional regenerator reference, slam target/timer state, a Wrath boolean, and captured flight states. Represent captured flight state with a nested package-private record that also captures the player's game mode.

```java
record FlightState(GameMode gameMode, boolean allowFlight, boolean flying, float flySpeed) {}
```

### 2. Extend the synchronous tick loop

**File:** `src/main/java/sir_draco/survivalskills/bosses/DragonBoss.java`

Before normal attack dispatch, update explosive minions, an active ground slam, the regenerator heal counter, and Dragon's Wrath. When the boss becomes invalid/dead, run encounter cleanup before cancelling. Ensure an active slam blocks selection of another normal attack until impact or timeout.

### 3. Add explosive endermites

**File:** `src/main/java/sir_draco/survivalskills/bosses/DragonBoss.java`

Spawn a small wave near valid ground locations, assign participant targets, and track only those spawned by this encounter. Each tick, detonate a tracked minion when a valid participant is within three blocks. Use a low-power, fire-free, block-safe explosion; capture affected players' walk speed, hold their walk speed and velocity at zero for 20 ticks, restore the captured speed afterward, and send `You have been stunned!` through `Utils.sendActionBarMessage`. This avoids deleting unrelated potion effects.

### 4. Add ground slam

**File:** `src/main/java/sir_draco/survivalskills/bosses/DragonBoss.java`

Reserve 5% of normal attack selections for a slam. Capture a nearby participant's ground position, set an appropriate dragon phase, and drive velocity toward that point each tick. On proximity/ground impact (or a bounded timeout), play a large particle/sound shockwave, damage eligible participants in the same world and radius with `player.damage(30, dragon)`, apply outward/upward knockback, and clear slam state.

### 5. Add Dragon's Wrath

**File:** `src/main/java/sir_draco/survivalskills/bosses/DragonBoss.java`

At `getHealthPercentage() <= 0.5`, announce Wrath once, capture each participant's current game mode and flight flags/speed, and immediately disable `flying` and `allowFlight`. Reapply disabled flags every tick while Wrath is active so commands, login restoration, respawn restoration, or other abilities cannot override it; if a new participant appears after activation, capture that player's state before the first suppression. On cleanup, restore captured state only for online players still in the captured game mode and clear the map. Expose package-private pure/static capture, suppress, and restore helpers for Mockito tests.

### 6. Add the regenerator

**File:** `src/main/java/sir_draco/survivalskills/bosses/DragonBoss.java`

After every dispatched normal attack, independently roll a 5% spawn chance. If no valid tracked regenerator exists, spawn a persistent custom-named enderman with max/current health 75, attack damage 10, scale 0.7, and a participant target. Every 100 ticks while both entities remain valid and co-located, heal the dragon by `maxHealth * 0.01`, capped at maximum health. Clear a dead/invalid regenerator reference and reset its counter.

### 7. Clean up owned state

**File:** `src/main/java/sir_draco/survivalskills/bosses/DragonBoss.java`

Create one idempotent cleanup method invoked by death animation and abnormal task termination. Remove live encounter-owned minions, remove the regenerator, restore flight, and clear slam/heal state before cancelling.

### 8. Add deterministic test seams

**File:** `src/main/java/sir_draco/survivalskills/bosses/DragonBoss.java`

Add package-private static helpers for squared-radius eligibility, ground-slam roll classification, capped one-percent regeneration, and participant-list merging. Production paths must call these helpers so tests prove the same calculations used in game.

### 9. Deny the flight command during Wrath

**Files:** `src/main/java/sir_draco/survivalskills/bosses/DragonBoss.java`, `src/main/java/sir_draco/survivalskills/skill_listeners/fighting/DragonManager.java`, `src/main/java/sir_draco/survivalskills/skill_listeners/FightingSkill.java`, `src/main/java/sir_draco/survivalskills/commands/skill_commands/FlightCommand.java`

Expose a read-only participant/Wrath query through the existing combat ownership chain. Check it before all reward and timer work in `/flight`; send a denial message and sound and return handled so the command cannot create, deactivate, or consume a flight timer during suppression. Retain per-tick enforcement for non-command sources.

## Edge Cases to Handle

- No participant is within target range when an attack is selected.
- A minion or regenerator dies independently or changes validity.
- The dragon reaches maximum health before a healing tick.
- A participant logs out, changes world, dies, or enters creative/spectator during Wrath.
- The slam target becomes invalid or the dragon never reaches the impact point.
- Attack chance boundaries must not accidentally make the ground slam common.

## Related Files (read-only context)

- `src/main/java/sir_draco/survivalskills/bosses/Boss.java` - Shared health percentage, stage, and cleanup contracts.
- `src/main/java/sir_draco/survivalskills/utils/Utils.java` - Action bar and attribute helpers.
- `src/main/java/sir_draco/survivalskills/commands/skill_commands/FlightCommand.java` - Flight flags Wrath temporarily suppresses.
