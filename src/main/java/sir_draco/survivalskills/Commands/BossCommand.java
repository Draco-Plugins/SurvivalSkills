package sir_draco.survivalskills.Commands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.Bosses.BroodMotherBoss;
import sir_draco.survivalskills.Bosses.DragonBoss;
import sir_draco.survivalskills.Bosses.GiantBoss;
import sir_draco.survivalskills.Bosses.VillagerBoss;
import sir_draco.survivalskills.SurvivalSkills;

public class BossCommand implements CommandExecutor {

    private final SurvivalSkills plugin;
    private GiantBoss giantBoss;
    private BroodMotherBoss broodMotherBoss;
    private VillagerBoss villagerBoss;
    private DragonBoss dragonBoss;

    public BossCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        plugin.getCommand("ssboss").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;

        if (strings.length == 0) {
            p.sendRawMessage(ChatColor.RED + "Correct Usage: " + ChatColor.GRAY + "/ssboss spawn/select/healthpercent/kill/" +
                    "toggleai/attack");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        if (strings[0].equalsIgnoreCase("spawn")) {
            if (strings.length == 1) {
                p.sendRawMessage(ChatColor.RED + "Correct Usage: " + ChatColor.GRAY + "/ssboss spawn <boss>");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }

            if (strings[1].equalsIgnoreCase("giant")) {
                if (giantBoss != null) {
                    p.sendRawMessage(ChatColor.RED + "Giant Boss already spawned!");
                    p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                    return true;
                }

                despawnOthers("giant");
                giantBoss = new GiantBoss(p.getLocation());
                giantBoss.runTaskTimer(plugin, 0, 1);
                if (!giantBoss.isSpawnSuccess()) {
                    p.sendRawMessage(ChatColor.RED + "Giant Boss failed to spawn!");
                    p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                    giantBoss = null;
                    return true;
                }
                p.sendRawMessage(ChatColor.GREEN + "Giant Boss spawned!");
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                plugin.getFightingListener().addBoss(p, giantBoss);
                return true;
            }

            if (strings[1].equalsIgnoreCase("broodmother")) {
                if (broodMotherBoss != null) {
                    p.sendRawMessage(ChatColor.RED + "BroodMother Boss already spawned!");
                    p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                    return true;
                }

                despawnOthers("broodmother");
                broodMotherBoss = new BroodMotherBoss(p.getLocation());
                broodMotherBoss.runTaskTimer(plugin, 0, 1);
                if (!broodMotherBoss.isSpawnSuccess()) {
                    p.sendRawMessage(ChatColor.RED + "BroodMother Boss failed to spawn!");
                    p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                    broodMotherBoss = null;
                    return true;
                }
                p.sendRawMessage(ChatColor.GREEN + "BroodMother Boss spawned!");
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                plugin.getFightingListener().addBoss(p, broodMotherBoss);
                return true;
            }

            if (strings[1].equalsIgnoreCase("villager")) {
                if (villagerBoss != null) {
                    p.sendRawMessage(ChatColor.RED + "Villager Boss already spawned!");
                    p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                    return true;
                }

                despawnOthers("villager");
                villagerBoss = new VillagerBoss(p.getLocation(), p);
                villagerBoss.runTaskTimer(plugin, 0, 1);
                if (!villagerBoss.isSpawnSuccess()) {
                    p.sendRawMessage(ChatColor.RED + "Villager Boss failed to spawn!");
                    p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                    villagerBoss = null;
                    return true;
                }
                p.sendRawMessage(ChatColor.GREEN + "Villager Boss spawned!");
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                plugin.getFightingListener().addBoss(p, villagerBoss);
                return true;
            }

            if (strings[1].equalsIgnoreCase("fishingboss")) {
                plugin.getFishingListener().spawnFishingBoss(p.getWorld(), p.getLocation(), new Vector(0, 0, 0));
                p.sendRawMessage(ChatColor.GREEN + "Fishing Boss spawned!");
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                return true;
            }

            p.sendRawMessage(ChatColor.RED + "Correct Usage: " + ChatColor.GRAY + "/ssboss spawn <giant/broodmother/villager/fishingboss>");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            return true;
        }

        if (strings[0].equalsIgnoreCase("select")) {
            for (Entity ent : p.getNearbyEntities(25, 25, 25)) {
                if (!ent.hasMetadata("boss")) continue;
                checkRegisteredBosses(ent, p);
            }
            return true;
        }

        if (strings[0].equalsIgnoreCase("healthpercent")) {
            if (strings.length == 1) {
                p.sendRawMessage(ChatColor.RED + "Correct Usage: " + ChatColor.GRAY + "/ssboss healthpercent <percent>");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }
            double healthPercentage;
            try {
                healthPercentage = Double.parseDouble(strings[1]);
            } catch (NumberFormatException e) {
                healthPercentage = 0.5;
            }

            if (giantBoss != null) {
                giantBoss.setHealthPercentage(healthPercentage);
                p.sendRawMessage(ChatColor.GREEN + "Giant Boss health percentage set to " + healthPercentage);
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                return true;
            }
            else if (broodMotherBoss != null) {
                broodMotherBoss.setHealthPercentage(healthPercentage);
                p.sendRawMessage(ChatColor.GREEN + "BroodMother Boss health percentage set to " + healthPercentage);
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                return true;
            }
            else if (dragonBoss != null) {
                dragonBoss.setHealthPercentage(healthPercentage);
                p.sendRawMessage(ChatColor.GREEN + "Dragon Boss health percentage set to " + healthPercentage);
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                return true;
            }
            else {
                villagerBoss.setHealthPercentage(healthPercentage);
                p.sendRawMessage(ChatColor.GREEN + "Villager Boss health percentage set to " + healthPercentage);
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                return true;
            }
        }

        if (strings[0].equalsIgnoreCase("kill")) {
            if (giantBoss != null) {
                if (giantBoss.getBoss().isDead()) {
                    p.sendRawMessage(ChatColor.RED + "Giant Boss died unnaturally!");
                    p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                    giantBoss = null;
                    return true;
                }

                giantBoss.death();
                giantBoss = null;
                p.sendRawMessage(ChatColor.GREEN + "Giant Boss killed!");
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                return true;
            }
            else if (broodMotherBoss != null) {
                if (broodMotherBoss.getBoss().isDead()) {
                    p.sendRawMessage(ChatColor.RED + "BroodMother Boss died unnaturally!");
                    p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                    broodMotherBoss = null;
                    return true;
                }

                broodMotherBoss.death();
                broodMotherBoss = null;
                p.sendRawMessage(ChatColor.GREEN + "BroodMother Boss killed!");
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                return true;
            }
            else if (dragonBoss != null) {
                if (dragonBoss.getBoss().isDead()) {
                    p.sendRawMessage(ChatColor.RED + "Dragon Boss died unnaturally!");
                    p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                    dragonBoss = null;
                    return true;
                }

                dragonBoss.setHealth(1);
                dragonBoss = null;
                p.sendRawMessage(ChatColor.GREEN + "Dragon Boss health at 1!");
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                return true;
            }
            else if (villagerBoss != null) {
                if (villagerBoss.getBoss().isDead()) {
                    p.sendRawMessage(ChatColor.RED + "Villager Boss died unnaturally!");
                    p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                    villagerBoss = null;
                    return true;
                }

                villagerBoss.death();
                villagerBoss = null;
                p.sendRawMessage(ChatColor.GREEN + "Villager Boss killed!");
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                return true;
            }

            p.sendRawMessage(ChatColor.RED + "No boss found!");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        }

        if (strings[0].equalsIgnoreCase("toggleai")) {
            if (giantBoss != null) {
                giantBoss.toggleAI();
                p.sendRawMessage(ChatColor.GREEN + "Giant Boss AI toggled!");
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                return true;
            }
            else if (broodMotherBoss != null) {
                broodMotherBoss.toggleAI();
                p.sendRawMessage(ChatColor.GREEN + "BroodMother Boss AI toggled!");
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                return true;
            }
            else if (dragonBoss != null) {
                dragonBoss.toggleAI();
                p.sendRawMessage(ChatColor.GREEN + "Dragon Boss AI toggled!");
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                return true;
            }
            else {
                villagerBoss.toggleAI();
                p.sendRawMessage(ChatColor.GREEN + "Villager Boss AI toggled!");
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                return true;
            }
        }

        if (strings[0].equalsIgnoreCase("attack")) {
            if (giantBoss != null) {
                giantBoss.attack();
                p.sendRawMessage(ChatColor.GREEN + "Giant Boss attacked!");
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                return true;
            }
            else if (broodMotherBoss != null) {
                broodMotherBoss.attack();
                p.sendRawMessage(ChatColor.GREEN + "BroodMother Boss attacked!");
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                return true;
            }
            else if (dragonBoss != null) {
                dragonBoss.attack();
                p.sendRawMessage(ChatColor.GREEN + "Dragon Boss attacked!");
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                return true;
            }
            else {
                villagerBoss.attack();
                p.sendRawMessage(ChatColor.GREEN + "Villager Boss attacked!");
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                return true;
            }
        }
        return true;
    }

    public void despawnOthers(String boss) {
        if (boss.equalsIgnoreCase("giant")) {
            if (broodMotherBoss != null) plugin.getFightingListener().removeBroodMother(broodMotherBoss.getBoss());
            if (villagerBoss != null) plugin.getFightingListener().removeVillager(villagerBoss.getBoss());
        }
        else if (boss.equalsIgnoreCase("broodmother")) {
            if (giantBoss != null) plugin.getFightingListener().removeGiant(giantBoss.getBoss());
            if (villagerBoss != null) plugin.getFightingListener().removeVillager(villagerBoss.getBoss());
        }
        else if (boss.equalsIgnoreCase("villager")) {
            if (giantBoss != null) plugin.getFightingListener().removeGiant(giantBoss.getBoss());
            if (broodMotherBoss != null) plugin.getFightingListener().removeBroodMother(broodMotherBoss.getBoss());
        }
    }

    public void checkRegisteredBosses(Entity ent, Player p) {
        LivingEntity boss = (LivingEntity) ent;
        for (GiantBoss giant : plugin.getFightingListener().getGiants()) {
            if (giant.getBoss().equals(boss)) {
                giantBoss = giant;
                despawnOthers("giant");
                p.sendRawMessage(ChatColor.GREEN + "Giant Boss selected!");
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                return;
            }
        }
        for (BroodMotherBoss broodMother : plugin.getFightingListener().getBroodMothers()) {
            if (broodMother.getBoss().equals(boss)) {
                broodMotherBoss = broodMother;
                despawnOthers("broodmother");
                p.sendRawMessage(ChatColor.GREEN + "BroodMother Boss selected!");
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                return;
            }
        }
        for (VillagerBoss villager : plugin.getFightingListener().getVillagerBosses()) {
            if (villager.getBoss().equals(boss)) {
                villagerBoss = villager;
                despawnOthers("villager");
                p.sendRawMessage(ChatColor.GREEN + "Villager Boss selected!");
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                return;
            }
        }
        if (plugin.getFightingListener().getDragonBoss() != null) {
            dragonBoss = plugin.getFightingListener().getDragonBoss();
            p.sendRawMessage(ChatColor.GREEN + "Dragon Boss selected!");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            return;
        }

        p.sendRawMessage(ChatColor.RED + "No boss found!");
        p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    }
}
