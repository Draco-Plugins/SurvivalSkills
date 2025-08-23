package sir_draco.survivalskills.commands.admin_commands;

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
import sir_draco.survivalskills.utils.ItemStackGenerator;
import sir_draco.survivalskills.SurvivalSkills;

import static sir_draco.survivalskills.skill_listeners.GodListener.previousPotionBagID;

@SuppressWarnings("NullableProblems")
public class SurvivalSkillsGetCommand implements CommandExecutor {

    public SurvivalSkillsGetCommand(SurvivalSkills plugin) {
        PluginCommand command = plugin.getCommand("ssget");
        if (command != null)
            command.setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player p))
            return false;
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
        } else if (strings[0].equalsIgnoreCase("wandererarmor")) {
            p.getInventory().addItem(ItemStackGenerator.getWandererHelmet());
            p.getInventory().addItem(ItemStackGenerator.getWandererChestplate());
            p.getInventory().addItem(ItemStackGenerator.getWandererLeggings());
            p.getInventory().addItem(ItemStackGenerator.getWandererBoots());
            p.sendRawMessage(ChatColor.GREEN + "You have received: Wanderer Armor");
            p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            return true;
        } else if (strings[0].equalsIgnoreCase("travelerarmor")) {
            p.getInventory().addItem(ItemStackGenerator.getTravelerHelmet());
            p.getInventory().addItem(ItemStackGenerator.getTravelerChestplate());
            p.getInventory().addItem(ItemStackGenerator.getTravelerLeggings());
            p.getInventory().addItem(ItemStackGenerator.getTravelerBoots());
            p.sendRawMessage(ChatColor.GREEN + "You have received: Traveler Armor");
            p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            return true;
        } else if (strings[0].equalsIgnoreCase("gillarmor")) {
            p.getInventory().addItem(ItemStackGenerator.getGillHelmet());
            p.getInventory().addItem(ItemStackGenerator.getGillChestplate());
            p.getInventory().addItem(ItemStackGenerator.getGillLeggings());
            p.getInventory().addItem(ItemStackGenerator.getGillBoots());
            p.sendRawMessage(ChatColor.GREEN + "You have received: Gill Armor");
            p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            return true;
        } else if (strings[0].equalsIgnoreCase("adventurerarmor")) {
            p.getInventory().addItem(ItemStackGenerator.getAdventurerHelmet());
            p.getInventory().addItem(ItemStackGenerator.getAdventurerChestplate());
            p.getInventory().addItem(ItemStackGenerator.getAdventurerLeggings());
            p.getInventory().addItem(ItemStackGenerator.getAdventurerBoots());
            p.sendRawMessage(ChatColor.GREEN + "You have received: Adventurer Armor");
            p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            return true;
        } else if (strings[0].equalsIgnoreCase("beaconarmor")) {
            p.getInventory().addItem(ItemStackGenerator.getBeaconHelmet());
            p.getInventory().addItem(ItemStackGenerator.getBeaconChestplate());
            p.getInventory().addItem(ItemStackGenerator.getBeaconLeggings());
            p.getInventory().addItem(ItemStackGenerator.getBeaconBoots());
            p.sendRawMessage(ChatColor.GREEN + "You have received: Beacon Armor");
            p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
            return true;
        } else if (strings[0].equalsIgnoreCase("powerarmor")) {
            p.getInventory().addItem(ItemStackGenerator.getPowerHelmet());
            p.getInventory().addItem(ItemStackGenerator.getPowerChestplate());
            p.getInventory().addItem(ItemStackGenerator.getPowerLeggings());
            p.getInventory().addItem(ItemStackGenerator.getPowerBoots());
            p.sendRawMessage(ChatColor.GREEN + "You have received: Power Armor");
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
        return switch (item.toLowerCase()) {
            case "unlimitedtorch" -> ItemStackGenerator.getUnlimitedTorch();
            case "jumpingboots" -> ItemStackGenerator.getJumpingBoots();
            case "miningboots" -> ItemStackGenerator.getMiningBoots();
            case "miningleggings" -> ItemStackGenerator.getMiningLeggings();
            case "miningchestplate" -> ItemStackGenerator.getMiningChestplate();
            case "mininghelmet" -> ItemStackGenerator.getMiningHelmet();
            case "beaconhelmet" -> ItemStackGenerator.getBeaconHelmet();
            case "beaconchestplate" -> ItemStackGenerator.getBeaconChestplate();
            case "beaconleggings" -> ItemStackGenerator.getBeaconLeggings();
            case "beaconboots" -> ItemStackGenerator.getBeaconBoots();
            case "wandererboots" -> ItemStackGenerator.getWandererBoots();
            case "wandererleggings" -> ItemStackGenerator.getWandererLeggings();
            case "wandererchestplate" -> ItemStackGenerator.getWandererChestplate();
            case "wandererhelmet" -> ItemStackGenerator.getWandererHelmet();
            case "cavefinder" -> ItemStackGenerator.getCaveFinder();
            case "travelerboots" -> ItemStackGenerator.getTravelerBoots();
            case "travelerleggings" -> ItemStackGenerator.getTravelerLeggings();
            case "travelerchestplate" -> ItemStackGenerator.getTravelerChestplate();
            case "travelerhelmet" -> ItemStackGenerator.getTravelerHelmet();
            case "gillboots" -> ItemStackGenerator.getGillBoots();
            case "gillleggings" -> ItemStackGenerator.getGillLeggings();
            case "gillchestplate" -> ItemStackGenerator.getGillChestplate();
            case "gillhelmet" -> ItemStackGenerator.getGillHelmet();
            case "adventurerboots" -> ItemStackGenerator.getAdventurerBoots();
            case "adventurerleggings" -> ItemStackGenerator.getAdventurerLeggings();
            case "adventurerchestplate" -> ItemStackGenerator.getAdventurerChestplate();
            case "adventurerhelmet" -> ItemStackGenerator.getAdventurerHelmet();
            case "wateringcan" -> ItemStackGenerator.getWateringCan();
            case "unlimitedbonemeal" -> ItemStackGenerator.getUnlimitedBoneMeal();
            case "harvester" -> ItemStackGenerator.getHarvester();
            case "giantsummoner" -> ItemStackGenerator.getGiantSummoner();
            case "broodmothersummoner" -> ItemStackGenerator.getBroodMotherSummoner();
            case "exiledsummoner" -> ItemStackGenerator.getVillagerSummoner();
            case "dragonhead" -> ItemStackGenerator.getEnderDragonBossItem();
            case "sortofstonepick" -> ItemStackGenerator.getSortOfStonePick();
            case "fireworkcannon" -> ItemStackGenerator.getFireworkCannon();
            case "sortwand" -> ItemStackGenerator.getSortWand();
            case "unlimitedtropicalfishbucket" -> ItemStackGenerator.getUnlimitedTropicalFishBucket();
            case "unlimitedwaterbucket" -> ItemStackGenerator.getUnlimitedWaterBucket();
            case "unlimitedlavabucket" -> ItemStackGenerator.getUnlimitedLavaBucket();
            case "weatherartifact" -> ItemStackGenerator.getWeatherArtifact();
            case "timeartifact" -> ItemStackGenerator.getTimeArtifact();
            case "xpvoucher" -> ItemStackGenerator.getExperienceMultiplierVoucher(2, 60);
            case "zapwand" -> ItemStackGenerator.getZapWand();
            case "magnet" -> ItemStackGenerator.getMagnet();
            case "webshooter" -> ItemStackGenerator.getWebShooter();
            case "unlimitedtippedarrow" -> ItemStackGenerator.getUnlimitedTippedArrow();
            case "villagerrevivalartifact" -> ItemStackGenerator.getVillagerRevivalArtifact();
            case "enderessence" -> ItemStackGenerator.getEnderEssence();
            case "creeperessence" -> ItemStackGenerator.getCreeperEssence();
            case "potionbag" -> ItemStackGenerator.getPotionBag(previousPotionBagID++);
            case "magicbagowind" -> ItemStackGenerator.getMagicBagOfWind();
            case "dragonbreathcannon" -> ItemStackGenerator.getDragonBreathCannon();
            case "unlimitedwitherrose" -> ItemStackGenerator.getUnlimitedWitherRose();
            case "turtlehelmet" -> ItemStackGenerator.getTurtleHelmet();
            case "goathorn" -> ItemStackGenerator.getGoatHorn();
            case "firstalbum" -> ItemStackGenerator.getFirstAlbum();
            case "secondalbum" -> ItemStackGenerator.getSecondAlbum();
            case "musicknowledgedisc" -> ItemStackGenerator.getMusicKnowledgeDisc();
            case "firstsherd" -> ItemStackGenerator.getFirstSherd();
            case "secondsherd" -> ItemStackGenerator.getSecondSherd();
            case "sherdrelic" -> ItemStackGenerator.getSherdRelic();
            case "firsttrim" -> ItemStackGenerator.getFirstTrim();
            case "secondtrim" -> ItemStackGenerator.getSecondTrim();
            case "trimrelic" -> ItemStackGenerator.getTrimRelic();
            case "warrioremblem" -> ItemStackGenerator.getWarriorEmblem();
            case "tridentlauncher" -> ItemStackGenerator.getTridentLauncher();
            case "powerore" -> ItemStackGenerator.getPowerOre();
            case "godtrophybase" -> ItemStackGenerator.getGodTrophyBase();
            case "unlimitedsponge" -> ItemStackGenerator.getUnlimitedSponge();
            case "giantbossitem" -> ItemStackGenerator.getGiantBossItem();
            case "broodmotherbossitem" -> ItemStackGenerator.getBroodMotherBossItem();
            case "villagerbossitem" -> ItemStackGenerator.getVillagerBossItem();
            case "powersword" -> ItemStackGenerator.getPowerSword();
            case "powerdrill" -> ItemStackGenerator.getPowerDrill();
            case "powerlaser" -> ItemStackGenerator.getPowerLaser();
            case "powerhelmet" -> ItemStackGenerator.getPowerHelmet();
            case "powerchestplate" -> ItemStackGenerator.getPowerChestplate();
            case "powerleggings" -> ItemStackGenerator.getPowerLeggings();
            case "powerboots" -> ItemStackGenerator.getPowerBoots();
            default -> null;
        };
    }
}
