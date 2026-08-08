package sir_draco.survivalskills.god_questline;

import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.scheduler.BukkitRunnable;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.trophy.Trophy;
import sir_draco.survivalskills.trophy.TrophyEffects;
import sir_draco.survivalskills.trophy.TrophyManager;
import sir_draco.survivalskills.utils.items.ItemStackGenerator;
import sir_draco.survivalskills.utils.items.ItemStackGeneratorUtils;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Objects;
import java.util.UUID;

public class GodTrophyQuest {

    private static final int MAX_PHASE = 59;

    private final UUID uuid;
    private final boolean allAdvancements;

    private int currentItemCount = 0;

    private int phase = 0;

    private BukkitRunnable activeDialogueTask;

    public record QuestProgress(String currentStep, int currentCount, int goal, String nextStep) {

        public QuestProgress {
            Objects.requireNonNull(currentStep);
            Objects.requireNonNull(nextStep);
            if (currentCount < 0)
                throw new IllegalArgumentException("Current count cannot be negative");
            if (goal < 1)
                throw new IllegalArgumentException("Goal must be positive");
        }
    }

    private record QuestStep(String currentStep, int goal, String nextStep) {

        private QuestStep {
            Objects.requireNonNull(currentStep);
            Objects.requireNonNull(nextStep);
            if (goal < 1)
                throw new IllegalArgumentException("Goal must be positive");
        }
    }

    private enum PhaseGroup {
        INTRO(0, 0, 0),
        FARMING(1, 12, 0),
        ORE(13, 21, 0),
        CREATURE(22, 26, 1),
        KNOWLEDGE(27, 44, 2),
        RELIC(45, 46, 3),
        VILLAGER(47, 47, 0),
        COMBAT(48, 48, 4),
        MOB_ITEM(49, 57, 0),
        ADVANCEMENT(58, 58, 0);

        final int start;
        final int end;
        final int stage;

        PhaseGroup(int start, int end, int stage) {
            this.start = start;
            this.end = end;
            this.stage = stage;
        }

        boolean contains(int phase) {
            return phase >= start && phase <= end;
        }

        int localIndex(int phase) {
            return phase - start + 1;
        }
    }

    private static final Map<Integer, PhaseGroup> PHASE_TO_GROUP = new HashMap<>();
    private static final List<PhaseGroup> GOD_QUEST_GROUPS = Arrays.stream(PhaseGroup.values())
            .filter((PhaseGroup group) -> group != PhaseGroup.INTRO)
            .toList();
    private static final List<Color> GOD_TROPHY_AURA_COLORS = List.of(
            Color.fromRGB(160, 160, 160), // Quest not started
            Color.fromRGB(60, 180, 75),   // Farming
            Color.fromRGB(255, 190, 35),  // Ore
            Color.fromRGB(160, 70, 220),  // Creature
            Color.fromRGB(55, 140, 255),  // Knowledge
            Color.fromRGB(255, 90, 180),  // Relic
            Color.fromRGB(45, 210, 120),  // Villager
            Color.fromRGB(235, 55, 55),   // Combat
            Color.fromRGB(75, 25, 130),   // Mob items
            Color.fromRGB(245, 245, 255)  // Advancements
    );
    private static final List<QuestStep> QUEST_STEPS = List.of(
            step("Speak to the God Trophy", 1, "2,000 bread"),
            step("Bread", 2_000, "5,000 carrots"),
            step("Carrots", 5_000, "5,000 potatoes"),
            step("Potatoes", 5_000, "500 poisonous potatoes"),
            step("Poisonous Potatoes", 500, "5,000 beetroots"),
            step("Beetroots", 5_000, "5,000 melon slices"),
            step("Melon Slices", 5_000, "2,500 pumpkins"),
            step("Pumpkins", 2_500, "500 sweet berries"),
            step("Sweet Berries", 500, "500 glow berries"),
            step("Glow Berries", 500, "500 apples"),
            step("Apples", 500, "64 chorus flowers"),
            step("Chorus Flowers", 64, "64 cakes"),
            step("Cakes", 64, "500 coal blocks"),
            step("Coal Blocks", 500, "500 copper blocks"),
            step("Copper Blocks", 500, "500 iron blocks"),
            step("Iron Blocks", 500, "500 lapis blocks"),
            step("Lapis Blocks", 500, "500 redstone blocks"),
            step("Redstone Blocks", 500, "500 gold blocks"),
            step("Gold Blocks", 500, "200 diamond blocks"),
            step("Diamond Blocks", 200, "200 emerald blocks"),
            step("Emerald Blocks", 200, "64 netherite blocks"),
            step("Netherite Blocks", 64, "Modified Turtle Helmet"),
            step("Modified Turtle Helmet", 1, "Goat Horn"),
            step("Goat Horn", 1, "Ochre Frog Light"),
            step("Ochre Frog Light", 1, "Verdant Frog Light"),
            step("Verdant Frog Light", 1, "Pearlescent Frog Light"),
            step("Pearlescent Frog Light", 1, "Music Knowledge Disc"),
            step("Music Knowledge Disc", 1, "Potion of Swiftness"),
            step("Potion of Swiftness", 1, "Potion of Fire Resistance"),
            step("Potion of Fire Resistance", 1, "Potion of Healing"),
            step("Potion of Healing", 1, "Potion of Harming"),
            step("Potion of Harming", 1, "Potion of Water Breathing"),
            step("Potion of Water Breathing", 1, "Potion of Night Vision"),
            step("Potion of Night Vision", 1, "Potion of Invisibility"),
            step("Potion of Invisibility", 1, "Potion of Leaping"),
            step("Potion of Leaping", 1, "Potion of Slow Falling"),
            step("Potion of Slow Falling", 1, "Potion of Strength"),
            step("Potion of Strength", 1, "Potion of Weakness"),
            step("Potion of Weakness", 1, "Potion of Regeneration"),
            step("Potion of Regeneration", 1, "Potion of Poison"),
            step("Potion of Poison", 1, "Potion of Infestation"),
            step("Potion of Infestation", 1, "Potion of Oozing"),
            step("Potion of Oozing", 1, "Potion of Weaving"),
            step("Potion of Weaving", 1, "Potion of Wind Charged"),
            step("Potion of Wind Charged", 1, "Sherd Relic"),
            step("Sherd Relic", 1, "Trim Relic"),
            step("Trim Relic", 1, "1,000 villager trades"),
            step("Villager Trades", 1_000, "Warrior Emblem"),
            step("Warrior Emblem", 1, "Web Shooter"),
            step("Web Shooter", 1, "Unlimited Tipped Arrow"),
            step("Unlimited Tipped Arrow", 1, "Villager Revival Artifact"),
            step("Villager Revival Artifact", 1, "Ender Essence"),
            step("Ender Essence", 1, "Creeper Essence"),
            step("Creeper Essence", 1, "Trident Launcher"),
            step("Trident Launcher", 1, "Magic Bag Of Wind"),
            step("Magic Bag Of Wind", 1, "Unlimited Wither Rose"),
            step("Unlimited Wither Rose", 1, "Dragon Breath Cannon"),
            step("Dragon Breath Cannon", 1, "All Minecraft advancements"),
            step("All Minecraft Advancements", 1, "Trial of the Gods")
    );
    static {
        if (QUEST_STEPS.size() != MAX_PHASE)
            throw new IllegalStateException("God quest step metadata must cover every active phase");
        for (int i = 0; i <= MAX_PHASE; i++) {
            for (PhaseGroup group : PhaseGroup.values()) {
                if (group.contains(i)) {
                    PHASE_TO_GROUP.put(i, group);
                    break;
                }
            }
        }
    }

    public GodTrophyQuest(UUID uuid) {
        this.uuid = uuid;
        loadProgress();
        allAdvancements = SurvivalSkills.getInstance().getConfig().getBoolean("AllAdvancements");
    }

    public void loadProgress() {
        File file = new File(SurvivalSkills.getInstance().getDataFolder(), "godquests.yml");
        if (!file.exists())
            SurvivalSkills.getInstance().saveResource("godquests.yml", true);
        FileConfiguration data = YamlConfiguration.loadConfiguration(file);

        if (!data.contains(uuid.toString()))
            return;
        currentItemCount = data.getInt(uuid + ".ItemCount");
        phase = data.getInt(uuid + ".Phase");
    }

    /**
     * Entry point for the NPC interaction. Routes to the appropriate phase handler.
     * Each PhaseGroup constant defines its phase range and local-index mapping.
     */
    public void handleNPCInteract(Player p) {
        if (phase >= MAX_PHASE)
            return;

        PhaseGroup group = PHASE_TO_GROUP.get(phase);
        if (group == null)
            return;

        switch (group) {
            case INTRO -> dialogueOpener(p);
            case FARMING -> checkFarmingQuest(p, phase);
            case ORE -> checkOreQuest(p, group.localIndex(phase));
            case CREATURE -> checkCreatureQuest(p, group.localIndex(phase));
            case KNOWLEDGE -> checkKnowledgeQuest(p, group.localIndex(phase));
            case RELIC -> checkRelicQuest(p, group.localIndex(phase));
            case VILLAGER -> checkVillagerTradingQuest(p);
            case COMBAT -> checkCombatQuest(p);
            case MOB_ITEM -> checkMobItemQuest(p, group.localIndex(phase));
            case ADVANCEMENT -> checkAdvancementsQuest(p);
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
        playSuccessSound(p);
        return false;
    }

    public boolean handleItemCheck(Player p, ItemStack item, String itemName) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType().isAir())
            return emptyHandItemStackDialogue(p, itemName);
        if (!hand.equals(item))
            return wrongItemDialogue(p, itemName);
        playSuccessSound(p);
        return false;
    }

    public boolean handleItemCheck(Player p, Material mat, String itemName) {
        if (p.getInventory().getItemInMainHand().getType().isAir())
            return emptyHandItemStackDialogue(p, itemName);
        if (!p.getInventory().getItemInMainHand().getType().equals(mat))
            return wrongItemDialogue(p, itemName);
        playSuccessSound(p);
        return false;
    }

    // -----------------------
    // Dialogue helpers
    // -----------------------
    public void dialogue(Player p, List<String> messages) {
        cancelActiveDialogue();
        activeDialogueTask = new BukkitRunnable() {
            private int counter = 0;

            @Override
            public void run() {
                if (!p.isOnline() || counter >= messages.size()) {
                    cancel();
                    activeDialogueTask = null;
                    return;
                }
                p.sendRawMessage(TrophyManager.npcName + ChatColor.WHITE + ": " + messages.get(counter));
                p.playSound(p, Sound.ENTITY_VILLAGER_AMBIENT, 1, 1);
                counter++;
            }
        };
        activeDialogueTask.runTaskTimer(SurvivalSkills.getInstance(), 0, 40);
    }

    private void cancelActiveDialogue() {
        if (activeDialogueTask == null)
            return;
        activeDialogueTask.cancel();
        activeDialogueTask = null;
    }

    public void dialogueOpener(Player p) {
        // Initial quest intro
        List<String> messages = lines(
                "I can grant you great powers",
                "First you must bring me items that show your dedication to this world",
                "Farming is the foundation of any society",
                "Bring me crops to show me you can feed a civilization",
                "Bring me " + ChatColor.AQUA + "2,000 " + ChatColor.WHITE + "bread to start!");
        dialogue(p, messages);
        advancePhase(p, 1);
    }

    public void dialogueItemCount(Player p, String item, int count, int max) {
        List<String> messages = lines(
                "You have brought me " + ChatColor.AQUA + count + ChatColor.WHITE + " " + item,
                "You need to bring me " + ChatColor.AQUA + (max - count) + ChatColor.WHITE + " more " + item);
        dialogue(p, messages);
    }

    // -----------------------
    // Phase handlers (kept public API, reduced duplication with helpers)
    // -----------------------
    public void checkFarmingQuest(Player p, int cropType) {
        switch (cropType) {
            case 1 -> bulkStep(p, 2000, Material.BREAD, "bread",
                    lines("Excellent Work!", "Now bring me " + ChatColor.AQUA + "5,000 " + ChatColor.WHITE + "carrots"),
                    false);
            case 2 -> bulkStep(p, 5000, Material.CARROT, "carrots",
                    lines("Excellent Work!",
                            "Now bring me " + ChatColor.AQUA + "5,000 " + ChatColor.WHITE + "potatoes"),
                    false);
            case 3 -> bulkStep(p, 5000, Material.POTATO, "potatoes",
                    lines("Excellent Work!",
                            "Now bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "poisonous potatoes"),
                    false);
            case 4 -> bulkStep(p, 500, Material.POISONOUS_POTATO, "poisonous potatoes",
                    lines("Excellent Work!",
                            "Now bring me " + ChatColor.AQUA + "5,000 " + ChatColor.WHITE + "beetroots"),
                    false);
            case 5 -> bulkStep(p, 5000, Material.BEETROOT, "beetroots",
                    lines("Excellent Work!",
                            "Now bring me " + ChatColor.AQUA + "5,000 " + ChatColor.WHITE + "melon slices"),
                    false);
            case 6 -> bulkStep(p, 5000, Material.MELON_SLICE, "melon slices",
                    lines("Excellent Work!",
                            "Now bring me " + ChatColor.AQUA + "2,500 " + ChatColor.WHITE + "pumpkins"),
                    false);
            case 7 -> bulkStep(p, 2500, Material.PUMPKIN, "pumpkins",
                    lines("Excellent Work!",
                            "Now bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "sweet berries"),
                    false);
            case 8 -> bulkStep(p, 500, Material.SWEET_BERRIES, "sweet berries",
                    lines("Excellent Work!",
                            "Now bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "glow berries"),
                    false);
            case 9 -> bulkStep(p, 500, Material.GLOW_BERRIES, "glow berries",
                    lines("Excellent Work!", "Now bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "apples"),
                    false);
            case 10 -> bulkStep(p, 500, Material.APPLE, "apples",
                    lines("Excellent Work!",
                            "Now bring me " + ChatColor.AQUA + "64 " + ChatColor.WHITE + "chorus flowers"),
                    false);
            case 11 -> bulkStep(p, 64, Material.CHORUS_FLOWER, "chorus flowers",
                    lines("Excellent Work!", "Now bring me " + ChatColor.AQUA + "64 " + ChatColor.WHITE + "cakes"),
                    false);
            case 12 -> bulkStep(p, 64, Material.CAKE, "cake",
                    lines(
                            "You have proven your dedication to farming",
                            "Now you must bring me your riches",
                            "Bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "coal blocks"),
                    true);
        }
    }

    public void checkOreQuest(Player p, int oreType) {
        switch (oreType) {
            case 1 -> bulkStep(p, 500, Material.COAL_BLOCK, "coal blocks",
                    lines("Excellent Work!",
                            "Now bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "copper blocks"),
                    false);
            case 2 -> bulkStep(p, 500, Material.COPPER_BLOCK, "copper blocks",
                    lines("Excellent Work!",
                            "Now bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "iron blocks"),
                    false);
            case 3 -> bulkStep(p, 500, Material.IRON_BLOCK, "iron blocks",
                    lines("Excellent Work!",
                            "Now bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "lapis blocks"),
                    false);
            case 4 -> bulkStep(p, 500, Material.LAPIS_BLOCK, "lapis blocks",
                    lines("Excellent Work!",
                            "Now bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "redstone blocks"),
                    false);
            case 5 -> bulkStep(p, 500, Material.REDSTONE_BLOCK, "redstone blocks",
                    lines("Excellent Work!",
                            "Now bring me " + ChatColor.AQUA + "500 " + ChatColor.WHITE + "gold blocks"),
                    false);
            case 6 -> bulkStep(p, 500, Material.GOLD_BLOCK, "gold blocks",
                    lines("Excellent Work!",
                            "Now bring me " + ChatColor.AQUA + "200 " + ChatColor.WHITE + "diamond blocks"),
                    false);
            case 7 -> bulkStep(p, 200, Material.DIAMOND_BLOCK, "diamond blocks",
                    lines("Excellent Work!",
                            "Now bring me " + ChatColor.AQUA + "200 " + ChatColor.WHITE + "emerald blocks"),
                    false);
            case 8 -> bulkStep(p, 200, Material.EMERALD_BLOCK, "emerald blocks",
                    lines("Excellent Work!",
                            "Now bring me " + ChatColor.AQUA + "64 " + ChatColor.WHITE + "netherite blocks"),
                    false);
            case 9 -> bulkStep(p, 64, Material.NETHERITE_BLOCK, "netherite blocks",
                    lines(
                            "You truly do embody the concept of wealth",
                            "Show me the exotic trophies of the creatures of this world",
                            "You must detach yourself from the idea of life or death",
                            "Bring me a modified turtle helmet",
                            "You can see the recipe by using " + ChatColor.YELLOW + "/godquest"),
                    true);
        }
    }

    public void checkCreatureQuest(Player p, int itemType) {
        switch (itemType) {
            case 1 -> handInItemStackStep(p, ItemStackGenerator.getTurtleHelmet(), "Turtle Helmet",
                    lines("Excellent Work!", "Now bring me the music of the goats",
                            "You can see the recipe by using " + ChatColor.YELLOW + "/godquest"),
                    false);
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
                            "You can see the recipe by using " + ChatColor.YELLOW + "/godquest"),
                    true);
        }
    }

    public void checkKnowledgeQuest(Player p, int itemType) {
        switch (itemType) {
            case 1 -> handInItemStackStep(p, ItemStackGenerator.getMusicKnowledgeDisc(), "Music Knowledge Disc",
                    lines("Excellent Work!", "Demonstrate your brewing knowledge by bringing me every potion",
                            "Start with a potion of swiftness"),
                    false);
            case 2 -> potionStep(p, PotionType.SWIFTNESS, "Potion of Swiftness",
                    lines("Excellent Work!", "Now bring me a potion of fire resistance"), false);
            case 3 -> potionStep(p, PotionType.FIRE_RESISTANCE, "Potion of Fire Resistance",
                    lines("Excellent Work!", "Now bring me a potion of healing"), false);
            case 4 -> potionStep(p, PotionType.HEALING, "Potion of Healing",
                    lines("Excellent Work!", "Now bring me a potion of harming"), false);
            case 5 -> potionStep(p, PotionType.HARMING, "Potion of Harming",
                    lines("Excellent Work!", "Now bring me a potion of water breathing"), false);
            case 6 -> potionStep(p, PotionType.WATER_BREATHING, "Potion of Water Breathing",
                    lines("Excellent Work!", "Now bring me a potion of night vision"), false);
            case 7 -> potionStep(p, PotionType.NIGHT_VISION, "Potion of Night Vision",
                    lines("Excellent Work!", "Now bring me a potion of invisibility"), false);
            case 8 -> potionStep(p, PotionType.INVISIBILITY, "Potion of Invisibility",
                    lines("Excellent Work!", "Now bring me a potion of leaping"), false);
            case 9 -> potionStep(p, PotionType.LEAPING, "Potion of Leaping",
                    lines("Excellent Work!", "Now bring me a potion of slow falling"), false);
            case 10 -> potionStep(p, PotionType.SLOW_FALLING, "Potion of Slow Falling",
                    lines("Excellent Work!", "Now bring me a potion of strength"), false);
            case 11 -> potionStep(p, PotionType.STRENGTH, "Potion of Strength",
                    lines("Excellent Work!", "Now bring me a potion of weakness"), false);
            case 12 -> potionStep(p, PotionType.WEAKNESS, "Potion of Weakness",
                    lines("Excellent Work!", "Now bring me a potion of regeneration"), false);
            case 13 -> potionStep(p, PotionType.REGENERATION, "Potion of Regeneration",
                    lines("Excellent Work!", "Now bring me a potion of poison"), false);
            case 14 -> potionStep(p, PotionType.POISON, "Potion of Poison",
                    lines("Excellent Work!", "Now bring me a potion of infestation"), false);
            case 15 -> potionStep(p, PotionType.INFESTED, "Potion of Infestation",
                    lines("Excellent Work!", "Now bring me a potion of oozing"), false);
            case 16 -> potionStep(p, PotionType.OOZING, "Potion of Oozing",
                    lines("Excellent Work!", "Now bring me a potion of weaving"), false);
            case 17 -> potionStep(p, PotionType.WEAVING, "Potion of Weaving",
                    lines("Excellent Work!", "Now bring me a potion of wind charged"), false);
            case 18 -> potionStep(p, PotionType.WIND_CHARGED, "Potion of Wind Charged",
                    lines(
                            "Thank you for demonstrating your knowledge",
                            "Now show me your refined taste in relics",
                            "Bring me something that exhibits refined pottery skills",
                            "You can see the recipe by using " + ChatColor.YELLOW + "/godquest"),
                    true);
        }
    }

    public void checkRelicQuest(Player p, int itemType) {
        switch (itemType) {
            case 1 -> handInItemStackStep(p, ItemStackGenerator.getSherdRelic(), "Sherd Relic",
                    lines("Excellent Work!",
                            "Demonstrate your ancient fashion by bringing me the culmination of armor fashion",
                            "You can see the recipe by using " + ChatColor.YELLOW + "/godquest"),
                    false);
            case 2 -> handInItemStackStep(p, ItemStackGenerator.getTrimRelic(), "Trim Relic",
                    lines("Your appreciation of times forgotten is noticed",
                            "Show me that you have truly connected with the people of this land",
                            "Trade with villagers 1,000 times"),
                    true);
        }
    }

    public void checkVillagerTradingQuest(Player p) {
        if (currentItemCount < 1000) {
            dialogue(p, List.of(
                    "You have traded with villagers " + ChatColor.AQUA + currentItemCount + ChatColor.WHITE + " times",
                    "You need to trade with villagers " + ChatColor.AQUA + (1000 - currentItemCount)
                            + ChatColor.WHITE + " more times"));
        } else {
            dialogue(p, List.of(
                    "The villagers clearly trust in you skills as a merchant",
                    "We are nearing the end of my tribulations",
                    "Soon you will need to prove yourself in a combat trial",
                    "Bring me some powerful gear to show me you know what it means to fight",
                    "You can see the recipe by using " + ChatColor.YELLOW + "/godquest"));
            currentItemCount = 0;
            advancePhase(p, 1);
            updateGodTrophyParticles();
        }
    }

    public void checkCombatQuest(Player p) {
        handInItemStackStep(p, ItemStackGenerator.getWarriorEmblem(), "Warrior Emblem",
                lines("Excellent Work!",
                        "You may have noticed the mobs of this world sometimes drop rare items",
                        "Bring me the item from a " + ChatColor.AQUA + "spider"),
                true);
    }

    public void checkMobItemQuest(Player p, int itemType) {
        switch (itemType) {
            case 1 -> handInItemStackStep(p, ItemStackGenerator.getWebShooter(), "Web Shooter",
                    lines("Excellent Work!", "Bring me the item from a " + ChatColor.AQUA + "skeleton"), false);
            case 2 -> handInItemStackStep(p, ItemStackGenerator.getUnlimitedTippedArrow(), "Unlimited Tipped Arrow",
                    lines("Excellent Work!", "Bring me the item from a " + ChatColor.AQUA + "zombie"), false);
            case 3 ->
                handInItemStackStep(p, ItemStackGenerator.getVillagerRevivalArtifact(), "Villager Revival Artifact",
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
            case 9 -> {
                if (allAdvancements) {
                    handInItemStackStep(p, ItemStackGenerator.getDragonBreathCannon(), "Dragon Breath Cannon",
                            lines("I now have several interesting mob drops",
                                    "However you must not forget we are playing a game!",
                                    "The final task is to complete all Minecraft advancements"),
                            true);
                } else {
                    handInItemStackStep(p, ItemStackGenerator.getDragonBreathCannon(), "Dragon Breath Cannon",
                            getGodTrialCompletionMessage(), true, 2);
                }
            }
        }
    }

    /**
     * Check to see if a player has achieved all Minecraft advancements
     * 
     * @param p
     */
    public void checkAdvancementsQuest(Player p) {
        if (hasAllAdvancements(p) || !allAdvancements) {
            onBulkComplete(p, getGodTrialCompletionMessage(), true);
        } else {
            dialogue(p, lines("You have not yet achieved all advancements.",
                    "Keep working hard and you will get there!"));
        }
    }

    private static List<String> getGodTrialCompletionMessage() {
        return List.of(
                "Excellent Work!",
                "You have completed all of my tasks",
                "Finally you must prove yourself in the " + ChatColor.RED + "Trial of the Gods " + ChatColor.WHITE
                        + "by defeating the god difficulty",
                "Do this and you will be given " + ChatColor.AQUA + "creative mode " + ChatColor.WHITE +
                        "using the power of the gods!",
                "You can start the trial at anytime, anywhere, by using " + ChatColor.YELLOW + "/godtrial");
    }

    public int removeMaterialsFromInventory(int currentProgress, int max, Inventory inv, Material mat) {
        int total = 0;
        for (ItemStack item : inv.getContents()) {
            if (currentProgress + total == max)
                return total;

            // Ensure the item exists and is not a custom item
            if (item == null)
                continue;
            if (!item.getType().equals(mat))
                continue;
            if (item.getItemMeta() != null && ItemStackGeneratorUtils.hasCustomModelData(item.getItemMeta()))
                continue;

            // Handle the item in the inventory
            total += item.getAmount();
            if (currentProgress + total > max) {
                // This stack puts us over the limit; only take what is needed
                int neededFromStack = max - currentProgress - (total - item.getAmount());
                item.setAmount(item.getAmount() - neededFromStack);
                if (item.getAmount() == 0)
                    inv.remove(item);
                return total;
            }
            item.setAmount(0);
            inv.remove(item);
        }
        return total;
    }

    public void removeItemFromMainHand(Player p) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType().isAir())
            return;
        hand.setAmount(hand.getAmount() - 1);
    }

    public void updateGodTrophyParticles() {
        updateGodTrophyParticles(true);
    }

    public void synchronizeGodTrophy() {
        updateGodTrophyParticles(false);
    }

    private void updateGodTrophyParticles(boolean playProgressBurst) {
        for (Map.Entry<Location, Trophy> trophy : SurvivalSkills.getInstance().getTrophyManager().getTrophies()
                .entrySet()) {
            if (!trophy.getValue().getType().equalsIgnoreCase("godtrophy"))
                continue;
            if (!trophy.getValue().getUuid().equals(uuid))
                continue;
            Optional<TrophyEffects> effects = trophy.getValue().getEffects();
            if (effects.isEmpty() || effects.get().getGodTrophy() == null)
                continue;
            GodTrophyEffects godTrophyEffects = effects.get().getGodTrophy();
            synchronizeGodTrophyEffects(godTrophyEffects);
            if (playProgressBurst)
                godTrophyEffects.playProgressBurst();
            return;
        }
    }

    public void synchronizeGodTrophyEffects(GodTrophyEffects godTrophyEffects) {
        Objects.requireNonNull(godTrophyEffects);
        int completedGroups = countCompletedGroups(phase);
        godTrophyEffects.setAuraColor(GOD_TROPHY_AURA_COLORS.get(completedGroups));
        godTrophyEffects.updateNpcPersonality(completedGroups);
    }

    static int countCompletedGroups(int phase) {
        return (int) GOD_QUEST_GROUPS.stream()
                .filter((PhaseGroup group) -> phase > group.end)
                .count();
    }

    static int completionPercentage(int phase) {
        return (int) Math.round(countCompletedGroups(phase) * 100.0 / GOD_QUEST_GROUPS.size());
    }

    static OptionalInt completedPercentageForTransition(int previousPhase, int currentPhase) {
        if (countCompletedGroups(currentPhase) <= countCompletedGroups(previousPhase))
            return OptionalInt.empty();
        return OptionalInt.of(completionPercentage(currentPhase));
    }

    public void setCurrentItemCount(int count) {
        this.currentItemCount = count;
    }

    public ItemStack getPotion(PotionType type) {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta == null)
            return potion;
        meta.setBasePotionType(type);
        potion.setItemMeta(meta);
        return potion;
    }

    public boolean hasAllAdvancements(Player p) {
        Iterator<Advancement> advancementIterator = Bukkit.advancementIterator();

        while (advancementIterator.hasNext()) {
            Advancement advancement = advancementIterator.next();
            // Skip recipe unlocks — they auto-complete and don't count as
            // visible advancements for the player
            if (advancement.getKey().getKey().startsWith("recipes/"))
                continue;
            if (!p.getAdvancementProgress(advancement).isDone())
                return false;
        }

        return true;
    }

    public int getCurrentItemCount() {
        return currentItemCount;
    }

    public Optional<QuestProgress> getProgress() {
        return progressForPhase(phase, currentItemCount, allAdvancements);
    }

    static Optional<QuestProgress> progressForPhase(int phase, int currentItemCount, boolean allAdvancements) {
        if (phase < 0 || phase >= QUEST_STEPS.size())
            return Optional.empty();

        QuestStep questStep = QUEST_STEPS.get(phase);
        String nextStep = phase == PhaseGroup.MOB_ITEM.end && !allAdvancements
                ? "Trial of the Gods"
                : questStep.nextStep();
        int safeCurrentCount = Math.clamp(currentItemCount, 0, questStep.goal());
        return Optional.of(new QuestProgress(questStep.currentStep(), safeCurrentCount, questStep.goal(), nextStep));
    }

    public boolean isVillagerTradingPhase() {
        return PHASE_TO_GROUP.get(phase) == PhaseGroup.VILLAGER;
    }

    public int getPhase() {
        return phase;
    }

    public Optional<Integer> getStage() {
        PhaseGroup group = PHASE_TO_GROUP.get(phase);
        if (group != null && group.stage > 0)
            return Optional.of(group.stage);
        return Optional.empty();
    }

    public int getMaxPhase() {
        return MAX_PHASE;
    }

    // -----------------------
    // Private helpers to reduce duplication
    // -----------------------

    /** Plays the standard success sound for completing a quest step. */
    private void playSuccessSound(Player p) {
        p.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        p.playSound(p, Sound.ENTITY_VILLAGER_CELEBRATE, 1, 1);
    }

    /**
     * Sends the "wrong item" dialogue and returns true
     * so callers can {@code return wrongItemDialogue(p, name)}.
     */
    private boolean wrongItemDialogue(Player p, String itemName) {
        p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        dialogue(p, lines(
                "You do not have the right item",
                "Use " + ChatColor.YELLOW + "/godquest" + ChatColor.WHITE + " to see the recipe for " + ChatColor.AQUA + itemName
        ));
        return true;
    }

    /**
     * Sends the "empty hand" dialogue (with recipe hint) and returns true
     * so callers can {@code return emptyHandItemStackDialogue(p, name)}.
     */
    private boolean emptyHandItemStackDialogue(Player p, String itemName) {
        p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        dialogue(p, lines(
                "You are not holding anything",
                "Use " + ChatColor.YELLOW + "/godquest" + ChatColor.WHITE + " to see the recipe for " + ChatColor.AQUA + itemName
        ));
        return true;
    }

    /** Small utility for building dialogue lists inline. */
    private static List<String> lines(String... text) {
        return List.of(text);
    }

    /**
     * Completes a bulk-collection step: resets count, sends dialogue, advances
     * phase, and optionally triggers trophy particle updates.
     */
    private void onBulkComplete(Player p, List<String> messages, boolean updateParticles) {
        currentItemCount = 0;
        dialogue(p, messages);
        advancePhase(p, 1);
        if (updateParticles)
            updateGodTrophyParticles();
    }

    /**
     * Completes a hand-in step: removes one from main hand, resets count, sends
     * dialogue, advances phase, and optionally triggers trophy particle updates.
     */
    private void onHandInComplete(Player p, List<String> messages, boolean updateParticles) {
        onHandInComplete(p, messages, updateParticles, 1);
    }

    private void onHandInComplete(Player p, List<String> messages, boolean updateParticles, int phasesToAdvance) {
        removeItemFromMainHand(p);
        currentItemCount = 0;
        dialogue(p, messages);
        advancePhase(p, phasesToAdvance);
        if (updateParticles)
            updateGodTrophyParticles();
    }

    /**
     * Bulk material collection step handler (e.g., farming/ore). When completed,
     * resets count and advances phase.
     */
    private void bulkStep(Player p, int required, Material mat, String itemName, List<String> successMessages,
            boolean updateParticles) {
        if (handleItemCheck(required, p, mat, itemName))
            return; // still collecting
        onBulkComplete(p, successMessages, updateParticles);
    }

    /** Hand-in step for exact ItemStack in main hand. */
    private void handInItemStackStep(Player p, ItemStack expected, String itemName, List<String> successMessages,
            boolean updateParticles) {
        handInItemStackStep(p, expected, itemName, successMessages, updateParticles, 1);
    }

    private void handInItemStackStep(Player p, ItemStack expected, String itemName, List<String> successMessages,
            boolean updateParticles, int phasesToAdvance) {
        if (handleItemCheck(p, expected, itemName))
            return; // wrong or missing item
        onHandInComplete(p, successMessages, updateParticles, phasesToAdvance);
    }

    /** Hand-in step for a specific Material in main hand. */
    private void handInMaterialStep(Player p, Material mat, String itemName, List<String> successMessages,
            boolean updateParticles) {
        if (handleItemCheck(p, mat, itemName))
            return; // wrong or missing item
        onHandInComplete(p, successMessages, updateParticles);
    }

    /** Potion hand-in convenience wrapper. */
    private void potionStep(Player p, PotionType type, String friendlyName, List<String> successMessages,
            boolean updateParticles) {
        if (handlePotionCheck(p, type, friendlyName))
            return;
        onHandInComplete(p, successMessages, updateParticles);
    }

    /** Checks a potion hand-in by base effect while accepting every potion container variant. */
    private boolean handlePotionCheck(Player p, PotionType requiredType, String itemName) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType().isAir())
            return emptyHandItemStackDialogue(p, itemName);

        if (!isPotionContainer(hand)
                || !(hand.getItemMeta() instanceof PotionMeta meta)
                || !meta.hasBasePotionType()
                || !hasSamePotionEffect(meta.getBasePotionType(), requiredType))
            return wrongItemDialogue(p, itemName);

        playSuccessSound(p);
        return false;
    }

    private static boolean isPotionContainer(ItemStack item) {
        return switch (item.getType()) {
            case POTION, SPLASH_POTION, LINGERING_POTION -> true;
            default -> false;
        };
    }

    private static boolean hasSamePotionEffect(PotionType heldType, PotionType requiredType) {
        if (heldType == null || requiredType == null)
            return false;

        List<PotionEffectType> heldEffects = heldType.getPotionEffects().stream()
                .map((PotionEffect effect) -> effect.getType())
                .toList();
        List<PotionEffectType> requiredEffects = requiredType.getPotionEffects().stream()
                .map((PotionEffect effect) -> effect.getType())
                .toList();
        return Objects.equals(heldEffects, requiredEffects);
    }

    private static QuestStep step(String currentStep, int goal, String nextStep) {
        return new QuestStep(currentStep, goal, nextStep);
    }

    private void advancePhase(Player p, int phasesToAdvance) {
        int previousPhase = phase;
        phase += phasesToAdvance;
        completedPercentageForTransition(previousPhase, phase).ifPresent((int percentage) ->
                Bukkit.broadcastMessage(ChatColor.AQUA + p.getName() + ChatColor.WHITE + " has completed "
                        + ChatColor.GOLD + percentage + "%" + ChatColor.WHITE + " of the God Quest!"));
    }
}
