package sir_draco.survivalskills.god_questline;

import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.trophy.Trophy;
import sir_draco.survivalskills.trophy.TrophyEffects;
import sir_draco.survivalskills.trophy.TrophyManager;
import sir_draco.survivalskills.utils.ItemStackGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class GodTrophyQuest {

    private final UUID uuid;
    private final int maxPhase = 59;

    private int currentItemCount = 0;

    private int phase = 0;

    public GodTrophyQuest(UUID uuid) {
        this.uuid = uuid;
        loadProgress();
    }

    public void loadProgress() {
        File file = new File(SurvivalSkills.getInstance().getDataFolder(), "godquests.yml");
        if (!file.exists()) SurvivalSkills.getInstance().saveResource("godquests.yml", true);
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);

        if (!data.contains(uuid.toString())) return;
        currentItemCount = data.getInt(uuid + ".ItemCount");
        phase = data.getInt(uuid + ".Phase");
    }

    /**
     * Entry point for the NPC interaction. Routes to the appropriate phase handler.
     * Phases overview (1-indexed tasks, 0 is the intro):
     *  - 0: Intro dialogue
     *  - 1..12: Farming collection (bulk materials)
     *  - 13..21: Ore/wealth collection (bulk materials)
     *  - 22..26: Creature trophies (crafted/held-in-hand items)
     *  - 27..44: Knowledge/potions sequence
     *  - 45..46: Relics (crafted items)
     *  - 47: Villager trading progress check
     *  - 48: Combat gear check
     *  - 49..57: Mob rare item sequence
     */
    public void handleNPCInteract(Player p) {
        if (phase >= maxPhase) return;

        if (phase == 0) {
            dialogueOpener(p);
            return;
        }

        // Farming 1..12
        if (phase >= 1 && phase <= 12) {
            checkFarmingQuest(p, phase);
            return;
        }

        // Ores 13..21 (local index 1..9)
        if (phase >= 13 && phase <= 21) {
            checkOreQuest(p, phase - 12);
            return;
        }

        // Creatures 22..26 (local index 1..5)
        if (phase >= 22 && phase <= 26) {
            checkCreatureQuest(p, phase - 21);
            return;
        }

        // Knowledge 27..44 (local index 1..18)
        if (phase >= 27 && phase <= 44) {
            checkKnowledgeQuest(p, phase - 26);
            return;
        }

        // Relics 45..46 (local index 1..2)
        if (phase >= 45 && phase <= 46) {
            checkRelicQuest(p, phase - 44);
            return;
        }

        // Villager trading 47
        if (phase == 47) {
            checkVillagerTradingQuest(p);
            return;
        }

        // Combat gear 48
        if (phase == 48) {
            checkCombatQuest(p);
            return;
        }

        // Mob items 49..57 (local index 1..9)
        if (phase >= 49 && phase <= 57) {
            checkMobItemQuest(p, phase - 48);
            return;
        }

        if (phase == 58) {
            checkAdvancementsQuest(p);
        }
    }

    /**
     * Returns true if the requirement has not been met for the specified material
     */
    public boolean handleItemCheck(int max, Player p, Material mat, String item) {
        currentItemCount += removeMaterialsFromInventory(currentItemCount, max, p.getInventory(), mat);
        if (currentItemCount < max) {
            dialogueItemCount(p, item, currentItemCount, max);
            return true;
        }
        p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        p.playSound(p, Sound.ENTITY_VILLAGER_CELEBRATE, 1, 1);
        return false;
    }

    public boolean handleItemCheck(Player p, ItemStack item, String itemName) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            ArrayList<String> messages = new ArrayList<>();
            messages.add("You are not holding anything");
            messages.add("Use " + ChatColor.YELLOW + "/godquest" + ChatColor.WHITE + " to see the recipe for " +
                    ChatColor.AQUA + itemName);
            dialogue(p, messages);
            return true;
        }

        if (!hand.equals(item)) {
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            ArrayList<String> messages = new ArrayList<>();
            messages.add("You do not have the right item");
            messages.add("Use " + ChatColor.YELLOW + "/godquest" + ChatColor.WHITE + " to see the recipe for " +
                    ChatColor.AQUA + itemName);
            dialogue(p, messages);
            return true;
        }

        p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        p.playSound(p, Sound.ENTITY_VILLAGER_CELEBRATE, 1, 1);
        return false;
    }

    public boolean handleItemCheck(Player p, Material mat, String itemName) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            p.sendRawMessage(TrophyManager.npcName + ChatColor.WHITE + ": " + "You are not holding anything");
        }

        if (!hand.getType().equals(mat)) {
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            ArrayList<String> messages = new ArrayList<>();
            messages.add("You do not have the right item");
            messages.add("Use " + ChatColor.YELLOW + "/godquest" + ChatColor.WHITE + " to see the recipe for " +
                    ChatColor.AQUA + itemName);
            dialogue(p, messages);
            return true;
        }

        p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        p.playSound(p, Sound.ENTITY_VILLAGER_CELEBRATE, 1, 1);
        return false;
    }

    // -----------------------
    // Dialogue helpers
    // -----------------------
    public void dialogue(Player p, ArrayList<String> messages) {
        new BukkitRunnable() {
            private int counter = 0;
            @Override
            public void run() {
                if (counter >= messages.size()) {
                    cancel();
                    return;
                }
                p.sendRawMessage(TrophyManager.npcName + ChatColor.WHITE + ": " + messages.get(counter));
                p.playSound(p, Sound.ENTITY_VILLAGER_AMBIENT, 1, 1);
                counter++;
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), 0, 40);
    }

    public void dialogueOpener(Player p) {
        // Initial quest intro
        ArrayList<String> messages = lines(
            "I can grant you great powers",
            "First you must bring me items that show your dedication to this world",
            "Farming is the foundation of any society",
            "Bring me crops to show me you can feed a civilization",
            "Bring me " + ChatColor.AQUA + "2,000 " + ChatColor.WHITE + "bread to start!"
        );
        dialogue(p, messages);
            phase++;
        }

        public void dialogueItemCount(Player p, String item, int count, int max) {
        ArrayList<String> messages = lines(
            "You have brought me " + ChatColor.AQUA + count + ChatColor.WHITE + " " + item,
            "You need to bring me " + ChatColor.AQUA + (max - count) + ChatColor.WHITE + " more " + item
        );
        dialogue(p, messages);
    }

    // -----------------------
    // Phase handlers (kept public API, reduced duplication with helpers)
    // -----------------------
    public void checkFarmingQuest(Player p, int cropType) {
        switch (cropType) {
            case 1 -> bulkStep(p, 2000, Material.BREAD, "bread",
                lines("Excellent work!", "Now bring me " + ChatColor.AQUA + "5,000 " + ChatColor.WHITE + "carrots"), false);
            case 2 -> bulkStep(p, 5000, Material.CARROT, "carrots",
                lines("Excellent work!", "Now bring me " + ChatColor.AQUA + "5,000 " + ChatColor.WHITE + "potatoes"), false);
            case 3 -> bulkStep(p, 5000, Material.POTATO, "potatoes",
                lines("Excellent work!", "Now bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "poisonous potatoes"), false);
            case 4 -> bulkStep(p, 500, Material.POISONOUS_POTATO, "poisonous potatoes",
                lines("Excellent work!", "Now bring me " + ChatColor.AQUA + "5,000 " + ChatColor.WHITE + "beetroots"), false);
            case 5 -> bulkStep(p, 5000, Material.BEETROOT, "beetroots",
                lines("Excellent work!", "Now bring me " + ChatColor.AQUA + "5,000 " + ChatColor.WHITE + "melon slices"), false);
            case 6 -> bulkStep(p, 5000, Material.MELON_SLICE, "melon slices",
                lines("Excellent work!", "Now bring me " + ChatColor.AQUA + "2,500 " + ChatColor.WHITE + "pumpkins"), false);
            case 7 -> bulkStep(p, 2500, Material.PUMPKIN, "pumpkins",
                lines("Excellent work!", "Now bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "sweet berries"), false);
            case 8 -> bulkStep(p, 500, Material.SWEET_BERRIES, "sweet berries",
                lines("Excellent work!", "Now bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "glow berries"), false);
            case 9 -> bulkStep(p, 500, Material.GLOW_BERRIES, "glow berries",
                lines("Excellent work!", "Now bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "apples"), false);
            case 10 -> bulkStep(p, 500, Material.APPLE, "apples",
                lines("Excellent work!", "Now bring me " + ChatColor.AQUA + "64 " + ChatColor.WHITE + "chorus flowers"), false);
            case 11 -> bulkStep(p, 64, Material.CHORUS_FLOWER, "chorus flowers",
                lines("Excellent work!", "Now bring me " + ChatColor.AQUA + "64 " + ChatColor.WHITE + "cakes"), false);
            case 12 -> bulkStep(p, 64, Material.CAKE, "cake",
                lines(
                    "You have proven your dedication to farming",
                    "Now you must bring me your riches",
                    "Bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "coal blocks"
                ), true);
        }
    }

    public void checkOreQuest(Player p, int oreType) {
        switch (oreType) {
            case 1 -> bulkStep(p, 500, Material.COAL_BLOCK, "coal blocks",
                lines("Excellent work!", "Now bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "copper blocks"), false);
            case 2 -> bulkStep(p, 500, Material.COPPER_BLOCK, "copper blocks",
                lines("Excellent work!", "Now bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "iron blocks"), false);
            case 3 -> bulkStep(p, 500, Material.IRON_BLOCK, "iron blocks",
                lines("Excellent work!", "Now bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "lapis blocks"), false);
            case 4 -> bulkStep(p, 500, Material.LAPIS_BLOCK, "lapis blocks",
                lines("Excellent work!", "Now bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "redstone blocks"), false);
            case 5 -> bulkStep(p, 500, Material.REDSTONE_BLOCK, "redstone blocks",
                lines("Excellent work!", "Now bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "gold blocks"), false);
            case 6 -> bulkStep(p, 500, Material.GOLD_BLOCK, "gold blocks",
                lines("Excellent work!", "Now bring me " + ChatColor.AQUA + "200 " + ChatColor.WHITE + "diamond blocks"), false);
            case 7 -> bulkStep(p, 200, Material.DIAMOND_BLOCK, "diamond blocks",
                lines("Excellent work!", "Now bring me " + ChatColor.AQUA + "200 " + ChatColor.WHITE + "emerald blocks"), false);
            case 8 -> bulkStep(p, 200, Material.EMERALD_BLOCK, "emerald blocks",
                lines("Excellent work!", "Now bring me " + ChatColor.AQUA + "64 " + ChatColor.WHITE + "netherite blocks"), false);
            case 9 -> bulkStep(p, 64, Material.NETHERITE_BLOCK, "netherite blocks",
                lines(
                    "You truly do embody the concept of wealth",
                    "Show me the exotic trophies of the creatures of this world",
                    "You must detach yourself from the idea of life or death",
                    "Bring me a modified turtle helmet",
                    "You can see the recipe by using " + ChatColor.YELLOW + "/godquest"
                ), true);
        }
    }

    public void checkCreatureQuest(Player p, int itemType) {
        switch (itemType) {
            case 1 -> handInItemStackStep(p, ItemStackGenerator.getTurtleHelmet(), "Turtle Helmet",
                lines("Excellent Work!", "Now bring me the music of the goats", "You can see the recipe by using " + ChatColor.YELLOW + "/godquest"), false);
            case 2 -> handInItemStackStep(p, ItemStackGenerator.getGoatHorn(), "Goat Horn",
                lines("Excellent Work!", "Now bring me the ochre frog light"), false);
            case 3 -> handInMaterialStep(p, Material.OCHRE_FROGLIGHT, "Ochre Frog Light",
                lines("Excellent Work!", "Now bring me the verdant frog light"), false);
            case 4 -> handInMaterialStep(p, Material.VERDANT_FROGLIGHT, "Verdant Frog Light",
                lines("Excellent Work!", "Now bring me the pearlescent frog light"), false);
            case 5 -> handInMaterialStep(p, Material.PEARLESCENT_FROGLIGHT, "Pearlescent Frog Light",
                lines(
                    "These creatures are just part of this world and there will always be more of them",
                    "Gods do not mourn their loss but appreciate their existence",
                    "Now let me understand the depth of your knowledge",
                    "Bring me an album of music like no other",
                    "You can see the recipe by using " + ChatColor.YELLOW + "/godquest"
                ), true);
        }
    }

    public void checkKnowledgeQuest(Player p, int itemType) {
        switch (itemType) {
            case 1 -> handInItemStackStep(p, ItemStackGenerator.getMusicKnowledgeDisc(), "Music Knowledge Disc",
                lines("Excellent Work!", "Demonstrate your brewing knowledge by bringing me every potion", "Start with a potion of swiftness"), false);
            case 2 -> potionStep(p, PotionType.SWIFTNESS, "Potion of Swiftness",
                lines("Excellent Work!", "Now bring me a potion of fire resistance"));
            case 3 -> potionStep(p, PotionType.FIRE_RESISTANCE, "Potion of Fire Resistance",
                lines("Excellent Work!", "Now bring me a potion of healing"));
            case 4 -> potionStep(p, PotionType.HEALING, "Potion of Healing",
                lines("Excellent Work!", "Now bring me a potion of harming"));
            case 5 -> potionStep(p, PotionType.HARMING, "Potion of Harming",
                lines("Excellent Work!", "Now bring me a potion of water breathing"));
            case 6 -> potionStep(p, PotionType.WATER_BREATHING, "Potion of Water Breathing",
                lines("Excellent Work!", "Now bring me a potion of night vision"));
            case 7 -> potionStep(p, PotionType.NIGHT_VISION, "Potion of Night Vision",
                lines("Excellent Work!", "Now bring me a potion of invisibility"));
            case 8 -> potionStep(p, PotionType.INVISIBILITY, "Potion of Invisibility",
                lines("Excellent Work!", "Now bring me a potion of leaping"));
            case 9 -> potionStep(p, PotionType.LEAPING, "Potion of Leaping",
                lines("Excellent Work!", "Now bring me a potion of slow falling"));
            case 10 -> potionStep(p, PotionType.SLOW_FALLING, "Potion of Slow Falling",
                lines("Excellent Work!", "Now bring me a potion of strength"));
            case 11 -> potionStep(p, PotionType.STRENGTH, "Potion of Strength",
                lines("Excellent Work!", "Now bring me a potion of weakness"));
            case 12 -> potionStep(p, PotionType.WEAKNESS, "Potion of Weakness",
                lines("Excellent Work!", "Now bring me a potion of regeneration"));
            case 13 -> potionStep(p, PotionType.REGENERATION, "Potion of Regeneration",
                lines("Excellent Work!", "Now bring me a potion of poison"));
            case 14 -> potionStep(p, PotionType.POISON, "Potion of Poison",
                lines("Excellent Work!", "Now bring me a potion of infestation"));
            case 15 -> potionStep(p, PotionType.INFESTED, "Potion of Infestation",
                lines("Excellent Work!", "Now bring me a potion of oozing"));
            case 16 -> potionStep(p, PotionType.OOZING, "Potion of Oozing",
                lines("Excellent Work!", "Now bring me a potion of weaving"));
            case 17 -> potionStep(p, PotionType.WEAVING, "Potion of Weaving",
                lines("Excellent Work!", "Now bring me a potion of wind charged"));
            // This potion has a different format due to being the last step
            case 18 -> handInMaterialStep(p, getPotion(PotionType.WIND_CHARGED).getType(), "Potion of Wind Charged",
                lines(
                    "Thank you for demonstrating your knowledge",
                    "Now show me your refined taste in relics",
                    "Bring me something that exhibits refined pottery skills",
                    "You can see the recipe by using " + ChatColor.YELLOW + "/godquest"
                ), true);
        }
    }

    public void checkRelicQuest(Player p, int itemType) {
        switch (itemType) {
            case 1 -> handInItemStackStep(p, ItemStackGenerator.getSherdRelic(), "Sherd Relic",
                lines("Excellent Work!", "Demonstrate your ancient fashion by bringing me the culmination of armor fashion", "You can see the recipe by using " + ChatColor.YELLOW + "/godquest"), false);
            case 2 -> handInItemStackStep(p, ItemStackGenerator.getTrimRelic(), "Trim Relic",
                lines("Your appreciation of times forgotten is noticed", "Show me that you have truly connected with the people of this land", "Trade with villagers 1,000 times"), true);
        }
    }

    public void checkVillagerTradingQuest(Player p) {
        if (currentItemCount < 1000) {
            ArrayList<String> messages = new ArrayList<>();
            messages.add("You have traded with villagers " + ChatColor.AQUA + currentItemCount + ChatColor.WHITE + " times");
            messages.add("You need to trade with villagers " + ChatColor.AQUA + (1000 - currentItemCount) + ChatColor.WHITE + " more times");
            dialogue(p, messages);
        }
        else {
            ArrayList<String> messages1 = new ArrayList<>();
            messages1.add("The villagers clearly trust in you skills as a merchant");
            messages1.add("We are nearing the end of my tribulations");
            messages1.add("Soon you will need to prove yourself in a combat trial");
            messages1.add("Bring me some powerful gear to show me you know what it means to fight");
            messages1.add("You can see the recipe by using " + ChatColor.YELLOW + "/godquest");
            dialogue(p, messages1);
            phase++;
            updateGodTrophyParticles();
        }
    }

    public void checkCombatQuest(Player p) {
        if (handleItemCheck(p, ItemStackGenerator.getWarriorEmblem(), "Warrior Emblem")) return;
        successAndAdvance(p, true, true, true, lines(
            "Excellent Work!",
            "You may have noticed the mobs of this world sometimes drop rare items",
            "Bring me the item from a " + ChatColor.AQUA + "spider"
        ));
    }

    public void checkMobItemQuest(Player p, int itemType) {
        switch (itemType) {
            case 1 -> handInItemStackStep(p, ItemStackGenerator.getWebShooter(), "Web Shooter",
                lines("Excellent Work!", "Bring me the item from a " + ChatColor.AQUA + "skeleton"), false);
            case 2 -> handInItemStackStep(p, ItemStackGenerator.getUnlimitedTippedArrow(), "Unlimited Tipped Arrow",
                lines("Excellent Work!", "Bring me the item from a " + ChatColor.AQUA + "zombie"), false);
            case 3 -> handInItemStackStep(p, ItemStackGenerator.getVillagerRevivalArtifact(), "Villager Revival Artifact",
                lines("Excellent Work!", "Bring me the item from a " + ChatColor.AQUA + "enderman"), false);
            case 4 -> handInItemStackStep(p, ItemStackGenerator.getEnderEssence(), "Ender Essence",
                lines("Excellent Work!", "Bring me the item from a " + ChatColor.AQUA + "creeper"), false);
            case 5 -> handInItemStackStep(p, ItemStackGenerator.getCreeperEssence(), "Creeper Essence",
                lines("Excellent Work!", "Bring me the item from a " + ChatColor.AQUA + "drowned"), false);
            case 6 -> handInItemStackStep(p, ItemStackGenerator.getTridentLauncher(), "Trident Launcher",
                lines("Excellent Work!", "Bring me the item from a " + ChatColor.AQUA + "breeze"), false);
            case 7 -> handInItemStackStep(p, ItemStackGenerator.getMagicBagOfWind(), "Magic Bag Of Wind",
                lines("Excellent Work!", "Bring me the item from a " + ChatColor.AQUA + "wither skeleton"), false);
            case 8 -> handInItemStackStep(p, ItemStackGenerator.getUnlimitedWitherRose(), "Unlimited Wither Rose",
                lines("Excellent Work!", "Bring me the item from a " + ChatColor.AQUA + "ender dragon"), false);
            case 9 -> handInItemStackStep(p, ItemStackGenerator.getDragonBreathCannon(), "Dragon Breath Cannon",
                lines("I now have serveral interesting mob drops", "However you must not forget we are playing a game!", 
                "The final task is to complete all Minecraft advancements"), true);
        }
    }

    /**
     * Check to see if a player has achieved all Minecraft advancements
     * @param p
     */
    public void checkAdvancementsQuest(Player p) {
        if (hasAllAdvancements(p)) {
            successAndAdvance(p, true, false, true, getStrings());
        } else {
            dialogue(p, lines("You have not yet achieved all advancements.",
                "Keep working hard and you will get there!"));
        }
    }

    private static ArrayList<String> getStrings() {
        ArrayList<String> messages9 = new ArrayList<>();
        messages9.add("Excellent Work!");
        messages9.add("You have completed all of my tasks");
        messages9.add("Finally you must prove yourself in the " + ChatColor.RED + "Trial of the Gods " + ChatColor.WHITE + "by defeating the god difficulty");
        messages9.add("Do this and you will be given " + ChatColor.AQUA + "creative mode " + ChatColor.WHITE +
                "using the power of the gods!");
        messages9.add("You can start the trial at anytime, anywhere, by using " + ChatColor.YELLOW + "/godtrial");
        return messages9;
    }


    @SuppressWarnings("deprecation")
    public int removeMaterialsFromInventory(int currentProgress, int max, Inventory inv, Material mat) {
        int total = 0;
        for (ItemStack item : inv.getContents()) {
            if (currentProgress + total == max) return total;

            // Ensure the item exists and is not a custom item
            if (item == null) continue;
            if (!item.getType().equals(mat)) continue;
            if (item.getItemMeta() != null && item.getItemMeta().hasCustomModelData()) continue;

            // Handle the item in the inventory
            total += item.getAmount();
            if (currentProgress + total > max) {
                int remaining = max - currentProgress + total - item.getAmount();
                item.setAmount(item.getAmount() - remaining);
                if (item.getAmount() == 0) inv.remove(item);
                return total;
            }
            item.setAmount(0);
            inv.remove(item);
        }
        return total;
    }

    public void removeItemFromMainHand(Player p) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) return;
        hand.setAmount(hand.getAmount() - 1);
    }

    public void updateGodTrophyParticles() {
        for (Map.Entry<Location, Trophy> trophy : SurvivalSkills.getInstance().getTrophyManager().getTrophies().entrySet()) {
            if (!trophy.getValue().getType().equalsIgnoreCase("godtrophy")) continue;
            if (!trophy.getValue().getUUID().equals(uuid)) continue;
            TrophyEffects effects = trophy.getValue().getEffects();
            if (effects.getGodTrophy() == null) continue;
            // TODO: Implement particle effects for the god trophy
            return;
        }
    }

    public void setCurrentItemCount(int count) {
        this.currentItemCount = count;
    }

    public ItemStack getPotion(PotionType type) {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta == null) return potion;
        meta.setBasePotionType(type);
        potion.setItemMeta(meta);
        return potion;
    }

    public boolean hasAllAdvancements(Player p) {
        Iterator<Advancement> advancementIterator = Bukkit.advancementIterator();

        while (advancementIterator.hasNext()) {
            Advancement advancement = advancementIterator.next();
            if (!p.getAdvancementProgress(advancement).isDone()) {
                return false;
            }
        }

        return true;
    }

    public int getCurrentItemCount() {
        return currentItemCount;
    }

    public int getPhase() {
        return phase;
    }

    public int getMaxPhase() {
        return maxPhase;
    }

    // -----------------------
    // Private helpers to reduce duplication
    // -----------------------

    /** Small utility for building dialogue lists inline. */
    private static ArrayList<String> lines(String... text) {
        ArrayList<String> list = new ArrayList<>();
        for (String t : text) list.add(t);
        return list;
    }

    /**
     * Common success flow: send dialogue, optionally reset counts, remove one from hand, update particles, and advance phase.
     */
    private void successAndAdvance(Player p, boolean resetCount, boolean removeFromHand, boolean updateParticles, ArrayList<String> messages) {
        if (removeFromHand) removeItemFromMainHand(p);
        dialogue(p, messages);
        phase++;
        if (resetCount) currentItemCount = 0;
        if (updateParticles) updateGodTrophyParticles();
    }

    /**
     * Bulk material collection step handler (e.g., farming/ore). When completed, resets count and advances phase.
     */
    private void bulkStep(Player p, int required, Material mat, String itemName, ArrayList<String> successMessages, boolean updateParticlesAfter) {
        if (handleItemCheck(required, p, mat, itemName)) return; // still collecting
        successAndAdvance(p, true, false, updateParticlesAfter, successMessages);
    }

    /** Hand-in step for exact ItemStack in main hand. */
    private void handInItemStackStep(Player p, ItemStack expected, String itemName, ArrayList<String> successMessages, boolean updateParticlesAfter) {
        if (handleItemCheck(p, expected, itemName)) return; // wrong or missing item
        successAndAdvance(p, true, true, updateParticlesAfter, successMessages);
    }

    /** Hand-in step for a specific Material in main hand. */
    private void handInMaterialStep(Player p, Material mat, String itemName, ArrayList<String> successMessages, boolean updateParticlesAfter) {
        if (handleItemCheck(p, mat, itemName)) return; // wrong or missing item
        successAndAdvance(p, true, true, updateParticlesAfter, successMessages);
    }

    /** Potion hand-in convenience wrapper. */
    private void potionStep(Player p, PotionType type, String friendlyName, ArrayList<String> successMessages) {
        if (handleItemCheck(p, getPotion(type), friendlyName)) return;
        successAndAdvance(p, true, true, false, successMessages);
    }
}
