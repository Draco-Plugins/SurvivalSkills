package sir_draco.survivalskills.GodQuestline.TrialMobs;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;
import java.util.HashMap;

public class WaveMob {

    private final String name;
    private final EntityType type;
    private final double speed;
    private final double size;
    private final ItemStack hand;
    private final ItemStack[] armor;
    private final HashMap<ItemStack, Double> drops;

    private int health;
    private int damage;
    private Entity entity = null;

    public WaveMob(String name, EntityType type, int health, int damage, double speed, double size, ItemStack hand,
                   ItemStack[] armor, HashMap<ItemStack, Double> drops) {
        this.name = name;
        this.type = type;
        this.health = health;
        this.damage = damage;
        this.speed = speed;
        this.size = size;
        this.hand = hand;
        this.armor = armor;
        this.drops = drops;
    }

    public void spawnMob(Location location, ArrayList<Player> players) {
        // Add mob to wave
        if (location.getWorld() == null) return;
        Entity mob = location.getWorld().spawnEntity(location, type);
        mob.setMetadata("trialmob", new FixedMetadataValue(SurvivalSkills.getInstance(), true));
        entity = mob;

        if (entity instanceof Ageable ageable) ageable.setAdult();
        if (mob instanceof Spider spider) {
            Player closestPlayer = null;
            double closestDistance = Double.MAX_VALUE;
            for (Player player : players) {
                double distance = player.getLocation().distance(location);
                if (distance < closestDistance) {
                    closestPlayer = player;
                    closestDistance = distance;
                }
            }
            spider.setTarget(closestPlayer);
        }
        if (mob instanceof Enderman enderman) {
            Player closestPlayer = null;
            double closestDistance = Double.MAX_VALUE;
            for (Player player : players) {
                double distance = player.getLocation().distance(location);
                if (distance < closestDistance) {
                    closestPlayer = player;
                    closestDistance = distance;
                }
            }
            enderman.setTarget(closestPlayer);
        }

        LivingEntity livingEntity = (LivingEntity) mob;
        livingEntity.setCanPickupItems(false);
        livingEntity.setCustomName(name);
        livingEntity.setCustomNameVisible(true);

        // Ensure the mob isn't wearing armor
        if (livingEntity.getEquipment() != null) livingEntity.getEquipment().clear();
        // Give the mob the specified equipment
        if (livingEntity.getEquipment() != null) {
            if (hand != null) {
                livingEntity.getEquipment().setItemInMainHand(hand);
                livingEntity.getEquipment().setItemInMainHandDropChance(0.1f);
            }
            if (armor != null) {
                for (int i = 0; i < armor.length; i++)
                    livingEntity.getEquipment().setArmorContents(armor);
                livingEntity.getEquipment().setHelmetDropChance(0.1f);
                livingEntity.getEquipment().setChestplateDropChance(0.1f);
                livingEntity.getEquipment().setLeggingsDropChance(0.1f);
                livingEntity.getEquipment().setBootsDropChance(0.1f);
            }
        }

        AttributeInstance healthAttribute = livingEntity.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttribute != null) {
            healthAttribute.setBaseValue(health);
            livingEntity.setHealth(health);
        }

        AttributeInstance damageAttribute = livingEntity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damageAttribute != null) damageAttribute.setBaseValue(damage);

        AttributeInstance speedAttribute = livingEntity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttribute != null) speedAttribute.setBaseValue(speed);

        AttributeInstance sizeAttribute = livingEntity.getAttribute(Attribute.SCALE);
        if (sizeAttribute != null) sizeAttribute.setBaseValue(size);
    }

    public int getHealth() {
        return health;
    }

    public void setMaxHealth(int health) {
        this.health = health;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public void dropItems() {
        if (entity == null) return;
        if (drops == null) return;
        for (ItemStack item : drops.keySet()) {
            double chance = drops.get(item);
            if (Math.random() < chance) entity.getWorld().dropItemNaturally(entity.getLocation(), item);
        }
    }

    public Entity getEntity() {
        return entity;
    }

    public WaveMob duplicate() {
        return new WaveMob(name, type, health, damage, speed, size, hand, armor, drops);
    }
}
