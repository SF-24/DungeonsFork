package dev.bekololek.dungeons.models;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Dungeon {

    private final String id;
    private final String displayName;
    private final List<String> description;
    private final String schematicFile;
    private final int minPartySize;
    private final int maxPartySize;
    private final String difficulty;
    private final int timeLimit;
    private final List<String> questIds;
    private final Material iconMaterial;
    private final int iconCustomModelData;
    private final List<String> rewardTables;
    private final long cooldown;
    private final double entryCost;
    private final String permission;
    private final boolean enabled;
    private final double spawnOffsetX;
    private final double spawnOffsetY;
    private final double spawnOffsetZ;
    private final List<Trigger> triggers;

    public Dungeon(String id, String displayName, List<String> description, String schematicFile,
                   int minPartySize, int maxPartySize, String difficulty, int timeLimit,
                   List<String> questIds, Material iconMaterial, int iconCustomModelData,
                   List<String> rewardTables, long cooldown, double entryCost,
                   String permission, boolean enabled, double spawnOffsetX, double spawnOffsetY,
                   double spawnOffsetZ, List<Trigger> triggers) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.schematicFile = schematicFile;
        this.minPartySize = minPartySize;
        this.maxPartySize = maxPartySize;
        this.difficulty = difficulty;
        this.timeLimit = timeLimit;
        this.questIds = questIds;
        this.iconMaterial = iconMaterial;
        this.iconCustomModelData = iconCustomModelData;
        this.rewardTables = rewardTables;
        this.cooldown = cooldown;
        this.entryCost = entryCost;
        this.permission = permission;
        this.enabled = enabled;
        this.spawnOffsetX = spawnOffsetX;
        this.spawnOffsetY = spawnOffsetY;
        this.spawnOffsetZ = spawnOffsetZ;
        this.triggers = triggers != null ? triggers : new ArrayList<>();
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public List<String> getDescription() { return description; }
    public String getSchematicFile() { return schematicFile; }
    public int getMinPartySize() { return minPartySize; }
    public int getMaxPartySize() { return maxPartySize; }
    public String getDifficulty() { return difficulty; }
    public int getTimeLimit() { return timeLimit; }
    public List<String> getQuestIds() { return questIds; }
    public Material getIconMaterial() { return iconMaterial; }
    public int getIconCustomModelData() { return iconCustomModelData; }
    public List<String> getRewardTables() { return rewardTables; }
    public long getCooldown() { return cooldown; }
    public double getEntryCost() { return entryCost; }
    public String getPermission() { return permission; }
    public boolean isEnabled() { return enabled; }
    public double getSpawnOffsetX() { return spawnOffsetX; }
    public double getSpawnOffsetY() { return spawnOffsetY; }
    public double getSpawnOffsetZ() { return spawnOffsetZ; }

    public List<Trigger> getTriggers() {
        return new ArrayList<>(triggers);
    }

    public Map<String, Trigger> getTriggersMap() {
        Map<String, Trigger> map = new HashMap<>();
        for (Trigger trigger : triggers) {
            map.put(trigger.getId(), trigger);
        }
        return map;
    }

    public Trigger getTrigger(String id) {
        for (Trigger trigger : triggers) {
            if (trigger.getId().equals(id)) {
                return trigger;
            }
        }
        return null;
    }

    public void addTrigger(Trigger trigger) {
        triggers.add(trigger);
    }

    public void removeTrigger(String id) {
        triggers.removeIf(trigger -> trigger.getId().equals(id));
    }

    @Override
    public String toString() {
        return "Dungeon{id='" + id + "', displayName='" + displayName + "', difficulty='" + difficulty + "'}";
    }
}
