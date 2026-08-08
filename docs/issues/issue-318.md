## Title: Teach Power Ore forging through the Zap Wand

## Tags

Complexity Classification: T2
Severity: Medium
Reason: Cross-cutting change spanning ~8-11 files: Zap Wand item lore (`ItemStackGenerator`), the Zap Wand use handler (`MiningSkill.useZapWand`), the lightning conversion pipeline (`PowerOreChallengeListener`), reward text (`RewardData`), the unlock flow (`PlayerRewards.handleReward`), and new-item registration (`SurvivalSkillsGetCommand`, `TabCompleter`), plus a new written-book item (`BookMeta` is not yet used anywhere in the codebase) and a new `ItemModelData` id. It also carries an open runtime question (whether `world.strikeLightning` reliably damages the player to fire `EntityDamageEvent(LIGHTNING)`) and a timing-sensitive transient-marker handoff between `PlayerInteractEvent` and `EntityDamageEvent`.
Needs research before implementation: Yes
Research needed: (1) Confirm in-game that `world.strikeLightning(loc)` on the block under the player reliably damages them so `EntityDamageEvent` with `DamageCause.LIGHTNING` fires — the Zap Wand path and the new hints depend on it. (2) Confirm the guide-book delivery flow (inventory full → drop at feet) and a free `ItemModelData` id. (3) Decide the backfill approach for players who already unlocked Power Ore (per-player state in `PlayerRewards`/`playerdata.yml` vs. no backfill).

## Summary

Power Ore is the most opaque mechanic in the plugin: the intended trigger, the Zap Wand, is never connected to the resource it produces, and the unlock text is inaccurate. This issue makes the Zap Wand the taught path to Power Ore by (1) adding lore to the Zap Wand plus contextual in-use hints when it is used on obsidian, (2) rewriting the Power Ore unlock notification and `/skills` tree description to be accurate, and (3) granting a written guide book the first time a player unlocks Power Ore (mining 95).

## Steps to Reproduce Context

1. Unlock the Zap Wand at mining level 28 and craft it. The item lore is only `Right click the ground to throw lightning` (`ItemStackGenerator.getZapWand`) — nothing connects it to Power Ore.
2. Unlock Power Ore at mining level 95. The unlock notification and `/skills` tree description say to "strike lightning on the block", which is inaccurate and never mentions the Zap Wand.
3. Stand on obsidian in the Overworld with 50 XP levels and use the Zap Wand on it. The conversion only triggers when the player is struck by lightning while standing on obsidian — `validateConversionPrerequisites` checks the block below the player, not the zapped block — but nothing tells the player they must be struck while standing on the obsidian.

## Expected Behavior

- The Zap Wand's lore states its Power Ore purpose.
- When a player uses the Zap Wand on an obsidian block, they receive an immediate, specific hint about which requirement is missing (stand on the obsidian being zapped, mining 95 unlock, 50 XP levels, Overworld only, or already in a challenge). Hints fire only for the player's own Zap Wand action — natural lightning (storms, other players' wands) stays silent.
- The Power Ore unlock notification and `/skills` tree description accurately describe the forge steps, mention the Zap Wand by name, and point to its lore.
- On first Power Ore unlock, the player receives a written guide book describing the forge steps and the power-item chain, dropped at their feet if the inventory is full.

## Actual Behavior

- The Zap Wand lore only says `Right click the ground to throw lightning` (`ItemStackGenerator.java` lines 68-74).
- `MiningSkill.useZapWand` (lines 232-257) strikes lightning at the clicked block with no obsidian check and no feedback about Power Ore.
- `PowerOreChallengeListener.onPlayerDamageByLightning` (lines 155-164) triggers the conversion on ANY lightning damage to the player with no origin check; `tryPowerOreConversion`/`validateConversionPrerequisites` (lines 223-270) check the block below the player (not the zapped block), the Overworld environment, a 50-level requirement, and an existing challenge.
- `RewardData` (lines 141-146) Power Ore notification and description say to "strike lightning on the block" (the player actually must be struck by the bolt), do not mention the Zap Wand, and do not mention the Overworld or the block-under-the-player positioning.

## Requirements for completed issue

1. Zap Wand lore explains that it is used to forge Power Ore.
2. Using the Zap Wand on an obsidian block gives contextual feedback naming the specific missing step (stand on the obsidian being zapped, mining 95 unlock, 50 XP levels, Overworld only, already in a challenge); hints only fire for the player's own Zap Wand use, not for natural lightning or other players' wands.
3. The Power Ore unlock notification and `/skills` tree description are rewritten to be accurate, mention the Zap Wand by name, and point to its lore. The existing `RewardDataTest` assertions (notification line 0 mentions "Power Ore" and uses `LIGHT_PURPLE`) still pass.
4. A written guide book is granted the first time Power Ore is unlocked, covering the forge steps and the power-item chain (Power Sword, Power Drill, Power Laser, Power Armor, Teleport Anchor, God Trophy). If the inventory is full, the book drops at the player's feet.
5. The new book item is registered in `SurvivalSkillsGetCommand.ITEM_SUPPLIERS` and `TabCompleter.handleGetItem` per the repo convention, with a new `ItemModelData` id.

## Context

- Files:
  - `src/main/java/sir_draco/survivalskills/utils/items/ItemStackGenerator.java` — `getZapWand()`, lines 68-74
  - `src/main/java/sir_draco/survivalskills/skill_listeners/MiningSkill.java` — `useZapWand`, lines 232-257
  - `src/main/java/sir_draco/survivalskills/skill_listeners/god/PowerOreChallengeListener.java` — `onPlayerDamageByLightning` lines 155-164, `tryPowerOreConversion` lines 223-234, `validateConversionPrerequisites` lines 246-270
  - `src/main/java/sir_draco/survivalskills/rewards/RewardData.java` — PowerOre entry, lines 141-146
  - `src/main/java/sir_draco/survivalskills/rewards/RewardNotifications.java` — `notifyPlayer`, `getRewardDescription`
  - `src/main/java/sir_draco/survivalskills/rewards/PlayerRewards.java` — `handleReward` lines 139-151, per-player boolean fields (e.g. `addedDeathResistance`, line 28)
  - `src/main/java/sir_draco/survivalskills/commands/default_commands/SkillsCommand.java` — `buildRewardItem` lines 410-420 (renders the `/skills` tree description)
  - `src/main/java/sir_draco/survivalskills/commands/admin_commands/SurvivalSkillsGetCommand.java` — `ITEM_SUPPLIERS`, lines 49-150
  - `src/main/java/sir_draco/survivalskills/commands/TabCompleter.java` — `handleGetItem`, lines 71-177
  - `src/main/java/sir_draco/survivalskills/utils/items/ItemModelData.java` — ids 1-68 in use
  - `src/main/resources/config.yml` — `ZapWand` level 28, `PowerOre` level 95
  - `src/test/java/sir_draco/survivalskills/rewards/RewardDataTest.java` — PowerOre tests, lines 143-158
  - `src/main/java/sir_draco/survivalskills/utils/FileUtils.java` — `CURRENT_CONFIG_VERSION = 2.32`, line 52
  - `pom.xml` — `<version>2.32</version>`, line 9
- Code Snippets:

  Zap Wand use — `MiningSkill.java` lines 232-257:
  ```java
  @EventHandler
  public void useZapWand(PlayerInteractEvent e) {
      if (e.getHand() == null) return;
      if (e.getHand().equals(EquipmentSlot.OFF_HAND)) return;
      if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
      if (e.getClickedBlock() == null) return;
      Player p = e.getPlayer();
      ItemStack hand = p.getInventory().getItemInMainHand();
      if (!ItemStackGeneratorUtils.isCustomItem(hand, CUSTOM_ITEM_ZAP_WAND)) return;

      PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
      if (rewards == null) return;

      if (!rewards.getReward(SkillCategory.MINING, "ZapWand").isApplied()) {
          e.setCancelled(true);
          p.sendRawMessage(ChatColor.RED + "Zap Wand unlocks at mining level " + ChatColor.AQUA +
                  rewards.getReward(SkillCategory.MINING, "ZapWand").getLevel());
          p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
          return;
      }

      World world = e.getClickedBlock().getWorld();
      Location loc = e.getClickedBlock().getLocation();
      world.strikeLightning(loc);
      e.setCancelled(true);
  }
  ```

  Conversion trigger — `PowerOreChallengeListener.java` lines 155-164:
  ```java
  @EventHandler
  public void onPlayerDamageByLightning(EntityDamageEvent e) {
      if (!(e.getEntity() instanceof Player p))
          return;
      if (!EntityDamageEvent.DamageCause.LIGHTNING.equals(e.getCause()))
          return;
      if (conversionCooldowns.contains(p))
          return;
      tryPowerOreConversion(p);
  }
  ```

  Prerequisite validation — `PowerOreChallengeListener.java` lines 246-270:
  ```java
  private Location validateConversionPrerequisites(Player p) {
      Block block = p.getLocation().getBlock().getRelative(0, -1, 0);
      if (!Material.OBSIDIAN.equals(block.getType()))
          return null;

      Location loc = block.getLocation();
      if (loc.getWorld() == null || loc.getWorld().getEnvironment() != World.Environment.NORMAL)
          return null;

      if (p.getLevel() < POWER_ORE_LEVEL_REQUIREMENT) {
          p.sendRawMessage(ChatColor.RED + "You need 50 levels of experience to power the ore");
          p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
          return null;
      }

      if (powerOreRegistry.hasPlayer(p.getUniqueId())) {
          PowerOreChallengeHandle existing = powerOreRegistry.forPlayer(p.getUniqueId());
          p.sendMessage(ChatColor.RED + "You are already attempting a Power Ore challenge at "
                  + existing.getOreLocation().getBlockX() + ", " + existing.getOreLocation().getBlockY() + ", "
                  + existing.getOreLocation().getBlockZ());
          return null;
      }

      return loc;
  }
  ```

  Current reward text — `RewardData.java` lines 141-146:
  ```java
  staticEntry(m, "PowerOre", lines(
          NEW_ITEM + "You can now forge " + LIGHT_PURPLE + "Power Ore",
          GREEN + "\nPlace obsidian down, stand on it, and strike lightning on the block" +
                  " sacrificing 50 levels of experience"),
          GRAY + "You can now forge " + LIGHT_PURPLE + "Power Ore\n" + GRAY
                  + "Place obsidian down, stand on it, and strike lightning on the block and yourself sacrificing 50 levels of experience");
  ```

  Zap Wand item — `ItemStackGenerator.java` lines 68-74:
  ```java
  public static ItemStack getZapWand() {
      return new ItemStackBuilder(Material.LIGHTNING_ROD, 1,
              ChatColor.GOLD.toString() + ChatColor.BOLD + "Zap Wand")
              .lore(ChatColor.GRAY + "Right click the ground to throw lightning")
              .modelData(ItemModelData.ZAP_WAND.getId())
              .build();
  }
  ```

  Reward unlock hook (candidate for book grant) — `PlayerRewards.java` lines 139-151:
  ```java
  public void handleReward(Player p, Skill skill, boolean notify) {
      int level = skill.getLevel();
      Reward reward = getLevelReward(skill.getSkillCategory(), level);
      if (reward == null) return;
      if (!reward.isEnabled()) return;
      if (reward.isApplied()) return;

      if (notify) RewardNotifications.notifyPlayer(skill.getSkillCategory().getDisplayName(), reward.getName(), p);
      reward.applyReward();

      RewardEffect effect = RewardEffects.getEffect(skill.getSkillCategory().getDisplayName(), reward.getName());
      if (effect != null) effect.apply(this, p);
  }
  ```

## Notes

- Positioning: the conversion validates the block below the player (`validateConversionPrerequisites`, `PowerOreChallengeListener.java` line 247), not the block the Zap Wand struck. Hints must state explicitly that the player must be standing on the obsidian they zap, or the flow stays confusing.
- Technical verification: whether `world.strikeLightning(loc)` (`MiningSkill.java` line 255) reliably damages a player standing on the struck block enough to fire `EntityDamageEvent` with `DamageCause.LIGHTNING` needs in-game confirmation before building hints on top of it.
- Natural lightning and other players' wands currently also trigger `tryPowerOreConversion`, since `onPlayerDamageByLightning` has no origin check; the transient-marker approach proposed for `useZapWand` would address that alongside the hint logic.
- Backfill: players who already unlocked Power Ore won't see the new notification or receive the book automatically. Granting the book on next join (if unlocked and not yet given) requires per-player state (e.g. a `PlayerRewards` boolean persisted via `playerdata.yml`), similar to the existing `addedDeathResistance` flag.
- Versioning: no `config.yml` reward-level changes are proposed, so no version bump is needed in `pom.xml`/`FileUtils.java`; the book is a new item and must still be registered per the repo convention (`SurvivalSkillsGetCommand` + `TabCompleter`).
- No written-book item exists in the codebase today (`BookMeta` is unused; `WRITABLE_BOOK` appears only as a tool-belt-acceptable material in `MiningSkill.java` line 577), so this introduces the first `BookMeta` usage.
