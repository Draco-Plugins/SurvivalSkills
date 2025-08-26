package sir_draco.survivalskills.god_questline.powerore;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.*;

/** Simon Says memory sequence task. */
public class PowerOreSimonSaysTask implements PowerOreTask {

    private final PowerOreChallenge challenge;
    private final Player player;
    private final int[] rounds = { 4, 6, 8, 10 };
    private final List<Material> colorMaterials = List.of(
            Material.RED_STAINED_GLASS_PANE,
            Material.BLUE_STAINED_GLASS_PANE,
            Material.GREEN_STAINED_GLASS_PANE,
            Material.YELLOW_STAINED_GLASS_PANE);
    private final Random random = new Random();
    private int roundIndex = 0;
    private int startDelay = 20;
    private List<Integer> currentSequence = new ArrayList<>();
    private int inputIndex = 0;
    private Inventory inventory;
    private boolean showing = false;

    public PowerOreSimonSaysTask(PowerOreChallenge challenge, Player player, Location oreLoc) {
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
        inputIndex = 0;
        currentSequence.clear();
        int length = rounds[roundIndex];
        for (int i = 0; i < length; i++)
            currentSequence.add(random.nextInt(colorMaterials.size()));
        player.sendMessage(ChatColor.AQUA + "Round " + (roundIndex + 1) + ": Memorize the sequence!");
        showSequence();
    }

    private void showSequence() {
        showing = true;
        new BukkitRunnable() {
            int idx = 0;
            int phaseTicks = 0;

            @Override
            public void run() {
                if (challenge.getStatus() != PowerOreChallenge.Status.RUNNING) {
                    cancel();
                    return;
                }

                // Make sure the start is delayed
                if (startDelay-- > 0)
                    return;

                if (idx >= currentSequence.size()) {
                    showing = false;
                    fillColorButtons();
                    player.sendMessage(ChatColor.GREEN + "Repeat the sequence!");
                    cancel();
                    return;
                }
                if (phaseTicks == 0) {
                    int colorIndex = currentSequence.get(idx);
                    ItemStack item = coloredItem(colorMaterials.get(colorIndex), ChatColor.YELLOW + "?", "");
                    inventory.setItem(4, item);
                    player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1 + (colorIndex * 0.1f));
                }
                phaseTicks++;
                if (phaseTicks >= 10)
                    inventory.setItem(4, null);
                if (phaseTicks >= 13) {
                    phaseTicks = 0;
                    idx++;
                }
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), 0, 2);
    }

    private void fillColorButtons() {
        for (int i = 0; i < colorMaterials.size(); i++) {
            inventory.setItem((2 * i) + 1, coloredItem(colorMaterials.get(i), ChatColor.WHITE + "Color " + (i + 1),
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

    public void handleClick(InventoryClickEvent e) {
        e.setCancelled(true);
        if (showing)
            return;

        if (challenge.getStatus() != PowerOreChallenge.Status.RUNNING)
            return;

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize())
            return;
        if (slot % 2 == 0)
            return;

        int colorIndex = (slot - 1) / 2;
        if (currentSequence.get(inputIndex) != colorIndex) {
            challenge.fail("Wrong sequence");
            player.closeInventory();
            return;
        }

        inputIndex++;
        player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1, 1 + (colorIndex * 0.1f));

        if (inputIndex < currentSequence.size())
            return;

        roundIndex++;
        if (roundIndex >= rounds.length) {
            player.closeInventory();
            challenge.complete();
            return;
        }

        // Go to next round
        new BukkitRunnable() {
            @Override
            public void run() {
                inventory.clear();
                startRound();
            }
        }.runTaskLater(SurvivalSkills.getInstance(), 40);

    }

    public void handleClose() {
        if (challenge.getStatus() == PowerOreChallenge.Status.RUNNING)
            challenge.fail("Simon Says closed early");
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
