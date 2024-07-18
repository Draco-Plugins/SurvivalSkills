package sir_draco.survivalskills;

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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import sir_draco.survivalskills.Abilities.AbilityTimer;
import sir_draco.survivalskills.Abilities.AutoTrash;
import sir_draco.survivalskills.Abilities.TrailEffect;
import sir_draco.survivalskills.Commands.*;
import sir_draco.survivalskills.Rewards.PlayerRewards;
import sir_draco.survivalskills.Rewards.Reward;
import sir_draco.survivalskills.Boards.Leaderboard;
import sir_draco.survivalskills.Boards.LeaderboardPlayer;
import sir_draco.survivalskills.Boards.SkillScoreboard;
import sir_draco.survivalskills.SkillListeners.*;
import sir_draco.survivalskills.Trophy.Trophy;

import java.io.*;
import java.util.*;

public final class SurvivalSkills extends JavaPlugin {

    private final HashMap<UUID, ArrayList<Skill>> playerSkills = new HashMap<>();
    private final HashMap<UUID, Boolean> toggledScoreboard = new HashMap<>();
    private final HashMap<Player, Scoreboard> scoreboardTracker = new HashMap<>();
    private final HashMap<UUID, HashMap<String, Boolean>> trophyTracker = new HashMap<>();
    private final HashMap<Location, Trophy> trophies = new HashMap<>();
    private final HashMap<Integer, ItemStack> trophyItems = new HashMap<>();
    private final HashMap<Player, PlayerRewards> rewardTracker = new HashMap<>();
    private final HashMap<Player, ArrayList<AbilityTimer>> timerTracker = new HashMap<>();
    private final HashMap<Player, TrailEffect> trailTracker = new HashMap<>();
    private final HashMap<String, Particle> trails = new HashMap<>();
    private final HashMap<UUID, LeaderboardPlayer> leaderboardTracker = new HashMap<>();
    private final ArrayList<Material> farmingList = new ArrayList<>();
    private final ArrayList<NamespacedKey> recipeKeys = new ArrayList<>();

    private PlayerRewards playerRewards; // Holds the default information for rewards
    private BuildingSkill buildingListener;
    private MiningSkill miningListener;
    private FishingSkill fishingListener;
    private ExploringSkill exploringListener;
    private FarmingSkill farmingListener;
    private FightingSkill fightingListener;
    private CraftingSkill craftingListener;
    private MainSkill mainListener;
    private PlayerListener playerListener;
    private double buildingXP;
    private double miningXP;
    private double fishingXP;
    private double exploringXP;
    private double farmingXP;
    private double fightingXP;
    private double craftingXP;
    private double multiplier = 1;
    private FileConfiguration config;
    private File trophyFile;
    private FileConfiguration trophyData;
    private File leaderboardFile;
    private FileConfiguration leaderboardData;
    private File permaTrashFile;
    private FileConfiguration permaTrashData;
    private boolean woolRecipes = false;
    private boolean griefPreventionEnabled = false;

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
        if (config.get("Version") == null || config.getDouble("Version") != 1.91) saveResource("config.yml", true);
        loadConfigSettings();

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

        // Load plugin features
        loadListeners();
        loadTrophies();
        RecipeMaker.trophyRecipes(this);
        RecipeMaker.rewardRecipes(this);
        loadCommands();
        createTrails();

        // If the plugin is reloaded without a restart
        if (!getServer().getOnlinePlayers().isEmpty())
            for (Player p : getServer().getOnlinePlayers()) playerJoin(p, true);

        if (getServer().getPluginManager().getPlugin("GriefPrevention") != null) griefPreventionEnabled = true;
        // Auto save skills every 15 minutes
        runSkillAutoSave();
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
            saveTrophies();
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

        getMiningListener().endSpelunkerAll();
    }

    public void loadConfigSettings() {
        // Load XP settings
        buildingXP = config.getDouble("BuildingXP");
        miningXP = config.getDouble("MiningXP");
        fishingXP = config.getDouble("FishingXP");
        exploringXP = config.getDouble("ExploringXP");
        farmingXP = config.getDouble("FarmingXP");
        fightingXP = config.getDouble("FightingXP");
        craftingXP = config.getDouble("CraftingXP");

        // Load global multiplier
        if (config.get("SkillXPMultiplier") != null) multiplier = config.getDouble("SkillXPMultiplier");

        // Load skill rewards
        playerRewards = new PlayerRewards();
        loadRewardConfig("Mining");
        loadRewardConfig("Exploring");
        loadRewardConfig("Farming");
        loadRewardConfig("Building");
        loadRewardConfig("Fighting");
        loadRewardConfig("Fishing");
        loadRewardConfig("Crafting");
        loadRewardConfig("Main");
    }

    public void loadRewardConfig(String type) {
        ConfigurationSection section = config.getConfigurationSection(type);
        if (section == null) return;
        section.getKeys(false).forEach(key -> {
            // Load whether it is enabled, the level it is unlocked, and the type of reward
            boolean enabled = config.getBoolean(type + "." + key + ".Enabled");
            int level = config.getInt(type + "." + key + ".Level");
            String rewardType = config.getString(type + "." + key + ".Type");
            playerRewards.addReward(type, new Reward(type, key, rewardType, level, enabled));
        });
    }

    public void loadCommands() {
        // Default Player Commands
        new SkillStatsCommand(this);
        new ToggleScoreboardCommand(this);
        new SpelunkerCommand(this);
        new VeinminerCommand(this);
        new NightVisionCommand(this);
        new PeacefulMinerCommand(this);
        new AutoEatCommand(this);
        new EatCommand(this);
        new FlightCommand(this);
        new MobScannerCommand(this);
        new WaterBreathingCommand(this);
        new DeathLocationCommand(this);
        new ToggleSpeedCommand(this);
        new ToggleTrailCommand(this);
        new AutoTrashCommand(this);
        new PermaTrashCommand(this);
        new TogglePhantomsCommand(this);
        new DeathReturnCommand(this);

        // Admin Commands
        new GetTrophyCommand(this);
        new SurvivalSkillsGetCommand(this);
        new CaveFinderCommand(this);
        new BossCommand(this);
        new SurvivalSkillsCommand(this);
        new SkillsMultiplierCommand(this);
        new ResetFirstDragon(this);
        new BossMusicCommand(this);
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
        AutoTrash trash = new AutoTrash(false);
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

        buildingListener = new BuildingSkill(this, buildingXP);
        miningListener = new MiningSkill(this, miningXP, config.getInt("VeinMinerHungerAmount"));
        fishingListener = new FishingSkill(this, fishingXP);
        exploringListener = new ExploringSkill(this, exploringXP);
        farmingListener = new FarmingSkill(this, farmingXP);
        fightingListener = new FightingSkill(this, fightingXP);
        craftingListener = new CraftingSkill(this, craftingXP);
        mainListener = new MainSkill(this);
        playerListener = new PlayerListener(this);
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
            trophyTracker.put(p.getUniqueId(), trophyList);
            playerSkills.put(p.getUniqueId(), skills);
            toggledScoreboard.put(p.getUniqueId(), true);

            savePlayerData(p);
            return;
        }

        UUID uuid = p.getUniqueId();
        if (!playerSkills.containsKey(uuid)) loadPlayerSkills(uuid, data);
        if (!toggledScoreboard.containsKey(uuid)) loadScoreboardSetting(uuid, data);
        if (!trophyTracker.containsKey(uuid)) loadPlayerTrophies(uuid, data);

        if (data.get(uuid + ".NoPhantoms") != null) {
            boolean phantoms = data.getBoolean(uuid + ".NoPhantoms");
            if (phantoms && !getFightingListener().getNoPhantomSpawns().contains(p)) {
                getFightingListener().getNoPhantomSpawns().add(p);
            }
        }

        if (data.get(uuid + ".Trail") != null) {
            String trailName = data.getString(uuid + ".Trail");
            if (trailName != null && !trailName.equals("None")) {
                if (trails.containsKey(trailName)) {
                    int dustType = 1;
                    if (trailName.equalsIgnoreCase("Dust")) dustType = 2;
                    else if (trailName.equalsIgnoreCase("Rainbow")) dustType = 3;
                    TrailEffect effect = new TrailEffect(p, trails.get(trailName), dustType, trailName);
                    effect.runTaskTimer(this, 60, 1);
                    trailTracker.put(p, effect);
                }
            }
        }

        if (data.get(uuid + ".AutoEat") != null) {
            boolean autoEat = data.getBoolean(uuid + ".AutoEat");
            if (autoEat && !farmingListener.getAutoEat().contains(p)) farmingListener.getAutoEat().add(p);
        }

        if (data.get(uuid + ".Veinminer") != null) {
            int veinminer = data.getInt(uuid + ".Veinminer");
            if (veinminer == 0 || veinminer == 1) miningListener.getVeinminerTracker().put(p, veinminer);
        }

        if (data.get(uuid + ".PeacefulMiner") != null) {
            boolean peacefulMiner = data.getBoolean(uuid + ".PeacefulMiner");
            if (peacefulMiner && !miningListener.getPeacefulMiners().contains(p)) miningListener.getPeacefulMiners().add(p);
        }
    }

    public void loadScoreboardSetting(UUID uuid, FileConfiguration data) {
        if (toggledScoreboard.containsKey(uuid)) return;
        if (data.get(uuid + ".Scoreboard") == null) {
            toggledScoreboard.put(uuid, true);
            return;
        }
        toggledScoreboard.put(uuid, data.getBoolean(uuid + ".Scoreboard"));
    }

    public void loadPlayerTrophies(UUID uuid, FileConfiguration data) {
        if (trophyTracker.containsKey(uuid)) return;
        HashMap<String, Boolean> trophyList = new HashMap<>();
        trophyList.put("CaveTrophy", data.getBoolean(uuid + ".CaveTrophy"));
        trophyList.put("ForestTrophy", data.getBoolean(uuid + ".ForestTrophy"));
        trophyList.put("FarmingTrophy", data.getBoolean(uuid + ".FarmingTrophy"));
        trophyList.put("OceanTrophy", data.getBoolean(uuid + ".OceanTrophy"));
        trophyList.put("FishingTrophy", data.getBoolean(uuid + ".FishingTrophy"));
        trophyList.put("ColorTrophy", data.getBoolean(uuid + ".ColorTrophy"));
        trophyList.put("NetherTrophy", data.getBoolean(uuid + ".NetherTrophy"));
        trophyList.put("EndTrophy", data.getBoolean(uuid + ".EndTrophy"));
        trophyList.put("ChampionTrophy", data.getBoolean(uuid + ".ChampionTrophy"));
        trophyList.put("GodTrophy", data.getBoolean(uuid + ".GodTrophy"));
        trophyTracker.put(uuid, trophyList);
    }

    public void loadPlayerSkills(UUID uuid, FileConfiguration data) {
        if (playerSkills.containsKey(uuid)) return;
        ArrayList<Skill> skills = new ArrayList<>();
        skills.add(new Skill(data.getDouble(uuid + ".Main.Experience"), data.getInt(uuid + ".Main.Level"), "Main"));
        skills.add(new Skill(data.getDouble(uuid + ".Building.Experience"), data.getInt(uuid + ".Building.Level"), "Building"));
        skills.add(new Skill(data.getDouble(uuid + ".Mining.Experience"), data.getInt(uuid + ".Mining.Level"), "Mining"));
        skills.add(new Skill(data.getDouble(uuid + ".Fishing.Experience"), data.getInt(uuid + ".Fishing.Level"), "Fishing"));
        skills.add(new Skill(data.getDouble(uuid + ".Exploring.Experience"), data.getInt(uuid + ".Exploring.Level"), "Exploring"));
        skills.add(new Skill(data.getDouble(uuid + ".Farming.Experience"), data.getInt(uuid + ".Farming.Level"), "Farming"));
        skills.add(new Skill(data.getDouble(uuid + ".Fighting.Experience"), data.getInt(uuid + ".Fighting.Level"), "Fighting"));
        skills.add(new Skill(data.getDouble(uuid + ".Crafting.Experience"), data.getInt(uuid + ".Crafting.Level"), "Crafting"));
        playerSkills.put(uuid, skills);
    }

    /**
     * Gets the location of trophies, their type, and
     * who placed the trophy from a file
     */
    public void loadTrophies() {
        ConfigurationSection section = trophyData.getConfigurationSection("");
        if (section == null) return;

        section.getKeys(false).forEach(key -> {
            Location loc = trophyData.getLocation(key + ".Location");
            String uuidString = trophyData.getString(key + ".UUID");
            if (uuidString == null) {
                trophyData.set(key, null);
            }
            else {
                int id = Integer.parseInt(key);
                UUID uuid = UUID.fromString(uuidString);
                String type = trophyData.getString(key + ".Type");
                String playerName = trophyData.getString(key + ".PlayerName");
                Trophy trophy = new Trophy(loc, uuid, type, id, playerName);
                trophy.spawnTrophy(this);
                trophies.put(loc, trophy);
            }
        });
    }

    public void loadPlayerRewards(Player p) {
        if (playerRewards == null || playerRewards.getRewardList() == null) {
            Bukkit.getLogger().warning("Player rewards are not loaded");
            return;
        }

        if (rewardTracker.containsKey(p)) {
            rewardTracker.get(p).enableRewards(p, playerSkills.get(p.getUniqueId()));
            return;
        }

        rewardTracker.put(p, new PlayerRewards(playerRewards.getRewardList()));
        rewardTracker.get(p).enableRewards(p, playerSkills.get(p.getUniqueId()));
    }

    public void savePlayerData(Player p) {
        File dataFile = new File(getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) saveResource("playerdata.yml", true);
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);

        if (playerSkills.isEmpty()) return;

        UUID uuid = p.getUniqueId();
        if (trophyTracker.containsKey(uuid)) {
            for (Map.Entry<String, Boolean> list : trophyTracker.get(uuid).entrySet()) {
                if (data.get(uuid + "." + list.getKey()) != null) {
                    boolean trophy = data.getBoolean(uuid + "." + list.getKey());
                    if (trophy) continue;
                    data.set(uuid + "." + list.getKey(), list.getValue());
                }
                else data.set(uuid + "." + list.getKey(), list.getValue());
            }
        }
        else Bukkit.getLogger().warning("Player " + p.getName() + " does not have a trophy status");

        if (toggledScoreboard.containsKey(uuid)) data.set(uuid + ".Scoreboard", toggledScoreboard.get(uuid));
        else Bukkit.getLogger().warning("Player " + p.getName() + " does not have a scoreboard status");

        if (getFightingListener().getNoPhantomSpawns().contains(p)) data.set(uuid + ".NoPhantoms", true);
        else data.set(uuid + ".NoPhantoms", false);

        if (trailTracker.containsKey(p)) data.set(uuid + ".Trail", trailTracker.get(p).getTrailName());
        else data.set(uuid + ".Trail", "None");

        if (farmingListener.getAutoEat().contains(p)) data.set(uuid + ".AutoEat", true);
        else data.set(uuid + ".AutoEat", false);

        data.set(uuid + ".Veinminer", miningListener.getVeinminerTracker().getOrDefault(p, -1));

        if (miningListener.getPeacefulMiners().contains(p)) data.set(uuid + ".PeacefulMiner", true);
        else data.set(uuid + ".PeacefulMiner", false);

        if (playerSkills.containsKey(uuid)) {
            for (Skill skill : playerSkills.get(uuid)) {
                data.set(uuid + "." + skill.getSkillName() + ".Level", skill.getLevel());
                data.set(uuid + "." + skill.getSkillName() + ".Experience", skill.getExperience());
            }
        }
        else Bukkit.getLogger().warning("Player " + p.getName() + " does not have any skills");

        savePermaTrash(p);

        try {
            data.save(dataFile);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void savePlayerData() throws IOException {
        if (playerSkills.isEmpty()) return;

        File dataFile = new File(getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) saveResource("playerdata.yml", true);
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);

        for (Map.Entry<UUID, HashMap<String, Boolean>> player : trophyTracker.entrySet()) {
            UUID uuid = player.getKey();
            data.set(uuid + ".Scoreboard", toggledScoreboard.get(uuid));
            for (Map.Entry<String, Boolean> list : player.getValue().entrySet()) {
                if (data.get(uuid + "." + list.getKey()) != null) {
                    boolean trophy = data.getBoolean(uuid + "." + list.getKey());
                    if (trophy) continue;
                    data.set(uuid + "." + list.getKey(), list.getValue());
                }
                else data.set(uuid + "." + list.getKey(), list.getValue());
            }
        }

        for (Map.Entry<UUID, ArrayList<Skill>> player : playerSkills.entrySet()) {
            UUID uuid = player.getKey();
            for (Skill skill : player.getValue()) {
                data.set(uuid + "." + skill.getSkillName() + ".Level", skill.getLevel());
                data.set(uuid + "." + skill.getSkillName() + ".Experience", skill.getExperience());
            }
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID uuid = p.getUniqueId();
            if (toggledScoreboard.containsKey(uuid)) data.set(uuid + ".Scoreboard", toggledScoreboard.get(uuid));
            else Bukkit.getLogger().warning("Player " + p.getName() + " does not have a scoreboard status");

            if (getFightingListener().getNoPhantomSpawns().contains(p)) data.set(uuid + ".NoPhantoms", true);
            else data.set(uuid + ".NoPhantoms", false);

            if (trailTracker.containsKey(p)) data.set(uuid + ".Trail", trailTracker.get(p).getTrailName());
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

    public void saveTrophies() throws IOException {
        if (trophyData == null) return;

        if (trophies.isEmpty()) {
            saveResource("trophydata.yml", true);
            return;
        }

        for (Map.Entry<Location, Trophy> trophy : trophies.entrySet()) {
            trophy.getValue().shutdownTrophy();
            trophyData.set(trophy.getValue().getID() + ".Location", trophy.getKey());
            trophyData.set(trophy.getValue().getID() + ".UUID", trophy.getValue().getUUID().toString());
            trophyData.set(trophy.getValue().getID() + ".Type", trophy.getValue().getType());
            trophyData.set(trophy.getValue().getID() + ".PlayerName", trophy.getValue().getPlayerName());
        }

        trophyData.save(trophyFile);
    }

    public void updateExploringStats(UUID uuid) {
        Skill exploring = getSkill(uuid, "Exploring");
        exploring.changeExperience(exploringListener.getPlayerSteps(uuid) * exploringXP, playerMaxSkillLevel(uuid));
    }

    /**
     * Gets the specified skill of a player
     */
    public Skill getSkill(UUID uuid, String skillName) {
        if (playerSkills.isEmpty()) {
            ArrayList<Skill> skills = new ArrayList<>();
            skills.add(new Skill(0, 0, skillName));
            playerSkills.put(uuid, skills);
        }
        if (playerSkills.get(uuid) == null) return new Skill(0, 0, skillName);
        for (Skill skill : playerSkills.get(uuid)) if (skill.getSkillName().equalsIgnoreCase(skillName)) return skill;
        Skill skill = new Skill(0, 0, skillName);
        playerSkills.get(uuid).add(skill);
        return skill;
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

    /**
     * Removes the player from the skills list and from the
     * scoreboard hashtable
     */
    public void playerQuit(Player p) {
        playerSkills.remove(p.getUniqueId());
        toggledScoreboard.remove(p.getUniqueId());
        scoreboardTracker.remove(p);
        if (trailTracker.containsKey(p)) {
            trailTracker.get(p).cancel();
            trailTracker.remove(p);
        }
        getFightingListener().getNoPhantomSpawns().remove(p);
        fightingListener.getActiveBerserkers().remove(p);
    }

    public void playerJoin(Player p, boolean overrideNewPlayer) {
        timerTracker.put(p, new ArrayList<>());

        File dataFile = new File(getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) saveResource("playerdata.yml", true);
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);

        boolean newPlayer = data.get(p.getUniqueId().toString()) == null;
        if (overrideNewPlayer) newPlayer = false;
        loadData(p, data);

        // Check if the player should activate any nearby trophies
        for (Map.Entry<Location, Trophy> trophy : trophies.entrySet()) {
            Location loc = trophy.getKey();
            if (!p.getWorld().equals(loc.getWorld())) continue;
            if (p.getLocation().distance(loc) > 50) continue;
            trophy.getValue().getEffects().checkForPlayers();
        }

        // Make sure the main XP level is correct, load the player's rewards, add them to active leaderboard players
        // Load their perma trash inventory, and hide any glowing blocks from other player's spelunker ability
        checkMainXP(p);
        loadPlayerRewards(p);
        Leaderboard.leaderboardJoin(this, p);
        loadPermaTrash(p);
        getMiningListener().hideGlowForPlayer(p);

        // Handle the scoreboard
        if (newPlayer) SkillScoreboard.initializeScoreboard(this, p);
        else if (toggledScoreboard.containsKey(p.getUniqueId()) && toggledScoreboard.get(p.getUniqueId()))
            SkillScoreboard.initializeScoreboard(this, p);
        else SkillScoreboard.hideScoreboard(this, p);
    }

    /**
     * Counts how many trophies a player has obtained and returns the
     * max level a player can be
     */
    public int playerMaxSkillLevel(UUID uuid) {
        int count = 0;
        if (trophyTracker.get(uuid) == null)  {
            // Try to load the player's trophies after a delay
            new BukkitRunnable() {
                @Override
                public void run() {
                    loadTrophies();
                }
            }.runTaskLater(this, 20);
            return 10;
        }
        for (Map.Entry<String, Boolean> list : trophyTracker.get(uuid).entrySet()) if (list.getValue()) count++;
        return 10 + (count * 10);
    }

    public int generateTrophyID() {
        int id = (int) Math.ceil(Math.random() * 1000000);
        if (trophies.isEmpty()) return id;
        for (Map.Entry<Location, Trophy> trophy : trophies.entrySet()) if (trophy.getValue().getID() == id) return generateTrophyID();
        return id;
    }

    public void removeTrophy(Location loc) {
        Trophy trophy = trophies.get(loc);
        trophies.remove(loc);
        if (trophyData.get("" + trophy.getID()) == null) return;
        trophyData.set("" + trophy.getID(), null);
    }

    public int getRewardLevel(String type, String reward) {
        for (Reward r : playerRewards.getRewardList().get(type)) {
            if (r.getName().equalsIgnoreCase(reward)) return r.getLevel();
        }
        return 0;
    }

    public ArrayList<Material> getFarmingList() {
        return farmingList;
    }

    public HashMap<UUID, Boolean> getToggledScoreboard() {
        return toggledScoreboard;
    }

    public HashMap<Location, Trophy> getTrophies() {
        return trophies;
    }

    public ItemStack getTrophyItem(int type) {
        if (!trophyItems.containsKey(type)) return new ItemStack(Material.AIR);
        return trophyItems.get(type);
    }

    public HashMap<Integer, ItemStack> getTrophyItems() {
        return trophyItems;
    }

    public HashMap<UUID, ArrayList<Skill>> getPlayerSkills() {
        return playerSkills;
    }

    public PlayerRewards getPlayerRewards(Player p) {
        if (!rewardTracker.containsKey(p)) {
            loadPlayerRewards(p);
            return rewardTracker.get(p);
        }
        return rewardTracker.get(p);
    }

    public HashMap<Player, ArrayList<AbilityTimer>> getTimerTracker() {
        return timerTracker;
    }

    public void endPlayerTimers(Player p) {
        if (!timerTracker.containsKey(p)) return;
        for (AbilityTimer timer : timerTracker.get(p)) timer.endAbility();
    }

    public void addAbility(Player p, AbilityTimer timer) {
        timerTracker.computeIfAbsent(p, k -> new ArrayList<>());
        timerTracker.get(p).add(timer);
    }

    public void removeAbility(Player p, String ability) {
        if (!timerTracker.containsKey(p)) return;
        timerTracker.get(p).removeIf(timer -> timer.getName().equalsIgnoreCase(ability));
    }

    public AbilityTimer getAbility(Player p, String ability) {
        if (!timerTracker.containsKey(p)) return null;
        for (AbilityTimer timer : timerTracker.get(p)) if (timer.getName().equalsIgnoreCase(ability)) return timer;
        return null;
    }

    public void checkMainXP(Player p) {
        if (!playerSkills.containsKey(p.getUniqueId())) return;
        double totalXP = 0;
        for (Skill skill : playerSkills.get(p.getUniqueId())) {
            if (skill.getSkillName().equalsIgnoreCase("Main")) continue;
            totalXP += skill.getExperience();
        }
        
        totalXP /= 7.0;
        Skill main = getSkill(p.getUniqueId(), "Main");
        if (main.getExperience() < totalXP || main.getExperience() > totalXP + 10.0) {
            main.setExperience((int) totalXP);
            savePlayerData(p);
        }
    }

    /**
     * Returns true if there is a claim there
     */
    public boolean checkForClaim(Player p, Location loc) {
        String noBuildReason = GriefPrevention.instance.allowBuild(p, loc);
        return (noBuildReason != null);
    }

    public void runSkillAutoSave() {
        new BukkitRunnable() {
            @Override
            public void run() {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            savePlayerData();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }.runTask(SurvivalSkills.this);
            }
        }.runTaskTimerAsynchronously(this, 900, 900);
    }

    public void createTrails() {
        trails.put("Dust", Particle.DUST);
        trails.put("Water", Particle.SPLASH);
        trails.put("Happy", Particle.HAPPY_VILLAGER);
        trails.put("Dragon", Particle.DRAGON_BREATH);
        trails.put("Electric", Particle.ELECTRIC_SPARK);
        trails.put("Enchantment", Particle.ENCHANT);
        trails.put("Ominous", Particle.TRIAL_OMEN);
        trails.put("Love", Particle.HEART);
        trails.put("Flame", Particle.FLAME);
        trails.put("BlueFlame", Particle.SOUL_FIRE_FLAME);
        trails.put("Cherry", Particle.CHERRY_LEAVES);
        trails.put("Rainbow", Particle.DUST);
    }

    public Enchantment getEnchantFromKey(String key) {
        for (Enchantment enchant : Registry.ENCHANTMENT) {
            if (enchant.getKey().toString().equalsIgnoreCase(key)) return enchant;
        }
        return Enchantment.EFFICIENCY;
    }

    public PlayerRewards getDefaultPlayerRewards() {
        return playerRewards;
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

    public HashMap<UUID, HashMap<String, Boolean>> getTrophyTracker() {
        return trophyTracker;
    }

    public ArrayList<NamespacedKey> getRecipeKeys() {
        return recipeKeys;
    }

    public HashMap<Player, TrailEffect> getTrailTracker() {
        return trailTracker;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }

    public boolean isGriefPreventionEnabled() {
        return griefPreventionEnabled;
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

    public HashMap<String, Particle> getTrails() {
        return trails;
    }

    public HashMap<Player, Scoreboard> getScoreboardTracker() {
        return scoreboardTracker;
    }

    public FileConfiguration getLeaderboardData() {
        return leaderboardData;
    }
}
