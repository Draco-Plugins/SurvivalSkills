# Requirements: Item Transfer Pipe System

## Things To Implement

### Custom Items & Crafting Recipes

- Add `WRENCH` and `TRANSFER_PIPE` entries to the `ItemModelData` enum with unique custom model data IDs.
- Add `getWrench()` and `getTransferPipe()` factory methods to `ItemStackGenerator` following the existing `ItemStackBuilder` + PDC `skillsItemKey` pattern. The wrench lore must explain its core interactions (sneak+left-click to attach/remove, left-click to link, right-click for info). The pipe lore must state it is consumed when attaching to a chest.
- Register a shaped crafting recipe for the wrench with shape `III:.R.:.D.` where I = Iron Ingot, R = Redstone Block, D = Diamond. Output: 1 wrench.
- Register a shaped crafting recipe for the transfer pipe with shape `GGG:CRC:GGG` where G = Glass, C = Copper Ingot, R = Redstone Block. Output: 2 transfer pipes.
- Both recipes must be registered through the existing `RecipeRegistrar` deferred-batch pipeline so they do not block server startup.

### Data Model & YAML Persistence

- Create an immutable `PipeLocation` Java record holding the world UUID and integer block x/y/z coordinates. Use it for persisted pipe identity and map keys instead of mutable Bukkit `Location` objects. Keep the persisted world name as descriptive/fallback YAML data, not as part of location equality. Resolve a Bukkit `World`, `Block`, or `Inventory` only after confirming that the world and every chunk covered by the chest are already loaded.
- Create a `PipeRecord` Java record holding: pipe UUID, owner UUID, `PipeLocation`, pipe type (`SENDER` or `RECEIVER`), `Optional<UUID>` sender UUID (receivers only), receiver UUID list (senders only), and whitelist (`Set<Material>`). Its canonical constructor must use `List.copyOf()` and `Set.copyOf()` so callers cannot mutate pipe state outside `PipeManager`.
- Create a `PipeManager` class that is the sole owner of pipe state and link mutations. It must maintain mutually consistent O(1) indexes by pipe UUID and by covered block location, plus a chunk index containing the pipe UUIDs whose single- or double-chest blocks intersect each chunk. Both halves of a double chest map to the same pipe UUID while only the canonical half is persisted.
- Maintain a loaded-sender set derived from the chunk index and chunk load/unload events. Populate it for chunks that are already loaded during plugin enable. A double-chest sender is eligible only when the chunks containing both halves are loaded. Interaction, transfer, breakage, and highlighting paths must query these indexes rather than scanning every pipe record.
- Treat `PipeRecord` as persistent configuration and keep frequently changing operational state separately. Per-sender receiver/source cursors and last-transfer status, per-player linking sessions, highlight entity IDs, loaded-chunk membership, and the persistence dirty flag are runtime-only and must not be written to YAML.
- Confine all `PipeManager` mutations and all Bukkit world, block, entity, and inventory access to the server main thread. Background persistence may receive only an immutable snapshot containing primitive values, strings, enum names, UUIDs, and immutable collections.
- Create a `pipedata.yml` file with load/save logic in `FileUtils`, following the existing `teleportanchors.yml` pattern. The YAML schema must store per pipe: owner UUID, type, world UUID and world name, x/y/z coordinates, receiver UUID list (senders only), sender UUID (receivers only), and whitelist material names (receivers only). Resolve the world by UUID first; use the stored name only as a fallback when that UUID is unavailable, then normalize the in-memory `PipeLocation` to the resolved world's UUID.
- Load pipe data in two phases without loading chunks: first parse and index structurally valid records by UUID, then reconcile links in O(pipe count + link count). The receiver's optional sender UUID is the authoritative persisted edge; rebuild each sender's receiver list from valid receiver edges. An edge with a missing sender, wrong pipe type, different owner, duplicate receiver, or out-of-range location must be discarded and the receiver loaded as orphaned. Log each invalid record or edge once during load rather than on every transfer cycle.
- Register pipe data loading in `SurvivalSkills.onEnable()` and saving in the existing auto-save cycle alongside other data files.
- Mark pipe data dirty only when attachment, removal, link, unlink, or whitelist state changes. Transfer cycles, power checks, timer ticks, HUD reads, and highlight changes must not mark it dirty. Skip an auto-save when data is clean.
- For an auto-save with dirty data, capture one immutable snapshot on the main thread and serialize/write only that isolated snapshot off the main thread. Coalesce overlapping save requests, clear the dirty flag only after a successful write of the latest snapshot, and retain it after failure so a later auto-save retries. On plugin disable, complete a final save of dirty data before shutdown.
- When a sender pipe is removed, all linked receiver pipes must have their sender UUID reference cleared (they become orphaned but remain attached to their chests).
- When a receiver pipe is removed, the sender's receiver UUID list must be updated to remove that receiver's UUID.

### Interaction System (Wrench + Chest)

- Create a pipe interaction listener (implementing `Listener`) that handles `PlayerInteractEvent` for all wrench-on-chest and wrench-on-air interactions. Register it in `SurvivalSkills.loadListeners()`.
- **Sneak + left-click on un-piped chest** (wrench in hand, pipe in inventory): consume one transfer pipe item from the player's inventory, attach a SENDER pipe to the chest, enter linking mode with a 60-second timer. If no pipe is in the inventory, send a failure message and do nothing.
- **Sneak + left-click on piped chest** (wrench in hand): remove the pipe from the chest, drop one transfer pipe item at the chest location, clean up the YAML entry and all link references. Only the pipe owner may remove a pipe; other players receive a denial message.
- **Left-click on sender chest** (no sneak, wrench in hand, within 250 blocks and active linking timer): re-select the sender, resetting the 60-second timer to full. If the player is beyond 250 blocks or the timer has expired, left-clicking still re-selects as long as the player is within 250 blocks (soft timer refresh).
- **Left-click on un-piped chest** (no sneak, wrench in hand, linking mode active): consume one transfer pipe, attach a RECEIVER pipe, link it to the currently selected sender, open the whitelist GUI immediately, reset the 60-second timer.
- **Left-click on orphaned receiver chest** (no sneak, wrench in hand, linking mode active): re-link the orphaned receiver to the currently selected sender, reset the 60-second timer. No pipe is consumed since the receiver already has one.
- **Right-click on sender chest** (wrench in hand): display a sender HUD via chat messages showing sender status, location, linked receiver count, current redstone power status, and transfer activity. The chest inventory must not open.
- **Right-click on receiver chest** (wrench in hand): open the whitelist GUI. The chest inventory must not open.
- **Right-click on air** (wrench in hand): display a linking status HUD via chat messages showing whether linking mode is active, timer remaining, sender location, and linked receiver count.
- **Right-click on un-piped chest** (wrench in hand): the chest opens normally (event passes through, no wrench action).
- **Left-click on receiver chest** (no sneak, wrench in hand): no special action (the receiver is not re-selected; only senders can be re-selected).
- Pipe ownership is enforced by UUID: only the player who placed a pipe can remove it, open its whitelist GUI, or re-link it. Other players wielding a wrench receive a denial message when attempting these actions on pipes they do not own.
- Double chest handling: when a pipe is attached to one half of a double chest, the pipe covers the entire double chest. Both block positions must resolve to the same `PipeRecord`. Block highlights must appear on both halves. The YAML stores only the canonical (primary) half location.

### Linking Timer & Range

- Store linking sessions in one manager keyed by player UUID and drive all countdowns from one shared synchronous task. Do not create one `BukkitRunnable` per player or retain `Player` references after logout.
- Represent session expiry as an absolute server-tick deadline. The shared task must detect threshold crossings so a lagged tick still sends each 30-, 10-, 3-, and 0-second message at most once. Re-selecting a sender replaces the deadline and resets the sent-threshold state.
- A 60-second linking timer starts when a sender pipe is first attached. While the timer is active and the player is within 250 blocks of the sender, the player can attach receiver pipes.
- The timer resets to 60 seconds each time a receiver is attached or the sender is re-selected.
- Countdown messages are sent to the player at 30 seconds, 10 seconds, 3 seconds, and 0 seconds remaining.
- If the player stops holding the wrench (switches item slot or hands), the timer is silently cancelled with no messages.
- If the player moves beyond 250 blocks from the sender chest, linking mode ends (timer cancelled with the 0-second message).
- If the timer naturally expires, linking mode ends (the 0-second message is sent).
- The 250-block range governs three things: the maximum distance for attaching receivers, the maximum distance for re-selecting a sender, and the maximum distance between a sender and its linked receivers.
- Compare range using a same-world check followed by squared block-coordinate distance against one precomputed `maxRangeSquared` value. Do not call square-root distance methods or repeatedly read configuration in interaction, timer, HUD, or load-reconciliation paths.

### Whitelist Filter GUI

- A 9-slot inventory titled "Pipe Filter" represents the whitelist for a receiver pipe.
- Shift-clicking an item from the player's inventory into the GUI adds a "ghost" copy (amount 1, Material only) to the whitelist, following the existing `AutoTrash` pattern. The original item is not consumed.
- Clicking a ghost item in the GUI removes it from the whitelist.
- An empty whitelist means all items are allowed to transfer to that receiver.
- A non-empty whitelist means only items whose `Material` matches an entry in the whitelist are transferred to that receiver.
- Whitelist changes must persist to the in-memory `PipeRecord` and be saved to YAML during the auto-save cycle.
- The GUI can be reopened at any time by right-clicking the receiver chest with the wrench.

### Block Highlighting

- When a player holds the wrench in their main hand, glowing `ItemDisplay` entities are spawned for owned pipe chest locations in the player's current world that are in loaded chunks within the server view distance, following the visual setup of `SpelunkerAbilitySync`. Do not create highlights in unrelated loaded chunks that the player cannot track.
- Highlight colors indicate pipe type and connection status:
  - **Blue**: sender chest with at least one linked receiver.
  - **Green**: receiver chest with a linked sender.
  - **Red**: sender chest with zero linked receivers, or receiver chest with no sender (orphaned). This signals to the player that the pipe is not connected to a functioning network.
- Highlights are only visible to the wrench-holding owner. All other online players have the entities hidden via `player.hideEntity()`.
- Highlights are removed when the player switches away from the wrench, logs out, or the pipe is removed.
- Highlight color must update in real time when a pipe's connection status changes (e.g., a sender that gains its first receiver transitions from red to blue; a receiver whose sender is removed transitions from green to red).
- Double chest pipes must show highlights on both block halves.
- Maintain one per-player highlight registry keyed by pipe UUID and covered block location. Reconcile it only when the player equips/unequips the wrench, crosses a chunk boundary, changes world, joins/quits, a relevant chunk loads/unloads, or an affected pipe changes. Query the chunk index for the newly visible area and add/remove only the delta; do not rescan every pipe or despawn and respawn unchanged displays on a repeating tick.
- Reuse an existing display when only its glow color changes, remove its old scoreboard-team entry before assigning the new one, and remove all display and team entries during cleanup. Highlight displays must be non-persistent and must be removed on plugin disable so they are never serialized into world chunk data.
- Apply owner-only visibility when a display is created and when another player begins tracking its area. Visibility reconciliation must be limited to players who could track that display rather than looping over all online players during every highlight refresh.

### Transfer Engine (Redstone-Triggered)

- Use one synchronous transfer scheduler for the entire pipe system. Spread loaded senders into stable work buckets across the configured interval and process one due bucket per tick, so every eligible sender is processed exactly once per interval without placing the full workload on a single tick. Assign a newly eligible sender to a least-populated bucket and keep that assignment until it becomes ineligible or the interval changes. Adding/removing senders or changing the configured interval must preserve the once-per-interval guarantee without creating per-sender tasks.
- The scheduler must iterate only the due UUIDs from the loaded-sender index. Before resolving any Bukkit block or inventory, verify the world is available and every chunk covered by the sender is loaded; apply the same check to each receiver. These checks must never call an API that loads or force-loads a chunk.
- Transfer occurs only when the sender chest block is powered (`Block.isBlockPowered()` or `Block.isBlockIndirectlyPowered()` returns true). This is an on/off check, not a pulse detector — continuous power means continuous transfer. Read and combine the sender's power state once per sender cycle, not once per receiver or source slot.
- Do not cache power or inventory contents across cycles because redstone, players, hoppers, and other plugins can change them between cycles.
- For each linked receiver, the sender pushes up to 1 stack of matching items per configured transfer interval. A sender with N receivers can move up to N stacks per interval total; with the default 20-tick interval this is N stacks per second.
- Process each linked receiver at most once per sender cycle, in circular order starting at a runtime-only receiver cursor. Stop early when no eligible source item remains, and advance the cursor after each cycle so receivers remain fair when the sender has fewer stacks than receivers. Use a runtime-only source-slot cursor as the starting point when inspecting the sender inventory so items in low-numbered slots cannot permanently starve later slots.
- Snapshot the sender inventory slots once at the start of its cycle and maintain an in-cycle index of non-empty source slots by `Material`. Update that local index as items move rather than rescanning the entire sender inventory for every receiver. Whitelist membership checks must use the receiver's `Set<Material>`.
- A receiver's one-stack allowance is one source-stack attempt, capped at that item's maximum stack size. Insert a clone that retains the source item's metadata, allow partial insertion when the receiver has some capacity, and decrement the source by exactly the amount accepted. Never remove from the source before the accepted amount is known; item loss or duplication is not permitted if only part of a stack fits.
- If a receiver is full, unloaded, orphaned, missing, no longer a chest, or cannot accept any eligible material, continue to the next receiver without a nested retry loop. Defer stale-record cleanup until after the current iteration so indexes are not modified while iterating, and ensure a stale record is quarantined or removed once rather than rediscovered and logged every cycle.
- Whitelist filtering: if a receiver has a non-empty whitelist, only items whose `Material` matches an entry in the whitelist are transferred to that receiver. A receiver with an empty whitelist accepts all items.
- Transfers only process when both the sender chunk and the receiver chunk are loaded. No chunk loading or force-loading is performed.
- Orphaned receivers (sender removed) do not receive items and are skipped by the transfer engine.
- Store each sender's last processed tick, amount moved, and resulting activity (`IDLE_UNPOWERED`, `ACTIVE`, or `BLOCKED`) in runtime state. The sender HUD must read this status instead of rescanning inventories: `ACTIVE` means the latest due cycle moved items, `BLOCKED` means it was powered but moved none, and `IDLE_UNPOWERED` means it was not powered.

### Chest Breakage Handling

- Listen for block break and explosion events on piped chests.
- When a piped chest is broken (by player or explosion), one transfer pipe item drops at the chest location.
- When a sender chest is broken: remove its YAML entry, clear the sender UUID reference on all linked receivers (they become orphaned).
- When a receiver chest is broken: remove its YAML entry, remove its UUID from the sender's receiver list.
- The pipe data must remain consistent after any breakage event.

### HUD Message Content

- **Air right-click HUD**: linking mode active/inactive, timer remaining (if active), sender location (if active), linked receiver count (if active).
- **Sender chest right-click HUD**: sender label, location, linked receiver count (and how many are within range), current redstone power status (powered/unpowered), and a hint for available wrench actions.

### Configuration

- Add the following entries to `config.yml`: pipe transfer interval in ticks (default 20), pipe max range in blocks (default 250), pipe linking time in seconds (default 60).
- Read these entries once into an immutable, exactly typed pipe configuration object during enable/reload. Reject non-positive transfer intervals and linking times and negative ranges with a warning and the documented default; precompute tick durations and squared range values used by hot paths.

## Tests To Create Or Update

- For `PipeRecord data structure`:
  - Verify all fields are accessible and the record is immutable.
  - Verify that a sender record can hold multiple receiver UUIDs and a receiver record can reference a sender UUID through `Optional<UUID>`.
  - Verify that mutating constructor input collections after creation cannot change the record's receiver list or whitelist and that returned collections reject mutation.
- For `PipeLocation and chunk-safe resolution`:
  - Verify block equality and hashing use world UUID plus integer x/y/z and are unaffected by Bukkit `Location` yaw/pitch or later mutation.
  - Verify unloaded or unavailable worlds/chunks are rejected before any block/inventory resolver is invoked, including a double chest crossing a chunk boundary.
- For `PipeManager lookup and link management`:
  - Verify adding a sender pipe stores it by UUID and by canonical location.
  - Verify adding a receiver pipe links it to the specified sender and adds the receiver UUID to the sender's list.
  - Verify removing a sender pipe clears the sender reference on all linked receivers (orphaning them).
  - Verify removing a receiver pipe removes its UUID from the sender's receiver list.
  - Verify that double chest locations resolve to the same `PipeRecord` regardless of which half is queried.
  - Verify that linking a receiver to a sender that already has a sender is rejected (one sender per receiver constraint).
  - Verify add, replace, remove, link, and unlink operations update the UUID, both-half location, chunk, sender, and loaded-sender indexes together without stale entries.
  - Verify a sender is added to and removed from the loaded-sender set by chunk load/unload state without scanning unrelated pipe records.
- For `YAML round-trip persistence`:
  - Verify that saving pipe data to YAML and loading it back preserves all fields: UUID, owner, type, location, receiver list, sender reference, whitelist.
  - Verify that orphaned receivers (`Optional.empty()` sender UUID) survive a save/load cycle.
  - Verify that a missing or corrupted YAML entry is skipped with a warning log, not a crash.
  - Verify two-phase loading rebuilds sender receiver lists from receiver edges in linear passes and orphans invalid, cross-owner, duplicate, wrong-type, missing, and out-of-range edges.
  - Verify loading records in unloaded chunks does not load those chunks or discard otherwise valid records.
- For `dirty and coalesced persistence`:
  - Verify state-changing pipe mutations mark data dirty while transfer/status/highlight operations do not.
  - Verify a clean auto-save performs no serialization or file write.
  - Verify an overlapping save request is coalesced, a successful latest write clears dirty state, and a failed or superseded write leaves data dirty for retry.
  - Verify the background writer receives only the immutable snapshot and does not access manager maps or Bukkit objects.
- For `whitelist filtering logic`:
  - Verify that an empty whitelist allows all materials.
  - Verify that a non-empty whitelist only allows materials present in the set.
  - Verify that adding a material to the whitelist and then removing it updates the allow/deny result correctly.
- For `transfer engine round-robin logic`:
  - Verify that items are distributed across receivers in round-robin order.
  - Verify receiver and source-slot cursors advance across cycles so limited source items and later inventory slots are not starved.
  - Verify that a full receiver is skipped and the next receiver is tried.
  - Verify that when all receivers are full, items remain in the sender.
  - Verify that a receiver with a whitelist only receives matching materials.
  - Verify that an orphaned receiver is skipped entirely.
  - Verify that an unpowered sender does not transfer any items.
  - Verify a partially full receiver accepts only its available amount and that the exact remainder stays in the sender without item loss or duplication.
  - Verify each receiver is attempted at most once in a cycle and the sender inventory is indexed once rather than rescanned for each receiver.
  - Verify the power state is read once per sender cycle and is not cached into the next cycle.
- For `transfer scheduler and loaded-sender work buckets`:
  - Verify every loaded sender is processed exactly once per configured interval and unloaded senders are not processed.
  - Verify sender work is distributed across interval ticks rather than all senders running on the same tick.
  - Verify adding/removing a sender updates scheduling without creating duplicate processing, and interval 1 remains valid.
- For `linking timer logic`:
  - Verify that the timer starts at 60 seconds when a sender is attached.
  - Verify that the timer resets to 60 seconds when a receiver is attached.
  - Verify that the timer resets to 60 seconds when the sender is re-selected.
  - Verify that the timer sends messages at the 30s, 10s, 3s, and 0s marks.
  - Verify a delayed scheduler tick detects crossed thresholds without duplicating or omitting threshold messages.
  - Verify that the timer is silently cancelled (no messages) when the wrench is no longer held.
  - Verify that the timer is cancelled when the player moves beyond 250 blocks.
  - Verify all sessions are handled by one scheduler and logout removes the UUID-keyed session without retaining a `Player` reference.
- For `range validation`:
  - Verify that a receiver within 250 blocks of the sender can be linked.
  - Verify that a receiver beyond 250 blocks of the sender cannot be linked.
  - Verify that re-selection of a sender beyond 250 blocks is rejected.
- For `highlight color logic`:
  - Verify that a sender with at least one linked receiver resolves to blue.
  - Verify that a sender with zero linked receivers resolves to red.
  - Verify that a receiver with a linked sender resolves to green.
  - Verify that an orphaned receiver (no sender) resolves to red.
  - Verify that the color transitions correctly when connection status changes (sender gains first receiver: red to blue; receiver loses sender: green to red).
- For `highlight lifecycle and reconciliation`:
  - Verify only owned pipes in loaded chunks within the wrench holder's current-world view distance receive displays.
  - Verify moving one chunk adds/removes only the display delta and does not recreate unchanged displays.
  - Verify a color-only state change reuses the display and cleans the old scoreboard-team entry.
  - Verify unequip, world change, logout, chunk unload, pipe removal, and plugin disable remove the applicable non-persistent displays and team entries.
  - Verify non-owners who begin tracking the area have the display hidden without a global all-player refresh.
- For `pipe configuration validation`:
  - Verify valid values are loaded once with the exact declared types and produce the expected tick deadlines, work buckets, and squared range.
  - Verify invalid interval, range, and linking-time values fall back to defaults and emit one warning per invalid value.
- For `recipe registration`:
  - Verify that the wrench recipe produces 1 wrench item with the correct custom model data.
  - Verify that the pipe recipe produces 2 transfer pipe items with the correct custom model data.
  - Verify that both recipes are registered through the deferred `RecipeRegistrar` pipeline.

## Important Background Information

- The plugin has no existing pipe, transfer, or conveyor system. This is entirely new functionality.
- Custom items are identified by a `PersistentDataContainer` key (`skillsItemKey`) set to `true` plus a custom model data float. The `ItemStackBuilder` always sets the PDC key. The `ItemModelData` enum maps model data IDs (currently 1-55 with a gap at 54, plus 999 for trophies).
- Recipes are registered declaratively via `SmallShapedSpec` / `ShapedSpec` records in data classes (e.g., `RewardRecipeData`), consumed by `RecipeMaker.createSmallShapedRecipe`, and pushed through `RecipeRegistrar` for deferred batch-drain on the main thread (10 per tick).
- Data persistence uses standard Bukkit `YamlConfiguration` via `FileUtils`. The teleport anchor system (`teleportanchors.yml`) is the closest existing pattern: it stores named entries with location, owner UUID, and loads/saves via `FileUtils.loadTeleportAnchors()` / `saveTeleportAnchors()`.
- `FileUtils` currently saves YAML synchronously. Pipe data can grow with every attached chest, so its auto-save path needs a dirty check and an immutable asynchronous persistence snapshot to keep file I/O out of gameplay ticks. Bukkit objects and live manager collections are not safe inputs to that background work.
- GUIs follow the `Bukkit.createInventory(null, size, title)` pattern with click handlers routed by inventory title or slot mapping. The `AutoTrash` system is the closest pattern for the whitelist GUI: it stores `Material` references (not real items), uses shift-click to add ghost items, and click to remove them.
- Block highlighting uses `ItemDisplay` entities spawned at block locations with `setGlowing(true)` and scoreboard teams for color. `SpelunkerAbilitySync` is a reference for the display's visual setup only: it spawns `ItemDisplay` at `block.getLocation().add(0.5, 0.5, 0.5)`, sets brightness and scale, and uses `player.hideEntity()` for visibility. Its per-player task, global-player visibility loop, and repeated add/remove approach must not be copied into the persistent pipe system. Blue and green teams already exist in that code.
- Timers use `BukkitRunnable` subclasses (e.g., `AbilityTimer`) running every 20 ticks. `AbilityTimer` provides countdown semantics to reference, but a pipe linking session is short-lived runtime state and all sessions share one manager task to avoid scheduler growth with player count.
- Player interactions are handled via `PlayerInteractEvent` listeners (e.g., `SortWandListener`). Shift detection uses `p.isSneaking()` or `e.getClick().isShiftClick()`. Hand checks use `e.getHand() == EquipmentSlot.HAND`.
- The project uses JUnit 5 for testing. Existing tests focus on pure logic methods (e.g., `AutoEatModeTest`, `PowerDrillTaskTest`) that do not require Bukkit API mocking. Test classes live in `src/test/java/sir_draco/survivalskills/` mirroring the main package structure.
- The plugin has `GriefPrevention` as a soft dependency but no existing pipe-related grief checks are required for this feature.
- Double chests in Bukkit are represented by `Chest.getInventory().getHolder()` returning a `DoubleChest` with `getLeftSide()` and `getRightSide()`. The canonical location should be the left side's location.
- Bukkit world, block, entity, inventory, and scheduler state is main-thread confined. The transfer engine therefore optimizes synchronous work through indexes, one-pass inventory preparation, stable work buckets, and event-driven cache maintenance rather than moving Bukkit API access asynchronously.
- Sender-to-receiver links are stored redundantly in YAML for readability, but the receiver's single sender UUID is the authoritative edge. Rebuilding the derived sender lists at load time prevents two independently trusted relationship lists from drifting and keeps transfer traversal direct after startup.
- The expected hot-path costs are O(1) for pipe lookup by UUID or chest block, O(view-distance chunks + pipes in those chunks) for highlight reconciliation, and O(due loaded senders + their linked receivers + inspected inventory slots) for a scheduler tick. These bounds are the reason for the UUID/location/chunk indexes and the ban on global pipe scans.

## Things To Ensure Are Not Done

- Do not add upgraded pipe variants (extended range, faster transfer, larger whitelist) in this feature. The 250-block range and 9-slot whitelist are the baseline; upgrades are a separate future feature.
- Do not support containers other than single chests and double chests. Barrels, shulker boxes, dispensers, droppers, hoppers, and ender chests are out of scope.
- Do not implement pulse-triggered transfer. The transfer engine checks `isBlockPowered()` as an on/off state, not a rising-edge pulse detector.
- Do not force-load or keep loaded chunks for transfer. Only process transfers when both sender and receiver chunks are already loaded.
- Do not call `getChunkAt()`, resolve a block/inventory, or otherwise use an API with chunk-loading side effects merely to check pipe status. Query world/chunk availability first from stored coordinates.
- Do not scan all pipe records for chest interactions, break/explosion handling, each transfer tick, player movement, or highlight updates. Use the UUID, covered-location, loaded-sender, and chunk indexes.
- Do not create a repeating scheduler task per sender, receiver, linking player, or highlighted player. Use the shared transfer/linking schedulers and event-driven highlight reconciliation.
- Do not run Bukkit world, block, inventory, entity, player, or live `YamlConfiguration` access asynchronously. Only serialization and file I/O over an isolated immutable pipe snapshot may run off the main thread.
- Do not save `pipedata.yml` for transfers, power/status changes, timer ticks, or highlight changes, and do not start overlapping file writes.
- Do not cache a chest inventory, item contents, redstone state, `Block`, `World`, or entity reference as persistent pipe data. These values can become stale or retain unloaded world/chunk state; resolve them only for an already-loaded due operation.
- Do not allow cross-player pipe networks. Pipes are owned by UUID and only the owner can interact with them. Other players cannot attach, remove, open whitelists, or view highlights for pipes they do not own.
- Do not allow a receiver to be linked to more than one sender simultaneously. The one-sender-per-receiver constraint must be enforced at link time.
- Do not allow right-click on a piped chest to open the chest inventory while the wrench is held. The wrench intercepts right-clicks on piped chests for HUD/GUI actions.
- Do not support enchanted books or item-NBT-based filtering in the whitelist. The whitelist matches by `Material` only, following the `AutoTrash` material pattern.
- Do not change existing chest, hopper, comparator, or redstone behavior. The pipe system reads redstone power but does not modify it.
- Do not add the wrench or pipes to any skill reward system or `CRAFT_RESTRICTIONS` map unless explicitly requested. They are standalone crafted items.
- Do not log success-path events. Per the project's `AGENTS.md`, logs should only be used when things go wrong.
- Do not emit the same missing chest, invalid link, unloaded chunk, full receiver, or blocked-transfer warning every cycle. Expected no-work states are silent; structural corruption is logged once when it is quarantined or repaired.

## User Decisions Made During Requirement Creation

| Decision Needed | Answer | Reason |
| --- | --- | --- |
| Timer re-selection logic | Soft timer, refreshable | Left-clicking the sender refreshes the 60s window as long as the player is within 250 blocks. Prevents players from being permanently locked out of linking mode by a single missed timer. |
| Transfer mechanism | Redstone-triggered (on/off) | Fits the "high tech" theme and gives players control via redstone builds. Comparators work naturally with this model. |
| Supported containers | Single and double chests only | Simplifies implementation. One pipe covers a full double chest. Other container types are a future expansion. |
| Overflow behavior | Round-robin receivers | If one receiver is full, try the next. If all are full, items stay in the sender. Simple and predictable. |
| Right-click on piped chest | Shows pipe HUD (not chest inventory) | The wrench is actively "using" the pipe network. Players switch to a different item to browse chest contents. |
| Right-click on air | Shows current linking status | Gives the player feedback on timer state and connection info without targeting a specific block. |
| Right-click on receiver | Opens whitelist GUI | Intuitive filter editing without re-attaching the pipe. |
| Wrench recipe | `III:.R.:.D.` (Iron Ingot, Redstone Block, Diamond) | User-specified. Iron frame, redstone energy core, diamond tip for a high-tech feel. |
| Pipe recipe | `GGG:CRC:GGG` (Glass, Copper Ingot, Redstone Block), yields 2 | User-specified. Glass shell, copper conduit, redstone power source. |
| Transfer rate | 1 stack per receiver per configured interval (20 ticks by default) | User-specified default behavior. A sender with N receivers moves up to N stacks per second at the default interval, while the configuration remains internally consistent if changed. |
| Chest breakage handling | Pipe drops as item, links cleaned | Player does not lose their pipe investment. YAML stays consistent. Sender removal orphans receivers; receiver removal updates sender's list. |
| Sender removal cascading | Receivers stay orphaned | Players can re-link orphaned receivers to a new sender without re-crafting pipes. Reduces material cost of reconfiguring networks. |
| Max range | 250 blocks | User-specified. Sufficient for a single base. Upgraded pipes could extend this later. |
| Linking timer duration | 60 seconds | User-specified. Enough time to walk around a base and link receivers. |
| Timer messages | 30s, 10s, 3s, 0s remaining | User-specified. Gives escalating urgency without spamming. |
| Wrench unequip behaviour | Silent timer cancel (no messages) | Prevents message spam when the player switches tools. Timer can be refreshed by re-selecting the sender. |
| Whitelist matching | Material only (no NBT/enchant matching) | Follows the existing `AutoTrash` pattern. Keeps the filter simple and predictable for item transfer use cases. |
| Disconnected pipe highlight color | Red for senders with no receivers and orphaned receivers | Gives players immediate visual feedback that a pipe is not connected to a functioning network, distinguishing it from active blue/green pipes. |
| Persistent location representation | Immutable `PipeLocation` block coordinates, not Bukkit `Location` map keys | Bukkit `Location` is mutable and contains orientation/world references that are irrelevant to a chest identity. An immutable world UUID + block-coordinate record makes hashing stable and permits chunk checks before Bukkit object resolution. |
| In-memory lookup architecture | UUID, covered-block, chunk, and loaded-sender indexes owned by `PipeManager` | Interactions and periodic work need direct lookup. Maintaining indexes on rare topology mutations avoids repeated global scans during frequent events and transfer cycles. |
| Pipe state mutation model | Immutable `PipeRecord` replacement on the main thread | Defensive immutable collections prevent listeners, GUIs, and tasks from changing links behind the manager's indexes. Main-thread confinement matches Bukkit's API rules and removes synchronization from gameplay paths. |
| Transfer scheduling | One scheduler with stable work buckets across the interval | Every loaded sender retains the configured transfer rate, but processing is staggered across ticks to avoid a once-per-second CPU spike and per-sender task overhead. |
| Chunk eligibility tracking | Maintain loaded senders from chunk events and check all double-chest chunks before resolution | This limits scheduled work to viable senders and prevents a status check from loading chunks. Double chests can cross a chunk boundary, so both halves must be available. |
| Transfer scan strategy | One sender-slot snapshot/index and at most one attempt per receiver per cycle | Building candidates once avoids repeatedly scanning up to 54 sender slots for every receiver. Bounded receiver passes also keep the work proportional to the network size. |
| Partial insertion semantics | Remove exactly the amount the receiver accepted | A receiver with partial capacity should still receive items. Calculating accepted quantity before source mutation prevents loss and duplication while preserving item metadata. |
| Round-robin fairness state | Runtime-only receiver and source-slot cursors | Cursors prevent receivers and later chest slots from starving when supply is limited. They change every cycle and do not affect network topology, so persisting them would add needless disk churn. |
| Linking timer architecture | UUID-keyed sessions with absolute tick deadlines on one shared task | One shared task scales with active players, avoids retained `Player` references, and threshold-crossing checks remain correct when the server lags. |
| Highlight scope and lifecycle | Delta-reconciled, non-persistent displays only within the owner's current-world view distance | Spawning displays for unrelated loaded chunks provides no visible benefit. Reusing nearby displays and reacting to lifecycle events avoids global scans, entity churn, scoreboard leaks, and serialized ghost entities. |
| Link authority during load | Receiver sender UUID is authoritative; sender receiver lists are rebuilt | A receiver can have only one sender, making that field the unambiguous edge. Rebuilding the redundant reverse lists in two linear passes repairs inconsistent YAML without quadratic searches. |
| Pipe persistence strategy | Dirty, coalesced immutable snapshots with background file I/O | Topology/filter changes are infrequent compared with transfer cycles. Skipping clean saves and isolating YAML writes keeps disk work off gameplay ticks without exposing Bukkit state to another thread. |
| Hot-path configuration and range checks | Validate once, store exact types, and compare squared distances | Repeated config lookups and square-root distance calculations add avoidable work to interaction, timer, load, and HUD paths. Precomputed validated values also make invalid configuration behavior predictable. |
| Sender transfer activity status | Cache only the latest cycle outcome in runtime state | HUD reads should not rescan inventories or duplicate transfer decisions. A three-state outcome distinguishes unpowered, actively moving, and powered-but-blocked senders with constant-time reads. |
