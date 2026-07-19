package sir_draco.survivalskills.skill_listeners.builderswand;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.skill_listeners.BuildingSkill;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.skills.SkillManager;
import sir_draco.survivalskills.utils.Utils;
import sir_draco.survivalskills.utils.items.ItemModelData;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;

public final class BuilderWandListener implements Listener {

    private static final int BUILDER_WAND_MODEL_DATA = ItemModelData.BUILDER_WAND.getId();
    private static final int PREVIEW_INTERVAL_TICKS = 2;
    private static final int PLACEMENT_COOLDOWN_TICKS = 10;
    private static final double DEFAULT_BLOCK_REACH = 4.5;
    private static final int FIRST_STORAGE_SLOT = 0;
    private static final int LAST_STORAGE_SLOT = 35;

    private final SurvivalSkills plugin;
    private final BuildingSkill buildingSkill;
    private final NamespacedKey modeKey;
    private final NamespacedKey reachModifierKey;
    private final Map<UUID, PreviewState> previews = new HashMap<>();
    private final Map<UUID, Long> placementCooldowns = new HashMap<>();
    private final Map<UUID, Long> interactionTicks = new HashMap<>();
    private final BukkitTask previewTask;
    private long currentTick;

    public BuilderWandListener(SurvivalSkills plugin, BuildingSkill buildingSkill) {
        this.plugin = plugin;
        this.buildingSkill = buildingSkill;
        this.modeKey = new NamespacedKey(plugin, "builder_wand_mode");
        this.reachModifierKey = new NamespacedKey(plugin, "builder_wand_reach");
        this.previewTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::updatePreviews,
                PREVIEW_INTERVAL_TICKS, PREVIEW_INTERVAL_TICKS);
    }

    @EventHandler
    public void onWandUse(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Optional<HeldWand> optionalHeldWand = getHeldWand(player);
        if (event.getHand() == null || optionalHeldWand.isEmpty()
                || !event.getHand().equals(optionalHeldWand.get().hand())) {
            return;
        }
        HeldWand heldWand = optionalHeldWand.get();

        if (event.getAction().equals(Action.LEFT_CLICK_AIR)) {
            event.setCancelled(true);
            cycleMode(player, heldWand);
            return;
        }
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }

        event.setCancelled(true);
        if (isDuplicateInteraction(player)) {
            return;
        }
        placeBlocks(player, heldWand);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearPreview(event.getPlayer().getUniqueId());
        removeReachModifier(event.getPlayer());
        placementCooldowns.remove(event.getPlayer().getUniqueId());
        interactionTicks.remove(event.getPlayer().getUniqueId());
    }

    public void shutdown() {
        previewTask.cancel();
        List.copyOf(previews.keySet()).forEach(this::clearPreview);
        plugin.getServer().getOnlinePlayers().forEach((Player player) -> removeReachModifier(player));
        placementCooldowns.clear();
        interactionTicks.clear();
    }

    private void updatePreviews() {
        currentTick += PREVIEW_INTERVAL_TICKS;
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Optional<HeldWand> heldWand = getHeldWand(player);
            if (heldWand.isEmpty() || !hasUnlockedWand(player)) {
                clearPreview(player.getUniqueId());
                removeReachModifier(player);
                continue;
            }
            updateReachModifier(player);
            Optional<PlacementPlan> plan = createPlacementPlan(player, heldWand.get());
            if (plan.isEmpty()) {
                clearPreview(player.getUniqueId());
                continue;
            }
            showPreview(player, plan.get());
        }
    }

    private void cycleMode(Player player, HeldWand heldWand) {
        int buildingLevel = getBuildingLevel(player);
        if (buildingLevel < BuilderWandTier.BASIC.getRequiredLevel()) {
            sendLevelRequirement(player);
            return;
        }

        ItemStack wand = heldWand.item();
        BuilderWandTier currentMode = getMode(wand, buildingLevel);
        BuilderWandTier nextMode = BuilderWandTier.nextMode(currentMode, buildingLevel);
        setMode(wand, nextMode);
        player.getInventory().setItem(heldWand.hand(), wand);
        player.sendRawMessage(ChatColor.GOLD + "Builder's Wand mode: " + ChatColor.AQUA
                + nextMode.getBlockLimit() + ChatColor.GOLD + " blocks");
        player.playSound(player, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f,
                0.8f + 0.2f * nextMode.ordinal());
        clearPreview(player.getUniqueId());
    }

    private void placeBlocks(Player player, HeldWand heldWand) {
        if (!hasUnlockedWand(player)) {
            sendLevelRequirement(player);
            return;
        }
        long cooldownEnd = placementCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (currentTick < cooldownEnd) {
            player.sendRawMessage(ChatColor.RED + "The Builder's Wand is still recharging.");
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.8f);
            return;
        }

        Optional<PlacementPlan> optionalPlan = createPlacementPlan(player, heldWand);
        if (optionalPlan.isEmpty() || optionalPlan.get().targets().isEmpty()) {
            player.sendRawMessage(ChatColor.RED + "There are no valid blocks to place here.");
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.8f);
            return;
        }

        PlacementPlan plan = optionalPlan.get();
        int requiredBlocks = plan.targets().size();
        if (!isCreative(player)
                && countAvailableBlocks(player.getInventory(), plan.reference().getType()) < requiredBlocks) {
            player.sendRawMessage(ChatColor.RED + "You need " + ChatColor.AQUA + requiredBlocks + " "
                    + readableMaterialName(plan.reference().getType()) + ChatColor.RED + " blocks in your inventory.");
            player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            return;
        }

        clearPreview(player.getUniqueId());
        int placedBlocks = placePlan(player, plan);
        if (placedBlocks == 0) {
            return;
        }
        if (!isCreative(player)) {
            consumeBlocks(player.getInventory(), plan.reference().getType(), placedBlocks);
        }
        buildingSkill.awardWandExperience(player);
        placementCooldowns.put(player.getUniqueId(), currentTick + PLACEMENT_COOLDOWN_TICKS);
        player.playSound(player, Sound.BLOCK_STONE_PLACE, 1.0f, 1.1f);
    }

    private int placePlan(Player player, PlacementPlan plan) {
        int placedBlocks = 0;
        buildingSkill.beginWandPlacement(player);
        try {
            for (Block target : plan.targets()) {
                if (!target.getType().isAir() || !canPlace(player, target)) {
                    continue;
                }
                BlockState replacedState = target.getState();
                target.setBlockData(plan.blockData().clone(), false);
                Block support = target.getRelative(plan.face().getOppositeFace());
                BlockPlaceEvent event = new BlockPlaceEvent(target, replacedState, support,
                        new ItemStack(plan.reference().getType()), player, true, plan.hand());
                plugin.getServer().getPluginManager().callEvent(event);
                if (event.isCancelled() || !event.canBuild()) {
                    replacedState.update(true, false);
                    continue;
                }
                placedBlocks++;
            }
        } finally {
            buildingSkill.endWandPlacement(player);
        }
        return placedBlocks;
    }

    private Optional<PlacementPlan> createPlacementPlan(Player player, HeldWand heldWand) {
        int buildingLevel = getBuildingLevel(player);
        BuilderWandTier mode = getMode(heldWand.item(), buildingLevel);
        double reach = getEffectiveReach(player);
        World world = player.getWorld();
        RayTraceResult result = world.rayTraceBlocks(player.getEyeLocation(),
                player.getEyeLocation().getDirection(), reach, FluidCollisionMode.NEVER, true);
        if (result == null || result.getHitBlock() == null || result.getHitBlockFace() == null) {
            return Optional.empty();
        }

        Block reference = result.getHitBlock();
        BlockFace face = result.getHitBlockFace();
        if (!isUsableReference(reference)) {
            return Optional.empty();
        }

        BuilderWandPosition origin = positionOf(reference);
        Material referenceType = reference.getType();
        List<BuilderWandPosition> positions = BuilderWandGeometry.findPlacements(origin, face,
                mode.getBlockLimit(),
                (BuilderWandPosition position) -> world.getBlockAt(position.x(), position.y(), position.z())
                        .getType().equals(referenceType),
                (BuilderWandPosition position) -> isValidTarget(player, world, position));
        if (positions.isEmpty()) {
            return Optional.empty();
        }

        BlockData placementData = preparePlacementData(reference.getBlockData());
        List<Block> targets = positions.stream()
                .map((BuilderWandPosition position) -> world.getBlockAt(position.x(), position.y(), position.z()))
                .toList();
        return Optional.of(new PlacementPlan(reference, face, placementData, targets, heldWand.hand()));
    }

    private boolean isValidTarget(Player player, World world, BuilderWandPosition position) {
        if (position.y() < world.getMinHeight() || position.y() >= world.getMaxHeight()) {
            return false;
        }
        Block target = world.getBlockAt(position.x(), position.y(), position.z());
        return target.getType().isAir() && canPlace(player, target);
    }

    private boolean canPlace(Player player, Block target) {
        if (plugin.isGriefPreventionEnabled() && Utils.checkForClaim(player, target.getLocation())) {
            return false;
        }
        return !plugin.isWorldGuardEnabled() || plugin.getWorldGuardProvider() == null
                || plugin.getWorldGuardProvider().canPlaceBlockInRegion(player, target.getLocation());
    }

    private boolean isUsableReference(Block block) {
        Material material = block.getType();
        if (!material.isBlock() || !material.isItem() || !material.isSolid()) {
            return false;
        }
        BlockData data = block.getBlockData();
        if (data instanceof Bed) {
            return false;
        }
        if (data instanceof Slab slab && slab.getType().equals(Slab.Type.DOUBLE)) {
            return false;
        }
        return !(data instanceof Bisected) || data instanceof Stairs;
    }

    private void showPreview(Player player, PlacementPlan plan) {
        List<BuilderWandPosition> positions = plan.targets().stream()
                .map(BuilderWandListener::positionOf)
                .toList();
        PreviewSignature signature = new PreviewSignature(plan.reference().getWorld().getUID(),
                plan.reference().getType(), plan.blockData().getAsString(), positions);
        PreviewState current = previews.get(player.getUniqueId());
        if (current != null && current.signature().equals(signature)) {
            return;
        }

        clearPreview(player.getUniqueId());
        List<ItemDisplay> displays = new ArrayList<>();
        for (Block target : plan.targets()) {
            Location location = target.getLocation().add(0.5, 0.5, 0.5);
            try {
                ItemDisplay display = target.getWorld().spawn(location, ItemDisplay.class, entity -> {
                    entity.setItemStack(new ItemStack(plan.reference().getType()));
                    entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
                    entity.setGlowing(true);
                    entity.setGlowColorOverride(Color.LIME);
                    entity.setBrightness(new Display.Brightness(8, 8));
                    entity.setTransformation(new Transformation(new Vector3f(), new Quaternionf(),
                            new Vector3f(1.8f), new Quaternionf()));
                    entity.setGravity(false);
                    entity.setInvulnerable(true);
                    entity.setPersistent(false);
                    entity.setVisibleByDefault(false);
                    entity.setViewRange(0.25f);
                    entity.setShadowStrength(0.0f);
                });
                player.showEntity(plugin, display);
                displays.add(display);
            } catch (IllegalArgumentException exception) {
                plugin.getServer().getLogger().log(Level.WARNING,
                        String.format("[SurvivalSkills] Failed to render Builder's Wand preview for %s",
                                player.getName()), exception);
            }
        }
        previews.put(player.getUniqueId(), new PreviewState(signature, List.copyOf(displays)));
    }

    private void clearPreview(UUID playerId) {
        PreviewState preview = previews.remove(playerId);
        if (preview == null) {
            return;
        }
        preview.displays().stream()
                .filter((ItemDisplay display) -> display.isValid())
                .forEach((ItemDisplay display) -> display.remove());
    }

    private BuilderWandTier getMode(ItemStack wand, int buildingLevel) {
        if (wand == null) {
            return BuilderWandTier.BASIC;
        }
        ItemMeta meta = wand.getItemMeta();
        if (meta == null) {
            return BuilderWandTier.BASIC;
        }
        int storedMode = meta.getPersistentDataContainer().getOrDefault(modeKey, PersistentDataType.INTEGER, 0);
        return BuilderWandTier.modeFor(storedMode, buildingLevel);
    }

    private void setMode(ItemStack wand, BuilderWandTier mode) {
        ItemMeta meta = wand.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(modeKey, PersistentDataType.INTEGER, mode.ordinal());
        wand.setItemMeta(meta);
    }

    private boolean isBuilderWand(ItemStack item) {
        return ItemStackGeneratorUtils.isCustomItem(item, BUILDER_WAND_MODEL_DATA);
    }

    private Optional<HeldWand> getHeldWand(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack mainHand = inventory.getItemInMainHand();
        ItemStack offHand = inventory.getItemInOffHand();
        Optional<EquipmentSlot> hand = selectWandHand(isBuilderWand(mainHand), isBuilderWand(offHand));
        return hand.map((EquipmentSlot selectedHand) -> new HeldWand(selectedHand,
                selectedHand.equals(EquipmentSlot.HAND) ? mainHand : offHand));
    }

    static Optional<EquipmentSlot> selectWandHand(boolean mainHandHasWand, boolean offHandHasWand) {
        if (mainHandHasWand) {
            return Optional.of(EquipmentSlot.HAND);
        }
        if (offHandHasWand) {
            return Optional.of(EquipmentSlot.OFF_HAND);
        }
        return Optional.empty();
    }

    private boolean isDuplicateInteraction(Player player) {
        UUID playerId = player.getUniqueId();
        Long previousInteractionTick = interactionTicks.put(playerId, currentTick);
        return previousInteractionTick != null && previousInteractionTick.longValue() == currentTick;
    }

    private boolean hasUnlockedWand(Player player) {
        return getBuildingLevel(player) >= BuilderWandTier.BASIC.getRequiredLevel();
    }

    private int getBuildingLevel(Player player) {
        return SkillManager.getSkillLevel(player.getUniqueId(), SkillCategory.BUILDING);
    }

    private double getEffectiveReach(Player player) {
        AttributeInstance reach = player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE);
        return reach == null ? DEFAULT_BLOCK_REACH : reach.getValue();
    }

    private void updateReachModifier(Player player) {
        AttributeInstance reach = player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE);
        if (reach == null) {
            return;
        }
        int reachBonus = BuilderWandTier.highestUnlockedAt(getBuildingLevel(player)).getReachBonus();
        Optional<AttributeModifier> existingModifier = reach.getModifiers().stream()
                .filter((AttributeModifier modifier) -> modifier.getKey().equals(reachModifierKey))
                .findFirst();
        if (existingModifier.isPresent() && existingModifier.get().getAmount() == reachBonus) {
            return;
        }
        existingModifier.ifPresent((AttributeModifier modifier) -> reach.removeModifier(modifier));
        if (reachBonus > 0) {
            reach.addModifier(new AttributeModifier(reachModifierKey, reachBonus,
                    AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
        }
    }

    private void removeReachModifier(Player player) {
        AttributeInstance reach = player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE);
        if (reach == null) {
            return;
        }
        reach.getModifiers().stream()
                .filter((AttributeModifier modifier) -> modifier.getKey().equals(reachModifierKey))
                .findFirst()
                .ifPresent((AttributeModifier modifier) -> reach.removeModifier(modifier));
    }

    private void sendLevelRequirement(Player player) {
        player.sendRawMessage(ChatColor.RED + "You need to be building level " + ChatColor.AQUA
                + BuilderWandTier.BASIC.getRequiredLevel() + ChatColor.RED + " to use the Builder's Wand.");
        player.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
    }

    private static boolean isCreative(Player player) {
        return player.getGameMode().equals(GameMode.CREATIVE);
    }

    static int countAvailableBlocks(PlayerInventory inventory, Material material) {
        int available = 0;
        for (int slot = FIRST_STORAGE_SLOT; slot <= LAST_STORAGE_SLOT; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (isEligibleInventoryStack(item, material)) {
                available += item.getAmount();
            }
        }
        return available;
    }

    static boolean isEligibleInventoryStack(ItemStack item, Material material) {
        return item != null && item.getType().equals(material) && !item.hasItemMeta();
    }

    static BlockData preparePlacementData(BlockData referenceData) {
        BlockData placementData = referenceData.clone();
        if (placementData instanceof Waterlogged waterlogged) {
            waterlogged.setWaterlogged(false);
        }
        return placementData;
    }

    static void consumeBlocks(PlayerInventory inventory, Material material, int amount) {
        int remaining = amount;
        for (int slot = FIRST_STORAGE_SLOT; slot <= LAST_STORAGE_SLOT && remaining > 0; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (!isEligibleInventoryStack(item, material)) {
                continue;
            }
            int consumed = Math.min(remaining, item.getAmount());
            int newAmount = item.getAmount() - consumed;
            if (newAmount == 0) {
                inventory.setItem(slot, null);
            } else {
                ItemStack remainingStack = item.clone();
                remainingStack.setAmount(newAmount);
                inventory.setItem(slot, remainingStack);
            }
            remaining -= consumed;
        }
        if (remaining != 0) {
            throw new IllegalStateException("Builder's Wand inventory changed during placement");
        }
    }

    private static BuilderWandPosition positionOf(Block block) {
        return new BuilderWandPosition(block.getX(), block.getY(), block.getZ());
    }

    private static String readableMaterialName(Material material) {
        String name = material.toString().toLowerCase().replace('_', ' ');
        return name.endsWith("s") ? name : name + "s";
    }

    private record HeldWand(EquipmentSlot hand, ItemStack item) {}

    private record PlacementPlan(Block reference, BlockFace face, BlockData blockData, List<Block> targets,
                                 EquipmentSlot hand) {}

    private record PreviewSignature(UUID worldId, Material material, String blockData,
                                    List<BuilderWandPosition> positions) {}

    private record PreviewState(PreviewSignature signature, List<ItemDisplay> displays) {}
}
