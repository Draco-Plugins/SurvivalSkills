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
import sir_draco.survivalskills.utils.ItemStackGenerator;
import sir_draco.survivalskills.rewards.Reward;
import sir_draco.survivalskills.rewards.RewardItemInfo;
import sir_draco.survivalskills.rewards.RewardNotifications;
import sir_draco.survivalskills.boards.Leaderboard;
import sir_draco.survivalskills.skills.Skill;
import sir_draco.survivalskills.skills.SkillCategory;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.utils.RecipeMaker;

import java.util.*;

public class SkillsCommand implements CommandExecutor {

    private final SurvivalSkills plugin;
    private final ArrayList<Inventory> recipeInventories = new ArrayList<>();
    private final ArrayList<RewardItemInfo> recipeLevelInformation = new ArrayList<>();

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
            for (Skill skill : plugin.getSkillManager().getPlayerSkills().get(p.getUniqueId()).getSkills())
                skill.printStats(p, true);
            return true;
        }

        if (strings[0].equalsIgnoreCase("tree")) {
            if (strings.length == 1) {
                p.sendRawMessage(ChatColor.RED + "Correct usage: " + ChatColor.GRAY  + "/skills tree <skill>");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }

            if (!SkillCategory.isSkillTreeSkill(strings[1])) {
                p.sendRawMessage(ChatColor.RED + "Skill does not have skill tree: " + ChatColor.YELLOW + strings[1]);
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }

            SkillCategory skillCategory = SkillCategory.fromString(strings[1]);
            if (skillCategory == SkillCategory.DEATHS) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Inventory inventory = Bukkit.createInventory(null, 9);
                        int deaths = Leaderboard.getLeaderboardScore(p, SkillCategory.DEATHS);
                        createDeathTree(inventory, deaths);
                        ArrayList<Inventory> inventories = new ArrayList<>();
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
            else {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        ArrayList<Inventory> inventories = createSkillTree(p, skillCategory);
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
            return true;
        }

        if (strings[0].equalsIgnoreCase("recipes")) {
            plugin.getPlayerListener().getCustomInventories().put(p, recipeInventories);
            p.openInventory(recipeInventories.getFirst());
        }

        if (strings[0].equalsIgnoreCase("commands")) {
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

        if (strings[0].equalsIgnoreCase("trophies")) {
            p.sendRawMessage(ChatColor.AQUA + "Trophy Information:");
            p.sendRawMessage(ChatColor.GRAY + "Trophies are crafted using items gathered throughout the game");
            p.sendRawMessage(ChatColor.GRAY + "Trophies increase the level cap of all skills by 10 (level cap starts at 10)");
            p.sendRawMessage(ChatColor.GRAY + "By crafting all trophies you can reach level 100 in all skills");
            p.sendRawMessage(ChatColor.GRAY + "Trophies have animations when placed but once crafted you no longer need the physical trophy");
            p.sendRawMessage(ChatColor.GRAY + "The easiest trophies to craft are the cave, farm, and forest trophies");
            p.sendRawMessage(ChatColor.RED + "Making the same trophy multiple times does not increase the cap");
            p.sendRawMessage(ChatColor.GOLD + "See trophy recipes by using " + ChatColor.AQUA + "/skills recipes");
            p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
            return true;
        }

        if (strings[0].equalsIgnoreCase("player")) {
            if (strings.length < 2) {
                p.sendRawMessage(ChatColor.RED + "Correct usage: /skills player <name>");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }

            Player play = null;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.getName().equals(strings[1])) continue;
                play = player;
            }
            if (play == null) {
                p.sendRawMessage(ChatColor.RED + "Player: " + ChatColor.YELLOW + strings[1] + ChatColor.RED + " not found");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }

            for (Skill skill : plugin.getSkillManager().getPlayerSkills().get(play.getUniqueId()).getSkills())
                skill.printStats(p, false);
            return true;
        }

        if (strings[0].equalsIgnoreCase("leaderboard")) {
            if (plugin.getLeaderboardTracker().isEmpty()) {
                p.sendRawMessage(ChatColor.RED + "Leaderboard is empty, level up a skill first!");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }

            // No parameters default to the top 10 players with the highest total skill score
            if (strings.length == 1) {
                ArrayList<String> leaderboard = Leaderboard.sortLeaderboard(SkillCategory.ALL, 10);
                for (String line : leaderboard) p.sendRawMessage(line);
                p.playSound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                return true;
            }

            // Check if the string is an actual skill
            if (!SkillCategory.isSkill(strings[1])) {
                p.sendRawMessage(ChatColor.RED + "Invalid skill: " + ChatColor.YELLOW + strings[1]);
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                return true;
            }

            // Calculate the total number of pages required to display all players
            double size = Math.ceil((double) plugin.getLeaderboardTracker().size() / 10);
            int maxPage = Math.max(1, (int) Math.ceil(size));

            // If they don't specify a page number print the first page
            if (strings.length != 3) {
                Leaderboard.printLeaderboard(p, SkillCategory.fromString(strings[1]), 1, maxPage);
                return true;
            }

            // Otherwise print the specified page
            try {
                int page = Integer.parseInt(strings[2]);
                if (page < 1 || page > maxPage) {
                    p.sendRawMessage(ChatColor.RED + "Invalid page number. Max page number is: " + ChatColor.AQUA + maxPage);
                    p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                    return true;
                }
                Leaderboard.printLeaderboard(p, SkillCategory.fromString(strings[1]), page, maxPage);
            } catch (NumberFormatException e) {
                p.sendRawMessage(ChatColor.RED + "Invalid page number.");
                p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            }
            return true;
        }
        return true;
    }

    private static ItemStack getItemStack(int playerLevel, int i) {
        ItemStack item;
        if (playerLevel >= i) {
            item = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return item;
            meta.setDisplayName("(" + ChatColor.GREEN + i + ChatColor.WHITE + ")");
            item.setItemMeta(meta);
        }
        else {
            item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) return item;
            meta.setDisplayName("(" + ChatColor.RED + i + ChatColor.WHITE + ")");
            item.setItemMeta(meta);
        }
        return item;
    }

    public void createRecipeInventories() {
        int recipeCounter = 1;
        int totalPages = plugin.getRecipeKeys().size() / 2 + 1;
        for (;recipeCounter <= plugin.getRecipeKeys().size(); recipeCounter++) {
            Inventory inv;
            if (recipeCounter % 2 == 1) {
                int pageNumber = (recipeCounter + 1) / 2;
                inv = Bukkit.createInventory(null, 36, "Skill Recipes " + pageNumber + "/" + totalPages);
                addSSRecipe(recipeCounter, inv);
                recipeInventories.add(inv);
                if (recipeCounter == plugin.getRecipeKeys().size()) addBarriers(inv);
            }
            else {
                inv = recipeInventories.getLast();
                addSSRecipe(recipeCounter, inv);
                addBarriers(inv);
            }
        }

        if (recipeCounter % 2 == 1) addBarriers(recipeInventories.getLast());
    }

    @SuppressWarnings("deprecation")
    public void addSSRecipe(int recipeCounter, Inventory inv) {
        NamespacedKey key = plugin.getRecipeKeys().get(recipeCounter - 1);
        if (key == null) return;
        List<Integer> slots = RecipeMaker.getRecipePositions(recipeCounter);
        Recipe recipe = Bukkit.getRecipe(key);
        switch (recipe) {
            case ShapedRecipe shapedRecipe -> {
                String[] shape = shapedRecipe.getShape();
                Map<Character, ItemStack> ingredients = shapedRecipe.getIngredientMap();
                Map<Character, RecipeChoice> recipeChoices = shapedRecipe.getChoiceMap();
                for (int i = 0; i < shape.length * 3; i++) {
                    int slot = i % 3;
                    String layer;
                    if (i <= 2) layer = shape[0];
                    else if (i <= 5) layer = shape[1];
                    else layer = shape[2];
                    if (slot >= layer.length()) continue;
                    char c = layer.charAt(slot);
                    if (c == ' ' || c == 'D') continue;

                    // Check if it is the fishing king item
                    ItemStack ingredient = ingredients.get(c);
                    if (ingredient != null && ingredient.getType().equals(Material.PRISMARINE_SHARD)
                            && ingredient.getItemMeta() != null && ingredient.getItemMeta().hasCustomModelData()) {
                        inv.setItem(slots.get(i), getResult(ingredient));
                        continue;
                    }

                    if (ingredients.containsKey(c)) inv.setItem(slots.get(i), ingredients.get(c));
                    else if (recipeChoices.containsKey(c)) {
                        RecipeChoice.ExactChoice choice = (RecipeChoice.ExactChoice) recipeChoices.get(c);
                        inv.setItem(slots.get(i), choice.getItemStack());
                    }
                }
                ItemStack result = getResult(shapedRecipe.getResult());
                inv.setItem(slots.get(9), result);
            }
            case ShapelessRecipe shapelessRecipe -> {
                List<ItemStack> ingredients = shapelessRecipe.getIngredientList();
                int slot = 0;
                if (!ingredients.isEmpty()) {
                    for (ItemStack ingredient : ingredients) {
                        inv.setItem(slots.get(slot), ingredient);
                        slot++;
                    }
                }

                ItemStack result = getResult(shapelessRecipe.getResult());
                inv.setItem(slots.get(9), result);
            }
            case null, default -> {}
        }
    }

    public ArrayList<Inventory> createSkillTree(Player p, SkillCategory skillCategory) {
        Map<SkillCategory, ArrayList<Reward>> allSkills = plugin.getSkillManager().getDefaultPlayerRewards().getRewardList();
        ArrayList<Reward> rewards = allSkills.get(skillCategory);
        Objects.requireNonNull(rewards, String.format("[Survival Skills] No reward list found for skill %s", skillCategory.getDisplayName()));

        ArrayList<Inventory> inventories = new ArrayList<Inventory>();
        int playerLevel = SkillManager.getSkill(p.getUniqueId(), skillCategory).getLevel();
        int startLevel = 1;
        for (int page = 0; page < 5 && startLevel <= Skill.MAX_LEVEL; page++) {
            startLevel = createSkillInventory(startLevel, playerLevel, rewards, inventories);
        }
        return inventories;
    }

    public int createSkillInventory(int start, int playerLevel, ArrayList<Reward> rewards, ArrayList<Inventory> inventories) {
        Inventory inv = Bukkit.createInventory(null, 54, "Skill Tree");
        int i = start;
        int slot = 0;
        for (;i <= start + 29; i++) {
            boolean foundReward = false;
            for (Reward reward : rewards) {
                if (reward.getLevel() != i) continue;
                if (!reward.isEnabled()) continue;
                ItemStack item;
                if (playerLevel >= i) {
                    item = new ItemStack(Material.EMERALD_BLOCK);
                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) continue;
                    meta.setDisplayName("(" + ChatColor.GREEN + i + ChatColor.WHITE + ") " + ChatColor.GREEN + addSpaces(reward.getName()));
                    meta.setLore(RewardNotifications.getLore(RewardNotifications.getRewardDescription(reward.getSkillCategory().getDisplayName(), reward.getName())));
                    item.setItemMeta(meta);
                }
                else {
                    item = new ItemStack(Material.REDSTONE_BLOCK);
                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) continue;
                    meta.setDisplayName("(" + ChatColor.RED + i + ChatColor.WHITE + ") " + ChatColor.RED + addSpaces(reward.getName()));
                    meta.setLore(RewardNotifications.getLore(RewardNotifications.getRewardDescription(reward.getSkillCategory().getDisplayName(), reward.getName())));
                    item.setItemMeta(meta);
                }
                inv.setItem(slot, item);
                foundReward = true;
                break;
            }
            
            if (!foundReward) {
                ItemStack item = getItemStack(playerLevel, i);
                inv.setItem(slot, item);
            }

            if (slot < 8) slot++; // right
            else if (slot == 8) slot = 17; // down
            else if (slot == 17) slot = 26; // down
            else if (slot > 18 && slot <= 26) slot--; // left
            else if (slot == 18) slot = 27; // down
            else if (slot == 27) slot = 36; // down
            else if (slot >= 36) slot++; // right

            if (i >= Skill.MAX_LEVEL) break;
        }

        addBarriers(inv);
        inventories.add(inv);
        return i - 1;
    }

    @SuppressWarnings("deprecation")
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
        meta.setCustomModelData(1);
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

    public String addSpaces(String input) {
        StringBuilder result = new StringBuilder();
        boolean firstRoman = true;
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (i == 0) {
                result.append(c);
                continue;
            }
            if (Character.isUpperCase(c) && i+1 < input.length() && Character.isLowerCase(input.charAt(i+1))) {
                result.append(" ");
                result.append(c);
                continue;
            }
            if (Character.isUpperCase(c) && c != 'I' && c != 'V' && c!='X') result.append(" ");
            if (c == 'I' || c == 'V' || c == 'X') {
                if (firstRoman) {
                    firstRoman = false;
                    result.append(" ");
                }
            }
            result.append(c);
        }
        return result.toString();
    }

    public void createDeathTree(Inventory inv, int deaths) {
        for (int i = 0; i < 6; i++) {
            if (i == 0) {
                ItemStack item;
                if (deaths >= 10) {
                    item = new ItemStack(Material.EMERALD_BLOCK);
                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) return;
                    meta.setDisplayName(ChatColor.WHITE + "(" + ChatColor.GREEN + "10 Deaths" + ChatColor.WHITE + ")"
                            + ChatColor.GREEN + " Temporary Speed Boost After Death");
                    item.setItemMeta(meta);
                }
                else {
                    item = new ItemStack(Material.REDSTONE_BLOCK);
                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) return;
                    meta.setDisplayName(ChatColor.WHITE + "(" + ChatColor.RED + "10 Deaths" + ChatColor.WHITE + ")"
                            + ChatColor.RED + " Temporary Speed Boost After Death");
                    item.setItemMeta(meta);
                }
                inv.setItem(i, item);
            }
            else if (i == 1) {
                ItemStack item;
                if (deaths >= 20) {
                    item = new ItemStack(Material.EMERALD_BLOCK);
                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) return;
                    meta.setDisplayName(ChatColor.WHITE + "(" + ChatColor.GREEN + "20 Deaths" + ChatColor.WHITE + ")"
                            + ChatColor.GREEN + " Temporary Strength Boost After Death");
                    item.setItemMeta(meta);
                }
                else {
                    item = new ItemStack(Material.REDSTONE_BLOCK);
                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) return;
                    meta.setDisplayName(ChatColor.WHITE + "(" + ChatColor.RED + "20 Deaths" + ChatColor.WHITE + ")"
                            + ChatColor.RED + " Temporary Strength Boost After Death");
                    item.setItemMeta(meta);
                }
                inv.setItem(i, item);
            }
            else if (i == 2) {
                ItemStack item;
                if (deaths >= 30) {
                    item = new ItemStack(Material.EMERALD_BLOCK);
                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) return;
                    meta.setDisplayName(ChatColor.WHITE + "(" + ChatColor.GREEN + "30 Deaths" + ChatColor.WHITE + ")"
                            + ChatColor.GREEN + " Temporary Regeneration Boost After Death");
                    item.setItemMeta(meta);
                }
                else {
                    item = new ItemStack(Material.REDSTONE_BLOCK);
                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) return;
                    meta.setDisplayName(ChatColor.WHITE + "(" + ChatColor.RED + "30 Deaths" + ChatColor.WHITE + ")"
                            + ChatColor.RED + " Temporary Regeneration Boost After Death");
                    item.setItemMeta(meta);
                }
                inv.setItem(i, item);
            }
            else if (i == 3) {
                ItemStack item;
                if (deaths >= 40) {
                    item = new ItemStack(Material.EMERALD_BLOCK);
                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) return;
                    meta.setDisplayName(ChatColor.WHITE + "(" + ChatColor.GREEN + "40 Deaths" + ChatColor.WHITE + ")"
                            + ChatColor.GREEN + " Fire Resistance");
                    item.setItemMeta(meta);
                }
                else {
                    item = new ItemStack(Material.REDSTONE_BLOCK);
                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) return;
                    meta.setDisplayName(ChatColor.WHITE + "(" + ChatColor.RED + "40 Deaths" + ChatColor.WHITE + ")"
                            + ChatColor.RED + " Fire Resistance");
                    item.setItemMeta(meta);
                }
                inv.setItem(i, item);
            }
            else if (i == 4) {
                ItemStack item;
                if (deaths >= 50) {
                    item = new ItemStack(Material.EMERALD_BLOCK);
                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) return;
                    meta.setDisplayName(ChatColor.WHITE + "(" + ChatColor.GREEN + "50 Deaths" + ChatColor.WHITE + ")"
                            + ChatColor.GREEN + " 10% Less Damage");
                    item.setItemMeta(meta);
                }
                else {
                    item = new ItemStack(Material.REDSTONE_BLOCK);
                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) return;
                    meta.setDisplayName(ChatColor.WHITE + "(" + ChatColor.RED + "50 Deaths" + ChatColor.WHITE + ")"
                            + ChatColor.RED + " 10% Less Damage");
                    item.setItemMeta(meta);
                }
                inv.setItem(i, item);
            }
            else {
                ItemStack item;
                if (deaths >= 75) {
                    item = new ItemStack(Material.EMERALD_BLOCK);
                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) return;
                    meta.setDisplayName(ChatColor.WHITE + "(" + ChatColor.GREEN + "75 Deaths" + ChatColor.WHITE + ")"
                            + ChatColor.GREEN + " Teleport To Death Location");
                    ArrayList<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "Use the command " + ChatColor.AQUA + "/deathreturn " + ChatColor.GRAY + "to teleport to your death location");
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                else {
                    item = new ItemStack(Material.REDSTONE_BLOCK);
                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) return;
                    meta.setDisplayName(ChatColor.WHITE + "(" + ChatColor.RED + "75 Deaths" + ChatColor.WHITE + ")"
                            + ChatColor.RED + " Teleport To Death Location");
                    ArrayList<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "Use the command " + ChatColor.AQUA + "/deathreturn " + ChatColor.GRAY + "to teleport to your death location");
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                inv.setItem(i, item);
            }
        }
    }

    public RewardItemInfo getRewardItemInfo(ItemStack item, SkillCategory skillCategory, String rewardName) {
        return new RewardItemInfo(item, skillCategory, plugin.getTrophyManager().getRewardLevel(skillCategory, rewardName));
    }

    public void createItemLevelInfo() {
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getUnlimitedTorch(), SkillCategory.MINING, "UnlimitedTorch"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getZapWand(), SkillCategory.MINING, "ZapWand"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getMiningHelmet(), SkillCategory.MINING, "MiningArmor"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getMiningChestplate(), SkillCategory.MINING, "MiningArmor"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getMiningLeggings(), SkillCategory.MINING, "MiningArmor"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getMiningBoots(), SkillCategory.MINING, "MiningArmor"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getBeaconHelmet(), SkillCategory.MINING, "BeaconArmor"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getBeaconChestplate(), SkillCategory.MINING, "BeaconArmor"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getBeaconLeggings(), SkillCategory.MINING, "BeaconArmor"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getBeaconBoots(), SkillCategory.MINING, "BeaconArmor"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getJumpingBoots(), SkillCategory.EXPLORING, "JumpingBoots"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getWandererHelmet(), SkillCategory.EXPLORING, "WandererArmor"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getWandererChestplate(), SkillCategory.EXPLORING, "WandererArmor"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getWandererLeggings(), SkillCategory.EXPLORING, "WandererArmor"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getWandererBoots(), SkillCategory.EXPLORING, "WandererArmor"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getTravelerHelmet(), SkillCategory.EXPLORING, "TravelerArmor"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getTravelerChestplate(), SkillCategory.EXPLORING, "TravelerArmor"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getTravelerLeggings(), SkillCategory.EXPLORING, "TravelerArmor"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getTravelerBoots(), SkillCategory.EXPLORING, "TravelerArmor"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getGillHelmet(), SkillCategory.EXPLORING, "GillArmor"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getGillChestplate(), SkillCategory.EXPLORING, "GillArmor"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getGillLeggings(), SkillCategory.EXPLORING, "GillArmor"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getGillBoots(), SkillCategory.EXPLORING, "GillArmor"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getAdventurerHelmet(), SkillCategory.EXPLORING, "AdventurerArmor"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getAdventurerChestplate(), SkillCategory.EXPLORING, "AdventurerArmor"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getAdventurerLeggings(), SkillCategory.EXPLORING, "AdventurerArmor"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getAdventurerBoots(), SkillCategory.EXPLORING, "AdventurerArmor"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getCaveFinder(), SkillCategory.EXPLORING, "CaveFinder"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getWateringCan(), SkillCategory.FARMING, "WateringCan"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getUnlimitedBoneMeal(), SkillCategory.FARMING, "UnlimitedBonemeal"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getHarvester(), SkillCategory.FARMING, "Harvester"));
        recipeLevelInformation.add(getRewardItemInfo(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE), SkillCategory.CRAFTING, "EnchantedGapple"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getFireworkCannon(), SkillCategory.MAIN, "FireworkCannon"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getSortWand(), SkillCategory.BUILDING, "AutoSortWand"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getGiantSummoner(), SkillCategory.FIGHTING, "GiantSummon"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getFishingBossItem(), SkillCategory.FIGHTING, "FishingKing"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getBroodMotherSummoner(), SkillCategory.FIGHTING, "BroodMotherSummon"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getVillagerSummoner(), SkillCategory.FIGHTING, "TheExiledOneSummon"));
        recipeLevelInformation.add(getRewardItemInfo(ItemStackGenerator.getMagnet(), SkillCategory.EXPLORING, "Magnet"));
    }

    public ItemStack getResult(ItemStack item) {
        for (RewardItemInfo info : recipeLevelInformation) {
            if (!info.isItem(item)) continue;
            ItemStack result = new ItemStack(info.item());
            ItemMeta meta = result.getItemMeta();
            if (meta == null) return item;
            List<String> lore = meta.getLore();
            String infoString = ChatColor.GRAY + "Unlocked when " + ChatColor.AQUA + info.skillCategory() + ChatColor.GRAY
                    + " reaches level " + ChatColor.AQUA + info.level();
            if (lore == null) {
                lore = new ArrayList<>();
                lore.add(infoString);
                meta.setLore(lore);
                result.setItemMeta(meta);
                return result;
            }
            lore.add("");
            lore.add(infoString);
            meta.setLore(lore);
            result.setItemMeta(meta);
            return result;
        }

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
