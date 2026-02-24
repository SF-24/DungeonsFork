package dev.bekololek.dungeons.models;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {

    private final UUID playerId;
    private final Map<String, Long> dungeonCooldowns;
    private final Map<String, Integer> dungeonCompletions;
    private long totalPlaytime;
    private int totalCompletions;
    private long lastSeen;

    public PlayerData(UUID playerId) {
        this.playerId = playerId;
        this.dungeonCooldowns = new HashMap<>();
        this.dungeonCompletions = new HashMap<>();
        this.totalPlaytime = 0;
        this.totalCompletions = 0;
        this.lastSeen = System.currentTimeMillis();
    }

    public UUID getPlayerId() { return playerId; }

    public Map<String, Long> getDungeonCooldowns() { return new HashMap<>(dungeonCooldowns); }

    public void setDungeonCooldown(String dungeonId, long durationSeconds) {
        long expiryTime = System.currentTimeMillis() + (durationSeconds * 1000);
        dungeonCooldowns.put(dungeonId, expiryTime);
    }

    public boolean hasCooldown(String dungeonId) {
        Long expiry = dungeonCooldowns.get(dungeonId);
        if (expiry == null) return false;

        if (System.currentTimeMillis() >= expiry) {
            dungeonCooldowns.remove(dungeonId);
            return false;
        }

        return true;
    }

    public long getRemainingCooldown(String dungeonId) {
        Long expiry = dungeonCooldowns.get(dungeonId);
        if (expiry == null) return 0;

        long remaining = expiry - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }

    public void clearCooldown(String dungeonId) { dungeonCooldowns.remove(dungeonId); }
    public void clearAllCooldowns() { dungeonCooldowns.clear(); }

    public Map<String, Integer> getDungeonCompletions() { return new HashMap<>(dungeonCompletions); }

    public int getCompletionCount(String dungeonId) {
        return dungeonCompletions.getOrDefault(dungeonId, 0);
    }

    public void incrementCompletion(String dungeonId) {
        int current = getCompletionCount(dungeonId);
        dungeonCompletions.put(dungeonId, current + 1);
        totalCompletions++;
    }

    public long getTotalPlaytime() { return totalPlaytime; }
    public void addPlaytime(long milliseconds) { this.totalPlaytime += milliseconds; }
    public int getTotalCompletions() { return totalCompletions; }
    public long getLastSeen() { return lastSeen; }
    public void updateLastSeen() { this.lastSeen = System.currentTimeMillis(); }

    @Override
    public String toString() {
        return "PlayerData{playerId=" + playerId + ", totalCompletions=" + totalCompletions
                + ", cooldowns=" + dungeonCooldowns.size() + "}";
    }
}
