package sir_draco.survivalskills.Rewards;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;

public class RewardNotifications {

    public static void notifyPlayer(String type, String reward, Player p) {
        switch (type) {
            case "Mining":
                switch (reward) {
                    case "UnlimitedTorch":
                        p.sendRawMessage(ChatColor.GREEN + "You can now craft an unlimited torch");
                        p.sendRawMessage(ChatColor.GRAY + "See the crafting recipe by using the command" + ChatColor.AQUA + " /skills recipes");
                        break;
                    case "FortuneI":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "20%" + ChatColor.GREEN
                                + " chance of getting double ores");
                        break;
                    case "SpelunkerI":
                        p.sendRawMessage(ChatColor.GREEN + "You can now use" + ChatColor.AQUA + " /spelunker" + ChatColor.GREEN
                                + " to highlight nearby ores and tell you where nearby ores are");
                        p.sendRawMessage(ChatColor.GREEN + "It currently lasts " + ChatColor.AQUA + "5 " + ChatColor.GREEN
                                + "minutes with a cooldown of " + ChatColor.AQUA + "60 " + ChatColor.GREEN + "minutes and a radius of "
                                + ChatColor.AQUA + "5 " + ChatColor.GREEN + "blocks");
                        break;
                    case "FortuneII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "40%" + ChatColor.GREEN
                                                        + " chance of getting double ores");
                        break;
                    case "VeinMinerI":
                        p.sendRawMessage(ChatColor.GREEN + "You can now enable vein miner using" + ChatColor.AQUA + " /veinminer");
                        p.sendRawMessage(ChatColor.GREEN + "Sneak to use it");
                        p.sendRawMessage(ChatColor.GREEN + "Vein miner takes your hunger to use it. " + ChatColor.YELLOW+ "Level up mining to get rid of this");
                        break;
                    case "FortuneIII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "50%" + ChatColor.GREEN
                                                        + " chance of getting double ores");
                        break;
                    case "NightVisionI":
                        p.sendRawMessage(ChatColor.GREEN + "You can now enable night vision using" + ChatColor.AQUA + " /ssnv");
                        p.sendRawMessage(ChatColor.GREEN + "Night vision lasts for " + ChatColor.AQUA + "15 " + ChatColor.GREEN + "minutes");
                        break;
                    case "ArmorI":
                        p.sendRawMessage(ChatColor.GREEN + "You now take " + ChatColor.AQUA + "5%" + ChatColor.GREEN
                                + " less damage from all sources");
                        break;
                    case "SpelunkerII":
                        p.sendRawMessage(ChatColor.GREEN + "Spelunker now lasts " + ChatColor.AQUA + "15 " + ChatColor.GREEN
                                + "minutes with a cooldown of " + ChatColor.AQUA + "30 " + ChatColor.GREEN + "minutes and a radius of "
                                + ChatColor.AQUA + "10 " + ChatColor.GREEN + "blocks");
                        break;
                    case "MiningArmor":
                        p.sendRawMessage(ChatColor.GREEN + "You can now craft Mining Armor");
                        p.sendRawMessage(ChatColor.GRAY + "See the crafting recipe by using the command" + ChatColor.AQUA + " /skills recipes");
                        break;
                    case "VeinMinerII":
                        p.sendRawMessage(ChatColor.GREEN + "Vein miner no longer makes you hungry");
                        break;
                    case "PeacefulMiner":
                        p.sendRawMessage(ChatColor.GREEN + "You can now use" + ChatColor.AQUA + " /peacefulminer" + ChatColor.GREEN
                                + " to stop mobs from spawning while you are below Y = 64");
                        break;
                    case "SpelunkerIII":
                        p.sendRawMessage(ChatColor.GREEN + "Spelunker now lasts " + ChatColor.AQUA + "30 " + ChatColor.GREEN
                                + "minutes with a cooldown of " + ChatColor.AQUA + "30 " + ChatColor.GREEN + "minutes and a radius of "
                                + ChatColor.AQUA + "15 " + ChatColor.GREEN + "blocks");
                        break;
                    case "ArmorII":
                        p.sendRawMessage(ChatColor.GREEN + "You now take " + ChatColor.AQUA + "10%" + ChatColor.GREEN
                                + " less damage from all sources");
                        break;
                    case "NightVisionII":
                        p.sendRawMessage(ChatColor.GREEN + "Night vision now lasts infinitely");
                        break;
                    case "ArmorIII":
                        p.sendRawMessage(ChatColor.GREEN + "You now take " + ChatColor.AQUA + "15%" + ChatColor.GREEN
                                + " less damage from all sources");
                        break;
                    case "UnbreakableTools":
                        p.sendRawMessage(ChatColor.GREEN + "Your tools will never break again");
                        break;
                    case "ArmorIV":
                        p.sendRawMessage(ChatColor.GREEN + "You now take " + ChatColor.AQUA + "20%" + ChatColor.GREEN
                                + " less damage from all sources");
                        break;
                    case "ZapWand":
                        p.sendRawMessage(ChatColor.GREEN + "You can now craft a Zap Wand");
                        p.sendRawMessage(ChatColor.GRAY + "See the crafting recipe by using the command" + ChatColor.AQUA + " /skills recipes");
                        break;
                    case "BeaconArmor":
                        p.sendRawMessage(ChatColor.GREEN + "You can now craft Beacon Armor");
                        p.sendRawMessage(ChatColor.GREEN + "Beacon Armor gives you and those nearby beacon effects!");
                        p.sendRawMessage(ChatColor.GRAY + "See the crafting recipe by using the command" + ChatColor.AQUA + " /skills recipes");
                        break;
                }
                break;
            case "Exploring":
                switch (reward) {
                    case "SpeedI":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "20%" + ChatColor.GREEN
                                + " speed boost");
                        break;
                    case "SpeedII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "40%" + ChatColor.GREEN
                                + " speed boost");
                        break;
                    case "SpeedIII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "60%" + ChatColor.GREEN
                                + " speed boost");
                        break;
                    case "SpeedIV":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "80%" + ChatColor.GREEN
                                + " speed boost");
                        break;
                    case "SpeedV":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "100%" + ChatColor.GREEN
                                + " speed boost");
                        break;
                    case "JumpingBoots":
                        p.sendRawMessage(ChatColor.GREEN + "You can now craft Jumping Boots");
                        break;
                    case "SwimI":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "40%" + ChatColor.GREEN
                                + " swim speed boost");
                        break;
                    case "SwimII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "80%" + ChatColor.GREEN
                                + " swim speed boost");
                        break;
                    case "SwimIII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "120%" + ChatColor.GREEN
                                + " swim speed boost");
                        break;
                    case "SwimIV":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "160%" + ChatColor.GREEN
                                + " swim speed boost");
                        break;
                    case "SwimV":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "200%" + ChatColor.GREEN
                                + " swim speed boost");
                        break;
                    case "WandererArmor":
                        p.sendRawMessage(ChatColor.GREEN + "You can now craft Wanderer Armor");
                        p.sendRawMessage(ChatColor.GREEN + "Wanderer Armor lets you travel faster!");
                        p.sendRawMessage(ChatColor.GRAY + "See the crafting recipe by using the command" + ChatColor.AQUA + " /skills recipes");
                        break;
                    case "HealthRegen":
                        p.sendRawMessage(ChatColor.GREEN + "You now have permanent health regen");
                        break;
                    case "CaveFinder":
                        p.sendRawMessage(ChatColor.GREEN + "You can now craft a Cave Finder");
                        p.sendRawMessage(ChatColor.GREEN + "Cave Finder shows you where nearby caves are! (Can give false positives with underground dark spots)");
                        p.sendRawMessage(ChatColor.GRAY + "See the crafting recipe by using the command" + ChatColor.AQUA + " /skills recipes");
                        break;
                    case "FallI":
                        p.sendRawMessage(ChatColor.GREEN + "You now take 25% less fall damage");
                        break;
                    case "FallII":
                        p.sendRawMessage(ChatColor.GREEN + "You now take 50% less fall damage");
                        break;
                    case "TravelerArmor":
                        p.sendRawMessage(ChatColor.GREEN + "You can now craft Traveler Armor");
                        p.sendRawMessage(ChatColor.GREEN + "Traveler Armor lets you travel even faster!");
                        p.sendRawMessage(ChatColor.GRAY + "See the crafting recipe by using the command" + ChatColor.AQUA + " /skills recipes");
                        break;
                    case "AdventurerArmor":
                        p.sendRawMessage(ChatColor.GREEN + "You can now craft Adventurer Armor");
                        p.sendRawMessage(ChatColor.GREEN + "Adventurer Armor lets you travel even faster and you won't take fall damage!");
                        p.sendRawMessage(ChatColor.GRAY + "See the crafting recipe by using the command" + ChatColor.AQUA + " /skills recipes");
                        break;
                    case "GillArmor":
                        p.sendRawMessage(ChatColor.GREEN + "You can now craft Gill Armor");
                        p.sendRawMessage(ChatColor.GREEN + "Gill Armor lets you breathe underwater and swim at hyper speed!");
                        p.sendRawMessage(ChatColor.GRAY + "See the crafting recipe by using the command" + ChatColor.AQUA + " /skills recipes");
                        break;
                }
                break;
            case "Farming":
                switch (reward) {
                    case "DoubleCropsI":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "25%" + ChatColor.GREEN
                                + " chance of getting double crops");
                        break;
                    case "DoubleCropsII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "50%" + ChatColor.GREEN
                                + " chance of getting double crops");
                        break;
                    case "DoubleCropsIII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "75%" + ChatColor.GREEN
                                + " chance of getting double crops");
                        break;
                    case "DoubleCropsIV":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "100%" + ChatColor.GREEN
                                + " chance of getting double crops");
                        break;
                    case "Harvester":
                        p.sendRawMessage(ChatColor.GREEN + "You can now craft a Harvester");
                        p.sendRawMessage(ChatColor.GREEN + "Break tons of crops at once and automatically replant them!");
                        p.sendRawMessage(ChatColor.GRAY + "See the crafting recipe by using the command" + ChatColor.AQUA + " /skills recipes");
                        break;
                    case "UnlimitedBoneMeal":
                        p.sendRawMessage(ChatColor.GREEN + "You can now craft an Unlimited Bone Meal");
                        p.sendRawMessage(ChatColor.GREEN + "Use it to instantly grow crops!");
                        p.sendRawMessage(ChatColor.GRAY + "See the crafting recipe by using the command" + ChatColor.AQUA + " /skills recipes");
                        break;
                    case "WateringCan":
                        p.sendRawMessage(ChatColor.GREEN + "You can now craft a Watering Can");
                        p.sendRawMessage(ChatColor.GREEN + "Use it to grow crops faster in a 5x5 area!");
                        p.sendRawMessage(ChatColor.GRAY + "See the crafting recipe by using the command" + ChatColor.AQUA + " /skills recipes");
                        break;
                    case "AutoEat":
                        p.sendRawMessage(ChatColor.GREEN + "You can now use Auto Eat by using" + ChatColor.AQUA + " /autoeat");
                        p.sendRawMessage(ChatColor.GREEN + "Auto Eat will automatically eat food from your inventory when you are hungry");
                        break;
                    case "Eat":
                        p.sendRawMessage(ChatColor.GREEN + "You can now feed yourself using " + ChatColor.AQUA + "/sseat");
                        break;
                    case "NoHunger":
                        p.sendRawMessage(ChatColor.GREEN + "You no longer need to eat food");
                        break;
                    case "HealthI":
                        p.sendRawMessage(ChatColor.GREEN + "You now have " + ChatColor.AQUA + "11" + ChatColor.GREEN + " hearts");
                        break;
                    case "HealthII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have " + ChatColor.AQUA + "12" + ChatColor.GREEN + " hearts");
                        break;
                    case "HealthIII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have " + ChatColor.AQUA + "13" + ChatColor.GREEN + " hearts");
                        break;
                    case "HealthIV":
                        p.sendRawMessage(ChatColor.GREEN + "You now have " + ChatColor.AQUA + "14" + ChatColor.GREEN + " hearts");
                        break;
                    case "HealthV":
                        p.sendRawMessage(ChatColor.GREEN + "You now have " + ChatColor.AQUA + "15" + ChatColor.GREEN + " hearts");
                        break;
                    case "HealthVI":
                        p.sendRawMessage(ChatColor.GREEN + "You now have " + ChatColor.AQUA + "16" + ChatColor.GREEN + " hearts");
                        break;
                    case "HealthVII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have " + ChatColor.AQUA + "17" + ChatColor.GREEN + " hearts");
                        break;
                    case "HealthVIII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have " + ChatColor.AQUA + "18" + ChatColor.GREEN + " hearts");
                        break;
                    case "HealthIX":
                        p.sendRawMessage(ChatColor.GREEN + "You now have " + ChatColor.AQUA + "19" + ChatColor.GREEN + " hearts");
                        break;
                    case "HealthX":
                        p.sendRawMessage(ChatColor.GREEN + "You now have " + ChatColor.AQUA + "20" + ChatColor.GREEN + " hearts");
                        break;
                }
                break;
            case "Building":
                switch (reward) {
                    case "BlockReturnI":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a "
                                + ChatColor.AQUA + "5%" + ChatColor.GREEN
                                + " chance of getting a (building) block back when you place it");
                        break;
                    case "BlockReturnII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a "
                                + ChatColor.AQUA + "10%" + ChatColor.GREEN
                                + " chance of getting a (building) block back when you place it");
                        break;
                    case "BlockReturnIII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a "
                                + ChatColor.AQUA + "15%" + ChatColor.GREEN
                                + " chance of getting a (building) block back when you place it");
                        break;
                    case "BlockReturnIV":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a "
                                + ChatColor.AQUA + "20%" + ChatColor.GREEN
                                + " chance of getting a (building) block back when you place it");
                        break;
                    case "BlockReturnV":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a "
                                + ChatColor.AQUA + "25%" + ChatColor.GREEN
                                + " chance of getting a (building) block back when you place it");
                        break;
                    case "BlockReturnVI":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a "
                                + ChatColor.AQUA + "30%" + ChatColor.GREEN
                                + " chance of getting a (building) block back when you place it");
                        break;
                    case "BlockReturnVII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a "
                                + ChatColor.AQUA + "35%" + ChatColor.GREEN
                                + " chance of getting a (building) block back when you place it");
                        break;
                    case "BlockReturnVIII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a "
                                + ChatColor.AQUA + "40%" + ChatColor.GREEN
                                + " chance of getting a (building) block back when you place it");
                        break;
                    case "BlockReturnIX":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a "
                                + ChatColor.AQUA + "45%" + ChatColor.GREEN
                                + " chance of getting a (building) block back when you place it");
                        break;
                    case "BlockReturnX":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a "
                                + ChatColor.AQUA + "50%" + ChatColor.GREEN
                                + " chance of getting a (building) block back when you place it");
                        break;
                    case "FlightI":
                        p.sendRawMessage(ChatColor.GREEN + "You can now use" + ChatColor.AQUA + " /flight" + ChatColor.GREEN
                                + " to fly for " + ChatColor.AQUA + "5 " + ChatColor.GREEN + "minutes with a cooldown of " + ChatColor.AQUA
                                + "60 " + ChatColor.GREEN + "minutes");
                        break;
                    case "FlightII":
                        p.sendRawMessage(ChatColor.GREEN + "Flight now lasts for " + ChatColor.AQUA + "15 "
                                + ChatColor.GREEN + "minutes with a cooldown of " + ChatColor.AQUA
                                + "30 " + ChatColor.GREEN + "minutes");
                        break;
                    case "FlightIII":
                        p.sendRawMessage(ChatColor.GREEN + "Flight now lasts for " + ChatColor.AQUA + "30 "
                                + ChatColor.GREEN + "minutes with a cooldown of " + ChatColor.AQUA
                                + "30 " + ChatColor.GREEN + "minutes");
                        break;
                    case "FlightIV":
                        p.sendRawMessage(ChatColor.GREEN + "Flight no longer has a timer!");
                        break;
                    case "ExtendedReach":
                        p.sendRawMessage(ChatColor.GREEN + "You can now reach further!");
                        break;
                    case "AutoSortWand":
                        p.sendRawMessage(ChatColor.GREEN + "You can now craft an Auto Sort Wand");
                        p.sendRawMessage(ChatColor.GREEN + "Use it to automatically sort a chests inventory!");
                        break;
                }
                break;
            case "Fighting":
                switch (reward) {
                    case "BerserkerI":
                        p.sendRawMessage(ChatColor.GREEN + "You can now enter berserker mode by sneaking and right clicking while holding a weapon");
                        p.sendRawMessage(ChatColor.GREEN + "Berserker mode lasts for " + ChatColor.AQUA + "3 "
                                + ChatColor.GREEN + "seconds with a cooldown of " + ChatColor.AQUA + "120 " + ChatColor.GREEN + "seconds");
                        break;
                    case "BerserkerII":
                        p.sendRawMessage(ChatColor.GREEN + "Berserker mode now lasts for " + ChatColor.AQUA + "5 "
                                + ChatColor.GREEN + "seconds with a cooldown of " + ChatColor.AQUA + "90 " + ChatColor.GREEN + "seconds");
                        break;
                    case "BerserkerIII":
                        p.sendRawMessage(ChatColor.GREEN + "Berserker mode now lasts for " + ChatColor.AQUA + "5 "
                                + ChatColor.GREEN + "seconds with a cooldown of " + ChatColor.AQUA + "60 " + ChatColor.GREEN + "seconds");
                        break;
                    case "BerserkerIV":
                        p.sendRawMessage(ChatColor.GREEN + "Berserker mode now lasts for " + ChatColor.AQUA + "5 "
                                + ChatColor.GREEN + "seconds with a cooldown of " + ChatColor.AQUA + "45 " + ChatColor.GREEN + "seconds");
                        break;
                    case "BerserkerV":
                        p.sendRawMessage(ChatColor.GREEN + "Berserker mode now lasts for " + ChatColor.AQUA + "8 "
                                + ChatColor.GREEN + "seconds with a cooldown of " + ChatColor.AQUA + "45 " + ChatColor.GREEN + "seconds");
                        break;
                    case "BerserkerVI":
                        p.sendRawMessage(ChatColor.GREEN + "Berserker mode now lasts for " + ChatColor.AQUA + "8 "
                                + ChatColor.GREEN + "seconds with a cooldown of " + ChatColor.AQUA + "30 " + ChatColor.GREEN + "seconds");
                        break;
                    case "BerserkerVII":
                        p.sendRawMessage(ChatColor.GREEN + "Berserker mode now lasts for " + ChatColor.AQUA + "10 "
                                + ChatColor.GREEN + "seconds with a cooldown of " + ChatColor.AQUA + "15 " + ChatColor.GREEN + "seconds");
                        break;
                    case "LifestealI":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "5% " + ChatColor.GREEN
                                + "chance of stealing health from enemies");
                        break;
                    case "LifestealII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "10% " + ChatColor.GREEN
                                + "chance of stealing health from enemies");
                        break;
                    case "LifestealIII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "15% " + ChatColor.GREEN
                                + "chance of stealing health from enemies");
                        break;
                    case "LifestealIV":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "20% " + ChatColor.GREEN
                                + "chance of stealing health from enemies");
                        break;
                    case "LifestealV":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "25% " + ChatColor.GREEN
                                + "chance of stealing health from enemies");
                        break;
                    case "CriticalI":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "10% " + ChatColor.GREEN
                                + "chance of doing double damage");
                        break;
                    case "CriticalII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "20% " + ChatColor.GREEN
                                + "chance of doing double damage");
                        break;
                    case "GiantSummon":
                        p.sendRawMessage(ChatColor.GREEN + "You can now craft a Giant Boss Summoning item");
                        p.sendRawMessage(ChatColor.GRAY + "See the crafting recipe by using the command" + ChatColor.AQUA + " /skills recipes");
                        break;
                    case "BroodMotherSummon":
                        p.sendRawMessage(ChatColor.GREEN + "You can now craft a Brood Mother Boss Summoning item");
                        p.sendRawMessage(ChatColor.GRAY + "See the crafting recipe by using the command" + ChatColor.AQUA + " /skills recipes");
                        break;
                    case "FishingKing":
                        p.sendRawMessage(ChatColor.GREEN + "You can now summon the Fishing King through fishing when it isn't raining");
                        break;
                    case "VillagerSummon":
                        p.sendRawMessage(ChatColor.GREEN + "You can now craft a Villager Boss Summoning item");
                        p.sendRawMessage(ChatColor.GRAY + "See the crafting recipe by using the command" + ChatColor.AQUA + " /skills recipes");
                        break;
                    case "MobScanner":
                        p.sendRawMessage(ChatColor.GREEN + "You can now use Mob Scanner by using" + ChatColor.AQUA + " /mobscanner");
                        p.sendRawMessage(ChatColor.GREEN + "Mob Scanner will show you where nearby mobs are");
                        break;
                    case "BloodyDomain":
                        p.sendRawMessage(ChatColor.GREEN + "You can now use Bloody Domain by using" + ChatColor.AQUA + " /togglebloodydomain");
                        p.sendRawMessage(ChatColor.GREEN + "Bloody Domain will kill all weak mobs within a 10 block radius");
                        break;
                }
                break;
            case "Fishing":
                switch (reward) {
                    case "CommonLootI":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a" + ChatColor.AQUA
                                + " 25% " + ChatColor.GREEN + "chance of getting common fishing loot");
                        break;
                    case "CommonLootII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a" + ChatColor.AQUA
                                + " 40% " + ChatColor.GREEN + "chance of getting common fishing loot");
                        break;
                    case "CommonLootIII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a" + ChatColor.AQUA
                                + " 55% " + ChatColor.GREEN + "chance of getting common fishing loot");
                        break;
                    case "CommonLootIV":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a" + ChatColor.AQUA
                                + " 65% " + ChatColor.GREEN + "chance of getting common fishing loot");
                        break;
                    case "CommonLootV":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a" + ChatColor.AQUA
                                + " 75% " + ChatColor.GREEN + "chance of getting common fishing loot");
                        break;
                    case "RareLootI":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a" + ChatColor.AQUA
                                + " 10% " + ChatColor.GREEN + "chance of getting rare fishing loot");
                        break;
                    case "RareLootII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a" + ChatColor.AQUA
                                + " 20% " + ChatColor.GREEN + "chance of getting rare fishing loot");
                        break;
                    case "RareLootIII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a" + ChatColor.AQUA
                                + " 30% " + ChatColor.GREEN + "chance of getting rare fishing loot");
                        break;
                    case "RareLootIV":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a" + ChatColor.AQUA
                                + " 35% " + ChatColor.GREEN + "chance of getting rare fishing loot");
                        break;
                    case "RareLootV":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a" + ChatColor.AQUA
                                + " 40% " + ChatColor.GREEN + "chance of getting rare fishing loot");
                        break;
                    case "EpicLootI":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a" + ChatColor.AQUA
                                + " 2% " + ChatColor.GREEN + "chance of getting epic fishing loot");
                        break;
                    case "EpicLootII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a" + ChatColor.AQUA
                                + " 3% " + ChatColor.GREEN + "chance of getting epic fishing loot");
                        break;
                    case "EpicLootIII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a" + ChatColor.AQUA
                                + " 4% " + ChatColor.GREEN + "chance of getting epic fishing loot");
                        break;
                    case "EpicLootIV":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a" + ChatColor.AQUA
                                + " 4.5% " + ChatColor.GREEN + "chance of getting epic fishing loot");
                        break;
                    case "EpicLootV":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a" + ChatColor.AQUA
                                + " 5% " + ChatColor.GREEN + "chance of getting epic fishing loot");
                        break;
                    case "LegendaryLootI":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a" + ChatColor.AQUA
                                + " 0.5% " + ChatColor.GREEN + "chance of getting legendary fishing loot");
                        break;
                    case "LegendaryLootII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a" + ChatColor.AQUA
                                + " 0.75% " + ChatColor.GREEN + "chance of getting legendary fishing loot");
                        break;
                    case "LegendaryLootIII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a" + ChatColor.AQUA
                                + " 1% " + ChatColor.GREEN + "chance of getting legendary fishing loot");
                        break;
                    case "ExperienceI":
                        p.sendRawMessage(ChatColor.GREEN + "You earn" + ChatColor.AQUA
                                + " 10% " + ChatColor.GREEN + "more experience from all sources");
                        break;
                    case "ExperienceII":
                        p.sendRawMessage(ChatColor.GREEN + "You earn" + ChatColor.AQUA
                                + " 20% " + ChatColor.GREEN + "more experience from all sources");
                        break;
                    case "ExperienceIII":
                        p.sendRawMessage(ChatColor.GREEN + "You earn" + ChatColor.AQUA
                                + " 30% " + ChatColor.GREEN + "more experience from all sources");
                        break;
                    case "ExperienceIV":
                        p.sendRawMessage(ChatColor.GREEN + "You earn" + ChatColor.AQUA
                                + " 40% " + ChatColor.GREEN + "more experience from all sources");
                        break;
                    case "ExperienceV":
                        p.sendRawMessage(ChatColor.GREEN + "You earn" + ChatColor.AQUA
                                + " 50% " + ChatColor.GREEN + "more experience from all sources");
                        break;
                    case "ExperienceVI":
                        p.sendRawMessage(ChatColor.GREEN + "You earn" + ChatColor.AQUA
                                + " 60% " + ChatColor.GREEN + "more experience from all sources");
                        break;
                    case "ExperienceVII":
                        p.sendRawMessage(ChatColor.GREEN + "You earn" + ChatColor.AQUA
                                + " 70% " + ChatColor.GREEN + "more experience from all sources");
                        break;
                    case "ExperienceVIII":
                        p.sendRawMessage(ChatColor.GREEN + "You earn" + ChatColor.AQUA
                                + " 80% " + ChatColor.GREEN + "more experience from all sources");
                        break;
                    case "ExperienceIX":
                        p.sendRawMessage(ChatColor.GREEN + "You earn" + ChatColor.AQUA
                                + " 90% " + ChatColor.GREEN + "more experience from all sources");
                        break;
                    case "ExperienceX":
                        p.sendRawMessage(ChatColor.GREEN + "You earn" + ChatColor.AQUA
                                + " 100% " + ChatColor.GREEN + "more experience from all sources");
                        break;
                    case "FasterFishingI":
                        p.sendRawMessage(ChatColor.GREEN + "Your base fishing speed has been increased");
                        break;
                    case "FasterFishingII":
                        p.sendRawMessage(ChatColor.GREEN + "Your base fishing speed has been increased even more");
                        break;
                    case "FasterFishingIII":
                        p.sendRawMessage(ChatColor.GREEN + "Your base fishing speed has been increased even more");
                        break;
                    case "FasterFishingIV":
                        p.sendRawMessage(ChatColor.GREEN + "Your base fishing speed has been increased even more");
                        break;
                    case "FasterFishingV":
                        p.sendRawMessage(ChatColor.GREEN + "Your base fishing speed has been maxed!");
                        break;
                    case "FishingLineI":
                        p.sendRawMessage(ChatColor.GREEN + "You now get the equivalent of" + ChatColor.AQUA
                                + " 2 " + ChatColor.GREEN + "fishing lines");
                        break;
                    case "FishingLineII":
                        p.sendRawMessage(ChatColor.GREEN + "You now get the equivalent of" + ChatColor.AQUA
                                + " 3 " + ChatColor.GREEN + "fishing lines");
                        break;
                    case "FishingLineIII":
                        p.sendRawMessage(ChatColor.GREEN + "You now get the equivalent of" + ChatColor.AQUA
                                + " 5 " + ChatColor.GREEN + "fishing lines");
                        break;
                    case "FishingLineIV":
                        p.sendRawMessage(ChatColor.GREEN + "You now get the equivalent of" + ChatColor.AQUA
                                + " 7 " + ChatColor.GREEN + "fishing lines");
                        break;
                    case "FishingLineV":
                        p.sendRawMessage(ChatColor.GREEN + "You now get the equivalent of" + ChatColor.AQUA
                                + " 10 " + ChatColor.GREEN + "fishing lines");
                        break;
                    case "WaterBreathingI":
                        p.sendRawMessage(ChatColor.GREEN + "You can now use " + ChatColor.AQUA + "/waterbreathing" + ChatColor.GREEN
                                + " to get water breathing for " + ChatColor.AQUA + "15 " + ChatColor.GREEN + "minutes with a cooldown of " + ChatColor.AQUA
                                + "60 " + ChatColor.GREEN + "minutes");
                        break;
                    case "WaterBreathingII":
                        p.sendRawMessage(ChatColor.GREEN + "Water Breathing now lasts for " + ChatColor.AQUA + "30 "
                                + ChatColor.GREEN + "minutes with a cooldown of " + ChatColor.AQUA
                                + "30 " + ChatColor.GREEN + "minutes");
                        break;
                    case "WaterBreathingIII":
                        p.sendRawMessage(ChatColor.GREEN + "You can now permanently breathe underwater");
                        break;
                    case "AutoTrashI":
                        p.sendRawMessage(ChatColor.GREEN + "You can now use Auto Trash by using" + ChatColor.AQUA + " /autotrash");
                        p.sendRawMessage(ChatColor.GREEN + "Auto Trash will automatically delete items that you pick up");
                        p.sendRawMessage(ChatColor.GREEN + "The Auto Trash inventory resets when you leave");
                        break;
                    case "AutoTrashII":
                        p.sendRawMessage(ChatColor.GREEN + "Your Auto Trash inventory has increased in size!");
                        break;
                    case "PermaTrash":
                        p.sendRawMessage(ChatColor.GREEN + "You can now use Perma Trash by using" + ChatColor.AQUA + " /permatrash");
                        p.sendRawMessage(ChatColor.GREEN + "The Perma Trash inventory saves all items you put in unless you remove them");
                        break;
                }
                break;
            case "Crafting":
                switch (reward) {
                    case "ExtraOutputI":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "5% " + ChatColor.GREEN
                                + "chance of getting double the output from crafting recipes");
                        p.sendRawMessage(ChatColor.YELLOW + "This does not apply to infinitely repeatable recipes " +
                                "(Coal -> Coal Block -> Coal...)");
                        break;
                    case "ExtraOutputII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "10% " + ChatColor.GREEN
                                + "chance of getting double the output from crafting recipes");
                        break;
                    case "ExtraOutputIII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "15% " + ChatColor.GREEN
                                + "chance of getting double the output from crafting recipes");
                        break;
                    case "ExtraOutputIV":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "20% " + ChatColor.GREEN
                                + "chance of getting double the output from crafting recipes");
                        break;
                    case "ExtraOutputV":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "25% " + ChatColor.GREEN
                                + "chance of getting double the output from crafting recipes");
                        break;
                    case "ExtraOutputVI":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "30% " + ChatColor.GREEN
                                + "chance of getting double the output from crafting recipes");
                        break;
                    case "ExtraOutputVII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "35% " + ChatColor.GREEN
                                + "chance of getting double the output from crafting recipes");
                        break;
                    case "ExtraOutputVIII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "40% " + ChatColor.GREEN
                                + "chance of getting double the output from crafting recipes");
                        break;
                    case "ExtraOutputIX":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "45% " + ChatColor.GREEN
                                + "chance of getting double the output from crafting recipes");
                        break;
                    case "ExtraOutputX":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "50% " + ChatColor.GREEN
                                + "chance of getting double the output from crafting recipes");
                        break;
                    case "MaterialsBackI":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "10% " + ChatColor.GREEN
                                + "chance of getting your materials back when crafting");
                        p.sendRawMessage(ChatColor.YELLOW + "This does not apply to infinitely repeatable recipes " +
                                "(Coal -> Coal Block -> Coal...)");
                        break;
                    case "MaterialsBackII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "20% " + ChatColor.GREEN
                                + "chance of getting your materials back when crafting");
                        break;
                    case "MaterialsBackIII":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "30% " + ChatColor.GREEN
                                + "chance of getting your materials back when crafting");
                        break;
                    case "MaterialsBackIV":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "40% " + ChatColor.GREEN
                                + "chance of getting your materials back when crafting");
                        break;
                    case "MaterialsBackV":
                        p.sendRawMessage(ChatColor.GREEN + "You now have a " + ChatColor.AQUA + "50% " + ChatColor.GREEN
                                + "chance of getting your materials back when crafting");
                        break;
                    case "EnchantedGapple":
                        p.sendRawMessage(ChatColor.GREEN + "You can now craft an Enchanted Golden Apple");
                        break;
                }
                break;
            case "Main":
                switch (reward) {
                    case "DeathLocationTracker":
                        p.sendRawMessage(ChatColor.GREEN + "You can now use" + ChatColor.AQUA + " /deathlocation" + ChatColor.GREEN
                                + " to find where you died" );
                        break;
                    case "FireworkCannon":
                        p.sendRawMessage(ChatColor.GREEN + "You can now craft a Firework Cannon");
                        p.sendRawMessage(ChatColor.GREEN + "Use it to shoot fireworks!");
                        break;
                    case "SetHomeI":
                        p.sendRawMessage(ChatColor.GREEN + "You can now set " + ChatColor.AQUA + "2 "
                                + ChatColor.GREEN + "homes using " + ChatColor.AQUA + "/sethome");
                        break;
                    case "SetHomeII":
                        p.sendRawMessage(ChatColor.GREEN + "You can now set " + ChatColor.AQUA + "3 "
                                + ChatColor.GREEN + "homes using " + ChatColor.AQUA + "/sethome");
                        break;
                    case "SetHomeIII":
                        p.sendRawMessage(ChatColor.GREEN + "You can now set " + ChatColor.AQUA + "4 "
                                + ChatColor.GREEN + "homes using " + ChatColor.AQUA + "/sethome");
                        break;
                    case "SetHomeIV":
                        p.sendRawMessage(ChatColor.GREEN + "You can now set " + ChatColor.AQUA + "5 "
                                + ChatColor.GREEN + "homes using " + ChatColor.AQUA + "/sethome");
                        break;
                    case "Gravestone":
                        p.sendRawMessage(ChatColor.GREEN + "Your items will now be held in a chest when you die");
                        break;
                    case "KeepExperience":
                        p.sendRawMessage(ChatColor.GREEN + "You will now keep your experience when you die");
                        break;
                    case "KeepInventory":
                        p.sendRawMessage(ChatColor.GREEN + "You will now keep your inventory when you die");
                        break;
                    case "DustTrail":
                        p.sendRawMessage(ChatColor.GREEN + "You have unlocked the dust trail");
                        p.sendRawMessage(ChatColor.GREEN + "Use " + ChatColor.AQUA + "/toggletrail dust" + ChatColor.GREEN + " to toggle it");
                        break;
                    case "WaterTrail":
                        p.sendRawMessage(ChatColor.GREEN + "You have unlocked the water trail");
                        p.sendRawMessage(ChatColor.GREEN + "Use " + ChatColor.AQUA + "/toggletrail water" + ChatColor.GREEN + " to toggle it");
                        break;
                    case "HappyTrail":
                        p.sendRawMessage(ChatColor.GREEN + "You have unlocked the happy trail");
                        p.sendRawMessage(ChatColor.GREEN + "Use " + ChatColor.AQUA + "/toggletrail happy" + ChatColor.GREEN + " to toggle it");
                        break;
                    case "DragonTrail":
                        p.sendRawMessage(ChatColor.GREEN + "You have unlocked the dragon trail");
                        p.sendRawMessage(ChatColor.GREEN + "Use " + ChatColor.AQUA + "/toggletrail dragon" + ChatColor.GREEN + " to toggle it");
                        break;
                    case "ElectricTrail":
                        p.sendRawMessage(ChatColor.GREEN + "You have unlocked the electric trail");
                        p.sendRawMessage(ChatColor.GREEN + "Use " + ChatColor.AQUA + "/toggletrail electric" + ChatColor.GREEN + " to toggle it");
                        break;
                    case "EnchantmentTrail":
                        p.sendRawMessage(ChatColor.GREEN + "You have unlocked the enchantment trail");
                        p.sendRawMessage(ChatColor.GREEN + "Use " + ChatColor.AQUA + "/toggletrail enchantment" + ChatColor.GREEN + " to toggle it");
                        break;
                    case "LoveTrail":
                        p.sendRawMessage(ChatColor.GREEN + "You have unlocked the love trail");
                        p.sendRawMessage(ChatColor.GREEN + "Use " + ChatColor.AQUA + "/toggletrail love" + ChatColor.GREEN + " to toggle it");
                        break;
                    case "FlameTrail":
                        p.sendRawMessage(ChatColor.GREEN + "You have unlocked the flame trail");
                        p.sendRawMessage(ChatColor.GREEN + "Use " + ChatColor.AQUA + "/toggletrail flame" + ChatColor.GREEN + " to toggle it");
                        break;
                    case "BlueFlameTrail":
                        p.sendRawMessage(ChatColor.GREEN + "You have unlocked the blue flame trail");
                        p.sendRawMessage(ChatColor.GREEN + "Use " + ChatColor.AQUA + "/toggletrail blueflame" + ChatColor.GREEN + " to toggle it");
                        break;
                    case "CherryTrail":
                        p.sendRawMessage(ChatColor.GREEN + "You have unlocked the cherry trail");
                        p.sendRawMessage(ChatColor.GREEN + "Use " + ChatColor.AQUA + "/toggletrail cherry" + ChatColor.GREEN + " to toggle it");
                        break;
                    case "RainbowTrail":
                        p.sendRawMessage(ChatColor.GREEN + "You have unlocked the rainbow trail");
                        p.sendRawMessage(ChatColor.GREEN + "Use " + ChatColor.AQUA + "/toggletrail rainbow" + ChatColor.GREEN + " to toggle it");
                        break;
                }
        }
    }

    public static String cooldown(int time) {
        int minutes = time / 60;
        int seconds = time % 60;
        return ChatColor.AQUA.toString() + minutes + ChatColor.GREEN + " minutes " + ChatColor.AQUA + seconds
                + ChatColor.GREEN + " seconds!";
    }

    public static String getRewardDescription(String type, String reward) {
        switch (type) {
            case "Mining":
                switch (reward) {
                    case "UnlimitedTorch":
                    case "MiningArmor":
                    case "ZapWand":
                    case "BeaconArmor":
                        return ChatColor.GRAY + "Craftable Item";
                    case "FortuneI":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "20%" + ChatColor.GRAY
                                + " chance of getting double ores";
                    case "SpelunkerI":
                        return ChatColor.GRAY + "You can now use" + ChatColor.AQUA + " /spelunker" + ChatColor.GRAY
                                + " to highlight\n" + ChatColor.GRAY + "nearby ores and tell you where nearby ores are.\n"
                                + ChatColor.GRAY + "It currently lasts " + ChatColor.AQUA + "5 " + ChatColor.GRAY
                                + "minutes with a cooldown of\n" + ChatColor.AQUA + "60 " + ChatColor.GRAY + "minutes and a radius of "
                                + ChatColor.AQUA + "5 " + ChatColor.GRAY + "blocks";
                    case "FortuneII":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "40%" + ChatColor.GRAY
                                + " chance of getting double ores";
                    case "VeinMinerI":
                        return ChatColor.GRAY + "You can now enable vein miner using" + ChatColor.AQUA + " /veinminer\n"
                                + ChatColor.GRAY + "Sneak to use it\n" + ChatColor.GRAY + "Vein miner takes your hunger to use it.";
                    case "FortuneIII":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "50%" + ChatColor.GRAY
                                + " chance of getting double ores";
                    case "NightVisionI":
                        return ChatColor.GRAY + "You can now enable night vision using" + ChatColor.AQUA + " /ssnv\n" +
                                ChatColor.GRAY + "Night vision lasts for " + ChatColor.AQUA + "15 " + ChatColor.GRAY + "minutes";
                    case "ArmorI":
                        return ChatColor.GRAY + "You now take " + ChatColor.AQUA + "5%" + ChatColor.GRAY
                                + " less damage from all sources";
                    case "SpelunkerII":
                        return ChatColor.GRAY + "Spelunker now lasts " + ChatColor.AQUA + "15 " + ChatColor.GRAY
                                + "minutes with a cooldown of\n" + ChatColor.AQUA + "30 " + ChatColor.GRAY + "minutes and a radius of "
                                + ChatColor.AQUA + "10 " + ChatColor.GRAY + "blocks";
                    case "VeinMinerII":
                        return ChatColor.GRAY + "Vein miner no longer makes you hungry";
                    case "PeacefulMiner":
                        return ChatColor.GRAY + "Stops mobs from spawning near you below Y-64";
                    case "SpelunkerIII":
                        return ChatColor.GRAY + "Spelunker now lasts " + ChatColor.AQUA + "30 " + ChatColor.GRAY
                                + "minutes with a cooldown of\n" + ChatColor.AQUA + "30 " + ChatColor.GRAY + "minutes and a radius of "
                                + ChatColor.AQUA + "15 " + ChatColor.GRAY + "blocks";
                    case "ArmorII":
                        return ChatColor.GRAY + "You now take " + ChatColor.AQUA + "10%" + ChatColor.GRAY
                                + " less damage from all sources";
                    case "NightVisionII":
                        return ChatColor.GRAY + "Night vision now lasts infinitely";
                    case "ArmorIII":
                        return ChatColor.GRAY + "You now take " + ChatColor.AQUA + "15%" + ChatColor.GRAY
                                + " less damage from all sources";
                    case "UnbreakableTools":
                        return ChatColor.GRAY + "Your tools will never break again";
                    case "ArmorIV":
                        return ChatColor.GRAY + "You now take " + ChatColor.AQUA + "20%" + ChatColor.GRAY
                                + " less damage from all sources";
                }
                break;
            case "Exploring":
                switch (reward) {
                    case "SpeedI":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "20%" + ChatColor.GRAY + " speed boost";
                    case "SpeedII":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "40%" + ChatColor.GRAY + " speed boost";
                    case "SpeedIII":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "60%" + ChatColor.GRAY + " speed boost";
                    case "SpeedIV":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "80%" + ChatColor.GRAY + " speed boost";
                    case "SpeedV":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "100%" + ChatColor.GRAY + " speed boost";
                    case "JumpingBoots":
                    case "CaveFinder":
                    case "WandererArmor":
                    case "TravelerArmor":
                    case "AdventurerArmor":
                    case "GillArmor":
                        return ChatColor.GRAY + "Craftable Item";
                    case "SwimI":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "40%" + ChatColor.GRAY + " swim speed boost";
                    case "SwimII":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "80%" + ChatColor.GRAY + " swim speed boost";
                    case "SwimIII":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "120%" + ChatColor.GRAY + " swim speed boost";
                    case "SwimIV":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "160%" + ChatColor.GRAY + " swim speed boost";
                    case "SwimV":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "200%" + ChatColor.GRAY + " swim speed boost";
                    case "HealthRegen":
                        return ChatColor.GRAY + "You now have permanent health regen";
                    case "FallI":
                        return ChatColor.GRAY + "You now take 25% less fall damage";
                    case "FallII":
                        return ChatColor.GRAY + "You now take 50% less fall damage";
                }
                break;
            case "Farming":
                switch (reward) {
                    case "DoubleCropsI":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "25%" + ChatColor.GRAY
                                + " chance of getting double crops";
                    case "DoubleCropsII":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "50%" + ChatColor.GRAY
                                + " chance of getting double crops";
                    case "DoubleCropsIII":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "75%" + ChatColor.GRAY
                                + " chance of getting double crops";
                    case "DoubleCropsIV":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "100%" + ChatColor.GRAY
                                + " chance of getting double crops";
                    case "Harvester":
                        return ChatColor.GREEN + "Break tons of crops at once and automatically replant them!\n"
                                + ChatColor.GRAY + "See the crafting recipe by using the command" + ChatColor.AQUA
                                + " /skills recipes\n" + ChatColor.GRAY + "Craftable Item";
                    case "UnlimitedBoneMeal":
                        return ChatColor.GRAY + "Craftable Item";
                    case "WateringCan":
                        return ChatColor.GREEN + "Use it to grow crops faster in a 5x5 area!\n"
                                + ChatColor.GRAY + "See the crafting recipe by using the command"
                                + ChatColor.AQUA + " /skills recipes\n" + ChatColor.GRAY + "Craftable Item";
                    case "AutoEat":
                        return ChatColor.GRAY + "Auto Eat will automatically eat food\n" + ChatColor.GRAY
                                + "from your inventory when you are hungry";
                    case "Eat":
                        return ChatColor.GRAY + "You can now feed yourself using " + ChatColor.AQUA + "/sseat";
                    case "NoHunger":
                        return ChatColor.GRAY + "You no longer need to eat food";
                    case "HealthI":
                        return ChatColor.GRAY + "You now have " + ChatColor.AQUA + "11" + ChatColor.GRAY + " hearts";
                    case "HealthII":
                        return ChatColor.GRAY + "You now have " + ChatColor.AQUA + "12" + ChatColor.GRAY + " hearts";
                    case "HealthIII":
                        return ChatColor.GRAY + "You now have " + ChatColor.AQUA + "13" + ChatColor.GRAY + " hearts";
                    case "HealthIV":
                        return ChatColor.GRAY + "You now have " + ChatColor.AQUA + "14" + ChatColor.GRAY + " hearts";
                    case "HealthV":
                        return ChatColor.GRAY + "You now have " + ChatColor.AQUA + "15" + ChatColor.GRAY + " hearts";
                    case "HealthVI":
                        return ChatColor.GRAY + "You now have " + ChatColor.AQUA + "16" + ChatColor.GRAY + " hearts";
                    case "HealthVII":
                        return ChatColor.GRAY + "You now have " + ChatColor.AQUA + "17" + ChatColor.GRAY + " hearts";
                    case "HealthVIII":
                        return ChatColor.GRAY + "You now have " + ChatColor.AQUA + "18" + ChatColor.GRAY + " hearts";
                    case "HealthIX":
                        return ChatColor.GRAY + "You now have " + ChatColor.AQUA + "19" + ChatColor.GRAY + " hearts";
                    case "HealthX":
                        return ChatColor.GRAY + "You now have " + ChatColor.AQUA + "20" + ChatColor.GRAY + " hearts";
                }
                break;
            case "Building":
                switch (reward) {
                    case "BlockReturnI":
                        return ChatColor.GRAY + "You now have a "
                                + ChatColor.AQUA + "5%" + ChatColor.GRAY
                                + " chance of getting\n" + ChatColor.GRAY + "a (building) block back when you place it";
                    case "BlockReturnII":
                        return ChatColor.GRAY + "You now have a "
                                + ChatColor.AQUA + "10%" + ChatColor.GRAY
                                + " chance of getting\n" + ChatColor.GRAY + "a (building) block back when you place it";
                    case "BlockReturnIII":
                        return ChatColor.GRAY + "You now have a "
                                + ChatColor.AQUA + "15%" + ChatColor.GRAY
                                + " chance of getting\n" + ChatColor.GRAY + "a (building) block back when you place it";
                    case "BlockReturnIV":
                        return ChatColor.GRAY + "You now have a "
                                + ChatColor.AQUA + "20%" + ChatColor.GRAY
                                + " chance of getting\n" + ChatColor.GRAY + "a (building) block back when you place it";
                    case "BlockReturnV":
                        return ChatColor.GRAY + "You now have a "
                                + ChatColor.AQUA + "25%" + ChatColor.GRAY
                                + " chance of getting\n" + ChatColor.GRAY + "a (building) block back when you place it";
                    case "BlockReturnVI":
                        return ChatColor.GRAY + "You now have a "
                                + ChatColor.AQUA + "30%" + ChatColor.GRAY
                                + " chance of getting\n" + ChatColor.GRAY + "a (building) block back when you place it";
                    case "BlockReturnVII":
                        return ChatColor.GRAY + "You now have a "
                                + ChatColor.AQUA + "35%" + ChatColor.GRAY
                                + " chance of getting\n" + ChatColor.GRAY + "a (building) block back when you place it";
                    case "BlockReturnVIII":
                        return ChatColor.GRAY + "You now have a "
                                + ChatColor.AQUA + "40%" + ChatColor.GRAY
                                + " chance of getting\n" + ChatColor.GRAY + "a (building) block back when you place it";
                    case "BlockReturnIX":
                        return ChatColor.GRAY + "You now have a "
                                + ChatColor.AQUA + "45%" + ChatColor.GRAY
                                + " chance of getting\n" + ChatColor.GRAY + "a (building) block back when you place it";
                    case "BlockReturnX":
                        return ChatColor.GRAY + "You now have a "
                                + ChatColor.AQUA + "50%" + ChatColor.GRAY
                                + " chance of getting\n" + ChatColor.GRAY + "a (building) block back when you place it";
                    case "FlightI":
                        return ChatColor.GRAY + "You can now use" + ChatColor.AQUA + " /flight" + ChatColor.GRAY
                                + " to fly\n" + ChatColor.GRAY + "for " + ChatColor.AQUA + "5 " + ChatColor.GRAY
                                + "minutes with a cooldown of " + ChatColor.AQUA + "60 " + ChatColor.GRAY + "minutes";
                    case "FlightII":
                        return ChatColor.GRAY + "Flight now lasts for " + ChatColor.AQUA + "15 "
                                + ChatColor.GRAY + "minutes\n" + ChatColor.GRAY + "with a cooldown of " + ChatColor.AQUA
                                + "30 " + ChatColor.GRAY + "minutes";
                    case "FlightIII":
                        return ChatColor.GRAY + "Flight now lasts for " + ChatColor.AQUA + "30 "
                                + ChatColor.GRAY + "minutes\n" + ChatColor.GRAY + "with a cooldown of " + ChatColor.AQUA
                                + "30 " + ChatColor.GRAY + "minutes";
                    case "FlightIV":
                        return ChatColor.GRAY + "Flight no longer has a timer!";
                    case "ExtendedReach":
                        return ChatColor.GRAY + "You can now reach further!";
                    case "AutoSortWand":
                        return ChatColor.GRAY + "Craftable Item";
                }
                break;
            case "Fighting":
                switch (reward) {
                    case "BerserkerI":
                        return ChatColor.GRAY + "You can now enter berserker mode by\n" + ChatColor.GRAY + "sneaking and right clicking while holding a weapon\n" +
                                ChatColor.GRAY + "Berserker mode takes " + ChatColor.AQUA + "25%" + ChatColor.GRAY + " of your" +
                                "health in exchange for " + ChatColor.AQUA + "50%" + ChatColor.GRAY + " more damage\n" +
                                ChatColor.GRAY + "Berserker mode lasts for " + ChatColor.AQUA + "3 "
                                + ChatColor.GRAY + "seconds\n" + ChatColor.GRAY + "with a cooldown of " + ChatColor.AQUA + "120 " + ChatColor.GRAY + "seconds";
                    case "BerserkerII":
                        return ChatColor.GRAY + "Berserker mode now lasts for " + ChatColor.AQUA + "5 "
                                + ChatColor.GRAY + "seconds\n" + ChatColor.GRAY + "with a cooldown of " + ChatColor.AQUA + "90 " + ChatColor.GRAY + "seconds";
                    case "BerserkerIII":
                        return ChatColor.GRAY + "Berserker mode now lasts for " + ChatColor.AQUA + "5 "
                                + ChatColor.GRAY + "seconds\n" + ChatColor.GRAY + "with a cooldown of " + ChatColor.AQUA + "60 " + ChatColor.GRAY + "seconds";
                    case "BerserkerIV":
                        return ChatColor.GRAY + "Berserker mode now lasts for " + ChatColor.AQUA + "5 "
                                + ChatColor.GRAY + "seconds\n" + ChatColor.GRAY + "with a cooldown of " + ChatColor.AQUA + "45 " + ChatColor.GRAY + "seconds";
                    case "BerserkerV":
                        return ChatColor.GRAY + "Berserker mode now lasts for " + ChatColor.AQUA + "8 "
                                + ChatColor.GRAY + "seconds\n" + ChatColor.GRAY + "with a cooldown of " + ChatColor.AQUA + "45 " + ChatColor.GRAY + "seconds";
                    case "BerserkerVI":
                        return ChatColor.GRAY + "Berserker mode now lasts for " + ChatColor.AQUA + "8 "
                                + ChatColor.GRAY + "seconds\n" + ChatColor.GRAY + "with a cooldown of " + ChatColor.AQUA + "30 " + ChatColor.GRAY + "seconds";
                    case "BerserkerVII":
                        return ChatColor.GRAY + "Berserker mode now lasts for " + ChatColor.AQUA + "10 "
                                + ChatColor.GRAY + "seconds\n" + ChatColor.GRAY + "with a cooldown of " + ChatColor.AQUA + "15 " + ChatColor.GRAY + "seconds";
                    case "LifestealI":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "5% " + ChatColor.GRAY
                                + "chance of\n" + ChatColor.GRAY + "stealing health from enemies";
                    case "LifestealII":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "10% " + ChatColor.GRAY
                                + "chance of\n" + ChatColor.GRAY + "stealing health from enemies";
                    case "LifestealIII":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "15% " + ChatColor.GRAY
                                + "chance of\n" + ChatColor.GRAY + "stealing health from enemies";
                    case "LifestealIV":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "20% " + ChatColor.GRAY
                                + "chance of\n" + ChatColor.GRAY + "stealing health from enemies";
                    case "LifestealV":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "25% " + ChatColor.GRAY
                                + "chance of\n" + ChatColor.GRAY + "stealing health from enemies";
                    case "CriticalI":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "10% " + ChatColor.GRAY
                                + "chance of\n" + ChatColor.GRAY + "doing double damage";
                    case "CriticalII":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "20% " + ChatColor.GRAY
                                + "chance of\n" + ChatColor.GRAY + "doing double damage";
                    case "GiantSummon":
                    case "BroodMotherSummon":
                    case "VillagerSummon":
                        return ChatColor.GRAY + "Craftable Item";
                    case "FishingKing":
                        return ChatColor.GRAY + "The Fishing King can spawn while fishing (1/100 chance)";
                    case "MobScanner":
                        return ChatColor.GRAY + "You can now use Mob Scanner by using" + ChatColor.AQUA + " /mobscanner\n" +
                                ChatColor.GRAY + "Mob Scanner will show you where nearby mobs are";
                    case "BloodyDomain":
                        return ChatColor.GRAY + "You can now use Bloody Domain by using" + ChatColor.AQUA + " /togglebloodydomain\n" +
                                ChatColor.GRAY + "Bloody Domain will kill all weak mobs in a 10 block radius";
                }
                break;
            case "Fishing":
                switch (reward) {
                    case "CommonLootI":
                        return ChatColor.GRAY + "You now have a" + ChatColor.AQUA
                                + " 25% " + ChatColor.GRAY + "chance of getting common fishing loot";
                    case "CommonLootII":
                        return ChatColor.GRAY + "You now have a" + ChatColor.AQUA
                                + " 40% " + ChatColor.GRAY + "chance of getting common fishing loot";
                    case "CommonLootIII":
                        return ChatColor.GRAY + "You now have a" + ChatColor.AQUA
                                + " 55% " + ChatColor.GRAY + "chance of getting common fishing loot";
                    case "CommonLootIV":
                        return ChatColor.GRAY + "You now have a" + ChatColor.AQUA
                                + " 65% " + ChatColor.GRAY + "chance of getting common fishing loot";
                    case "CommonLootV":
                        return ChatColor.GRAY + "You now have a" + ChatColor.AQUA
                                + " 75% " + ChatColor.GRAY + "chance of getting common fishing loot";
                    case "RareLootI":
                        return ChatColor.GRAY + "You now have a" + ChatColor.AQUA
                                + " 10% " + ChatColor.GRAY + "chance of getting rare fishing loot";
                    case "RareLootII":
                        return ChatColor.GRAY + "You now have a" + ChatColor.AQUA
                                + " 20% " + ChatColor.GRAY + "chance of getting rare fishing loot";
                    case "RareLootIII":
                        return ChatColor.GRAY + "You now have a" + ChatColor.AQUA
                                + " 30% " + ChatColor.GRAY + "chance of getting rare fishing loot";
                    case "RareLootIV":
                        return ChatColor.GRAY + "You now have a" + ChatColor.AQUA
                                + " 35% " + ChatColor.GRAY + "chance of getting rare fishing loot";

                    case "RareLootV":
                        return ChatColor.GRAY + "You now have a" + ChatColor.AQUA
                                + " 40% " + ChatColor.GRAY + "chance of getting rare fishing loot";
                    case "EpicLootI":
                        return ChatColor.GRAY + "You now have a" + ChatColor.AQUA
                                + " 2% " + ChatColor.GRAY + "chance of getting epic fishing loot";
                    case "EpicLootII":
                        return ChatColor.GRAY + "You now have a" + ChatColor.AQUA
                                + " 3% " + ChatColor.GRAY + "chance of getting epic fishing loot";
                    case "EpicLootIII":
                        return ChatColor.GRAY + "You now have a" + ChatColor.AQUA
                                + " 4% " + ChatColor.GRAY + "chance of getting epic fishing loot";
                    case "EpicLootIV":
                        return ChatColor.GRAY + "You now have a" + ChatColor.AQUA
                                + " 4.5% " + ChatColor.GRAY + "chance of getting epic fishing loot";
                    case "EpicLootV":
                        return ChatColor.GRAY + "You now have a" + ChatColor.AQUA
                                + " 5% " + ChatColor.GRAY + "chance of getting epic fishing loot";
                    case "LegendaryLootI":
                        return ChatColor.GRAY + "You now have a" + ChatColor.AQUA
                                + " 0.5% " + ChatColor.GRAY + "chance of getting legendary fishing loot";
                    case "LegendaryLootII":
                        return ChatColor.GRAY + "You now have a" + ChatColor.AQUA
                                + " 0.75% " + ChatColor.GRAY + "chance of getting legendary fishing loot";
                    case "LegendaryLootIII":
                        return ChatColor.GRAY + "You now have a" + ChatColor.AQUA
                                + " 1% " + ChatColor.GRAY + "chance of getting legendary fishing loot";
                    case "ExperienceI":
                        return ChatColor.GRAY + "You earn" + ChatColor.AQUA
                                + " 10% " + ChatColor.GRAY + "more experience from all sources";
                    case "ExperienceII":
                        return ChatColor.GRAY + "You earn" + ChatColor.AQUA
                                + " 20% " + ChatColor.GRAY + "more experience from all sources";
                    case "ExperienceIII":
                        return ChatColor.GRAY + "You earn" + ChatColor.AQUA
                                + " 30% " + ChatColor.GRAY + "more experience from all sources";
                    case "ExperienceIV":
                        return ChatColor.GRAY + "You earn" + ChatColor.AQUA
                                + " 40% " + ChatColor.GRAY + "more experience from all sources";
                    case "ExperienceV":
                        return ChatColor.GRAY + "You earn" + ChatColor.AQUA
                                + " 50% " + ChatColor.GRAY + "more experience from all sources";
                    case "ExperienceVI":
                        return ChatColor.GRAY + "You earn" + ChatColor.AQUA
                                + " 60% " + ChatColor.GRAY + "more experience from all sources";
                    case "ExperienceVII":
                        return ChatColor.GRAY + "You earn" + ChatColor.AQUA
                                + " 70% " + ChatColor.GRAY + "more experience from all sources";
                    case "ExperienceVIII":
                        return ChatColor.GRAY + "You earn" + ChatColor.AQUA
                                + " 80% " + ChatColor.GRAY + "more experience from all sources";
                    case "ExperienceIX":
                        return ChatColor.GRAY + "You earn" + ChatColor.AQUA
                                + " 90% " + ChatColor.GRAY + "more experience from all sources";
                    case "ExperienceX":
                        return ChatColor.GRAY + "You earn" + ChatColor.AQUA
                                + " 100% " + ChatColor.GRAY + "more experience from all sources";
                    case "FasterFishingI":
                        return ChatColor.GRAY + "Your base fishing speed has been increased";
                    case "FasterFishingII":
                    case "FasterFishingIV":
                    case "FasterFishingIII":
                        return ChatColor.GRAY + "Your base fishing speed has been increased even more";
                    case "FasterFishingV":
                        return ChatColor.GRAY + "Your base fishing speed has been maxed!";
                    case "FishingLineI":
                        return ChatColor.GRAY + "You now get the equivalent of" + ChatColor.AQUA
                                + " 2 " + ChatColor.GRAY + "fishing lines";
                    case "FishingLineII":
                        return ChatColor.GRAY + "You now get the equivalent of" + ChatColor.AQUA
                                + " 3 " + ChatColor.GRAY + "fishing lines";
                    case "FishingLineIII":
                        return ChatColor.GRAY + "You now get the equivalent of" + ChatColor.AQUA
                                + " 5 " + ChatColor.GRAY + "fishing lines";
                    case "FishingLineIV":
                        return ChatColor.GRAY + "You now get the equivalent of" + ChatColor.AQUA
                                + " 7 " + ChatColor.GRAY + "fishing lines";
                    case "FishingLineV":
                        return ChatColor.GRAY + "You now get the equivalent of" + ChatColor.AQUA
                                + " 10 " + ChatColor.GRAY + "fishing lines";
                    case "WaterBreathingI":
                        return ChatColor.GRAY + "You can now use " + ChatColor.AQUA + "/waterbreathing" + ChatColor.GRAY
                                + " to get water breathing\n" + ChatColor.GRAY + "for " + ChatColor.AQUA + "15 "
                                + ChatColor.GRAY + "minutes with a cooldown of " + ChatColor.AQUA + "60 " + ChatColor.GRAY + "minutes";
                    case "WaterBreathingII":
                        return ChatColor.GRAY + "Water Breathing now lasts for " + ChatColor.AQUA + "30 "
                                + ChatColor.GRAY + "minutes\n" + ChatColor.GRAY + "with a cooldown of " + ChatColor.AQUA
                                + "30 " + ChatColor.GRAY + "minutes";
                    case "WaterBreathingIII":
                        return ChatColor.GRAY + "You can now permanently breathe underwater";
                    case "AutoTrashI":
                        return ChatColor.GRAY + "You can now use " + ChatColor.AQUA + "/autotrash" + ChatColor.GRAY
                                + " to automatically trash\n" + ChatColor.GRAY + "items that you pick up. This resets " +
                                "when you leave!";
                    case "AutoTrashII":
                        return ChatColor.GRAY + "Your Auto Trash inventory has increased!";
                    case "PermaTrash":
                        return ChatColor.GRAY + "You can now use " + ChatColor.AQUA + "/permatrash" + ChatColor.GRAY
                                + " to permanently trash\n" + ChatColor.GRAY + "items that you pick up";
                }
                break;
            case "Crafting":
                switch (reward) {
                    case "ExtraOutputI":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "5% " + ChatColor.GRAY
                                + "chance of getting\n" + ChatColor.GRAY + "double the output from crafting recipes";

                    case "ExtraOutputII":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "10% " + ChatColor.GRAY
                                + "chance of getting\n" + ChatColor.GRAY + "double the output from crafting recipes";
                    case "ExtraOutputIII":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "15% " + ChatColor.GRAY
                                + "chance of getting\n" + ChatColor.GRAY + "double the output from crafting recipes";
                    case "ExtraOutputIV":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "20% " + ChatColor.GRAY
                                + "chance of getting\n" + ChatColor.GRAY + "double the output from crafting recipes";
                    case "ExtraOutputV":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "25% " + ChatColor.GRAY
                                + "chance of getting\n" + ChatColor.GRAY + "double the output from crafting recipes";
                    case "ExtraOutputVI":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "30% " + ChatColor.GRAY
                                + "chance of getting\n" + ChatColor.GRAY + "double the output from crafting recipes";
                    case "ExtraOutputVII":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "35% " + ChatColor.GRAY
                                + "chance of getting\n" + ChatColor.GRAY + "double the output from crafting recipes";
                    case "ExtraOutputVIII":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "40% " + ChatColor.GRAY
                                + "chance of getting\n" + ChatColor.GRAY + "double the output from crafting recipes";
                    case "ExtraOutputIX":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "45% " + ChatColor.GRAY
                                + "chance of getting\n" + ChatColor.GRAY + "double the output from crafting recipes";
                    case "ExtraOutputX":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "50% " + ChatColor.GRAY
                                + "chance of getting\n" + ChatColor.GRAY + "double the output from crafting recipes";
                    case "MaterialsBackI":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "10% " + ChatColor.GRAY
                                + "chance of getting\n" + ChatColor.GRAY + "your materials back when crafting";
                    case "MaterialsBackII":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "20% " + ChatColor.GRAY
                                + "chance of getting\n" + ChatColor.GRAY + "your materials back when crafting";
                    case "MaterialsBackIII":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "30% " + ChatColor.GRAY
                                + "chance of getting\n" + ChatColor.GRAY + "your materials back when crafting";
                    case "MaterialsBackIV":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "40% " + ChatColor.GRAY
                                + "chance of getting\n" + ChatColor.GRAY + "your materials back when crafting";
                    case "MaterialsBackV":
                        return ChatColor.GRAY + "You now have a " + ChatColor.AQUA + "50% " + ChatColor.GRAY
                                + "chance of getting\n" + ChatColor.GRAY + "your materials back when crafting";
                    case "EnchantedGapple":
                        return ChatColor.GRAY + "Craftable Item";
                }
                break;
            case "Main":
                switch (reward) {
                    case "DeathLocationTracker":
                        return ChatColor.GRAY + "You can now use" + ChatColor.AQUA + " /deathlocation" + ChatColor.GRAY
                                + " to find where you died";
                    case "FireworkCannon":
                        return ChatColor.GRAY + "Craftable Item";
                    case "SetHomeI":
                        return ChatColor.GRAY + "You can now set " + ChatColor.AQUA + "2 "
                                + ChatColor.GRAY + "homes using " + ChatColor.AQUA + "/sethome";
                    case "SetHomeII":
                        return ChatColor.GRAY + "You can now set " + ChatColor.AQUA + "3 "
                                + ChatColor.GRAY + "homes using " + ChatColor.AQUA + "/sethome";
                    case "SetHomeIII":
                        return ChatColor.GRAY + "You can now set " + ChatColor.AQUA + "4 "
                                + ChatColor.GRAY + "homes using " + ChatColor.AQUA + "/sethome";
                    case "SetHomeIV":
                        return ChatColor.GRAY + "You can now set " + ChatColor.AQUA + "5 "
                                + ChatColor.GRAY + "homes using " + ChatColor.AQUA + "/sethome";
                    case "Gravestone":
                        return ChatColor.GRAY + "Your items will now be held in a chest when you die";
                    case "KeepExperience":
                        return ChatColor.GRAY + "You will now keep your experience when you die";
                    case "KeepInventory":
                        return ChatColor.GRAY + "You will now keep your inventory when you die";
                    case "DustTrail":
                        return ChatColor.GREEN + "Dust particles follow you when you walk\n" + ChatColor.GREEN
                                + "Use " + ChatColor.AQUA + "/toggletrail dust" + ChatColor.GREEN + " to toggle it";
                    case "WaterTrail":
                        return ChatColor.GREEN + "Water particles follow you when you walk\n" + ChatColor.GREEN
                                + "Use " + ChatColor.AQUA + "/toggletrail water" + ChatColor.GREEN + " to toggle it";
                    case "HappyTrail":
                        return ChatColor.GREEN + "Happy particles follow you when you walk\n" + ChatColor.GREEN
                                + "Use " + ChatColor.AQUA + "/toggletrail happy" + ChatColor.GREEN + " to toggle it";
                    case "DragonTrail":
                        return ChatColor.GREEN + "Dragon particles follow you when you walk\n" + ChatColor.GREEN
                                + "Use " + ChatColor.AQUA + "/toggletrail dragon" + ChatColor.GREEN + " to toggle it";
                    case "ElectricTrail":
                        return ChatColor.GREEN + "Electric particles follow you when you walk\n" + ChatColor.GREEN
                                + "Use " + ChatColor.AQUA + "/toggletrail electric" + ChatColor.GREEN + " to toggle it";
                    case "EnchantmentTrail":
                        return ChatColor.GREEN + "Enchantment particles follow you when you walk\n" + ChatColor.GREEN
                                + "Use " + ChatColor.AQUA + "/toggletrail enchantment" + ChatColor.GREEN + " to toggle it";
                    case "OminousTrail":
                        return ChatColor.GREEN + "Ominous particles follow you when you walk\n" + ChatColor.GREEN
                                + "Use " + ChatColor.AQUA + "/toggletrail ominous" + ChatColor.GREEN + " to toggle it";
                    case "LoveTrail":
                        return ChatColor.GREEN + "Love particles follow you when you walk\n" + ChatColor.GREEN
                                + "Use " + ChatColor.AQUA + "/toggletrail love" + ChatColor.GREEN + " to toggle it";
                    case "FlameTrail":
                        return ChatColor.GREEN + "Flame particles follow you when you walk\n" + ChatColor.GREEN
                                + "Use " + ChatColor.AQUA + "/toggletrail flame" + ChatColor.GREEN + " to toggle it";
                    case "BlueFlameTrail":
                        return ChatColor.GREEN + "Blue flame particles follow you when you walk\n" + ChatColor.GREEN
                                + "Use " + ChatColor.AQUA + "/toggletrail blueflame" + ChatColor.GREEN + " to toggle it";
                    case "CherryTrail":
                        return ChatColor.GREEN + "Cherry blossom particles follow you when you walk\n" + ChatColor.GREEN
                                + "Use " + ChatColor.AQUA + "/toggletrail cherry" + ChatColor.GREEN + " to toggle it";
                    case "RainbowTrail":
                        return ChatColor.GREEN + "Rainbow particles follow you when you walk\n" + ChatColor.GREEN
                                + "Use " + ChatColor.AQUA + "/toggletrail rainbow" + ChatColor.GREEN + " to toggle it";
                }
        }
        return "";
    }

    public static ArrayList<String> getLore(String string) {
        String[] split = string.split("\n");
        return new ArrayList<>(Arrays.asList(split));
    }
}
