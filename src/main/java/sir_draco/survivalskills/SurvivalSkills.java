package sir_draco.survivalskills;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.*;
import sir_draco.survivalskills.Abilities.AbilityManager;
import sir_draco.survivalskills.Abilities.AutoTrash;
import sir_draco.survivalskills.Abilities.TrailEffect;
import sir_draco.survivalskills.Commands.*;
import sir_draco.survivalskills.Boards.Leaderboard;
import sir_draco.survivalskills.Boards.LeaderboardPlayer;
import sir_draco.survivalskills.Boards.SkillScoreboard;
import sir_draco.survivalskills.SkillListeners.*;
import sir_draco.survivalskills.Skills.Skill;
import sir_draco.survivalskills.Skills.SkillManager;
import sir_draco.survivalskills.Trophy.Trophy;
import sir_draco.survivalskills.Trophy.TrophyListener;
import sir_draco.survivalskills.Trophy.TrophyManager;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class SurvivalSkills extends JavaPlugin {

    private final HashMap<UUID, Boolean> toggledScoreboard = new HashMap<>();
    private final HashMap<Player, Scoreboard> scoreboardTracker = new HashMap<>();
    private final HashMap<UUID, LeaderboardPlayer> leaderboardTracker = new HashMap<>();
    private final ArrayList<Material> farmingList = new ArrayList<>();
    private final ArrayList<NamespacedKey> recipeKeys = new ArrayList<>();

    // Managers
    private AbilityManager abilityManager;
    private SkillManager skillManager;
    private TrophyManager trophyManager;

    // Listeners
    private BuildingSkill buildingListener;
    private MiningSkill miningListener;
    private FishingSkill fishingListener;
    private ExploringSkill exploringListener;
    private FarmingSkill farmingListener;
    private FightingSkill fightingListener;
    private CraftingSkill craftingListener;
    private MainSkill mainListener;
    private PlayerListener playerListener;

    // Configs
    private FileConfiguration config;
    private File trophyFile;
    private FileConfiguration trophyData;
    private File leaderboardFile;
    private FileConfiguration leaderboardData;
    private File permaTrashFile;
    private FileConfiguration permaTrashData;
    private File toolBeltFile;
    private FileConfiguration toolBeltData;

    private boolean woolRecipes = false;
    private boolean griefPreventionEnabled = false;
    private boolean worldGuardEnabled = false;
    private RegionContainer container = null;

    @Override
    public void onEnable() {
        // Make sure there are no stragglers from before
        World world = Bukkit.getWorld("world");
        if (world != null) {
            for (Entity ent : world.getEntities()) {
                if (!ent.getType().equals(EntityType.ITEM)) continue;
                Item item = (Item) ent;
                if (item.getOwner() == null) continue;
                if (item.getOwner().equals(UUID.fromString("00000000-0000-0000-0000-000000000000"))) item.remove();
            }
        }

        // See if the config has ever been saved before
        saveDefaultConfig();
        File configFile = new File(getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);

        // See if an update needs to be made to the config
        if (config.get("Version") == null || config.getDouble("Version") != 1.942) updateConfig();
        skillManager = new SkillManager(this);

        trophyFile = new File(getDataFolder(), "trophydata.yml");
        if (!trophyFile.exists()) saveResource("trophydata.yml", true);
        trophyData = YamlConfiguration.loadConfiguration(trophyFile);

        leaderboardFile = new File(getDataFolder(), "leaderboard.yml");
        if (!leaderboardFile.exists()) saveResource("leaderboard.yml", true);
        leaderboardData = YamlConfiguration.loadConfiguration(leaderboardFile);
        loadLeaderboard();

        permaTrashFile = new File(getDataFolder(), "permatrash.yml");
        if (!permaTrashFile.exists()) saveResource("permatrash.yml", true);
        permaTrashData = YamlConfiguration.loadConfiguration(permaTrashFile);

        toolBeltFile = new File(getDataFolder(), "toolbelt.yml");
        if (!toolBeltFile.exists()) saveResource("toolbelt.yml", true);
        toolBeltData = YamlConfiguration.loadConfiguration(toolBeltFile);

        // Load plugin features
        loadListeners();
        trophyManager = new TrophyManager(this);
        RecipeMaker.trophyRecipes(this);
        RecipeMaker.rewardRecipes(this);
        abilityManager = new AbilityManager(this);
        loadCommands();

        // If the plugin is reloaded without a restart
        if (!getServer().getOnlinePlayers().isEmpty())
            for (Player p : getServer().getOnlinePlayers()) playerJoin(p, true);

        if (getServer().getPluginManager().getPlugin("GriefPrevention") != null) griefPreventionEnabled = true;
        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardEnabled = true;
            container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        }
    }

    @Override
    public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.closeInventory();
            savePermaTrash(p);
        }

        try {
            savePlayerData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            trophyManager.saveTrophies();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            mainListener.saveGraves();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            saveLeaderboard();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        abilityManager.saveToolBelts();

        getMiningListener().endSpelunkerAll();
    }

    public void loadCommands() {
        // Default Player Commands
        new AutoEatCommand(this);
        new AutoTrashCommand(this);
        new DeathLocationCommand(this);
        new DeathReturnCommand(this);
        new EatCommand(this);
        new FlightCommand(this);
        new MobScannerCommand(this);
        new NightVisionCommand(this);
        new PeacefulMinerCommand(this);
        new PermaTrashCommand(this);
        new SkillStatsCommand(this);
        new SpelunkerCommand(this);
        new ToggleMaxSkillMessageCommand(this);
        new TogglePhantomsCommand(this);
        new ToggleScoreboardCommand(this);
        new ToggleSpeedCommand(this);
        new ToggleTrailCommand(this);
        new ToolBeltCommand(this);
        new VeinminerCommand(this);
        new WaterBreathingCommand(this);

        // Admin Commands
        new BossCommand(this);
        new BossMusicCommand(this);
        new CaveFinderCommand(this);
        new GetTrophyCommand(this);
        new ResetFirstDragon(this);
        new SkillsMultiplierCommand(this);
        new SurvivalSkillsCommand(this);
        new SurvivalSkillsGetCommand(this);
    }

    public void loadLeaderboard() {
        ConfigurationSection section = leaderboardData.getConfigurationSection("");
        if (section == null) return;
        section.getKeys(false).forEach(key -> {
            String name = leaderboardData.getString(key + ".Name");
            int level = leaderboardData.getInt(key + ".Level");
            int building = leaderboardData.getInt(key + ".Building");
            int mining = leaderboardData.getInt(key + ".Mining");
            int fishing = leaderboardData.getInt(key + ".Fishing");
            int exploring = leaderboardData.getInt(key + ".Exploring");
            int farming = leaderboardData.getInt(key + ".Farming");
            int fighting = leaderboardData.getInt(key + ".Fighting");
            int crafting = leaderboardData.getInt(key + ".Crafting");
            int main = leaderboardData.getInt(key + ".Main");
            int deaths = leaderboardData.getInt(key + ".Deaths");
            LeaderboardPlayer leaderboard = new LeaderboardPlayer(name, level, building, mining, fishing, exploring,
                    farming, fighting, crafting, main, deaths);
            leaderboardTracker.put(UUID.fromString(key), leaderboard);
        });
    }

    public void loadPermaTrash(Player p) {
        UUID uuid = p.getUniqueId();
        ConfigurationSection perma = permaTrashData.getConfigurationSection(uuid.toString());
        if (perma == null) return;

        ConfigurationSection materials = permaTrashData.getConfigurationSection(uuid + ".Materials");
        boolean big = false;
        if (permaTrashData.contains(uuid + ".BigTrash")) big = permaTrashData.getBoolean(uuid + ".BigTrash");
        AutoTrash trash = new AutoTrash(big);
        if (materials != null) {
            // get the list of materials from the config
            materials.getKeys(false).forEach(key -> {
                String type = permaTrashData.getString(uuid + ".Materials." + key);
                if (type != null) {
                    Material material = Material.getMaterial(type);
                    if (material == null) {
                        Bukkit.getLogger().warning("Material " + key + " for " + uuid + " is not valid");
                        return;
                    }
                    trash.addTrashItem(new ItemStack(material));
                }
                else Bukkit.getLogger().warning("Material " + key + " for " + uuid + " is not valid");
            });
        }

        ConfigurationSection enchants = permaTrashData.getConfigurationSection(uuid + ".Enchants");
        if (enchants != null) {
            ArrayList<String> keyNames = new ArrayList<>();
            // get the list of items from the config
            enchants.getKeys(false).forEach(key -> {
                String keyName = permaTrashData.getString(uuid + ".Enchants." + key);
                keyNames.add(keyName);
            });

            for (String key : keyNames) {
                Enchantment enchant = getEnchantFromKey(key);
                if (enchant == null) {
                    Bukkit.getLogger().warning("Enchantment " + key + " is not valid");
                    return;
                }

                ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
                EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
                if (meta == null) return;
                meta.addStoredEnchant(enchant, 1, false);
                item.setItemMeta(meta);
                trash.addTrashItem(item);
            }
        }

        getFishingListener().getPermaTrash().put(p, trash);
    }

    public void loadListeners() {
        // Keep farming list global for use by multiple listeners
        createFarmingList();

        buildingListener = new BuildingSkill(this);
        miningListener = new MiningSkill(this, config.getInt("VeinMinerHungerAmount"));
        fishingListener = new FishingSkill(this);
        exploringListener = new ExploringSkill(this);
        farmingListener = new FarmingSkill(this);
        fightingListener = new FightingSkill(this);
        craftingListener = new CraftingSkill(this);
        mainListener = new MainSkill(this);
        playerListener = new PlayerListener(this);
        TrophyListener trophyListener = new TrophyListener(this);
        TabCompleter tabCompleter = new TabCompleter(this);

        getServer().getPluginManager().registerEvents(buildingListener, this);
        getServer().getPluginManager().registerEvents(miningListener, this);
        getServer().getPluginManager().registerEvents(fishingListener, this);
        getServer().getPluginManager().registerEvents(exploringListener, this);
        getServer().getPluginManager().registerEvents(farmingListener, this);
        getServer().getPluginManager().registerEvents(fightingListener, this);
        getServer().getPluginManager().registerEvents(craftingListener, this);
        getServer().getPluginManager().registerEvents(mainListener, this);
        getServer().getPluginManager().registerEvents(playerListener, this);
        getServer().getPluginManager().registerEvents(trophyListener, this);
        getServer().getPluginManager().registerEvents(tabCompleter, this);
    }

    public void loadData(Player p, FileConfiguration data) {
        HashMap<String, Boolean> trophyList = new HashMap<>();
        if (!data.contains(p.getUniqueId().toString())) {
            ArrayList<Skill> skills = new ArrayList<>();
            trophyList.put("CaveTrophy", false);
            trophyList.put("ForestTrophy", false);
            trophyList.put("FarmingTrophy", false);
            trophyList.put("OceanTrophy", false);
            trophyList.put("FishingTrophy", false);
            trophyList.put("ColorTrophy", false);
            trophyList.put("NetherTrophy", false);
            trophyList.put("EndTrophy", false);
            trophyList.put("ChampionTrophy", false);
            trophyList.put("GodTrophy", false);

            skills.add(new Skill(0, 1, "Main"));
            skills.add(new Skill(0, 1, "Building"));
            skills.add(new Skill(0, 1, "Mining"));
            skills.add(new Skill(0, 1, "Fishing"));
            skills.add(new Skill(0, 1, "Exploring"));
            skills.add(new Skill(0, 1, "Farming"));
            skills.add(new Skill(0, 1, "Fighting"));
            skills.add(new Skill(0, 1, "Crafting"));
            trophyManager.getTrophyTracker().put(p.getUniqueId(), trophyList);
            skillManager.getPlayerSkills().put(p.getUniqueId(), skills);
            skillManager.getMaxSkillMessage().put(p, true);
            toggledScoreboard.put(p.getUniqueId(), true);

            savePlayerData(p);
            return;
        }

        UUID uuid = p.getUniqueId();
        if (!skillManager.getPlayerSkills().containsKey(uuid)) skillManager.loadPlayerSkills(uuid, data);
        if (!toggledScoreboard.containsKey(uuid)) loadScoreboardSetting(uuid, data);
        if (!trophyManager.getTrophyTracker().containsKey(uuid)) trophyManager.loadPlayerTrophies(uuid, data);

        if (data.contains(uuid + ".NoPhantoms")) {
            boolean phantoms = data.getBoolean(uuid + ".NoPhantoms");
            if (phantoms && !getFightingListener().getNoPhantomSpawns().contains(p)) {
                getFightingListener().getNoPhantomSpawns().add(p);
            }
        }

        if (data.contains(uuid + ".Trail")) {
            String trailName = data.getString(uuid + ".Trail");
            if (trailName != null && !trailName.equals("None")) {
                if (abilityManager.getTrails().containsKey(trailName)) {
                    int dustType = 1;
                    if (trailName.equalsIgnoreCase("Dust")) dustType = 2;
                    else if (trailName.equalsIgnoreCase("Rainbow")) dustType = 3;
                    TrailEffect effect = new TrailEffect(p, abilityManager.getTrails().get(trailName), dustType, trailName);
                    effect.runTaskTimer(this, 60, 1);
                    abilityManager.getTrailTracker().put(p, effect);
                }
            }
        }

        if (data.contains(uuid + ".AutoEat")) {
            boolean autoEat = data.getBoolean(uuid + ".AutoEat");
            if (autoEat && !farmingListener.getAutoEat().contains(p)) farmingListener.getAutoEat().add(p);
        }

        if (data.contains(uuid + ".Veinminer")) {
            int veinminer = data.getInt(uuid + ".Veinminer");
            if (veinminer == 0 || veinminer == 1) miningListener.getVeinminerTracker().put(p, veinminer);
        }

        if (data.contains(uuid + ".PeacefulMiner")) {
            boolean peacefulMiner = data.getBoolean(uuid + ".PeacefulMiner");
            if (peacefulMiner && !miningListener.getPeacefulMiners().contains(p)) miningListener.getPeacefulMiners().add(p);
        }

        if (data.contains(uuid + ".MaxSkillMessage")) {
            boolean maxSkillMessage = data.getBoolean(uuid + ".MaxSkillMessage");
            skillManager.getMaxSkillMessage().put(p, maxSkillMessage);
        }
    }

    public void loadScoreboardSetting(UUID uuid, FileConfiguration data) {
        if (toggledScoreboard.containsKey(uuid)) return;
        if (data.contains(uuid + ".Scoreboard")) {
            toggledScoreboard.put(uuid, true);
            return;
        }
        toggledScoreboard.put(uuid, data.getBoolean(uuid + ".Scoreboard"));
    }

    public void savePlayerData(Player p) {
        if (skillManager.getPlayerSkills().isEmpty()) return;

        File dataFile = new File(getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) saveResource("playerdata.yml", true);
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);

        UUID uuid = p.getUniqueId();
        trophyManager.savePlayerTrophyData(uuid, data);

        if (toggledScoreboard.containsKey(uuid)) data.set(uuid + ".Scoreboard", toggledScoreboard.get(uuid));
        else Bukkit.getLogger().warning("Player " + p.getName() + " does not have a scoreboard status");

        if (getFightingListener().getNoPhantomSpawns().contains(p)) data.set(uuid + ".NoPhantoms", true);
        else data.set(uuid + ".NoPhantoms", false);

        if (abilityManager.getTrailTracker().containsKey(p))
            data.set(uuid + ".Trail", abilityManager.getTrailTracker().get(p).getTrailName());
        else data.set(uuid + ".Trail", "None");

        if (farmingListener.getAutoEat().contains(p)) data.set(uuid + ".AutoEat", true);
        else data.set(uuid + ".AutoEat", false);

        data.set(uuid + ".Veinminer", miningListener.getVeinminerTracker().getOrDefault(p, -1));

        if (miningListener.getPeacefulMiners().contains(p)) data.set(uuid + ".PeacefulMiner", true);
        else data.set(uuid + ".PeacefulMiner", false);

        skillManager.savePlayerSkillData(uuid, data);
        savePermaTrash(p);

        try {
            data.save(dataFile);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void savePlayerData() throws IOException {
        if (skillManager.getPlayerSkills().isEmpty()) return;

        File dataFile = new File(getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) saveResource("playerdata.yml", true);
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);

        trophyManager.saveTrophyData(data);
        skillManager.saveSkillData(data);

        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID uuid = p.getUniqueId();
            if (toggledScoreboard.containsKey(uuid)) data.set(uuid + ".Scoreboard", toggledScoreboard.get(uuid));
            else Bukkit.getLogger().warning("Player " + p.getName() + " does not have a scoreboard status");

            if (fightingListener.getNoPhantomSpawns().contains(p)) data.set(uuid + ".NoPhantoms", true);
            else data.set(uuid + ".NoPhantoms", false);

            if (abilityManager.getTrailTracker().containsKey(p))
                data.set(uuid + ".Trail", abilityManager.getTrailTracker().get(p).getTrailName());
            else data.set(uuid + ".Trail", "None");

            if (farmingListener.getAutoEat().contains(p)) data.set(uuid + ".AutoEat", true);
            else data.set(uuid + ".AutoEat", false);

            data.set(uuid + ".Veinminer", miningListener.getVeinminerTracker().getOrDefault(p, -1));

            if (miningListener.getPeacefulMiners().contains(p)) data.set(uuid + ".PeacefulMiner", true);
            else data.set(uuid + ".PeacefulMiner", false);
        }

        data.save(dataFile);
    }

    public void saveLeaderboard() throws IOException {
        if (leaderboardTracker.isEmpty()) return;
        if (leaderboardData == null) return;

        for (Map.Entry<UUID, LeaderboardPlayer> player : leaderboardTracker.entrySet()) {
            leaderboardData.set(player.getKey() + ".Name", player.getValue().getName());
            leaderboardData.set(player.getKey() + ".Level", player.getValue().getScore());
            leaderboardData.set(player.getKey() + ".Building", player.getValue().getBuildingScore());
            leaderboardData.set(player.getKey() + ".Mining", player.getValue().getMiningScore());
            leaderboardData.set(player.getKey() + ".Fishing", player.getValue().getFishingScore());
            leaderboardData.set(player.getKey() + ".Exploring", player.getValue().getExploringScore());
            leaderboardData.set(player.getKey() + ".Farming", player.getValue().getFarmingScore());
            leaderboardData.set(player.getKey() + ".Fighting", player.getValue().getFightingScore());
            leaderboardData.set(player.getKey() + ".Crafting", player.getValue().getCraftingScore());
            leaderboardData.set(player.getKey() + ".Main", player.getValue().getMainScore());
            leaderboardData.set(player.getKey() + ".Deaths", player.getValue().getDeathScore());
        }

        try {
            leaderboardData.save(leaderboardFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void savePermaTrash(Player p) {
        if (permaTrashData == null) return;
        UUID uuid = p.getUniqueId();
        if (!getFishingListener().getPermaTrash().containsKey(p)) return;
        AutoTrash trash = getFishingListener().getPermaTrash().get(p);
        if (trash == null) return;

        permaTrashData.set(uuid + ".BigTrash", trash.isBig());

        int i = 0;
        if (trash.getTrashMaterials().isEmpty()) permaTrashData.set(uuid + ".Materials", null);
        for (Material mat : trash.getTrashMaterials()) {
            permaTrashData.set(uuid + ".Materials." + i, mat.toString());
            i++;
        }

        i = 0;
        if (trash.getEnchants().isEmpty()) permaTrashData.set(uuid + ".Enchants", null);
        for (Enchantment enchant : trash.getEnchants()) {
            permaTrashData.set(uuid + ".Enchants." + i, enchant.getKey().toString());
            i++;
        }

        try {
            permaTrashData.save(permaTrashFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateConfig() {
        // Load the default configuration from the JAR
        InputStream defConfigStream = getClass().getClassLoader().getResourceAsStream("config.yml");
        if (defConfigStream == null) {
            throw new RuntimeException("Failed to load default config");
        }
        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));
        mergeConfigWithOrder(config, defConfig, "");

        // Save the merged configuration
        File file = new File(getDataFolder(), "config.yml");
        try {
            defConfig.save(file);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save config file", e);
        }
    }

    /**
     * Takes the new default file and copies existing values from the old config file into the new one
     */
    private void mergeConfigWithOrder(ConfigurationSection target, ConfigurationSection source, String parentKey) {
        for (String key : source.getKeys(false)) {
            // Get the current path
            String fullKey = parentKey.isEmpty() ? key : parentKey + "." + key;

            // If both target and source contain the key, and they are sections, recurse
            if (source.isConfigurationSection(key) && target.isConfigurationSection(key)) {
                ConfigurationSection targetSection = target.getConfigurationSection(key);
                ConfigurationSection sourceSection = source.getConfigurationSection(key);
                if (targetSection != null && sourceSection != null) {
                    mergeConfigWithOrder(targetSection, sourceSection, fullKey);
                } else {
                    Bukkit.getLogger().warning("Failed to handle section: " + fullKey);
                }
            }
            // If it is simply a value and target contains it, copy it to the source
            else if (target.contains(key)) {
                Object value = target.get(key);
                source.set(key, value);
            }
            else if (target.get(key) != null) {
                Object value = target.get(key);
                source.set(key, value);
            }
        }
    }

    /**
     * Removes the player from the skills list and from the
     * scoreboard hashtable
     */
    public void playerQuit(Player p) {
        skillManager.getPlayerSkills().remove(p.getUniqueId());
        if (miningListener.getToolBelts().containsKey(p)) {
            abilityManager.saveToolBelt(p, miningListener.getToolBelts().get(p));
            miningListener.getToolBelts().remove(p);
        }
        toggledScoreboard.remove(p.getUniqueId());
        scoreboardTracker.remove(p);
        HashMap<Player, TrailEffect> trailTracker = abilityManager.getTrailTracker();
        if (trailTracker.containsKey(p)) {
            trailTracker.get(p).cancel();
            trailTracker.remove(p);
        }
        fightingListener.getNoPhantomSpawns().remove(p);
        fightingListener.getActiveBerserkers().remove(p);
    }

    public void playerJoin(Player p, boolean overrideNewPlayer) {
        abilityManager.getTimerTracker().put(p, new ArrayList<>());

        File dataFile = new File(getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) saveResource("playerdata.yml", true);
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);

        boolean newPlayer = data.get(p.getUniqueId().toString()) == null;
        if (overrideNewPlayer) newPlayer = false;
        loadData(p, data);

        // Check if the player should activate any nearby trophies
        for (Map.Entry<Location, Trophy> trophy : trophyManager.getTrophies().entrySet()) {
            Location loc = trophy.getKey();
            if (!p.getWorld().equals(loc.getWorld())) continue;
            if (p.getLocation().distance(loc) > 50) continue;
            trophy.getValue().getEffects().checkForPlayers();
        }

        // Make sure the main XP level is correct, load the player's rewards, add them to active leaderboard players
        // Load their perma trash inventory, and hide any glowing blocks from other player's spelunker ability
        skillManager.checkMainXP(p);
        skillManager.loadPlayerRewards(p);
        Leaderboard.leaderboardJoin(this, p);
        loadPermaTrash(p);
        getMiningListener().hideGlowForPlayer(p);

        // Handle the scoreboard
        if (newPlayer) SkillScoreboard.initializeScoreboard(this, p);
        else if (toggledScoreboard.containsKey(p.getUniqueId()) && toggledScoreboard.get(p.getUniqueId()))
            SkillScoreboard.initializeScoreboard(this, p);
        else SkillScoreboard.hideScoreboard(this, p);
    }

    public void createFarmingList() {
        //farmingList.add(Material.SUGAR_CANE); // Needs to be bug prevented || cactus, bamboo, seaweed
        farmingList.add(Material.SUGAR_CANE);
        farmingList.add(Material.CACTUS);
        farmingList.add(Material.KELP_PLANT);
        farmingList.add(Material.KELP);
        farmingList.add(Material.WHEAT);
        farmingList.add(Material.WHEAT_SEEDS);
        farmingList.add(Material.CARROTS);
        farmingList.add(Material.CARROT);
        farmingList.add(Material.POTATOES);
        farmingList.add(Material.POTATO);
        farmingList.add(Material.BEETROOTS);
        farmingList.add(Material.BEETROOT_SEEDS);
        farmingList.add(Material.MELON);
        farmingList.add(Material.MELON_SEEDS);
        farmingList.add(Material.PUMPKIN);
        farmingList.add(Material.PUMPKIN_SEEDS);
        farmingList.add(Material.COCOA_BEANS);
        farmingList.add(Material.COCOA);
        farmingList.add(Material.BROWN_MUSHROOM_BLOCK);
        farmingList.add(Material.BROWN_MUSHROOM);
        farmingList.add(Material.RED_MUSHROOM_BLOCK);
        farmingList.add(Material.RED_MUSHROOM);
        farmingList.add(Material.NETHER_WART);
    }

    public ArrayList<Material> getFarmingList() {
        return farmingList;
    }

    public HashMap<UUID, Boolean> getToggledScoreboard() {
        return toggledScoreboard;
    }

    /**
     * Returns true if there is a claim there
     */
    public boolean checkForClaim(Player p, Location loc) {
        String noBuildReason = GriefPrevention.instance.allowBuild(p, loc);
        return (noBuildReason != null);
    }

    public boolean canPlaceBlockInRegion(Player p, Location loc) {
        World world = loc.getWorld();
        if (world == null) return true;
        RegionManager regions = container.get(BukkitAdapter.adapt(world));
        if (regions == null) return true;

        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        ApplicableRegionSet applicableRegions = regions.getApplicableRegions(BlockVector3.at(loc.getX(), loc.getY(), loc.getZ()));
        for (ProtectedRegion region : applicableRegions) {
            if (region == null) continue;
            if (!region.contains(BlockVector3.at(x, y, z))) continue;
            StateFlag.State state = region.getFlag(Flags.BLOCK_PLACE);
            boolean allowed = StateFlag.test(state);
            if (p.hasPermission("worldguard.region.bypass." + region.getId()) || p.isOp()) allowed = true;
            if (!allowed) return false;
        }

        return true;
    }

    public Enchantment getEnchantFromKey(String key) {
        for (Enchantment enchant : Registry.ENCHANTMENT) {
            if (enchant.getKey().toString().equalsIgnoreCase(key)) return enchant;
        }
        return Enchantment.EFFICIENCY;
    }

    public MiningSkill getMiningListener() {
        return miningListener;
    }

    public FarmingSkill getFarmingListener() {
        return farmingListener;
    }

    public BuildingSkill getBuildingListener() {
        return buildingListener;
    }

    public FishingSkill getFishingListener() {
        return fishingListener;
    }

    public FightingSkill getFightingListener() {
        return fightingListener;
    }

    public CraftingSkill getCraftingListener() {
        return craftingListener;
    }

    public ExploringSkill getExploringListener() {
        return exploringListener;
    }

    public MainSkill getMainListener() {
        return mainListener;
    }

    public PlayerListener getPlayerListener() {
        return playerListener;
    }

    public boolean isForced(Player p, String[] args) {
        if (!p.hasPermission("survivalskills.op")) return false;
        if (args.length < 1) return false;
        for (String arg : args) if (arg.equalsIgnoreCase("force")) return true;
        return false;
    }

    public FileConfiguration getTrueConfig() {
        return config;
    }

    public FileConfiguration getTrophyData() {
        return trophyData;
    }

    public File getTrophyFile() {
        return trophyFile;
    }

    public FileConfiguration getLeaderboardData() {
        return leaderboardData;
    }

    public File getToolBeltFile() {
        return toolBeltFile;
    }

    public FileConfiguration getToolBeltData() {
        return toolBeltData;
    }

    public ArrayList<NamespacedKey> getRecipeKeys() {
        return recipeKeys;
    }

    public boolean isGriefPreventionEnabled() {
        return griefPreventionEnabled;
    }

    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }

    public HashMap<UUID, LeaderboardPlayer> getLeaderboardTracker() {
        return leaderboardTracker;
    }

    public boolean isWoolRecipes() {
        return woolRecipes;
    }

    public void setWoolRecipes(boolean woolRecipes) {
        this.woolRecipes = woolRecipes;
    }

    public HashMap<Player, Scoreboard> getScoreboardTracker() {
        return scoreboardTracker;
    }

    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    public TrophyManager getTrophyManager() {
        return trophyManager;
    }

    public SkillManager getSkillManager() {
        return skillManager;
    }
}
