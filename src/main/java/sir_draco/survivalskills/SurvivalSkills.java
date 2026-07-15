package sir_draco.survivalskills;

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
import sir_draco.survivalskills.external.listeners.CitizensTrophyListener;
import sir_draco.survivalskills.external.providers.WorldGuardProvider;
import sir_draco.survivalskills.god_questline.*;
import sir_draco.survivalskills.god_questline.trial.Trial;
import sir_draco.survivalskills.god_questline.trial.TrialManager;
import sir_draco.survivalskills.god_questline.trial.TrialUpgradeManager;
import sir_draco.survivalskills.rewards.PlayerRewards;
import sir_draco.survivalskills.skill_listeners.*;
import sir_draco.survivalskills.skill_listeners.ArmorListener.ArmorType;
import sir_draco.survivalskills.skills.Skill;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.skills.SkillManager;
import sir_draco.survivalskills.trophy.Trophy;
import sir_draco.survivalskills.trophy.TrophyEffects;
import sir_draco.survivalskills.trophy.TrophyListener;
import sir_draco.survivalskills.trophy.TrophyManager;
import sir_draco.survivalskills.utils.CommandRegistry;
import sir_draco.survivalskills.utils.DependencyChecker;
import sir_draco.survivalskills.utils.FileUtils;
import sir_draco.survivalskills.utils.RecipeMaker;
import sir_draco.survivalskills.utils.RecipeRegistrar;
import sir_draco.survivalskills.utils.Utils;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

public final class SurvivalSkills extends JavaPlugin {

    private static SurvivalSkills instance;

    private final HashMap<UUID, Boolean> showScoreboard = new HashMap<>();
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
    private WorldGuardProvider worldGuardProvider = null;

    @Override
    public void onEnable() {
        instance = this;

        // Make sure there are no trophy item stragglers from before 
        World world = Bukkit.getWorld(Bukkit.getWorlds().get(0).getName());
        if (world != null) {
            for (Entity ent : world.getEntities()) {
                Utils.tryRemovingTrophyItem(ent);
            }
        }

        // Check for plugin dependencies
        DependencyChecker.check();

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
                RecipeRegistrar.emptyRecipeStack(SurvivalSkills.getInstance());
            }
        }.runTaskAsynchronously(this);

        abilityManager = new AbilityManager(this);
        CommandRegistry.registerAll(this);

        TrialManager.loadProtectedAreas();

        // If the plugin is reloaded without a restart
        Utils.loadOnlinePlayers(this);
    }

    @Override
    public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.closeInventory();
            FileUtils.savePermaTrash(p, permaTrashData, permaTrashFile);
        }

        FileUtils.savePlayerData();

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

        FileUtils.saveLeaderboard(leaderboardTracker, leaderboardData, leaderboardFile);

        FileUtils.saveTeleportAnchors(godListener.getTeleportAnchors());
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
        armorListener = new ArmorListener();
        TrophyListener trophyListener = new TrophyListener(this);
        TabCompleter tabCompleter = new TabCompleter(this);
        godListener = new GodListener();
        SortWandListener sortWandListener = new SortWandListener(this);
        FlightRespawnListener flightRespawnListener = new FlightRespawnListener(this);

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
        godListener.register(this);
        getServer().getPluginManager().registerEvents(sortWandListener, this);
        getServer().getPluginManager().registerEvents(flightRespawnListener, this);
        getServer().getPluginManager().registerEvents(new TrialManager(), this);
        getServer().getPluginManager().registerEvents(new TrialUpgradeManager(), this);
        
        if (citizensEnabled) {
            getServer().getPluginManager().registerEvents(new CitizensTrophyListener(this), this);
        }
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
        showScoreboard.remove(p.getUniqueId());
        scoreboardTracker.remove(p);
        Map<Player, TrailEffect> trailTracker = abilityManager.getTrailTracker();
        if (trailTracker.containsKey(p)) {
            trailTracker.get(p).cancel();
            trailTracker.remove(p);
        }
        fightingListener.getNoPhantomSpawns().remove(p);
        farmingListener.getBlacklistedFoods().remove(p);
        farmingListener.getAutoEatModes().remove(p);

        if (!TrialManager.getTrials().isEmpty())
            for (Trial trial : TrialManager.getTrials())
                if (trial.getPlayers().contains(p))
                    trial.quitTrial(p);
    }

    public void playerJoin(Player p) {
        abilityManager.getTimerTracker().put(p, new ArrayList<>());

        File dataFile = new File(getDataFolder(), FileUtils.PLAYERDATA_YML);
        if (!dataFile.exists())
            saveResource(FileUtils.PLAYERDATA_YML, true);
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);

        FileUtils.loadData(p, data);

        // Check if the player should activate any nearby trophies
        for (Map.Entry<Location, Trophy> trophy : trophyManager.getTrophies().entrySet()) {
            Location loc = trophy.getKey();
            if (!p.getWorld().equals(loc.getWorld()) || p.getLocation().distance(loc) > 50)
                continue;
            trophy.getValue().getEffects().ifPresent((TrophyEffects trophyEffects) -> trophyEffects.checkForPlayers());
        }

        // Make sure the main XP level is correct, load the player's rewards, add them to active leaderboard players
        skillManager.syncMainSkill(p);
        skillManager.loadPlayerRewards(p);
        skillManager.loadPlayerMultiplier(p, data);
        LeaderboardPlayer leaderboardPlayer = Leaderboard.initializeLeaderboardForPlayer(p);
        PlayerRewards.handleDeathSkillEffects(p);
        
        // Handle abilities
        FileUtils.loadPermaTrash(p, permaTrashData);
        getMiningListener().hideGlowForPlayer(p);
        armorListener.playerWearingArmor(p, p.getInventory().getArmorContents(), ArmorType.BEACON);
        abilityManager.loadFlight(p, data);
        abilityManager.loadSpelunker(p, data);

        // Handle the scoreboard
        if (showScoreboard.containsKey(p.getUniqueId())
                && Boolean.FALSE.equals(showScoreboard.get(p.getUniqueId()))) {
            // Hidden scoreboard
            SkillScoreboard.hideScoreboard(p);
        } else {
            new BukkitRunnable() {
                @Override
                public void run() {
                    SkillScoreboard.initializeScoreboard(p);
                    SkillScoreboard.updateScoreboard(p, SkillCategory.MAIN);

                    int deaths = leaderboardPlayer.getScore(SkillCategory.DEATHS);
                    SkillScoreboard.updateScoreboardDeaths(p, deaths);

                    SkillScoreboard.updateNametags(p);
                }
            }.runTaskLater(this, 20);
        }

        if (SkillManager.getSkillLevel(p.getUniqueId(), SkillCategory.MAIN) == Skill.MAX_LEVEL
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

    public Map<UUID, Boolean> getShowScoreboard() {
        return showScoreboard;
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
        if (!p.hasPermission("survivalskills.op"))
            return false;
        if (args.length < 1)
            return false;
        for (String arg : args)
            if (arg.equalsIgnoreCase("force"))
                return true;
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

    public WorldGuardProvider getWorldGuardProvider() {
        return worldGuardProvider;
    }

    public void setSkillManager(SkillManager skillManager) {
        this.skillManager = skillManager;
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
