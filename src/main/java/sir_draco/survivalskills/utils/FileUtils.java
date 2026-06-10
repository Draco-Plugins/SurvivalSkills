package sir_draco.survivalskills.utils;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.Plugin;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.abilities.AutoTrash;
import sir_draco.survivalskills.abilities.TrailEffect;
import sir_draco.survivalskills.abilities.godItems.TeleporterAnchor;
import sir_draco.survivalskills.boards.LeaderboardPlayer;
import sir_draco.survivalskills.commands.admin_commands.*;
import sir_draco.survivalskills.commands.default_commands.*;
import sir_draco.survivalskills.commands.skill_commands.*;
import sir_draco.survivalskills.skills.Skill;
import sir_draco.survivalskills.skills.SkillManager;
import sir_draco.survivalskills.skills.SkillsHolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

@SuppressWarnings("deprecation")
public class FileUtils {
    public static final String CONFIG_YML = "config.yml";
    public static final String CONFIG_UPDATE = "ConfigUpdate";
    public static final String BIG_TRASH = ".BigTrash";
    public static final String PLAYER = "Player ";
    public static final String PLAYERDATA_YML = "playerdata.yml";
    public static final String GODQUESTS_YML = "godquests.yml";
    public static final String SCOREBOARD = ".Scoreboard";
    public static final String NO_BOSS_MUSIC = ".NoBossMusic";
    public static final String BLOODY_DOMAIN = ".BloodyDomain";
    public static final String PEACEFUL_MINER = ".PeacefulMiner";
    public static final String VEINMINER = ".Veinminer";
    public static final String AUTO_EAT = ".AutoEat";
    public static final String TRAIL = ".Trail";
    public static final String NO_PHANTOMS = ".NoPhantoms";

    private FileUtils() {
        // Prevent instantiation
    }

    public static void loadFiles() {
        // See if the config has ever been saved before
        SurvivalSkills.getInstance().saveDefaultConfig();
        File configFile = new File(SurvivalSkills.getInstance().getDataFolder(), CONFIG_YML);
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        SurvivalSkills.getInstance().setConfig(config);

        // See if an update needs to be made to the config
        if (config.get("Version") == null || config.getDouble("Version") != 2.22)
            updateConfig(config);
        SurvivalSkills.getInstance().setSkillManager(new SkillManager(SurvivalSkills.getInstance()));

        File trophyFile = new File(SurvivalSkills.getInstance().getDataFolder(), "trophydata.yml");
        SurvivalSkills.getInstance().setTrophyFile(trophyFile);
        if (!trophyFile.exists())
            SurvivalSkills.getInstance().saveResource("trophydata.yml", true);
        FileConfiguration trophyData = YamlConfiguration.loadConfiguration(trophyFile);
        SurvivalSkills.getInstance().setTrophyData(trophyData);

        File leaderboardFile = new File(SurvivalSkills.getInstance().getDataFolder(), "leaderboard.yml");
        SurvivalSkills.getInstance().setLeaderboardFile(leaderboardFile);
        if (!leaderboardFile.exists())
            SurvivalSkills.getInstance().saveResource("leaderboard.yml", true);
        FileConfiguration leaderboardData = YamlConfiguration.loadConfiguration(leaderboardFile);
        SurvivalSkills.getInstance().setLeaderboardData(leaderboardData);
        loadLeaderboard(leaderboardData, SurvivalSkills.getInstance().getLeaderboardTracker());

        File permaTrashFile = new File(SurvivalSkills.getInstance().getDataFolder(), "permatrash.yml");
        SurvivalSkills.getInstance().setPermaTrashFile(permaTrashFile);
        if (!permaTrashFile.exists())
            SurvivalSkills.getInstance().saveResource("permatrash.yml", true);
        FileConfiguration permaTrashData = YamlConfiguration.loadConfiguration(permaTrashFile);
        SurvivalSkills.getInstance().setPermaTrashData(permaTrashData);

        File toolBeltFile = new File(SurvivalSkills.getInstance().getDataFolder(), "toolbelt.yml");
        SurvivalSkills.getInstance().setToolBeltFile(toolBeltFile);
        if (!toolBeltFile.exists())
            SurvivalSkills.getInstance().saveResource("toolbelt.yml", true);
        FileConfiguration toolBeltData = YamlConfiguration.loadConfiguration(toolBeltFile);
        SurvivalSkills.getInstance().setToolBeltData(toolBeltData);
    }

    public static void checkPluginDependencies(World world) {
        Plugin griefPrevention = Bukkit.getServer().getPluginManager().getPlugin("GriefPrevention");
        if (griefPrevention != null && griefPrevention.isEnabled())
            SurvivalSkills.getInstance().setGriefPreventionEnabled(true);

        checkWorldGuard(world);

        Plugin citizens = Bukkit.getServer().getPluginManager().getPlugin("Citizens");
        if (citizens != null && citizens.isEnabled())
            SurvivalSkills.getInstance().setCitizensEnabled(true);
    }

    public static void checkWorldGuard(World world) {
        Plugin worldGuard = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
        if (worldGuard == null || !worldGuard.isEnabled()) return;

        SurvivalSkills.getInstance().setWorldGuardEnabled(true);
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        SurvivalSkills.getInstance().setContainer(container);
        if (world == null) {
            Bukkit.getLogger().warning("Could not find world for worldguard");
        } else {
            RegionManager regions = container.get(BukkitAdapter.adapt(world));
            if (regions == null) {
                Bukkit.getLogger().warning("Could not find region manager for worldguard");
            } else {
                ProtectedRegion spawnRegion = regions.getRegion("spawn");
                if (spawnRegion == null) {
                    Bukkit.getLogger().warning("Could not find spawn region in worldguard. Please create a region called 'spawn'.");
                }
                SurvivalSkills.getInstance().setRegion(spawnRegion);
            }
        }
    }

    public static void updateConfig(FileConfiguration config) {
        // Load the default configuration from the JAR
        InputStream defConfigStream = SurvivalSkills.getInstance().getClass().getClassLoader()
                .getResourceAsStream(CONFIG_YML);
        if (defConfigStream == null) {
            Bukkit.getLogger().log(Level.SEVERE, "Default config file not found in JAR");
            return;
        }
        YamlConfiguration defConfig = YamlConfiguration
                .loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8));

        // Merge new defaults into the existing config (existing config is modified)
        mergeConfigWithOrder(config, defConfig, "");

        // Save the updated existing configuration
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
        // Iterate through all keys in the new default config
        for (String key : defaultConfig.getKeys(false)) {
            String fullKey = parentKey.isEmpty() ? key : parentKey + "." + key;

            // Case 1: Both configs have this key as a configuration section - recurse into
            // it
            if (defaultConfig.isConfigurationSection(key) && existingConfig.isConfigurationSection(key)) {
                handleBothSectionsHaveKey(existingConfig, defaultConfig, key, fullKey);
            }
            // Case 2: Default has a section but existing doesn't - create the section in
            // existing config
            else if (defaultConfig.isConfigurationSection(key) && !existingConfig.contains(key)) {
                handleKeyInDefaultButNotExisting(existingConfig, defaultConfig, key, fullKey);
            }
            // Case 3: Existing config already has this key (not a section) - keep user's
            // value so do nothing
            // Case 4: Key only exists in default config - add it to existing config
            else if (!existingConfig.contains(key)) {
                Object defaultValue = defaultConfig.get(key);
                if (defaultValue != null) {
                    existingConfig.set(key, defaultValue);
                    Bukkit.getLogger().log(Level.INFO,
                            String.format("[SurvivalSkills] Added new config key: %s = %s", fullKey, defaultValue),
                            CONFIG_UPDATE);
                }
            }
        }
    }

    private static void handleKeyInDefaultButNotExisting(ConfigurationSection existingConfig,
            ConfigurationSection defaultConfig, String key, String fullKey) {
        ConfigurationSection defaultSection = defaultConfig.getConfigurationSection(key);
        if (defaultSection != null) {
            ConfigurationSection newSection = existingConfig.createSection(key);
            // Recursively copy all values from the default section
            mergeConfigWithOrder(newSection, defaultSection, fullKey);
            Bukkit.getLogger().log(Level.INFO, String.format("[SurvivalSkills] Added new config section: %s", fullKey),
                    CONFIG_UPDATE);
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
                    String.format("[SurvivalSkills] Failed to merge section: %s", fullKey), CONFIG_UPDATE);
        }
    }

    public static void loadLeaderboard(FileConfiguration leaderboardData,
            Map<UUID, LeaderboardPlayer> leaderboardTracker) {
        ConfigurationSection section = leaderboardData.getConfigurationSection("");
        if (section == null)
            return;
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
            int trialScore = leaderboardData.getInt(key + ".Trials");
            int coopTrialScore = leaderboardData.getInt(key + ".CoopTrials");
            LeaderboardPlayer leaderboard = new LeaderboardPlayer(name, level, building, mining, fishing, exploring,
                    farming, fighting, crafting, main, deaths, trialScore, coopTrialScore);
            leaderboardTracker.put(UUID.fromString(key), leaderboard);
        });
    }

    public static void loadCommands() {
        SurvivalSkills plugin = SurvivalSkills.getInstance();
        // Default Player Commands
        plugin.setFlightCommand(new FlightCommand(plugin));
        new AutoEatCommand(plugin);
        new AutoTrashCommand(plugin);
        new DeathLocationCommand(plugin);
        new DeathReturnCommand(plugin);
        new EatCommand(plugin);
        new MobScannerCommand(plugin);
        new NightVisionCommand(plugin);
        new PeacefulMinerCommand(plugin);
        new PermaTrashCommand(plugin);
        new SpelunkerCommand(plugin);
        new ToggleMaxSkillMessageCommand(plugin);
        new TogglePhantomsCommand(plugin);
        new ToggleScoreboardCommand(plugin);
        new ToggleSpeedCommand(plugin);
        new ToggleTrailCommand(plugin);
        new ToolBeltCommand(plugin);
        new VeinminerCommand(plugin);
        new WaterBreathingCommand(plugin);
        new ToggleBloodyDomainCommand();
        new ToggleTrashCommand(plugin);
        new GodQuestCommand(plugin);
        new ToggleBossMusic();
        new UpCommand(plugin);
        new GodTrialCommand(plugin);
        new CreativeCommand(plugin);

        // Admin Commands
        new BossCommand(plugin);
        new BossMusicCommand(plugin);
        new CaveFinderCommand(plugin);
        new GetTrophyCommand(plugin);
        new ResetFirstDragon(plugin);
        new SkillsMultiplierCommand(plugin);
        new SurvivalSkillsCommand(plugin);
        new SurvivalSkillsGetCommand(plugin);
        new ToggleOverworldFirstDragon(plugin);
        new DragonStatusCommand();
        new ToggleGodQuestCommand(plugin);
        new ResetAllCommand(plugin);
        new StoreTrialBuildingCommand(plugin);
        new CancelAbilityCooldownsCommand(plugin);
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
        boolean big = false;
        if (permaTrashData.contains(uuid + BIG_TRASH))
            big = permaTrashData.getBoolean(uuid + BIG_TRASH);
        AutoTrash trash = new AutoTrash(big, true);
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
                    if (trash.getTrashMaterials().contains(material))
                        return;
                    trash.addTrashItem(new ItemStack(material));
                } else
                    Bukkit.getLogger().warning("Material " + key + " for " + uuid + " is not valid");
            });
        }
        return trash;
    }

    private static boolean loadTrashEnchants(UUID uuid, AutoTrash trash, FileConfiguration permaTrashData) {
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
                    Bukkit.getLogger().log(Level.WARNING,
                            String.format("Enchantment %s for %s is not valid", key, uuid), CONFIG_UPDATE);
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
        }
        return false;
    }

    public static Enchantment getEnchantFromKey(String key) {
        for (Enchantment enchant : Registry.ENCHANTMENT) {
            if (enchant.getKey().toString().equalsIgnoreCase(key))
                return enchant;
        }
        return Enchantment.EFFICIENCY;
    }

    public static void loadData(Player p, FileConfiguration data) {
        SurvivalSkills plugin = SurvivalSkills.getInstance();
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

            plugin.getTrophyManager().getTrophyTracker().put(p.getUniqueId(), trophyList);
            SkillsHolder holder = new SkillsHolder(skills, plugin.getSkillManager().getNewPlayerRewards());
            holder.setMaxSkillMessageEnabled(true);
            if (!plugin.getSkillManager().getPlayerSkills().containsKey(p.getUniqueId()))
                plugin.getSkillManager().getPlayerSkills().put(p.getUniqueId(), holder);
            else
                Bukkit.getLogger().warning(PLAYER + p.getName() + " already has skills loaded");
            plugin.getToggledScoreboard().put(p.getUniqueId(), true);

            savePlayerData(p);
            return;
        }

        UUID uuid = p.getUniqueId();
        if (!plugin.getSkillManager().getPlayerSkills().containsKey(uuid))
            plugin.getSkillManager().loadPlayerSkills(uuid, data);
        if (!plugin.getToggledScoreboard().containsKey(uuid))
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
            int veinminer = data.getInt(uuid + VEINMINER);
            if (veinminer == 0 || veinminer == 1)
                SurvivalSkills.getInstance().getMiningListener().getVeinminerTracker().put(p, veinminer);
        }
    }

    private static void loadAutoEat(Player p, FileConfiguration data, UUID uuid) {
        if (data.contains(uuid + AUTO_EAT)) {
            boolean autoEat = data.getBoolean(uuid + AUTO_EAT);
            if (autoEat && !SurvivalSkills.getInstance().getFarmingListener().getAutoEat().contains(p))
                SurvivalSkills.getInstance().getFarmingListener().getAutoEat().add(p);
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
        if (SurvivalSkills.getInstance().getToggledScoreboard().containsKey(uuid))
            return;
        if (!data.contains(uuid + SCOREBOARD)) {
            // default to having the scoreboard enabled when no setting exists
            SurvivalSkills.getInstance().getToggledScoreboard().put(uuid, true);
        } else {
            SurvivalSkills.getInstance().getToggledScoreboard().put(uuid, data.getBoolean(uuid + SCOREBOARD));
        }
    }

    /**
     * Loads all teleport anchors from the teleportanchors.yml file.
     * This method reconstructs anchor objects and validates their locations.
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

                // Validate that the location still has a respawn anchor
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
        if (SkillManager.getSkill(uuid, "Main").getLevel() == 100)
            plugin.getTrophyManager().savePlayerGodQuestData(uuid, godQuestData);

        if (plugin.getToggledScoreboard().containsKey(uuid))
            data.set(uuid + SCOREBOARD, plugin.getToggledScoreboard().get(uuid));
        else
            Bukkit.getLogger().warning(PLAYER + p.getName() + " does not have a scoreboard status");

        if (plugin.getAbilityManager().getTrailTracker().containsKey(p))
            data.set(uuid + TRAIL, plugin.getAbilityManager().getTrailTracker().get(p).getTrailName());
        else
            data.set(uuid + TRAIL, "None");

        data.set(uuid + NO_PHANTOMS, plugin.getFightingListener().getNoPhantomSpawns().contains(p));
        data.set(uuid + AUTO_EAT, plugin.getFarmingListener().getAutoEat().contains(p));
        data.set(uuid + VEINMINER, plugin.getMiningListener().getVeinminerTracker().getOrDefault(p, -1));
        data.set(uuid + PEACEFUL_MINER, plugin.getMiningListener().getPeacefulMiners().contains(p));
        data.set(uuid + BLOODY_DOMAIN, plugin.getAbilityManager().getBloodyDomainTracker().containsKey(p));
        data.set(uuid + NO_BOSS_MUSIC, plugin.getFightingListener().getNoBossMusic().contains(p));

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

    public static void savePlayerData() throws IOException {
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
            if (plugin.getToggledScoreboard().containsKey(uuid))
                data.set(uuid + SCOREBOARD, plugin.getToggledScoreboard().get(uuid));
            else
                Bukkit.getLogger().warning(PLAYER + p.getName() + " does not have a scoreboard status");

            if (plugin.getAbilityManager().getTrailTracker().containsKey(p))
                data.set(uuid + TRAIL, plugin.getAbilityManager().getTrailTracker().get(p).getTrailName());
            else
                data.set(uuid + TRAIL, "None");

            data.set(uuid + NO_PHANTOMS, plugin.getFightingListener().getNoPhantomSpawns().contains(p));
            data.set(uuid + AUTO_EAT, plugin.getFarmingListener().getAutoEat().contains(p));
            data.set(uuid + VEINMINER, plugin.getMiningListener().getVeinminerTracker().getOrDefault(p, -1));
            data.set(uuid + PEACEFUL_MINER, plugin.getMiningListener().getPeacefulMiners().contains(p));
            data.set(uuid + BLOODY_DOMAIN, plugin.getAbilityManager().getBloodyDomainTracker().containsKey(p));
            data.set(uuid + NO_BOSS_MUSIC, plugin.getFightingListener().getNoBossMusic().contains(p));

            plugin.getAbilityManager().saveFlightTimer(p, data);
            plugin.getAbilityManager().saveSpelunkerTimer(p, data);

            plugin.getSkillManager().savePlayerMultiplier(p, data);
        }

        data.save(dataFile);
        godQuestData.save(godQuestFile);
    }

    public static void saveLeaderboard(Map<UUID, LeaderboardPlayer> leaderboardTracker,
            FileConfiguration leaderboardData, File leaderboardFile) throws IOException {
        if (leaderboardTracker.isEmpty())
            return;
        if (leaderboardData == null)
            return;

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
            leaderboardData.set(player.getKey() + ".SoloTrials", player.getValue().getTrialScore());
            leaderboardData.set(player.getKey() + ".CoopTrials", player.getValue().getCoopTrialScore());
        }

        try {
            leaderboardData.save(leaderboardFile);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save leaderboard file", e);
        }
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

        try {
            permaTrashData.save(permaTrashFile);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save perma trash file", e);
        }
    }

    public static void savePotionBags() {
        SurvivalSkills plugin = SurvivalSkills.getInstance();
        File potionBagFile = new File(plugin.getDataFolder(), "potionbags.yml");
        if (!potionBagFile.exists())
            plugin.saveResource("potionbags.yml", true);
        FileConfiguration potionBagData = YamlConfiguration.loadConfiguration(potionBagFile);
        plugin.getGodListener().savePotionBags(potionBagData);

        try {
            potionBagData.save(potionBagFile);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save potion bags file", e);
        }
    }

    public static void savePowerOreConversions() {
        SurvivalSkills plugin = SurvivalSkills.getInstance();
        File powerOreFile = new File(plugin.getDataFolder(), "poweroreconversions.yml");
        if (!powerOreFile.exists())
            plugin.saveResource("poweroreconversions.yml", true);
        FileConfiguration powerOreData = YamlConfiguration.loadConfiguration(powerOreFile);
        plugin.getGodListener().savePowerOreConversions(powerOreData);

        try {
            powerOreData.save(powerOreFile);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save power ore conversions file", e);
        }
    }

    /**
     * Saves all teleport anchors to the teleportanchors.yml file.
     * This method preserves all anchor data including name, location, and owner.
     */
    public static void saveTeleportAnchors(Map<Location, TeleporterAnchor> teleportAnchors) {
        File file = new File(SurvivalSkills.getInstance().getDataFolder(), "teleportanchors.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Clear existing data
        config.set("TeleportAnchors", null);

        if (teleportAnchors.isEmpty()) {
            try {
                config.save(file);
            } catch (IOException e) {
                Bukkit.getLogger().log(Level.SEVERE,
                        "[SurvivalSkills] Failed to save teleport anchors file", e);
            }
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

        try {
            config.save(file);
            Bukkit.getLogger().log(Level.INFO,
                    String.format("[SurvivalSkills] Saved %d teleport anchors", teleportAnchors.size()));
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE,
                    "[SurvivalSkills] Failed to save teleport anchors file", e);
        }
    }
}
