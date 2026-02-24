package dev.bekololek.dungeons.models;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class Trigger {

    private final String id;
    private TriggerType type;
    private String description;
    private TriggerCondition condition;
    private List<TriggerAction> actions;
    private boolean repeatable;
    private int cooldown;

    public Trigger(String id, TriggerType type, TriggerCondition condition,
                   List<TriggerAction> actions, boolean repeatable, int cooldown) {
        this.id = id;
        this.type = type;
        this.description = "";
        this.condition = condition;
        this.actions = actions;
        this.repeatable = repeatable;
        this.cooldown = cooldown;
    }

    public Trigger(String id, TriggerType type) {
        this.id = id;
        this.type = type;
        this.description = "";
        this.condition = null;
        this.actions = new ArrayList<>();
        this.repeatable = false;
        this.cooldown = 0;
    }

    public String getId() { return id; }
    public TriggerType getType() { return type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TriggerCondition getCondition() { return condition; }
    public List<TriggerAction> getActions() { return actions; }
    public boolean isRepeatable() { return repeatable; }
    public int getCooldown() { return cooldown; }

    public void setType(TriggerType type) { this.type = type; }
    public void setCondition(TriggerCondition condition) { this.condition = condition; }
    public void setRepeatable(boolean repeatable) { this.repeatable = repeatable; }
    public void setCooldown(int cooldown) { this.cooldown = cooldown; }
    public void addAction(TriggerAction action) { this.actions.add(action); }

    // Convenience methods for accessing condition properties
    public double getTriggerX() { return condition != null ? condition.getX() : 0; }
    public void setTriggerX(double x) { if (condition != null) condition.setX(x); }
    public double getTriggerY() { return condition != null ? condition.getY() : 0; }
    public void setTriggerY(double y) { if (condition != null) condition.setY(y); }
    public double getTriggerZ() { return condition != null ? condition.getZ() : 0; }
    public void setTriggerZ(double z) { if (condition != null) condition.setZ(z); }
    public double getTriggerRadius() { return condition != null ? condition.getRadius() : 0; }
    public void setTriggerRadius(double radius) { if (condition != null) condition.setRadius(radius); }
    public int getTriggerTime() { return condition != null ? condition.getTime() : 0; }
    public void setTriggerTime(int time) { if (condition != null) condition.setTime(time); }
    public String getQuestId() { return condition != null ? condition.getQuestId() : null; }
    public void setQuestId(String questId) { if (condition != null) condition.setQuestId(questId); }
    public EntityType getMobType() { return condition != null ? condition.getMobType() : null; }
    public void setMobType(EntityType mobType) { if (condition != null) condition.setMobType(mobType); }
    public int getKillCount() { return condition != null ? condition.getMobCount() : 0; }
    public void setKillCount(int count) { if (condition != null) condition.setMobCount(count); }
    public String getBossId() { return condition != null ? condition.getBossId() : null; }
    public void setBossId(String bossId) { if (condition != null) condition.setBossId(bossId); }

    @Override
    public String toString() {
        return "Trigger{id='" + id + "', type=" + type + ", repeatable=" + repeatable + "}";
    }

    public enum TriggerType {
        LOCATION,
        TIMER,
        MOB_KILL,
        QUEST_COMPLETE,
        PLAYER_DEATH,
        BOSS_KILL
    }

    public static class TriggerCondition {
        private final TriggerType type;
        private double x, y, z;
        private double radius;
        private int time;
        private int mobCount;
        private EntityType mobType;
        private String questId;
        private String bossId;

        private TriggerCondition(TriggerType type) {
            this.type = type;
        }

        public static TriggerCondition location(double x, double y, double z, double radius) {
            TriggerCondition condition = new TriggerCondition(TriggerType.LOCATION);
            condition.x = x; condition.y = y; condition.z = z; condition.radius = radius;
            return condition;
        }

        public static TriggerCondition timer(int seconds) {
            TriggerCondition condition = new TriggerCondition(TriggerType.TIMER);
            condition.time = seconds;
            return condition;
        }

        public static TriggerCondition mobKill(int count) {
            TriggerCondition condition = new TriggerCondition(TriggerType.MOB_KILL);
            condition.mobCount = count;
            return condition;
        }

        public static TriggerCondition questComplete(String questId) {
            TriggerCondition condition = new TriggerCondition(TriggerType.QUEST_COMPLETE);
            condition.questId = questId;
            return condition;
        }

        public static TriggerCondition bossKill(String bossId) {
            TriggerCondition condition = new TriggerCondition(TriggerType.BOSS_KILL);
            condition.bossId = bossId;
            return condition;
        }

        public TriggerType getType() { return type; }
        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public double getRadius() { return radius; }
        public int getTime() { return time; }
        public int getMobCount() { return mobCount; }
        public String getQuestId() { return questId; }
        public String getBossId() { return bossId; }
        public EntityType getMobType() { return mobType; }

        public void setX(double x) { this.x = x; }
        public void setY(double y) { this.y = y; }
        public void setZ(double z) { this.z = z; }
        public void setRadius(double radius) { this.radius = radius; }
        public void setTime(int time) { this.time = time; }
        public void setMobCount(int mobCount) { this.mobCount = mobCount; }
        public void setMobType(EntityType mobType) { this.mobType = mobType; }
        public void setQuestId(String questId) { this.questId = questId; }
        public void setBossId(String bossId) { this.bossId = bossId; }

        public boolean isLocationInRange(Location location, Location dungeonOrigin) {
            if (type != TriggerType.LOCATION) return false;

            double triggerX = dungeonOrigin.getX() + x;
            double triggerY = dungeonOrigin.getY() + y;
            double triggerZ = dungeonOrigin.getZ() + z;

            double dx = location.getX() - triggerX;
            double dy = location.getY() - triggerY;
            double dz = location.getZ() - triggerZ;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            return distance <= radius;
        }
    }

    public static class TriggerAction {
        private final ActionType type;
        private EntityType mobType;
        private String customMobId;
        private int mobCount;
        private double spawnX, spawnY, spawnZ;
        private double teleportX, teleportY, teleportZ;
        private String itemMaterial;
        private int itemAmount;
        private double damage;
        private String message;
        private String command;
        private PotionEffectType potionType;
        private int potionDuration;
        private int potionAmplifier;

        private TriggerAction(ActionType type) {
            this.type = type;
        }

        public static TriggerAction spawnMob(EntityType mobType, int count, double x, double y, double z) {
            TriggerAction action = new TriggerAction(ActionType.SPAWN_MOB);
            action.mobType = mobType; action.mobCount = count;
            action.spawnX = x; action.spawnY = y; action.spawnZ = z;
            return action;
        }

        public static TriggerAction spawnCustomMob(String customMobId, int count, double x, double y, double z) {
            TriggerAction action = new TriggerAction(ActionType.SPAWN_MOB);
            action.customMobId = customMobId; action.mobCount = count;
            action.spawnX = x; action.spawnY = y; action.spawnZ = z;
            return action;
        }

        public static TriggerAction dropItem(org.bukkit.Material material, int amount, double x, double y, double z) {
            TriggerAction action = new TriggerAction(ActionType.DROP_ITEM);
            action.itemMaterial = material != null ? material.name() : "DIAMOND";
            action.itemAmount = amount;
            action.spawnX = x; action.spawnY = y; action.spawnZ = z;
            return action;
        }

        public static TriggerAction damagePlayer(double damage) {
            TriggerAction action = new TriggerAction(ActionType.DAMAGE_PLAYER);
            action.damage = damage;
            return action;
        }

        public static TriggerAction teleport(double x, double y, double z) {
            TriggerAction action = new TriggerAction(ActionType.TELEPORT);
            action.teleportX = x; action.teleportY = y; action.teleportZ = z;
            return action;
        }

        public static TriggerAction message(String message) {
            TriggerAction action = new TriggerAction(ActionType.MESSAGE);
            action.message = message;
            return action;
        }

        public static TriggerAction command(String command) {
            TriggerAction action = new TriggerAction(ActionType.COMMAND);
            action.command = command;
            return action;
        }

        public static TriggerAction potionEffect(PotionEffectType type, int duration, int amplifier) {
            TriggerAction action = new TriggerAction(ActionType.POTION_EFFECT);
            action.potionType = type; action.potionDuration = duration; action.potionAmplifier = amplifier;
            return action;
        }

        public ActionType getType() { return type; }
        public EntityType getMobType() { return mobType; }
        public String getCustomMobId() { return customMobId; }
        public int getMobCount() { return mobCount; }
        public double getSpawnX() { return spawnX; }
        public double getSpawnY() { return spawnY; }
        public double getSpawnZ() { return spawnZ; }
        public String getItemMaterial() { return itemMaterial; }
        public int getItemAmount() { return itemAmount; }
        public double getDamage() { return damage; }
        public String getMessage() { return message; }
        public String getCommand() { return command; }
        public PotionEffectType getPotionType() { return potionType; }
        public int getPotionDuration() { return potionDuration; }
        public int getPotionAmplifier() { return potionAmplifier; }
        public double getTeleportX() { return teleportX; }
        public double getTeleportY() { return teleportY; }
        public double getTeleportZ() { return teleportZ; }

        public double getDropX() { return spawnX; }
        public double getDropY() { return spawnY; }
        public double getDropZ() { return spawnZ; }

        public org.bukkit.Material getDropMaterial() {
            if (itemMaterial != null) {
                try { return org.bukkit.Material.valueOf(itemMaterial); }
                catch (IllegalArgumentException e) { return null; }
            }
            return null;
        }

        public int getDropAmount() { return itemAmount; }

        // Setters for editor
        public void setMobType(EntityType mobType) { this.mobType = mobType; }
        public void setCustomMobId(String customMobId) { this.customMobId = customMobId; }
        public void setMobCount(int mobCount) { this.mobCount = mobCount; }
        public void setSpawnX(double spawnX) { this.spawnX = spawnX; }
        public void setSpawnY(double spawnY) { this.spawnY = spawnY; }
        public void setSpawnZ(double spawnZ) { this.spawnZ = spawnZ; }
        public void setTeleportX(double teleportX) { this.teleportX = teleportX; }
        public void setTeleportY(double teleportY) { this.teleportY = teleportY; }
        public void setTeleportZ(double teleportZ) { this.teleportZ = teleportZ; }
        public void setDropMaterial(org.bukkit.Material material) { this.itemMaterial = material != null ? material.name() : null; }
        public void setDropAmount(int amount) { this.itemAmount = amount; }
        public void setDropX(double x) { this.spawnX = x; }
        public void setDropY(double y) { this.spawnY = y; }
        public void setDropZ(double z) { this.spawnZ = z; }
        public void setDamage(double damage) { this.damage = damage; }
        public void setMessage(String message) { this.message = message; }
        public void setCommand(String command) { this.command = command; }
        public void setPotionType(PotionEffectType potionType) { this.potionType = potionType; }
        public void setPotionDuration(int potionDuration) { this.potionDuration = potionDuration; }
        public void setPotionAmplifier(int potionAmplifier) { this.potionAmplifier = potionAmplifier; }

        public enum ActionType {
            SPAWN_MOB,
            DROP_ITEM,
            DAMAGE_PLAYER,
            MESSAGE,
            COMMAND,
            TELEPORT,
            POTION_EFFECT
        }
    }

    public static class Builder {
        private String id;
        private TriggerType type;
        private String description = "";
        private TriggerCondition condition;
        private List<TriggerAction> actions = new ArrayList<>();
        private boolean repeatable = false;
        private int cooldown = 0;

        public Builder(String id) { this.id = id; }

        public Builder description(String description) { this.description = description; return this; }

        public Builder location(double x, double y, double z, double radius) {
            this.type = TriggerType.LOCATION;
            this.condition = TriggerCondition.location(x, y, z, radius);
            return this;
        }

        public Builder timer(int seconds) {
            this.type = TriggerType.TIMER;
            this.condition = TriggerCondition.timer(seconds);
            return this;
        }

        public Builder mobKill(int count) {
            this.type = TriggerType.MOB_KILL;
            this.condition = TriggerCondition.mobKill(count);
            return this;
        }

        public Builder questComplete(String questId) {
            this.type = TriggerType.QUEST_COMPLETE;
            this.condition = TriggerCondition.questComplete(questId);
            return this;
        }

        public Builder bossKill(String bossId) {
            this.type = TriggerType.BOSS_KILL;
            this.condition = TriggerCondition.bossKill(bossId);
            return this;
        }

        public Builder addAction(TriggerAction action) { this.actions.add(action); return this; }
        public Builder repeatable(boolean repeatable) { this.repeatable = repeatable; return this; }
        public Builder cooldown(int seconds) { this.cooldown = seconds; return this; }

        public Trigger build() {
            Trigger trigger = new Trigger(id, type, condition, actions, repeatable, cooldown);
            trigger.setDescription(description);
            return trigger;
        }
    }
}
