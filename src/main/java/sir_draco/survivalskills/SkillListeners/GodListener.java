package sir_draco.survivalskills.SkillListeners;

import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import sir_draco.survivalskills.Abilities.GodItems.EnderEssence;
import sir_draco.survivalskills.SurvivalSkills;
import sir_draco.survivalskills.Utils.ItemStackGenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class GodListener implements Listener {

    public static final NamespacedKey potionBagKey = new NamespacedKey(SurvivalSkills.getInstance(), "potion_bag");
    public static int previousPotionBagID = 0;

    private final HashMap<EntityType, ItemStack> godItems = new HashMap<>();
    private final HashMap<Integer, Inventory> potionBags = new HashMap<>();
    private final ArrayList<PotionEffectType> potionEffects = new ArrayList<>();
    private final ArrayList<Inventory> openPotionBags = new ArrayList<>();

    public GodListener() {
        createGodWeaponMap();
        createPotionList();
    }

    @EventHandler
    public void dropGodWeapon(EntityDeathEvent e) {
        EntityType type = e.getEntityType();
        double chance = Math.random();
        if (!godItems.containsKey(type)) return;

        if (type.equals(EntityType.ENDER_DRAGON) && chance <= 0.1)
            e.getDrops().add(godItems.get(type));
        else if (type.equals(EntityType.CREEPER)) {
            Creeper creeper = (Creeper) e.getEntity();
            if (creeper.isPowered() && chance <= 0.01)
                e.getDrops().add(godItems.get(type));
            else if (chance <= 0.001)
                e.getDrops().add(godItems.get(type));
        }
        else if (chance <= 0.001)
            e.getDrops().add(godItems.get(type));
    }

    @EventHandler
    public void onUseGodItem(PlayerInteractEvent e) {
        if (e.getHand() == null || !e.getHand().equals(EquipmentSlot.HAND)) return;
        if (!e.getAction().equals(Action.RIGHT_CLICK_AIR) && !e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;
        Player p = e.getPlayer();
        ItemStack mainHand = p.getInventory().getItemInMainHand();

        if (ItemStackGenerator.isCustomItem(mainHand, 33)) {
            Vector velocity = p.getLocation().getDirection().multiply(2);
            FallingBlock cobweb = p.getWorld().spawnFallingBlock(p.getLocation().clone().add(0, 1, 0),
                    Material.COBWEB.createBlockData());
            cobweb.setHurtEntities(false);
            cobweb.setVelocity(velocity);
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EGG_THROW, 1, 1);
        }
        else if (ItemStackGenerator.isCustomItem(mainHand, 36)) {
            e.setCancelled(true);
            Location loc = p.getLocation().clone().add(p.getLocation().getDirection().multiply(5));
            new EnderEssence(p, loc).runTaskAsynchronously(SurvivalSkills.getInstance());
        }
        else if (ItemStackGenerator.isCustomItem(mainHand, 37)) {
            p.getWorld().createExplosion(p.getLocation(), 5, false, true, p);
        }
        else if (ItemStackGenerator.isCustomItem(mainHand, 38)) {
            int id = getPotionBagID(mainHand);

            if (potionBags.containsKey(id)) {
                openPotionBags.add(potionBags.get(id));
                p.openInventory(potionBags.get(id));
                return;
            }

            // Try to get the bag data from config
            File file = new File(SurvivalSkills.getInstance().getDataFolder(), "potion_bags.yml");
            if (!file.exists()) SurvivalSkills.getInstance().saveResource("potion_bags.yml", false);
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            if (potionBagExists(id, config)) {
                Inventory bag = loadPotionBag(id, config);
                openPotionBags.add(bag);
                p.openInventory(bag);
                return;
            }

            Inventory potionBag = Bukkit.createInventory(null, 9, "Potion Bag");
            potionBags.put(id, potionBag);
            openPotionBags.add(potionBag);
            p.openInventory(potionBag);
        }
    }

    @EventHandler
    public void reviveZombie(PlayerInteractEntityEvent e) {
        if (!e.getHand().equals(EquipmentSlot.HAND)) return;
        if (!e.getRightClicked().getType().equals(EntityType.ZOMBIE_VILLAGER)) return;
        Player p = e.getPlayer();
        ItemStack mainHand = p.getInventory().getItemInMainHand();

        if (ItemStackGenerator.isCustomItem(mainHand, 35)) {
            ZombieVillager zombie = (ZombieVillager) e.getRightClicked();
            zombie.setConversionTime(40);
            Location loc = e.getRightClicked().getLocation();
            if (loc.getWorld() == null) return;
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1, 1);
            loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 30, Math.random(), Math.random(), Math.random());
        }
    }

    @EventHandler
    public void onBowShoot(EntityShootBowEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        ItemStack arrow = e.getConsumable();
        if (!ItemStackGenerator.isCustomItem(arrow, 34)) return;

        Arrow oldArrow = (Arrow) e.getProjectile();
        Arrow projectile = p.launchProjectile(Arrow.class, oldArrow.getVelocity());
        e.setCancelled(true);
        projectile.addCustomEffect(getRandomPotionEffect(), true);
        projectile.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
        projectile.setColor(Color.fromRGB(new Random().nextInt(256), new Random().nextInt(256), new Random().nextInt(256)));

        // p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1, 1);
    }

    @EventHandler
    public void protectPlayerFromCreeperEssence(EntityDamageEvent e) {
        if (!e.getCause().equals(EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) &&
                !e.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)) return;
        if (!(e.getEntity() instanceof Player)) return;
        Player p = (Player) e.getEntity();
        if (ItemStackGenerator.isCustomItem(p.getInventory().getItemInMainHand(), 37))
            e.setCancelled(true);
    }

    @EventHandler
    public void potionBagClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (e.getClick().equals(ClickType.DOUBLE_CLICK)) {
            if (!openPotionBags.contains(p.getOpenInventory().getTopInventory())) return;
            if (e.getCurrentItem() == null) return;
            if (isNotPotion(e.getCurrentItem().getType())) {
                e.setCancelled(true);
                return;
            }
        }

        if (!openPotionBags.contains(e.getInventory())) return;
        if (e.getCursor() == null) return;
        if (isNotPotion(e.getCursor().getType())) {
            e.setCancelled(true);
            p.sendRawMessage(ChatColor.RED + "Only potions can go in this bag");
            p.playSound(p, Sound.ENTITY_ENDERMAN_TELEPORT , 1, 1);
        }
    }

    @EventHandler
    public void potionDrag(InventoryDragEvent e) {
        if (!openPotionBags.contains(e.getInventory())) return;
        Player p = (Player) e.getWhoClicked();
        if (isNotPotion(e.getOldCursor().getType())) {
            e.setCancelled(true);
            p.sendRawMessage(ChatColor.RED + "Only potions can go in this bag");
            p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT , 1, 1);
        }
    }

    @EventHandler
    public void onPotionBagDestroy(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Item)) return;
        Item item = (Item) e.getEntity();
        if (!ItemStackGenerator.isCustomItem(item.getItemStack(), 38)) return;

        int id = getPotionBagID(item.getItemStack());
        item.remove();
        removePotionBag(id);
    }

    public PotionEffect getRandomPotionEffect() {
        return new PotionEffect(potionEffects.get((int) Math.floor(Math.random() * potionEffects.size())),
                160, 0);
    }

    public void setVillagerTypeFromBiome(Villager villager, Biome biome) {
        switch (biome) {
            case ERODED_BADLANDS:
            case WOODED_BADLANDS:
            case DESERT:
            case BADLANDS:
                villager.setVillagerType(Villager.Type.DESERT);
                break;
            case SPARSE_JUNGLE:
            case JUNGLE:
            case BAMBOO_JUNGLE:
                villager.setVillagerType(Villager.Type.JUNGLE);
                break;
            case SAVANNA_PLATEAU:
            case WINDSWEPT_SAVANNA:
            case SAVANNA:
                villager.setVillagerType(Villager.Type.SAVANNA);
                break;
            case ICE_SPIKES:
            case FROZEN_OCEAN:
            case FROZEN_RIVER:
            case FROZEN_PEAKS:
            case DEEP_FROZEN_OCEAN:
            case SNOWY_PLAINS:
            case SNOWY_TAIGA:
            case SNOWY_SLOPES:
            case SNOWY_BEACH:
                villager.setVillagerType(Villager.Type.SNOW);
                break;
            case OLD_GROWTH_PINE_TAIGA:
            case OLD_GROWTH_SPRUCE_TAIGA:
            case TAIGA:
                villager.setVillagerType(Villager.Type.TAIGA);
                break;
            default:
                villager.setVillagerType(Villager.Type.PLAINS);
        }
    }

    public boolean isNotPotion(Material mat) {
        return !mat.equals(Material.POTION)
                && !mat.equals(Material.SPLASH_POTION)
                && !mat.equals(Material.LINGERING_POTION);
    }

    public void savePotionBags(FileConfiguration config) {
        for (Map.Entry<Integer, Inventory> bag : potionBags.entrySet()) {
            config.set(bag.getKey() + ".Items", null);
            if (bag.getValue().isEmpty()) {
                config.set(bag.getKey().toString(), false);
                continue;
            }
            else config.set(bag.getKey().toString(), true);

            int i = 0;
            for (ItemStack item : bag.getValue().getContents()) {
                config.set(bag.getKey() + ".Items." + i, item);
                i++;
            }
        }
    }

    public boolean potionBagExists(int id, FileConfiguration config) {
        return config.contains(String.valueOf(id));
    }

    public Inventory loadPotionBag(int id, FileConfiguration config) {
        Inventory bag = Bukkit.createInventory(null, 9, "Potion Bag");
        potionBags.put(id, bag);
        if (!config.getBoolean(String.valueOf(id))) return bag;
        if (!config.contains(id + ".Items")) return bag;

        config.getConfigurationSection(id + ".Items").getKeys(false).forEach(key -> {
            ItemStack item = config.getItemStack(id + ".Items." + key);
            if (item == null) return;
            bag.addItem(item);
        });
        return bag;
    }

    public void removePotionBag(int id) {
        potionBags.remove(id);

        File potionBagFile = new File(SurvivalSkills.getInstance().getDataFolder(), "potionbags.yml");
        if (!potionBagFile.exists()) SurvivalSkills.getInstance().saveResource("potionbags.yml", true);
        FileConfiguration potionBagData = YamlConfiguration.loadConfiguration(potionBagFile);

        potionBagData.set(String.valueOf(id), null);

        try {
            potionBagData.save(potionBagFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getPotionBagID(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        int id;
        if (container.has(potionBagKey, PersistentDataType.INTEGER)) {
            id = container.get(potionBagKey, PersistentDataType.INTEGER);
        }
        else {
            id = previousPotionBagID++;
            container.set(potionBagKey, PersistentDataType.INTEGER, id);
            item.setItemMeta(meta);
        }

        return id;
    }

    public void createGodWeaponMap() {
        // God Quest Mapping
        godItems.put(EntityType.SPIDER, ItemStackGenerator.getWebShooter());
        godItems.put(EntityType.SKELETON, ItemStackGenerator.getUnlimitedTippedArrow());
        godItems.put(EntityType.ZOMBIE, ItemStackGenerator.getVillagerRevivalArtifact());
        godItems.put(EntityType.ENDERMAN, ItemStackGenerator.getEnderEssence());
        godItems.put(EntityType.CREEPER, ItemStackGenerator.getCreeperEssence());
        godItems.put(EntityType.WITCH, ItemStackGenerator.getPotionBag(previousPotionBagID++));
    }

    public void createPotionList() {
        for (PotionEffectType type : Registry.EFFECT)
            potionEffects.add(type);
    }
}
