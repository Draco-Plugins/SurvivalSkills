package sir_draco.survivalskills.god_questline.trial_mobs;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import sir_draco.survivalskills.SurvivalSkills;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WaveMob {

    private final String name;
    private final EntityType type;
    private final double speed;
    private final double size;
    private final ItemStack hand;
    private final ItemStack[] armor;
    private final Map<ItemStack, Double> drops;

    private int maxHealth;
    private int damage;
    private Entity entity = null;

    public WaveMob(String name, EntityType type, int maxHealth, int damage, double speed, double size, ItemStack hand,
                   ItemStack[] armor, Map<ItemStack, Double> drops) {
        this.name = name;
        this.type = type;
        this.maxHealth = maxHealth;
        this.damage = damage;
        this.speed = speed;
        this.size = size;
        this.hand = hand;
        this.armor = armor;
        this.drops = drops;
    }

    public void spawnMob(Location location, ArrayList<Player> players) {
        if (location.getWorld() == null) return;

        Entity mob = location.getWorld().spawnEntity(location, type);
        mob.setMetadata("trialmob", new FixedMetadataValue(SurvivalSkills.getInstance(), true));
        entity = mob;

        if (mob instanceof Ageable ageable) ageable.setAdult();

        Player closestPlayer = findClosestPlayer(location, players);
        if (mob instanceof Spider spider) spider.setTarget(closestPlayer);
        if (mob instanceof Enderman enderman) enderman.setTarget(closestPlayer);

        if (!(mob instanceof LivingEntity livingEntity)) return;

        livingEntity.setCanPickupItems(false);
        livingEntity.setCustomName(name);
        livingEntity.setCustomNameVisible(true);

        EntityEquipment equipment = livingEntity.getEquipment();
        if (equipment != null) {
            equipment.clear();
            if (hand != null) {
                equipment.setItemInMainHand(hand);
                equipment.setItemInMainHandDropChance(0.1f);
            }
            if (armor != null) {
                if (armor.length > 0) equipment.setBoots(armor[0]);
                if (armor.length > 1) equipment.setLeggings(armor[1]);
                if (armor.length > 2) equipment.setChestplate(armor[2]);
                if (armor.length > 3) equipment.setHelmet(armor[3]);
                equipment.setHelmetDropChance(0.1f);
                equipment.setChestplateDropChance(0.1f);
                equipment.setLeggingsDropChance(0.1f);
                equipment.setBootsDropChance(0.1f);
            }
        }

        AttributeInstance healthAttribute = livingEntity.getAttribute(Attribute.MAX_HEALTH);
        if (healthAttribute != null) {
            healthAttribute.setBaseValue(maxHealth);
            livingEntity.setHealth(maxHealth);
        }

        AttributeInstance damageAttribute = livingEntity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (damageAttribute != null) damageAttribute.setBaseValue(damage);

        AttributeInstance speedAttribute = livingEntity.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttribute != null) speedAttribute.setBaseValue(speed);

        AttributeInstance sizeAttribute = livingEntity.getAttribute(Attribute.SCALE);
        if (sizeAttribute != null) sizeAttribute.setBaseValue(size);
    }

    private Player findClosestPlayer(Location location, List<Player> players) {
        Player closestPlayer = null;
        double closestDistance = Double.MAX_VALUE;
        for (Player player : players) {
            double distance = player.getLocation().distance(location);
            if (distance < closestDistance) {
                closestPlayer = player;
                closestDistance = distance;
            }
        }
        return closestPlayer;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(int maxHealth) {
        this.maxHealth = maxHealth;
    }

    public int getDamage() {
        return damage;
    }

    public void setDamage(int damage) {
        this.damage = damage;
    }

    public void dropItems() {
        if (entity == null || drops == null) return;
        drops.forEach((item, chance) -> {
            if (chance != null && Math.random() < chance) {
                entity.getWorld().dropItemNaturally(entity.getLocation(), item);
            }
        });
    }

    public Entity getEntity() {
        return entity;
    }

    public static class Builder {
        private final String name;
        private final EntityType type;
        private int maxHealth;
        private int damage;
        private double speed;
        private double size;
        private ItemStack hand;
        private ItemStack[] armor;
        private Map<ItemStack, Double> drops;

        public Builder(String name, EntityType type) {
            this.name = name;
            this.type = type;
        }

        public Builder health(int maxHealth) { this.maxHealth = maxHealth; return this; }
        public Builder damage(int damage) { this.damage = damage; return this; }
        public Builder speed(double speed) { this.speed = speed; return this; }
        public Builder size(double size) { this.size = size; return this; }
        public Builder hand(ItemStack hand) { this.hand = hand; return this; }
        public Builder armor(ItemStack[] armor) { this.armor = armor; return this; }
        public Builder drops(Map<ItemStack, Double> drops) { this.drops = drops; return this; }

        public WaveMob build() {
            return new WaveMob(name, type, maxHealth, damage, speed, size, hand, armor, drops);
        }
    }

    public WaveMob duplicate() {
        ItemStack handClone = hand != null ? hand.clone() : null;
        ItemStack[] armorClone = armor != null ? armor.clone() : null;
        Map<ItemStack, Double> dropsClone = drops != null ? new HashMap<>(drops) : null;
        return new WaveMob(name, type, maxHealth, damage, speed, size, handClone, armorClone, dropsClone);
    }
}
