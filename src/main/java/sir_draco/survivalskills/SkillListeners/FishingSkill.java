package sir_draco.survivalskills.SkillListeners;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Drowned;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.Abilities.AutoTrash;
import sir_draco.survivalskills.Bosses.ProjectileCalculator;
import sir_draco.survivalskills.ItemStackGenerator;
import sir_draco.survivalskills.Rewards.PlayerRewards;
import sir_draco.survivalskills.Skills.Skill;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class FishingSkill implements Listener {

    private final SurvivalSkills plugin;
    private final ArrayList<Player> waterBreathers = new ArrayList<>();
    private final ArrayList<Player> openTrashInventories = new ArrayList<>();
    private final HashMap<Player, Integer> rainFishers = new HashMap<>();
    private final HashMap<Player, Integer> nonStackableItems = new HashMap<>();
    private final HashMap<Player, AutoTrash> trashInventories = new HashMap<>();
    private final HashMap<Player, AutoTrash> permaTrash = new HashMap<>();

    private final ArrayList<Material> commonLootTable = new ArrayList<>();
    private final ArrayList<Material> rareLootTable = new ArrayList<>();
    private final HashMap<Enchantment, Integer> rareEnchantments = new HashMap<>();
    private final ArrayList<Material> epicLootTable = new ArrayList<>();
    private final HashMap<Enchantment, Integer> epicEnchantments = new HashMap<>();
    private final ArrayList<Material> legendaryLootTable = new ArrayList<>();
    private final HashMap<Enchantment, Integer> legendaryEnchantments = new HashMap<>();

    private int id = 1;

    public FishingSkill(SurvivalSkills plugin) {
        this.plugin = plugin;
        createCommonLootTable();
        createRareLootTable();
        createEpicLootTable();
        createLegendaryLootTable();
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent e) {
        if (e.getState() == PlayerFishEvent.State.REEL_IN || e.getState() == PlayerFishEvent.State.FAILED_ATTEMPT)
            rainFishers.remove(e.getPlayer());

        if (e.getState() == PlayerFishEvent.State.FISHING) {
            // Get player fishing lure level
            FishHook hook = e.getHook();
            Player p = e.getPlayer();
            int lureLevel = 0;
            ItemStack rod = p.getInventory().getItemInMainHand();
            if (rod.containsEnchantment(Enchantment.LURE)) lureLevel = Math.min(3, rod.getEnchantmentLevel(Enchantment.LURE));

            // Get fishing skill speed level
            int minSpeed = plugin.getSkillManager().getPlayerRewards(p).getFishingMinTickSpeed();
            int maxSpeed = plugin.getSkillManager().getPlayerRewards(p).getFishingMaxTickSpeed();
            minSpeed = Math.min(Math.max(1, minSpeed - (lureLevel * 10)), 79);
            maxSpeed = Math.max(minSpeed + 5, maxSpeed - (lureLevel * 20));

            if (!hook.isSkyInfluenced()) {
                minSpeed = minSpeed * 2;
                maxSpeed = maxSpeed * 2;
            }
            else if (hook.getWorld().hasStorm()) {
                int pID = id;
                id++;
                rainFishers.put(p, pID);
                int speed = Math.max(10, minSpeed + 10);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!hook.getLocation().getBlock().getType().equals(Material.WATER)) return;
                        if (!rainFishers.containsKey(p) || rainFishers.get(p) != pID) {
                            hook.remove();
                            return;
                        }
                        // Check if the player is using a fishing rod with luck of the sea
                        int luckLevel = 0;
                        if (rod.containsEnchantment(Enchantment.LUCK_OF_THE_SEA)) luckLevel = rod.getEnchantmentLevel(Enchantment.LUCK_OF_THE_SEA);
                        ArrayList<ItemStack> items = getItemsToDrop(p, luckLevel);

                        // Get the velocity of the entity that was caught and then remove the entity
                        Location loc = e.getHook().getLocation();
                        World world = e.getHook().getWorld();
                        Vector velocity = ProjectileCalculator.getVector(loc, p.getLocation(), 0.5);

                        // Set the velocity of all the items in the list to the velocity of the entity that was caught
                        if (!items.isEmpty()) for (ItemStack item : items) world.dropItem(loc, item).setVelocity(velocity);

                        handleFishingExperience(p);
                        handleDurability(rod);
                        Skill.experienceEvent(plugin, p, plugin.getSkillManager().getFishingXP(), "Fishing");
                        hook.remove();
                        rainFishers.remove(p);
                    }
                }.runTaskLater(plugin, speed);
                return;
            }

            hook.setMinLureTime(minSpeed);
            hook.setMaxLureTime(maxSpeed);
            return;
        }

        if (e.getState() != PlayerFishEvent.State.CAUGHT_FISH) return;
        if (e.getCaught() == null) return;
        // Get the list of items to drop
        Player p = e.getPlayer();

        // Check if the player is above fighting level 50 and try to spawn fishing boss
        if (plugin.getSkillManager().getPlayerRewards(p).getReward("Fighting", "FishingKing").isApplied()) {
            double chance = Math.random();

            if (!plugin.getTrophyManager().getTrophyTracker().get(p.getUniqueId()).get("FishingTrophy")) {
                if (chance <= 0.004) {
                    Location loc = e.getHook().getLocation();
                    World world = e.getHook().getWorld();
                    Vector velocity = ProjectileCalculator.getVector(loc, p.getLocation(), 1);
                    e.getCaught().remove();

                    // Spawn fishing boss
                    spawnFishingBoss(world, loc, velocity);
                    return;
                }
            }
            else {
                if (chance <= 0.01) {
                    Location loc = e.getHook().getLocation();
                    World world = e.getHook().getWorld();
                    Vector velocity = ProjectileCalculator.getVector(loc, p.getLocation(), 1);
                    e.getCaught().remove();

                    // Spawn fishing boss
                    spawnFishingBoss(world, loc, velocity);
                    return;
                }
            }
        }

        // Check if the player is using a fishing rod with luck of the sea
        ItemStack rod = p.getInventory().getItemInMainHand();
        int luckLevel = 0;
        if (rod.containsEnchantment(Enchantment.LUCK_OF_THE_SEA)) luckLevel = rod.getEnchantmentLevel(Enchantment.LUCK_OF_THE_SEA);
        ArrayList<ItemStack> items = getItemsToDrop(p, luckLevel);

        // Get the velocity of the entity that was caught and then remove the entity
        Location loc = e.getHook().getLocation();
        World world = e.getHook().getWorld();
        Vector velocity = ProjectileCalculator.getVector(loc, p.getLocation(), 0.5);
        e.getCaught().remove();

        // Set the velocity of all the items in the list to the velocity of the entity that was caught
        if (!items.isEmpty()) for (ItemStack item : items) world.dropItem(loc, item).setVelocity(velocity);

        Skill.experienceEvent(plugin, p, plugin.getSkillManager().getFishingXP(), "Fishing");
    }

    @EventHandler
    public void experienceEvent(PlayerExpChangeEvent e) {
        Player p = e.getPlayer();
        double xpMultiplier = plugin.getSkillManager().getPlayerRewards(p).getExperienceMultiplier();
        e.setAmount((int) (e.getAmount() * xpMultiplier));
    }

    @EventHandler
    public void playerMoveInWater(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!p.isSwimming() && !p.isInWater()) return;
        if (plugin.getSkillManager().getPlayerRewards(p).getReward("Fishing", "WaterBreathingIII").isApplied()) {
            p.setRemainingAir(300);
            return;
        }
        if (waterBreathers.contains(p)) p.setRemainingAir(300);
    }

    @EventHandler
    public void killFishingBoss(EntityDeathEvent e) {
        if (!e.getEntity().hasMetadata("fishingboss")) return;
        e.getDrops().clear();
        e.setDroppedExp(0);
        e.getEntity().getWorld().dropItemNaturally(e.getEntity().getLocation(), ItemStackGenerator.getFishingBossItem());
    }

    @EventHandler
    public void rainEvent(WeatherChangeEvent e) {
        if (e.getWorld().hasStorm()) return;
        Bukkit.broadcastMessage(ChatColor.YELLOW + "Rain has started falling. Fishing speeds greatly increased!");
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p, Sound.ENTITY_FISHING_BOBBER_SPLASH, 1, 1);
        }
    }

    @EventHandler
    public void onTrashClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        if (!openTrashInventories.contains(p)) return;
        openTrashInventories.remove(p);
    }

    @EventHandler
    public void onClickTrashInventory(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (!openTrashInventories.contains(p)) return;
        if (!trashInventories.containsKey(p) && !permaTrash.containsKey(p)) {
            openTrashInventories.remove(p);
            return;
        }

        AutoTrash trash = null;
        if (trashInventories.containsKey(p)) {
            AutoTrash check = trashInventories.get(p);
            if (e.getClick().isShiftClick()) {
                // get the upper inventory of the player
                Inventory top = p.getOpenInventory().getTopInventory();
                if (Objects.equals(top, check.getTrashInventory())) trash = check;
            }
            else if (Objects.equals(e.getInventory(), check.getTrashInventory())) trash = check;
        }
        if (permaTrash.containsKey(p) && trash == null) {
            AutoTrash check = permaTrash.get(p);
            if (Objects.equals(e.getInventory(), check.getTrashInventory())) trash = check;
        }
        if (trash == null) return;

        if (e.getClick().equals(ClickType.DOUBLE_CLICK)) {
            e.setCancelled(true);
            return;
        }

        if (e.getRawSlot() < 0) return;
        if (e.getRawSlot() >= trash.getTrashInventory().getSize() && !e.isShiftClick()) return;

        if (e.isShiftClick()) {
            if (e.getCurrentItem() == null) {
                e.setCancelled(true);
                return;
            }

            if (e.getRawSlot() >= trash.getTrashInventory().getSize()) {
                ItemStack item = e.getCurrentItem();
                if (item == null) return;
                e.setCancelled(true);
                int slot = trash.findOpenSlot();
                if (slot == -1) return;
                trash.addTrashItem(item, slot);
            }
            else {
                e.setCancelled(true);
                trash.removeTrashItem(e.getCurrentItem(), e.getRawSlot());
            }
        }
        else {
            if (e.getCurrentItem() != null) {
                e.setCancelled(true);
                trash.removeTrashItem(e.getCurrentItem(), e.getRawSlot());
            }
            else {
                ItemStack item = e.getCursor();
                if (item == null) return;
                e.setCancelled(true);
                trash.addTrashItem(item, e.getRawSlot());
            }
        }
    }

    @EventHandler
    public void onDragTrashInventory(InventoryDragEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (!openTrashInventories.contains(p)) return;
        if (!trashInventories.containsKey(p) && !permaTrash.containsKey(p)) {
            openTrashInventories.remove(p);
            return;
        }

        AutoTrash trash = null;
        if (trashInventories.containsKey(p)) {
            AutoTrash check = trashInventories.get(p);
            if (Objects.equals(e.getInventory(), check.getTrashInventory())) trash = check;
        }
        if (permaTrash.containsKey(p) && trash == null) {
            AutoTrash check = permaTrash.get(p);
            if (Objects.equals(e.getInventory(), check.getTrashInventory())) trash = check;
        }
        if (trash == null) return;
        e.setCancelled(true);

        int slot = e.getRawSlots().iterator().next();
        if (slot >= trash.getTrashInventory().getSize() || slot < 0) return;
        if (trash.getTrashInventory().getItem(slot) != null && e.getCursor() != null) trash.removeTrashItem(e.getCursor(), slot);
        else {
            ItemStack item = e.getOldCursor();
            if (item.getType().isAir()) return;
            trash.addTrashItem(item, slot);
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        if (!trashInventories.containsKey(p) && !permaTrash.containsKey(p)) return;

        ItemStack item = e.getItem().getItemStack();
        if (trashInventories.containsKey(p)) {
            AutoTrash trash = trashInventories.get(p);
            if (trash == null) return;
            if (item.getType().equals(Material.ENCHANTED_BOOK)) {
                ItemMeta meta = item.getItemMeta();
                if (meta == null) return;
                if (!(meta instanceof EnchantmentStorageMeta)) return;
                EnchantmentStorageMeta enchantMeta = (EnchantmentStorageMeta) meta;
                if (enchantMeta.getStoredEnchants().isEmpty()) return;
                Enchantment enchant = enchantMeta.getStoredEnchants().keySet().iterator().next();

                if (!trash.getEnchants().isEmpty() && trash.getEnchants().contains(enchant)) {
                    e.setCancelled(true);
                    e.getItem().remove();
                    return;
                }
            }
            else if (trash.getTrashMaterials().contains(item.getType())) {
                e.setCancelled(true);
                e.getItem().remove();
                return;
            }
        }
        if (permaTrash.containsKey(p)) {
            AutoTrash trash = permaTrash.get(p);
            if (item.getType().equals(Material.ENCHANTED_BOOK)) {
                ItemMeta meta = item.getItemMeta();
                if (meta == null) return;
                if (!(meta instanceof EnchantmentStorageMeta)) return;
                EnchantmentStorageMeta enchantMeta = (EnchantmentStorageMeta) meta;
                if (enchantMeta.getStoredEnchants().isEmpty()) return;
                Enchantment enchant = enchantMeta.getStoredEnchants().keySet().iterator().next();

                if (trash.getEnchants().isEmpty()) return;
                if (!trash.getEnchants().contains(enchant)) return;
                e.setCancelled(true);
                e.getItem().remove();
                return;
            }
            if (!trash.getTrashMaterials().contains(item.getType())) return;
            e.setCancelled(true);
            e.getItem().remove();
        }
    }

    public void createCommonLootTable() {
        commonLootTable.add(Material.BAMBOO);
        commonLootTable.add(Material.BONE);
        commonLootTable.add(Material.BOOK);
        commonLootTable.add(Material.BOWL);
        commonLootTable.add(Material.COAL);
        commonLootTable.add(Material.COD);
        commonLootTable.add(Material.INK_SAC);
        commonLootTable.add(Material.LEATHER);
        commonLootTable.add(Material.PUFFERFISH);
        commonLootTable.add(Material.ROTTEN_FLESH);
        commonLootTable.add(Material.SALMON);
        commonLootTable.add(Material.STICK);
        commonLootTable.add(Material.STRING);
        commonLootTable.add(Material.TROPICAL_FISH);
        commonLootTable.add(Material.GLASS_BOTTLE);
        commonLootTable.add(Material.DIRT);
    }

    public void createRareLootTable() {
        rareLootTable.add(Material.ENCHANTED_BOOK);
        rareLootTable.add(Material.ENCHANTED_BOOK);
        rareLootTable.add(Material.ENDER_PEARL);
        rareLootTable.add(Material.EXPERIENCE_BOTTLE);
        rareLootTable.add(Material.GOLD_INGOT);
        rareLootTable.add(Material.IRON_INGOT);
        rareLootTable.add(Material.LAPIS_LAZULI);
        rareLootTable.add(Material.LEATHER_HORSE_ARMOR);
        rareLootTable.add(Material.LILY_PAD);
        rareLootTable.add(Material.NAME_TAG);
        rareLootTable.add(Material.REDSTONE);
        rareLootTable.add(Material.SADDLE);
        rareLootTable.add(Material.SLIME_BALL);
        rareLootTable.add(Material.TNT);

        rareEnchantments.put(Enchantment.POWER, 2);
        rareEnchantments.put(Enchantment.PUNCH, 1);
        rareEnchantments.put(Enchantment.SHARPNESS, 2);
        rareEnchantments.put(Enchantment.BANE_OF_ARTHROPODS, 2);
        rareEnchantments.put(Enchantment.SMITE, 2);
        rareEnchantments.put(Enchantment.DEPTH_STRIDER, 1);
        rareEnchantments.put(Enchantment.EFFICIENCY, 2);
        rareEnchantments.put(Enchantment.UNBREAKING, 1);
        rareEnchantments.put(Enchantment.FROST_WALKER, 1);
        rareEnchantments.put(Enchantment.IMPALING, 2);
        rareEnchantments.put(Enchantment.KNOCKBACK, 1);
        rareEnchantments.put(Enchantment.LUCK_OF_THE_SEA, 1);
        rareEnchantments.put(Enchantment.LURE, 1);
        rareEnchantments.put(Enchantment.RESPIRATION, 1);
        rareEnchantments.put(Enchantment.PIERCING, 1);
        rareEnchantments.put(Enchantment.PROTECTION, 2);
        rareEnchantments.put(Enchantment.BLAST_PROTECTION, 2);
        rareEnchantments.put(Enchantment.FEATHER_FALLING, 2);
        rareEnchantments.put(Enchantment.FIRE_PROTECTION, 2);
        rareEnchantments.put(Enchantment.PROJECTILE_PROTECTION, 2);
        rareEnchantments.put(Enchantment.QUICK_CHARGE, 1);
        rareEnchantments.put(Enchantment.RIPTIDE, 1);
        rareEnchantments.put(Enchantment.SOUL_SPEED, 1);
        rareEnchantments.put(Enchantment.SWEEPING_EDGE, 1);
    }

    public void createEpicLootTable() {
        epicLootTable.add(Material.DIAMOND);
        epicLootTable.add(Material.EMERALD);
        epicLootTable.add(Material.ENCHANTED_BOOK);
        epicLootTable.add(Material.ENCHANTED_BOOK);
        epicLootTable.add(Material.EXPERIENCE_BOTTLE);
        epicLootTable.add(Material.GHAST_TEAR);
        epicLootTable.add(Material.GLOWSTONE_DUST);
        epicLootTable.add(Material.GOLDEN_APPLE);
        epicLootTable.add(Material.GOLDEN_HORSE_ARMOR);
        epicLootTable.add(Material.GUNPOWDER);
        epicLootTable.add(Material.HEART_OF_THE_SEA);
        epicLootTable.add(Material.IRON_HORSE_ARMOR);
        epicLootTable.add(Material.NAUTILUS_SHELL);
        epicLootTable.add(Material.QUARTZ);
        epicLootTable.add(Material.EXPERIENCE_BOTTLE);

        epicEnchantments.put(Enchantment.POWER, 5);
        epicEnchantments.put(Enchantment.FLAME, 1);
        epicEnchantments.put(Enchantment.PUNCH, 2);
        epicEnchantments.put(Enchantment.CHANNELING, 1);
        epicEnchantments.put(Enchantment.SHARPNESS, 4);
        epicEnchantments.put(Enchantment.BANE_OF_ARTHROPODS, 5);
        epicEnchantments.put(Enchantment.SMITE, 5);
        epicEnchantments.put(Enchantment.DEPTH_STRIDER, 3);
        epicEnchantments.put(Enchantment.EFFICIENCY, 5);
        epicEnchantments.put(Enchantment.UNBREAKING, 3);
        epicEnchantments.put(Enchantment.FIRE_ASPECT, 2);
        epicEnchantments.put(Enchantment.FROST_WALKER, 2);
        epicEnchantments.put(Enchantment.IMPALING, 5);
        epicEnchantments.put(Enchantment.KNOCKBACK, 2);
        epicEnchantments.put(Enchantment.FORTUNE, 3);
        epicEnchantments.put(Enchantment.LOOTING, 3);
        epicEnchantments.put(Enchantment.LOYALTY, 3);
        epicEnchantments.put(Enchantment.LUCK_OF_THE_SEA, 3);
        epicEnchantments.put(Enchantment.LURE, 3);
        epicEnchantments.put(Enchantment.MULTISHOT, 1);
        epicEnchantments.put(Enchantment.RESPIRATION, 3);
        epicEnchantments.put(Enchantment.PIERCING, 4);
        epicEnchantments.put(Enchantment.PROTECTION, 4);
        epicEnchantments.put(Enchantment.BLAST_PROTECTION, 4);
        epicEnchantments.put(Enchantment.FEATHER_FALLING, 4);
        epicEnchantments.put(Enchantment.FIRE_PROTECTION, 4);
        epicEnchantments.put(Enchantment.PROJECTILE_PROTECTION, 4);
        epicEnchantments.put(Enchantment.QUICK_CHARGE, 3);
        epicEnchantments.put(Enchantment.RIPTIDE, 3);
        epicEnchantments.put(Enchantment.SILK_TOUCH, 1);
        epicEnchantments.put(Enchantment.SOUL_SPEED, 3);
        epicEnchantments.put(Enchantment.SWEEPING_EDGE, 3);
        epicEnchantments.put(Enchantment.SWIFT_SNEAK, 3);
        epicEnchantments.put(Enchantment.THORNS, 3);
        epicEnchantments.put(Enchantment.AQUA_AFFINITY, 1);
    }

    public void createLegendaryLootTable() {
        legendaryLootTable.add(Material.BEACON);
        legendaryLootTable.add(Material.DIAMOND_HORSE_ARMOR);
        legendaryLootTable.add(Material.DRAGON_BREATH);
        legendaryLootTable.add(Material.SHULKER_SHELL);
        legendaryLootTable.add(Material.ENCHANTED_BOOK);
        legendaryLootTable.add(Material.ENCHANTED_GOLDEN_APPLE);
        legendaryLootTable.add(Material.NETHERITE_INGOT);
        legendaryLootTable.add(Material.TOTEM_OF_UNDYING);
        legendaryLootTable.add(Material.TRIDENT);
        legendaryLootTable.add(Material.EXPERIENCE_BOTTLE);

        legendaryEnchantments.put(Enchantment.POWER, 5);
        legendaryEnchantments.put(Enchantment.INFINITY, 1);
        legendaryEnchantments.put(Enchantment.SHARPNESS, 5);
        legendaryEnchantments.put(Enchantment.MENDING, 1);
    }

    public ItemStack getEnchantedBook(HashMap<Enchantment, Integer> enchantments, boolean isEpic, boolean isLegendary) {
        int random = getRandomPositiveInteger(enchantments.size());
        int i = 1;
        for (Enchantment enchantment : enchantments.keySet()) {
            if (i != random) {
                i++;
                continue;
            }

            int level = enchantments.get(enchantment);
            ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
            EnchantmentStorageMeta meta = (EnchantmentStorageMeta) book.getItemMeta();
            if (meta == null || enchantment == null) return new ItemStack(Material.AIR);
            if (isLegendary) meta.addStoredEnchant(enchantment, level, false);
            else if (isEpic && level >= 4) meta.addStoredEnchant(enchantment, Math.max(3, getRandomPositiveInteger(level)), false);
            else if (level == 1) meta.addStoredEnchant(enchantment, 1, false);
            else meta.addStoredEnchant(enchantment, Math.max(1, getRandomPositiveInteger(level)), false);
            book.setItemMeta(meta);
            return book;
        }
        return new ItemStack(Material.AIR);
    }

    public int getRandomPositiveInteger(int size) {
        return (int) Math.ceil((Math.random() * size));
    }

    public int getFishingLineNumber(Player p) {
        PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
        if (rewards.getReward("Fishing", "FishingLineV").isApplied()) return 10;
        if (rewards.getReward("Fishing", "FishingLineIV").isApplied()) return 7;
        if (rewards.getReward("Fishing", "FishingLineIII").isApplied()) return 5;
        if (rewards.getReward("Fishing", "FishingLineII").isApplied()) return 3;
        if (rewards.getReward("Fishing", "FishingLineI").isApplied()) return 2;
        return 1;
    }

    public ArrayList<ItemStack> getItemsToDrop(Player p, int luckLevel) {
        ArrayList<ItemStack> items = new ArrayList<>();
        int lineNumber = getFishingLineNumber(p);
        PlayerRewards rewards = plugin.getSkillManager().getPlayerRewards(p);
        double commonPercentage = rewards.getCommonFishingLootChance() + (luckLevel * 0.15);
        double rarePercentage = rewards.getRareFishingLootChance() + (luckLevel * 0.1);
        double epicPercentage = rewards.getEpicFishingLootChance() + (luckLevel * 0.05);
        double legendaryPercentage = rewards.getLegendaryFishingLootChance() + (luckLevel * 0.01);

        if (rewards.getCommonFishingLootChance() == 0) commonPercentage = 0;
        if (rewards.getRareFishingLootChance() == 0) rarePercentage = 0;
        if (rewards.getEpicFishingLootChance() == 0) epicPercentage = 0;
        if (rewards.getLegendaryFishingLootChance() == 0) legendaryPercentage = 0;

        for (int i = 1; i <= lineNumber; i++) {
            double chance = Math.random();
            if (legendaryPercentage != 0 && chance < legendaryPercentage) {
                Material mat = getMaterial(p, 1);
                if (mat.equals(Material.ENCHANTED_BOOK)) {
                    items.add(getEnchantedBook(legendaryEnchantments, false, true));
                }
                else if (mat.equals(Material.EXPERIENCE_BOTTLE)) {
                    items.add(new ItemStack(Material.EXPERIENCE_BOTTLE, Math.max(48, getRandomPositiveInteger(64))));
                }
                else items.add(new ItemStack(mat));
            } else if (epicPercentage != 0 && chance < epicPercentage) {
                Material mat = getMaterial(p, 2);
                if (mat.equals(Material.ENCHANTED_BOOK)) {
                    items.add(getEnchantedBook(epicEnchantments, true, false));
                }
                else if (mat.equals(Material.EXPERIENCE_BOTTLE)) {
                    items.add(new ItemStack(Material.EXPERIENCE_BOTTLE, Math.max(12, getRandomPositiveInteger(24))));
                }
                else if (!mat.equals(Material.GOLDEN_HORSE_ARMOR) && !mat.equals(Material.IRON_HORSE_ARMOR)
                        && !mat.equals(Material.HEART_OF_THE_SEA) && !mat.equals(Material.GHAST_TEAR)) {
                    items.add(new ItemStack(mat, getRandomPositiveInteger(3)));
                }
                else items.add(new ItemStack(mat));
            } else if (rarePercentage != 0 && chance < rarePercentage) {
                Material mat = getMaterial(p, 3);
                if (mat.equals(Material.ENCHANTED_BOOK)) {
                    items.add(getEnchantedBook(rareEnchantments, false, false));
                }
                else if (!mat.equals(Material.LEATHER_HORSE_ARMOR) && !mat.equals(Material.NAME_TAG) && !mat.equals(Material.SADDLE)) {
                    items.add(new ItemStack(mat, getRandomPositiveInteger(3)));
                }
                else items.add(new ItemStack(mat));
            } else if (commonPercentage != 0 && chance < commonPercentage) {
                Material mat = getMaterial(p, 4);
                items.add(new ItemStack(mat, getRandomPositiveInteger(2)));
            }
        }

        return items;
    }

    public Material getMaterial(Player p, int type) {
        if (!nonStackableItems.containsKey(p)) nonStackableItems.put(p, 0);
        if (type == 1) {
            Material mat;
            if (nonStackableItems.get(p) >= 5) return getStackableMaterial(type, p);

            int random = getRandomPositiveInteger(legendaryLootTable.size()) - 1;
            mat = legendaryLootTable.get(random);
            checkStackable(p, mat);
            if (mat.equals(Material.ELYTRA) && !p.getWorld().hasMetadata("killedfirstdragon")) return getMaterial(p, 1);
            return mat;
        }
        else if (type == 2) {
            Material mat;
            if (nonStackableItems.get(p) >= 5) return getStackableMaterial(type, p);

            int random = getRandomPositiveInteger(epicLootTable.size()) - 1;
            mat = epicLootTable.get(random);
            checkStackable(p, mat);
            return mat;
        }
        else if (type == 3) {
            Material mat;
            if (nonStackableItems.get(p) >= 5) return getStackableMaterial(type, p);

            int random = getRandomPositiveInteger(rareLootTable.size()) - 1;
            mat = rareLootTable.get(random);
            checkStackable(p, mat);
            return mat;
        }
        else {
            Material mat;
            if (nonStackableItems.get(p) >= 5) return getStackableMaterial(type, p);

            int random = getRandomPositiveInteger(commonLootTable.size()) - 1;
            mat = commonLootTable.get(random);
            checkStackable(p, mat);
            return mat;
        }
    }

    public void checkStackable(Player p, Material mat) {
        if (mat.toString().contains("HORSE")) nonStackableItems.put(p, nonStackableItems.get(p) + 1);
        if (mat.equals(Material.SADDLE)) nonStackableItems.put(p, nonStackableItems.get(p) + 1);
    }

    public Material getStackableMaterial(int type, Player p) {
        if (type == 1) {
            int random = getRandomPositiveInteger(legendaryLootTable.size()) - 1;
            Material mat = legendaryLootTable.get(random);
            if (mat.toString().contains("HORSE")) return getStackableMaterial(type, p);
            if (mat.equals(Material.SADDLE)) return getStackableMaterial(type, p);
            if (mat.equals(Material.ELYTRA) && !p.getWorld().hasMetadata("killedfirstdragon")) return getStackableMaterial(type, p);
            return mat;
        }
        else if (type == 2) {
            int random = getRandomPositiveInteger(epicLootTable.size()) - 1;
            Material mat = epicLootTable.get(random);
            if (mat.toString().contains("HORSE")) return getStackableMaterial(type, p);
            if (mat.equals(Material.SADDLE)) return getStackableMaterial(type, p);
            return mat;
        }
        else if (type == 3) {
            int random = getRandomPositiveInteger(rareLootTable.size()) - 1;
            Material mat = rareLootTable.get(random);
            if (mat.toString().contains("HORSE")) return getStackableMaterial(type, p);
            if (mat.equals(Material.SADDLE)) return getStackableMaterial(type, p);
            return mat;
        }
        else {
            int random = getRandomPositiveInteger(commonLootTable.size()) - 1;
            Material mat = commonLootTable.get(random);
            if (mat.toString().contains("HORSE")) return getStackableMaterial(type, p);
            if (mat.equals(Material.SADDLE)) return getStackableMaterial(type, p);
            return mat;
        }
    }

    public void handleFishingExperience(Player p) {
        int xp = (int) (Math.random() * 6);
        int xpMultiplier = (int) plugin.getSkillManager().getPlayerRewards(p).getExperienceMultiplier();
        if (xp != 0) p.getWorld().spawn(p.getLocation(), ExperienceOrb.class).setExperience(xp * xpMultiplier);
    }

    public void handleDurability(ItemStack fishingRod) {
        if (fishingRod == null) return;
        if (fishingRod.getType().equals(Material.FISHING_ROD)) {
            Damageable meta = (Damageable) fishingRod.getItemMeta();
            if (meta == null) return;

            if (fishingRod.containsEnchantment(Enchantment.UNBREAKING)) {
                double chance = Math.random() * 100;
                int level = fishingRod.getEnchantmentLevel(Enchantment.UNBREAKING);
                if (chance <= (100f / (level + 1))) meta.setDamage(meta.getDamage() + 1);
            }
            else meta.setDamage(meta.getDamage() + 1);
        }
    }

    public void spawnFishingBoss(World world, Location loc, Vector velocity) {
        Drowned boss = (Drowned) world.spawnEntity(loc, org.bukkit.entity.EntityType.DROWNED);
        boss.setVelocity(velocity);
        boss.setMetadata("fishingboss", new FixedMetadataValue(plugin, true));

        ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
        ItemStack chestplate = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemStack leggings = new ItemStack(Material.DIAMOND_LEGGINGS);
        ItemStack boots = new ItemStack(Material.DIAMOND_BOOTS);
        ItemStack trident = new ItemStack(Material.TRIDENT);

        helmet.addUnsafeEnchantment(Enchantment.THORNS, 6);
        chestplate.addUnsafeEnchantment(Enchantment.THORNS, 6);
        leggings.addUnsafeEnchantment(Enchantment.THORNS, 6);
        boots.addUnsafeEnchantment(Enchantment.THORNS, 6);
        trident.addUnsafeEnchantment(Enchantment.IMPALING, 10);

        if (boss.getEquipment() == null) return;
        boss.getEquipment().setHelmet(helmet);
        boss.getEquipment().setChestplate(chestplate);
        boss.getEquipment().setLeggings(leggings);
        boss.getEquipment().setBoots(boots);
        boss.getEquipment().setItemInMainHand(trident);

        AttributeInstance healthAttribute = boss.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (healthAttribute != null) healthAttribute.setBaseValue(100);
        boss.setHealth(100);
        AttributeInstance speedInstance = boss.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedInstance != null) speedInstance.setBaseValue(0.35);
    }

    public ArrayList<Player> getWaterBreathers() {
        return waterBreathers;
    }

    public ArrayList<Player> getOpenTrashInventories() {
        return openTrashInventories;
    }

    public HashMap<Player, AutoTrash> getTrashInventories() {
        return trashInventories;
    }

    public HashMap<Player, AutoTrash> getPermaTrash() {
        return permaTrash;
    }

    public void addTrashInventory(Player p, AutoTrash trash) {
        trashInventories.put(p, trash);
    }
}
