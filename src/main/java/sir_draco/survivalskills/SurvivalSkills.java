package sir_draco.survivalskills;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import sir_draco.survivalskills.abilities.AbilityManager;
import sir_draco.survivalskills.abilities.TrailEffect;
import sir_draco.survivalskills.commands.*;
import sir_draco.survivalskills.boards.Leaderboard;
import sir_draco.survivalskills.boards.LeaderboardPlayer;
import sir_draco.survivalskills.boards.SkillScoreboard;
import sir_draco.survivalskills.commands.skill_commands.*;
import sir_draco.survivalskills.god_questline.*;
import sir_draco.survivalskills.god_questline.trial.Trial;
import sir_draco.survivalskills.god_questline.trial.TrialManager;
import sir_draco.survivalskills.god_questline.trial.TrialUpgradeManager;
import sir_draco.survivalskills.skill_listeners.*;
import sir_draco.survivalskills.skills.SkillManager;
import sir_draco.survivalskills.trophy.Trophy;
import sir_draco.survivalskills.trophy.TrophyListener;
import sir_draco.survivalskills.trophy.TrophyManager;
import sir_draco.survivalskills.utils.FileUtils;
import sir_draco.survivalskills.utils.RecipeMaker;
import sir_draco.survivalskills.utils.Utils;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

public final class SurvivalSkills extends JavaPlugin {

    private static SurvivalSkills instance;

    private final HashMap<UUID, Boolean> toggledScoreboard = new HashMap<>();
    private final HashMap<Player, Scoreboard> scoreboardTracker = new HashMap<>();
    private final HashMap<UUID, LeaderboardPlayer> leaderboardTracker = new HashMap<>();
    private final ArrayList<Material> farmingList = new ArrayList<>();
    private final ArrayList<NamespacedKey> recipeKeys = new ArrayList<>();
    private final HashMap<NamespacedKey, Integer> godRecipeKeys = new HashMap<>();

    // Managers
    private AbilityManager abilityManager;
    private SkillManager skillManager;
    private TrophyManager trophyManager;

    // Listeners
    private MiningSkill miningListener;
    private FishingSkill fishingListener;
    private ExploringSkill exploringListener;
    private FarmingSkill farmingListener;
    private FightingSkill fightingListener;
    private MainSkill mainListener;
    private PlayerListener playerListener;
    private ArmorListener armorListener;
    private GodListener godListener;
    private FlightCommand flightCommand;

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
    private boolean citizensEnabled = false;
    private boolean exponentialXP = false;
    private RegionContainer container = null;
    private ProtectedRegion region = null;

    @Override
    public void onEnable() {
        instance = this;

        // Make sure there are no stragglers from before
        World world = Bukkit.getWorld("world");
        if (world != null) {
            for (Entity ent : world.getEntities()) {
                Utils.tryRemovingTrophyItem(ent);
            }
        }

        FileUtils.loadFiles();

        // Load plugin features
        loadListeners();
        trophyManager = new TrophyManager(this);

        new BukkitRunnable() {
            @Override
            public void run() {
                RecipeMaker.trophyRecipes(SurvivalSkills.getInstance());
                RecipeMaker.rewardRecipes(SurvivalSkills.getInstance());
                RecipeMaker.godRecipes(SurvivalSkills.getInstance());
                RecipeMaker.emptyRecipeStack(SurvivalSkills.getInstance());
            }
        }.runTaskAsynchronously(this);

        abilityManager = new AbilityManager(this);
        FileUtils.loadCommands();

        TrialManager.loadProtectedAreas();

        // If the plugin is reloaded without a restart
        Utils.loadOnlinePlayers(this);

        // Check for plugin dependencies
        FileUtils.checkPluginDependencies(world);
    }

    @Override
    public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.closeInventory();
            FileUtils.savePermaTrash(p, permaTrashData, permaTrashFile);
        }

        try {
            FileUtils.savePlayerData();
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save player data", e);
        }

        try {
            trophyManager.saveTrophies();
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save trophy data", e);
        }
        trophyManager.disableTrophies();

        try {
            mainListener.saveGraves();
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save grave data", e);
        }

        try {
            FileUtils.saveLeaderboard(leaderboardTracker, leaderboardData, leaderboardFile);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save leaderboard data", e);
        }

        FileUtils.savePowerOreConversions();
        FileUtils.savePotionBags();
        abilityManager.saveToolBelts();
        abilityManager.removeGlowFromScannedMobs();

        TrialManager.handleTrials();

        getMiningListener().endSpelunkerAll();
    }

    public void loadListeners() {
        // Keep farming list global for use by multiple listeners
        createFarmingList();

        // Listeners
        BuildingSkill buildingListener = new BuildingSkill(this);
        miningListener = new MiningSkill(this, config.getInt("VeinMinerHungerAmount"));
        fishingListener = new FishingSkill(this);
        exploringListener = new ExploringSkill(this);
        farmingListener = new FarmingSkill(this);
        fightingListener = new FightingSkill(this);
        CraftingSkill craftingListener = new CraftingSkill(this);
        mainListener = new MainSkill(this);
        playerListener = new PlayerListener(this);
        armorListener = new ArmorListener(this);
        TrophyListener trophyListener = new TrophyListener(this);
        TabCompleter tabCompleter = new TabCompleter(this);
        godListener = new GodListener();

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
        getServer().getPluginManager().registerEvents(armorListener, this);
        getServer().getPluginManager().registerEvents(godListener, this);
        getServer().getPluginManager().registerEvents(new TrialManager(), this);
        getServer().getPluginManager().registerEvents(new TrialUpgradeManager(), this);
    }

    /**
     * Removes the player from the skills list and from the
     * scoreboard hashtable
     */
    public void playerQuit(Player p) {
        abilityManager.endPlayerTimers(p);
        skillManager.getPlayerSkills().remove(p.getUniqueId());
        if (miningListener.getToolBelts().containsKey(p)) {
            abilityManager.saveToolBelt(p, miningListener.getToolBelts().get(p));
            miningListener.getToolBelts().remove(p);
        }
        toggledScoreboard.remove(p.getUniqueId());
        scoreboardTracker.remove(p);
        Map<Player, TrailEffect> trailTracker = abilityManager.getTrailTracker();
        if (trailTracker.containsKey(p)) {
            trailTracker.get(p).cancel();
            trailTracker.remove(p);
        }
        fightingListener.getNoPhantomSpawns().remove(p);
        fightingListener.getActiveBerserkers().remove(p);

        if (!TrialManager.getTrials().isEmpty())
            for (Trial trial : TrialManager.getTrials())
                if (trial.getPlayers().contains(p)) trial.quitTrial(p);
    }

    public void playerJoin(Player p, boolean overrideNewPlayer) {
        abilityManager.getTimerTracker().put(p, new ArrayList<>());

        File dataFile = new File(getDataFolder(), FileUtils.PLAYERDATA_YML);
        if (!dataFile.exists()) saveResource(FileUtils.PLAYERDATA_YML, true);
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);

        boolean newPlayer = data.get(p.getUniqueId().toString()) == null;
        if (overrideNewPlayer) newPlayer = false;
        FileUtils.loadData(p, data);

        // Check if the player should activate any nearby trophies
        for (Map.Entry<Location, Trophy> trophy : trophyManager.getTrophies().entrySet()) {
            Location loc = trophy.getKey();
            if (!p.getWorld().equals(loc.getWorld()) || p.getLocation().distance(loc) > 50) continue;
            trophy.getValue().getEffects().checkForPlayers();
        }

        // Make sure the main XP level is correct, load the player's rewards, add them to active leaderboard players
        // Load their perma trash inventory, and hide any glowing blocks from other player's spelunker ability
        skillManager.checkMainXP(p);
        skillManager.loadPlayerRewards(p);
        skillManager.loadPlayerMultiplier(p, data);
        Leaderboard.leaderboardJoin(this, p);
        FileUtils.loadPermaTrash(p, permaTrashData);
        getMiningListener().hideGlowForPlayer(p);
        armorListener.playerWearingBeaconArmor(p, p.getInventory().getArmorContents());
        abilityManager.loadFlight(p, data);
        abilityManager.loadSpelunker(p, data);

        // Handle the scoreboard
        if (newPlayer) SkillScoreboard.initializeScoreboard(this, p);
        else if (toggledScoreboard.containsKey(p.getUniqueId()) && Boolean.TRUE.equals(toggledScoreboard.get(p.getUniqueId()))){
            new BukkitRunnable() {
                @Override
                public void run() {
                    SkillScoreboard.initializeScoreboard(instance, p);
                }
            }.runTaskLater(this, 20);
        }
        else SkillScoreboard.hideScoreboard(this, p);

        if (SkillManager.getSkillLevel(p.getUniqueId(), "Main") == 100
                && !getTrophyManager().getPlayerGodQuestData().containsKey(p.getUniqueId())) {
            GodTrophyQuest quest = new GodTrophyQuest(p.getUniqueId());
            SurvivalSkills.getInstance().getTrophyManager().getPlayerGodQuestData().put(p.getUniqueId(), quest);
        }
    }

    public void createFarmingList() {
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

    public List<Material> getFarmingList() {
        return farmingList;
    }

    public Map<UUID, Boolean> getToggledScoreboard() {
        return toggledScoreboard;
    }

    public MiningSkill getMiningListener() {
        return miningListener;
    }

    public FarmingSkill getFarmingListener() {
        return farmingListener;
    }

    public FishingSkill getFishingListener() {
        return fishingListener;
    }

    public FightingSkill getFightingListener() {
        return fightingListener;
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

    public File getPermaTrashFile() {
        return permaTrashFile;
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

    public FileConfiguration getPermaTrashData() {
        return permaTrashData;
    }

    public List<NamespacedKey> getRecipeKeys() {
        return recipeKeys;
    }

    public Map<NamespacedKey, Integer> getGodRecipeKeys() {
        return godRecipeKeys;
    }

    public boolean isGriefPreventionEnabled() {
        return griefPreventionEnabled;
    }

    public void setGriefPreventionEnabled(boolean griefPreventionEnabled) {
        this.griefPreventionEnabled = griefPreventionEnabled;
    }

    public void setCitizensEnabled(boolean citizensEnabled) {
        this.citizensEnabled = citizensEnabled;
    }

    public void setWorldGuardEnabled(boolean worldGuardEnabled) {
        this.worldGuardEnabled = worldGuardEnabled;
    }

    public void setContainer(RegionContainer container) {
        this.container = container;
    }

    public void setRegion(ProtectedRegion region) {
        this.region = region;
    }

    public void setFlightCommand(FlightCommand flightCommand) {
        this.flightCommand = flightCommand;
    }

    public boolean isWorldGuardEnabled() {
        return worldGuardEnabled;
    }

    public Map<UUID, LeaderboardPlayer> getLeaderboardTracker() {
        return leaderboardTracker;
    }

    public boolean isWoolRecipes() {
        return woolRecipes;
    }

    public void setWoolRecipes(boolean woolRecipes) {
        this.woolRecipes = woolRecipes;
    }

    public Map<Player, Scoreboard> getScoreboardTracker() {
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

    public GodListener getGodListener() {
        return godListener;
    }

    public void setSkillManager(SkillManager skillManager) {
        this.skillManager = skillManager;
    }

    public FlightCommand getFlightCommand() {
        return flightCommand;
    }

    public boolean isCitizensEnabled() {
        return citizensEnabled;
    }

    public boolean isExponentialXP() {
        return exponentialXP;
    }

    public void setExponentialXP(boolean exponentialXP) {
        this.exponentialXP = exponentialXP;
    }

    public ProtectedRegion getRegion() {
        return region;
    }

    public RegionContainer getContainer() {
        return container;
    }

    public void setConfig(FileConfiguration config) {
        this.config = config;
    }

    public void setTrophyData(FileConfiguration trophyData) {
        this.trophyData = trophyData;
    }

    public void setLeaderboardData(FileConfiguration leaderboardData) {
        this.leaderboardData = leaderboardData;
    }

    public void setPermaTrashData(FileConfiguration permaTrashData) {
        this.permaTrashData = permaTrashData;
    }

    public void setToolBeltData(FileConfiguration toolBeltData) {
        this.toolBeltData = toolBeltData;
    }

    public void setLeaderboardFile(File leaderboardFile) {
        this.leaderboardFile = leaderboardFile;
    }

    public void setTrophyFile(File trophyFile) {
        this.trophyFile = trophyFile;
    }

    public void setPermaTrashFile(File permaTrashFile) {
        this.permaTrashFile = permaTrashFile;
    }

    public void setToolBeltFile(File toolBeltFile) {
        this.toolBeltFile = toolBeltFile;
    }

    public static SurvivalSkills getInstance() {
        return instance;
    }
}
