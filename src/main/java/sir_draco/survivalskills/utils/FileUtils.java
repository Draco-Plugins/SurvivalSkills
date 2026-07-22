package sir_draco.survivalskills.utils;

import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.abilities.AutoTrash;
import sir_draco.survivalskills.abilities.TrailEffect;
import sir_draco.survivalskills.abilities.godItems.TeleporterAnchor;
import sir_draco.survivalskills.boards.LeaderboardPlayer;
import sir_draco.survivalskills.pipes.PipeManager;
import sir_draco.survivalskills.skills.Skill;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.skills.SkillManager;
import sir_draco.survivalskills.skills.SkillsHolder;
import sir_draco.survivalskills.skill_listeners.AutoEatMode;
import sir_draco.survivalskills.trophy.TrophyType;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;

@SuppressWarnings("deprecation")
public class FileUtils {
    public static final String CONFIG_YML = "config.yml";
    public static final String BIG_TRASH = ".BigTrash";
    public static final String PLAYER = "Player ";
    public static final String PLAYERDATA_YML = "playerdata.yml";
    public static final String PIPEDATA_YML = "pipedata.yml";
    public static final String GODQUESTS_YML = "godquests.yml";
    public static final String SCOREBOARD = ".Scoreboard";
    public static final String NO_BOSS_MUSIC = ".NoBossMusic";
    public static final String BLOODY_DOMAIN = ".BloodyDomain";
    public static final String PEACEFUL_MINER = ".PeacefulMiner";
    public static final String VEINMINER = ".Veinminer";
    public static final String AUTO_EAT = ".AutoEat";
    public static final String AUTO_EAT_BLACKLIST = ".AutoEatBlacklistedFoods";
    public static final String AUTO_EAT_MODE = ".AutoEatMode";
    public static final String TRAIL = ".Trail";
    public static final String NO_PHANTOMS = ".NoPhantoms";

    private static final double CURRENT_CONFIG_VERSION = 2.32;

    private FileUtils() {
        // Prevent instantiation
    }

    public static void loadFiles() {
        SurvivalSkills plugin = SurvivalSkills.getInstance();

        // Load main config
        plugin.saveDefaultConfig();
        File configFile = new File(plugin.getDataFolder(), CONFIG_YML);
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        plugin.setConfig(config);

        if (requiresConfigUpdate(config))
            updateConfig(config);
        plugin.setSkillManager(new SkillManager(plugin));

        // Load data files
        loadDataFile("trophydata.yml", plugin::setTrophyFile, plugin::setTrophyData);
        loadDataFile("leaderboard.yml", plugin::setLeaderboardFile, plugin::setLeaderboardData);
        loadLeaderboard(plugin.getLeaderboardData(), plugin.getLeaderboardTracker());
        loadDataFile("permatrash.yml", plugin::setPermaTrashFile, plugin::setPermaTrashData);
        loadDataFile("toolbelt.yml", plugin::setToolBeltFile, plugin::setToolBeltData);
    }

    static boolean requiresConfigUpdate(FileConfiguration config) {
        return config.get("Version") == null || config.getDouble("Version") != CURRENT_CONFIG_VERSION;
    }

    public static void loadPipeData(PipeManager pipeManager) {
        Objects.requireNonNull(pipeManager).load();
    }

    public static void savePipeData(PipeManager pipeManager) {
        Objects.requireNonNull(pipeManager).requestSave();
    }

    public static void savePipeDataNow(PipeManager pipeManager) {
        Objects.requireNonNull(pipeManager).saveNow();
    }

    private static void loadDataFile(String fileName,
            Consumer<File> fileSetter,
            Consumer<FileConfiguration> configSetter) {
        File file = new File(SurvivalSkills.getInstance().getDataFolder(), fileName);
        if (!file.exists())
            SurvivalSkills.getInstance().saveResource(fileName, true);
        fileSetter.accept(file);
        configSetter.accept(YamlConfiguration.loadConfiguration(file));
    }

    public static void updateConfig(FileConfiguration config) {
        InputStream defConfigStream = SurvivalSkills.getInstance().getClass().getClassLoader()
                .getResourceAsStream(CONFIG_YML);
        if (defConfigStream == null) {
            Bukkit.getLogger().log(Level.SEVERE, "Default config file not found in JAR");
            return;
        }
        YamlConfiguration defConfig = YamlConfiguration
                .loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));

        mergeConfigWithOrder(config, defConfig, "");
        config.set("Version", CURRENT_CONFIG_VERSION);

        File file = new File(SurvivalSkills.getInstance().getDataFolder(), CONFIG_YML);
        try {
            config.save(file);
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save updated config file", e);
        }
    }

    /**
     * Merges an existing config with a new default config, preserving user values
     * while adding new defaults.
     * The existing config is modified to include any new keys from the default
     * config.
     *
     * @param existingConfig The current config file with user customizations (will
     *                       be modified)
     * @param defaultConfig  The new default config with potentially new keys and
     *                       structure
     * @param parentKey      Used for recursive calls to track the current path
     */
    private static void mergeConfigWithOrder(ConfigurationSection existingConfig, ConfigurationSection defaultConfig,
            String parentKey) {
        for (String key : defaultConfig.getKeys(false)) {
            String fullKey = parentKey.isEmpty() ? key : parentKey + "." + key;

            if (defaultConfig.isConfigurationSection(key) && existingConfig.isConfigurationSection(key)) {
                handleBothSectionsHaveKey(existingConfig, defaultConfig, key, fullKey);
            } else if (defaultConfig.isConfigurationSection(key) && !existingConfig.contains(key)) {
                handleKeyInDefaultButNotExisting(existingConfig, defaultConfig, key, fullKey);
            } else if (!existingConfig.contains(key)) {
                Object defaultValue = defaultConfig.get(key);
                if (defaultValue != null) {
                    existingConfig.set(key, defaultValue);
                    Bukkit.getLogger().log(Level.INFO,
                            String.format("[SurvivalSkills] Added new config key: %s = %s", fullKey, defaultValue));
                }
            }
        }
    }

    private static void handleKeyInDefaultButNotExisting(ConfigurationSection existingConfig,
            ConfigurationSection defaultConfig, String key, String fullKey) {
        ConfigurationSection defaultSection = defaultConfig.getConfigurationSection(key);
        if (defaultSection != null) {
            ConfigurationSection newSection = existingConfig.createSection(key);
            mergeConfigWithOrder(newSection, defaultSection, fullKey);
            Bukkit.getLogger().log(Level.INFO,
                    String.format("[SurvivalSkills] Added new config section: %s", fullKey));
        }
    }

    private static void handleBothSectionsHaveKey(ConfigurationSection existingConfig,
            ConfigurationSection defaultConfig, String key, String fullKey) {
        ConfigurationSection existingSection = existingConfig.getConfigurationSection(key);
        ConfigurationSection defaultSection = defaultConfig.getConfigurationSection(key);

        if (existingSection != null && defaultSection != null) {
            mergeConfigWithOrder(existingSection, defaultSection, fullKey);
        } else {
            Bukkit.getLogger().log(Level.WARNING,
                    String.format("[SurvivalSkills] Failed to merge section: %s", fullKey));
        }
    }

    public static void loadLeaderboard(FileConfiguration leaderboardData,
            Map<UUID, LeaderboardPlayer> leaderboardTracker) {
        ConfigurationSection section = leaderboardData.getConfigurationSection("");
        if (section == null) return;

        section.getKeys(false).forEach(key -> {
            EnumMap<SkillCategory, Integer> scores = new EnumMap<>(SkillCategory.class);
            for (SkillCategory cat : SkillCategory.allSkills()) {
                scores.put(cat, leaderboardData.getInt(key + "." + cat.getDisplayName().replace(" ", "")));
            }
            String name = leaderboardData.getString(key + ".Name");
            leaderboardTracker.put(UUID.fromString(key), new LeaderboardPlayer(name, scores));
        });
    }

    public static void loadPermaTrash(Player p, FileConfiguration permaTrashData) {
        UUID uuid = p.getUniqueId();
        ConfigurationSection perma = permaTrashData.getConfigurationSection(uuid.toString());
        if (perma == null)
            return;

        AutoTrash trash = loadTrashMaterials(uuid, permaTrashData);
        if (loadTrashEnchants(uuid, trash, permaTrashData))
            return;

        SurvivalSkills.getInstance().getFishingListener().getPermaTrash().put(p, trash);
    }

    private static AutoTrash loadTrashMaterials(UUID uuid, FileConfiguration permaTrashData) {
        ConfigurationSection materials = permaTrashData.getConfigurationSection(uuid + ".Materials");
        boolean big = permaTrashData.contains(uuid + BIG_TRASH)
                && permaTrashData.getBoolean(uuid + BIG_TRASH);
        AutoTrash trash = new AutoTrash(big, true);
        if (materials != null) {
            materials.getKeys(false).forEach(key -> {
                String type = permaTrashData.getString(uuid + ".Materials." + key);
                if (type != null) {
                    Material material = Material.getMaterial(type);
                    if (material == null) {
                        Bukkit.getLogger().warning(String.format("Material %s for %s is not valid", key, uuid));
                        return;
                    }
                    if (trash.getTrashMaterials().contains(material))
                        return;
                    trash.addTrashItem(new ItemStack(material));
                } else {
                    Bukkit.getLogger().warning(String.format("Material %s for %s is not valid", key, uuid));
                }
            });
        }
        return trash;
    }

    private static boolean loadTrashEnchants(UUID uuid, AutoTrash trash, FileConfiguration permaTrashData) {
        ConfigurationSection enchants = permaTrashData.getConfigurationSection(uuid + ".Enchants");
        if (enchants == null)
            return false;

        ArrayList<String> keyNames = new ArrayList<>();
        enchants.getKeys(false).forEach(key -> {
            String keyName = permaTrashData.getString(uuid + ".Enchants." + key);
            keyNames.add(keyName);
        });

        for (String key : keyNames) {
            Enchantment enchant = getEnchantFromKey(key);
            if (enchant == null) {
                Bukkit.getLogger().log(Level.WARNING,
                        String.format("Enchantment %s for %s is not valid", key, uuid));
                return true;
            }

            ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) item.getItemMeta();
            if (meta == null)
                return true;
            meta.addStoredEnchant(enchant, 1, false);
            item.setItemMeta(meta);
            trash.addTrashItem(item);
        }
        return false;
    }

    public static Enchantment getEnchantFromKey(String key) {
        for (Enchantment enchant : Registry.ENCHANTMENT) {
            if (enchant.getKey().toString().equalsIgnoreCase(key))
                return enchant;
        }
        return null;
    }

    public static void loadData(Player p, FileConfiguration data) {
        SurvivalSkills plugin = SurvivalSkills.getInstance();
        HashMap<TrophyType, Boolean> trophyList = new HashMap<>();
        if (!data.contains(p.getUniqueId().toString())) {
            ArrayList<Skill> skills = new ArrayList<>();
            for (TrophyType type : TrophyType.values()) {
                trophyList.put(type, false);
            }

            skills.add(new Skill(0, 1, SkillCategory.MAIN));
            skills.add(new Skill(0, 1, SkillCategory.BUILDING));
            skills.add(new Skill(0, 1, SkillCategory.MINING));
            skills.add(new Skill(0, 1, SkillCategory.FISHING));
            skills.add(new Skill(0, 1, SkillCategory.EXPLORING));
            skills.add(new Skill(0, 1, SkillCategory.FARMING));
            skills.add(new Skill(0, 1, SkillCategory.FIGHTING));
            skills.add(new Skill(0, 1, SkillCategory.CRAFTING));

            plugin.getTrophyManager().getTrophyTracker().put(p.getUniqueId(), trophyList);
            SkillsHolder holder = new SkillsHolder(skills, plugin.getSkillManager().getNewPlayerRewards());
            holder.setMaxSkillMessageEnabled(true);
            if (!plugin.getSkillManager().getPlayerSkills().containsKey(p.getUniqueId()))
                plugin.getSkillManager().getPlayerSkills().put(p.getUniqueId(), holder);
            else
                Bukkit.getLogger().warning(PLAYER + p.getName() + " already has skills loaded");
            plugin.getShowScoreboard().put(p.getUniqueId(), true);

            savePlayerData(p);
            return;
        }

        UUID uuid = p.getUniqueId();
        if (!plugin.getSkillManager().getPlayerSkills().containsKey(uuid))
            plugin.getSkillManager().loadPlayerSkills(uuid, data);
        if (!plugin.getShowScoreboard().containsKey(uuid))
            loadScoreboardSetting(uuid, data);
        if (!plugin.getTrophyManager().getTrophyTracker().containsKey(uuid))
            plugin.getTrophyManager().loadPlayerTrophies(uuid, data);

        loadUserData(p, data, uuid);
    }

    private static void loadUserData(Player p, FileConfiguration data, UUID uuid) {
        loadPhantoms(p, data, uuid);
        loadTrails(p, data, uuid);
        loadAutoEat(p, data, uuid);
        loadVeinMiner(p, data, uuid);
        loadPeacefulMiner(p, data, uuid);
        loadMaxSkillMessage(data, uuid);
        loadBloodyDomain(p, data, uuid);
        loadBossMusic(p, data, uuid);
    }

    private static void loadBossMusic(Player p, FileConfiguration data, UUID uuid) {
        if (data.contains(uuid + NO_BOSS_MUSIC)) {
            boolean bossMusic = data.getBoolean(uuid + NO_BOSS_MUSIC);
            if (bossMusic)
                SurvivalSkills.getInstance().getFightingListener().getNoBossMusic().add(p);
        }
    }

    private static void loadBloodyDomain(Player p, FileConfiguration data, UUID uuid) {
        if (data.contains(uuid + BLOODY_DOMAIN)) {
            boolean bloodyDomain = data.getBoolean(uuid + BLOODY_DOMAIN);
            if (bloodyDomain)
                SurvivalSkills.getInstance().getAbilityManager().startBloodyDomain(p);
        }
    }

    private static void loadMaxSkillMessage(FileConfiguration data, UUID uuid) {
        if (data.contains(uuid + ".MaxSkillMessage")) {
            boolean maxSkillMessage = data.getBoolean(uuid + ".MaxSkillMessage");
            if (SurvivalSkills.getInstance().getSkillManager().getPlayerSkills().containsKey(uuid))
                SurvivalSkills.getInstance().getSkillManager().getPlayerSkills().get(uuid)
                        .setMaxSkillMessageEnabled(maxSkillMessage);
        }
    }

    private static void loadPeacefulMiner(Player p, FileConfiguration data, UUID uuid) {
        if (data.contains(uuid + PEACEFUL_MINER)) {
            boolean peacefulMiner = data.getBoolean(uuid + PEACEFUL_MINER);
            if (peacefulMiner && !SurvivalSkills.getInstance().getMiningListener().getPeacefulMiners().contains(p))
                SurvivalSkills.getInstance().getMiningListener().getPeacefulMiners().add(p);
        }
    }

    private static void loadVeinMiner(Player p, FileConfiguration data, UUID uuid) {
        if (data.contains(uuid + VEINMINER)) {
            boolean noHunger = data.getBoolean(uuid + VEINMINER);
            SurvivalSkills.getInstance().getMiningListener().getVeinminerTracker().put(p, noHunger);
        }
    }

    private static void loadAutoEat(Player p, FileConfiguration data, UUID uuid) {
        if (data.contains(uuid + AUTO_EAT)) {
            boolean autoEat = data.getBoolean(uuid + AUTO_EAT);
            if (autoEat && !SurvivalSkills.getInstance().getFarmingListener().getAutoEat().contains(p))
                SurvivalSkills.getInstance().getFarmingListener().getAutoEat().add(p);
        }
        Set<Material> blacklistedFoods = data.getStringList(uuid + AUTO_EAT_BLACKLIST).stream()
                .map(Material::matchMaterial)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        SurvivalSkills.getInstance().getFarmingListener().setBlacklistedFoods(p, blacklistedFoods);

        String storedMode = data.getString(uuid + AUTO_EAT_MODE);
        if (storedMode == null) return;
        try {
            SurvivalSkills.getInstance().getFarmingListener().setAutoEatMode(p, AutoEatMode.valueOf(storedMode));
        } catch (IllegalArgumentException exception) {
            Bukkit.getLogger().log(Level.WARNING,
                    String.format("[SurvivalSkills] Ignoring invalid Auto Eat mode for player %s: %s", p.getName(), storedMode),
                    exception);
        }
    }

    private static void loadPhantoms(Player p, FileConfiguration data, UUID uuid) {
        if (data.contains(uuid + NO_PHANTOMS)) {
            boolean phantoms = data.getBoolean(uuid + NO_PHANTOMS);
            if (phantoms && !SurvivalSkills.getInstance().getFightingListener().getNoPhantomSpawns().contains(p)) {
                SurvivalSkills.getInstance().getFightingListener().getNoPhantomSpawns().add(p);
            }
        }
    }

    private static void loadTrails(Player p, FileConfiguration data, UUID uuid) {
        if (data.contains(uuid + TRAIL)) {
            String trailName = data.getString(uuid + TRAIL);
            if (trailName != null
                    && !trailName.equals("None")
                    && SurvivalSkills.getInstance().getAbilityManager().getTrails().containsKey(trailName)) {
                int dustType = 1;
                if (trailName.equalsIgnoreCase("Dust"))
                    dustType = 2;
                else if (trailName.equalsIgnoreCase("Rainbow"))
                    dustType = 3;
                TrailEffect effect = new TrailEffect(p,
                        SurvivalSkills.getInstance().getAbilityManager().getTrails().get(trailName), dustType,
                        trailName);
                effect.runTaskTimer(SurvivalSkills.getInstance(), 60, 1);
                SurvivalSkills.getInstance().getAbilityManager().getTrailTracker().put(p, effect);
            }
        }
    }

    public static void loadScoreboardSetting(UUID uuid, FileConfiguration data) {
        if (SurvivalSkills.getInstance().getShowScoreboard().containsKey(uuid))
            return;
        if (!data.contains(uuid + SCOREBOARD)) {
            SurvivalSkills.getInstance().getShowScoreboard().put(uuid, true);
        } else {
            SurvivalSkills.getInstance().getShowScoreboard().put(uuid, data.getBoolean(uuid + SCOREBOARD));
        }
    }

    /**
     * Loads all teleport anchors from the teleportanchors.yml file.
     */
    public static void loadTeleportAnchors(Map<Location, TeleporterAnchor> anchors) {
        File file = new File(SurvivalSkills.getInstance().getDataFolder(), "teleportanchors.yml");
        if (!file.exists()) {
            Bukkit.getLogger().log(Level.INFO,
                    "[SurvivalSkills] No teleport anchors file found, creating file");
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        if (!config.contains("TeleportAnchors")) {
            return;
        }

        ConfigurationSection section = config.getConfigurationSection("TeleportAnchors");
        if (section == null) {
            Bukkit.getLogger().log(Level.WARNING,
                    "[SurvivalSkills] Invalid teleport anchors section in config");
            return;
        }

        int loadedCount = 0;
        int skippedCount = 0;

        for (String key : section.getKeys(false)) {
            try {
                String name = config.getString("TeleportAnchors." + key + ".Name");
                Location location = config.getLocation("TeleportAnchors." + key + ".Location");
                String ownerString = config.getString("TeleportAnchors." + key + ".Owner");

                if (name == null || location == null || ownerString == null) {
                    Bukkit.getLogger().log(Level.WARNING,
                            String.format("[SurvivalSkills] Skipping invalid anchor data for key: %s", key));
                    skippedCount++;
                    continue;
                }

                UUID ownerId = UUID.fromString(ownerString);

                if (location.getWorld() == null ||
                        location.getBlock().getType() != Material.RESPAWN_ANCHOR) {
                    Bukkit.getLogger().log(Level.WARNING,
                            String.format("[SurvivalSkills] Skipping anchor '%s' - block no longer exists at location",
                                    name));
                    skippedCount++;
                    continue;
                }

                TeleporterAnchor anchor = new TeleporterAnchor(name, location, ownerId);
                anchors.put(location, anchor);
                loadedCount++;

            } catch (Exception e) {
                Bukkit.getLogger().log(Level.WARNING,
                        String.format("[SurvivalSkills] Error loading teleport anchor for key: %s", key), e);
                skippedCount++;
            }
        }

        Bukkit.getLogger().log(Level.INFO,
                String.format("[SurvivalSkills] Loaded %d teleport anchors (%d skipped)",
                        loadedCount, skippedCount));
    }

    public static void savePlayerData(Player p) {
        SurvivalSkills plugin = SurvivalSkills.getInstance();
        if (plugin.getSkillManager().getPlayerSkills().isEmpty())
            return;

        File dataFile = new File(plugin.getDataFolder(), PLAYERDATA_YML);
        if (!dataFile.exists())
            plugin.saveResource(PLAYERDATA_YML, true);
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);

        File godQuestFile = new File(plugin.getDataFolder(), GODQUESTS_YML);
        if (!godQuestFile.exists())
            plugin.saveResource(GODQUESTS_YML, true);
        FileConfiguration godQuestData = YamlConfiguration.loadConfiguration(godQuestFile);

        UUID uuid = p.getUniqueId();
        plugin.getTrophyManager().savePlayerTrophyData(uuid, data);
        if (SkillManager.getSkill(uuid, SkillCategory.MAIN).getLevel() == Skill.MAX_LEVEL)
            plugin.getTrophyManager().savePlayerGodQuestData(uuid, godQuestData);

        savePlayerSettings(p, uuid, data);

        plugin.getAbilityManager().saveFlightTimer(p, data);
        plugin.getAbilityManager().saveSpelunkerTimer(p, data);

        plugin.getSkillManager().savePlayerSkillData(uuid, data);
        plugin.getSkillManager().savePlayerMultiplier(p, data);
        savePermaTrash(p, plugin.getPermaTrashData(), plugin.getPermaTrashFile());

        try {
            data.save(dataFile);
            godQuestData.save(godQuestFile);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, String.format("Failed to save player data for %s", p.getName()), e);
        }
    }

    public static void savePlayerData() {
        SurvivalSkills plugin = SurvivalSkills.getInstance();
        if (plugin.getSkillManager().getPlayerSkills().isEmpty())
            return;

        File dataFile = new File(plugin.getDataFolder(), PLAYERDATA_YML);
        if (!dataFile.exists())
            plugin.saveResource(PLAYERDATA_YML, true);
        FileConfiguration data = YamlConfiguration.loadConfiguration(dataFile);

        File godQuestFile = new File(plugin.getDataFolder(), GODQUESTS_YML);
        if (!godQuestFile.exists())
            plugin.saveResource(GODQUESTS_YML, true);
        FileConfiguration godQuestData = YamlConfiguration.loadConfiguration(godQuestFile);

        plugin.getTrophyManager().saveTrophyData(data);
        plugin.getTrophyManager().saveGodQuestData(godQuestData);
        plugin.getSkillManager().saveSkillData(data);

        for (Player p : Bukkit.getOnlinePlayers()) {
            UUID uuid = p.getUniqueId();
            savePlayerSettings(p, uuid, data);
            plugin.getAbilityManager().saveFlightTimer(p, data);
            plugin.getAbilityManager().saveSpelunkerTimer(p, data);
            plugin.getSkillManager().savePlayerMultiplier(p, data);
        }

        saveToFile(data, dataFile, "Failed to save player data");
        saveToFile(godQuestData, godQuestFile, "Failed to save god quest data");
    }

    private static void savePlayerSettings(Player p, UUID uuid, FileConfiguration data) {
        SurvivalSkills plugin = SurvivalSkills.getInstance();

        if (plugin.getShowScoreboard().containsKey(uuid))
            data.set(uuid + SCOREBOARD, plugin.getShowScoreboard().get(uuid));
        else
            Bukkit.getLogger().warning(PLAYER + p.getName() + " does not have a scoreboard status");

        if (plugin.getAbilityManager().getTrailTracker().containsKey(p))
            data.set(uuid + TRAIL, plugin.getAbilityManager().getTrailTracker().get(p).getTrailName());
        else
            data.set(uuid + TRAIL, "None");

        data.set(uuid + NO_PHANTOMS, plugin.getFightingListener().getNoPhantomSpawns().contains(p));
        data.set(uuid + AUTO_EAT, plugin.getFarmingListener().getAutoEat().contains(p));
        data.set(uuid + AUTO_EAT_BLACKLIST, plugin.getFarmingListener().getBlacklistedFoods(p).stream()
                .map((Material material) -> material.name()).sorted().toList());
        data.set(uuid + AUTO_EAT_MODE, plugin.getFarmingListener().getAutoEatMode(p).name());
        Boolean veinminer = plugin.getMiningListener().getVeinminerTracker().get(p);
        if (veinminer != null)
            data.set(uuid + VEINMINER, veinminer);
        data.set(uuid + PEACEFUL_MINER, plugin.getMiningListener().getPeacefulMiners().contains(p));
        data.set(uuid + BLOODY_DOMAIN, plugin.getAbilityManager().getBloodyDomainTracker().containsKey(p));
        data.set(uuid + NO_BOSS_MUSIC, plugin.getFightingListener().getNoBossMusic().contains(p));
    }

    public static void saveLeaderboard(Map<UUID, LeaderboardPlayer> leaderboardTracker,
            FileConfiguration leaderboardData, File leaderboardFile) {
        if (leaderboardTracker.isEmpty()) return;
        if (leaderboardData == null) return;

        for (Map.Entry<UUID, LeaderboardPlayer> player : leaderboardTracker.entrySet()) {
            LeaderboardPlayer lp = player.getValue();
            leaderboardData.set(player.getKey() + ".Name", lp.getName());
            leaderboardData.set(player.getKey() + ".Level", lp.getScore());
            for (SkillCategory cat : SkillCategory.allSkills()) {
                leaderboardData.set(
                        player.getKey() + "." + cat.getDisplayName().replace(" ", ""),
                        lp.getScore(cat));
            }
        }

        saveToFile(leaderboardData, leaderboardFile, "Failed to save leaderboard file");
    }

    public static void savePermaTrash(Player p, FileConfiguration permaTrashData, File permaTrashFile) {
        if (permaTrashData == null)
            return;
        UUID uuid = p.getUniqueId();
        if (!SurvivalSkills.getInstance().getFishingListener().getPermaTrash().containsKey(p))
            return;
        AutoTrash trash = SurvivalSkills.getInstance().getFishingListener().getPermaTrash().get(p);
        if (trash == null)
            return;

        permaTrashData.set(uuid.toString(), null);
        permaTrashData.set(uuid + BIG_TRASH, trash.isBig());

        int i = 0;
        if (trash.getTrashMaterials().isEmpty())
            permaTrashData.set(uuid + ".Materials", null);
        for (Material mat : trash.getTrashMaterials()) {
            permaTrashData.set(uuid + ".Materials." + i, mat.toString());
            i++;
        }

        i = 0;
        if (trash.getEnchants().isEmpty())
            permaTrashData.set(uuid + ".Enchants", null);
        for (Enchantment enchant : trash.getEnchants()) {
            permaTrashData.set(uuid + ".Enchants." + i, enchant.getKey().toString());
            i++;
        }

        saveToFile(permaTrashData, permaTrashFile, "Failed to save perma trash file");
    }

    public static void savePotionBags() {
        SurvivalSkills plugin = SurvivalSkills.getInstance();
        File potionBagFile = new File(plugin.getDataFolder(), "potionbags.yml");
        if (!potionBagFile.exists())
            plugin.saveResource("potionbags.yml", true);
        FileConfiguration potionBagData = YamlConfiguration.loadConfiguration(potionBagFile);
        plugin.getGodListener().savePotionBags(potionBagData);

        saveToFile(potionBagData, potionBagFile, "Failed to save potion bags file");
    }

    public static void savePowerOreConversions() {
        SurvivalSkills plugin = SurvivalSkills.getInstance();
        File powerOreFile = new File(plugin.getDataFolder(), "poweroreconversions.yml");
        if (!powerOreFile.exists())
            plugin.saveResource("poweroreconversions.yml", true);
        FileConfiguration powerOreData = YamlConfiguration.loadConfiguration(powerOreFile);
        plugin.getGodListener().savePowerOreConversions(powerOreData);

        saveToFile(powerOreData, powerOreFile, "Failed to save power ore conversions file");
    }

    /**
     * Saves all teleport anchors to the teleportanchors.yml file.
     */
    public static void saveTeleportAnchors(Map<Location, TeleporterAnchor> teleportAnchors) {
        File file = new File(SurvivalSkills.getInstance().getDataFolder(), "teleportanchors.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        config.set("TeleportAnchors", null);

        if (teleportAnchors.isEmpty()) {
            saveToFile(config, file, "Failed to save teleport anchors file");
            return;
        }

        for (Map.Entry<Location, TeleporterAnchor> entry : teleportAnchors.entrySet()) {
            Location loc = entry.getKey();
            TeleporterAnchor anchor = entry.getValue();

            if (loc.getWorld() == null)
                continue;

            String path = "TeleportAnchors." + anchor.name();
            config.set(path + ".Location", loc);
            config.set(path + ".Owner", anchor.ownerId().toString());
        }

        saveToFile(config, file, "Failed to save teleport anchors file");
        Bukkit.getLogger().log(Level.INFO,
                String.format("[SurvivalSkills] Saved %d teleport anchors", teleportAnchors.size()));
    }

    public static String loadSpawnRegionName() {
        FileConfiguration config = SurvivalSkills.getInstance().getTrueConfig();

        String spawnName = config.getString("WorldGuardSpawnRegion");
        if (spawnName == null)
            return "spawn";
        return spawnName;
    }

    private static void saveToFile(FileConfiguration data, File file, String errorMessage) {
        try {
            data.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, errorMessage, e);
        }
    }
}
