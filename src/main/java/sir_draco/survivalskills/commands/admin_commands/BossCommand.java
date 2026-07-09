package sir_draco.survivalskills.commands.admin_commands;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.bosses.Boss;
import sir_draco.survivalskills.bosses.BroodMotherBoss;
import sir_draco.survivalskills.bosses.DragonBoss;
import sir_draco.survivalskills.bosses.GiantBoss;
import sir_draco.survivalskills.bosses.VillagerBoss;
import sir_draco.survivalskills.SurvivalSkills;

public class BossCommand implements CommandExecutor {

    private static final String USAGE_PREFIX = ChatColor.RED + "Correct Usage: " + ChatColor.GRAY;
    private static final Sound SUCCESS_SOUND = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
    private static final Sound ERROR_SOUND = Sound.ENTITY_ENDERMAN_TELEPORT;

    private final SurvivalSkills plugin;
    private Boss activeBoss;
    private DragonBoss dragonBoss;

    public BossCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("ssboss");
        if (command != null)
            command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player p))
            return false;
        if (strings.length == 0) {
            sendUsage(p, "/ssboss spawn/select/healthpercent/kill/toggleai/attack");
            return true;
        }

        switch (strings[0].toLowerCase()) {
            case "spawn" -> handleSpawn(p, strings);
            case "select" -> handleSelect(p);
            case "healthpercent" -> handleHealthPercent(p, strings);
            case "remove" -> handleRemove(p);
            case "kill" -> handleKill(p);
            case "toggleai" -> handleToggleAI(p);
            case "attack" -> handleAttack(p);
        }
        return true;
    }

    private void sendSuccess(Player p, String message) {
        p.sendRawMessage(ChatColor.GREEN + message);
        p.playSound(p, SUCCESS_SOUND, 1, 1);
    }

    private void sendError(Player p, String message) {
        p.sendRawMessage(ChatColor.RED + message);
        p.playSound(p, ERROR_SOUND, 1, 1);
    }

    private void sendUsage(Player p, String usage) {
        p.sendRawMessage(USAGE_PREFIX + usage);
        p.playSound(p, ERROR_SOUND, 1, 1);
    }

    private static String bossTypeName(Boss boss) {
        if (boss instanceof DragonBoss)
            return "Dragon";
        if (boss instanceof GiantBoss)
            return "Giant";
        if (boss instanceof BroodMotherBoss)
            return "BroodMother";
        if (boss instanceof VillagerBoss)
            return "Villager";
        return "Unknown";
    }

    private void handleSpawn(Player p, String[] strings) {
        if (strings.length == 1) {
            sendUsage(p, "/ssboss spawn <boss>");
            return;
        }

        switch (strings[1].toLowerCase()) {
            case "giant" -> spawnGiant(p);
            case "broodmother" -> spawnBroodMother(p);
            case "villager" -> spawnVillager(p);
            case "fishingboss" -> spawnFishingBoss(p);
            default -> sendUsage(p, "/ssboss spawn <giant/broodmother/villager/fishingboss>");
        }
    }

    private void spawnGiant(Player p) {
        if (activeBoss instanceof GiantBoss) {
            sendError(p, "Giant Boss already spawned!");
            return;
        }
        despawnCurrentBoss();
        GiantBoss boss = GiantBoss.create(p.getLocation());
        if (boss == null) {
            sendError(p, "Giant Boss failed to spawn!");
            return;
        }
        activeBoss = boss;
        boss.runTaskTimer(plugin, 0, 1);
        sendSuccess(p, "Giant Boss spawned!");
        plugin.getFightingListener().addBoss(p, boss);
    }

    private void spawnBroodMother(Player p) {
        if (activeBoss instanceof BroodMotherBoss) {
            sendError(p, "BroodMother Boss already spawned!");
            return;
        }
        despawnCurrentBoss();
        BroodMotherBoss boss = BroodMotherBoss.create(p.getLocation());
        if (boss == null) {
            sendError(p, "BroodMother Boss failed to spawn!");
            return;
        }
        activeBoss = boss;
        boss.runTaskTimer(plugin, 0, 1);
        sendSuccess(p, "BroodMother Boss spawned!");
        plugin.getFightingListener().addBoss(p, boss);
    }

    private void spawnVillager(Player p) {
        if (activeBoss instanceof VillagerBoss) {
            sendError(p, "Villager Boss already spawned!");
            return;
        }
        despawnCurrentBoss();
        VillagerBoss boss = VillagerBoss.create(p.getLocation(), p,
                plugin.getFightingListener().getNoBossMusic().contains(p));
        if (boss == null) {
            sendError(p, "Villager Boss failed to spawn!");
            return;
        }
        activeBoss = boss;
        boss.runTaskTimer(plugin, 0, 1);
        sendSuccess(p, "Villager Boss spawned!");
        plugin.getFightingListener().addBoss(p, boss);
    }

    private void spawnFishingBoss(Player p) {
        plugin.getFishingListener().spawnFishingBoss(p.getWorld(), p.getLocation(), new Vector(0, 0, 0));
        sendSuccess(p, "Fishing Boss spawned!");
    }

    private void handleSelect(Player p) {
        for (Entity ent : p.getNearbyEntities(25, 25, 25)) {
            if (!ent.hasMetadata("boss"))
                continue;
            selectBossFromEntity(ent, p);
        }
    }

    private void selectBossFromEntity(Entity ent, Player p) {
        LivingEntity entity = (LivingEntity) ent;

        for (GiantBoss giant : plugin.getFightingListener().getGiants()) {
            if (giant.getBoss().equals(entity)) {
                if (activeBoss != giant) {
                    despawnCurrentBoss();
                    activeBoss = giant;
                }
                sendSuccess(p, "Giant Boss selected!");
                return;
            }
        }
        for (BroodMotherBoss broodMother : plugin.getFightingListener().getBroodMothers()) {
            if (broodMother.getBoss().equals(entity)) {
                if (activeBoss != broodMother) {
                    despawnCurrentBoss();
                    activeBoss = broodMother;
                }
                sendSuccess(p, "BroodMother Boss selected!");
                return;
            }
        }
        for (VillagerBoss villager : plugin.getFightingListener().getVillagerBosses()) {
            if (villager.getBoss().equals(entity)) {
                if (activeBoss != villager) {
                    despawnCurrentBoss();
                    activeBoss = villager;
                }
                sendSuccess(p, "Villager Boss selected!");
                return;
            }
        }
        DragonBoss found = plugin.getFightingListener().getDragonBoss();
        if (found != null && found.getBoss().equals(entity)) {
            dragonBoss = found;
            sendSuccess(p, "Dragon Boss selected!");
            return;
        }
        sendError(p, "No boss found!");
    }

    private void handleHealthPercent(Player p, String[] strings) {
        if (strings.length == 1) {
            sendUsage(p, "/ssboss healthpercent <percent>");
            return;
        }

        double healthPercentage;
        try {
            healthPercentage = Double.parseDouble(strings[1]);
        } catch (NumberFormatException e) {
            healthPercentage = 0.5;
        }

        Boss target = activeBoss != null ? activeBoss : dragonBoss;
        if (target == null) {
            sendError(p, "No boss found!");
            return;
        }

        target.setHealthPercentage(healthPercentage);
        sendSuccess(p, bossTypeName(target) + " Boss health percentage set to " + healthPercentage);
    }

    private void handleRemove(Player p) {
        if (activeBoss == null) {
            sendError(p, "No boss found!");
            return;
        }
        String name = bossTypeName(activeBoss);
        if (activeBoss.getBoss().isDead()) {
            sendError(p, name + " Boss died unnaturally!");
            activeBoss = null;
            return;
        }
        activeBoss.cleanup();
        activeBoss = null;
        sendSuccess(p, name + " Boss Removed!");
    }

    private void handleKill(Player p) {
        if (activeBoss != null) {
            String name = bossTypeName(activeBoss);
            if (activeBoss.getBoss().isDead()) {
                sendError(p, name + " Boss died unnaturally!");
                activeBoss = null;
                return;
            }
            activeBoss.death();
            activeBoss = null;
            sendSuccess(p, name + " Boss killed!");
            return;
        }

        if (dragonBoss != null) {
            if (dragonBoss.getBoss().isDead()) {
                sendError(p, "Dragon Boss died unnaturally!");
                dragonBoss = null;
                return;
            }
            // DragonBoss uses setHealth(1) instead of death() for "kill"
            dragonBoss.setHealth(1);
            dragonBoss = null;
            sendSuccess(p, "Dragon Boss health at 1!");
            return;
        }

        sendError(p, "No boss found!");
    }

    private void handleToggleAI(Player p) {
        Boss target = activeBoss != null ? activeBoss : dragonBoss;
        if (target == null) {
            sendError(p, "No boss found!");
            return;
        }
        target.toggleAI();
        sendSuccess(p, bossTypeName(target) + " Boss AI toggled!");
    }

    private void handleAttack(Player p) {
        Boss target = activeBoss != null ? activeBoss : dragonBoss;
        if (target == null) {
            sendError(p, "No boss found!");
            return;
        }
        target.attack();
        sendSuccess(p, bossTypeName(target) + " Boss attacked!");
    }

    private void despawnCurrentBoss() {
        if (activeBoss == null)
            return;
        plugin.getFightingListener().removeBoss(activeBoss);
        activeBoss.death();
        activeBoss = null;
    }
}
