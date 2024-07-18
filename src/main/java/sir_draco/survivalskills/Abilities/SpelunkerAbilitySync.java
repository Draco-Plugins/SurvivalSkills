package sir_draco.survivalskills.Abilities;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.HashMap;
import java.util.Map;

public class SpelunkerAbilitySync extends BukkitRunnable {

    private final SurvivalSkills plugin;
    private final int radius;
    private final Player p;
    private final HashMap<Block, ItemDisplay> glowingBlocks = new HashMap<>();

    private SpelunkerAbilityAsync blockTracker;
    private int counter = 0;

    public SpelunkerAbilitySync(SurvivalSkills plugin, int radius, Player p) {
        this.plugin = plugin;
        this.radius = radius;
        this.p = p;
    }

    @Override
    public void run() {
        // Make sure it is still active
        if (!p.isOnline()) return;
        AbilityTimer timer = plugin.getAbilityManager().getAbility(p, "Spelunker");
        if (timer == null || !timer.isActive()) {
            p.sendRawMessage(ChatColor.YELLOW + "Spelunker has run out of time!");
            p.playSound(p, Sound.BLOCK_ANVIL_FALL, 1, 1);
            plugin.getMiningListener().getSpelunkerTracker().remove(p);
            endThread();
            this.cancel();
            return;
        }

        // Create glowing blocks from previous async
        if (blockTracker != null) {
            if (blockTracker.getClosestLocation() != null && counter == 0) {
                double distance = p.getLocation().distance(blockTracker.getClosestLocation().getLocation());
                counter = (int) distance;
                playSonarSound(blockTracker.getClosestLocation());
            }
            if (!glowingBlocks.isEmpty() && blockTracker.getGlowsToRemove() != null && !blockTracker.getGlowsToRemove().isEmpty())
                for (Block block : blockTracker.getGlowsToRemove()) removeGlow(block);
            if (!blockTracker.getGlowsToAdd().isEmpty()) for (Block block : blockTracker.getGlowsToAdd()) makeGlowBlock(block);
        }
        if (counter != 0) counter--;

        // Send to async event to check each block
        blockTracker = new SpelunkerAbilityAsync(plugin, p.getLocation().getBlock(), radius, glowingBlocks);
        blockTracker.runTaskAsynchronously(plugin);
    }

    public void playSonarSound(Block block) {
        String team = plugin.getMiningListener().getOreTeam(block.getType());
        if (team.equalsIgnoreCase("none")) return;
        if (team.equalsIgnoreCase(ChatColor.GREEN + "uncommon")) p.playSound(block.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 1, 1);
        else if (team.equalsIgnoreCase(ChatColor.BLUE + "rare")) p.playSound(block.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1, 1);
        else p.playSound(block.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);

    }

    public void makeGlowBlock(Block block) {
        String team = plugin.getMiningListener().getOreTeam(block.getType());
        if (team.equalsIgnoreCase("none")) return;
        World world = block.getLocation().getWorld();
        if (world == null) return;
        ItemDisplay item = world.spawn(block.getLocation().add(0.5, 0.5, 0.5), ItemDisplay.class);
        item.setItemStack(new ItemStack(block.getType()));
        item.setShadowStrength(0);
        item.setBrightness(new Display.Brightness(15, 15));
        Transformation transformation = item.getTransformation();
        transformation.getScale().set(1.01);
        item.setTransformation(transformation);
        hideGlowForAll(item);

        // Set color
        if (team.equalsIgnoreCase(ChatColor.GREEN + "uncommon")) addGreenTeam(item.getUniqueId().toString());
        if (team.equalsIgnoreCase(ChatColor.BLUE + "rare")) addBlueTeam(item.getUniqueId().toString());
        item.setGlowing(true);
        glowingBlocks.put(block, item);
    }

    public void hideGlowForAll(ItemDisplay ent) {
        if (Bukkit.getOnlinePlayers().isEmpty()) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (p.equals(player)) continue;
            hideGlowForPlayer(player, ent);
        }
    }

    public void hideGlowForPlayer(Player player, ItemDisplay ent) {
        player.hideEntity(plugin, ent);
    }

    public void hideAllGlowForPlayer(Player player) {
        if (glowingBlocks.isEmpty()) return;
        for (Map.Entry<Block, ItemDisplay> ent : glowingBlocks.entrySet()) hideGlowForPlayer(player, ent.getValue());
    }

    public void endThread() {
        if (glowingBlocks.isEmpty()) return;
        for (Map.Entry<Block, ItemDisplay> item : glowingBlocks.entrySet()) item.getValue().remove();
        this.cancel();
    }

    public void removeGlow(Block block) {
        if (!glowingBlocks.containsKey(block)) return;
        glowingBlocks.get(block).remove();
        glowingBlocks.remove(block);
    }

    public boolean containsBlock(Block block) {
        return glowingBlocks.containsKey(block);
    }

    public void addGreenTeam(String uuid) {
        Team team = p.getScoreboard().getTeam(ChatColor.GREEN + "uncommon");
        if (team == null) {
            Team greenTeam = p.getScoreboard().registerNewTeam(ChatColor.GREEN + "uncommon");
            greenTeam.setColor(ChatColor.GREEN);
            greenTeam.setPrefix(ChatColor.GREEN + "");
            greenTeam.setDisplayName(ChatColor.GREEN + "uncommon");
            greenTeam.addEntry(uuid);
        }
        else team.addEntry(uuid);
    }

    public void addBlueTeam(String uuid) {
        Team team = p.getScoreboard().getTeam(ChatColor.BLUE + "rare");
        if (team == null) {
            Team blueTeam = p.getScoreboard().registerNewTeam(ChatColor.BLUE + "rare");
            blueTeam.setColor(ChatColor.BLUE);
            blueTeam.setPrefix(ChatColor.BLUE + "");
            blueTeam.setDisplayName(ChatColor.BLUE + "rare");
            blueTeam.addEntry(uuid);
        }
        else team.addEntry(uuid);
    }
}
