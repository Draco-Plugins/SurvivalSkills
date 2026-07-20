package sir_draco.survivalskills.commands.default_commands;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.skills.SkillManager;
import sir_draco.survivalskills.rewards.Reward;
import sir_draco.survivalskills.rewards.RewardItemInfo;
import sir_draco.survivalskills.rewards.RewardNotifications;
import sir_draco.survivalskills.pipes.PipeRewardGate;
import sir_draco.survivalskills.boards.Leaderboard;
import sir_draco.survivalskills.skills.Skill;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.skill_listeners.SilkyShearsListener;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.super_enchanting.SuperEnchantingItems;
import sir_draco.survivalskills.super_enchanting.SuperEnchantingTableManager;
import sir_draco.survivalskills.utils.RecipeSlotLayout;
import sir_draco.survivalskills.utils.SkillDisplay;
import sir_draco.survivalskills.utils.items.ItemStackGenerator;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;

import java.util.*;
import java.util.Locale;

public class SkillsCommand implements CommandExecutor {

    private record DeathTier(int threshold, String displaySuffix, List<String> lore) {}

    private static final List<DeathTier> DEATH_TIERS = List.of(
            new DeathTier(10, " Temporary Speed Boost After Death", List.of()),
            new DeathTier(20, " Temporary Strength Boost After Death", List.of()),
            new DeathTier(30, " Temporary Regeneration Boost After Death", List.of()),
            new DeathTier(40, " Fire Resistance", List.of()),
            new DeathTier(50, " 10% Less Damage", List.of()),
            new DeathTier(75, " Teleport To Death Location",
                    List.of(ChatColor.GRAY + "Use the command " + ChatColor.AQUA + "/deathreturn "
                            + ChatColor.GRAY + "to teleport to your death location"))
    );

    private static final int[] SKILL_TREE_SLOTS = {
             0,  1,  2,  3,  4,  5,  6,  7,  8,
            17, 26, 25, 24, 23, 22, 21, 20, 19,
            18, 27, 36, 37, 38, 39, 40, 41, 42,
            43, 44, 45
    };

    private final SurvivalSkills plugin;
    private final List<Inventory> recipeInventories = new ArrayList<>();
    private final Map<ItemStack, RewardItemInfo> recipeLevelInformation = new HashMap<>();

    public SkillsCommand(SurvivalSkills plugin) {
        this.plugin = plugin;
        PluginCommand command = plugin.getCommand("skills");
        if (command == null) return;
        command.setExecutor(this);
        createItemLevelInfo();
        createRecipeInventories();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] strings) {
        if (!(sender instanceof Player p)) return false;

        if (strings.length == 0) {
            handleStats(p);
            return true;
        }

        switch (strings[0].toLowerCase(Locale.ROOT)) {
            case "tree" -> handleTree(p, strings);
            case "recipes" -> handleRecipes(p);
            case "commands" -> handleCommands(p);
            case "trophies" -> handleTrophies(p);
            case "player" -> handlePlayer(p, strings);
            case "leaderboard" -> handleLeaderboard(p, strings);
            default -> {
                sendError(p, "Unknown subcommand: " + strings[0]);
            }
        }
        return true;
    }

    private static void sendError(Player p, String message) {
        p.sendRawMessage(ChatColor.RED + message);
        p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    }

    private void handleStats(Player p) {
        int maxSkillLevel = plugin.getTrophyManager().playerMaxSkillLevel(p.getUniqueId());
        for (Skill skill : plugin.getSkillManager().getPlayerSkills().get(p.getUniqueId()).getSkills())
            SkillDisplay.printStats(p, skill, true, maxSkillLevel);
    }

    private void handleTree(Player p, String[] strings) {
        if (strings.length == 1) {
            sendError(p, "Correct usage: " + ChatColor.GRAY + "/skills tree <skill>");
            return;
        }

        if (!SkillCategory.isSkillTreeSkill(strings[1])) {
            sendError(p, "Skill does not have skill tree: " + ChatColor.YELLOW + strings[1]);
            return;
        }

        SkillCategory skillCategory = SkillCategory.fromString(strings[1]);
        if (skillCategory == SkillCategory.DEATHS) {
            showDeathTree(p);
        } else {
            showSkillTree(p, skillCategory);
        }
    }

    private void showDeathTree(Player p) {
        new BukkitRunnable() {
            @Override
            public void run() {
                Inventory inventory = Bukkit.createInventory(null, 9);
                int deaths = Leaderboard.getLeaderboardScore(p, SkillCategory.DEATHS);
                createDeathTree(inventory, deaths);
                List<Inventory> inventories = new ArrayList<>();
                inventories.add(inventory);
                if (inventory.isEmpty()) return;
                plugin.getPlayerListener().getCustomInventories().put(p, inventories);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        p.openInventory(inventory);
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }

    private void showSkillTree(Player p, SkillCategory skillCategory) {
        new BukkitRunnable() {
            @Override
            public void run() {
                List<Inventory> inventories = createSkillTree(p, skillCategory);
                if (inventories == null || inventories.isEmpty()) return;
                plugin.getPlayerListener().getCustomInventories().put(p, inventories);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        p.openInventory(inventories.getFirst());
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }

    private void handleRecipes(Player p) {
        plugin.getPlayerListener().getCustomInventories().put(p, recipeInventories);
        p.openInventory(recipeInventories.getFirst());
    }

    private void handleCommands(Player p) {
        p.sendRawMessage(ChatColor.AQUA + "Commands:");
        p.sendRawMessage(ChatColor.GRAY + "/skills - View your skill stats");
        p.sendRawMessage(ChatColor.GRAY + "/skills tree <skill> - View the skill tree for a specific skill");
        p.sendRawMessage(ChatColor.GRAY + "/skills recipes - View all skill recipes");
        p.sendRawMessage(ChatColor.GRAY + "/skills trophies - Explains how the trophy system works");
        p.sendRawMessage(ChatColor.GRAY + "/skills player <name> - Gets the skill stats of another player");
        p.sendRawMessage(ChatColor.GRAY + "/spelunker - tells you where ores are, unlocked through mining skill");
        p.sendRawMessage(ChatColor.GRAY + "/veinminer - unlocked through mining skill");
        p.sendRawMessage(ChatColor.GRAY + "/toolbelt - an inventory to store tools, unlocked through mining skill");
        p.sendRawMessage(ChatColor.GRAY + "/ssnv - night vision, unlocked through mining skill");
        p.sendRawMessage(ChatColor.GRAY + "/peacefulminer - mobs won't spawn while mining, unlocked through mining skill");
        p.sendRawMessage(ChatColor.GRAY + "/autoeat - automatically eat food from your inventory, unlocked through farming skill");
        p.sendRawMessage(ChatColor.GRAY + "/sseat - feeds you without needing food, unlocked through farming skill");
        p.sendRawMessage(ChatColor.GRAY + "/flight - unlocked through building skill");
        p.sendRawMessage(ChatColor.GRAY + "/mobscanner - shows nearby mobs, unlocked through fighting skill");
        p.sendRawMessage(ChatColor.GRAY + "/togglephantoms - disables phantom spawns nearby, unlocked through fighting skill");
        p.sendRawMessage(ChatColor.GRAY + "/togglebloodydomain - kill all nearby mobs automatically, unlocked through fighting skill");
        p.sendRawMessage(ChatColor.GRAY + "/waterbreathing - unlocked through fishing skill");
        p.sendRawMessage(ChatColor.GRAY + "/autotrash - unlocked through fishing skill");
        p.sendRawMessage(ChatColor.GRAY + "/toggletrash - disables auto trash and perma trash, unlocked through fishing skill");
        p.sendRawMessage(ChatColor.GRAY + "/permatrash - unlocked through fishing skill");
        p.sendRawMessage(ChatColor.GRAY + "/deathlocation - shows your death location, unlocked through main skill");
        p.sendRawMessage(ChatColor.GRAY + "/togglescorboard - Enable or disable the scoreboard");
        p.sendRawMessage(ChatColor.GRAY + "/toggletrail <trail> - enables or disables a particle trail, unlocked through main skill");
        p.sendRawMessage(ChatColor.GRAY + "/togglemaxskillmessage - toggles the message that appears when you reach the max level of a skill");
        p.sendRawMessage(ChatColor.GRAY + "/togglespeed - Allows you to toggle exploring speed boosts");
        p.sendRawMessage(ChatColor.GRAY + "/deathreturn - returns you to your death location, unlocked through death skill");
        p.sendRawMessage(ChatColor.GRAY + "/godquest - Unlocked while in the god quest");
    }

    private void handleTrophies(Player p) {
        p.sendRawMessage(ChatColor.AQUA + "Trophy Information:");
        p.sendRawMessage(ChatColor.GRAY + "Trophies are crafted using items gathered throughout the game");
        p.sendRawMessage(ChatColor.GRAY + "Trophies increase the level cap of all skills by 10 (level cap starts at 10)");
        p.sendRawMessage(ChatColor.GRAY + "By crafting all trophies you can reach level 100 in all skills");
        p.sendRawMessage(ChatColor.GRAY + "Trophies have animations when placed but once crafted you no longer need the physical trophy");
        p.sendRawMessage(ChatColor.GRAY + "The easiest trophies to craft are the cave, farm, and forest trophies");
        p.sendRawMessage(ChatColor.RED + "Making the same trophy multiple times does not increase the cap");
        p.sendRawMessage(ChatColor.GOLD + "See trophy recipes by using " + ChatColor.AQUA + "/skills recipes");
        p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
    }

    private void handlePlayer(Player p, String[] strings) {
        if (strings.length < 2) {
            sendError(p, "Correct usage: /skills player <name>");
            return;
        }

        Player play = Bukkit.getPlayerExact(strings[1]);
        if (play == null) {
            sendError(p, "Player: " + ChatColor.YELLOW + strings[1] + ChatColor.RED + " not found");
            return;
        }

        for (Skill skill : plugin.getSkillManager().getPlayerSkills().get(play.getUniqueId()).getSkills())
            SkillDisplay.printStats(p, skill, false);
    }

    private void handleLeaderboard(Player p, String[] strings) {
        if (plugin.getLeaderboardTracker().isEmpty()) {
            sendError(p, "Leaderboard is empty, level up a skill first!");
            return;
        }

        // No parameters default to the top 10 players with the highest total skill score
        if (strings.length == 1) {
            List<String> leaderboard = Leaderboard.sortLeaderboard(SkillCategory.ALL, 10);
            for (String line : leaderboard) p.sendRawMessage(line);
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            return;
        }

        // Check if the string is an actual skill
        if (!SkillCategory.isSkill(strings[1])
                && !SkillCategory.DEATHS.name().equalsIgnoreCase(strings[1])) {
            sendError(p, "Invalid skill: " + ChatColor.YELLOW + strings[1]);
            return;
        }

        // Calculate the total number of pages required to display all players
        double size = Math.ceil((double) plugin.getLeaderboardTracker().size() / 10);
        int maxPage = Math.max(1, (int) Math.ceil(size));

        // If they don't specify a page number print the first page
        if (strings.length != 3) {
            Leaderboard.printLeaderboard(p, SkillCategory.fromString(strings[1]), 1, maxPage);
            return;
        }

        // Otherwise print the specified page
        try {
            int page = Integer.parseInt(strings[2]);
            if (page < 1 || page > maxPage) {
                sendError(p, "Invalid page number. Max page number is: " + ChatColor.AQUA + maxPage);
                return;
            }
            Leaderboard.printLeaderboard(p, SkillCategory.fromString(strings[1]), page, maxPage);
        } catch (NumberFormatException e) {
            sendError(p, "Invalid page number.");
        }
    }

    private static ItemStack getItemStack(int playerLevel, int i) {
        boolean unlocked = playerLevel >= i;
        ItemStack item = new ItemStack(unlocked ? Material.GREEN_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        ChatColor color = unlocked ? ChatColor.GREEN : ChatColor.RED;
        meta.setDisplayName("(" + color + i + ChatColor.WHITE + ")");
        item.setItemMeta(meta);
        return item;
    }

    public void createRecipeInventories() {
        int totalRecipes = plugin.getRecipeKeys().size();
        int totalPages = totalRecipes / 2 + 1;
        for (int recipeCounter = 1; recipeCounter <= totalRecipes; recipeCounter++) {
            boolean startsNewPage = recipeCounter % 2 == 1;
            Inventory inv;
            if (startsNewPage) {
                int pageNumber = (recipeCounter + 1) / 2;
                inv = Bukkit.createInventory(null, 36, "Skill Recipes " + pageNumber + "/" + totalPages);
                recipeInventories.add(inv);
            } else {
                inv = recipeInventories.getLast();
            }
            addSSRecipe(recipeCounter, inv);
            // Finalize a page once both recipe slots are filled, or when the last recipe lands on an odd slot
            if (!startsNewPage || recipeCounter == totalRecipes) addBarriers(inv);
        }
    }

    public void addSSRecipe(int recipeCounter, Inventory inv) {
        NamespacedKey key = plugin.getRecipeKeys().get(recipeCounter - 1);
        if (key == null) return;
        List<Integer> slots = RecipeSlotLayout.getRecipePositions(recipeCounter);
        Recipe recipe = Bukkit.getRecipe(key);
        if (recipe instanceof ShapedRecipe shaped) addShapedRecipe(shaped, slots, inv);
        else if (recipe instanceof ShapelessRecipe shapeless) addShapelessRecipe(shapeless, slots, inv);
    }

    private void addShapedRecipe(ShapedRecipe recipe, List<Integer> slots, Inventory inv) {
        String[] shape = recipe.getShape();
        Map<Character, ItemStack> ingredients = recipe.getIngredientMap();
        Map<Character, RecipeChoice> choices = recipe.getChoiceMap();
        for (int i = 0; i < shape.length * 3; i++) {
            String layer = shape[i / 3];
            int pos = i % 3;
            if (pos >= layer.length()) continue;
            char c = layer.charAt(pos);
            if (c == ' ' || c == 'D') continue;

            ItemStack ingredient = ingredients.get(c);
            if (isFishingKingItem(ingredient)) {
                inv.setItem(slots.get(i), getResult(ingredient));
            } else if (ingredients.containsKey(c)) {
                inv.setItem(slots.get(i), ingredient);
            } else if (choices.containsKey(c)) {
                ItemStack choiceItem = choiceToItem(choices.get(c));
                if (choiceItem != null) inv.setItem(slots.get(i), choiceItem);
            }
        }
        inv.setItem(slots.get(9), getResult(recipe.getResult()));
    }

    private void addShapelessRecipe(ShapelessRecipe recipe, List<Integer> slots, Inventory inv) {
        List<ItemStack> ingredients = recipe.getIngredientList();
        int slot = 0;
        for (ItemStack ingredient : ingredients) {
            inv.setItem(slots.get(slot), ingredient);
            slot++;
        }
        inv.setItem(slots.get(9), getResult(recipe.getResult()));
    }

    private static boolean isFishingKingItem(ItemStack ingredient) {
        return ingredient != null && ingredient.getType() == Material.PRISMARINE_SHARD
                && ingredient.getItemMeta() != null
                && ItemStackGeneratorUtils.hasCustomModelData(ingredient.getItemMeta());
    }

    private static ItemStack choiceToItem(RecipeChoice choice) {
        if (choice instanceof RecipeChoice.ExactChoice exact) return exact.getItemStack();
        if (choice instanceof RecipeChoice.MaterialChoice material && !material.getChoices().isEmpty()) {
            return new ItemStack(material.getChoices().get(0));
        }
        return null;
    }

    public List<Inventory> createSkillTree(Player p, SkillCategory skillCategory) {
        Map<SkillCategory, ArrayList<Reward>> allSkills = plugin.getSkillManager().getDefaultPlayerRewards().getRewardList();
        List<Reward> rewards = allSkills.get(skillCategory);
        Objects.requireNonNull(rewards, String.format("[Survival Skills] No reward list found for skill %s", skillCategory.getDisplayName()));

        List<Inventory> inventories = new ArrayList<>();
        int playerLevel = SkillManager.getSkill(p.getUniqueId(), skillCategory).getLevel();
        int startLevel = 1;
        for (int page = 0; page < 5 && startLevel <= Skill.MAX_LEVEL; page++) {
            startLevel = createSkillInventory(startLevel, playerLevel, rewards, inventories);
        }
        return inventories;
    }

    public int createSkillInventory(int start, int playerLevel, List<Reward> rewards, List<Inventory> inventories) {
        Inventory inv = Bukkit.createInventory(null, 54, "Skill Tree");
        int level = start;
        for (int slotIdx = 0; slotIdx < SKILL_TREE_SLOTS.length && level <= start + 29; slotIdx++, level++) {
            int slot = SKILL_TREE_SLOTS[slotIdx];
            boolean foundReward = false;
            for (Reward reward : rewards) {
                if (reward.getLevel() != level || !reward.isEnabled()) continue;
                ItemStack item = buildRewardItem(reward, level, playerLevel >= level);
                if (item == null) continue;
                inv.setItem(slot, item);
                foundReward = true;
                break;
            }

            if (!foundReward) inv.setItem(slot, getItemStack(playerLevel, level));
            if (level >= Skill.MAX_LEVEL) break;
        }

        addBarriers(inv);
        inventories.add(inv);
        return level - 1;
    }

    private ItemStack buildRewardItem(Reward reward, int level, boolean unlocked) {
        ItemStack item = new ItemStack(unlocked ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        ChatColor color = unlocked ? ChatColor.GREEN : ChatColor.RED;
        meta.setDisplayName("(" + color + level + ChatColor.WHITE + ") " + color + addSpaces(reward.getName()));
        meta.setLore(RewardNotifications.getLore(RewardNotifications.getRewardDescription(
                reward.getSkillCategory().getDisplayName(), reward.getName())));
        item.setItemMeta(meta);
        return item;
    }

    private void addBarriers(Inventory inv) {
        ItemStack item = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);

        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta meta = back.getItemMeta();
        if (meta == null) return;
        meta.setDisplayName(ChatColor.RED + "Back");
        back.setItemMeta(meta);

        ItemStack front = new ItemStack(Material.ARROW);
        meta = front.getItemMeta();
        if (meta == null) return;
        meta.setDisplayName(ChatColor.BLUE + "Next");
        ItemStackGeneratorUtils.setCustomModelData(meta, 1);
        front.setItemMeta(meta);

        if (inv.getSize() >= 53) {
            inv.setItem(45, item);
            inv.setItem(46, item);
            inv.setItem(47, item);
            inv.setItem(48, back);
            inv.setItem(49, item);
            inv.setItem(50, front);
            inv.setItem(51, item);
            inv.setItem(52, item);
            inv.setItem(53, item);
        }
        else {
            inv.setItem(27, item);
            inv.setItem(28, item);
            inv.setItem(29, item);
            inv.setItem(30, back);
            inv.setItem(31, item);
            inv.setItem(32, front);
            inv.setItem(33, item);
            inv.setItem(34, item);
            inv.setItem(35, item);
        }
    }

    private static final Set<Character> ROMAN_NUMERAL_CHARS = Set.of('I', 'V', 'X');

    public String addSpaces(String input) {
        StringBuilder result = new StringBuilder();
        boolean firstRoman = true;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (i > 0) result.append(spaceBefore(c, i, input, firstRoman));
            // The leading uppercase of a roman-numeral run (e.g. the first "I" in "SpeedII") gets one
            // separating space; subsequent roman chars are packed against it. The very first char is skipped.
            if (i > 0 && ROMAN_NUMERAL_CHARS.contains(c) && firstRoman && !isFollowedByLower(input, i)) {
                firstRoman = false;
            }
            result.append(c);
        }
        return result.toString();
    }

    private static String spaceBefore(char c, int i, String input, boolean firstRoman) {
        if (Character.isUpperCase(c) && isFollowedByLower(input, i)) return " ";
        if (Character.isUpperCase(c) && !ROMAN_NUMERAL_CHARS.contains(c)) return " ";
        if (ROMAN_NUMERAL_CHARS.contains(c) && firstRoman) return " ";
        return "";
    }

    private static boolean isFollowedByLower(String input, int i) {
        return i + 1 < input.length() && Character.isLowerCase(input.charAt(i + 1));
    }

    public void createDeathTree(Inventory inv, int deaths) {
        for (int i = 0; i < DEATH_TIERS.size(); i++) {
            DeathTier tier = DEATH_TIERS.get(i);
            boolean unlocked = deaths >= tier.threshold;
            ChatColor color = unlocked ? ChatColor.GREEN : ChatColor.RED;

            ItemStack item = new ItemStack(unlocked ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return;

            meta.setDisplayName(ChatColor.WHITE + "(" + color + tier.threshold + " Deaths" + ChatColor.WHITE + ")"
                    + color + tier.displaySuffix);
            if (!tier.lore.isEmpty()) meta.setLore(new ArrayList<>(tier.lore));
            item.setItemMeta(meta);
            inv.setItem(i, item);
        }
    }

    public RewardItemInfo getRewardItemInfo(ItemStack item, SkillCategory skillCategory, String rewardName) {
        return new RewardItemInfo(item, skillCategory, plugin.getTrophyManager().getRewardLevel(skillCategory, rewardName));
    }

    private void addSingleReward(ItemStack item, SkillCategory category, String rewardName) {
        recipeLevelInformation.putIfAbsent(levelInfoKey(item), getRewardItemInfo(item, category, rewardName));
    }

    private void addArmorSet(SkillCategory category, String rewardName,
                              ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots) {
        recipeLevelInformation.putIfAbsent(levelInfoKey(helmet), getRewardItemInfo(helmet, category, rewardName));
        recipeLevelInformation.putIfAbsent(levelInfoKey(chestplate), getRewardItemInfo(chestplate, category, rewardName));
        recipeLevelInformation.putIfAbsent(levelInfoKey(leggings), getRewardItemInfo(leggings, category, rewardName));
        recipeLevelInformation.putIfAbsent(levelInfoKey(boots), getRewardItemInfo(boots, category, rewardName));
    }

    public void createItemLevelInfo() {
        addSingleReward(ItemStackGenerator.getUnlimitedTorch(), SkillCategory.MINING, "UnlimitedTorch");
        addSingleReward(ItemStackGenerator.getZapWand(), SkillCategory.MINING, "ZapWand");
        addArmorSet(SkillCategory.MINING, "MiningArmor",
                ItemStackGenerator.getMiningHelmet(), ItemStackGenerator.getMiningChestplate(),
                ItemStackGenerator.getMiningLeggings(), ItemStackGenerator.getMiningBoots());
        addArmorSet(SkillCategory.MINING, "BeaconArmor",
                ItemStackGenerator.getBeaconHelmet(), ItemStackGenerator.getBeaconChestplate(),
                ItemStackGenerator.getBeaconLeggings(), ItemStackGenerator.getBeaconBoots());
        addSingleReward(ItemStackGenerator.getJumpingBoots(), SkillCategory.EXPLORING, "JumpingBoots");
        addArmorSet(SkillCategory.EXPLORING, "WandererArmor",
                ItemStackGenerator.getWandererHelmet(), ItemStackGenerator.getWandererChestplate(),
                ItemStackGenerator.getWandererLeggings(), ItemStackGenerator.getWandererBoots());
        addArmorSet(SkillCategory.EXPLORING, "TravelerArmor",
                ItemStackGenerator.getTravelerHelmet(), ItemStackGenerator.getTravelerChestplate(),
                ItemStackGenerator.getTravelerLeggings(), ItemStackGenerator.getTravelerBoots());
        addArmorSet(SkillCategory.EXPLORING, "GillArmor",
                ItemStackGenerator.getGillHelmet(), ItemStackGenerator.getGillChestplate(),
                ItemStackGenerator.getGillLeggings(), ItemStackGenerator.getGillBoots());
        addArmorSet(SkillCategory.EXPLORING, "AdventurerArmor",
                ItemStackGenerator.getAdventurerHelmet(), ItemStackGenerator.getAdventurerChestplate(),
                ItemStackGenerator.getAdventurerLeggings(), ItemStackGenerator.getAdventurerBoots());
        addSingleReward(ItemStackGenerator.getCaveFinder(), SkillCategory.EXPLORING, "CaveFinder");
        addSingleReward(ItemStackGenerator.getWateringCan(), SkillCategory.FARMING, "WateringCan");
        addSingleReward(ItemStackGenerator.getUnlimitedBoneMeal(), SkillCategory.FARMING, "UnlimitedBonemeal");
        addSingleReward(ItemStackGenerator.getHarvester(), SkillCategory.FARMING, "Harvester");
        addSingleReward(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE), SkillCategory.CRAFTING, "EnchantedGapple");
        addSingleReward(ItemStackGenerator.getWrench(), SkillCategory.CRAFTING, PipeRewardGate.REWARD_NAME);
        addSingleReward(ItemStackGenerator.getTransferPipe(), SkillCategory.CRAFTING, PipeRewardGate.REWARD_NAME);
        addSingleReward(SuperEnchantingItems.createSuperEnchantingTable(plugin), SkillCategory.CRAFTING,
                SuperEnchantingTableManager.REWARD_NAME);
        addSingleReward(ItemStackGenerator.getSilkyShears(), SkillCategory.CRAFTING,
                SilkyShearsListener.REWARD_NAME);
        addSingleReward(ItemStackGenerator.getFireworkCannon(), SkillCategory.MAIN, "FireworkCannon");
        addSingleReward(ItemStackGenerator.getSortWand(), SkillCategory.BUILDING, "AutoSortWand");
        addSingleReward(ItemStackGenerator.getBuilderWand(), SkillCategory.BUILDING, "BuildersWand");
        addSingleReward(ItemStackGenerator.getGiantSummoner(), SkillCategory.FIGHTING, "GiantSummon");
        addSingleReward(ItemStackGenerator.getFishingBossItem(), SkillCategory.FIGHTING, "FishingKing");
        addSingleReward(ItemStackGenerator.getBroodMotherSummoner(), SkillCategory.FIGHTING, "BroodMotherSummon");
        addSingleReward(ItemStackGenerator.getVillagerSummoner(), SkillCategory.FIGHTING, "TheExiledOneSummon");
        addSingleReward(ItemStackGenerator.getMagnet(), SkillCategory.EXPLORING, "Magnet");
    }

    public ItemStack getResult(ItemStack item) {
        return enrichWithLevelInfo(item).orElseGet(() -> addNoRequirementsFallback(item));
    }

    private Optional<ItemStack> enrichWithLevelInfo(ItemStack item) {
        RewardItemInfo info = recipeLevelInformation.get(levelInfoKey(item));
        if (info == null) return Optional.empty();
        return Optional.of(applyLevelLore(item, info));
    }

    private ItemStack applyLevelLore(ItemStack input, RewardItemInfo info) {
        ItemStack result = new ItemStack(info.item());
        ItemMeta meta = result.getItemMeta();
        if (meta == null) return input;
        String infoString = ChatColor.GRAY + "Unlocked when " + ChatColor.AQUA + info.skillCategory() + ChatColor.GRAY
                + " reaches level " + ChatColor.AQUA + info.level();
        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();
        else lore.add("");
        lore.add(infoString);
        meta.setLore(lore);
        result.setItemMeta(meta);
        return result;
    }

    // isSimilar ignores stack size, so normalize the amount to use an ItemStack as a stable hash key.
    private static ItemStack levelInfoKey(ItemStack item) {
        ItemStack key = item.clone();
        key.setAmount(1);
        return key;
    }

    private ItemStack addNoRequirementsFallback(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        List<String> lore = meta.getLore();
        if (lore == null) return item;
        lore.add("");
        lore.add(ChatColor.GRAY + "No requirements to unlock recipe");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
