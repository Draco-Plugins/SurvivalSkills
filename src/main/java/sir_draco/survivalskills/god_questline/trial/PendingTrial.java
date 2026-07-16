package sir_draco.survivalskills.god_questline.trial;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PendingTrial {

    private final Player trialMaster;
    private final ArrayList<Player> players = new ArrayList<>();
    private final ArrayList<Player> blockedPlayers = new ArrayList<>();

    private boolean solo = true;
    private boolean chosenDifficulty = false;
    private int trialDifficulty = 1;
    private Inventory playerManager = null;
    private boolean success = false;

    public PendingTrial(Player trialMaster) {
        this.trialMaster = trialMaster;
        players.add(trialMaster);
    }

    public void handleNewPartyMember(Player p) {
        players.add(p);
        for (Player player : players) {
            player.sendRawMessage(ChatColor.GREEN + p.getName() + " has joined the party.");
            player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BANJO, 1, 1);
        }
        updatePlayerManager();
    }

    public void handleBlockPartyMember(Player p) {
        players.remove(p);
        blockedPlayers.add(p);
        updatePlayerManager();
    }

    public void handleOfflinePlayerRemoval(Player notifier, String offlinePlayerName) {
        players.removeIf(player -> player.getName().equals(offlinePlayerName));
        notifier.sendRawMessage(ChatColor.RED + offlinePlayerName + " is no longer online and has been removed from the party");
        notifier.playSound(notifier, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        updatePlayerManager();
    }

    public void updatePlayerManager() {
        if (playerManager == null) playerManager = Bukkit.createInventory(null, 9,
                ChatColor.DARK_PURPLE + "Party Manager");
        playerManager.clear();

        for (int i = 0; i < players.size(); i++) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = head.getItemMeta();
            if (meta == null) continue;
            meta.setDisplayName(ChatColor.GREEN + players.get(i).getName());
            if (meta instanceof SkullMeta skullMeta) {
                skullMeta.setOwningPlayer(players.get(i));
            }
            head.setItemMeta(meta);
            playerManager.setItem(i, head);
        }

        ItemStack confirm = new ItemStack(Material.LIME_DYE);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName(ChatColor.GREEN + "Confirm Party");
            confirm.setItemMeta(confirmMeta);
            playerManager.setItem(8, confirm);
        }
        if (!TrialManager.isTrialSelectionInventory(playerManager)) {
            TrialManager.registerTrialSelectionInventory(playerManager);
        }

        trialMaster.openInventory(playerManager);
    }

    public void endPendingTrial() {
        if (success) return;
        for (Player p : players) {
            if (p.equals(trialMaster)) continue;
            p.sendRawMessage(ChatColor.RED + "The party has been disbanded");
            p.playSound(p, Sound.BLOCK_NOTE_BLOCK_HARP, 1, 0.5f);
        }
    }

    public void setSolo(boolean solo) {
        this.solo = solo;
    }

    public void setTrialDifficulty(int trialDifficulty) {
        this.trialDifficulty = trialDifficulty;
    }

    public boolean isSolo() {
        return solo;
    }

    public int getTrialDifficulty() {
        return trialDifficulty;
    }

    public List<Player> getPlayers() {
        return Collections.unmodifiableList(players);
    }

    public void clearPlayers() {
        players.clear();
    }

    public void removePlayer(Player player) {
        players.remove(player);
        updatePlayerManager();
    }

    public Player getTrialMaster() {
        return trialMaster;
    }

    public List<Player> getBlockedPlayers() {
        return Collections.unmodifiableList(blockedPlayers);
    }

    public boolean isChosenDifficulty() {
        return chosenDifficulty;
    }

    public void setChosenDifficulty(boolean chosenDifficulty) {
        this.chosenDifficulty = chosenDifficulty;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
