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
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.super_enchanting.SuperEnchantingItems;
import sir_draco.survivalskills.utils.items.ItemStackGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class SurvivalSkillsGetCommand implements CommandExecutor {

    private static final Sound GIFT_SOUND = Sound.ENTITY_PLAYER_LEVELUP;
    private static final float GIFT_SOUND_VOLUME = 1.0f;
    private static final float GIFT_SOUND_PITCH = 1.0f;

    // A single armor set: its display name plus the keys of its constituent pieces,
    // each of which is resolvable through ITEM_SUPPLIERS.
    private record ArmorSet(String displayName, List<String> partKeys) {}

    private static final Map<String, ArmorSet> ARMOR_SETS = Map.of(
            "miningarmor", new ArmorSet("Mining Armor",
                    List.of("mininghelmet", "miningchestplate", "miningleggings", "miningboots")),
            "wandererarmor", new ArmorSet("Wanderer Armor",
                    List.of("wandererhelmet", "wandererchestplate", "wandererleggings", "wandererboots")),
            "travelerarmor", new ArmorSet("Traveler Armor",
                    List.of("travelerhelmet", "travelerchestplate", "travelerleggings", "travelerboots")),
            "gillarmor", new ArmorSet("Gill Armor",
                    List.of("gillhelmet", "gillchestplate", "gillleggings", "gillboots")),
            "adventurerarmor", new ArmorSet("Adventurer Armor",
                    List.of("adventurerhelmet", "adventurerchestplate", "adventurerleggings", "adventurerboots")),
            "beaconarmor", new ArmorSet("Beacon Armor",
                    List.of("beaconhelmet", "beaconchestplate", "beaconleggings", "beaconboots")),
            "powerarmor", new ArmorSet("Power Armor",
                    List.of("powerhelmet", "powerchestplate", "powerleggings", "powerboots")));

    private static final Map<String, Supplier<ItemStack>> ITEM_SUPPLIERS = new HashMap<>();

    static {
        ITEM_SUPPLIERS.put("unlimitedtorch", ItemStackGenerator::getUnlimitedTorch);
        ITEM_SUPPLIERS.put("jumpingboots", ItemStackGenerator::getJumpingBoots);
        ITEM_SUPPLIERS.put("miningboots", ItemStackGenerator::getMiningBoots);
        ITEM_SUPPLIERS.put("miningleggings", ItemStackGenerator::getMiningLeggings);
        ITEM_SUPPLIERS.put("miningchestplate", ItemStackGenerator::getMiningChestplate);
        ITEM_SUPPLIERS.put("mininghelmet", ItemStackGenerator::getMiningHelmet);
        ITEM_SUPPLIERS.put("beaconhelmet", ItemStackGenerator::getBeaconHelmet);
        ITEM_SUPPLIERS.put("beaconchestplate", ItemStackGenerator::getBeaconChestplate);
        ITEM_SUPPLIERS.put("beaconleggings", ItemStackGenerator::getBeaconLeggings);
        ITEM_SUPPLIERS.put("beaconboots", ItemStackGenerator::getBeaconBoots);
        ITEM_SUPPLIERS.put("wandererboots", ItemStackGenerator::getWandererBoots);
        ITEM_SUPPLIERS.put("wandererleggings", ItemStackGenerator::getWandererLeggings);
        ITEM_SUPPLIERS.put("wandererchestplate", ItemStackGenerator::getWandererChestplate);
        ITEM_SUPPLIERS.put("wandererhelmet", ItemStackGenerator::getWandererHelmet);
        ITEM_SUPPLIERS.put("cavefinder", ItemStackGenerator::getCaveFinder);
        ITEM_SUPPLIERS.put("travelerboots", ItemStackGenerator::getTravelerBoots);
        ITEM_SUPPLIERS.put("travelerleggings", ItemStackGenerator::getTravelerLeggings);
        ITEM_SUPPLIERS.put("travelerchestplate", ItemStackGenerator::getTravelerChestplate);
        ITEM_SUPPLIERS.put("travelerhelmet", ItemStackGenerator::getTravelerHelmet);
        ITEM_SUPPLIERS.put("gillboots", ItemStackGenerator::getGillBoots);
        ITEM_SUPPLIERS.put("gillleggings", ItemStackGenerator::getGillLeggings);
        ITEM_SUPPLIERS.put("gillchestplate", ItemStackGenerator::getGillChestplate);
        ITEM_SUPPLIERS.put("gillhelmet", ItemStackGenerator::getGillHelmet);
        ITEM_SUPPLIERS.put("adventurerboots", ItemStackGenerator::getAdventurerBoots);
        ITEM_SUPPLIERS.put("adventurerleggings", ItemStackGenerator::getAdventurerLeggings);
        ITEM_SUPPLIERS.put("adventurerchestplate", ItemStackGenerator::getAdventurerChestplate);
        ITEM_SUPPLIERS.put("adventurerhelmet", ItemStackGenerator::getAdventurerHelmet);
        ITEM_SUPPLIERS.put("wateringcan", ItemStackGenerator::getWateringCan);
        ITEM_SUPPLIERS.put("unlimitedbonemeal", ItemStackGenerator::getUnlimitedBoneMeal);
        ITEM_SUPPLIERS.put("harvester", ItemStackGenerator::getHarvester);
        ITEM_SUPPLIERS.put("giantsummoner", ItemStackGenerator::getGiantSummoner);
        ITEM_SUPPLIERS.put("elderguardianspawnegg", ItemStackGenerator::getElderGuardianSpawnEgg);
        ITEM_SUPPLIERS.put("broodmothersummoner", ItemStackGenerator::getBroodMotherSummoner);
        ITEM_SUPPLIERS.put("exiledsummoner", ItemStackGenerator::getVillagerSummoner);
        ITEM_SUPPLIERS.put("dragonhead", ItemStackGenerator::getEnderDragonBossItem);
        ITEM_SUPPLIERS.put("fireworkcannon", ItemStackGenerator::getFireworkCannon);
        ITEM_SUPPLIERS.put("sortwand", ItemStackGenerator::getSortWand);
        ITEM_SUPPLIERS.put("builderwand", ItemStackGenerator::getBuilderWand);
        ITEM_SUPPLIERS.put("unlimitedtropicalfishbucket", ItemStackGenerator::getUnlimitedTropicalFishBucket);
        ITEM_SUPPLIERS.put("unlimitedwaterbucket", ItemStackGenerator::getUnlimitedWaterBucket);
        ITEM_SUPPLIERS.put("unlimitedlavabucket", ItemStackGenerator::getUnlimitedLavaBucket);
        ITEM_SUPPLIERS.put("unlimitedpowdersnowbucket", ItemStackGenerator::getUnlimitedPowderSnowBucket);
        ITEM_SUPPLIERS.put("unlimitedemptybucket", ItemStackGenerator::getUnlimitedEmptyBucket);
        ITEM_SUPPLIERS.put("weatherartifact", ItemStackGenerator::getWeatherArtifact);
        ITEM_SUPPLIERS.put("timeartifact", ItemStackGenerator::getTimeArtifact);
        ITEM_SUPPLIERS.put("xpvoucher", () -> ItemStackGenerator.getExperienceMultiplierVoucher(2, 60));
        ITEM_SUPPLIERS.put("zapwand", ItemStackGenerator::getZapWand);
        ITEM_SUPPLIERS.put("magnet", ItemStackGenerator::getMagnet);
        ITEM_SUPPLIERS.put("webshooter", ItemStackGenerator::getWebShooter);
        ITEM_SUPPLIERS.put("unlimitedtippedarrow", ItemStackGenerator::getUnlimitedTippedArrow);
        ITEM_SUPPLIERS.put("villagerrevivalartifact", ItemStackGenerator::getVillagerRevivalArtifact);
        ITEM_SUPPLIERS.put("enderessence", ItemStackGenerator::getEnderEssence);
        ITEM_SUPPLIERS.put("creeperessence", ItemStackGenerator::getCreeperEssence);
        ITEM_SUPPLIERS.put("chargedcreeperessence", ItemStackGenerator::getChargedCreeperEssence);
        ITEM_SUPPLIERS.put("ravagerdash", ItemStackGenerator::getRavagerDash);
        ITEM_SUPPLIERS.put("biomefinder", ItemStackGenerator::getBiomeFinder);
        ITEM_SUPPLIERS.put("potionbag", ItemStackGenerator::getNewPotionBag);
        ITEM_SUPPLIERS.put("magicbagowind", ItemStackGenerator::getMagicBagOfWind);
        ITEM_SUPPLIERS.put("fireballcannon", ItemStackGenerator::getFireballCannon);
        ITEM_SUPPLIERS.put("dragonbreathcannon", ItemStackGenerator::getDragonBreathCannon);
        ITEM_SUPPLIERS.put("unlimitedwitherrose", ItemStackGenerator::getUnlimitedWitherRose);
        ITEM_SUPPLIERS.put("witherskullcannon", ItemStackGenerator::getWitherSkullCannon);
        ITEM_SUPPLIERS.put("turtlehelmet", ItemStackGenerator::getTurtleHelmet);
        ITEM_SUPPLIERS.put("goathorn", ItemStackGenerator::getGoatHorn);
        ITEM_SUPPLIERS.put("firstalbum", ItemStackGenerator::getFirstAlbum);
        ITEM_SUPPLIERS.put("secondalbum", ItemStackGenerator::getSecondAlbum);
        ITEM_SUPPLIERS.put("musicknowledgedisc", ItemStackGenerator::getMusicKnowledgeDisc);
        ITEM_SUPPLIERS.put("firstsherd", ItemStackGenerator::getFirstSherd);
        ITEM_SUPPLIERS.put("secondsherd", ItemStackGenerator::getSecondSherd);
        ITEM_SUPPLIERS.put("sherdrelic", ItemStackGenerator::getSherdRelic);
        ITEM_SUPPLIERS.put("firsttrim", ItemStackGenerator::getFirstTrim);
        ITEM_SUPPLIERS.put("secondtrim", ItemStackGenerator::getSecondTrim);
        ITEM_SUPPLIERS.put("trimrelic", ItemStackGenerator::getTrimRelic);
        ITEM_SUPPLIERS.put("warrioremblem", ItemStackGenerator::getWarriorEmblem);
        ITEM_SUPPLIERS.put("tridentlauncher", ItemStackGenerator::getTridentLauncher);
        ITEM_SUPPLIERS.put("powerore", ItemStackGenerator::getPowerOre);
        ITEM_SUPPLIERS.put("godtrophybase", ItemStackGenerator::getGodTrophyBase);
        ITEM_SUPPLIERS.put("unlimitedsponge", ItemStackGenerator::getUnlimitedSponge);
        ITEM_SUPPLIERS.put("giantbossitem", ItemStackGenerator::getGiantBossItem);
        ITEM_SUPPLIERS.put("broodmotherbossitem", ItemStackGenerator::getBroodMotherBossItem);
        ITEM_SUPPLIERS.put("villagerbossitem", ItemStackGenerator::getVillagerBossItem);
        ITEM_SUPPLIERS.put("fishingbossitem", ItemStackGenerator::getFishingBossItem);
        ITEM_SUPPLIERS.put("powersword", ItemStackGenerator::getPowerSword);
        ITEM_SUPPLIERS.put("powerdrill", ItemStackGenerator::getPowerDrill);
        ITEM_SUPPLIERS.put("powerlaser", ItemStackGenerator::getPowerLaser);
        ITEM_SUPPLIERS.put("powerhelmet", ItemStackGenerator::getPowerHelmet);
        ITEM_SUPPLIERS.put("powerchestplate", ItemStackGenerator::getPowerChestplate);
        ITEM_SUPPLIERS.put("powerleggings", ItemStackGenerator::getPowerLeggings);
        ITEM_SUPPLIERS.put("powerboots", ItemStackGenerator::getPowerBoots);
        ITEM_SUPPLIERS.put("pipewrench", ItemStackGenerator::getWrench);
        ITEM_SUPPLIERS.put("transferpipe", ItemStackGenerator::getTransferPipe);
        ITEM_SUPPLIERS.put("trialfragment", () -> SuperEnchantingItems.createTrialFragment(
                SurvivalSkills.getInstance(), 1));
        ITEM_SUPPLIERS.put("superenchantingtable", () -> SuperEnchantingItems.createSuperEnchantingTable(
                SurvivalSkills.getInstance()));
    }

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

        String key = strings[0].toLowerCase();

        ArmorSet armorSet = ARMOR_SETS.get(key);
        if (armorSet != null) {
            giveArmorSet(p, armorSet);
            return true;
        }

        Supplier<ItemStack> supplier = ITEM_SUPPLIERS.get(key);
        if (supplier == null) {
            p.sendRawMessage(ChatColor.RED + "Invalid item: " + strings[0]);
            return false;
        }
        return giveItemToPlayer(p, supplier.get());
    }

    private void giveArmorSet(Player p, ArmorSet set) {
        for (String partKey : set.partKeys()) {
            Supplier<ItemStack> supplier = ITEM_SUPPLIERS.get(partKey);
            if (supplier != null)
                p.getInventory().addItem(supplier.get());
        }
        p.sendRawMessage(ChatColor.GREEN + "You have received: " + set.displayName());
        p.playSound(p, GIFT_SOUND, GIFT_SOUND_VOLUME, GIFT_SOUND_PITCH);
    }

    private boolean giveItemToPlayer(Player p, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            Bukkit.getLogger().warning("Item " + item.getType() + " does not have item meta");
            return false;
        }
        p.getInventory().addItem(item);
        p.sendRawMessage(ChatColor.GREEN + "You have received: " + meta.getDisplayName());
        p.playSound(p, GIFT_SOUND, GIFT_SOUND_VOLUME, GIFT_SOUND_PITCH);
        return true;
    }
}
