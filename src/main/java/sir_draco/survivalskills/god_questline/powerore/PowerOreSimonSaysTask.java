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
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.*;

/** Simon Says memory sequence task. */
public class PowerOreSimonSaysTask implements PowerOreInventoryTask {

    private static final int[] SEQUENCE_LENGTHS = { 4, 6, 8, 10 };
    private static final int TICKS_BEFORE_SEQUENCE = 40;
    private static final int TICKS_PER_HIGHLIGHT = 10;
    private static final int TICKS_BETWEEN_HIGHLIGHTS = 3;
    private static final int TICK_INTERVAL = 2;
    private static final int CENTER_SLOT = 4;
    private static final float SOUND_PITCH_INCREMENT = 0.1f;
    private static final float ROUND_COMPLETION_SOUND_VOLUME = 1.0f;
    private static final float ROUND_COMPLETION_SOUND_PITCH = 1.0f;
    private static final int NEXT_ROUND_DELAY_TICKS = 40;

    private enum DisplayStep {
        HIGHLIGHT(TICKS_PER_HIGHLIGHT),
        GAP(TICKS_BETWEEN_HIGHLIGHTS);

        private final int duration;

        DisplayStep(int duration) {
            this.duration = duration;
        }

        public int getDuration() {
            return duration;
        }
    }

    private final PowerOreChallenge challenge;
    private final Player player;
    private final List<Material> colorMaterials = List.of(
            Material.RED_STAINED_GLASS_PANE,
            Material.BLUE_STAINED_GLASS_PANE,
            Material.GREEN_STAINED_GLASS_PANE,
            Material.YELLOW_STAINED_GLASS_PANE);
    private final Random random = new Random();

    private int roundIndex = 0;
    private int inputIndex = 0;
    private final List<Integer> currentSequence = new ArrayList<>();
    private Inventory inventory;
    private boolean acceptingInput = false;
    private boolean finished = false;

    public PowerOreSimonSaysTask(PowerOreChallenge challenge, Player player) {
        this.challenge = challenge;
        this.player = player;
    }

    @Override
    public void start() {
        inventory = Bukkit.createInventory(null, 9, ChatColor.DARK_PURPLE + "Simon Says");
        player.openInventory(inventory);
        startRound();
    }

    private void startRound() {
        acceptingInput = false;
        inputIndex = 0;
        currentSequence.clear();
        int length = SEQUENCE_LENGTHS[roundIndex];
        for (int i = 0; i < length; i++)
            currentSequence.add(random.nextInt(colorMaterials.size()));
        player.sendMessage(ChatColor.AQUA + "Round " + (roundIndex + 1) + ": Memorize the sequence!");
        showSequence();
    }

    private void showSequence() {
        acceptingInput = false;
        new BukkitRunnable() {
            int idx = 0;
            int phaseTicks = 0;
            DisplayStep currentStep = DisplayStep.HIGHLIGHT;

            @Override
            public void run() {
                if (challenge.getStatus() != PowerOreChallenge.Status.RUNNING) {
                    cancel();
                    return;
                }

                if (idx >= currentSequence.size()) {
                    fillColorButtons();
                    acceptingInput = true;
                    player.sendMessage(ChatColor.GREEN + "Repeat the sequence!");
                    cancel();
                    return;
                }

                phaseTicks++;
                if (currentStep == DisplayStep.HIGHLIGHT) {
                    if (phaseTicks == 1) {
                        int colorIndex = currentSequence.get(idx);
                        ItemStack item = coloredItem(colorMaterials.get(colorIndex), ChatColor.YELLOW + "?", "");
                        inventory.setItem(CENTER_SLOT, item);
                        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1,
                                1 + (colorIndex * SOUND_PITCH_INCREMENT));
                    }
                    if (phaseTicks >= currentStep.getDuration()) {
                        inventory.setItem(CENTER_SLOT, null);
                        currentStep = DisplayStep.GAP;
                        phaseTicks = 0;
                    }
                } else if (currentStep == DisplayStep.GAP) {
                    if (phaseTicks >= currentStep.getDuration()) {
                        currentStep = DisplayStep.HIGHLIGHT;
                        phaseTicks = 0;
                        idx++;
                    }
                }
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), TICKS_BEFORE_SEQUENCE, TICK_INTERVAL);
    }

    private void fillColorButtons() {
        for (int i = 0; i < colorMaterials.size(); i++) {
            inventory.setItem((2 * i) + 1,
                    coloredItem(colorMaterials.get(i), ChatColor.WHITE + "Color " + (i + 1),
                            ChatColor.GRAY + "Click in order"));
        }
    }

    private ItemStack coloredItem(Material mat, String name, String loreLine) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (loreLine != null && !loreLine.isBlank())
                meta.setLore(List.of(loreLine));
            item.setItemMeta(meta);
        }
        return item;
    }

    private Optional<Integer> getClickedColorIndex(InventoryClickEvent e) {
        if (!acceptingInput)
            return Optional.empty();
        if (challenge.getStatus() != PowerOreChallenge.Status.RUNNING)
            return Optional.empty();
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize())
            return Optional.empty();
        if (slot % 2 == 0)
            return Optional.empty();
        return Optional.of((slot - 1) / 2);
    }

    @Override
    public boolean ownsInventory(Inventory candidateInventory) {
        return inventory != null && inventory.equals(candidateInventory);
    }

    @Override
    public void handleClick(InventoryClickEvent e) {
        if (!ownsInventory(e.getView().getTopInventory()))
            return;
        e.setCancelled(true);
        Optional<Integer> colorIndexOpt = getClickedColorIndex(e);
        if (colorIndexOpt.isEmpty())
            return;

        int colorIndex = colorIndexOpt.get();
        if (inputIndex >= currentSequence.size())
            return;
        if (currentSequence.get(inputIndex) != colorIndex) {
            finished = true;
            challenge.fail("Wrong sequence");
            player.closeInventory();
            return;
        }

        inputIndex++;
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1,
                1 + (colorIndex * SOUND_PITCH_INCREMENT));

        if (inputIndex < currentSequence.size())
            return;

        advanceRound();
    }

    private void advanceRound() {
        acceptingInput = false;
        player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, ROUND_COMPLETION_SOUND_VOLUME,
                ROUND_COMPLETION_SOUND_PITCH);
        roundIndex++;
        if (roundIndex >= SEQUENCE_LENGTHS.length) {
            finished = true;
            player.closeInventory();
            challenge.complete();
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                inventory.clear();
                startRound();
            }
        }.runTaskLater(SurvivalSkills.getInstance(), NEXT_ROUND_DELAY_TICKS);
    }

    @Override
    public void handleClose(InventoryCloseEvent event) {
        if (!ownsInventory(event.getInventory()))
            return;
        if (finished)
            return;
        if (challenge.getStatus() == PowerOreChallenge.Status.RUNNING) {
            finished = true;
            challenge.fail("Simon Says closed early");
        }
    }

    @Override
    public void handleDrag(InventoryDragEvent event) {
        if (ownsInventory(event.getView().getTopInventory()))
            event.setCancelled(true);
    }

    @Override
    public void cleanup() {
    }

    @Override
    public String name() {
        return "Simon Says";
    }

    @Override
    public Player getPlayer() {
        return player;
    }
}
