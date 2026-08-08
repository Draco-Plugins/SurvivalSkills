## Title: Unlimited Powder Snow Bucket is consumed when used

## Tags

Complexity Classification: T2
Severity: Medium
Reason: Bug isolated to `FishingAbilityManager.onBucketUse`/`preservePowderSnowBucket` plus its test (Blast Radius=1, 1-2 files). The root cause is server-internal: Paper does not honor `PlayerBucketEmptyEvent.setItemStack` for the powder-snow (`SolidBucketItem`) placement path, so the fix has moderate uncertainty and cannot be validated by the existing mock-based test (Testing=2, Uncertainty=2). The fix touches event cancellation, block placement, and item preservation logic (Behavior=3) and is trivially reversible (Reversibility=1). Total=9.
Needs research before implementation: Yes

## Summary

The custom "Unlimited Powder Snow Bucket" (fishing loot item) is not actually unlimited: when a player right-clicks to place powder snow, the bucket is consumed and becomes a regular empty bucket, instead of remaining in hand for unlimited uses.

## Steps to Reproduce Context

1. Obtain an "Unlimited Powder Snow Bucket" (e.g., via fishing loot, `fishing_loot.yml` `UNLIMITED_POWDER_SNOW_BUCKET`, or `/skillsget unlimitedpowdersnowbucket`).
2. Right-click a block to place powder snow.
3. Observe the item held in the player's hand afterwards.

## Expected Behavior

- The powder snow is placed and the "Unlimited Powder Snow Bucket" remains in the player's hand, matching the behavior of the unlimited water and lava buckets (infinite uses).

## Actual Behavior

- The powder snow is placed, but the unlimited bucket is consumed and replaced by a regular empty bucket (`Material.BUCKET`).

## Requirements for completed issue

1. Using the unlimited powder snow bucket places powder snow without consuming the custom item; the unlimited powder snow bucket remains in the player's hand after use.
2. Behavior is consistent with the unlimited water/lava buckets (the custom bucket item is never lost).

## Context

- Files:
    - `src/main/java/sir_draco/survivalskills/skill_listeners/fishing/FishingAbilityManager.java` — `onBucketUse` (lines 92-111) and `preservePowderSnowBucket` (lines 113-118) contain the entire unlimited-bucket handling.
    - `src/main/java/sir_draco/survivalskills/skill_listeners/FishingSkill.java` — routes `PlayerBucketEmptyEvent` to `abilityManager.onBucketUse` (lines 101-104); registered as a Bukkit listener in `SurvivalSkills.java`.
    - `src/main/java/sir_draco/survivalskills/utils/items/ItemStackGenerator.java` — `getUnlimitedPowderSnowBucket` (lines 581-587) creates the item (`Material.POWDER_SNOW_BUCKET`, custom model data 52).
    - `src/main/java/sir_draco/survivalskills/utils/items/ItemModelData.java` — `UNLIMITED_POWDER_SNOW_BUCKET(52)` (line 61).
    - `src/test/java/sir_draco/survivalskills/skill_listeners/fishing/FishingAbilityManagerTest.java` — mock-based test that cannot detect this server-internal behavior.
- Code Snippets:

    The unlimited powder snow bucket branch relies on `setItemStack` without cancelling the event — `FishingAbilityManager.java` lines 92-118:

    ```java
    public void onBucketUse(PlayerBucketEmptyEvent e) {
        ItemStack hand = ItemStackGeneratorUtils.getItemInHand(e.getPlayer(), e.getHand());
        if (ItemStackGeneratorUtils.isCustomItem(hand,
                ItemModelData.UNLIMITED_POWDER_SNOW_BUCKET.getId())) {
            preservePowderSnowBucket(e, hand);
            return;
        }
        if (!ItemStackGeneratorUtils.isCustomItem(hand))
            return;

        if (hand.getType().equals(Material.WATER_BUCKET)) {
            e.setCancelled(true);
            Block relative = e.getBlockClicked().getRelative(e.getBlockFace());
            relative.setType(Material.WATER);
        } else if (hand.getType().equals(Material.LAVA_BUCKET)) {
            e.setCancelled(true);
            Block relative = e.getBlockClicked().getRelative(e.getBlockFace());
            relative.setType(Material.LAVA);
        }
    }

    static void preservePowderSnowBucket(PlayerBucketEmptyEvent e, ItemStack hand) {
        // Powder snow uses Minecraft's solid-bucket placement path. Let the
        // server perform that placement and only replace the resulting empty
        // bucket with the original unlimited bucket.
        e.setItemStack(hand.clone());
    }
    ```

    Note the contrast: water and lava unlimited buckets cancel the event and place the block manually, which prevents consumption. The powder snow path does not cancel and instead depends on the event's result item stack.

## Notes

- Commit `3d510b8` ("Fixes unlimited powder snow buckets") changed the powder snow handling from the cancel-and-manually-place approach to the `setItemStack`-based approach shown above; the bug report indicates the new approach still consumes the bucket.
- Server-side evidence (Paper 26.2 / MC 1.21.x): `CraftEventFactory.getPlayerBucketEvent` constructs `PlayerBucketEmptyEvent` with a default result item stack of an empty bucket (`Items.BUCKET`), and Paper only wires the event result item stack through `BucketItem.getEmptySuccessItem()` for water/lava (`BucketItem`). Powder snow uses the separate `SolidBucketItem` class, whose placement path consumes the in-hand item and leaves an empty bucket, so `e.setItemStack(...)` does not preserve the unlimited bucket.
- The existing unit test (`FishingAbilityManagerTest.unlimitedPowderSnowUsesVanillaPlacementAndKeepsBucket`) only verifies that `setItemStack` is called on a mocked event and `setCancelled` is never called; it cannot catch this regression.
