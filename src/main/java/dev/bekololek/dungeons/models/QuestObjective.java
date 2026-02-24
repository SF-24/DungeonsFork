package dev.bekololek.dungeons.models;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public class QuestObjective {

    private final String displayName;
    private final EntityType mobType;
    private final Material material;
    private final String bossId;
    private final int amount;
    private final int x, y, z;
    private final int radius;
    private final int duration;

    private QuestObjective(Builder builder) {
        this.displayName = builder.displayName;
        this.mobType = builder.mobType;
        this.material = builder.material;
        this.bossId = builder.bossId;
        this.amount = builder.amount;
        this.x = builder.x;
        this.y = builder.y;
        this.z = builder.z;
        this.radius = builder.radius;
        this.duration = builder.duration;
    }

    public String getDisplayName() { return displayName; }
    public EntityType getMobType() { return mobType; }
    public Material getMaterial() { return material; }
    public String getBossId() { return bossId; }
    public int getAmount() { return amount; }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public int getRadius() { return radius; }
    public int getDuration() { return duration; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String displayName;
        private EntityType mobType;
        private Material material;
        private String bossId;
        private int amount;
        private int x, y, z;
        private int radius;
        private int duration;

        public Builder displayName(String displayName) { this.displayName = displayName; return this; }
        public Builder mobType(EntityType mobType) { this.mobType = mobType; return this; }
        public Builder material(Material material) { this.material = material; return this; }
        public Builder bossId(String bossId) { this.bossId = bossId; return this; }
        public Builder amount(int amount) { this.amount = amount; return this; }
        public Builder location(int x, int y, int z) { this.x = x; this.y = y; this.z = z; return this; }
        public Builder radius(int radius) { this.radius = radius; return this; }
        public Builder duration(int duration) { this.duration = duration; return this; }

        public QuestObjective build() {
            return new QuestObjective(this);
        }
    }

    @Override
    public String toString() {
        return "QuestObjective{displayName='" + displayName + "', amount=" + amount + "}";
    }
}
