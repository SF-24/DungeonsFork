package dev.bekololek.dungeons.models;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

public class DungeonInstance {

    private final UUID instanceId;
    private final Dungeon dungeon;
    private final Party party;
    private final GridSlot gridSlot;
    private final Location spawnLocation;
    private final long startTime;
    private final long endTime;
    private long completionTime;

    private final Map<String, QuestProgress> questProgress;
    private final Set<UUID> spawnedMobs;
    private final Map<UUID, Integer> playerDeaths;

    private final Set<String> firedTriggers;
    private final Map<String, Long> triggerCooldowns;
    private int totalMobKills;

    private InstanceState state;
    private boolean completed;
    private boolean failed;

    public DungeonInstance(UUID instanceId, Dungeon dungeon, Party party, GridSlot gridSlot, Location spawnLocation) {
        this.instanceId = instanceId;
        this.dungeon = dungeon;
        this.party = party;
        this.gridSlot = gridSlot;
        this.spawnLocation = spawnLocation;
        this.startTime = System.currentTimeMillis();

        if (dungeon.getTimeLimit() > 0) {
            this.endTime = startTime + (dungeon.getTimeLimit() * 1000L);
        } else {
            this.endTime = -1;
        }

        this.completionTime = 0;
        this.questProgress = new HashMap<>();
        this.spawnedMobs = new HashSet<>();
        this.playerDeaths = new HashMap<>();
        this.firedTriggers = new HashSet<>();
        this.triggerCooldowns = new HashMap<>();
        this.totalMobKills = 0;
        this.state = InstanceState.STARTING;
        this.completed = false;
        this.failed = false;
    }

    public UUID getInstanceId() { return instanceId; }
    public Dungeon getDungeon() { return dungeon; }
    public Party getParty() { return party; }
    public GridSlot getGridSlot() { return gridSlot; }
    public Location getSpawnLocation() { return spawnLocation; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public InstanceState getState() { return state; }

    public void setState(InstanceState state) { this.state = state; }

    public boolean isCompleted() { return completed; }

    public void setCompleted(boolean completed) {
        this.completed = completed;
        if (completed) {
            this.completionTime = System.currentTimeMillis();
            this.state = InstanceState.COMPLETED;
        }
    }

    public boolean isFailed() { return failed; }

    public void setFailed(boolean failed) {
        this.failed = failed;
        if (failed) {
            this.state = InstanceState.FAILED;
        }
    }

    public boolean isExpired() {
        if (endTime == -1) return false;
        return System.currentTimeMillis() >= endTime;
    }

    public long getRemainingTime() {
        if (endTime == -1) return -1;
        long remaining = endTime - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }

    public long getElapsedTime() {
        return (System.currentTimeMillis() - startTime) / 1000;
    }

    public void initializeQuests(List<Quest> quests) {
        for (Quest quest : quests) {
            questProgress.put(quest.getId(), new QuestProgress(quest));
        }
    }

    public QuestProgress getQuestProgress(String questId) {
        return questProgress.get(questId);
    }

    public Collection<QuestProgress> getAllQuestProgress() {
        return questProgress.values();
    }

    public boolean areRequiredQuestsComplete() {
        for (QuestProgress progress : questProgress.values()) {
            if (progress.getQuest().isRequired() && !progress.isCompleted()) {
                return false;
            }
        }
        return true;
    }

    public boolean areAllQuestsComplete() {
        for (QuestProgress progress : questProgress.values()) {
            if (!progress.isCompleted()) {
                return false;
            }
        }
        return true;
    }

    public List<Player> getOnlinePlayers() {
        return party.getOnlinePlayers();
    }

    public boolean hasPlayer(UUID playerId) {
        return party.isMember(playerId);
    }

    public void addSpawnedMob(UUID mobId) { spawnedMobs.add(mobId); }
    public void removeSpawnedMob(UUID mobId) { spawnedMobs.remove(mobId); }
    public Set<UUID> getSpawnedMobs() { return new HashSet<>(spawnedMobs); }

    public void recordDeath(UUID playerId) {
        playerDeaths.put(playerId, playerDeaths.getOrDefault(playerId, 0) + 1);
    }

    public int getDeathCount(UUID playerId) {
        return playerDeaths.getOrDefault(playerId, 0);
    }

    public boolean isActive() {
        return state == InstanceState.ACTIVE;
    }

    public boolean isFinished() {
        return state == InstanceState.COMPLETED || state == InstanceState.POST_COMPLETION
                || state == InstanceState.FAILED || state == InstanceState.CLEANING_UP;
    }

    public boolean canTriggerFire(Trigger trigger) {
        String triggerId = trigger.getId();

        if (!trigger.isRepeatable() && firedTriggers.contains(triggerId)) {
            return false;
        }

        if (trigger.isRepeatable() && trigger.getCooldown() > 0) {
            Long lastFired = triggerCooldowns.get(triggerId);
            if (lastFired != null) {
                long timeSinceLastFire = (System.currentTimeMillis() - lastFired) / 1000;
                if (timeSinceLastFire < trigger.getCooldown()) {
                    return false;
                }
            }
        }

        return true;
    }

    public void markTriggerFired(Trigger trigger) {
        String triggerId = trigger.getId();
        firedTriggers.add(triggerId);

        if (trigger.isRepeatable() && trigger.getCooldown() > 0) {
            triggerCooldowns.put(triggerId, System.currentTimeMillis());
        }
    }

    public boolean hasTriggerFired(String triggerId) {
        return firedTriggers.contains(triggerId);
    }

    public void incrementMobKills() { this.totalMobKills++; }
    public int getTotalMobKills() { return totalMobKills; }

    public Set<String> getFiredTriggers() { return new HashSet<>(firedTriggers); }

    public void resetTrigger(String triggerId) {
        firedTriggers.remove(triggerId);
        triggerCooldowns.remove(triggerId);
    }

    public long getCompletionTime() { return completionTime; }

    public long getCompletionDuration() {
        if (completionTime == 0) return 0;
        return (completionTime - startTime) / 1000;
    }

    public boolean hasTimeLimit() {
        return endTime != -1;
    }

    public enum InstanceState {
        STARTING,
        ACTIVE,
        COMPLETED,
        POST_COMPLETION,
        FAILED,
        CLEANING_UP
    }

    @Override
    public String toString() {
        return "DungeonInstance{id=" + instanceId + ", dungeon=" + dungeon.getId()
                + ", party=" + party.getPartyId() + ", state=" + state
                + ", slot=" + gridSlot.getCoordinateString() + "}";
    }
}
