package sir_draco.survivalskills.Trophy.GodQuestline;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.Trophy.Trophy;
import sir_draco.survivalskills.Trophy.TrophyEffects;
import sir_draco.survivalskills.Trophy.TrophyManager;
import sir_draco.survivalskills.Utils.ItemStackGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

public class GodTrophyQuest {

    private final UUID uuid;

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

    public void handleNPCInteract(Player p) {
        if (phase >= 49) return;
        switch (phase) {
            case 0: dialogueOpener(p);
            case 1: checkFarmingQuest(p, 1);
            case 2: checkFarmingQuest(p, 2);
            case 3: checkFarmingQuest(p, 3);
            case 4: checkFarmingQuest(p, 4);
            case 5: checkFarmingQuest(p, 5);
            case 6: checkFarmingQuest(p, 6);
            case 7: checkFarmingQuest(p, 7);
            case 8: checkOreQuest(p, 1);
            case 9: checkOreQuest(p, 2);
            case 10: checkOreQuest(p, 3);
            case 11: checkOreQuest(p, 4);
            case 12: checkOreQuest(p, 5);
            case 13: checkCreatureQuest(p, 1);
            case 14: checkCreatureQuest(p, 2);
            case 15: checkCreatureQuest(p, 3);
            case 16: checkCreatureQuest(p, 4);
            case 17: checkCreatureQuest(p, 5);
            case 18: checkKnowledgeQuest(p, 1);
            case 19: checkKnowledgeQuest(p, 2);
            case 20: checkKnowledgeQuest(p, 3);
            case 21: checkKnowledgeQuest(p, 4);
            case 22: checkKnowledgeQuest(p, 5);
            case 23: checkKnowledgeQuest(p, 6);
            case 24: checkKnowledgeQuest(p, 7);
            case 25: checkKnowledgeQuest(p, 8);
            case 26: checkKnowledgeQuest(p, 9);
            case 27: checkKnowledgeQuest(p, 10);
            case 28: checkKnowledgeQuest(p, 11);
            case 29: checkKnowledgeQuest(p, 12);
            case 30: checkKnowledgeQuest(p, 13);
            case 31: checkKnowledgeQuest(p, 14);
            case 32: checkKnowledgeQuest(p, 15);
            case 33: checkKnowledgeQuest(p, 16);
            case 34: checkKnowledgeQuest(p, 17);
            case 35: checkKnowledgeQuest(p, 18);
            case 36: checkRelicQuest(p, 1);
            case 37: checkRelicQuest(p, 2);
            case 38: checkVillagerTradingQuest(p);
            case 39: checkCombatQuest(p);
            case 40: checkMobItemQuest(p, 1);
            case 41: checkMobItemQuest(p, 2);
            case 42: checkMobItemQuest(p, 3);
            case 43: checkMobItemQuest(p, 4);
            case 44: checkMobItemQuest(p, 5);
            case 45: checkMobItemQuest(p, 6);
            case 46: checkMobItemQuest(p, 7);
            case 47: checkMobItemQuest(p, 8);
            case 48: checkMobItemQuest(p, 9);
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
        return false;
    }

    public boolean handleItemCheck(Player p, ItemStack item, String itemName) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType().isAir()) {
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
            p.sendRawMessage(TrophyManager.npcName + ChatColor.WHITE + ": " + "You are not holding anything");
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
        return false;
    }

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
                counter++;
            }
        }.runTaskTimer(SurvivalSkills.getInstance(), 0, 40);
    }

    public void dialogueOpener(Player p) {
        ArrayList<String> messages = new ArrayList<>();
        messages.add("I can grant you great powers");
        messages.add("First you must bring me items that show your dedication to this world");
        messages.add("Farming is the foundation of any society");
        messages.add("Bring me crops to show me you can feed a civilization");
        messages.add("Bring me " + ChatColor.AQUA + "25,000 " + ChatColor.WHITE + "carrots to start!");
        dialogue(p, messages);
    }

    public void dialogueItemCount(Player p, String item, int count, int max) {
        ArrayList<String> messages = new ArrayList<>();
        messages.add("You have brought me " + ChatColor.AQUA + count + ChatColor.WHITE + " " + item);
        messages.add("You need to bring me " + ChatColor.AQUA + (max - count) + ChatColor.WHITE + " more " + item);
        dialogue(p, messages);
    }


    public void checkFarmingQuest(Player p, int cropType) {
        switch (cropType) {
            case 1:
                if (handleItemCheck(25000, p, Material.CARROT, "carrots")) return;
                ArrayList<String> messages = new ArrayList<>();
                messages.add("Excellent work!");
                messages.add("Now bring me " + ChatColor.AQUA + "25,000 " + ChatColor.WHITE + "melon slices");
                dialogue(p, messages);
                phase++;
                currentItemCount = 0;
                break;
            case 2:
                if (handleItemCheck(25000, p, Material.MELON_SLICE, "melon slices")) return;
                ArrayList<String> messages2 = new ArrayList<>();
                messages2.add("Excellent work!");
                messages2.add("Now bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "apples");
                dialogue(p, messages2);
                phase++;
                currentItemCount = 0;
                break;
            case 3:
                if (handleItemCheck(500, p, Material.APPLE, "apples")) return;
                ArrayList<String> messages3 = new ArrayList<>();
                messages3.add("Excellent work!");
                messages3.add("Now bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "poisonous potatoes");
                dialogue(p, messages3);
                phase++;
                currentItemCount = 0;
                break;
            case 4:
                if (handleItemCheck(500, p, Material.POISONOUS_POTATO, "poisonous potatoes")) return;
                ArrayList<String> messages4 = new ArrayList<>();
                messages4.add("Excellent work!");
                messages4.add("Now bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "glow berries");
                dialogue(p, messages4);
                phase++;
                currentItemCount = 0;
                break;
            case 5:
                if (handleItemCheck(500, p, Material.GLOW_BERRIES, "glow berries")) return;
                ArrayList<String> messages5 = new ArrayList<>();
                messages5.add("Excellent work!");
                messages5.add("Now bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "chorus fruit");
                dialogue(p, messages5);
                phase++;
                currentItemCount = 0;
                break;
            case 6:
                if (handleItemCheck(500, p, Material.CHORUS_FRUIT, "chorus fruit")) return;
                ArrayList<String> messages6 = new ArrayList<>();
                messages6.add("Excellent work!");
                messages6.add("Now bring me " + ChatColor.AQUA + "64 " + ChatColor.WHITE + "chorus flowers");
                dialogue(p, messages6);
                phase++;
                currentItemCount = 0;
                break;
            case 7:
                if (handleItemCheck(64, p, Material.CHORUS_FLOWER, "chorus flowers")) return;
                ArrayList<String> messages7 = new ArrayList<>();
                messages7.add("You have proven your dedication to farming");
                messages7.add("Now you must bring me your riches");
                messages7.add("Bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "iron blocks");
                dialogue(p, messages7);
                phase++;
                currentItemCount = 0;
                updateGodTrophyParticles();
                break;
        }
    }

    public void checkOreQuest(Player p, int oreType) {
        switch (oreType) {
            case 1:
                if (handleItemCheck(500, p, Material.IRON_BLOCK, "iron blocks")) return;
                ArrayList<String> messages = new ArrayList<>();
                messages.add("Excellent work!");
                messages.add("Now bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "gold blocks");
                dialogue(p, messages);
                phase++;
                currentItemCount = 0;
                break;
            case 2:
                if (handleItemCheck(500, p, Material.GOLD_BLOCK, "gold blocks")) return;
                ArrayList<String> messages2 = new ArrayList<>();
                messages2.add("Excellent work!");
                messages2.add("Now bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "diamond blocks");
                dialogue(p, messages2);
                phase++;
                currentItemCount = 0;
                break;
            case 3:
                if (handleItemCheck(500, p, Material.DIAMOND_BLOCK, "diamond blocks")) return;
                ArrayList<String> messages3 = new ArrayList<>();
                messages3.add("Excellent work!");
                messages3.add("Now bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "emerald blocks");
                dialogue(p, messages3);
                phase++;
                currentItemCount = 0;
                break;
            case 4:
                if (handleItemCheck(500, p, Material.EMERALD_BLOCK, "emerald blocks")) return;
                ArrayList<String> messages4 = new ArrayList<>();
                messages4.add("Excellent work!");
                messages4.add("Now bring me " + ChatColor.AQUA + "64 " + ChatColor.WHITE + "netherite blocks");
                dialogue(p, messages4);
                phase++;
                currentItemCount = 0;
                break;
            case 5:
                if (handleItemCheck(64, p, Material.NETHERITE_BLOCK, "netherite blocks")) return;
                ArrayList<String> messages5 = new ArrayList<>();
                messages5.add("You truly do embody the concept of wealth");
                messages5.add("Show me the exotic trophies of the creatures of this world");
                messages5.add("You must detach yourself from the idea of life or death");
                messages5.add("Bring me a modified turtle helmet");
                messages5.add("You can see the recipe by using " + ChatColor.YELLOW + "/godquest");
                dialogue(p, messages5);
                phase++;
                currentItemCount = 0;
                updateGodTrophyParticles();
                break;
        }
    }

    public void checkCreatureQuest(Player p, int itemType) {
        switch (itemType) {
            case 1:
                if (handleItemCheck(p, ItemStackGenerator.getTurtleHelmet(), "Turtle Helmet")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages1 = new ArrayList<>();
                messages1.add("Excellent Work!");
                messages1.add("Now bring me the music of the goats");
                dialogue(p, messages1);
                phase++;
                break;
            case 2:
                if (handleItemCheck(p, ItemStackGenerator.getGoatHorn(), "Goat Horn")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages2 = new ArrayList<>();
                messages2.add("Excellent Work!");
                messages2.add("Now bring me the ochre frog light");
                dialogue(p, messages2);
                phase++;
                break;
            case 3:
                if (handleItemCheck(p, Material.OCHRE_FROGLIGHT, "Ochre Frog Light")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages3 = new ArrayList<>();
                messages3.add("Excellent Work!");
                messages3.add("Now bring me the verdant frog light");
                dialogue(p, messages3);
                phase++;
                break;
            case 4:
                if (handleItemCheck(p, Material.VERDANT_FROGLIGHT, "Verdant Frog Light")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages4 = new ArrayList<>();
                messages4.add("Excellent Work!");
                messages4.add("Now bring me the pearlescent frog light");
                dialogue(p, messages4);
                phase++;
                break;
            case 5:
                if (handleItemCheck(p, Material.PEARLESCENT_FROGLIGHT, "Pearlescent Frog Light")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages5 = new ArrayList<>();
                messages5.add("These creatures are just part of this world and there will always be more of them");
                messages5.add("Do not mourn their loss");
                messages5.add("Now let me understand the depth of your knowledge");
                messages5.add("Show me your mastery of potions");
                messages5.add("You can see the recipe by using " + ChatColor.YELLOW + "/godquest");
                dialogue(p, messages5);
                phase++;
                updateGodTrophyParticles();
                break;
        }
    }

    public void checkKnowledgeQuest(Player p, int itemType) {
        switch (itemType) {
            case 1:
                if (handleItemCheck(p, ItemStackGenerator.getMusicKnowledgeDisc(), "Music Knowledge Disc")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages1 = new ArrayList<>();
                messages1.add("Excellent Work!");
                messages1.add("Demonstrate your brewing knowledge by bringing me every potion");
                messages1.add("Start with a potion of swiftness");
                dialogue(p, messages1);
                phase++;
                break;
            case 2:
                if (handleItemCheck(p, getPotion(PotionType.SWIFTNESS), "Potion of Swiftness")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages2 = new ArrayList<>();
                messages2.add("Excellent Work!");
                messages2.add("Now bring me a potion of fire resistance");
                dialogue(p, messages2);
                phase++;
                break;
            case 3:
                if (handleItemCheck(p, getPotion(PotionType.FIRE_RESISTANCE), "Potion of Fire Resistance")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages3 = new ArrayList<>();
                messages3.add("Excellent Work!");
                messages3.add("Now bring me a potion of healing");
                dialogue(p, messages3);
                phase++;
                break;
            case 4:
                if (handleItemCheck(p, getPotion(PotionType.HEALING), "Potion of Healing")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages4 = new ArrayList<>();
                messages4.add("Excellent Work!");
                messages4.add("Now bring me a potion of harming");
                dialogue(p, messages4);
                phase++;
                break;
            case 5:
                if (handleItemCheck(p, getPotion(PotionType.HARMING), "Potion of Harming")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages5 = new ArrayList<>();
                messages5.add("Excellent Work!");
                messages5.add("Now bring me a potion of water breathing");
                dialogue(p, messages5);
                phase++;
                break;
            case 6:
                if (handleItemCheck(p, getPotion(PotionType.WATER_BREATHING), "Potion of Water Breathing")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages6 = new ArrayList<>();
                messages6.add("Excellent Work!");
                messages6.add("Now bring me a potion of night vision");
                dialogue(p, messages6);
                phase++;
                break;
            case 7:
                if (handleItemCheck(p, getPotion(PotionType.NIGHT_VISION), "Potion of Night Vision")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages7 = new ArrayList<>();
                messages7.add("Excellent Work!");
                messages7.add("Now bring me a potion of invisibility");
                dialogue(p, messages7);
                phase++;
                break;
            case 8:
                if (handleItemCheck(p, getPotion(PotionType.INVISIBILITY), "Potion of Invisibility")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages8 = new ArrayList<>();
                messages8.add("Excellent Work!");
                messages8.add("Now bring me a potion of leaping");
                dialogue(p, messages8);
                phase++;
                break;
            case 9:
                if (handleItemCheck(p, getPotion(PotionType.LEAPING), "Potion of Leaping")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages9 = new ArrayList<>();
                messages9.add("Excellent Work!");
                messages9.add("Now bring me a potion of slow falling");
                dialogue(p, messages9);
                phase++;
                break;
            case 10:
                if (handleItemCheck(p, getPotion(PotionType.SLOW_FALLING), "Potion of Slow Falling")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages10 = new ArrayList<>();
                messages10.add("Excellent Work!");
                messages10.add("Now bring me a potion of strength");
                dialogue(p, messages10);
                phase++;
                break;
            case 11:
                if (handleItemCheck(p, getPotion(PotionType.STRENGTH), "Potion of Strength")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages11 = new ArrayList<>();
                messages11.add("Excellent Work!");
                messages11.add("Now bring me a potion of weakness");
                dialogue(p, messages11);
                phase++;
                break;
            case 12:
                if (handleItemCheck(p, getPotion(PotionType.WEAKNESS), "Potion of Weakness")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages14 = new ArrayList<>();
                messages14.add("Excellent Work!");
                messages14.add("Now bring me a potion of regeneration");
                dialogue(p, messages14);
                phase++;
                break;
            case 13:
                if (handleItemCheck(p, getPotion(PotionType.REGENERATION), "Potion of Regeneration")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages15 = new ArrayList<>();
                messages15.add("Excellent Work!");
                messages15.add("Now bring me a potion of poison");
                dialogue(p, messages15);
                phase++;
                break;
            case 14:
                if (handleItemCheck(p, getPotion(PotionType.POISON), "Potion of Poison")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages16 = new ArrayList<>();
                messages16.add("Excellent Work!");
                messages16.add("Now bring me a potion of infestation");
                dialogue(p, messages16);
                phase++;
                break;
            case 15:
                if (handleItemCheck(p, getPotion(PotionType.INFESTED), "Potion of Infestation")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages17 = new ArrayList<>();
                messages17.add("Excellent Work!");
                messages17.add("Now bring me a potion of weakness");
                dialogue(p, messages17);
                phase++;
                break;
            case 16:
                if (handleItemCheck(p, getPotion(PotionType.OOZING), "Potion of the Oozing")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages18 = new ArrayList<>();
                messages18.add("Excellent Work!");
                messages18.add("Now bring me a potion of weaving");
                dialogue(p, messages18);
                phase++;
                break;
            case 17:
                if (handleItemCheck(p, getPotion(PotionType.WEAVING), "Potion of Weaving")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages19 = new ArrayList<>();
                messages19.add("Excellent Work!");
                messages19.add("Now bring me a potion of wind charged");
                dialogue(p, messages19);
                phase++;
                break;
            case 18:
                if (handleItemCheck(p, getPotion(PotionType.WIND_CHARGED), "Potion of Wind Charged")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages20 = new ArrayList<>();
                messages20.add("Thank you for demonstrating your knowledge");
                messages20.add("Now show me your refined taste in relics");
                messages20.add("Bring me something that exhibits refined pottery skills");
                messages20.add("You can see the recipe by using " + ChatColor.YELLOW + "/godquest");
                dialogue(p, messages20);
                phase++;
                updateGodTrophyParticles();
                break;
        }
    }

    public void checkRelicQuest(Player p, int itemType) {
        switch (itemType) {
            case 1:
                if (handleItemCheck(p, ItemStackGenerator.getSherdRelic(), "Sherd Relic")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages1 = new ArrayList<>();
                messages1.add("Excellent Work!");
                messages1.add("Demonstrate your ancient fashion by bringing me the culmination of armor fashion");
                messages1.add("You can see the recipe by using " + ChatColor.YELLOW + "/godquest");
                dialogue(p, messages1);
                phase++;
                break;
            case 2:
                if (handleItemCheck(p, ItemStackGenerator.getTrimRelic(), "Trim Relic")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages2 = new ArrayList<>();
                messages2.add("Your appreciation of times forgotten is noticed");
                messages2.add("Show me that you have truly connected with the people of this land");
                messages2.add("Trade with villagers 1,000 times");
                dialogue(p, messages2);
                phase++;
                updateGodTrophyParticles();
                break;
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
        removeItemFromMainHand(p);
        ArrayList<String> messages1 = new ArrayList<>();
        messages1.add("Excellent Work!");
        messages1.add("Now the final task");
        messages1.add("You may have noticed the mobs of this world sometimes drop rare items");
        messages1.add("Bring me the item from a " + ChatColor.AQUA + "spider");
        dialogue(p, messages1);
        phase++;
        updateGodTrophyParticles();
    }

    public void checkMobItemQuest(Player p, int itemType) {
        switch (itemType) {
            case 1:
                if (handleItemCheck(p, ItemStackGenerator.getWebShooter(), "Web Shooter")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages1 = new ArrayList<>();
                messages1.add("Excellent Work!");
                messages1.add("Bring me the item from a " + ChatColor.AQUA + "skeleton");
                dialogue(p, messages1);
                phase++;
                break;
            case 2:
                if (handleItemCheck(p, ItemStackGenerator.getUnlimitedTippedArrow(), "Unlimited Tipped Arrow")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages2 = new ArrayList<>();
                messages2.add("Excellent Work!");
                messages2.add("Bring me the item from a " + ChatColor.AQUA + "zombie");
                dialogue(p, messages2);
                phase++;
                break;
            case 3:
                if (handleItemCheck(p, ItemStackGenerator.getVillagerRevivalArtifact(), "Villager Revival Artifact")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages3 = new ArrayList<>();
                messages3.add("Excellent Work!");
                messages3.add("Bring me the item from a " + ChatColor.AQUA + "enderman");
                dialogue(p, messages3);
                phase++;
                break;
            case 4:
                if (handleItemCheck(p, ItemStackGenerator.getEnderEssence(), "Ender Essence")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages4 = new ArrayList<>();
                messages4.add("Excellent Work!");
                messages4.add("Bring me the item from a " + ChatColor.AQUA + "creeper");
                dialogue(p, messages4);
                phase++;
                break;
            case 5:
                if (handleItemCheck(p, ItemStackGenerator.getCreeperEssence(), "Creeper Essence")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages5 = new ArrayList<>();
                messages5.add("Excellent Work!");
                messages5.add("Bring me the item from a " + ChatColor.AQUA + "drowned");
                dialogue(p, messages5);
                phase++;
                break;
            case 6:
                if (handleItemCheck(p, ItemStackGenerator.getTridentLauncher(), "Trident Launcher")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages6 = new ArrayList<>();
                messages6.add("Excellent Work!");
                messages6.add("Bring me the item from a " + ChatColor.AQUA + "breeze");
                dialogue(p, messages6);
                phase++;
                break;
            case 7:
                if (handleItemCheck(p, ItemStackGenerator.getMagicBagOfWind(), "Magic Bag Of Wind")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages7 = new ArrayList<>();
                messages7.add("Excellent Work!");
                messages7.add("Bring me the item from a " + ChatColor.AQUA + "ender dragon");
                dialogue(p, messages7);
                phase++;
                break;
            case 8:
                if (handleItemCheck(p, ItemStackGenerator.getDragonBreathCannon(), "Dragon Breath Cannon")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages8 = new ArrayList<>();
                messages8.add("Excellent Work!");
                messages8.add("Bring me the item from a " + ChatColor.AQUA + "wither skeleton");
                dialogue(p, messages8);
                phase++;
                break;
            case 9:
                if (handleItemCheck(p, ItemStackGenerator.getUnlimitedWitherRose(), "Unlimited Wither Rose")) return;
                removeItemFromMainHand(p);
                ArrayList<String> messages9 = new ArrayList<>();
                messages9.add("Excellent Work!");
                messages9.add("You have completed all of my tasks");
                messages9.add("Finally you must prove yourself in the " + ChatColor.RED + "Trial of the Gods");
                messages9.add("Do this and you will be given " + ChatColor.AQUA + "creative mode " + ChatColor.WHITE +
                        "and the ability of " + ChatColor.AQUA + " teleportation");
                messages9.add("You can start the trial at anytime by using " + ChatColor.YELLOW + "/godtrial");
                dialogue(p, messages9);
                phase++;
                updateGodTrophyParticles();
                break;
        }
    }


    public int removeMaterialsFromInventory(int currentProgress, int max, Inventory inv, Material mat) {
        int total = 0;
        for (ItemStack item : inv.getContents().clone()) {
            if (currentProgress + total == max) return total;

            // Ensure the item exists and is not a custom item
            if (item == null) continue;
            if (item.getType() != mat) continue;
            if (item.getItemMeta() != null) continue;
            if (item.getItemMeta().hasCustomModelData()) continue;
            if (item.getItemMeta().hasLore()) continue;

            // Handle the item in the inventory
            total += item.getAmount();
            if (currentProgress + total > max) {
                int remaining = max - currentProgress;
                item.setAmount(item.getAmount() - remaining);
                return total;
            }
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
            effects.getGodTrophy().increaseQuestParticles();
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

    public int getCurrentItemCount() {
        return currentItemCount;
    }

    public int getPhase() {
        return phase;
    }
}
