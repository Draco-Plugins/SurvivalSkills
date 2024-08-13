package sir_draco.survivalskills.Commands.AdminCommands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import sir_draco.survivalskills.Utils.ItemStackGenerator;
import sir_draco.survivalskills.SurvivalSkills;

import static sir_draco.survivalskills.SkillListeners.GodListener.previousPotionBagID;

public class SurvivalSkillsGetCommand implements CommandExecutor {
    public SurvivalSkillsGetCommand(SurvivalSkills plugin) {
        PluginCommand command = plugin.getCommand("ssget");
        if (command != null) command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player)) return false;
        Player p = (Player) sender;
        if (strings.length != 1) {
            p.sendRawMessage(ChatColor.RED + "Usage: /ssget <item>");
            return false;
        }

        if (strings[0].equalsIgnoreCase("miningarmor")) {
            p.getInventory().addItem(ItemStackGenerator.getMiningHelmet());
            p.getInventory().addItem(ItemStackGenerator.getMiningChestplate());
            p.getInventory().addItem(ItemStackGenerator.getMiningLeggings());
            p.getInventory().addItem(ItemStackGenerator.getMiningBoots());
            p.sendRawMessage(ChatColor.GREEN + "You have received: Mining Armor");
            p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            return true;
        }
        else if (strings[0].equalsIgnoreCase("wandererarmor")) {
            p.getInventory().addItem(ItemStackGenerator.getWandererHelmet());
            p.getInventory().addItem(ItemStackGenerator.getWandererChestplate());
            p.getInventory().addItem(ItemStackGenerator.getWandererLeggings());
            p.getInventory().addItem(ItemStackGenerator.getWandererBoots());
            p.sendRawMessage(ChatColor.GREEN + "You have received: Wanderer Armor");
            p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            return true;
        }
        else if (strings[0].equalsIgnoreCase("travelerarmor")) {
            p.getInventory().addItem(ItemStackGenerator.getTravelerHelmet());
            p.getInventory().addItem(ItemStackGenerator.getTravelerChestplate());
            p.getInventory().addItem(ItemStackGenerator.getTravelerLeggings());
            p.getInventory().addItem(ItemStackGenerator.getTravelerBoots());
            p.sendRawMessage(ChatColor.GREEN + "You have received: Traveler Armor");
            p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            return true;
        }
        else if (strings[0].equalsIgnoreCase("gillarmor")) {
            p.getInventory().addItem(ItemStackGenerator.getGillHelmet());
            p.getInventory().addItem(ItemStackGenerator.getGillChestplate());
            p.getInventory().addItem(ItemStackGenerator.getGillLeggings());
            p.getInventory().addItem(ItemStackGenerator.getGillBoots());
            p.sendRawMessage(ChatColor.GREEN + "You have received: Gill Armor");
            p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            return true;
        }
        else if (strings[0].equalsIgnoreCase("adventurerarmor")) {
            p.getInventory().addItem(ItemStackGenerator.getAdventurerHelmet());
            p.getInventory().addItem(ItemStackGenerator.getAdventurerChestplate());
            p.getInventory().addItem(ItemStackGenerator.getAdventurerLeggings());
            p.getInventory().addItem(ItemStackGenerator.getAdventurerBoots());
            p.sendRawMessage(ChatColor.GREEN + "You have received: Adventurer Armor");
            p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            return true;
        }
        else if (strings[0].equalsIgnoreCase("beaconarmor")) {
            p.getInventory().addItem(ItemStackGenerator.getBeaconHelmet());
            p.getInventory().addItem(ItemStackGenerator.getBeaconChestplate());
            p.getInventory().addItem(ItemStackGenerator.getBeaconLeggings());
            p.getInventory().addItem(ItemStackGenerator.getBeaconBoots());
            p.sendRawMessage(ChatColor.GREEN + "You have received: Beacon Armor");
            p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            return true;
        }

        ItemStack item = getCorrectItem(strings[0]);
        if (item == null) {
            p.sendRawMessage(ChatColor.RED + "Invalid item: " + strings[0]);
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            Bukkit.getLogger().warning("Item " + item.getType() + " does not have item meta");
            return false;
        }

        p.getInventory().addItem(item);
        p.sendRawMessage(ChatColor.GREEN + "You have received: " + meta.getDisplayName());
        p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        return true;
    }

    public ItemStack getCorrectItem(String item) {
        switch (item.toLowerCase()) {
            case "unlimitedtorch":
                return ItemStackGenerator.getUnlimitedTorch();
            case "jumpingboots":
                return ItemStackGenerator.getJumpingBoots();
            case "miningboots":
                return ItemStackGenerator.getMiningBoots();
            case "miningleggings":
                return ItemStackGenerator.getMiningLeggings();
            case "miningchestplate":
                return ItemStackGenerator.getMiningChestplate();
            case "mininghelmet":
                return ItemStackGenerator.getMiningHelmet();
            case "beaconhelmet":
                return ItemStackGenerator.getBeaconHelmet();
            case "beaconchestplate":
                return ItemStackGenerator.getBeaconChestplate();
            case "beaconleggings":
                return ItemStackGenerator.getBeaconLeggings();
            case "beaconboots":
                return ItemStackGenerator.getBeaconBoots();
            case "wandererboots":
                return ItemStackGenerator.getWandererBoots();
            case "wandererleggings":
                return ItemStackGenerator.getWandererLeggings();
            case "wandererchestplate":
                return ItemStackGenerator.getWandererChestplate();
            case "wandererhelmet":
                return ItemStackGenerator.getWandererHelmet();
            case "cavefinder":
                return ItemStackGenerator.getCaveFinder();
            case "travelerboots":
                return ItemStackGenerator.getTravelerBoots();
            case "travelerleggings":
                return ItemStackGenerator.getTravelerLeggings();
            case "travelerchestplate":
                return ItemStackGenerator.getTravelerChestplate();
            case "travelerhelmet":
                return ItemStackGenerator.getTravelerHelmet();
            case "gillboots":
                return ItemStackGenerator.getGillBoots();
            case "gillleggings":
                return ItemStackGenerator.getGillLeggings();
            case "gillchestplate":
                return ItemStackGenerator.getGillChestplate();
            case "gillhelmet":
                return ItemStackGenerator.getGillHelmet();
            case "adventurerboots":
                return ItemStackGenerator.getAdventurerBoots();
            case "adventurerleggings":
                return ItemStackGenerator.getAdventurerLeggings();
            case "adventurerchestplate":
                return ItemStackGenerator.getAdventurerChestplate();
            case "adventurerhelmet":
                return ItemStackGenerator.getAdventurerHelmet();
            case "wateringcan":
                return ItemStackGenerator.getWateringCan();
            case "unlimitedbonemeal":
                return ItemStackGenerator.getUnlimitedBoneMeal();
            case "harvester":
                return ItemStackGenerator.getHarvester();
            case "giantsummoner":
                return ItemStackGenerator.getGiantSummoner();
            case "broodmothersummoner":
                return ItemStackGenerator.getBroodMotherSummoner();
            case "exiledsummoner":
                return ItemStackGenerator.getVillagerSummoner();
            case "dragonhead":
                return ItemStackGenerator.getEnderDragonBossItem();
            case "sortofstonepick":
                return ItemStackGenerator.getSortOfStonePick();
            case "fireworkcannon":
                return ItemStackGenerator.getFireworkCannon();
            case "sortwand":
                return ItemStackGenerator.getSortWand();
            case "unlimitedtropicalfishbucket":
                return ItemStackGenerator.getUnlimitedTropicalFishBucket();
            case "unlimitedwaterbucket":
                return ItemStackGenerator.getUnlimitedWaterBucket();
            case "unlimitedlavabucket":
                return ItemStackGenerator.getUnlimitedLavaBucket();
            case "weatherartifact":
                return ItemStackGenerator.getWeatherArtifact();
            case "timeartifact":
                return ItemStackGenerator.getTimeArtifact();
            case "xpvoucher":
                return ItemStackGenerator.getExperienceMultiplierVoucher();
            case "zapwand":
                return ItemStackGenerator.getZapWand();
            case "magnet":
                return ItemStackGenerator.getMagnet();
            case "webshooter":
                return ItemStackGenerator.getWebShooter();
            case "unlimitedtippedarrow":
                return ItemStackGenerator.getUnlimitedTippedArrow();
            case "villagerrevivalartifact":
                return ItemStackGenerator.getVillagerRevivalArtifact();
            case "enderessence":
                return ItemStackGenerator.getEnderEssence();
            case "creeperessence":
                return ItemStackGenerator.getCreeperEssence();
            case "potionbag":
                return ItemStackGenerator.getPotionBag(previousPotionBagID++);
            case "magicbagowind":
                return ItemStackGenerator.getMagicBagOfWind();
            case "dragonbreathcannon":
                return ItemStackGenerator.getDragonBreathCannon();
            case "unlimitedwitherrose":
                return ItemStackGenerator.getUnlimitedWitherRose();
            default:
                return null;
        }
    }
}
