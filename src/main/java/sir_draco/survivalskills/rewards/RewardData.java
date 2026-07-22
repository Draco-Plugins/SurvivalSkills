package sir_draco.survivalskills.rewards;

import org.bukkit.ChatColor;
import sir_draco.survivalskills.external.providers.TimbermanProvider;

import java.util.*;

public class RewardData {

    public record RewardStyle(ChatColor body, ChatColor highlight) {
        public static final RewardStyle NOTIFY_ABILITY = new RewardStyle(ChatColor.YELLOW, ChatColor.AQUA);
        public static final RewardStyle DESC = new RewardStyle(ChatColor.GRAY, ChatColor.AQUA);
        public static final RewardStyle DESC_TRAIL = new RewardStyle(ChatColor.GREEN, ChatColor.AQUA);
    }

    public record RewardEntry(List<String> notification, String description) {}

    private static final String[] ROMAN = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private static String toRoman(int n) {
        if (n < 1 || n > 10) throw new IllegalArgumentException("Roman numeral out of range: " + n);
        return ROMAN[n - 1];
    }

    private static final ChatColor GREEN = ChatColor.GREEN;
    private static final ChatColor AQUA = ChatColor.AQUA;
    private static final ChatColor GRAY = ChatColor.GRAY;
    private static final ChatColor YELLOW = ChatColor.YELLOW;
    private static final ChatColor LIGHT_PURPLE = ChatColor.LIGHT_PURPLE;
    private static final ChatColor DARK_BLUE = ChatColor.DARK_BLUE;

    private static final String NEW_ITEM = LIGHT_PURPLE.toString() + "[New Item] ";
    private static final String NEW_ABILITY = YELLOW.toString() + "[New Ability] ";
    private static final String NEW_TRAIL = DARK_BLUE.toString() + "[New Trail] ";

    private static final TimbermanProvider TIMBERMAN_PROVIDER = new TimbermanProvider();
    private static final Map<String, Map<String, RewardEntry>> MESSAGE_DATA = buildData();

    public static List<String> getNotification(String type, String reward) {
        var typeMap = MESSAGE_DATA.get(type);
        if (typeMap == null) return null;
        var entry = typeMap.get(reward);
        return entry != null ? entry.notification() : null;
    }

    public static Map<String, Set<String>> getRegisteredRewards() {
        var result = new LinkedHashMap<String, Set<String>>();
        for (var entry : MESSAGE_DATA.entrySet())
            result.put(entry.getKey(), Collections.unmodifiableSet(entry.getValue().keySet()));
        return result;
    }

    public static String getDescription(String type, String reward) {
        var typeMap = MESSAGE_DATA.get(type);
        if (typeMap == null) return "";
        var entry = typeMap.get(reward);
        return entry != null ? entry.description() : "";
    }

    // -----------------------------------------------------------------------
    // Top-level builder
    // -----------------------------------------------------------------------

    private static Map<String, Map<String, RewardEntry>> buildData() {
        var data = new HashMap<String, Map<String, RewardEntry>>();
        data.put("Mining", mining());
        data.put("Exploring", exploring());
        data.put("Farming", farming());
        data.put("Building", building());
        data.put("Fighting", fighting());
        data.put("Fishing", fishing());
        data.put("Crafting", crafting());
        data.put("Main", main());
        return Collections.unmodifiableMap(data);
    }

    // -----------------------------------------------------------------------
    // Per-skill builders
    // -----------------------------------------------------------------------

    private static Map<String, RewardEntry> mining() {
        var m = new HashMap<String, RewardEntry>();

        staticEntry(m, "UnlimitedTorch", lines(
                NEW_ITEM + "You can now craft an unlimited torch",
                GRAY + "See the crafting recipe by using the command" + AQUA + " /skills recipes"),
                GRAY + "Craftable Item");

        tieredPercent(m, "Fortune", map(1, "20%", 2, "40%", 3, "50%"),
                "You now have a {value} chance of getting double ores");

        staticEntry(m, "ToolBelt", lines(
                NEW_ABILITY + "You can now use " + AQUA + "/toolbelt" + YELLOW + " to access your Tool Belt"),
                GRAY + "Use " + AQUA + "/toolbelt" + GRAY + " to access your Tool Belt");

        staticEntry(m, "UpCommand", lines(
                NEW_ABILITY + "You can now use " + AQUA + "/ssup" + YELLOW + " to teleport to the surface"),
                GRAY + "Use " + AQUA + "/ssup" + GRAY + " to teleport to the surface");

        spelunkerEntries(m);
        veinMinerEntries(m);

        staticEntry(m, "NightVisionI", lines(
                NEW_ABILITY + "You can now enable night vision using" + AQUA + " /ssnv",
                GREEN + "\nNight vision lasts for " + AQUA + "15 " + GREEN + "minutes"),
                GRAY + "You can now enable night vision using" + AQUA + " /ssnv\n" +
                        GRAY + "Night vision lasts for " + AQUA + "15 " + GRAY + "minutes");

        staticEntry(m, "NightVisionII", lines(
                NEW_ABILITY + "Night vision now lasts infinitely"),
                GRAY + "Night vision now lasts infinitely");

        tieredPercent(m, "Armor", map(1, "5%", 2, "10%", 3, "15%", 4, "20%"),
                "You now take {value} less damage from all sources");

        staticEntry(m, "MiningArmor", lines(
                NEW_ITEM + "You can now craft Mining Armor",
                GRAY + "See the crafting recipe by using the command" + AQUA + " /skills recipes"),
                GRAY + "Craftable Item");

        staticEntry(m, "PeacefulMiner", lines(
                NEW_ABILITY + "You can now use" + AQUA + " /peacefulminer" + YELLOW
                        + " to stop mobs from spawning while you are below Y = 64"),
                GRAY + "Stops mobs from spawning near you below Y-64");

        staticEntry(m, "UnbreakableTools", lines(
                NEW_ABILITY + "Your tools will never break again"),
                GRAY + "Your tools will never break again");

        staticEntry(m, "ZapWand", lines(
                NEW_ITEM + "You can now craft a Zap Wand",
                GRAY + "See the crafting recipe by using the command" + AQUA + " /skills recipes"),
                GRAY + "Craftable Item");

        staticEntry(m, "BeaconArmor", lines(
                NEW_ITEM + "You can now craft Beacon Armor",
                GREEN + "\nBeacon Armor gives you and those nearby you beacon effects!",
                GRAY + "See the crafting recipe by using the command" + AQUA + " /skills recipes"),
                GRAY + "Craftable Item");

        staticEntry(m, "PowerOre", lines(
                NEW_ITEM + "You can now forge " + LIGHT_PURPLE + "Power Ore",
                GREEN + "\nPlace obsidian down, stand on it, and strike lightning on the block" +
                        " sacrificing 50 levels of experience"),
                GRAY + "You can now forge " + LIGHT_PURPLE + "Power Ore\n" + GRAY
                        + "Place obsidian down, stand on it, and strike lightning on the block and yourself sacrificing 50 levels of experience");

        return m;
    }

    private static Map<String, RewardEntry> exploring() {
        var m = new HashMap<String, RewardEntry>();

        tieredPercent(m, "Speed", map(1, "20%", 2, "40%", 3, "60%", 4, "80%", 5, "100%"),
                "You now have a {value} speed boost");

        staticEntry(m, "JumpingBoots", lines(
                NEW_ITEM + "You can now craft Jumping Boots"),
                GRAY + "Craftable Item");

        staticEntry(m, "Magnet", lines(
                NEW_ITEM + "You can now craft a Magnet",
                GRAY + "See the crafting recipe by using the command" + AQUA + " /skills recipes"),
                GRAY + "Attracts items to you when held in your hand\n"
                        + GRAY + "See the crafting recipe by using the command" + AQUA + " /skills recipes\n"
                        + GRAY + "Craftable Item");

        tieredPercent(m, "Swim", map(1, "40%", 2, "80%", 3, "120%", 4, "160%", 5, "200%"),
                "You now have a {value} swim speed boost");

        staticEntry(m, "WandererArmor", lines(
                NEW_ITEM + "You can now craft Wanderer Armor",
                GREEN + "\nWanderer Armor lets you travel faster!",
                GRAY + "See the crafting recipe by using the command" + AQUA + " /skills recipes"),
                GRAY + "Craftable Item");

        staticEntry(m, "HealthRegen", lines(
                NEW_ABILITY + "You now have permanent health regen"),
                GRAY + "You now have permanent health regen");

        staticEntry(m, "CaveFinder", lines(
                NEW_ITEM + "You can now craft a Cave Finder",
                GREEN + "\nCave Finder shows you where nearby caves are! (Can give false positives with underground dark spots)",
                GRAY + "See the crafting recipe by using the command" + AQUA + " /skills recipes"),
                GRAY + "Craftable Item");

        tieredPercent(m, "Fall", map(1, "25%", 2, "50%"),
                "You now take {value} less fall damage");

        staticEntry(m, "TravelerArmor", lines(
                NEW_ITEM + "You can now craft Traveler Armor",
                GREEN + "\nTraveler Armor lets you travel even faster!",
                GRAY + "See the crafting recipe by using the command" + AQUA + " /skills recipes"),
                GRAY + "Craftable Item");

        staticEntry(m, "AdventurerArmor", lines(
                NEW_ITEM + "You can now craft Adventurer Armor",
                GREEN + "\nAdventurer Armor lets you travel even faster, step up one block automatically, and you won't take fall damage!",
                GRAY + "See the crafting recipe by using the command" + AQUA + " /skills recipes"),
                GRAY + "Craftable Item");

        staticEntry(m, "GillArmor", lines(
                NEW_ITEM + "You can now craft Gill Armor",
                GREEN + "\nGill Armor lets you breathe underwater and swim at hyper speed!",
                GRAY + "See the crafting recipe by using the command" + AQUA + " /skills recipes"),
                GRAY + "Craftable Item");

        return m;
    }

    private static Map<String, RewardEntry> farming() {
        var m = new HashMap<String, RewardEntry>();

        tieredPercent(m, "DoubleCrops", map(1, "25%", 2, "50%", 3, "75%", 4, "100%"),
                "You now have a {value} chance of getting double crops");

        staticEntry(m, "Harvester", lines(
                NEW_ITEM + "You can now craft a Harvester",
                GREEN + "\nBreak tons of crops at once and automatically replant them!",
                GREEN + "See the crafting recipe by using the command" + AQUA + " /skills recipes"),
                GRAY + "Break tons of crops at once and automatically replant them!\n"
                        + GRAY + "See the crafting recipe by using the command" + AQUA
                        + " /skills recipes\n" + GRAY + "Craftable Item");

        staticEntry(m, "UnlimitedBoneMeal", lines(
                NEW_ITEM + "You can now craft an Unlimited Bone Meal",
                GREEN + "\nUse it to instantly grow crops!",
                GRAY + "See the crafting recipe by using the command" + AQUA + " /skills recipes"),
                GRAY + "Craftable Item");

        staticEntry(m, "WateringCan", lines(
                NEW_ITEM + "You can now craft a Watering Can",
                GREEN + "\nUse it to grow crops faster in a 5x5 area!",
                GREEN + "See the crafting recipe by using the command" + AQUA + " /skills recipes"),
                GRAY + "Use it to grow crops faster in a 5x5 area!\n"
                        + GRAY + "See the crafting recipe by using the command"
                        + AQUA + " /skills recipes\n" + GRAY + "Craftable Item");

        staticEntry(m, "AutoEat", lines(
                NEW_ABILITY + "You can now use Auto Eat by using" + AQUA + " /autoeat",
                GREEN + "\nAuto Eat will automatically eat food from your inventory when you are hungry"),
                GRAY + "Auto Eat will automatically eat food\n" + GRAY
                        + "from your inventory when you are hungry");

        staticEntry(m, "Eat", lines(
                NEW_ABILITY + "You can now feed yourself using " + AQUA + "/sseat"),
                GRAY + "You can now feed yourself using " + AQUA + "/sseat");

        staticEntry(m, "NoHunger", lines(
                NEW_ABILITY + "You no longer need to eat food"),
                GRAY + "You no longer need to eat food");

        tieredLinearInt(m, "Health", 10, 11, 1, "",
                "You now have {value} hearts");

        if (TIMBERMAN_PROVIDER.isAvailable()) {
            staticEntry(m, "Timberman", lines(
                    NEW_ABILITY + "You can now crouch to cut down whole trees instantly"),
                    GRAY + "You can now crouch to cut down whole trees instantly");
        }

        return m;
    }

    private static Map<String, RewardEntry> building() {
        var m = new HashMap<String, RewardEntry>();

        tieredLinearInt(m, "BlockReturn", 10, 5, 5, "%",
                "You now have a {value} chance of getting a (building) block back when you place it",
                "You now have a {value} chance of getting\na (building) block back when you place it");

        flightEntries(m);

        staticEntry(m, "ExtendedReach", lines(
                NEW_ABILITY + "You can now reach further!"),
                GRAY + "You can now reach further!");

        staticEntry(m, "AutoSortWand", lines(
                NEW_ITEM + "You can now craft an Auto Sort Wand",
                GREEN + "\nUse it to automatically sort a chests inventory!"),
                GRAY + "Craftable Item");

        staticEntry(m, "BuildersWand", lines(
                NEW_ITEM + "You can now craft a Builder's Wand",
                GREEN + "\nUse it to place matching blocks in groups!"),
                GRAY + "Craftable Item");

        staticEntry(m, "BuildersWandII", lines(
                NEW_ABILITY + "Your Builder's Wand can now use Reinforced mode",
                GREEN + "\nReinforced mode reaches 2 blocks farther and places up to 11 blocks"),
                GRAY + "Unlocks Reinforced mode for the Builder's Wand");

        staticEntry(m, "BuildersWandIII", lines(
                NEW_ABILITY + "Your Builder's Wand can now use Master mode",
                GREEN + "\nMaster mode reaches 4 blocks farther and places up to 21 blocks"),
                GRAY + "Unlocks Master mode for the Builder's Wand");

        return m;
    }

    private static Map<String, RewardEntry> fighting() {
        var m = new HashMap<String, RewardEntry>();

        berserkerEntries(m);

        tieredPercent(m, "Lifesteal", map(1, "5%", 2, "10%", 3, "15%", 4, "20%", 5, "25%"),
                NEW_ABILITY + "You now have a {value} chance of stealing health from enemies",
                "You now have a {value} chance of\nstealing health from enemies");

        tieredPercent(m, "Critical", map(1, "10%", 2, "20%"),
                NEW_ABILITY + "You now have a {value} chance of doing double damage",
                "You now have a {value} chance of\ndoing double damage");

        staticEntry(m, "GiantSummon", lines(
                NEW_ITEM + "You can now craft a Giant Boss Summoning item",
                GRAY + "See the crafting recipe by using the command" + AQUA + " /skills recipes"),
                GRAY + "Craftable Item");

        staticEntry(m, "ElderGuardianSpawnEgg", lines(
                NEW_ITEM + "You can now craft an Elder Guardian Spawn Egg",
                GRAY + "It can only be used underwater inside an Ocean Monument",
                GRAY + "See the crafting recipe by using the command" + AQUA + " /skills recipes"),
                GRAY + "Craftable Item\n" + GRAY
                        + "Can only be used underwater inside an Ocean Monument");

        staticEntry(m, "BroodMotherSummon", lines(
                NEW_ITEM + "You can now craft a Brood Mother Boss Summoning item",
                GRAY + "See the crafting recipe by using the command" + AQUA + " /skills recipes"),
                GRAY + "Craftable Item");

        staticEntry(m, "TogglePhantomSpawns", lines(
                NEW_ABILITY + "You can now use " + AQUA + "/togglephantoms" + YELLOW
                        + " to enable or disable phantom spawns"),
                GRAY + "Use " + AQUA + "/togglephantoms" + GRAY + " to enable or disable phantom spawns");

        staticEntry(m, "WardrobeI", lines(
                NEW_ABILITY + "You can now use " + AQUA + "/wardrobe" + YELLOW
                        + " to store and swap your first armor set"),
                GRAY + "Use " + AQUA + "/wardrobe" + GRAY + " to store and swap armor sets");

        staticEntry(m, "WardrobeII", lines(
                NEW_ABILITY + "Your second armor set is now available in " + AQUA + "/wardrobe"),
                GRAY + "Your second armor set is unlocked in " + AQUA + "/wardrobe");

        staticEntry(m, "WardrobeIII", lines(
                NEW_ABILITY + "Your third armor set is now available in " + AQUA + "/wardrobe"),
                GRAY + "Your third armor set is unlocked in " + AQUA + "/wardrobe");

        staticEntry(m, "FishingKing", lines(
                NEW_ITEM + "You can now summon the Fishing King through fishing when it isn't raining"),
                GRAY + "The Fishing King can spawn while fishing (1/100 chance)");

        staticEntry(m, "VillagerSummon", lines(
                NEW_ITEM + "You can now craft a Villager Boss Summoning item",
                GRAY + "See the crafting recipe by using the command" + AQUA + " /skills recipes"),
                GRAY + "Craftable Item");

        staticEntry(m, "TheExiledOneSummon", lines(
                NEW_ITEM + "You can now craft a The Exiled One Boss Summoning item",
                GRAY + "See the crafting recipe by using the command" + AQUA + " /skills recipes"),
                GRAY + "Craftable Item");

        staticEntry(m, "MobScanner", lines(
                NEW_ITEM + "You can now use Mob Scanner by using" + AQUA + " /mobscanner",
                GREEN + "\nMob Scanner will show you where nearby mobs are"),
                GRAY + "You can now use Mob Scanner by using" + AQUA + " /mobscanner\n" +
                        GRAY + "Mob Scanner will show you where nearby mobs are");

        staticEntry(m, "BloodyDomain", lines(
                NEW_ABILITY + "You can now use Bloody Domain by using" + AQUA + " /togglebloodydomain",
                GREEN + "Bloody Domain will kill all weak mobs within a 10 block radius"),
                GRAY + "You can now use Bloody Domain by using" + AQUA + " /togglebloodydomain\n" +
                        GRAY + "Bloody Domain will kill all weak mobs in a 10 block radius");

        return m;
    }

    private static Map<String, RewardEntry> fishing() {
        var m = new HashMap<String, RewardEntry>();

        tieredValue(m, "CommonLoot", map(1, "25%", 2, "40%", 3, "55%", 4, "65%", 5, "75%"),
                "You now have a {value} chance of getting common fishing loot");

        tieredValue(m, "RareLoot", map(1, "5%", 2, "15%", 3, "20%", 4, "25%", 5, "30%"),
                "You now have a {value} chance of getting rare fishing loot");

        tieredValue(m, "EpicLoot", map(1, "0.5%", 2, "1%", 3, "1.5%", 4, "2%", 5, "2.5%"),
                "You now have a {value} chance of getting epic fishing loot");

        tieredValue(m, "LegendaryLoot", map(1, "0.2%", 2, "0.35%", 3, "0.5%"),
                "You now have a {value} chance of getting legendary fishing loot");

        tieredLinearInt(m, "Experience", 10, 10, 10, "%",
                "You earn {value} more experience from all sources");

        fasterFishingEntries(m);

        tieredValue(m, "FishingLine", map(1, "2", 2, "3", 3, "5", 4, "7", 5, "10"),
                "You now get the equivalent of {value} fishing lines");

        waterBreathingEntries(m);

        staticEntry(m, "AutoTrashI", lines(
                NEW_ABILITY + "You can now use Auto Trash by using" + AQUA + " /autotrash",
                GREEN + "\nAuto Trash will automatically delete items that you pick up that are in the Auto Trash inventory",
                GREEN + "The Auto Trash inventory resets when you leave the server"),
                GRAY + "You can now use " + AQUA + "/autotrash" + GRAY
                        + " to automatically trash\n" + GRAY + "items that you pick up. This resets " +
                        "when you leave!");

        staticEntry(m, "AutoTrashII", lines(
                NEW_ABILITY + "Your Auto Trash inventory has increased in size!"),
                GRAY + "Your Auto Trash inventory has increased!");

        staticEntry(m, "PermaTrashI", lines(
                NEW_ABILITY + "You can now use Perma Trash by using" + AQUA + " /permatrash",
                GREEN + "\nThe Perma Trash inventory saves all items you put in the inventory unless you remove them yourself"),
                GRAY + "You can now use " + AQUA + "/permatrash" + GRAY
                        + " to permanently trash\n" + GRAY + "items that you pick up");

        staticEntry(m, "PermaTrashII", lines(
                NEW_ABILITY + "Your Perma Trash inventory has increased in size!"),
                GRAY + "Your Perma Trash inventory has increased in size!");

        return m;
    }

    private static Map<String, RewardEntry> crafting() {
        var m = new HashMap<String, RewardEntry>();

        tieredLinearInt(m, "ExtraOutput", 10, 5, 5, "%",
                "You now have a {value} chance of getting double the output from crafting recipes",
                "You now have a {value} chance of getting\ndouble the output from crafting recipes",
                List.of(GRAY + "This does not apply to infinitely repeatable recipes " +
                        "(Coal -> Coal Block -> Coal...)"));

        tieredLinearInt(m, "MaterialsBack", 5, 10, 10, "%",
                "You now have a {value} chance of getting your materials back when crafting",
                "You now have a {value} chance of getting\nyour materials back when crafting",
                List.of(GRAY + "This does not apply to infinitely repeatable recipes " +
                        "(Coal -> Coal Block -> Coal...)"));

        staticEntry(m, "ItemTransferPipes", lines(
                NEW_ITEM + "You can now craft and use Item Transfer Pipes",
                GRAY + "See the crafting recipes by using the command" + AQUA + " /skills recipes"),
                GRAY + "Craft and use pipes to transfer items between chests");

        staticEntry(m, "SuperEnchantingTable", lines(
                NEW_ITEM + "You can now craft a Super Enchanting Table",
                GRAY + "See the crafting recipe by using the command" + AQUA + " /skills recipes"),
                GRAY + "Craftable Item used to upgrade enchantments beyond their vanilla limits");

        staticEntry(m, "SpawnerMover", lines(
                NEW_ITEM + "You can now craft a Spawner Mover",
                GRAY + "Pick up and place one spawner while retaining all of its settings",
                GRAY + "See the crafting recipe by using the command" + AQUA + " /skills recipes"),
                GRAY + "Moves one spawner while retaining all of its settings");

        staticEntry(m, "SilkyShears", lines(
                NEW_ITEM + "You can now craft Silky Shears",
                GRAY + "Shear sheep to receive triple wool drops",
                GRAY + "See the crafting recipe by using the command" + AQUA + " /skills recipes"),
                GRAY + "Craftable shears that triple wool drops from sheep");

        staticEntry(m, "EnchantedGapple", lines(
                NEW_ITEM + "You can now craft an Enchanted Golden Apple"),
                GRAY + "Craftable Item");

        return m;
    }

    private static Map<String, RewardEntry> main() {
        var m = new HashMap<String, RewardEntry>();

        staticEntry(m, "DeathLocationTracker", lines(
                NEW_ABILITY + "You can now use" + AQUA + " /deathlocation" + GREEN + " to find where you died"),
                GRAY + "You can now use" + AQUA + " /deathlocation" + GRAY + " to find where you died");

        staticEntry(m, "FireworkCannon", lines(
                NEW_ITEM + "You can now craft a Firework Cannon",
                GREEN + "\nUse it to shoot fireworks!"),
                GRAY + "Craftable Item");

        tieredLinearInt(m, "SetHome", 4, 2, 1, "",
                "You can now set {value} homes using " + AQUA + "/sethome",
                "You can now set {value} homes using " + AQUA + "/sethome");

        staticEntry(m, "Gravestone", lines(
                NEW_ABILITY + "Your items will now be held in a chest when you die"),
                GRAY + "Your items will now be held in a chest when you die");

        staticEntry(m, "KeepExperience", lines(
                NEW_ABILITY + "You will now keep your experience when you die"),
                GRAY + "You will now keep your experience when you die");

        staticEntry(m, "KeepInventory", lines(
                NEW_ABILITY + "You will now keep your inventory when you die"),
                GRAY + "You will now keep your inventory when you die");

        trailEntries(m);

        return m;
    }

    // -----------------------------------------------------------------------
    // Complex family builders
    // -----------------------------------------------------------------------

    private static void spelunkerEntries(Map<String, RewardEntry> m) {
        // Spelunker I
        staticEntry(m, "SpelunkerI", lines(
                NEW_ABILITY + "You can now use" + AQUA + " /spelunker" + GREEN
                        + " to highlight nearby ores and tell you where nearby ores are",
                GREEN + "\nIt currently lasts " + AQUA + "5 " + GREEN
                        + "minutes with a cooldown of " + AQUA + "60 " + GREEN + "minutes and a radius of "
                        + AQUA + "5 " + GREEN + "blocks"),
                GRAY + "You can now use" + AQUA + " /spelunker" + GRAY
                        + " to highlight\n" + GRAY + "nearby ores and tell you where nearby ores are.\n"
                        + GRAY + "It currently lasts " + AQUA + "5 " + GRAY
                        + "minutes with a cooldown of\n" + AQUA + "60 " + GRAY + "minutes and a radius of "
                        + AQUA + "5 " + GRAY + "blocks");

        // Spelunker II
        staticEntry(m, "SpelunkerII", lines(
                NEW_ABILITY + "Spelunker now lasts " + AQUA + "15 " + GREEN
                        + "minutes with a cooldown of " + AQUA + "30 " + GREEN + "minutes and a radius of "
                        + AQUA + "10 " + GREEN + "blocks"),
                GRAY + "Spelunker now lasts " + AQUA + "15 " + GRAY
                        + "minutes with a cooldown of\n" + AQUA + "30 " + GRAY + "minutes and a radius of "
                        + AQUA + "10 " + GRAY + "blocks");

        // Spelunker III
        staticEntry(m, "SpelunkerIII", lines(
                NEW_ABILITY + "Spelunker now lasts " + AQUA + "30 " + GREEN
                        + "minutes with a cooldown of " + AQUA + "30 " + GREEN + "minutes and a radius of "
                        + AQUA + "15 " + GREEN + "blocks"),
                GRAY + "Spelunker now lasts " + AQUA + "30 " + GRAY
                        + "minutes with a cooldown of\n" + AQUA + "30 " + GRAY + "minutes and a radius of "
                        + AQUA + "15 " + GRAY + "blocks");
    }

    private static void veinMinerEntries(Map<String, RewardEntry> m) {
        staticEntry(m, "VeinMinerI", lines(
                NEW_ABILITY + "You can now enable vein miner using" + AQUA + " /veinminer",
                GREEN + "\nSneak to use it",
                GREEN + "Vein miner takes your hunger to use it. " + YELLOW + "Level up mining further to get rid of this drawback"),
                GRAY + "You can now enable vein miner using" + AQUA + " /veinminer\n"
                        + GRAY + "Sneak to use it\n" + GRAY + "Vein miner takes your hunger to use it.");

        staticEntry(m, "VeinMinerII", lines(
                NEW_ABILITY + "Vein miner no longer makes you hungry"),
                GRAY + "Vein miner no longer makes you hungry");
    }

    private static void flightEntries(Map<String, RewardEntry> m) {
        staticEntry(m, "FlightI", lines(
                NEW_ABILITY + "You can now use" + AQUA + " /flight" + GREEN
                        + " to fly for " + AQUA + "5 " + GREEN + "minutes with a cooldown of " + AQUA
                        + "60 " + GREEN + "minutes"),
                GRAY + "You can now use" + AQUA + " /flight" + GRAY
                        + " to fly\n" + GRAY + "for " + AQUA + "5 " + GRAY
                        + "minutes with a cooldown of " + AQUA + "60 " + GRAY + "minutes");

        staticEntry(m, "FlightII", lines(
                NEW_ABILITY + "Flight now lasts for " + AQUA + "15 "
                        + GREEN + "minutes with a cooldown of " + AQUA
                        + "30 " + GREEN + "minutes"),
                GRAY + "Flight now lasts for " + AQUA + "15 "
                        + GRAY + "minutes\n" + GRAY + "with a cooldown of " + AQUA
                        + "30 " + GRAY + "minutes");

        staticEntry(m, "FlightIII", lines(
                NEW_ABILITY + "Flight now lasts for " + AQUA + "30 "
                        + GREEN + "minutes with a cooldown of " + AQUA
                        + "30 " + GREEN + "minutes"),
                GRAY + "Flight now lasts for " + AQUA + "30 "
                        + GRAY + "minutes\n" + GRAY + "with a cooldown of " + AQUA
                        + "30 " + GRAY + "minutes");

        staticEntry(m, "FlightIV", lines(
                NEW_ABILITY + "Flight no longer has a timer!"),
                GRAY + "Flight no longer has a timer!");
    }

    private static void berserkerEntries(Map<String, RewardEntry> m) {
        // Berserker I (unique extra description text)
        staticEntry(m, "BerserkerI", lines(
                NEW_ABILITY + "You can now enter berserker mode by sneaking and right clicking while holding a weapon",
                GREEN + "\nBerserker mode lasts for " + AQUA + "3 "
                        + GREEN + "seconds with a cooldown of " + AQUA + "120 " + GREEN + "seconds"),
                GRAY + "You can now enter berserker mode by\n" + GRAY + "sneaking and right clicking while holding a weapon\n" +
                        GRAY + "Berserker mode takes " + AQUA + "25%" + GRAY + " of your" +
                        "health in exchange for " + AQUA + "50%" + GRAY + " more damage\n" +
                        GRAY + "Berserker mode lasts for " + AQUA + "3 "
                        + GRAY + "seconds\n" + GRAY + "with a cooldown of " + AQUA + "120 " + GRAY + "seconds");

        staticEntry(m, "BerserkerII", lines(
                NEW_ABILITY + "Berserker mode now lasts for " + AQUA + "5 "
                        + GREEN + "seconds with a cooldown of " + AQUA + "90 " + GREEN + "seconds"),
                GRAY + "Berserker mode now lasts for " + AQUA + "5 "
                        + GRAY + "seconds\n" + GRAY + "with a cooldown of " + AQUA + "90 " + GRAY + "seconds");

        staticEntry(m, "BerserkerIII", lines(
                NEW_ABILITY + "Berserker mode now lasts for " + AQUA + "5 "
                        + GREEN + "seconds with a cooldown of " + AQUA + "60 " + GREEN + "seconds"),
                GRAY + "Berserker mode now lasts for " + AQUA + "5 "
                        + GRAY + "seconds\n" + GRAY + "with a cooldown of " + AQUA + "60 " + GRAY + "seconds");

        staticEntry(m, "BerserkerIV", lines(
                NEW_ABILITY + "Berserker mode now lasts for " + AQUA + "5 "
                        + GREEN + "seconds with a cooldown of " + AQUA + "45 " + GREEN + "seconds"),
                GRAY + "Berserker mode now lasts for " + AQUA + "5 "
                        + GRAY + "seconds\n" + GRAY + "with a cooldown of " + AQUA + "45 " + GRAY + "seconds");

        staticEntry(m, "BerserkerV", lines(
                NEW_ABILITY + "Berserker mode now lasts for " + AQUA + "8 "
                        + GREEN + "seconds with a cooldown of " + AQUA + "45 " + GREEN + "seconds"),
                GRAY + "Berserker mode now lasts for " + AQUA + "8 "
                        + GRAY + "seconds\n" + GRAY + "with a cooldown of " + AQUA + "45 " + GRAY + "seconds");

        staticEntry(m, "BerserkerVI", lines(
                NEW_ABILITY + "Berserker mode now lasts for " + AQUA + "8 "
                        + GREEN + "seconds with a cooldown of " + AQUA + "30 " + GREEN + "seconds"),
                GRAY + "Berserker mode now lasts for " + AQUA + "8 "
                        + GRAY + "seconds\n" + GRAY + "with a cooldown of " + AQUA + "30 " + GRAY + "seconds");

        staticEntry(m, "BerserkerVII", lines(
                NEW_ABILITY + "Berserker mode now lasts for " + AQUA + "10 "
                        + GREEN + "seconds with a cooldown of " + AQUA + "15 " + GREEN + "seconds"),
                GRAY + "Berserker mode now lasts for " + AQUA + "10 "
                        + GRAY + "seconds\n" + GRAY + "with a cooldown of " + AQUA + "15 " + GRAY + "seconds");
    }

    private static void fasterFishingEntries(Map<String, RewardEntry> m) {
        staticEntry(m, "FasterFishingI", lines(
                NEW_ABILITY + "Your base fishing speed has been increased"),
                GRAY + "Your base fishing speed has been increased");

        // II, III, IV share the same message
        groupStaticEntry(m, List.of("FasterFishingII", "FasterFishingIII", "FasterFishingIV"),
                lines(NEW_ABILITY + "Your base fishing speed has been increased even more"),
                GRAY + "Your base fishing speed has been increased even more");

        staticEntry(m, "FasterFishingV", lines(
                NEW_ABILITY + "Your base fishing speed has been maxed!"),
                GRAY + "Your base fishing speed has been maxed!");
    }

    private static void waterBreathingEntries(Map<String, RewardEntry> m) {
        staticEntry(m, "WaterBreathingI", lines(
                NEW_ABILITY + "You can now use " + AQUA + "/waterbreathing" + GREEN
                        + " to get water breathing for " + AQUA + "15 " + GREEN + "minutes with a cooldown of " + AQUA
                        + "60 " + GREEN + "minutes"),
                GRAY + "You can now use " + AQUA + "/waterbreathing" + GRAY
                        + " to get water breathing\n" + GRAY + "for " + AQUA + "15 "
                        + GRAY + "minutes with a cooldown of " + AQUA + "60 " + GRAY + "minutes");

        staticEntry(m, "WaterBreathingII", lines(
                NEW_ABILITY + "Water Breathing now lasts for " + AQUA + "30 "
                        + GREEN + "minutes with a cooldown of " + AQUA
                        + "30 " + GREEN + "minutes"),
                GRAY + "Water Breathing now lasts for " + AQUA + "30 "
                        + GRAY + "minutes\n" + GRAY + "with a cooldown of " + AQUA
                        + "30 " + GRAY + "minutes");

        staticEntry(m, "WaterBreathingIII", lines(
                NEW_ABILITY + "You can now permanently breathe underwater"),
                GRAY + "You can now permanently breathe underwater");
    }

    private static void trailEntries(Map<String, RewardEntry> m) {
        trail(m, "DustTrail", "Dust", "dust");
        trail(m, "WaterTrail", "Water", "water");
        trail(m, "HappyTrail", "Happy", "happy");
        trail(m, "DragonTrail", "Dragon", "dragon");
        trail(m, "ElectricTrail", "Electric", "electric");
        trail(m, "EnchantmentTrail", "Enchantment", "enchantment");
        trail(m, "OminousTrail", "Ominous", "ominous");
        trail(m, "LoveTrail", "Love", "love");
        trail(m, "FlameTrail", "Flame", "flame");
        trail(m, "BlueFlameTrail", "Blue flame", "blueflame");
        trail(m, "CherryTrail", "Cherry blossom", "cherry");
        trail(m, "RainbowTrail", "Rainbow", "rainbow");
    }

    // -----------------------------------------------------------------------
    // Generic helper methods
    // -----------------------------------------------------------------------

    private static RewardEntry entry(List<String> notification, String description) {
        return new RewardEntry(notification, description);
    }

    private static List<String> lines(String... lines) {
        return List.of(lines);
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> map(Object... entries) {
        if (entries.length % 2 != 0) throw new IllegalArgumentException("Odd number of entries");
        var map = new LinkedHashMap<K, V>();
        for (int i = 0; i < entries.length; i += 2)
            map.put((K) entries[i], (V) entries[i + 1]);
        return map;
    }

    private static void staticEntry(Map<String, RewardEntry> m, String name, List<String> notif, String desc) {
        m.put(name, entry(notif, desc));
    }

    private static void groupStaticEntry(Map<String, RewardEntry> m, List<String> names, List<String> notif, String desc) {
        var e = entry(notif, desc);
        for (var name : names) m.put(name, e);
    }

    // -----------------------------------------------------------------------
    // Template helpers
    // -----------------------------------------------------------------------

    private static String fillValue(String template, String value, RewardStyle style) {
        return template.replace("{value}", style.highlight() + value + style.body());
    }

    private static String multilineDesc(String descTemplate, ChatColor bodyColor) {
        return bodyColor.toString() + descTemplate.replace("\n", "\n" + bodyColor.toString());
    }

    private static void tieredPercent(Map<String, RewardEntry> m, String prefix, Map<Integer, String> levelValues, String template) {
        tieredPercent(m, prefix, levelValues, NEW_ABILITY + template, template);
    }

    private static void tieredPercent(Map<String, RewardEntry> m, String prefix, Map<Integer, String> levelValues, String notifTemplate, String descTemplate) {
        for (var entry : levelValues.entrySet()) {
            String name = prefix + toRoman(entry.getKey());
            String notif = fillValue(notifTemplate, entry.getValue(), RewardStyle.NOTIFY_ABILITY);
            String desc = fillValue(multilineDesc(descTemplate, RewardStyle.DESC.body()), entry.getValue(), RewardStyle.DESC);
            m.put(name, entry(List.of(notif), desc));
        }
    }

    private static void tieredLinearInt(Map<String, RewardEntry> m, String prefix,
                                         int maxLevel, int start, int step, String suffix,
                                         String template) {
        tieredLinearInt(m, prefix, maxLevel, start, step, suffix, template, template, List.of());
    }

    private static void tieredLinearInt(Map<String, RewardEntry> m, String prefix,
                                         int maxLevel, int start, int step, String suffix,
                                         String notifTemplate, String descTemplate) {
        tieredLinearInt(m, prefix, maxLevel, start, step, suffix, notifTemplate, descTemplate, List.of());
    }

    private static void tieredLinearInt(Map<String, RewardEntry> m, String prefix,
                                         int maxLevel, int start, int step, String suffix,
                                         String notifTemplate, String descTemplate,
                                         List<String> extraNotifLines) {
        for (int level = 1; level <= maxLevel; level++) {
            String name = prefix + toRoman(level);
            String value = (start + (level - 1) * step) + suffix;
            String notif = fillValue(NEW_ABILITY + notifTemplate, value, RewardStyle.NOTIFY_ABILITY);
            String desc = fillValue(multilineDesc(descTemplate, RewardStyle.DESC.body()), value, RewardStyle.DESC);
            List<String> notifLines = new ArrayList<>();
            notifLines.add(notif);
            notifLines.addAll(extraNotifLines);
            m.put(name, entry(notifLines, desc));
        }
    }

    private static void tieredValue(Map<String, RewardEntry> m, String prefix,
                                     Map<Integer, String> levelValues, String template) {
        tieredValue(m, prefix, levelValues, template, template);
    }

    private static void tieredValue(Map<String, RewardEntry> m, String prefix,
                                     Map<Integer, String> levelValues,
                                     String notifTemplate, String descTemplate) {
        for (var entry : levelValues.entrySet()) {
            String name = prefix + toRoman(entry.getKey());
            String notif = fillValue(NEW_ABILITY + notifTemplate, entry.getValue(), RewardStyle.NOTIFY_ABILITY);
            String desc = fillValue(multilineDesc(descTemplate, RewardStyle.DESC.body()), entry.getValue(), RewardStyle.DESC);
            m.put(name, entry(List.of(notif), desc));
        }
    }

    private static void trail(Map<String, RewardEntry> m, String rewardName, String displayName, String commandName) {
        String notif1 = NEW_TRAIL + "You have unlocked the " + LIGHT_PURPLE + displayName + DARK_BLUE + " trail";
        String notif2 = GREEN + "\nUse " + AQUA + "/toggletrail " + commandName + GREEN + " to toggle it";
        String desc = GRAY + displayName + " particles follow you when you walk\n"
                + GRAY + "Use " + AQUA + "/toggletrail " + commandName + GRAY + " to toggle it";
        m.put(rewardName, entry(List.of(notif1, notif2), desc));
    }

}
