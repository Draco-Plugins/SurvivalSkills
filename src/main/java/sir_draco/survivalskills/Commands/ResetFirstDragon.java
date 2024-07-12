package sir_draco.survivalskills.Commands;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.boss.DragonBattle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;

public class ResetFirstDragon implements CommandExecutor {

    private final SurvivalSkills plugin;

    public ResetFirstDragon(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("resetfirstdragon");
        if (command != null) command.setExecutor(this);
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;
        World world = p.getWorld();
        if (!world.getEnvironment().equals(World.Environment.THE_END)) {
            p.sendRawMessage(ChatColor.RED + "You must be in the end to reset the dragon!");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        if (world.hasMetadata("killedfirstdragon")) {
            world.removeMetadata("killedfirstdragon", SurvivalSkills.getPlugin(SurvivalSkills.class));
            for (Entity entity : world.getEntities()) if (entity instanceof EnderDragon) entity.remove();
            DragonBattle battle = world.getEnderDragonBattle();
            if (battle != null) {
                Location loc = battle.getEndPortalLocation();
                if (loc == null) {
                    p.sendRawMessage(ChatColor.RED + "The end portal has not been found!");
                    p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                    return true;
                }
                respawnConditions(loc);

                battle.setPreviouslyKilled(true);
                battle.initiateRespawn();
                battle.setPreviouslyKilled(false);
            }

            p.sendRawMessage(ChatColor.GREEN + "Dragon has been reset!");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            return true;
        }
        else {
            boolean found = false;
            for (Entity entity : world.getEntities()) {
                if (entity instanceof EnderDragon) {
                    found = true;
                    break;
                }
            }

            if (found) {
                p.sendRawMessage(ChatColor.RED + "The dragon has not been killed yet!");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            }
            else {
                DragonBattle battle = world.getEnderDragonBattle();
                if (battle != null) {
                    Location loc = battle.getEndPortalLocation();
                    if (loc == null) {
                        p.sendRawMessage(ChatColor.RED + "The end portal has not been found!");
                        p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                        return true;
                    }
                    respawnConditions(loc);

                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            battle.setPreviouslyKilled(true);
                            battle.initiateRespawn();
                            battle.setPreviouslyKilled(false);
                        }
                    }.runTaskLater(plugin, 2);
                }

                p.sendRawMessage(ChatColor.GREEN + "Dragon has been reset!");
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            }
        }
        return true;
    }

    public void respawnConditions(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;

        EnderCrystal crystal = world.spawn(loc.clone().add(3.5, 0.5, 0.5), EnderCrystal.class);
        crystal.setShowingBottom(false);
        EnderCrystal crystal2 = world.spawn(loc.clone().add(-2.5, 0.5, 0.5), EnderCrystal.class);
        crystal2.setShowingBottom(false);
        EnderCrystal crystal3 = world.spawn(loc.clone().add(0.5, 0.5, 3.5), EnderCrystal.class);
        crystal3.setShowingBottom(false);
        EnderCrystal crystal4 = world.spawn(loc.clone().add(0.5, 0.5, -2.5), EnderCrystal.class);
        crystal4.setShowingBottom(false);

        Block egg = loc.clone().add(0, 4, 0).getBlock();
        egg.setType(Material.DRAGON_EGG);
        egg.getState().update();
    }
}
