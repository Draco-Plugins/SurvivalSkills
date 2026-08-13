package sir_draco.survivalskills.god_questline.powerore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

/** Inventory-based pair matching challenge for Power Ore conversion. */
public final class PowerOreMemoryMatchTask implements PowerOreInventoryTask {

    private static final int INVENTORY_SIZE = 54;
    private static final int INVENTORY_COLUMNS = 9;
    private static final int INVENTORY_ROWS = INVENTORY_SIZE / INVENTORY_COLUMNS;
    private static final long PREVIEW_TICKS = 10L * 20L;
    private static final long MISMATCH_FEEDBACK_TICKS = 20L;
    private static final long NEXT_ROUND_DELAY_TICKS = 40L;
    private static final String INVENTORY_TITLE = ChatColor.DARK_PURPLE + "Memory Match";
    private static final String HIDDEN_MATCH_NAME = ChatColor.GRAY + "Hidden Match";
    private static final List<Material> MATCH_MATERIALS = List.of(
            Material.DIAMOND,
            Material.EMERALD,
            Material.GOLD_INGOT,
            Material.IRON_INGOT,
            Material.REDSTONE,
            Material.LAPIS_LAZULI,
            Material.COAL,
            Material.COPPER_INGOT,
            Material.QUARTZ,
            Material.AMETHYST_SHARD,
            Material.PRISMARINE_SHARD,
            Material.BLAZE_POWDER,
            Material.SLIME_BALL,
            Material.ENDER_PEARL,
            Material.GHAST_TEAR,
            Material.RABBIT_FOOT,
            Material.FEATHER,
            Material.BONE,
            Material.WHEAT,
            Material.APPLE,
            Material.CLOCK);

    private final PowerOreChallenge challenge;
    private final Player player;
    private final PowerOreMemoryMatchGame game;
    private final Set<BukkitTask> scheduledTasks = new HashSet<>();
    private Optional<Inventory> inventory = Optional.empty();
    private List<Integer> boardSlots = List.of();
    private boolean finished;

    public PowerOreMemoryMatchTask(PowerOreChallenge challenge, Player player) {
        this(challenge, player, new Random());
    }

    PowerOreMemoryMatchTask(PowerOreChallenge challenge, Player player, Random random) {
        this.challenge = challenge;
        this.player = player;
        this.game = new PowerOreMemoryMatchGame(random);
    }

    @Override
    public void start() {
        Inventory memoryInventory = Bukkit.createInventory(null, INVENTORY_SIZE, INVENTORY_TITLE);
        inventory = Optional.of(memoryInventory);
        player.openInventory(memoryInventory);
        startRound();
    }

    private void startRound() {
        PowerOreMemoryMatchGame.RoundDefinition round = game.getRoundDefinition();
        boardSlots = createBoardSlots(round);
        Inventory memoryInventory = getInventory();
        memoryInventory.clear();
        IntStream.range(0, round.tileCount()).forEach((int position) ->
                memoryInventory.setItem(boardSlots.get(position), createMatchItem(game.getPairIdentifier(position))));
        int displayedRound = game.getRoundIndex();
        player.sendMessage(ChatColor.AQUA + "Memory Match round " + (displayedRound + 1) + " of "
                + PowerOreMemoryMatchGame.getRoundDefinitions().size() + ": "
                + ChatColor.YELLOW + "Memorize the pairs! You have 10 seconds.");

        schedule(() -> hideRound(displayedRound), PREVIEW_TICKS);
    }

    private void hideRound(int displayedRound) {
        if (!isCurrentRoundRunning(displayedRound))
            return;
        game.beginMatching();
        IntStream.range(0, boardSlots.size())
                .filter((int position) -> !game.isMatched(position))
                .forEach((int position) -> getInventory().setItem(boardSlots.get(position), createHiddenItem()));
        player.sendMessage(ChatColor.GREEN + "Find the matching pairs! " + ChatColor.RED
                + game.getLivesRemaining() + " lives remaining.");
    }

    @Override
    public boolean ownsInventory(Inventory candidateInventory) {
        return inventory.filter((Inventory taskInventory) -> taskInventory.equals(candidateInventory)).isPresent();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        if (!ownsInventory(event.getView().getTopInventory()))
            return;
        event.setCancelled(true);

        int rawSlot = event.getRawSlot();
        int position = boardSlots.indexOf(rawSlot);
        if (position < 0)
            return;

        PowerOreMemoryMatchGame.SelectionResult result = game.select(position);
        revealPositions(result.revealedPositions());
        switch (result.outcome()) {
            case IGNORED -> {
                return;
            }
            case FIRST_REVEALED -> player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1);
            case MATCH -> playMatchSound();
            case MISMATCH -> handleMismatch(result);
            case ROUND_COMPLETE -> handleRoundComplete();
            case GAME_COMPLETE -> handleGameComplete();
            case GAME_FAILED -> handleGameFailed();
        }
    }

    private void handleMismatch(PowerOreMemoryMatchGame.SelectionResult result) {
        player.playSound(player, Sound.BLOCK_GLASS_BREAK, 1, 0.7f);
        player.sendMessage(ChatColor.RED + "No match! " + result.livesRemaining() + " lives remaining.");
        int displayedRound = game.getRoundIndex();
        List<Integer> mismatchedPositions = result.revealedPositions();
        schedule(() -> {
            if (!isCurrentRoundRunning(displayedRound))
                return;
            mismatchedPositions.stream()
                    .filter((Integer position) -> !game.isMatched(position))
                    .forEach((Integer position) -> getInventory().setItem(boardSlots.get(position), createHiddenItem()));
            game.resolveMismatch();
        }, MISMATCH_FEEDBACK_TICKS);
    }

    private void handleRoundComplete() {
        playMatchSound();
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        player.sendMessage(ChatColor.GREEN + "Round complete! Get ready for the next board.");
        int completedRound = game.getRoundIndex();
        schedule(() -> {
            if (!isCurrentRoundRunning(completedRound))
                return;
            game.advanceRound();
            startRound();
        }, NEXT_ROUND_DELAY_TICKS);
    }

    private void handleGameComplete() {
        playMatchSound();
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1, 1.2f);
        finished = true;
        player.closeInventory();
        challenge.complete();
    }

    private void handleGameFailed() {
        player.playSound(player, Sound.BLOCK_GLASS_BREAK, 1, 0.5f);
        player.sendMessage(ChatColor.RED + "You lost your third life.");
        finished = true;
        player.closeInventory();
        challenge.fail("No lives remaining in Memory Match");
    }

    private void revealPositions(List<Integer> positions) {
        positions.forEach((Integer position) -> getInventory().setItem(boardSlots.get(position),
                createMatchItem(game.getPairIdentifier(position))));
    }

    private void playMatchSound() {
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1.2f);
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        if (!ownsInventory(event.getInventory()) || finished)
            return;
        if (PowerOreChallenge.Status.RUNNING.equals(challenge.getStatus())) {
            finished = true;
            challenge.fail("Memory Match closed early");
        }
    }

    @Override
    public void handleDrag(InventoryDragEvent event) {
        if (ownsInventory(event.getView().getTopInventory()))
            event.setCancelled(true);
    }

    @Override
    public void cleanup() {
        finished = true;
        scheduledTasks.forEach((BukkitTask scheduledTask) -> scheduledTask.cancel());
        scheduledTasks.clear();
    }

    @Override
    public String name() {
        return "Memory Match";
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    private List<Integer> createBoardSlots(PowerOreMemoryMatchGame.RoundDefinition round) {
        int startingColumn = (INVENTORY_COLUMNS - round.columns()) / 2;
        int startingRow = (INVENTORY_ROWS - round.rows()) / 2;
        return IntStream.range(0, round.tileCount())
                .map((int position) -> ((startingRow + (position / round.columns())) * INVENTORY_COLUMNS)
                        + startingColumn + (position % round.columns()))
                .boxed()
                .toList();
    }

    private ItemStack createMatchItem(int pairIdentifier) {
        ItemStack item = new ItemStack(MATCH_MATERIALS.get(pairIdentifier));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Match " + (pairIdentifier + 1));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createHiddenItem() {
        ItemStack item = new ItemStack(Material.STONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(HIDDEN_MATCH_NAME);
            meta.setLore(List.of(ChatColor.DARK_GRAY + "Click to reveal"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void schedule(Runnable action, long delayTicks) {
        BukkitTask scheduledTask = Bukkit.getScheduler().runTaskLater(SurvivalSkills.getInstance(), action, delayTicks);
        scheduledTasks.add(scheduledTask);
    }

    private boolean isCurrentRoundRunning(int expectedRound) {
        return !finished
                && PowerOreChallenge.Status.RUNNING.equals(challenge.getStatus())
                && game.getRoundIndex() == expectedRound;
    }

    private Inventory getInventory() {
        return inventory.orElseThrow(() -> new IllegalStateException("Memory Match inventory has not been opened"));
    }
}
