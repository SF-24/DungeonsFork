package dev.bekololek.dungeons.models;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomMob {

    private final String id;
    private EntityType entityType;
    private String customName;
    private boolean showName;
    private double health;
    private double damage;
    private double speed;

    private ItemStack helmet;
    private ItemStack chestplate;
    private ItemStack leggings;
    private ItemStack boots;
    private ItemStack mainHand;
    private ItemStack offHand;

    private float helmetDropChance;
    private float chestplateDropChance;
    private float leggingsDropChance;
    private float bootsDropChance;
    private float mainHandDropChance;
    private float offHandDropChance;

    private List<PotionEffect> potionEffects;

    public CustomMob(String id, EntityType entityType) {
        this.id = id;
        this.entityType = entityType;
        this.customName = null;
        this.showName = true;
        this.health = -1;
        this.damage = -1;
        this.speed = -1;
        this.potionEffects = new ArrayList<>();
        this.helmetDropChance = 0.0f;
        this.chestplateDropChance = 0.0f;
        this.leggingsDropChance = 0.0f;
        this.bootsDropChance = 0.0f;
        this.mainHandDropChance = 0.0f;
        this.offHandDropChance = 0.0f;
    }

    public String getId() { return id; }
    public EntityType getEntityType() { return entityType; }
    public void setEntityType(EntityType entityType) { this.entityType = entityType; }
    public String getCustomName() { return customName; }
    public void setCustomName(String customName) { this.customName = customName; }
    public boolean isShowName() { return showName; }
    public void setShowName(boolean showName) { this.showName = showName; }
    public double getHealth() { return health; }
    public void setHealth(double health) { this.health = health; }
    public double getDamage() { return damage; }
    public void setDamage(double damage) { this.damage = damage; }
    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }

    public ItemStack getHelmet() { return helmet; }
    public void setHelmet(ItemStack helmet) { this.helmet = helmet; }
    public ItemStack getChestplate() { return chestplate; }
    public void setChestplate(ItemStack chestplate) { this.chestplate = chestplate; }
    public ItemStack getLeggings() { return leggings; }
    public void setLeggings(ItemStack leggings) { this.leggings = leggings; }
    public ItemStack getBoots() { return boots; }
    public void setBoots(ItemStack boots) { this.boots = boots; }
    public ItemStack getMainHand() { return mainHand; }
    public void setMainHand(ItemStack mainHand) { this.mainHand = mainHand; }
    public ItemStack getOffHand() { return offHand; }
    public void setOffHand(ItemStack offHand) { this.offHand = offHand; }

    public float getHelmetDropChance() { return helmetDropChance; }
    public void setHelmetDropChance(float v) { this.helmetDropChance = v; }
    public float getChestplateDropChance() { return chestplateDropChance; }
    public void setChestplateDropChance(float v) { this.chestplateDropChance = v; }
    public float getLeggingsDropChance() { return leggingsDropChance; }
    public void setLeggingsDropChance(float v) { this.leggingsDropChance = v; }
    public float getBootsDropChance() { return bootsDropChance; }
    public void setBootsDropChance(float v) { this.bootsDropChance = v; }
    public float getMainHandDropChance() { return mainHandDropChance; }
    public void setMainHandDropChance(float v) { this.mainHandDropChance = v; }
    public float getOffHandDropChance() { return offHandDropChance; }
    public void setOffHandDropChance(float v) { this.offHandDropChance = v; }

    public List<PotionEffect> getPotionEffects() { return new ArrayList<>(potionEffects); }
    public void setPotionEffects(List<PotionEffect> potionEffects) {
        this.potionEffects = potionEffects != null ? new ArrayList<>(potionEffects) : new ArrayList<>();
    }
    public void addPotionEffect(PotionEffect effect) { this.potionEffects.add(effect); }
    public void removePotionEffect(PotionEffectType type) {
        this.potionEffects.removeIf(effect -> effect.getType().equals(type));
    }

    public void applyToEntity(LivingEntity entity) {
        if (customName != null && !customName.isEmpty()) {
            entity.setCustomName(ChatColor.translateAlternateColorCodes('&', customName));
            entity.setCustomNameVisible(showName);
        }

        if (health > 0) {
            entity.getAttribute(Attribute.MAX_HEALTH).setBaseValue(health);
            entity.setHealth(health);
        }

        if (damage > 0 && entity.getAttribute(Attribute.ATTACK_DAMAGE) != null) {
            entity.getAttribute(Attribute.ATTACK_DAMAGE).setBaseValue(damage);
        }

        if (speed > 0) {
            entity.getAttribute(Attribute.MOVEMENT_SPEED).setBaseValue(speed);
        }

        EntityEquipment equipment = entity.getEquipment();
        if (equipment != null) {
            if (helmet != null) { equipment.setHelmet(helmet.clone()); equipment.setHelmetDropChance(helmetDropChance); }
            if (chestplate != null) { equipment.setChestplate(chestplate.clone()); equipment.setChestplateDropChance(chestplateDropChance); }
            if (leggings != null) { equipment.setLeggings(leggings.clone()); equipment.setLeggingsDropChance(leggingsDropChance); }
            if (boots != null) { equipment.setBoots(boots.clone()); equipment.setBootsDropChance(bootsDropChance); }
            if (mainHand != null) { equipment.setItemInMainHand(mainHand.clone()); equipment.setItemInMainHandDropChance(mainHandDropChance); }
            if (offHand != null) { equipment.setItemInOffHand(offHand.clone()); equipment.setItemInOffHandDropChance(offHandDropChance); }
        }

        for (PotionEffect effect : potionEffects) {
            entity.addPotionEffect(effect);
        }
    }

    public static ItemStack createItem(Material material, Map<Enchantment, Integer> enchantments) {
        ItemStack item = new ItemStack(material);
        if (enchantments != null && !enchantments.isEmpty()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                    meta.addEnchant(entry.getKey(), entry.getValue(), true);
                }
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    public static class Builder {
        private final CustomMob mob;

        public Builder(String id, EntityType type) { this.mob = new CustomMob(id, type); }
        public Builder customName(String name) { mob.setCustomName(name); return this; }
        public Builder showName(boolean show) { mob.setShowName(show); return this; }
        public Builder health(double health) { mob.setHealth(health); return this; }
        public Builder damage(double damage) { mob.setDamage(damage); return this; }
        public Builder speed(double speed) { mob.setSpeed(speed); return this; }
        public Builder helmet(ItemStack item, float dropChance) { mob.setHelmet(item); mob.setHelmetDropChance(dropChance); return this; }
        public Builder chestplate(ItemStack item, float dropChance) { mob.setChestplate(item); mob.setChestplateDropChance(dropChance); return this; }
        public Builder leggings(ItemStack item, float dropChance) { mob.setLeggings(item); mob.setLeggingsDropChance(dropChance); return this; }
        public Builder boots(ItemStack item, float dropChance) { mob.setBoots(item); mob.setBootsDropChance(dropChance); return this; }
        public Builder mainHand(ItemStack item, float dropChance) { mob.setMainHand(item); mob.setMainHandDropChance(dropChance); return this; }
        public Builder offHand(ItemStack item, float dropChance) { mob.setOffHand(item); mob.setOffHandDropChance(dropChance); return this; }
        public Builder addPotionEffect(PotionEffect effect) { mob.addPotionEffect(effect); return this; }
        public Builder potionEffects(List<PotionEffect> effects) { mob.setPotionEffects(effects); return this; }
        public CustomMob build() { return mob; }
    }

    @Override
    public String toString() {
        return "CustomMob{id='" + id + "', type=" + entityType + ", name='" + customName + "'}";
    }
}
